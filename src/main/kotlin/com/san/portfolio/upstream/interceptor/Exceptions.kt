package com.san.portfolio.upstream.interceptor

class UpstreamBadGatewayException(message: String) : RuntimeException(message)
class UpstreamTimeoutException(message: String) : RuntimeException(message)
