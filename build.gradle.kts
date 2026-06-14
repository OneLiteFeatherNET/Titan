import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "8.6.0" apply false
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

    configure<SpotlessExtension> {
        java {
            licenseHeaderFile("${rootDir}/header.java")
            removeUnusedImports()
            eclipse().configFile("${rootDir}/Default.xml")
        }
    }
}