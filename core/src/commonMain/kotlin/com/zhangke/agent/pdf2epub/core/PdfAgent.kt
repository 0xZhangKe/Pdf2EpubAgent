package com.zhangke.agent.pdf2epub.core

import ai.koog.prompt.streaming.StreamFrame
import com.zhangke.agent.pdf2epub.core.llm.LlmClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PdfAgent(
    private val llmClient: LlmClient,
) {

    fun process(path: String): Flow<String> {
        return flow {
            require(path.isNotBlank()) {
                "Please pass a PDF file path."
            }

            llmClient.sendStreaming(buildProcessPrompt(path)).collect { frame ->
                when (frame) {
                    is StreamFrame.TextDelta -> emit(frame.text)
                    is StreamFrame.ToolCallComplete -> emit(renderToolCall(frame))
                    else -> Unit
                }
            }
        }
    }

    private fun buildProcessPrompt(path: String): String {
        return """
            请严格按照 System Prompt 中定义的完整流程处理下面的 PDF：

            PDF 文件路径：
            $path

            执行要求：
            1. 必须先调用 pdf_to_images(path="$path")。
            2. pdf_to_images 返回页面图片路径后，针对每个页面必须调用 pdf_ocr(image_path, page, page_image) 执行 OCR/page_analysis。
            3. pdf_ocr 会把图片作为 LLM 多模态 image input 传给 OCR 子模型，并返回符合 System Prompt 页级 JSON Schema 的 JSON。
            4. 必须将 pdf_ocr 返回的 JSON 用 write_text_file 写入 work/pages/page-XXXX.json。
            5. 严禁使用 read_text_file 或其他文本读取工具读取 .png/.jpg/.jpeg/.webp/.bmp/.gif 等页面图片；图片文件不是文本输入。
            6. read_text_file 只能读取已经落盘的 .json/.xhtml/.html/.css/.opf/.txt 等文本文件。
            7. 必须按 page_analysis、continuous_reconstruction、book_reconstruction、epub_content_generation、epub_packaging 的顺序执行。
            8. 每个阶段的中间结果必须按 System Prompt 指定路径落盘。
            9. 最终必须调用 build_epub(input_dir="work/epub", output="output/book.epub")。
            10. 完成后只返回生成的 EPUB 文件路径；如果必须停止，返回停止原因。
        """.trimIndent()
    }

    private fun renderToolCall(frame: StreamFrame.ToolCallComplete): String {
        return "\n[tool] ${frame.name} ${frame.content}\n"
    }
}
