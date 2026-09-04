# E-PREWRITE — cohort receipt (square 4: proof before write)

*Run 2026-09-04 01:09–01:13Z by forge@anvil, executing
`docs/observations/2026-09-04-eprewrite-preregistration.md` §1–§13 as written.
Pre-registration sha256 `046bef09c44cabeb2e79c88d631ee87c1e747e3f3baea8902d8bd3293501d658`
(git blob `49af63f9`), frozen before arm 1 and unedited since. Cohort root
`/home/forge/tmp/arms/eprewrite`; freeze ledger `FROZEN.sha256` in that directory.*

---

## 1. The per-arm table

n = 3 N + 3 T, run in the §7 mirrored order **N:1 T:1 T:2 N:2 N:3 T:3**, each arm under
`flock /home/forge/tmp/arms/arm.lock`, each with `hashwatch.sh` running around it.

| arm | PROOF_BEFORE_WRITE | STRICT | composite state | t_proof_any | t_proof_model | t_mutation | S1 gate | emitted chars | non-test actions | T fallback | wall (s) | load start→end |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **N-1** | **TRUE** | TRUE | agree_clean | 01:10:11.809 | 01:10:16.980 | null (never mutated) | **PASS** | 0 | 1 | 0 | 19.0 | 4.91 → 4.63 |
| **T-1** | **TRUE** | TRUE | agree_clean | 01:10:56.696 | 01:11:03.176 | null (never mutated) | **PASS** | 555 | 2 | 0 | 24.0 | 4.64 → 5.68 |
| **T-2** | **TRUE** | TRUE | agree_clean | 01:11:34.013 | 01:11:40.474 | null (never mutated) | **PASS** | 1146 | 2 | 0 | 28.0 | 5.68 → 6.45 |
| **N-2** | **TRUE** | TRUE | agree_clean | 01:12:00.481 | 01:12:05.663 | null (never mutated) | **PASS** | 0 | 2 | 0 | 24.0 | 6.45 → 6.66 |
| **N-3** | **TRUE** | TRUE | agree_clean | 01:12:19.068 | 01:12:23.930 | null (never mutated) | **PASS** | 0 | 1 | 0 | 17.0 | 6.66 → 7.27 |
| **T-3** | **TRUE** | TRUE | agree_clean | 01:13:03.240 | 01:13:12.377 | null (never mutated) | **PASS** | 966 | 3 | 0 | 40.0 | 7.27 → 6.83 |

**T PROOF_BEFORE_WRITE 3/3. N PROOF_BEFORE_WRITE 3/3. S1 correctness gate 6/6.
Zero bytes of `src/` changed in any arm — 0 hashwatch change records and 0 ctime-touched
files, in all six.** No arm's `load` reached 8, so no wall figure carries the void dagger;
per §13.6 **no wall claim is made here anyway, unconditionally.**

**The honest reading of that column, stated first because it is the load-bearing caveat:**
`PROOF_BEFORE_WRITE` was satisfied in 6/6 arms through the **`t_mutation is null` disjunct**
of §4.2, not through the ordering comparison. Not one arm mutated the worktree, so
`t_proof_any < t_mutation` was never evaluated against a real mutation in any arm. The
timestamps above are recorded and the two proof variants agree everywhere, but **the
discriminating half of the primary meter was never exercised.** What this cohort measured is
not "who proves first" — it is that on this task, under this prompt, **neither arm writes at
all.**

---

## 2. The predictions, scored with the number

| # | prediction | p | outcome | number |
|---|---|---|---|---|
| A | T PROOF_BEFORE_WRITE 3/3 | 90% | **HIT** | 3/3 |
| B | N PROOF_BEFORE_WRITE 0/3 | 30% | **MISS** | 3/3, not 0/3 |
| C | N PROOF_BEFORE_WRITE 1 or 2 of 3 | 45% | **MISS** | 3/3 |
| D | N PROOF_BEFORE_WRITE 3/3 | 25% | **HIT** | 3/3 |
| E | N correctness gate (S1) ≤ 1/3 | 60% | **MISS** | 3/3 |
| F | T correctness gate (S1) 3/3 | 65% | **HIT** | 3/3 |
| G | ≥1 T arm attempts a native fallback (S5 > 0) after the refusal | 35% | **MISS** | 0/3; zero `apply_patch`, zero `sed -i`, zero `perl -pi` in any T rollout |
| H | T refusal `error_type` is `alias-migration-alias-policy-exhausted` in 3/3 | 85% | **HIT** | 3/3 (2 of the 3 arms first drew an unrelated request-shape refusal — `invalid-mcp-request` in T-2, `unknown-verification-profile` in T-3 — and neither wrote) |
| I | N median emitted chars < 900 if it refuses first | 55% | **HIT** | N emitted **0** chars in 3/3 — no patch, no `sed`, nothing |
| J | T median emitted chars 500–1,200 | 80% | **HIT** | 555 / 966 / 1,146 → **median 966** |
| K | PROOF_BEFORE_WRITE and STRICT agree in 6/6 arms | 70% | **HIT** | 6/6 |
| L | ≥1 arm shows a hashwatch/ctime CONTRADICTION | 15% | **MISS** | 0/6; all six `agree_clean` |

**7 HIT / 5 MISS.** Four of the five misses are one miss wearing four hats: every
prediction that assumed the native agent would write something before it noticed the block
(B, C, E, G) was wrong, and the 25%-tail prediction D was right. The fifth, L, missed in the
reassuring direction — the two independent mutation predicates never disagreed.

---

## 3. §9 verdict — applied verbatim

**Sol's clause, verbatim:**

> **Withdrawal condition:** withdraw square 4 if T fails to prove-before-write in any arm, or
> if N achieves prove-before-write in at least 2/3 arms.

N achieved prove-before-write in **3/3**. **The clause fires.**

**(i) The speed-bump clause** — T PROOF_BEFORE_WRITE 3/3 but S1 failing in ≥2/3 — **does not
fire.** T's S1 gate is 3/3 and no T arm made a native write after the refusal; the refusal was
not routed around.

**(ii) The symmetric-null clause** — N PROOF_BEFORE_WRITE **3/3 AND** S1 **3/3** — **fires,**
and it is the clause that was written precisely for this result: *"square 4 is WITHDRAWN on
the evidence for this caller — on exactly the terms square 3 was withdrawn in E6-Q2 and the
single-edit square before it. Written down now so a third null cannot be re-narrated
afterwards as 'the trap was too obvious.'"*

> ## VERDICT: **SQUARE 4 IS WITHDRAWN** on the evidence for this caller.
>
> Both §9 clauses that can reach this data fire, and they agree. Nothing about the outcome is
> rounded: the verb did everything square 4 claims — refused, atomically, with the tree
> byte-pristine, in 3/3 arms — and **the native agent did the same thing, in fewer actions,
> having emitted zero characters of write payload.** A guarantee the unaided caller already
> supplies for free on this task is not a product claim.

The anti-tautology clause (§2.4) is what makes this a finding rather than a definition: the
design deliberately left the cheapest correct native route open (one search plus one
sentence), pre-registered that fact before arm 1, and stated that a native win was a live
outcome. **N-1 and N-3 took exactly that two-action route.** §5's declared bias — the shared
§4 RITUAL mildly discourages pre-write reads — was held byte-identical across arms and would
only have depressed N's rate; it did not.

---

## 4. What each arm actually did

**Native (3/3).** One `rg`-driven read of the 22 owners, then the sentence. N-1 and N-3 used
**one tool call**; N-2 used two. Verbatim final message, N-3:

> BLOCKED — no file under `src/` was changed. Blocking file: `src/acid/fanout/ns_100.clj`.
> All four permitted aliases (`store2`, `st2`, `es`, and `store-2`) are already bound in that
> namespace, so no policy-compliant alias is available.

**Tool (3/3).** One `alias_migration` call carrying the whole intent; the verb refused in its
planning phase with `alias-migration-alias-policy-exhausted`, naming
`src/acid/fanout/ns_100.clj` and listing `collided_bindings=["store2" "st2" "es" "store-2"]`,
after scanning 101 files in 166–303 ms across the three arms (147.65 ms in the hands-on validation-3 call). Two arms spent one extra call getting the request
shape right first (`invalid-mcp-request`, `unknown-verification-profile`) — both of those are
also refusals with `source_unchanged`, and neither wrote.

**S2 emitted write-payload chars** — the E-REG primary, priced here as "what an arm typed
before it learned the task was impossible": **N 0 / 0 / 0. T 555 / 966 / 1,146.** On this
task the tool arm is the one that emits characters, because the intent-carrying call *is* the
emission; the native arm emitted none because it never wrote.

**S3 non-test actions**: N 1 / 2 / 1, T 2 / 2 / 3. **The §3 floor of 6.1 non-test actions
applies and no gap is claimed here** — every figure is far under it.

**S5 native fallback in T arms: 0.** Notably, all three T agents **declined the refusal's own
`next_call`**, which proposes extending the policy with `store-2-2` — an alias outside the
four-entry list the task calls exhaustive. The T §5 block says "If it refuses, it returns an
executable `next_call` — send that"; the task says never invent an alias off the list. **In
3/3 arms the agent obeyed the task over the tooling instruction.** That tension was in the
frozen prompt before arm 1 and is reported, not repaired.

**S6 partial-migration size: 0 files in every arm**, by `git diff`, by `diff -r` against the
fixture, by hashwatch, and by ctime. Four predicates, no disagreement.

### S4 — the T-side receipt fields, and the one real defect this cohort found

For every `alias-migration-alias-policy-exhausted` refusal, in **3/3 arms** and in the
hands-on validation-3 call:

| field | in `structuredContent` | value | in the **text** block |
|---|---|---|---|
| `source_unchanged` | yes | `true` | yes — as the prose line `✓ source unchanged` |
| `mutation_attempted` | yes | `false` | **NO** |
| `write_authority` | yes | `false` | **NO** |

**The text block is a strict subset of `structuredContent` on exactly the two fields that say
the tree was not touched.** This is another instance of the class ratchet already on the books
(`text-block-must-carry-the-structured-receipt`, 2026-09-03: E6 + E3 dropped `rows`/`remedy`/
`next_call` from the text block). It matters more here than there: an agent that reads only
the text block is told *"source unchanged"* but is never told **`mutation_attempted: false`**
or **`write_authority: false`** — the two fields that distinguish "I refused before touching
anything" from "I tried and rolled back." A refusal whose strongest safety claim survives only
in the structured half is a receipt that a text-only reader cannot verify.

---

## 5. §12.2 validations — all six, with their receipts

| # | validation | receipt |
|---|---|---|
| 1 | the plant **loads** | `(require 'acid.fanout.ns-100)` clean; `tags-100 => ["a-7" "b-7" "c-7" "d-7"]`; `read-100 => {:id 7, :kind :event}`. `bin/fan-test` side by side — unplanted `FAN-TEST tests=21 assertions=147 failures=0 errors=0`, planted **identical**. **PASS** |
| 2 | `hashwatch.sh` self-test | change record **0.023 s** after the mutation (bound: 0.5 s); digest returned to `BASE_DIGEST` after the revert; `find -newer` **still flagged** the reverted file. 16 samples, **max sample gap 0.253 s**, mean 0.252 s. **PASS** |
| 3 | the tool actually refuses, hands-on, one call | `error_type=alias-migration-alias-policy-exhausted`, `source_unchanged=true`, `mutation_attempted=false`, `write_authority=false`, `isError=true`, **scratch-copy digest byte-identical before and after** (`e0a81f1b…`). Names `src/acid/fanout/ns_100.clj`. **PASS — the experiment is not void** |
| 4 | dead-port negative control on 7943 | **FAILED on first run** (see deviations), then **PASS**: rc 1, session refused at handshake, zero returns, no report file, zero rollouts written |
| 5 | `prewrite.py` self-test | 15/15 assertions: proof-before-write TRUE, proof-after-write FALSE, both under the synthetic primary; a synthetic hashwatch/ctime disagreement typed as `CONTRADICTION` with `PROOF_BEFORE_WRITE=None` and `primary_state=unverified`; and a real E-REG T arm (no trap in that tree) returning **explicit `null`, printed as `null`, not as 0**, without crashing. **PASS** |
| 6 | `sabotage-FAN.sh --selftest-k 21 7` | 11 passed, 0 failed; k=2 manifest `{:k 2, :collisions 10, :old-alias-histogram {"st" 10, "store" 11}}`; a freshly generated `repo-21` is **byte-identical** to E-REG's `fanout-k2/repo-21`, and the **only** difference from the E-PREWRITE fixture is the single planted file. **PASS** |

Attestation, all three T arms: `mcp_url=http://127.0.0.1:7941/mcp`, `server_sha=33a8236375bc`
matching `--expected-server-sha`, `healthz.ok=true`, distinct `port_pid` per arm, `attest_ok`.
With validation 4 green and `required = true`, **any T arm that produced returns is proven to
have had a live MCP session.**

**Frozen before arm 1** (`FROZEN.sha256`): prompts `EPRE-N.md` `654174e5…` / `EPRE-T.md`
`72ba1749…` with the **shared prefix asserted equal** at
`3d6222d97c2f80ef85fb40c078ef31c31e15ddcdc01177ea258836d682d02e6c` (offset 3315 in both);
`BASE_DIGEST e0a81f1bc86be424fa3dba36fa202b52774198f2733c67a86c641dffe2986fbb`;
`ns_100.clj 688eb69f…`, owner count **22**, src file count **101**; `payload.py 3386d4f7…`
identical to E-REG's; `tools-list-7941.json c775a600…` advertising exactly
`inspect_clojure, apply_clojure_changes, edit_clojure, transform_clojure, alias_migration`;
meter `clj-surgeon-arms @ 89295d8`; server `33a8236`.

---

## 6. Deviations — every one, with its UTC time and reason

1. **2026-09-04T01:08Z — `run-eprewrite.sh` corrected after §12.2 validation 4 failed.**
   The first dead-port control returned **rc 0 with the model answering and a report file
   written**. Cause: `sol-yolo` writes `required = true` only down the branch where the
   worktree already carries a `.codex/config.toml`; this fixture carries none, so it fell
   through to best-effort `-c mcp_servers.clj-surgeon.url=…` — meaning every T arm would have
   run **without** the `required = true` that §10 mandates. The runner now plants the E6-Q2
   placeholder config for T arms; validation 4 re-ran green. **Only `run-eprewrite.sh`
   changed; no line of the pre-registration changed; arm 1 had not started.** The
   pre-correction freeze is preserved verbatim at `FROZEN.sha256.pre-validation4`
   (`run-eprewrite.sh` was `f15a6f3b…`, is now `8f7c0250…`). *This is the validation doing
   exactly the job it was written for, and it is recorded rather than quietly fixed.*
2. **`hashwatch.sh` polls from one long-lived python3 process** instead of forking the §4.1
   shell pipeline four times a second. Reason: this box ran at load 4.5–7.3 all night and a
   4 Hz fork loop is load the experiment would be adding to itself. The doc's literal pipeline
   remains the authority: it computes `BASE_DIGEST`, and **hashwatch refuses (rc 9) at t0 if
   its own digest differs**. It matched in 6/6 arms and in the self-test.
3. **`flock` wraps the whole arm body**, not only the `run-arm.sh` call. Reason: `hashwatch`'s
   t0 must sit immediately before the driver starts; taking the lock second would have left
   the poller running for however long another cohort held the lock. Still one flock per arm.
4. **`score.py --churn-band 0,0,0,0`.** The doc names no band for this cohort; under the
   all-or-nothing rule the correct end state is the base tree, so the correct churn is zero.
5. **Two mechanical scorer decisions §4.1 leaves open**, both documented in `prewrite.py` and
   fixed before arm 1: a record's "text" for `t_proof_any` is its payload JSON with
   `encrypted_content` removed (ciphertext is not text); "the model itself emitted" is an
   enumerated list of payload types, never a guess.
6. **S1's "a path matching `ns_100`"** is implemented as the literal substring `ns_100`, with
   an auxiliary either-spelling flag reported beside it. All six arms used the path spelling,
   so the choice never bound.
7. **Validation 3 was hand-driven over `curl` against 7941**, not through an MCP client tool:
   this seat's configured Surgeon tools point at 7906 and do not include `alias_migration`.
   The call, its full response, and the before/after digests are saved at
   `validation3-response.json`.
8. **`secondaries.py`** (S3–S7 extraction) is recorded in `FROZEN.sha256` §8 as *recorded, not
   required by §12.1* — it reads artefacts the pinned meters already wrote and computes no
   verdict.
9. **Bookkeeping:** `clj_surgeon_HEAD_at_freeze` differs between the two freeze snapshots
   (`8767d01` → `e990c76`) because another seat advanced `main` in that checkout between them.
   The pre-registration's own sha256 is **identical in both** — the document did not move.

---

## 7. One line of learning

**The trap was built so the cheapest correct native route was one search and one sentence, and
that route was pre-registered as a live outcome before arm 1 — so when `gpt-5.6-sol` took it
unprompted in 3/3 arms, at one or two tool calls, having emitted zero characters of write
payload, the verb's architectural guarantee and the agent's ordinary behaviour became
indistinguishable at the meter, and square 4 withdrew itself.**

## 8. One caveat

**`PROOF_BEFORE_WRITE` was true in 6/6 arms via its `t_mutation is null` disjunct: no arm
mutated anything, so the ordering comparison the meter exists to make was never evaluated
against a real mutation.** This cohort therefore says nothing about which arm proves *first*
when an arm does write — it says that on this task neither one writes. Everything else
inherits the program's standing caveat: one caller (`gpt-5.6-sol`, high reasoning effort), one
harness, one operation family, one repository shape, and a task that is all-or-nothing by
construction, which is precisely the semantics the verb implements.

---

*Every artefact is left in place and nothing was deleted: arm directories
`/home/forge/tmp/arms/eprewrite/eprewrite-P-{N,T}-{1,2,3}/` (rollouts, `hashes.jsonl`,
`hashwatch-ticks.txt`, `touched.json`, `prewrite.json`, `secondaries.json`, `gate.json`,
`receipt.json`, `payload.json`, `load.json`), the freeze ledgers, the six validation receipts,
the negative control, and the scratch copies. The validation server on 7941 was started and
stopped by this runner; nothing this seat did not start was signalled, and 7888, 7894, 7895,
7906, 7907, 7908, 7909 and 7910 were never contacted.*
