package com.pinhoquest.ui.quests

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.pinhoquest.R
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestSession
import com.pinhoquest.ui.PinhoCream
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoParchment
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoGraphicButton

@Composable
fun QuestScreen(
    ownerName: String,
    currentQuest: Quest?,
    activeSession: QuestSession?,
    loading: Boolean,
    onGenerateQuest: (QuestMode) -> Unit,
    onOpenThemeSelection: () -> Unit,
    onStartQuest: () -> Unit,
    onCompleteQuest: () -> Unit,
    onAbandonQuest: () -> Unit,
    flowerCount: Int = 0,
    lifetimeXp: Int = 0,
    dark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            resource = if (dark) PinhoQuestBackgrounds.HOME_NIGHT else PinhoQuestBackgrounds.HOME_DAY,
            overlayAlpha = 0f,
        )

        if (activeSession != null && currentQuest != null) {
            ActiveQuestContent(
                quest = currentQuest,
                onCompleteQuest = onCompleteQuest,
                onAbandonQuest = onAbandonQuest,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (currentQuest != null) {
            GeneratedQuestContent(
                quest = currentQuest,
                onStartQuest = onStartQuest,
                onAnotherQuest = { onGenerateQuest(QuestMode.NORMAL) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The approved start background contains the PinhoQuest wordmark and slogan.
                // Leave that upper composition unobstructed and start live content below it.
                Spacer(Modifier.height(maxHeight * 0.24f))
                GardenSummaryCard(
                    ownerName = ownerName,
                    flowerCount = flowerCount,
                    lifetimeXp = lifetimeXp,
                    dark = dark,
                )
                Spacer(Modifier.height(18.dp))

                PinhoGraphicButton(
                    resource = R.drawable.btn_quest_draw,
                    contentDescription = "Sortear quest",
                    onClick = onOpenThemeSelection,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(0.92f),
                    aspectRatio = 349f / 95f,
                    label = "Sortear quest",
                    labelColor = Color(0xFFFFF4DD),
                    labelFontSize = 20.sp,
                    labelStartFraction = 0.28f,
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PinhoGraphicButton(
                        resource = R.drawable.btn_quest_random,
                        contentDescription = "Quest aleatória",
                        onClick = { onGenerateQuest(QuestMode.RANDOM) },
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                        aspectRatio = 165f / 90f,
                        label = "Quest aleatória",
                        labelColor = Color(0xFF4A2114),
                        labelFontSize = 15.sp,
                        labelStartFraction = 0.02f,
                        labelEndFraction = 0.25f,
                    )
                    PinhoGraphicButton(
                        resource = R.drawable.btn_quest_game,
                        contentDescription = "Quest de jogo",
                        onClick = { onGenerateQuest(QuestMode.GAME) },
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                        aspectRatio = 174f / 86f,
                        label = "Quest de Jogo",
                        labelColor = Color(0xFF4A2114),
                        labelFontSize = 15.sp,
                        labelStartFraction = 0.31f,
                    )
                }

                if (loading) {
                    Spacer(Modifier.height(18.dp))
                    CircularProgressIndicator(
                        color = PinhoCream,
                        modifier = Modifier
                            .semantics { contentDescription = "Preparando sua quest" }
                            .width(28.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        "Preparando uma ideia para você…",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun GardenSummaryCard(
    ownerName: String,
    flowerCount: Int,
    lifetimeXp: Int,
    dark: Boolean,
) {
    val cardResource = if (dark) R.drawable.card_home_night else R.drawable.card_home_day
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth(0.90f)
            .aspectRatio(612f / 292f),
    ) {
        Image(
            painter = painterResource(cardResource),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        // These two figures sit beside the flower/star already drawn into the card.
        // Keep their text inside the cream left area and do not redraw the decorative icons.
        Text(
            text = "$flowerCount flores",
            color = PinhoInk,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = maxWidth * 0.145f, y = maxHeight * 0.16f)
                .fillMaxWidth(0.23f),
        )
        Text(
            text = "$lifetimeXp XP",
            color = PinhoInk,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = maxWidth * 0.405f, y = maxHeight * 0.16f)
                .fillMaxWidth(0.145f),
        )
        Text(
            text = "Jardim de $ownerName",
            color = PinhoInk,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = maxWidth * 0.035f, y = maxHeight * 0.39f)
                .fillMaxWidth(0.48f),
        )
        Text(
            text = if (dark) {
                "Pequenas descobertas sob as estrelas."
            } else {
                "Pequenas descobertas, um jardim crescendo."
            },
            color = PinhoInk,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = maxWidth * 0.035f, y = maxHeight * 0.66f)
                .fillMaxWidth(0.48f),
        )
    }
}

@Composable
private fun GeneratedQuestContent(
    quest: Quest,
    onStartQuest: () -> Unit,
    onAnotherQuest: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinhoParchment(modifier = Modifier.fillMaxWidth()) {
            Text(quest.title, style = MaterialTheme.typography.headlineSmall, color = PinhoInk)
            Spacer(Modifier.height(8.dp))
            Text(quest.description, color = PinhoInk)
            Spacer(Modifier.height(14.dp))
            Text("🎯 Objetivos", style = MaterialTheme.typography.titleMedium, color = PinhoForest)
            quest.objectives.forEach { objective ->
                Text("• ${objective.text}", color = PinhoInk, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "⏱ ${quest.estimatedDuration.minMinutes}–${quest.estimatedDuration.maxMinutes} min · ${quest.difficulty.name.lowercase()}",
                color = PinhoInk,
            )
        }
        Spacer(Modifier.height(18.dp))
        PinhoGraphicButton(
            resource = R.drawable.btn_confirm,
            contentDescription = "Começar quest",
            onClick = onStartQuest,
            modifier = Modifier.fillMaxWidth(0.68f),
            aspectRatio = 187f / 86f,
            label = "Começar quest",
            labelColor = Color(0xFF4A2114),
            labelFontSize = 17.sp,
            labelStartFraction = 0.28f,
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.TextButton(onClick = onAnotherQuest) {
            Text("Outra ideia", color = Color.White)
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun ActiveQuestContent(
    quest: Quest,
    onCompleteQuest: () -> Unit,
    onAbandonQuest: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinhoParchment(modifier = Modifier.fillMaxWidth()) {
            Text("Em andamento 🌱", color = PinhoForest, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(quest.title, color = PinhoInk, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            quest.objectives.forEach { objective ->
                Text("○ ${objective.text}", color = PinhoInk, modifier = Modifier.padding(top = 5.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "⏱ Estimativa: ${quest.estimatedDuration.minMinutes}–${quest.estimatedDuration.maxMinutes} min",
                color = PinhoInk,
            )
        }
        Spacer(Modifier.height(18.dp))
        PinhoGraphicButton(
            resource = R.drawable.btn_confirm,
            contentDescription = "Concluir quest",
            onClick = onCompleteQuest,
            modifier = Modifier.fillMaxWidth(0.68f),
            aspectRatio = 187f / 86f,
            label = "Concluir quest",
            labelColor = Color(0xFF4A2114),
            labelFontSize = 17.sp,
            labelStartFraction = 0.28f,
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.TextButton(onClick = onAbandonQuest) {
            Text("Preciso parar", color = Color.White)
        }
        Spacer(Modifier.height(90.dp))
    }
}
