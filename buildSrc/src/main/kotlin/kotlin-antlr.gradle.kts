import gradle.kotlin.dsl.accessors._ee6bd3267d5c93464822b7f79c2016cf.main
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val antlrKotlinVersion = "1.0.0"
val antlrKotlinRuntime = "com.strumenta:antlr-kotlin-runtime:$antlrKotlinVersion"

val antlrKotlin = configurations.create("antlrKotlin")
dependencies {
    add(antlrKotlin.name, "org.antlr:antlr4:4.13.1")
    add(antlrKotlin.name, "com.strumenta:antlr-kotlin-target:$antlrKotlinVersion")
}

val generateKotlinGrammarSource = tasks.register<JavaExec>("generateKotlinGrammarSource") {
    val grammarSources = fileTree("src") { include("**/antlr/**/*.g4") }
    val generatedGrammarDirectory = layout.buildDirectory.dir("generated/antlr")

    dependsOn("cleanGenerateKotlinGrammarSource")
    inputs.files(grammarSources).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(generatedGrammarDirectory)
    classpath = antlrKotlin
    mainClass = "org.antlr.v4.Tool"
    maxHeapSize = "512m"
    args("-Dlanguage=Kotlin", "-encoding", "UTF-8", "-o", generatedGrammarDirectory.get().asFile.absolutePath)
    args(grammarSources.files.sorted().map { it.absolutePath })
}

plugins.withId("kotlin-multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.commonMain {
            kotlin.srcDir(generateKotlinGrammarSource)
            dependencies { implementation(antlrKotlinRuntime) }
        }
    }
}

plugins.withId("kotlin-jvm") {
    extensions.configure<KotlinJvmProjectExtension> {
        sourceSets.main { kotlin.srcDir(generateKotlinGrammarSource) }
    }
    dependencies.add("implementation", antlrKotlinRuntime)
    tasks.withType<KotlinCompile>().configureEach { dependsOn(generateKotlinGrammarSource) }
}
