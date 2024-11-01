plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.3"
    id("info.solidsoft.pitest") apply true
}

dependencies {
    implementation(platform(libs.microtus.bom))
    implementation(libs.microtus)
    implementation(libs.aves)
    implementation(libs.caffeine)

    implementation(project(":common"))
    implementation(project(":agones"))


    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(platform(libs.microtus.bom))
    testImplementation(libs.microtus)
    testImplementation(libs.microtus.testing)
    testImplementation(libs.mockito)
}
application {
    mainClass.set("net.onelitefeather.titan.app.TitanApplication")
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("titan.jar")
        mergeServiceFiles()
    }
    test {
        useJUnitPlatform()
    }
}
