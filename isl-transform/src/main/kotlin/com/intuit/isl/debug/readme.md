# Execution hooks

- **[IExecutionHook](IExecutionHook.kt)** — optional callbacks around statement execution and function boundaries (`ExecutionContext.executionHook`).
- **[CodeCoverageHook](CodeCoverageHook.kt)** — coverage with two views:
  - **`lineHitsSnapshot()`** — file → line → total hits (editor gutter / line heatmap).
  - **`statementHitsSnapshot()`** / **`statementHitsForFile(file)`** — per compiled command (`BaseCommand.coverageStatementId`) with [SourceCoverageSpan](CoverageSnapshots.kt) for sub-line decorations (e.g. modifiers). Use [SourceCoverageSpan.toVsCodeRange](CoverageSnapshots.kt) for VS Code `Range` (0-based indices).
- **[CoverageStatementIdAssigner](../commands/CoverageStatementIdAssigner.kt)** — runs once per `TransformModule` when a hook sets `IExecutionHook.preparesStatementIds` (e.g. `CodeCoverageHook`), not on every compile.
- **DAP** — the `isl-debug-adapter` module uses `IslDebugHook`, an `IExecutionHook` for breakpoints and stepping.
