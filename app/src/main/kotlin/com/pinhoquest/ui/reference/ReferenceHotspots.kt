package com.pinhoquest.ui.reference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

data class ReferenceRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

@Composable
fun ReferenceHotspot(
    rect: ReferenceRect,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(
                    x = maxWidth * rect.left,
                    y = maxHeight * rect.top,
                )
                .size(
                    width = maxWidth * rect.width,
                    height = maxHeight * rect.height,
                )
                .semantics { this.contentDescription = contentDescription }
                .clickable(onClick = onClick),
        )
    }
}
