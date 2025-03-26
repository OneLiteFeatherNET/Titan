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
            version("microtus-bom", "1.5.1")
            version("publishdata", "1.4.0")
            version("aves", "1.6.1")

            version("togglz", "4.4.0")
            version("caffeine", "3.2.0")

            version("agones4j", "2.0.2")
            version("grpc", "1.68.0")
            version("tomcat-annotations-api", "6.0.53")

            version("mockito", "5.16.1")

            // Minestom
            library("microtus-bom", "net.onelitefeather.microtus", "bom").versionRef("microtus-bom")
            library("microtus", "net.onelitefeather.microtus", "Microtus").withoutVersion()
            library("aves", "de.icevizion.lib", "aves").versionRef("aves")

            library("togglz", "org.togglz", "togglz-core").versionRef("togglz")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").versionRef("caffeine")

            library("agones4j", "net.infumia", "agones4j").versionRef("agones4j")
            library("grpc.stub", "io.grpc", "grpc-stub").versionRef("grpc")
            library("grpc.protobuf", "io.grpc", "grpc-protobuf").versionRef("grpc")
            library("grpc.netty", "io.grpc", "grpc-netty").versionRef("grpc")
            library("grpc.okhttp", "io.grpc", "grpc-okhttp").versionRef("grpc")
            library("tomcat-annotations-api", "org.apache.tomcat", "annotations-api").versionRef("tomcat-annotations-api")

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
