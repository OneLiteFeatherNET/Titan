plugins {
    id("java")
    `java-library`
}
dependencies {
    implementation(platform(libs.microtus.bom))
    compileOnly(libs.microtus)

    testImplementation(platform("org.junit:junit-bom:5.13.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}