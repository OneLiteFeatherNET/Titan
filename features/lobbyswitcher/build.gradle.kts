plugins {
    id("titan.column")
}

dependencies {
    // LobbySwitcherSettings reads lobbyswitcher.* from the io.avaje.config.Config facade;
    // titan.column does not pull it in because most columns don't need it.
    implementation(libs.avaje.config)
}
