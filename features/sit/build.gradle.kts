plugins {
    id("titan.column")
}

dependencies {
    // SitModule reads sit.* directly from the io.avaje.config.Config facade; titan.column does not
    // pull it in because most columns don't need it. SnakeYAML is optional in avaje-config's POM,
    // so it is declared here too, to get the full YAML parser avaje-config needs at runtime.
    implementation(libs.avaje.config)
    implementation(libs.snakeyaml)
}
