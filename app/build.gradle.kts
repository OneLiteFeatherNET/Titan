plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.5"
    id("info.solidsoft.pitest") apply true
    `maven-publish`
    alias(libs.plugins.publishdata)
}

dependencies {
    implementation(platform(libs.microtus.bom))
    implementation(libs.microtus)
    implementation(libs.aves)
    implementation(libs.caffeine)

    implementation(project(":common"))
    implementation(project(":agones"))


    testImplementation(platform("org.junit:junit-bom:5.11.4"))
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
    useGitlabReposForProject("106", "https://gitlab.onelitefeather.dev/")
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
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }


            name = "Gitlab"
            // Get the detected repository from the publishing data
            url = uri(publishData.getRepository())
        }
    }
}