package com.pinhoquest.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.R
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoParchment
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoTagGraphicButton
import com.pinhoquest.ui.PinhoVisualTagIds

@Composable
fun ProfileScreen(
    ownerName: String,
    tags: List<Tag>,
    onTagToggled: (TagId, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    dark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val foreground = if (dark) Color(0xFFFFF0D0) else PinhoForest
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            resource = if (dark) PinhoQuestBackgrounds.PROFILE_NIGHT else PinhoQuestBackgrounds.PROFILE_DAY,
            overlayAlpha = 0f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .padding(bottom = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Perfil",
                    color = foreground,
                    fontSize = 34.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "⚙",
                    color = foreground,
                    fontSize = 34.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .semantics {
                            contentDescription = "Abrir configurações"
                            role = Role.Button
                        }
                        .clickableProfile(onOpenSettings),
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (dark) 972f / 448f else 996f / 461f),
            ) {
                Image(
                    painter = painterResource(
                        if (dark) R.drawable.card_profile_night else R.drawable.card_profile_day,
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Jardim de $ownerName",
                        color = PinhoInk,
                        fontSize = 24.sp,
                    )
                    Text(
                        "Pequenas preferências, grandes descobertas.",
                        color = PinhoForest,
                        fontSize = 14.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1304f / 376f),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.card_profile_tags),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    "Suas preferências",
                    color = PinhoForest,
                    fontSize = 20.sp,
                )
            }

            PinhoParchment(modifier = Modifier.fillMaxWidth()) {
                val canonicalTags = PinhoVisualTagIds.mapNotNull { id ->
                    tags.firstOrNull { it.id == id } ?: SystemTagCatalog.byId(id.value)
                }
                canonicalTags.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        row.forEach { tag ->
                            PinhoTagGraphicButton(
                                tagId = tag.id,
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
            }
        }
    }
}

private fun Modifier.clickableProfile(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
