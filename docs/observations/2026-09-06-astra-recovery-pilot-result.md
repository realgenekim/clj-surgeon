# Astra: fresh caller chooses a patch gate, then native fallback

Recorded 2026-09-06T12:31:10.559277+00:00. Two prospective usability callers, not a replicated speedup experiment. Both completed the real-source-derived Maven 59-site change correctly. The preregistered ordinary-batch adoption goal failed.

| Caller | Complete wall | Actor subprocess | Outer actions | Actual mutation route |
|---|---:|---:|---:|---|
| Native N1 | 67.262 s | 60.838 s | 5 | One generated native patch |
| Surgeon encouraged D1 | 87.680 s | 81.451 s | 7 | One refused patch-gate call, then the same patch applied natively |

Complete wall covers pin checks, fresh copy, packet rendering, caller startup/actions, external gate, new witness, scope audit and cleanup. Initial global admission/import/preflight and final receipt serialization sit outside the individual row clocks. Preparation/review/overlapping queue through admission took **2,293.098 seconds (38.2 minutes)**. This is an elapsed interval, not exclusive operator active time; queue was not independently timed and cannot be added to it. Separate validation took 15.037 seconds. No ratio or generalized speed claim is earned by these two rows.

## What the caller actually did

D1 first assessed the tool catalog and read the installed Surgeon skill. It generated a 30,960-byte native patch from the supplied owner inventory, then sent it once to `admit_clojure_patch` with `mode=commit, verify=none`. The actual typed refusal was `verification-incomplete`, with `verification-not-requested`; the receipt explicitly says source unchanged, no mutation attempted and not committed. Service time was 577 ms, with a 591 ms observed MCP-call duration.

D1 then passed the **same stored patch** to native `apply_patch` and ran the supplied proofs. There were zero ordinary batch-edit calls and zero sites committed by Surgeon. This is native-only completion after a refused admission, not mixed migration or successful tool adoption. It neither attempted compact require migration nor used the earlier remove/restore-require workaround. The failure of the intended discovery route remains a result.

The response made the verification requirement visible. Do not weaken that requirement to produce an adoption statistic. The call also produced structural facts about the patch, but this pilot does not establish a quality advantage from them. The public catalog exposes `verify=focused`, and current source resolves focused profiles from `.clj-surgeon/focused-test.edn` and server configuration. We did not execute the counterfactual focused request; do not claim it would have accepted this fixture or automatically reused its supplied proof commands.

This caller's choice supports a narrower product hypothesis: make a gate fit the patch workflow the model already knows, with proof authority that is ready to use. It does not establish that another editing interface is wanted. That is an inference from one caller, not a routing ruling or an implementation request.

## Fairness and verification

The prospective order was N1 then D1, fresh actual `gpt-6-astra` at medium effort, one attempt each, no model substitution or retry. Both received the same roughly 3KB task packet, 20-file authorization, optional read access to the same owner inventory, and the same executable proofs. Both could batch native scripts. The tool caller was encouraged to assess Surgeon but could fall back. This is not unprompted free-choice adoption.

Unlike the old fanout-B cohort, this packet did not force compact relations and did not supply the 92KB expanded source dossier. It explicitly allowed the necessary missing require addition. These are several declared changes, so this pilot cannot isolate a refusal-text effect. The registered service stayed on the same observed process and catalog; source revision181c365c is recorded rebuild provenance, not a complete Clojure-heap attestation. D1's installed-skill read is retained in its transcript but was not separately pinned. Do not claim fully closed cold-context inputs.

A separately named witness fixes the old false-negative on require-clause layout. It preserves existing require syntax/comments and every byte outside that clause after authorized qualifier reversion, and permits any valid new alias. Static review, six actual driver mock tests, ten actual layout cases, three complete valid candidates, two expected negatives and the original gate preceded caller admission. The absent-helper baseline had71 failures plus one explicit missing-libspec error; negative tests were not described as green proofs. Old cohort files and verdicts were not changed.

Both caller outputs independently pass the original gate **58 tests/190 assertions** and new witness **19 tests/493 assertions**, zero failures/errors, with59 migrated sites across20 files. Source inventories, all1545 final frozen pins, file modes and empty cleanup were independently checked without rerunning the callers. Both callers themselves ran the supplied proofs before the parent repeated them; proof authorship was already supplied, but proof execution was duplicated. Neither authored a replacement proof.

| Accounted tokens | Native N1 | Tool-encouraged D1 |
|---|---:|---:|
| Input, cumulative bound session | 163,597 | 353,528 |
| Cached input, subset | 142,208 | 304,384 |
| Output | 1,180 | 1,522 |
| Reasoning, subset of output | 54 | 194 |

These are last cumulative session counters, not sums of snapshots or unique source bytes. Dollar cost is unknown. The time difference cannot be attributed wholly to the subsecond MCP call or to reasoning.

## Evidence and next action

Artifacts: `/var/tmp/forge/astra-recovery-pilot-runner-fx/` contains the frozen admission, raw packets, live catalog, both result trees, bound rollout references, validation receipts and `independent-pilot-outcome-astra.md`. Frozen manifest SHA: `30501ccf224dada62b3fdb7234c9c5c110313825203da60dac7a453242d6ebac`.

The inherited aggregate parser mislabeled this refusal `invalid-mcp-request`; the actual structured result is authoritative. It can also collapse multiple MCP operations inside one outer call. Adoption here was checked against raw requests/results and the identical stored native patch, not inferred from route labels or aggregate counters.

Next: correct usage guidance around commit verification, and assess existing proof-profile reuse on a genuinely needed task. Keep the compact recovery proposal offline; this caller never reached that refusal. No new full cohort is justified solely by improved refusal wording.

The collector/docs landing itself completed as b912f714 only after a fresh battery: the current commit-age rule counted observation documents, reaching44 against a30 ceiling even though Clojure source was unchanged. The successful remedy cost about14 minutes of battery plus about5 minutes of landing. This is documented process cost, not editing time; the rule was not weakened after the refusal.
