package com.zhangke.agent.pdf2epub.core.llm.tool

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import kotlinx.serialization.Serializable

internal expect fun appendTextFile(path: String, text: String): String

object AppendTextFileTool : SimpleTool<AppendTextFileTool.Args>(
    argsType = typeToken<Args>(),
    name = "append_text_file",
    description = "Append the provided text content to the end of a file and return the absolute file path.",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Absolute or working-directory-relative path to the target file.")
        val path: String,
        @property:LLMDescription("Text content to append to the target file.")
        val text: String,
    )

    override suspend fun execute(args: Args): String {
        return try {
            appendTextFile(args.path, args.text)
        } catch (e: IllegalArgumentException) {
            throw ToolException.ValidationFailure(e.message ?: "Invalid file path: ${args.path}")
        }
    }
}
