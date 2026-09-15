plugins {
    id("kotlin-jvm")
}

val r2dbc = sourceSets.create("r2dbc") {
    kotlin.srcDir("src/r2dbc/kotlin")

    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += output + compileClasspath
}

val r2dbcImplementation = configurations.getByName("r2dbcImplementation") {
    extendsFrom(configurations["implementation"])
}

val r2dbcJar = tasks.register<Jar>("r2dbcJar") {
    archiveClassifier.set("r2dbc")
    from(r2dbc.output)
}

val r2dbcOutput = configurations.register("r2dbc") {
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(configurations[r2dbc.runtimeClasspathConfigurationName])

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }

    outgoing.artifact(r2dbcJar)
}

dependencies {
    implementation(libs.bundles.exposed)
    implementation(projects.utils.types)

    r2dbcImplementation(libs.exposed.r2dbc)
    r2dbcImplementation(libs.exposed.migration.r2dbc)
    r2dbcImplementation(libs.r2dbc.postgres)

    implementation(libs.nanoid)
    implementation(libs.logging)
}
