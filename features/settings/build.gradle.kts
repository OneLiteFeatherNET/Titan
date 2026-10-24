plugins {
    id("titan.column")
}

dependencies {
    // Menu and command texts are MiniMessage translations, as in the dance and navigator columns.
    implementation(libs.adventure.minimessage)
    implementation(libs.slf4j.api)
    // SettingsOptions reads settings.enabled and the otis.* write keys directly through Config.
    implementation(libs.avaje.config)

    // ListAppender, for capturing the save-failure warning in a test.
    testImplementation(libs.logback.classic)
}
