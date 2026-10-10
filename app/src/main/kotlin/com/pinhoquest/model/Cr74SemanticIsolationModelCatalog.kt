package com.pinhoquest.model

/**
 * The one production package known to match the P3 native ToolCall contract.
 *
 * Keep this metadata in code rather than accepting a mutable server manifest: the
 * downloader and AndroidModelStore must agree on the exact bytes that may be
 * promoted to the active model directory.
 */
object Cr74SemanticIsolationModelCatalog : ModelPackageCatalog {
    const val MODEL_ID = "cr74_semantic_isolation"
    const val VERSION = "1"
    const val RUNTIME_FORMAT = "litertlm"
    const val SHA256 = "e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb"
    const val BYTES = 284_692_656L
    const val CONTEXT_LIMIT = 1_280
    const val LICENSE = "Gemma Terms of Use; FunctionGemma derivative"
    const val SOURCE = "PinhoQuest CR-7.4 semantic-isolation release artifact"
    const val DOWNLOAD_URL =
        "https://github.com/sasandralean-prog/PinhoQuest/releases/download/" +
            "cr74-semantic-isolation-v1/pinhoquest-cr74-semantic-isolation-v1.litertlm"

    override fun current(): ModelDownloadSpec = ModelDownloadSpec(
        modelId = MODEL_ID,
        version = VERSION,
        runtimeFormat = RUNTIME_FORMAT,
        sha256 = SHA256,
        bytes = BYTES,
        license = LICENSE,
        source = SOURCE,
        contextLimit = CONTEXT_LIMIT,
        supportedBackends = listOf("CPU"),
        downloadUrl = DOWNLOAD_URL,
    )
}
