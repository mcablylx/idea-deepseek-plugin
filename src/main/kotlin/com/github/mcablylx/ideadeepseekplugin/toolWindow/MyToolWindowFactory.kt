package com.github.mcablylx.ideadeepseekplugin.toolWindow

import com.github.mcablylx.ideadeepseekplugin.client.ChatMessage
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiClient
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiException
import com.github.mcablylx.ideadeepseekplugin.settings.DeepSeekSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.RoundRectangle2D
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.border.AbstractBorder


class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
    
    /**
     * 圆角边框类
     */
    class RoundBorder(private val backgroundColor: Color, private val radius: Int = 12) : AbstractBorder() {
        override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
            if (g is Graphics2D) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g.color = backgroundColor
                val roundRect = RoundRectangle2D.Float(x.toFloat(), y.toFloat(), width - 1f, height - 1f, radius.toFloat(), radius.toFloat())
                g.fill(roundRect)
            }
        }
        
        override fun getBorderInsets(c: Component?) = JBUI.insets(10, 12, 10, 12)
        
        override fun isBorderOpaque() = true
    }

    class MyToolWindow(private val toolWindow: ToolWindow) {

        private val project = toolWindow.project
        private val apiClient = service<DeepSeekApiClient>()
        private val settings = DeepSeekSettings.instance
        
        private val chatPanel = JPanel(BorderLayout())
        private val messagesPanel = JPanel()
        private val messagesScrollPane: JScrollPane
        private val inputField = JTextField()
        private val sendButton = JButton("发送")
        private val clearButton = JButton("清空")
        
        private val chatHistory = mutableListOf<ChatMessage>()
        private var currentResponseArea: JTextArea? = null
        
        /**
         * 将 Color 对象转换为十六进制字符串（用于 HTML 样式）
         */
        private fun toHex(color: java.awt.Color): String {
            return String.format("#%02x%02x%02x", color.red, color.green, color.blue)
        }
        
        /**
         * 获取命名颜色（支持浅色/深色主题）
         */
        private fun namedColor(name: String, defaultColor: java.awt.Color): java.awt.Color {
            return JBColor.namedColor(name, defaultColor)
        }

        init {
            // 设置消息面板为从顶部开始排列，使用 BoxLayout.Y_AXIS
            messagesPanel.layout = javax.swing.BoxLayout(messagesPanel, javax.swing.BoxLayout.Y_AXIS)
            messagesPanel.alignmentX = Component.LEFT_ALIGNMENT
            
            // 创建一个包装面板，让消息从顶部开始，靠左对齐
            val wrapperPanel = JPanel(BorderLayout()).apply {
                add(messagesPanel, BorderLayout.NORTH)
            }
            
            messagesScrollPane = JBScrollPane(wrapperPanel).apply {
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                border = BorderFactory.createEmptyBorder()
            }
        }

        fun getContent() = chatPanel.apply {
            // 顶部：消息显示区域
            add(messagesScrollPane, BorderLayout.CENTER)
            
            // 底部：输入区域
            val inputPanel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(5)
            }
            
            inputField.toolTipText = "输入您的问题..."
            inputPanel.add(inputField, BorderLayout.CENTER)
            
            val buttonPanel = JPanel().apply {
                layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS)
                add(sendButton)
                add(clearButton)
            }
            inputPanel.add(buttonPanel, BorderLayout.EAST)
            
            add(inputPanel, BorderLayout.SOUTH)
            
            // 添加欢迎消息
            addMessage("assistant", "您好！我是 DeepSeek AI 助手，有什么可以帮助您的吗？")
            
            // 发送按钮事件
            sendButton.addActionListener {
                sendMessage()
            }
            
            // 回车发送消息
            inputField.addActionListener {
                sendMessage()
            }
            
            // 清空按钮事件
            clearButton.addActionListener {
                clearChat()
            }
        }
        
        private fun sendMessage() {
            val message = inputField.text.trim()
            if (message.isBlank()) return
            
            // 检查 API Key 是否配置
            if (settings.apiKey.isBlank()) {
                addMessage("assistant", "⚠️ 请先在设置中配置 DeepSeek API Key\n路径：Settings -> Tools -> DeepSeek AI")
                return
            }
            
            // 添加用户消息
            addMessage("user", message)
            chatHistory.add(ChatMessage("user", message))
            inputField.text = ""
            
            // 禁用输入和发送按钮
            inputField.isEnabled = false
            sendButton.isEnabled = false
            
            // 创建 AI 回复区域
            val aiResponseArea = JTextArea().apply {
                lineWrap = true
                wrapStyleWord = true
                isEditable = false
                background = null
            }
            currentResponseArea = aiResponseArea
            
            // 调用 API
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val fullResponse = apiClient.chatStream(chatHistory) { chunk ->
                        // 在 EDT 线程更新 UI
                        SwingUtilities.invokeLater {
                            aiResponseArea.text = aiResponseArea.text + chunk
                            scrollToBottom()
                        }
                    }
                    
                    // 保存 AI 回复到历史，并添加到界面
                    chatHistory.add(ChatMessage("assistant", fullResponse))
                    SwingUtilities.invokeLater {
                        addMessageComponent("assistant", aiResponseArea)
                    }
                } catch (e: Exception) {
                    thisLogger().error("调用 DeepSeek API 失败", e)
                    SwingUtilities.invokeLater {
                        val errorMessage = when (e) {
                            is DeepSeekApiException.InsufficientBalance -> {
                                "💰 余额不足\n${e.message}\n\n请前往 DeepSeek 平台充值：\nhttps://platform.deepseek.com"
                            }
                            is DeepSeekApiException.InvalidApiKey -> {
                                "🔑 API Key 无效\n${e.message}\n\n请在设置中检查您的 API Key 配置。"
                            }
                            is DeepSeekApiException.RateLimitExceeded -> {
                                "⏱️ 请求过于频繁\n${e.message}\n\n请稍等片刻后再试。"
                            }
                            is DeepSeekApiException.GeneralError -> {
                                "❌ API 错误\n${e.message}"
                            }
                            else -> "❌ 错误：${e.message}"
                        }
                        addErrorMessage("error", errorMessage)
                    }
                } finally {
                    SwingUtilities.invokeLater {
                        inputField.isEnabled = true
                        sendButton.isEnabled = true
                        inputField.requestFocusInWindow()
                    }
                }
            }
        }
        
        private fun addMessage(role: String, content: String) {
            // 创建消息容器面板（带左右边距）
            val messageContainer = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4, 8)
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
                alignmentX = Component.LEFT_ALIGNMENT
            }
            
            val fgColor = com.intellij.ui.JBColor.foreground()
            
            // 创建标题标签面板
            val headerPanel = JPanel(BorderLayout()).apply {
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                border = JBUI.Borders.empty(0, 0, 6, 0)
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, 30)
                
                val titleLabel = if (role == "user") {
                    JBLabel("👤 我").apply {
                        font = font.deriveFont(java.awt.Font.BOLD, 12f)
                        foreground = com.intellij.ui.JBColor.namedColor("Link.activeForeground", java.awt.Color(41, 121, 255))
                    }
                } else {
                    JBLabel("🤖 ${settings.modelName}").apply {
                        font = font.deriveFont(java.awt.Font.BOLD, 12f)
                        foreground = com.intellij.ui.JBColor.namedColor("Plugin.Details.linkColor", java.awt.Color(0, 180, 0))
                    }
                }
                add(titleLabel, BorderLayout.WEST)
            }
            
            // 创建消息内容标签（使用 HTML 支持换行）
            val htmlContent = "<html><body style='width: 100%; color: ${toHex(fgColor)}; font-size: 13px; line-height: 1.6;'>${content.replace("\n", "<br>")}</body></html>"
            val contentLabel = JBLabel(htmlContent).apply {
                maximumSize = java.awt.Dimension(600, Int.MAX_VALUE)  // 限制最大宽度，促进换行
            }
            
            // 创建圆角气泡面板
            val bubblePanel = JPanel(BorderLayout()).apply {
                // 根据角色设置不同颜色
                background = if (role == "user") {
                    com.intellij.ui.JBColor(
                        java.awt.Color(220, 240, 255),  // 浅色主题：淡蓝色
                        java.awt.Color(40, 60, 90)      // 深色主题：深蓝色
                    )
                } else {
                    com.intellij.ui.JBColor(
                        java.awt.Color(240, 240, 240),  // 浅色主题：淡灰色
                        java.awt.Color(55, 55, 55)      // 深色主题：深灰色
                    )
                }
                isOpaque = false  // 重要：让圆角边框生效
                border = RoundBorder(background, 12)  // 使用自定义圆角边框
                add(contentLabel, BorderLayout.CENTER)
                maximumSize = java.awt.Dimension(600, Int.MAX_VALUE)
            }
            
            // 组合：标题 + 气泡（带左边距）
            val bubbleWithMargin = JPanel(BorderLayout()).apply {
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                border = JBUI.Borders.empty(0, if (role == "user") 40 else 0, 0, 0)  // 用户消息靠右一些
                add(bubblePanel, BorderLayout.CENTER)
            }
            
            messageContainer.add(headerPanel, BorderLayout.NORTH)
            messageContainer.add(bubbleWithMargin, BorderLayout.CENTER)
            
            SwingUtilities.invokeLater {
                messagesPanel.add(messageContainer)
                messagesPanel.revalidate()
                messagesPanel.repaint()
                scrollToBottom()
            }
        }
        
        private fun addMessageComponent(role: String, component: JTextArea) {
            // 创建消息容器面板（带左右边距）
            val messageContainer = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4, 8)
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
                alignmentX = Component.LEFT_ALIGNMENT
            }
            
            // 创建标题面板
            val headerPanel = JPanel(BorderLayout()).apply {
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                border = JBUI.Borders.empty(0, 0, 6, 0)
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, 30)
                
                val titleLabel = if (role == "user") {
                    JBLabel("👤 我").apply {
                        font = font.deriveFont(java.awt.Font.BOLD, 12f)
                        foreground = com.intellij.ui.JBColor.namedColor("Link.activeForeground", java.awt.Color(41, 121, 255))
                    }
                } else {
                    JBLabel("🤖 ${settings.modelName}").apply {
                        font = font.deriveFont(java.awt.Font.BOLD, 12f)
                        foreground = com.intellij.ui.JBColor.namedColor("Plugin.Details.linkColor", java.awt.Color(0, 180, 0))
                    }
                }
                add(titleLabel, BorderLayout.WEST)
            }
            
            // 设置文本区域样式：使用主题颜色，根据内容调整高度
            component.apply {
                background = if (role == "user") {
                    com.intellij.ui.JBColor(
                        java.awt.Color(220, 240, 255),  // 浅色主题：淡蓝色
                        java.awt.Color(40, 60, 90)      // 深色主题：深蓝色
                    )
                } else {
                    com.intellij.ui.JBColor(
                        java.awt.Color(240, 240, 240),  // 浅色主题：淡灰色
                        java.awt.Color(55, 55, 55)      // 深色主题：深灰色
                    )
                }
                foreground = com.intellij.ui.JBColor.foreground()
                border = BorderFactory.createEmptyBorder(8, 10, 8, 10)
                isOpaque = false  // 让圆角边框生效
                
                // 根据内容计算合适的行数（每行约 40 个字符）
                val lineCount = text.split("\n").size
                val maxLines = Math.min(lineCount, 15) // 最多显示 15 行，避免过高
                rows = Math.max(2, maxLines)
                
                // 设置合适的字体大小和行距
                font = font.deriveFont(13f)
                maximumSize = java.awt.Dimension(600, Int.MAX_VALUE)
            }
            
            // 创建圆角气泡面板包裹文本区域
            val bubblePanel = JPanel(BorderLayout()).apply {
                background = component.background
                isOpaque = false
                border = RoundBorder(background, 12)
                add(component, BorderLayout.CENTER)
                maximumSize = java.awt.Dimension(600, Int.MAX_VALUE)
            }
            
            // 组合：标题 + 气泡（带左边距）
            val bubbleWithMargin = JPanel(BorderLayout()).apply {
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                border = JBUI.Borders.empty(0, if (role == "user") 40 else 0, 0, 0)
                add(bubblePanel, BorderLayout.CENTER)
            }
            
            messageContainer.add(headerPanel, BorderLayout.NORTH)
            messageContainer.add(bubbleWithMargin, BorderLayout.CENTER)
            
            SwingUtilities.invokeLater {
                messagesPanel.add(messageContainer)
                messagesPanel.revalidate()
                messagesPanel.repaint()
                scrollToBottom()
            }
        }
        
        private fun addErrorMessage(role: String, message: String) {
            // 创建消息容器面板（带左右边距）
            val messageContainer = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4, 8)
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
                alignmentX = Component.LEFT_ALIGNMENT
            }
            
            // 创建错误内容标签（使用 HTML 支持换行）
            val htmlMessage = "<html><body style='width: 100%; color: #d32f2f; font-size: 13px; line-height: 1.6;'>${message.replace("\n", "<br>")}</body></html>"
            val label = JBLabel(htmlMessage).apply {
                maximumSize = java.awt.Dimension(600, Int.MAX_VALUE)
            }
            
            // 创建标题标签（错误提示）
            val titleLabel = JBLabel("⚠️ 错误").apply {
                font = font.deriveFont(java.awt.Font.BOLD, 12f)
                foreground = java.awt.Color(211, 47, 47)  // 红色表示错误
            }
            
            // 创建标题面板
            val headerPanel = JPanel(BorderLayout()).apply {
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                border = JBUI.Borders.empty(0, 0, 6, 0)
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, 30)
                add(titleLabel, BorderLayout.WEST)
            }
            
            // 错误气泡面板（淡红色背景 + 红色圆角边框）
            val errorColor = com.intellij.ui.JBColor(
                java.awt.Color(255, 235, 238),  // 浅色主题：淡红色
                java.awt.Color(80, 40, 40)      // 深色主题：深红色
            )
            
            val bubblePanel = JPanel(BorderLayout()).apply {
                background = errorColor
                isOpaque = false
                // 使用自定义圆角边框，带红色边线
                border = object : AbstractBorder() {
                    override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
                        if (g is Graphics2D) {
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                            // 填充背景
                            g.color = errorColor
                            val roundRect = RoundRectangle2D.Float(x.toFloat(), y.toFloat(), width - 1f, height - 1f, 12f, 12f)
                            g.fill(roundRect)
                            // 绘制红色边框
                            g.color = java.awt.Color(211, 47, 47)
                            g.draw(roundRect)
                        }
                    }
                    
                    override fun getBorderInsets(c: Component?) = JBUI.insets(10, 12, 10, 12)
                    
                    override fun isBorderOpaque() = true
                }
                add(label, BorderLayout.CENTER)
                maximumSize = java.awt.Dimension(600, Int.MAX_VALUE)
            }
            
            // 组合：标题 + 气泡（带左边距）
            val bubbleWithMargin = JPanel(BorderLayout()).apply {
                background = com.intellij.ui.JBColor.background()
                isOpaque = true
                border = JBUI.Borders.empty(0, 0, 0, 0)
                add(bubblePanel, BorderLayout.CENTER)
            }
            
            messageContainer.add(headerPanel, BorderLayout.NORTH)
            messageContainer.add(bubbleWithMargin, BorderLayout.CENTER)
            
            SwingUtilities.invokeLater {
                messagesPanel.add(messageContainer)
                messagesPanel.revalidate()
                messagesPanel.repaint()
                scrollToBottom()
            }
        }
        
        private fun scrollToBottom() {
            SwingUtilities.invokeLater {
                val verticalScrollBar = messagesScrollPane.verticalScrollBar
                verticalScrollBar.value = verticalScrollBar.maximum
            }
        }
        
        private fun clearChat() {
            chatHistory.clear()
            messagesPanel.removeAll()
            addMessage("assistant", "您好！我是 DeepSeek AI 助手，有什么可以帮助您的吗？")
            messagesPanel.revalidate()
            messagesPanel.repaint()
        }
    }
}
