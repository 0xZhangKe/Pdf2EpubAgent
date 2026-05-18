package com.zhangke.agent.pdf2epub.core.llm.model

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import com.zhangke.agent.pdf2epub.core.llm.getEnvironmentVariable
import com.zhangke.agent.pdf2epub.core.llm.tool.AppendTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.BuildEpubTool
import com.zhangke.agent.pdf2epub.core.llm.tool.PdfToImagesTool
import com.zhangke.agent.pdf2epub.core.llm.tool.ReadTextFileTool
import com.zhangke.agent.pdf2epub.core.llm.tool.WriteTextFileTool

fun buildOpenAIAgent(
    systemPrompt: String,
    additionalTools: List<Tool<*, *>> = emptyList(),
    maxIterations: Int = 200,
): GraphAIAgent<String, String> {
    val apiKey = getEnvironmentVariable("OPENAI_API_KEY").orEmpty()
    val client = OpenAILLMClient(
        apiKey = apiKey,
    )

    return AIAgent(
        promptExecutor = MultiLLMPromptExecutor(client),
        systemPrompt = systemPrompt,
        llmModel = OpenAIModels.Chat.GPT5_4,
        temperature = 0.4,
        maxIterations = maxIterations,
        toolRegistry = ToolRegistry {
            for (toolItem in additionalTools) {
                tool(toolItem)
            }
        },
    ) {
        handleEvents {
            onToolCallStarting { eventContext ->
                println("Tool called: ${eventContext.toolName} with args ${eventContext.toolArgs}")
            }
        }
    } as GraphAIAgent<String, String>
}
