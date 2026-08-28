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
    // Every falco artifact carries mycelium-bom as a platform dependency, and it is a release
    // ahead of the one aonyx-bom brings. Highest-wins would move Minestom underneath us with
    // nothing to announce it, which is what NFR-001 forbids: the Minestom version is the one
    // aonyx-bom prescribes. Excluded here rather than pinned, so the next falco release cannot
    // move it either.
    implementation(libs.falco.anvil) { exclude(group = "net.onelitefeather", module = "mycelium-bom") }
    implementation(libs.falco.light) { exclude(group = "net.onelitefeather", module = "mycelium-bom") }
    // Runtime only for us: nothing here names a falco-instance type, but linking
    // ChunkLightScheduler resolves FalcoLightingChunk and its FalcoChunk supertype. See the
    // version catalog for the details.
    runtimeOnly(libs.falco.instance) { exclude(group = "net.onelitefeather", module = "mycelium-bom") }

    // No CloudNet here anymore: anything touching the CloudNet bridge lives in the
    // :bridge extension; common only talks to it through the JDK-typed
    // TitanServerConnector / TitanPermissionBridge holders.

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.falco.anvil) { exclude(group = "net.onelitefeather", module = "mycelium-bom") }
    testImplementation(libs.falco.light) { exclude(group = "net.onelitefeather", module = "mycelium-bom") }
    testRuntimeOnly(libs.falco.instance) { exclude(group = "net.onelitefeather", module = "mycelium-bom") }
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