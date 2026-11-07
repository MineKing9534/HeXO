plugins {
    id("kotlin-multiplatform")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.board.recognition)

            implementation(libs.bundles.ktor.client)
        }
    }
}
