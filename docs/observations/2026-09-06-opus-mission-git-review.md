# Independent Opus review — saved-mission Git publication seam

- **Reviewed commit:** `9a45063167b74a5096f4b0b7fe6775f6fbd8b708` (`astra/opus-git-review`,
  "Refuse unsupported Spark executor before mission readiness")
- **Worktree:** `/var/tmp/forge/astra-opus-git-review-fx` (clean at review start and end;
  only untracked `review-scratch/` and this file added)
- **Reviewer model:** Claude Opus 5 (`claude-opus-5`)
- **Date:** 2026-09-06
- **Subject:** `src/clj_surgeon/mission_git.clj`, `mission_git_process.clj`,
  `mission_git_ledger.clj`, and the `commit` integration in `mission_cli.clj` / `bin/mission`
- **Plans read:** `docs/plans/mission-git-receipt.md`, `docs/plans/mission-git-ledger.md`

## Verdict: **HOLD**

One demonstrated defect publishes a tree entry that was never part of the verified mission
while the receipt reports `:ok true` and `:index-staging false` (Finding 1). That is the
failure class the contract exists to prevent — a success claim covering an unverified change.
The fix is small and I verified that the obvious remedy is sufficient at the Git level.

Everything else I probed behaved as documented. In particular the properties the brief
singled out held under executed adverse probes: source and ledger bytes were byte-identical
after every refusal, the uncertain ref-update outcome was explicit and carried a real
inspectable object, and no probe produced a typed failure reported as success.

Six further findings are low severity and non-blocking (2–7).

---

## Findings

### 1. HIGH — the staged-scope guard is configurable away; a staged submodule gitlink is published

**Where.** `src/clj_surgeon/mission_git.clj:127` derives the observed staged set from

```
git diff --cached --no-ext-diff --name-only -z --
```

`git diff` honours `diff.ignoreSubmodules` (system/global/local config) and
`submodule.<name>.ignore` from a working-tree `.gitmodules`. At `ignore = all` a staged
gitlink is invisible to that command, while `git write-tree` (`mission_git.clj:126`) still
puts it in the tree that is committed. `plan`'s `:git-staged-scope` check
(`mission_git.clj:61-63`) therefore compares mission paths against an under-reported set and
admits.

The seam already pins `core.fsmonitor=false`, `core.hooksPath=/dev/null`,
`commit.gpgSign=false` and strips every `GIT_*` environment variable
(`mission_git.clj:97-98`, `mission_git_process.clj:19-21`). This is a gap in that same
hardening list: an ordinary Git configuration key that is not part of the verified proof
changes what the command publishes.

**Repro (executed).** `opus.probe-git/b5-staged-gitlink-outside-mission-scope-must-not-be-published`
and `opus.probe-git2/f1-gitmodules-ignore-all-also-hides-a-staged-gitlink`.

Fresh repository, one committed preimage of `src/a.clj`, the verified postimage staged, then:

```
git update-index --add --cacheinfo 160000,<oid>,vendor/sub
git config diff.ignoreSubmodules all          # vector A
# or, with no repo config at all:
printf '[submodule "vendor/sub"]\n\tpath = vendor/sub\n\turl = ./nowhere\n\tignore = all\n' > .gitmodules   # vector B
```

- **Expected:** `{:ok false :error-type :git-staged-scope}`, branch unchanged.
- **Observed (both vectors):** `{:ok true}`, branch advanced, and the published tree is

```
040000 tree 36369bad…  src
100644 blob a2ccdb35…  src/a.clj
040000 tree 1c3441bf…  vendor
160000 commit e11c8faf…  vendor/sub
```

The boundary's own view of the index was `["src/a.clj"]`.

**Scope of the vector.** Only `ignore = all` hides it; I measured all four levels on
git 2.53.0 — `none`, `untracked` and `dirty` all still report `vendor/sub`.

**Fix direction (not applied).** Add `--ignore-submodules=none` to the diff argv.
`opus.probe-git2/f2-explicit-ignore-submodules-none-would-see-it` confirms this restores
visibility under the hostile config:

```
as-shipped                       => "src/a.clj\0"
with --ignore-submodules=none    => "src/a.clj\0vendor/sub\0"
```

Stronger still would be to stop trusting a porcelain-ish diff for a security boundary and
instead compare the full `git ls-tree -r HEAD` listing against `git ls-files --stage`, and
refuse any index entry whose mode is not `100644`/`100755`. That also closes the general
class rather than this one instance.

---

### 2. MEDIUM-LOW — the 1 MiB output cap loses its typed reason

**Where.** `src/clj_surgeon/mission_git_process.clj:23-27` raises `:git-output-limit` inside
the `output` future; line 36 retrieves it with `deref`, which rethrows
`java.util.concurrent.ExecutionException`. `ex-data` on that wrapper is `nil`, so
`mission-git/execute!` and `mission-git/commit!` fall through to `:git-boundary-failed`
(`mission_git.clj:92`, `158-159`). The same applies to the `writer` future.

**Repro (executed).** `opus.probe-git/e1-output-cap-is-a-typed-refusal` — a child writing
1 MiB + 64 bytes.

- **Expected:** `:git-output-limit`.
- **Observed:** `{:class "java.util.concurrent.ExecutionException" :error-type nil}`.

The existing witness `mission_git_fence_test/argv-output-cap-refuses` asserts only
`(thrown? Exception …)`, so it passes over the gap; `mission-git-receipt.md` lists "output
cap" among the validated typed behaviours. No unbounded read occurs and no private child
output leaks — this is a taxonomy defect, not a safety one.

**Fix direction.** Unwrap in `await`: rethrow `(.getCause e)` for `ExecutionException`.

---

### 3. LOW — in-process lock contention is `:git-boundary-failed`, not `:git-lock-busy`

**Where.** `mission_git.clj:151-157`. A second `FileChannel.tryLock` on the same lock file
*within one JVM* throws `OverlappingFileLockException` rather than returning `nil`, so the
`(refuse :git-lock-busy)` arm is unreachable in-process.

**Repro (executed).** `opus.probe-git2/g3-concurrent-publication-in-one-jvm-is-typed`, two
concurrent `git/commit!` calls with a slow `ledger-current?`.

- **Expected:** winner `{:ok true}`, loser `{:error-type :git-lock-busy}`.
- **Observed:** winner `{:ok true :git-ref-updated true}`, loser
  `{:ok false :error-type :git-boundary-failed :git-ref-updated false}`.

Safety held: exactly one publication, and `HEAD` equalled the winner's commit. Only the
typed reason is wrong. See Limitations — I did not probe cross-process contention, which is
the deployed shape.

---

### 4. LOW — real detached and unborn HEAD refuse as `:git-command-failed`; the documented `:git-unsupported-head` path is unreachable for them

**Where.** `mission_git.clj:120-124`. `rev-parse --verify HEAD` (unborn) and
`symbolic-ref -q HEAD` (detached) exit non-zero, so `run-process!` raises
`:git-command-failed` before `oid?`/`branch?` are ever consulted.

**Repro (executed).** `opus.probe-git/a1-unborn-head-refuses`, `a2-detached-head-refuses`.

- **Expected (per `mission-git-receipt.md`, "detached HEAD refuse"):** `:git-unsupported-head`.
- **Observed:** `{:ok false :error-type :git-command-failed :git-ref-updated false}`; HEAD
  absent / unchanged and source bytes unchanged in both cases.

Worth noting because the unit matrix row that appears to cover this
(`mission_git_test.clj:38`, `["detached" provenance (assoc observed :branch nil)]`) feeds
`:branch nil` straight into `plan`; no real observation ever produces that shape. The
refusal is correct; the witness does not describe the path that runs.

---

### 5. LOW — identity preflight runs before repository validation

**Where.** `mission_git.clj:146-149` runs `git var GIT_AUTHOR_IDENT` /
`GIT_COMMITTER_IDENT` before `rev-parse --absolute-git-dir` and before `observe!` resolves
the real Git top level.

**Repro (executed).** `opus.probe-git2/g4-worktree-root-that-is-not-a-repository-refuses` —
a `--workspace` that is an ordinary directory, not a repository.

- **Expected:** a refusal naming the repository problem.
- **Observed:** `{:ok false :error-type :git-identity-unavailable}` with
  `:decision "Configure repository-local user.name and user.email explicitly…"` — advice
  that cannot fix the actual problem (a mistyped workspace path).

Environment-dependent: this reproduces when no global Git identity is configured, which is
the case on this seat (`git config --global --get user.email` exits 1). With a global
identity the same input surfaces as `:git-command-failed`, which is also unspecific. Ordering
the repository/root check first would make both cases self-explanatory.

---

### 6. LOW — `Ledger-SHA256` / `Receipt-SHA256` do not canonically bind the admitted record

**Where.** `mission_git_ledger.clj:115` and `:123` parse the artifacts with
`clojure.edn/read-string`, which returns the **first** form and silently discards everything
after it, while `artifact` (`:94-102`) digests the whole file.

**Repro (executed).** `opus.probe-final/l1-trailing-bytes-after-the-first-edn-form-are-admitted-and-hashed`
(mission EDN) and `opus.probe-ledger/i3-trailing-bytes-in-the-undo-artifact-are-also-tolerated`
(undo.edn). Appending `\n;; a later writer appended this\n{:id "M-9999" :state :verified}\n`
to the saved mission file:

- **Observed:** `{:ok true}`; the published body records
  `Ledger-SHA256: 106ea9e7…` = SHA-256 of the file *including* the ignored trailer, which
  differs from `227b338d…`, the hash of the same admitted record without it.

This is not an admission bypass — the trailer cannot change what is admitted, and the digest
honestly describes the bytes that were on disk. The cost is auditability: the same verified
mission publishes under many different digests, so a later verifier recomputing
`Ledger-SHA256` cannot conclude anything about the record's content. `clojure.edn/read` with
an EOF sentinel plus a "trailing data" refusal would make the digest canonical.

---

### 7. LOW, structural, **not demonstrated** — a post-success teardown throw would be reported as a definite `:git-ref-updated false`

**Where.** `mission_git.clj:151-161`. `execute!`'s success map is returned through
`(try … (finally (.close channel)))` nested inside `with-open`, all inside the outer
`(catch Exception e … (refuse …))`. `refuse` (`:12-13`) hard-codes
`:git-ref-updated false`. Any throw *after* `update-ref` has already advanced the branch
therefore becomes a definite claim that the branch is unchanged — precisely the outcome the
contract forbids and the reason `:git-ref-update-uncertain` exists.

`mission-git-receipt.md` records this exact class occurring once ("BB's unsupported
`FileLockImpl.release` method after a successful ref update … an erroneous failure report").
The fix taken addressed which close method is called, not the structure that converts any
late failure into a false negative.

**Not reproduced.** `FileChannel.close` does not throw in this environment, so I have no
executed repro; I am reporting it as plausible-by-inspection, not confirmed. Binding
`execute!`'s result and returning it regardless of teardown outcome removes the class.

---

## What I confirmed working (independently executed)

Every item below is an assertion in a probe I wrote and ran; none is a restatement of an
existing test I only read.

**Exact successful publication** (`probe-git/d1`, `probe-git2/d1b`)
- The stored commit object's body is byte-identical to `mission-git/message`; headers are
  `tree`/`parent`/`author`/`committer` only, with **no `gpgsig`**.
- Author and committer come from the repository's own config.
- `diff-tree -r HEAD` against the parent is exactly `src/a.clj` — nothing else moved.
- Working-tree source bytes and `ls-files --stage` output are unchanged by the success.
- A `pre-commit` hook that touches a canary and exits 1 does not run and does not block.

**Refusals leave every byte alone** — `HEAD`, `write-tree` oid, and source bytes verified
unchanged for: stale ledger callback, drifted live source, an unexpected staged regular file,
a staged deletion of an unrelated file, a partially staged two-file mission, a staged
executable-bit change, an oversized (>1 MiB) source, non-UTF-8 staged bytes, a symlinked
source, a `ledger-current?` that throws (its message does not leak into the receipt), and a
`ledger-current?` returning a truthy non-`true` value.
(`probe-git/b1–b4, c1`; `probe-git2/g1, g2, g5, g6`)

**Frozen branches** — `refs/heads/main` and `refs/heads/MCP/main` both refuse
`:git-unsupported-head` with HEAD unchanged, and an ordinary branch (`mainline`) is *not*
over-refused. (`probe-git/a3`)

**Missing identity** — empty `user.name`/`user.email` yields `:git-identity-unavailable`
with an explicit configuration decision; nothing is installed; HEAD, index and source are
unchanged. (`probe-git/a4`)

**Uncertain ref result is explicit** — with `.git/refs/heads/fixture.lock` pre-created so
`update-ref` genuinely fails: `{:ok false :error-type :git-ref-update-uncertain
:git-ref-updated :unknown :possible-commit "…"}`; `git cat-file -t <possible-commit>` returns
`commit`, so the caller can actually inspect it; HEAD and source unchanged. End-to-end
through `bin/mission`, exit 1 with
`:next-action "Inspect the Git branch and possible-commit before retrying…"`.
(`probe-git/c2`, `probe-ledger/j4`)

**Saved-ledger revalidation** — `ledger-current?` is invoked **3 times** per publication
(initial observation, pre-object drift check, post-object drift check). A ledger that goes
stale *after* `commit-tree` refuses `:git-stale-ledger` with `:git-ref-updated false` and the
ref is never touched — `false` is honest there because `update-ref` was never issued.
(`probe-final/k1, k2`)

**Ledger admission** — nine reviewer-chosen adverse *inverse* records all refuse (their suite
varies the saved mission, not the inverse): a file outside the owner set, `absent-before`
(creation), non-empty `created-directories`, duplicate file entries, tampered `result-source`,
tampered `original-source`, wrong `:operation`, empty file list, and — the interesting one —
a tampered body whose `:receipt-hash` was **recomputed to match**, which still refuses because
the hash must simultaneously equal `[:undo :receipt_hash]` and `[:receipt :receipt_hash]`.
Six saved/root/proof variants also refuse, including a plan root disagreeing with the mission
root and a gate/acceptance pair sharing an id. A symlinked ledger artifact refuses; an
on-disk inverse whose first form was altered refuses without advancing HEAD.
(`probe-ledger/h1, h2, i1, i2`)

**Public CLI boundary** — seven option-gate shapes (no workspace, no id, `../bad` id, extra
positional, `--config`, `--receipt-dir`, `--spec-file -`) all exit 1 with
`:mission-commit-options`, no spec is read and no exception text reaches stderr. An unknown
mission is a typed refusal (`:git-ledger-artifact-unavailable`) at exit 1 with a runnable
`show` example. `help commit`, `commit --help`, `--help commit` and
`--workspace R commit --help` all exit 0 and all name "stages nothing", "hooks", "signing",
"push". Replaying `commit` after a success refuses at exit 1 (`:git-staged-scope`), so the
same mission cannot be double-published. (`probe-ledger/j1–j4`)

**Bounded subprocess behaviour** — argv is never a shell string (a `; touch <canary>` payload
produced `:git-command-failed` and no canary); the child environment contains exactly one
`GIT_*` variable, `GIT_TERMINAL_PROMPT=0`; a non-reading child handed 2 MiB of stdin under a
200 ms budget refuses `:git-timeout` in ~200 ms and the child is **killed and reaped** —
verified by `ps -eo pid,args`, not by `pgrep -f`, which self-matches.
(`probe-git/e2, e3, e4`; `probe-git2/e2b`)

---

## Executed tests and probes

All runs: `~/bin/suite-run` (nice 10), isolated `CLJ_SURGEON_EVENTS_FILE` per run,
`clojure -J-Xmx512m -J-XX:ActiveProcessorCount=2 -M:clj-surgeon/test-deps[:opus-probe]`.
Cold JVM gate only; no inherited nREPL was used and no full parallel suite was invoked.

**Baseline — the branch's own six Git namespaces**

```
clojure … -M:clj-surgeon/test-deps -m clj-surgeon.mcp-test-runner --ns \
  clj-surgeon.mission-git-test clj-surgeon.mission-git-boundary-test \
  clj-surgeon.mission-git-fence-test clj-surgeon.mission-git-process-test \
  clj-surgeon.mission-git-ledger-test clj-surgeon.mission-commit-cli-test
→ Ran 22 tests containing 156 assertions. 0 failures, 0 errors.
  test-isolation: 0 violations across 6 namespaces.
```

This matches the count claimed in `mission-git-ledger.md`. I ran it; I did not merely read it.

**Reviewer probes** (`review-scratch/probe/opus/`, added via
`-Sdeps '{:aliases {:opus-probe {:extra-paths ["review-scratch/probe"]}}}'`, not committed)

| namespace | tests | assertions | failures |
|---|---|---|---|
| `opus.probe-git` + `opus.probe-git2` | 27 | 86 | **4** (Findings 1 ×2 vectors, 2, 3) |
| `opus.probe-ledger` + `opus.probe-final` | 12 | 118 | 0 |

The four failures are the intended repros: each probe asserts the safe expectation, so the
failure text *is* the finding's expected/observed pair.

Shell-level measurements taken alongside: `git diff --cached --name-only -z` under
`diff.ignoreSubmodules` ∈ {`none`, `untracked`, `dirty`, `all`} on git 2.53.0; ref backend
confirmed `files`; `clojure.edn/read-string` trailing-form tolerance.

**Fixture hygiene.** Every probe repository was created under `java.io.tmpdir`
(`/var/tmp/forge` on this seat) or under
`/var/tmp/forge/astra-opus-git-review-fx/review-scratch/fx`, and removed in `finally`.
Post-run scan found no leaked `opus-*`, `typist-executor-test-*` or `mission-git-*`
directories (the pre-existing `/var/tmp/forge/opus-arms`, dated 2026-09-05, is not mine).
Nothing was written to `/tmp`.

---

## Limitations

- **Not probed:** cross-process lock contention (two `bin/mission commit` processes racing) —
  the deployed shape; Finding 3 is measured only in-JVM. Also unprobed: the `reftable` ref
  backend, non-Linux filesystems, a repository containing a genuinely checked-out submodule,
  and case-insensitive path collisions.
- **Finding 7 is by inspection, not execution.** I could not make `FileChannel.close` throw.
- **Finding 5 is environment-dependent** on this seat's absent global Git identity; the
  ordering issue is real either way, the observed error type is not portable.
- I did not attempt any service or network exploitation, spawn any model, contact any
  provider, touch any real user mission, or perform any push or merge. `main` and `MCP/main`
  were not written to and the reviewed worktree's HEAD is unchanged at `9a450631`.
- No implementation, test, or shared-worktree file was modified. My probe sources and this
  report are the only additions; the probes live in untracked `review-scratch/` and are not
  committed.
- I reviewed `clj_surgeon/mission.clj` only for the hashing and state-path helpers the seam
  depends on (`sha256`, `snapshot`, `workspace-state-dir`, `mission-file`, `help-text`), and
  `mission_cli.clj` only for `parse-flags`, `commit-options`, `commit!` and `-main` dispatch.
  The rest of the mission kernel was out of scope.
- **Adjacent observation, not a finding:** `test/clj_surgeon/mission_git_boundary_test.clj:14`
  hard-codes `/var/tmp/forge` as its fixture base rather than `java.io.tmpdir`. It works on
  this seat, but `CLAUDE.md`'s test doctrine says temp dirs come from `java.io.tmpdir`, and
  four other Git namespaces inherit that fixture.

## Recommendation

Fix Finding 1 and re-run the two probe vectors (`probe-git/b5`, `probe-git2/f1`) plus the
branch's own six namespaces. Findings 2–7 are follow-ups that do not need to gate the seam.
I applied no code fixes.
