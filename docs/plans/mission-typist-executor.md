# Mission typist executor — flagged prototype

Owner: Astra. Deadline: 2026-09-06 09:31Z; checkpoints 03:00Z, 06:00Z, 09:00Z.
Base: bridge/mission-ledger f2efc87c. Gene authorized the seven-piece experiment;
Fable owns the real-repository mission, narrow gate and independent witness.

## Observable contract

An explicit typist executor request is decided during planning and persisted with
the mission's frozen dossier and proof authority. Ordinary missions retain their
current executor. A typist sees source fragments, owner identities, exact allowed
spans and the intended change, never provider keys or authority to expand scope.
The route is refused before network or write when any admission fact is missing.

Eligibility requires complete discovery; a mechanical mission class; a measured
gate below 5000 ms with named receipt evidence; a distinct acceptance witness;
bounded named files/spans and change budget; supported plain Clojure source; and
a guarded commit/rollback authority. Generated, reader-conditional and declared
format-sensitive inputs refuse. Missing proof or rate evidence is not zero cost
or a guessed success rate. Rates bind to a mission class and a pinned provider.
Candidate count: >=85% verified gives 1, <=70% gives 5, otherwise 3.

Primary provider is OpenRouter openai/gpt-oss-120b pinned to Cerebras with upstream
fallback disabled. An explicit bounded typed provider refusal may activate Groq;
record both attempts and actual provider/model. Spark is a separately selected
route for reliability. Runtime keys come from configured EDN files, never env,
prompts, receipts or model-authored settings. Secret errors are replaced with
typed redacted diagnostics. No automatic unpinned provider selection.

## Seven implementation checkpoints

1. Pure admission and frozen dossier projection from planner-owned spans; literal
   tests for every refusal and rate boundary. Receipt names the failed condition.
2. Flagged executor inside mission apply; route/k/provider/proof carried from plan.
   First fake-candidate end-to-end fixture goes through this path before paid calls.
3. Pure context-anchored candidate compilation, then existing guarded transaction
   machinery. Model output edits scratch candidates only; live source is written
   only after independent proof and stale-source recheck. Reject duplicate blocks,
   ambiguous anchors, escapes, overlap, budget breach and unauthorized changes.
4. Fable's real-repository gate, including measured cost and command authority.
5. Independent acceptance outside candidate write authority; behavioral checks plus
   byte/mode identity outside the allowed spans/files. Corrupt witnesses must fail.
6. Runtime key loading and bounded pinned provider transport. Full retained usage,
   reasoning-token count, typed refusal and terminal candidate state; secrets absent.
7. Real-repository and resident-session A/B, fence review, repository landing gates.
   No self-landing, no production-ready claim before these pass.

Each checkpoint is independently committed with its verification command. Scoped
kernel/API changes follow the repository's linked-intent workflow when reached.
Early pure modules and prototype CLI seams are outside that scoped path set.

## Initial behavior matrix and ratchets

Admission: disabled flag; unknown class; incomplete/empty dossier; missing owners;
invalid path/span; unknown source flags; generated/CLJC/format-sensitive source;
missing, negative or slow gate cost; missing/shared acceptance authority; absent
atomicity; invalid counts or class/provider mismatch; rate boundaries 70/85;
missing scope budget; valid route and frozen evidence.

Candidate compilation: unique anchor success; missing/ambiguous anchor; duplicate
file block (field regression: prototype silently discards earlier block); alias
paths; same-file disjoint hunks; overlap; blank/empty change; added/removed files
only if planned; stale snapshot; budget; parse failure; unchanged protected bytes.

Executor: no network on refusal; fake good/bad candidates; all rejected; gate pass
but independent acceptance fail; provider length/refusal/wrong upstream/model;
redacted transport errors; fallback counted; apply failure and rollback; interrupted
apply/resume; guarded undo; receipts that describe what actually happened.

## Experiments and stopping rule

Preregister prediction, falsifier, exact prompts/fixture/proof and lifecycle before
each A/B. Reserve a quiet window, use slot -t, sample load, retain all failures.
Compare whole task with the same independent acceptance. Include deterministic
Surgeon when its planner already computes the entire edit: adding a typist there
may lose, and that loss is informative. Separate a supplied-dossier author test
from problem-to-done timing, and cold from resident-session results.

First prediction: a known complete one-file fixture can reach a verified mission
through the flagged executor; performance is unknown until startup, dossier,
provider, candidate proof and commit are all charged. A real JVM proof above the
route's budget refuses admission rather than becoming a hidden slow exception.
If the executor cannot beat native, keep reusable dossier/proof primitives and
report the lost route. Do not scale candidate count to conceal a bad experiment.
