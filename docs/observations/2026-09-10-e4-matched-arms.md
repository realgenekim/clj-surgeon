# E4 reproduced in an isolated fixture, with an independent oracle, and three matched arms

Run 2026-09-10 on forge@anvil. Block 1 of the "programs to contracts" plan of record
(`2026-09-10-ethnography-plan-of-record.md`, next-4-hours items 1 and 2; Astra's consult
questions 2 and 6). Nothing was committed or pushed anywhere. All work under
`/var/tmp/forge/ethno/4h`; six detached worktrees of `/home/forge/src/curtaincall-cfp` at
`00e8f0fa` (the spike's commit), all removed at the end. Surgeon MCP `127.0.0.1:7906`
(trunk `bd124492`), called with `~/bin/surgeon-call`. Lint via `~/bin/clj-kondo`.

**The intent (E4), identical for every arm.** In
`src/cfp_scheduler_killer/views/schedule.clj`, change the require alias of
`cfp-scheduler-killer.events` from `events` to `ev`, and rewrite its uses. The file
contains the bytes `events/` on **31 lines**: **2** are uses of the alias
(`(events/day-hours event)` in `agenda-days` at line 153 and in `schedule-page` at line
1112) and **29** are URL string literals (`"/events/"`, `"/api/events/"`) spanning **22
distinct route templates**. The correct edit touches **3 lines in 3 top-level forms** out of
35 in the file.

---

## The headline table (the settled meter, plan-of-record item 1)

Wall is a diagnostic column, not the verdict. n = 1 per arm; all arms ran on the same box,
same commit, same oracle, back to back, and every "correct" arm produced the identical file.

| | **py — mistaken request** | **py — correct request** | **native (reviewed Edit)** | **surgeon — mistaken request** | **surgeon — correct request** |
|---|---|---|---|---|---|
| **Independent correctness (oracle)** | **FAIL** (5 of 11 checks) | **PASS** (11/11) | **PASS** (11/11) | **PASS after 2 refusals + 1 repair** | **PASS** (11/11) |
| **— gate-detected defects** | **0** | 0 | 0 | 0 | 0 |
| **— gate-green defects** | **29 route sites / 22 route templates** | 0 | 0 | 0 | 0 |
| **Complete elapsed** (intent → oracle verdict) | 11.1 s | 13.8 s | 21.3 s | 14.9 s | **8.5 s** |
| — authoring (observable) | 10.46 s | 13.11 s | 20.20 s (3 reviewing reads + 3 edits) | 12.5 s (2 requests + repair) | 7.14 s |
| — execution | 0.023 s | 0.025 s | (in authoring) | 0.14 s + 0.52 s + 1.19 s | 0.73 s (server `elapsed_ms` 672) |
| — formatting (`standard-clj fix`) | 0.149 s | not run | not run | none needed | none needed |
| — verification (oracle) | 0.58 s | 0.58 s | 0.60 s | 0.58 s | 0.58 s |
| — verification (repo gate, kaocha) | 19.19 s | 19.0 s | 19.0 s | 19.0 s | 18.95 s |
| **First-attempt success** | no (silently) | yes | yes | no (loudly) | yes |
| **Refusals — warranted / unwarranted** | 0 / 0 | 0 / 0 | 0 / 0 | **2 / 0** | 0 / 0 |
| **Recovery cost after refusal** | n/a — nothing refused | n/a | n/a | **one field, `31`→`2`; 1.19 s; zero extra lookups** | n/a |
| **Preservation — unintended top-level forms changed** | **7** (11 after `standard-clj fix`) | **0** | **0** | **0** | **0** |
| **Preservation — churn (`git diff --numstat`)** | 32/32 edit-only, **413/413** formatted | 3/3 | 3/3 | 3/3 | 3/3 |
| **Result hash** | `966636c2…` (broken) | `02332a74…` | `02332a74…` | `02332a74…` | `02332a74…` |
| **Evidence coverage** | oracle A+B+C; no HTTP/route-behaviour test exists in the repo | same | same | same + typed refusal payload naming both true sites | same |

`02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c` is also the hash the
2026-09-10 spike recorded for its three correct entrances — an independent cross-run
confirmation that all five correct paths converge on the same bytes.

### The gates, on the broken file

| gate | verdict on the file with 29 broken route sites |
|---|---|
| parses (rewrite-clj, clj-kondo) | **GREEN** |
| `clj-kondo` with the repo's own config | **GREEN** — 0 errors, 4 warnings, exactly baseline |
| `standard-clj fix` idempotent (ran twice, identical hash) | **GREEN** |
| `bin/kaocha unit`, both namespaces that touch `views.schedule` | **GREEN** — 41 tests, 561 assertions, **0 failures** |

That is the E4 reproduction: every gate in the pipeline green, 22 route templates pointed at
`/ev/` and `/api/ev/`.

---

## The independent oracle

`/var/tmp/forge/ethno/4h/oracle.sh <worktree> [--gate]`. It never asks the repository's gates
whether the edit is correct, and it does not use `clj-surgeon` to compute form boundaries —
the tool under test may not be its own judge. Three decisive families and one recorded-only
family:

- **A. Intent achieved** — from `clj-kondo --config '{:analysis true :output {:format :edn}}'`:
  A1 exactly one `namespace-usage` of `cfp-scheduler-killer.events` bound to alias `ev`;
  A2 exactly two `var-usages` to it; A3 the usage *set* is exactly
  `{day-hours via ev from agenda-days arity 1, day-hours via ev from schedule-page arity 1}`;
  A4 zero residual uses of `events`.
- **B. Preservation** — babashka's bundled **rewrite-clj** as an independent reader.
  B1 the file still has 35 top-level forms; B2 every form other than `ns`, `agenda-days`,
  `schedule-page` is byte-identical (SHA-256 per form) to `00e8f0fa`; B3 every inter-form gap
  (whitespace **and top-level comments**) is byte-identical; B4 the two alias-holding forms
  are byte-identical to baseline once the new alias token is mapped back.
- **C. Routes** — every string literal is extracted from the tree. C1 the count of literals
  containing `events/` is still 29; C2 every one of them survives verbatim; C3 the *whole*
  string-literal multiset is unchanged.
- **D. Recorded, never decisive** — `clj-kondo` findings vs baseline and, with `--gate`,
  `bin/kaocha unit` focused on the two namespaces that touch this file.

Baseline bytes come from `git show 00e8f0fa:<file>`, not from a worktree.

**Proof the oracle is red on the spike's broken output.** `e4_py_mistaken.py` reproduces
Astra's program from the spike doc verbatim (`s.replace('events/','ev/')`, plus the census's
typical guard `assert n == 31`). The guard passed. The oracle then reported:

```
  PASS A1-alias-binding ... alias=(ev)          <- the intent WAS achieved
  PASS A2/A3/A4                                  <- 2 usages, correct set, no stale alias
  FAIL B2-only-intended-forms-changed -- UNINTENDED: ("agenda-export-hint" "block-card"
       "conflict-chips" "day-tab" "place-form" "placed-card" "unpublished-drift-banner")
  FAIL B4 / C1 / C2 -- BROKEN ROUTE STRINGS: 29
  ORACLE: FAIL (5 checks)
  [secondary] clj-kondo in-repo: 0 errors, 4 warnings   -> GREEN
  [secondary] repo gate (kaocha, 2 ns): 41 tests, 561 assertions, 0 failures -> GREEN
```

Running `standard-clj fix` afterwards, as Astra's program did, **raises** the unintended-form
count from 7 to 11 and the churn from 32/32 to 413/413, and is idempotent, so it passes its
own gate too. Recorded in `oracle-py-mistaken-editonly.txt`, `-gate.txt`, `-formatted.txt`.

## The two Surgeon refusals, classified

| variant | reason | server time | source | classification |
|---|---|---|---|---|
| literal `from: "events/" → "ev/"`, `matches: 31` | `invalid-intent-form` — "Invalid symbol: `ev/`" | 33 ms | `source_unchanged: true` | **warranted.** The substring edit is not expressible at all; there is no form to name. |
| `from: "events/day-hours" → "ev/day-hours"`, `matches: 31` | `expect-count-mismatch` — "Intent 1 expected 31 matches, found 2" | 456 ms | `source_unchanged: true` | **warranted.** The author's count of *strings* collided with the code's count of *forms* before anything was written. |

Zero unwarranted refusals. The second refusal's payload is what makes recovery nearly free:
it returns `actual_count: 2`, `per_file_counts`, a `selector_sha256`, a `snapshot_guard` hash,
and `items` naming **both real sites by line (153, 1112) and preorder address**. Repair was a
single field, `31` → `2`; the re-call landed 3 edits in 1.19 s and the oracle passed.

## One missing verb, probed rather than assumed

`require_change` — the named alias/require verb, and the one the census found had **0 calls in
6,124 program edits** — cannot express this intent. Probed with `plan_only: true` against the
pristine worktree; it refuses in 15 ms with a typed `invalid-request`:
`"Target library cannot also be removed"`, `mutation_attempted: false`. It is an add/remove
verb "without symbol edits"; an alias **rename** with its use sites is out of scope. The
working Surgeon route for E4 is `edit_clojure` with `within` + `from`/`to` + a declared count.
That is a real gap next to the insertion gap already on the list.

---

## Learning

The three correct arms produced **byte-identical files** and differ only in what stood between
the author and a wrong one. Python's correct arm needed the lookbehind
`(?<![\w/.\-"])events/` — a hand-built Clojure symbol-boundary lexer, written inside a task
that was not about lexing — and I only wrote it because the spike had already told me the trap
existed; its 13.1 s of authoring is contaminated with that foreknowledge, and the *uncontaminated*
Python arm is the mistaken one, which finished 2.7 s faster, passed its own `assert`, passed
every repository gate, and broke 22 route templates. The native reviewed patch was the slowest
arm (21.3 s) and was correct, because reviewing three forms is exactly the act the program
replaced with a `count()`. Surgeon was the fastest complete arm (8.5 s) and, on the mistaken
request, was the only arm that failed **loudly**: two typed refusals, source unchanged both
times, and a refusal payload that handed back the true count and both true sites, so the whole
mistaken→correct path still finished in 14.9 s — cheaper than the native arm and, unlike the
mistaken Python arm, ending in a correct file. Every arm's wall is dwarfed by the 19 s test
JVM that follows it, and that JVM is exactly the gate that said green on the broken file: the
axis that separates these routes is not seconds, it is whether a wrong request can be executed
silently. Astra's own sentence is the finding — *a count of strings is not a count of forms* —
and the measurement here is that the count guard costs about a second and buys the only
detection anything in this pipeline had.

## Caveat

**n = 1 per arm, one intent, one file, one operator, one model.** These are paired
observations on the same commit with the same oracle, not a sample: nothing here supports a
distribution, a mean, or a claim about a *class* of edit. The authoring intervals include my
own latency and are not human authoring times, they are not comparable across arms with
different amounts of prior context (I had read the spike before every arm, which helps the
correct-Python arm most and the Surgeon arm second), and the mistaken-Python arm is the only
one whose authoring is honestly naive. The oracle proves **byte preservation** of the 29 route
strings, not that the routes still serve — this repository has no HTTP test that exercises
them, which is precisely why the defect is gate-green, and an oracle built from the same
absence cannot certify behaviour. Finally, `standard-clj fix` is charged only to the arms that
ran it; the spike's finding that it rewrites 452 lines of untouched source in this repository
is a Curtain Call formatting decision, not a property of any route measured here.

---

### Artifacts kept

```
/var/tmp/forge/ethno/4h/oracle.sh                        the independent oracle
/var/tmp/forge/ethno/4h/e4_py_mistaken.py                Astra's program, reproduced
/var/tmp/forge/ethno/4h/e4_py_correct.py                 the corrected program
/var/tmp/forge/ethno/4h/{sm1,sm2,sm3,s-correct,rc1}.json Surgeon requests as sent
/var/tmp/forge/ethno/4h/{sm1,sm2,sm3,s-correct,rc1}.out  Surgeon receipts as returned
/var/tmp/forge/ethno/4h/oracle-*.txt                     every oracle run
/var/tmp/forge/ethno/4h/times-*.txt                      the raw timings
/var/tmp/forge/ethno/4h/snap-py-mistaken-editonly.clj    the broken file, pre-formatter
```

Worktrees `wt-{oracle,py-mistaken,py-correct,native,surgeon,surgeon-mistaken}` were removed
with `git worktree remove --force` after the run. To reproduce: recreate any worktree at
`00e8f0fa` and run `oracle.sh <worktree> --gate`.
