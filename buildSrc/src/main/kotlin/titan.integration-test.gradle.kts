// Gradle JVM test suite "integrationTest" for modules whose tests need Docker (Testcontainers).
// "test" stays free of Docker and network; "check" runs both.

import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = libs.findLibrary(alias).get()

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(platform(lib("aonyx-bom")))
                implementation(lib("junit.api"))
                implementation(lib("testcontainers-postgresql"))
                implementation(lib("testcontainers-junit"))
                implementation(lib("logback-classic"))
                runtimeOnly(lib("junit.platform.launcher"))
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
