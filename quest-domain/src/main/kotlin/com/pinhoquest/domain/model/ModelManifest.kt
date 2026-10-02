package com.pinhoquest.domain.model

data class ModelManifest(
    val modelId: String,
    val version: String,
    val runtimeFormat: String,
    val sha256: String,
    val bytes: Long,
    val license: String,
    val source: String,
    val contextLimit: Int,
    val supportedBackends: Set<String>,
) {
    init {
        require(modelId.isNotBlank()) { "modelId must not be blank" }
        require(version.isNotBlank()) { "version must not be blank" }
        require(runtimeFormat.isNotBlank()) { "runtimeFormat must not be blank" }
        require(sha256.matches(SHA256_PATTERN)) { "sha256 must be a lowercase SHA-256 digest" }
        require(bytes > 0L) { "bytes must be positive" }
        require(license.isNotBlank()) { "license must not be blank" }
        require(source.isNotBlank()) { "source must not be blank" }
        require(contextLimit > 0) { "contextLimit must be positive" }
        require(supportedBackends.isNotEmpty()) { "supportedBackends must not be empty" }
    }

    private companion object {
        val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}
