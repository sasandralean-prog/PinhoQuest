package com.pinhoquest

import android.app.Application
import com.pinhoquest.core.backup.BackupCodec
import com.pinhoquest.core.completion.QuestCompletionService
import com.pinhoquest.core.garden.FlowerInvestigationService
import com.pinhoquest.core.inference.AppWorkload
import com.pinhoquest.core.inference.InferenceAdmissionController
import com.pinhoquest.core.inference.InferenceModelDescriptor
import com.pinhoquest.core.inference.InferenceResourceSnapshot
import com.pinhoquest.core.inference.LearnedInferenceProfile
import com.pinhoquest.core.inference.RuntimeState
import com.pinhoquest.core.inference.micro.AdmissionAwareMicroQuestComposer
import com.pinhoquest.core.inference.micro.MicroQuestComposer
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.core.quest.QuestEngine
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.core.quest.QuestValidator
import com.pinhoquest.core.profile.RandomProfileIdFactory
import com.pinhoquest.core.research.GameCatalogCachePolicy
import com.pinhoquest.core.research.GameQuestCandidateSelector
import com.pinhoquest.core.research.GameQuestGenerationCoordinator
import com.pinhoquest.core.reward.RewardEngine
import com.pinhoquest.core.session.QuestContextProvider
import com.pinhoquest.core.session.QuestSessionService
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.data.AndroidDataGraph
import com.pinhoquest.data.model.AndroidModelStore
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.reward.RewardOpportunityId
import com.pinhoquest.inference.AndroidLiteRtLmInferencePort
import com.pinhoquest.inference.AndroidResourceSnapshotProvider
import java.io.File
import java.util.UUID

class PinhoQuestAppGraph(application: Application) {
    private val data = AndroidDataGraph(application)
    private val modelStore = AndroidModelStore(File(application.filesDir, "models"))
    private val resourceSnapshotProvider = AndroidResourceSnapshotProvider(application)

    val profileIdFactory = RandomProfileIdFactory

    val profileRepository = data.profileRepository
    val tagRepository = data.tagRepository
    val questRepository = data.questRepository
    val sessionRepository = data.sessionRepository
    val preferencesStore = data.preferencesStore
    val bootstrapper = data.bootstrapper
    val gardenRepository = data.gardenRepository
    private val backupSnapshotBuilder = data.backupSnapshotBuilder
    private val backupCodec = BackupCodec()

    suspend fun buildBackupBytes(): Result<ByteArray> = runCatching {
        when (val result = backupSnapshotBuilder.build(BuildConfig.VERSION_NAME)) {
            is com.pinhoquest.domain.backup.BackupSnapshotBuildResult.Ready -> backupCodec.encode(result.snapshot)
            com.pinhoquest.domain.backup.BackupSnapshotBuildResult.NoProfile -> error("Nenhum jardim para copiar ainda.")
            com.pinhoquest.domain.backup.BackupSnapshotBuildResult.MultipleProfiles -> error("Não foi possível preparar a cópia deste jardim.")
            is com.pinhoquest.domain.backup.BackupSnapshotBuildResult.InvalidState -> error(result.reason)
        }
    }

    val investigationService = FlowerInvestigationService(
        store = data.investigationStore,
        transactionIdFactory = { XpTransactionId(UUID.randomUUID().toString()) },
        nowEpochMillis = System::currentTimeMillis,
    )

    val completionService = QuestCompletionService(
        store = data.completionStore,
        rewardEngine = RewardEngine(),
        completionIdFactory = { CompletionId(UUID.randomUUID().toString()) },
        xpTransactionIdFactory = { XpTransactionId(UUID.randomUUID().toString()) },
        rewardOpportunityIdFactory = { RewardOpportunityId(UUID.randomUUID().toString()) },
        nowEpochMillis = System::currentTimeMillis,
    )

    private val contextProvider = QuestContextProvider {
        val profile = profileRepository.current()
        if (profile == null) {
            QuestContext()
        } else {
            val tags = tagRepository.list(profile.id)
            QuestContext(categoryAffinities = SystemTagCatalog.categoryAffinities(tags))
        }
    }

    private val gameQuestGenerationCoordinator = GameQuestGenerationCoordinator(
        catalogStore = data.gameDiscoveryCatalogStore,
        usageStore = data.gameDiscoveryCatalogStore,
        candidateSelector = GameQuestCandidateSelector(
            policy = GameCatalogCachePolicy.DEFAULT,
        ),
        nowEpochMillis = System::currentTimeMillis,
    )

    private val productionComposer = createProductionComposer(application)

    val sessionService = QuestSessionService(
        engine = QuestEngine(
            planner = QuestPlanner(),
            composer = productionComposer,
            validator = QuestValidator(),
        ),
        questRepository = questRepository,
        sessionRepository = sessionRepository,
        contextProvider = contextProvider,
        gameQuestGenerationCoordinator = gameQuestGenerationCoordinator,
    )

    private fun createProductionComposer(application: Application) = run {
        val active = modelStore.active() ?: return@run ProceduralComposer()
        val inference = AndroidLiteRtLmInferencePort(
            modelPath = active.modelFile.absolutePath,
            cacheDir = File(application.cacheDir, "litertlm").apply { mkdirs() }.absolutePath,
        )
        val descriptor = InferenceModelDescriptor(
            manifest = active.manifest,
            estimatedWorkingSetBytes = 959_030L * 1024L,
        )
        AdmissionAwareMicroQuestComposer(
            local = MicroQuestComposer(inference = inference),
            snapshotProvider = {
                resourceSnapshotProvider.snapshot(
                    runtimeState = RuntimeState.READY,
                    appWorkload = AppWorkload.IDLE,
                    inferenceBusy = false,
                    learnedProfile = LearnedInferenceProfile(),
                )
            },
            modelDescriptorProvider = { descriptor },
            admissionController = InferenceAdmissionController(),
        )
    }
}
