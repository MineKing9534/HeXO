import com.github.gmazzo.buildconfig.BuildConfigValue.Expression
import com.varabyte.kobweb.gradle.application.util.configAsKobwebApplication
import kotlinx.html.link
import kotlinx.html.unsafe

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
                unsafe {
                    raw("<!-- HEXO_OPEN_GRAPH -->")
                }
            }
        }
        export {
            addExtraRoute("/games/export", exportPath = "games/dynamic.html")
            addExtraRoute("/sessions/export", exportPath = "sessions/dynamic.html")
            addExtraRoute("/watchparty/export", exportPath = "watchparty/dynamic.html")
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
            implementation(projects.game.hds)
            implementation(projects.discord.oauth2.client)

            implementation(projects.watchparty.client)
            implementation(projects.solver)
            implementation(projects.web.analysisWorker)

            implementation(projects.utils.types)

            implementation(libs.kobweb.core)
            implementation(libs.bundles.compose.html)
            implementation(libs.compose.html.svg)

            implementation(libs.kotlin.serialization.json)

            implementation(libs.logging)
        }

        resources.srcDir(layout.buildDirectory.dir("generated/resources"))
    }
}

tailwindcss {
    sourceSetName = "jsMain"
    resourcePath = "public"
}

val webHdsApiUrl = providers.gradleProperty("web.hdsApiUrl")
    .orElse(provider { "" })

val webHmdApiUrl = providers.gradleProperty("web.hmdApiUrl")
    .orElse(provider { "" })

buildConfig {
    buildConfigField<String>("HDS_API_URL", webHdsApiUrl.map { Expression("\"$it\"") })
    buildConfigField<String>("HMD_API_URL", webHmdApiUrl.map { Expression("\"$it\"") })
}

tasks.matching {
    it.name in setOf("compileKotlinJs", "compileDevelopmentExecutableKotlinJs", "compileProductionExecutableKotlinJs")
}.configureEach {
    inputs.property("web.hdsApiUrl", webHdsApiUrl)
    inputs.property("web.hmdApiUrl", webHmdApiUrl)
}
