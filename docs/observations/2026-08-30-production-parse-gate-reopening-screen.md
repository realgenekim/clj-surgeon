# Captain's log: the production window closes the native parse-gate grave

Date: 2026-08-30

## Decision

The grave stays closed. Retire the proposal to put a reader-parse gate in
front of every native Clojure write.

The fresh non-surgeon production population contains five mechanically
eligible write -> failure -> same-target rewrite candidates. Bounded review
classifies three as genuine accidents, one as an intentional red witness, and
one as non-causal or undecidable. Three genuine loops over 3.071 weeks is
**0.98 per week**, far below the registered reopening threshold of 15.

The conclusion does not depend on judgment at the classification boundary.
Only 32 writes were eligible in the complete window, so even the impossible
upper bound in which every write became a genuine loop is **10.42 per week**.
The threshold cannot be reached by this population.

The independent shadow parser found useful signal but not a reopening case.
It classified 28 of 32 write actions unambiguously: 26 accepted and two
refused. Bounded review confirmed both refusals as real unmatched-delimiter
defects and found zero false refusals. Two other actions were reconstruction-
ambiguous and two were unavailable. The two true catches were repaired on the
same target inside five minutes, or **0.65 catchable repair loops per week**.
Neither of the three genuine transcript-observed loops was reader-catchable.

Gene's registered prediction was `<15 genuine/week`. The independent
prediction agreed before candidate inspection. The clean production
denominator confirms it.

## Frozen production windows

The counting window is half-open:

- UTC: `2026-08-08T18:08:23.783Z` through `2026-08-30T06:00:00.000Z`
- Pacific: `2026-08-08 11:08:23.783 PDT` through
  `2026-08-29 23:00:00.000 PDT`
- Exposure: 21.494 days, or 3.071 weeks

Only first-record `session_meta` entries with an exact admitted production cwd
were eligible. The three retained strata were:

| Privacy-safe stratum | Retained sessions | Session-start span (UTC) | Last admitted event (UTC) | Eligible writes | Candidates |
|---|---:|---|---|---:|---:|
| service A, sessionize family | 111 | Aug 8 18:08:23.783 -> Aug 28 04:55:33.210 | Aug 30 05:49:01.442 | 13 | 2 |
| service B, curtain-call staging | 2 | Aug 11 22:52:12.724 -> Aug 12 02:16:04.506 | Aug 12 02:43:19.788 | 19 | 3 |
| service B, curtain-call reconcile | 1 | Aug 11 15:29:19.769 | Aug 11 15:33:45.579 | 0 | 0 |

The rate uses the frozen calendar exposure, not summed tool-active minutes or
the short span between a stratum's first and last call. This is the same kind
of wall-window denominator used by the relayed audit and avoids rewarding a
bursty session merely because its writes were close together.

## Independent method

The program is a new standard-library implementation. It does not import or
execute the earlier circular-corpus script.

1. Enumerate only the known Codex layout
   `~/.codex/sessions/*/*/*/*.jsonl`. No other home directory is scanned.
2. Admit a rollout only when its first record is `session_meta`, its original
   timestamp is inside the frozen half-open window, and its cwd exactly matches
   one of the three production roots.
3. Admit a write only when `patch_apply_end` reports success for an update to
   an existing `.clj`, `.cljs`, or `.cljc` target. Adds, deletes, failed
   patches, prose writes, shell rewrites, and structural-tool writes are not in
   this matched-method population.
4. Treat a tool output as failure-bearing when it has a declared reader,
   compiler, runtime, test-failure, or nonzero-exit signature.
5. Form one candidate from the latest prior eligible write, the first matching
   failure, and the first later write sharing a target, all inside 300 seconds.
   Repeated diagnostics between the same two writes count once.
6. Review all five candidates in bounded transcript neighborhoods containing
   only the task intent and the relevant write, failure, and rewrite
   transitions. No prompt, source body, path, person, domain, or account data
   is copied into this observation.
7. Independently reconstruct shadow post-states by applying each retained hunk
   with zero fuzz to every matching historical Git blob and every previously
   generated matching state. Parse each candidate with
   `rewrite-clj.parser/parse-string-all`. Classify a target only when all
   reconstructed post-states agree; preserve mixed outcomes as ambiguous and
   no-match outcomes as unavailable.

The entire audit used local deterministic programs and local retained data.
It made zero model calls and no network calls.

## Bounded review of every candidate

| Session/write key | Classification | Causal? | Post-state parser refused? | Privacy-safe failure shape |
|---|---|---:|---:|---|
| `53aa8501e9a2/264` | genuine accident | yes | no | direct-handler request missed production parameter middleware |
| `e7935cd15b8f/167` | undecidable or non-causal | no | no | malformed structural-tool stdin; later rewrite was scope-cleanup reapplication |
| `e7935cd15b8f/186` | intentional red | yes | yes | expected topology witness; the write also carried an accidental delimiter defect |
| `c1f7aaacac3e/175` | genuine accident | yes | no | missing namespace require |
| `c1f7aaacac3e/369` | genuine accident | yes | no | rendered-HTML assertion depended on attribute order |

The mixed action at `e7935cd15b8f/186` needs both labels to remain honest. The
observed failing test was deliberately red so the transcript loop is
intentional. Independently, another target in that same write had an unmatched
closing delimiter, so a pre-landing parser would correctly have refused the
batch. It does not enter the genuine transcript-loop numerator, but it does
enter the shadow parser's true-catch count.

## Shadow parser screen

| Unit | Accepted | True refusal | Ambiguous | Unavailable | Total |
|---|---:|---:|---:|---:|---:|
| Write actions | 26 | 2 | 2 | 2 | 32 |
| Target occurrences | 35 | 2 | 2 | 2 | 41 |

The two unanimous refusals were bounded-review true catches:

- an unmatched closing delimiter in a production view source, followed by a
  same-target rewrite 245.298 seconds later;
- an unmatched closing delimiter in a test namespace import, followed by a
  same-target rewrite 242.232 seconds later.

No unanimous parser refusal was a valid source state, so the observed false-
refusal count is zero. This is favorable directional evidence for
`rewrite-clj` as a shadow parser. It is not a complete false-refusal proof:
four actions lack unanimous reconstruction, and the retained patch events do
not contain immutable complete post-state bytes.

## Registered condition

| Predicate | Result | Verdict |
|---|---:|---|
| At least 15 genuine loops/week | 0.98/week | fail |
| At least 15 shadow-catchable repair loops/week | 0.65/week | fail |
| Absolute eligible-write upper bound reaches 15/week | 10.42/week | impossible |
| Shadow catches materially exceed false refusals | 2 versus 0 on unanimous states | favorable but small and incomplete |

The reopening condition is conjunctive. Its incidence predicate fails by more
than an order of magnitude, and the complete eligible-write upper bound also
falls below the threshold. The favorable two-to-zero shadow result cannot
rescue it.

This production confirmation ends the question. Do not run another adoption
cohort, build a native-write wrapper, or reopen this stopped option under the
same product claim. A future parser mechanism would need to be justified as a
different contract with a different benefit, not as continuation of this
grave.

## Receipt

- Relay commit: `a02368a2f3e7b95a81c852b0c6ccf4afef6db3c4`
- Independent program:
  `dev/experiments/production_parse_gate_reopening_audit.py`
- Program SHA-256:
  `a6b529cc754449d059bff5b92399987de47465dcc16eaa3a8d7cb161ff10845c`
- Complete local receipt:
  `/private/tmp/production-parse-gate-reopening-receipt.json`
- Receipt SHA-256:
  `a6aa8f7e4a3fafd8abe5def6b6fdf5d3cd2d9c74943fce510a4335be60f0d0ed`

The receipt contains hashed session and target keys, event indices, aggregate
counts, bounded classifications, and parser outcomes. It contains no prompt or
source body and no production workspace path.
