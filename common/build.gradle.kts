plugins {
    id("java")
    id("java-library")
}
dependencies {
    implementation(enforcedPlatform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    api(libs.togglz)
    api(project(":api"))
    api(libs.aves)

    implementation(platform(libs.cloudnet.bom))
    api(libs.cloudnet.jvm.wrapper)
    api(libs.cloudnet.bridge)

    testImplementation(libs.aves)
    testImplementation(platform("org.junit:junit-bom:5.13.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
tasks.test {
    useJUnitPlatform()
}