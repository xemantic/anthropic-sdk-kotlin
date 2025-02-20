@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

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

println(
"""
+--------------------------------------------  
| Project: ${project.name}
| Version: ${project.version}
| Release build: $isReleaseBuild
+--------------------------------------------
"""
)

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
    freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    extraWarnings.set(true)
    progressiveMode = true
  }

  jvm {
    // set up according to https://jakewharton.com/gradle-toolchains-are-rarely-a-good-idea/
    compilerOptions {
      jvmTarget = JvmTarget.fromTarget(javaTarget)
      freeCompilerArgs.add("-Xjdk-release=$javaTarget")
    }
    withJava()
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
  }

  sourceSets {

    commonMain {
      dependencies {
        api(libs.xemantic.ai.tool.schema)
        api(libs.xemantic.ai.money)
        api(libs.xemantic.ai.file.magic)
        api(libs.kotlinx.datetime)
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
    footerMessage.set("(c) 2024 Xemantic")
  }
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
  description = "A Javadoc JAR containing Dokka Javadoc"
  dependsOn(tasks.dokkaGeneratePublicationHtml)
  from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}

// HTML jar commented out, as it might be preventing release from landing in Maven Central
//val dokkaHtmlJar by tasks.registering(Jar::class) {
//  description = "A HTML Documentation JAR containing Dokka HTML"
//  from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
//  archiveClassifier.set("html-doc")
//}

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
      artifact(dokkaJavadocJar)
      //artifact(dokkaHtmlJar)
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
            name = "The Apache Software License, Version 2.0"
            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
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

tasks.withType(JavaCompile::class.java) {
  targetCompatibility = javaTarget
  sourceCompatibility = javaTarget
}
