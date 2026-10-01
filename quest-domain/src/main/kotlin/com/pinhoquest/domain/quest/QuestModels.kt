package com.pinhoquest.domain.quest

@JvmInline value class QuestId(val value: String)
@JvmInline value class ObjectiveId(val value: String)

enum class QuestMode { NORMAL, GAME, RANDOM }
enum class QuestState { GENERATED, ACCEPTED, ACTIVE, COMPLETED, ABANDONED, REJECTED }
enum class QuestCategory { CODING, GAMING, CREATIVE, EXPLORATION, LEARNING, RANDOM }
enum class QuestEnvironment { ANDROID, WINDOWS, ANYWHERE, OUTDOOR }
enum class QuestDifficulty { CASUAL, EASY, MEDIUM, CHALLENGE }

data class EstimatedDuration(
    val minMinutes: Int,
    val maxMinutes: Int,
)

data class QuestSessionFilters(
    val minMinutes: Int? = null,
    val maxMinutes: Int? = null,
    val environments: Set<QuestEnvironment> = emptySet(),
    val categories: Set<QuestCategory> = emptySet(),
    val desiredDifficulty: QuestDifficulty? = null,
)

data class QuestRequest(
    val mode: QuestMode,
    val filters: QuestSessionFilters = QuestSessionFilters(),
)

data class QuestObjective(
    val id: ObjectiveId,
    val text: String,
    val optional: Boolean = false,
)

data class QuestDraft(
    val title: String,
    val description: String,
    val objectives: List<QuestObjective>,
    val category: QuestCategory,
    val environment: QuestEnvironment,
    val estimatedDuration: EstimatedDuration,
    val difficulty: QuestDifficulty,
)

data class Quest(
    val id: QuestId,
    val title: String,
    val description: String,
    val objectives: List<QuestObjective>,
    val category: QuestCategory,
    val environment: QuestEnvironment,
    val estimatedDuration: EstimatedDuration,
    val difficulty: QuestDifficulty,
    val state: QuestState = QuestState.GENERATED,
)
