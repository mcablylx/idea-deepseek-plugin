package com.github.mcablylx.ideadeepseekplugin.actions

import com.github.mcablylx.ideadeepseekplugin.client.ChatMessage
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiClient
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * 内联问答动作
 * 快捷键 Ctrl+Shift+Q 触发
 */
class InlineQuestionAction : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        val selectionModel = editor.selectionModel
        val selectedCode = selectionModel.selectedText
        
        // 打开问答对话框
        val dialog = InlineQuestionDialog(project, selectedCode)
        dialog.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    /**
     * 内联问答对话框
     */
    class InlineQuestionDialog(
        private val project: Project,
        private val selectedCode: String?
    ) : DialogWrapper(project) {

        private val questionArea = JBTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            font = font.deriveFont(13f)
            rows = 5
        }
        private val answerArea = JBTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            isEditable = false
            font = font.deriveFont(13f)
        }

        init {
            init()
            title = "AI 代码问答 (Ctrl+Shift+Q)"
            setSize(700, 500)
        }

        override fun createCenterPanel(): JComponent {
            val panel = JPanel(BorderLayout(10, 10))
            
            // 问题输入区
            val questionPanel = JPanel(BorderLayout()).apply {
                border = javax.swing.BorderFactory.createTitledBorder("问题")
            }
            
            if (!selectedCode.isNullOrBlank()) {
                val codeLabel = javax.swing.JLabel("已选中的代码：")
                val codePreview = JBTextArea(selectedCode.take(200) + if (selectedCode.length > 200) "..." else "").apply {
                    isEditable = false
                    rows = 3
                    font = font.deriveFont(11f)
                }
                questionPanel.add(codeLabel, BorderLayout.NORTH)
                questionPanel.add(JBScrollPane(codePreview), BorderLayout.CENTER)
            }
            
            val questionScrollPane = JBScrollPane(questionArea)
            questionPanel.add(questionScrollPane, if (!selectedCode.isNullOrBlank()) BorderLayout.SOUTH else BorderLayout.CENTER)
            
            // 答案显示区
            val answerPanel = JPanel(BorderLayout()).apply {
                border = javax.swing.BorderFactory.createTitledBorder("AI 回答")
            }
            answerPanel.add(JBScrollPane(answerArea), BorderLayout.CENTER)
            
            panel.add(questionPanel, BorderLayout.NORTH)
            panel.add(answerPanel, BorderLayout.CENTER)
            
            return panel
        }

        override fun createActions(): Array<javax.swing.Action> {
            return arrayOf(
                okAction,
                cancelAction
            )
        }

        override fun doOKAction() {
            val question = questionArea.text.trim()
            if (question.isBlank()) {
                Messages.showWarningDialog(project, "请输入您的问题", "提示")
                return
            }

            okAction.isEnabled = false
            answerArea.text = "正在思考..."

            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val apiClient = ApplicationManager.getApplication().service<DeepSeekApiClient>()

                    val messages = mutableListOf(
                        ChatMessage("system", """
                            你是一个专业的编程助手，回答开发者关于代码的问题。

                            要求：
                            1. 用中文回答
                            2. 提供具体的代码示例
                            3. 解释原理和最佳实践
                        """.trimIndent())
                    )

                    if (!selectedCode.isNullOrBlank()) {
                        messages.add(ChatMessage("user", "参考以下代码：\n```$selectedCode```\n\n问题：$question"))
                    } else {
                        messages.add(ChatMessage("user", question))
                    }

                    val answer = withContext(Dispatchers.IO) {
                        apiClient.chatNonStream(messages)
                    }

                    SwingUtilities.invokeLater {
                        answerArea.text = answer
                        okAction.isEnabled = true
                        okAction.putValue(javax.swing.Action.NAME, "继续提问")
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        answerArea.text = "❌ 错误：${e.message}"
                        okAction.isEnabled = true
                        okAction.putValue(javax.swing.Action.NAME, "重试")
                    }
                }
            }
        }
    }
}
