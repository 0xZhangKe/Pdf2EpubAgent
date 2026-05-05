package com.zhangke.agent.pdf2epub.core

import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

fun main() {
    val koin = startKoin {
        modules(
            coreKoinModule
        )
    }
    var exitCode = 0
    try {
        val pdfAgent = koin.koin.get<PdfAgent>()
        runBlocking {
            val demoFilePath = "/Users/zhangke/Downloads/DemoPdf.pdf"
            pdfAgent.process(demoFilePath)
                .collect { println(it) }
        }
    } catch (throwable: Throwable) {
        exitCode = 1
        throwable.printStackTrace()
    } finally {
        koin.close()
    }
    exitProcess(exitCode)
}
