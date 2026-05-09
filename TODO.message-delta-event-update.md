# TODO: Align `Event.MessageDelta.Delta` with current Anthropic API

## Context

While widening `Event.MessageDelta.Usage` to capture the full cumulative usage shape (cache fields + server-tool usage), I noticed that the upstream `RawMessageDeltaEvent.Delta` has grown beyond what our SDK currently models. This is **out of scope** for the cost-aggregation work and was left for a separate session.

Source of truth: official `@anthropic-ai/sdk` (npm) — `package/resources/messages/messages.d.ts`, search for `RawMessageDeltaEvent`.

## Current state (this repo)

`src/commonMain/kotlin/event/Events.kt`:

```kotlin
data class MessageDelta(
    val delta: Delta,
    val usage: Usage
) : Event {

    @Serializable
    data class Delta(
        @SerialName("stop_reason")
        val stopReason: StopReason,             // non-nullable
        @SerialName("stop_sequence")
        val stopSequence: String?
    )
    // ...
}
```

## Upstream shape (Anthropic TS SDK)

```ts
export declare namespace RawMessageDeltaEvent {
    interface Delta {
        container: MessagesAPI.Container | null;
        stop_details: MessagesAPI.RefusalStopDetails | null;
        stop_reason: MessagesAPI.StopReason | null;     // now nullable
        stop_sequence: string | null;
    }
}
```

And `RefusalStopDetails`:

```ts
export interface RefusalStopDetails {
    category: 'cyber' | 'bio' | null;
    type: 'refusal';
    explanation?: string | null;
}
```

(`Container` is the code-execution container reference — unrelated to our current scope; check the SDK for its full shape.)

## Required changes

1. Make `Delta.stopReason` nullable: `val stopReason: StopReason?`.
2. Add `@SerialName("stop_details") val stopDetails: RefusalStopDetails? = null` and introduce a `RefusalStopDetails` data class with the fields above (`category`, `type`, `explanation`).
3. Add `@SerialName("container") val container: Container? = null` (only if we choose to support it — code-execution tooling may not be modeled elsewhere yet; check before adding).
4. Audit `toMessageResponse()` in `src/commonMain/kotlin/message/MessageStreaming.kt`. It currently does:
   ```kotlin
   stopReason = event.delta.stopReason
   ```
   With nullable `stopReason`, decide the policy: keep the existing `MessageResponse.stopReason` if delta's is null, or propagate null. The `MessageResponse.stopReason` field's nullability should match.
5. Update / add tests under `src/commonTest/kotlin/` exercising a refusal stop and (if scope includes it) container info.

## Verification approach

- Compile + run all existing tests after the change.
- Add a test that triggers a refusal-style stop reason if feasible (or a unit test that decodes a fixture JSON that includes `stop_details`).
- Re-run `ResponseStreamingTest` to make sure nothing regressed.

## Related, already-done

- `Event.MessageDelta.Usage` was widened in the same area to mirror `MessageDeltaUsage` (input/output/cache/server-tool fields, all cumulative). `toMessageResponse()` was changed from add-semantics to replace-with-fallback for those fields. See git log around the time this TODO was created for context.
- `usage.ServerToolUse` was renamed to `usage.ServerToolUsage` to align with the canonical API type name. (`content.ServerToolUse<Input>` — the abstract block base — was deliberately **not** renamed.)