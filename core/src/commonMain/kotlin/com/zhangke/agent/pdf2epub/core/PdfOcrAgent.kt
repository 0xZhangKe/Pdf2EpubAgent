package com.zhangke.agent.pdf2epub.core

import com.zhangke.agent.pdf2epub.core.llm.SimpleLlmClient
import com.zhangke.agent.pdf2epub.core.llm.prompt.pdfOcrPrompt

class PdfOcrAgent {

    private val llmClient = SimpleLlmClient(systemPrompt = pdfOcrPrompt)

    suspend fun analyzePage(
        prompt: String,
        imagePath: String,
    ): String {
        return llmClient.send(
            text = prompt,
            imagePaths = listOf(imagePath),
        )
    }
}
