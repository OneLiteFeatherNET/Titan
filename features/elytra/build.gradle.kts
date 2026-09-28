plugins {
    id("titan.column")
}

dependencies {
    // Reads elytra.burnDurationTicks/cooldownTicks from the merged application.yaml at runtime;
    // titan.column's mergeTestDefaults task gives its tests this column's own defaults.
    implementation(libs.avaje.config)
}
