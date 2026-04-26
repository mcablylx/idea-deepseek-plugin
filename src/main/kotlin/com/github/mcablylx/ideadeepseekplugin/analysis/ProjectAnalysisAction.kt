package com.github.mcablylx.ideadeepseekplugin.analysis

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
 * 项目分析动作
 * 右键项目或工具菜单中触发，分析整个项目结构并生成文档
 */
class ProjectAnalysisAction : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val project = e.project ?: return
        val projectDir = project.guessProjectDir()

        if (projectDir == null) {
            Messages.showWarningDialog(
                project,
                "无法获取项目目录",
                "项目分析"
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在分析项目结构...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "正在扫描项目文件..."
                    indicator.fraction = 0.1

                    // 收集项目信息
                    val projectInfo = collectProjectInfo(projectDir, indicator)

                    indicator.text = "正在生成分析文档..."
                    indicator.fraction = 0.5

                    val apiClient = ApplicationManager.getApplication().service<DeepSeekApiClient>()

                    val messages = listOf(
                        ChatMessage("system", """
                            你是一个专业的软件架构师和技术文档专家。请分析提供的项目结构并生成详细的技术文档。
                            
                            要求：
                            1. 生成项目概述
                            2. 分析目录结构
                            3. 识别主要模块和组件
                            4. 分析技术栈和依赖
                            5. 提供架构建议
                            6. 指出潜在的改进点
                            7. 使用Markdown格式
                            8. 文档应该清晰、结构化
                        """.trimIndent()),
                        ChatMessage("user", "请分析以下项目结构并生成技术文档：\n\n```\n$projectInfo\n```")
                    )

                    indicator.fraction = 0.7
                    indicator.text = "正在调用AI分析..."

                    val analysis = runBlocking {
                        apiClient.chatNonStream(messages)
                    }

                    indicator.fraction = 0.95
                    indicator.text = "正在保存文档..."

                    // 保存分析文档
                    ApplicationManager.getApplication().invokeLater {
                        saveAnalysisDocument(project, projectDir, analysis)
                    }

                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "项目分析失败：${e.message}",
                            "项目分析"
                        )
                    }
                }
            }
        })
    }

    /**
     * 收集项目信息
     */
    private fun collectProjectInfo(projectDir: VirtualFile, indicator: ProgressIndicator): String {
        val sb = StringBuilder()

        sb.append("项目名称: ${projectDir.name}\n\n")
        sb.append("目录结构:\n")
        sb.append("```\n")

        scanDirectory(projectDir, sb, "", 0, 3, indicator)

        sb.append("```\n\n")

        // 收集关键文件信息
        sb.append("\n关键文件:\n")
        collectKeyFiles(projectDir, sb, indicator)

        return sb.toString()
    }

    /**
     * 扫描目录结构
     */
    private fun scanDirectory(
        dir: VirtualFile,
        sb: StringBuilder,
        prefix: String,
        currentDepth: Int,
        maxDepth: Int,
        indicator: ProgressIndicator
    ) {
        if (currentDepth > maxDepth) return

        val children = dir.children.sortedBy { it.name }
        for ((index, child) in children.withIndex()) {
            indicator.checkCanceled()

            // 忽略隐藏文件和常见忽略目录
            if (child.name.startsWith(".") ||
                child.name in listOf("build", "target", "node_modules", ".gradle", ".idea")) {
                continue
            }

            val isLast = index == children.size - 1
            val connector = if (isLast) "└── " else "├── "

            sb.append("$prefix$connector${child.name}")

            if (child.isDirectory) {
                sb.append("/")
            }
            sb.append("\n")

            if (child.isDirectory) {
                val newPrefix = prefix + if (isLast) "    " else "│   "
                scanDirectory(child, sb, newPrefix, currentDepth + 1, maxDepth, indicator)
            }
        }
    }

    /**
     * 收集关键文件信息
     */
    private fun collectKeyFiles(projectDir: VirtualFile, sb: StringBuilder, indicator: ProgressIndicator) {
        val keyFiles = listOf(
            "build.gradle.kts", "build.gradle", "pom.xml", "package.json",
            "settings.gradle.kts", "settings.gradle",
            "README.md", "CHANGELOG.md",
            "src/main/AndroidManifest.xml"
        )

        for (fileName in keyFiles) {
            indicator.checkCanceled()

            val file = findFile(projectDir, fileName)
            if (file != null && file.exists()) {
                sb.append("- $fileName (${formatFileSize(file.length)})\n")
            }
        }

        // 统计代码文件数量
        val codeFiles = mutableListOf<String>()
        countCodeFiles(projectDir, codeFiles, indicator)

        sb.append("\n代码统计:\n")
        sb.append("- Kotlin文件: ${codeFiles.count { it.endsWith(".kt") }}\n")
        sb.append("- Java文件: ${codeFiles.count { it.endsWith(".java") }}\n")
        sb.append("- XML文件: ${codeFiles.count { it.endsWith(".xml") }}\n")
        sb.append("- 总文件数: ${codeFiles.size}\n")
    }

    /**
     * 查找文件
     */
    private fun findFile(dir: VirtualFile, path: String): VirtualFile? {
        val parts = path.split("/")
        var current: VirtualFile? = dir

        for (part in parts) {
            if (current == null) return null
            current = current.findChild(part)
        }

        return current
    }

    /**
     * 统计代码文件
     */
    private fun countCodeFiles(dir: VirtualFile, files: MutableList<String>, indicator: ProgressIndicator) {
        indicator.checkCanceled()

        if (dir.name in listOf("build", "target", "node_modules", ".gradle", ".idea")) {
            return
        }

        for (child in dir.children) {
            indicator.checkCanceled()

            if (child.isDirectory) {
                countCodeFiles(child, files, indicator)
            } else if (child.extension in listOf("kt", "java", "xml", "json", "yml", "yaml", "properties", "kts", "gradle")) {
                files.add(child.name)
            }
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    /**
     * 保存分析文档
     */
    private fun saveAnalysisDocument(project: Project, projectDir: VirtualFile, analysis: String) {
        try {
            val docPath = projectDir.path + "/PROJECT_ANALYSIS.md"
            val docFile = File(docPath)

            // 写入文档
            docFile.writeText(
                """
                # 项目分析报告

                生成时间: ${java.time.LocalDateTime.now()}

                ---

                $analysis
                """.trimIndent()
            )

            // 刷新并打开文档
            val virtualFile = projectDir.findChild("PROJECT_ANALYSIS.md")
            virtualFile?.refresh(false, false)

            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
            }

            Messages.showInfoMessage(
                project,
                "项目分析文档已生成：\n$docPath",
                "项目分析"
            )

        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "保存文档失败：${e.message}",
                "项目分析"
            )
        }
    }
}
