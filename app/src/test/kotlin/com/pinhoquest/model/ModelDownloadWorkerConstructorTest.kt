package com.pinhoquest.model

import android.content.Context
import androidx.work.WorkerParameters
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelDownloadWorkerConstructorTest {
    @Test
    fun exposesTheConstructorRequiredByWorkManagersDefaultWorkerFactory() {
        val constructor = ModelDownloadWorker::class.java.getDeclaredConstructor(
            Context::class.java,
            WorkerParameters::class.java,
        )

        assertNotNull(constructor)
    }
}
