plugins {
    id("kotlin-multiplatform")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    js {
        browser {
            binaries.executable()
        }
    }

    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(libs.kotlin.coroutines.test)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.jna)
            implementation(libs.kotlin.serialization.json)
        }
    }

    sourceSets.jsMain {
        dependencies {
            implementation(libs.kotlin.coroutines.core)
        }
    }
}

val copyWasmPackageResources = tasks.register<Sync>("copyWasmPackageResources") {
    from(layout.projectDirectory.dir("src/jsMain/resources/pkg"))
    into(rootProject.layout.buildDirectory.dir("js/packages/HeXO-solver/kotlin/pkg"))
}

tasks.matching {
    it.name in setOf(
        "jsBrowserDevelopmentWebpack",
        "jsBrowserProductionWebpack",
        "jsBrowserTest",
    )
}.configureEach {
    dependsOn(copyWasmPackageResources)
}

tasks.named("jsJar") {
    dependsOn(copyWasmPackageResources)
}
