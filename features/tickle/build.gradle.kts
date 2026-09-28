plugins {
    id("titan.column")
}

dependencies {
    // TickleModule/TickleAttackHandler read tickle.* directly from the io.avaje.config.Config
    // facade; titan.column does not pull it in because most columns don't need it.
    implementation(libs.avaje.config)
    // TickleAttackHandler formats the tickle broadcast with MiniMessage.
    implementation(libs.adventure.minimessage)
}
