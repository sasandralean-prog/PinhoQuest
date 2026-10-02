package com.pinhoquest

import android.app.Application
import com.pinhoquest.data.AndroidDataGraph
import com.pinhoquest.core.completion.QuestCompletionService
import com.pinhoquest.core.garden.FlowerInvestigationService
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.core.quest.QuestEngine
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.core.profile.RandomProfileIdFactory
import com.pinhoquest.core.quest.QuestValidator
import com.pinhoquest.core.session.QuestContextProvider
import com.pinhoquest.core.reward.RewardEngine
import com.pinhoquest.core.session.QuestSessionService
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.domain.progression.CompletionId
import com.pinhoquest.domain.progression.XpTransactionId
import com.pinhoquest.domain.reward.RewardOpportunityId
import java.util.UUID

class PinhoQuestAppGraph(application: Application) {
    private val data = AndroidDataGraph(application)

    val profileIdFactory = RandomProfileIdFactory

    val profileRepository = data.profileRepository
    val tagRepository = data.tagRepository
    val questRepository = data.questRepository
    val sessionRepository = data.sessionRepository
    val preferencesStore = data.preferencesStore
    val bootstrapper = data.bootstrapper
    val gardenRepository = data.gardenRepository

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

    val sessionService = QuestSessionService(
        engine = QuestEngine(
            planner = QuestPlanner(),
            composer = ProceduralComposer(),
            validator = QuestValidator(),
        ),
        questRepository = questRepository,
        sessionRepository = sessionRepository,
        contextProvider = contextProvider,
    )
}
