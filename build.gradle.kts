import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "8.10.3" apply false
    // No "id(\"com.gradleup.shadow\") ... apply false" here: buildSrc's own
    // "implementation(\"com.gradleup.shadow:shadow-gradle-plugin:...\")" (needed so the
    // titan.app-variant convention plugin can apply Shadow) already puts the plugin on every
    // subproject's classpath; a second, versioned request for the same plugin here conflicts with
    // that ("already on the classpath with an unknown version"). :setup applies it the same,
    // version-less way titan.app-variant does.
}

// release-please bumps the annotated version line below on each release.
allprojects {
    version = "2.3.0" // x-release-please-version
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
