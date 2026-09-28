import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "8.10.3" apply false
    // Declared once here so :app and :setup can apply it without repeating the version.
    id("com.gradleup.shadow") version "9.6.1" apply false
}

// gradle.properties carries the release-please annotation inline
// (`version = 1.10.x # x-release-please-version`). A mid-line `#` is not a
// comment in .properties files, so Gradle reads it as part of the version -
// strip it (for every project) so the published artifact version is clean.
allprojects {
    version = (version as String).substringBefore('#').trim()
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

// :app depends on every features/* column found by the settings.gradle.kts scan, so a new column
// (or a column moving out of :app in a later wave) needs no change to app/build.gradle.kts.
// Wired here, after every project is configured, so :app's "implementation" configuration already
// exists.
@Suppress("UNCHECKED_CAST")
val titanFeatureProjectPaths = gradle.extensions.extraProperties["titanFeatureProjectPaths"] as List<String>

gradle.projectsEvaluated {
    val app = project(":app")
    titanFeatureProjectPaths.forEach { featurePath ->
        app.dependencies.add("implementation", app.dependencies.project(featurePath))
    }
}
