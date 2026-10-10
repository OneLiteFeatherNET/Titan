// The extension bootstrap platform: starts Minestom through minestom-extensions, which loads the
// extensions/ folder before the server accepts players. Only a variant that includes it loads
// extensions, see openspec/changes/optional-extensions-bootstrap.
plugins {
    id("titan.java-conventions")
    `java-library`
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(platform(libs.minestom.extensions.bom))
    implementation(libs.minestom.extensions)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}
