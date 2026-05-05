package com.zhangke.agent.pdf2epub.core.llm.tool

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import kotlinx.serialization.Serializable

internal expect fun readTextFile(path: String): String

object ReadTextFileTool : SimpleTool<ReadTextFileTool.Args>(
    argsType = typeToken<Args>(),
    name = "read_text_file",
    description = "Read UTF-8 text content from a small text file. Do not use this tool for PDF page images or other binary files.",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Absolute or working-directory-relative path to a text file. Never pass image files such as .png, .jpg, .jpeg, .webp, .bmp, or .gif.")
        val path: String,
    )

    override suspend fun execute(args: Args): String {
        return try {
            readTextFile(args.path)
        } catch (e: IllegalArgumentException) {
            throw ToolException.ValidationFailure(e.message ?: "Invalid file path: ${args.path}")
        }
    }
}
