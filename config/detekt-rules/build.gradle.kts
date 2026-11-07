plugins {
    id("kotlin-jvm")
}

dependencies {
    compileOnly(libs.detekt.api)
    compileOnly(libs.detekt.analysis.api)

    testImplementation(libs.detekt.tooling)
    testRuntimeOnly(libs.detekt.cli)
}
