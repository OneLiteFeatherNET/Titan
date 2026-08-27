import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("titan.java-conventions")
    id("titan.publish-conventions")
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
    compileOnly(libs.cloudnet.bridge)
    compileOnly(libs.cloudnet.bridge.impl)
}

// Stamp the project version into extension.json (@version@ placeholder).
tasks.processResources {
    val tokens = mapOf("version" to project.version.toString())
    inputs.properties(tokens)
    filesMatching("extension.json") {
        filter<ReplaceTokens>("tokens" to tokens)
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "titan-bridge"
    artifact(tasks.named("jar"))
    pom {
        name = "Titan Bridge"
        description = "CloudNet bridge extension that resolves permissions through LuckPerms"
    }
}
