import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.20" // 使用稍新的 Kotlin 版本
    id("org.jetbrains.intellij.platform") version "2.15.0" // 升级到最新稳定版
    id("org.jetbrains.intellij.platform.migration") version "2.2.1" // 添加迁移插件
    id("org.jetbrains.changelog") version "2.2.1"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // 核心依赖: 使用 create 方法来指定目标 IDE 和版本
    intellijPlatform {
        create("IC", "2024.3.1") // 'IC' 代表 IntelliJ IDEA Community Edition
        testFramework(TestFrameworkType.Platform)
    }

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog
        changeNotes = version.map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    org.jetbrains.changelog.Changelog.OutputType.HTML,
                )
            }
        }
    }
}

changelog {
    groups.empty()
    versionPrefix.set("")
}