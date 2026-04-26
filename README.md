# DeepSeek AI Assistant

![构建状态](https://github.com/mcablylx/idea-deepseek-plugin/workflows/Build/badge.svg)
[![版本](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![下载量](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## 功能介绍

这是一个强大的 IntelliJ IDEA 插件，集成了 DeepSeek AI 助手，类似于 Qoder 的功能。它可以帮助您：

### 核心功能
- 💬 **智能对话** - 与 DeepSeek AI 进行实时对话，获取编程帮助
- 🤖 **智能代码补全** - 上下文感知的行内代码补全（`Ctrl+Space`）
- 📝 **代码生成** - 自然语言转代码，一键插入编辑器
- 🔍 **代码解释** - 右键菜单快速解释代码逻辑
- 🔧 **重构建议** - 智能代码优化和最佳实践建议
- 🔄 **AI差异对比** - 可视化对比原始代码和AI优化代码
- 🧪 **测试生成** - 自动生成完整的单元测试用例
- 📊 **项目分析** - 分析整个项目结构并生成技术文档
- 📦 **依赖建议** - 智能分析依赖并提供优化建议
- ⚡ **流式响应** - 实时显示 AI 回复，无需等待
## 使用说明

### 1. 配置 API Key

1. 打开 IDEA 设置：`File -> Settings` (Windows/Linux) 或 `IntelliJ IDEA -> Preferences` (macOS)
2. 导航到 `Tools -> DeepSeek AI`
3. 填入您的 DeepSeek API Key（从 [DeepSeek 平台](https://platform.deepseek.com) 获取）
4. 可选：配置 API Base URL、模型名称、温度等参数
5. 点击 `Apply` 保存设置

### 2. 使用 AI 助手

1. 在 IDEA 右侧找到 `DeepSeek AI` 工具窗口，点击打开
2. 在输入框中输入您的问题或代码需求，按回车或点击“发送”按钮
3. AI 会实时流式显示回复内容，包括生成的代码
4. 点击“清空”按钮可以清除聊天记录

<!-- Plugin description -->
DeepSeek AI Assistant 是一款强大的 IntelliJ IDEA 插件，集成了 DeepSeek 的人工智能能力。

### 主要功能：
- 💬 智能对话：与 AI 进行实时对话，获取编程帮助
- 🤖 代码补全：上下文感知的智能代码补全
- 📝 代码生成：对话中直接生成代码（描述需求即可）
- 🔍 代码解释：快速理解复杂代码逻辑
- 🔧 重构建议：智能代码优化和最佳实践
- 🔄 AI差异对比：可视化对比代码修改
- 🧪 测试生成：自动生成完整的单元测试
- 📊 项目分析：分析项目结构并生成文档
- 📦 依赖建议：智能分析和优化依赖
- ⚡ 流式响应：实时显示 AI 回复

### 如何使用：
1. 在设置中配置 DeepSeek API Key
2. 打开右侧的 DeepSeek AI 工具窗口
3. 直接输入问题或代码需求，AI 会智能回答或生成代码
<!-- Plugin description end -->

## 安装方式

- 使用 IDE 内置插件系统：

  <kbd>设置/首选项</kbd> > <kbd>插件</kbd> > <kbd>市场</kbd> > <kbd>搜索 "idea-deepseek-plugin"</kbd> >
  <kbd>安装</kbd>

- 通过 JetBrains Marketplace：

  访问 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)，如果您的 IDE 正在运行，点击 <kbd>安装到 ...</kbd> 按钮进行安装。

  您也可以从 JetBrains Marketplace 下载[最新版本](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions)，然后通过
  <kbd>设置/首选项</kbd> > <kbd>插件</kbd> > <kbd>⚙️</kbd> > <kbd>从磁盘安装插件...</kbd> 手动安装。

- 手动安装：

  从 [GitHub 发布页](https://github.com/mcablylx/idea-deepseek-plugin/releases/latest) 下载[最新版本](https://github.com/mcablylx/idea-deepseek-plugin/releases/latest)，然后通过
  <kbd>设置/首选项</kbd> > <kbd>插件</kbd> > <kbd>⚙️</kbd> > <kbd>从磁盘安装插件...</kbd> 手动安装。


---
本插件基于 [IntelliJ Platform Plugin 模板][template]。

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation