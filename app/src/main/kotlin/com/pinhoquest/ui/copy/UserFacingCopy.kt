package com.pinhoquest.ui.copy

import com.pinhoquest.core.inference.ModelInstallRejection
import com.pinhoquest.core.quest.GenerationUnavailableReason
import com.pinhoquest.core.quest.QuestGenerationResult
import com.pinhoquest.core.session.SessionCommandResult
import com.pinhoquest.domain.backup.BackupSnapshotBuildResult
import com.pinhoquest.domain.backup.RestorePlanResult

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

    fun forBackupSnapshotResult(result: BackupSnapshotBuildResult): String = when (result) {
        BackupSnapshotBuildResult.NoProfile ->
            "Ainda não há um jardim pronto para guardar neste aparelho."
        BackupSnapshotBuildResult.MultipleProfiles ->
            "Encontrei mais de um jardim neste aparelho. Vou manter tudo como está até isso ser resolvido."
        is BackupSnapshotBuildResult.InvalidState ->
            "Não consegui preparar a cópia do seu jardim agora. Seu progresso local continua salvo."
        is BackupSnapshotBuildResult.Ready ->
            "Cópia do seu jardim preparada com segurança."
    }

    fun forRestorePlanResult(result: RestorePlanResult): String = when (result) {
        is RestorePlanResult.Invalid ->
            "Essa cópia não parece íntegra ou compatível com esta versão. Seu jardim atual continua intacto."
        is RestorePlanResult.Ready ->
            "A cópia foi validada e está pronta para restaurar."
    }

    fun forSessionResult(result: SessionCommandResult<*>): String = when (result) {
        is SessionCommandResult.GenerationFailed -> {
            val generation = result.result
            when (generation) {
                is QuestGenerationResult.Unavailable -> when (generation.reason) {
                    GenerationUnavailableReason.GAME_CATALOG_NOT_FRESH ->
                        "Ainda não tenho um catálogo fresquinho de jogos para sortear. Vamos tentar mais tarde."
                    GenerationUnavailableReason.FILTERS_UNSATISFIABLE ->
                        "Esses filtros não combinam com o modo escolhido. Ajuste os filtros ou escolha outra modalidade."
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
