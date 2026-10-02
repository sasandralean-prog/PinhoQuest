package com.pinhoquest.model

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelDownloadTransportTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun downloadsCompletePackageToPartialPath() = runBlocking {
        val payload = "creative-brain-v1".repeat(64).toByteArray()
        var requestedOffset = -1L
        val transport = ModelDownloadTransport(connectionFactory = { _, offset ->
            requestedOffset = offset
            FakeHttpConnection(payload, offset, HttpURLConnection.HTTP_OK)
        })
        val partial = temporaryFolder.newFile("model.part")
        transport.download(spec(payload), partial)
        assertEquals(0L, requestedOffset)
        assertEquals(payload.toList(), partial.readBytes().toList())
    }

    @Test fun resumesExistingPartialDownloadUsingRange() = runBlocking {
        val payload = "0123456789".repeat(128).toByteArray()
        val prefix = payload.copyOf(payload.size / 3)
        var requestedOffset = -1L
        val transport = ModelDownloadTransport(connectionFactory = { _, offset ->
            requestedOffset = offset
            FakeHttpConnection(payload, offset, HttpURLConnection.HTTP_PARTIAL)
        })
        val partial = temporaryFolder.newFile("model.part")
        partial.writeBytes(prefix)
        transport.download(spec(payload), partial)
        assertEquals(prefix.size.toLong(), requestedOffset)
        assertEquals(payload.toList(), partial.readBytes().toList())
    }

    private fun spec(payload: ByteArray) = ModelDownloadSpec(
        modelId = "creative-model", version = "1", runtimeFormat = "litertlm",
        sha256 = "0".repeat(64), bytes = payload.size.toLong(), license = "test",
        source = "test", contextLimit = 1024, supportedBackends = listOf("CPU"),
        downloadUrl = "https://example.invalid/model",
    )

    private class FakeHttpConnection(private val payload: ByteArray, private val offset: Long, private val code: Int) : HttpURLConnection(URL("https://example.invalid/model")) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = code
        override fun getInputStream(): InputStream = ByteArrayInputStream(payload.copyOfRange(offset.toInt(), payload.size))
        override fun setRequestProperty(key: String?, value: String?) = Unit
        override fun getRequestProperty(key: String?): String? = null
        override fun getOutputStream(): java.io.OutputStream = throw UnsupportedOperationException()
        override fun getErrorStream(): InputStream? = null
        override fun getHeaderField(name: String?): String? = null
        override fun getContentLength(): Int = payload.size - offset.toInt()
    }
}