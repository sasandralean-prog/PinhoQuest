package com.pinhoquest.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.pinhoquest.R
import com.pinhoquest.domain.tag.Tag
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.ui.reference.ReferenceHotspot
import com.pinhoquest.ui.reference.ReferenceRect

@Composable
fun ProfileScreen(
    tags: List<Tag>,
    onTagToggled: (TagId, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_profile),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        ReferenceHotspot(
            rect = ReferenceRect(0.84f, 0.015f, 0.13f, 0.10f),
            contentDescription = "Abrir configurações",
            onClick = onOpenSettings,
        )

        val rects = listOf(
            ReferenceRect(0.08f, 0.23f, 0.27f, 0.09f),
            ReferenceRect(0.36f, 0.23f, 0.27f, 0.09f),
            ReferenceRect(0.65f, 0.23f, 0.27f, 0.09f),
            ReferenceRect(0.08f, 0.33f, 0.27f, 0.09f),
            ReferenceRect(0.36f, 0.33f, 0.27f, 0.09f),
            ReferenceRect(0.65f, 0.33f, 0.27f, 0.09f),
            ReferenceRect(0.08f, 0.43f, 0.27f, 0.09f),
            ReferenceRect(0.36f, 0.43f, 0.27f, 0.09f),
            ReferenceRect(0.65f, 0.43f, 0.27f, 0.09f),
            ReferenceRect(0.08f, 0.53f, 0.27f, 0.09f),
            ReferenceRect(0.36f, 0.53f, 0.27f, 0.09f),
            ReferenceRect(0.65f, 0.53f, 0.27f, 0.09f),
        )

        tags.take(rects.size).forEachIndexed { index, tag ->
            ReferenceHotspot(
                rect = rects[index],
                contentDescription = tag.label,
                onClick = { onTagToggled(tag.id, !tag.enabled) },
            )
        }
    }
}
