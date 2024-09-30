# anthropic-sdk-kotlin

Unofficial Kotlin multiplatform variant of the
[Antropic SDK](https://docs.anthropic.com/en/api/client-sdks).

## Why?

I like Kotlin. I like even more the multiplatform aspect of pure Kotlin - that a library code written once
can be utilized as a:

* regular Java library to be used on backend, desktop, Android, etc.
* Kotlin library to be used on backend, desktop, Android.
* Kotlin app transpiled to JavaScript
  * JavaScript library
  * TypeScript library
* A native library
* An iOS library
* Kotlin app transpiled to WebAssembly

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
