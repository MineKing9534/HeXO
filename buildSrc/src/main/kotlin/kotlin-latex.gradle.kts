import de.mineking.kotlinlatex.gradle.latex
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("de.mineking.kotlinlatex")
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        latex()
    }
}

latexPackage {
    embedRuntime = false
    embedDependencies = true
}
