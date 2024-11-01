package com.xemantic.anthropic.batch

import com.xemantic.anthropic.Response
import com.xemantic.anthropic.message.Message
import kotlinx.datetime.LocalDateTime
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


@Serializable
@SerialName("message_batch")
data class MessageBatchResponse(
  val id: String,
  @SerialName("processing_status")
  val processingStatus: ProcessingStatus,
  @SerialName("request_counts")
  val requestCounts: RequestCounts,
  @SerialName("ended_at")
  val endedAt: LocalDateTime?,
  @SerialName("created_at")
  val createdAt: LocalDateTime,
  @SerialName("expires_at")
  val expiresAt: LocalDateTime,
  @SerialName("cancel_initiated_at")
  val cancelInitiatedAt: LocalDateTime?,
  @SerialName("results_url")
  val resultsUrl: String?
) : Response(type = "message_batch")
