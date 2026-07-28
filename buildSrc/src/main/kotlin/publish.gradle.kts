import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    `maven-publish`
}

val release = System.getenv("RELEASE") == "true"

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<JavaPluginExtension>("java") {
        withSourcesJar()
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        withSourcesJar(publish = true)
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.mineking.dev/" + (if (release) "releases" else "snapshots"))
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_SECRET")
            }
        }
    }

    publications {
        // only create for JVM modules that don't already have KMP publications
        if (findByName("kotlinMultiplatform") == null && findByName("jvm") == null) {
            create<MavenPublication>("mavenJvm") {
                from(components["kotlin"])
            }
        }
    }

}

afterEvaluate {
    publishing.publications.withType<MavenPublication>().configureEach {
        val suffix = artifactId.removePrefix(project.name)
        artifactId = "${project.path.removePrefix(":").replace(":", "-")}$suffix"
        groupId = "${rootProject.group}"

        version = if (release) "${ project.version }" else System.getenv("BRANCH")
    }
}
