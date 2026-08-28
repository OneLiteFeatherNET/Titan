plugins {
    id("titan.java-conventions")
    `java-library`
}

dependencies {
    implementation(platform(libs.aonyx.bom))
    api(libs.minestom)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}
