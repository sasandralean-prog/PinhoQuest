package com.pinhoquest.core.inference.micro

import org.junit.Assert.assertEquals
import org.junit.Test

class FunctionGemmaResponseSanitizerTest {
    private val sanitizer = FunctionGemmaResponseSanitizer()

    @Test
    fun stripsKnownFunctionGemmaMarkersAndExtractsJson() {
        val raw = """<start_of_turn>model<escape>{"title":"Explorar","description":"Procure um detalhe novo ao seu redor.","objectives":["Encontre um detalhe"]}<end_of_turn>"""

        val result = sanitizer.extract(raw)

        assertEquals("Explorar", result.title)
        assertEquals(listOf("Encontre um detalhe"), result.objectives)
    }

    @Test
    fun acceptsChatMlAndMarkdownEnvelope() {
        val raw = """<|im_start|>model\n```json\n{"title":"Criar","description":"Faça algo pequeno e diferente hoje.","objectives":["Escolha uma ideia","Execute a ideia"]}\n```\n<|im_end|>"""

        val result = sanitizer.extract(raw)

        assertEquals("Criar", result.title)
        assertEquals(2, result.objectives.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsExtraFields() {
        sanitizer.extract("""{"title":"X","description":"Descrição suficiente aqui.","objectives":["Fazer"],"xp":10}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonStringObjective() {
        sanitizer.extract("""{"title":"X","description":"Descrição suficiente aqui.","objectives":[123]}""")
    }
}
