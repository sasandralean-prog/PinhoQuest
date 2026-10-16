package com.pinhoquest

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Observer
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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID

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
            var modelInstallWorkId by rememberSaveable { mutableStateOf<String?>(null) }
            var modelInstallStatusVisible by rememberSaveable { mutableStateOf(false) }
            var modelWorkInfo by remember(modelInstallWorkId) {
                mutableStateOf<WorkInfo?>(null)
            }
            DisposableEffect(modelInstallWorkId) {
                val id = modelInstallWorkId
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val liveData = id?.let {
                    WorkManager.getInstance(this@MainActivity).getWorkInfoByIdLiveData(it)
                }
                val observer = liveData?.let {
                    Observer<WorkInfo?> { info -> modelWorkInfo = info }
                }
                if (liveData != null && observer != null) {
                    liveData.observeForever(observer)
                }
                onDispose {
                    if (liveData != null && observer != null) {
                        liveData.removeObserver(observer)
                    }
                }
            }
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
                        // Script typography is reserved for short display headings.
                        // Body copy, labels, navigation, metrics and controls stay legible.
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
                            onShowModelInstallStatus = if (modelInstallWorkId != null) ({ modelInstallStatusVisible = true }) else null,
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
                                            val workId = graph.modelInstallCoordinator.enqueue()
                                            if (workId == null) {
                                                viewModel.showMessage("O cérebro criativo ainda não está disponível.")
                                            } else {
                                                modelInstallWorkId = workId.toString()
                                                modelInstallStatusVisible = true
                                                viewModel.showMessage("Pedido de download enviado. Você pode acompanhar o progresso aqui.")
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

                        if (modelInstallStatusVisible) {
                            val workInfo = modelWorkInfo
                            val workState = workInfo?.state
                            val stage = workInfo?.progress?.getString(com.pinhoquest.model.ModelDownloadWorker.KEY_STAGE)
                            val percent = workInfo?.progress?.getInt(com.pinhoquest.model.ModelDownloadWorker.KEY_PERCENT, 0)?.coerceIn(0, 100) ?: 0
                            val downloaded = workInfo?.progress?.getLong(com.pinhoquest.model.ModelDownloadWorker.KEY_DOWNLOADED, 0L) ?: 0L
                            val total = workInfo?.progress?.getLong(com.pinhoquest.model.ModelDownloadWorker.KEY_TOTAL, com.pinhoquest.model.Cr74SemanticIsolationModelCatalog.BYTES) ?: com.pinhoquest.model.Cr74SemanticIsolationModelCatalog.BYTES
                            val terminal = workState == WorkInfo.State.SUCCEEDED || workState == WorkInfo.State.FAILED || workState == WorkInfo.State.CANCELLED
                            val statusText = when (workState) {
                                WorkInfo.State.ENQUEUED -> "Aguardando conexão ou início do download…"
                                WorkInfo.State.BLOCKED -> "Aguardando a conclusão de outra tarefa…"
                                WorkInfo.State.RUNNING -> when (stage) {
                                    com.pinhoquest.model.ModelDownloadWorker.STAGE_VERIFYING -> "Verificando integridade do arquivo…"
                                    com.pinhoquest.model.ModelDownloadWorker.STAGE_INSTALLING -> "Instalando e ativando o modelo…"
                                    com.pinhoquest.model.ModelDownloadWorker.STAGE_RETRYING -> "Conexão interrompida. Tentando retomar…"
                                    else -> "Baixando modelo…"
                                }
                                WorkInfo.State.SUCCEEDED -> "Modelo baixado, validado e instalado com sucesso."
                                WorkInfo.State.FAILED -> "Não foi possível baixar, validar ou instalar o modelo. Você pode tentar novamente."
                                WorkInfo.State.CANCELLED -> "O download foi cancelado."
                                else -> "Preparando acompanhamento do download…"
                            }
                            AlertDialog(
                                onDismissRequest = { if (terminal) modelInstallStatusVisible = false },
                                title = { Text("Cérebro criativo") },
                                text = {
                                    androidx.compose.foundation.layout.Column {
                                        Text(statusText)
                                        if (!terminal) {
                                            Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                                            LinearProgressIndicator(progress = { percent / 100f }, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                                            Spacer(modifier = androidx.compose.ui.Modifier.height(6.dp))
                                            Text(
                                                if (stage == com.pinhoquest.model.ModelDownloadWorker.STAGE_VERIFYING || stage == com.pinhoquest.model.ModelDownloadWorker.STAGE_INSTALLING) {
                                                    "Arquivo recebido: " + (total / (1024L * 1024L)) + " MB. Conclusão: " + percent + "%"
                                                } else {
                                                    (downloaded / (1024L * 1024L)).toString() + " de " + (total / (1024L * 1024L)) + " MB · " + percent + "%"
                                                },
                                            )
                                            Text("Você pode fechar esta janela; o trabalho continua em segundo plano.")
                                        } else if (workState == WorkInfo.State.FAILED) {
                                            Text("O modelo não será usado até que a verificação de integridade termine com sucesso.")
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = { modelInstallStatusVisible = false },
                                        enabled = terminal,
                                    ) { Text("Fechar") }
                                },
                                dismissButton = {
                                    if (!terminal) {
                                        TextButton(onClick = { modelInstallStatusVisible = false }) { Text("Continuar em segundo plano") }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
