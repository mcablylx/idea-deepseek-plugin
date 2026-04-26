package com.github.mcablylx.ideadeepseekplugin.actions

import com.github.mcablylx.ideadeepseekplugin.client.ChatMessage
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiClient
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.NotNull

/**
 * 重构建议动作
 * 右键菜单中选择"获取重构建议"时使用
 */
class RefactorSuggestionAction : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        val selectionModel = editor.selectionModel
        val selectedCode = selectionModel.selectedText
        
        if (selectedCode.isNullOrBlank()) {
            Messages.showWarningDialog(
                project,
                "请先选中要重构的代码",
                "提示"
            )
            return
        }
        
        ProgressManager.getInstance().run(object : Task.Modal(
            project,
            "AI 重构建议",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在分析代码..."
                indicator.isIndeterminate = true
                
                try {
                    val apiClient = ApplicationManager.getApplication().service<DeepSeekApiClient>()
                    
                    val messages = listOf(
                        ChatMessage("system", """
                            你是一个代码重构专家。分析代码并提供重构建议。
                            
                            要求：
                            1. 识别代码异味（code smell）
                            2. 提供具体的重构方案
                            3. 给出重构后的代码示例
                            4. 解释重构的原因和好处
                        """.trimIndent()),
                        ChatMessage("user", "请分析并提供以下代码的重构建议：\n\n```\n$selectedCode\n```")
                    )
                    
                    indicator.text = "正在生成建议..."
                    val suggestion = runBlocking {
                        apiClient.chatNonStream(messages)
                    }
                    
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showMessageDialog(
                            project,
                            suggestion,
                            "AI 重构建议",
                            null
                        )
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "分析失败：${e.message}",
                            "错误"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = hasSelection
    }
}
