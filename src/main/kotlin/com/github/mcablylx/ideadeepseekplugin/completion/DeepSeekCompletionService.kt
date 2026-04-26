package com.github.mcablylx.ideadeepseekplugin.completion

import com.github.mcablylx.ideadeepseekplugin.client.ChatMessage
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiClient
import com.github.mcablylx.ideadeepseekplugin.settings.DeepSeekSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class DeepSeekCompletionService(private val project: Project) {

    private val apiClient = ApplicationManager.getApplication().getService(DeepSeekApiClient::class.java)
    private val settings = DeepSeekSettings.instance

    private val completionCache = object : LinkedHashMap<String, String>(100, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 100
        }
    }

    suspend fun getInlineCompletion(
        editor: Editor,
        offset: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (settings.apiKey.isBlank()) {
                return@withContext null
            }

            val (context, localOffset) = buildCompletionContext(editor, offset)
            val fileType = editor.virtualFile?.extension ?: "txt"

            val cacheKey = "${editor.virtualFile?.path}-${offset}-${context.hashCode()}"
            completionCache[cacheKey]?.let { return@withContext it }

            val prompt = buildCompletionPrompt(context, localOffset, fileType)

            val messages = listOf(
                ChatMessage("system", "你是一个代码补全助手。只返回补全的代码，不要任何解释。"),
                ChatMessage("user", prompt)
            )

            val completion = apiClient.chatNonStream(messages)

            if (completion.isNotBlank()) {
                completionCache[cacheKey] = completion
            }

            completion.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            thisLogger().warn("代码补全失败", e)
            null
        }
    }

    private fun buildCompletionContext(editor: Editor, offset: Int): Pair<String, Int> {
        val document = editor.document
        val text = document.text

        val start = maxOf(0, offset - 2000)
        val end = minOf(text.length, offset + 2000)

        val context = text.substring(start, end)
        val localOffset = offset - start
        return Pair(context, localOffset)
    }

    private fun buildCompletionPrompt(context: String, localOffset: Int, fileType: String): String {
        val cursorMarker = "<|CURSOR|>"
        val markedContext = context.insertCursorMarker(localOffset, cursorMarker)

        return """
            请为以下${fileType}代码提供补全建议。

            代码上下文：
            ```${fileType}
            $markedContext
            ```

            要求：
            1. 只返回光标位置之后应该补充的代码
            2. 保持代码风格一致
            3. 遵循最佳实践
            4. 不要重复已有的代码
            5. 不要包含任何解释，只返回代码

            补全代码：
        """.trimIndent()
    }

    suspend fun getBlockCompletion(
        editor: Editor,
        offset: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (settings.apiKey.isBlank()) {
                return@withContext null
            }

            val context = getLargerContext(editor, offset)

            val prompt = """
                请为以下代码提供完整的代码块补全建议。

                当前代码：
                ```
                $context
                ```

                要求：
                1. 提供完整的代码块（可能包含多行）
                2. 保持代码风格一致
                3. 包含必要的注释
                4. 遵循该语言的最佳实践

                补全代码：
            """.trimIndent()

            val messages = listOf(
                ChatMessage("system", "你是一个代码补全助手。返回完整的代码块建议，不要任何解释。"),
                ChatMessage("user", prompt)
            )

            val completion = apiClient.chatNonStream(messages)
            completion.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            thisLogger().warn("代码块补全失败", e)
            null
        }
    }

    private fun getLargerContext(editor: Editor, offset: Int): String {
        val document = editor.document
        val text = document.text

        val start = maxOf(0, offset - 5000)
        val end = minOf(text.length, offset + 5000)

        return text.substring(start, end)
    }

    fun clearCache() {
        completionCache.clear()
    }
}

private fun String.insertCursorMarker(offset: Int, marker: String): String {
    val safeOffset = offset.coerceIn(0, this.length)
    return this.substring(0, safeOffset) + marker + this.substring(safeOffset)
}
