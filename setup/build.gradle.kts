plugins {
    id("titan.java-conventions")
    id("titan.publish-conventions")
    id("com.gradleup.shadow")
    application
}

dependencies {
    implementation(project(":common"))
    implementation(project(":api"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.togglz)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)
    implementation(libs.caffeine)

    // Logging. See :app - the setup server had the same silent-logger problem.
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(platform(libs.sentry.bom))
    runtimeOnly(libs.sentry.logback)

    testImplementation(platform(libs.aonyx.bom))
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
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "titan-setup"
    artifact(tasks.shadowJar)
    pom {
        name = "Titan Setup"
        description = "Titan Setup Server for OneLiteFeather"
    }
}
