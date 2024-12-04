package com.xemantic.anthropic

actual val envApiKey: String?
  get() = null

actual val missingApiKeyMessage: String
  get() = "apiKey is missing, it has to be provided as a parameter."
