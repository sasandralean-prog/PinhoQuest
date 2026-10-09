package com.pinhoquest.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.pinhoquest.domain.tag.TagId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.R
import com.pinhoquest.ui.navigation.MainTab

val PinhoForest = Color(0xFF215B3A)
val PinhoLeaf = Color(0xFF3F7A45)
val PinhoCream = Color(0xFFFFF4DD)
val PinhoCreamSoft = Color(0xFFF7E9C9)
val PinhoWood = Color(0xFF8A5A32)
val PinhoInk = Color(0xFF24452F)
val PinhoGold = Color(0xFFFFD86B)
val PinhoNight = Color(0xFF102D55)

@Composable
fun PinhoQuestBackground(
    @DrawableRes resource: Int,
    modifier: Modifier = Modifier,
    overlay: Color = Color.Transparent,
    overlayAlpha: Float = 0f,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier = modifier.fillMaxSize().background(overlay)) {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlay.copy(alpha = overlayAlpha)),
            )
        }
    }
}

@Composable
fun PinhoGraphicButton(
    @DrawableRes resource: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showSelectionState: Boolean = false,
    enabled: Boolean = true,
    aspectRatio: Float = 2.7f,
    label: String? = null,
    labelColor: Color = Color.White,
    labelFontSize: TextUnit = 16.sp,
    labelAlignment: Alignment = Alignment.Center,
    labelStartFraction: Float = 0f,
) {
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .pinhoSelectedGlow(selected)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                if (showSelectionState) {
                    stateDescription = if (selected) "Selecionado" else "Não selecionado"
                }
            }
            .clickable(
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            alpha = if (enabled) 1f else 0.55f,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        label?.let { visibleLabel ->
            val labelModifier = if (labelStartFraction > 0f) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth((1f - labelStartFraction).coerceIn(0.1f, 1f))
                    .padding(end = 8.dp, top = 3.dp, bottom = 3.dp)
            } else {
                Modifier
                    .align(labelAlignment)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            }
            Text(
                text = visibleLabel,
                color = labelColor,
                fontSize = labelFontSize,
                textAlign = TextAlign.Center,
                lineHeight = labelFontSize * 1.05f,
                modifier = labelModifier,
            )
        }
    }
}

@Composable
fun PinhoBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Voltar",
) {
    Box(
        modifier = modifier
            .size(58.dp)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.btn_back),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun PinhoParchment(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .background(PinhoCream.copy(alpha = 0.96f), RoundedCornerShape(24.dp))
            .border(2.dp, Color(0xFFB99162), RoundedCornerShape(24.dp))
            .padding(14.dp),
        content = { content() },
    )
}

@Composable
fun PinhoBottomNavigation(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    val backgroundResource = if (dark) R.drawable.nav_bar_night else R.drawable.nav_bar_day
    val aspectRatio = if (dark) 798f / 167f else 768f / 181f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .aspectRatio(aspectRatio),
    ) {
        Image(
            painter = painterResource(backgroundResource),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PinhoNavItem(
                label = "Início",
                icon = "⌂",
                selected = selectedTab == MainTab.QUESTS,
                onClick = { onTabSelected(MainTab.QUESTS) },
                modifier = Modifier.weight(1f),
                dark = dark,
            )
            PinhoNavItem(
                label = "Jardim",
                icon = "🌷",
                selected = selectedTab == MainTab.GARDEN,
                onClick = { onTabSelected(MainTab.GARDEN) },
                modifier = Modifier.weight(1f),
                dark = dark,
            )
            PinhoNavItem(
                label = "Perfil",
                icon = "▣",
                selected = selectedTab == MainTab.PROFILE,
                onClick = { onTabSelected(MainTab.PROFILE) },
                modifier = Modifier.weight(1f),
                dark = dark,
            )
        }
    }
}

@Composable
private fun PinhoNavItem(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    Column(
        modifier = modifier
            .pinhoSelectedGlow(selected, cornerRadius = 22.dp)
            .semantics { role = Role.Button }
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = icon,
            fontSize = 25.sp,
            color = if (selected) PinhoGold else if (dark) PinhoCream else PinhoInk,
        )
        Text(
            text = label,
            color = if (dark) PinhoCream else PinhoInk,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(1.dp))
    }
}

fun Modifier.pinhoSelectedGlow(
    selected: Boolean,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
): Modifier {
    if (!selected) return this
    return drawBehind {
        val radius = cornerRadius.toPx()
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    PinhoGold.copy(alpha = 0.24f),
                    PinhoGold.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
    }
}

val PinhoShapes = androidx.compose.material3.Shapes(
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

object PinhoQuestBackgrounds {
    val HOME_NIGHT = R.drawable.canonical_bg_start_night
    val HOME_DAY = R.drawable.canonical_bg_start_day
    val HOME = HOME_DAY
    val GARDEN = R.drawable.canonical_bg_gardem_art_day
    val EMPTY_GARDEN = R.drawable.canonical_bg_gardem_empty_day
    val PROFILE = R.drawable.canonical_bg_profile_day
    val SETTINGS = R.drawable.canonical_bg_config_day
    val ROOM = R.drawable.canonical_bg_config_day
    val START = R.drawable.canonical_bg_start_day
    val ONBOARDING_NAME_REFERENCE = R.drawable.canonical_bg_start_day
    val ONBOARDING_TAGS_REFERENCE = R.drawable.canonical_bg_start_day

    val SETTINGS_DAY = R.drawable.canonical_bg_config_day
    val SETTINGS_NIGHT = R.drawable.canonical_bg_config_night
    val GARDEN_ART_DAY = R.drawable.canonical_bg_gardem_art_day
    val GARDEN_ART_NIGHT = R.drawable.canonical_bg_gardem_art_night
    val GARDEN_EMPTY_DAY = R.drawable.canonical_bg_gardem_empty_day
    val GARDEN_EMPTY_NIGHT = R.drawable.canonical_bg_gardem_empty_night
    val PROFILE_DAY = R.drawable.canonical_bg_profile_day
    val PROFILE_NIGHT = R.drawable.canonical_bg_profile_night
    val START_DAY = R.drawable.canonical_bg_start_day
    val START_NIGHT = R.drawable.canonical_bg_start_night

    // Compatibility aliases while remaining screen consumers are migrated.
    val CANONICAL_SETTINGS_DAY = SETTINGS_DAY
    val CANONICAL_GARDEN_EMPTY_ALT = GARDEN_EMPTY_DAY
    val CANONICAL_GARDEN_ART_DAY = GARDEN_ART_DAY
    val CANONICAL_GARDEN_ART_NIGHT = GARDEN_ART_NIGHT
    val CANONICAL_GARDEN_EMPTY_DAY = GARDEN_EMPTY_DAY
    val CANONICAL_PROFILE_DAY = PROFILE_DAY
    val CANONICAL_PROFILE_NIGHT = PROFILE_NIGHT
    val CANONICAL_START = START_DAY
    val CANONICAL_START_DAY = START_DAY

    val HOME_CARD_DAY = R.drawable.card_home_day
    val HOME_CARD_NIGHT = R.drawable.card_home_night
    val PROFILE_CARD_DAY = R.drawable.card_profile_day
    val PROFILE_CARD_NIGHT = R.drawable.card_profile_night
    val PROFILE_TAGS_CARD = R.drawable.card_profile_tags
    val EMPTY_GARDEN_CARD_DAY = R.drawable.card_garden_empty_day
    val EMPTY_GARDEN_CARD_NIGHT = R.drawable.card_garden_empty_night
    val UNKNOWN_FLOWER_CARD_DAY = R.drawable.card_flower_unknown_day
    val UNKNOWN_FLOWER_CARD_NIGHT = R.drawable.card_flower_unknown_night
}

@DrawableRes
private fun tagGraphicAsset(tagId: TagId): Int = when (tagId.value) {
    "music" -> R.drawable.category_music
    "photography" -> R.drawable.category_photography
    "nature" -> R.drawable.category_nature
    "animals" -> R.drawable.category_animal
    "learning" -> R.drawable.category_learn
    // These legacy semantic tags have no exact matching category art in the new pack.
    // A neutral skin keeps the controls usable without falsely assigning another category icon.
    "technology", "relax", "create", "fantasy" -> R.drawable.btn_font_size
    else -> R.drawable.btn_font_size
}

val PinhoVisualTagIds: List<TagId> = listOf(
    TagId("music"),
    TagId("photography"),
    TagId("nature"),
    TagId("technology"),
    TagId("animals"),
    TagId("learning"),
    TagId("relax"),
    TagId("create"),
    TagId("fantasy"),
)

@Composable
fun PinhoTagGraphicButton(
    tagId: TagId,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val asset = requireNotNull(tagGraphicAsset(tagId)) {
        "No canonical visual asset registered for system tag: " + tagId.value
    }

    Box(
        modifier = modifier
            .height(78.dp)
            .pinhoSelectedGlow(selected, cornerRadius = 18.dp)
            .semantics {
                role = Role.Button
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
            }
            .clickable(onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(asset),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = contentDescription,
            color = if (selected) PinhoForest else PinhoInk,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 4.dp),
        )
    }
}
