plugins {
    id("titan.column")
}

dependencies {
    // songs.yaml is read with SnakeYAML; the shipped jar already carries this version through common.
    implementation(libs.snakeyaml)
    implementation(libs.slf4j.api)

    // Titles, lore and the menu texts are MiniMessage translations, as in the navigator.
    implementation(libs.adventure.minimessage)

    // ListAppender, for capturing the WARN lines of rejected MIDI files in a test.
    testImplementation(libs.logback.classic)
}
