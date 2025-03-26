plugins {
    id("java")
    `java-library`
}
dependencies {
    implementation(platform(libs.microtus.bom))
    implementation(libs.microtus)
    api(libs.togglz)
    api(project(":api"))
    implementation(libs.aves)

    implementation(platform(libs.cloudnet.bom))
    implementation(libs.cloudnet.jvm.wrapper)
    implementation(libs.cloudnet.bridge)

    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
tasks.test {
    useJUnitPlatform()
}