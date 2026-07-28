import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    id("com.github.node-gradle.node")
}

node {
    download = true
    version = "22.11.0"
    npmVersion = "10.9.0"
}

abstract class TailwindExtension {
    abstract val sourceSetName: Property<String>
    abstract val resourcePath: Property<String>
}

val extension = extensions.create<TailwindExtension>("tailwindcss")

val tailwindVersion = "4.3.0"
val generatedTailwindDirectory = layout.buildDirectory.dir("generated/tailwindcss")
val tailwindInputCss = layout.projectDirectory.file("src/css/styles.css")

val installTailwindCss = tasks.register<NpmTask>("installTailwindCss") {
    group = "build"
    description = "Installs tailwindcss packages used by the CSS generation task"

    inputs.property("tailwindVersion", tailwindVersion)
    outputs.dir(layout.projectDirectory.dir("node_modules/@tailwindcss/cli"))
    outputs.dir(layout.projectDirectory.dir("node_modules/tailwindcss"))

    args.set(listOf(
        "install",
        "--no-save",
        "--package-lock=false",
        "@tailwindcss/cli@$tailwindVersion",
        "tailwindcss@$tailwindVersion",
    ))
}

val tailwindTask = tasks.register<NpmTask>("generateTailwindCss") {
    group = "build"
    description = "Generates the tailwindcss file"

    dependsOn(installTailwindCss)

    inputs.files(
        fileTree("src") {
            include("**/*.css", "**/*.html", "**/*.kt")
        }
    )
    outputs.dir(generatedTailwindDirectory)

    val file = generatedTailwindDirectory.get()
        .dir(extension.resourcePath.get())
        .file("styles.css")
        .asFile

    doFirst {
        file.parentFile.mkdirs()
    }

    args.set(listOf(
        "exec",
        "--",
        "tailwindcss",
        "-i", tailwindInputCss.asFile.absolutePath,
        "-o", file.absolutePath,
        "--minify",
    ))
}

fun configureResources(sourceSets: NamedDomainObjectContainer<KotlinSourceSet>) {
    afterEvaluate {
        val sourceSet = sourceSets.getByName(extension.sourceSetName.get())
        sourceSet.resources.srcDir(tailwindTask)
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        configureResources(sourceSets)
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        configureResources(sourceSets)
    }
}
