package com.pinhoquest.core.inference

import com.pinhoquest.domain.model.ModelManifest

enum class MemoryState { HEALTHY, PRESSURED, CRITICAL }
enum class ThermalState { NOMINAL, WARM, SEVERE, CRITICAL, UNKNOWN }
enum class RuntimeState { READY, UNAVAILABLE }
enum class AppWorkload { IDLE, ACTIVE, HEAVY }

data class LearnedInferenceProfile(
    val minimumFreeMemoryBytes: Long = 256L * 1024L * 1024L,
    val maximumObservedPeakBytes: Long = 0L,
    val consecutiveFailures: Int = 0,
    val cooldownUntilEpochMillis: Long = 0L,
)

data class InferenceResourceSnapshot(
    val memoryState: MemoryState,
    val availableMemoryBytes: Long,
    val thermalState: ThermalState,
    val runtimeState: RuntimeState,
    val appWorkload: AppWorkload,
    val inferenceBusy: Boolean,
    val learnedProfile: LearnedInferenceProfile = LearnedInferenceProfile(),
    val nowEpochMillis: Long = 0L,
)

sealed interface AdmissionDecision {
    data object Admit : AdmissionDecision
    data class UseFallback(val reason: InferenceFallbackReason) : AdmissionDecision
}

enum class InferenceFallbackReason {
    NO_MODEL,
    RUNTIME_UNAVAILABLE,
    MEMORY_PRESSURE,
    THERMAL_PRESSURE,
    INFERENCE_BUSY,
    APP_WORKLOAD_TOO_HIGH,
    LEARNED_COOLDOWN,
    INSUFFICIENT_MEMORY_HEADROOM,
}

data class InferenceModelDescriptor(
    val manifest: ModelManifest,
    val estimatedWorkingSetBytes: Long,
)