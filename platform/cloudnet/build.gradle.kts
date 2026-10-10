// The CloudNet platform module: the deliver a CloudNet service uses to move players through the
// bridge. It needs the extension bootstrap, because the bridge only loads as an extension.
plugins {
    id("titan.java-conventions")
    `java-library`
}

// Same name as apps/cloudnet: under the shared group Gradle would resolve this platform to the app.
group = "net.onelitefeather.platform"

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))
    implementation(project(":platform:extensions"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.slf4j.api)

    // Compile-time dependency injection; see openspec/changes/avaje-dependency-injection.
    implementation(libs.avaje.inject)
    annotationProcessor(libs.avaje.inject.generator)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    // ListAppender, for capturing the warning MessageChannelDeliver logs.
    testImplementation(libs.logback.classic)
    testRuntimeOnly(libs.junit.engine)
}
