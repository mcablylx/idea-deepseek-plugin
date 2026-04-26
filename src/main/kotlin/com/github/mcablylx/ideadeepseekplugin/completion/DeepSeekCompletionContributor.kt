package com.github.mcablylx.ideadeepseekplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import kotlinx.coroutines.runBlocking
import javax.swing.Icon

/**
 * DeepSeek 代码补全贡献者
 * 集成到 IDEA 的代码补全框架中
 */
class DeepSeekCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.editor.project ?: return
                    
                    // 在后台线程中获取补全建议
                    val completionService = project.service<DeepSeekCompletionService>()
                    
                    try {
                        // 使用同步方式获取补全（IDEA补全框架要求）
                        val completion = runBlocking {
                            completionService.getInlineCompletion(
                                parameters.editor,
                                parameters.offset
                            )
                        }
                        
                        if (!completion.isNullOrBlank()) {
                            // 创建补全项
                            val lookupElement = LookupElementBuilder.create(completion)
                                .withPresentableText("DeepSeek AI")
                                .withTypeText("AI 补全", true)
                                .withBoldness(true)
                                .withInsertHandler { ctx, _ ->
                                    // 插入补全内容
                                    val document = ctx.document
                                    val startOffset = ctx.startOffset
                                    document.replaceString(
                                        startOffset,
                                        ctx.selectionEndOffset,
                                        completion
                                    )
                                }
                            
                            result.addElement(lookupElement)
                        }
                    } catch (e: Exception) {
                        thisLogger().debug("DeepSeek 补全失败: ${e.message}")
                    }
                }
            }
        )
    }
}
