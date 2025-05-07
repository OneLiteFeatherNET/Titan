rootProject.name = "titan"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}


dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "OneLiteFeatherRepository"
            url = uri("https://repo.onelitefeather.dev/onelitefeather")
            if (System.getenv("CI") != null) {
                credentials {
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            } else {
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }

    versionCatalogs {
        create("libs") {
            version("aonyx-bom", "0.3.1")
            version("mycelium-bom", "1.2.3")
            version("publishdata", "1.4.0")
            version("cloudnet", "4.0.0-RC11.2")

            version("togglz", "4.4.0")
            version("caffeine", "3.2.0")

            version("tomcat-annotations-api", "6.0.53")

            version("mockito", "5.17.0")

            // Minestom
            library("aonyx-bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx-bom")
            library("mycelium-bom", "net.onelitefeather", "mycelium-bom").versionRef("mycelium-bom")
            library("minestom","net.minestom", "minestom-snapshots").withoutVersion()
            library("aves", "net.theevilreaper", "aves").withoutVersion()
            library("adventure.minimessage", "net.kyori", "adventure-text-minimessage").withoutVersion()

            library("togglz", "org.togglz", "togglz-core").versionRef("togglz")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").versionRef("caffeine")
            library("tomcat-annotations-api", "org.apache.tomcat", "annotations-api").versionRef("tomcat-annotations-api")

            library("cloudnet-bom", "eu.cloudnetservice.cloudnet", "bom").versionRef("cloudnet")
            library("cloudnet-bridge", "eu.cloudnetservice.cloudnet", "bridge").withoutVersion()
            library("cloudnet-jvm-wrapper", "eu.cloudnetservice.cloudnet", "wrapper-jvm").withoutVersion()


            library("cyano", "net.onelitefeather", "cyano").withoutVersion()
            library("mockito", "org.mockito", "mockito-core").versionRef("mockito")

            plugin("publishdata", "de.chojo.publishdata").versionRef("publishdata")
        }
    }
}
include("app")
include("common")
include("api")
include("setup")

findProject(":app")?.projectDir = file("app")
