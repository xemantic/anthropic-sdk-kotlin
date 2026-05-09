# TODO: Refactor `toMessageResponse()` and add fixture-based test coverage

## Context

`src/commonMain/kotlin/message/MessageStreaming.kt` collects streamed events
into a `MessageResponse` by mutating a single `response` variable as it goes.
Every `MessageDelta` rebuilds it via `copy(...)`, and the merge policy
(fallback to `MessageStart` values for optional usage fields) is read live off
the previous mutation through `val startUsage = response!!.usage`.

Two issues:

1. The collection loop and the merge policy are conflated — harder to read,
   harder to test, fragile if a second `MessageDelta` ever arrives or the
   field set grows.
2. Test coverage is thin. `src/commonTest/kotlin/ResponseStreamingTest.kt`
   contains only live integration tests that assert text content, partial
   usage under cache scenarios, and an error path. Nothing offline pins down
   the full reconstructed `MessageResponse` (id, model, role, content list,
   complete usage, stopReason, stopSequence) against a known event sequence.
   A regression that, for example, dropped `id` or mismerged `inputTokens`
   would slide through.

This TODO depends on no other work and should be done **before**
`TODO.message-delta-event-update.md` (item 4 there flips `stopReason` to
nullable — much easier to express once the merge is a pure function).

## Goals

1. Split `toMessageResponse()` into:
   - an event-collection loop that records *what was seen*, and
   - a pure `mergeStreamedResponse(...)` function that builds the final
     `MessageResponse`.
2. Keep public API identical (`Flow<Event>.toMessageResponse(): MessageResponse`).
3. Add an offline, deterministic test that drives `toMessageResponse()` from
   a hand-built event sequence and asserts the entire reconstructed
   `MessageResponse`.

## Step 1 — Refactor `MessageStreaming.kt`

Replace the single mutating `response` with separate captured fields, and
move all the merge logic into a pure function:

```kotlin
suspend fun Flow<Event>.toMessageResponse(): MessageResponse {
    var start: MessageResponse? = null
    var deltaUsage: Event.MessageDelta.Usage? = null
    var deltaStopReason: StopReason? = null
    var deltaStopSequence: String? = null

    val content = mutableListOf<Content>()
    val contentBuilder = StringBuilder()
    var toolUse: ToolUse? = null
    var messageStopped = false

    collect { event ->
        when (event) {
            is MessageStart      -> start = event.message
            is ContentBlockStart -> { /* unchanged */ }
            is ContentBlockDelta -> { /* unchanged */ }
            is ContentBlockStop  -> { /* unchanged — appends to content */ }
            is MessageDelta -> {
                deltaUsage        = event.usage
                deltaStopReason   = event.delta.stopReason
                deltaStopSequence = event.delta.stopSequence
            }
            is MessageStop       -> messageStopped = true
            is Ping              -> { }
            is Error             -> throw AnthropicApiException(...)
        }
    }
    check(messageStopped) { "No final message_stop event received" }
    return mergeStreamedResponse(
        start = checkNotNull(start) { "No message_start event received" },
        content = content,
        deltaUsage = deltaUsage,
        deltaStopReason = deltaStopReason,
        deltaStopSequence = deltaStopSequence,
    )
}

internal fun mergeStreamedResponse(
    start: MessageResponse,
    content: List<Content>,
    deltaUsage: Event.MessageDelta.Usage?,
    deltaStopReason: StopReason?,
    deltaStopSequence: String?,
): MessageResponse = start.copy(
    content = content,
    stopReason = deltaStopReason ?: start.stopReason,
    stopSequence = deltaStopSequence ?: start.stopSequence,
    usage = if (deltaUsage == null) start.usage else Usage {
        inputTokens              = deltaUsage.inputTokens              ?: start.usage.inputTokens
        outputTokens             = deltaUsage.outputTokens
        cacheCreationInputTokens = deltaUsage.cacheCreationInputTokens ?: start.usage.cacheCreationInputTokens
        cacheReadInputTokens     = deltaUsage.cacheReadInputTokens     ?: start.usage.cacheReadInputTokens
        cacheCreation            = start.usage.cacheCreation
        serverToolUse            = deltaUsage.serverToolUse            ?: start.usage.serverToolUse
    },
)
```

Notes:

- `mergeStreamedResponse` is `internal` so the test can drive it directly when
  isolating merge behavior from flow handling.
- Policy: when the delta is missing a field, fall back to `MessageStart` —
  same semantics as today, just expressed in one place.
- `stopReason` and `stopSequence` are now also fallback-merged. Today the
  code blindly replaces with `event.delta.stopReason`. Once
  `TODO.message-delta-event-update.md` flips it nullable, fallback is what
  we want — keeps the policy decision local and obvious for that future
  change.
- `start.usage.cacheCreation` is always carried from `MessageStart` since
  `MessageDelta.Usage` doesn't carry the breakdown — same as today.

Bonus cleanup while in the file: fix the typo `emtpyJson` → `emptyJson`
(`MessageStreaming.kt:53`, `:118`).

## Step 2 — Add `src/commonTest/kotlin/message/MessageStreamingTest.kt`

Pure flow-driven test, no API key, no network. Sketch:

```kotlin
class MessageStreamingTest {

    @Test
    fun `should reconstruct full response from event stream`() = runTest {
        // given
        val events: List<Event> = listOf(
            MessageStart(message = MessageResponse(
                model = "claude-haiku-4-5-20251001",
                id = "msg_test_1",
                role = Role.ASSISTANT,
                content = emptyList(),
                stopReason = null,
                stopSequence = null,
                usage = Usage {
                    inputTokens = 10; outputTokens = 1
                    cacheCreationInputTokens = 0
                    cacheReadInputTokens = 100
                },
            )),
            ContentBlockStart(index = 0, contentBlock = Event.ContentBlockStart.ContentBlock.Text("")),
            ContentBlockDelta(index = 0, delta = Event.ContentBlockDelta.Delta.TextDelta("Hello")),
            ContentBlockDelta(index = 0, delta = Event.ContentBlockDelta.Delta.TextDelta(", world")),
            ContentBlockStop(index = 0),
            ContentBlockStart(index = 1, contentBlock = Event.ContentBlockStart.ContentBlock.ToolUse(
                id = "toolu_1", name = "get_weather", input = buildJsonObject {},
            )),
            ContentBlockDelta(index = 1, delta = Event.ContentBlockDelta.Delta.InputJsonDelta("""{"city":"""")),
            ContentBlockDelta(index = 1, delta = Event.ContentBlockDelta.Delta.InputJsonDelta("""Berlin"}""")),
            ContentBlockStop(index = 1),
            MessageDelta(
                delta = Event.MessageDelta.Delta(
                    stopReason = StopReason.TOOL_USE, stopSequence = null,
                ),
                usage = Event.MessageDelta.Usage(
                    inputTokens = null,                  // unchanged → falls back
                    outputTokens = 42,
                    cacheCreationInputTokens = null,
                    cacheReadInputTokens = null,
                    serverToolUse = null,
                ),
            ),
            MessageStop(),
        )

        // when
        val response = events.asFlow().toMessageResponse()

        // then
        response should {
            have(id == "msg_test_1")
            have(model == "claude-haiku-4-5-20251001")
            have(role == Role.ASSISTANT)
            have(stopReason == StopReason.TOOL_USE)
            have(stopSequence == null)
            have(content.size == 2)
            have((content[0] as Text).text == "Hello, world")
            (content[1] as ToolUse) should {
                have(id == "toolu_1")
                have(name == "get_weather")
                have(input["city"]!!.jsonPrimitive.content == "Berlin")
            }
            usage should {
                have(inputTokens == 10)              // fell back to start
                have(outputTokens == 42)             // from delta
                have(cacheReadInputTokens == 100)    // fell back
                have(cacheCreationInputTokens == 0)
            }
        }
    }

    @Test
    fun `merge should fall back to start usage when delta omits optional fields`() {
        // direct call to internal mergeStreamedResponse with three variants:
        // (a) deltaUsage == null            → start.usage preserved as-is
        // (b) deltaUsage with all nulls except outputTokens → outputTokens replaces, rest fall back
        // (c) deltaUsage replaces non-null fields → replacement wins
    }

    @Test
    fun `should fail without message_stop`() = runTest {
        assertFailsWith<IllegalStateException> {
            flowOf(MessageStart(...) /* no MessageStop */).toMessageResponse()
        }
    }

    @Test
    fun `should propagate Error event as AnthropicApiException`() = runTest {
        // build a flow ending in an Error event, expect AnthropicApiException
    }
}
```

What this pins down that the live tests don't:

- `id`, `model`, `role` survive from `MessageStart` into the final response.
- Multi-block content (text + tool-use) reconstructed in order.
- Tool-use input JSON correctly assembled from `InputJsonDelta` fragments.
- Per-field merge policy on `Usage` (the part most likely to silently
  regress when `TODO.message-delta-event-update.md` lands).
- Stop reason and stop sequence end up on the final response.
- Error path throws `AnthropicApiException`.
- Missing `message_stop` fails fast.

Per `CLAUDE.md`, retain `// given`, `// when`, `// then` comment structure.

## Step 3 — Verify

- `./gradlew build -PjvmOnlyBuild=true` to compile and run common tests fast.
- Existing `ResponseStreamingTest` integration tests stay green when
  `ANTHROPIC_API_KEY` is set (per `CLAUDE.md`, they fail rather than skip
  without it).

## Out of scope (deliberately)

- `TODO.message-delta-event-update.md` changes (nullable `stopReason`,
  `stop_details`, `container`). The merge function is the right *home* for
  that policy; the actual schema change is a separate session. Once this
  refactor lands, that TODO becomes a one-line change in
  `mergeStreamedResponse` plus an additional test fixture.