# Prelaunch cross-user write-geometry addendum

Frozen before any measured Codex process.

The first Anvil zero-model preflight proved that both production tools were
advertised and inspection succeeded, but `edit_clojure` refused atomically with
`transaction-write-failed` / `Permission denied`. The shared MCP service runs as
the dedicated `surgeon` account, while the disposable fixture clones were mode
755/644 and owned by `dev-a`. Native patch succeeded, so the original geometry
made only one route executable and correctly failed the preregistered gate.

The sole repair is in `reset_workspace`: after copying each synthetic fixture
and before initializing its private Git repository, set that disposable clone's
directories to 0777 and files to 0666. This changes no model-visible byte,
prompt, tool schema, result, payload, fixture content, schedule, prediction,
validity field, scorer, decision threshold, or retention rule. It grants the
two already-declared execution identities equal write access only inside each
fresh synthetic clone. No shared installation, service, or source tree is
changed or reloaded.

The superseded freeze is retained as `freeze-original.json`. A new freeze must
hash this addendum, the original freeze, and the repaired harness. The complete
U/P production preflight, native-patch preflight, exact-byte oracle, distractor
check, and independent verifier must all pass in a new remote path before the
first measured process. The failed zero-model preflight remains retained and
is not an experimental attempt.
