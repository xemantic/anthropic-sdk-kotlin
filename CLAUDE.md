# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Building
```bash
./gradlew build
```

### Testing
Set the `ANTHROPIC_API_KEY` environment variable before running tests:
```bash
export ANTHROPIC_API_KEY=your-api-key
./gradlew test
```

Many tests are integration tests that call Anthropic APIs, so they require a valid API key.

### JVM-only Build
For faster iteration during development:
```bash
./gradlew build -PjvmOnlyBuild=true
```

### Single Test Execution
```bash
./gradlew :jvmTest --tests "SpecificTestClass"
```

### Publishing
For releases:
```bash
./gradlew publishToSonatype
```

### Documentation Generation
```bash
./gradlew dokkaGeneratePublicationHtml
```

## Project Architecture

This is a Kotlin Multiplatform project providing an unofficial SDK for Anthropic's Claude API. The project is structured as a single module with platform-specific source sets.

### Source Structure
- `src/commonMain/kotlin/`: Shared Kotlin code for all platforms
- `src/jvmMain/kotlin/`: JVM-specific implementations
- `src/jsMain/kotlin/`: JavaScript-specific implementations  
- `src/nativeMain/kotlin/`: Native platform implementations
- `src/commonTest/kotlin/`: Shared test code

### Core Packages
- `com.xemantic.ai.anthropic`: Main API client (`Anthropic` class)
- `com.xemantic.ai.anthropic.message`: Message handling and request/response types
- `com.xemantic.ai.anthropic.content`: Content types (text, images, tool uses)
- `com.xemantic.ai.anthropic.tool`: Tool definition and execution framework
- `com.xemantic.ai.anthropic.event`: Streaming event handling
- `com.xemantic.ai.anthropic.cost`: Usage tracking and cost calculation
- `com.xemantic.ai.anthropic.error`: Error handling and exception types

### Key Design Patterns

#### Tool System
The SDK uses Kotlin's type system to provide type-safe tool definitions. Tools are defined using data classes with `@Serializable` annotations, and JSON schemas are automatically generated using the xemantic-ai-tool-schema library.

#### Streaming Support
The API supports both blocking message creation and streaming via Kotlin Flow. Streaming responses emit `Event` objects that can be filtered and processed.

#### Cost Tracking
Built-in cost calculation using the `CostCollector` class that tracks token usage and associated costs across API calls.

#### Platform Abstraction
Uses `expect`/`actual` declarations for platform-specific functionality like environment variable access.

## Dependencies

### Core Dependencies
- **Ktor**: HTTP client and content negotiation
- **kotlinx.serialization**: JSON serialization
- **xemantic-ai-tool-schema**: JSON schema generation for tools
- **xemantic-ai-money**: Money representation for cost calculation

### Platform-Specific HTTP Clients
- JVM: `ktor-client-java`
- Native (Linux/Windows): `ktor-client-curl`
- Native (macOS/iOS): `ktor-client-darwin`

### Gradle Configuration
- Uses version catalogs (`gradle/libs.versions.toml`)
- Kotlin target version: 2.2
- Java target version: 17
- Progressive Kotlin compilation mode enabled
- Power Assert plugin for enhanced test assertions

## Testing Notes

- Many tests are integration tests requiring `ANTHROPIC_API_KEY`
- Tests default to Claude Haiku model to reduce API costs
- Tests may be flaky due to AI model variability
- Some native target tests are disabled on CI
- Release builds skip tests to avoid flakiness during releases
- Test timeout for JS tests: 60 seconds

## Adding New Models

When adding new models to the `Model` enum in `src/commonMain/kotlin/Models.kt`:
- Always verify current pricing at anthropic.com/pricing
- Update the `cost` field with accurate `inputTokens` and `outputTokens` values using the `dollarsPerMillion` extension (e.g., `"3".dollarsPerMillion`)
- Verify `contextWindow`, `maxOutput`, and `messageBatchesApi` support from the official documentation
- Add the new model enum entry following the existing pattern

## Multiplatform Targets

### Tier 1 (Fully Supported)
- JVM
- macOS (x64, ARM64)
- iOS (ARM64, x64, Simulator ARM64)

### Tier 2 (Best Effort)
- Linux (x64, ARM64)  
- Windows (x64)
- watchOS, tvOS variants

### JavaScript
- Node.js and Browser targets supported
- Karma + Chrome Headless for browser tests
- Mocha for Node.js tests

## Important Files

- `src/commonMain/kotlin/Anthropic.kt`: Main API client
- `docs/tool_use.md`: Comprehensive tool usage documentation
- `gradle/libs.versions.toml`: Dependency version management
- `build.gradle.kts`: Build configuration with multiplatform setup