# Astra consultation: LID inside `safe-refactor`

## Verdict

Fable is directionally right: require LID as a **conditional gate in step 2**, after the observable contract is recovered in step 1 and before code moves. Do not merge the skills. `safe-refactor` should invoke `linked-intent-testing` by name and leave its registry, tagging, five-step procedure, and ratchet ladder authoritative there.

The case for the gate is strong. Existing suites proved both refactors at one snapshot, but they did not preserve why the new shared seams exist. A future maintainer can inline one caller, narrow blank-value fallback, or change JSON options while rewriting the same tests; everything may remain green and the promise evaporates. A stable intent row plus independently linked witness makes that loss loud. Step 2 is the exact point because LID changes what must be proved before movement, not how the movement is executed.

The case against a universal gate is also strong. LID explicitly forbids ceremony for changes whose complete behavior fits in ordinary examples, and traceability proves that intent is witnessed—not that it is correct. A row per extraction or helper would freeze implementation topology, tax mechanical moves, and invite empty misreadings. Source scans are especially dangerous as the default: they prove spellings and ownership, not caller-visible behavior. Typed refusals are appropriate only when the invalid state can actually be excluded by construction; adding one merely to reach rung e changes the contract.

My commitment is therefore **yes, as a classification gate, not as an unconditional document gate**. Rungs a and b remain the baseline for a load-bearing promise; c, d, or e are added only when their conditions fit. An existing test may be linked only if its literal assertions independently witness the named promise. “The suite covers this somewhere” is not enough.

## Exact replacement text for workflow step 2

> 2. Before moving code, classify the recovered contract. If the refactor re-expresses a load-bearing promise that a later edit could silently narrow while existing tests stay green, run `linked-intent-testing`’s five steps, link an independent boundary witness, and take the highest fitting ratchet rung. A source scan may witness an architecture promise; it never substitutes for observable behavior. If the change is only semantics-preserving relocation—with no new or consolidated authority, policy, boundary, refusal, or behavior—record `LID: skipped — pure move; no promise added or changed`, then preserve the motivating real-program fixture. Failure to name the promise is a stop, not a skip.

(101 words; ready to replace the current step 2. Steps 1 and 3–6 stay unchanged.)

## Skip rule

The skip is earned only by an affirmative diff-level claim: the change relocates or renames code while preserving public inputs, outputs, failures, side effects, authority, and all boundary ownership; it neither consolidates competing copies into a new source of truth nor creates a policy seam. The complete behavior must already fit in ordinary direct examples, and a future narrowing could not plausibly stay green by editing those examples alongside the code. Record the one-line reason in the refactor record. Inability to articulate a promise is missing discovery, not proof of purity. By this rule, neither refactor supplied by Gene qualifies for a skip.

## The two refactors

**marvin-voice-remote.** Parse and write are two atomic promises; combining them would violate the EARS “no `and` outcome” rule. I would append `{:id :JSON-PARSE-001 :status :active :ears "When the application decodes an inbound JSON object, it shall expose object keys as Clojure keywords." :misreadings ["Only selected endpoints need keyword keys." "Moving parsing behind a helper permits default string keys."] :boundaries ["nested objects" "non-object JSON values" "malformed JSON remains a parse failure"] :tests [:shared-json-policy-is-observable] :rationale "The 2026-09-07 extraction centralized nine inbound-body parse sites; the existing suites did not name this policy."}` and `{:id :JSON-WRITE-001 :status :active :ears "When the application serializes JSON, it shall preserve clojure.data.json encoding semantics plus every explicit per-call option." :misreadings ["Helper defaults may override caller options." "Selected emitters may bypass the shared policy without consequence."] :boundaries ["keyword and string keys" ":escape-slash false" "JSONL framing remains caller-owned"] :tests [:shared-json-policy-is-observable] :rationale "The 2026-09-07 extraction routed 21 writes through one policy seam without a named witness."}`. The rung-b witness should use hand-written literals to show nested keywordization and exact option pass-through, with both `INTENT-TEST` tags; a separate narrow architecture assertion may scan production source to allow direct `read-str`/`write-str` only in the helper. Tag `parse` and `write` separately. The source scan proves routing, while the literal examples prove behavior; neither alone is sufficient. No rung-e refusal is justified by this refactor.

**curtaincall-cfp.** I would append `{:id :SPK-006 :status :active :ears "While a submitted speaker block may contain stale identity data, when portal, letter, or file views resolve it, the application shall choose each identity field by precedence: nonblank canonical person value, then submitted value." :misreadings ["Overwrite a submitted value with blank canonical data." "Use current identity on only some of the three surfaces." "Route exports through this helper although exports has distinct presence-aware semantics."] :boundaries ["missing person" "canonical name only" "canonical email only" "exports.clj remains separate"] :tests [:shared-current-speaker-identity-precedence-test] :pins "The 2026-09-07 refactor replaced three private copies; green legacy suites did not preserve the shared precedence rule."}`. The rung-b witness should be a table of literal speaker/person/expected maps covering missing person, blank canonical fields, and partial/full overlay. Add a narrow source-topology assertion that exactly portal, inform, and handlers/files use the shared helper while exports retains its deliberately different copy. Put the code tag on the shared policy entry point and the test tag immediately above the named test. This is an identity promise, so LID is required; no typed refusal naturally fits.

## Coupling anti-patterns to reject

- “An intent per refactor/helper” instead of one per observable promise.
- Treating “could not name a promise” as permission to skip.
- Registering internal topology (“must call helper X”) as the behavioral EARS outcome.
- Using a source scan as the sole witness, or retroactively tagging a broad green suite whose assertions do not prove the row.
- Combining parse and write into one EARS outcome.
- Adding a refusal solely to claim rung e, or keeping a Prolog rung that found no missed counterexample.
- Copying LID’s five steps or registry schema into `safe-refactor`, allowing the two texts to drift.
- Putting the traceability check outside the normal suite.

The clean coupling is one dependency edge: `safe-refactor` decides whether a promise gate fires; `linked-intent-testing` remains the sole authority on how that promise is registered and ratcheted.

— Astra, OpenAI GPT-5 (Codex), 2026-09-07T16:35:10Z UTC
