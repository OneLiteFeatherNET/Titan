import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("info.solidsoft.pitest") version "1.15.0" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "info.solidsoft.pitest")

    configure<PitestPluginExtension> {
        pitestVersion.set("1.16.3")
        junit5PluginVersion.set("1.2.1")
        threads.set(8)
        enableDefaultIncrementalAnalysis.set(true)

        outputFormats.addAll("XML", "HTML")
        timestampedReports.set(false)

        if (project.name in listOf("common", "api", "setup")) {
            failWhenNoMutations.set(false)
        }
        targetClasses.add("net.onelitefeather.titan.*")
    }
    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(24))
    }
}