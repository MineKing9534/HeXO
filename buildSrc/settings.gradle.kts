pluginManagement {
    apply(from = "../gradle/repositories.settings.gradle.kts")
}

rootProject.name = "buildSrc"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
