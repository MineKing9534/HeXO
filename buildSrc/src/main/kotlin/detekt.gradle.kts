plugins {
    id("dev.detekt")
}

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:${detekt.toolVersion.get()}")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt.yml")
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    detekt {
        val languages = listOf("common", "js", "jvm", "latex")
        val files = languages
            .flatMap { listOf("${it}Main", "${it}Test") }
            .map { "src/$it/kotlin" }

        source = files(files)
    }
}
