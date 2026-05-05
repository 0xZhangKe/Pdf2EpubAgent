package com.zhangke.agent.pdf2epub.core.llm.tool

import java.io.File

internal actual fun readTextFile(path: String): String {
    require(path.isNotBlank()) {
        "Please pass a file path."
    }

    val file = File(path).absoluteFile
    require(file.isFile) {
        "File does not exist: ${file.absolutePath}"
    }
    require(file.extension.lowercase() in textFileExtensions) {
        "read_text_file only supports text files. Do not read binary files or page images: ${file.absolutePath}"
    }

    return file.readText()
}

private val textFileExtensions = setOf(
    "txt",
    "json",
    "xml",
    "xhtml",
    "html",
    "htm",
    "css",
    "opf",
    "ncx",
    "md",
    "csv",
    "tsv",
)
