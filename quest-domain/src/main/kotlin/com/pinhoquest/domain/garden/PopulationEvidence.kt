package com.pinhoquest.domain.garden

enum class CountingBasis {
    INDIVIDUALS,
    MATURE_INDIVIDUALS,
}

enum class EstimateConfidence {
    LOW,
    MEDIUM,
    HIGH,
}

data class GlobalPopulationEstimate(
    val estimatedIndividuals: Long?,
    val lowerBound: Long?,
    val upperBound: Long?,
    val estimateDateEpochMillis: Long?,
    val evidenceSourceUris: Set<String>,
    val confidence: EstimateConfidence,
    val countingBasis: CountingBasis,
)
