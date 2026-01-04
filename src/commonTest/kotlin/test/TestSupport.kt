/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.ai.anthropic.test

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.Model
import com.xemantic.ai.anthropic.envApiProviderToTest
import com.xemantic.kotlin.test.gradleRootDir
import com.xemantic.kotlin.test.isBrowserPlatform
import kotlinx.io.files.Path

val testDataDir: Path get() = Path(gradleRootDir, "test-data")

/**
 * Creates an Anthropic client for testing.
 *
 * By default, uses Anthropic's Claude models. To test with other providers,
 * set the API_PROVIDER_TO_TEST environment variable:
 *
 * - `API_PROVIDER_TO_TEST=anthropic` or omitted: Uses Claude Sonnet 4.5 (default)
 * - `API_PROVIDER_TO_TEST=moonshot`: Uses Kimi K2 0905 Preview
 *
 * Example:
 * ```bash
 * # Test with Anthropic (default)
 * ./gradlew test
 *
 * # Test with Moonshot
 * API_PROVIDER_TO_TEST=moonshot ./gradlew test
 * ```
 *
 * The provider's API key must be available as an environment variable
 * (ANTHROPIC_API_KEY, MOONSHOT_API_KEY, etc.).
 */
fun testAnthropic(
    block: Anthropic.Config.() -> Unit = {}
): Anthropic = Anthropic {
    val testProvider = envApiProviderToTest?.lowercase()

    if (testProvider != null && testProvider != "anthropic") {
        val model = when (testProvider) {
            "moonshot" -> Model.KIMI_K2_0905_PREVIEW
            else -> error("Unknown API_PROVIDER_TO_TEST: $testProvider. Supported: anthropic, moonshot")
        }
        defaultModel = model
        println("🧪 Testing with provider: $testProvider, model: ${model.id}")
    }

    block()
    directBrowserAccess = isBrowserPlatform
}
