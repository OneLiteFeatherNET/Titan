plugins {
   java
    jacoco
}

dependencies {
    implementation(project(":api"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.togglz)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)
    // Falco replaces Minestom's AnvilLoader and light engine (US-1.01 - US-1.03).
    implementation(libs.falco.anvil)
    implementation(libs.falco.light)

    // No CloudNet here anymore: anything touching the CloudNet bridge lives in the
    // :bridge extension; common only talks to it through the JDK-typed
    // TitanServerConnector / TitanPermissionBridge holders.

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.falco.anvil)
    testImplementation(libs.falco.light)
    testImplementation(libs.cyano)
    testImplementation(libs.aves)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

tasks {
    test {
        useJUnitPlatform()
        jvmArgs("-Dminestom.inside-test=true")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}