package com.xemantic.anthropic.content

import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.StopReason
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

const val testPdf = "JVBERi0xLjEKJcKlwrHDqwoKMSAwIG9iagogIDw8IC9UeXBlIC9DYXRhbG9nCiAgICAgL1BhZ2VzIDIgMCBSCiAgPj4KZW5" +
    "kb2JqCgoyIDAgb2JqCiAgPDwgL1R5cGUgL1BhZ2VzCiAgICAgL0tpZHMgWzMgMCBSXQogICAgIC9Db3VudCAxCiAgICAgL01lZGlhQm94IFswID" +
    "AgMzAwIDE0NF0KICA+PgplbmRvYmoKCjMgMCBvYmoKICA8PCAgL1R5cGUgL1BhZ2UKICAgICAgL1BhcmVudCAyIDAgUgogICAgICAvUmVzb3VyY" +
    "2VzCiAgICAgICA8PCAvRm9udAogICAgICAgICAgIDw8IC9GMQogICAgICAgICAgICAgICA8PCAvVHlwZSAvRm9udAogICAgICAgICAgICAgICAg" +
    "ICAvU3VidHlwZSAvVHlwZTEKICAgICAgICAgICAgICAgICAgL0Jhc2VGb250IC9UaW1lcy1Sb21hbgogICAgICAgICAgICAgICA+PgogICAgICA" +
    "gICAgID4+CiAgICAgICA+PgogICAgICAvQ29udGVudHMgNCAwIFIKICA+PgplbmRvYmoKCjQgMCBvYmoKICA8PCAvTGVuZ3RoIDU1ID4+CnN0cm" +
    "VhbQogIEJUCiAgICAvRjEgMTggVGYKICAgIDAgMCBUZAogICAgKEhlbGxvIFdvcmxkKSBUagogIEVUCmVuZHN0cmVhbQplbmRvYmoKCnhyZWYKM" +
    "CA1CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAxOCAwMDAwMCBuIAowMDAwMDAwMDc3IDAwMDAwIG4gCjAwMDAwMDAxNzggMDAwMDAgbiAK" +
    "MDAwMDAwMDQ1NyAwMDAwMCBuIAp0cmFpbGVyCiAgPDwgIC9Sb290IDEgMCBSCiAgICAgIC9TaXplIDUKICA+PgpzdGFydHhyZWYKNTY1CiUlRU9" +
    "GCg=="

class DocumentTest {

  @Test
  fun `Should read text FOO from test PDF`() = runTest {
    // given
    val client = Anthropic {
      anthropicBeta = "pdfs-2024-09-25"
    }

    // when
    val response = client.messages.create {
      +Message {
        +Document(
          source = Document.Source(
            mediaType = Document.MediaType.APPLICATION_PDF,
            data = testPdf
          )
        )
        +"What's in the document?"
      }
    }

    // then
    response should {
      have(stopReason == StopReason.END_TURN)
      have(content.size == 1)
      content[0] should {
        be<Text>()
        assert("HELLO WORLD" in text.uppercase())
      }
    }
  }

}
