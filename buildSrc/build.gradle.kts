plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.mineking.dev/snapshots")
}

dependencies {
    implementation(libs.build.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.testlogger.gradle.plugin)
    implementation(libs.node.gradle.plugin)
    implementation(libs.latex.gradle.plugin)
}
