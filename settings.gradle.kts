rootProject.name = "titan"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}


dependencyResolutionManagement {
    if (System.getenv("CI") != null) {
        repositoriesMode = RepositoriesMode.PREFER_SETTINGS
        repositories {
            maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            maven("https://repo.htl-md.schule/repository/Gitlab-Runner/")
            maven {
                val groupdId = 28 // Gitlab Group
                val ciApiv4Url = System.getenv("CI_API_V4_URL")
                url = uri("$ciApiv4Url/groups/$groupdId/-/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class.java) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }

    versionCatalogs {
        create("libs") {
            version("microtus-bom", "1.5.0-SNAPSHOT")
            version("publishdata", "1.4.0")
            version("aves", "1.5.0")

            version("togglz", "4.4.0")
            version("caffeine", "3.1.8")

            // Minestom
            library("microtus-bom", "net.onelitefeather.microtus", "bom").versionRef("microtus-bom")
            library("microtus", "net.onelitefeather.microtus", "Microtus").withoutVersion()
            library("aves", "de.icevizion.lib", "aves").versionRef("aves")

            library("togglz", "org.togglz", "togglz-core").versionRef("togglz")
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").versionRef("caffeine")

            plugin("publishdata", "de.chojo.publishdata").versionRef("publishdata")
        }
    }
}
