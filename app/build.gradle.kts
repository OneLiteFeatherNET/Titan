import java.nio.file.Files

plugins {
    id("titan.java-conventions")
    id("titan.publish-conventions")
    id("com.gradleup.shadow")
    application
}

dependencies {
    compileOnly(libs.luckperms.api) {
        exclude(group = "net.kyori.adventure")
    }
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(platform(libs.aonyx.bom))
    implementation(libs.togglz)
    implementation(libs.aves)
    implementation(libs.adventure.minimessage)
    implementation(libs.caffeine)
    implementation(libs.minestom)
    implementation(platform(libs.minestom.extensions.bom))
    implementation(libs.minestom.extensions)
    implementation(libs.butterfly.minestom)

    runtimeOnly(libs.luckperms.minestom.loader) {
        exclude(group = "net.kyori.adventure")
    }
    compileOnly(libs.luckperms.minestom.loader) {
        exclude(group = "net.kyori.adventure")
    }


    // CloudNet is provided by the CloudNet wrapper at runtime and the bridge is
    // loaded as a Minestom extension (separate classloader), so :app neither
    // references nor bundles it.
    // Guava was previously pulled in transitively by CloudNet; LuckPerms expects
    // it (unrelocated) on the classpath, so bundle it explicitly now.
    implementation(libs.guava)

    testImplementation(platform(libs.aonyx.bom))
    testImplementation(libs.minestom)
    testImplementation(libs.aves)
    testImplementation(libs.cyano)
    testImplementation(libs.mockito)

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

// The LuckPerms minestom-loader is a JarInJar bootstrap that bundles an
// unrelocated, outdated Gson. Tests don't load LuckPerms, but as a runtimeOnly
// dependency the loader leaks into the test runtime classpath where its bundled
// Gson shadows the real one and breaks Minestom's registry init
// (GsonBuilder.disableJdkUnsafe NoSuchMethodError). Keep it off the test path.
configurations.testRuntimeClasspath {
    exclude(group = "net.luckperms", module = "minestom-loader")
}

application {
    mainClass.set("net.onelitefeather.titan.app.TitanApplication")
}

tasks {
    jar {
        archiveClassifier.set("unshaded")
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("app-titan.jar")
        mergeServiceFiles()
        // Shaded deps ship signed and multi-release jars that break a
        // relocation-free application fat jar; drop signatures and module-info.
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("module-info.class", "META-INF/versions/**/module-info.class")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

// ---- Ahead-of-Time cache (JDK 25 / JEP 514) for faster lobby startup ----
// A training run boots the shaded lobby once and records a portable AOT cache.
// Trained with the relative classpath "app-titan.jar" so the cache stays valid
// for any deployment launched as:
//   java -XX:AOTCache=app-titan.aot -jar app-titan.jar
// (Cache is tied to the JDK 25 build and this jar; regenerated on every build.)
val aotTrainSeconds = providers.gradleProperty("titan.aot.trainSeconds").orElse("20")
val aotRunDir = layout.buildDirectory.dir("aot")
val aotCacheFile = layout.buildDirectory.file("aot/app-titan.aot")

val generateAotCache = tasks.register<Exec>("generateAotCache") {
    group = "build"
    description = "Generates a JDK 25 AOT cache (app-titan.aot) for faster lobby startup."

    val shadowJarTask = tasks.named("shadowJar")
    dependsOn(shadowJarTask)
    val jarProvider = shadowJarTask.flatMap { (it as Jar).archiveFile }
    val worldsDir = rootProject.layout.projectDirectory.dir("worlds")
    inputs.file(jarProvider)
    inputs.dir(worldsDir)
    outputs.file(aotCacheFile)

    val launcher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }
    val runDir = aotRunDir.get().asFile
    val rootDir = rootProject.projectDir
    val trainSeconds = aotTrainSeconds
    workingDir = runDir

    doFirst {
        runDir.deleteRecursively()
        runDir.mkdirs()
        // Relative classpath: the cache records "app-titan.jar", matching the
        // deployment launch command above.
        jarProvider.get().asFile.copyTo(runDir.resolve("app-titan.jar"), overwrite = true)
        // The lobby loads worlds/ (+ app.json) relative to the CWD while booting.
        Files.createSymbolicLink(runDir.resolve("worlds").toPath(), rootDir.resolve("worlds").toPath())
        rootDir.resolve("app.json").takeIf { it.exists() }?.copyTo(runDir.resolve("app.json"), overwrite = true)
        executable = launcher.get().executablePath.asFile.absolutePath
        args(
            "-Dtitan.aot.trainSeconds=${trainSeconds.get()}",
            "-XX:AOTCacheOutput=app-titan.aot",
            "-jar", "app-titan.jar"
        )
    }
}

publishing.publications.named<MavenPublication>("maven") {
    artifactId = "titan-app"
    artifact(tasks.shadowJar)
    // AOT cache shipped alongside the jar for faster startup; see generateAotCache.
    artifact(aotCacheFile) {
        classifier = "aot"
        extension = "aot"
        builtBy(generateAotCache)
    }
    pom {
        name = "Titan App"
        description = "Titan App Server for OneLiteFeather"
    }
}
