import com.varabyte.kobweb.gradle.worker.util.configAsKobwebWorker
import com.varabyte.kobweb.project.KobwebFolder
import com.varabyte.kobweb.project.conf.KobwebConfFile
import org.gradle.api.GradleException

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

val kobwebFolder = KobwebFolder.fromChildPath(layout.projectDirectory.asFile.toPath())
    ?: throw GradleException("Unable to locate the parent Kobweb project")

val kobwebConfig = KobwebConfFile(kobwebFolder)
val kobwebBasePath = kobwebConfig.content?.site?.basePath
    ?: throw GradleException("Unable to read ${kobwebConfig.path}", kobwebConfig.deserializationException)

val patchGeneratedWorkerBasePath = tasks.register("patchGeneratedWorkerBasePath") {
    val generatedWorker = layout.buildDirectory.file(
        "generated/ksp/js/jsMain/kotlin/de/mineking/hexo/web/worker/AnalysisWorker.kt",
    )

    inputs.file(generatedWorker)
    inputs.file(kobwebConfig.path)
    inputs.property("kobwebBasePath", kobwebBasePath)
    outputs.file(generatedWorker)
    dependsOn("kspKotlinJs")

    doLast {
        val file = generatedWorker.get().asFile
        val workerPath = "/_kobweb/workers/de-mineking-hexo-web-analysis-worker/analysis-worker.js"
        val patched = file.readText()
            .replace(
                Regex("""Worker\("[^"]*${Regex.escape(workerPath)}"\)"""),
                "Worker(\"${kobwebBasePath.trimEnd('/')}$workerPath\")",
            )
        file.writeText(patched)
    }
}

tasks.named("compileKotlinJs") {
    dependsOn(patchGeneratedWorkerBasePath)
}
