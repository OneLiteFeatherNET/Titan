plugins {
    id("titan.column")
}

dependencies {
    // Reads spawn.minHeight/maxHeight/simulationDistance directly via Config; titan.column does
    // not pull this in for every column.
    implementation(libs.avaje.config)
}

// Outside the merged variant application.yaml (assembled only for :app/apps/*), tests that read
// spawn.* via Config need to point avaje-config at this column's own defaults file directly.
tasks.test {
    systemProperty("config.file", "titan/defaults/spawn.yaml")
}
