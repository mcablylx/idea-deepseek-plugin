# DeepSeek AI Assistant 使用指南

## 快速开始

### 1. 获取 DeepSeek API Key

1. 访问 [DeepSeek 开放平台](https://platform.deepseek.com)
2. 注册或登录账号
3. 在控制台中创建 API Key
4. 复制您的 API Key

### 2. 配置插件

1. 打开 IDEA 设置
   - Windows/Linux: `File -> Settings`
   - macOS: `IntelliJ IDEA -> Preferences`

2. 导航到 `Tools -> DeepSeek AI`

3. 填写配置信息：
   - **API Key**: 必填，从 DeepSeek 平台获取
   - **API Base URL**: 默认 `https://api.deepseek.com`
   - **Model**: 选择要使用的模型（下拉框）
     - `deepseek-v4-flash` - 快速响应，适合日常使用（推荐）
     - `deepseek-v4-pro` - 高质量回答，适合复杂任务（推荐）
     - `deepseek-chat` - 通用对话模型（⚠️ 2026/07/24 弃用）
     - `deepseek-reasoner` - 推理专用模型（⚠️ 2026/07/24 弃用）
   - **Temperature**: 0.0-1.0，控制回复的创造性（默认 0.7）
   - **Max Tokens**: 最大回复长度（默认 4096）
   - **System Prompt**: 系统提示词，定义 AI 的角色和行为

4. 点击 `Apply` 保存设置

### 3. 开始使用

1. 在 IDEA 右侧工具栏找到 `DeepSeek AI` 图标，点击打开
2. 在输入框中输入您的问题
3. 按 `Enter` 键或点击 `发送` 按钮
4. AI 会实时显示回复内容
5. 点击 `清空` 可以开始新的对话

## 使用场景

### 💡 代码解释
```
请解释一下这段代码的作用：
[list code snippet]
```

### 🐛 调试帮助
```
我的代码出现了以下错误，请帮我分析原因：
[error message]
```

### 🚀 代码优化
```
如何优化这段代码的性能？
[code snippet]
```

### 📝 代码生成
```
请用 Kotlin 写一个单例模式的实现
```

### 🎯 最佳实践
```
在 IntelliJ 插件开发中，如何处理后台任务？
```

## 高级技巧

### 1. 使用上下文
- 可以复制粘贴代码到对话框中
- 提供详细的错误信息和堆栈跟踪
- 描述您期望的行为

### 2. 多轮对话
- 插件支持对话历史
- 可以针对 AI 的回复继续提问
- 点击"清空"开始新话题

### 3. 模型选择

插件支持以下四种模型：

- **deepseek-v4-flash** ⭐推荐
  - 响应速度快
  - 适合日常对话和简单任务
  - 成本低，性价比高
  
- **deepseek-v4-pro** ⭐推荐
  - 回答质量更高
  - 适合复杂问题和专业任务
  - 代码能力和推理能力更强
  
- **deepseek-chat**
  - 通用对话模型
  - ⚠️ 将于 2026/07/24 弃用
  - 建议迁移到 v4 系列模型
  
- **deepseek-reasoner**
  - 推理专用模型
  - 擅长逻辑推理和数学计算
  - ⚠️ 将于 2026/07/24 弃用
  - 建议迁移到 v4 系列模型

### 4. 参数调优
- **Temperature**: 
  - 0.0-0.3: 更确定、保守的回答
  - 0.4-0.7: 平衡创造性和准确性（推荐）
  - 0.8-1.0: 更有创造性，但可能不准确
  
- **Max Tokens**: 
  - 简单问题: 1024-2048
  - 复杂问题: 4096-8192

## 常见问题

### Q: API Key 在哪里获取？
A: 访问 [DeepSeek 开放平台](https://platform.deepseek.com) 注册后即可获取。

### Q: 支持哪些模型？
A: 目前支持 `deepseek-chat` 和 `deepseek-coder`，后续会支持更多模型。

### Q: 如何查看 API 使用量？
A: 登录 DeepSeek 平台的控制台即可查看使用情况和计费信息。

### Q: 响应速度慢怎么办？
A: 这取决于网络和 API 服务器负载，可以尝试：
- 检查网络连接
- 减少 Max Tokens 设置
- 使用更简洁的提问方式

### Q: 是否支持代码补全？
A: 当前版本主要提供对话功能，代码补全功能正在开发中。

## 技术支持

- 提交问题: [GitHub Issues](https://github.com/mcablylx/idea-deepseek-plugin/issues)
- 查看文档: [README](./README.md)
- 获取更新: [Releases](https://github.com/mcablylx/idea-deepseek-plugin/releases)

## 更新日志

查看 [CHANGELOG.md](./CHANGELOG.md) 了解最新版本更新。
