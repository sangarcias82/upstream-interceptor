package com.san.portfolio.upstream.interceptor

interface UpstreamInterceptor {
    suspend fun <T> intercept(call: UpstreamCall<T>): T
}
