package com.github.mcablylx.ideadeepseekplugin.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

/**
 * DeepSeek 设置页面
 * 在 IDE 设置中显示配置界面
 */
class DeepSeekSettingsConfigurable : Configurable {

    private var settings: DeepSeekSettings = DeepSeekSettings.instance
    private lateinit var apiKeyField: JBTextField
    private lateinit var apiBaseUrlField: JBTextField
    private lateinit var modelComboBox: ComboBox<String>
    private lateinit var temperatureField: JBTextField
    private lateinit var maxTokensField: JBTextField
    private lateinit var systemPromptField: JBTextField
    
    // 可用模型列表
    private val availableModels = arrayOf(
        "deepseek-v4-flash",
        "deepseek-v4-pro",
        "deepseek-chat",
        "deepseek-reasoner"
    )

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String = "DeepSeek AI"

    override fun createComponent(): JComponent {
        apiKeyField = JBTextField(settings.apiKey, 40)
        apiBaseUrlField = JBTextField(settings.apiBaseUrl, 40)
        
        // 初始化模型下拉框
        modelComboBox = ComboBox(availableModels)
        modelComboBox.selectedItem = settings.modelName
        
        temperatureField = JBTextField(settings.temperature.toString(), 10)
        maxTokensField = JBTextField(settings.maxTokens.toString(), 10)
        systemPromptField = JBTextField(settings.systemPrompt, 40)

        val panel: DialogPanel = panel {
            row("API Key:") {
                cell(apiKeyField)
                comment("从 DeepSeek 平台获取的 API Key")
            }
            
            row("API Base URL:") {
                cell(apiBaseUrlField)
                comment("DeepSeek API 基础 URL")
            }
            
            row("Model:") {
                cell(modelComboBox)
                comment("选择要使用的 DeepSeek 模型")
            }
            
            row("Temperature:") {
                cell(temperatureField)
                comment("控制回复的随机性 (0.0-1.0)")
            }
            
            row("Max Tokens:") {
                cell(maxTokensField)
                comment("最大回复 token 数")
            }
            
            row("System Prompt:") {
                cell(systemPromptField)
                comment("系统提示词")
            }
            
            row {
                comment("⚠️ 注意: deepseek-chat 和 deepseek-reasoner 将于 2026/07/24 弃用，建议使用 deepseek-v4-flash 或 deepseek-v4-pro")
            }
        }

        return panel
    }

    override fun isModified(): Boolean {
        return settings.apiKey != apiKeyField.text.trim() ||
               settings.apiBaseUrl != apiBaseUrlField.text.trim() ||
               settings.modelName != (modelComboBox.selectedItem as? String ?: "") ||
               settings.temperature != temperatureField.text.toDoubleOrNull() ?: 0.7 ||
               settings.maxTokens != maxTokensField.text.toIntOrNull() ?: 4096 ||
               settings.systemPrompt != systemPromptField.text.trim()
    }

    override fun apply() {
        settings.apiKey = apiKeyField.text.trim()
        settings.apiBaseUrl = apiBaseUrlField.text.trim()
        settings.modelName = (modelComboBox.selectedItem as? String ?: "deepseek-v4-flash").trim()
        settings.temperature = temperatureField.text.toDoubleOrNull() ?: 0.7
        settings.maxTokens = maxTokensField.text.toIntOrNull() ?: 4096
        settings.systemPrompt = systemPromptField.text.trim()
    }

    override fun reset() {
        apiKeyField.text = settings.apiKey
        apiBaseUrlField.text = settings.apiBaseUrl
        modelComboBox.selectedItem = settings.modelName
        temperatureField.text = settings.temperature.toString()
        maxTokensField.text = settings.maxTokens.toString()
        systemPromptField.text = settings.systemPrompt
    }
}
