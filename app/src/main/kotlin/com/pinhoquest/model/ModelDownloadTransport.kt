package com.pinhoquest.model

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadTransport(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
    private val connectionFactory: (String, Long) -> HttpURLConnection = { url, offset -> openConnection(url, offset, connectTimeoutMs, readTimeoutMs) },
) {
    suspend fun download(spec: ModelDownloadSpec, partial: File) {
        var offset = if (partial.exists()) partial.length() else 0L
        if (offset > spec.bytes) {
            partial.delete()
            offset = 0L
        }
        var connection = connectionFactory(spec.downloadUrl, offset)
        try {
            if (offset > 0L && connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                partial.delete()
                offset = 0L
                connection = connectionFactory(spec.downloadUrl, 0L)
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_PARTIAL && code != HttpURLConnection.HTTP_OK) throw IOException("download HTTP status $code")
            if (offset > 0L && code != HttpURLConnection.HTTP_PARTIAL) throw IOException("server refused resume")
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(partial, offset > 0L).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        if (partial.length() != spec.bytes) throw IOException("download size does not match manifest")
    }

    companion object {
        private fun openConnection(urlString: String, offset: Long, connectTimeoutMs: Int, readTimeoutMs: Int): HttpURLConnection {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.instanceFollowRedirects = true
            if (offset > 0L) connection.setRequestProperty("Range", "bytes=$offset-")
            return connection
        }
    }
}