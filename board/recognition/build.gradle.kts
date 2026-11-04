plugins {
    id("kotlin-jvm")
    id("publish")
}

dependencies {
    api(projects.board)
    implementation(libs.boofcv.feature)
    implementation(libs.boofcv.io)
    implementation(libs.ddogleg)

    testImplementation(projects.board.render)
}
