plugins {
    id("titan.column")
}

dependencies {
    // JumprunSettings reads jumprun.* from an io.avaje.config Configuration; titan.column does not
    // pull it in because most columns don't need it.
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)

    // ListAppender, for asserting captured log lines.
    testImplementation(libs.logback.classic)

    // Real YAML loading in the tests, as in production, where apps/cloudnet brings it transitively.
    testRuntimeOnly(libs.snakeyaml)
}
