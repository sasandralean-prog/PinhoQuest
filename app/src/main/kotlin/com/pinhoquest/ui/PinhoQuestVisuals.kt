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
import com.pinhoquest.domain.tag.TagId
import com.pinhoquest.core.tag.SystemTagCatalog
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
    enabled: Boolean = true,
    aspectRatio: Float = 2.7f,
) {
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .pinhoSelectedGlow(selected)
            .semantics {
                role = Role.Button
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
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
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
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
            .background(PinhoWood, RoundedCornerShape(18.dp))
            .border(2.dp, Color(0xFF5D351F), RoundedCornerShape(18.dp))
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
            contentScale = ContentScale.FillBounds,
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
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PinhoCream.copy(alpha = 0.97f))
            .border(1.dp, Color(0xFFB99162))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PinhoNavItem(
            label = "Início",
            icon = "⌂",
            selected = selectedTab == MainTab.QUESTS,
            onClick = { onTabSelected(MainTab.QUESTS) },
            modifier = Modifier.weight(1f),
        )
        PinhoNavItem(
            label = "Jardim",
            icon = "🌷",
            selected = selectedTab == MainTab.GARDEN,
            onClick = { onTabSelected(MainTab.GARDEN) },
            modifier = Modifier.weight(1f),
        )
        PinhoNavItem(
            label = "Perfil",
            icon = "▣",
            selected = selectedTab == MainTab.PROFILE,
            onClick = { onTabSelected(MainTab.PROFILE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PinhoNavItem(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            color = if (selected) PinhoForest else PinhoInk,
        )
        Text(
            text = label,
            color = PinhoInk,
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
    val HOME_NIGHT = R.drawable.pq_bg_lake_night
    val HOME_DAY = R.drawable.pq_bg_lake_day
    val HOME = R.drawable.bg_home
    val GARDEN = R.drawable.bg_garden
    val EMPTY_GARDEN = R.drawable.bg_empty_garden
    val PROFILE = R.drawable.bg_profile
    val SETTINGS = R.drawable.bg_settings
    val ROOM = R.drawable.pq_bg_room
    val START = R.drawable.bg_start
    val ONBOARDING_NAME_REFERENCE = R.drawable.bg_onboarding_name
    val ONBOARDING_TAGS_REFERENCE = R.drawable.bg_onboarding_tags
}


@DrawableRes
private fun tagGraphicAsset(tagId: TagId): Int? = when (tagId.value) {
    "music" -> R.drawable.btn_tag_musica
    "photography" -> R.drawable.btn_tag_fotografia
    "nature" -> R.drawable.btn_tag_natureza
    "technology" -> R.drawable.btn_tag_tecnologia
    "animals" -> R.drawable.btn_tag_animais
    "adventures" -> R.drawable.btn_tag_aventuras
    "relax" -> R.drawable.btn_tag_relaxar
    "create" -> R.drawable.btn_tag_criar
    "fantasy" -> R.drawable.btn_tag_fantasia
    else -> null
}

val PinhoVisualTagIds: List<TagId> = listOf(
    TagId("music"),
    TagId("photography"),
    TagId("nature"),
    TagId("technology"),
    TagId("animals"),
    TagId("adventures"),
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
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
