rootProject.name = "titan"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("minestom", "1.1.3")
            version("publishdata", "1.4.0")
            version("shadow", "8.1.1")
            version("adventure", "4.14.0")

            // Minestom
            library("minestom", "net.onelitefeather.microtus", "Minestom").versionRef("minestom")
            library("adventure.minimessage", "net.kyori", "adventure-text-minimessage").versionRef("adventure")

            plugin("publishdata", "de.chojo.publishdata").versionRef("publishdata")
            plugin("shadow", "com.github.johnrengelman.shadow").versionRef("shadow")
        }
    }
}
