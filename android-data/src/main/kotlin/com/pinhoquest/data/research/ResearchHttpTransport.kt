package com.pinhoquest.data.research

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ResearchHttpTransport {
    suspend fun get(url: String): ResearchHttpResponse
}

data class ResearchHttpResponse(
    val statusCode: Int,
    val body: String,
)

class UrlConnectionResearchHttpTransport(
    private val connectTimeoutMillis: Int = 5_000,
    private val readTimeoutMillis: Int = 8_000,
) : ResearchHttpTransport {
    override suspend fun get(url: String): ResearchHttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMillis
            connection.readTimeout = readTimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "PinhoQuest/0.1 research")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            ResearchHttpResponse(
                statusCode = status,
                body = stream?.bufferedReader()?.use { it.readText() }.orEmpty(),
            )
        } finally {
            connection.disconnect()
        }
    }
}
