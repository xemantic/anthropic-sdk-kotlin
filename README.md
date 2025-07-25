# anthropic-sdk-kotlin

Unofficial Kotlin multiplatform variant of the
[Anthropic SDK](https://docs.anthropic.com/en/api/client-sdks).

[<img alt="Maven Central Version" src="https://img.shields.io/maven-central/v/com.xemantic.ai/anthropic-sdk-kotlin">](https://central.sonatype.com/artifact/com.xemantic.ai/anthropic-sdk-kotlin)
[<img alt="GitHub Release Date" src="https://img.shields.io/github/release-date/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/releases)
[<img alt="license" src="https://img.shields.io/github/license/xemantic/anthropic-sdk-kotlin?color=blue">](https://github.com/xemantic/anthropic-sdk-kotlin/blob/main/LICENSE)

[<img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/xemantic/anthropic-sdk-kotlin/build-main.yml">](https://github.com/xemantic/anthropic-sdk-kotlin/actions/workflows/build-main.yml)
[<img alt="GitHub branch check runs" src="https://img.shields.io/github/check-runs/xemantic/anthropic-sdk-kotlin/main">](https://github.com/xemantic/anthropic-sdk-kotlin/actions/workflows/build-main.yml)
[<img alt="GitHub commits since latest release" src="https://img.shields.io/github/commits-since/xemantic/anthropic-sdk-kotlin/latest">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)
[<img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)

[<img alt="GitHub contributors" src="https://img.shields.io/github/contributors/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/graphs/contributors)
[<img alt="GitHub commit activity" src="https://img.shields.io/github/commit-activity/t/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)
[<img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/xemantic/anthropic-sdk-kotlin">]()
[<img alt="GitHub Created At" src="https://img.shields.io/github/created-at/xemantic/anthropic-sdk-kotlin">](https://github.com/xemantic/anthropic-sdk-kotlin/commits)
[<img alt="kotlin version" src="https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fxemantic%2Fanthropic-sdk-kotlin%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.kotlin&label=kotlin">](https://kotlinlang.org/docs/releases.html)
[<img alt="ktor version" src="https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fxemantic%2Fanthropic-sdk-kotlin%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.ktor&label=ktor">](https://ktor.io/)
[<img alt="discord users online" src="https://img.shields.io/discord/811561179280965673">](https://discord.gg/vQktqqN2Vn)
[![Bluesky](https://img.shields.io/badge/Bluesky-0285FF?logo=bluesky&logoColor=fff)](https://bsky.app/profile/xemantic.com)

> [!IMPORTANT]
> 🤖 **Build Your Own AI Agents** - Join our one-day Agentic AI & Creative Coding Workshop in Berlin (Spring 2025), led by AI hack Berlin hackathon winner Kazik Pogoda. Learn to create autonomous AI agents using Anthropic API, engineer advanced prompts, and give your agents tools to control machines. Workshops run Tuesdays (Feb 25 - Mar 25) at Prachtsaal Berlin, limited to 15 participants. 150 EUR contribution supports open source development (solidarity access available, no questions asked). All examples use Kotlin (crash course included) but focus on meta-principles of AI agent development. Details: <https://xemantic.com/ai/workshops>

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
    implementation("com.xemantic.ai:anthropic-sdk-kotlin:0.22.1")
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
    kotlin("plugin.serialization") version "2.1.10"
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
            +"Hello, Claude"
        }
    }
    println(response)
}
```

### Response streaming

Streaming is also possible:

```kotlin
fun main() = runBlocking {
    val client = Anthropic()
    client.messages.stream {
        +"Write me a poem."
    }
        .filter { it.delta is Event.ContentBlockDelta.Delta.TextDelta }
        .map { (it.delta as Event.ContentBlockDelta.Delta.TextDelta).text }
        .collect { delta -> print(delta) }
}
```

The `toMessageResponse` function will return the complete `MessageResponse` from the stream:

```kotlin
fun main() = runBlocking {
    val client = Anthropic()
    val response = client.messages.stream {
        +"Write me a poem."
    }
        .onEach { println("Event: $it") }
        .toMessageResponse()
    println(response)
}
```

### Using tools

> [!NOTE]
> Check [Tool Use conventions](docs/tool_use.md) document for full documentation.

If you want to write AI agents, you need tools, and this is where this library shines, while removing any boilerplate code:

```kotlin
@SerialName("get_weather")
@Description("Get the weather for a specific location")
data class GetWeather(val location: String)

fun main() = runBlocking {
    val tool = Tool<GetWeather> { "The weather is 73f" }
    val myTools = listOf(tool)
    val anthropic = Anthropic()

    val conversation = mutableListOf<Message>()
    conversation += "What is the weather in SF?"

    val initialResponse = client.messages.create {
        messages = conversation
        tools = myTools
    }
    println("Initial response: ${initialResponse.text}")

    conversation += initialResponse
    conversation += initialResponse.useTools()

    val finalResponse = client.messages.create {
        messages = conversation
        tools = myTools
    }
    println("Final response: ${finalResponse.text}")
}
```

The JSON schema of `get_weather` tool is automatically extracted from the class definition and sent to the Anthropic API when creating the message containing `tools`.

For the reference check equivalent examples in the official Anthropic SDKs:

* [TypeScript](https://github.com/anthropics/anthropic-sdk-typescript/blob/main/examples/tools.ts)
* [Python](https://github.com/anthropics/anthropic-sdk-python/blob/main/examples/tools.py)
* [Go](https://github.com/anthropics/anthropic-sdk-go/blob/main/examples/tools/main.go)

### Calculating usage costs

An instance of `Anthropic` client has a property `costWithUsage` of the [CostWithUsage](src/commonMain/kotlin/cost/CostCollection.kt) class which holds the cumulative usage statistics together with the overall cost calculation.

Each returned `MessageResponse` also provides the `costWithUsage`. The [CostCollector](src/commonMain/kotlin/cost/CostCollection.kt) class can be used to cumulate this data by just adding `CostWithUsage` instance to the `CostCollector` instance:

```kotlin
val anthropic = Anthropic()
val costCollector = CostCollector()
// ...
val response = anthropic.messages.create {
    +"Hi Claude"
}
costCollector += response.costWithUsage
```

> [!NOTE]
> The `CostCollector` is using atomic operations to ensure thread-safety in concurrent environment.

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
example, if the test image is misinterpreted, or Claude is randomly fantasizing too much.

## Project dependencies

API dependencies (will be provided as transitive dependencies of `anthropic-sdk-kotlin`):

* [xemantic-ai-tool-schema](https://github.com/xemantic/xemantic-ai-tool-schema)
* [xemantic-ai-money](https://github.com/xemantic/xemantic-ai-money)
* [xemantic-kotlin-core](https://github.com/xemantic/xemantic-kotlin-core)

Implementation dependencies:

* [ktor](https://ktor.io/)
