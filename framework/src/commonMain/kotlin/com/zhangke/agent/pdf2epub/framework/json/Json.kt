package com.zhangke.agent.pdf2epub.framework.json

import kotlinx.serialization.json.Json

val globalJson: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}
