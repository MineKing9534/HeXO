plugins {
    id("kotlin-multiplatform")
}

kotlin {
    js { browser() }
    jvm()

    sourceSets.commonMain {
        dependencies {
            api(projects.sync.common)
            implementation(projects.hds.model)

            implementation(libs.bundles.ktor.client)
            implementation(libs.ktor.client.websockets)

            implementation(libs.logging)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}
