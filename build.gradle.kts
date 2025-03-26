import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    alias(libs.plugins.publishdata)
    id("info.solidsoft.pitest") version "1.15.0" apply false
}

group = "net.onelitefeather.titan"
version = "2.1.0"

publishData {
    addBuildData()
    addMainRepo("https://repo.onelitefeather.dev/onelitefeather-releases")
    addMasterRepo("https://repo.onelitefeather.dev/onelitefeather-releases")
    addSnapshotRepo("https://repo.onelitefeather.dev/onelitefeather-snapshots")
    publishTask("shadowJar")
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

        if (project.name in listOf("common", "api", "agones", "setup")) {
            failWhenNoMutations.set(false)
        }
        targetClasses.add("net.onelitefeather.titan.*")
    }
}