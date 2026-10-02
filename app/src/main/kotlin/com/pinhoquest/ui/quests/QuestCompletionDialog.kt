package com.pinhoquest.ui.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

data class QuestCompletionUi(
    val questTitle: String,
    val xpAward: Int,
    val flowerId: String?,
    val flowerName: String?,
    val flowerRarityLabel: String?,
)

@Composable
fun QuestCompletionDialog(
    completion: QuestCompletionUi,
    onDismiss: () -> Unit,
    onOpenGarden: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🌱 Quest concluída!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(completion.questTitle)
                Text("+" + completion.xpAward + " XP")
                if (completion.flowerName != null) {
                    Text("🌷 Você encontrou uma nova flor!")
                    Text(completion.flowerName)
                    completion.flowerRarityLabel?.let { Text("Raridade: " + it) }
                } else {
                    Text("Seu jardim continua crescendo.")
                }
            }
        },
        confirmButton = {
            if (completion.flowerName != null) {
                Button(onClick = onOpenGarden) { Text("Ver no jardim") }
            } else {
                Button(onClick = onDismiss) { Text("Fechar") }
            }
        },
    )
}
