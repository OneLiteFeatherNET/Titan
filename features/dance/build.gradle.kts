plugins {
    id("titan.column")
}

dependencies {
    // Dialog and command texts are MiniMessage translations, as in the navigator.
    implementation(libs.adventure.minimessage)
    implementation(libs.slf4j.api)
}
