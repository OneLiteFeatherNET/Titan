rootProject.name = "titan"


dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // minestom-extensions is published here and, unlike the onelitefeather repository
        // below, needs no credentials - keep it separate so a fresh clone resolves it.
        maven {
            name = "OneLiteFeatherReleases"
            url = uri("https://repo.onelitefeather.dev/releases")
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
            version("cloudnet", "4.0.0-RC18-SNAPSHOT")
            version("butterfly", "1.0.23")

            version("luckperms", "5.6-SNAPSHOT")

            version("togglz", "4.6.4")
            version("caffeine", "3.2.4")

            version("tomcat-annotations-api", "6.0.53")

            version("guava", "33.7.1-jre")
            version("minestom-extensions", "2.2.0")

            version("mockito", "5.23.0")

            version("slf4j", "2.0.18")
            version("logback", "1.5.31")
            version("sentry", "8.30.0")

            // Minestom
            library("aonyx-bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx-bom")
            library("minestom","net.minestom", "minestom").withoutVersion()
            // OneLiteFeather fork of the archived hollow-cube/minestom-ce-extensions. Same
            // packages (net.hollowcube.minestom.extensions, net.minestom.server.extensions), but
            // extension dependencies resolve through Maven Resolver instead of the Kotlin-based
            // DependencyGetter, so no Kotlin stdlib is needed on the class path anymore.
            library("minestom-extensions-bom", "net.onelitefeather", "minestom-extensions-bom").versionRef("minestom-extensions")
            library("minestom-extensions", "net.onelitefeather", "minestom-extensions").withoutVersion()
            // Generates extension.json from @ExtensionInfo at compile time; source retention, so
            // the annotation itself never reaches the extension jar.
            library("minestom-extensions-processor", "net.onelitefeather", "minestom-extensions-processor").withoutVersion()
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

            // Logging. slf4j-api used to arrive transitively through Minestom and no binding was
            // ever declared, so the shipped fat jars logged nothing at all ("No SLF4J providers
            // were found"). Declare both explicitly: the API where code compiles against it, the
            // Logback binding as runtimeOnly in the two application modules.
            library("slf4j-api", "org.slf4j", "slf4j-api").versionRef("slf4j")
            library("logback-classic", "ch.qos.logback", "logback-classic").versionRef("logback")

            // Error reporting. sentry-logback is the appender referenced from logback.xml; it
            // pulls io.sentry:sentry, which TitanObservability compiles against.
            library("sentry-bom", "io.sentry", "sentry-bom").versionRef("sentry")
            library("sentry", "io.sentry", "sentry").withoutVersion()
            library("sentry-logback", "io.sentry", "sentry-logback").withoutVersion()
        }
    }
}

include("app")
include("api")
include("common")
include("setup")
include("bridge")

findProject(":app")?.projectDir = file("app")
