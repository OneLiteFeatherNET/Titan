plugins {
    java
}

// Minestom extension that bridges CloudNet permission checks to LuckPerms. It is
// packaged as a standalone extension jar (dropped into a CloudNet service's
// extensions/ folder next to the CloudNet bridge) and never bundled into the fat
// jar. Everything it compiles against is provided at runtime: the CloudNet driver
// and bridge by the CloudNet wrapper / bridge extension, Minestom and our
// TitanPermissionBridge holder by the application classloader.
dependencies {
    compileOnly(platform(libs.aonyx.bom))
    compileOnly(libs.minestom)
    compileOnly(libs.minestom.ce.extensions)
    compileOnly(project(":common"))

    compileOnly(platform(libs.cloudnet.bom))
    compileOnly(libs.cloudnet.driver.api)
    compileOnly(libs.cloudnet.bridge.impl)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
