plugins {
    id("java")
    `java-library`
}
dependencies {
    implementation(platform(libs.microtus.bom))
    compileOnly(libs.microtus)
    api(libs.togglz)
    api(project(":api"))
    compileOnly(libs.aves)

    implementation(platform(libs.cloudnet.bom))
    compileOnly(libs.cloudnet.jvm.wrapper)
    compileOnly(libs.cloudnet.bridge)

    testImplementation(libs.aves)
    testImplementation(platform("org.junit:junit-bom:5.13.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
tasks.test {
    useJUnitPlatform()
}