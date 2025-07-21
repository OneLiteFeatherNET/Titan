plugins {
    java
    application
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.8"
    id("com.diffplug.spotless") version "7.2.1" apply true
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

publishing {
    publications.create<MavenPublication>("maven") {
        artifact(project.tasks.getByName("shadowJar"))
        version = rootProject.version as String
        artifactId = "titan-setup"
        groupId = rootProject.group as String
        pom {
            name = "Titan Setup"
            description = "Titan Setup Server for OneLiteFeather"
            url = "https://github.com/OneLiteFeatherNET/titan"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "themeinerlp"
                    name = "Phillipp Glanz"
                    email = "p.glanz@madfix.me"
                }
            }
            scm {
                connection = "scm:git:git://github.com:OneLiteFeatherNET/Titan.git"
                developerConnection = "scm:git:ssh://git@github.com:OneLiteFeatherNET/Titan.git"
                url = "https://github.com/OneLiteFeatherNET/titan"
            }
        }
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
            val releasesRepoUrl = uri("https://repo.onelitefeather.dev/onelitefeather-releases")
            val snapshotsRepoUrl = uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
            url = if (version.toString().contains("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}