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
 * 解释代码动作
 * 右键菜单中选择"解释代码"时使用
 */
class ExplainCodeAction : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // 获取选中的代码
        val selectionModel = editor.selectionModel
        val selectedCode = selectionModel.selectedText
        
        if (selectedCode.isNullOrBlank()) {
            Messages.showWarningDialog(
                project,
                "请先选中要解释的代码",
                "提示"
            )
            return
        }
        
        // 使用进度条显示
        ProgressManager.getInstance().run(object : Task.Modal(
            project,
            "AI 解释代码",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在分析代码..."
                indicator.isIndeterminate = true
                
                try {
                    val apiClient = ApplicationManager.getApplication().service<DeepSeekApiClient>()
                    
                    val messages = listOf(
                        ChatMessage("system", """
                            你是一个代码解释助手。用简洁易懂的中文解释代码的功能。
                            
                            要求：
                            1. 解释代码的主要功能
                            2. 说明关键逻辑和算法
                            3. 指出潜在的问题或改进点
                            4. 使用通俗易懂的语言
                        """.trimIndent()),
                        ChatMessage("user", "请解释以下代码：\n\n```\n$selectedCode\n```")
                    )
                    
                    indicator.text = "正在生成解释..."
                    val explanation = runBlocking {
                        apiClient.chatNonStream(messages)
                    }
                    
                    // 在 EDT 线程显示结果
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showMessageDialog(
                            project,
                            explanation,
                            "AI 代码解释",
                            null
                        )
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "解释失败：${e.message}",
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
