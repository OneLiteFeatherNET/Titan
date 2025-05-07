plugins {
    id("java")
}
dependencies {
    implementation(platform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.togglz)
    implementation(project(":api"))
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)

    implementation(platform(libs.cloudnet.bom))
    implementation(libs.cloudnet.jvm.wrapper)
    implementation(libs.cloudnet.bridge)

    testImplementation(libs.aves)
    testImplementation(platform("org.junit:junit-bom:5.13.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
tasks.test {
    useJUnitPlatform()
}