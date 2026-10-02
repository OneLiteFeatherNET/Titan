plugins {
    id("titan.column")
}

dependencies {
    implementation(libs.slf4j.api)

    // ListAppender, for asserting captured log lines.
    testImplementation(libs.logback.classic)
}
