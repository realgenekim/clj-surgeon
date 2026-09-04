# E-GATE-R (square 1, replay half) and the chars/s-vs-load regression — two zero-arm-run analyses

*forge@anvil, 2026-09-04T01:39Z. Both halves are replays over frozen bytes: **zero arm-runs, no cohort
arms, no `arm.lock`, no server started, no port opened, no Surgeon MCP tool called** (the
admission verb is the subject and was invoked as a library function in one JVM). Designs of
record: `2026-09-03-brainfleet-hills.md` §29 §2 (E-GATE-R, Opus, 01:27Z) and §29 §1's
"Characters are not seconds" clause plus its §5 free item. Pre-registration written before any
predicate was computed: `/home/forge/tmp/arms/egater/preregistration.md`,
sha256 `bb6df4332dec474e4d8c1adf888a2f8fd1f7d6fade5ca251c008ad0ec0234e74`. Inputs under
`/home/forge/tmp/arms/{ereg,eafford,eprewrite,eceiling80}` were read only and are untouched.*

---

## Headline, before anything else

| | result |
|---|---|
| **E-GATE-R differential** | **0 of 14.** Withdrawal **W1 fires**: square 1's DETECTION claim is withdrawn for this caller and task family; E-GATE-D does not run on this evidence; the gate is re-scoped below to a coverage/compliance build. |
| **E-GATE-R shape-refusals** | **0 of 14.** W2 does **not** fire — the verb accepted 14 real unified diffs, 21 files each, and computed form identity on all of them. The E1 grammar scar did not recur. |
| **A defect found anyway, not covered by W2** | **14 of 14: the gate's `clj-kondo` half never ran.** `verification_status = "unverified"`, reason `clj-kondo-unavailable`, because the analyzer's EDN output (11,999–21,883 bytes) exceeds `exact-verification-visible-bytes = 12000` and is truncated before `edn/read-string` can parse it. On this corpus the margin is decided by the digit-count of a temp-directory name. Filed as a defect. |
| **chars/s vs load** | **H0 SUPPORTED on the slopes**: native **−0.31** and tool **+0.77** chars/s per load unit across a **3.58 → 17.75** mean-load range (n=17 native, n=16 tool); Spearman rho −0.036 and +0.188. **The pre-registered native band 130–160 MISSED** (3 of 17). Verdict below: the single-band conversion is **struck**; a **per-emission-mode** conversion survives and is now measured over a 5× wider load range than §29 §1 claimed. |

---

# PART A — E-GATE-R

## What was replayed, and how

**The verb.** `admit_clojure_patch` exists only on `origin/bridge/admit-gate` at `17125fe`
(`src/clj_surgeon/mcp_admit_tool.clj`, 1,688 lines); it is **not on main**. It ships **no CLI**.
Its MCP entrance `handle-admit-clojure-patch` is a thin wrapper over `execute-request!`, so the
replay called `clj-surgeon.mcp-admit-tool/execute-request!` directly in **one JVM** over a
`git archive` of that branch — the entrance that replays a patch over a frozen tree without a
live agent. Config `{:project-root <pre-tree>}` only: **no stubbed runners**, so the real
`default-lint-runner` (clj-kondo pre/post finding delta) and real `default-test-runner` were in
play. Call: `{:patch <diff.patch verbatim> :mode "preview" :verify "focused"}`. `mode=preview`
never writes; every receipt carries `mutation_attempted false`, `committed false`,
`source-unchanged true`.

**The corpus**, exactly as pre-registered in §29 §2: the 14 frozen native final-state patches —
E-REG 8 (`ereg-k{1,2,3,6}-N-{1,2}`) + E-AFFORD 6 (`eafford-{Nd,Ns}-N-{1,2,3}`), all 6/6 and
byte-identical to canonical. E-PREWRITE's 3 native arms (wrote nothing) and E6-Q2 (read-only)
are excluded, and that exclusion was pre-registered in the design.

**Patch format — the conversion clause did not fire.** Each `diff.patch` on disk is already
git-format unified diff (`diff --git` / `index` / `---`/`+++` / `@@`), produced by `git diff` in
the arm's worktree. The model emitted apply_patch/V4A *during* the arm; the frozen final-state
artefact is unified diff. **No conversion step was performed and none was needed** — so no
recorded conversion is claimed. Had the verb refused this text on grammar grounds it would have
counted as a shape-refusal; it did not.

**The trees.** For each arm: `post` = `cp -a` of the arm's worktree unchanged (its final state);
`pre` = the same copy with `git checkout -- .` run **inside the copy**. Verified per arm:
`git status --porcelain` empty in `pre`, `HEAD` = `base.sha`, and `git diff | wc -c` in `post`
equal to `stat -c%s diff.patch` — 16,878 / 17,054 / 17,017 / 17,177 / 16,878 bytes respectively,
14 for 14. Nothing under the arm directories was written.

## The 14 × 3 table

`differential` = substantive-catch AND NOT named by `clj-kondo --lint` AND NOT named by
`bin/fan-test`.

| # | arm | P1 gate class (as shipped) | P1 gate class (cap lifted, post-hoc) | gate hazards | P2 kondo post / pre (Δ) | P2 finding types | P3 `bin/fan-test` | differential |
|---:|---|---|---|---|---|---|---|:-:|
| 1 | ereg-k1-N-1 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 2 | ereg-k1-N-2 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 3 | ereg-k2-N-1 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 54 / 54 (Δ0) | duplicate-require, unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 4 | ereg-k2-N-2 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 54 / 54 (Δ0) | duplicate-require, unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 5 | ereg-k3-N-1 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 65 / 65 (Δ0) | duplicate-require, unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 6 | ereg-k3-N-2 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 65 / 65 (Δ0) | duplicate-require, unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 7 | ereg-k6-N-1 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 79 / 79 (Δ0) | duplicate-require, unused-binding, unused-namespace, unused-referred-var | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 8 | ereg-k6-N-2 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 79 / 79 (Δ0) | duplicate-require, unused-binding, unused-namespace, unused-referred-var | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 9 | eafford-Nd-N-1 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 10 | eafford-Nd-N-2 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 11 | eafford-Nd-N-3 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 12 | eafford-Ns-N-1 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 13 | eafford-Ns-N-2 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |
| 14 | eafford-Ns-N-3 | clean-structural / `:unverified` | clean | 21 `require-removed`, all class `note` | 44 / 44 (Δ0) | unused-binding, unused-namespace | PASS — tests=21 assertions=147 failures=0 errors=0 | **no** |

Totals: **shape-refusals 0/14 · substantive-catches 0/14 · differential 0/14 ·
kondo Δ 0 in all 14 · `bin/fan-test` 14/14 green (147 assertions each) · 294 hazards, every one
of them class `note`.**

## The receipts, verbatim

The as-shipped receipt (`ereg-k1-N-1`; the other 13 differ only in file hashes and owner names):

```json
{"ok": true,
 "operation": "admit-patch-preview",
 "verification_complete": false,
 "verification_status": "unverified",
 "verification_reasons": ["clj-kondo-unavailable", "no-focused-test-profile"],
 "byte_drift_outside_hunks": 0,
 "protected_node_drift": {},
 "mutation_attempted": false,
 "committed": false,
 "source-unchanged": true,
 "lint_delta": {"ran": false, "ok": false, "status": "unverified",
                "error-type": "clj-kondo-unavailable",
                "error": "clj-kondo did not produce readable findings"},
 "tests": {"ran": false, "reason": "no-focused-test-profile",
           "profile_absent": true, "profile_source": "none",
           "profile_source_namespaces": "path-convention",
           "tests-run": 0, "passed": 0, "failed": 0, "skipped": 0,
           "namespaces": [], "focused_namespaces": {}}}
```

The only hazard class the gate ever emitted on this corpus — 294 instances, all identical in
shape, all **`"class": "note"`**, i.e. explicitly admitted rather than blocking:

```json
{"type": "require-removed",
 "file": "src/acid/fanout/ns_003.clj",
 "owner": "acid.fanout.ns-003",
 "span": [1, 5],
 "class": "note",
 "message": "The ns form no longer requires acid.fanout.store; admitted as a dead-require removal -- the patched image references it nowhere, by alias, fully qualified name, or referred symbol",
 "libraries": ["acid.fanout.store"],
 "reference-sites": []}
```

That message is the gate at its best: it noticed 21 removed requires, proved each one dead
against the patched image, and **declined to refuse**. It is the correct behaviour and it is
also, on this corpus, the entire output.

## The defect: `clj-kondo-unavailable` on 14 of 14

**The as-shipped replay produced a null that no instrument had taken a reading for.** Both of
the gate's substantive detectors were off:

- **tests** — pre-registered and expected: these fixture trees ship no
  `.clj-surgeon/focused-test.edn`, so `resolve-focused-test` returns nil and the runner returns
  `:no-focused-test-profile`. Predicate 3 covers that surface independently, which is what the
  three-predicate design is for. No profile was invented; fabricating one the fixture never
  shipped would have been a fabricated input.
- **lint** — **not expected, and a defect.** `default-lint-runner` materializes the pre and post
  images into temp trees, lints them with
  `clj-kondo --lint {files} --cache false --config {:output {:format :edn}}` through
  `mcp-change-buffer/run-process!`, and parses the EDN. `run-process!` passes
  `:visible-byte-limit exact-verification-visible-bytes`, **12,000**
  (`src/clj_surgeon/mcp_change_buffer.clj:29`). A 21-file fan-out image emits more EDN than that,
  the output comes back `:output-truncated true`, `edn/read-string` fails, and the runner returns
  `{:ok false :error-type :clj-kondo-unavailable}` — which makes `(and (:ok pre) (:ok post))`
  false and the whole delta `:unverified`.

Measured directly, with the gate's own temp-directory prefixes, both images per arm:

| arm | pre image bytes | truncated | parses | post image bytes | truncated | parses |
|---|---:|:-:|:-:|---:|:-:|:-:|
| ereg-k1-N-1 | 11,999 | no | yes | 12,043 | **yes** | no |
| ereg-k1-N-2 | 12,043 | **yes** | no | 12,043 | **yes** | no |
| ereg-k2-N-1 | 14,768 | **yes** | no | 14,822 | **yes** | no |
| ereg-k2-N-2 | 14,768 | **yes** | no | 14,822 | **yes** | no |
| ereg-k3-N-1 | 17,853 | **yes** | no | 17,918 | **yes** | no |
| ereg-k3-N-2 | 17,788 | **yes** | no | 17,918 | **yes** | no |
| ereg-k6-N-1 | 21,804 | **yes** | no | 21,725 | **yes** | no |
| ereg-k6-N-2 | 21,883 | **yes** | no | 21,804 | **yes** | no |
| eafford-Nd-N-1 | 12,043 | **yes** | no | 12,043 | **yes** | no |
| eafford-Nd-N-2 | 12,043 | **yes** | no | 12,087 | **yes** | no |
| eafford-Nd-N-3 | 12,043 | **yes** | no | 12,087 | **yes** | no |
| eafford-Ns-N-1 | 11,999 | no | yes | 12,043 | **yes** | no |
| eafford-Ns-N-2 | 11,999 | no | yes | 12,087 | **yes** | no |
| eafford-Ns-N-3 | 12,043 | **yes** | no | 12,087 | **yes** | no |

**Read the 11,999 / 12,043 / 12,087 column carefully.** Those are 44-byte steps, and 44 is the
number of kondo findings in a k=1 fixture: one extra digit in
`Files/createTempDirectory("clj-surgeon-admit-pre", …)`'s random suffix lengthens every
`:filename` in the EDN by one character. **Whether the gate verifies a k=1 patch at all is
currently decided by how many digits the JVM happened to put in a temp-directory name.** At k=2
and above the margin is gone and it fails every time.

This is the class the house rules call *a verifier blind to its own subject*: the receipt said
`ok: true` while the detector that produces its substance had not run. It did **not** lie —
`verification_status` was honestly `"unverified"` and `verification_complete` was `false`, which
is the gate's design working — but a reader scoring "did the gate catch anything?" against
`ok`/`hazards` alone would have recorded a clean null. **Filed as a defect** (see Ratchets).

## The post-hoc replay: making the null informative

Because a null from an instrument that never took a reading settles nothing, a **second,
explicitly not-pre-registered** replay was run: identical in every respect except
`(alter-var-root #'cb/exact-verification-visible-bytes (constantly 4000000))` in the replay JVM,
so the lint half could execute. **No branch source was modified.**

With the cap lifted, `lint_delta` ran on all 14. Verbatim, `ereg-k6-N-1` (the largest baseline):

```json
{"ran": true, "ok": true,
 "baseline-count": 79, "future-count": 79, "unchanged-count": 79,
 "introduced-count": 0, "removed-count": 0,
 "blocking-introduced": [], "blocking-introduced-count": 0}
```

**0 introduced, 0 removed, 0 blocking, in all 14** — independently corroborated by predicate 2,
which measured the same 44 → 44, 54 → 54, 65 → 65, 79 → 79 with a bare `clj-kondo --lint` outside
the gate. Hazards stayed 294/294 class `note`. `byte_drift_outside_hunks` 0 and
`protected_node_drift` `{}` on every arm.

So the null is now a real reading: **with its substantive surface live, the gate names nothing on
14 native patches that kondo and the suite also do not name.** That is the answer square 1
needed, and it is Opus's predicted answer for Opus's predicted reason — this caller's patches
are correct.

## Predictions scored

| id | prediction (Opus, §29 §2, on record before replay 1) | p | measured | verdict |
|---|---|---:|---|---|
| P1 | shape-refusal ≤ 1 of 14 | 60% | **0 of 14** | **HIT** |
| P2 | substantive catches 0 of 14 | 70% | **0 of 14** | **HIT** |
| P3 | `differential` = 0 of 14 | 85% | **0 of 14** | **HIT** |

3 for 3. **The caveat that must travel with P2 and P3:** on the as-shipped replay they were
satisfied trivially, because neither substantive detector executed. They survive as real results
only on the strength of the post-hoc cap-lifted replay and predicate 2's independent corroboration.
Quoted without that sentence, they would be exactly the "tidy board" failure the house rules name.

## Withdrawal applied

**W1 fires.** `differential` = 0 of 14. Therefore, in this same document:

> **Square 1's DETECTION claim is WITHDRAWN for this caller (`gpt-5.6-sol` at high reasoning
> effort, writing `apply_patch`) and this task family (21-owner alias migration on the
> `fanout-k*` fixtures). E-GATE-D does not run on this evidence.** The measured defect base rate
> on this program's task family is zero — 14/14 byte-identical, 14/14 suite-green, 14/14 kondo-Δ-0
> — and a differential detector cannot have measurable value against a zero base rate. This is a
> structural fact about the corpus, not a tuning problem.
>
> **The gate is re-scoped to a coverage/compliance build.** Its claim from here is Hill 4's:
> *100% of `.clj` writes pass through it*, with `z7c`'s wall-neutrality (0.975×, Welch p 0.79,
> n=9 vs 12, gate 7.4% of wall) as the affordability receipt. **That is a build result, not a
> vs-native square, and must never be reported as one.**

**W2 does not fire.** Shape-refusals are 0 of 14, not ≥ 2. The dual-grammar work of round five
holds: 14 real unified diffs, 21 files each, all parsed, form identity computed, byte drift 0.
The E1 scar (69% of admit calls refused on patch grammar) did not recur on this corpus.

**But "W2 did not fire" is not "merge-ready."** The `clj-kondo-unavailable` defect above is a
separate, deterministic, load-immune failure of the gate's own substantive surface on every
patch in the only corpus it has been replayed against, and the tech tree's status line for this
verb ("BUILT but LOST E1 …; not ready for merge") stands.

## What may honestly be said about the gate now

- It **accepts** the caller's real output format at fan-out scale. 0 shape-refusals on 14
  patches × 21 files.
- It **reports structure correctly and refuses nothing correct**: 294 dead-require removals
  found, each proved dead against the patched image, each admitted as a note. That is the
  false-refusal failure mode of round one *not* recurring.
- Its **substantive surface has never been shown to add anything over `clj-kondo` plus the
  focused suite** on this caller's patches — and as shipped, on this corpus, it does not even
  execute.
- **"Is the gate just clj-kondo in a hat?"** — the question §29 §2 said a reviewer would ask
  first. Measured answer: on this corpus the gate's *lint* half is clj-kondo (and could not run),
  its *test* half is the repo's own suite (and had no profile), and its *unique* half — form
  identity, owner delta, byte drift, protected-node drift, the hazard set — found **zero**
  blocking findings. Its distinct contribution here is **admitting** correct work with a proof,
  not catching bad work.

---

# PART B — chars/s vs load

## Design

Corpus: **every arm carrying both `payload.json` and `load.json` on disk** — E-REG 16 (the two
`*.VOID-rollout-rotated*` directories excluded, the cohort's own exclusion), E-AFFORD 9,
E-PREWRITE 6, E-CEILING80 5 (all finished) = **36 arms**. `emitted_payload_chars`,
`emission_gap_s` and `chars_per_s` are read verbatim from the cohorts' own additive scorer
(validated against E3-P N-1 to one decimal); nothing is recomputed from rollouts. `mean load` =
`(load_start + load_end) / 2` from `load.json`. **3 arms excluded from the regressions** —
`eprewrite-P-N-{1,2,3}` wrote nothing, so they carry no rate; they appear in the table with an
em dash.

## The scatter, sorted by mean load

| cohort | arm | cell | type | emitted chars | gap s | chars/s | load start | load end | mean load |
|---|---|---|---|---:|---:|---:|---:|---:|---:|
| ereg | ereg-k2-T-3 | k2 | T | 1260 | 7.31 | 172.4 | 3.00 | 4.15 | 3.58 |
| ereg | ereg-k2-N-1 | k2 | N | 9724 | 70.14 | 138.6 | 5.48 | 3.32 | 4.40 |
| ereg | ereg-k2-N-2 | k2 | N | 7377 | 51.61 | 142.9 | 3.32 | 6.22 | 4.77 |
| eprewrite | eprewrite-P-N-1 | P | N | 0 | — | — | 4.91 | 4.63 | 4.77 |
| eprewrite | eprewrite-P-T-1 | P | T | 555 | 3.23 | 172.0 | 4.64 | 5.68 | 5.16 |
| eafford | eafford-Ns-N-2 | Ns | N | 10090 | 71.16 | 141.8 | 5.02 | 6.17 | 5.60 |
| eafford | eafford-Nd-N-2 | Nd | N | 1869 | 12.31 | 151.8 | 6.46 | 5.02 | 5.74 |
| eceiling80 | eceiling80-P-N-1 | P | N | 841 | 4.98 | **168.9** | 5.90 | 6.07 | 5.99 |
| eprewrite | eprewrite-P-T-2 | P | T | 1146 | 6.22 | 184.4 | 5.68 | 6.45 | 6.07 |
| ereg | ereg-k2-T-1 | k2 | T | 1019 | 5.59 | 182.2 | 6.86 | 5.48 | 6.17 |
| ereg | ereg-k1-N-2 | k1 | N | 1929 | 12.53 | 154.0 | 6.00 | 6.86 | 6.43 |
| eprewrite | eprewrite-P-N-2 | P | N | 0 | — | — | 6.45 | 6.66 | 6.56 |
| eafford | eafford-T-T-1 | T | T | 1214 | 7.02 | 172.9 | 6.42 | 6.93 | 6.68 |
| eafford | eafford-Nd-N-1 | Nd | N | 8977 | 63.43 | 141.5 | 5.63 | 8.23 | 6.93 |
| eceiling80 | eceiling80-P-T-1 | P | T | 1094 | 6.29 | 173.8 | 6.35 | 7.53 | 6.94 |
| eprewrite | eprewrite-P-N-3 | P | N | 0 | — | — | 6.66 | 7.27 | 6.97 |
| eprewrite | eprewrite-P-T-3 | P | T | 966 | 5.70 | 169.4 | 7.27 | 6.83 | 7.05 |
| eafford | eafford-Ns-N-1 | Ns | N | 2822 | 20.54 | 137.4 | 7.81 | 6.54 | 7.18 |
| eafford | eafford-T-T-3 | T | T | 1136 | 6.27 | 181.1 | 7.97 | 7.20 | 7.59 |
| ereg | ereg-k6-T-2 | k6 | T | 580 | 3.40 | 170.3 | 8.09 | 7.72 | 7.91 |
| eafford | eafford-Ns-N-3 | Ns | N | 3604 | 24.71 | 145.9 | 8.17 | 7.97 | 8.07 |
| eafford | eafford-T-T-2 | T | T | 549 | 3.32 | 165.2 | 6.48 | 9.87 | 8.18 |
| eceiling80 | eceiling80-P-N-2 | P | N | 722 | 4.44 | **162.6** | 6.45 | 10.08 | 8.27 |
| ereg | ereg-k1-T-2 | k1 | T | 485 | 2.94 | 164.9 | 11.76 | 6.00 | 8.88 |
| ereg | ereg-k3-N-1 | k3 | N | 8202 | 57.90 | 141.7 | 7.70 | 11.05 | 9.38 |
| ereg | ereg-k6-N-2 | k6 | N | 9636 | 70.08 | 137.5 | 10.78 | 8.09 | 9.44 |
| eafford | eafford-Nd-N-3 | Nd | N | 10090 | 70.99 | 142.1 | 9.87 | 9.03 | 9.45 |
| eceiling80 | eceiling80-P-N-3 | P | N | 12576 | 73.55 | **171.0** | 12.49 | 7.23 | 9.86 |
| eceiling80 | eceiling80-P-T-2 | P | T | 1508 | 8.65 | 174.3 | 10.23 | 13.99 | 12.11 |
| ereg | ereg-k1-N-1 | k1 | N | 8594 | 58.46 | 147.0 | 10.38 | 13.90 | 12.14 |
| ereg | ereg-k6-N-1 | k6 | N | 16531 | 116.63 | 141.7 | 13.96 | 10.78 | 12.37 |
| ereg | ereg-k1-T-1 | k1 | T | 1013 | 5.69 | 178.1 | 13.90 | 11.76 | 12.83 |
| ereg | ereg-k3-T-1 | k3 | T | 1067 | 5.86 | 182.1 | 11.05 | 14.63 | 12.84 |
| ereg | ereg-k3-T-2 | k3 | T | 912 | 4.82 | 189.1 | 14.63 | 17.40 | 16.02 |
| ereg | ereg-k6-T-1 | k6 | T | 1075 | 6.00 | 179.0 | 18.10 | 13.96 | 16.03 |
| ereg | ereg-k3-N-2 | k3 | N | 8179 | 57.79 | 141.5 | 17.40 | 18.10 | 17.75 |

## The regression

| arm type | n | chars/s min | max | mean | **slope b** (chars/s per load unit) | intercept | **Spearman rho** |
|---|---:|---:|---:|---:|---:|---:|---:|
| **native** | 17 | 137.4 | 171.0 | 147.5 | **−0.313** | 150.2 | **−0.036** |
| **tool** | 16 | 164.9 | 189.1 | 175.7 | **+0.768** | 168.8 | **+0.188** |

Mean-load range over the regression corpus: **3.58 → 17.75** — a 5× span on a 16-core box,
against the 5.02 → 9.87 that §29 §1 had.

The highest-load arm in the whole corpus (`ereg-k3-N-2`, mean load 17.75) emitted at **141.5
chars/s**; the lowest-load *native* arm (`ereg-k2-N-1`, mean load 4.40) emitted at **138.6**.
**The busiest arm was the faster of the two.**

### The stratification that broke the pre-registered band

| arm type | write kind | n | chars/s min | max | mean | slope b | mean load |
|---|---|---:|---:|---:|---:|---:|---:|
| native | `apply_patch` | 8 | 137.5 | 154.0 | **143.1** | −0.113 | 9.58 |
| native | `literal-patch` | 3 | 141.5 | 142.1 | **141.8** | +0.099 | 7.32 |
| native | `programmatic-generation` | 6 | 137.4 | 171.0 | **156.3** | +2.385 | 7.52 |
| tool | `alias_migration` | 11 | 164.9 | 189.1 | **176.7** | +0.805 | 9.32 |
| tool | `tool-call` | 5 | 165.2 | 181.1 | **173.5** | −0.040 | 8.30 |

**All three native arms outside 130–160 are `programmatic-generation`, and all three are
E-CEILING80** (`P-N-1` 168.9 at 841 chars, `P-N-2` 162.6 at 722 chars, `P-N-3` 171.0 at 12,576
chars). Restricted to Opus's original 25-arm corpus (E-REG + E-AFFORD + E-PREWRITE, i.e.
excluding E-CEILING80), native is **n=14, 137.4–154.0, mean 143.2, slope −0.165** — inside the
pre-registered band with room to spare. The band broke because the corpus was widened to
N=80, exactly where the brief told me to widen it.

**The mechanism the numbers point at:** emission rate tracks *what is being emitted*, not load.
Verbatim transcription of existing source (literal patch hunks) runs at ~137–154 chars/s across
every cohort and every load; novel program text and short structured tool arguments run at
~162–189. Native's rate rises at N=80 because at N=80 native stops transcribing and starts
generating. This is an observed stratification, not a proven mechanism.

## Predictions scored (mine, pre-registered before computing)

| id | prediction | p | measured | verdict |
|---|---|---:|---|---|
| B1 | native slope \|b\| ≤ 5 | 70% | −0.313 | **HIT** |
| B2 | tool slope \|b\| ≤ 5 | 60% | +0.768 | **HIT** |
| B3 | every native arm inside 130–160 | 70% | 3 of 17 outside (162.6, 168.9, 171.0) | **MISS** |
| B4 | every tool arm inside 160–190 | 50% | 16 of 16 inside | **HIT** |
| B5 | \|Spearman rho\| < 0.5, native | 60% | −0.036 | **HIT** |
| B6 | \|Spearman rho\| < 0.5, tool | 50% | +0.188 | **HIT** |

5 of 6. **The one miss is the one I flagged as blind:** the pre-registration disclosed that
B3/B4 were partially informed for 9 of 36 arms and blind for the other 27, and the miss landed
squarely in the blind set (E-CEILING80, whose rates I had never seen).

## Verdict

**The pre-registered verdict rule required BOTH slopes inside ±5 AND both bands to hold. B3
missed, so by the letter of the rule the conversion as written is STRUCK. It is struck, and the
reason is named:**

> **STRUCK:** the single-band native conversion asserted in §29 §1 — *"Native chars/s stayed
> inside 137.4–151.8"* — does not survive the widened corpus. Three native arms emit at
> **162.6–171.0 chars/s**, above the top of that band and inside the *tool* band. Any sentence
> that converts native characters to seconds using one native constant is wrong at N=80, and
> **§29 §1's conversion sentence must not be quoted bare.**

**What survives, and is now measured over a 5× wider load range than the claim it replaces:**

> **H0 is SUPPORTED. Emission rate is load-invariant in the measured band.** Across mean loads
> **3.58 to 17.75**, the least-squares slope of chars/s on load is **−0.31** for native (n=17,
> Spearman rho −0.036) and **+0.77** for tool (n=16, rho +0.188) — both an order of magnitude
> inside the pre-registered ±5 threshold, and both with rank correlations indistinguishable from
> zero. **Emitted characters therefore convert to emission seconds at the measured rate, and
> every character claim may be quoted as an emission-time claim — provided the rate used is the
> one for that emission mode**: literal-patch native **143.1 chars/s** (n=11, 137.5–154.0),
> programmatic native **156.3** (n=6, 137.4–171.0), tool **175.7** (n=16, 164.9–189.1). The
> caveat that travels with every such quote: this is the observed duration of the model's own
> output, **not box wall**, and it is a rate per emission mode, **not one native constant**.

Concretely, for the surviving square-2 headline: E-AFFORD's 549–1,214 tool characters are
**3.1–6.9 emission-seconds** at 175.7 chars/s; its 1,869–10,090 native characters are
**13.1–70.5 emission-seconds** at 143.1 chars/s. The directly measured `emission_gap_s` column above spans
3.32–7.02 s tool and 12.31–71.16 s native, so the conversion reproduces the measured durations
to within a few percent per arm — which is the check that it is not doing any work the receipts
had not already done, and the reason it may be quoted at all.

---

## Ratchets

**R1 — the gate's analyzer output cap (the defect this run found).**
`exact-verification-visible-bytes = 12000` in `mcp_change_buffer.clj:29` is a *display* budget
being used as a *parsing* budget. A truncated EDN document is not a smaller finding set; it is an
unparseable one, and it degrades silently into `:clj-kondo-unavailable`. Ladder, cheapest rung
that fits: (a) a named intent that the analyzer's structured output is parsed from an untruncated
source — read the analyzer's stdout from its file rather than from the visible-byte window, or
give structured verification its own limit sized to the workspace; (b) an example witness that
replays a 21-file fan-out patch and asserts `lint_delta.ran = true`; (c) a **typed refusal** —
if the analyzer's output is truncated, `lint_delta` must say `:analyzer-output-truncated` with
the byte count and the cap, never `:clj-kondo-unavailable`, because the two states have different
fixes and a shared name is how a reader stops looking. Rung (c) is the one that matters: the
existing code already distinguishes "no focused-test profile" from "profile misconfigured" for
exactly this reason (`MCP-OP-ADMIT-118`); the analyzer half has the same hole and no such split.

**R2 — a receipt whose `ok` is true while its detectors are off must be unquotable as a null.**
The gate was honest (`verification_status: "unverified"`) and a scorer reading `ok`/`hazards`
would still have recorded a clean pass. Any future gate cohort scores
`verification_status == "verified"` as a **precondition of the arm**, not as a column — an arm
whose verifier did not run is `:unverified`, never `clean`.

**R3 — a gate cohort states its analyzer and suite preconditions before arm 1.** E-GATE-R's
design named `clj-kondo` and `bin/fan-test` as the comparators but never asserted that the gate's
*own* copies of them would execute on the fixture. Same class as
`ambient-state-is-an-invisible-precondition`: gates own their fixtures or name a PRECONDITION.

**R4 — chars/s is quoted per emission mode, or not at all.** The classifier that already
partitions write kinds (`apply_patch` / `literal-patch` / `programmatic-generation` /
`tool-call` / the planned `minimal-edit`) is now also the **rate** partition. Any report
converting characters to seconds names the mode and its rate; a bare "native chars/s" figure is
struck on sight.

## Provenance

- Pre-registration: `/home/forge/tmp/arms/egater/preregistration.md`
  (sha256 `bb6df4332dec474e4d8c1adf888a2f8fd1f7d6fade5ca251c008ad0ec0234e74`), written before any
  predicate ran.
- Gate source: `git archive origin/bridge/admit-gate` (`17125fe`) → `/home/forge/tmp/arms/egater/gate-src`. Unmodified.
- Replay receipts: `/home/forge/tmp/arms/egater/p1b/*.json` (as shipped),
  `p1c/*.json` (cap lifted, post-hoc). Predicate 2: `p2-kondo.txt`, `p2-kondo-pre.txt`.
  Predicate 3: `p3-fantest.txt`. Truncation probe: `probe4.clj`. Trees + verification:
  `trees/manifest.txt`. Part B: `partb.py`, `partb-rows.json`, `partb-out.txt`, `partb-kinds.txt`.
- Cost: **0 arm-runs, 0 cohort ports, 0 servers started, 0 Surgeon MCP tool calls.** Three short
  JVMs (10 s, 12 s, ~40 s) and 14 `bb` suite runs. Box load 2.9–9.9 throughout; every meter in
  this document is a count, a byte, or a ratio, and none of them is wall.
