package com.zhangke.agent.pdf2epub.framework.http

import com.zhangke.agent.pdf2epub.framework.json.globalJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

val sharedHttpClient: HttpClient by lazy {
    createHttpClient(globalJson, createHttpClientEngine())
}

expect fun createHttpClientEngine(): HttpClientEngine

private fun createHttpClient(
    json: Json,
    engine: HttpClientEngine,
): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15.seconds.inWholeMilliseconds
            connectTimeoutMillis = 15.seconds.inWholeMilliseconds
            socketTimeoutMillis = 15.seconds.inWholeMilliseconds
        }
    }
}
