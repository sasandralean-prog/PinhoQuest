package com.pinhoquest.ui.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.R
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.PinhoBackButton
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoTagGraphicButton
import com.pinhoquest.ui.PinhoGraphicButton

/**
 * The selection surface opened by Home > SORTEAR QUEST.
 *
 * It edits the same persisted system-theme/tag state used by Profile. It does
 * not own a second selection store.
 */
@Composable
fun QuestThemeSelectionScreen(
    tags: List<Tag>,
    onTagToggled: (TagId, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val canonicalTags = SystemTagCatalog.all.mapNotNull { canonical ->
        tags.firstOrNull { it.id == canonical.id } ?: canonical
    }

    Box(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            resource = if (dark) PinhoQuestBackgrounds.HOME_NIGHT else PinhoQuestBackgrounds.HOME_DAY,
            modifier = Modifier.fillMaxSize(),
            overlay = if (dark) Color(0xFF061B36) else Color(0xFFFFE7B0),
            overlayAlpha = if (dark) 0.10f else 0.16f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                PinhoBackButton(onClick = onBack)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "O que vamos descobrir?",
                color = if (dark) Color.White else PinhoForest,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Escolha um ou vários temas. Você pode combinar todos.",
                color = if (dark) Color.White.copy(alpha = 0.92f) else PinhoInk,
                fontSize = 15.sp,
            )

            Spacer(Modifier.height(18.dp))

            canonicalTags.chunked(3).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEachIndexed { columnIndex, tag ->
                        PinhoTagGraphicButton(
                            index = rowIndex * 3 + columnIndex,
                            contentDescription = tag.label,
                            selected = tag.enabled,
                            onClick = { onTagToggled(tag.id, !tag.enabled) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            val selectedCount = canonicalTags.count { it.enabled }
            Text(
                text = if (selectedCount == 0) {
                    "Nenhum tema selecionado — vou escolher por você."
                } else {
                    "$selectedCount temas selecionados. Essa escolha fica salva para a próxima vez."
                },
                color = if (dark) Color.White else PinhoInk,
                fontSize = 14.sp,
            )

            Spacer(Modifier.height(16.dp))
            PinhoGraphicButton(
                resource = R.drawable.pq_btn_continue,
                contentDescription = "Sortear com estes temas",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(0.66f),
                aspectRatio = 3.2f,
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}
