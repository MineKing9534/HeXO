plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)

    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.board)
            implementation(projects.board.binary)

            api(libs.kotlin.io)
            implementation(libs.kotlin.serialization.core)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.boofcv.feature)
            implementation(libs.boofcv.io)
            implementation(libs.ddogleg)

            implementation(libs.kotlin.coroutines.core)
            runtimeOnly(libs.ktor.client.cio)
        }
    }

    sourceSets.jsMain {
        dependencies {
            runtimeOnly(libs.ktor.client.js)
        }
    }

    sourceSets.jvmTest {
        dependencies {
            implementation(projects.board.render)
            implementation(libs.kotlin.coroutines.test)
        }
    }
}
