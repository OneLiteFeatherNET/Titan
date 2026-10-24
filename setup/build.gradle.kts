import net.onelitefeather.titan.buildsrc.git.GitCommitValueSource

plugins {
    id("titan.java-conventions")
    id("titan.publish-conventions")
    id("com.gradleup.shadow")
    application
}

dependencies {
    implementation(project(":common"))
    implementation(project(":core"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)

    // Logging. See runtime/build.gradle.kts - the setup server had the same silent-logger problem.
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(platform(libs.sentry.bom))
    runtimeOnly(libs.sentry.logback)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.archunit)
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.cyano)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    // ListAppender, for capturing a log line in a test; logback-classic itself is runtimeOnly above.
    testImplementation(libs.logback.classic)
    testRuntimeOnly(libs.junit.engine)
}

application {
    mainClass.set("net.onelitefeather.titan.setup.TitanLauncher")
    applicationDefaultJvmArgs = listOf("-DTITAN_LOBBY_MAP=halloween")
}

// Git-Commit names the build in the startup log line, like titan.app-variant's shaded jars; it is
// resolved lazily when the shaded jar is written.
val gitCommit = providers.of(GitCommitValueSource::class.java) {
    parameters.workingDirectory.set(rootProject.layout.projectDirectory)
}.orElse("unknown")

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
        manifest {
            attributes("Git-Commit" to gitCommit)
        }
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "titan-setup"
    artifact(tasks.shadowJar)
    pom {
        name = "Titan Setup"
        description = "Titan Setup Server for OneLiteFeather"
    }
}
