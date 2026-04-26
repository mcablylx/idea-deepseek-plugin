package com.github.mcablylx.ideadeepseekplugin.actions

import com.github.mcablylx.ideadeepseekplugin.client.ChatMessage
import com.github.mcablylx.ideadeepseekplugin.client.DeepSeekApiClient
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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
 * 依赖建议动作
 * 分析项目依赖并提供优化建议
 */
class DependencySuggestionAction : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val projectDir = project.guessProjectDir()

        if (projectDir == null) {
            Messages.showWarningDialog(
                project,
                "无法获取项目目录",
                "依赖建议"
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在分析项目依赖...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "正在收集依赖信息..."
                    indicator.fraction = 0.1

                    // 收集依赖配置文件
                    val dependencyInfo = collectDependencyInfo(projectDir, indicator)

                    if (dependencyInfo.isBlank()) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(
                                project,
                                "未找到依赖配置文件（build.gradle.kts, build.gradle, pom.xml）",
                                "依赖建议"
                            )
                        }
                        return
                    }

                    indicator.text = "正在分析依赖..."
                    indicator.fraction = 0.4

                    val apiClient = ApplicationManager.getApplication().service<DeepSeekApiClient>()

                    val messages = listOf(
                        ChatMessage("system", """
                            你是一个依赖管理专家。请分析项目的依赖配置并提供优化建议。
                            
                            要求：
                            1. 识别过时的依赖版本
                            2. 推荐更新的稳定版本
                            3. 发现潜在的依赖冲突
                            4. 建议缺失的常用依赖
                            5. 指出未使用的依赖
                            6. 提供安全性建议
                            7. 使用Markdown格式输出
                            8. 给出具体的依赖声明代码
                        """.trimIndent()),
                        ChatMessage("user", "请分析以下项目依赖并提供优化建议：\n\n```\n$dependencyInfo\n```")
                    )

                    indicator.fraction = 0.6
                    indicator.text = "正在生成建议..."

                    val suggestions = runBlocking {
                        apiClient.chatNonStream(messages)
                    }

                    indicator.fraction = 0.95
                    indicator.text = "正在保存建议..."

                    // 保存建议文档
                    ApplicationManager.getApplication().invokeLater {
                        saveDependencySuggestions(project, projectDir, suggestions)
                    }

                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "依赖分析失败：${e.message}",
                            "依赖建议"
                        )
                    }
                }
            }
        })
    }

    /**
     * 收集依赖信息
     */
    private fun collectDependencyInfo(projectDir: VirtualFile, indicator: ProgressIndicator): String {
        val sb = StringBuilder()

        // 查找并读取构建文件
        val buildFiles = listOf(
            "build.gradle.kts",
            "build.gradle",
            "pom.xml",
            "settings.gradle.kts",
            "settings.gradle",
            "package.json"
        )

        for (fileName in buildFiles) {
            indicator.checkCanceled()

            val file = projectDir.findChild(fileName)
            if (file != null && file.exists()) {
                sb.append("### $fileName\n\n")
                sb.append("```\n")
                sb.append(file.inputStream.reader().readText())
                sb.append("\n```\n\n")
            }
        }

        // 查找子模块的构建文件
        findSubmoduleBuildFiles(projectDir, sb, indicator)

        return sb.toString()
    }

    /**
     * 查找子模块的构建文件
     */
    private fun findSubmoduleBuildFiles(
        dir: VirtualFile,
        sb: StringBuilder,
        indicator: ProgressIndicator,
        depth: Int = 0
    ) {
        if (depth > 2) return // 最多查找2层

        for (child in dir.children) {
            indicator.checkCanceled()

            if (child.isDirectory && child.name !in listOf("build", "target", "node_modules", ".gradle", ".idea")) {
                // 检查是否有构建文件
                val buildFile = child.findChild("build.gradle.kts")
                    ?: child.findChild("build.gradle")
                    ?: child.findChild("pom.xml")

                if (buildFile != null && buildFile.exists()) {
                    sb.append("### ${child.name}/${buildFile.name}\n\n")
                    sb.append("```\n")
                    sb.append(buildFile.inputStream.reader().readText())
                    sb.append("\n```\n\n")
                }

                // 递归查找
                findSubmoduleBuildFiles(child, sb, indicator, depth + 1)
            }
        }
    }

    /**
     * 保存依赖建议
     */
    private fun saveDependencySuggestions(project: Project, projectDir: VirtualFile, suggestions: String) {
        try {
            val docPath = projectDir.path + "/DEPENDENCY_SUGGESTIONS.md"
            val docFile = File(docPath)

            // 写入建议
            docFile.writeText(
                """
                # 依赖优化建议

                生成时间: ${java.time.LocalDateTime.now()}

                ---

                $suggestions

                ---

                ## 注意事项

                1. 在更新依赖版本前，请先备份项目
                2. 建议逐个更新依赖并运行测试
                3. 注意检查 breaking changes
                4. 更新后务必进行完整的功能测试
                """.trimIndent()
            )

            // 刷新并打开文档
            val virtualFile = projectDir.findChild("DEPENDENCY_SUGGESTIONS.md")
            virtualFile?.refresh(false, false)

            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }

            Messages.showInfoMessage(
                project,
                "依赖建议已生成：\n$docPath",
                "依赖建议"
            )

        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "保存建议失败：${e.message}",
                "依赖建议"
            )
        }
    }
}
