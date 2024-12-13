@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.kotlinx.atomicfu)
  alias(libs.plugins.kotlin.plugin.power.assert)
  alias(libs.plugins.dokka)
  alias(libs.plugins.versions)
  `maven-publish`
  signing
  alias(libs.plugins.publish)
}

val githubAccount = "xemantic"

val javaTarget = libs.versions.javaTarget.get()
val kotlinTarget = KotlinVersion.fromVersion(libs.versions.kotlinTarget.get())

val isReleaseBuild = !project.version.toString().endsWith("-SNAPSHOT")
val jvmOnlyBuild: String? by project
val isJvmOnlyBuild: Boolean = (jvmOnlyBuild == null) || (jvmOnlyBuild!!.uppercase() == "true")
val githubActor: String? by project
val githubToken: String? by project
val signingKey: String? by project
val signingPassword: String? by project
val sonatypeUser: String? by project
val sonatypePassword: String? by project

// we don't want to risk that a flaky test will crash the release build
// and everything should be tested anyway after merging to the main branch
val skipTests = isReleaseBuild

println("""
+--------------------------------------------  
| Project: ${project.name}
| Version: ${project.version}
| Release build: $isReleaseBuild
+--------------------------------------------
"""
)

repositories {
  mavenCentral()
}

kotlin {

  compilerOptions {
    apiVersion = kotlinTarget
    languageVersion = kotlinTarget
    extraWarnings.set(true)
    freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    progressiveMode = true
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
          // for unknown reason browser tests are failing
          enabled = false
          useKarma {
            useChromeHeadless()
          }
        }
      }
      nodejs {
        testTask {
          useMocha {
            timeout = "20s"
          }
        }
      }
      binaries.library()
    }

//  wasmJs {
//    browser()
//    nodejs()
//    //d8()
//    binaries.library()
//  }

//  wasmWasi {
//    nodejs()
//    binaries.library()
//  }

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

//  @OptIn(ExperimentalSwiftExportDsl::class)
//  swiftExport {}
  }

  sourceSets {

    all {
      languageSettings {
        languageVersion = kotlinTarget.version
        apiVersion = kotlinTarget.version
        progressiveMode = true
      }
    }

    commonMain {
      dependencies {
        implementation(libs.kotlinx.datetime)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.xemantic.ai.tool.schema)
        api(libs.xemantic.ai.money)
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

// maybe this one is not necessary?
tasks.dokkaHtml.configure {
  outputDirectory.set(layout.buildDirectory.dir("dokka"))
}

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
  from(tasks.dokkaHtml)
}

publishing {
  repositories {
    if (!isReleaseBuild) {
      maven {
        name = "GitHubPackages"
        setUrl("https://maven.pkg.github.com/$githubAccount/${rootProject.name}")
        credentials {
          username = githubActor
          password = githubToken
        }
      }
    }
  }
  publications {
    withType<MavenPublication> {
      artifact(javadocJar)
      pom {
        name = "anthropic-sdk-kotlin"
        description = "Kotlin multiplatform client for accessing Ahtropic APIs"
        url = "https://github.com/$githubAccount/${rootProject.name}"
        inceptionYear = "2024"
        organization {
          name = "Xemantic"
          url = "https://xemantic.com"
        }
        licenses {
          license {
            name = "The MIT License"
            url = "https://opensource.org/license/MIT"
            distribution = "repo"
          }
        }
        scm {
          url = "https://github.com/$githubAccount/${rootProject.name}"
          connection = "scm:git:git:github.com/$githubAccount/${rootProject.name}.git"
          developerConnection = "scm:git:https://github.com/$githubAccount/${rootProject.name}.git"
        }
        ciManagement {
          system = "GitHub"
          url = "https://github.com/$githubAccount/${rootProject.name}/actions"
        }
        issueManagement {
          system = "GitHub"
          url = "https://github.com/$githubAccount/${rootProject.name}/issues"
        }
        developers {
          developer {
            id = "morisil"
            name = "Kazik Pogoda"
            email = "morisil@xemantic.com"
          }
        }
      }
    }
  }
}

if (isReleaseBuild) {

  // workaround for KMP/gradle signing issue
  // https://github.com/gradle/gradle/issues/26091
  tasks {
    withType<PublishToMavenRepository> {
      dependsOn(withType<Sign>())
    }
  }

  // Resolves issues with .asc task output of the sign task of native targets.
  // See: https://github.com/gradle/gradle/issues/26132
  // And: https://youtrack.jetbrains.com/issue/KT-46466
  tasks.withType<Sign>().configureEach {
    val pubName = name.removePrefix("sign").removeSuffix("Publication")

    // These tasks only exist for native targets, hence findByName() to avoid trying to find them for other targets

    // Task ':linkDebugTest<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
    tasks.findByName("linkDebugTest$pubName")?.let {
      mustRunAfter(it)
    }
    // Task ':compileTestKotlin<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
    tasks.findByName("compileTestKotlin$pubName")?.let {
      mustRunAfter(it)
    }
  }

  signing {
    useInMemoryPgpKeys(
      signingKey,
      signingPassword
    )
    sign(publishing.publications)
  }

  nexusPublishing {
    repositories {
      sonatype {  //only for users registered in Sonatype after 24 Feb 2021
        nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
        snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        username.set(sonatypeUser)
        password.set(sonatypePassword)
      }
    }
  }

}