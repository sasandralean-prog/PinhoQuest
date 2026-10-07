package com.pinhoquest.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoParchment
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoTagGraphicButton

@Composable
fun ProfileScreen(
    ownerName: String,
    tags: List<Tag>,
    onTagToggled: (TagId, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    dark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            PinhoQuestBackgrounds.HOME_NIGHT,
            overlay = if (dark) Color(0xFF0B1E38) else Color(0xFFFFE8BC),
            overlayAlpha = if (dark) 0.18f else 0.10f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Perfil",
                    color = Color.White,
                    fontSize = 34.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "⚙",
                    color = Color.White,
                    fontSize = 34.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickableProfile(onOpenSettings),
                )
            }

            Spacer(Modifier.height(12.dp))

            PinhoParchment(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Jardim de $ownerName",
                    color = PinhoInk,
                    fontSize = 24.sp,
                )
                Text(
                    "Suas preferências",
                    color = PinhoForest,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(12.dp))

                tags.take(12).chunked(3).forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        row.forEachIndexed { colIndex, tag ->
                            PinhoTagGraphicButton(
                                index = rowIndex * 3 + colIndex,
                                contentDescription = tag.label,
                                selected = tag.enabled,
                                onClick = { onTagToggled(tag.id, !tag.enabled) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Descreva de forma livre o que você gosta e te faz feliz e tornaremos parte das suas quests. ✎",
                    color = PinhoInk,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pequenas preferências, grandes descobertas.",
                    color = PinhoForest,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private fun Modifier.clickableProfile(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
