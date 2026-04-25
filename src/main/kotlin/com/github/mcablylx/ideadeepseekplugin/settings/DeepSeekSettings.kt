package com.github.mcablylx.ideadeepseekplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * DeepSeek API 配置类
 * 用于持久化存储用户的 API 配置信息
 */
@State(
    name = "com.github.mcablylx.ideadeepseekplugin.settings.DeepSeekSettings",
    storages = [Storage("deepseek-settings.xml")]
)
class DeepSeekSettings : PersistentStateComponent<DeepSeekSettings> {

    var apiKey: String = ""
    var apiBaseUrl: String = "https://api.deepseek.com"
    var modelName: String = "deepseek-v4-flash"  // 默认使用 v4-flash
    var temperature: Double = 0.7
    var maxTokens: Int = 4096
    var systemPrompt: String = "你是一个专业的编程助手，帮助开发者解决问题。"

    override fun getState(): DeepSeekSettings = this

    override fun loadState(state: DeepSeekSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        val instance: DeepSeekSettings
            get() = ApplicationManager.getApplication().getService(DeepSeekSettings::class.java)
    }
}
