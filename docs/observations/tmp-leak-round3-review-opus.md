## GO-WITH-FIX

Round-three independent review of `bridge/tmp-leak-ratchet` at **df5c568e** (temp-dir hygiene
ratchet, the fix round after round two's GO-WITH-FIX).
Reviewer: Opus, forge@anvil, 2026-09-04. Verification is by EXECUTION, not by reading the diff.

**Provenance.** Clone `/home/forge/tmp/sol/tmpleak1-wt`, HEAD proven and tree clean:

```
$ git fetch origin bridge/tmp-leak-ratchet MCP/main
   86bd9de3..df5c568e  bridge/tmp-leak-ratchet -> origin/bridge/tmp-leak-ratchet
   96d12a45..b0967214  MCP/main                -> origin/MCP/main
$ git checkout -q df5c568e && git rev-parse HEAD && git status --porcelain
df5c568ed424dfe8e2b855e6a5fb85f9d3869dc9
(no output -- clean)
```

Nothing in the clone was modified. All sabotage ran on a `git archive df5c568e` export under
`/var/tmp/forge/tmpleak3-review-fx/opus`. No server contacted. Nothing under `/tmp` or
`/var/tmp/forge` that I did not create was touched.

(review in progress -- sections appended as each item is executed)

---

## 1. The seam escape is CLOSED (round two's one blocking fix) — verified by execution

Round two's arm, re-driven exactly: `findmnt` shimmed dead, a forged mounts table claiming `ext4`
for the base, and a base on a **real, writable tmpfs I own** (`/run/user/1011`, mode 700, `df` says
`tmpfs 3.1G`). Round two got `EXIT=0` and a full run on RAM out of this. At `df5c568e`:

```
###### 1a SEAM ESCAPE: findmnt dead + forged ext4 table + REAL writable tmpfs base ######
PROBE role=parent max-mb=25061 tmpdir=/tmp args=[]
tmp-refused: java.io.tmpdir base=/run/user/1011/opus3-seam-escape has an UNDETERMINABLE filesystem
  type -- neither findmnt nor the mounts table could answer, so nothing proves it is not RAM.
  Refusing rather than assuming disk. ...
EXIT=97
```

**Exit 97 with the UNDETERMINABLE message, and no `PROBE role=child` line** — the child never ran.
That is the fix.

The three controls that prove the fix is surgical rather than blunt:

```
1b control -- same base, seam UNSET, findmnt dead:
   tmp-refused: ... is RAM-backed (tmpfs, fstype=tmpfs).                      EXIT=97
1c seam still SOUND in the REFUSING direction (forged table says tmpfs, base is real disk):
   tmp-refused: ... is RAM-backed (tmpfs, fstype=tmpfs).                      EXIT=97
1d arm 3e regression -- findmnt dead, REAL /proc/mounts, real-disk base:
   PROBE role=child ... PROBE leak-exit=0                                     EXIT=0
```

1d is the one that mattered most to check: the fallback mounts table is still **live code** and
still able to prove disk when it is the *real* table. The seam is now write-only in the refusing
direction, exactly as round two's finding 1 prescribed. Arms 3d and 3e are both green (see the full
gate run in §6).

---

## 2. `${TMPDIR:-/tmp}` is gone, and the new regex is the one that catches it

**No live site remains** in the scanned scope:

```
$ grep -n 'TMPDIR:-/tmp' Makefile test/*.sh bench/*.sh
test/tmp_leak_ratchet_test.sh:362:hardcoded=$(grep -nE '...|TMPDIR:-/tmp\}' Makefile test/*.sh bench/*.sh || true)
```

That single hit is the gate's own regex literal, and it does **not self-match**: the pattern requires
a literal `}` immediately after, and the file carries `\}`. Confirmed by running the real scan over
the gate file:

```
$ grep -nE '(^|[^A-Za-z0-9_.-])/tmp/[A-Za-z0-9_.]|TMPDIR:-/tmp\}' test/tmp_leak_ratchet_test.sh
(no output -- no self-match)
```

The Clojure audit's own file carries the shape once in a comment (`tmp_leak_support_test.clj:189`),
but `.clj` files are not in either scan's file list, so it cannot self-match either.

**The builder's claim about the old regex is TRUE, and it is the point of the change:**

```
subject line:  root=$(mktemp -d "${TMPDIR:-/tmp}/x.XXXX")
OLD pattern  (?:^|[^A-Za-z0-9_.-])/tmp/[A-Za-z0-9_.]                    -> 0 hits
NEW pattern  (?:^|[^A-Za-z0-9_.-])/tmp/[A-Za-z0-9_.]|TMPDIR:-/tmp\}     -> 1 hit
```

The old regex never matched that shape — `${TMPDIR:-/tmp}` has `}` after `/tmp`, not `[A-Za-z0-9_.]`
— so round two's finding 3 was not merely an exemption being removed; the exemption was *decorative*
and the pattern had to grow. It did.

### The gate goes RED when the shape is planted (both layers)

Planted on the export at `bench/retain_benchmark_result.sh:116`:

```
PLANT: root=$(mktemp -d "${TMPDIR:-/tmp}/clj-surgeon-retention-self-test.XXXXXX")

shell scan   -> bench/retain_benchmark_result.sh:116: ...   SCAN-MATCHED (gate fails at step 10)
clj audit    -> FAIL in (no-gate-names-a-hard-coded-ram-path)
                hard-coded /tmp write targets: ["bench/retain_benchmark_result.sh:116: ..."]
                Ran 1 tests containing 1 assertions.  1 failures, 0 errors.
```

And the second plant proves `bench/*.sh` is genuinely newly in scope, not just newly mentioned:

```
PLANT: root=$(mktemp -d "/tmp/clj-surgeon-retention-self-test.XXXXXX")
round-two scope (Makefile test/*.sh)            -> MISS
round-three scope (+ bench/*.sh)                -> MATCH
```

Export restored byte-identical after each plant (`diff -q` against the clone: same).

Round two's finding 2 site is fixed at source: all 18 `mktemp` calls across `bench/*.sh` now derive
from `${TMPDIR:-/var/tmp}`, and a literal `/tmp/<name>` appears nowhere in `Makefile`, `test/*.sh`
or `bench/*.sh`.

---

## 3. The Make layer redirects `/tmp` and `/dev/shm` — but NOT `/tmp/<subpath>` (the one fix)

`Makefile:43`:

```make
SELF_TEST_TMP ?= $(if $(filter /tmp /dev/shm,$(TMPDIR)),/var/tmp,$(or $(TMPDIR),/var/tmp))
```

Executed against the real Makefile (a throwaway `--eval` target, make's own expansion, not a text
read):

```
TMPDIR=/tmp             -> SELF_TEST_TMP=/var/tmp          OK  (redirected)
TMPDIR=/dev/shm         -> SELF_TEST_TMP=/var/tmp          OK  (redirected)
TMPDIR=/var/tmp/forge   -> SELF_TEST_TMP=/var/tmp/forge    OK  (honoured)
TMPDIR=<unset>          -> SELF_TEST_TMP=/var/tmp          OK  (default)
TMPDIR=/tmp/x           -> SELF_TEST_TMP=/tmp/x            ** NOT REDIRECTED **
TMPDIR=/dev/shm/y       -> SELF_TEST_TMP=/dev/shm/y        ** NOT REDIRECTED **
```

**Cause.** GNU make's `$(filter)` is exact-match unless the pattern carries `%`. `/tmp` and
`/dev/shm` are listed as literals, so any RAM path one segment deeper walks straight through.

**Why it is not cosmetic.** This is the exact class MCP-OP-TMPHYG-012 exists for, in the builder's
own words on the assignment: *"these recipes hand the value straight to shell harnesses that never
reach Clojure."* They do:

```
Makefile:759   bash bench/run_anvil_portfolio_pair.sh  $(SELF_TEST_TMP)/clj-surgeon-anvil-pair-self-test ...
Makefile:761   bash bench/run_anvil_public_cfp_cleanup.sh $(SELF_TEST_TMP)/clj-surgeon-public-cfp-self-test ...
Makefile:763   bash bench/run_anvil_format_extraction.sh $(SELF_TEST_TMP)/clj-surgeon-format-extraction-self-test ...

$ TMPDIR=/tmp/opus3-escape make -s --eval='__p:; @echo "$(SELF_TEST_TMP)/clj-surgeon-anvil-pair-self-test"' __p
/tmp/opus3-escape/clj-surgeon-anvil-pair-self-test
```

Three bash harnesses, no Clojure in the path, writing under a RAM-backed tmpfs. That is the inode
fire, one directory level down from the shape that was fixed.

**Both witnesses are blind to it, and that is the more important half.** The Clojure witness
(`the-make-layer-does-not-propagate-a-ram-tmpdir`) iterates `["/tmp" "/dev/shm"]`; the shell arm 11
iterates `for ram in /tmp /dev/shm`. Arm 11's `case` pattern *does* list `/tmp/*` and `/dev/shm/*` —
so the assertion was written expecting subpaths, and the loop never supplies one. A witness whose
predicate is wider than its inputs looks like coverage it does not have.

**Fix (one line + two loop entries):**

```make
SELF_TEST_TMP ?= $(if $(filter /tmp /tmp/% /dev/shm /dev/shm/%,$(TMPDIR)),/var/tmp,$(or $(TMPDIR),/var/tmp))
```

and add `/tmp/x` and `/dev/shm/y` to both witness loops. `$(filter)` does honour `%` patterns, so
this is a genuine one-liner. **This is the single reason the verdict is GO-WITH-FIX rather than GO.**

Note for symmetry: the Clojure layer is NOT blind here — `literal-ram-path?` is prefix-based and
refuses `/tmp/x` (round two recorded it over-refusing `/tmp/../var/tmp/forge` for the same reason).
The gap is confined to the Make layer, which is precisely the layer -012 was written to cover.

---

## 4. `sweep-root!` is now a receipt of what happened — verified

```
undeletable root (parent chmod 500):  sweep-root! -> false   still-exists? true
deletable own root:                   sweep-root! -> true    still-exists? false
foreign dir (not our prefix):         sweep-root! -> false   still-exists? true
  tmp-refused: refusing to delete .../someone-elses-dir -- it is not a private per-run root ...
```

Round two's finding 5 is closed: an undeletable root is no longer counted as swept. The refusal for
a foreign directory is unchanged and still typed and printed.

---

## 5. Bench self-tests: green under `TMPDIR=/var/tmp/forge`, nothing created under `/tmp`

Every bench entry point the `test:` target actually invokes, run with `TMPDIR=/var/tmp/forge`, with
a before/after listing of `/tmp` around the whole set:

```
bash bench/retain_benchmark_result.sh --self-test              EXIT=0  (root under /var/tmp/forge/...)
bash bench/retain_benchmark_result.sh --verify-tracked         EXIT=0
BENCH_SCHEDULE_SELF_TEST=true bash bench/run_clean_codex.sh    EXIT=0
BENCH_HARNESS_SELF_TEST=true  bash bench/run_clean_codex.sh    EXIT=0
make benchmark-inspect-mcp-self-test                           EXIT=0
   inspect benchmark self-test passed: /var/tmp/forge/clj-surgeon-inspect-self-test.GUCpsx

$ ls -A /tmp | diff <before> -
(empty -- nothing created under /tmp by any of them)
```

`git status --porcelain` on the clone is still empty afterwards: the retention self-test cleans up
its `bench/results/retention-self-test-*` artifact.

### `run_clean_claude.sh` — the pre-existing failure, with a correction to the brief

`CLAUDE_BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_claude.sh` exits **1** at the tip and
exits **1** on a clean `origin/MCP/main` export. `gtimeout` is not installed on this box (macOS
coreutils name) and the script requires it at line 133. Cause confirmed pre-existing.

**The file is NOT byte-identical to trunk, and the brief's expectation should be corrected.** Round
three changed exactly two lines in it, and they are the fix, not a regression:

```
86c86  <   self_test_root=$(mktemp -d /tmp/clj-surgeon-claude-harness-self-test.XXXXXX)
       >   self_test_root=$(mktemp -d "${TMPDIR:-/var/tmp}/clj-surgeon-claude-harness-self-test.XXXXXX")
159c159 < expected_skill=$(mktemp /tmp/clj-surgeon-expected-skill.XXXXXX)
        > expected_skill=$(mktemp "${TMPDIR:-/var/tmp}/clj-surgeon-expected-skill.XXXXXX")
```

Those were two literal `/tmp/<name>` write targets on trunk — real instances of the defect this leaf
exists to kill, newly caught because `bench/*.sh` entered the scan. The `gtimeout` requirement at
line 133 is untouched. **Behaviour is equivalent**: both runs emit the same three
`[claude-receipt] ... failed` lines (ordering differs run to run; the outputs are otherwise
identical modulo temp paths) and both exit 1.

---

## 6. Gates — all green, all counts match the builder's claim

```
$ ~/bin/suite-run bb test/run_all.clj
Ran 825 tests containing 6766 assertions.
0 failures, 0 errors.
BB EXIT=0                                                 (grep -c 'temp-leak' = 0)

$ sh test/tmp_leak_ratchet_test.sh
tmp-leak ratchet witness passed            real 0m48.3s   GATE EXIT=0
   including:  3d/3g UNDETERMINABLE refusals, 3e accepted, 3h tmpfs refusal,
               all five runners exit 97 on TMPDIR=/tmp,
               --- SELF_TEST_TMP with TMPDIR=/tmp     -> /var/tmp ---
               --- SELF_TEST_TMP with TMPDIR=/dev/shm -> /var/tmp ---
               --- SELF_TEST_TMP with TMPDIR unset    -> /var/tmp ---
               --- SELF_TEST_TMP with TMPDIR=<fx>     -> <fx> ---

$ make mcp-operation-oracle
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]   EXIT=0

$ make repository-hygiene
repository hygiene: no machine-local build cache is tracked at any depth                        EXIT=0

$ ~/bin/suite-run bb -e '(clj-surgeon.mcp-intent-contract/audit-current-repository)'
:ok true
:violations []
```

**The builder's claim about the intent audit is TRUE.** `df5c568e` was written because
`repository-operation-intent-contract-is-coherent` went RED with
`:missing-test-witness MCP-OP-TMPHYG-012` and `:missing-implementation-witness MCP-OP-TMPHYG-013`;
at the tip the audit is `:ok true` with an empty violations vector. The -012 remedy is the right
shape too: a Clojure witness that **executes** make's expansion rather than a bare marker.

`~/bin/suite-run clojure -M:clj-surgeon/mcp-test` result is recorded in §6b below.

---

## 7. Sabotage — three closures reverted one at a time, three RED gates

All on the `git archive df5c568e` export, restored byte-identical after each (verified by `diff -q`
against the clone).

**A. The seam fix alone** (`mount-fstype` restored to `(or (findmnt-fstype dir)
(mounts-table-fstype dir) :unknown)`, nothing else touched):

```
$ sh test/tmp_leak_ratchet_test.sh
tmp-leak-ratchet witness FAILED: 3g: a forged mounts table PROVED disk --
  the witness seam granted a pass (exit 0)                          GATE EXIT=1

$ bb clj-surgeon.tmp-leak-support-test/a-seam-sourced-fstype-can-never-prove-real-disk
expected: (= :unknown-fstype (:reason (tmp-leak/base-refusal disk)))
  actual: (not (= :unknown-fstype nil))
Ran 1 tests containing 5 assertions.  2 failures, 0 errors.
```

This is the arm the brief asked for, and it is the one that matters: **the shell gate names the
defect by number and in plain words, and it fails first.**

**B. The Make-layer redirect alone** (`SELF_TEST_TMP ?= $(or $(TMPDIR),/var/tmp)`):

```
clj witness -> Ran 1 tests containing 4 assertions.  2 failures, 0 errors.
shell arm 11 value with TMPDIR=/tmp -> /tmp   (arm 11 would fail: "the Make layer propagated a RAM TMPDIR")
```

**C. The `sweep-root!` receipt alone** (back to unconditional `true`):

```
clj witness -> actual: (not (false? true))
Ran 1 tests containing 5 assertions.  1 failures, 0 errors.
```

**D. The scan regex** — covered in §2: planting `${TMPDIR:-/tmp}` reddens both the shell scan and the
Clojure audit; planting a literal `/tmp/<name>` in a bench script reddens the round-three scan and is
MISSED by round two's scope, which is the proof that `bench/*.sh` is genuinely in scope now.

---

## 8. The ordering blemish — it does not matter at the tip, and it is one item wider than disclosed

Traced the `- [x]` state of each new intent through every commit of the branch:

| intent | RED witness | GREEN fix | first commit where the spec says `[x]` |
|---|---|---|---|
| TMPHYG-011 | `0b4402af` | `3d9ad3a0` | `3d9ad3a0` — its own GREEN. Correct. |
| TMPHYG-012 | `bf1f7dab` | `416abccd` | `c2aaf97e` — **one commit BEFORE its RED** (the disclosed blemish) |
| TMPHYG-013 | `030dabbe` | `50d06ec9` | `030dabbe` — **its own RED commit** (not disclosed) |

So the builder disclosed one of two. Both are the same class.

**Ruling: it does not matter at the tip, and it is not a discipline failure in substance — but the
disclosure should be corrected to name both.** The reason is that I checked the thing the checkbox
is a proxy for: *were the RED commits actually red?* Both were.

```
bf1f7dab (-012 RED):  SELF_TEST_TMP ?= $(or $(TMPDIR),/var/tmp)
                      arm 11 exists at that commit; executed there:
                      TMPDIR=/tmp     -> /tmp        <- arm 11 fails
                      TMPDIR=/dev/shm -> /dev/shm    <- arm 11 fails
                      (the Clojure witness did not exist yet -- "Ran 0 tests" -- it
                       arrived with df5c568e to satisfy the intent audit)

030dabbe (-013 RED):  Ran 1 tests containing 5 assertions.  1 failures, 0 errors.
```

The witnesses failed first in both cases. A checkbox in a design document is a claim about the
branch, not the ratchet; the ratchet is the failing witness, and it was there. What merges is the
tip, where every box is `[x]` and every witness is green. **Not a blocker, not worth another round —
a one-line correction to the disclosure.**

### 6b. The MCP suite

```
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 712 tests containing 8437 assertions.
0 failures, 0 errors.
MCPTEST EXIT=0                                            (grep -c 'temp-leak' = 0)
```

Both suite counts match the builder's claim **to the assertion**: 825/6766/0 and 712/8437/0, neither
with a `temp-leak:` line.

---

## FINDINGS SUMMARY

| # | Finding | Severity | Round-two item |
|---|---|---|---|
| 1 | `SELF_TEST_TMP` does not redirect `TMPDIR=/tmp/<subpath>` or `/dev/shm/<subpath>`; both witnesses' loops are blind to it | **fix before merge** (one line + two loop entries) | new — a residue of finding 4 |
| 2 | The `-012` / `-013` `[x]` ordering blemish; `-013`'s was not disclosed | cosmetic — correct the disclosure | disclosed (half) |
| 3 | `bench/run_clean_claude.sh` is not byte-identical to trunk (two lines, both the fix); failure cause is identical and pre-existing | none — brief's expectation corrected | new |

Round two's five open items are **all closed and all closed by execution**: finding 1 (the seam,
§1 + §7A), finding 2 (bench in scope, §2 + §5), finding 3 (`${TMPDIR:-/tmp}` exemption, §2),
finding 4 (Make layer, §3 — closed for the two shapes tested, open for subpaths), finding 5
(`sweep-root!` receipt, §4).

## Mergeability

Trunk has moved again: `origin/MCP/main` is now **b096721460517895a42c755539e3540cdcaf63ae**
(round two reviewed against `96d12a45`).

```
$ git merge-tree --write-tree HEAD origin/MCP/main
CONFLICT (content): Merge conflict in Makefile     <- the only CONFLICT line, the only file listed
$ git merge-file -p ours base theirs | grep -c '^<<<<<<<'
1
--- tokens only in OURS ---     tmp-leak-ratchet-self-test
--- tokens only in THEIRS ---   fanout-selftests
```

**One conflict, one hunk, one line** — the `.PHONY` list. Unchanged in shape from round two.
Resolution: keep both target names.

### Minor note, not a finding

`bench/run_clean_claude.sh` **leaks its self-test root when it aborts** on the missing `gtimeout`:
each run left one `clj-surgeon-claude-harness-self-test.XXXXXX` behind (mine, and trunk's run of the
same script, so it is pre-existing and not introduced here). It is on real disk under
`/var/tmp/forge`, not RAM, so it is outside this leaf's blast radius -- but it is the same species
this leaf exists to kill, and a `make test` that fails on this box leaves a directory behind every
time. Worth a `trap ... EXIT` in a follow-up bead, not in this branch.

## Fixture cleanup

```
$ rm -rf /var/tmp/forge/tmpleak3-review-fx /run/user/1011/opus3-seam-escape
$ ls -d /var/tmp/forge/tmpleak3-review-fx     ls: cannot access ...: No such file or directory
$ ls -d /run/user/1011/opus3-seam-escape      ls: cannot access ...: No such file or directory
$ ls -d /var/tmp/forge/clj-surgeon-suite-* /tmp/clj-surgeon-suite-*
                                              (none)
```

The four self-test roots my own bench runs left behind (10:57) were removed. One
`clj-surgeon-claude-harness-self-test.6PuK2l` dated 10:13 **predates this session and was left
untouched** -- I do not delete entries I did not create.

Clone re-verified after all work: HEAD `df5c568e`, `git status --porcelain` empty.

---

## GO-WITH-FIX

The one blocking item from round two — the `CLJ_SURGEON_MOUNTS_FILE` seam granting a PASS — is
**closed, and I closed it by execution**: round two's exact escape arm (findmnt dead, forged ext4
table, a real writable tmpfs base on `/run/user/1011`) now exits 97 with the UNDETERMINABLE message
and the child never starts, while arm 3e still proves disk from the *real* mounts table and arm 3h
still refuses on a seam-sourced tmpfs. Reverting that one closure alone turns the gate red with a
diagnostic that names it. The other four round-two items are closed the same way. All six gates are
green and both suite counts match the builder's claim to the assertion.

The remaining fix is one line: `$(filter /tmp /dev/shm,$(TMPDIR))` is exact-match, so a `TMPDIR`
one segment deeper (`/tmp/x`) walks through the Make layer untouched and reaches three bash
harnesses that never see Clojure — the same class MCP-OP-TMPHYG-012 was written to close, and both
of its witnesses iterate only the two shapes that already work. Use
`$(filter /tmp /tmp/% /dev/shm /dev/shm/%,$(TMPDIR))` and add the subpath cases to both loops.

**Mergeability:** this tip is GO for `origin/MCP/main` (trunk **b0967214**) once the `$(filter)`
subpath fix lands, subject to the single one-line `.PHONY` conflict, resolved by keeping both
`tmp-leak-ratchet-self-test` and `fanout-selftests`.
