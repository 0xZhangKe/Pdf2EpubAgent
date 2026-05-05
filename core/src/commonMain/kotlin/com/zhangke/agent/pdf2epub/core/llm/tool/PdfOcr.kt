package com.zhangke.agent.pdf2epub.core.llm.tool

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import com.zhangke.agent.pdf2epub.core.llm.LlmClient
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class PdfOcrTool(
    private val llmClient: LlmClient,
) : SimpleTool<PdfOcrTool.Args>(
    argsType = typeToken<Args>(),
    name = "pdf_ocr",
    description = "Run OCR and layout analysis for one PDF page image. The image is passed to the LLM as multimodal image input and the tool returns a valid page-analysis JSON object.",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Absolute path to one PDF page image, such as /path/to/page-1.png. This file is passed as image input and must not be read as text.")
        val image_path: String,
        @property:LLMDescription("1-based PDF page number represented by this image.")
        val page: Int,
        @property:LLMDescription("Image file name to write into the pageImage field, such as page-0001.png or page-1.png.")
        val page_image: String,
    )

    override suspend fun execute(args: Args): String {
        return try {
            validate(args)
            val response = llmClient.send(
                text = buildOcrPrompt(args),
                imagePaths = listOf(args.image_path),
            )
            normalizeJson(response)
        } catch (e: IllegalArgumentException) {
            throw ToolException.ValidationFailure(e.message ?: "Invalid pdf_ocr arguments.")
        } catch (e: SerializationException) {
            throw ToolException.ValidationFailure("pdf_ocr response is not valid JSON: ${e.message}")
        }
    }

    private fun validate(args: Args) {
        require(args.image_path.isNotBlank()) {
            "Please pass an image path."
        }
        require(args.page > 0) {
            "Page number must be greater than 0."
        }
        require(args.page_image.isNotBlank()) {
            "Please pass page_image."
        }
        val extension = args.image_path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        require(extension in imageFileExtensions) {
            "pdf_ocr only accepts page image files: ${args.image_path}"
        }
    }

    private fun buildOcrPrompt(args: Args): String {
        return """
            请对随消息传入的页面图片进行 OCR 和版面结构分析。

            页面信息：
            page: ${args.page}
            pageImage: ${args.page_image}
            imagePath: ${args.image_path}

            必须严格按 System Prompt 中的 JSON Schema 输出单个 JSON object。
            不要输出 Markdown、解释、代码块或额外文本。
        """.trimIndent()
    }

    private fun normalizeJson(response: String): String {
        val json = response.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val jsonObject: JsonObject = Json.parseToJsonElement(json).jsonObject
        return jsonObject.toString()
    }

    private companion object {
        private val imageFileExtensions = setOf("png", "jpg", "jpeg", "webp", "bmp", "gif")
    }
}
