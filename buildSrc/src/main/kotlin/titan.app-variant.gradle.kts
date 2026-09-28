// Convention for a Titan lobby app variant under apps/*: it bundles runtime with every
// features/* column (D5 in design.md), unless the variant's own build.gradle.kts excludes one via
// titanVariant { exclude(...) }. A new column needs no change to a variant's build file - it is
// picked up by the same features/* scan titan.column and this convention both read from
// settings.gradle.kts.

import java.nio.file.Files
import net.onelitefeather.titan.buildsrc.config.MergeApplicationDefaultsTask
import org.gradle.api.provider.Property

plugins {
    id("titan.java-conventions")
    id("titan.publish-conventions")
    id("com.gradleup.shadow")
    application
}

/**
 * A variant's own knobs: which columns it deliberately leaves out, and whether it ships an AOT
 * cache (only `cloudnet` does).
 */
abstract class TitanVariantExtension {
    abstract val aotCache: Property<Boolean>

    internal val excludedColumns: MutableSet<String> = mutableSetOf()

    fun exclude(vararg columnNames: String) {
        excludedColumns += columnNames
    }
}

val titanVariant = extensions.create<TitanVariantExtension>("titanVariant")
titanVariant.aotCache.convention(false)

application {
    mainClass.set("net.onelitefeather.titan.runtime.TitanApplication")
}

dependencies {
    implementation(project(":runtime"))
}

// Deferred to afterEvaluate: it reads titanVariant.exclude(...)/aotCache, which a variant's own
// build.gradle.kts sets after this plugin's "plugins { id(\"titan.app-variant\") }" block already
// applied it.
afterEvaluate {
    @Suppress("UNCHECKED_CAST")
    val allFeaturePaths = gradle.extensions.extraProperties["titanFeatureProjectPaths"] as List<String>
    val excluded = titanVariant.excludedColumns
    val includedFeaturePaths = allFeaturePaths.filterNot { path -> excluded.contains(path.substringAfterLast(':')) }
    val includedColumnNames = includedFeaturePaths.map { path -> path.substringAfterLast(':') }.sorted()

    includedFeaturePaths.forEach { featurePath ->
        dependencies.add("implementation", dependencies.project(featurePath))
    }

    // This variant's own titan/defaults/runtime.yaml plus every included column's - see D4 in
    // design.md. A column left out via titanVariant.exclude(...) also loses its defaults, so the
    // shipped application.yaml never advertises a setting the variant cannot act on.
    val titanDefaultsFiles = files(
        fileTree(project(":runtime").file("src/main/resources/titan/defaults")) { include("*.yaml") },
        *includedFeaturePaths.map { path -> fileTree(project(path).file("src/main/resources/titan/defaults")) { include("*.yaml") } }.toTypedArray()
    )

    val mergeApplicationDefaults = tasks.register<MergeApplicationDefaultsTask>("mergeApplicationDefaults") {
        group = "build"
        description = "Concatenates runtime's and every included column's titan/defaults/*.yaml into the classpath application.yaml."
        defaultFiles.from(titanDefaultsFiles)
        outputDir.set(layout.buildDirectory.dir("generated/titanDefaults"))
    }

    sourceSets.main {
        resources.srcDir(mergeApplicationDefaults.map { it.outputDir })
    }

    // The classpath application.yaml (see mergeApplicationDefaults above) is the single source of
    // the shipped defaults - it is no longer hand-duplicated as src/dist/application.example.yaml.
    // This copies it, renamed, into the distribution instead, so an operator still finds a
    // commented example next to the jar.
    val applicationExampleYaml = tasks.register<Copy>("applicationExampleYaml") {
        group = "distribution"
        description = "Copies the classpath application.yaml into the distribution as application.example.yaml."
        from(mergeApplicationDefaults.map { it.outputDir.file("application.yaml") })
        into(layout.buildDirectory.dir("generated/applicationExampleYaml"))
        rename { "application.example.yaml" }
    }

    distributions {
        main {
            contents {
                from(applicationExampleYaml)
            }
        }
    }

    // This variant's own META-INF/titan/variant.properties (D5 in design.md): the startup check in
    // net.onelitefeather.titan.runtime.variant reads it to abort when a column the variant expects
    // didn't load.
    val generateVariantProperties = tasks.register<WriteProperties>("generateVariantProperties") {
        group = "build"
        description = "Writes META-INF/titan/variant.properties naming this variant and its columns."
        destinationFile.set(layout.buildDirectory.file("generated/titanVariant/META-INF/titan/variant.properties"))
        property("name", project.name)
        property("columns", includedColumnNames.joinToString(","))
    }

    sourceSets.main {
        resources.srcDir(layout.buildDirectory.dir("generated/titanVariant"))
    }
    tasks.named("processResources") {
        dependsOn(generateVariantProperties)
    }

    // ---- Ahead-of-Time cache (JDK 25 / JEP 514) for faster lobby startup, only for a variant that
    // opts in via titanVariant { aotCache.set(true) } (only cloudnet). ----
    if (titanVariant.aotCache.get()) {
        val variantJarName = "titan-${project.name}.jar"
        val aotCacheFileName = "titan-${project.name}.aot"
        val aotTrainSeconds = providers.gradleProperty("titan.aot.trainSeconds").orElse("20")
        val aotRunDir = layout.buildDirectory.dir("aot")
        val aotCacheFile = layout.buildDirectory.file("aot/$aotCacheFileName")

        val generateAotCache = tasks.register<Exec>("generateAotCache") {
            group = "build"
            description = "Generates a JDK 25 AOT cache ($aotCacheFileName) for faster lobby startup."

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
                // Relative classpath: the cache records the variant jar name, matching the
                // deployment launch command "java -XX:AOTCache=<name>.aot -jar <name>.jar".
                jarProvider.get().asFile.copyTo(runDir.resolve(variantJarName), overwrite = true)
                // The lobby loads worlds/ (+ app.json) relative to the CWD while booting.
                Files.createSymbolicLink(runDir.resolve("worlds").toPath(), rootDir.resolve("worlds").toPath())
                rootDir.resolve("app.json").takeIf { it.exists() }?.copyTo(runDir.resolve("app.json"), overwrite = true)
                executable = launcher.get().executablePath.asFile.absolutePath
                args(
                    "-Dtitan.aot.trainSeconds=${trainSeconds.get()}",
                    "-XX:AOTCacheOutput=$aotCacheFileName",
                    "-jar", variantJarName
                )
            }
        }

        publishing.publications.named<MavenPublication>("maven") {
            artifact(aotCacheFile) {
                classifier = "aot"
                extension = "aot"
                builtBy(generateAotCache)
            }
        }
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
        archiveFileName.set("titan-${project.name}.jar")
        mergeServiceFiles()
        // Shaded deps ship signed and multi-release jars that break a
        // relocation-free application fat jar; drop signatures and module-info.
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("module-info.class", "META-INF/versions/**/module-info.class")
        // Every features/* column ships its own META-INF/services/io.avaje.inject.spi.InjectExtension
        // entry; DuplicatesStrategy.EXCLUDE (this task's own default) drops every duplicate path
        // before mergeServiceFiles()'s transformer ever sees them, so only one column's Avaje
        // module would survive the shade (see docs/lobby-modules.md, "Wie eine Column
        // Plattform-Beans bekommt"). Scoping INCLUDE to service files only lets those duplicates
        // reach the transformer while everything else (LICENSE, NOTICE, ...) keeps the default
        // EXCLUDE, so the jar doesn't end up with duplicate non-service entries.
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
