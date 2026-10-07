package com.pinhoquest.ui.quests

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onStartQuest: () -> Unit,
    onCompleteQuest: () -> Unit,
    onAbandonQuest: () -> Unit,
    flowerCount: Int = 0,
    lifetimeXp: Int = 0,
    dark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val background = if (dark) PinhoQuestBackgrounds.HOME_NIGHT else PinhoQuestBackgrounds.HOME_DAY
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            resource = PinhoQuestBackgrounds.HOME_NIGHT,
            overlay = if (dark) Color(0xFF061B36) else Color(0xFFFFE7B0),
            overlayAlpha = if (dark) 0.04f else 0.22f,
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
                    .padding(horizontal = 22.dp, vertical = 86.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GardenSummaryCard(
                    ownerName = ownerName,
                    flowerCount = flowerCount,
                    lifetimeXp = lifetimeXp,
                    dark = dark,
                )
                Spacer(Modifier.height(34.dp))

                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_sort_quest,
                    contentDescription = "Sortear quest",
                    onClick = { onGenerateQuest(QuestMode.NORMAL) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(0.92f),
                    selected = !loading,
                    aspectRatio = 3.05f,
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PinhoGraphicButton(
                        resource = R.drawable.pq_btn_quest_random,
                        contentDescription = "Quest aleatória",
                        onClick = { onGenerateQuest(QuestMode.RANDOM) },
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                        aspectRatio = 1.95f,
                    )
                    PinhoGraphicButton(
                        resource = R.drawable.pq_btn_quest_game,
                        contentDescription = "Quest de jogo",
                        onClick = { onGenerateQuest(QuestMode.GAME) },
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                        aspectRatio = 1.95f,
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
    PinhoParchment(
        modifier = Modifier.fillMaxWidth(0.9f),
    ) {
        Text(
            text = "🌷 \${flowerCount} flores    ⭐ \${lifetimeXp} XP",
            color = PinhoInk,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Jardim de \${ownerName}",
            color = PinhoInk,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = if (dark) "Pequenas descobertas sob as estrelas." else "Pequenas descobertas, um jardim crescendo.",
            color = PinhoInk,
            style = MaterialTheme.typography.bodyMedium,
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
                Text("• \${objective.text}", color = PinhoInk, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "⏱ \${quest.estimatedDuration.minMinutes}–\${quest.estimatedDuration.maxMinutes} min · \${quest.difficulty.name.lowercase()}",
                color = PinhoInk,
            )
        }
        Spacer(Modifier.height(18.dp))
        PinhoGraphicButton(
            resource = R.drawable.pq_btn_continue,
            contentDescription = "Começar quest",
            onClick = onStartQuest,
            modifier = Modifier.fillMaxWidth(0.68f),
            aspectRatio = 3.25f,
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
                Text("○ \${objective.text}", color = PinhoInk, modifier = Modifier.padding(top = 5.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "⏱ Estimativa: \${quest.estimatedDuration.minMinutes}–\${quest.estimatedDuration.maxMinutes} min",
                color = PinhoInk,
            )
        }
        Spacer(Modifier.height(18.dp))
        PinhoGraphicButton(
            resource = R.drawable.pq_btn_confirm,
            contentDescription = "Concluir quest",
            onClick = onCompleteQuest,
            modifier = Modifier.fillMaxWidth(0.68f),
            aspectRatio = 3.25f,
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.TextButton(onClick = onAbandonQuest) {
            Text("Preciso parar", color = Color.White)
        }
        Spacer(Modifier.height(90.dp))
    }
}
