# Hot-Verification Response-Stream Design

`clj-surgeon.mcp-hot-verify/verify!` sends one `eval` to the application nREPL
and reports the focused test summary it evaluates in that JVM.

Measured 2026-09-06 (inb-adcc9e), a HANG REPAIR on this one call -- not a suite
timing and not a comparison against a native editing route: every hot verification blocked for the whole
configured `:timeout-ms` — 61.1 s at 60000, 3.08 s at 3000, 0.79 s at 700 —
while the evaluation itself took roughly 0.8 s. The cause was reading the
response stream with `(doall (client msg))`. `nrepl.core/client` returns a lazy
seq that ends only when the transport closes or a single `recv` exceeds the
response timeout; it does not end at the message's `done` status. `doall`
therefore always paid the ceiling.

The reader now:

1. sends the `eval` with an explicit `:id` and reads with
   `nrepl.transport/recv` directly;
2. keeps responses carrying that `:id` and stops at the first one whose status
   contains a terminal status (`done`, `error`, `eval-error`, `interrupted`),
   so an error the server never follows with `done` also ends the read;
3. bounds every blocking read by the time remaining against one deadline
   computed when the read starts, making `:timeout-ms` a ceiling on the whole
   read rather than on each response;
4. returns a typed `hot-verification-timeout` when the ceiling passes with no
   terminal status, and a typed `hot-verification-transport-closed` when the
   transport disconnects first.

The profile shape, the validator, and every reported field (`summary`, `pid`,
`cwd`, `reload-count`, `law-count`, `output`, `elapsed_ms`) are unchanged.

Requirements: `hot-verification-specs.md`.
