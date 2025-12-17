package com.san.portfolio.upstream.interceptor

import org.slf4j.Logger
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class UpstreamErrorInterceptor(
    private val logger: Logger
) : UpstreamInterceptor {

    override suspend fun <T> intercept(call: UpstreamCall<T>): T {
        try {
            return call.block()
        } catch (t: TimeoutException) {
            val msg = "[${call.upstream}_timeout] <${call.operation}> Timeout :: ${call.context}"
            logger.error(msg, t)
            throw UpstreamTimeoutException(msg)
        } catch (t: SocketTimeoutException) {
            val msg = "[${call.upstream}_timeout] <${call.operation}> SocketTimeout :: ${call.context}"
            logger.error(msg, t)
            throw UpstreamTimeoutException(msg)
        } catch (e: Exception) {
            val payload = mapOf(
                "upstream" to call.upstream,
                "operation" to call.operation,
                "context" to call.context,
                "exception" to e.javaClass.simpleName
            )
            logger.error("[${call.upstream}_error] <${call.operation}> Failure payload={}", payload, e)
            throw UpstreamBadGatewayException("Upstream failure for ${call.upstream} in ${call.operation}")
        }
    }
}
