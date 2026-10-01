package com.pinhoquest.domain.garden

import com.pinhoquest.domain.profile.ProfileId

enum class FlowerDiscoveryState {
    HIDDEN,
    HINTED,
    REVEALED,
    COLLECTED,
}

data class FlowerDiscovery(
    val profileId: ProfileId,
    val flowerId: FlowerId,
    val state: FlowerDiscoveryState,
)
