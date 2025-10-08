# Tool Use Guide

The ability to use [tools](https://docs.anthropic.com/en/docs/build-with-claude/tool-use) is essential for building AI agents. This SDK makes tool use simple and type-safe using idiomatic Kotlin.

## Quick Start: Your First Tool

Let's start with the simplest possible example—getting structured output from Claude:

```kotlin
@Serializable
class Sonnet(val verses: List<Verse>)

@Serializable
class Verse(val text: String)

val anthropic = Anthropic()

suspend fun writeSonnet(): Sonnet {
    val response = anthropic.messages.create {
        +"Write me a sonnet"
        tools = listOf(Tool<Sonnet>())
        toolChoice = ToolChoice.Tool<Sonnet>()
    }
    return response.toolUseInput<Sonnet>()
}

fun main() = runBlocking {
    val sonnet = writeSonnet()
    sonnet.verses.forEach {
        println(it.text)
    }
}
```

That's it! You've defined a tool and extracted structured output from Claude.

**What's happening here:**
- We define a `Sonnet` class to describe the data structure we want
- `Tool<Sonnet>()` creates a tool from that class
- `toolChoice = ToolChoice.Tool<Sonnet>()` forces Claude to use this specific tool
- `response.toolUseInput<Sonnet>()` extracts the structured data

> [!NOTE]
> This is called **structured output**—using tools to get type-safe data from Claude without actually executing functions. Perfect for data extraction from documents, images, or text.

## Structured Output: Extracting Data from Documents

Structured output shines when extracting data from PDFs, images, or complex documents:

```kotlin
@Serializable
@SerialName("document")
data class ResearchPaper(
    val title: String,
    val authors: List<Author>,
    val year: Int
)

@Serializable
data class Author(val name: String)

val anthropic = Anthropic()

suspend fun extractPaperMetadata(): ResearchPaper {
    val response = anthropic.messages.create {
        +Document("research_paper.pdf")
        +"Extract the paper's metadata"
        tools = listOf(Tool<ResearchPaper>())
        toolChoice = ToolChoice.Tool<ResearchPaper>()
    }
    return response.toolUseInput<ResearchPaper>()
}

fun main() = runBlocking {
    val paper = extractPaperMetadata()
    println("${paper.title} by ${paper.authors.joinToString { it.name }} (${paper.year})")
}
```

> [!TIP]
> See [StructuredOutputTest.kt](../src/commonTest/kotlin/tool/StructuredOutputTest.kt) for complete working examples.

## Understanding Tool Schema

The SDK automatically generates JSON Schema from your Kotlin classes using [xemantic-ai-tool-schema](https://github.com/xemantic/xemantic-ai-tool-schema):

```kotlin
val tool = Tool<Sonnet>()
println(tool) // Prints the JSON Schema sent to Claude
```

This schema tells Claude exactly what data structure to return.

## Tools that Execute: The Weather Example

Now let's create a tool that actually *does something*—the classic weather example:

```kotlin
@SerialName("get_weather")
@Description("Get the current weather in a given location")
data class GetWeather(
    @Description("The city and state, e.g. San Francisco, CA")
    val location: String
)

val toolbox = Toolbox {
    tool<GetWeather> {
        // In production, call a weather API here
        "15 degrees in $location"
    }
}

val anthropic = Anthropic {
    defaultTools = toolbox.tools
}

suspend fun getWeatherReport(): String {
    val conversation = mutableListOf<Message>()
    conversation += "What is the weather like in San Francisco?"

    // Step 1: Claude decides to use the tool
    val response1 = anthropic.messages.create {
        messages = conversation
    }
    conversation += response1

    println(response1.stopReason) // StopReason.TOOL_USE

    // Step 2: Execute the tool and get results
    conversation += response1.useTools(toolbox)

    // Step 3: Claude provides final answer with tool results
    val response2 = anthropic.messages.create {
        messages = conversation
    }

    return response2.text!! // "The weather in San Francisco is 15 degrees"
}

fun main() = runBlocking {
    val report = getWeatherReport()
    println(report)
}
```

**Key concepts:**
- **Toolbox**: Container for all your tools and their execution logic
- **defaultTools**: Makes tools available for all requests
- **response.useTools(toolbox)**: Executes tools and returns results as a Message
- **Conversation flow**: Request → Tool use → Tool result → Final answer

> [!NOTE]
> The `@Description` annotation both documents your code and helps Claude understand when to use the tool. It implies `@Serializable`, so you can omit that annotation.

> [!TIP]
> See [GetWeatherToolTest.kt](../src/commonTest/kotlin/tool/GetWeatherToolTest.kt) for the complete working example.

## Tool Naming

Tools need names. Here are the rules:

**Implicit naming:**
```kotlin
class GetWeather(val location: String)

Tool<GetWeather>() // name: "your_package_GetWeather"
```

> [!NOTE]
> With implicit naming the full class name will be extracted and sent to Claude as a tool definition. It is always better to name tools explicitly to avoid implicit and possibly long names.

> [!WARNING]
> Due to the nature of reflection in Kotlin JVM / native / JS, the implicit class name might differ across platforms. For example Kotlin JS will use only simple class name without package.

**Automatic naming:**
```kotlin
@SerialName("get_weather")
class GetWeather(val location: String)

Tool<GetWeather>() // name: "get_weather"
```

**Explicit naming:**
```kotlin
class GetWeather(val location: String)

Tool<GetWeather>(name = "get_weather") // explicitly set name
```

**Naming rules:**
- **Best practice**: Always use `@SerialName` or explicit `name` parameter for consistent, portable tool names
- **Priority**: Explicit `name` parameter > `@SerialName` annotation > implicit class name
- **Implicit naming**: Uses fully qualified class name (e.g., `com.example.GetWeather`), but behavior varies by platform
- **Name normalization**: All names are normalized before sending to Claude:
  - Trailing `$` characters removed
  - `.` (dot) and `$` (dollar) characters replaced with `_` (underscore)
  - Truncated to maximum 64 characters

See [NormalizedToolNameTest.kt](../src/commonTest/kotlin/tool/NormalizedToolNameTest.kt) for examples.

## Toolbox: Managing Multiple Tools

The `Toolbox` is your central tool registry:

```kotlin
val toolbox = Toolbox {
    tool<GetWeather> {
        fetchWeatherFromAPI(location)
    }

    tool<Calculator> {
        calculate()
    }

    tool<SearchDatabase> {
        database.query(sql)
    }
}

// Set as default for all requests
val anthropic = Anthropic {
    defaultTools = toolbox.tools
}

suspend fun calculate(): String {
    // Or use per-request
    val response = anthropic.messages.create {
        +"Calculate 15 * 7"
        tools = toolbox.tools
    }
    return response.text!!
}

fun main() = runBlocking {
    println(calculate())
}
```

**Benefits:**
- Single place to define all tools and their behavior
- Easy dependency injection (databases, HTTP clients, etc.)
- Clean separation between tool definition and execution

## Tool Dependencies and Dependency Injection

Tools often need external dependencies like databases or HTTP clients:

```kotlin
// Your dependencies
interface Database {
    suspend fun execute(sql: String): List<String>
}

val database: Database = createDatabase()

// Define the tool
@SerialName("query_database")
@Description("Executes SQL queries on the database")
data class QueryDatabase(val sql: String)

// Inject dependencies via closures
val toolbox = Toolbox {
    tool<QueryDatabase> {
        database.execute(sql) // 'database' captured from outer scope
    }
}

val anthropic = Anthropic {
    defaultTools = toolbox.tools
}

suspend fun queryCustomers(): Message {
    val response = anthropic.messages.create {
        +"List all customers from the database"
    }
    return response.useTools(toolbox)
}

fun main() = runBlocking {
    val results = queryCustomers()
    println(results)
}
```

**Pattern:**
- Dependencies are captured from the surrounding scope
- The tool lambda has access to tool input properties (`sql`) and outer scope variables (`database`)
- This works for any dependency: HTTP clients, caches, configuration, etc.

> [!TIP]
> See [ToolWithDependenciesTest.kt](../src/commonTest/kotlin/tool/ToolWithDependenciesTest.kt) for complete examples.

## Working with Multiple Tools

Claude can intelligently choose between multiple tools:

```kotlin
@SerialName("calculator")
@Description("Performs arithmetic operations")
data class Calculator(
    val operation: Operation,
    val a: Double,
    val b: Double
) {
    enum class Operation { ADD, SUBTRACT, MULTIPLY, DIVIDE }

    fun calculate(): Double = when (operation) {
        Operation.ADD -> a + b
        Operation.SUBTRACT -> a - b
        Operation.MULTIPLY -> a * b
        Operation.DIVIDE -> a / b
    }
}

@SerialName("fibonacci_calculator")
@Description("Calculates the n-th Fibonacci number")
data class FibonacciCalculator(val n: Int) {
    fun calculate(): Int = fibonacci(n)
}

val toolbox = Toolbox {
    tool<Calculator> { calculate() }
    tool<FibonacciCalculator> { calculate() }
}

val anthropic = Anthropic {
    defaultTools = toolbox.tools
}

suspend fun complexCalculation(): String {
    val conversation = mutableListOf<Message>()
    conversation += "Calculate Fibonacci number 42 and divide it by 42"

    // First tool use: FibonacciCalculator
    val response1 = anthropic.messages.create {
        messages = conversation
        toolChoice = ToolChoice.Tool<FibonacciCalculator>()
    }
    conversation += response1
    conversation += response1.useTools(toolbox)

    // Second tool use: Calculator
    val response2 = anthropic.messages.create {
        messages = conversation
        toolChoice = ToolChoice.Tool<Calculator>()
    }
    conversation += response2
    conversation += response2.useTools(toolbox)

    // Final response with answer
    val response3 = anthropic.messages.create {
        messages = conversation
    }

    return response3.text!! // "6,378,911.8..."
}

fun main() = runBlocking {
    println(complexCalculation())
}
```

> [!TIP]
> See [UseToolsTest.kt](../src/commonTest/kotlin/tool/UseToolsTest.kt) for complete multi-tool examples.

## Tool Choice: Controlling Tool Usage

Control when and which tools Claude uses:

```kotlin
// Auto: Claude decides if/which tools to use (default)
toolChoice = ToolChoice.Auto()

// Any: Claude MUST use at least one tool (any available tool)
toolChoice = ToolChoice.Any()

// Tool by name: Force a specific tool
toolChoice = ToolChoice.Tool("get_weather")

// Tool by type: Force a specific tool (type-safe)
toolChoice = ToolChoice.Tool<GetWeather>()

// Disable parallel tool use
toolChoice = ToolChoice.Auto {
    disableParallelToolUse = true
}
```

**When to use each:**
- `Auto`: Normal operation, let Claude decide
- `Any`: When you definitely want a tool call, but don't care which
- `Tool`: When you need a specific tool (e.g., structured output extraction)

## Tool Return Types

The SDK automatically converts tool return values to appropriate `ToolResult` content. Here's how different return types are handled:

### Basic Types

```kotlin
// String → Text content
tool<GetWeather> {
    "15 degrees in $location"
}

// Number → Text content (converted to string)
tool<Calculate> {
    42.5 // Returns as Text("42.5")
}

// Unit → Default "ok" message
tool<SendEmail> {
    sendEmail(to, subject, body)
    // Returns Text("ok") automatically
}
```

### Content Types

SDK content types (`Text`, `Image`, `Document`) are automatically recognized:

```kotlin
// Image content
tool<Screenshot> {
    Image {
        source = Source.Base64 {
            mediaType(MediaType.PNG)
            data = captureScreen()
        }
    }
}

// Document content
tool<GeneratePDF> {
    Document {
        source = Source.Base64 {
            mediaType(MediaType.PDF)
            data = pdfData
        }
    }
}

// Text content (explicit)
tool<FormatOutput> {
    Text("Formatted result")
}
```

### Complex Objects

Serializable objects are automatically converted to JSON:

```kotlin
@Serializable
data class UserProfile(val name: String, val age: Int)

tool<GetUserProfile> {
    UserProfile(name = "Alice", age = 30)
    // Returns as Text('{"name":"Alice","age":30}')
}
```

Non-serializable objects fall back to `toString()`:

```kotlin
tool<GetTime> {
    LocalDateTime.now() // Returns Text with toString() result
}
```

### Lists

Lists become multiple content elements in the `ToolResult`:

```kotlin
// List of strings → Multiple Text elements
tool<ListFiles> {
    listOf("file1.txt", "file2.txt", "file3.txt")
    // Returns ToolResult with 3 Text content elements
}

// Mixed content types
tool<GenerateReport> {
    listOf(
        Text("Summary: ..."),
        Image { source = chartImage },
        Document { source = reportPdf }
    )
}

// Null elements → Text("null")
tool<GetOptionalValues> {
    listOf("value1", null, "value3")
    // Returns: Text("value1"), Text("null"), Text("value3")
}
```

### Error Handling

When a tool throws an exception, it's automatically converted to an error `ToolResult`:

```kotlin
tool<RiskyOperation> {
    throw Exception("Operation failed: timeout")
    // Returns ToolResult with:
    // - isError = true
    // - content = [Text("Operation failed: timeout")]
}
```

**Conversion Rules Summary:**
- `Content` types (`Text`, `Image`, `Document`) → Used directly
- `String` → `Text`
- Numbers, booleans → `Text` (via `toString()`)
- `@Serializable` objects → `Text` (as JSON)
- Non-serializable objects → `Text` (via `toString()`)
- `List<*>` → Multiple content elements (each element converted individually)
- `null` in lists → `Text("null")`
- `Unit` → `Text("ok")`
- Exceptions → `Text(exception.message)` with `isError = true`

> [!TIP]
> See [MessageResponseUseToolsTest.kt](../src/commonTest/kotlin/message/MessageResponseUseToolsTest.kt) for comprehensive examples of all return type conversions.

## Streaming Structured Output

You can stream structured output as it's being generated, receiving partial JSON as Claude builds the response:

```kotlin
@Serializable
class Sonnet(
    val verses: List<Verse>
)

@Serializable
class Verse(
    val text: String
)

val anthropic = Anthropic()

fun streamSonnet(): Flow<String> = anthropic.messages.stream {
    +"Write me a sonnet about artificial intelligence"
    tools = listOf(Tool<Sonnet>())
    toolChoice = ToolChoice.Tool<Sonnet>()
}
    .filterIsInstance<Event.ContentBlockDelta>()
    .filter { it.delta is Event.ContentBlockDelta.Delta.InputJsonDelta }
    .map { it.partialJson }

fun main() = runBlocking {
    println("Streaming JSON fragments as they arrive:")
    streamSonnet().collect { jsonFragment ->
        print(jsonFragment)
    }
    println("\n\nDone!")
}
```

**What's happening:**
- Returns a `Flow<String>` of JSON fragments instead of the complete response
- `.filterIsInstance<Event.ContentBlockDelta>()` filters for delta events
- `.map { }` extracts `partialJson` from `InputJsonDelta` events
- Consumer can collect and display fragments in real-time

**Use cases:**
- Show progress for long-running structured data extraction
- Display partial results as they arrive (e.g., showing verses as they're written)
- Provide user feedback during document parsing or data extraction
- Build custom UI components that react to streaming JSON updates

> [!TIP]
> See [GetWeatherStreamingToolTest.kt](../src/commonTest/kotlin/tool/GetWeatherStreamingToolTest.kt) for more streaming examples with tools.

## Error Handling

Handle tool execution errors gracefully:

```kotlin
val toolbox = Toolbox {
    exceptionHandler = { exception ->
        logger.error("Tool execution failed", exception)
        // Log to monitoring system, send alerts, etc.
    }

    tool<RiskyOperation> {
        // If this throws, exceptionHandler is called
        performRiskyOperation()
    }
}
```

The exception is logged, and an error message is returned to Claude automatically.

## Advanced: Tool Properties

Customize tool behavior with properties:

```kotlin
val tool = Tool<GetWeather> {
    cacheControl = CacheControl.Ephemeral()
}
```

This enables prompt caching for tools, reducing costs for repeated tool definitions.

## Built-in Tools

Anthropic provides special built-in tools for computer control (requires Beta access):

```kotlin
val anthropic = Anthropic {
    +Beta.COMPUTER_USE_2025_01_24
}


val toolbox = Toolbox {
    tool(
        Computer {
            displayWidthPx = 1920
            displayHeightPx = 1080
        }
    ) { input ->
        // Handle computer control actions
        when (input.action) {
            Computer.Action.SCREENSHOT -> takeScreenshot()
            Computer.Action.MOUSE_MOVE -> moveMouse(input.coordinate!!)
            // ... etc
        }
    }
}
```

Other built-in tools:
- `Bash`: Execute bash commands
- `TextEditor`: Edit text files

> [!WARNING]
> Built-in tools require the `COMPUTER_USE_2025_01_24` beta feature flag and proper security measures.

## Complete Example: Multi-Turn Conversation

Here's a complete example tying everything together:

```kotlin
@SerialName("get_weather")
@Description("Get the current weather in a given location")
data class GetWeather(
    @Description("The city and state, e.g. San Francisco, CA")
    val location: String
)

val toolbox = Toolbox {
    tool<GetWeather> {
        // In production, call weather API
        when (location.lowercase()) {
            "san francisco" -> "15°C, sunny"
            "new york" -> "5°C, cloudy"
            else -> "Weather data not available"
        }
    }
}

val anthropic = Anthropic {
    defaultTools = toolbox.tools
}

suspend fun multiCityWeather(): String {
    val conversation = mutableListOf<Message>()
    conversation += "What's the weather in San Francisco and New York?"

    // Agent loop: Continue until no more tool use
    do {
        val response = anthropic.messages.create {
            messages = conversation
        }
        conversation += response

        if (response.stopReason == StopReason.TOOL_USE) {
            conversation += response.useTools(toolbox)
        }
    } while (response.stopReason == StopReason.TOOL_USE)

    val finalResponse = conversation.last() as MessageResponse
    return finalResponse.text!!
    // "In San Francisco it's 15°C and sunny, while New York is 5°C and cloudy."
}

fun main() = runBlocking {
    println(multiCityWeather())
}
```

## Best Practices

1. **Use descriptive tool and parameter names** – Claude works better with clear, semantic names
2. **Add @Description annotations** – Helps Claude understand when and how to use tools
3. **Keep tool inputs simple** – Flat structures work better than deep nesting
4. **Use Toolbox for organization** – Centralize tool definitions and dependencies
5. **Handle errors gracefully** – Always set an exceptionHandler
6. **Test with different prompts** – Claude's tool selection depends on how you phrase requests
7. **Use structured output for data extraction** – Don't execute functions if you just need data
8. **Set defaultTools when appropriate** – Reduces boilerplate for commonly-used tools

## Working Examples

For complete, runnable examples, see:
- [StructuredOutputTest.kt](../src/commonTest/kotlin/tool/StructuredOutputTest.kt) – Data extraction
- [GetWeatherToolTest.kt](../src/commonTest/kotlin/tool/GetWeatherToolTest.kt) – Basic tool execution
- [UseToolsTest.kt](../src/commonTest/kotlin/tool/UseToolsTest.kt) – Multi-tool scenarios
- [ToolWithDependenciesTest.kt](../src/commonTest/kotlin/tool/ToolWithDependenciesTest.kt) – Dependency injection
- [GetWeatherStreamingToolTest.kt](../src/commonTest/kotlin/tool/GetWeatherStreamingToolTest.kt) – Streaming

For more sophisticated examples across different platforms, see [anthropic-sdk-kotlin-demo](https://github.com/xemantic/anthropic-sdk-kotlin-demo).

## API Reference

Key types and functions:

- `Tool<T>()` – Create a tool from a data class
- `Toolbox { }` – Container for tools and their execution logic
- `response.useTools(toolbox)` – Execute tools and return results
- `response.toolUseInput<T>()` – Extract structured output
- `ToolChoice.Auto/Any/Tool` – Control which tools Claude uses
- `@Description` – Document tools and parameters for Claude
- `@SerialName` – Specify tool name explicitly