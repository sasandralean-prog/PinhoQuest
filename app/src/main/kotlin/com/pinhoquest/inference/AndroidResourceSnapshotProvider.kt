package com.pinhoquest.inference

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.pinhoquest.core.inference.AppWorkload
import com.pinhoquest.core.inference.InferenceResourceSnapshot
import com.pinhoquest.core.inference.LearnedInferenceProfile
import com.pinhoquest.core.inference.MemoryState
import com.pinhoquest.core.inference.RuntimeState
import com.pinhoquest.core.inference.ThermalState

class AndroidResourceSnapshotProvider(
    context: Context,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun snapshot(
        runtimeState: RuntimeState,
        appWorkload: AppWorkload,
        inferenceBusy: Boolean,
        learnedProfile: LearnedInferenceProfile = LearnedInferenceProfile(),
    ): InferenceResourceSnapshot {
        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        val memoryState = when {
            memory.lowMemory -> MemoryState.CRITICAL
            memory.availMem <= memory.threshold -> MemoryState.PRESSURED
            else -> MemoryState.HEALTHY
        }
        return InferenceResourceSnapshot(
            memoryState = memoryState,
            availableMemoryBytes = memory.availMem,
            thermalState = thermalState(),
            runtimeState = runtimeState,
            appWorkload = appWorkload,
            inferenceBusy = inferenceBusy,
            learnedProfile = learnedProfile,
            nowEpochMillis = nowEpochMillis(),
        )
    }

    private fun thermalState(): ThermalState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalState.UNKNOWN
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NOMINAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.NOMINAL
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.WARM
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN,
            -> ThermalState.CRITICAL
            else -> ThermalState.UNKNOWN
        }
    }
}