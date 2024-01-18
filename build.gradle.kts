plugins {
    kotlin("jvm") version "1.9.22"
    alias(libs.plugins.shadow)
    alias(libs.plugins.publishdata)
    `maven-publish`
}

group = "net.onelitefeather.titan"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        url = uri("https://gitlab.themeinerlp.dev/api/v4/groups/28/-/packages/maven")
        name = "GitLab"
        credentials(HttpHeaderCredentials::class.java) {
            name = if (System.getenv().containsKey("CI")) {
                "Job-Token"
            } else {
                "Private-Token"
            }
            val gitLabPrivateToken: String? by project
            value = gitLabPrivateToken ?: System.getenv("CI_JOB_TOKEN")
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }

}

dependencies {
    compileOnly(libs.minestom)
    compileOnly(libs.aves)
    implementation(libs.adventure.minimessage)
    implementation(libs.guava)

    compileOnly(libs.cloudnet.bridge)
    compileOnly(libs.cloudnet.driver)
    compileOnly(libs.cloudnet.wrapper.jvm)
}

publishData {
    addBuildData()
    useGitlabReposForProject("106", "https://gitlab.themeinerlp.dev/")
    publishTask("shadowJar")
}


tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}.${archiveExtension.getOrElse("jar")}")
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        // configure the publication as defined previously.
        publishData.configurePublication(this)
    }

    repositories {
        maven {
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }


            name = "Gitlab"
            // Get the detected repository from the publish data
            url = uri(publishData.getRepository())
        }
    }
}


