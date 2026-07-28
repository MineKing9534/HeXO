@file:OptIn(ExperimentalDistributionDsl::class)

import com.github.gmazzo.buildconfig.BuildConfigValue.Expression
import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import kotlinx.html.link
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl

plugins {
    id("kotlin-common")
    kotlin("multiplatform")

    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kobweb.application)

    id("tailwindcss")
}

repositories {
    google()
    mavenCentral()
}

kobweb {
    app {
        index {
            faviconPath = "/favicon.png"
            head.add {
                link(rel = "stylesheet", type = "text/css", href = basePath.prependTo("/styles.css"))
            }
        }
    }
}

kotlin {
    configAsKobwebApplication(includeServer = false)

    sourceSets.jsMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.board.render)
            implementation(projects.board.render.compose)

            implementation(projects.board.parse)
            implementation(projects.hds)

            implementation(projects.sync.client)

            implementation(libs.kobweb.core)
            implementation(libs.bundles.compose.html)
            implementation(libs.compose.html.svg)

            implementation(libs.kotlin.serialization.json)
        }

        resources.srcDir(layout.buildDirectory.dir("generated/resources"))
    }
}

tailwindcss {
    sourceSetName = "jsMain"
    resourcePath = "public"
}

val webApiProxy = providers.gradleProperty("web.apiProxy")
    .orElse(provider { "" })
    .map { Expression("\"$it\"") }

val webToolsApi = providers.gradleProperty("web.toolsApi")
    .orElse(provider { "" })
    .map { Expression("\"$it\"") }

buildConfig {
    buildConfigField<String>("API_PROXY", webApiProxy)
    buildConfigField<String>("TOOLS_API", webToolsApi)
}
