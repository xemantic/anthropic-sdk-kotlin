package com.xemantic.ai.anthropic

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual val envApiKey: String?
    get() = getenv("ANTHROPIC_API_KEY")?.toKString()

actual val missingApiKeyMessage: String
    get() = "apiKey is missing, it has to be provided as a parameter or as an ANTHROPIC_API_KEY environment variable."
