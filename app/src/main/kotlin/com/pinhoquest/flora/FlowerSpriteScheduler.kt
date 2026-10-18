package com.pinhoquest.flora

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class FlowerSpriteRequest(
    val scientificName: String,
    val commonName: String,
)

/**
 * Enqueues one background research job per distinct taxon and writes a deterministic
 * collection manifest. Cached sprites and recent "no eligible result" markers avoid
 * unnecessary downloads whenever the garden state is rebuilt.
 */
class FlowerSpriteScheduler(context: Context) {
    private val appContext = context.applicationContext

    fun scheduleCollection(flowers: List<FlowerSpriteRequest>): String {
        val validFlowers = flowers
            .filter { FlowerSpriteIdentity.normalizeScientificName(it.scientificName).isNotBlank() }
            .distinctBy { FlowerSpriteIdentity.flowerSha256(it.scientificName) }
        val flowerShas = validFlowers.map { FlowerSpriteIdentity.flowerSha256(it.scientificName) }
        val collectionSha = FlowerSpriteIdentity.collectionSha256(flowerShas)
        writeCollectionManifest(collectionSha, validFlowers, flowerShas)

        validFlowers.forEach { flower ->
            val flowerSha = FlowerSpriteIdentity.flowerSha256(flower.scientificName)
            val folder = File(appContext.filesDir, "flora-sprites/$flowerSha")
            if (hasValidSprite(folder, flowerSha) || hasFreshNegativeCache(folder)) return@forEach

            val request = OneTimeWorkRequestBuilder<FlowerSpriteWorker>()
                .setInputData(
                    workDataOf(
                        FlowerSpriteWorker.KEY_SCIENTIFIC_NAME to flower.scientificName,
                        FlowerSpriteWorker.KEY_COMMON_NAME to flower.commonName,
                        FlowerSpriteWorker.KEY_FLOWER_SHA to flowerSha,
                        FlowerSpriteWorker.KEY_COLLECTION_SHA to collectionSha,
                    ),
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                .addTag(FlowerSpriteWorker.TAG)
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                "flower-sprite-$flowerSha",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
        return collectionSha
    }

    private fun hasValidSprite(folder: File, flowerSha: String): Boolean {
        val sprite = File(folder, FlowerSpriteWorker.SPRITE_FILE)
        val metadata = File(folder, FlowerSpriteWorker.METADATA_FILE)
        if (!sprite.isFile || sprite.length() <= 0L || !metadata.isFile) return false
        return runCatching {
            val json = JSONObject(metadata.readText(StandardCharsets.UTF_8))
            json.optString("flowerSha256") == flowerSha &&
                json.optJSONObject("sprite")?.optString("sha256").orEmpty().isNotBlank()
        }.getOrDefault(false)
    }

    private fun hasFreshNegativeCache(folder: File): Boolean {
        val marker = File(folder, FlowerSpriteWorker.STATUS_FILE)
        if (!marker.isFile) return false
        return runCatching {
            JSONObject(marker.readText(StandardCharsets.UTF_8))
                .optLong("retryAfterEpochMillis", 0L) > System.currentTimeMillis()
        }.getOrDefault(false)
    }

    private fun writeCollectionManifest(
        collectionSha: String,
        flowers: List<FlowerSpriteRequest>,
        flowerShas: List<String>,
    ) {
        val directory = File(appContext.filesDir, "flora-sprites/collections")
        if (!directory.exists() && !directory.mkdirs()) return
        val target = File(directory, "$collectionSha.json")
        if (target.isFile) return
        val members = flowers.sortedBy { FlowerSpriteIdentity.normalizeScientificName(it.scientificName) }
        val json = JSONObject()
            .put("schemaVersion", 1)
            .put("identitySchemaVersion", FlowerSpriteIdentity.IDENTITY_SCHEMA_VERSION)
            .put("collectionSha256", collectionSha)
            .put("hashAlgorithm", "SHA-256")
            .put("memberFlowerSha256s", JSONArray(flowerShas.distinct().sorted()))
            .put(
                "flowers",
                JSONArray().apply {
                    members.forEach { flower ->
                        put(
                            JSONObject()
                                .put("commonName", flower.commonName)
                                .put("scientificName", FlowerSpriteIdentity.normalizeScientificName(flower.scientificName))
                                .put("flowerSha256", FlowerSpriteIdentity.flowerSha256(flower.scientificName)),
                        )
                    }
                },
            )
            .put("createdAtUtc", java.time.Instant.now().toString())
        val temporary = File(directory, "$collectionSha.json.part")
        temporary.writeText(json.toString(2), StandardCharsets.UTF_8)
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }
}
