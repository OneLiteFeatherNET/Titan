rootProject.name = "titan"


dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // minestom-ce-extensions pulls com.github.Minestom:DependencyGetter from JitPack;
        // resolve it through the OneLiteFeather reposilite proxy that caches JitPack.
        maven {
            name = "reposiliteRepositoryOnelitefeatherProxy"
            url = uri("https://repo.onelitefeather.dev/onelitefeather-proxy")
        }
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
            version("aonyx-bom", "0.8.0")
            version("cloudnet", "4.0.0-RC17-SNAPSHOT")
            version("butterfly", "1.0.23")

            version("luckperms", "5.6-SNAPSHOT")

            version("togglz", "4.6.2")
            version("caffeine", "3.2.4")

            version("tomcat-annotations-api", "6.0.53")

            version("guava", "33.6.0-jre")
            version("kotlin", "2.2.21")

            version("mockito", "5.23.0")

            // Minestom
            library("aonyx-bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx-bom")
            library("minestom","net.minestom", "minestom").withoutVersion()
            library("minestom-ce-extensions", "dev.hollowcube", "minestom-ce-extensions").version("1.2.0")
            library("aves", "net.theevilreaper", "aves").withoutVersion()
            library("adventure.minimessage", "net.kyori", "adventure-text-minimessage").withoutVersion()
            library("butterfly-minestom", "net.onelitefeather", "butterfly-minestom").versionRef("butterfly")

            library("togglz", "org.togglz", "togglz-core").versionRef("togglz")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").versionRef("caffeine")
            library("tomcat-annotations-api", "org.apache.tomcat", "annotations-api").versionRef("tomcat-annotations-api")

            library("cloudnet-bom", "eu.cloudnetservice.cloudnet", "bom").versionRef("cloudnet")
            library("cloudnet-bridge", "eu.cloudnetservice.cloudnet", "bridge-api").withoutVersion()
            library("cloudnet-bridge-impl", "eu.cloudnetservice.cloudnet", "bridge-impl").withoutVersion()
            library("cloudnet-driver-api", "eu.cloudnetservice.cloudnet", "driver-api").withoutVersion()
            library("cloudnet-driver-impl", "eu.cloudnetservice.cloudnet", "driver-impl").withoutVersion()
            library("cloudnet-platform-inject", "eu.cloudnetservice.cloudnet", "platform-inject-api").withoutVersion()
            library("cloudnet-jvm-wrapper", "eu.cloudnetservice.cloudnet", "wrapper-jvm-api").withoutVersion()

            library("junit.api", "org.junit.jupiter", "junit-jupiter-api").withoutVersion()
            library("junit.engine", "org.junit.jupiter", "junit-jupiter-engine").withoutVersion()
            library("junit.params", "org.junit.jupiter", "junit-jupiter-params").withoutVersion()
            library("junit.platform.launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()

            library("luckperms.api", "net.luckperms", "api").versionRef("luckperms")
            library("luckperms.minestom.loader", "net.luckperms", "minestom-loader").versionRef("luckperms")

            library("cyano", "net.onelitefeather", "cyano").withoutVersion()
            library("mockito", "org.mockito", "mockito-core").versionRef("mockito")

            // Guava: unrelocated, expected by LuckPerms (was transitive via CloudNet).
            library("guava", "com.google.guava", "guava").versionRef("guava")
            // Kotlin stdlib: needed by minestom-ce-extensions' MavenRepository.kt.
            library("kotlin-stdlib-jdk8", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin")
        }
    }
}

include("app")
include("api")
include("common")
include("setup")
include("bridge")

findProject(":app")?.projectDir = file("app")
