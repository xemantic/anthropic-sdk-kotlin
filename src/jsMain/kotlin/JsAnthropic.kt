package com.xemantic.ai.anthropic

actual val envApiKey: String?
    get() = js("process.env.ANTHROPIC_API_KEY")

actual val missingApiKeyMessage: String
    get() = "apiKey is missing, it has to be provided as a parameter."
