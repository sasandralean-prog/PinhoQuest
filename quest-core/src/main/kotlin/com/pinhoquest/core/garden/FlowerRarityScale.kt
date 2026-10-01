package com.pinhoquest.core.garden

import com.pinhoquest.domain.garden.CountingBasis
import com.pinhoquest.domain.garden.FlowerRarity
import com.pinhoquest.domain.garden.GlobalPopulationEstimate
import kotlin.math.log10

data class FlowerRarityScale(
    val version: Int,
    val referenceMinPopulation: Long,
    val referenceMaxPopulation: Long,
    val requiredCountingBasis: CountingBasis,
) {
    init {
        require(version > 0) { "version must be positive" }
        require(referenceMinPopulation > 0) { "referenceMinPopulation must be positive" }
        require(referenceMaxPopulation > referenceMinPopulation) {
            "referenceMaxPopulation must be greater than referenceMinPopulation"
        }
    }

    fun classify(estimate: GlobalPopulationEstimate): FlowerRarity {
        val population = estimate.estimatedIndividuals ?: return FlowerRarity.UNKNOWN
        if (population <= 0L) return FlowerRarity.UNKNOWN
        val estimateDate = estimate.estimateDateEpochMillis
        if (estimateDate == null || estimateDate <= 0L) {
            return FlowerRarity.UNKNOWN
        }
        if (estimate.evidenceSourceUris.isEmpty() || estimate.evidenceSourceUris.any { it.isBlank() }) {
            return FlowerRarity.UNKNOWN
        }
        if (estimate.countingBasis != requiredCountingBasis) return FlowerRarity.UNKNOWN

        val minLog = log10(referenceMinPopulation.toDouble())
        val maxLog = log10(referenceMaxPopulation.toDouble())
        val populationLog = log10(population.toDouble())
        val position = ((maxLog - populationLog) / (maxLog - minLog)).coerceIn(0.0, 1.0)

        return when {
            position < 0.25 -> FlowerRarity.COMMON
            position < 0.50 -> FlowerRarity.UNCOMMON
            position < 0.75 -> FlowerRarity.RARE
            else -> FlowerRarity.RAREST
        }
    }
}
