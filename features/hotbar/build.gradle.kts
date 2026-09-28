plugins {
    id("titan.column")
}

dependencies {
    // ListAppender, for capturing a log line in HotbarLobbyItemsIntegrationTest; logback-classic
    // itself is already on the runtime classpath via apps/*'s runtime dependency.
    testImplementation(libs.logback.classic)
}
