# Production CLI routing and developer MCP guard

## Intent

The installed Babashka CLI is the supported production entrance. Complex
requests must travel as EDN data through `:spec-file -`, so shell quoting does
not become a second request language. The persistent MCP entrance remains an
explicit development experiment.

## Observable requirements

- `clj-surgeon up --help` identifies the command as development-only and names
  `--force`.
- `clj-surgeon up` without `--force` returns a bounded `:development-only`
  refusal with an executable `:next_call` and performs no onboarding.
- `clj-surgeon up WORKSPACE --force` and `clj-surgeon up --force WORKSPACE`
  accept the same workspace.
- Global help shows `:spec-file -` as the structured-input route.
- Skills and README route production callers to the CLI, record timing
  conditions, and identify native tools as the floor for small edits.

## Witnesses

`test/clj_surgeon/cli_dispatch_test.clj` covers pure argument parsing, help,
refusal shape, and subprocess help. The CLI and skill mirror checks cover the
published entrances. The safe-refactor skill records the same route for
cross-file work.
