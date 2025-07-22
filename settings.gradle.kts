rootProject.name = "titan"


dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://repository.derklaro.dev/snapshots/")
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
            version("aonyx-bom", "0.5.1")
            version("mycelium-bom", "1.4.2")
            version("cloudnet", "4.0.0-RC14-SNAPSHOT")
            version("butterfly", "1.0.2")

            version("togglz", "4.4.0")
            version("caffeine", "3.2.2")

            version("tomcat-annotations-api", "6.0.53")

            version("mockito", "5.18.0")

            // Minestom
            library("aonyx-bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx-bom")
            library("mycelium-bom", "net.onelitefeather", "mycelium-bom").versionRef("mycelium-bom")
            library("minestom","net.minestom", "minestom").withoutVersion()
            library("aves", "net.theevilreaper", "aves").version("1.9.0")
            library("adventure.minimessage", "net.kyori", "adventure-text-minimessage").withoutVersion()
            library("butterfly-minestom", "net.onelitefeather", "butterfly-minestom").versionRef("butterfly")

            library("togglz", "org.togglz", "togglz-core").versionRef("togglz")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").versionRef("caffeine")
            library("tomcat-annotations-api", "org.apache.tomcat", "annotations-api").versionRef("tomcat-annotations-api")

            library("cloudnet-bom", "eu.cloudnetservice.cloudnet", "bom").versionRef("cloudnet")
            library("cloudnet-bridge", "eu.cloudnetservice.cloudnet", "bridge-api").withoutVersion()
            library("cloudnet-bridge-impl", "eu.cloudnetservice.cloudnet", "bridge-impl").withoutVersion()
            library("cloudnet-driver-impl", "eu.cloudnetservice.cloudnet", "driver-impl").withoutVersion()
            library("cloudnet-platform-inject", "eu.cloudnetservice.cloudnet", "platform-inject-api").withoutVersion()
            library("cloudnet-jvm-wrapper", "eu.cloudnetservice.cloudnet", "wrapper-jvm-api").withoutVersion()

            library("junit.api", "org.junit.jupiter", "junit-jupiter-api").withoutVersion()
            library("junit.engine", "org.junit.jupiter", "junit-jupiter-engine").withoutVersion()
            library("junit.params", "org.junit.jupiter", "junit-jupiter-params").withoutVersion()
            library("junit.platform.launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()


            library("cyano", "net.onelitefeather", "cyano").withoutVersion()
            library("mockito", "org.mockito", "mockito-core").versionRef("mockito")
        }
    }
}

include("app")
include("api")
include("common")
include("setup")

findProject(":app")?.projectDir = file("app")
