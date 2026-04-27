package com.zhangke.agent.pdf2epub.framework

import kotlinx.serialization.Serializable

@Serializable
data class Platform(
    val name: String,
)

expect fun currentPlatform(): Platform
