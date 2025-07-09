plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.6"
    id("info.solidsoft.pitest") apply true
    `maven-publish`
    alias(libs.plugins.publishdata)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(platform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.togglz)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)
    implementation(libs.caffeine)
    implementation(libs.minestom)

    implementation(platform(libs.cloudnet.bom))
    implementation(libs.cloudnet.jvm.wrapper)
    implementation(libs.cloudnet.bridge)
    implementation(libs.cloudnet.bridge.impl)
    implementation(libs.cloudnet.driver.impl)
    implementation(libs.cloudnet.platform.inject)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.aves)
    testImplementation(libs.cyano)
    testImplementation(libs.mockito)

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
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