plugins {
    id("java")
    `java-library`
}
dependencies {
    implementation(platform(libs.microtus.bom))
    implementation(libs.microtus)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}