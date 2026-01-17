package com.xemantic.ai.anthropic

internal actual fun getEnvApiKey(provider: String): String? {
    val envVarName = "${provider.uppercase().replace("-", "_")}_API_KEY"
    return js("process.env[envVarName]")
}

internal actual val envApiProviderToTest: String?
    get() = js("process.env.API_PROVIDER_TO_TEST")
