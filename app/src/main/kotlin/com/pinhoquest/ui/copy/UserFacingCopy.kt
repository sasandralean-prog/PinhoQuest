package com.pinhoquest.ui.copy

import com.pinhoquest.core.session.SessionCommandResult

object UserFacingCopy {
    fun forSessionResult(result: SessionCommandResult<*>): String = when (result) {
        is SessionCommandResult.GenerationFailed ->
            "Não consegui montar essa quest agora. Tenta outra?"
        is SessionCommandResult.InvalidTransition ->
            "Essa quest já mudou de estado. Atualizei tudo por aqui."
        is SessionCommandResult.NotFound ->
            "Não encontrei essa quest por aqui. Que tal sortear outra?"
        is SessionCommandResult.Success<*> -> ""
    }
}
