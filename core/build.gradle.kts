plugins {
    id("titan.java-conventions")
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(platform(libs.aonyx.bom))
    api(libs.minestom)
    api(platform(libs.opentelemetry.bom))
    api(libs.opentelemetry.api)

    // FeatureNode wraps every listener in ListenerGuard, which reports a failure via SLF4J/MDC.
    implementation(libs.slf4j.api)

    // LabelPlaceholders exposes MiniMessage and TagResolver in its public API.
    api(libs.adventure.minimessage)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(testFixtures(project()))
    testImplementation(platform(libs.opentelemetry.bom))
    testImplementation(libs.cyano)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.archunit)
    // ListAppender, for capturing a log line in a test; logback-classic itself has no runtime use here.
    testImplementation(libs.logback.classic)
    testRuntimeOnly(libs.junit.engine)

    testFixturesImplementation(platform(libs.aonyx.bom))
    testFixturesImplementation(libs.minestom)
    testFixturesImplementation(platform(libs.opentelemetry.bom))
    testFixturesApi(libs.opentelemetry.sdk.testing)
    testFixturesImplementation(libs.cyano)
    testFixturesImplementation(libs.archunit)
    // ColumnArchitectureRules references BeanScope/PostConstruct/Singleton, not to depend on them.
    testFixturesImplementation(libs.avaje.inject)
    testFixturesImplementation(libs.junit.api)
    // CapturedLog's public API exposes logback's ILoggingEvent, so consumers need it on their classpath.
    testFixturesApi(libs.logback.classic)
    // ActiveLobby stubs the MapProvider and LobbyMap of :common. compileOnly keeps :common off the
    // test classpath of every column; the apps that use ActiveLobby bring :common and Mockito themselves.
    testFixturesCompileOnly(project(":common"))
    testFixturesCompileOnly(libs.mockito)
}

// The public API of core is documented (openspec maintainability-gates, design D6). The core-only ruleset
// adds CommentRequired on top of the shared PMD rules.
pmd {
    ruleSetFiles = rootProject.files("config/quality/pmd-ruleset.xml", "config/quality/pmd-core-api-ruleset.xml")
}

// doclint's "missing" group would also demand @param and @return on every method, which the requirement does
// not ask for; CommentRequired checks the presence of a comment. The remaining groups are errors in the docs.
tasks.javadoc {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
    (options as StandardJavadocDocletOptions).addBooleanOption("Werror", true)
}

tasks.check {
    dependsOn(tasks.javadoc)
}
