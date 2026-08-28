// Shared publishing setup for the three published Titan modules (:app, :setup,
// :bridge). Coordinates, licence, developer, SCM and the target repository are
// identical everywhere; only artifactId, POM name, POM description and the
// published artifacts differ, so those stay in the module build files:
//
//   publishing.publications.named<MavenPublication>("maven") {
//       artifactId = "titan-<module>"
//       artifact(tasks.shadowJar)
//       pom {
//           name = "..."
//           description = "..."
//       }
//   }

plugins {
    `maven-publish`
}

publishing {
    publications.create<MavenPublication>("maven") {
        groupId = rootProject.group.toString()
        version = rootProject.version.toString()

        pom {
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
            name = "OneLiteFeatherRepository"
            authentication {
                credentials(PasswordCredentials::class) {
                    // Those credentials need to be set under "Settings -> Secrets -> Actions" in your repository
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            }
            url = if (rootProject.version.toString().contains("SNAPSHOT")) {
                uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
            } else {
                uri("https://repo.onelitefeather.dev/onelitefeather-releases")
            }
        }
    }
}
