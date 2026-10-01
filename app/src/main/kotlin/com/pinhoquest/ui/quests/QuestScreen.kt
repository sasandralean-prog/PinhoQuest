package com.pinhoquest.ui.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestSession

@Composable
fun QuestScreen(
    ownerName: String,
    currentQuest: Quest?,
    activeSession: QuestSession?,
    loading: Boolean,
    onGenerateQuest: (QuestMode) -> Unit,
    onStartQuest: () -> Unit,
    onAbandonQuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Oi, " + ownerName + " 🌱", style = MaterialTheme.typography.titleMedium)
        Text("O que vamos inventar hoje?", style = MaterialTheme.typography.headlineSmall)

        if (activeSession != null && currentQuest != null) {
            ActiveQuestCard(
                quest = currentQuest,
                onAbandonQuest = onAbandonQuest,
            )
        } else if (currentQuest != null) {
            GeneratedQuestCard(
                quest = currentQuest,
                onStartQuest = onStartQuest,
                onAnotherQuest = { onGenerateQuest(QuestMode.NORMAL) },
            )
        } else {
            Button(
                onClick = { onGenerateQuest(QuestMode.NORMAL) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "Pensando em alguma coisa legal…" else "SORTEAR QUEST")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onGenerateQuest(QuestMode.GAME) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Quest de Jogo")
                }
                OutlinedButton(
                    onClick = { onGenerateQuest(QuestMode.RANDOM) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Quest Aleatória")
                }
            }
            Text(
                "Uma ação, uma ideia. Sem feed infinito por aqui.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GeneratedQuestCard(
    quest: Quest,
    onStartQuest: () -> Unit,
    onAnotherQuest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(quest.title, style = MaterialTheme.typography.titleLarge)
            Text(quest.description)
            Text("🎯 Objetivo", style = MaterialTheme.typography.titleMedium)
            quest.objectives.forEach { objective ->
                Text("• " + objective.text)
            }
            Text(
                "⏱ " + quest.estimatedDuration.minMinutes + "–" +
                    quest.estimatedDuration.maxMinutes + " min · " +
                    quest.difficulty.name.lowercase(),
            )
            Button(
                onClick = onStartQuest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("COMEÇAR QUEST")
            }
            OutlinedButton(
                onClick = onAnotherQuest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Outra")
            }
        }
    }
}

@Composable
private fun ActiveQuestCard(
    quest: Quest,
    onAbandonQuest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(quest.title, style = MaterialTheme.typography.titleLarge)
            Text("Em andamento 🌱")
            quest.objectives.forEach { objective ->
                Text("○ " + objective.text)
            }
            Text(
                "⏱ Estimativa: " + quest.estimatedDuration.minMinutes + "–" +
                    quest.estimatedDuration.maxMinutes + " min",
            )
            OutlinedButton(
                onClick = onAbandonQuest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Preciso parar")
            }
        }
    }
}
