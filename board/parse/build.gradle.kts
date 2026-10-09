plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.game.model)

            implementation(projects.utils.cache)

            implementation(libs.kotlin.antlr.runtime)
            implementation(libs.kotlin.serialization.core)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(libs.kotlin.coroutines.test)
        }
    }
}

val generateKotlinGrammarSource = tasks.register<JavaExec>("generateKotlinGrammarSource") {
    val grammarSources = fileTree("src/commonMain/antlr") {
        include("**/*.g4")
    }
    val generatedGrammarDirectory = layout.buildDirectory.dir("generated/antlr/commonMain")

    dependsOn("cleanGenerateKotlinGrammarSource")

    inputs.files(grammarSources).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(generatedGrammarDirectory)

    classpath = antlrKotlin
    mainClass = "org.antlr.v4.Tool"
    maxHeapSize = "512m"

    args(
        "-Dlanguage=Kotlin",
        "-encoding", "UTF-8",
        "-o", generatedGrammarDirectory.get().asFile.absolutePath,
    )
    args(grammarSources.files.sorted().map(File::getAbsolutePath))
}

kotlin {
    sourceSets.commonMain {
        kotlin.srcDir(generateKotlinGrammarSource)
    }
}

val antlrKotlin = configurations.create("antlrKotlin")
dependencies {
    add(antlrKotlin.name, libs.antlr.tool)
    add(antlrKotlin.name, libs.kotlin.antlr.target)
}
