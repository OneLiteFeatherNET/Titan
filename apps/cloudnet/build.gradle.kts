// The production variant: every column, published as titan-cloudnet with an AOT cache.
plugins {
    id("titan.app-variant")
}

titanVariant {
    aotCache.set(true)
}

dependencies {
    // The cross-column tests moved here from runtime (see docs/lobby-modules.md, "Wie eine
    // Column Plattform-Beans bekommt") reach into core and common directly (FeatureFlags,
    // MapProvider, LobbyMap, ...), which titan.app-variant's own "implementation(project(\":runtime\"))"
    // does not expose transitively - runtime depends on both as "implementation", not "api".
    testImplementation(project(":core"))
    testImplementation(project(":common"))
    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.mockito)
    testImplementation(libs.avaje.inject)
    testImplementation(libs.avaje.config)
    // TestTitanNode - see the example feature's own test.
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

// Same reason as runtime/build.gradle.kts: LuckPerms' minestom-loader is a JarInJar bootstrap
// bundling an outdated, unrelocated Gson that shadows the real one on the test runtime classpath.
configurations.testRuntimeClasspath {
    exclude(group = "net.luckperms", module = "minestom-loader")
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "titan-cloudnet"
    artifact(tasks.shadowJar)
    pom {
        name = "Titan Cloudnet"
        description = "Titan lobby server variant for CloudNet"
    }
}
