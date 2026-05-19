rootProject.name = "titan"


dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://repository.derklaro.dev/snapshots/")
        maven("https://repository.derklaro.dev/releases/")
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
            version("aonyx-bom", "0.7.1")
            version("mycelium-bom", "1.6.4")
            version("cloudnet", "4.0.0-RC17-SNAPSHOT")
            version("butterfly", "1.0.23")

            version("luckperms", "5.6-SNAPSHOT")

            version("togglz", "4.6.1")
            version("caffeine", "3.2.3")

            version("tomcat-annotations-api", "6.0.53")

            version("mockito", "5.23.0")

            // Minestom
            library("aonyx-bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx-bom")
            library("mycelium-bom", "net.onelitefeather", "mycelium-bom").versionRef("mycelium-bom")
            library("minestom","net.minestom", "minestom").version("2026.05.17-1.21.11")
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

            library("luckperms.api", "net.luckperms", "api").versionRef("luckperms")
            library("luckperms.minestom", "net.luckperms", "minestom").versionRef("luckperms")
            library("luckperms.minestom.app", "net.luckperms", "minestom-app").versionRef("luckperms")
            library("luckperms.common", "net.luckperms", "common").versionRef("luckperms")
            library("luckperms.common.loader.utils", "net.luckperms", "loader-utils").versionRef("luckperms")
            library("luckperms.minestom.loader", "net.luckperms", "minestom-loader").versionRef("luckperms")

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
