plugins {
    id("java")
    `java-library`
}

dependencies {

    implementation(project(":api"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}