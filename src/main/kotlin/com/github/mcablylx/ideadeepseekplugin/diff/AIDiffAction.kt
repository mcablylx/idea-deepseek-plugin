package com.github.mcablylx.ideadeepseekplugin.diff

import com.github.mcablylx.ideadeepseekplugin.client.ChatMessage
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiClient
import com.github.mcablylx.ideadeepseekplugin.util.extractCodeFromResponse
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.changes.Change
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * AI代码差异对比动作
 * 选中代码后右键选择"🔄 AI差异对比"，查看AI建议的修改
 */
class AIDiffAction : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val selectionModel = editor.selectionModel

        if (!selectionModel.hasSelection()) {
            Messages.showWarningDialog(
                project,
                "请先选中需要对比的代码",
                "AI差异对比"
            )
            return
        }

        val selectedCode = selectionModel.selectedText ?: return

        // 调用AI生成修改建议
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在生成AI修改建议...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "正在分析代码..."
                    indicator.fraction = 0.2

                    val apiClient = ApplicationManager.getApplication().service<DeepSeekApiClient>()

                    val messages = listOf(
                        ChatMessage("system", """
                            你是一个代码优化专家。请分析提供的代码并给出优化版本。
                            
                            要求：
                            1. 保持原有功能不变
                            2. 优化代码结构和可读性
                            3. 遵循最佳实践
                            4. 只返回优化后的代码，不要其他解释
                            5. 使用代码块格式包裹
                        """.trimIndent()),
                        ChatMessage("user", "请优化以下代码：\n\n```\n$selectedCode\n```")
                    )

                    indicator.text = "正在生成优化代码..."
                    indicator.fraction = 0.6

                    val aiResponse = runBlocking {
                        apiClient.chatNonStream(messages)
                    }

                    indicator.fraction = 0.9

                    // 提取AI返回的代码
                    val aiCode = extractCodeFromResponse(aiResponse)

                    if (aiCode.isBlank()) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "AI未返回有效的代码建议",
                                "AI差异对比"
                            )
                        }
                        return
                    }

                    // 显示差异对比
                    ApplicationManager.getApplication().invokeLater {
                        showDiff(project, selectedCode, aiCode)
                    }

                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "生成失败：${e.message}",
                            "AI差异对比"
                        )
                    }
                }
            }
        })
    }

    /**
     * 显示差异对比对话框
     */
    private fun showDiff(project: Project, originalCode: String, aiCode: String) {
        val diffContentFactory = DiffContentFactory.getInstance()

        val originalContent = diffContentFactory.create(project, originalCode)
        val aiContent = diffContentFactory.create(project, aiCode)

        val diffRequest = SimpleDiffRequest(
            "AI代码优化建议",
            originalContent,
            aiContent,
            "原始代码",
            "AI优化代码"
        )

        DiffManager.getInstance().showDiff(project, diffRequest)
    }

}
