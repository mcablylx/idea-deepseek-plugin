package com.github.mcablylx.ideadeepseekplugin.client

/**
 * DeepSeek API 异常类
 * 用于封装 API 返回的错误信息
 */
sealed class DeepSeekApiException(message: String) : Exception(message) {
    
    /**
     * 余额不足异常
     */
    class InsufficientBalance(message: String = "账户余额不足，请充值后再使用") : DeepSeekApiException(message)
    
    /**
     * API Key 无效异常
     */
    class InvalidApiKey(message: String = "API Key 无效，请检查设置") : DeepSeekApiException(message)
    
    /**
     * 请求限制异常
     */
    class RateLimitExceeded(message: String = "请求过于频繁，请稍后再试") : DeepSeekApiException(message)
    
    /**
     * 通用 API 错误
     */
    class GeneralError(message: String) : DeepSeekApiException(message)
}

/**
 * 解析 API 错误响应
 */
fun parseErrorResponse(errorResponse: String, responseCode: Int): DeepSeekApiException {
    return try {
        val gson = com.google.gson.Gson()
        val json = gson.fromJson(errorResponse, com.google.gson.JsonObject::class.java)
        val errorObj = json.getAsJsonObject("error")
        
        if (errorObj != null) {
            val message = errorObj.get("message")?.asString ?: ""
            val code = errorObj.get("code")?.asString ?: ""
            
            // 检测余额不足
            if (message.contains("Insufficient Balance", ignoreCase = true) ||
                message.contains("insufficient", ignoreCase = true) ||
                code.contains("insufficient")) {
                return DeepSeekApiException.InsufficientBalance()
            }
            
            // 检测 API Key 无效
            if (message.contains("invalid_api_key", ignoreCase = true) ||
                message.contains("Invalid API Key", ignoreCase = true) ||
                responseCode == 401) {
                return DeepSeekApiException.InvalidApiKey(message)
            }
            
            // 检测请求限制
            if (responseCode == 429 || 
                message.contains("rate limit", ignoreCase = true) ||
                code.contains("rate_limit")) {
                return DeepSeekApiException.RateLimitExceeded(message)
            }
            
            DeepSeekApiException.GeneralError(message)
        } else {
            DeepSeekApiException.GeneralError("API 请求失败: $responseCode")
        }
    } catch (e: Exception) {
        DeepSeekApiException.GeneralError("API 请求失败: $responseCode - ${e.message}")
    }
}
