package com.pinhoquest.ui.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsScreen(
    tags: List<Tag>,
    onTagToggled: (TagId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("🏷 Coisas que você gosta", style = MaterialTheme.typography.headlineSmall)
        Text("Isso ajuda a escolher quests que combinam mais com você.")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = tag.enabled,
                    onClick = { onTagToggled(tag.id, !tag.enabled) },
                    label = { Text(tag.label) },
                )
            }
        }
        Text(
            "Mais tarde você também poderá me contar coisas do seu jeito por aqui.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
