plugins {
    id("titan.column")
}

dependencies {
    // MiniMessage renders the feather's and the navigator inventory's display names.
    implementation(libs.adventure.minimessage)

    // DefaultNavigatorFeatureFlagsTest loads the shipped titan/defaults/navigator.yaml to check it
    // against Destination's fixed feature flags - a test-only reader, never a main-code dependency
    // (see ColumnArchitectureTest's navigatorDoesNotDependOnAvajeConfig rule).
    testImplementation(libs.avaje.config)
}
