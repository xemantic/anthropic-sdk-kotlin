package com.xemantic.anthropic.event

import com.xemantic.anthropic.message.MessageResponse
import com.xemantic.anthropic.message.StopReason
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

// reference https://docs.spring.io/spring-ai/reference/_images/anthropic-claude3-events-model.jpg

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Event {

  @Serializable
  @SerialName("message_start")
  data class MessageStart(
    val message: MessageResponse
  ) : Event()

  @Serializable
  @SerialName("message_delta")
  data class MessageDelta(
    val delta: Delta,
    val usage: Usage
  ) : Event() {

    @Serializable
    data class Delta(
      @SerialName("stop_reason")
      val stopReason: StopReason,
      @SerialName("stop_sequence")
      val stopSequence: String? // TODO is that correct?
    )

    @Serializable
    data class Usage(
      @SerialName("output_tokens")
      val outputTokens: Int
    )

  }

  @Serializable
  @SerialName("message_stop")
  class MessageStop : Event() {
    override fun toString(): String = "MessageStop"
  }

  @Serializable
  @SerialName("content_block_start")
  data class ContentBlockStart(
    val index: Int,
    @SerialName("content_block")
    val contentBlock: ContentBlock
  ) : Event()

  @Serializable
  @SerialName("content_block_stop")
  data class ContentBlockStop(
    val index: Int
  ) : Event()

  @Serializable
  @SerialName("ping")
  class Ping: Event() {
    override fun toString(): String = "Ping"
  }

  @Serializable
  @SerialName("content_block_delta")
  data class ContentBlockDelta(
    val index: Int,
    val delta: Delta
  ) : Event()

}





// TODO error event is missing, should we rename all of these to events?



@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class ContentBlock {

  @Serializable
  @SerialName("text")
  class Text(
    val text: String
  ) : ContentBlock()

  @Serializable
  @SerialName("tool_use")
  class ToolUse(
    val text: String // TODO tool_id
  ) : ContentBlock()

}



@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Delta {

  @Serializable
  @SerialName("text_delta")
  data class TextDelta(
    val text: String
  ) : Delta()

}

