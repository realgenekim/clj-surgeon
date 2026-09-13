# Gene report — 2026-09-13 ~02:30Z (Sat 19:30 Pacific), covering "Go on block 2!" (09-12 ~01Z) to now

## 1. Versus native, measured

The week's first vs-native table, preregistered with both bettors' rows fixed before any run, executed on the landed trunk 759974c7 by an Opus runner under the amended protocol (records 2026-09-13-probe-measure-report.md; every number in 2026-09-13-probe-measure/attempt2-summary.json).

| Stratum | NATIVE median (cold focused JVM) | PROBE median (warm image) | P/N | n per arm | unknown cells |
|---|---:|---:|---:|---:|---:|
| bb-portable namespaces (T1–T3) | 3,525 ms | 662 ms | 0.19 | 9 | 0 of 18 |
| JVM-only namespaces (T4–T6) | 4,210 ms | 1,170 ms | 0.28 | 9 | 0 of 18 |
| NATIVE variance-floor controls (T1, T4) | 3,107 / 2,007 ms | — | — | 6 + 6 | 0 of 12 |
| Planted non-test targets | no verdict (frozen NATIVE command defect) | 260 ms, typed refusal 2/2 | — | 2 | 2 of 4 |

Acceptance and first-attempt success 6/6 on both arms; noise gate (2 sd of controls ≈ 60 ms) cleared by ~48x; setup 16.1 s repaid in six invocations; the stale-result negative control passed on both arms (no stale green from the warm image). Caveat: six namespaces, one idle box, an image pre-populated by the untimed admission pass; per-task P/N 0.12–0.31; the saving is not a constant JVM start, so P/N belongs to the target namespace, not the verb.

Bets: Fable held, held (0.28 ≤ 0.30 with 0.02 to spare), held; refusal column unsettled. Astra held, held, held; refusal split. Astra's direction (a smaller JVM advantage than bb) was right.

Everything else this period was apparatus and correctness, not measurement; the two spent cohorts (long-context, arm N) wait on your 1b ordering and are unchanged.

## 2. Wins and losses

Wins
- **bb-rewrite-tower blocks A+B LANDED** on MCP/main = 759974c7 = stable/2026-09-13.1 after nine ship rounds; the probe verb landed with target authorization, complete closure through the shared ns parser, roots from the image classpath, a bounded and Throwable-safe HTTP boundary, seven registered refusal kinds each naming its native failure, and no verification boolean it cannot make false. TEST-ISO-016 (six runs per runtime, two-sd conservative ratio, JVM as reference, 99 portable / 10 bb-ineligible with reasons / 0 refused) and the computed bb ceiling 343,102 ms landed with it. Fast coordinator makespan 83 s → 24 s.
- **The seat's tooling can run this repository's own landing gate inside a packet** (ship v3.12, three rounds, two installs): eighteen check-only prewarm packets satisfied ship admission; twenty-one out-of-envelope writers and three trunk defects fixed, every one found by a refusal.
- **The ship's v3 auto-fix loop ran end to end for the first time** (round 6: Sol's fix captured, the seat's independent reviewer GO, witness, apply, re-seal, delta GO, battery green).
- **Six Sol NO-GOs, each a real class**: timing policy from single samples; unbounded probe output; a spec lying about a receipt; a probe that executed production code on request; incomplete closure (prefix lists; then hardcoded roots); a Throwable escaping the servlet. Each closed red-first with a typed refusal. After the fence rule change, Sol's round-8 verdict named the class and the oracle, and the oracle found thirteen escapes including two shapes he had not listed.
- **Data-not-code block** (Astra's design, your "safer / Rich Hickey" ask) built and verified: envelope admission at the artifact boundary with a typed refusal, refusal classification as data, evidence rows the manifest consumes with receipts opened, sleep identities; plus the committed diff-impact class oracle that found the verifier's regression before any patch. Round 7 is merging the landed trunk now.
- **wiki-llm opened** in the records lane: 19 entries from the landing program, 66 mined candidates, Astra's receipt-by-receipt pass (31 held / 26 weakened / 10 refuted, five deleted), her encounter-contract rule adopted over my two, six more entries tonight.

Losses
- **Zero measurement for the first 22 hours** of the period; window score 7 by my own meter; the tighten canary's apparatus share 69%. Recovered only tonight.
- **My loop shape**: instance-at-a-time discovery (seven packets for one class of writers; two ship rounds for one class of closure holes); twenty hand-written briefs; four false premises stated from summary numbers; six of eighteen prewarm packets forced by my own evidence commits moving the tree; two batteries flaked because I ran suites concurrently.
- **Round 7 was my own rule broken within the hour** (a failing ledger row committed as evidence); round 6 lost its green battery to an unqualified refspec in the ship (inb-db9e78).
- **The probe measure's first attempt stopped before any cell** on a frozen instrument that contradicted its own protocol; the refusal bet stays unsettled because the frozen NATIVE command had never met a non-test target.

## 3. Learnings → ratchets

| Learning | Ratchet | Where |
|---|---|---|
| A refusal that names a class gets the class oracle next, never another instance | tighten SKILL.md rule 1; fence appendix "Name the class"; memory class-oracle-before-another-round; inb-f346bc | installed on this seat; mayor to merge the skill (inb-fa856d) |
| Evidence commits move the tip and kill the prewarm receipt | tighten rule 2; memory evidence-commits-move-the-tip; ship v3.13 inb-257a96 (receipt binds the code tree) | filed |
| The wiki is a view of encounter contracts, not advice; metric = repeated defects per eligible encounter, false refusals tracked | tighten rule 3; wiki-llm/README rule | adopted |
| A receipt's identity is the candidate's ancestry, not a row | battery-fresh (exists); wiki #67 | enforced |
| Packets overrode every worker's heap/tmpdir; the check-only packet path was missing; the reviewer override was fixture-only | ship v3.12 (installed, PROVEN) | done |
| Autofix must regenerate the census; receipt-chain must qualify its refspec; packet battery rows must not land in the tree; short packet tmp roots | inb-61761d, inb-db9e78, inb-16b396, inb-da1941 | filed for v3.13 |
| Timing assertions with no variance floor flake under load | inb-2d3049 (census of timing assertions) | filed |
| The warm image writes its identity file into the checkout root | inb-3c65d7 (places) | filed |
| A frozen instrument must be dry-run against the frozen subject; controls must meet the planted negatives | wiki #71, #72; amendment 2 owed | owed |
| The Codex filter refuses crash-shaped fixtures | route to Opus at brief time; memory sol-live-on-anvil-seat | known |

## 4. What's next (in your 1b/2b order)

1. Data-not-code round 7 (merge of trunk) → check-only prewarm → ship with Sol's fence brief (written) → landing; that lands the safety pick (a typed refusal in the op itself) and moves the skills'-terms index 5 → 6 with the measure already scored.
2. Restart 7906 onto 759974c7 (announced, inb-609ebe) and verify a probe round-trip.
3. Amendment 2 of the probe preregistration (the NATIVE command on planted targets) and rerun only the planted cells to settle the refusal column.
4. Then, and only on your word: the spent cohorts (1a) or tower block C. Not before a decision on the wiki promotion queue (D3, ten entries, Astra's order) if you want it promoted.

Sublime: trunk 9 (unchanged); window 7 → 8 tonight on the measure; skills'-terms index 4 → 5 on the landing, 6 when data-not-code lands.
