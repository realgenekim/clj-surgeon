# Gene report — the two-hour aperture window (2026-09-05, ≈18:35–20:35Z, Fable + Astra in charge)

## 1. Measured, versus what came before

| what | before | after | n | status |
|---|---|---|---|---|
| Sol as a real caller of the mission ledger, six-helper task | 24 returns, 6m11s, never verified (probe 1, first build) | **11 returns, 2m20.8s, verified + undo byte-identical** (probe 2, fixed build) | 1 + 1 | measured, same caller, same task |
| Astra as a caller (spec in hand) | — | 9.9 s propose→verified on a one-helper scratch (kernel 22.6 ms) | 1 | measured |
| mission read verbs (show / ready / list / help) | 6.7 / 8.4 / 6.7 / ~5 s (cold JVM) | **0.091 / 0.029 / 0.030 / 0.04 s** (bb) | 3 runs each | measured |
| mission write verbs (open+plan / apply / undo) | — | 5.7 / 6.0 / 6.0 s cold JVM; 0.10 / 0.32 / 0.013 s in-process | 1 run each | measured (proof profile /bin/true = floor) |
| stale-resume | — | one changed owner file → refusal before staging, nothing written | witnessed | measured |
| thread-parameter planner (Astra idea #1), Sol typing under a proof gate | — | built in 11.2 min; 6 tests / 49 assertions / 0 failures; builder tabletop: native 5 returns vs planner 2 | 1 | built; caller-measured: no |
| Spark as typist, three-way race | — | VOID: all three arms hit the Spark per-model limit mid-task; 0 gated results | 3 | untested, not refuted |
| vs native, whole task | none measured (oracle over-constraint closed the cohort) | none measured | 0 | unchanged; preregistered |

Plainly: no new number against native exists. What exists is a measured usability delta on the same caller and task (24 → 11 returns; never → 2m21s) and a measured read-latency delta (seconds → tens of milliseconds) on the object both of us said we would love using.

## 2. Wins and losses

Wins
- **The mission ledger exists and works end to end** (bridge/mission-ledger, six rounds, 20 witnesses / 283 assertions): open / plan / show / apply / resume, plain-EDN state readable with cat, stale-resume refusing on a moved tree, native adjudication with a stated reason, dependency links with cycle refusal, a dependency release that invalidates a stale dossier, proof authority stored at plan time, repair in place via supersedes, help with runnable examples, honest exit codes, bb-routed reads. Astra: "The feel is already closer to Beads than to an editor: the useful unit is M-1, not the individual edit."
- **Four independent perspectives converged** (Astra, Sol, Terra, Opus) on the same shape — an intent compiler returning terminal proof, with a review instrument in front — and on beads as the usability bar; the twenty-idea list and the bitter-lesson argument are written, argued from both sides, and on trunk.
- **A second cheap prototype in eleven minutes** (thread-parameter) shows the fast-typist arrangement works when the typist is Sol and the gate is the judge.
- **Landed on trunk during the window** (all gated): Astra's telemetry emission for the ninth tool, his production CLI entrance and analyzer opt-in, all riffs and studies.

Losses
- **Spark race void on a usage limit**, and the brief was wrong for the model: Spark's own instructions say "prefer mistakes over over-exploring, do not run tests"; we asked it to read six files and run a JVM gate. The typist hypothesis stays untested tonight.
- **My tooling still cost real time:** an audit-check race in `land` refused a docs-only landing twice; the shared seat checkout carried another agent's unreviewed commits to trunk once (reverted within minutes); usage-watch pushed a stale ref twice. Each is ratcheted (records worktree with guards; docs-only landing rule; captured audit output; `fence-run`).
- **Sol's first probe was a usability failure I should have predicted:** the closed request shape was undiscoverable and the profile config was never read — both named by the perspectives an hour earlier.

## 3. Learnings → ratchets
- A prototype's feel is measured by a caller who did not write it: probe 1 vs probe 2 is the method (same model, same task, before/after). Kept as the acceptance for any entrance change.
- Reads must never pay a JVM start: the pure core is Babashka-safe by construction; the launcher routes read verbs to bb (witnessed byte-identical to the JVM path).
- A refusal that names a field without showing the shape costs seven guesses; every request-shape refusal carries a runnable `:example` (production change in flight).
- Config is found where the caller put it, and `show` says where it looked.
- Spark is a synchronous typist: dossier in, patch out, effort low, gate outside, N-way.
- Never push from a checkout another agent can switch; servers and records live in their own worktrees.

## 4. What is next
- Land the mission ledger? Not yet: it is a prototype on a branch; the pass condition Astra and I agreed (≥2 returns removed on a fan-out task without lowering acceptance, native still winning the one-site square) needs a preregistered run with a fresh caller, plus the boundary `:example` change.
- Spark three-way (Astra's design: native apply_patch vs Spark emits the mission request vs Spark emits a native patch) when the Spark limit resets.
- The performance question is unchanged: a fresh preregistered epoch with six accepted native controls before any speed claim.

Headroom, count-first: Astra's Codex meter 10% weekly at 14:35Z (not re-read since; Spark per-model limit exhausted until its own reset); this seat's Claude meter not readable from inside the session.

## Addendum 20:21Z — the boundary :example change landed
The production change built from Sol's probe (every request-shape refusal carries :field, :decision and a runnable :example) landed on MCP/main as 13c12401 through Sol fence GO, the pane agent's confirm, and the full landing gate. It is the one item from the window that is on trunk as product.
