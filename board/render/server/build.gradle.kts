plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.board.render)
    implementation(projects.board.parse)
    implementation(projects.board.binary)

    implementation(projects.server)
}
