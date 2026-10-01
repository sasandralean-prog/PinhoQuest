package com.pinhoquest.domain.reward

import com.pinhoquest.domain.garden.FlowerId
import com.pinhoquest.domain.profile.ProfileId
import com.pinhoquest.domain.progression.CompletionId

@JvmInline value class RewardOpportunityId(val value: String)

data class RewardOpportunity(
    val id: RewardOpportunityId,
    val profileId: ProfileId,
    val completionId: CompletionId,
    val createdAtEpochMillis: Long,
)

enum class DeferredRewardReason {
    NO_ELIGIBLE_FLOWER,
}

sealed interface RewardResolution {
    data class Awarded(
        val opportunityId: RewardOpportunityId,
        val flowerId: FlowerId,
    ) : RewardResolution

    data class Deferred(
        val opportunityId: RewardOpportunityId,
        val reason: DeferredRewardReason,
    ) : RewardResolution
}
