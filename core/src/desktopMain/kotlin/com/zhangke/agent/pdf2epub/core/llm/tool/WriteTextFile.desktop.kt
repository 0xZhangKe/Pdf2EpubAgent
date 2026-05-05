package com.zhangke.agent.pdf2epub.core.llm.tool

import java.io.File

internal actual fun writeTextFile(path: String, text: String): String {
    require(path.isNotBlank()) {
        "Please pass a target file path."
    }

    val targetFile = File(path).absoluteFile
    targetFile.parentFile?.mkdirs()
    require(targetFile.parentFile == null || targetFile.parentFile.isDirectory) {
        "Failed to create parent directory: ${targetFile.parentFile.absolutePath}"
    }

    targetFile.writeText(text)
    return targetFile.absolutePath
}
