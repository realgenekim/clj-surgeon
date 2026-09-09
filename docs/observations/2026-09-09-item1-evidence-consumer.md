# Item 1 — Surgeon/landing evidence consumed by the landing gate

Frame 7 item 1 of Astra's order. Spec: §4–5 of
`/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-frame7-protocol-and-consumer-contract.md`.
One builder. Started 2026-09-09 13:54:42Z; finished inside the 6-hour box.

---

## 1. The headline

| | |
|---|---|
| **What the consumer replaces** | the landing gate: **169–265 s** measured (four real receipts: 169,383 / 170,279 / 171,272 / 265,263 ms) |
| **What the consumer costs** | **3.9 s** end-to-end (3,856 / 3,901 / 3,812 ms) |
| **Net, per eligible clean request** | **≈ +165 s to +261 s faster at the landing** |
| **Marginal cost over v3.6's stage-name check** | **≈ 0.26 s** (`print-gate-obligations` 2.20 s vs `print-gate-stages` 2.02 s; + 0.09 s toolchain + 0.03 s matcher) |
| **Corpus** | ship v3.7's own: **73 rows, 1 mismatch** — the generated sweep is RED (see §1a3). `install.sh` proof from round 2: **INSTALL OK, 11/11**; it would now report NOT PROVEN |
| **Status** | **NOT SHIPPABLE.** Producer side committed and green; consumer side has 810 generated + 5 red-team acceptances still to bind |
| **Replay** | **both** consumed landings of 2026-09-09 would have been **refused** — and one of them consumed a gate that never ran the alias battery |
| **Production decision today** | **`run-required reason=custody-unverified` on every landing.** ship observed nothing independently, so it cannot discharge an obligation. The saving below is what the consumer will buy once item 2's recorder can attest observation; it is **not** being banked today |

Astra's forecast was *central 0–30 s saved per eligible clean request, plausible −10 to +120 s*. The
measured landing-side saving is **above the top of that range**, because the forecast is about a
*duplicated check* and this consumer replaces the whole seven-stage gate on the merged tree with a
four-second derivation-and-match. §5 states the caveat plainly: that saving is real only when ship
ran the gate ahead of the landing, and request-to-landed **L** improves only if that run overlapped
something else (review). If it did not, the work moved earlier and L is unchanged.

---

## 1a. Sol's NO-GO, and what changed (2026-09-09, verdict `20260909T145900Z-1ed995c63dac`)

Sol returned NO-GO on `1ed995c6` with four blockers, all one class: **the consumer could return
`consume` while authority and execution evidence were unknown.** All four are fixed; the honest
consequence is that **the production path now refuses on every landing**, which Sol and §4 both say
is the correct answer today.

| id | the hole | the repair | witnessed by |
|---|---|---|---|
| **SOL-EC-001** | `:custody :self-reported` consumed. A fixture Sol derived with `:inputs-manifest`, `:workspace-snapshot-sha256` and every execution's `:inputs-manifest`, `:environment-manifest`, `:declared-skips`, `:focus-omissions`, `:unexecuted-tests` **removed** returned the same `consume` — because the matcher tested the omission vectors with `seq`, and `(seq nil)` is nil, so a *missing* field read as *no omissions* | consumption requires `:custody :observed` attested by a principal that is not the producer; every listed field must be **present** (presence, not emptiness); `:proof/:complete?` must be an explicit `true` | 14 rows incl. `sol-stripped-provenance` → `provenance-missing`, `self-reported-custody` / `observer-is-the-producer` / `observer-unnamed` / `observer-no-observation-id` → `custody-unverified` |
| **SOL-EC-002** | the fourteen `mcp-test-checks` members were a **second hand-written list**; one passing `mcp-test` exit discharged them all; namespace-only matching could not see a Var that stopped running | members are **derived from the recipe text** the coordinator executes; the consumer requires a per-check execution for each derived member and **Var identities**, expected ⊇ executed | `nested-checks-are-derived-from-the-recipe-not-a-second-list` (repo) + `no-nested-executions`, `nested-check-red`, `missing-var`, `no-vars-evidence` |
| **SOL-EC-003** | `:policy-sha256` omitted the bytes of the Prolog oracle, four Python oracles, eight shell self-tests and every selected test's own implementation, so a weakened rule could be consumed | rule files are **derived from the recipe** (including `-s/-p` unittest discoveries) and from each selected namespace's file. **R4: 2 → 81 declared inputs; R5: 1 → 50**, none absent | `the-policy-hash-covers-the-bytes-of-every-rule-the-recipe-invokes` (repo) |
| **SOL-EC-004** | the receipt claimed nine tools, land observed eight, and the comparison ran only where the observed map already had the key — so `:sh` was compared against nothing; and `:sh`'s "version" was the string `"sh"` from `echo $0`, identical on every box | land observes `:sh` too, **with the resolved path** (`/usr/bin/dash` here); **any key the receipt claims that the observer cannot produce is `toolchain-unobserved`**, never a skip | `the-shell-identity-is-a-resolved-path-not-the-word-sh` (repo) + `extra-tool-claim` |

**A fifth thing fell out of the repair.** Emitting 913 + 891 Var identities inline pushed the
envelope to **230,479 bytes**, and the writer refused — §5: *"larger detailed evidence is
content-addressed by path/digest/byte length … Do not truncate proof to fit."* The Var lists moved to
sidecars beside the envelope and the execution keeps a `:vars-ref` with path, digest, byte length and
counts. The envelope is **23,831 bytes**. The consumer resolves the reference under the envelope's own
directory and refuses on a missing file, a disagreeing digest, a disagreeing length, or a path that
tries to escape the root — four more rows.

**Deviation 7 is now closed the other way.** My §7 said "no live consumption is qualified" while the
consumer would in fact have consumed once installed. It no longer will: the production answer is
`custody-unverified`, and the consume path is proven only against a controlled complete fixture whose
observer attestation ship cannot produce. Sol's audit table entries 1, 2, 3, 4, 7 and 9 are all
addressed; 5, 6 and 8 were already preserved.

## 1a2. Sol round 2 (verdict `20260909T153507Z-45beeb7eaff3`) — NO-GO, both blockers fixed

Sol found two more of the same family and the coordinator's instruction was to fix them as **one
rule**, not two patches: *the consumer validates every provenance VALUE against the independently
derived inventory; anything it cannot validate refuses by name.*

| id | the hole | the repair |
|---|---|---|
| **SOL-EC-005** | `resolve-vars` took the inline branch whenever `:expected-vars`/`:executed-vars` were present and never looked at a simultaneous `:vars-ref`. Sol added **empty inline vectors beside a valid reference** and the consumer authorized the empty census while the real one sat there as decorative bytes | inline and `:vars-ref` are **mutually exclusive** → `vars-ambiguous`; an empty census where the tree selects ≥1 namespace refuses; the reference's advertised `:expected-count`/`:executed-count` must match the sidecar it names |
| **SOL-EC-006** | every required key present but `:inputs-manifest` / `:environment-manifest` replaced by `{}` still consumed — presence was checked, values never were; no `:closure-basis`, no fixture/hook identities, and it consumed anyway | `:inputs-manifest` must **account for the obligation's required inputs by digest**; `:environment-manifest` must account for **every key the inventory's `environment-policy` names** (digested, or explicitly listed `:unset`); `:closure-basis` and `:fixture-identities` are required where the **inventory** names them; every reference field is type-checked; every `:result` value must be a measurement, not `nil`/`{}`/`[]` |

**The ratchet that stops round 4.** Three rounds each found the next field nobody had thought to
strip, because hand-listing negatives is a race against the reviewer's imagination. So the corpus now
**generates** them: `fixtures/gen-field-mutants.clj` walks every authority-bearing field of the
controlled-complete receipt and emits five mutations each — absent, `[]`, `{}`, `nil`, and
*duplicated-alongside-an-alternate* (the SOL-EC-005 shape) — then asserts refusal.

```
field-mutants: 845 generated from 240 fields, 845 refused, 0 CONSUMED
```

It is not allowed a second list either: it asks `gate-consume.clj --print-inert` for the fields the
consumer **declares** inert and attacks everything else, and it reads the inventory to know which
evidence each obligation actually requires (R1 has no Var census; R2 is discharged by the landing
box, so mutating its execution proves nothing). On its first run it found **513** accepted fields;
closing them produced the envelope schema — every field is validated or declared inert, with
declared types, and an **unknown field refuses**, so a producer cannot introduce one the consumer
silently ignores.

Two rules moved out of the consumer's private knowledge and into the tree, because a rule living in
one reader's head is the same defect Sol found at the nested-check level: the **environment policy**
(which keys must be accounted for) and **`:discharge {:by :landing-box-execution}`** for R2, which
had been a hard-coded `(= :R2 …)` in the matcher.

## 1c. The fence worktree was not released on early exits (field defect)

`/home/forge/src/clj-surgeon-fence` was found holding an untracked
`docs/observations/proof-burden-fence.md`, which makes the **next** run refuse on
`fence-worktree-dirty`. Cause: the pre-review sweep sets `FENCE_RELEASED=1`, and `fence_release`
short-circuits on that flag — so every exit between launching a reviewer and archiving its verdict
called `fence_release` and returned instantly, after the reviewer had already written into the tree.
A second guard then refused to sweep when no verdict had been archived, which is exactly the case.

Repaired: the flag is cleared **at the moment the tree starts being written**, not after the archive
exists; and the no-archive guard no longer skips the sweep, because the sweep already copies every
doomed file out before cleaning — the copy *is* the archive, and a failed copy still stops it.

**v3.4's own ratchet then caught a regression in that fix**, which is the system working: ship began
tidying away *another* run's litter, defeating the `fence-worktree-dirty` refusal whose purpose is to
make an unexplained failure loud. Scoped with `FENCE_OURS`, set only when this run launches a
reviewer: our dirt is swept, someone else's is reported and kept. New fixture row
`9-an-early-exit-still-returns-the-borrowed-fence-tree-clean-or-says-why`.

## 1a3. Sol round 3 (`20260909T165932Z-9dc25924fc38`) — NOT SHIPPABLE as it stands

Repository half committed (`2f924bee`); consumer half substantially built and **still red**. I am
reporting the true state rather than a green number.

| id | fixed? | what changed |
|---|---|---|
| **SOL-EC-007** | yes | `:policy-sha256` now covers the whole rule surface — obligations, `environment-policy`, stage manifest, inventory version. `:environment-manifest/:selected` values must be 64-hex digests (a `nil` digest consumed before). `:obligations/:manifest-sha256` and `:candidate/:inputs-manifest/:sha256` are bound to the **canonical digest of the obligations this tree derives** (64 `f` characters consumed before) |
| **SOL-EC-008** | yes | the sweep has **no skip set and no depth limit**: it walks every path of the receipt (1,637 paths, 9,698 mutants incl. `:zero` and `:alien-string`), runs the consumer **in process**, and its only exemption is the consumer's own printed declaration |
| **SOL-EC-009** | yes | the tree now **derives** the fixtures each selected namespace registers (`use-fixtures` in its own source) and the consumer compares sets; `["not/a/real-fixture"]` refuses |

**A duplicate the repair surfaced:** R1 named `admit_transaction_recovery_battery.clj` twice, so
dropping a manifest entry still left the path "accounted for". `:required-inputs` is deduplicated and
the manifest is now compared by **canonical equality**, not coverage.

### The sweep is RED, and that is the finding

```
field-mutants: 9698 over 1637 paths (full walk, no skip set);
  8170 refused, 604 consumed-by-declared-rule (134 observer-dependent),
  810 BAD, 163 inert-declared-but-load-bearing
```

`run-ship-v3.7` is therefore **73 rows, 1 mismatch**, and an install would report NOT PROVEN. The 810
are not a new defect class — they are the *same* one, at fields I had not yet bound. Each remaining
block is mechanical: compare the field against the value the inventory derives, or declare it
observer-dependent with a reason. I stopped adding rules rather than keep going unsupervised past the
timebox.

`163 inert-declared-but-load-bearing` is a second honest signal from the same sweep: `:inert` was
doing two jobs. It now means *no authority at all* (proved by mutating and requiring the decision and
the discharged set to be identical), and `:validated-elsewhere` is the known-field allowlist.

### Red-team pass (as asked): "what unrelated value would this obligation accept?"

Run against the controlled-complete receipt with the installed consumer:

| substitution | decision |
|---|---|
| R4's `:executed-tests` ← R5's identity list | `run-required/scope-incomplete` |
| R4's `:check-contract-sha256` ← R5's recipe digest | `run-required/recipe-mismatch` |
| R3's `:fixture-identities` ← R4's fixture ids | `run-required/provenance-missing` |
| R4's `:obligation-ids` ← `[:R5]` | `run-required/evidence-missing` |
| R4's `:runtime` ← R5's bb runtime | `run-required/runtime-mismatch` |
| `:toolchain/:java/:version` ← the Clojure version | `run-required/toolchain-mismatch` |
| **R5's `:vars-ref` ← R4's `:vars-ref`** | **`consume`** — a Var census belonging to a different obligation |
| **R1's `:result` ← arms 99/99 passed** | **`consume`** — the arm count is not bound to the recovery battery's own receipt |
| **R6's `:result` ← `{:exit 0 :wall-ms 999999}`** | **`consume`** — a shell obligation's result is only `:exit`-checked |
| **`:candidate/:tree` ← `:phase-a-tree`** | **`consume`** — the two are compared to the merged tree separately, and equal values pass both |
| **environment digest ← another key's digest** | **`consume`** — a digest is shape-checked, and the landing box cannot recompute the producer box's environment |

Five acceptances, all the same shape: **a value that is well-formed and belongs to something else.**
The first three are fixable by binding (the reference must name its own obligation; the arm count must
come from the recovery receipt; a shell result needs the recipe's own success predicate). The last two
need item 2's observer — the landing box cannot know another machine's environment, and
`:candidate/:tree` equalling `:phase-a-tree` is only wrong if something attests they differed.

**Verdict: do not ship this consumer.** The producer-side work is sound and committed; the consumer
needs the 810 + 5 bound before it earns a GO. Every real receipt still refuses at
`custody-unverified`, so nothing is banked in the meantime.

## 1b. The install defect (found by the coordinator, same day)

`install.sh` reported `run-ship-v3.6: 8 rows, mismatches: 6` → **INSTALL NOT PROVEN**. Reproduced
exactly and fixed.

**Cause, two halves.** The installer copies only `"$SRC/fixtures"/*.sh` into the canonical fixture
directory, so my new `stub-gate.clj` — which the three migrated fixtures build their producer with —
was never copied; and it re-runs the fixtures with `V3_DIR=$DEST`, so my fixtures' `$V3/fixtures/stub-gate.clj`
resolved to `/home/forge/bin/fixtures/`, which does not exist. The `cp` failed silently, the fixture
repository was committed **without** the helper, `landing-gate-prewarm` ran `bb stub-gate.clj` against
a missing file, the fast lane went RED, and six rows failed as "run stopped / did not land" — fail-closed,
and completely mysterious from outside.

**Why my corpus missed it:** `run-corpus.sh` sets `V3_DIR` to the *staging* directory, where
`fixtures/stub-gate.clj` does exist. I proved the staged files against staged fixtures **in their
staging layout**, never in the layout the installer creates. That is the same class as the scar in §7
of this report: a verifier blind to the arrangement its subject will actually be in.

**Fixes:** each fixture resolves its helper **beside itself** (`$(dirname "$0")`), with fallbacks, and
**refuses loudly** when it cannot find it — a fixture that quietly builds a broken repository reports a
code defect where there is a missing file. `install.sh` now copies `*.clj` and `*.edn` helpers as well
as `*.sh`, fails the install if a fixture copy fails, and runs `run-ship-v3.7` in its post-install
list. The v3.7 corpus resolves its binaries from `$V3_DIR` and its inputs from beside itself, so the
post-install run tests the **installed** bytes.

**A second install-proof blocker, found by running `install.sh` itself:** the installer decides
OK/NOT-PROVEN by matching **`mismatches: 0`** against each fixture's last line, and my new corpus
ended `== corpus: 72 passed, 0 failed` — which never matches. Adding it to the installer's list made
**every** install report NOT PROVEN however green it was. A fixture whose verdict its own runner
cannot read reports nothing. Its summary line now speaks the installer's vocabulary.

**Proved by running `install.sh` itself** against a scratch `DEST` + `FIXTURES` (it already supports
both as env overrides), with the ship lease free:

```
== re-running the fixtures against the INSTALLED files
   run-land-auto: land-auto fixtures: 19 rows, mismatches: 0
   run-land-publication-truth: land-publication-truth fixtures: 12 rows, mismatches: 0
   run-ship-v2: ship-v2 fixtures: 28 rows, mismatches: 0
   run-ship-v3: ship-v3 fixtures: 22 rows, mismatches: 0
   run-ship-v3.1: ship-v3.1 fixtures: 13 rows, mismatches: 0
   run-ship-v3.2: ship-v3.2 fixtures: 8 rows, mismatches: 0
   run-ship-v3.3: ship-v3.3 fixtures: 13 rows, mismatches: 0
   run-ship-v3.4: ship-v3.4 fixtures: 14 rows, mismatches: 0
   run-ship-v3.5: ship-v3.5 fixtures: 5 rows, mismatches: 0
   run-ship-v3.6: ship-v3.6 fixtures: 9 rows, mismatches: 0
   run-ship-v3.7: ship-v3.7 fixtures: 73 rows, mismatches: 0
INSTALL OK v3.7 stamp=20260909T164437Z files='ship land fence-run receipt-chain land-auto
  records-push ship-fix-block-spec.md run-bg sol-yolo gate-envelope.clj gate-consume.clj mutate.clj'
  — every fixture green against the installed bytes.   (rc=0)
```

**On isolation:** the fixtures already export `SHIP_ROOT="$FX/ship"`, so the ship **lease** is
per-fixture and a live ship cannot collide through it; their gate stages are `echo` stubs, so they
take no box-wide slots. The one genuinely shared thing was the installer's own refusal —
`INSTALL REFUSED reason=ship-lease-live` — which is correct, and is why this proof waited for
`/var/tmp/forge/ship/.lease/owner` to disappear. Nothing was installed; `~/bin` was not touched.

---

## 2. What was built

### A. Producer — repository, branch `fable/evidence-consumer`

Worktree `/home/forge/src/clj-surgeon-item1`, created from `origin/MCP/main`; HEAD proved equal to
**3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7**. Four commits, author `forge-anvil <forge-anvil@anvil>`,
Gene + Fable trailers. **Not pushed.**

- **`7582c0d9`** — `gate: print-gate-obligations, and a landing receipt that says what it discharged`
- **`1ed995c6`** — `gate: a policy hash covers the RULES, not the data the rules read`
- **`45beeb7e`** — `gate: rule bytes, derived nested checks, Var identities, a resolved shell` (Sol round 1)
- **`9dc25924`** — `gate: the inventory states the environment policy and what evidence discharges what` (Sol round 2)

HEAD `9dc25924fc3851af3fbf8767df0a1bed6cffe304`. Four commits, none pushed.

| file | what |
|---|---|
| `test/clj_surgeon/gate_obligations.clj` (new) | derives R1–R10 from the tree: transitive Makefile recipe digests, runtime, exact selected test identities, declared exclusions, required-input digests with a `:policy`/`:data` role, result predicates, `:policy-sha256` |
| `test/clj_surgeon/toolchain_identity.clj` (new) | runs each gate tool, digests stdout and stderr, takes the **first non-banner line** as the version, files launcher noise under `:diagnostic`, returns `:status :unknown` for a failed or banner-only command |
| `test/clj_surgeon/gate_obligations_test.clj` (new, `:fast`) | **15 tests / 63 assertions** |
| `test/clj_surgeon/battery_parallel_runner.clj` | the gate receipt becomes `:receipt-version 2` and gains `:toolchain`, `:obligations`, `:executions` |
| `Makefile` | **one** new target, `print-gate-obligations`. `print-gate-stages` kept unchanged |
| `lane_manifest.clj`, `lane_manifest_test.clj`, `deftest_census.edn` | new namespace registered; census regenerated through the direct entrance, diff read |

**The independent derivation reproduces §4's hand-read counts exactly**: R3 = 2 namespaces,
R4 = 55 fast + 7 integration = 62, R5 = 49 — before my own test namespace exists. After it, the tree
says 56 + 7 = 63, and the gate's own arithmetic accounts for the whole difference (898 → 910 tests,
11,197 → 11,238 assertions).

**A policy hash covers the RULES, not the data the rules read.** Found by red-teaming my own first
version, before it left the branch. `:required-inputs` entries now carry `:role :policy` or
`:role :data`. `docs/observations/battery-ledger.edn` is the only `:data` input: R2's freshness check
*reads* it, and §5's landing delta allowlist explicitly permits its bytes to change between the gate
running and the landing merging. My first version hashed its digest into `:policy-sha256`, so **every
landing that raced a battery run would have refused with `obligations-policy-mismatch` — for a file
the allowlist exists to permit.** A safe failure, and one that would have killed the feature exactly
where it was designed to help. A `:data` input keeps its path and its role in the hashed projection
(so removing it, adding one, or reclassifying it still moves the hash) and loses only its digest.
Three assertions witness it, driven through a substituted digest rather than by rewriting the
repository's real ledger.

### B. Consumer — ship v3.7, staged at `/var/tmp/forge/ship-v3.7`

Copied from v3.6. **`~/bin` untouched; no installer run; nothing pushed.**

| file | sha256 (16) |
|---|---|
| `ship` | `e95ea5e60d130c56` |
| `land` | `647a734413567a86` |
| `gate-envelope.clj` (new) | `034b5afed6023b29` |
| `gate-consume.clj` (new) | `6448222daab675d9` |
| `mutate.clj` (new) | `cb28ac1a416bdee4` |
| `replay.clj` (new) | `0b4b267c9ed89c92` |
| `install.sh` | `6b980cad87d3a7d8` |
| `fixtures/stub-gate.clj` (new) | `bface800a969df10` |
| `fixtures/gen-field-mutants.clj` (new) | `044c446daa9ba216` |
| `fixtures/run-ship-v3.6.sh` | `3d2b972a7ac754c6` |
| `fixtures/run-ship-v3.7.sh` (new) | `dc58d565d86cd930` |

Fixture bytes at `/var/tmp/forge/item1-fixtures/`: `envelope-green.edn` `5528f54ea4ae83c9` (18,887 B),
`inventory-3ea3803e.edn` `d887ddd138883ea7`, `producer-landing.edn` `e06788657ab182ff`,
`toolchain-observed.edn` `eee701645e3c0cbd`, `replay.edn` `92c4bb58901bafb6`.

`install.sh` lists the three new runtime files, so a future install carries them; a partial install
degrades to `reason=reader-unavailable`, never to a guess.

**What changed in ship.** The `printf` receipt is gone. Values reach a real EDN writer
(`gate-envelope.clj`) as environment variables; the toolchain comes from the process that *ran* the
gate, not from a `head -1` ship runs minutes later in a different shell. The envelope is bounded at
65,536 bytes and the 250 KB producer receipt is copied beside it and cited as `{:path :sha256 :bytes}`.
The writer reads its own bytes back with a strict reader, and **withholds an envelope whose proof is
incomplete** — so v3.6's four typed "sources" (`unparseable`, `not-a-prewarm`,
`edn-reader-unavailable`, `command-targets`) are deleted along with the claim they protected: ship is
not the authority on what the gate ran.

**What changed in land.** `--consume-gate-receipt` now:
1. asks the **merged tree** `make -s print-gate-obligations`;
2. runs `make -s battery-fresh` **itself** — R2 is a predicate over the current clock, so no receipt can carry it and a prewarm receipt legitimately omits it;
3. observes this box's toolchain by running the real commands;
4. hands all of it to `gate-consume.clj`, which discharges obligations one at a time and prints the table into the landing log.

`LAND-GATES consumed` prints only when every staged obligation plus R8 is discharged. Otherwise
`LAND-GATES rerun reason=<typed> obligation=R<n> -- <detail>`, and the gates run exactly as they
always have. The consumer's working directory (`/var/tmp/forge/land-consume-<tip>`) is the landing's
decision record: the derived inventory, the observed toolchain, the typed decision and its table.

---

## 3. The obligation table, on the real tree

`make test` on `fable/evidence-consumer` at `1ed995c6` / tree `49887df4…`, 2026-09-09 14:47–14:50Z:
7/7 stages exit 0, `:problems []`, **169,383 ms**, alias 164/3,495, mcp 910/11,238, bb 891/7,888,
isolation 0, skipped-preconditions 0, `:ok true`. Full decision at
`/var/tmp/forge/item1-fixtures/decision-green.txt`.

| id | stage | discharge source | what the consumer actually checked |
|---|---|---|---|
| R1 | `admit-transaction-recovery-battery` | receipt execution | exit 0, recipe digest, runtime `:jvm`/`:cold`, **arms 3/3, failed-arms []** — the *existence* of `target/admit-transaction-recovery-battery-receipt.edn` is explicitly not evidence |
| R2 | `battery-fresh` | **the landing box's own run** | exit 0 on the merged tree, at the landing clock |
| R3 | `alias-migration-test` | receipt execution | **2/2 named identities executed**, 0 failures/errors/isolation/skipped |
| R4 | `mcp-test` | receipt execution | **63/63 named identities executed**, fast-before-integration 56 + 7, 0 failures/errors/isolation/skipped |
| R5 | `test-bb` | receipt execution | **49/49 named identities executed under the `bb` runtime** — a JVM run of the same source does not discharge it |
| R6 | `repository-hygiene` | receipt execution | exit 0, recipe digest, final-candidate lifecycle |
| R7 | `intent-audit` | receipt execution | exit 0, `:ok true`, `:violations []`, on this candidate |
| R8 | — | **consumer derivation** | union census reconciled **by name**: no missing, no unexpected, no within-obligation duplicate |
| R9 | — | **NOT discharged, by design** | task acceptance and independent review are out of band |
| R10 | — | **NOT discharged, by design** | publication freshness belongs to the observer |

Before the envelope is opened at all: strict EDN (one form + EOF, duplicate keys, unknown tags,
65,536-byte bound), `:receipt-version 2`, `:kind :landing-evidence`, the merged tree, a **re-derived**
phase-A→landing delta against the registered allowlist, the obligation **policy hash** re-derived from
the merged tree, exactly one of `:landing?`/`:prewarm?`, no obligation id this tree does not define,
and no producer-declared pending proof.

---

## 4. Offline replay — what v3.7 would have decided over retained bytes

`bb /var/tmp/forge/ship-v3.7/replay.clj <inventory>`; machine-readable at
`/var/tmp/forge/item1-fixtures/replay.edn`.

| artifact | bytes | transport | v3.7 decision | first undischarged |
|---|---:|---|---|---|
| ship 05:16Z → **LANDED 45b28c8** | 2,537 | `not-valid-edn` | **run-required** | never reaches the matcher (`Unsupported escape character: \.`) |
| ship 08:05Z → **LANDED 479222d** | 3,014 | `not-valid-edn` | **run-required** | same |
| gate-lanes r3 prewarm | 248,285 | parses (adapter) | **run-required** | R1 `toolchain-missing` |
| gate-lanes r3 full | 248,381 | parses (adapter) | **run-required** | R1 `toolchain-missing` |
| ship v3.6 `proof/real-prewarm.edn` | 857 | parses (adapter) | **run-required** | R3/R4/R5 `scope-incomplete` — **no executed identities at all**; R8 names 112 selected identities that never ran |
| v3.7 green envelope (this branch) | 18,887 | parses | **consume** | R9/R10 only, by design |

Through the *production* strict reader all four retained pre-repair artifacts refuse at the door —
`envelope-oversized` for the two 248 KB producer receipts, `schema-unsupported` for the 857-byte shape
fixture. The per-obligation rows above come from an **explicit legacy adapter** inside `replay.clj`
that recovers stages and per-namespace runs; it cannot invent a toolchain, custody or policy that were
never written. That is what §5 forbids inferring, and it is exactly what the rows show.

### The finding

`/var/tmp/forge/ship/20260909T051658Z-e6ecd8158ee1/gates-prewarm.edn`, sha256 `25b99927b4028a11…`,
the receipt consumed by the landing of **45b28c8** at 05:30Z, records:

```
 :lanes "admit-transaction-recovery-battery battery-fresh mcp-test repository-hygiene test-bb"
 :all-green "true"
 :toolchain-java "Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge"
```

No `alias-migration-test`. No `intent-audit`. Its landing log
(`/var/tmp/forge/land-45b28c80c0def301b321a3bb23a7585f4443735a.log`) reports two lanes — `Ran 886 …`
and `Ran 891 …` — and prints `LAND-GATES consumed`. **164 tests / 3,495 assertions of alias battery
never ran on that merge, and the intent audit never ran as a stage.** Nothing on the screen said so.

The 08:05Z landing of **479222d** named all seven stages and its lanes really did run (164/3,495 +
898/11,197 + 891/7,888). v3.7 still refuses it, at the transport: those 3,014 bytes are not EDN. That
is a **conservative** refusal — complete work, rerun anyway — and it is the correct trade, because a
reader that could pull fields out of those bytes is the same reader that accepted the 05:16Z receipt.

Both receipts record the JAVA_TOOL_OPTIONS banner as the Java version. Every landing agreed with every
other landing about a fact none of them observed, and two boxes on different JDKs print that same
line: the check had **zero operating effectiveness**.

---

## 5. Cost, and the honest accounting

| component | measured (3 runs) |
|---|---|
| `make -s print-gate-obligations` on the merged tree | 2,260 / 2,165 / 2,257 ms |
| `make -s battery-fresh` (R2, observed by the landing box) | 2,499 / 2,298 / 2,243 ms |
| toolchain observation (8 commands) | 90 / 82 ms |
| `bb gate-consume.clj` (the matcher itself) | 28 / 36 / 29 ms |
| **consumer end-to-end** | **3,856 / 3,901 / 3,812 ms** |

| what it replaces | wall |
|---|---|
| `make test` on this branch, run 9 (7 stages, Sol round 2) | 171,885 ms |
| `make test` on this branch, run 6 (7 stages, Sol round 1) | 169,140 ms |
| `make test` on this branch, run 5 (7 stages) | 169,383 ms |
| `make test` on this branch, run 4 | 170,279 ms |
| `make test` on this branch, run 2 | 171,272 ms |
| retained `gate-lanes/r3` full receipt | 216,519 ms |
| retained `gate-lanes/r3` prewarm (6 stages) | 265,263 ms |

**Caveats, stated rather than smoothed.**

1. `battery-fresh` (2.3 s of the 3.9 s) is a **required obligation**, not overhead: the landing gate runs it too. Overhead proper is ~1.6 s.
2. Against v3.6's consumer the marginal cost is **~0.26 s**: `print-gate-obligations` costs ~0.2 s more than `print-gate-stages`, because both are dominated by bb classpath startup.
3. **The saving is landing-side.** Request-to-landed improves only where ship's prewarm overlapped review; where it did not, the same seconds were paid earlier.
4. Measured on Anvil at load ~1.0–1.5 with the suite lock held. A busy box moves the gate wall far more than it moves the 3.9 s.
5. Consumption is **not** permission to land. R9 and R10 stay open and the landing log says so.

---

## 6. Corpora

**ship v3.7's own corpus — 72 rows, 72 pass, 0 fail.** The positive is a **controlled complete**
fixture (the real producer bytes plus the observer attestation and per-check executions ship cannot
yet supply, labelled as such in `:extensions`); the **real** ship bytes are a *negative* row that must
refuse with `custody-unverified`. The other 70 are fail-closed negatives.

Retained inputs used **as bytes**, never regenerated: the exact 3,014-byte historical envelope (with
the digest the ledger records, and with a wrong digest); `/var/tmp/forge/gate-lanes/r3/{prewarm,full}-receipt.edn`;
`/var/tmp/forge/ship-v3.6/proof/real-prewarm.edn`, labelled a derived shape fixture and never raw
proof. Every synthetic row is generated by `mutate.clj` and carries
`:extensions {:derived-from {:path :sha256} :mutation}`.

Reasons covered: `not-valid-edn` · `receipt-digest` · `envelope-oversized` · `trailing-form` ·
`duplicate-keys` · unknown tag · truncation · old flat vector · `schema-unsupported` (v1 version,
wrong kind) · `evidence-missing` (omitted alias battery, omitted intent audit, absent file) ·
`stage-duplicate` · `unexpected-obligation` · `obligation-red` (nonzero / string / null exit, short
recovery arms) · `lane-red` (including **an absent counter, which is unknown and not zero**) ·
`skipped-preconditions` · `isolation-nonzero` · `unexecuted-test` · `focus-omission` ·
`scope-incomplete` (missing BB namespace, **same-count rename**, missing MCP namespace, no executed
identities) · `inputs-mismatch` · `candidate-mismatch` · `delta-outside-allowlist` (including a delta
that **cannot be computed**, which is unknown and fails closed) · `obligations-policy-mismatch` ·
`recipe-mismatch` · `runtime-mismatch` · `proof-pending` · `authority-inconsistent` ·
`toolchain-unknown` (banner-only Java) · `toolchain-mismatch` · `toolchain-missing` ·
`toolchain-unobserved` (a tool the receipt claims and the observer cannot produce) · `evidence-stale`
(red freshness) · `no-obligation-inventory` · `reader-unavailable` · `custody-unverified`
(self-reported, self-attested, unattributed, and no observation id) · `provenance-missing` (Sol's
all-fields-stripped fixture, and each field on its own) · `nested-check-evidence-missing` ·
`scope-incomplete` on a **missing Var** · `evidence-missing` on a Var reference that is absent, whose
digest disagrees, or whose path tries to escape the envelope's directory.

**The whole bridge corpus THE INSTALLER'S WAY** — `DEST` and `FIXTURES` pointed at scratch copies,
`V3_DIR=$DEST`, only the files the installer would have copied, nothing edited during the run:

```
run-land-auto                19 rows, 0 mismatches
run-land-publication-truth   12 rows, 0 mismatches
run-ship-v2                  28 rows, 0 mismatches
run-ship-v3                  22 rows, 0 mismatches
run-ship-v3.1                13 rows, 0 mismatches
run-ship-v3.2                 8 rows, 0 mismatches   (migrated)
run-ship-v3.3                13 rows, 0 mismatches
run-ship-v3.4                14 rows, 0 mismatches
run-ship-v3.5                 5 rows, 0 mismatches   (migrated)
run-ship-v3.6                 9 rows, 0 mismatches   (migrated + fence row)
run-ship-v3.7                73 rows, 0 mismatches   (new, incl. the 845-mutant sweep)
```

### The fixture migration, named rather than silent

v3.2, v3.5 and v3.6 asserted on the **v1 receipt's field vocabulary** (`ran-stages`,
`ran-stages-source`, `ran-stages-receipt`, `expected-stages`, `lanes`, `gate-manifest-sha256`) and read
it with a **sed field extractor** — the same shape of read that happily pulled fields out of bytes
`clojure.edn/read-string` refuses. v3.7 deletes that vocabulary. Rather than retire the rows I
migrated each to **the property it pins**, in place:

| was | is now |
|---|---|
| `tree-mismatch` | `candidate-mismatch` |
| `toolchain-java` | `toolchain-mismatch`, and the refusal must **name the tool** |
| `gate-manifest-mismatch` (re-sealed receipt drops a lane) | `evidence-missing`, and the refusal must **name the dropped check** |
| `stage-set-mismatch missing=…alias-migration-test` | `evidence-missing obligation=R3`, still naming `alias-migration-test` |
| `stage-set-mismatch extra=…all` | `unexpected-obligation`, naming `R99` |
| `no-stage-manifest` | `no-obligation-inventory` |
| `not-a-prewarm` (authority by position) | `authority-inconsistent` (authority by flags) |
| ship's four typed "sources" | ship **withholds** the envelope and names the problem in `gate-envelope.log`; land runs the gates |
| `rf()` — a sed field reader | `ef()` — an EDN reader |

Three real findings fell out of that migration:

- **v3.2's prewarm was a pinned decomposed command**, so nothing wrote a producer receipt at all.
  Under v1 that was fine — ship inferred stage names from the command — and under v2 it is not
  evidence. The fixture now uses the repository's own `landing-gate-prewarm` target.
- **v3.2's toolchain-drift row set a fixture variable ship read.** Under v3.7 identity is an
  *observation on both sides*, so the drift must now be injected where land looks — a `java` earlier
  on its PATH. That the old injection stopped biting is the repair working.
- **v3.5's `retire_targets` pushed a trunk commit that outlived its row**, so a later row silently
  inherited a crippled trunk. Added `restore_targets`, and said so at the pin.

The fixture repositories gained `fixtures/stub-gate.clj`, which derives the inventory **and** the v2
receipt from the fixture's own Makefile by the same code — so a green row proves agreement between two
independently computed answers rather than between two hand-typed strings.

---

## 7. §4/§5 items I could not implement as written

Stated, not reinterpreted.

1. **Custody is `:self-reported`, and now it REFUSES.** §4: "A signer of producer-supplied assertions
   has not observed execution." ship signs nothing it observed independently, so the consumer will
   not discharge any obligation on its receipts: **every real landing runs its own gate today**, with
   `reason=custody-unverified`. Item 2 (the recorder) is where observed custody must come from. This
   was deviation 1 in the first report, where I named it and then let the consumer consume anyway —
   Sol SOL-EC-001. Naming a deviation is not enforcing it.
2. **`:environment-manifest` and `:workspace-snapshot-sha256` are partial.** The receipt carries the
   runner's `source-digest` (Makefile, deps.edn, bb.edn, and regular files under `src`, `test`,
   `resources`, `docs/intent`) plus a content-addressed reference to the obligation inventory. §4's
   conservative boundary — "exact Git tree plus observed worktree/index, relevant untracked/ignored
   inputs, external file digests and environment" — is **not** fully implemented: untracked and ignored
   inputs, and a structured policy-selected environment, are not captured. Consequence: a change
   confined to an untracked input the gate reads would move no digest the consumer checks.
3. **`:declared-skips`, `:focus-omissions` and `:closure-basis`: presence is now required, but an
   emitted `[]` still means "this runner has no focus mechanism", not "the closure was proven
   complete".** The consumer refuses when a field is *absent* (SOL-EC-001) and when a vector is
   *non-empty*; what it cannot yet do is distinguish a proven-empty closure from a runner that has no
   closure concept. `:closure-basis` is still not emitted. When Kaocha evidence arrives it must be
   added as an additive, validated-together field group, and absence must stay unknown.
4. **`mcp-test-checks` members are derived from the recipe and per-check evidence is now REQUIRED —
   and the producer cannot yet supply it, so R4 refuses.** The consumer demands a `:nested-executions`
   record per derived member; the gate runs `mcp-test` as one Make target and reports one exit, so a
   real receipt refuses with `nested-check-evidence-missing`. That is Sol's "or refuse", and it is a
   second independent reason a real landing reruns today (the first being custody). Closing it for
   real needs per-check receipts from those fourteen targets.
5. **The replay's per-obligation rows for pre-repair artifacts are counterfactual.** They are computed
   against **this** tree's inventory, and those receipts describe other trees (`da100208…`,
   `5b7c4ad7…`). Through the production path each refuses earlier — at transport, or on
   `candidate-mismatch`. Deriving each artifact's own historical inventory would mean checking out and
   building each of those trees; I did not, so the rows read "what these executions would discharge if
   the tree matched", which is how §4's obligation-by-obligation question is answerable offline at all.
6. **R9/R10 have no mechanism, only a slot.** They are in the inventory, marked `:out-of-band`, and
   printed as NOT discharged on every consumption. Nothing here consumes a review verdict or a
   destination-ref observation.
7. **No live consumption is qualified, and the code now enforces that rather than relying on it.**
   §5's migration step 5 ("enable live consumption on an authorized slice after custody qualifies") is
   not reached, and the consumer refuses accordingly. `~/bin` is untouched by me, nothing was pushed,
   nothing landed. (The coordinator did install v3.7 once; the install refused to call itself proven,
   which is §1b.)
8. **The nine missing Kaocha raw receipts named in `receipts-to-verify.edn` are still missing.** The
   stream-line rows in §5's corpus table test a parser I did not build. Not implemented.
9. **Per-Var matching is now implemented** (it was a deviation in the first report and Sol
   SOL-EC-002). The receipt carries `:expected-vars`/`:executed-vars` folded from the child runs
   (913 each for mcp-test), behind a content-addressed `:vars-ref` once they exceed the envelope
   bound, and the consumer requires executed ⊇ expected. What remains open: **fixture and hook
   identity** are still not represented, so a Var that ran without its fixture would not be seen.

---

## 8. Reproduction

```bash
# the branch (three commits, not pushed)
git -C /home/forge/src/clj-surgeon-item1 log --oneline -3   # 45beeb7e, 1ed995c6, 7582c0d9; base 3ea3803e
cd /home/forge/src/clj-surgeon-item1
make -s print-gate-obligations | head -c 200
~/bin/suite-run make test                                   # 7/7 green, 169 s

# the consumer's own corpus (72 rows), inputs beside the script
FX=/var/tmp/forge/ship-v3.7/fixtures
FX_DIR=$FX GREEN_TREE=$(bb -e '(println (get-in (clojure.edn/read-string (slurp (first *command-line-args*))) [:candidate :tree]))' $FX/envelope-green.edn) \
  bash /var/tmp/forge/ship-v3.7/fixtures/run-ship-v3.7.sh

# every bridge fixture THE INSTALLER'S WAY, against scratch copies (never ~/bin)
#   see §1b; DEST=/var/tmp/forge/v37-scratchbin  FIXTURES=/var/tmp/forge/v37-scratchfx

# the offline replay
bb /var/tmp/forge/ship-v3.7/replay.clj /var/tmp/forge/item1-fixtures/inventory-3ea3803e.edn
```

Artifacts: `/var/tmp/forge/item1-fixtures/` (green envelope, inventory, observed toolchain, the green
decision table, `replay.edn`) · `/var/tmp/forge/item1-corpus/` (per-row decisions) ·
`/var/tmp/forge/ship-v3.7/corpus/` (per-fixture logs) · `/var/tmp/forge/item1-corpus-frozen.log`
(the frozen batch) · `/var/tmp/forge/item1-full5.log` (the green landing gate on the committed tree).

Boundaries held: no `pkill`/`pgrep -f`; every temp path under `/var/tmp/forge`; suites via
`~/bin/suite-run` under the lock, in the background, verdict read; no contact with ports
7888/7890/7894/7895/8300-8339; `~/bin` not edited; no installer run by me; no push.
