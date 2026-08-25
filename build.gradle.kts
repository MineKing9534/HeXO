group = "de.mineking.hexo"
version = "1.5.0"

subprojects {
    version = rootProject.version

    apply(plugin = "detekt")
    apply(plugin = "testlogger")

    this.group = "${rootProject.group}${this.path.replace(":", ".").removeSuffix(".")}"
}
