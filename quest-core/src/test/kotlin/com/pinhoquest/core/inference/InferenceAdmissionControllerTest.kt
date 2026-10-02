package com.pinhoquest.core.inference

import com.pinhoquest.domain.model.ModelManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InferenceAdmissionControllerTest {
    private val controller = InferenceAdmissionController()
    private val model = InferenceModelDescriptor(
        manifest = ModelManifest(
            modelId = "creative-model", version = "1", runtimeFormat = "litertlm",
            sha256 = "0".repeat(64), bytes = 100L, license = "test", source = "test",
            contextLimit = 1024, supportedBackends = setOf("CPU"),
        ),
        estimatedWorkingSetBytes = 256L * 1024L * 1024L,
    )

    @Test fun lowMemoryAlwaysFallsBackEvenWhenAppWorkloadIsIdle() {
        val decision = controller.decide(snapshot(memoryState = MemoryState.CRITICAL, available = 2L * 1024L * 1024L), model)
        assertEquals(InferenceFallbackReason.MEMORY_PRESSURE, (decision as AdmissionDecision.UseFallback).reason)
    }

    @Test fun severeThermalStateFallsBackBeforeInference() {
        val decision = controller.decide(snapshot(thermal = ThermalState.SEVERE), model)
        assertEquals(InferenceFallbackReason.THERMAL_PRESSURE, (decision as AdmissionDecision.UseFallback).reason)
    }

    @Test fun missingModelFallsBackWithoutLoadingRuntime() {
        val decision = controller.decide(snapshot(), null)
        assertEquals(InferenceFallbackReason.NO_MODEL, (decision as AdmissionDecision.UseFallback).reason)
    }

    @Test fun unavailableRuntimeFallsBack() {
        val decision = controller.decide(snapshot(runtime = RuntimeState.UNAVAILABLE), model)
        assertEquals(InferenceFallbackReason.RUNTIME_UNAVAILABLE, (decision as AdmissionDecision.UseFallback).reason)
    }

    @Test fun concurrentInferenceFallsBack() {
        val decision = controller.decide(snapshot(busy = true), model)
        assertEquals(InferenceFallbackReason.INFERENCE_BUSY, (decision as AdmissionDecision.UseFallback).reason)
    }

    @Test fun healthyHeadroomIsAdmitted() {
        val decision = controller.decide(snapshot(available = 1024L * 1024L * 1024L), model)
        assertTrue(decision is AdmissionDecision.Admit)
    }

    @Test fun heavyAppWorkloadFallsBackWithoutCpuPercentageShortcut() {
        val decision = controller.decide(snapshot(workload = AppWorkload.HEAVY, available = 1024L * 1024L * 1024L), model)
        assertEquals(InferenceFallbackReason.APP_WORKLOAD_TOO_HIGH, (decision as AdmissionDecision.UseFallback).reason)
    }

    private fun snapshot(
        memoryState: MemoryState = MemoryState.HEALTHY,
        available: Long = 1024L * 1024L * 1024L,
        thermal: ThermalState = ThermalState.NOMINAL,
        runtime: RuntimeState = RuntimeState.READY,
        workload: AppWorkload = AppWorkload.IDLE,
        busy: Boolean = false,
    ) = InferenceResourceSnapshot(
        memoryState = memoryState, availableMemoryBytes = available, thermalState = thermal,
        runtimeState = runtime, appWorkload = workload, inferenceBusy = busy,
    )
}