plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(libs.bundles.exposed)
    implementation(libs.exposed.migration)
    implementation(libs.postgres)

    implementation(projects.utils.types)

    implementation(libs.nanoid)
    implementation(libs.logging)
}
