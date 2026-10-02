plugins {
    id("titan.java-conventions")
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(platform(libs.aonyx.bom))
    api(libs.minestom)

    // FeatureNode wraps every listener in ListenerGuard, which reports a failure via SLF4J/MDC.
    implementation(libs.slf4j.api)

    // LabelPlaceholders exposes MiniMessage and TagResolver in its public API.
    api(libs.adventure.minimessage)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
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
    testFixturesImplementation(libs.cyano)
    testFixturesImplementation(libs.archunit)
    // ColumnArchitectureRules references BeanScope/PostConstruct/Singleton, not to depend on them.
    testFixturesImplementation(libs.avaje.inject)
    testFixturesImplementation(libs.junit.api)
}
