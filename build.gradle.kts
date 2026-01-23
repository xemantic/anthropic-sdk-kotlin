/*
 * Copyright 2024-2026 Xemantic contributors
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

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jreleaser.model.Active

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.power.assert)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.versions)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.xemantic.conventions)
}

group = "com.xemantic.ai"

xemantic {
    description = "Unofficial Kotlin multiplatform variant of the Anthropic SDK"
    inceptionYear = "2024"
    applyAllConventions()
}

fun MavenPomDeveloperSpec.projectDevs() {
    developer {
        id = "morisil"
        name = "Kazik Pogoda"
        url = "https://github.com/morisil"
    }
    developer {
        id = "anakori"
        name = "Anastazja Borówka"
        url = "https://github.com/anakori"
    }
}

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

kotlin {

    compilerOptions {
        apiVersion = kotlinTarget
        languageVersion = kotlinTarget
        freeCompilerArgs.addAll(
            "-Xcontext-sensitive-resolution"
        )
        extraWarnings = true
        progressiveMode = true
        optIn.addAll("kotlin.time.ExperimentalTime")
        coreLibrariesVersion = libs.versions.kotlin.get()
    }

    jvm {
        // set up according to https://jakewharton.com/gradle-toolchains-are-rarely-a-good-idea/
        compilerOptions {
            apiVersion = kotlinTarget
            languageVersion = kotlinTarget
            jvmTarget = JvmTarget.fromTarget(javaTarget)
            freeCompilerArgs.addAll(
                "-Xjdk-release=$javaTarget",
                "-jvm-default=enable" // needed for forward compatibility, e.g. Kotlin Notebook
            )
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

        wasmJs {
            browser {
                testTask {
                    // these tests are stuck for some reason
                    enabled = false
                }
            }
            nodejs()
            //d8()
            binaries.library()
        }

//        wasmWasi {
//            nodejs()
//            binaries.library()
//        }

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
        androidNativeArm32()
        androidNativeArm64()
        androidNativeX86()
        androidNativeX64()
        mingwX64()
        //watchosDeviceArm64()

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
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
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
            }
        }

        jvmTest {
            dependencies {
                runtimeOnly(libs.ktor.client.java)
                runtimeOnly(libs.logback.classic)
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

repositories {
    mavenCentral()
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

    // linux tests hang for unknown reason
    tasks.named("linuxX64Test") { enabled = false }
}

powerAssert {
    functions = listOf(
        "com.xemantic.kotlin.test.assert",
        "com.xemantic.kotlin.test.have"
    )
}

dokka {
    pluginsConfiguration.html {
        footerMessage = xemantic.copyright
    }
}

mavenPublishing {

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {

        name = rootProject.name
        description = xemantic.description
        inceptionYear = xemantic.inceptionYear
        url = "https://github.com/${xemantic.gitHubAccount}/${rootProject.name}"

        organization {
            name = xemantic.organization
            url = xemantic.organizationUrl
        }

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        scm {
            url = "https://github.com/${xemantic.gitHubAccount}/${rootProject.name}"
            connection = "scm:git:git://github.com/${xemantic.gitHubAccount}/${rootProject.name}.git"
            developerConnection = "scm:git:ssh://git@github.com/${xemantic.gitHubAccount}/${rootProject.name}.git"
        }

        ciManagement {
            system = "GitHub"
            url = "https://github.com/${xemantic.gitHubAccount}/${rootProject.name}/actions"
        }

        issueManagement {
            system = "GitHub"
            url = "https://github.com/${xemantic.gitHubAccount}/${rootProject.name}/issues"
        }

        developers {
            projectDevs()
        }

    }

}

val releaseAnnouncementSubject = """🚀 ${rootProject.name} $version has been released!"""
val releaseAnnouncement = """
$releaseAnnouncementSubject

${xemantic.description}

${xemantic.releasePageUrl}
""".trim()

jreleaser {

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

// for JVM tests
tasks.withType<JavaCompile> {
    targetCompatibility = javaTarget
    sourceCompatibility = javaTarget
}
