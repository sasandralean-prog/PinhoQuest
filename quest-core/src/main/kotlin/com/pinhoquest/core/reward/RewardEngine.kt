package com.pinhoquest.core.reward

import com.pinhoquest.domain.garden.CatalogPack
import com.pinhoquest.domain.garden.FlowerInventory
import com.pinhoquest.domain.reward.DeferredRewardReason
import com.pinhoquest.domain.reward.RewardOpportunity
import com.pinhoquest.domain.reward.RewardResolution
import kotlin.random.Random

fun interface UniformIndexSource {
    fun nextInt(bound: Int): Int
}

class RewardEngine(
    private val indexSource: UniformIndexSource = UniformIndexSource { bound ->
        Random.Default.nextInt(bound)
    },
) {
    fun resolve(
        opportunity: RewardOpportunity,
        catalog: CatalogPack,
        inventory: FlowerInventory,
    ): RewardResolution {
        val eligible = catalog.entries
            .asSequence()
            .filter { it.eligible }
            .map { it.flower.id }
            .filterNot(inventory.acquiredFlowerIds::contains)
            .sortedBy { it.value }
            .toList()

        if (eligible.isEmpty()) {
            return RewardResolution.Deferred(
                opportunityId = opportunity.id,
                reason = DeferredRewardReason.NO_ELIGIBLE_FLOWER,
            )
        }

        val index = indexSource.nextInt(eligible.size)
        require(index in eligible.indices) {
            "UniformIndexSource returned out-of-range index " + index + " for bound " + eligible.size
        }
        return RewardResolution.Awarded(
            opportunityId = opportunity.id,
            flowerId = eligible[index],
        )
    }
}
