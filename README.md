# anthropic-sdk-kotlin

Unofficial Kotlin multiplatform variant of the
[Antropic SDK](https://docs.anthropic.com/en/api/client-sdks).

[<img alt="Maven Central Version" src="https://img.shields.io/maven-central/v/com.xemantic.anthropic/anthropic-sdk-kotlin?style=for-the-badge">](https://central.sonatype.com/namespace/com.xemantic.anthropic)
[<img alt="GitHub Release Date" src="https://img.shields.io/github/release-date/xemantic/anthropic-sdk-kotlin?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/releases)
[<img alt="license" src="https://img.shields.io/github/license/xemantic/anthropic-sdk-kotlin?color=blue&style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/blob/main/LICENSE)

[<img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/xemantic/anthropic-sdk-kotlin/build-main.yml?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/actions/workflows/build-main.yml)
[<img alt="GitHub branch check runs" src="https://img.shields.io/github/check-runs/xemantic/anthropic-sdk-kotlin/main?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/actions/workflows/build-main.yml)
[<img alt="GitHub commits since latest release" src="https://img.shields.io/github/commits-since/xemantic/anthropic-sdk-kotlin/latest?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)
[<img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/xemantic/anthropic-sdk-kotlin?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)

[<img alt="GitHub contributors" src="https://img.shields.io/github/contributors/xemantic/anthropic-sdk-kotlin?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/graphs/contributors)
[<img alt="GitHub commit activity" src="https://img.shields.io/github/commit-activity/t/xemantic/anthropic-sdk-kotlin?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/commits/main/)
[<img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/xemantic/anthropic-sdk-kotlin?style=for-the-badge">]()
[<img alt="GitHub Created At" src="https://img.shields.io/github/created-at/xemantic/anthropic-sdk-kotlin?style=for-the-badge">](https://github.com/xemantic/anthropic-sdk-kotlin/commit/39c1fa4c138d4c671868c973e2ad37b262ae03c2)
[<img alt="kotlin version" src="https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fxemantic%2Fanthropic-sdk-kotlin%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.kotlin&style=for-the-badge&label=kotlin">](https://kotlinlang.org/docs/releases.html)
[<img alt="ktor version" src="https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fxemantic%2Fanthropic-sdk-kotlin%2Fmain%2Fgradle%2Flibs.versions.toml&query=versions.ktor&style=for-the-badge&label=ktor">](https://ktor.io/)

[<img alt="discord server" src="https://dcbadge.limes.pink/api/server/https://discord.gg/vQktqqN2Vn">](https://discord.gg/vQktqqN2Vn)
[<img alt="discord users online" src="https://img.shields.io/discord/811561179280965673?style=for-the-badge">](https://discord.gg/vQktqqN2Vn)
[<img alt="X (formerly Twitter) Follow" src="https://img.shields.io/twitter/follow/KazikPogoda?style=for-the-badge">](https://x.com/KazikPogoda)

## Why?

I like Kotlin. I like even more the multiplatform aspect of pure Kotlin - that a library code written once
can be utilized as a:

* regular Java library to be used on backend, desktop, Android, etc.
* Kotlin library to be used on backend, desktop, Android.
* executable native binary (e.g. a command line tool)
* Kotlin app transpiled to JavaScript
* Kotlin app compiled to WebAssembly
* JavaScript library
* TypeScript library
* native library, working also with Swift/iOS

Having Kotlin multiplatform library for the Anthropic APIs allows
me to write AI code once, and target all the platforms automatically.

## Usage

The simplest code would look like:

```kotlin
fun main() {
  val client = Anthropic()
  val response = runBlocking {
    client.messages.create {
      +Message {
        +"Hello World!"
      }
    }
  }
  println(response)
}
```

This SDK is in an early stage o development, but at the same time it is completely functional and passing
live test cases.

More examples coming soon.