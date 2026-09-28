// The development variant: every column, no AOT cache, never published - a developer runs it
// standalone, without CloudNet.
plugins {
    id("titan.app-variant")
}

// Off by default - a developer builds with LuckPerms only when testing permissions locally, via
// ./gradlew :apps:local:build -Ptitan.luckperms.
if (providers.gradleProperty("titan.luckperms").isPresent) {
    titanVariant {
        platform("luckperms")
    }
}

dependencies {
    // See apps/cloudnet/build.gradle.kts: its own VariantStartTest-equivalent coverage reaches
    // into core and common directly (FeatureFlags, MapProvider, LobbyMap, ...) - available here too
    // since runtime exposes both as "api".
    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.mockito)
    testImplementation(libs.avaje.inject)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

// Not published (see design.md D8): titan.app-variant's titan.publish-conventions still creates
// the "maven" publication, so this variant's publish tasks are disabled instead.
tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}
