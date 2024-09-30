@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.kotlin.plugin.power.assert)
  alias(libs.plugins.dokka)
  alias(libs.plugins.versions)
  `maven-publish`
  signing
}

val githubAccount = "xemantic"

val javaTarget = libs.versions.javaTarget.get()
val kotlinTarget = KotlinVersion.fromVersion(libs.versions.kotlinTarget.get())

val isReleaseBuild = !project.version.toString().endsWith("-SNAPSHOT")
val githubActor: String? by project
val githubToken: String? by project
val signingKey: String? by project
val signingPassword: String? by project
val sonatypeUser: String? by project
val sonatypePassword: String? by project

println("""
  Project: ${project.name}
  Version: ${project.version}
  Release: $isReleaseBuild
""".trimIndent()
)

repositories {
    mavenCentral()
}

kotlin {

  jvm {}

  linuxX64()

  sourceSets {

    commonMain {
      dependencies {
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

    nativeTest {
      dependencies {
        // on mac/ios it should be rather Darwin
        runtimeOnly(libs.ktor.client.curl)
      }
    }

  }

}

// set up according to https://jakewharton.com/gradle-toolchains-are-rarely-a-good-idea/
tasks.withType<KotlinJvmCompile> {
  compilerOptions {
    apiVersion = kotlinTarget
    languageVersion = kotlinTarget
    jvmTarget = JvmTarget.fromTarget(javaTarget)
    freeCompilerArgs.add("-Xjdk-release=$javaTarget")
    progressiveMode = true
  }
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

@Suppress("OPT_IN_USAGE")
powerAssert {
  functions = listOf(
    "kotlin.assert",
    "kotlin.test.assertTrue",
    "kotlin.test.assertEquals",
    "kotlin.test.assertNull"
  )
  includedSourceSets = listOf("commonTest", "jvmTest", "nativeTest")
}

val javadocJar by tasks.register<Jar>("dokkaHtmlJar") {
  group = "documentation"
  dependsOn(tasks.dokkaHtml)
  from(tasks.dokkaHtml.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}

val sourcesJar by tasks.named("sourcesJar")

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
    create<MavenPublication>("maven") {
      from(components["kotlin"])
      artifact(javadocJar)
      artifact(sourcesJar)
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
  signing {
    useInMemoryPgpKeys(
      signingKey,
      signingPassword
    )
    sign(publishing.publications["maven"])
  }
}
