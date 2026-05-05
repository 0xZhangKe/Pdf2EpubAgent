package com.zhangke.agent.pdf2epub.core.llm.model

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIMessage
import ai.koog.prompt.executor.clients.openai.base.models.OpenAITool
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIToolChoice
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import com.zhangke.agent.pdf2epub.core.llm.tool.AppendTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.BuildEpubTool
import com.zhangke.agent.pdf2epub.core.llm.tool.PdfToImagesTool
import com.zhangke.agent.pdf2epub.core.llm.tool.ReadTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.WriteTextFileTool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

fun buildKimiAgent(
    systemPrompt: String,
    apiKey: String,
    toolsEnabled: Boolean = true,
    additionalTools: List<Tool<*, *>> = emptyList(),
    maxIterations: Int = 10,
): GraphAIAgent<String, String> {
    val baseUrl = "https://api.moonshot.ai"
    val client = KimiOpenAILLMClient(
        apiKey = apiKey,
        settings = OpenAIClientSettings(
            baseUrl = baseUrl,
            chatCompletionsPath = "v1/chat/completions",
            modelsPath = "v1/models",
        )
    )

    return AIAgent(
        promptExecutor = MultiLLMPromptExecutor(client),
        systemPrompt = systemPrompt,
        llmModel = KimiModels.K2_6,
        temperature = 0.6,
        toolRegistry = if (toolsEnabled) {
            ToolRegistry {
                tool(AppendTextFileTool)
                tool(BuildEpubTool)
                tool(PdfToImagesTool)
                tool(ReadTextFileTool)
                tool(WriteTextFileTool)
                tools(additionalTools)
            }
        } else {
            ToolRegistry.EMPTY
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

private class KimiOpenAILLMClient(
    apiKey: String,
    settings: OpenAIClientSettings,
) : OpenAILLMClient(apiKey = apiKey, settings = settings) {

    override fun serializeProviderChatRequest(
        messages: List<OpenAIMessage>,
        model: LLModel,
        tools: List<OpenAITool>?,
        toolChoice: OpenAIToolChoice?,
        params: LLMParams,
        stream: Boolean,
    ): String {
        val request = Json.parseToJsonElement(
            super.serializeProviderChatRequest(
                messages = messages,
                model = model,
                tools = tools,
                toolChoice = toolChoice,
                params = params,
                stream = stream,
            ),
        ).jsonObject

        return buildJsonObject {
            request.entries
                .filterNot { (key, _) -> key == "additional_properties" || key == "thinking" }
                .forEach { (key, value) -> put(key, value) }
            put("temperature", JsonPrimitive(0.6))
            put(
                "thinking",
                buildJsonObject {
                    put("type", JsonPrimitive("disabled"))
                },
            )
        }.toString()
    }
}

private object KimiModels {
    val K2_6: LLModel = LLModel(
        provider = LLMProvider.OpenAI,
        id = "kimi-k2.6",
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
            LLMCapability.Vision.Image,
            LLMCapability.Vision.Video,
            LLMCapability.Completion,
            LLMCapability.MultipleChoices,
            LLMCapability.OpenAIEndpoint.Completions,
        ),
    )
}
