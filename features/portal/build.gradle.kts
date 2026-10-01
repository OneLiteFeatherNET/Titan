plugins {
    id("titan.column")
}

dependencies {
    implementation(libs.slf4j.api)

    // ListAppender, for asserting the label source warning.
    testImplementation(libs.logback.classic)
}
