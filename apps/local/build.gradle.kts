// The development variant: every column, no AOT cache, never published - a developer runs it
// standalone, without CloudNet.
plugins {
    id("titan.app-variant")
}

dependencies {
    // See apps/cloudnet/build.gradle.kts: its own VariantStartTest-equivalent coverage reaches
    // into core and common directly (FeatureFlags, MapProvider, LobbyMap, ...).
    testImplementation(project(":core"))
    testImplementation(project(":common"))
    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.mockito)
    testImplementation(libs.avaje.inject)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

configurations.testRuntimeClasspath {
    exclude(group = "net.luckperms", module = "minestom-loader")
}

// Not published (see design.md D8): titan.app-variant's titan.publish-conventions still creates
// the "maven" publication, so this variant's publish tasks are disabled instead.
tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}
