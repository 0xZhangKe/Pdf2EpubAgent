package com.zhangke.agent.pdf2epub.core

import kotlinx.coroutines.runBlocking
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import kotlin.system.exitProcess

fun main() {
    val koin = startKoin {
        modules(
            coreKoinModule
        )
    }
    val exitCode = startAgent(koin)
//    val exitCode = testSinglePage(koin)
    exitProcess(exitCode)
}

private fun startAgent(koinApp: KoinApplication): Int {
    var exitCode = 0
    try {
        val pdfAgent = koinApp.koin.get<PdfAgent>()
        runBlocking {
            val demoFilePath = "/Users/zhangke/Downloads/DemoPdf.pdf"
            pdfAgent.process(demoFilePath)
                .collect { println(it) }
        }
    } catch (throwable: Throwable) {
        exitCode = 1
        throwable.printStackTrace()
    } finally {
        koinApp.close()
    }
    return exitCode
}

private fun testSinglePage(koinApp: KoinApplication): Int {
    val prompt = """
            请对随消息传入的页面图片进行 OCR 和版面结构分析。

            页面信息：
            page: 54
            pageImage: page-54.png
            imagePath: /Users/zhangke/Personal/code/PdfToEpubAgent/core/files/DemoPdf/page-54.png

            必须严格按 System Prompt 中的 JSON Schema 输出单个 JSON object。
            不要输出 Markdown、解释、代码块或额外文本。
        """.trimIndent()
    var exitCode = 0
    try {
        runBlocking {
            val ocrAgent = koinApp.koin.get<PdfOcrAgent>()
            val output = ocrAgent.analyzePage(prompt, "/Users/zhangke/Personal/code/PdfToEpubAgent/core/files/DemoPdf/page-54.png")
            println("OCR Output:\n$output")
        }
    } catch (e: Throwable) {
        exitCode = 1
        e.printStackTrace()
    } finally {
        koinApp.close()
    }
    return exitCode
}
