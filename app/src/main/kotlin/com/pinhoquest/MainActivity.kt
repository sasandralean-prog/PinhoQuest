package com.pinhoquest

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.ui.PinhoCream
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoShapes
import com.pinhoquest.ui.navigation.PinhoQuestNav
import com.pinhoquest.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as PinhoQuestApplication).graph

        setContent {
            val viewModel: PinhoQuestViewModel = viewModel(
                factory = PinhoQuestViewModel.Factory(graph),
            )
            val state by viewModel.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDark = when (state.theme) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            val platformDensity = LocalDensity.current
            val effectiveDensity = Density(
                density = platformDensity.density,
                fontScale = platformDensity.fontScale * state.fontScale,
            )

            var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
            var creativeBrainTermsVisible by remember { mutableStateOf(false) }
            val backupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
            ) { uri: Uri? ->
                val bytes = pendingBackup
                pendingBackup = null
                if (uri == null || bytes == null) {
                    viewModel.showMessage("A cópia não foi guardada.")
                    return@rememberLauncherForActivityResult
                }
                lifecycleScope.launch {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(bytes)
                        } ?: error("Não consegui abrir o destino da cópia.")
                    }.onSuccess {
                        viewModel.showMessage("Cópia do seu jardim guardada.")
                    }.onFailure {
                        viewModel.showMessage("Não consegui guardar a cópia desta vez.")
                    }
                }
            }

            CompositionLocalProvider(LocalDensity provides effectiveDensity) {
                MaterialTheme(
                    colorScheme = if (useDark) {
                        darkColorScheme(
                            primary = PinhoForest,
                            secondary = PinhoForest,
                            background = PinhoCream,
                            surface = PinhoCream,
                            onBackground = PinhoInk,
                            onSurface = PinhoInk,
                        )
                    } else {
                        lightColorScheme(
                            primary = PinhoForest,
                            secondary = PinhoForest,
                            background = PinhoCream,
                            surface = PinhoCream,
                            onBackground = PinhoInk,
                            onSurface = PinhoInk,
                        )
                    },
                    typography = Typography().copy(
                        headlineLarge = Typography().headlineLarge.copy(fontFamily = FontFamily.Serif),
                        headlineMedium = Typography().headlineMedium.copy(fontFamily = FontFamily.Serif),
                        headlineSmall = Typography().headlineSmall.copy(fontFamily = FontFamily.Serif),
                        titleLarge = Typography().titleLarge.copy(fontFamily = FontFamily.Serif),
                        titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily.Serif),
                    ),
                    shapes = PinhoShapes,
                ) {
                    if (state.initializing) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Text("Preparando seu jardim…")
                        }
                    } else if (state.onboardingRequired) {
                        OnboardingScreen(
                            onComplete = viewModel::completeOnboarding,
                        )
                    } else {
                        PinhoQuestNav(
                            state = state,
                            onTabSelected = viewModel::selectTab,
                            onGenerateQuest = viewModel::generateQuest,
                            onOpenQuestThemeSelection = viewModel::openQuestThemeSelection,
                            onCloseQuestThemeSelection = viewModel::closeQuestThemeSelection,
                            onGenerateQuestFromThemeSelection = viewModel::confirmQuestThemeSelection,
                            onStartQuest = viewModel::startCurrentQuest,
                            onCompleteQuest = viewModel::completeCurrentQuest,
                            onAbandonQuest = viewModel::abandonCurrentQuest,
                            onFlowerSelected = viewModel::selectFlower,
                            onInvestigateFlower = viewModel::investigateFlower,
                            onDismissFlower = viewModel::dismissFlower,
                            onDismissCompletion = viewModel::dismissCompletion,
                            onOpenGardenFromCompletion = viewModel::openGardenFromCompletion,
                            onTagToggled = viewModel::setTagEnabled,
                            onThemeSelected = viewModel::setTheme,
                            onFontScaleSelected = viewModel::setFontScale,
                            onInstallCreativeBrain = { creativeBrainTermsVisible = true },
                            onBackup = {
                                lifecycleScope.launch {
                                    graph.buildBackupBytes()
                                        .onSuccess { bytes ->
                                            pendingBackup = bytes
                                            backupLauncher.launch("PinhoQuest-backup.pqbackup")
                                        }
                                        .onFailure {
                                            viewModel.showMessage("Não consegui preparar a cópia do seu jardim.")
                                        }
                                }
                            },
                            dark = useDark,
                            onDonate = {
                                viewModel.showMessage("O link de apoio será conectado ao destino externo canônico.")
                            },
                        )

                        if (creativeBrainTermsVisible) {
                            AlertDialog(
                                onDismissRequest = { creativeBrainTermsVisible = false },
                                title = { Text("Baixar cérebro criativo?") },
                                text = {
                                    Text(
                                        "O download tem cerca de 285 MB e instala um modelo " +
                                            "FunctionGemma ajustado para criar quests no aparelho. " +
                                            "Ao continuar, você reconhece que ele é distribuído sob os " +
                                            "Termos de Uso Gemma e as restrições de uso correspondentes.",
                                    )
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            creativeBrainTermsVisible = false
                                            if (graph.modelInstallCoordinator.enqueue() == null) {
                                                viewModel.showMessage("O cérebro criativo ainda não está disponível.")
                                            } else {
                                                viewModel.showMessage("Download do cérebro criativo iniciado.")
                                            }
                                        },
                                    ) { Text("Aceitar e baixar") }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { creativeBrainTermsVisible = false },
                                    ) { Text("Agora não") }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
