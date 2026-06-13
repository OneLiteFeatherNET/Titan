plugins {
    java
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

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Dminestom.inside-test=true")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}