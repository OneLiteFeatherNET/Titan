// The Otis settings adapter: the player settings store that talks to Otis over java.net.http.
// It needs only core; the feature column that uses the store is in features/settings.
plugins {
    id("titan.java-conventions")
    id("titan.integration-test")
    `java-library`
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.minestom)
    implementation(libs.slf4j.api)
    // OtisSettings reads otis.* through Config; the JSON mapping uses Gson, declared here explicitly.
    implementation(libs.avaje.config)
    implementation(libs.gson)

    // Compile-time dependency injection; see openspec/changes/avaje-dependency-injection.
    implementation(libs.avaje.inject)
    annotationProcessor(libs.avaje.inject.generator)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.archunit)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    // ListAppender, for the log-redaction test that captures what the adapter logs.
    testImplementation(libs.logback.classic)
    testRuntimeOnly(libs.junit.engine)
}

// The integrationTest suite runs the adapter against a JDK HttpServer on a loopback port 0; it
// does not inherit main's implementation dependencies, so the ones it compiles against are listed.
testing {
    suites {
        named<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project(":core"))
                implementation(libs.minestom)
                implementation(libs.gson)
                implementation(libs.slf4j.api)
            }
        }
    }
}
