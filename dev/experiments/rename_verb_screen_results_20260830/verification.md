# Verification receipt

All commands ran from the frozen experiment worktree at harness commit
`15c3acbe99f38bfb8d1ce242f8450b8a137de7c5` unless noted.

- `bench/run_rename_verb_screen.sh --self-test`: PASS. Six proxy/fixture tests,
  39 Clojure assertions; three scorer falsifier tests.
- V and T zero-model Codex MCP registry preflights: PASS. Each projection
  exposed exactly one `edit_clojure` tool and the expected arm schema.
- Standard Clojure Style 0.24.0: formatted all six new Clojure and fixture
  files before tests.
- Paved clj-kondo entrance: zero errors. One combined before/after invocation
  reported seven expected duplicate-namespace warnings because both frozen
  versions were linted in the same JVM. Later separate lint attempts were
  pressure-deferred by the repository load governor.
- `bash -n bench/run_rename_verb_screen.sh`: PASS.
- Python 3.12 compile plus unittest: PASS.
- Committed top-level `SHA256SUMS`: PASS.
- Extracted `raw-streams.tgz` internal `SHA256SUMS`: PASS across 269 files.
- `make test`: the fast layer passed 647 tests / 5,562 assertions and the
  analyzer layer passed 4 tests / 20 assertions. The MCP layer ran 300 tests /
  3,433 assertions with two failures in the existing
  `cold-clj-kondo-admission-timeout-is-unverified` test: it expected
  `:admission-timeout` but observed `:delegated`. A standalone rerun of that
  exact Var passed one test / seven assertions. `make mcp-test` then reproduced
  the same two full-suite-only assertions. There are no experiment changes in
  `src/` or `test/` relative to product source `c55de227`.

The full-suite timeout-classification flake does not touch the experiment
proxy, fixture, scorer, raw results, or KILL decision. It is retained rather
than hidden.

