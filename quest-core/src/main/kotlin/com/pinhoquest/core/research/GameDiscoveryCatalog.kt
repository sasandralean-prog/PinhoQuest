package com.pinhoquest.core.research

class GameDiscoveryCatalog(
    initialItems: List<GameDiscovery> = emptyList(),
) {
    private val itemsByIdentity = linkedMapOf<String, GameDiscovery>()

    init {
        merge(initialItems)
    }

    fun snapshot(): List<GameDiscovery> = itemsByIdentity.values.toList()

    fun merge(items: List<GameDiscovery>): List<GameDiscovery> {
        items.forEach { incoming ->
            val key = incoming.identityKey
            val existing = itemsByIdentity[key]
            itemsByIdentity[key] = if (existing == null) {
                incoming
            } else {
                existing.copy(
                    platforms = existing.platforms + incoming.platforms,
                    genres = existing.genres + incoming.genres,
                    availability = mergeAvailability(existing.availability, incoming.availability),
                    provenance = existing.provenance + incoming.provenance,
                )
            }
        }
        return snapshot()
    }

    fun isEmpty(): Boolean = itemsByIdentity.isEmpty()

    private fun mergeAvailability(
        current: GameAvailability,
        incoming: GameAvailability,
    ): GameAvailability = when {
        current == incoming -> current
        current == GameAvailability.UNKNOWN -> incoming
        incoming == GameAvailability.UNKNOWN -> current
        else -> GameAvailability.UNKNOWN
    }
}
