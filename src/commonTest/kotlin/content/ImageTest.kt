package com.xemantic.anthropic.content

import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.StopReason
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

const val testImage = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAACXBIWXMAABOvAAATrwFj5o7DAAAAGXRFWHRTb2Z0d2FyZ" +
    "QB3d3cuaW5rc2NhcGUub3Jnm+48GgAAA61JREFUeJztmNtrFVcUxn/b+lYwxEuN8RKR1giiQmqxEomCghSU0krBUpqiTyraUn3oU+m/UPsoiD6J" +
    "iiBeW4T64P1FKBSLGlBDjE21NV7r/SwfZp1zhmHPmRnjnPWQ/cFmcfZe69tfvuyZWTNORBjNGGMtwBrBAGsB1ggGWAuwRjDAWoA1ggHWAqwRDLA" +
    "WYI1ggLUAawQDrAVYIxhgLcAawQBrAdYIBlgLsMbYZm/onGsBJhOZPyQi90bINw5oA94B/hGRu4Xq0z6LO+d2AT0FuM6IyDcpXJOArcBnQGdi+T" +
    "KwD/glr3jnXCvwPfA5MDex3AfsV77bmWQi4h3Ar4AUGCdSeNYC9xO5j4Gnibn/gDVpemJ8nwJ3E7X/A08Sc/eBrzL5chjwB/BFjtHj4dgAVJTnJ" +
    "rAZmKJrY4CZwI/AHc15BaxvoOlr4KXmDhGdqum65oDpwA+6Jrr3lpEa8FuWiyn1i4DnyvE70NogtwP4U3OfAQs8OfP1Py3AWWBSA7524KLmvgA+" +
    "tjDgtNb3AS058juAR2mXE3Bc1waAiTn42oBhrTnXVAOArti1mHqkPXU/xY5uZ2y+M3YpfVeAb1tMR5cvp6w+YIXGCnCgQN1ejQ5YFZtfrnMQPTG" +
    "K8FUfc6t9CWUZsEjjZRF5kLdIRK4At/TnHA/fgIgMFeAbBK56+GrIY8BK55xkjPWJmvc0/p1XbAxVA6aWyFdDWSdgnMZHb1Bb7QzHv2W+Cb7FPK" +
    "3weWBLRk5/4ve/Gt/NwZ9Ei8b4H/u2+WrIY8ADEblYcNObGjuKFDnnHDBLf14bKZ/ifQ9fDWVdAqc1fuCcm1agbh71o3ohNn9KY5tzznsz88E5N" +
    "wuY4eGroSwDDhB1dAAbC9Rt0vgMOBqbPwI8TOQU4XsFHPRmlNgJ7tT6J6Q0IYn8Hup9/g7P+s/UW9vuHHwL1UgB9jS1E9T6yUSPICF62VnaIPcT" +
    "oru1AIN4+nygFbiuOcPAygZ8y4hunNW9pzbdAOXoov6mVwEOAb1AN7AEWEe9x6+K/bAB39yYqQIcU45uHb26R7VtHs46LaUaoDwdwImYaN+o6H4" +
    "zc/C1A4cz+AQ4CczO4mv0RehbdfwvEdnuTSoA59xioo8jHwFTiO4N/UTfG3ZL1AYX4VsIfEnUJrcTvXrfIHqt3i0il3LxpBkwWjDqvwoHA6wFWC" +
    "MYYC3AGsEAawHWCAZYC7BGMMBagDWCAdYCrBEMsBZgjWCAtQBrBAOsBVgjGGAtwBqvASNwHwnSLggJAAAAAElFTkSuQmCC"

class ImageTest {

  @Test
  fun `Should read text FOO from test image`() = runTest {
    // given
    val client = Anthropic()

    // when
    val response = client.messages.create {
      +Message {
        +Image(
          source = Image.Source(
            data = testImage,
            mediaType = Image.MediaType.IMAGE_PNG
          )
        )
        +"What's on this picture?"
      }
    }

    // then
    response should {
      have(stopReason == StopReason.END_TURN)
      have(content.size == 1)
      content[0] should {
        be<Text>()
        have("FOO" in text.uppercase())
      }
    }
  }

}
