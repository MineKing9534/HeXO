plugins {
    id("kotlin-jvm")

    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.jda)
    implementation(libs.kotlin.serialization.core)

    implementation(libs.exposed.core)
}
