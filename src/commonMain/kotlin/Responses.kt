package com.xemantic.anthropic

import com.xemantic.anthropic.batch.ProcessingStatus
import com.xemantic.anthropic.batch.RequestCounts
import com.xemantic.anthropic.message.Content
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.Role
import com.xemantic.anthropic.message.StopReason
import com.xemantic.anthropic.message.Usage
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Response(
  val type: String
)

@Serializable
@SerialName("error")
data class ErrorResponse(
  val error: Error
) : Response(type = "error")

@Serializable
@SerialName("message")
data class MessageResponse(
  val id: String,
  val role: Role,
  val content: List<Content>, // limited to Text and ToolUse
  val model: String,
  @SerialName("stop_reason")
  val stopReason: StopReason?,
  @SerialName("stop_sequence")
  val stopSequence: String?,
  val usage: Usage
) : Response(type = "message") {

  fun asMessage(): Message = Message {
    role = Role.ASSISTANT
    content += this@MessageResponse.content
  }

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
) : Response(type = "message_batch") {}

@Serializable
data class Error(
  val type: String, val message: String
)
