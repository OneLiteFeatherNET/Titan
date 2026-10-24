// Convention for every Titan lobby feature under features/*: a column depends only on
// core - never on another column, an apps/* variant or runtime - and gets the same test stack
// every other column uses, so a new feature needs no build-file boilerplate beyond applying this
// plugin.
//
// Looked up through VersionCatalogsExtension rather than the generated "libs" accessor: that
// accessor is not generated for a buildSrc precompiled script plugin, only for a project's own
// build.gradle.kts.

import net.onelitefeather.titan.buildsrc.architecture.PackageDescription
import net.onelitefeather.titan.buildsrc.config.MergeApplicationDefaultsTask
import org.gradle.api.GradleException
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
    // Tests read application-test.yaml with the same SnakeYAML parser production uses; without it
    // avaje-config silently drops YAML values, so a column test would pass on a broken config.
    add("testRuntimeOnly", lib("snakeyaml"))
}

// A column's tests that read io.avaje.config.Config need this column's own shipped defaults, not
// a whole variant's merged application.yaml (only assembled for apps/*). Concatenating this
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

// The column describes itself in the package-info.java of its base package (openspec
// maintainability-gates, design D8). Only presence is checked; the wording stays a review question.
val verifyPackageDescription = tasks.register("verifyPackageDescription") {
    group = "verification"
    description = "Fails when the column's package-info.java has no Javadoc description."
    val sourceRoot = layout.projectDirectory.dir("src/main/java")
    inputs.dir(sourceRoot)
    doLast {
        val packageInfo = sourceRoot.asFile.walk()
            .filter { it.name == "package-info.java" }
            .minByOrNull { it.path.length }
        if (packageInfo == null || !PackageDescription.hasDescription(packageInfo.readText())) {
            throw GradleException("${project.path}: package-info.java has no Javadoc description of the column")
        }
    }
}

tasks.named("check") {
    dependsOn(verifyPackageDescription)
}
