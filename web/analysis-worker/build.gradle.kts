import com.varabyte.kobweb.gradle.worker.util.configAsKobwebWorker

plugins {
    id("kotlin-common")
    kotlin("multiplatform")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kobweb.worker)
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    configAsKobwebWorker("analysis-worker")

    sourceSets.jsMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.solver)

            api(libs.kotlin.serialization.json)
            implementation(libs.kotlin.coroutines.core)
            implementation(libs.kobweb.core)
            implementation(libs.kobweb.worker)
            implementation(libs.kobwebx.serialization.kotlinx)
        }
    }
}

tasks.named<Sync>("kobwebCopyWorkerJsOutput") {
    include("*.wasm")
}

val patchGeneratedWorkerBasePath = tasks.register("patchGeneratedWorkerBasePath") {
    val generatedWorker = layout.buildDirectory.file(
        "generated/ksp/js/jsMain/kotlin/de/mineking/hexo/web/worker/AnalysisWorker.kt",
    )

    inputs.file(generatedWorker)
    outputs.file(generatedWorker)
    dependsOn("kspKotlinJs")

    doLast {
        val file = generatedWorker.get().asFile
        val patched = file.readText()
            .replace(
                "Worker(\"/_kobweb/workers/de-mineking-hexo-web-analysis-worker/analysis-worker.js\")",
                "Worker(\"/app/_kobweb/workers/de-mineking-hexo-web-analysis-worker/analysis-worker.js\")",
            )
        file.writeText(patched)
    }
}

tasks.named("compileKotlinJs") {
    dependsOn(patchGeneratedWorkerBasePath)
}
