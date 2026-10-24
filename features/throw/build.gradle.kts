plugins {
    id("titan.column")
}

dependencies {
    // Refusal, actionbar and sound texts are MiniMessage translations, as in the dance column.
    implementation(libs.adventure.minimessage)
    implementation(libs.slf4j.api)
}
