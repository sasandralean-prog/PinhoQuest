package com.pinhoquest.domain.tag

@JvmInline
value class TagId(val value: String)

enum class TagSource { SYSTEM, USER, DERIVED }

data class Tag(
    val id: TagId,
    val label: String,
    val source: TagSource,
    val affinity: Double = 0.5,
    val enabled: Boolean = true,
)
