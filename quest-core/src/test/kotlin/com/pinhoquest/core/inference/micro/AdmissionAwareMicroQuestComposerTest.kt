package com.pinhoquest.core.inference.micro

import com.pinhoquest.core.inference.AppWorkload
import com.pinhoquest.core.inference.InferenceModelDescriptor
import com.pinhoquest.core.inference.InferenceResourceSnapshot
import com.pinhoquest.core.inference.LearnedInferenceProfile
import com.pinhoquest.core.inference.MemoryState
import com.pinhoquest.core.inference.RuntimeState
import com.pinhoquest.core.inference.ThermalState
import com.pinhoquest.core.quest.ComposerPort
import com.pinhoquest.core.quest.ProceduralComposer
import com.pinhoquest.core.quest.QuestContext
import com.pinhoquest.core.quest.QuestGenerationPlan
import com.pinhoquest.core.quest.QuestPlanner
import com.pinhoquest.domain.model.ModelManifest
import com.pinhoquest.domain.quest.QuestMode
import com.pinhoquest.domain.quest.QuestRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AdmissionAwareMicroQuestComposerTest {
    private val plan: QuestGenerationPlan = QuestPlanner().plan(
        QuestRequest(QuestMode.NORMAL),
        QuestContext(),
    )

    private val model = InferenceModelDescriptor(
        manifest = ModelManifest(
            modelId = "cr74-functiongemma",
            version = "cr-7.4",
            runtimeFormat = "litertlm",
            sha256 = "e815c8ddb5400d777e2a0653a057692b25f6b7e0a9d9197992dc423ec9d67dfb",
            bytes = 284_692_656L,
            license = "local-validation",
            source = "canonical-cr74",
            contextLimit = 1280,
            supportedBackends = setOf("CPU"),
        ),
        estimatedWorkingSetBytes = 512L * 1024L * 1024L,
    )

    @Test
    fun admittedInferenceIsAttempted() = runTest {
        var localCalls = 0
        val local = ComposerPort {
            localCalls++
            ProceduralComposer().compose(it)
        }
        val composer = AdmissionAwareMicroQuestComposer(
            local = local,
            snapshotProvider = { healthySnapshot() },
            modelDescriptorProvider = { model },
        )

        composer.compose(plan)

        assertEquals(1, localCalls)
    }

    @Test
    fun nonAdmittedInferenceIsNeverInvoked() = runTest {
        var localCalls = 0
        val local = ComposerPort {
            localCalls++
            error("local inference must not be invoked")
        }
        val composer = AdmissionAwareMicroQuestComposer(
            local = local,
            snapshotProvider = { pressuredSnapshot() },
            modelDescriptorProvider = { model },
        )

        val draft = composer.compose(plan)

        assertEquals(0, localCalls)
        assertEquals(ProceduralComposer().compose(plan), draft)
    }

    @Test
    fun missingModelUsesFallbackWithoutAttemptingInference() = runTest {
        var localCalls = 0
        val local = ComposerPort {
            localCalls++
            error("local inference must not be invoked")
        }
        val composer = AdmissionAwareMicroQuestComposer(
            local = local,
            snapshotProvider = { healthySnapshot() },
            modelDescriptorProvider = { null },
        )

        composer.compose(plan)

        assertEquals(0, localCalls)
    }

    @Test
    fun busyLocalInferenceMakesSecondRequestUseFallback() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var localCalls = 0
        val local = ComposerPort {
            localCalls++
            started.complete(Unit)
            release.await()
            ProceduralComposer().compose(it)
        }
        val composer = AdmissionAwareMicroQuestComposer(
            local = local,
            snapshotProvider = { healthySnapshot() },
            modelDescriptorProvider = { model },
        )

        val first = async { composer.compose(plan) }
        started.await()
        val second = composer.compose(plan)
        release.complete(Unit)
        first.await()

        assertEquals(1, localCalls)
        assertEquals(ProceduralComposer().compose(plan), second)
    }

    private fun healthySnapshot() = InferenceResourceSnapshot(
        memoryState = MemoryState.HEALTHY,
        availableMemoryBytes = 2L * 1024L * 1024L * 1024L,
        thermalState = ThermalState.NOMINAL,
        runtimeState = RuntimeState.READY,
        appWorkload = AppWorkload.IDLE,
        inferenceBusy = false,
        learnedProfile = LearnedInferenceProfile(minimumFreeMemoryBytes = 256L * 1024L * 1024L),
        nowEpochMillis = 1L,
    )

    private fun pressuredSnapshot() = healthySnapshot().copy(
        memoryState = MemoryState.PRESSURED,
    )
}
