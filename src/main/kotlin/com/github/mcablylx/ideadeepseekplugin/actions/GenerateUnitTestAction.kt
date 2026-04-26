package com.github.mcablylx.ideadeepseekplugin.actions

import com.github.mcablylx.ideadeepseekplugin.client.ChatMessage
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiClient
import com.github.mcablylx.ideadeepseekplugin.util.extractCodeFromResponse
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.NotNull
import java.io.File

/**
 * 生成单元测试动作
 * 右键菜单中选择"🧪 生成单元测试"
 */
class GenerateUnitTestAction : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectionModel = editor.selectionModel

        // 获取当前文件
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile == null || !virtualFile.extension.equals("kt", ignoreCase = true) &&
            !virtualFile.extension.equals("java", ignoreCase = true)) {
            Messages.showWarningDialog(
                project,
                "当前文件不是Kotlin或Java文件",
                "生成单元测试"
            )
            return
        }

        val selectedCode = if (selectionModel.hasSelection()) {
            selectionModel.selectedText
        } else {
            editor.document.text
        }

        val fileName = virtualFile.name

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在生成单元测试...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "正在分析代码..."
                    indicator.fraction = 0.1

                    val apiClient = ApplicationManager.getApplication().service<DeepSeekApiClient>()

                    val testFramework = if (virtualFile.extension == "kt") {
                        "JUnit 5 + Kotest"
                    } else {
                        "JUnit 5"
                    }

                    val messages = listOf(
                        ChatMessage("system", """
                            你是一个专业的单元测试专家。请为提供的代码生成完整的单元测试。
                            
                            要求：
                            1. 使用 $testFramework 框架
                            2. 覆盖正常流程、边界情况、异常情况
                            3. 包含 Mock 数据（如需要）
                            4. 测试方法命名清晰，使用 should_xxx 格式
                            5. 添加必要的注释说明测试意图
                            6. 测试代码应该可以直接运行
                            7. 只返回测试代码，不要其他解释
                            8. 使用代码块格式包裹
                        """.trimIndent()),
                        ChatMessage("user", "请为以下代码生成单元测试（文件名：$fileName）：\n\n```\n$selectedCode\n```")
                    )

                    indicator.text = "正在生成测试代码..."
                    indicator.fraction = 0.6

                    val aiResponse = runBlocking {
                        apiClient.chatNonStream(messages)
                    }

                    indicator.fraction = 0.9

                    // 提取测试代码
                    val testCode = extractCodeFromResponse(aiResponse)

                    if (testCode.isBlank()) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "AI未返回有效的测试代码",
                                "生成单元测试"
                            )
                        }
                        return
                    }

                    // 创建测试文件
                    ApplicationManager.getApplication().invokeLater {
                        createTestFile(project, virtualFile, testCode)
                    }

                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "生成失败：${e.message}",
                            "生成单元测试"
                        )
                    }
                }
            }
        })
    }

    /**
     * 创建测试文件并打开
     */
    private fun createTestFile(project: Project, sourceFile: VirtualFile, testCode: String) {
        try {
            // 推断测试文件路径
            val projectDir = project.guessProjectDir()
            if (projectDir == null) {
                Messages.showErrorDialog(
                    project,
                    "无法获取项目目录",
                    "生成单元测试"
                )
                return
            }

            val sourcePath = sourceFile.path
            val isKotlin = sourceFile.extension.equals("kt", ignoreCase = true)

            // 将 src/main/kotlin 替换为 src/test/kotlin
            val testPath = if (isKotlin) {
                sourcePath.replace("/src/main/kotlin/", "/src/test/kotlin/")
                    .replace("/src/main/java/", "/src/test/kotlin/")
            } else {
                sourcePath.replace("/src/main/java/", "/src/test/java/")
                    .replace("/src/main/kotlin/", "/src/test/java/")
            }

            val testFileName = if (isKotlin) {
                sourceFile.nameWithoutExtension + "Test.kt"
            } else {
                sourceFile.nameWithoutExtension + "Test.java"
            }

            val testFile = File(testPath.replace(sourceFile.name, testFileName))

            // 创建目录（如果不存在）
            testFile.parentFile.mkdirs()

            // 写入测试代码
            testFile.writeText(testCode)

            // 刷新并打开文件
            val testVirtualFile = projectDir.findFileByRelativePath(
                testFile.absolutePath.removePrefix(projectDir.path).removePrefix("/")
            )

            if (testVirtualFile != null) {
                testVirtualFile.refresh(false, false)
                FileEditorManager.getInstance(project).openFile(testVirtualFile, true)

                Messages.showInfoMessage(
                    project,
                    "测试文件已生成：\n${testFile.absolutePath}",
                    "生成单元测试"
                )
            } else {
                Messages.showInfoMessage(
                    project,
                    "测试文件已生成，请手动刷新项目：\n${testFile.absolutePath}",
                    "生成单元测试"
                )
            }

        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "创建测试文件失败：${e.message}",
                "生成单元测试"
            )
        }
    }

}
