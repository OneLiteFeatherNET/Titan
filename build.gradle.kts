import com.diffplug.gradle.spotless.SpotlessExtension
import java.nio.file.Files
import net.onelitefeather.titan.buildsrc.quality.PmdRatchet
import org.gradle.api.GradleException

plugins {
    id("com.diffplug.spotless") version "8.10.4" apply false
    // No "id(\"com.gradleup.shadow\") ... apply false" here: buildSrc's own
    // "implementation(\"com.gradleup.shadow:shadow-gradle-plugin:...\")" (needed so the
    // titan.app-variant convention plugin can apply Shadow) already puts the plugin on every
    // subproject's classpath; a second, versioned request for the same plugin here conflicts with
    // that ("already on the classpath with an unknown version"). :setup applies it the same,
    // version-less way titan.app-variant does.
}

// release-please bumps the annotated version line below on each release.
allprojects {
    version = "4.12.0" // x-release-please-version
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    // Deferred until the java plugin is actually applied: the features/ and (later) apps/
    // directory scans in settings.gradle.kts create a synthetic aggregator project (":features")
    // for the parent of a nested path like ":features:protection" that carries no build file and
    // no java plugin of its own.
    plugins.withType<JavaBasePlugin> {
        configure<SpotlessExtension> {
            java {
                licenseHeaderFile("${rootDir}/header.java")
                removeUnusedImports()
                eclipse().configFile("${rootDir}/Default.xml")
            }
        }
    }
}

// One PMD ratchet for the whole build (openspec maintainability-gates, D4): it reads every module's PMD
// report and compares the counts with config/quality/pmd-baseline.properties. Every module's check runs it.
val pmdRatchet = tasks.register("pmdRatchet") {
    group = "verification"
    description = "Fails on PMD violations above the baseline; -PupdatePmdBaseline rewrites the baseline."
    dependsOn(provider { subprojects.mapNotNull { it.tasks.findByName("pmdMain") } })
    doLast {
        val baselineFile = rootDir.toPath().resolve("config/quality/pmd-baseline.properties")
        val actual = HashMap<String, Int>()
        val lines = HashMap<String, List<Int>>()
        subprojects.forEach { module ->
            val report = module.layout.buildDirectory.file("reports/pmd/main.xml").get().asFile.toPath()
            if (Files.isRegularFile(report)) {
                PmdRatchet.violationLines(report, rootDir.toPath()).forEach { (key, found) ->
                    actual[key] = found.size
                    lines[key] = found
                }
            }
        }
        if (project.hasProperty("updatePmdBaseline")) {
            PmdRatchet.writeBaseline(baselineFile, actual)
            logger.lifecycle("PMD baseline written to $baselineFile; review the diff before committing it.")
            return@doLast
        }
        val outcome = PmdRatchet.compare(actual, PmdRatchet.readBaseline(baselineFile))
        outcome.shrinkable().forEach { logger.warn("PMD baseline: $it (run ./gradlew pmdRatchet -PupdatePmdBaseline)") }
        if (!outcome.passed()) {
            val details = outcome.failures().map { failure ->
                val key = failure.substringBefore(" has ")
                "$failure, at lines ${lines[key]}"
            }
            throw GradleException(details.joinToString(separator = "\n", prefix = "PMD violations above the baseline:\n"))
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("pmdRatchet"))
    }
}
