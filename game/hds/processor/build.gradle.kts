plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlin.poet)
}
