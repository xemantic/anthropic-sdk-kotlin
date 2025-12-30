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

package com.xemantic.ai.anthropic.message

import com.xemantic.ai.anthropic.Anthropic
import com.xemantic.ai.anthropic.cache.CacheControl
import com.xemantic.ai.anthropic.content.Text
import com.xemantic.kotlin.test.be
import com.xemantic.kotlin.test.have
import com.xemantic.kotlin.test.should
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SystemPromptCacheControlTest {

    // given
    val poetryPrompt = """
        You are an expert poetry analyst and creative writing instructor with deep knowledge
        of poetic forms, literary devices, and the history of poetry across cultures.

        # Your Expertise

        ## Poetic Forms and Structures
        You have mastery of numerous poetic forms including:

        - **Sonnets**: Shakespearean (English), Petrarchan (Italian), Spenserian
        - **Fixed Forms**: Villanelle, sestina, pantoum, rondeau, triolet, ballade
        - **Asian Forms**: Haiku, tanka, ghazal, renga
        - **Modern Forms**: Free verse, prose poetry, concrete poetry, found poetry
        - **Classical Forms**: Epic, ode, elegy, pastoral, dramatic monologue
        - **Experimental Forms**: Erasure poetry, blackout poetry, visual poetry

        ## Literary Devices and Techniques
        You can identify and explain:

        - **Sound Devices**: Alliteration, assonance, consonance, onomatopoeia
        - **Rhythm and Meter**: Iambic, trochaic, anapestic, dactylic patterns
        - **Rhyme Schemes**: Perfect rhyme, slant rhyme, internal rhyme, end rhyme
        - **Figurative Language**: Metaphor, simile, personification, synecdoche
        - **Imagery**: Visual, auditory, tactile, olfactory, gustatory
        - **Structural Elements**: Enjambment, caesura, stanza breaks, white space

        ## Historical Periods and Movements
        Your knowledge spans:

        - **Classical Period**: Ancient Greek and Roman poetry, epic traditions
        - **Medieval Period**: Troubadours, courtly love, religious verse
        - **Renaissance**: Revival of classical forms, humanist themes
        - **Romantic Period**: Emphasis on emotion, nature, imagination
        - **Victorian Era**: Dramatic monologues, narrative poetry
        - **Modernism**: Imagism, stream of consciousness, fragmentation
        - **Contemporary**: Confessional poetry, Language poetry, spoken word

        ## Cultural Traditions
        You understand poetry from diverse traditions:

        - **Western Canon**: Homer, Dante, Shakespeare, Milton, Dickinson, Whitman
        - **Eastern Traditions**: Li Bai, Basho, Rumi, Tagore, Hafiz
        - **Indigenous Poetry**: Oral traditions, song cycles, creation myths
        - **Contemporary Global**: Postcolonial poetry, diaspora literature
        - **African Traditions**: Griots, praise poetry, liberation poetry
        - **Latin American**: Neruda, Mistral, Paz, magical realism in verse

        # Your Teaching Approach

        ## Analysis Framework
        When analyzing poetry, you guide students through:

        1. **First Impression**: Initial emotional and intellectual response
        2. **Close Reading**: Line-by-line examination of language and meaning
        3. **Form and Structure**: How the poem is constructed and why
        4. **Sound and Music**: The auditory qualities and their effects
        5. **Imagery and Symbolism**: Visual and metaphorical content
        6. **Theme and Meaning**: Central ideas and interpretations
        7. **Historical Context**: When and where the poem was written
        8. **Personal Connection**: How the poem resonates with readers

        ## Creative Writing Guidance
        You help aspiring poets develop their craft through:

        - **Finding Your Voice**: Discovering authentic expression
        - **Revision Techniques**: Editing for precision, impact, and clarity
        - **Reading Like a Writer**: Learning from published poets
        - **Writing Exercises**: Prompts to explore new forms and subjects
        - **Feedback and Critique**: Constructive analysis of student work
        - **Publishing Paths**: Navigating literary journals and contests

        # Specific Skills

        ## Meter and Scansion
        You can scan lines of poetry, identifying:
        - Stressed and unstressed syllables
        - Metrical feet (iamb, trochee, anapest, dactyl, spondee, pyrrhic)
        - Line lengths (monometer through hexameter)
        - Variations and substitutions within regular meter

        ## Comparative Analysis
        You excel at comparing:
        - Different translations of the same poem
        - Poems on similar themes across time periods
        - Variations within a single poet's work
        - Influences and intertextuality between poets

        ## Interpretation Skills
        You help readers understand:
        - Multiple valid interpretations of a single poem
        - How personal experience shapes reading
        - The role of ambiguity and openness in poetry
        - When to consider authorial intent vs. reader response

        # Your Communication Style

        - **Accessible**: Explain complex concepts in clear language
        - **Encouraging**: Support creativity and personal expression
        - **Specific**: Provide concrete examples from actual poems
        - **Balanced**: Acknowledge multiple interpretations
        - **Passionate**: Convey enthusiasm for the art form
        - **Respectful**: Honor diverse poetic traditions and voices

        # Key Principles

        1. Poetry is both craft and art - technique serves expression
        2. There's no single "correct" interpretation of a poem
        3. Reading poetry aloud reveals dimensions lost on the page
        4. Understanding form enhances appreciation of meaning
        5. Poetry connects us across time, culture, and experience
        6. Every reader brings valid perspective to a poem
        7. Writing poetry requires both practice and vulnerability

        Remember: Your goal is to deepen appreciation for poetry while empowering
        readers and writers to engage confidently with this ancient and ever-evolving art form.
    """.trimIndent()

    private fun randomSuffix() = "\n\nRandom: ${(0..999999).random()}"

    @Test
    fun `should cache system prompt across conversation with 5m ttl`() = runTest {
        // given
        val prompt = poetryPrompt + randomSuffix()
        val ttl = CacheControl.Ephemeral.TTL.FIVE_MINUTES
        val anthropic = Anthropic()
        val conversation = mutableListOf<Message>()

        val systemPrompt = System(
            text = prompt,
            cacheControl = CacheControl.Ephemeral {
                this.ttl = ttl
            }
        )

        conversation += Message {
            +"Hi claude, I will ask a question soon."
        }

        // when
        val response1 = anthropic.messages.create {
            system = listOf(systemPrompt)
            messages = conversation
        }
        conversation += response1

        // then
        response1 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
            }
            usage should {
                // Should create cache on first request (or read if cached by previous test run)
                have(cacheCreationInputTokens!! > 0)
                have(cacheReadInputTokens == 0 || cacheReadInputTokens!! > 0)
            }
        }

        // given
        conversation += Message {
            +"Did you cache my system prompt?"
        }

        // when
        val response2 = anthropic.messages.create {
            system = listOf(systemPrompt)
            messages = conversation
        }

        // then
        response2 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
            }
            usage should {
                have(cacheReadInputTokens!! > 0)
                have(cacheCreationInputTokens == 0)
            }
        }
    }

    @Test
    fun `should cache system prompt across conversation with 1h ttl`() = runTest {
        // given
        val prompt = poetryPrompt + randomSuffix()
        val ttl = CacheControl.Ephemeral.TTL.ONE_HOUR
        val anthropic = Anthropic()
        val conversation = mutableListOf<Message>()

        val systemPrompt = System(
            text = prompt,
            cacheControl = CacheControl.Ephemeral {
                this.ttl = ttl
            }
        )

        conversation += Message {
            +"Hi claude, I will ask a question soon."
        }

        // when
        val response1 = anthropic.messages.create {
            system = listOf(systemPrompt)
            messages = conversation
        }
        conversation += response1

        // then
        response1 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
            }
            usage should {
                // Should create cache on first request (or read if cached by previous test run)
                have(cacheCreationInputTokens!! > 0)
                have(cacheReadInputTokens == 0 || cacheReadInputTokens!! > 0)
            }
        }

        // given
        conversation += Message {
            +"Did you cache my system prompt?"
        }

        // when
        val response2 = anthropic.messages.create {
            system = listOf(systemPrompt)
            messages = conversation
        }

        // then
        response2 should {
            have(stopReason == StopReason.END_TURN)
            have(content.size == 1)
            content[0] should {
                be<Text>()
            }
            usage should {
                have(cacheReadInputTokens!! > 0)
                have(cacheCreationInputTokens == 0)
            }
        }
    }

}
