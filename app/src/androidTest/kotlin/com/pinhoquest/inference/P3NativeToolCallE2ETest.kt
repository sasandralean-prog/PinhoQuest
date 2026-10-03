package com.pinhoquest.inference

import androidx.test.platform.app.InstrumentationRegistry
import com.pinhoquest.core.inference.GenerationRequest
import com.pinhoquest.core.inference.InferenceOutcome
import com.pinhoquest.core.inference.InferenceTelemetrySink
import com.pinhoquest.core.inference.micro.MicroQuestToolContract
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class P3NativeToolCallE2ETest {
    @Test
    fun realModelReturnsExactlyOneCanonicalToolCall() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val modelPath = arguments.getString("p3_model_path")
        assumeTrue("p3_model_path instrumentation argument is required", !modelPath.isNullOrBlank())

        val telemetry = mutableListOf<com.pinhoquest.core.inference.InferenceTelemetry>()
        val runtime = AndroidLiteRtLmInferencePort(
            modelPath = modelPath!!,
            cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.absolutePath,
            telemetrySink = InferenceTelemetrySink { telemetry += it },
        )

        try {
            val startedAtNanos = System.nanoTime()
            val outcome = runtime.generate(
                GenerationRequest(
                    prompt = """
                        Crie uma micro-quest de programação.
                        Categoria: PROGRAMMING.
                        Ambiente: DESKTOP.
                        Dificuldade: EASY.
                        Tempo: 10 minutos.
                        Idioma: pt-BR. Tom: casual e convidativo.
                        Gere exatamente uma chamada para compose_quest_text.
                        Nao invente XP, recompensa, regras ou metadata.
                    """.trimIndent(),
                ),
            )
            val elapsedMs = (System.nanoTime() - startedAtNanos) / 1_000_000

            assertTrue(
                "expected native tool call, got $outcome; elapsedMs=$elapsedMs; telemetry=$telemetry",
                outcome is InferenceOutcome.ToolCall,
            )
            val call = outcome as InferenceOutcome.ToolCall
            assertEquals(MicroQuestToolContract.NAME, call.name)
            assertEquals(MicroQuestToolContract.REQUIRED_ARGUMENTS.toSet(), call.arguments.keys)
            assertTrue(call.arguments[MicroQuestToolContract.TITLE] is String)
            assertTrue(call.arguments[MicroQuestToolContract.DESCRIPTION_FIELD] is String)
            assertTrue(call.arguments[MicroQuestToolContract.OBJECTIVES] is List<*>)

            val event = telemetry.single()
            assertEquals(1, event.observedToolCalls)
        } finally {
            runtime.close()
        }
    }
}
