package com.pinhoquest.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.core.tag.SystemTagCatalog
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
    val tooLong =
        trimmed.codePointCount(0, trimmed.length) > GardenOwnerName.MAX_CHARACTERS

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("🌱 Oi! Eu sou o Pinho Quest.", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Quando bater aquele “não sei o que fazer”, eu posso inventar uma pequena quest para você.",
        )
        Text("Como você quer chamar seu jardim?", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Seu nome") },
            supportingText = {
                if (tooLong) {
                    Text("Use até 20 caracteres.")
                } else {
                    Text("Vai aparecer como Jardim de " + trimmed.ifBlank { "..." } + " 🌷")
                }
            },
            isError = tooLong,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Do que você gosta?", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SystemTagCatalog.all.forEach { tag ->
                val isSelected = tag.id.value in selected
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selected = if (isSelected) {
                            selected - tag.id.value
                        } else {
                            selected + tag.id.value
                        }
                    },
                    label = { Text(tag.label) },
                )
            }
        }
        Text(
            "Você pode mudar isso depois. Nada aqui vira regra eterna.",
            style = MaterialTheme.typography.bodySmall,
        )
        onInstallCreativeBrain?.let { install ->
            CreativeBrainCard(onInstall = install)
        }
        Button(
            onClick = { onComplete(trimmed, selected) },
            enabled = validName,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Criar meu jardim")
        }
    }
}
