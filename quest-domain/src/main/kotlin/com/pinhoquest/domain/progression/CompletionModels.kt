package com.pinhoquest.domain.progression

import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.quest.ObjectiveId
import com.pinhoquest.domain.quest.QuestId
import com.pinhoquest.domain.quest.QuestSessionId

data class QuestCompletion(
    val id: CompletionId,
    val sessionId: QuestSessionId,
    val questId: QuestId,
    val profileId: ProfileId,
    val completedObjectiveIds: Set<ObjectiveId>,
    val xpAward: Int,
    val completedAtEpochMillis: Long,
)
