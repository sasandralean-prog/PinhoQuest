package com.pinhoquest.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pinhoquest.R

val PinhoForest = Color(0xFF215B3A)
val PinhoLeaf = Color(0xFF3F7A45)
val PinhoCream = Color(0xFFFFF4DD)
val PinhoCreamSoft = Color(0xFFF7E9C9)
val PinhoWood = Color(0xFF8A5A32)
val PinhoInk = Color(0xFF24452F)

@Composable
fun PinhoQuestBackground(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    alpha: Float = 0.16f,
    overlay: Color = PinhoCream,
) {
    Box(modifier = modifier.fillMaxSize().background(overlay)) {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = alpha,
        )
    }
}

@Composable
fun PinhoQuestSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.background(PinhoCream.copy(alpha = 0.91f)),
    ) {
        content()
    }
}

val PinhoShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

object PinhoQuestBackgrounds {
    val HOME = R.drawable.bg_home
    val GARDEN = R.drawable.bg_garden
    val COLLECTION = R.drawable.bg_collection
    val PROFILE = R.drawable.bg_profile
    val SETTINGS = R.drawable.bg_settings
    val TAGS = R.drawable.bg_onboarding_tags
    val EMPTY_GARDEN = R.drawable.bg_empty_garden
    val START = R.drawable.bg_start
    val ONBOARDING_NAME = R.drawable.bg_onboarding_name
}
