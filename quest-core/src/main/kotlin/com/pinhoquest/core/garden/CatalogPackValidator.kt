package com.pinhoquest.core.garden

import com.pinhoquest.domain.garden.CatalogPack
import com.pinhoquest.domain.garden.FlowerRarity

data class CatalogValidationResult(
    val errors: List<String>,
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

class CatalogPackValidator(
    private val rarityScale: FlowerRarityScale,
) {
    fun validate(pack: CatalogPack): CatalogValidationResult {
        val errors = mutableListOf<String>()
        if (pack.entries.isEmpty()) errors += "catalog must contain at least one entry"

        pack.entries.forEachIndexed { index, entry ->
            val flower = entry.flower
            val prefix = "entry[" + index + "] " + flower.id.value
            if (flower.id.value.isBlank()) errors += prefix + " has blank id"
            if (flower.commonName.isBlank()) errors += prefix + " has blank commonName"
            if (flower.scientificName.isBlank()) errors += prefix + " has blank scientificName"

            if (flower.rarity != FlowerRarity.UNKNOWN) {
                val evidence = flower.populationEstimate
                if (evidence == null) {
                    errors += prefix + " classified rarity has no population evidence"
                } else {
                    if (flower.rarityScaleVersion != rarityScale.version) {
                        errors += prefix + " uses wrong rarity scale version"
                    }
                    val derived = rarityScale.classify(evidence)
                    if (derived != flower.rarity) {
                        errors += prefix + " stored rarity " + flower.rarity + " differs from derived " + derived
                    }
                }
            }
        }

        val scientificNames = pack.entries.map { it.flower.scientificName.trim().lowercase() }
        if (scientificNames.distinct().size != scientificNames.size) {
            errors += "scientific names must be unique inside a catalog pack"
        }

        return CatalogValidationResult(errors)
    }
}
