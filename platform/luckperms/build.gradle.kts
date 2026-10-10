plugins {
    id("titan.java-conventions")
    `java-library`
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)

    compileOnly(libs.luckperms.api) {
        exclude(group = "net.kyori.adventure")
    }
    runtimeOnly(libs.luckperms.minestom.loader) {
        exclude(group = "net.kyori.adventure")
    }
    compileOnly(libs.luckperms.minestom.loader) {
        exclude(group = "net.kyori.adventure")
    }

    // LuckPerms expects Guava unrelocated on the classpath.
    implementation(libs.guava)

    // Compile-time dependency injection; see openspec/changes/avaje-dependency-injection.
    implementation(libs.avaje.inject)
    annotationProcessor(libs.avaje.inject.generator)

    // Only for LuckPermsResultsTest's Tristate mapping - compileOnly deps of the main source set
    // are not visible to test by default, at compile time or at runtime.
    testCompileOnly(libs.luckperms.api) {
        exclude(group = "net.kyori.adventure")
    }
    testRuntimeOnly(libs.luckperms.api) {
        exclude(group = "net.kyori.adventure")
    }

    // TestTelemetry: an in-memory span exporter and metric reader for LuckPermsTelemetryTest.
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

// The LuckPerms minestom-loader is a JarInJar bootstrap that bundles an unrelocated, outdated
// Gson. Tests here don't load LuckPerms, but as a runtimeOnly dependency the loader leaks into the
// test runtime classpath where its bundled Gson shadows the real one and breaks Minestom's
// registry init (GsonBuilder.disableJdkUnsafe NoSuchMethodError). Keep it off the test path.
configurations.testRuntimeClasspath {
    exclude(group = "net.luckperms", module = "minestom-loader")
}
