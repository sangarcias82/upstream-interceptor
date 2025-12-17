package com.san.portfolio.upstream

import com.san.portfolio.upstream.client.HttpClientAdapter
import com.san.portfolio.upstream.client.HttpResponseDTO
import com.san.portfolio.upstream.client.ThirdPartyClient
import com.san.portfolio.upstream.interceptor.UpstreamBadGatewayException
import com.san.portfolio.upstream.interceptor.UpstreamCall
import com.san.portfolio.upstream.interceptor.UpstreamClient
import com.san.portfolio.upstream.interceptor.UpstreamErrorInterceptor
import com.san.portfolio.upstream.interceptor.UpstreamInterceptor
import com.san.portfolio.upstream.interceptor.UpstreamTimeoutException
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest

class UpstreamErrorInterceptorTest {
    private val server = MockWebServer()
    private val logger = LoggerFactory.getLogger("test")

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful upstream call passes through interceptor untouched`() = runTest {
        val http = object : HttpClientAdapter {
            override suspend fun get(path: String): HttpResponseDTO {
                return HttpResponseDTO(
                    status = 200,
                    body = """{"id":"123","name":"ok"}"""
                )
            }
        }

        val client = ThirdPartyClient(
            http = http,
            upstreamClient = UpstreamClient(
                interceptor = UpstreamErrorInterceptor(logger)
            ),
            logger = logger
        )

        val result = client.fetchResource("123")

        assertTrue(result.id == "123")
        assertTrue(result.name == "ok")
    }

    @Test
    fun `non-timeout upstream error is normalized to UpstreamBadGatewayException`() = runTest {
        val http = object : HttpClientAdapter {
            override suspend fun get(path: String): HttpResponseDTO {
                throw IllegalStateException("Upstream exploded")
            }
        }

        val client = ThirdPartyClient(
            http = http,
            upstreamClient = UpstreamClient(
                interceptor = UpstreamErrorInterceptor(logger)
            ),
            logger = logger
        )

        val result = runCatching { client.fetchResource("456") }

        val ex = result.exceptionOrNull()
        assertTrue(ex is UpstreamBadGatewayException)
        assertTrue(ex?.message!!.contains("third_party_api"))
        assertTrue(ex.message!!.contains("fetchResource"))
    }

    @Test
    fun `with interceptor the timeout is normalized to UpstreamTimeoutException`() = runTest {
        val http = object : HttpClientAdapter {
            override suspend fun get(path: String): HttpResponseDTO {
                throw SocketTimeoutException("Simulated timeout")
            }
        }

        val client = ThirdPartyClient(
            http = http,
            upstreamClient = UpstreamClient(
                interceptor = UpstreamErrorInterceptor(logger)
            ),
            logger = logger
        )

        val result = runCatching { client.fetchResource("123") }

        val ex = result.exceptionOrNull()
        assertTrue(ex is UpstreamTimeoutException)
        assertTrue(ex?.message!!.contains("third_party_api"))
        assertTrue(ex.message!!.contains("fetchResource"))
    }

    @Test
    fun `without interceptor the raw exception leaks to the service`(): Unit = runTest {
        val http = object : HttpClientAdapter {
            override suspend fun get(path: String): HttpResponseDTO {
                throw SocketTimeoutException("Simulated timeout from provider")
            }
        }

        val rawClient = ThirdPartyClient(
            http = http,
            upstreamClient = UpstreamClient( // passing a noop interceptor
                interceptor = object : UpstreamInterceptor {
                    override suspend fun <T> intercept(call: UpstreamCall<T>): T {
                        // no normalization, just call the block
                        return call.block()
                    }
                }
            ),
            logger = logger
        )

        val result = runCatching { rawClient.fetchResource("123") }

        assertTrue(result.exceptionOrNull() is SocketTimeoutException)
    }

    @Test
    fun `normalized exception preserves original cause`() = runTest {
        val original = SocketTimeoutException("Upstream timeout")

        val http = object : HttpClientAdapter {
            override suspend fun get(path: String): HttpResponseDTO {
                throw original
            }
        }

        val client = ThirdPartyClient(
            http = http,
            upstreamClient = UpstreamClient(
                interceptor = UpstreamErrorInterceptor(logger)
            ),
            logger = logger
        )

        val result = runCatching { client.fetchResource("999") }

        val ex = result.exceptionOrNull()
        assertTrue(ex is UpstreamTimeoutException)
        assertTrue(ex?.cause === original)
    }

    @Test
    fun `already normalized exception is not re-wrapped`() = runTest {
        val http = object : HttpClientAdapter {
            override suspend fun get(path: String): HttpResponseDTO {
                throw UpstreamTimeoutException("Already normalized")
            }
        }

        val client = ThirdPartyClient(
            http = http,
            upstreamClient = UpstreamClient(
                interceptor = UpstreamErrorInterceptor(logger)
            ),
            logger = logger
        )

        val result = runCatching { client.fetchResource("777") }

        val ex = result.exceptionOrNull()
        assertTrue(ex is UpstreamTimeoutException)
        assertTrue(ex?.message!!.contains("Already normalized"))
    }

}
