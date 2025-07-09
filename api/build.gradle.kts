plugins {
    java
    `java-library`
    id("com.diffplug.spotless") version "7.1.0" apply true
}
dependencies {
    implementation(platform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    api(libs.minestom)

    testImplementation(platform(libs.mycelium.bom))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

tasks.test {
    useJUnitPlatform()
}