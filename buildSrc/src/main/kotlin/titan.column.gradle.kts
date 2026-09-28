// Convention for every Titan lobby feature under features/*: a column depends only on
// core - never on another column, :app or runtime - and gets the same test stack every other
// column uses, so a new feature needs no build-file boilerplate beyond applying this plugin.
//
// Looked up through VersionCatalogsExtension rather than the generated "libs" accessor: that
// accessor is not generated for a buildSrc precompiled script plugin, only for a project's own
// build.gradle.kts.

import net.onelitefeather.titan.buildsrc.config.MergeApplicationDefaultsTask
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("titan.java-conventions")
    `java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = libs.findLibrary(alias).get()

dependencies {
    implementation(project(":core"))
    implementation(platform(lib("aonyx-bom")))
    implementation(lib("minestom"))
    implementation(lib("aves"))

    // Compile-time dependency injection for the column; see
    // openspec/changes/avaje-dependency-injection.
    implementation(lib("avaje-inject"))
    add("annotationProcessor", lib("avaje-inject-generator"))

    add("testImplementation", platform(lib("aonyx-bom")))
    add("testImplementation", lib("minestom"))
    add("testImplementation", lib("cyano"))
    add("testImplementation", lib("mockito"))
    add("testImplementation", lib("junit.api"))
    add("testImplementation", lib("junit.platform.launcher"))
    add("testImplementation", lib("archunit"))
    // ColumnArchitectureRules and the shared test fixtures (TestTitanNode, DummyDeliver,
    // EventListenerCounter) - see core's testFixtures.
    add("testImplementation", testFixtures(project(":core")))
    add("testRuntimeOnly", lib("junit.engine"))
}

// A column's tests that read io.avaje.config.Config need this column's own shipped defaults, not
// a whole variant's merged application.yaml (only assembled for :app/apps/*, see design.md D4:
// "Column-Tests, die Standardwerte brauchen, laden ihre eigene Default-Datei"). Concatenating this
// column's own titan/defaults/*.yaml into application-test.yaml lets avaje-config's own built-in
// test-resource discovery (io.avaje.config.Configuration, "Test configuration") load it - no
// column needs a hand-copied application-test.yaml, a systemProperty on tasks.test, or its own
// merge task for this. A column without a titan/defaults directory gets an empty, inert file.
val mergeTestDefaults = tasks.register<MergeApplicationDefaultsTask>("mergeTestDefaults") {
    group = "verification"
    description = "Concatenates this column's own titan/defaults/*.yaml into a test-only application-test.yaml."
    defaultFiles.from(fileTree("src/main/resources/titan/defaults") { include("*.yaml") })
    outputFileName.set("application-test.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/titanTestDefaults"))
}

sourceSets.test {
    resources.srcDir(mergeTestDefaults.map { it.outputDir })
}
