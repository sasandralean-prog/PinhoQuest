package com.pinhoquest.core.progression

object LevelPolicyV1 {
    const val VERSION = 1

    private val thresholds = listOf(
        0,
        100,
        200,
        400,
        700,
        1_100,
        1_600,
        2_200,
        2_900,
        3_700,
    )

    fun levelFor(lifetimeXp: Int): Int {
        require(lifetimeXp >= 0) { "lifetimeXp must be non-negative" }
        return thresholds.count { lifetimeXp >= it }.coerceAtLeast(1)
    }
}
