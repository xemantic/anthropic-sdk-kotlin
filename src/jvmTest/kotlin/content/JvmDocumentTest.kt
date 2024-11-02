package com.xemantic.anthropic.content

import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.MessageRequest
import com.xemantic.anthropic.test.testJson
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test

class JvmDocumentTest {

  @Test
  fun shouldSerializePdfToBase64InMessageRequest() = runTest {
    val messageRequest = MessageRequest {
      +Message {
        +Document("test-data/minimal.pdf")
        +"What's in the document?"
      }
    }

    // when
    val request = testJson.encodeToString(messageRequest)

    // then
    request shouldEqualJson  """
      {
        "model": "claude-3-5-sonnet-latest",
        "messages": [
          {
            "role": "user",
            "content": [
              {
                "type": "document",
                "source": {
                  "type": "base64",
                  "media_type": "application/pdf",
                  "data": "JVBERi0xLjEKJcKlwrHDqwoKMSAwIG9iagogIDw8IC9UeXBlIC9DYXRhbG9nCiAgICAgL1BhZ2VzIDIgMCBSCiAgPj4KZW5kb2JqCgoyIDAgb2JqCiAgPDwgL1R5cGUgL1BhZ2VzCiAgICAgL0tpZHMgWzMgMCBSXQogICAgIC9Db3VudCAxCiAgICAgL01lZGlhQm94IFswIDAgMzAwIDE0NF0KICA+PgplbmRvYmoKCjMgMCBvYmoKICA8PCAgL1R5cGUgL1BhZ2UKICAgICAgL1BhcmVudCAyIDAgUgogICAgICAvUmVzb3VyY2VzCiAgICAgICA8PCAvRm9udAogICAgICAgICAgIDw8IC9GMQogICAgICAgICAgICAgICA8PCAvVHlwZSAvRm9udAogICAgICAgICAgICAgICAgICAvU3VidHlwZSAvVHlwZTEKICAgICAgICAgICAgICAgICAgL0Jhc2VGb250IC9UaW1lcy1Sb21hbgogICAgICAgICAgICAgICA+PgogICAgICAgICAgID4+CiAgICAgICA+PgogICAgICAvQ29udGVudHMgNCAwIFIKICA+PgplbmRvYmoKCjQgMCBvYmoKICA8PCAvTGVuZ3RoIDU1ID4+CnN0cmVhbQogIEJUCiAgICAvRjEgMTggVGYKICAgIDAgMCBUZAogICAgKE1lbmUgTWVuZSBUZWtlbCBVcGhhcnNpbikgVGoKICBFVAplbmRzdHJlYW0KZW5kb2JqCgp4cmVmCjAgNQowMDAwMDAwMDAwIDY1NTM1IGYKMDAwMDAwMDAxOCAwMDAwMCBuCjAwMDAwMDAwNzcgMDAwMDAgbgowMDAwMDAwMTc4IDAwMDAwIG4KMDAwMDAwMDQ1NyAwMDAwMCBuCnRyYWlsZXIKICA8PCAgL1Jvb3QgMSAwIFIKICAgICAgL1NpemUgNQogID4+CnN0YXJ0eHJlZgo1NjUKJSVFT0YK"
                }
              },
              {
                "type": "text",
                "text": "What's in the document?"
              }
            ]
          }
        ],
        "max_tokens": 8182
      }
    """.trimIndent()

  }

}