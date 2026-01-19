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

package com.xemantic.ai.anthropic.content

import com.xemantic.ai.anthropic.message.Message
import com.xemantic.ai.anthropic.message.StopReason
import com.xemantic.ai.anthropic.test.testAnthropic
import com.xemantic.ai.file.magic.MediaType
import com.xemantic.kotlin.test.assert
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test

class JvmDocumentTest {

    /**
     * Note: This test would work with all the platforms supporting [kotlinx.io.files.SystemFileSystem],
     * however the path of the working directory used by the test container is predictable only
     * for JVM platform, therefore common tests cases are using relative path.
     */
    @Test
    fun `should read test PDF with path specified as String`() = runTest {
        // given
        val anthropic = testAnthropic()

        // when
        val response = anthropic.messages.create {
            +Message {
                +Document("test-data/test.pdf")
                +"What's in the document?"
            }
        }

        // then
        response should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
                assert("FOO" in text.uppercase())
                assert("BAR" in text.uppercase())
            }
        }
    }

    @Test
    fun `should read document file specified as java File`() {
        Document(File("test-data/test.pdf")) should {
            source should {
                be<Source.Base64>()
                have(mediaType == MediaType.PDF.mime)
            }
        }
    }

}
