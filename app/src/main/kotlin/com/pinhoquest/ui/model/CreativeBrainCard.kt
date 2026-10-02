package com.pinhoquest.ui.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreativeBrainCard(
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🧠 Um cérebro criativo", style = MaterialTheme.typography.titleMedium)
            Text("Se quiser, posso baixar um pequeno cérebro para inventar quests mais variadas. Ele fica no seu celular e não precisa da internet para criar quests depois.")
            Button(onClick = onInstall) {
                Text("Baixar cérebro criativo")
            }
        }
    }
}