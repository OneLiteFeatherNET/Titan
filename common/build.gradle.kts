plugins {
    id("titan.java-conventions")
}

dependencies {
    implementation(project(":api"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.togglz)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)
    // Logging was relying on Minestom's transitive slf4j-api; declare it where it is used.
    implementation(libs.slf4j.api)
    // TitanObservability compiles against the Sentry API. The Logback appender that actually
    // reports is a runtime concern of the two application modules.
    implementation(platform(libs.sentry.bom))
    implementation(libs.sentry)

    // No CloudNet here anymore: anything touching the CloudNet bridge lives in the
    // :bridge extension; common only talks to it through the JDK-typed
    // TitanServerConnector / TitanPermissionBridge holders.

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.aves)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}
