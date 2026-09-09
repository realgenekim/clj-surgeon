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
| **Corpus** | ship v3.7's own: **48 rows, 48 pass**. The whole bridge corpus against **frozen** staged bytes: **11/11 fixtures, 0 mismatches** |
| **Replay** | **both** consumed landings of 2026-09-09 would have been **refused** — and one of them consumed a gate that never ran the alias battery |

Astra's forecast was *central 0–30 s saved per eligible clean request, plausible −10 to +120 s*. The
measured landing-side saving is **above the top of that range**, because the forecast is about a
*duplicated check* and this consumer replaces the whole seven-stage gate on the merged tree with a
four-second derivation-and-match. §5 states the caveat plainly: that saving is real only when ship
ran the gate ahead of the landing, and request-to-landed **L** improves only if that run overlapped
something else (review). If it did not, the work moved earlier and L is unchanged.

---

## 2. What was built

### A. Producer — repository, branch `fable/evidence-consumer`

Worktree `/home/forge/src/clj-surgeon-item1`, created from `origin/MCP/main`; HEAD proved equal to
**3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7**. Two commits, author `forge-anvil <forge-anvil@anvil>`,
Gene + Fable trailers. **Not pushed.**

- **`7582c0d9`** — `gate: print-gate-obligations, and a landing receipt that says what it discharged`
- **`1ed995c6`** — `gate: a policy hash covers the RULES, not the data the rules read`

HEAD `1ed995c63dacacffd95156e6f0c27914206119b6`, tree `49887df4648481e4c9cb688a8b7aec3accffb4b8`.

| file | what |
|---|---|
| `test/clj_surgeon/gate_obligations.clj` (new) | derives R1–R10 from the tree: transitive Makefile recipe digests, runtime, exact selected test identities, declared exclusions, required-input digests with a `:policy`/`:data` role, result predicates, `:policy-sha256` |
| `test/clj_surgeon/toolchain_identity.clj` (new) | runs each gate tool, digests stdout and stderr, takes the **first non-banner line** as the version, files launcher noise under `:diagnostic`, returns `:status :unknown` for a failed or banner-only command |
| `test/clj_surgeon/gate_obligations_test.clj` (new, `:fast`) | **12 tests / 40 assertions** |
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

| file | sha256 (16) | bytes |
|---|---|---|
| `ship` | `7463e9444ff29ea4` | 107,228 |
| `land` | `9c80e9117232912d` | 13,402 |
| `gate-envelope.clj` (new) | `5b5c225de6f4cd44` | 12,063 |
| `gate-consume.clj` (new) | `a8d8bee9d674bfb6` | 25,141 |
| `mutate.clj` (new) | `8366fcc3f160f00c` | 5,630 |
| `replay.clj` (new) | `0b4b267c9ed89c92` | 11,731 |
| `install.sh` | `852d4c7d47de221d` | 8,186 |
| `run-corpus.sh` | `5c29105a711317a2` | 940 |
| `fixtures/stub-gate.clj` (new) | `4c984f42611cbc90` | 7,993 |
| `fixtures/run-ship-v3.2.sh` | `e7b4d4bc379808af` | 19,942 |
| `fixtures/run-ship-v3.5.sh` | `a66e252a98480f84` | 21,585 |
| `fixtures/run-ship-v3.6.sh` | `e55387480a287fa3` | 22,345 |
| `fixtures/run-ship-v3.7.sh` (new) | `65116a947fdf09da` | 10,842 |

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

**ship v3.7's own corpus — 48 rows, 48 pass, 0 fail.** One positive built by ship's own writer from
the **real** producer bytes of a green `make test`, and 47 fail-closed negatives.

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
`toolchain-unknown` (banner-only Java) · `toolchain-mismatch` · `toolchain-missing` · `evidence-stale`
(red freshness) · `no-obligation-inventory` · `reader-unavailable`.

**The whole bridge corpus against FROZEN staged bytes** — every fixture the landing bridge has ever
shipped, run against the v3.7 files whose hashes are in §2B, nothing edited during the run
(`/var/tmp/forge/item1-corpus-frozen.log`; per-fixture logs in `/var/tmp/forge/ship-v3.7/corpus/`):

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
run-ship-v3.6                 8 rows, 0 mismatches   (migrated)
run-ship-v3.7                48 rows, 0 mismatches   (new)
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

1. **Custody is `:self-reported`, and stays that way.** §4: "A signer of producer-supplied assertions
   has not observed execution." ship signs nothing it observed independently; the envelope says
   `:custody :self-reported` and the consumer treats it as a producer claim. Item 2 (the recorder) is
   where custody qualification lives. Nothing here should be read as custody having qualified.
2. **`:environment-manifest` and `:workspace-snapshot-sha256` are partial.** The receipt carries the
   runner's `source-digest` (Makefile, deps.edn, bb.edn, and regular files under `src`, `test`,
   `resources`, `docs/intent`) plus a content-addressed reference to the obligation inventory. §4's
   conservative boundary — "exact Git tree plus observed worktree/index, relevant untracked/ignored
   inputs, external file digests and environment" — is **not** fully implemented: untracked and ignored
   inputs, and a structured policy-selected environment, are not captured. Consequence: a change
   confined to an untracked input the gate reads would move no digest the consumer checks.
3. **`:declared-skips`, `:focus-omissions` and `:closure-basis` are emitted as empty vectors, not
   derived.** This runner produces no Kaocha-style scope/basis/focus data, so there is nothing to read.
   The consumer *refuses* on a non-empty `:focus-omissions` or `:unexecuted-tests` (both witnessed),
   but an emitted `[]` here means "this runner has no focus mechanism", **not** "the closure was proven
   complete". §5's `legacy-scope-unknown` distinction is therefore not representable in this producer
   yet; when Kaocha evidence arrives it must be added as an additive, validated-together field group,
   and absence must stay unknown.
4. **`mcp-test-checks` is pinned by name and by transitive recipe digest, not by execution.** The
   fourteen members (Prolog oracle, four Python unittest discoveries, eight self-test Make targets) are
   listed in R4's `:nested-checks` and their recipe bytes are inside R4's digest, so a changed or
   removed recipe refuses, and a check that ran and failed already fails the stage. But the receipt
   records **one** exit for `mcp-test`, not fourteen per-check results — a check that was *skipped
   inside a passing recipe* would not be caught. Closing that needs per-check receipts from those
   targets.
5. **The replay's per-obligation rows for pre-repair artifacts are counterfactual.** They are computed
   against **this** tree's inventory, and those receipts describe other trees (`da100208…`,
   `5b7c4ad7…`). Through the production path each refuses earlier — at transport, or on
   `candidate-mismatch`. Deriving each artifact's own historical inventory would mean checking out and
   building each of those trees; I did not, so the rows read "what these executions would discharge if
   the tree matched", which is how §4's obligation-by-obligation question is answerable offline at all.
6. **R9/R10 have no mechanism, only a slot.** They are in the inventory, marked `:out-of-band`, and
   printed as NOT discharged on every consumption. Nothing here consumes a review verdict or a
   destination-ref observation.
7. **No live consumption is qualified.** §5's migration step 5 ("enable live consumption on an
   authorized slice after custody qualifies") is not reached: `~/bin` is untouched, the installer was
   not run, nothing was pushed and nothing landed.
8. **The nine missing Kaocha raw receipts named in `receipts-to-verify.edn` are still missing.** The
   stream-line rows in §5's corpus table test a parser I did not build. Not implemented.
9. **One §4 sentence I implemented in a narrower place than it is written.** "Missing, duplicate and
   unexpected obligations … refuse" is enforced over `:obligation-ids` and over the executed census;
   it is **not** enforced over per-Var identities, because this runner reports namespaces, not Vars,
   in its suite receipts. A namespace that executed with a Var silently removed would pass R4's scope
   check and be caught only by the deftest census inside the suite — which is a real ratchet, but a
   different one from the consumer's.

---

## 8. Reproduction

```bash
# the branch (two commits, not pushed)
git -C /home/forge/src/clj-surgeon-item1 log --oneline -2   # 1ed995c6, 7582c0d9; base 3ea3803e
cd /home/forge/src/clj-surgeon-item1
make -s print-gate-obligations | head -c 200
~/bin/suite-run make test                                   # 7/7 green, 169 s

# the consumer's own corpus (48 rows)
GREEN_TREE=49887df4648481e4c9cb688a8b7aec3accffb4b8 \
  bash /var/tmp/forge/ship-v3.7/fixtures/run-ship-v3.7.sh

# every bridge fixture against the staged bytes (11 fixtures)
bash /var/tmp/forge/ship-v3.7/run-corpus.sh

# the offline replay
bb /var/tmp/forge/ship-v3.7/replay.clj /var/tmp/forge/item1-fixtures/inventory-3ea3803e.edn
```

Artifacts: `/var/tmp/forge/item1-fixtures/` (green envelope, inventory, observed toolchain, the green
decision table, `replay.edn`) · `/var/tmp/forge/item1-corpus/` (per-row decisions) ·
`/var/tmp/forge/ship-v3.7/corpus/` (per-fixture logs) · `/var/tmp/forge/item1-corpus-frozen.log`
(the frozen batch) · `/var/tmp/forge/item1-full5.log` (the green landing gate on the committed tree).

Boundaries held: no `pkill`/`pgrep -f`; every temp path under `/var/tmp/forge`; suites via
`~/bin/suite-run` under the lock, in the background, verdict read; no contact with ports
7888/7890/7894/7895/8300-8339; `~/bin` not edited; no installer run; no push.
