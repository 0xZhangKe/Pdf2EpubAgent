package com.zhangke.agent.pdf2epub.core.llm.tool

import com.zhangke.agent.pdf2epub.core.PDFProcessor

internal actual fun pdf2Images(path: String): List<String> {
    return PDFProcessor().pdf2Images(path)
}
