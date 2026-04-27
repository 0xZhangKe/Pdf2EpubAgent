package com.zhangke.agent.pdf2epub.core

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreKoinModule = module {
    singleOf(::PdfToEpubService)
}
