package com.pinhoquest.domain.profile

@JvmInline
value class GardenOwnerName private constructor(val value: String) {
    companion object {
        const val MAX_CHARACTERS: Int = 20

        fun create(raw: String): Result<GardenOwnerName> {
            val trimmed = raw.trim()
            val characterCount = trimmed.codePointCount(0, trimmed.length)
            return if (characterCount in 1..MAX_CHARACTERS) {
                Result.success(GardenOwnerName(trimmed))
            } else {
                Result.failure(
                    IllegalArgumentException("Garden owner name must contain 1 to 20 characters"),
                )
            }
        }
    }
}
