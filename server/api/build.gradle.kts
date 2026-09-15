plugins {
    id("kotlin-jvm")
}

dependencies {
    api(libs.ktor.server.core)
    api(libs.ktor.server.rateLimit)
}
