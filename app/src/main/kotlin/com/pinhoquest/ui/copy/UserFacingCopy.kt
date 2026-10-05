package com.pinhoquest.ui.copy

import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.core.quest.GenerationUnavailableReason
import com.pinhoquest.core.quest.QuestGenerationResult
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
        is SessionCommandResult.GenerationFailed -> {
            val generation = result.result
            when (generation) {
                is QuestGenerationResult.Unavailable -> when (generation.reason) {
                    GenerationUnavailableReason.GAME_CATALOG_NOT_FRESH ->
                        "Ainda não tenho um catálogo fresquinho de jogos para sortear. Vamos tentar mais tarde."
                    GenerationUnavailableReason.GAME_CATALOG_UNAVAILABLE,
                    GenerationUnavailableReason.GAME_CATALOG_NOT_CONFIGURED,
                    GenerationUnavailableReason.GAME_CANDIDATE_REQUIRED,
                    -> "Não consegui montar uma quest de jogo agora. Tenta outra?"
                }
                else -> "Não consegui montar essa quest agora. Tenta outra?"
            }
        }
        is SessionCommandResult.InvalidTransition ->
            "Essa quest já mudou de estado. Atualizei tudo por aqui."
        is SessionCommandResult.NotFound ->
            "Não encontrei essa quest por aqui. Que tal sortear outra?"
        is SessionCommandResult.Success<*> -> ""
    }
}
