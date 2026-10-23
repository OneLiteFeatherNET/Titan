plugins {
    id("titan.column")
}

dependencies {
    // Warnings for skipped map entries, as in the portal column.
    implementation(libs.slf4j.api)
    // ListAppender, for asserting the warnings of skipped entries.
    testImplementation(libs.logback.classic)
}
