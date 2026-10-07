package com.pinhoquest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinhoquest.R
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoGraphicButton
import com.pinhoquest.ui.PinhoParchment
import com.pinhoquest.ui.PinhoQuestBackground
import com.pinhoquest.ui.PinhoQuestBackgrounds
import com.pinhoquest.ui.PinhoBackButton
import com.pinhoquest.ui.PinhoInk

@Composable
fun SettingsScreen(
    ownerName: String,
    theme: ThemePreference,
    fontScale: Float,
    onThemeSelected: (ThemePreference) -> Unit,
    onFontScaleSelected: (Float) -> Unit,
    onInstallCreativeBrain: (() -> Unit)? = null,
    onBackup: (() -> Unit)? = null,
    onDonate: (() -> Unit)? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = theme == ThemePreference.DARK
    Column(
        modifier = modifier
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
            Spacer(Modifier.weight(1f))
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.pq_topbar_config),
                contentDescription = "Configurações",
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(72.dp),
            )
        }

        Spacer(Modifier.height(14.dp))

        PinhoQuestBackground(
            resource = PinhoQuestBackgrounds.ROOM,
            modifier = Modifier
                .fillMaxWidth()
                .height(0.dp),
        )

        PinhoParchment(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Jardim de $ownerName",
                color = PinhoInk,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(16.dp))

            Text("Tema", color = PinhoForest, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_day,
                    contentDescription = "Dia",
                    onClick = { onThemeSelected(ThemePreference.LIGHT) },
                    selected = !isDark,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 1.55f,
                )
                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_night,
                    contentDescription = "Noite",
                    onClick = { onThemeSelected(ThemePreference.DARK) },
                    selected = isDark,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 1.55f,
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("Tamanho da fonte", color = PinhoForest, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_menor,
                    contentDescription = "Menor",
                    onClick = { onFontScaleSelected(0.9f) },
                    selected = fontScale == 0.9f,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 2.35f,
                )
                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_medio,
                    contentDescription = "Médio",
                    onClick = { onFontScaleSelected(1.0f) },
                    selected = fontScale == 1.0f,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 2.35f,
                )
                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_maior,
                    contentDescription = "Maior",
                    onClick = { onFontScaleSelected(1.15f) },
                    selected = fontScale == 1.15f,
                    modifier = Modifier.weight(1f),
                    aspectRatio = 2.35f,
                )
            }

            Spacer(Modifier.height(20.dp))
            onBackup?.let {
                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_backup,
                    contentDescription = "Guardar uma cópia do seu jardim",
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    aspectRatio = 4.45f,
                )
            }

            Spacer(Modifier.height(12.dp))
            onDonate?.let {
                PinhoGraphicButton(
                    resource = R.drawable.pq_btn_donate,
                    contentDescription = "Apoie o criador Pinho Abacaxi",
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(0.86f),
                    aspectRatio = 3.7f,
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
                    resource = R.drawable.pq_btn_confirm,
                    contentDescription = "Baixar cérebro criativo",
                    onClick = install,
                    modifier = Modifier.fillMaxWidth(0.70f),
                    aspectRatio = 3.25f,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Suas escolhas ficam salvas neste aparelho.",
                color = PinhoInk.copy(alpha = 0.78f),
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}
