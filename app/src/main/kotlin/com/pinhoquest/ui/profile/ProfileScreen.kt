package com.pinhoquest.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import com.pinhoquest.R
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.domain.tag.TagSource
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.ui.LocalPinhoBottomNavigationInset
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoDisplayFontFamily
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
    onCreateCustomTag: (String) -> Unit = {},
    onDeleteCustomTag: (TagId) -> Unit = {},
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
                .padding(bottom = LocalPinhoBottomNavigationInset.current + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Perfil",
                    color = foreground,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = PinhoDisplayFontFamily,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(996f / 461f),
            ) {
                Image(
                    painter = painterResource(
                        if (dark) R.drawable.card_profile_night else R.drawable.card_profile_day,
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                // Leaf crest occupies the top; dynamic copy belongs in the lower parchment plate.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.82f)
                        .padding(bottom = maxHeight * 0.01f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Jardim de $ownerName",
                        color = PinhoInk,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            lineHeight = 19.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        text = "Pequenas preferências, grandes descobertas.",
                        color = PinhoForest,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    "Descreva de forma livre o que você gosta e te faz feliz e tornaremos parte das suas quests.",
                    color = PinhoInk,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(12.dp))
                val customTags = tags.filter { it.source == TagSource.USER }
                var customTagDraft by rememberSaveable { mutableStateOf("") }
                Text(
                    "Seus temas personalizados (${customTags.size}/10)",
                    color = PinhoForest,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Crie até 10 temas com nomes de até 28 caracteres.",
                    color = PinhoInk,
                    fontSize = 12.sp,
                )
                OutlinedTextField(
                    value = customTagDraft,
                    onValueChange = { value ->
                        if (value.codePointCount(0, value.length) <= 28) customTagDraft = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Novo tema") },
                    placeholder = { Text("Ex.: astronomia, jardinagem…") },
                )
                TextButton(
                    onClick = {
                        val label = customTagDraft.trim()
                        if (label.isNotEmpty() && customTags.size < 10) {
                            onCreateCustomTag(label)
                            customTagDraft = ""
                        }
                    },
                    enabled = customTagDraft.isNotBlank() && customTags.size < 10,
                ) {
                    Text(if (customTags.size >= 10) "Limite atingido" else "Adicionar tema")
                }
                customTags.forEach { customTag ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = customTag.label,
                            color = PinhoInk,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onDeleteCustomTag(customTag.id) }) {
                            Text("Remover")
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableProfile(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
