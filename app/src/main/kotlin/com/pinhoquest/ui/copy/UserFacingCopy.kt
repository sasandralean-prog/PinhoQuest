package com.pinhoquest.ui.copy

import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.core.session.SessionCommandResult

object UserFacingCopy {
    fun forModelInstallRejection(reason: ModelInstallRejection): String = when (reason) {
        ModelInstallRejection.STAGED_FILE_MISSING,
        ModelInstallRejection.STAGED_FILE_IS_DIRECTORY,
        ModelInstallRejection.PARTIAL_FILE,
        ModelInstallRejection.SIZE_MISMATCH,
        ModelInstallRejection.HASH_MISMATCH,
        ModelInstallRejection.INVALID_DESTINATION,
        ModelInstallRejection.PROMOTION_FAILED,
        -> "Não consegui preparar o cérebro criativo agora. Você pode tentar de novo daqui a pouco."
    }

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
