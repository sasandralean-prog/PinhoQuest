package com.pinhoquest.ui.onboarding

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.R
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoGraphicButton
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoTagGraphicButton
import com.pinhoquest.ui.PinhoVisualTagIds
import com.pinhoquest.ui.PinhoBackButton

@Composable
fun OnboardingScreen(
    onComplete: (String, Set<String>) -> Unit,
    onInstallCreativeBrain: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }

    Box(modifier = modifier.fillMaxSize()) {
        when (step) {
            0 -> {
                PinhoQuestBackground(PinhoQuestBackgrounds.START)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 34.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    PinhoGraphicButton(
                        resource = R.drawable.pq_btn_continue,
                        contentDescription = "Começar",
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(0.62f),
                        aspectRatio = 3.2f,
                    )
                    Spacer(Modifier.height(76.dp))
                }
            }

            1 -> {
                PinhoQuestBackground(
                    PinhoQuestBackgrounds.ONBOARDING_NAME_REFERENCE,
                    overlay = PinhoForest,
                    overlayAlpha = 0.04f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                ) {
                    PinhoBackButton(
                        onClick = { step = 0 },
                        modifier = Modifier.padding(top = 16.dp, start = 4.dp),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 104.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        androidx.compose.material3.Text(
                            text = "PinhoQuest",
                            color = PinhoForest,
                            fontSize = 42.sp,
                            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                        )
                        androidx.compose.material3.Text(
                            text = "Como você quer chamar seu jardim?",
                            color = PinhoForest,
                            fontSize = 24.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.Text(
                            text = "🌼",
                            fontSize = 30.sp,
                        )
                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(0.86f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.pq_label_name),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                BasicTextField(
                                    value = name,
                                onValueChange = { value ->
                                    if (value.trim().codePointCount(0, value.trim().length) <= GardenOwnerName.MAX_CHARACTERS) {
                                        name = value
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontSize = 22.sp,
                                ),
                                    modifier = Modifier
                                        .fillMaxWidth(0.72f)
                                        .height(56.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        androidx.compose.material3.Text(
                            text = "Vai aparecer como Jardim de ... 🌷",
                            color = PinhoInk,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(24.dp))
                        PinhoGraphicButton(
                            resource = R.drawable.pq_btn_confirm,
                            contentDescription = "Confirmar nome",
                            onClick = {
                                if (GardenOwnerName.create(name.trim()).isSuccess) step = 2
                            },
                            modifier = Modifier.fillMaxWidth(0.56f),
                            aspectRatio = 3.25f,
                        )
                    }
                }
            }

            else -> {
                PinhoQuestBackground(
                    PinhoQuestBackgrounds.ONBOARDING_TAGS_REFERENCE,
                    overlay = Color(0xFFFFE7B0),
                    overlayAlpha = 0.25f,
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        PinhoBackButton(onClick = { step = 1 })
                    }
                    Spacer(Modifier.height(26.dp))
                    androidx.compose.material3.Text(
                        "PinhoQuest",
                        color = PinhoForest,
                        fontSize = 38.sp,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                    )
                    androidx.compose.material3.Text(
                        "O que você gosta?",
                        color = PinhoForest,
                        fontSize = 25.sp,
                    )
                    androidx.compose.material3.Text(
                        "Escolha algumas coisas para me contar sobre você.",
                        color = PinhoForest,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(18.dp))

                    val tags = PinhoVisualTagIds.mapNotNull { id ->
                        SystemTagCatalog.byId(id.value)
                    }
                    tags.chunked(3).forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEachIndexed { colIndex, tag ->
                                PinhoTagGraphicButton(
                                    tagId = tag.id,
                                    contentDescription = tag.label,
                                    selected = tag.id.value in selected,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selected = if (tag.id.value in selected) {
                                            selected - tag.id.value
                                        } else {
                                            selected + tag.id.value
                                        }
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.weight(1f))
                    PinhoGraphicButton(
                        resource = R.drawable.pq_btn_continue,
                        contentDescription = "Continuar",
                        onClick = { onComplete(name.trim(), selected) },
                        modifier = Modifier.fillMaxWidth(0.62f),
                        aspectRatio = 3.2f,
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

