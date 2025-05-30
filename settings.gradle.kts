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
            url = uri("https://repo.onelitefeather.dev/onelitefeather-releases")
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
            version("microtus-bom", "1.5.1")
            version("publishdata", "1.4.0")
            version("aves", "1.6.1")
            version("cloudnet", "4.0.0-RC11.2")

            version("togglz", "4.4.0")
            version("caffeine", "3.2.0")

            version("grpc", "1.73.0")
            version("tomcat-annotations-api", "6.0.53")

            version("mockito", "5.17.0")

            // Minestom
            library("microtus-bom", "net.onelitefeather.microtus", "bom").versionRef("microtus-bom")
            library("microtus", "net.onelitefeather.microtus", "Microtus").withoutVersion()
            library("aves", "de.icevizion.lib", "aves").versionRef("aves")

            library("togglz", "org.togglz", "togglz-core").versionRef("togglz")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").versionRef("caffeine")

            library("grpc.stub", "io.grpc", "grpc-stub").versionRef("grpc")
            library("grpc.protobuf", "io.grpc", "grpc-protobuf").versionRef("grpc")
            library("grpc.netty", "io.grpc", "grpc-netty").versionRef("grpc")
            library("grpc.okhttp", "io.grpc", "grpc-okhttp").versionRef("grpc")
            library("tomcat-annotations-api", "org.apache.tomcat", "annotations-api").versionRef("tomcat-annotations-api")

            library("cloudnet-bom", "eu.cloudnetservice.cloudnet", "bom").versionRef("cloudnet")
            library("cloudnet-bridge", "eu.cloudnetservice.cloudnet", "bridge").withoutVersion()
            library("cloudnet-jvm-wrapper", "eu.cloudnetservice.cloudnet", "wrapper-jvm").withoutVersion()


            library("microtus.testing", "net.onelitefeather.microtus.testing", "testing").withoutVersion()
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
