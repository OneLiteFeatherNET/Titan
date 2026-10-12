plugins {
    id("titan.column")
}

dependencies {
    // NavigatorSettings reads navigator.refreshSeconds from the io.avaje.config.Config facade.
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)

    // MiniMessage renders the feather's and the navigator inventory's display names.
    implementation(libs.adventure.minimessage)
}
