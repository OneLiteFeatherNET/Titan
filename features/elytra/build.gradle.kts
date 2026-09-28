import net.onelitefeather.titan.buildsrc.config.MergeApplicationDefaultsTask

plugins {
    id("titan.column")
}

dependencies {
    // Reads elytra.burnDurationTicks/cooldownTicks from the merged application.yaml at runtime.
    implementation(libs.avaje.config)
}

// A column's own tests that read Config values get only this column's own defaults, not the
// whole variant's merged application.yaml (see design.md D4: "Column-Tests, die Standardwerte
// brauchen, laden ihre eigene Default-Datei"). Reuses :app's own merge task, scoped to just
// this column's titan/defaults/*.yaml.
val mergeTestDefaults = tasks.register<MergeApplicationDefaultsTask>("mergeTestDefaults") {
    group = "verification"
    description = "Concatenates this column's own titan/defaults/*.yaml into a test-only classpath application.yaml."
    defaultFiles.from(fileTree("src/main/resources/titan/defaults") { include("*.yaml") })
    outputDir.set(layout.buildDirectory.dir("generated/titanTestDefaults"))
}

sourceSets.test {
    resources.srcDir(mergeTestDefaults.map { it.outputDir })
}
