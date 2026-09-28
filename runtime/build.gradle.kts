plugins {
    id("titan.java-conventions")
    `java-library`
}

dependencies {
    // Public beans (Titan, VariantDescriptor, ...) expose core and common types, so consumers of
    // runtime need them on their own compile classpath too.
    api(project(":core"))
    api(project(":common"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.adventure.minimessage)
    implementation(libs.minestom)
    implementation(platform(libs.minestom.extensions.bom))
    implementation(libs.minestom.extensions)

    // Compile-time dependency injection for the lobby feature modules; see
    // openspec/changes/avaje-dependency-injection. No jakarta.annotation-api needed: avaje-inject
    // pulls jakarta.inject-api transitively, and @Priority is io.avaje.inject.Priority.
    implementation(libs.avaje.inject)
    annotationProcessor(libs.avaje.inject.generator)

    // CloudNet is provided by the CloudNet wrapper at runtime and the bridge is
    // loaded as a Minestom extension (separate classloader), so runtime neither
    // references nor bundles it.

    // Logging. Without a binding every log call in the shipped jar answered "No SLF4J providers
    // were found" and was dropped; sentry-logback is the appender logback.xml refers to.
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(platform(libs.sentry.bom))
    runtimeOnly(libs.sentry.logback)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.mockito)
    // ListAppender, for capturing a log line in a test; logback-classic itself is runtimeOnly above.
    testImplementation(libs.logback.classic)

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.archunit)
    // TestTitanNode, DummyDeliver, EventListenerCounter, ColumnArchitectureRules.
    testImplementation(testFixtures(project(":core")))
    testRuntimeOnly(libs.junit.engine)
}
