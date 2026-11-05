import net.dv8tion.jda.api.interactions.DiscordLocale

plugins {
    id("kotlin-jvm")
    id("kotlin-antlr")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dtk)
}

dependencies {
    implementation(projects.discord.core)
    implementation(projects.discord.config)
    implementation(projects.discord.link)
    implementation(projects.discord.oauth2.service)

    implementation(projects.board)
    implementation(projects.board.binary)
    implementation(projects.board.parse)
    implementation(projects.board.recognition)
    implementation(projects.board.render)

    implementation(projects.game.model)

    implementation(projects.server.api)
    implementation(projects.utils.coroutines)
    implementation(projects.utils.types)
    implementation(projects.utils.cache)

    implementation(libs.kotlin.coroutines.core)

    implementation(libs.jda)
    implementation(libs.jda.emoji)

    implementation(libs.logging)

    runtimeOnly(kotlin("reflect"))
}

discordToolKit {
    extensions()

    core {
        stateReferences = true
    }

    localization {
        locales.addAll(DiscordLocale.ENGLISH_US)
        basePackage = "de.mineking.hexo.discord.bot"
    }
}

val emojiGenerator = sourceSets.create("emojiGenerator")
dependencies {
    add(emojiGenerator.implementationConfigurationName, projects.board)
    add(emojiGenerator.implementationConfigurationName, projects.board.parse)
    add(emojiGenerator.implementationConfigurationName, projects.board.render)
    add(emojiGenerator.implementationConfigurationName, libs.kotlin.poet)
}

val generatedEmojiKotlin = layout.buildDirectory.dir("generated/themeEmojis/kotlin")
val generatedEmojiResources = layout.buildDirectory.dir("generated/themeEmojis/resources")
val generateThemeEmojis = tasks.register<JavaExec>("generateThemeEmojis") {
    group = "build"
    description = "Generates Discord theme emoji images and their enum."
    classpath = emojiGenerator.runtimeClasspath
    mainClass.set("de.mineking.hexo.discord.bot.generator.GenerateThemeEmojisKt")

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(CommonConfig.JAVA_VERSION))
    })
    systemProperty("java.awt.headless", "true")
    inputs.files(emojiGenerator.runtimeClasspath)
        .withPropertyName("generatorClasspath")
        .withNormalizer(ClasspathNormalizer::class)

    outputs.dir(generatedEmojiKotlin).withPropertyName("kotlinSources")
    outputs.dir(generatedEmojiResources).withPropertyName("resources")

    args(
        generatedEmojiKotlin.get().asFile.absolutePath,
        generatedEmojiResources.get().asFile.absolutePath,
    )
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(files(generatedEmojiKotlin).builtBy(generateThemeEmojis))
        resources.srcDir(files(generatedEmojiResources).builtBy(generateThemeEmojis))
    }
}

tasks.named("compileKotlin") { dependsOn(generateThemeEmojis) }
tasks.named("processResources") { dependsOn(generateThemeEmojis) }
