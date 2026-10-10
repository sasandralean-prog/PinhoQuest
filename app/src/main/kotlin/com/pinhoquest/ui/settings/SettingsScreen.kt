package com.pinhoquest.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.abs
import com.pinhoquest.R
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoGraphicButton
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoBackButton
import com.pinhoquest.ui.PinhoInk

@Composable
fun SettingsScreen(
    ownerName: String,
    theme: ThemePreference,
    fontScale: Float,
    effectiveDark: Boolean = theme == ThemePreference.DARK,
    onThemeSelected: (ThemePreference) -> Unit,
    onFontScaleSelected: (Float) -> Unit,
    onInstallCreativeBrain: (() -> Unit)? = null,
    onBackup: (() -> Unit)? = null,
    onDonate: (() -> Unit)? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = effectiveDark
    Box(modifier = modifier.fillMaxSize()) {
        PinhoQuestBackground(
            resource = if (isDark) PinhoQuestBackgrounds.SETTINGS_NIGHT else PinhoQuestBackgrounds.SETTINGS_DAY,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PinhoBackButton(onClick = onBack)
                Spacer(Modifier.width(8.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.name_bar),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    val artworkWidth = minOf(maxWidth, maxHeight * (388f / 124f))
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (maxWidth - artworkWidth) * 0.5f + artworkWidth * 0.15f)
                            .width(artworkWidth * 0.70f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "⚙",
                            color = Color(0xFF4A2114),
                            fontSize = 18.sp,
                            maxLines = 1,
                        )
                        Text(
                            text = "Configurações",
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF4A2114),
                            fontSize = 18.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Tema", color = PinhoForest, fontSize = 23.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PinhoGraphicButton(
                    resource = R.drawable.btn_theme_day,
                    contentDescription = "Selecionar tema dia",
                    onClick = { onThemeSelected(ThemePreference.LIGHT) },
                    selected = theme == ThemePreference.LIGHT,
                    showSelectionState = true,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 300f / 180f,
                    selectionCornerRadius = 18.dp,
                    label = "Dia",
                    labelColor = PinhoInk,
                    labelFontSize = 18.sp,
                    labelAlignment = Alignment.BottomCenter,
                    labelBottomFraction = 0.10f,
                )
                PinhoGraphicButton(
                    resource = R.drawable.btn_theme_night,
                    contentDescription = "Selecionar tema noite",
                    onClick = { onThemeSelected(ThemePreference.DARK) },
                    selected = isDark,
                    showSelectionState = true,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 300f / 180f,
                    selectionCornerRadius = 18.dp,
                    label = "Noite",
                    labelColor = Color(0xFFFFF0BD),
                    labelFontSize = 18.sp,
                    labelAlignment = Alignment.BottomCenter,
                    labelBottomFraction = 0.10f,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Tamanho da fonte", color = PinhoForest, fontSize = 23.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                PinhoGraphicButton(
                    resource = R.drawable.btn_font_size,
                    contentDescription = "Tamanho de fonte menor",
                    onClick = { onFontScaleSelected(0.9f) },
                    selected = abs(fontScale - 0.9f) < 0.001f,
                    showSelectionState = true,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 224f / 68f,
                    selectionCornerRadius = 12.dp,
                    label = "Menor",
                    labelColor = Color(0xFF4A2114),
                    labelFontSize = 16.sp,
                )
                PinhoGraphicButton(
                    resource = R.drawable.btn_font_size,
                    contentDescription = "Tamanho de fonte médio",
                    onClick = { onFontScaleSelected(1.0f) },
                    selected = abs(fontScale - 1.0f) < 0.001f,
                    showSelectionState = true,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 224f / 68f,
                    selectionCornerRadius = 12.dp,
                    label = "Médio",
                    labelColor = Color(0xFF4A2114),
                    labelFontSize = 16.sp,
                )
                PinhoGraphicButton(
                    resource = R.drawable.btn_font_size,
                    contentDescription = "Tamanho de fonte maior",
                    onClick = { onFontScaleSelected(1.15f) },
                    selected = abs(fontScale - 1.15f) < 0.001f,
                    showSelectionState = true,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 224f / 68f,
                    selectionCornerRadius = 12.dp,
                    label = "Maior",
                    labelColor = Color(0xFF4A2114),
                    labelFontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(22.dp))
            onBackup?.let {
                PinhoGraphicButton(
                    resource = R.drawable.btn_garden_backup,
                    contentDescription = "Guardar uma cópia do seu jardim",
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    aspectRatio = 682f / 160f,
                    label = "Guardar uma cópia do seu jardim",
                    labelColor = Color(0xFF4A2114),
                    labelFontSize = 18.sp,
                    labelStartFraction = 0.20f,
                )
            }

            Spacer(Modifier.height(12.dp))
            onDonate?.let {
                PinhoGraphicButton(
                    resource = R.drawable.btn_support_creator,
                    contentDescription = "Apoie o criador Pinho Abacaxi",
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(0.86f),
                    aspectRatio = 428f / 128f,
                    label = "Apoie o criador\nPinho Abacaxi",
                    labelColor = Color(0xFF4A2114),
                    labelFontSize = 18.sp,
                    labelStartFraction = 0.22f,
                )
            }

            onInstallCreativeBrain?.let { install ->
                Spacer(Modifier.height(12.dp))
                Text(
                    "O cérebro criativo pode continuar aqui sem mudar o jeito que o jardim funciona.",
                    color = PinhoInk,
                    fontSize = 13.sp,
                )
                PinhoGraphicButton(
                    resource = R.drawable.btn_confirm,
                    contentDescription = "Baixar cérebro criativo",
                    onClick = install,
                    modifier = Modifier.fillMaxWidth(0.70f),
                    aspectRatio = 187f / 86f,
                    label = "Baixar cérebro criativo",
                    labelColor = Color(0xFF4A2114),
                    labelFontSize = 15.sp,
                    labelStartFraction = 0.28f,
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
