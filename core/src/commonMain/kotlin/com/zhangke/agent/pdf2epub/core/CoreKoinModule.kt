package com.zhangke.agent.pdf2epub.core

import com.zhangke.agent.pdf2epub.core.llm.tool.PdfOcrTool
import org.koin.dsl.module

val coreKoinModule = module {
    single {
        PdfOcrAgent()
    }
    single {
        PdfOcrTool(
            pdfOcrAgent = get(),
        )
    }
    single {
        PdfAgent(
            pdfOcrTool = get(),
        )
    }
}
