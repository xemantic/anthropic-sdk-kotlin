package com.xemantic.anthropic.batch

import com.xemantic.anthropic.message.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageBatchRequest(
  val requests: List<Request>
)

@Serializable
data class Request(
  @SerialName("custom_id")
  val customId: String,
  val params: Params
) {

  @Serializable
  data class Params(
    val model: String,
    val maxTokens: Int,
    val messages: List<Message>
  )

}

@Serializable
data class RequestCounts(
  val processing: Int,
  val succeeded: Int,
  val errored: Int,
  val canceled: Int,
  val expired: Int
)

/**
 * Processing status of the Message Batch.
 */
enum class ProcessingStatus {
  @SerialName("in_progress")
  IN_PROGRESS,
  @SerialName("canceling")
  CANCELING,
  @SerialName("ended")
  ENDED
}
