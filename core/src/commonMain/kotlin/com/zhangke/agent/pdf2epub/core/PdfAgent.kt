package com.zhangke.agent.pdf2epub.core

import ai.koog.prompt.streaming.StreamFrame
import com.zhangke.agent.pdf2epub.core.llm.LlmClient
import com.zhangke.agent.pdf2epub.core.llm.prompt.pdfAgentPrompt
import com.zhangke.agent.pdf2epub.core.llm.tool.AppendTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.BuildEpubTool
import com.zhangke.agent.pdf2epub.core.llm.tool.PdfOcrTool
import com.zhangke.agent.pdf2epub.core.llm.tool.PdfToImagesTool
import com.zhangke.agent.pdf2epub.core.llm.tool.ReadTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.WriteTextFileTool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PdfAgent(
    pdfOcrTool: PdfOcrTool,
) {

    private val llmClient = LlmClient(
        systemPrompt = pdfAgentPrompt,
        additionalTools = listOf(
            AppendTextFileTool,
            BuildEpubTool,
            pdfOcrTool,
            PdfToImagesTool,
            ReadTextFileTool,
            WriteTextFileTool,
        ),
    )

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
            2. pdf_to_images 返回页面图片路径后，必须把返回内容视为按页排序的图片列表，并按列表顺序逐页处理。
            3. pdf_ocr 会把图片作为 LLM 多模态 image input 传给 OCR 子模型，并返回符合 System Prompt 页级 JSON Schema 的 JSON。
            4. 每处理一页时，必须先调用 pdf_ocr(image_path, page, page_image)，然后立刻调用 write_text_file(path, json) 写入该页 JSON；写入完成后才能处理下一页。
            5. write_text_file 的 path 必须是当前页面图片所在文件夹下的 output/page-XXXX.json，例如页面图片 /a/b/DemoPdf/page-1.png 的第 1 页输出为 /a/b/DemoPdf/output/page-0001.json。
            6. 禁止重复处理已经完成 pdf_ocr 且已经 write_text_file 的页面。
            7. 严禁使用 read_text_file 或其他文本读取工具读取 .png/.jpg/.jpeg/.webp/.bmp/.gif 等页面图片；图片文件不是文本输入。
            8. read_text_file 只能读取已经落盘的 .json/.xhtml/.html/.css/.opf/.txt 等文本文件。
            9. 必须按 page_analysis、continuous_reconstruction、book_reconstruction、epub_content_generation、epub_packaging 的顺序执行。
            10. 每个阶段的中间结果必须按 System Prompt 指定路径落盘。
            11. 最终必须调用 build_epub(input_dir="work/epub", output="output/book.epub")。
            12. 完成后只返回生成的 EPUB 文件路径；如果必须停止，返回停止原因。
        """.trimIndent()
    }

    private fun renderToolCall(frame: StreamFrame.ToolCallComplete): String {
        return "\n[tool] ${frame.name} ${frame.content}\n"
    }
}
