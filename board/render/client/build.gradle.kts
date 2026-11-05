plugins {
    id("kotlin-multiplatform")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.board.render)
            api(libs.kotlin.io)
            implementation(projects.board.binary)

            implementation(libs.bundles.ktor.client)
            runtimeOnly(libs.ktor.client.cio)
        }
    }
}
