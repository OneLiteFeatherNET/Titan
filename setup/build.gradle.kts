plugins {
    java
    application
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
    alias(libs.plugins.publishdata)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":api"))
    implementation(enforcedPlatform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.togglz)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)
    implementation(libs.caffeine)

    testImplementation(platform(libs.mycelium.bom))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}
application {
    mainClass.set("net.onelitefeather.titan.setup.TitanLauncher")
    applicationDefaultJvmArgs = listOf("-DTITAN_LOBBY_MAP=halloween")
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
        archiveFileName.set("setup-titan.jar")
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