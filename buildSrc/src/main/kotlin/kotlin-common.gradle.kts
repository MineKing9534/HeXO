import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.mineking.dev/releases")
    maven("https://maven.mineking.dev/snapshots")
}


pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        jvmToolchain(CommonConfig.JAVA_VERSION)
        compilerOptions.freeCompilerArgs.addAll(CommonConfig.COMMON_COMPILER_ARGS)
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        jvmToolchain(CommonConfig.JAVA_VERSION)
        compilerOptions.freeCompilerArgs.addAll(CommonConfig.COMMON_COMPILER_ARGS)
    }
}

tasks.withType<Jar> {
    archiveBaseName = project.path.removePrefix(":").replace(":", "-")
}
