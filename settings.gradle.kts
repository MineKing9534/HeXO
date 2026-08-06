pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.mineking.dev/snapshots")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "HeXO"

include(":board")
include(":board:parse")
include(":board:parse:hds")
include(":board:render")

include(":board:render:compose")
include(":web:analysis-worker")
include(":web")

include(":hds:model")
include(":hds:implementation")
include(":hds:implementation:processor")

include(":solver")

include(":discord:link")
include(":discord:bot")

include(":server")
include(":server:service")
include(":launcher")

include(":sync:service")
include(":sync:common")
include(":sync:client")

include(":utils:coroutines")
include(":utils:omissible")
