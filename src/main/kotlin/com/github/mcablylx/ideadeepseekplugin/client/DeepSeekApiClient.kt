package com.github.mcablylx.ideadeepseekplugin.client

import com.github.mcablylx.ideadeepseekplugin.settings.DeepSeekSettings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * DeepSeek API 客户端服务
 * 负责与 DeepSeek API 进行通信
 */
@Service(Service.Level.APP)
class DeepSeekApiClient {

    private val gson = Gson()

    /**
     * 发送聊天请求并获取流式响应
     */
    suspend fun chatStream(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ): String {
        val settings = DeepSeekSettings.instance
        
        if (settings.apiKey.isBlank()) {
            throw IllegalStateException("请先在设置中配置 DeepSeek API Key")
        }

        return withContext(Dispatchers.IO) {
            val url = URL("${settings.apiBaseUrl}/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                
                // 构建请求体
                val requestBody = buildRequestBody(messages, settings)
                connection.outputStream.use {
                    it.write(requestBody.toByteArray())
                }
                
                // 读取流式响应
                val fullResponse = StringBuilder()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (line!!.startsWith("data: ")) {
                                val data = line!!.substring(6)
                                if (data == "[DONE]") break
                                
                                try {
                                    val json = gson.fromJson(data, JsonObject::class.java)
                                    val choices = json.getAsJsonArray("choices")
                                    if (choices.size() > 0) {
                                        val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                                        val content = delta?.get("content")?.asString
                                        if (content != null) {
                                            fullResponse.append(content)
                                            onChunk(content)
                                        }
                                    }
                                } catch (e: Exception) {
                                    thisLogger().warn("解析响应块失败: ${e.message}")
                                }
                            }
                        }
                    }
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).readText()
                    throw parseErrorResponse(errorResponse, connection.responseCode)
                }
                
                fullResponse.toString()
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * 发送聊天请求并获取完整响应（非流式）
     */
    suspend fun chat(messages: List<ChatMessage>): String {
        val settings = DeepSeekSettings.instance
        
        if (settings.apiKey.isBlank()) {
            throw IllegalStateException("请先在设置中配置 DeepSeek API Key")
        }

        return withContext(Dispatchers.IO) {
            val url = URL("${settings.apiBaseUrl}/v1/chat/completions")
            val connection = url.openConnection() as HttpURLConnection
            
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                
                val requestBody = buildRequestBody(messages, settings, stream = false)
                connection.outputStream.use {
                    it.write(requestBody.toByteArray())
                }
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        val response = reader.readText()
                        val json = gson.fromJson(response, JsonObject::class.java)
                        val choices = json.getAsJsonArray("choices")
                        if (choices.size() > 0) {
                            val message = choices[0].asJsonObject.getAsJsonObject("message")
                            message?.get("content")?.asString ?: ""
                        } else {
                            ""
                        }
                    }
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).readText()
                    throw parseErrorResponse(errorResponse, connection.responseCode)
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun buildRequestBody(
        messages: List<ChatMessage>,
        settings: DeepSeekSettings,
        stream: Boolean = true
    ): String {
        val request = JsonObject()
        request.addProperty("model", settings.modelName)
        request.addProperty("temperature", settings.temperature)
        request.addProperty("max_tokens", settings.maxTokens)
        request.addProperty("stream", stream)
        
        val messagesArray = com.google.gson.JsonArray()
        if (settings.systemPrompt.isNotBlank()) {
            val systemMessage = JsonObject()
            systemMessage.addProperty("role", "system")
            systemMessage.addProperty("content", settings.systemPrompt)
            messagesArray.add(systemMessage)
        }
        
        messages.forEach { msg ->
            val messageObj = JsonObject()
            messageObj.addProperty("role", msg.role)
            messageObj.addProperty("content", msg.content)
            messagesArray.add(messageObj)
        }
        
        request.add("messages", messagesArray)
        return gson.toJson(request)
    }
}

/**
 * 聊天消息数据类
 */
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)
