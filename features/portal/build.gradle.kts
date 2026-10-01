plugins {
    id("titan.column")
}

dependencies {
    // PortalSettings reads portal.* from the io.avaje.config.Config facade; titan.column does not
    // pull it in because most columns don't need it.
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)

    // Real YAML loading in the tests, as in production, where apps/cloudnet brings it transitively.
    testRuntimeOnly(libs.snakeyaml)
}
