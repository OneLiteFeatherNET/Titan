plugins {
    id("titan.column")
}

dependencies {
    // ListAppender, for capturing a log line in HotbarLobbyItemsIntegrationTest; logback-classic
    // itself is already on the runtime classpath via :app.
    testImplementation(libs.logback.classic)
}
