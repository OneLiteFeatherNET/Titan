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
            version("aves", "1.3.1+48463bee")
            version("guava", "32.1.2-jre")
            version("cloudnet", "4.0.0-RC9")

            // Minestom
            library("minestom", "net.onelitefeather.microtus", "Minestom").versionRef("minestom")
            library("adventure.minimessage", "net.kyori", "adventure-text-minimessage").versionRef("adventure")
            library("aves", "de.icevizion.lib", "Aves").versionRef("aves")
            library("guava", "com.google.guava", "guava").versionRef("guava")

            library("cloudnet.bridge", "eu.cloudnetservice.cloudnet", "bridge").versionRef("cloudnet")
            library("cloudnet.wrapper-jvm", "eu.cloudnetservice.cloudnet", "wrapper-jvm").versionRef("cloudnet")
            library("cloudnet.driver", "eu.cloudnetservice.cloudnet", "driver").versionRef("cloudnet")

            plugin("publishdata", "de.chojo.publishdata").versionRef("publishdata")
            plugin("shadow", "com.github.johnrengelman.shadow").versionRef("shadow")
        }
    }
}
