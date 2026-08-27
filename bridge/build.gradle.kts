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
    annotationProcessor(platform(libs.minestom.extensions.bom))
    annotationProcessor(libs.minestom.extensions.processor)

    compileOnly(platform(libs.aonyx.bom))
    compileOnly(libs.minestom)
    compileOnly(platform(libs.minestom.extensions.bom))
    compileOnly(libs.minestom.extensions)
    compileOnly(libs.minestom.extensions.processor)
    compileOnly(project(":common"))

    compileOnly(platform(libs.cloudnet.bom))
    compileOnly(libs.cloudnet.driver.api)
    compileOnly(libs.cloudnet.bridge)
    compileOnly(libs.cloudnet.bridge.impl)
}

// The annotation processor generates extension.json but cannot know the project version.
// Subprojects do not inherit the root version, so read it from the root project - the same
// source the publications use.
tasks.compileJava {
    options.compilerArgs.add("-Aminestom.extension.version=${rootProject.version}")
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "titan-bridge"
    artifact(tasks.named("jar"))
    pom {
        name = "Titan Bridge"
        description = "CloudNet bridge extension that resolves permissions through LuckPerms"
    }
}
