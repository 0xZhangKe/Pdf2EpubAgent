package com.zhangke.agent.pdf2epub.core.llm.tool

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import kotlinx.serialization.Serializable

internal expect fun buildEpub(inputDirectoryPath: String, outputPath: String?): String

object BuildEpubTool : SimpleTool<BuildEpubTool.Args>(
    argsType = typeToken<Args>(),
    name = "build_epub",
    description = "Package the contents of a directory into an EPUB file and return the absolute EPUB file path.",
) {

    @Serializable
    data class Args(
        @property:LLMDescription("Absolute or working-directory-relative path to the EPUB source directory, for example work/epub.")
        val input_dir: String,
        @property:LLMDescription("Absolute or working-directory-relative path to the EPUB file to create, for example output/book.epub. If omitted, the EPUB is created next to input_dir.")
        val output: String? = null,
    )

    override suspend fun execute(args: Args): String {
        return try {
            buildEpub(args.input_dir, args.output)
        } catch (e: IllegalArgumentException) {
            throw ToolException.ValidationFailure(e.message ?: "Invalid EPUB packaging arguments: input_dir=${args.input_dir}, output=${args.output}")
        }
    }
}
