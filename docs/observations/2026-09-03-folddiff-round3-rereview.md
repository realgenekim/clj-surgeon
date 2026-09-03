# curtain-call fold-diff-tool 2b56a484 — Sol executed round-3 re-check: GO-WITH-FIX for the mayor's exact production read (6/6 prior CLOSED; 3 residuals → round 4)

GO-WITH-FIX for the mayor’s exact production read—not yet a clean approval of the tool as a general-purpose gate.

Run `bin/fold-diff-checkpoint` directly with `FOLD_DIFF_REDACT_RELATIONS` unset or explicitly `sessions,api-keys`. Treat only exits 0/1 as a verdict; exit 2 means nothing was compared.

1. CLOSED — original `origin/main` compilation blocker. [bin/fold-diff-checkpoint:213](bin/fold-diff-checkpoint:213) copies `store_checkpoint.clj`; [line 264](bin/fold-diff-checkpoint:264) remaps unexpected emitter exits. My four-case run produced 2/refused, 0/vacuous-identical, 1/differences with digests `4b86153b…7b4d` versus `5293fb7b…5fa0`, and 3/`FAILED :baseline-emit-failed`.

2. CLOSED — greedy JDBC userinfo redaction. [db.clj:198](src/cfp_scheduler_killer/db.clj:198) redacts through the last `@`. My matrix produced:

   - `cfpuser:p@ssw0rd@host` → `REDACTED@host`
   - `a/b/c@host` → `REDACTED@host`
   - no userinfo → unchanged
   - IPv6 host, with or without userinfo → host preserved, credentials removed

   Focused suite: 6 tests, 18 assertions, zero failures.

3. CLOSED — deployed mismatch travels with the report. [bin/fold-diff-checkpoint:153](bin/fold-diff-checkpoint:153) builds the note and [fold_diff.clj:540](src/cfp_scheduler_killer/fold_diff.clj:540) renders it beside the baseline. My real driver run printed `DEPLOYED_REVISION is NOT the baseline` inside the report between baseline and candidate refs.

4. CLOSED — write guard hardening. [fold_diff.clj:358](src/cfp_scheduler_killer/fold_diff.clj:358) contains all nine vars. The live receipt named `store/append!`, `store/append-all!`, checkpoint write, Postgres append, `db/migrate!`, `db/start!`, `store-pg/start!`, `ensure-schema!`, and `ensure-idempotency-index!`. Focused suite passed 43 tests/165 assertions.

5. CLOSED — default sensitive-value rendering and alternate-render leak audit. [fold_diff.clj:495](src/cfp_scheduler_killer/fold_diff.clj:495) emits only length and digest; [line 575](src/cfp_scheduler_killer/fold_diff.clj:575) is the only difference-value branch. The sessions fixture retained `[:sessions "sess-redact-1" :token]` and counts while rendering `<redacted: 30 chars, sha256 5fb0fa9879c9…>` and `<redacted: 27 chars, sha256 24b6729ea0e0…>`. There is no separate “changed from → to” renderer, and frontier/gap lines print only indices and counts.

6. CLOSED — Make exit collapse. [Makefile:318](Makefile:318) and [docs/fold-diff.md:20](docs/fold-diff.md:20) document it. Direct `bin/` runs preserved 0/1/2/3; forcing bin exit 3 through `make fold-diff-checkpoint` returned make exit 2 while reporting `Error 3`.

7. OPEN — `store_checkpoint.clj` is silently candidate-owned. [bin/fold-diff-checkpoint:228](bin/fold-diff-checkpoint:228) overwrites both refs’ copies with the candidate file, contradicting the unconditional claim at [registry.edn:765](docs/intent/registry.edn:765). It can change checkpoint acceptance, event parsing, and therefore which events reach both folds—potentially hiding or exposing a fold difference. For the exact `origin/main`→`2b56a484` pair, the diff shows only the semantics-preserving extraction of `validate`, so this production run remains usable. The general contract needs to make this helper explicitly tool-owned and digested/reported, or retain each ref’s behavior.

8. OPEN — exit-2 refusals are not explicitly phase-labelled or named in CLI output. The refusal kind exists in memory at [fold_diff.clj:206](src/cfp_scheduler_killer/fold_diff.clj:206), but [line 862](src/cfp_scheduler_killer/fold_diff.clj:862) prints only `:message`. The driver then propagates emitter exit 2 at [bin/fold-diff-checkpoint:264](bin/fold-diff-checkpoint:264). My invalid-checkpoint emitter run exited 2 without `REFUSED :checkpoint-invalid`; a candidate gap also exited 2 without `REFUSED :log-past-checkpoint`. One can infer the phase only from whether a preceding `wrote … baseline.edn` appeared. Also, [line 317](bin/fold-diff-checkpoint:317) calls a candidate-diff crash `:candidate-emit-failed`.

9. PARTIAL — `FOLD_DIFF_REDACT_RELATIONS` parsing. [fold_diff.clj:803](src/cfp_scheduler_killer/fold_diff.clj:803) safely trims spaces, ignores empty comma fields, and restores defaults for unset/blank input. My seven-case parser probe passed. However, unknown names are accepted as keywords at [line 816](src/cfp_scheduler_killer/fold_diff.clj:816), and the set replaces rather than extends the defaults. Thus `unknown` silently disables both sensitive defaults and can expose values. Unknowns should fail closed, or mandatory sensitive relations should always be unioned in.

10. CLOSED — registry fixup. [registry.edn:758](docs/intent/registry.edn:758) now names three real FOLD-DIFF-009 deftests. The source-discovery architecture suite passed 2 tests/580 assertions, unique-definition suite 1/150, and active-intent traceability 1/120. Every current `FOLD-DIFF-0xx :tests` entry resolves to exactly one real tagged `deftest`.

11. CLOSED — requested gates. `bin/kaocha unit`: 1,089 tests, 13,314 assertions, zero failures. `make compile-check`: passed under `-Xmx1g`. All JVM work ran under `suite-run`, with `STORE_BACKEND` unset.

The worktree remains unchanged apart from the two pre-existing `.codex/config.toml` status entries. Temporary evidence remains under `/home/forge/tmp/fd-sol/`; the temporary `data` symlink was removed.