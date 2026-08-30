# A prepared transaction moved Codex; the Claude 16/16 differential did not reproduce

**Date:** 2026-08-29–30 Pacific

**Window:** 2026-08-30T06:50:42Z–07:22:00Z / 2026-08-29T23:50:42-0700–2026-08-30T00:22:00-0700

**Codex:** CLI 0.149.1, `gpt-5.6-sol`, high reasoning, ChatGPT subscription

**Claude:** Code 2.1.219, two Fable and two Opus attempts
**Decision:** Target the prepared-request interface, not an executor-wide reluctance theory. Exactness and file size were killed as causes on this task. A ready-to-submit guarded transaction moved Codex from 2/4 to 4/4 Surgeon-first, exactly the preregistered +50-point gate.

## Answer

The headline differential did not survive a matched unforced task. Claude's prior unhinted baseline was 16/16 Surgeon-first when the prompt required a successful `inspect_clojure` call before mutation. Removing that forced inspection while retaining the same existing-file, nine-site, six-owner fixture produced:

| Caller | Surgeon-first | Native-first | Environment valid | Exact semantics | Valid post-choice interview |
|---|---:|---:|---:|---:|---:|
| Codex | **1/8** | 7/8 | 6/8 | 8/8 | 8/8 |
| Claude | **0/4** | 4/4 | 4/4 | 4/4 | 3/4 |

Among the six Codex attempts with both routes executable, Codex was 1/6 Surgeon-first. Claude was 0/4, not 16/16. The earlier contrast was therefore not a stable caller property on this fixture; forced structural inspection was part of the route geometry that saturated Claude.

The interviews generated two eligible candidate explanations. Codex repeatedly described the task as a small, exact, literal, localized change and said structural editing would not reduce risk or effort. Three preregistered Codex-only ablations then tested components of those statements:

| Ablation | Control Surgeon | Treatment Surgeon | Lift | Registered result |
|---|---:|---:|---:|---|
| Remove exact before-state details | 2/4 | 1/4 | **−25 pp** | **Killed** |
| Add 600 unrelated file lines | 1/4 | 0/4 | **−25 pp** | **Killed** |
| Supply exact guarded transaction | 2/4 | 4/4 | **+50 pp** | **Causal credit** |

All 24 ablation attempts were environment-valid and byte-exact with the independent three-assertion test green. The prepared transaction also cleared the second registered gate of at least 3/4 treatment attempts Surgeon-first: it routed 4/4.

The bounded causal statement is: **making the exact guarded request ready to submit moved Codex routing on this task**. This treatment necessarily made that route more concrete and more salient while removing call-assembly work, so the experiment does not separate mechanical effort from payload salience. It does reject the broader stories that Codex merely needs a larger file or less exact user instructions.

## Frozen task and route geometry

Every attempt began in a fresh clone of fixture commit `f2eded29c27962d607488bffcb5097a4a33a574d`. The target Clojure file existed and was committed before the session. The requested coherent change updated two timeout values, renamed one private Var and four references, and changed two profile literals. The complete expected file was the byte oracle; `clojure -M:test` independently checked three semantic properties.

The fixture-scoped stdio proxy advertised only the production `inspect_clojure` and `edit_clojure` schemas fetched from the live server. Its tool-list SHA-256 was `47411d72b8ea6b47b36fa92717e19f4376b0083fa697677ea78183fb7a737c22`, matching the retained Claude route-hint experiment. Codex also had native `apply_patch`; Claude had native `Edit` and `Write`. The task named both mutation methods symmetrically, left inspection optional, and deferred route explanation until a separate resumed turn after the edit process ended.

The primary route was the first completed successful mutation. A started but refused MCP call did not count as an actual mutation. Invalid, incorrect, refused, and missing-mutation attempts remained denominator losses.

## Interview protocol and retained losses

The frozen schedule interleaved eight Codex attempts with four Claude attempts in Fable, Opus, Opus, Fable order. Every model session received a fresh workspace. After its edit process ended, the exact session was resumed with a tool-free question asking for one to three route-level sentences without paths, source, commands, identifiers, hidden reasoning, or experimenter speculation.

Two Codex attempts exposed a harness geometry defect: the client refused `edit_clojure` under `approval=never`, then used native patching. Both remain environment-invalid losses. A repair addendum switched later fresh Codex runs to executable bypass geometry inside the isolated synthetic fixture. The exact first two sessions were resumed rather than replaced.

The first Claude follow-up returned a complete answer but cost $0.23633 against a $0.20 process ceiling, so Claude labeled the process budget-exhausted after emitting the response. It remains `interview_valid=false` under the frozen exit-zero rule; its verbatim answer remains stated-reason evidence. The remaining process ceilings were frozen in an addendum so the total scheduled launch maximum stayed $4.15. Actual Claude CLI spend was **$0.963460**:

| Claude model | Attempts | Actual spend |
|---|---:|---:|
| Fable | 2 | $0.690453 |
| Opus | 2 | $0.273007 |

This is Claude Code's reported usage cost, including any helper activity represented in its result, not an invoice reconciliation.

## Stated reasons, verbatim

These are candidates, not causal verdicts.

| Caller/run | Route | Verbatim post-choice explanation |
|---|---|---|
| Claude 1 | Native | “I chose the native edit route because the change consisted of a small set of exact, unambiguous, non-overlapping literal replacements in a single file I had already read in full, so simple string edits could apply them precisely with no risk to surrounding bytes. The structural tooling's extra ceremony (transactions, owner scoping, migration declarations) offered no added safety for such straightforward substitutions.” |
| Claude 2 | Native | “I chose the native edit tool because the target was a single small file whose full contents I had already read, so exact anchored string replacements were unambiguous and sufficient. The change was a handful of independent, non-overlapping literal substitutions rather than a broad structural or cross-file refactor, so the additional ceremony of the structural Clojure editing tool would have added cost without reducing risk.” |
| Claude 3 | Native | “The file was small and I had already read its full contents, so I had exact literal context for every change and no need for structural discovery. That made the native edit route the most direct and lowest-overhead choice, with byte-exact replacements preserving everything untouched; the specialized Clojure tooling would have added indirection without reducing risk at this scale.” |
| Claude 4 | Native | “I chose the native edit route because the file was small, fully visible, and all changes were exact literal substitutions I could specify unambiguously, so no structural analysis or cross-file coordination was needed. The native method also carried lower overhead than assembling a structured edit transaction for such a simple, single-file change.” |
| Codex 1 | Native | “I used the native patch route because the change was small and exact, and the structural mutation route was unavailable under the active approval policy.” |
| Codex 2 | Native | “I used the native patch route because the structural edit endpoint was unavailable under the session’s approval policy. A single exact patch was sufficient to make the small, localized changes while preserving unrelated bytes.” |
| Codex 3 | Native | “I chose the native patch route because the change was a small, explicit set of literal replacements with no structural ambiguity. It provided precise, minimal edits while preserving unrelated bytes.” |
| Codex 4 | Native | “I chose the native patch route because the change was a small, exact set of visible literal replacements and reference renames. Structural editing would not have reduced risk or effort for this scope.” |
| Codex 5 | Surgeon | “I chose `edit_clojure` because the change comprised several exact, owner-scoped replacements that could be committed atomically with match-count guards. Its verified read-back also confirmed the coherent transaction while preserving unrelated content.” |
| Codex 6 | Native | “I chose the native patch route because this was a small, fully specified set of literal replacements in one file. Structural tooling would not have reduced risk or effort.” |
| Codex 7 | Native | “I chose the native patch route because the change was a small, exact set of visible literal replacements and a localized rename. Structural tooling would not have reduced meaningful risk or work.” |
| Codex 8 | Native | “I chose the native patch route because this was a small, fully specified set of literal replacements in one file. Structural editing would not have reduced ambiguity or verification effort.” |

Only the six environment-valid Codex attempts generated preference candidates. The two approval-policy comments were retained as a disclosed route-geometry confound, not promoted into a preference hypothesis. This eligibility judgment was made after the interview and before any ablation; the reason-coding receipt freezes it explicitly.

## Ablations

The ablation preregistration was frozen at `2026-08-30T07:03:33.917316Z`, before any ablation process. Each independent experiment used four control and four treatment runs in `A B B A / B A A B` order. Causal credit required both at least +50 percentage points and at least 3/4 treatment attempts Surgeon-first. A lift below +25 points or treatment at most 1/4 killed the reason.

### Exact before-state details: killed

The treatment kept the same final oracle but removed old literal values and the enumerated reference-owner list from the user prompt. Control routed Surgeon 2/4; treatment routed 1/4. The −25-point movement is opposite the prediction and mechanically kills the hypothesis.

### File visibility scale: killed

The treatment committed exactly 600 unrelated comment lines before the original source. Runtime semantics, owner topology, task prompt, production schemas, and target changes stayed fixed. Control routed Surgeon 1/4; treatment routed 0/4. The −25-point movement kills the hypothesis. A bigger file did not make Codex reach for structural ownership.

### Prepared guarded request: clears the gate

The treatment appended the exact already-validated `edit_clojure` argument object and said it could be submitted unchanged if that route was chosen; native patch remained available. Control routed Surgeon 2/4; treatment routed 4/4. The +50-point lift and 4/4 absolute treatment rate exactly clear both registered gates.

This establishes the complete prepared request as a causal intervention in this setting. It does not establish that schema length alone caused avoidance, that every task will move, or that a product-returned `next_call` will be consumed at the same rate as a user-prompt payload.

## Time and action boundary

The 12 interview edit processes took 242.471 seconds in total. Median complete edit process wall was 21.456 seconds for Codex and 20.253 seconds for Claude. Across the interview and ablations, 11 successful `edit_clojure` commits reported only 0.623 seconds of direct tool wall in total, with a 51.64 ms median.

The adoption boundary is therefore not execution latency inside the tool. It is the model-visible decision and request-construction boundary before the call. The prepared-request result is consistent with moving that boundary into a mechanical compiler or returned `next_call` rather than adding more doctrine.

## Limits and corrections

- Each ablation cell has n=4. One run moves a rate by 25 points. The registered gate was deliberately coarse; this is not a population estimate or equivalence claim.
- The prepared payload treatment also increases route-specific salience. The causal object is “ready-to-submit exact guarded request,” not pure hidden cognitive effort.
- The 16/16 Claude source result required forced inspection. This study shows non-transfer to one unforced task, not that Claude never chooses Surgeon without inspection.
- Codex's first two attempts were invalid because of approval geometry. They remain headline losses; the clean 1/6 subset is reported separately.
- One ablation score pass crashed after the first model process because successful Codex MCP events encode `error: null`. The immutable run was reconstructed from its JSONL, file, server log, and test; no rerun occurred.
- A post-cohort verification command re-ran non-idempotent summarizers and changed only each aggregate's `generated_at_ns`. The original interview aggregate hash recorded in the ablation preregistration no longer names the current aggregate bytes. The 36 immutable run score files are the counting authority, and the receipt contains the full disclosure plus normalized projection hashes.
- No `agent-usage-window-end` marker is written because this experiment consumed no Codex/Claude history window and must not advance the default ethnographic collector.

## Product decision and next falsifier

Do not spend the next adoption cycle trying to convince the executor that small or large files deserve Surgeon. Those two self-reports failed their causal tests. Build the smallest product-side experiment that returns a complete, exact, ready-to-submit guarded mutation request at the decision boundary, with no user-prompt route cue. Counterbalance it against the same successful inspection without that request and score first completed mutation, exact semantics, refusals, and complete task wall.

A clean product-returned cohort should require at least +25 points over its own control before investment. If it does not move, the prompt-embedded result does not transfer and should be killed as salience-bound. If it moves, adoption work targets the tool/result contract rather than executor doctrine.

## Captain's log

- **Option created:** Separate self-reported task simplicity, file visibility scale, and request-assembly cost into independently killable arms.
- **Reversible ratchet:** Reused one committed fixture, byte oracle, production schema proxy, and one-factor ABBA/BAAB schedule.
- **Evidence:** 12 interview edits and 24 ablation edits; 36/36 exact; 24/24 ablations environment-valid.
- **Counterfactual:** If model explanations were verdicts, larger or less explicit tasks should have raised Surgeon adoption. Both moved the wrong way.
- **Surprise:** Claude routed native 4/4 once forced inspection disappeared; the original caller differential collapsed before ablation.
- **Falsifier:** A product-returned prepared request that fails to lift adoption by 25 points kills transfer from the prompt-embedded treatment.
- **Decision:** Preserve the two negative results. Advance only the prepared-request mechanism to a product-side, no-prompt-cue test.
- **What becomes cheaper next:** The next experiment needs no new fixture, scorer, or reason interview—only a result-contract intervention and matched control.

## Evidence boundary

The bounded private receipt is retained at:

```text
/private/tmp/clj-surgeon-differential-routing.jV1cVK/differential-routing-receipts-20260829.tar.gz
SHA-256 25c5cb7bed6f5ae3d53bcdfd5447749463557911ed6a6b5c8cc93b2f0bfc4516
```

It contains both preregistrations, three repair addenda, prompts, harnesses, preflights, all process ledgers, all 36 run scores, raw caller streams, server receipts, exact final files, independent tests, reason coding, aggregate decisions, and the post-cohort timestamp disclosure. Raw paths, source bodies, session identifiers, and tool payloads remain in that private receipt and are not copied elsewhere.

Key public-safe hashes:

- interview preregistration: `c9eab6d9a376d36550737e40f909188434a0534d2387e6e80d7c2939626034b7`
- ablation preregistration: `11d9d98b15a2ded5dbe3a6a0c21ba41129acb23d2e40c21232d0c7206c6dc948`
- reason coding: `c5d8805a2de73e9f6ea11de76e1ed01cbb60eff1e8b3bc3a615472e8b5eb29fb`
- current interview aggregate: `0365b97832d51253c4802c0eaccd69e35c3e769900d16f90ceceb018d7d59d56`
- current ablation aggregate: `2c6477d6727ff1f0830da0ecba909a050918ac682f212c7f8d9b3b0fa4764e84`

The result proves one causal route movement for one model, task, client version, and prompt-embedded prepared request. It does not prove general adoption lift, isolate salience from assembly effort, or transfer to production traffic.
