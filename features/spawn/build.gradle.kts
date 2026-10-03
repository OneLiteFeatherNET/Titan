plugins {
    id("titan.column")
}

dependencies {
    // Reads spawn.minHeight/maxHeight/simulationDistance directly via Config; titan.column does
    // not pull this in for every column. titan.column's mergeTestDefaults task gives its tests
    // this column's own defaults.
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)
}
