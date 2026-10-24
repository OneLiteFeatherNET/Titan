// Shared Java setup for every Titan module. Previously this block was copied into
// each module build file: the toolchain five times, the test configuration four
// times, and Jacoco into only two of the five modules.

import net.onelitefeather.titan.buildsrc.architecture.ArchitectureTestGuard
import net.onelitefeather.titan.buildsrc.architecture.ModuleBoundaries
import org.gradle.api.GradleException
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.ProjectDependency

plugins {
    java
    jacoco
    pmd
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Production code only: the PMD gate covers src/main (openspec maintainability-gates, D4).
pmd {
    toolVersion = extensions.getByType<VersionCatalogsExtension>().named("libs").findVersion("pmd").get().requiredVersion
    ruleSetFiles = rootProject.files("config/quality/pmd-ruleset.xml")
    sourceSets = listOf(java.sourceSets.getByName("main"))
    isConsoleOutput = true
    isIgnoreFailures = true
}

tasks.withType<Pmd>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}

// Checked while configuring, so a violation fails with the module named instead of surfacing later as a
// compile or runtime problem: forbidden project dependencies (design D2) and a missing architecture test (D3).
afterEvaluate {
    val boundaryViolations = configurations
        .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
        .mapNotNull { ModuleBoundaries.violation(project.path, it.path).orElse(null) }
    val guardViolations = ArchitectureTestGuard.violation(projectDir.toPath(), project.path).stream().toList()
    val violations = boundaryViolations + guardViolations
    if (violations.isNotEmpty()) {
        throw GradleException(violations.joinToString(separator = "\n"))
    }
}

// TitanObservability reports this as the Sentry release, so an issue can be traced back to the
// deploy that introduced it. Package.getImplementationVersion() reads it from the jar the classes
// were loaded from, which for both server processes is the shaded jar. Git-Commit is not set here:
// it only belongs on the shaded application jars (see titan.app-variant and :setup), so a commit
// does not rebuild every library jar.
tasks.withType<Jar>().configureEach {
    manifest {
        attributes("Implementation-Version" to rootProject.version.toString())
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.withType<Test>().configureEach {
    // ArchUnit freezing rules (openspec maintainability-gates, D5): the baseline is committed under
    // src/test/resources and never created or rewritten by a normal build.
    systemProperty("archunit.freeze.store.default.path", "src/test/resources/archunit_store")
    systemProperty("archunit.freeze.store.default.allowStoreCreation", "false")
    // Only a deliberate run changes a store: ./gradlew test -ParchunitStoreUpdate=true, then review the diff.
    systemProperty("archunit.freeze.store.default.allowStoreUpdate", providers.gradleProperty("archunitStoreUpdate").getOrElse("false"))
    useJUnitPlatform()
    // Minestom refuses to initialise its registries outside a real server process
    // unless this flag is set.
    jvmArgs("-Dminestom.inside-test=true")
    finalizedBy(tasks.matching { it.name == "jacocoTestReport" })
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Parallelism comes from forked JVMs only: Minestom keeps its server in static state, so Env tests
// must never share a JVM concurrently. Half the cores, capped at 4, keeps forks x 512 MiB default
// heap well inside a 7 GB runner. Docker-backed integrationTest tasks keep one fork (one container).
tasks.named<Test>("test") {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
}

tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.matching { it.name == "test" })
    reports {
        xml.required.set(true)
    }
}
