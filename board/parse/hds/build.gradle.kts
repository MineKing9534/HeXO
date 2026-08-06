plugins {
    id("kotlin-multiplatform")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.board.parse)

            implementation(projects.hds.model)
        }
    }
}
