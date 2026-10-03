import net.onelitefeather.titan.buildsrc.config.MergeApplicationDefaultsTask

plugins {
    id("titan.column")
    id("titan.integration-test")
    `java-test-fixtures`
}

dependencies {
    // PersistenceUnit, and Hibernate (SessionFactory, jakarta.persistence) through its api.
    implementation(project(":persistence"))

    // JumprunSettings reads jumprun.* from an io.avaje.config Configuration; titan.column does not
    // pull it in because most columns don't need it.
    implementation(libs.avaje.config)
    implementation(libs.slf4j.api)

    // FakeRunStore and the RunStoreContract both stores must satisfy.
    testFixturesImplementation(platform(libs.aonyx.bom))
    testFixturesImplementation(libs.junit.api)

    // ListAppender, for asserting captured log lines.
    testImplementation(libs.logback.classic)

    // Real YAML loading in the tests, as in production, where apps/cloudnet brings it transitively.
    testRuntimeOnly(libs.snakeyaml)
}

// Integration tests build the Avaje scope like production, so they see the production dependencies.
configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.implementation.get())
}
dependencies {
    // ConfigurationProperties: a scope builder with one Configuration per test.
    "integrationTestImplementation"(testFixtures(project(":persistence")))
    // Minestom registries, for the column beans that build items.
    "integrationTestImplementation"(libs.cyano)
    // The RunStoreContract the Postgres store must satisfy.
    "integrationTestImplementation"(testFixtures(project()))
    "integrationTestRuntimeOnly"(libs.snakeyaml)
}

// JumprunModule reads the shipped jumprun.* defaults at startup, like the unit tests do.
sourceSets.named("integrationTest") {
    resources.srcDir(tasks.named<MergeApplicationDefaultsTask>("mergeTestDefaults").map { it.outputDir })
}
