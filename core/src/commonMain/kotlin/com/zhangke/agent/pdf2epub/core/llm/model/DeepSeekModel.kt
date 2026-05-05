package com.zhangke.agent.pdf2epub.core.llm.model

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.zhangke.agent.pdf2epub.core.llm.tool.AppendTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.BuildEpubTool
import com.zhangke.agent.pdf2epub.core.llm.tool.PdfToImagesTool
import com.zhangke.agent.pdf2epub.core.llm.tool.ReadTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.WriteTextFileTool

fun buildDeepSeekAgent(
    systemPrompt: String,
    apiKey: String,
    baseUrl: String = DeepSeekDefaults.BASE_URL,
    model: LLModel = DeepSeekModels.CHAT,
    temperature: Double = 0.4,
    maxIterations: Int = 10,
): GraphAIAgent<String, String> {
    require(apiKey.isNotBlank()) {
        "DeepSeek API key is blank. Set DEEPSEEK_API_KEY or pass apiKey when creating LlmClient."
    }

    val client = OpenAILLMClient(
        apiKey = apiKey,
        settings = OpenAIClientSettings(
            baseUrl = baseUrl,
            chatCompletionsPath = "v1/chat/completions",
            modelsPath = "v1/models",
        ),
    )

    return AIAgent(
        promptExecutor = MultiLLMPromptExecutor(client),
        systemPrompt = systemPrompt,
        llmModel = model,
        temperature = temperature,
        toolRegistry = ToolRegistry {
            tool(AppendTextFileTool)
            tool(BuildEpubTool)
            tool(PdfToImagesTool)
            tool(ReadTextFileTool)
            tool(WriteTextFileTool)
        },
        maxIterations = maxIterations,
    ) {
        handleEvents {
            onToolCallStarting { eventContext ->
                println("Tool called: ${eventContext.toolName} with args ${eventContext.toolArgs}")
            }
        }
    } as GraphAIAgent<String, String>
}

object DeepSeekDefaults {

    const val BASE_URL = "https://api.deepseek.com"
}

object DeepSeekModels {

    val CHAT: LLModel = LLModel(
        provider = LLMProvider.OpenAI,
        id = "deepseek-chat",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
            LLMCapability.Completion,
            LLMCapability.MultipleChoices,
            LLMCapability.OpenAIEndpoint.Completions,
        ),
    )

    val REASONER: LLModel = LLModel(
        provider = LLMProvider.OpenAI,
        id = "deepseek-reasoner",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Completion,
            LLMCapability.MultipleChoices,
            LLMCapability.OpenAIEndpoint.Completions,
        ),
    )
}
