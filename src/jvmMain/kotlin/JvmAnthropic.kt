package com.xemantic.anthropic

actual val envApiKey: String?
  get() = System.getenv("ANTHROPIC_API_KEY")

actual val missingApiKeyMessage: String
  get() = "apiKey is missing, it has to be provided as a parameter or as an ANTHROPIC_API_KEY environment variable."
