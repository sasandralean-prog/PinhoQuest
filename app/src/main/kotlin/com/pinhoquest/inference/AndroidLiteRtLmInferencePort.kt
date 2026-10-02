package com.pinhoquest.inference

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.ResponseFormat
import com.pinhoquest.core.inference.GenerationRequest
import com.pinhoquest.core.inference.InferenceOutcome
import com.pinhoquest.core.inference.LocalInferencePort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidLiteRtLmInferencePort(
    modelPath: String,
    cacheDir: String,
    private val maxNumTokens: Int = 1280,
) : LocalInferencePort, AutoCloseable {
    private val engine = Engine(
        EngineConfig(
            modelPath,
            Backend.CPU(),
            null,
            null,
            maxNumTokens,
            null,
            cacheDir,
        ),
    )

    @Volatile
    private var initialized = false

    override suspend fun generate(request: GenerationRequest): InferenceOutcome =
        withContext(Dispatchers.Default) {
            runCatching {
                ensureInitialized()
                createConversation().use { conversation ->
                    val response = conversation.sendMessage(
                        Message.Companion.of(request.prompt),
                        emptyMap(),
                        null,
                        null,
                        null,
                        request.maxOutputTokens,
                        ThinkingConfig(false, 0),
                        ResponseFormat.json(textSchema()),
                    )
                    InferenceOutcome.Success(
                        conversation.renderMessageIntoString(response, emptyMap()),
                    )
                }
            }.getOrElse { error ->
                classify(error)
            }
        }

    private fun ensureInitialized() {
        if (!initialized) synchronized(this) {
            if (!initialized) {
                engine.initialize()
                initialized = true
            }
        }
    }

    private fun createConversation(): Conversation = engine.createConversation(
        ConversationConfig(
            null,
            emptyList(),
            emptyList(),
            SamplerConfig(40, 0.9, 0.2, 42),
            false,
            emptyList(),
            emptyMap(),
            null,
            false,
            128,
            ThinkingConfig(false, 0),
            true,
        ),
    )

    private fun classify(error: Throwable): InferenceOutcome =
        when {
            error.message.orEmpty().contains("NOT_FOUND", ignoreCase = true) ->
                InferenceOutcome.ModelUnavailable
            error.message.orEmpty().contains("UNAVAILABLE", ignoreCase = true) ->
                InferenceOutcome.RuntimeUnavailable
            error.message.orEmpty().contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                InferenceOutcome.InsufficientResources
            else -> InferenceOutcome.TechnicalFailure
        }

    override fun close() {
        synchronized(this) {
            if (initialized) engine.close()
            initialized = false
        }
    }

    private fun textSchema(): Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "title" to mapOf("type" to "string", "minLength" to 1, "maxLength" to 80),
            "description" to mapOf("type" to "string", "minLength" to 12, "maxLength" to 220),
            "objectives" to mapOf(
                "type" to "array",
                "minItems" to 1,
                "maxItems" to 4,
                "items" to mapOf("type" to "string", "minLength" to 1, "maxLength" to 120),
            ),
        ),
        "required" to listOf("title", "description", "objectives"),
        "additionalProperties" to false,
    )
}
