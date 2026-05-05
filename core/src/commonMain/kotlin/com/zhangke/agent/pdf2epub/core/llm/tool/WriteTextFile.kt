package com.zhangke.agent.pdf2epub.core.llm.tool

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import kotlinx.serialization.Serializable

internal expect fun writeTextFile(path: String, text: String): String

object WriteTextFileTool : SimpleTool<WriteTextFileTool.Args>(
    argsType = typeToken<Args>(),
    name = "write_text_file",
    description = "Write the provided text content into a file and return the absolute file path.",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Absolute or working-directory-relative path to the target file.")
        val path: String,
        @property:LLMDescription("Text content to write into the target file. Existing file content will be overwritten.")
        val text: String? = null,
        @property:LLMDescription("JSON content to write into the target file. Use this when the prompt calls write_text_file(path, json).")
        val json: String? = null,
    )

    override suspend fun execute(args: Args): String {
        return try {
            val content = args.text ?: args.json
            require(content != null) {
                "Please pass either text or json content."
            }
            writeTextFile(args.path, content)
        } catch (e: IllegalArgumentException) {
            throw ToolException.ValidationFailure(e.message ?: "Invalid file path: ${args.path}")
        }
    }
}
