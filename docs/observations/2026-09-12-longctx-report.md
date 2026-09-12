# Long-context replication

Manifest: `55cebd2bf102007e504aa3d81b348aedf811f595a79f2119836663bca00a6759`. Subject: `2ef1e713c36128f63a4874cd3a1818732a695635`.

Observed 96/104 completed task outcomes across 8/8 closed scored sessions (six primary, two controls). Independent acceptance remains external.

Verdict: **fail for complete execution**. All six primary sequences and the Sonnet control completed. The Opus control submitted position 6, then a classifier serialization error closed its caller; positions 7–13 were never delivered. There are 97 frozen submissions, 96 normally completed outcomes, and seven undelivered positions. No caller was rerun. This is apparatus noncompletion, not context overflow.

The classifier returned a regex Match for a basename reference, then failed JSON serialization. Its Boolean conversion is repaired in the delivered code. The launch source and [failure reproduction](sessions/attempt4/classifier-failure-reproduction.log) are preserved. The sixth control submission was classified and its unchanged oracle first run after closure: **pass**, gate unknown. Its original failure outcome and 57.1-second elapsed clock are retained; the supplementary 1.15-second oracle run is outside that clock. [Recovery receipt](sessions/attempt4/B-claude-opus-5-control/06-example-R-e4/postclose-scoring.json).

## Delivery and apparatus

Isolated arm probes attested B one paragraph and AC/H zero, with no memory files. Both H fixture edits were refused. These are caller attestations, not independent captures of rendered system bytes. Probes are excluded from scored target exposure.

The original Sonnet B launch attested zero twice despite bypass mode. Local Claude 2.1.267 code revealed that injection also depends on its bash-first assignment. B uses `CLAUDE_CODE_THRIFTY_SONIC=1` to enable the existing registered paragraph; both callers then attested one. CLAUDE.md, task text and CLI argv were unchanged. This delivery repair and all nine probes remain disclosed in [diagnosis](sessions/attempt4/harness-injection-diagnosis.json) and [admission](sessions/attempt4/delivery-admission.json).

B CLAUDE.md SHA-256: `2f5435d3d5f712b41a9a12b6571bfe74182dbafd3843a2c34ca448d7caac88b9`; AC/H: `1c3236c45dfab7df8f3a0a7a1669a0464cf0debe9cff6046eb52daeafc7f5137`. H executes `CLJ_HOOK_MODE=H python3 /var/tmp/forge/cohort-fx/apparatus/lib/hook_h.py`, SHA-256 `c1076e585bf22af7b83d9ba9d8ecbdb3c613e002dbe5389998b70de55bb61850`.

The harness supplies this paragraph; it is never copied into B CLAUDE.md (paragraph SHA-256 `c22f56ffa34a2efdd714bdb882bd178363e0b9ae07527b67e7c5adb3c8df423e`):

```
While bypass permissions mode is active:

Do your work through the Bash tool wherever it can accomplish the job: read files with cat, head, or sed -n, search with grep and find, and make file changes with sed, heredocs, or short scripts, rather than using the dedicated Read, Edit, or Write tools. Fall back to a dedicated tool only when Bash genuinely cannot do the job.
```

The packet no-server rule conflicts with full-suite gates that launch JVM HTTP/stdio servers. Owned gate groups are terminated and scored unknown; caller verification JVM subtrees invoking that same full suite are stopped while preserving the caller session. These environment interventions are separate from H-hook exposure. [Gate guard](sessions/attempt4/gate-guard.jsonl) and [caller verification guard](sessions/attempt4/tool-guard.jsonl) record exact affected tasks. B position 1 walls include the initial server-gate diagnosis delay. The first reordered Sonnet B gate also exposed a coordinator re-exec that needs a second JVM; the [nested-JVM guard](sessions/attempt4/nested-jvm-guard.jsonl) records it and subsequent such checks as unknown. Those walls include diagnosis time; later matching checks terminate promptly. Walls therefore include apparatus intervention timing and are not clean arm-speed comparisons.

## Both bettor tables beside measurements

Quality-unknown counts unresolved oracle-wrong AND gate-green conjunctions. An oracle pass rules out this defect even when its gate is unknown; an oracle failure with an unknown gate remains unresolved. Unknown gates are never acceptance. All gates are shown per task below.

Shares pool executed target mutation calls P/(P+E) in bins 1–4 / 5–9 / 10–13. Surgeon S is separate. NA means no denominator or unobserved work, never an observed zero. Point bets have no registered tolerance; exact point equality is reported separately from the descriptive bars.

| Arm / caller | Fable share % | Astra share % | Measured share % | Fable / Astra wrong | Measured wrong, gates green | Quality unknown |
|---|---|---|---|---|---:|---:|
| B-sonnet | 0 / 10 / 25 | 0 / 5 / 15 | 0.0 / 0.0 / 20.0 | 0 / 0 | 0 | 2 |
| B-opus | 25 / 45 / 60 | 25 / 40 / 50 | 0.0 / 20.0 / 25.0 | 1 / 1 | 0 | 0 |
| AC-sonnet | 0 / 0 / 10 | 0 / 0 / 5 | 0.0 / 0.0 / 0.0 | 0 / 0 | 0 | 5 |
| AC-opus | 0 / 10 / 25 | 5 / 10 / 20 | 0.0 / 0.0 / 0.0 | 0 / 0 | 0 | 6 |
| H-sonnet | 0 / 0 / 0 | 0 / 0 / 0 | 0.0 / 0.0 / 0.0 | 0 / 0 | 0 | 4 |
| H-opus | 0 / 5 / 10 | 0 / 5 / 10 | 0.0 / 0.0 / 0.0 | 0 / 0 | 0 | 1 |

| H caller | Fable exposure | Astra exposure by bin | Measured target-exposed tasks | All refusals by bin |
|---|---|---|---|---|
| H-opus | 2–4, only bins 2–3 | 0 / 1 / 1 | 0 / 0 / 0 | 0 / 0 / 0 |
| H-sonnet | 0–1 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 |

## Counts and reordered B control

| Session | P/E/S per bin | P/(P+E+S) % | Any-program tasks per bin | Completed | Oracle pass / fail | Gate green / red / unknown |
|---|---|---|---|---:|---|---|
| B-sonnet | 0/5/0 ; 0/6/0 ; 1/4/0 | 0.0 / 0.0 / 20.0 | 0 / 0 / 1 | 13 | 11 / 2 | 0 / 1 / 12 |
| B-opus | 0/5/0 ; 1/4/0 ; 1/3/0 | 0.0 / 20.0 / 25.0 | 0 / 1 / 1 | 13 | 13 / 0 | 0 / 1 / 12 |
| AC-sonnet | 0/5/0 ; 0/6/0 ; 0/5/0 | 0.0 / 0.0 / 0.0 | 0 / 0 / 0 | 13 | 8 / 5 | 0 / 1 / 12 |
| AC-opus | 0/5/0 ; 0/6/0 ; 0/5/0 | 0.0 / 0.0 / 0.0 | 0 / 0 / 0 | 13 | 7 / 6 | 0 / 1 / 12 |
| H-sonnet | 0/5/0 ; 0/7/0 ; 0/6/0 | 0.0 / 0.0 / 0.0 | 0 / 0 / 0 | 13 | 9 / 4 | 0 / 1 / 12 |
| H-opus | 0/5/0 ; 0/6/0 ; 0/6/0 | 0.0 / 0.0 / 0.0 | 0 / 0 / 0 | 13 | 12 / 1 | 0 / 1 / 12 |
| B-sonnet control | 0/5/0 ; 0/7/0 ; 0/5/0 | 0.0 / 0.0 / 0.0 | 0 / 0 / 0 | 13 | 10 / 3 | 0 / 0 / 13 |
| B-opus control | 4/0/0 ; 2/0/0 ; NA | 100.0 / 100.0 / NA | 4 / 2 / NA | 5 | 4 / 2 | 0 / 0 / 13 |

| Session | Audited tasks | Scratch authoring calls excluded | Request preparation calls | Final mechanisms (audited) |
|---|---:|---:|---:|---|
| B-sonnet | 13 | 2 | 0 | {'editor': 12, 'program': 1} |
| B-opus | 13 | 9 | 0 | {'editor': 11, 'program': 2} |
| AC-sonnet | 13 | 1 | 0 | {'editor': 13} |
| AC-opus | 13 | 5 | 0 | {'editor': 13} |
| H-sonnet | 13 | 1 | 0 | {'editor': 13} |
| H-opus | 13 | 5 | 0 | {'editor': 13} |
| B-sonnet control | 13 | 4 | 0 | {'editor': 13} |
| B-opus control | 6 | 3 | 0 | {'program': 6} |

Scratch totals include classified fixture authoring and manually corrected target-read scripts. Request preparation is distinct from target mutation. Every audited call retains its full input and result; unaudited rows remain explicitly unaudited.


| Caller | Fixed B share % | Reordered B share % |
|---|---|---|
| claude-sonnet-5 | 0.0 / 0.0 / 20.0 | 0.0 / 0.0 / 0.0 |
| claude-opus-5 | 0.0 / 20.0 / 25.0 | 100.0 / 100.0 (partial) / NA |

The reordered Opus B early bin is 100% program writes, versus 0% in fixed order. High program use therefore appeared immediately in this control; delayed emergence is not required in this sample. Its middle bin covers only positions 5–6, and its late bin is unobserved. Sonnet reordered B used only editors, including E4 at position 6; fixed-order E4 used a program at position 13. These are descriptive order-sensitive observations from one control per caller.

Fixed order: T-01..T-04, R-01..R-04, I-01..I-04, example-R-e4.

Control seed (registration SHA-256): `d957b6747e4ce3ced2dc3e87c3b73b15db593e61b63dcbebaa3c6cc87dc1456d`.

Control order: R-03-workspace-string-alias, R-02-recovery-require-measured, I-04-mission-display-decision-view, T-01-outline-memory-spec-marker, T-02-study-row-line-range, example-R-e4, R-01-core-discovery-require-edn, I-02-mission-cli-admitted-profiles, T-04-lane-manifest-total, I-03-heap-sample-retention, R-04-namespace-split-require-cheshire, T-03-alias-migration-kind-count, I-01-mission-cli-stale.

## Bet outcomes and falsification bars

Fable's B-opus exact share bet missed; its exact gate-green-wrong count bet is missed.

Fable's B-sonnet exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Fable's AC-opus exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Fable's AC-sonnet exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Fable's H-opus exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Fable's H-sonnet exact share bet held; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Astra's B-opus exact share bet missed; its exact gate-green-wrong count bet is missed.

Astra's B-sonnet exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Astra's AC-opus exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Astra's AC-sonnet exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Astra's H-opus exact share bet missed; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Astra's H-sonnet exact share bet held; its exact gate-green-wrong count bet is unscored because outcomes remain unknown.

Fable's late B <25% bar: not triggered (25.0%). This cannot prove that the harness line alone explains the census: B-minus-line is absent.

Astra's length support bar (late ≥40%, rise ≥20 points): missed. Defeat bar (late ≤25%, rise ≤10): not triggered. Early=0.0%, late=25.0%, rise=25.0 points. High share from the start supports persistence rather than delayed emergence.

Fable's AC-late >B-late bar: not triggered. Astra's bundle suppression bar (AC ≥20 points below B): held; reversal (AC ≥10 points above B): not triggered; gap ≤10 points in magnitude: no. B−AC=25.0 points. This contrast does not isolate the exception, plate, or paragraph removal.

Fable's H-opus exposure bet missed. Astra's H-opus exact exposure bet missed. Exposed positions: [].

Fable's H-sonnet exposure bet held. Astra's H-sonnet exact exposure bet held. Exposed positions: [].

Astra’s delayed-exposure support requires at least two true H Opus exposed tasks, first after position 4; zero exposure defeats that forecast and early exposure defeats its timing. Fixture refusals do not count. H−H0 repair efficacy is unidentified because H0 is absent; H−AC is the refusal-plus-repair bundle.

Astra’s quality defeat bar is ≥2 observed gate-green wrong tasks in an AC/H cell. Not observed; unknown outcomes cannot establish a held near-zero forecast.

## Per-task context and complete walls

Context is the first assistant request’s input + cache creation + cache read tokens at each task. It is not cumulative session usage. Complete wall begins before the frozen target checkout and includes caller, snapshot, oracle, gate, and measurement work. For the failed control position, elapsed wall stops at the apparatus error and is not a complete wall; its post-close oracle time is reported separately above. Undelivered positions remain unknown. Raw request usages and compaction events are retained per result.

| Session | Position | Task | Context tokens | Caller s | Elapsed s | Oracle | Gate | Outcome |
|---|---:|---|---:|---:|---:|---|---|---|
| B-sonnet | 1 | T-01-outline-memory-spec-marker | 32901 | 14.8 | 806.8 | pass | unknown | completed |
| B-sonnet | 2 | T-02-study-row-line-range | 35376 | 30.2 | 31.2 | fail | unknown | completed |
| B-sonnet | 3 | T-03-alias-migration-kind-count | 40795 | 114.2 | 116.8 | pass | unknown | completed |
| B-sonnet | 4 | T-04-lane-manifest-total | 60821 | 22.3 | 23.3 | pass | unknown | completed |
| B-sonnet | 5 | R-01-core-discovery-require-edn | 64661 | 9.8 | 19.7 | pass | red | completed |
| B-sonnet | 6 | R-02-recovery-require-measured | 66199 | 15.9 | 16.9 | pass | unknown | completed |
| B-sonnet | 7 | R-03-workspace-string-alias | 67988 | 13.0 | 14.0 | pass | unknown | completed |
| B-sonnet | 8 | R-04-namespace-split-require-cheshire | 69471 | 9.1 | 10.4 | pass | unknown | completed |
| B-sonnet | 9 | I-01-mission-cli-stale | 70926 | 154.6 | 155.7 | pass | unknown | completed |
| B-sonnet | 10 | I-02-mission-cli-admitted-profiles | 98572 | 33.3 | 34.3 | pass | unknown | completed |
| B-sonnet | 11 | I-03-heap-sample-retention | 110181 | 18.8 | 22.2 | fail | unknown | completed |
| B-sonnet | 12 | I-04-mission-display-decision-view | 113295 | 29.4 | 30.5 | pass | unknown | completed |
| B-sonnet | 13 | example-R-e4 | 120426 | 21.6 | 22.9 | pass | unknown | completed |
| B-opus | 1 | T-01-outline-memory-spec-marker | 21727 | 15.2 | 806.7 | pass | unknown | completed |
| B-opus | 2 | T-02-study-row-line-range | 27097 | 185.1 | 186.0 | pass | unknown | completed |
| B-opus | 3 | T-03-alias-migration-kind-count | 41402 | 101.2 | 103.8 | pass | unknown | completed |
| B-opus | 4 | T-04-lane-manifest-total | 64104 | 57.9 | 58.9 | pass | unknown | completed |
| B-opus | 5 | R-01-core-discovery-require-edn | 76324 | 16.9 | 26.7 | pass | red | completed |
| B-opus | 6 | R-02-recovery-require-measured | 78379 | 26.3 | 27.3 | pass | unknown | completed |
| B-opus | 7 | R-03-workspace-string-alias | 82098 | 26.2 | 27.3 | pass | unknown | completed |
| B-opus | 8 | R-04-namespace-split-require-cheshire | 85947 | 21.6 | 23.0 | pass | unknown | completed |
| B-opus | 9 | I-01-mission-cli-stale | 88910 | 61.3 | 62.4 | pass | unknown | completed |
| B-opus | 10 | I-02-mission-cli-admitted-profiles | 108846 | 34.6 | 35.7 | pass | unknown | completed |
| B-opus | 11 | I-03-heap-sample-retention | 113775 | 27.7 | 29.5 | pass | unknown | completed |
| B-opus | 12 | I-04-mission-display-decision-view | 118958 | 29.4 | 30.5 | pass | unknown | completed |
| B-opus | 13 | example-R-e4 | 126213 | 31.9 | 33.2 | pass | unknown | completed |
| AC-sonnet | 1 | T-01-outline-memory-spec-marker | 34136 | 16.0 | 16.4 | pass | unknown | completed |
| AC-sonnet | 2 | T-02-study-row-line-range | 37045 | 15.9 | 16.9 | fail | unknown | completed |
| AC-sonnet | 3 | T-03-alias-migration-kind-count | 40406 | 90.5 | 93.1 | pass | unknown | completed |
| AC-sonnet | 4 | T-04-lane-manifest-total | 71788 | 141.2 | 142.2 | pass | unknown | completed |
| AC-sonnet | 5 | R-01-core-discovery-require-edn | 100827 | 6.7 | 16.3 | pass | red | completed |
| AC-sonnet | 6 | R-02-recovery-require-measured | 102420 | 9.1 | 10.1 | pass | unknown | completed |
| AC-sonnet | 7 | R-03-workspace-string-alias | 104094 | 10.7 | 11.7 | pass | unknown | completed |
| AC-sonnet | 8 | R-04-namespace-split-require-cheshire | 105882 | 7.1 | 8.4 | pass | unknown | completed |
| AC-sonnet | 9 | I-01-mission-cli-stale | 107929 | 66.6 | 67.8 | fail | unknown | completed |
| AC-sonnet | 10 | I-02-mission-cli-admitted-profiles | 126196 | 71.3 | 72.4 | fail | unknown | completed |
| AC-sonnet | 11 | I-03-heap-sample-retention | 139236 | 25.7 | 27.5 | fail | unknown | completed |
| AC-sonnet | 12 | I-04-mission-display-decision-view | 143352 | 47.4 | 48.4 | fail | unknown | completed |
| AC-sonnet | 13 | example-R-e4 | 151931 | 26.3 | 27.6 | pass | unknown | completed |
| AC-opus | 1 | T-01-outline-memory-spec-marker | 22831 | 19.9 | 20.3 | pass | unknown | completed |
| AC-opus | 2 | T-02-study-row-line-range | 26354 | 90.0 | 90.9 | fail | unknown | completed |
| AC-opus | 3 | T-03-alias-migration-kind-count | 43596 | 60.4 | 63.1 | pass | unknown | completed |
| AC-opus | 4 | T-04-lane-manifest-total | 61630 | 51.2 | 52.2 | fail | unknown | completed |
| AC-opus | 5 | R-01-core-discovery-require-edn | 75617 | 25.9 | 35.5 | pass | red | completed |
| AC-opus | 6 | R-02-recovery-require-measured | 78424 | 21.0 | 22.0 | pass | unknown | completed |
| AC-opus | 7 | R-03-workspace-string-alias | 81326 | 27.1 | 28.1 | pass | unknown | completed |
| AC-opus | 8 | R-04-namespace-split-require-cheshire | 85381 | 20.0 | 21.4 | pass | unknown | completed |
| AC-opus | 9 | I-01-mission-cli-stale | 88227 | 52.4 | 53.6 | fail | unknown | completed |
| AC-opus | 10 | I-02-mission-cli-admitted-profiles | 102165 | 101.3 | 102.4 | fail | unknown | completed |
| AC-opus | 11 | I-03-heap-sample-retention | 117133 | 41.7 | 43.5 | fail | unknown | completed |
| AC-opus | 12 | I-04-mission-display-decision-view | 122731 | 74.0 | 75.1 | fail | unknown | completed |
| AC-opus | 13 | example-R-e4 | 137586 | 63.1 | 64.4 | pass | unknown | completed |
| H-sonnet | 1 | T-01-outline-memory-spec-marker | 34130 | 14.1 | 14.5 | pass | unknown | completed |
| H-sonnet | 2 | T-02-study-row-line-range | 36146 | 113.0 | 114.0 | pass | unknown | completed |
| H-sonnet | 3 | T-03-alias-migration-kind-count | 55725 | 76.6 | 79.1 | pass | unknown | completed |
| H-sonnet | 4 | T-04-lane-manifest-total | 71519 | 73.8 | 74.8 | pass | unknown | completed |
| H-sonnet | 5 | R-01-core-discovery-require-edn | 87848 | 12.4 | 22.0 | pass | red | completed |
| H-sonnet | 6 | R-02-recovery-require-measured | 89595 | 23.3 | 24.4 | pass | unknown | completed |
| H-sonnet | 7 | R-03-workspace-string-alias | 92567 | 18.8 | 19.8 | pass | unknown | completed |
| H-sonnet | 8 | R-04-namespace-split-require-cheshire | 94844 | 12.3 | 13.7 | pass | unknown | completed |
| H-sonnet | 9 | I-01-mission-cli-stale | 96778 | 105.0 | 106.1 | fail | unknown | completed |
| H-sonnet | 10 | I-02-mission-cli-admitted-profiles | 115520 | 75.2 | 76.4 | fail | unknown | completed |
| H-sonnet | 11 | I-03-heap-sample-retention | 130897 | 44.7 | 46.5 | fail | unknown | completed |
| H-sonnet | 12 | I-04-mission-display-decision-view | 137015 | 69.3 | 70.3 | fail | unknown | completed |
| H-sonnet | 13 | example-R-e4 | 146559 | 30.5 | 31.8 | pass | unknown | completed |
| H-opus | 1 | T-01-outline-memory-spec-marker | 22829 | 15.3 | 15.8 | pass | unknown | completed |
| H-opus | 2 | T-02-study-row-line-range | 26351 | 161.0 | 162.0 | fail | unknown | completed |
| H-opus | 3 | T-03-alias-migration-kind-count | 47987 | 77.7 | 80.3 | pass | unknown | completed |
| H-opus | 4 | T-04-lane-manifest-total | 68215 | 80.2 | 81.3 | pass | unknown | completed |
| H-opus | 5 | R-01-core-discovery-require-edn | 88863 | 15.8 | 25.9 | pass | red | completed |
| H-opus | 6 | R-02-recovery-require-measured | 91509 | 22.0 | 23.1 | pass | unknown | completed |
| H-opus | 7 | R-03-workspace-string-alias | 94493 | 32.8 | 33.9 | pass | unknown | completed |
| H-opus | 8 | R-04-namespace-split-require-cheshire | 99613 | 17.5 | 18.9 | pass | unknown | completed |
| H-opus | 9 | I-01-mission-cli-stale | 102279 | 45.6 | 266.7 | pass | unknown | completed |
| H-opus | 10 | I-02-mission-cli-admitted-profiles | 120615 | 55.5 | 56.5 | pass | unknown | completed |
| H-opus | 11 | I-03-heap-sample-retention | 130306 | 28.5 | 30.3 | pass | unknown | completed |
| H-opus | 12 | I-04-mission-display-decision-view | 135319 | 54.5 | 55.6 | pass | unknown | completed |
| H-opus | 13 | example-R-e4 | 147131 | 57.8 | 59.1 | pass | unknown | completed |
| B-sonnet control | 1 | R-03-workspace-string-alias | 32862 | 17.0 | 263.2 | pass | unknown | completed |
| B-sonnet control | 2 | R-02-recovery-require-measured | 35192 | 10.6 | 12.0 | pass | unknown | completed |
| B-sonnet control | 3 | I-04-mission-display-decision-view | 37121 | 150.2 | 156.3 | pass | unknown | completed |
| B-sonnet control | 4 | T-01-outline-memory-spec-marker | 73025 | 11.8 | 12.3 | pass | unknown | completed |
| B-sonnet control | 5 | T-02-study-row-line-range | 75345 | 115.8 | 116.8 | fail | unknown | completed |
| B-sonnet control | 6 | example-R-e4 | 95911 | 25.9 | 27.2 | pass | unknown | completed |
| B-sonnet control | 7 | R-01-core-discovery-require-edn | 102986 | 11.0 | 12.6 | pass | unknown | completed |
| B-sonnet control | 8 | I-02-mission-cli-admitted-profiles | 108401 | 164.3 | 169.7 | fail | unknown | completed |
| B-sonnet control | 9 | T-04-lane-manifest-total | 133103 | 82.9 | 84.6 | pass | unknown | completed |
| B-sonnet control | 10 | I-03-heap-sample-retention | 148203 | 33.9 | 37.8 | fail | unknown | completed |
| B-sonnet control | 11 | R-04-namespace-split-require-cheshire | 153332 | 12.4 | 14.6 | pass | unknown | completed |
| B-sonnet control | 12 | T-03-alias-migration-kind-count | 155486 | 108.7 | 111.3 | pass | unknown | completed |
| B-sonnet control | 13 | I-01-mission-cli-stale | 187047 | 40.1 | 45.8 | pass | unknown | completed |
| B-opus control | 1 | R-03-workspace-string-alias | 21688 | 43.7 | 45.5 | pass | unknown | completed |
| B-opus control | 2 | R-02-recovery-require-measured | 28742 | 54.2 | 55.5 | pass | unknown | completed |
| B-opus control | 3 | I-04-mission-display-decision-view | 37341 | 139.0 | 145.3 | fail | unknown | completed |
| B-opus control | 4 | T-01-outline-memory-spec-marker | 67619 | 20.9 | 21.3 | pass | unknown | completed |
| B-opus control | 5 | T-02-study-row-line-range | 72600 | 101.0 | 102.0 | fail | unknown | completed |
| B-opus control | 6 | example-R-e4 | 82699 | 57.0 | 57.1 | pass | unknown | noncompletion-error |
| B-opus control | 7 | R-01-core-discovery-require-edn | NA | NA | NA | unknown | unknown | not-delivered-after-noncompletion |
| B-opus control | 8 | I-02-mission-cli-admitted-profiles | NA | NA | NA | unknown | unknown | not-delivered-after-noncompletion |
| B-opus control | 9 | T-04-lane-manifest-total | NA | NA | NA | unknown | unknown | not-delivered-after-noncompletion |
| B-opus control | 10 | I-03-heap-sample-retention | NA | NA | NA | unknown | unknown | not-delivered-after-noncompletion |
| B-opus control | 11 | R-04-namespace-split-require-cheshire | NA | NA | NA | unknown | unknown | not-delivered-after-noncompletion |
| B-opus control | 12 | T-03-alias-migration-kind-count | NA | NA | NA | unknown | unknown | not-delivered-after-noncompletion |
| B-opus control | 13 | I-01-mission-cli-stale | NA | NA | NA | unknown | unknown | not-delivered-after-noncompletion |

Frozen-oracle failures are not all string damage: several check exact helper names or preservation (for example `checkpoint!` versus the oracle’s `sample-retention!`). The oracles were not changed. [Every oracle failure and its exact output](sessions/attempt4/oracle-failures.json).


## MCP invocation permission

Observed permission-layer denials before MCP execution: [('AC-opus', 13, 'mcp__clj-surgeon__edit_clojure'), ('H-opus', 13, 'mcp__clj-surgeon__edit_clojure')]. The servers were connected/offered, but these calls were not authorized by Claude. They are neither executed Surgeon writes nor Surgeon refusals. Thus S=0 must not be read as freely choosing against an available write capability. The frozen acceptEdits allowance covers Bash/Edit/Write, not MCP calls.


## Limits and receipts

E4 gate is unknown under Amendment 3 in either order. Other gates retain gate-v2 and their untouched own-SHA baselines with the no-new-failures rule; no baseline is reset to absorb earlier edits. Unparsed gates remain unknown because the composed fixture is unqualified at every prefix. Oracle evaluation uses frozen first-submission target snapshots; no hidden feedback is sent to callers. Prior results remain in Git history when a later target’s own-SHA checkout supersedes them.

Oracle files are held out of the task prompt but not protected from filesystem reads; isolation remains owed. Program classification is a transcript measurement, with ambiguous calls requiring audit. One session per cell makes these descriptive comparisons; dependent task outcomes are not independent replications. The two reordered controls measure order sensitivity, not independent replication.

[Measurements](sessions/attempt4/measurements.json), [packet ledger](sessions/ledger.tsv), [input hashes](sessions/attempt4/frozen-input-verification.json), [transport checks](sessions/attempt4/transport-check-scored.log), [execution freeze](sessions/attempt4/execution-freeze.json), [process census](sessions/attempt4/process-census-summary.json).

Closure validation passed: frozen inputs unchanged; 97 snapshots and mechanism audits checked; 104 task receipt rows; no memory files, prior-result preservation breaches, or live session processes. The process census observed a peak of one JVM over 16,148 samples at a nominal 0.2-second interval; this is sampled evidence. Maximum observed task-start context was 187,047 tokens, with no compaction events recorded. No noncompletion is attributed to overflow.

The five transport/admission tests, syntax checks, and actual-transcript classifier regression passed. Report field shapes, receipt paths, and all 104 per-task EDN rows validated. [Closure](sessions/attempt4/closure.json), [Python checks](sessions/attempt4/final-python-checks.log), [EDN checks](sessions/attempt4/final-edn-checks.log). The delivered runner integrates the guards that were attached during this scored attempt; [launch source hashes](sessions/attempt4/scored-implementation.json) and [delivered source hashes](sessions/attempt4/delivered-source-digests.json) distinguish them. No new owner-worktree commit was created.
