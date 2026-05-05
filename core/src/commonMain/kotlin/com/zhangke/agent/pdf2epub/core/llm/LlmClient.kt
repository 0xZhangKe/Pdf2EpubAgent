package com.zhangke.agent.pdf2epub.core.llm

import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.environment.GenericAgentEnvironment
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponses
import com.zhangke.agent.pdf2epub.core.llm.model.buildKimiAgent
import com.zhangke.agent.pdf2epub.core.llm.prompt.pdfOcrPrompt
import com.zhangke.agent.pdf2epub.core.llm.tool.PdfOcrTool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.files.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class LlmClient(
    private val systemPrompt: String,
    private val apiKey: String = getEnvironmentVariable("KIMI_API_KEY").orEmpty(),
    private val toolsEnabled: Boolean = true,
) {

    private val pdfOcrClient: LlmClient by lazy {
        LlmClient(
            systemPrompt = pdfOcrPrompt,
            apiKey = apiKey,
            toolsEnabled = false,
        )
    }

    suspend fun send(
        text: String,
        imagePaths: List<String> = emptyList(),
    ): String {
        return executeConversation(
            text = text,
            imagePaths = imagePaths,
        )
    }

    fun sendStreaming(
        text: String,
        imagePaths: List<String> = emptyList(),
    ): Flow<StreamFrame> = flow {
        executeConversation(
            text = text,
            imagePaths = imagePaths,
            onFrame = ::emit,
        )
    }

    fun sendStreamingWithResult(
        text: String,
        imagePaths: List<String> = emptyList(),
    ): Flow<LlmStreamingResult> = flow {
        val result = executeConversation(
            text = text,
            imagePaths = imagePaths,
            onFrame = { frame -> emit(LlmStreamingResult.Stream(frame)) },
        )
        emit(LlmStreamingResult.Completed(result))
    }

    private suspend fun executeConversation(
        text: String,
        imagePaths: List<String> = emptyList(),
        onFrame: suspend (StreamFrame) -> Unit = {},
    ): String {
        require(text.isNotBlank()) {
            "Message text is blank."
        }

        val agent = buildAiAgent()
        try {
            val config = agent.agentConfig
            val promptExecutor = agent.promptExecutor
            val toolDescriptors = agent.toolRegistry.tools.map { it.descriptor }
            val environment = GenericAgentEnvironment(
                agentId = agent.id,
                logger = logger,
                toolRegistry = agent.toolRegistry,
                serializer = config.serializer,
            )

            var currentPrompt =
                prompt(config.prompt.withParams(config.prompt.params.withKimiThinkingDisabled())) {
                    user {
                        text(text)
                        imagePaths.forEach { image(Path(it)) }
                    }
                }

            repeat(config.maxAgentIterations) {
                val frames = mutableListOf<StreamFrame>()
                promptExecutor.executeStreaming(
                    prompt = currentPrompt,
                    model = config.model,
                    tools = toolDescriptors,
                ).collect { frame ->
                    frames += frame
                    onFrame(frame)
                }

                val responses = frames.toMessageResponses().withoutEmptyAssistantMessages()
                currentPrompt = prompt(currentPrompt) {
                    messages(responses)
                }

                val toolCalls = responses.filterIsInstance<Message.Tool.Call>()
                if (toolCalls.isEmpty()) {
                    val assistantMessage = responses
                        .filterIsInstance<Message.Assistant>()
                        .joinToString(separator = "\n") { it.content }
                    if (assistantMessage.isFinalAgentMessage()) {
                        return assistantMessage
                    }
                    currentPrompt = prompt(currentPrompt) {
                        user(buildContinuationPrompt(assistantMessage))
                    }
                    return@repeat
                }

                val toolResults = environment.executeTools(toolCalls)
                currentPrompt = prompt(currentPrompt) {
                    messages(toolResults.map { it.toMessage(agent.clock) })
                }
            }

            error("LlmClient exceeded max iterations: ${config.maxAgentIterations}")
        } finally {
            agent.promptExecutor.close()
        }
    }

    private fun buildAiAgent(): GraphAIAgent<String, String> {
        return buildKimiAgent(
            systemPrompt = systemPrompt,
            apiKey = apiKey,
            toolsEnabled = toolsEnabled,
            maxIterations = if (toolsEnabled) 80 else 3,
            additionalTools = if (toolsEnabled) {
                listOf(PdfOcrTool(pdfOcrClient))
            } else {
                emptyList()
            },
        )
    }

    private fun List<Message.Response>.withoutEmptyAssistantMessages(): List<Message.Response> {
        return filterNot { response ->
            response is Message.Assistant && response.content.isBlank()
        }
    }

    private fun String.isFinalAgentMessage(): Boolean {
        val text = trim()
        if (text.isBlank()) return false
        return text.contains(".epub") ||
            text.contains("output/book.epub") ||
            text.contains("停止原因") ||
            text.contains("必须停止") ||
            text.contains("无法继续")
    }

    private fun buildContinuationPrompt(previousMessage: String): String {
        return """
            你还没有完成 System Prompt 要求的完整 PDF 转 EPUB 工作流。

            上一条回复：
            $previousMessage

            请不要结束任务。继续从当前进度执行后续步骤：
            1. 如果 page_analysis 还未完成，继续调用 pdf_ocr，并用 write_text_file 落盘每页 JSON。
            2. 如果 page_analysis 已完成，继续 continuous_reconstruction。
            3. 然后继续 book_reconstruction、epub_content_generation。
            4. 最终必须调用 build_epub(input_dir="work/epub", output="output/book.epub")。

            只有生成 EPUB 后，或遇到 System Prompt 定义的必须停止条件时，才允许返回最终文本。
        """.trimIndent()
    }

    private fun LLMParams.withKimiThinkingDisabled(): LLMParams {
        return copy(
            additionalProperties = buildMap {
                additionalProperties?.let { putAll(it) }
                put("thinking", JsonObject(mapOf("type" to JsonPrimitive("disabled"))))
            },
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

sealed interface LlmStreamingResult {

    data class Stream(val frame: StreamFrame) : LlmStreamingResult

    data class Completed(val message: String) : LlmStreamingResult
}
