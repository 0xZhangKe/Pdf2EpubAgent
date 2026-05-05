package com.zhangke.agent.pdf2epub.core.llm

internal actual fun getEnvironmentVariable(name: String): String? = System.getenv(name)
