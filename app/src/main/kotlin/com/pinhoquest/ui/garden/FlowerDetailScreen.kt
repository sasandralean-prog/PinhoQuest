package com.pinhoquest.ui.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinhoquest.domain.garden.FlowerDiscoveryState

@Composable
fun FlowerDetailScreen(
    flower: GardenFlowerUi,
    spendableXp: Int,
    onInvestigate: () -> Unit,
    onDismiss: () -> Unit = {},
) {
    val investigationCost = flower.investigationCost
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (flower.discoveryState) {
                    FlowerDiscoveryState.HIDDEN -> "❔ Ainda desconhecida"
                    else -> flower.commonName
                },
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                when (flower.discoveryState) {
                    FlowerDiscoveryState.HIDDEN -> {
                        Text("Espécie: ???")
                        Text("Raridade: ???")
                    }
                    FlowerDiscoveryState.HINTED -> {
                        Text("Espécie: ???")
                        Text("Raridade: " + flower.rarity.label)
                    }
                    FlowerDiscoveryState.REVEALED -> {
                        Text(flower.scientificName)
                        Text("Raridade: " + flower.rarity.label)
                        Text(flower.description)
                    }
                    FlowerDiscoveryState.COLLECTED -> {
                        Text(flower.scientificName)
                        Text("Raridade: " + flower.rarity.label)
                        flower.xpAward?.let { Text("XP da quest: +" + it) }
                        flower.questTitle?.let { Text("Conquistada com: " + it) }
                        Text(flower.description)
                    }
                }

                if (investigationCost != null) {
                    Text("Investigar — " + investigationCost + " XP")
                    if (spendableXp < investigationCost) {
                        Text(
                            "Faltam " + (investigationCost - spendableXp) +
                                " XP para investigar esta flor.",
                        )
                    } else {
                        Text("Seu progresso total não diminui.")
                    }
                }
            }
        },
        confirmButton = {
            if (investigationCost != null) {
                Button(
                    onClick = onInvestigate,
                    enabled = spendableXp >= investigationCost,
                ) {
                    Text("Investigar")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        },
        dismissButton = {
            if (investigationCost != null) {
                TextButton(onClick = onDismiss) { Text("Agora não") }
            }
        },
    )
}
