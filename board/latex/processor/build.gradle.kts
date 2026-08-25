plugins {
    id("kotlin-jvm")
    id("publish")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlin.poet)
}
