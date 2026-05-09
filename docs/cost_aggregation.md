# Cost aggregation

The SDK does not aggregate usage or compute cost during a request. Each `MessageResponse` carries the API-reported `Usage`; turning that into a `Cost` and accumulating across calls is the caller's responsibility.

## Computing per-response cost

Pick the [`Model`](../src/commonMain/kotlin/Models.kt) you intend to use and multiply the response's `usage` by its `cost`:

```kotlin
val haiku = Model.CLAUDE_HAIKU_4_5_20251001
val response = anthropic.messages.create {
    model(haiku)
    +"Hello, Claude"
}
val cost = response.usage * haiku.cost
println(cost)
```

> [!NOTE]
> `Cost` exposes per-bucket `Money` (input, output, cache 5m/1h write, cache read) and a `total`. Multiplication is commutative — `haiku.cost * response.usage` works the same.

For a model that isn't predefined as a constant in `Model.Companion`, construct a `Model(...)` instance directly with its id, context window, and pricing, and use it the same way.

## Aggregating across calls

When you also need to track the underlying `Usage` alongside cost — for reporting or auditing — pair them with `CostWithUsage` and accumulate with `+=`:

```kotlin
val anthropic = Anthropic()
val model = Model.CLAUDE_HAIKU_4_5_20251001
var costWithUsage = CostWithUsage.ZERO

repeat(3) {
    val response = anthropic.messages.create {
        model(model)
        +"Tell me a one-sentence joke."
    }
    costWithUsage += response.usage.pricedBy(model)
}

println(costWithUsage) // total usage + total cost
```

`Usage.pricedBy(model)` returns a `CostWithUsage` carrying both the per-response cost and the `Usage` itself.

## Aggregating in scopes

Because aggregation is not global, you can scope it however you need:

```kotlin
class Conversation(
    private val anthropic: Anthropic,
    private val model: Model
) {

    var costWithUsage = CostWithUsage.ZERO
        private set

    suspend fun ask(prompt: String): String {
        val response = anthropic.messages.create {
            model(this@Conversation.model)
            +prompt
        }
        costWithUsage += response.usage.pricedBy(model)
        return response.text.orEmpty()
    }
}
```

A separate accumulator per conversation, request handler, or tenant gives you isolated totals without any cross-talk.

## Sharing across coroutines

A `var costWithUsage` is fine for sequential code within a single coroutine, but it is not safe to share across concurrent producers. When several coroutines need to contribute to the same total, use [`CostCollector`](../src/commonMain/kotlin/cost/CostCollection.kt), which wraps the same accumulation in an atomic reference:

```kotlin
val costs = CostCollector()

coroutineScope {
    repeat(3) {
        launch {
            val response = anthropic.messages.create { +"Hi" }
            costs += response.usage.pricedBy(model)
        }
    }
}

println(costs.costWithUsage)
```

## Reporting

To produce a human-readable breakdown, use [`costReport`](../src/commonMain/kotlin/cost/CostReporting.kt), which renders a Markdown-style table comparing a single request against a running total:

```kotlin
val response = anthropic.messages.create { +"Hi" }
val requestCost = response.usage.pricedBy(model)
costWithUsage += requestCost

println(
    costReport(
        stats = requestCost,
        totalStats = costWithUsage
    )
)
```

## Streaming

`Flow<Event>.toMessageResponse()` consumes the stream and returns a `MessageResponse` whose `usage` carries the full final breakdown (input, output, cache, server tool counts) — same shape as a non-streaming response. So the streaming case collapses to the non-streaming pattern:

```kotlin
val haiku = Model.CLAUDE_HAIKU_4_5_20251001
val response = anthropic.messages
    .stream {
        model(haiku)
        +"Write me a haiku."
    }
    .toMessageResponse()
costWithUsage += response.usage.pricedBy(model)
```

If you need to react to events as they arrive (for token-by-token UI updates) and still want the final usage, tap the flow with `onEach { ... }` before calling `toMessageResponse()`.