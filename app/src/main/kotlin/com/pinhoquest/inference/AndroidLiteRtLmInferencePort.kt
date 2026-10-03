package com.pinhoquest.inference

import com.pinhoquest.core.inference.GenerationRequest
import com.pinhoquest.core.inference.InferenceBudget
import com.pinhoquest.core.inference.InferenceOutcome
import com.pinhoquest.core.inference.InferenceTelemetrySink
import com.pinhoquest.core.inference.LocalInferencePort
import com.pinhoquest.inference.bridge.LiteRtLmRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kotlin-facing adapter for the native LiteRT-LM bridge.
 *
 * The bridge owns the LiteRT-LM API and exposes only the governed core
 * InferenceOutcome boundary to the rest of the app.
 */
class AndroidLiteRtLmInferencePort(
    modelPath: String,
    cacheDir: String,
    private val budget: InferenceBudget = InferenceBudget.P3,
    telemetrySink: InferenceTelemetrySink = InferenceTelemetrySink { },
) : LocalInferencePort, AutoCloseable {
    private val runtime = LiteRtLmRuntime(modelPath, cacheDir, budget, telemetrySink)

    override suspend fun generate(request: GenerationRequest): InferenceOutcome =
        withContext(Dispatchers.Default) {
            runtime.generate(request)
        }

    override fun close() {
        runtime.close()
    }
}
