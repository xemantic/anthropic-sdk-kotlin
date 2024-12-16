/*
 * Copyright 2024 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.ai.anthropic;

import com.xemantic.ai.anthropic.content.Text;
import com.xemantic.ai.anthropic.message.Message;
import com.xemantic.ai.anthropic.message.MessageResponse;
import com.xemantic.ai.anthropic.message.Role;
import org.junit.Test;

import java.util.List;

public class JavaAnthropicTest {

  @Test
  public void foo() {
    JavaAnthropic anthropic = JavaAnthropic.create(config -> {
    });
    MessageResponse response = anthropic.messages.createBlocking(builder -> builder.messages(
        new Message(
            Role.USER,
            List.of(
                new Text(
                    "Hi Claude",
                    null
                )
            )
        )
    ));
    System.out.println(response);
    throw new RuntimeException("test");
  }

}
