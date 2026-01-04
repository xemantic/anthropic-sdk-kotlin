package com.xemantic.ai.anthropic

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun getEnvApiKey(provider: String): String? {
    val envVarName = "${provider.uppercase().replace("-", "_")}_API_KEY"
    return getenv(envVarName)?.toKString()
}

@OptIn(ExperimentalForeignApi::class)
internal actual val envApiProviderToTest: String?
    get() = getenv("API_PROVIDER_TO_TEST")?.toKString()
