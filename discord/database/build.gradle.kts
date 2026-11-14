plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.discord.core)
    implementation(libs.exposed.core)
}
