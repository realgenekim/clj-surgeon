# rf2 re-review at 5ccb4f0 — GO-WITH-FIX; items 1, 2, 3, 7 before the queue (2026-09-03T03:26Z)

Prior eight: 1 CLOSED (escape → typed refusal, outside file untouched), 2 CLOSED (argv `:command`; pasted
`:command_shell` created no PWNED; `shell-quote-token` byte-correct on 11 hostile tokens), 3 CLOSED,
4 PARTIAL (compile-alias walk bounded; `load-project-aliases` one-arity walk still reads ancestor config and
SCI-evaluates `compile-field` forms — pre-existing, documented, no subprocess reach), 5 CLOSED on the probe /
PARTIAL in class, 6 CLOSED, 7 CLOSED in substance, 8 CLOSED both modes. `resolve-discovered-source-path`
strictly tighter than `file-seq`; a symlinked file with an outside target is refused.

| # | sev | new hole (witnessed) | fix |
|---|---|---|---|
| 1 | MED | skip-list names matched by bare `.getName` anywhere under src: `app.out.writer` silently dropped, receipt `:complete true` | match only at the root; report skipped dirs in `:discovery` |
| 2 | MED | a 700 KB caller skipped, left broken, receipt `:complete true :callers-unresolved []`; `max-workspace-file-bytes` private, no override | `:complete` false/qualified on any skip; override or state none |
| 3 | MED | cap remedy broken: `:max-workspace-files` rejected on `:extract`, and `(long "3000")` throws on `:extract!` → `:extraction-snapshot-failed`; spec `[x]` on a function-level fixture | both arg maps; coerce with typed refusal; witness via the CLI |
| 4 | MED usability | out-of-root dir symlink anywhere → EVERY extraction refuses, incl. dry run and `:rewire-callers false`, even a link to an empty dir or to `target`; refusal mislabelled `invalid-relative-source-path` | skip-name before link branch; escape only when a `.clj` is reached through the link; name the escape |
| 5 | MED/LOW | "Could not locate … on classpath" naming a touched ns → `:unverified` instead of `:ok false`; a failure naming no file → `{:ok false}` contrary to the docstring | attribute self-inflicted misses; route no-file to `:unverified` |
| 6 | LOW | symlinked file inside the root counted twice and replaced by a regular file | dedup on the canonical path |
| 7 | MED (posture) | `:to /outside/pwn.clj` applied, wrote outside the derived root, stamped an illegal ns `.home.forge…` into the require | validate ns legality; decide and state `:to` confinement |
| 8 | file | `load-project-aliases` walk; `apply-command-for` and the undo `:command` still interpolate unquoted | document / quote later |

Round three launched on `~/src/clj-surgeon-rf2`; the oracle now runs on this box (swipl installed).
