package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.AdmissionDecision
import com.pinhoquest.core.inference.InferenceAdmissionController
import com.pinhoquest.core.inference.InferenceModelDescriptor
import com.pinhoquest.core.inference.InferenceResourceSnapshot
import com.pinhoquest.core.quest.ComposerPort
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.domain.quest.QuestDraft
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

    override suspend fun compose(plan: QuestGenerationPlan): QuestDraft {
        val model = modelDescriptorProvider()
            ?: return fallback.compose(plan)

        if (inferenceBusy.get()) {
            return fallback.compose(plan)
        }

        val snapshot = snapshotProvider()
        return when (admissionController.decide(snapshot, model)) {
            AdmissionDecision.Admit -> {
                if (!inferenceBusy.compareAndSet(false, true)) {
                    fallback.compose(plan)
                } else {
                    try {
                        local.compose(plan)
                    } finally {
                        inferenceBusy.set(false)
                    }
                }
            }

            is AdmissionDecision.UseFallback -> fallback.compose(plan)
        }
    }
}
