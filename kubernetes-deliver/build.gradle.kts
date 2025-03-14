plugins {
    id("java")
    `java-library`
}

dependencies {

    implementation(project(":api"))

    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}