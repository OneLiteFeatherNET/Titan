// Shared Java setup for every Titan module. Previously this block was copied into
// each module build file: the toolchain five times, the test configuration four
// times, and Jacoco into only two of the five modules.

import net.onelitefeather.titan.buildsrc.git.GitCommitValueSource

plugins {
    java
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// TitanObservability reports this as the Sentry release, so an issue can be traced back to the
// deploy that introduced it. Package.getImplementationVersion() reads it from the jar the classes
// were loaded from, which for both server processes is the shaded jar.
// Git-Commit names the build in the startup log line (TitanBuild), so a deploy is traceable in Loki.
val gitCommit = providers.of(GitCommitValueSource::class.java) {
    parameters.workingDirectory.set(rootProject.layout.projectDirectory)
}.getOrElse("unknown")

tasks.withType<Jar>().configureEach {
    manifest {
        attributes("Implementation-Version" to rootProject.version.toString(), "Git-Commit" to gitCommit)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Minestom refuses to initialise its registries outside a real server process
    // unless this flag is set.
    jvmArgs("-Dminestom.inside-test=true")
    finalizedBy(tasks.matching { it.name == "jacocoTestReport" })
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.matching { it.name == "test" })
    reports {
        xml.required.set(true)
    }
}
