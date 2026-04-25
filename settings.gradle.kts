pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://jetbrains.bintray.com/intellij-plugin-service")
        maven("https://plugins.gradle.org/m2/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "idea-deepseek-plugin"