/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.xemantic.gradle.conventions.License
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jreleaser.model.Active

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.power.assert)
    alias(libs.plugins.dokka)
    alias(libs.plugins.versions)
    `maven-publish`
    signing
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.xemantic.conventions)
}

group = "com.xemantic.ai"

xemantic {
    description = "Unofficial Kotlin multiplatform variant of the Anthropic SDK"
    inceptionYear = 2024
    license = License.APACHE
    developer(
        id = "morisil",
        name = "Kazik Pogoda",
        email = "morisil@xemantic.com"
    )
}

val releaseAnnouncementSubject = """🚀 ${rootProject.name} $version has been released!"""

val releaseAnnouncement = """
$releaseAnnouncementSubject

${xemantic.description}

${xemantic.releasePageUrl}
"""

val javaTarget = libs.versions.javaTarget.get()
val kotlinTarget = KotlinVersion.fromVersion(libs.versions.kotlinTarget.get())

val isReleaseBuild = !project.version.toString().endsWith("-SNAPSHOT")
val jvmOnlyBuild: String? by project
val isJvmOnlyBuild: Boolean = (jvmOnlyBuild == null) || (jvmOnlyBuild!!.lowercase() == "true")

// we don't want to risk that a flaky test will crash the release build
// and everything should be tested anyway after merging to the main branch
val skipTests = isReleaseBuild

val gradleRootDir: String = rootDir.absolutePath

val anthropicApiKey: String? = System.getenv("ANTHROPIC_API_KEY")

tasks.withType<KotlinJvmTest>().configureEach {
    environment("GRADLE_ROOT_DIR", gradleRootDir)
}

tasks.withType<KotlinJsTest>().configureEach {
    environment("GRADLE_ROOT_DIR", gradleRootDir)
    if (anthropicApiKey != null) {
        environment("ANTHROPIC_API_KEY", anthropicApiKey)
    }
}

tasks.withType<KotlinNativeTest>().configureEach {
    environment("GRADLE_ROOT_DIR", gradleRootDir)
    environment("SIMCTL_CHILD_GRADLE_ROOT_DIR", gradleRootDir)
    if (anthropicApiKey != null) {
        environment("ANTHROPIC_API_KEY", anthropicApiKey)
        environment("SIMCTL_CHILD_ANTHROPIC_API_KEY", anthropicApiKey)
    }
}

repositories {
    mavenCentral()
}

kotlin {

    compilerOptions {
        apiVersion = kotlinTarget
        languageVersion = kotlinTarget
        freeCompilerArgs.add(
            "-Xcontext-sensitive-resolution"
        )
        extraWarnings.set(true)
        progressiveMode = true
        optIn.addAll("kotlin.time.ExperimentalTime")
    }

    jvm {
        // set up according to https://jakewharton.com/gradle-toolchains-are-rarely-a-good-idea/
        compilerOptions {
            apiVersion = kotlinTarget
            languageVersion = kotlinTarget
            jvmTarget = JvmTarget.fromTarget(javaTarget)
            freeCompilerArgs.add("-Xjdk-release=$javaTarget")
            progressiveMode = true
        }
    }

    if (!isJvmOnlyBuild) {
        js {
            // browser tests switched off for a moment
            browser {
                testTask {
                    useKarma {
                        useChromeHeadless()
                    }
                }
            }
            nodejs {
                testTask {
                    useMocha {
                        timeout = "60s"
                    }
                }
            }
            binaries.library()
        }

//    // wasm targets are still buggy
//    wasmJs {
//      browser {
//        testTask {
//          // also these tests are stuck
//          enabled = false
//        }
//      }
//      nodejs()
//      //d8()
//      binaries.library()
//    }

//    wasmWasi {
//      nodejs()
//      binaries.library()
//    }

        // native, see https://kotlinlang.org/docs/native-target-support.html
        // tier 1
        macosX64()
        macosArm64()
        iosSimulatorArm64()
        iosX64()
        iosArm64()

        // tier 2
        linuxX64()
        linuxArm64()
        watchosSimulatorArm64()
        watchosX64()
        watchosArm32()
        watchosArm64()
        tvosSimulatorArm64()
        tvosX64()
        tvosArm64()

//  // tier 3
//  androidNativeArm32()
//  androidNativeArm64()
//  androidNativeX86()
//  androidNativeX64()
        mingwX64()
//  watchosDeviceArm64()

        @OptIn(ExperimentalSwiftExportDsl::class)
        swiftExport {}

//        targets.withType<KotlinNativeTarget> {
//            binaries.all {
//                freeCompilerArgs += "-Xdisable-phases=RemoveRedundantCallsToStaticInitializersPhase"
//            }
//        }

//        // setup tests running in RELEASE mode
//        targets.withType<KotlinNativeTarget>().configureEach {
//            binaries.test(listOf(RELEASE))
//        }
//        targets.withType<KotlinNativeTargetWithTests<*>>().configureEach {
//            testRuns.create("releaseTest") {
//                setExecutionSourceFrom(binaries.getTest(RELEASE))
//            }
//        }
    }

    sourceSets {

        commonMain {
            dependencies {
                api(libs.xemantic.ai.tool.schema)
                api(libs.xemantic.ai.money)
                api(libs.xemantic.ai.file.magic)
                api(libs.xemantic.kotlin.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.xemantic.kotlin.test)
                implementation(libs.kotest.assertions.json)
            }
        }

        jvmTest {
            dependencies {
                runtimeOnly(libs.log4j.slf4j2)
                runtimeOnly(libs.log4j.core)
                runtimeOnly(libs.jackson.databind)
                runtimeOnly(libs.jackson.dataformat.yaml)
                runtimeOnly(libs.ktor.client.java)
            }
        }

        if (!isJvmOnlyBuild) {
            linuxTest {
                dependencies {
                    implementation(libs.ktor.client.curl)
                }
            }

            mingwTest {
                dependencies {
                    implementation(libs.ktor.client.curl)
                }
            }

            macosTest {
                dependencies {
                    implementation(libs.ktor.client.darwin)
                }
            }

        }

    }

}

if (!isJvmOnlyBuild) {
    // linux test native test temporarily disabled as it is causing GitHub action to stall
//    tasks.named("linuxX64Test") { enabled = false }
    // skip tests for which system environment variable retrival is not implemented at the moment
    //tasks.named("wasmWasiNodeTest") { enabled = false }
//// skip test for certain targets which are not fully supported by kotest
////tasks.named("compileTestKotlinWasmWasi") { enabled = false}
    tasks.named("iosSimulatorArm64Test") { enabled = false }
    tasks.named("watchosSimulatorArm64Test") { enabled = false }
    tasks.named("tvosSimulatorArm64Test") { enabled = false }
//tasks.named("androidNativeArm64Test") { enabled = false }
//tasks.named("androidNativeX64Test") { enabled = false }
//tasks.named("androidNativeX86Test") { enabled = false }
//tasks.named("compileTestKotlinAndroidNativeX64") { enabled = false }
//
//// skip tests which require XCode components to be installed
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

tasks.withType<Test> {
    enabled = !skipTests
    testLogging {
        events(
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED
        )
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

powerAssert {
    functions = listOf(
        "com.xemantic.kotlin.test.assert",
        "com.xemantic.kotlin.test.have"
    )
}

// https://kotlinlang.org/docs/dokka-migration.html#adjust-configuration-options
dokka {
    pluginsConfiguration.html {
        footerMessage.set(xemantic.copyright)
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaGeneratePublicationHtml)
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            xemantic.configurePom(this)
        }
    }
}

jreleaser {
    project {
        description = xemantic.description
        copyright = xemantic.copyright
        license = xemantic.license!!.spxdx
        links {
            homepage = xemantic.homepageUrl
            documentation = xemantic.documentationUrl
        }
        authors = xemantic.authorIds
    }
    deploy {
        maven {
            mavenCentral {
                create("maven-central") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    applyMavenCentralRules = false
                    maxRetries = 240
                    stagingRepository(xemantic.stagingDeployDir.path)
                    // workaround: https://github.com/jreleaser/jreleaser/issues/1784
                    kotlin.targets.forEach { target ->
                        if (target !is KotlinJvmTarget) {
                            val nonJarArtifactId = if (target.platformType == KotlinPlatformType.wasm) {
                                "${name}-wasm-${target.name.lowercase().substringAfter("wasm")}"
                            } else {
                                "${name}-${target.name.lowercase()}"
                            }
                            artifactOverride {
                                artifactId = nonJarArtifactId
                                jar = false
                                verifyPom = false
                                sourceJar = false
                                javadocJar = false
                            }
                        }
                    }
                }
            }
        }
    }
    release {
        github {
            skipRelease = true // we are releasing through GitHub UI
            skipTag = true
            token = "empty"
            changelog {
                enabled = false
            }
        }
    }
    checksum {
        individual = false
        artifacts = false
        files = false
    }
    announce {
        webhooks {
            create("discord") {
                active = Active.ALWAYS
                message = releaseAnnouncement
                messageProperty = "content"
                structuredMessage = true
            }
        }
        linkedin {
            active = Active.ALWAYS
            subject = releaseAnnouncementSubject
            message = releaseAnnouncement
        }
        bluesky {
            active = Active.ALWAYS
            status = releaseAnnouncement
        }
    }
}

tasks.withType(JavaCompile::class.java) {
    targetCompatibility = javaTarget
    sourceCompatibility = javaTarget
}
