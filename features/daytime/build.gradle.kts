plugins {
    id("titan.column")
}

dependencies {
    // DaytimeModule reads daytime.* directly from the io.avaje.config.Config facade;
    // titan.column does not pull it in because most columns don't need it.
    implementation(libs.avaje.config)

    // ListAppender, for capturing the module's WARN lines in DaytimeModuleTest.
    testImplementation(libs.logback.classic)
}
