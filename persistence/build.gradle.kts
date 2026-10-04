plugins {
    id("titan.java-conventions")
    id("titan.integration-test")
    `java-library`
    `java-test-fixtures`
}

dependencies {
    // SessionFactory is the type other modules inject, so Hibernate is part of the API.
    api(libs.hibernate.core)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    // Context only: DatabaseWriter hands the caller's span to its worker thread. The agent
    // provides the implementation at runtime.
    implementation(platform(libs.opentelemetry.bom))
    implementation(libs.opentelemetry.api)

    implementation(libs.avaje.inject)
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)
    annotationProcessor(libs.avaje.inject.generator)

    testFixturesImplementation(libs.avaje.inject)
    testFixturesImplementation(libs.avaje.config)

    testImplementation(testFixtures(project()))
    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.logback.classic)
    testRuntimeOnly(libs.snakeyaml)
    testRuntimeOnly(libs.junit.engine)
}

// Integration tests build the same Avaje scope as production, so they see the same dependencies.
configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.implementation.get())
}
dependencies {
    "integrationTestImplementation"(testFixtures(project()))
}
dependencies {
    "integrationTestRuntimeOnly"(libs.snakeyaml)
}
