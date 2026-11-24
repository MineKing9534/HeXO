import org.gradle.api.artifacts.dsl.RepositoryHandler

// Shared by the main build and buildSrc, for plugins and dependencies.
fun RepositoryHandler.configureRepositories() {
    mavenCentral()
    gradlePluginPortal()
    google()

    maven("https://central.sonatype.com/repository/maven-snapshots")
    maven("https://maven.mineking.dev/snapshots")
}

settings.pluginManagement.repositories.configureRepositories()

gradle.beforeProject {
    repositories.configureRepositories()
}
