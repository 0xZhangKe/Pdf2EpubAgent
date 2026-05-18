package com.zhangke.agent.pdf2epub.core.llm

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import com.zhangke.agent.pdf2epub.core.llm.model.buildOpenAIAgent
import kotlinx.io.files.Path

class SimpleLlmClient(
    private val systemPrompt: String,
) {

    suspend fun send(
        text: String,
        imagePaths: List<String> = emptyList(),
    ): String {
        println("SimpleLlmClient: Sending message with text: $text and imagePaths: $imagePaths")
        require(text.isNotBlank()) {
            "Message text is blank."
        }

        val agent = buildOpenAIAgent(
            systemPrompt = systemPrompt,
            additionalTools = emptyList(),
            maxIterations = 1,
        )
        try {
            val config = agent.agentConfig
            val currentPrompt =
                prompt(config.prompt.withParams(config.prompt.params.withKimiThinkingDisabled())) {
                    user {
                        text(text)
                        imagePaths.forEach { image(Path(it)) }
                    }
                }

            val responses = agent.promptExecutor.execute(
                prompt = currentPrompt,
                model = config.model,
                tools = emptyList(),
            )

            return responses
                .filterIsInstance<Message.Assistant>()
                .joinToString(separator = "\n") { it.content }
                .ifBlank {
                    responses.joinToString(separator = "\n") { it.content }
                }.also {
                    println("SimpleLlmClient: Received response: $it")
                }
        } finally {
            agent.promptExecutor.close()
        }
    }

    private fun LLMParams.withKimiThinkingDisabled(): LLMParams {
        return this
    }
}
