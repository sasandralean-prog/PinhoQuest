package com.pinhoquest.domain.garden

@JvmInline value class CatalogPackId(val value: String)

data class CatalogEntry(
    val flower: FlowerDefinition,
    val eligible: Boolean,
)

data class CatalogPack(
    val id: CatalogPackId,
    val version: Int,
    val generatedAtEpochMillis: Long,
    val entries: List<CatalogEntry>,
    val sourceUris: Set<String>,
    val integrityHash: String,
) {
    init {
        require(version > 0) { "catalog version must be positive" }
        require(entries.map { it.flower.id }.distinct().size == entries.size) {
            "catalog flower ids must be unique"
        }
        require(integrityHash.isNotBlank()) { "integrityHash must not be blank" }
    }
}

data class FlowerInventory(
    val acquiredFlowerIds: Set<FlowerId>,
)
