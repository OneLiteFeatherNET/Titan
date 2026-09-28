plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // Same version the root build resolves via the aonyx BOM; buildSrc is a separate build with no
    // access to that BOM, so it is pinned directly here.
    implementation("org.yaml:snakeyaml:2.7")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testImplementation("org.junit.platform:junit-platform-launcher:6.1.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
}

tasks.test {
    useJUnitPlatform()
}
