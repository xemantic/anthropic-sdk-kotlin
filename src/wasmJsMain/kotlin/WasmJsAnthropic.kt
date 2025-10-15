package com.xemantic.ai.anthropic

actual val envApiKey: String?
    get() = getenv("ANTHROPIC_API_KEY")

actual val missingApiKeyMessage: String
    get() = "apiKey is missing, it has to be provided as a parameter."

private fun getenv(name: String): String? = js("process.env[name]")
