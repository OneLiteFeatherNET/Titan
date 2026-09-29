plugins {
    id("titan.column")
}

dependencies {
    // SeasonConfigReader reads seasons.* directly from the io.avaje.config.Config facade;
    // titan.column does not pull it in because most columns don't need it.
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)

    // ListAppender, for capturing the column's WARN and INFO lines in its tests.
    testImplementation(libs.logback.classic)
}
