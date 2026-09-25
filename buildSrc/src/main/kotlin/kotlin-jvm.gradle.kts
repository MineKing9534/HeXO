plugins {
    id("kotlin-common")
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlin("test"))

    CommonConfig.JVM_TEST_DEPENDENCIES.forEach {
        testImplementation(it)
    }

    CommonConfig.JVM_TEST_RUNTIME_DEPENDENCIES.forEach {
        testRuntimeOnly(it)
    }
}

tasks.test {
    useJUnitPlatform()
}
