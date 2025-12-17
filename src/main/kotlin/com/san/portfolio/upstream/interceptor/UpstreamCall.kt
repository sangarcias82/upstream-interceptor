package com.san.portfolio.upstream.interceptor

data class UpstreamCall<T>(
    val upstream: String,
    val operation: String,
    val context: String,
    val block: suspend () -> T
)
