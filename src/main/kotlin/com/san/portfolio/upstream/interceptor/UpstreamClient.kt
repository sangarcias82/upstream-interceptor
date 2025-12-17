package com.san.portfolio.upstream.interceptor

class UpstreamClient(
    private val interceptor: UpstreamInterceptor
) {
    suspend fun <T> call(
        upstream: String,
        operation: String,
        context: String,
        block: suspend () -> T
    ): T {
        return interceptor.intercept(UpstreamCall(upstream, operation, context, block))
    }
}
