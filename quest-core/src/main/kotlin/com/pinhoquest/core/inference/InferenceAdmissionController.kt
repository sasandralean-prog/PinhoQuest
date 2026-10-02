package com.pinhoquest.core.inference

class InferenceAdmissionController(
    private val minimumHeadroomBytes: Long = 192L * 1024L * 1024L,
) {
    fun decide(
        snapshot: InferenceResourceSnapshot,
        model: InferenceModelDescriptor?,
    ): AdmissionDecision {
        if (model == null) return AdmissionDecision.UseFallback(InferenceFallbackReason.NO_MODEL)
        if (snapshot.runtimeState != RuntimeState.READY) {
            return AdmissionDecision.UseFallback(InferenceFallbackReason.RUNTIME_UNAVAILABLE)
        }
        if (snapshot.inferenceBusy) {
            return AdmissionDecision.UseFallback(InferenceFallbackReason.INFERENCE_BUSY)
        }
        if (snapshot.thermalState == ThermalState.SEVERE || snapshot.thermalState == ThermalState.CRITICAL) {
            return AdmissionDecision.UseFallback(InferenceFallbackReason.THERMAL_PRESSURE)
        }
        if (snapshot.memoryState == MemoryState.CRITICAL || snapshot.memoryState == MemoryState.PRESSURED) {
            return AdmissionDecision.UseFallback(InferenceFallbackReason.MEMORY_PRESSURE)
        }
        if (snapshot.appWorkload == AppWorkload.HEAVY) {
            return AdmissionDecision.UseFallback(InferenceFallbackReason.APP_WORKLOAD_TOO_HIGH)
        }
        if (snapshot.nowEpochMillis < snapshot.learnedProfile.cooldownUntilEpochMillis) {
            return AdmissionDecision.UseFallback(InferenceFallbackReason.LEARNED_COOLDOWN)
        }

        val learnedHeadroom = snapshot.learnedProfile.minimumFreeMemoryBytes
        val requiredHeadroom = maxOf(minimumHeadroomBytes, learnedHeadroom, model.estimatedWorkingSetBytes)
        if (snapshot.availableMemoryBytes < requiredHeadroom) {
            return AdmissionDecision.UseFallback(InferenceFallbackReason.INSUFFICIENT_MEMORY_HEADROOM)
        }
        return AdmissionDecision.Admit
    }
}