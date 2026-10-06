package com.pinhoquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinhoquest.data.settings.ThemePreference
import com.pinhoquest.ui.PinhoCream
import com.pinhoquest.ui.PinhoForest
import com.pinhoquest.ui.PinhoInk
import com.pinhoquest.ui.PinhoShapes
import com.pinhoquest.ui.navigation.PinhoQuestNav
import com.pinhoquest.ui.onboarding.OnboardingScreen

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
                        )
                    }
                }
            }
        }
    }
}
