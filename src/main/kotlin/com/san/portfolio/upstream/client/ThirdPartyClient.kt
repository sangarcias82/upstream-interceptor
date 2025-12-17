package com.san.portfolio.upstream.client

import com.san.portfolio.upstream.interceptor.UpstreamBadGatewayException
import com.san.portfolio.upstream.interceptor.UpstreamClient
import org.slf4j.Logger

/**
 * Generic third-party API client used to demonstrate the interceptor pattern.
 *
 * This class focuses on the domain-level behavior: invoking an external provider
 * and delegating error normalization to the UpstreamClient and its interceptors.
 *
 * No transport or framework concerns are included here.
 */
class ThirdPartyClient(
    private val http: HttpClientAdapter,
    private val upstreamClient: UpstreamClient,
    private val logger: Logger
) {

    suspend fun fetchResource(resourceId: String): ExternalResourceDTO {
        return upstreamClient.call(
            upstream = "third_party_api",
            operation = "fetchResource",
            context = "resourceId=$resourceId"
        ) {
            val response = http.get("/resources/$resourceId")

            if (response.status in 200..299) {
                parseResource(response.body)
            } else {
                logger.error(
                    "third_party_api responded with non-2xx status ${response.status} for resourceId=$resourceId"
                )
                throw UpstreamBadGatewayException(
                    "Unexpected upstream status: ${response.status}"
                )
            }
        }
    }

    private fun parseResource(json: String?): ExternalResourceDTO {
        if (json.isNullOrBlank()) {
            return ExternalResourceDTO(id = "unknown", name = "empty")
        }

        // Minimal parsing to keep the example technology-neutral
        val content = json.replace("{", "")
            .replace("}", "")
            .replace("\"", "")
            .split(",")
            .map { it.trim() }

        val map = content.mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()

        return ExternalResourceDTO(
            id = map["id"] ?: "n/a",
            name = map["name"] ?: "n/a"
        )
    }
}

data class ExternalResourceDTO(
    val id: String,
    val name: String
)

/**
 * A very small abstraction to keep the example decoupled from any HTTP library.
 */
interface HttpClientAdapter {
    suspend fun get(path: String): HttpResponseDTO
}

data class HttpResponseDTO(
    val status: Int,
    val body: String?
)
