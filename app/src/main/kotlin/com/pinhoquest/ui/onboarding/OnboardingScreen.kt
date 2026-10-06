package com.pinhoquest.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.ui.PinhoCream
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.model.CreativeBrainCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (String, Set<String>) -> Unit,
    onInstallCreativeBrain: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    val trimmed = name.trim()
    val validName = GardenOwnerName.create(trimmed).isSuccess
    val tooLong = trimmed.codePointCount(0, trimmed.length) > GardenOwnerName.MAX_CHARACTERS

    Box(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            resource = PinhoQuestBackgrounds.ONBOARDING_NAME,
            alpha = 0.24f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "🌱 Oi! Eu sou o Pinho Quest.",
                style = MaterialTheme.typography.headlineSmall,
                color = PinhoForest,
            )
            Text(
                "Pequenas descobertas, grandes jardins.",
                style = MaterialTheme.typography.titleMedium,
                color = PinhoInk,
            )
            Text(
                "Como você quer chamar seu jardim?",
                style = MaterialTheme.typography.headlineSmall,
                color = PinhoForest,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Seu nome") },
                supportingText = {
                    Text(
                        if (tooLong) "Use até 20 caracteres."
                        else "Vai aparecer como Jardim de " + trimmed.ifBlank { "..." } + " 🌷",
                    )
                },
                isError = tooLong,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
            )

            Text(
                "O que você gosta?",
                style = MaterialTheme.typography.headlineSmall,
                color = PinhoForest,
            )
            Text(
                "Escolha algumas coisas que podem inspirar suas quests.",
                color = PinhoInk,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SystemTagCatalog.all.forEach { tag ->
                    val isSelected = tag.id.value in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selected = if (isSelected) selected - tag.id.value
                            else selected + tag.id.value
                        },
                        label = { Text(tag.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = PinhoCream.copy(alpha = 0.94f),
                            labelColor = PinhoInk,
                            selectedContainerColor = PinhoForest,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        ),
                    )
                }
            }

            Text(
                "Você pode mudar isso depois. Nada aqui vira regra eterna. 💚",
                style = MaterialTheme.typography.bodySmall,
                color = PinhoInk,
            )

            onInstallCreativeBrain?.let { install ->
                CreativeBrainCard(onInstall = install)
            }

            Button(
                onClick = { onComplete(trimmed, selected) },
                enabled = validName,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinhoForest,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text("Criar meu jardim")
            }
        }
    }
}
