package com.pinhoquest.data.repository

import com.pinhoquest.core.session.QuestRepository
import com.pinhoquest.data.db.dao.QuestDao
import com.pinhoquest.data.db.entity.QuestEntity
import com.pinhoquest.data.db.entity.QuestObjectiveEntity
import com.pinhoquest.domain.quest.EstimatedDuration
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.Quest
import com.pinhoquest.domain.quest.QuestCategory
import com.pinhoquest.domain.quest.QuestDifficulty
import com.pinhoquest.domain.quest.QuestEnvironment
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestObjective
import com.pinhoquest.domain.quest.QuestState

class RoomQuestRepository(
    private val dao: QuestDao,
) : QuestRepository {
    override suspend fun upsert(quest: Quest) {
        dao.replace(
            quest = QuestEntity(
                questId = quest.id.value,
                title = quest.title,
                description = quest.description,
                category = quest.category.name,
                environment = quest.environment.name,
                minMinutes = quest.estimatedDuration.minMinutes,
                maxMinutes = quest.estimatedDuration.maxMinutes,
                difficulty = quest.difficulty.name,
                state = quest.state.name,
            ),
            objectives = quest.objectives.map { objective ->
                QuestObjectiveEntity(
                    questId = quest.id.value,
                    objectiveId = objective.id.value,
                    text = objective.text,
                    optional = objective.optional,
                )
            },
        )
    }

    override suspend fun get(questId: QuestId): Quest? =
        dao.getWithObjectives(questId.value)?.let { row ->
            val entity = row.quest
            Quest(
                id = QuestId(entity.questId),
                title = entity.title,
                description = entity.description,
                objectives = row.objectives
                    .sortedBy { it.objectiveId }
                    .map {
                        QuestObjective(
                            id = ObjectiveId(it.objectiveId),
                            text = it.text,
                            optional = it.optional,
                        )
                    },
                category = QuestCategory.valueOf(entity.category),
                environment = QuestEnvironment.valueOf(entity.environment),
                estimatedDuration = EstimatedDuration(
                    minMinutes = entity.minMinutes,
                    maxMinutes = entity.maxMinutes,
                ),
                difficulty = QuestDifficulty.valueOf(entity.difficulty),
                state = QuestState.valueOf(entity.state),
            )
        }

    override suspend fun recentCategories(limit: Int): List<QuestCategory> =
        dao.recentCategories(limit.coerceAtLeast(0)).mapNotNull { category ->
            runCatching { QuestCategory.valueOf(category) }.getOrNull()
        }
}
