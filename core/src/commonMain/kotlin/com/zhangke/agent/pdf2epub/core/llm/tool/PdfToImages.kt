package com.zhangke.agent.pdf2epub.core.llm.tool

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import kotlinx.serialization.Serializable

internal expect fun pdf2Images(path: String): List<String>

object PdfToImagesTool : SimpleTool<PdfToImagesTool.Args>(
    argsType = typeToken<Args>(),
    name = "pdf_to_images",
    description = "Convert a PDF file into PNG images and return the absolute image paths in page order.",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Absolute or working-directory-relative path to the PDF file.")
        val path: String,
    )

    override suspend fun execute(args: Args): String {
        return try {
            pdf2Images(args.path).joinToString(separator = "\n")
        } catch (e: IllegalArgumentException) {
            throw ToolException.ValidationFailure(e.message ?: "Invalid PDF file path: ${args.path}")
        }
    }
}
