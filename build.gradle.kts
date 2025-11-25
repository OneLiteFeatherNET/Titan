import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "8.1.0" apply false
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