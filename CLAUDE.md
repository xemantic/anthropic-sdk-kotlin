# CLAUDE.md

This file captures only what cannot be inferred from the codebase itself.

## Rules for editing this file

Both developers and AI agents are expected to add entries as they encounter surprises.

- **Add an entry** when you encounter something unexpected: a build quirk, a non-obvious constraint, a dependency gotcha, or any behavior that would surprise the next agent or developer.
- **Add an entry** when a developer flags an anti-pattern produced by AI — describe the anti-pattern and the preferred alternative.
- **Do not** add codebase overviews, directory listings, or anything discoverable by reading the source.
- Keep entries concise: one line per lesson, grouped under a heading if a theme emerges.

## Known gotchas

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