plugins {
    id("titan.column")
}

dependencies {
    // LobbySwitcherSettings reads lobbyswitcher.* from the io.avaje.config.Config facade;
    // titan.column does not pull it in because most columns don't need it.
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)

    // ListAppender, for asserting the read failure log lines.
    testImplementation(libs.logback.classic)
}
