package com.xemantic.anthropic

import com.xemantic.anthropic.message.*
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.UsableTool
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

/**
 * This test tool is based on the
 * [article by Dan Nguyen](https://gist.github.com/dannguyen/faaa56cebf30ad51108a9fe4f8db36d8),
 * who showed how to extract financial disclosure reports as structured data by using OpenAI API.
 * I wanted to try out the same approach with Anthropic API, and it seems like a great test case of this library.
 */
@AnthropicTool(
  name = "DisclosureReport",
  description = "Extract the text from this image"
)
class DisclosureReport(
  val assets: List<Asset>
) : UsableTool {
  override suspend fun use(toolUseId: String) = ToolResult(
    toolUseId, "Data provided to client"
  )
}

@Serializable
data class Asset(
  val assetName: String,
  val owner: String,
  val location: String?,
  val assetValueLow: Int?,
  val assetValueHigh: Int?,
  val incomeType: String,
  val incomeLow: Int?,
  val incomeHigh: Int?,
  val txGt1000: Boolean
)

/**
 * This test is located in the jvmTest folder, so it can use File API to read image files.
 * In the future it can probably be moved to jvmAndPosix to support all the Kotlin platforms
 * having access to the filesystem.
 */
class StructuredOutputTest {

  @Test
  fun shouldDecodeStructuredOutputFromReportImage() = runTest {
    val client = Anthropic {
      tool<DisclosureReport>()
    }

    val response = client.messages.create {
      +Message {
        +"Decode structured output from supplied image"
        +Image(
          path = "test-data/financial-disclosure-report.png",
          mediaType = Image.MediaType.IMAGE_PNG
        )
      }
      useTool<DisclosureReport>()
    }

    val tool = response.content.filterIsInstance<ToolUse>().first()
    val report = tool.input<DisclosureReport>()

    report.assets shouldHaveSize 6
    assertSoftly(report.assets[0]) {
      assetName shouldBe "11 Zinfandel Lane - Home & Vineyard [RP]"
      owner shouldBe "JT"
      location shouldBe "St. Helena/Napa, CA, US"
      assetValueLow shouldBe 5000001
      assetValueHigh shouldBe 25000000
      incomeType shouldBe "Grape Sales"
      incomeLow shouldBe 100001
      incomeHigh shouldBe 1000000
      txGt1000 shouldBe false
    }
    assertSoftly(report.assets[1]) {
      assetName shouldBe "25 Point Lobos - Commercial Property [RP]"
      owner shouldBe "SP"
      location shouldBe "San Francisco/San Francisco, CA, US"
      assetValueLow shouldBe 5000001
      assetValueHigh shouldBe 25000000
      incomeType shouldBe "Rent"
      incomeLow shouldBe 100001
      incomeHigh shouldBe 1000000
      txGt1000 shouldBe false
    }
  }

}
