plugins {
    id("titan.column")
}

dependencies {
    implementation(libs.slf4j.api)

    // ListAppender, for asserting the column's DEBUG delivery line in its tests.
    testImplementation(libs.logback.classic)
}
