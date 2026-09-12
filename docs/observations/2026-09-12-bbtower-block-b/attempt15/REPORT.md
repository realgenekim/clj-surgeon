# Block B attempt 15: PASS

The requested repair and first prewarm pass at source commit
`0d92fe92c679cff0341ef81a75dccf686f4e19e8`. Prewarm ran from
2026-09-12T13:00:31.542098977Z to 2026-09-12T13:06:56.366781727Z;
these timestamps come from [gate-receipt.edn](gate-receipt.edn).
Its actual values are `:state :passed`, `:problems []`, `:prewarm? true`,
and `:landing? false`. All seven stages and the shell-check node exited zero.
There was one `make test-fast` and one `make landing-gate-prewarm`; no gate
repair or final retry was needed. No budget, membership, runtime, or skip changed.

## Findings and repair

The [Landlock wrapper](restricted-oracle.py) made every path outside a fresh
writable TMPDIR read-only for the oracle and its children, including the real
`/var/tmp/forge` parent and `/tmp`. It changed no shared directory permissions.
The unmodified oracle reproduced exactly the three reported PermissionErrors:
[oracle-red.log](oracle-red.log), 20 tests, three errors, two existing skips.
The retained fourth packet evidence is in [prewarm-checkonly-run4/](prewarm-checkonly-run4/).

Two errors were mkdir attempts under `/var/tmp/forge`. The 120-byte test instead
failed at the actual socket bind after production selected `/tmp/csg-<uid>`.
Changing only the long TMPDIR literal would not fix that third error.
The repaired test builds its exactly 120 UTF-8 byte path under a disk-policy
TMPDIR scratch, then substitutes a private writable short-root location for
the real bind. Pure tests still cover the production short-root spelling and
selection. Kernel bind, occupied-slot refusal, the byte budget, directory mode,
and cleanup remain exercised. A policy table additionally covers empty/unset,
RAM-root, disk-root, and prefix-boundary inputs.

An [intermediate run](oracle-intermediate.log) caught a test namespace that
made the private-root slot exceed 100 bytes. A UUID namespace inside that
already-private root corrected it. The [same Landlock harness then passed](oracle-green.log):
21 tests, zero failures/errors, the same two platform skips.
The over-budget witness also now restores the inherited backend environment.

The three mission tests use `with-temp-dir`, respecting the runner's
TMPDIR-derived `java.io.tmpdir` and deleting the scratch in `finally`.
Process timeout and identity filtering use real cwd paths. Inspection showed
the ledger invalid-options test refuses before I/O; its named workspace was
nevertheless converted as requested. Three changed JVM witnesses passed with
11 assertions ([mission-jvm.log](mission-jvm.log)). A preliminary bare bb load
lacked nrepl on its classpath ([mission-bb-load-refusal.log](mission-bb-load-refusal.log));
it ran no tests, and did not lead to a runtime classification change.

The [literal census](literal-census.md) accounts for all 54 matching lines at
the starting commit and all occurrences on each line. The current scan has 48
matching lines. There is no `lib/`; the actual `libs/` tree has zero matches.
Retained negative paths, policy expectations, documentation, and configurable
battery/experiment defaults are individually explained. This is not a claim
that a full battery can use those defaults inside the packet envelope.

## Verification

| Check | Observed result | Evidence |
|---|---|---|
| Fresh empty TMPDIR oracle | 21 tests; zero errors/failures; two existing skips; scratch empty | [oracle-fresh.log](oracle-fresh.log) |
| `/var/tmp/forge/gate-*` entries | Identical before/after; no new entries | [before](gate-entries-before.txt), [after](gate-entries-after.txt) |
| Formatter | All three Clojure files formatted | [format.log](format.log) |
| Lint | Zero errors/warnings with existing macro's binding semantics supplied via `:lint-as` | [lint-configured.log](lint-configured.log) |
| `make test-fast` | Exit 0; 94 namespaces; zero isolation violations; fast sum 43,906 / 60,000 ms; makespan 24,584 ms | [test-fast.log](test-fast.log) |
| Prewarm alias suite | Exit 0; sum 81,517 ms; makespan 89,324 ms | [gate.md](gate.md) |
| Prewarm MCP suite | Exit 0; fast sum 45,181 / 60,000 ms; integration sum 66,360 / 240,000 ms; makespan 89,391 ms | [gate.md](gate.md) |
| Prewarm bb suite | Exit 0; serial-equivalent total 239,633 ms; bb runtime sum 233,916 / 343,102 ms; makespan 85,216 ms | [gate.md](gate.md) |
| Gate-slot inside prewarm | 21 tests; zero errors; two existing skips | [shell stderr](prewarm-run/lane-0.err), [command](prewarm-run/lane-0.out) |
| Complete prewarm | Exit 0; 384,824 ms; no problems | [gate-receipt.edn](gate-receipt.edn) |

`gate.md` is the verbatim complete prewarm stdout/stderr capture, checked with
`cmp` against `/var/tmp/forge/bbtower-fx/attempt15-prewarm.log`. It includes every
emitted budget line. The full [prewarm-run/](prewarm-run/) directory retains
per-worker output, checks, and receipts, including shell prerequisite output.
No distinct refusal-census summary line was emitted; none is invented here.
The source diff passes `git diff --check`. The evidence-only diff reports
whitespace already present in raw command output; those bytes are intentionally
preserved ([evidence-whitespace.log](evidence-whitespace.log)).
The initial unconfigured lint treated the existing `with-temp-dir` macro as
an ordinary function and reported three unresolved bindings ([lint.log](lint.log));
the configured lint tells it the macro's established binding semantics.

## DOGFOOD

| Source edit | Intent | Mechanism | Refusal/error | Did repair text suffice? |
|---|---|---|---|---|
| `test/oracles/test_gate_slot.py` | TMPDIR policy, byte-exact private bind fixture, cleanup and environment restoration | Native `apply_patch`; real oracle under Landlock and inside prewarm | Intermediate test-only slot exceeded budget | Yes: typed refusal named path and 100-byte cap; shorten UUID namespace in private root |
| `test/clj_surgeon/mission_git_process_test.clj` | Owned cwd for timeout boundary | Exact-form native `apply_patch`, then installed `standard-clj` | Initial batched native patch had an accidental empty trailing hunk and was rejected before application | Yes: remove the empty hunk; repaired batch applied |
| `test/clj_surgeon/mission_git_identity_test.clj` | Owned cwd encoded with `pr-str` in child expression | Same native batch and formatter | Same batch rejection; no Surgeon refusal | Yes, same repair |
| `test/clj_surgeon/mission_git_ledger_test.clj` | Owned workspace in invalid-options witness | Exact-form native `apply_patch`, then formatter | None | N/A |
| `attempt15/restricted-oracle.py` | Reproducible filesystem-write envelope | New-file native `apply_patch`; executed red and green | None; Python emits a ctypes layout deprecation warning | N/A |

Native patches are the working-tree routing skill's default for these known
ordinary edits. No routed class was invoked or admitted, so Surgeon fallback
and performance telemetry are not applicable. Lint used `~/bin/clj-kondo`.

## Commits, limits, and owed

- Source: `0d92fe92c679cff0341ef81a75dccf686f4e19e8` on `bb-rewrite-tower-local`,
  based on `2f7b4cf8`. The commit containing this report adds only attempt15
  evidence and is deliberately last. The source tested by prewarm is unchanged.
- No push, merge, tag, shared installation change, or external message was made.
- Least sure: the real-bind fixture requires enough room under its TMPDIR for
  a short socket root and a 120-byte path; it asserts this explicitly. It does
  not silently escape the writable envelope if supplied an excessively long
  TMPDIR. Native macOS was not exercised on this Linux box.
- Disagreement with the incident description: the 120-byte failure was a
  `/tmp` fallback bind, and ledger's invalid-options path does not perform I/O.
  Both are documented from source and the red trace, rather than assumed.
- Owed: Fable's independent review and any full landing/battery-fresh authority.
  The full prewarm here ran outside the packet as the binding brief requires;
  the focused oracle, not the full prewarm, was freshly tested under Landlock.
  Historical block measurements and previously owed certification are not
  re-claimed by this test-fixture repair.
