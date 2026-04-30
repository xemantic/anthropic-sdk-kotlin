# CLAUDE.md

This file captures only what cannot be inferred from the codebase itself.

## Rules for editing this file

Both developers and AI agents are expected to add entries as they encounter surprises.

- **Add an entry** when you encounter something unexpected: a build quirk, a non-obvious constraint, a dependency gotcha, or any behavior that would surprise the next agent or developer.
- **Add an entry** when a developer flags an anti-pattern produced by AI — describe the anti-pattern and the preferred alternative.
- **Do not** add codebase overviews, directory listings, or anything discoverable by reading the source.
- Keep entries concise: one line per lesson, grouped under a heading if a theme emerges.

## Known gotchas

- Copyright year range (e.g. 2025-2026) is applied on autosave — new files should use only the current year (e.g. 2026).
- Kotlin context-sensitive resolution (`-Xcontext-sensitive-resolution`, preview in 2.2 / refined in 2.3) is enabled in the convention plugin. Inside a `when` whose subject has a known sealed type (or for `is`/`as` against that type), drop the type prefix on subclass references — write `is Heading` / `Paragraph`, not `is BlockMode.Heading` / `BlockMode.Paragraph`. CSR also applies to explicit return types, declared variable types, and parameter types when an outer expected type drives resolution. It does NOT apply to functions, properties with parameters, extension properties with receivers, type-annotation positions for variables, supertype lists, or generic constraints — keep the prefix in those positions.
- In Claude Code "auto mode", never commit on your own — leave changes in the working tree so the user can review the diff first. Only commit when the user explicitly asks for it.
- When generating backtick-quoted Kotlin identifiers (e.g. test names) from arbitrary input, strip CR, LF, and ``` ` \ < > [ ] / . : ; * ? " | ``` before wrapping in backticks.

### Testing

- Most tests are live integration tests against Anthropic APIs and require `ANTHROPIC_API_KEY` in the environment; without it they fail rather than skip.
- Tests default to Claude Haiku to keep API costs down — preserve that default when adding new tests unless a specific model is under test.
- Tests can be flaky due to AI model variability; release builds intentionally skip tests for this reason, so don't gate releases on green test runs.
- Tests must retain `// given`, `// when`, `// then` comment structure — AI agents tend to omit these.

### Auto mode

- Do not commit when auto mode is active — wait for an explicit commit instruction from the user.

### Building

- `./gradlew build -PjvmOnlyBuild=true` skips non-JVM targets for faster local iteration — useful when you don't need to verify multiplatform output.

### Adding new models

- Verify pricing at anthropic.com/pricing before adding a `Model` enum entry — pricing isn't derivable from code and is the most common source of incorrect entries.

## Anti-patterns to avoid

- Do not add content to this file that is already discoverable by reading the source or build scripts — that inflates context without adding signal, reducing AI agent task success rates (see [arxiv 2602.11988](https://arxiv.org/abs/2602.11988)).