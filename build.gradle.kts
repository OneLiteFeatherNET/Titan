import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "7.2.1" apply false
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<SpotlessExtension> {
        java {
            licenseHeaderFile("${rootDir}/header.java")
            removeUnusedImports()
            eclipse()
        }
    }
}