package com.xemantic.anthropic.message

import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlin.test.Test

class MessageTest {

  @Test
  fun `Default Message should have Role USER`() {
    Message {} should {
      have(role == Role.USER)
    }
  }

}
