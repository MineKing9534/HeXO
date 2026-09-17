import com.github.gmazzo.buildconfig.BuildConfigValue.Expression

plugins {
    id("kotlin-jvm")
    id("application")

    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

val generatedResources = layout.buildDirectory.dir("generated/resources")
val copyWebSite = tasks.register<Sync>("copyWebSite") {
    dependsOn(":web:kobwebExport")
    finalizedBy(":web:kobwebStop")

    from(project(":web").layout.projectDirectory.dir(".kobweb/site"))
    into(generatedResources.map { it.dir("web") })
}

sourceSets.main {
    resources.srcDir(generatedResources)
}

tasks.processResources {
    dependsOn(copyWebSite)
}

dependencies {
    implementation(projects.launcher)

    implementation(libs.kotlin.serialization.core)

    implementation(libs.kotlin.html)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    runtimeOnly(libs.logback)
}

application {
    mainClass = "de.mineking.hexo.launcher.web.MainKt"
}

val webHmdApiUrl = providers.gradleProperty("web.hmdApiUrl")
    .orElse(provider { "" })

buildConfig {
    buildConfigField<String>("HMD_API_URL", webHmdApiUrl.map { Expression("\"$it\"") })
}

tasks.compileKotlin {
    inputs.property("web.hmdApiUrl", webHmdApiUrl)
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    filesMatching("META-INF/*.kotlin_module") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    mergeServiceFiles()
}
