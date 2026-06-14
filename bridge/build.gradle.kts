import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    `maven-publish`
}

// Minestom extension that bridges CloudNet permission checks to LuckPerms. It is
// packaged as a standalone extension jar (dropped into a CloudNet service's
// extensions/ folder next to the CloudNet bridge) and never bundled into the fat
// jar. Everything it compiles against is provided at runtime: the CloudNet driver
// and bridge by the CloudNet wrapper / bridge extension, Minestom and our
// TitanPermissionBridge holder by the application classloader.
dependencies {
    compileOnly(platform(libs.aonyx.bom))
    compileOnly(libs.minestom)
    compileOnly(libs.minestom.ce.extensions)
    compileOnly(project(":common"))

    compileOnly(platform(libs.cloudnet.bom))
    compileOnly(libs.cloudnet.driver.api)
    compileOnly(libs.cloudnet.bridge)
    compileOnly(libs.cloudnet.bridge.impl)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Stamp the project version into extension.json (@version@ placeholder).
tasks.processResources {
    val tokens = mapOf("version" to project.version.toString())
    inputs.properties(tokens)
    filesMatching("extension.json") {
        filter<ReplaceTokens>("tokens" to tokens)
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifact(tasks.named("jar"))
        version = rootProject.version as String
        artifactId = "titan-bridge"
        groupId = rootProject.group as String
        pom {
            name = "Titan Bridge"
            description = "CloudNet bridge extension that resolves permissions through LuckPerms"
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
