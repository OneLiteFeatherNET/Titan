import com.diffplug.gradle.spotless.SpotlessExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("info.solidsoft.pitest") version "1.15.0" apply false
    id("com.diffplug.spotless") version "7.2.1" apply false
    id("jacoco")
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "info.solidsoft.pitest")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "jacoco")

    configure<PitestPluginExtension> {
        pitestVersion.set("1.16.3")
        junit5PluginVersion.set("1.2.1")
        threads.set(8)
        enableDefaultIncrementalAnalysis.set(true)

        outputFormats.addAll("XML", "HTML")
        timestampedReports.set(false)

        if (project.name in listOf("common", "api", "setup")) {
            failWhenNoMutations.set(false)
        }
        targetClasses.add("net.onelitefeather.titan.*")
    }
    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(24))
    }
    configure<SpotlessExtension> {
        java {
            licenseHeaderFile("${rootDir}/header.java")
        }
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    tasks.withType<Test> {
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = "0.0".toBigDecimal()
                }
            }
        }
    }
}

// Task to create an aggregated JaCoCo report for the entire project
tasks.register<JacocoReport>("jacocoRootReport") {
    description = "Generates an aggregate report from all subprojects"
    group = "Verification"

    // Don't depend on tests to allow report generation even if tests fail
    // Instead, just collect execution data from existing test executions

    additionalSourceDirs.setFrom(files(subprojects.map { it.the<SourceSetContainer>()["main"].allSource.srcDirs }))
    sourceDirectories.setFrom(files(subprojects.map { it.the<SourceSetContainer>()["main"].allSource.srcDirs }))
    classDirectories.setFrom(files(subprojects.map { it.the<SourceSetContainer>()["main"].output }))
    executionData.setFrom(files(subprojects.map { it.tasks.withType<JacocoReport>().map { report -> report.executionData } }))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}
