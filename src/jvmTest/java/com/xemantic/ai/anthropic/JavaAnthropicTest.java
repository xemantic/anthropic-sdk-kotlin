/*
 * Copyright 2024-2025 Kazimierz Pogoda / Xemantic
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

import com.xemantic.ai.anthropic.message.Role;
import org.junit.Test;

import java.util.List;

import static com.xemantic.ai.anthropic.content.Contents.text;
import static com.xemantic.ai.anthropic.message.Messages.message;

public class JavaAnthropicTest {

    @Test
    public void shouldSendMessage() {
        var anthropic = JavaAnthropic.create();
        var response = anthropic.messages.createBlocking(builder -> {
            builder.messages(
                    message($ -> {
                        $.role = Role.USER;
                        $.content = List.of(text("Hi Claude"));
                    })
            );
        });
        System.out.println(response);
    }

}
