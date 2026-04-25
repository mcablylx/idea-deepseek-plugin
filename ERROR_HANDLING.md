# 错误处理功能说明

## 功能概述

插件现已支持智能错误检测和友好的错误提示，能够识别 DeepSeek API 返回的各种错误，并在界面上以清晰的方式展示给用户。

## 支持的错误类型

### 1. 💰 余额不足 (Insufficient Balance)

**触发条件：**
- API 返回 `"Insufficient Balance"` 错误
- 账户余额不足以支付 API 调用费用

**错误提示：**
```
💰 余额不足
账户余额不足，请充值后再使用

请前往 DeepSeek 平台充值：
https://platform.deepseek.com
```

**界面效果：**
- 红色背景高亮显示
- 提供充值链接
- 图标提示

### 2. 🔑 API Key 无效 (Invalid API Key)

**触发条件：**
- API Key 格式错误
- API Key 已过期或被禁用
- HTTP 状态码 401

**错误提示：**
```
🔑 API Key 无效
API Key 无效，请检查设置

请在设置中检查您的 API Key 配置。
```

### 3. ⏱️ 请求过于频繁 (Rate Limit Exceeded)

**触发条件：**
- 短时间内发送过多请求
- 超过 API 调用频率限制
- HTTP 状态码 429

**错误提示：**
```
⏱️ 请求过于频繁
请求过于频繁，请稍后再试

请稍等片刻后再试。
```

### 4. ❌ 通用 API 错误

**触发条件：**
- 其他未分类的 API 错误
- 网络问题
- 服务器错误

**错误提示：**
```
❌ API 错误
[具体错误信息]
```

## 技术实现

### 1. 异常类层次结构

```kotlin
sealed class DeepSeekApiException(message: String) : Exception(message) {
    class InsufficientBalance(message: String)
    class InvalidApiKey(message: String)
    class RateLimitExceeded(message: String)
    class GeneralError(message: String)
}
```

### 2. 错误解析逻辑

- 自动解析 JSON 错误响应
- 识别错误消息中的关键字
- 根据 HTTP 状态码判断错误类型
- 提供友好的中文错误提示

### 3. UI 显示优化

- 错误消息使用红色背景高亮
- 使用图标增强视觉识别
- 提供解决建议和操作指引
- 支持链接点击（如充值链接）

## 使用示例

### 场景 1：余额不足

1. 用户在聊天窗口发送消息
2. API 返回 `{"error":{"message":"Insufficient Balance","type":"unknown_error"}}`
3. 插件检测到余额不足错误
4. 界面显示红色错误提示，包含充值链接

### 场景 2：API Key 配置错误

1. 用户输入了错误的 API Key
2. API 返回 401 状态码
3. 插件提示 API Key 无效
4. 引导用户检查设置

## 错误处理流程

```
用户发送消息
    ↓
调用 DeepSeek API
    ↓
检测 HTTP 响应状态码
    ↓
如果是错误 (非 200)
    ↓
解析错误响应 JSON
    ↓
识别错误类型
    ↓
抛出对应的异常
    ↓
UI 层捕获异常
    ↓
显示友好的错误提示
```

## 未来优化方向

- [ ] 添加错误日志记录
- [ ] 支持错误报告提交
- [ ] 添加重试机制
- [ ] 提供离线模式提示
- [ ] 显示 API 使用统计

## 相关文件

- `DeepSeekApiException.kt` - 异常类定义和错误解析
- `DeepSeekApiClient.kt` - API 客户端错误处理
- `MyToolWindowFactory.kt` - UI 错误显示
