package com.pinhoquest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.ui.PinhoCream
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.model.CreativeBrainCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    ownerName: String,
    theme: ThemePreference,
    fontScale: Float,
    onThemeSelected: (ThemePreference) -> Unit,
    onFontScaleSelected: (Float) -> Unit,
    onInstallCreativeBrain: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("⚙ Configurações", style = MaterialTheme.typography.headlineSmall)
        Text("Jardim de " + ownerName)

        Text("Tema", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreference.entries.forEach { option ->
                FilterChip(
                    selected = theme == option,
                    onClick = { onThemeSelected(option) },
                    label = {
                        Text(
                            when (option) {
                                ThemePreference.SYSTEM -> "Sistema"
                                ThemePreference.LIGHT -> "Claro"
                                ThemePreference.DARK -> "Escuro"
                            },
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PinhoCream.copy(alpha = 0.94f),
                        labelColor = PinhoInk,
                        selectedContainerColor = PinhoForest,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                    ),
                )
            }
        }

        Text("Aparência", style = MaterialTheme.typography.titleMedium)
        Text(
            "Escolha o tema e um tamanho de texto confortável para você.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Tamanho do texto", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.9f to "Menor", 1.0f to "Médio", 1.15f to "Maior").forEach { pair ->
                FilterChip(
                    selected = fontScale == pair.first,
                    onClick = { onFontScaleSelected(pair.first) },
                    label = { Text(pair.second) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PinhoCream.copy(alpha = 0.94f),
                        labelColor = PinhoInk,
                        selectedContainerColor = PinhoForest,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                    ),
                )
            }
        }

        onInstallCreativeBrain?.let { install ->
            CreativeBrainCard(onInstall = install)
        }

        Text(
            "Suas escolhas ficam salvas neste aparelho.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
