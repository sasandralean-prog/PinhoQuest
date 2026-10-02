package com.pinhoquest.core.inference.micro

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class FunctionGemmaToolCallExtractorTest {
    private val extractor = FunctionGemmaToolCallExtractor()

    @Test
    fun extractsNativeFunctionCallArguments() {
        val raw = """<start_function_call>call:compose_quest_text{"title":"Caca aos detalhes","description":"Encontre tres detalhes novos ao seu redor.","objectives":["Observe o ambiente","Anote tres detalhes"]}<end_function_call>"""
        assertEquals(
            MicroQuestText("Caca aos detalhes", "Encontre tres detalhes novos ao seu redor.", listOf("Observe o ambiente", "Anote tres detalhes")),
            extractor.extract(raw),
        )
    }

    @Test
    fun rejectsUnexpectedFunction() {
        assertFailure { extractor.extract("<start_function_call>call:delete_file{\"path\":\"x\"}<end_function_call>") }
    }

    @Test
    fun rejectsExtraArguments() {
        assertFailure { extractor.extract("<start_function_call>call:compose_quest_text{\"title\":\"x\",\"description\":\"Uma descricao valida qualquer.\",\"objectives\":[\"a\"],\"xp\":50}<end_function_call>") }
    }

    @Test
    fun rejectsMissingMarkers() {
        assertFailure { extractor.extract("call:compose_quest_text{\"title\":\"x\"}") }
    }

    private fun assertFailure(block: () -> Unit) {
        try {
            block()
            fail("expected failure")
        } catch (_: IllegalArgumentException) {
        }
    }
}