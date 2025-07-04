plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.6"
    id("info.solidsoft.pitest") apply true
    `maven-publish`
    alias(libs.plugins.publishdata)
}

dependencies {
    implementation(platform(libs.microtus.bom))
    compileOnly(libs.microtus)
    compileOnly(libs.aves)
    implementation(libs.caffeine)

    implementation(project(":common"))


    testImplementation(libs.aves)
    testImplementation(platform("org.junit:junit-bom:5.13.3"))
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
publishData {
    addBuildData()
    addMainRepo("https://repo.onelitefeather.dev/onelitefeather-releases")
    addMasterRepo("https://repo.onelitefeather.dev/onelitefeather-releases")
    addSnapshotRepo("https://repo.onelitefeather.dev/onelitefeather-snapshots")
    publishTask("shadowJar")
}

publishing {
    publications.create<MavenPublication>("maven") {
        // configure the publication as defined previously.
        publishData.configurePublication(this)
        version = publishData.getVersion(false)
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    // Those credentials need to be set under "Settings -> Secrets -> Actions" in your repository
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            }

            name = "OneLiteFeatherRepository"
            // Get the detected repository from the publish data
            url = uri(publishData.getRepository())
        }
    }
}