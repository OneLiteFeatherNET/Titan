plugins {
   java
    id("com.diffplug.spotless") version "7.1.0" apply true
}

dependencies {
    implementation(project(":api"))
    implementation(enforcedPlatform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.togglz)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)

    implementation(platform(libs.cloudnet.bom))
    implementation(libs.cloudnet.jvm.wrapper)
    implementation(libs.cloudnet.bridge)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.aves)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

tasks.test {
    useJUnitPlatform()
}