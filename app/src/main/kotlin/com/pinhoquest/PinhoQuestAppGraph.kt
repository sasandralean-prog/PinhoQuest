package com.pinhoquest

import android.app.Application
import com.pinhoquest.data.AndroidDataGraph
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.core.quest.QuestEngine
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.core.profile.RandomProfileIdFactory
import com.pinhoquest.core.quest.QuestValidator
import com.pinhoquest.core.session.QuestContextProvider
import com.pinhoquest.core.session.QuestSessionService
import com.pinhoquest.core.tag.SystemTagCatalog

class PinhoQuestAppGraph(application: Application) {
    private val data = AndroidDataGraph(application)

    val profileIdFactory = RandomProfileIdFactory

    val profileRepository = data.profileRepository
    val tagRepository = data.tagRepository
    val questRepository = data.questRepository
    val sessionRepository = data.sessionRepository
    val preferencesStore = data.preferencesStore

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
