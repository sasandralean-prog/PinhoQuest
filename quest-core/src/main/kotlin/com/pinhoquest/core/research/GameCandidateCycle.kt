package com.pinhoquest.core.research

class GameCandidateCycleSequence(
    private var nextSequence: Long = 1L,
) {
    init { require(nextSequence > 0L) { "nextSequence must be positive" } }

    fun next(researchedAtEpochMillis: Long): GameCandidateCycleId {
        require(researchedAtEpochMillis > 0L) { "researchedAtEpochMillis must be positive" }
        return GameCandidateCycleId.create(researchedAtEpochMillis, nextSequence++)
    }
}
