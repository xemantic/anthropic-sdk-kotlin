/*
 * Copyright 2024-2026 Xemantic contributors
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
import com.xemantic.kotlin.test.gradleRootDir
import com.xemantic.kotlin.test.isBrowserPlatform
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.io.files.Path
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val testDataDir: Path get() = Path(gradleRootDir, "test-data")

fun testAnthropic(
    block: Anthropic.Config.() -> Unit = {}
): Anthropic = Anthropic {
    directBrowserAccess = isBrowserPlatform
    // defaulting to HAIKU in tests to reduce costs of integration testing
    defaultModel = Model.CLAUDE_HAIKU_4_5_20251001
    // block invoked at the end, so that we can still alter these defaults
    block()
}

@OptIn(ExperimentalUuidApi::class)
fun uniqueSuffix() = "\n\nSuffix: ${Uuid.random()}"

suspend fun fetchText(
    url: String
): String = HttpClient().use { client ->
    client.get(url).bodyAsText()
}

suspend fun fetchSkillText() = fetchText(
    "https://raw.githubusercontent.com/anthropics/skills/main/skills/skill-creator/SKILL.md"
)
