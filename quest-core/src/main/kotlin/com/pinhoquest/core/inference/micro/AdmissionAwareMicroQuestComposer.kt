package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.AdmissionDecision
import com.pinhoquest.core.inference.InferenceAdmissionController
import com.pinhoquest.core.inference.InferenceModelDescriptor
import com.pinhoquest.core.inference.InferenceResourceSnapshot
import com.pinhoquest.core.quest.ComposerPort
import com.pinhoquest.core.quest.QuestCompositionOrigin
import com.pinhoquest.core.quest.QuestCompositionOutcome
import com.pinhoquest.core.quest.QuestFallbackReason
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestGenerationPlan
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Admission is evaluated before the local composer is invoked, so a rejected
 * request never loads or enters the local runtime. Both admitted and fallback
 * paths remain ordinary ComposerPort outputs and converge through QuestValidator.
 */
class AdmissionAwareMicroQuestComposer(
    private val local: ComposerPort,
    private val snapshotProvider: () -> InferenceResourceSnapshot,
    private val modelDescriptorProvider: () -> InferenceModelDescriptor?,
    private val admissionController: InferenceAdmissionController = InferenceAdmissionController(),
    private val fallback: ComposerPort = ProceduralComposer(),
) : ComposerPort {
    private val inferenceBusy = AtomicBoolean(false)

    override suspend fun compose(plan: QuestGenerationPlan) =
        composeWithOutcome(plan).draft

    override suspend fun composeWithOutcome(plan: QuestGenerationPlan): QuestCompositionOutcome {
        val model = modelDescriptorProvider()
            ?: return fallbackOutcome(plan, QuestFallbackReason.MODEL_NOT_INSTALLED)

        if (inferenceBusy.get()) {
            return fallbackOutcome(plan, QuestFallbackReason.INFERENCE_BUSY)
        }

        val snapshot = snapshotProvider()
        return when (admissionController.decide(snapshot, model)) {
            AdmissionDecision.Admit -> {
                if (!inferenceBusy.compareAndSet(false, true)) {
                    fallbackOutcome(plan, QuestFallbackReason.INFERENCE_BUSY)
                } else {
                    try {
                        local.composeWithOutcome(plan)
                    } finally {
                        inferenceBusy.set(false)
                    }
                }
            }

            is AdmissionDecision.UseFallback ->
                fallbackOutcome(plan, QuestFallbackReason.ADMISSION_DENIED)
        }
    }

    private suspend fun fallbackOutcome(
        plan: QuestGenerationPlan,
        reason: QuestFallbackReason,
    ): QuestCompositionOutcome =
        fallback.composeWithOutcome(plan).copy(
            origin = QuestCompositionOrigin.PROCEDURAL_FALLBACK,
            fallbackReason = reason,
        )
}
