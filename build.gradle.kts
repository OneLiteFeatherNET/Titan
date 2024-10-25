plugins {
    kotlin("jvm") version "1.9.22"
    alias(libs.plugins.publishdata)
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.8"

}

group = "net.onelitefeather.titan"
version = "1.1.1"

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        url = uri("https://gitlab.onelitefeather.dev/api/v4/groups/28/-/packages/maven")
        name = "GitLab"
        credentials(HttpHeaderCredentials::class.java) {
            name = "Private-Token"
            val gitLabPrivateToken: String? by project
            value = gitLabPrivateToken
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }

}

dependencies {
    implementation(platform(libs.microtus.bom))
    implementation(libs.microtus)
    implementation(libs.aves)

    implementation(libs.togglz)
    implementation(libs.caffeine)
}

tasks {
    compileKotlin {
        kotlinOptions {
            options.freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
    jar {
        archiveClassifier.set("unshaded")
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
    }
}

publishData {
    addBuildData()
    useGitlabReposForProject("106", "https://gitlab.themeinerlp.dev/")
    publishTask("shadowJar")
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


