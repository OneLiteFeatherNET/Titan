// Convention for every Titan lobby feature under features/* (D8): a column depends only on
// core - never on another column, :app or runtime - and gets the same test stack every other
// column uses, so a new feature needs no build-file boilerplate beyond applying this plugin.
//
// Looked up through VersionCatalogsExtension rather than the generated "libs" accessor: that
// accessor is not generated for a buildSrc precompiled script plugin, only for a project's own
// build.gradle.kts.

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
    // EventListenerCounter) - see core's testFixtures (D7).
    add("testImplementation", testFixtures(project(":core")))
    add("testRuntimeOnly", lib("junit.engine"))
}
