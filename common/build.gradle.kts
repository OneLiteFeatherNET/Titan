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
    // Coris ships no runtime dependency of its own except mycelium-bom, whose
    // constraints would outrank the aonyx-bom line Titan pins (it moves Minestom a
    // release forward). Keep the bom out; the jar is all we want.
    implementation(libs.coris) {
        exclude(group = "net.onelitefeather", module = "mycelium-bom")
    }
    implementation(libs.adventure.minimessage)

    // No CloudNet here anymore: anything touching the CloudNet bridge lives in the
    // :bridge extension; common only talks to it through the JDK-typed
    // TitanServerConnector / TitanPermissionBridge holders.

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.aves)
    testImplementation(libs.coris) {
        exclude(group = "net.onelitefeather", module = "mycelium-bom")
    }
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
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