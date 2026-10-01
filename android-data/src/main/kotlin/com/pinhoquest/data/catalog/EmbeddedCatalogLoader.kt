package com.pinhoquest.data.catalog

import com.pinhoquest.core.garden.CatalogPackValidator
import com.pinhoquest.core.garden.CatalogValidationResult
import com.pinhoquest.core.garden.FlowerRarityScale
import com.pinhoquest.domain.garden.CatalogEntry
import com.pinhoquest.domain.garden.CatalogPack
import com.pinhoquest.domain.garden.CatalogPackId
import com.pinhoquest.domain.garden.CountingBasis
import com.pinhoquest.domain.garden.EstimateConfidence
import com.pinhoquest.domain.garden.FlowerDefinition
import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.garden.GlobalPopulationEstimate
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class EmbeddedCatalogLoadResult(
    val pack: CatalogPack,
    val scale: FlowerRarityScale,
    val validation: CatalogValidationResult,
)

object EmbeddedCatalogLoader {
    private const val RESOURCE_PATH = "catalog/collection_i.json"

    fun loadCollectionI(): EmbeddedCatalogLoadResult {
        val bytes = requireNotNull(
            EmbeddedCatalogLoader::class.java.classLoader?.getResourceAsStream(RESOURCE_PATH),
        ) { "Missing embedded catalog resource: $RESOURCE_PATH" }.use { it.readBytes() }
        val raw = bytes.toString(Charsets.UTF_8)
        val root = Json.parseToJsonElement(raw).jsonObject
        val scale = parseScale(root.requiredObject("scale"))
        val entries = root.requiredArray("entries").map { element ->
            parseEntry(element.jsonObject, scale)
        }
        val sourceUris = entries
            .flatMap { it.flower.populationEstimate?.evidenceSourceUris.orEmpty() }
            .toSet()
        val pack = CatalogPack(
            id = CatalogPackId(root.requiredString("packId")),
            version = root.requiredInt("version"),
            generatedAtEpochMillis = root.requiredLong("generatedAtEpochMillis"),
            entries = entries,
            sourceUris = sourceUris,
            integrityHash = sha256(bytes),
        )
        val validation = CatalogPackValidator(scale).validate(pack)
        return EmbeddedCatalogLoadResult(pack, scale, validation)
    }

    private fun parseScale(obj: JsonObject) = FlowerRarityScale(
        version = obj.requiredInt("version"),
        referenceMinPopulation = obj.requiredLong("referenceMinPopulation"),
        referenceMaxPopulation = obj.requiredLong("referenceMaxPopulation"),
        requiredCountingBasis = CountingBasis.valueOf(obj.requiredString("requiredCountingBasis")),
    )

    private fun parseEntry(
        obj: JsonObject,
        scale: FlowerRarityScale,
    ): CatalogEntry {
        val population = obj["population"]?.jsonObject?.let(::parsePopulation)
        val rarity = population?.let(scale::classify) ?: FlowerRarity.UNKNOWN
        return CatalogEntry(
            flower = FlowerDefinition(
                id = FlowerId(obj.requiredString("id")),
                commonName = obj.requiredString("commonName"),
                scientificName = obj.requiredString("scientificName"),
                description = obj.requiredString("description"),
                rarity = rarity,
                rarityScaleVersion = if (rarity == FlowerRarity.UNKNOWN) null else scale.version,
                populationEstimate = population,
            ),
            eligible = obj.requiredBoolean("eligible"),
        )
    }

    private fun parsePopulation(obj: JsonObject) = GlobalPopulationEstimate(
        estimatedIndividuals = obj.optionalLong("estimatedIndividuals"),
        lowerBound = obj.optionalLong("lowerBound"),
        upperBound = obj.optionalLong("upperBound"),
        estimateDateEpochMillis = obj.optionalLong("estimateDateEpochMillis"),
        evidenceSourceUris = obj.requiredArray("evidenceSourceUris")
            .mapNotNull { it.jsonPrimitive.contentOrNull }
            .toSet(),
        confidence = EstimateConfidence.valueOf(obj.requiredString("confidence")),
        countingBasis = CountingBasis.valueOf(obj.requiredString("countingBasis")),
    )

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
}

private fun JsonObject.requiredString(key: String): String =
    requireNotNull(this[key]?.jsonPrimitive?.contentOrNull) { "Missing string: $key" }

private fun JsonObject.requiredInt(key: String): Int =
    requiredLong(key).toInt()

private fun JsonObject.requiredLong(key: String): Long =
    requireNotNull(this[key]?.jsonPrimitive?.longOrNull) { "Missing long: $key" }

private fun JsonObject.optionalLong(key: String): Long? =
    this[key]?.jsonPrimitive?.longOrNull

private fun JsonObject.requiredBoolean(key: String): Boolean =
    requireNotNull(this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()) {
        "Missing boolean: $key"
    }

private fun JsonObject.requiredObject(key: String): JsonObject =
    requireNotNull(this[key]) { "Missing object: $key" }.jsonObject

private fun JsonObject.requiredArray(key: String) =
    requireNotNull(this[key]) { "Missing array: $key" }.jsonArray
