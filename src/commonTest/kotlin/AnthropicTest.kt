package com.xemantic.anthropic

import com.xemantic.anthropic.event.ContentBlockDelta
import com.xemantic.anthropic.event.Delta.TextDelta
import com.xemantic.anthropic.message.Image
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.MessageResponse
import com.xemantic.anthropic.message.Role
import com.xemantic.anthropic.message.StopReason
import com.xemantic.anthropic.message.Text
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnthropicTest {

  @Test
  fun shouldReceiveAnIntroductionFromClaude() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +"Hello World! What's your name?"
      }
      model = "claude-3-opus-20240229"
      maxTokens = 1024
    }

    // then
    response.apply {
      assertTrue(type == MessageResponse.Type.MESSAGE)
      assertTrue(role == Role.ASSISTANT)
      assertTrue(model == "claude-3-opus-20240229")
      assertTrue(content.size == 1)
      assertTrue(content[0] is Text)
      val text = content[0] as Text
      assertTrue(text.text.contains("Claude"))
      assertTrue(stopReason == StopReason.END_TURN)
      assertNull(stopSequence)
      assertEquals(usage.inputTokens, 15)
      assertTrue(usage.outputTokens > 0)
    }
  }

  @Test
  fun shouldReadTextFooFromTestImage() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +Image(
          source = Image.Source(
            data = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAAYUlEQVR4nO2VwQrAMAhDk7H//+XsIBQ3T4XmMEhOYjGvIraUBKcuq3sAAQQAALhnimQFR5b8Cyj3g+8Hu9e6e6mOZkNbLb5msAokdfcez8wGwCE7wD4D5sMJIIAAAvgD4AGFvjMy4P7S/QAAAABJRU5ErkJggg==",
            mediaType = Image.MediaType.IMAGE_PNG
          )
        )
        +"What's on this picture?"
      }
    }

    // then
    response.apply {
      assertTrue(1 == content.size)
      assertTrue(content[0] is Text)
      val text = content[0] as Text
      assertTrue(text.text.lowercase().contains("foo"))
    }
  }

  @Test
  fun shouldStreamTheResponse() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.stream {
        +Message {
          role = Role.USER
          +"Say: 'The quick brown fox jumps over the lazy dog'"
        }
      }
        .filterIsInstance<ContentBlockDelta>()
        .map { (it.delta as TextDelta).text }
        .toList()
        .joinToString(separator = "")

    // then
    assertTrue(response == "The quick brown fox jumps over the lazy dog.")
  }

}
