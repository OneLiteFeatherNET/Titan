import java.nio.file.Files

plugins {
    java
    application
    id("com.gradleup.shadow") version "9.6.0"
    `maven-publish`
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
    implementation(libs.minestom.ce.extensions)
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
    // minestom-ce-extensions loads extension dependencies via a Kotlin class
    // (net.minestom.dependencies.maven.MavenRepository); without the Kotlin stdlib
    // on the classpath ExtensionBootstrap init crashes with
    // NoClassDefFoundError: kotlin/jvm/internal/Intrinsics. Bundle it.
    implementation(libs.kotlin.stdlib.jdk8)

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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
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
    test {
        useJUnitPlatform()
        jvmArgs("-Dminestom.inside-test=true")
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
publishing {
    publications.create<MavenPublication>("maven") {
        artifact(project.tasks.getByName("shadowJar"))
        // AOT cache shipped alongside the jar for faster startup; see generateAotCache.
        artifact(aotCacheFile) {
            classifier = "aot"
            extension = "aot"
            builtBy(generateAotCache)
        }
        version = rootProject.version as String
        artifactId = "titan-app"
        groupId = rootProject.group as String
        pom {
            name = "Titan App"
            description = "Titan App Server for OneLiteFeather"
            url = "https://github.com/OneLiteFeatherNET/titan"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "themeinerlp"
                    name = "Phillipp Glanz"
                    email = "p.glanz@madfix.me"
                }
            }
            scm {
                connection = "scm:git:git://github.com:OneLiteFeatherNET/Titan.git"
                developerConnection = "scm:git:ssh://git@github.com:OneLiteFeatherNET/Titan.git"
                url = "https://github.com/OneLiteFeatherNET/titan"
            }
        }
    }

    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    // Those credentials need to be set under "Settings -> Secrets -> Actions" in your repository
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            }

            name = "OneLiteFeatherRepository"
            val releasesRepoUrl = uri("https://repo.onelitefeather.dev/onelitefeather-releases")
            val snapshotsRepoUrl = uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
            url = if (version.toString().contains("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}