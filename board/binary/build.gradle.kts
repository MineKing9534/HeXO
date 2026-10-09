plugins {
    id("kotlin-multiplatform")
    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            api(projects.board)
            implementation(libs.okio)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(libs.kotlin.serialization.core)
        }
    }
}
