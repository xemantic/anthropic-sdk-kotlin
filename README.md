# anthropic-sdk-kotlin

Unofficial Kotlin multiplatform variant of the
[Antropic SDK](https://docs.anthropic.com/en/api/client-sdks).

[<img alt="Maven Central Version" src="https://img.shields.io/maven-central/v/com.xemantic.ai/anthropic-sdk-kotlin">](https://central.sonatype.com/namespace/com.xemantic.ai/anthropic-sdk-kotlin)
[<img alt="GitHub Release Date" src="https://img.shields.io/github/release-date/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/releases)
[<img alt="license" src="https://img.shields.io/github/license/xemantic/anthropic-sdk-kotlin?color=blue">](https://github.com/xemantic/anthropic-sdk-kotlin/blob/main/LICENSE)

[<img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/xemantic/anthropic-sdk-kotlin/build-main.yml">](https://github.com/xemantic/anthropic-sdk-kotlin/actions/workflows/build-main.yml)
[<img alt="GitHub branch check runs" src="https://img.shields.io/github/check-runs/xemantic/anthropic-sdk-kotlin/main">](https://github.com/xemantic/anthropic-sdk-kotlin/actions/workflows/build-main.yml)
[<img alt="GitHub commits since latest release" src="https://img.shields.io/github/commits-since/xemantic/anthropic-sdk-kotlin/latest">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)
[<img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)

[<img alt="GitHub contributors" src="https://img.shields.io/github/contributors/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/graphs/contributors)
[<img alt="GitHub commit activity" src="https://img.shields.io/github/commit-activity/t/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)
[<img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/xemantic/anthropic-sdk-kotlin">]()
[<img alt="GitHub Created At" src="https://img.shields.io/github/created-at/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/commit/39c1fa4c138d4c671868c973e2ad37b262ae03c2)
[<img alt="kotlin version" src="https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fxemantic%2Fanthropic-sdk-kotlin%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.kotlin&label=kotlin">](https://kotlinlang.org/docs/releases.html)
[<img alt="ktor version" src="https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fxemantic%2Fanthropic-sdk-kotlin%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.ktor&label=ktor">](https://ktor.io/)

[<img alt="discord server" src="https://dcbadge.limes.pink/api/server/https://discord.gg/vQktqqN2Vn?style=flat">](https://discord.gg/vQktqqN2Vn)
[<img alt="discord users online" src="https://img.shields.io/discord/811561179280965673">](https://discord.gg/vQktqqN2Vn)
[<img alt="X (formerly Twitter) Follow" src="https://img.shields.io/twitter/follow/KazikPogoda">](https://x.com/KazikPogoda)

> [!IMPORTANT]
> I am teaching how to use Anthropic API with this SDK and Claudine, in Berlin.
> [Check out the workshop page](https://xemantic.com/workshops/2024/agentic-ai-for-artists-2024-10-26/).

## Why?

Because I believe that coding AI agents should be as easy as possible. I am coming from the [creative coding community](https://creativecode.berlin/), where we are teaching artists, without prior programming experience, how to express their creations through code as a medium.
I want to give creators of all kinds this extremely powerful tool, so that also **you can turn your own machine into an outside window, through which, the AI system can perceive your world, values and needs, and act upon this information.** My first AI agent, which emerged on top of this project, is called [Claudine](https://github.com/xemantic/claudine).

There is no official Anthropic SDK for Kotlin, a de facto standard for Android development. The one for Java is also lacking. Even if they will appear one day, we can expect them to be autogenerated by the
[Stainless API bot](https://www.stainlessapi.com/), which is used by both, Anthropic and OpenAI, to automate
their SDK development based on evolving API. While such an approach seem to work with dynamically typed languages,
it might fail short with statically typed languages like Kotlin, sacrificing typical language idioms in favor
of [over-verbose constructs](https://github.com/anthropics/anthropic-sdk-go/blob/main/examples/tools/main.go).
This library is a [Kotlin multiplatform](https://kotlinlang.org/docs/multiplatform.html)
one, therefore your AI agents developed with it can be seamlessly used in Android, JVM, JavaScript, macOS, iOS, WebAssembly,
and many other environments.

## Usage

> [!CAUTION]
> This SDK is in the early stage of development, so still a subject to API changes,
> however at the same time it is completely functional and passing all the
> [test cases](src/commonTest/kotlin).

The easiest way to use this project is to start with [anthropic-sdk-kotlin-jvm-template](https://github.com/xemantic/anthropic-sdk-kotlin-jvm-template) as a template repository. There are also many ready examples and use cases in the
[anthropic-sdk-kotlin-demo](https://github.com/xemantic/anthropic-sdk-kotlin-demo) repo.

Otherwise, you need to add to your `build.gradle.kts`:

```kotlin
dependencies {
  implementation("com.xemantic.ai:anthropic-sdk-kotlin:0.12")
}
```

, and in case of JVM:

```kotlin
dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
  implementation("io.ktor:ktor-client-java:3.0.1") // or the latest ktor version
  // and if you don't care about configuring logging
  implementation("org.slf4j:slf4j-simple:2.0.16")
}
```

, if you are planning to use tools, you will also need:

```kotlin
plugins {
  // ... other plugins like kotlin jvm or multiplatform
  kotlin("plugin.serialization") version "2.0.21"
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
}
```

The simplest code look like:

```kotlin
fun main() {
  val anthropic = Anthropic()
  val response = runBlocking {
    anthropic.messages.create {
      +Message {
        +"Hello, Claude"
      }
    }
  }
  println(response)
}
```

### Response streaming

Streaming is also possible:

```kotlin
fun main() {
  val client = Anthropic()
  runBlocking {
    client.messages.stream {
      +Message { +"Write me a poem." }
    }
      .filterIsInstance<ContentBlockDeltaEvent>()
      .map { (it.delta as Delta.TextDelta).text }
      .collect { delta -> print(delta) }
  }
}
```

### Using tools

If you want to write AI agents, you need tools, and this is where this library shines:

```kotlin
@AnthropicTool("get_weather")
@Description("Get the weather for a specific location")
data class WeatherTool(val location: String) : ToolInput() {
  init {
    use {
      // in the real world it should use some external service
      "The weather is 73f"
    }
  }
}

fun main() = runBlocking {

  val client = Anthropic {
    tool<WeatherTool>()
  }

  val conversation = mutableListOf<Message>()
  conversation += Message { +"What is the weather in SF?" }

  val initialResponse = client.messages.create {
    messages = conversation
    allTools()
  }
  println("Initial response:")
  println(initialResponse)

  conversation += initialResponse
  conversation += initialResonse.useTools()

  val finalResponse = client.messages.create {
    messages = conversation
    allTools()
  }
  println("Final response:")
  println(finalResponse)
}
```

The advantage comes no only from reduced verbosity, but also the class annotated with
the `@AnthropicTool` will have its JSON schema automatically sent to the Anthropic API when
defining the tool to use. For the reference check equivalent examples in the official
Anthropic SDKs:

* [TypeScript](https://github.com/anthropics/anthropic-sdk-typescript/blob/main/examples/tools.ts)
* [Python](https://github.com/anthropics/anthropic-sdk-python/blob/main/examples/tools.py)
* [Go](https://github.com/anthropics/anthropic-sdk-go/blob/main/examples/tools/main.go)

None of them is taking the advantage of automatic schema generation, which becomes crucial
for maintaining agents expecting more complex and structured input from the LLM.

### Injecting dependencies to tools

Tools can be provided with dependencies, for example singleton
services providing some facilities, like HTTP client to connect to the
internet or DB connection pool to access the database.

```kotlin
@AnthropicTool("query_database")
@Description("Executes SQL on the database")
data class QueryDatabase(val sql: String) : ToolInput() {

  @Transient
  internal lateinit var connection: Connection

  init {
    use {
      connection.prepareStatement(sql).use { statement ->
        statement.executeQuery().use { resultSet ->
          resultSet.toString()
        }
      }
    }
  }

}

fun main() = runBlocking {

  val client = Anthropic {
    tool<QueryDatabase> {
      connection = DriverManager.getConnection("jdbc:...")
    }
  }

  val response = client.messages.create {
    +Message { +"Select all the users who never logged in to the the system" }
    singleTool<QueryDatabase>()
  }

  val tool = response.content.filterIsInstance<ToolUse>().first()
  val toolResult = tool.use()
  println(toolResult)
}
```

After the `DatabaseQueryTool` is decoded from the API response, it can be processed
by the lambda function passed to the tool definition. In case of the example above,
the lambda will inject a JDBC connection to the tool.

More sophisticated code examples targeting various Kotlin platforms can be found in the
[anthropic-sdk-kotlin-demo](https://github.com/xemantic/anthropic-sdk-kotlin-demo)
project.

## Projects using anthropic-sdk-kotlin

* [anthropic-sdk-kotlin-demo](https://github.com/xemantic/anthropic-sdk-kotlin-demo): more complex examples
  and use cases
* [claudine](https://github.com/xemantic/claudine): Claudine, the only AI assistant you will ever need, the actual
  reason why `anthropic-sdk-kotlin` came to being, to allow me building Claudine and other AI agents.

## Building the project

```shell
export ANTHROPIC_API_KEY=your-key-goes-here
./gradlew build
```

Many [unit tests](src/commonTest/kotlin) are actually integration tests calling Anthropic APIs
and asserting against results. This is the reason why they might be flaky from time to time. For
example if the test image is misinterpreted, or Claude is randomly fantasizing too much.

## Project dependencies

API dependencies (will be provided as transitive dependencies of `anthropic-sdk-kotlin`):

* [xemantic-ai-tool-schema](https://github.com/xemantic/xemantic-ai-tool-schema)
* [xemantic-ai-money](https://github.com/xemantic/xemantic-ai-money)
* [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)

Implementation dependencies:

* [ktor](https://ktor.io/)
