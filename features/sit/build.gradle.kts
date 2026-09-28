plugins {
    id("titan.column")
}

dependencies {
    // SitModule reads sit.* directly from the io.avaje.config.Config facade; titan.column does not
    // pull it in because most columns don't need it.
    implementation(libs.avaje.config)
}
