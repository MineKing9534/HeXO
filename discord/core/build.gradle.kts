plugins {
    id("kotlin-multiplatform")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.serialization.core)
        }
    }


    sourceSets.jvmMain {
        dependencies {
            api(libs.jda)
            implementation(libs.exposed.core)
        }
    }
}
