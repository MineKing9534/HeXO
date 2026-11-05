import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("dev.detekt")
}

repositories {
    mavenCentral()
}

dependencies {
    if (project.path != ":detekt-rules") {
        detektPlugins(project(":detekt-rules"))
    }
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:${detekt.toolVersion.get()}")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
    val generatedSources = layout.buildDirectory.dir("generated")
    // Generated directories can be source roots, so match absolute paths rather than relative patterns.
    exclude { it.file.toPath().startsWith(generatedSources.get().asFile.toPath()) }
}

// The plain task skips rules requiring type resolution; include compilation-specific analysis.
pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.getByType<KotlinJvmProjectExtension>().target.compilations.configureEach {
        val taskName = "detekt${name.replaceFirstChar { it.uppercase() }}"
        tasks.named("detekt") {
            dependsOn(taskName)
        }
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    detekt {
        val languages = listOf("common", "js", "jvm", "latex")
        val files = languages
            .flatMap { listOf("${it}Main", "${it}Test") }
            .map { "src/$it/kotlin" }

        source = files(files)
    }

    extensions.getByType<KotlinMultiplatformExtension>().targets
        .matching { it.platformType == KotlinPlatformType.jvm }
        .configureEach {
            val targetSuffix = name.replaceFirstChar { it.uppercase() }
            compilations.configureEach {
                val taskName = "detekt${name.replaceFirstChar { it.uppercase() }}$targetSuffix"
                tasks.named("detekt") {
                    dependsOn(taskName)
                }
            }
        }
}
