package com.pinhoquest.flora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class FlowerArtworkCredit(
    val creator: String,
    val licenseName: String,
    val sourcePageUrl: String,
    val sourceSha256: String,
    val spriteSha256: String,
)

@Composable
fun FlowerArtworkAttribution(
    flowerSha256: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var credit by remember(flowerSha256) { mutableStateOf<FlowerArtworkCredit?>(null) }

    LaunchedEffect(flowerSha256) {
        credit = withContext(Dispatchers.IO) {
            val metadata = File(
                context.filesDir,
                "${FlowerSpriteWorker.ROOT_DIRECTORY}/$flowerSha256/${FlowerSpriteWorker.METADATA_FILE}",
            )
            if (!metadata.isFile) return@withContext null
            runCatching {
                val record = JSONObject(metadata.readText(StandardCharsets.UTF_8))
                require(record.optString("flowerSha256") == flowerSha256)
                val original = record.optJSONObject("originalPhoto") ?: JSONObject()
                val copyright = record.optJSONObject("copyright") ?: JSONObject()
                val sprite = record.optJSONObject("sprite") ?: JSONObject()
                FlowerArtworkCredit(
                    creator = copyright.optString("creator").ifBlank { "Autor não informado pela fonte" },
                    licenseName = copyright.optString("licenseName").ifBlank { "Licença não identificada" },
                    sourcePageUrl = original.optString("sourcePageUrl"),
                    sourceSha256 = original.optString("sha256"),
                    spriteSha256 = sprite.optString("sha256"),
                )
            }.getOrNull()
        }
    }

    val sourceCredit = credit ?: return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("Arte floral gerada a partir de fotografia", fontSize = 13.sp)
        Text("Autor: ${sourceCredit.creator}", fontSize = 12.sp)
        Text("Licença declarada: ${sourceCredit.licenseName}", fontSize = 12.sp)
        if (sourceCredit.sourceSha256.isNotBlank()) {
            Text("SHA-256 da foto: ${sourceCredit.sourceSha256.take(16)}…", fontSize = 10.sp)
        }
        if (sourceCredit.spriteSha256.isNotBlank()) {
            Text("SHA-256 do sprite: ${sourceCredit.spriteSha256.take(16)}…", fontSize = 10.sp)
        }
        if (sourceCredit.sourcePageUrl.startsWith("https://commons.wikimedia.org/")) {
            TextButton(
                onClick = { runCatching { uriHandler.openUri(sourceCredit.sourcePageUrl) } },
            ) { Text("Ver foto original e licença") }
        }
    }
}
