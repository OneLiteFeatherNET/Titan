// The production variant: every column, published as titan-cloudnet with an AOT cache.
plugins {
    id("titan.app-variant")
}

titanVariant {
    aotCache.set(true)
    // The only production variant: LuckPerms is mandatory here, never optional.
    platform("luckperms")
}

dependencies {
    // The cross-column tests moved here from runtime (see docs/lobby-modules.md, "Wie eine
    // Column Plattform-Beans bekommt") reach into core and common directly (FeatureFlags,
    // MapProvider, LobbyMap, ...); runtime exposes both as "api", so titan.app-variant's own
    // "implementation(project(\":runtime\"))" already puts them on this classpath.
    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.mockito)
    testImplementation(libs.archunit)
    testImplementation(libs.avaje.inject)
    testImplementation(libs.avaje.config)
    // ApplicationYamlParityTest reads the merged application.yaml raw, with the production parser.
    testImplementation(libs.snakeyaml)
    // The season wiring test asserts on captured log lines.
    testImplementation(libs.slf4j.api)
    testImplementation(libs.logback.classic)
    // TestTitanNode - see the example feature's own test.
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "titan-cloudnet"
    artifact(tasks.shadowJar)
    pom {
        name = "Titan Cloudnet"
        description = "Titan lobby server variant for CloudNet"
    }
}
