package com.zhangke.agent.pdf2epub.core

import com.zhangke.agent.pdf2epub.core.llm.LlmClient
import com.zhangke.agent.pdf2epub.core.llm.prompt.pdfAgentPrompt
import org.koin.dsl.module

val coreKoinModule = module {
    single {
        PdfAgent(
            llmClient = get(),
        )
    }
    factory {
        LlmClient(systemPrompt = pdfAgentPrompt)
    }
}
