# Independent executed RE-REVIEW — ebbf4389 (astra/typist-route)

Reviewer: independent seat (forge@anvil). Method: execution only; every claim below is backed by a
quoted command output.
Worktree: `/home/forge/src/clj-surgeon-review-ebbf4389` (branch `review/ebbf4389` @
`ebbf438958acce4a7e1177362849bec6271bf824`). Scratch: `/var/tmp/forge/review-ebbf4389-fx`.
No source edits, no commit, no push, no model calls, no forbidden ports. Battery not run (Astra owns it).

Predecessor review: `docs/observations/2026-09-06-opus-review-astra-git-seam-b3dbd9e4.md` (GO-WITH-FIX,
three defects). This pass re-executes the three fixes plus the earlier passing probes.

## Setup

    $ git -C /home/forge/src/clj-surgeon-records fetch origin astra/typist-route
    From https://github.com/realgenekim/clj-surgeon
     * branch              astra/typist-route -> FETCH_HEAD

    $ /home/forge/bin/worktree-add /home/forge/src/clj-surgeon-records \
        /home/forge/src/clj-surgeon-review-ebbf4389 review/ebbf4389 ebbf4389
    worktree /home/forge/src/clj-surgeon-review-ebbf4389 branch review/ebbf4389
      HEAD ebbf438958acce4a7e1177362849bec6271bf824 (trusted)

The three repairs, isolated:

    $ git show 8ec4071a --stat   # "Preserve explicit seat Git identity without config overrides"
     src/clj_surgeon/mission_git_process.clj           |  4 ++-
     test/clj_surgeon/mission_git_identity_fixture.clj | 17 +++++++++
     test/clj_surgeon/mission_git_identity_test.clj    | 42 +++++++++++++++++++++++
    $ git show 189e0086 --stat   # "Refuse staged gitlinks hidden by Git submodule configuration"
     src/clj_surgeon/mission_git.clj                 |  2 +-
     test/clj_surgeon/mission_git_submodule_test.clj | 38 +++
    $ git show e50c4403 --stat   # "Block silent source undo after Git publication ..."
     src/clj_surgeon/mission.clj                   |  33 ++---
     src/clj_surgeon/mission_cli.clj               |  86 ++++++-------
     src/clj_surgeon/mission_git_ledger.clj        | 150 ++++++++++++++++++++++-
     test/clj_surgeon/mission_publication_test.clj | 169 ++++++++++++++++++++++++++

The identity repair is an exact six-name allowlist inside `run-process!`:

    -    (doseq [k (vec (.keySet env)) :when (str/starts-with? k "GIT_")] (.remove env k))
    +    (doseq [k (vec (.keySet env)) :when (and (str/starts-with? k "GIT_")
    +                                          (not (contains? #{"GIT_AUTHOR_NAME" "GIT_AUTHOR_EMAIL" "GIT_AUTHOR_DATE"
    +                                                            "GIT_COMMITTER_NAME" "GIT_COMMITTER_EMAIL" "GIT_COMMITTER_DATE"} k)))] (.remove env k))

The scope repair is the literal flag:

    -     :staged-paths (... (run ["diff" "--cached" "--no-ext-diff" "--name-only" "-z" "--"] nil) ...)
    +     :staged-paths (... (run ["diff" "--cached" "--no-ext-diff" "--ignore-submodules=none" "--name-only" "-z" "--"] nil) ...)

Harness: `/var/tmp/forge/review-ebbf4389-fx/probe.clj` and `probe2.clj`, run as
`nice -n 10 ~/bin/suite-run clojure -M:clj-surgeon/test-deps -i <probe>` from the review worktree.
Same shape as the b3dbd9e4 pass: a **real** saved verified mission (`cli/propose!` + `cli/apply!`
over the repo's own typist fixture) in a throwaway git repo, driven through the **public**
`bin/mission` binary. `bin/mission` is launched via `ProcessBuilder` so the child's `GIT_*`
environment can be added to and removed from per case.

---

## VECTOR 1 — identity

### 1a. Which `GIT_*` variables survive the strip

The probe JVM was launched with eleven `GIT_*` variables exported, then called `run-process!`
directly on `["env"]`:

    PROBE jvm-env-git-vars    ("GIT_AUTHOR_DATE" "GIT_AUTHOR_EMAIL" "GIT_AUTHOR_NAME" "GIT_COMMITTER_DATE"
                               "GIT_COMMITTER_EMAIL" "GIT_COMMITTER_NAME" "GIT_CONFIG_GLOBAL" "GIT_DIR"
                               "GIT_EDITOR" "GIT_INDEX_FILE" "GIT_SSH_COMMAND")
    PROBE surviving-GIT-vars  ("GIT_AUTHOR_DATE" "GIT_AUTHOR_EMAIL" "GIT_AUTHOR_NAME" "GIT_COMMITTER_DATE"
                               "GIT_COMMITTER_EMAIL" "GIT_COMMITTER_NAME" "GIT_TERMINAL_PROMPT")

**Exactly the six identity variables survive.** `GIT_DIR`, `GIT_INDEX_FILE`, `GIT_CONFIG_GLOBAL`,
`GIT_SSH_COMMAND` and `GIT_EDITOR` are all stripped; `GIT_TERMINAL_PROMPT=0` is injected as before.

### 1b. Seat identity wins over a conflicting repo config; injected `GIT_DIR`/`GIT_INDEX_FILE` are ignored

Repo config was `Gene Kim <genek@itrevolution.com>`; the child was additionally poisoned with
`GIT_INDEX_FILE=<root>/poison-index`, `GIT_DIR=/var/tmp/forge/review-ebbf4389-fx/poison.git`,
`GIT_CONFIG_GLOBAL=<a config declaring user POISON>`:

    PROBE b-fixture-state          :verified
    PROBE b-commit-exit            0
    PROBE b-poison-index-created   false
    PROBE b-author                 "forge-anvil <forge-anvil@anvil>"
    PROBE b-committer              "forge-anvil <forge-anvil@anvil>"
    PROBE b-repo-config-name       "Gene Kim"
    PROBE b-committed-paths        ["src/fixture/core.clj"]

**PASS.** Author *and* committer are the seat. The poisoned index file was never created, the poisoned
`GIT_DIR` had no effect (the commit landed in the fixture repo), and the poisoned global config's
`POISON` identity never appeared. Defect 1's executed reproduction — `Gene Kim` in both fields with
`GIT_AUTHOR_NAME=forge-anvil` live — no longer reproduces.

### 1c. Seat env absent, repo config present

    PROBE c-exit             0
    PROBE c-ok               true
    PROBE c-error-type       nil
    PROBE c-git-ref-updated  true
    PROBE c-author           "Gene Kim <genek@itrevolution.com>"
    PROBE c-committer        "Gene Kim <genek@itrevolution.com>"

**It does NOT refuse — it falls back to repository configuration and commits.** The brief asked for
`:git-identity-unavailable` here. This is a deliberate, *documented* design choice, not an oversight;
`docs/plans/mission-git-receipt.md` in 8ec4071a states it in as many words:

    With the six variables absent, ordinary explicit local configuration behavior remains; the tool
    cannot infer the intended human/agent author from a name. Fleet seats must continue exporting
    their own identity, as required by the Anvil resume/house rule.

Judgement: the *blocking* half of defect 1 is fixed — an exported seat identity can no longer be
silently overwritten. The residual is that a seat which forgets to export still publishes machine
commits as `Gene Kim` in any shared clone whose config carries his name, which is exactly the
condition the house rule exists for. `git var GIT_AUTHOR_IDENT` cannot distinguish "config" from
"env", so the current preflight cannot detect it; refusing would require comparing the resolved ident
against the env vars. **Finding 1 (non-blocking, recommended follow-up), not a re-block.**

---

## VECTOR 2 — publication / undo

### 2a. Undo after a successful publication

    PROBE b-undo-exit                1
    PROBE b-undo-out
      {:operation "mission",
       :mutation_attempted false,
       :published-commit "651f58613fd3a1eb97a113f1a9c8977fa29c836f",
       :error_type "mission-undo-after-git-publication",
       :id "M-1",
       :ok false,
       :publication-status :published,
       :error "Source undo is blocked because Git publication succeeded or requires recovery.
               Git will not be undone automatically; inspect the branch and publication records."
       :example {:argv ["bin/mission" "show" "M-1" "--workspace" ... "--state-home" ...] ...}}
    PROBE b-undo-head-unchanged      true
    PROBE b-source-still-mutated     true

**PASS — defect 2 is closed.** Undo now exits **1** with a typed refusal that names the published oid,
asserts `mutation_attempted false` / `source-mutation-attempted false`, and hands back a runnable
`show` example. No silent success. (Previously: exit 0, source reverted, ref left standing.)

### 2b. The ledger carries the published oid

    PROBE b-ledger-git-publication
      {:version 1, :id "M-1", :workspace-root "/var/tmp/forge/typist-executor-test-3386801126322596536",
       :ledger-sha256 "46cb8094f6a99f6774d6e89917f9bc0dd6711f7c77d90668d81919bb3eaf5f51",
       :status :published, :commit "651f58613fd3a1eb97a113f1a9c8977fa29c836f",
       :tree "eb35d1478bcb19539523cbfd1bbfac483f9f85f0",
       :parent "94c88201251b1e3f8cc5687975836f51ddd07092"}
    PROBE b-sidecar                  <byte-identical to the above>
    PROBE b-ledger-next-action       nil
    PROBE b-show-exit                0
    PROBE b-show-mentions-publication true
    PROBE b-show-next-action         nil

**PASS.** oid, tree and parent are recorded in *both* the mission ledger and the atomic sidecar, the
stored `next-action` is nulled so nothing prescribes a resume, and `bin/mission show` surfaces the
publication with no next action.

### 2c. Metadata write fails AFTER `commit-tree` (fault injection at the exact seam)

`chmod` cannot express this case: the sidecar *intent* is written before Git, so a read-only state dir
refuses before any commit. I injected the failure where it actually lives, by making
`mission/write-mission!` throw `IOException("No space left on device")` and calling
`mission-git-ledger/commit!` in process:

    PROBE m-ok                     false
    PROBE m-error-type             :git-publication-metadata-failed
    PROBE m-metadata-recorded      false
    PROBE m-git-ref-updated        true
    PROBE m-commit                 "f34a322598118a4e601596f8cc484f3cd65402e1"
    PROBE m-decision               "Git outcome is preserved in this response. Inspect Git and the
                                    publication sidecar before any recovery; source undo remains blocked."
    PROBE m-ref-actually-moved     true
    PROBE m-head-now               "f34a322598118a4e601596f8cc484f3cd65402e1"
    PROBE m-sidecar-exists         true
    PROBE m-sidecar                {:version 1, :id "M-1", ..., :status :published,
                                    :commit "f34a322598118a4e601596f8cc484f3cd65402e1",
                                    :tree "eb35d...", :parent "c43c7..."}
    PROBE m-ledger-has-publication false
    PROBE m-undo-exit              1
    PROBE m-undo-error             "mission-undo-after-git-publication"
    PROBE m-undo-status            :published
    PROBE m-undo-source-still-mutated true
    PROBE m-retry-exit             1
    PROBE m-retry-error            :git-publication-recovery-required
    PROBE m-retry-head-unchanged   true

**PASS.** The state is consistent and typed: the ref *was* updated and the receipt says so truthfully
(`git-ref-updated true` with the real oid), the failure is named `:git-publication-metadata-failed`
with `metadata-recorded false`, the durable sidecar independently records `:status :published` with
the correct oid even though the mission ledger write was lost, undo still refuses off the sidecar
alone, and a retry refuses `:git-publication-recovery-required` rather than double-publishing.
This is the "both or neither, and say which" property the brief asked for.

---

## VECTOR 3 — staged gitlink hidden by submodule configuration

Two independent hiding mechanisms, each on its own fresh fixture. In both the mission's own file was
staged correctly *and* a gitlink was added with
`git update-index --add --cacheinfo 160000,<sha>,sub`.

**(a) `git config diff.ignoreSubmodules all`:**

    PROBE config-ignoreSubmodules-cached-diff-default  "src/fixture/core.clj"
    PROBE config-ignoreSubmodules-cached-diff-none     "src/fixture/core.clj\nsub"
    PROBE config-ignoreSubmodules-exit                 1
    PROBE config-ignoreSubmodules-head-unchanged       true
    PROBE config-ignoreSubmodules-sidecar-cleared      true

**(b) `.gitmodules` with `ignore = all` (plus `diff.ignoreSubmodules dirty`):**

    PROBE gitmodules-ignore-all-cached-diff-default    "src/fixture/core.clj"
    PROBE gitmodules-ignore-all-cached-diff-none       "src/fixture/core.clj\nsub"
    PROBE gitmodules-ignore-all-exit                   1
    PROBE gitmodules-ignore-all-head-unchanged         true
    PROBE gitmodules-ignore-all-sidecar-cleared        true

The two `cached-diff-*` lines are the defect itself, executed: **the default cached diff cannot see
`sub`; `--ignore-submodules=none` can.** The typed refusal, read with a parser that skips the JVM
banner:

    PROBE gitlink-exit         1
    PROBE gitlink-error-type   :git-staged-scope
    PROBE gitlink-out
      {:error-type :git-staged-scope,
       :contract "Git ref publication from saved verified proof; stages nothing, changes no source,
                  skips Git hooks and signing, never pushes.",
       :operation "mission-git-commit", :hooks-run false, :ok false, :git-ref-updated false, ...}
    PROBE gitlink-head-unchanged true

**No commit was created** in either case (`head-unchanged true`). Removing the gitlink lets the same
mission proceed, committing exactly its own path:

    PROBE config-ignoreSubmodules-retry-exit        0
    PROBE config-ignoreSubmodules-retry-head-moved  true
    PROBE config-ignoreSubmodules-retry-paths       ["src/fixture/core.clj"]
    PROBE gitmodules-ignore-all-retry-exit          0
    PROBE gitmodules-ignore-all-retry-head-moved    true
    PROBE gitmodules-ignore-all-retry-paths         ["src/fixture/core.clj"]

**PASS — defect 3 (Astra's own reviewer's) is closed**, and the pre-ref refusal correctly clears the
sidecar so a repaired index can retry.

---

## VECTOR 4 — regressions in the tests, and lane enrollment

Nine namespaces (the six from the last pass plus the three new witness namespaces), forced:

    $ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e "(clojure.test/run-tests
        'clj-surgeon.mission-commit-cli-test 'clj-surgeon.mission-git-test
        'clj-surgeon.mission-git-ledger-test 'clj-surgeon.mission-git-boundary-test
        'clj-surgeon.mission-git-fence-test 'clj-surgeon.mission-git-process-test
        'clj-surgeon.mission-git-identity-test 'clj-surgeon.mission-git-submodule-test
        'clj-surgeon.mission-publication-test)"

    Ran 34 tests containing 263 assertions.
    0 failures, 0 errors.

**Lane enrollment — defect 3 of the previous pass (housekeeping) is closed:**

    $ grep -n "mission-git\|mission-commit\|mission-publication" test/clj_surgeon/lane_manifest.clj
    97:   'clj-surgeon.mission-git-test :fast
    162:   'clj-surgeon.mission-git-boundary-test :battery
    163:   'clj-surgeon.mission-git-identity-test :battery
    164:   'clj-surgeon.mission-git-submodule-test :battery
    165:   'clj-surgeon.mission-publication-test :battery
    166:   'clj-surgeon.mission-git-fence-test :battery
    167:   'clj-surgeon.mission-git-process-test :battery
    168:   'clj-surgeon.mission-git-ledger-test :battery
    169:   'clj-surgeon.mission-commit-cli-test :battery

    $ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e "(clojure.test/run-tests 'clj-surgeon.lane-manifest-test)"
    Ran 25 tests containing 113 assertions.
    0 failures, 0 errors.

All nine are declared, and the manifest test that was 6-red on b3dbd9e4 is green standalone. (The
full fast lane was not run: Astra is running a battery and the brief forbids it.)

---

## VECTOR 5 — the earlier passing probes, re-executed

    PROBE h-escape-refusal            :git-invalid-provenance
    PROBE h-git-calls-made            0
    PROBE h-path?-dotdot              false
    PROBE h-path?-abs                 false
    PROBE h-path?-dotgit              false
    PROBE f-branch-refuses-main       false     (false = REFUSED)
    PROBE f-branch-refuses-mcp-main   false     (refused)
    PROBE f-branch-allows-other       true
    PROBE a-nothing-staged            [1 :git-staged-scope]
    PROBE b-extra-staged              [1 :git-staged-scope]
    PROBE g-readonly-exit             1
    PROBE g-readonly-typed            {:error-type :git-boundary-failed, :git-ref-updated :unknown,
                                       :index-staging false, :source-mutation-attempted false,
                                       :hooks-run false}
    PROBE g-head-unchanged            true
    PROBE g-source-unchanged          true
    PROBE g-hook-fired                ""
    PROBE g-untracked-still-untracked true
    PROBE g-unrelated-blob-unchanged  true
    PROBE g-committed-paths           ["unrelated.txt"]

    $ grep -rn "\"push\"|\"remote\"|\"fetch\"|https://" src/clj_surgeon/mission_git*.clj
    (no output)

**No regression.** An escaping path still refuses `:git-invalid-provenance` with **zero git
subprocesses spawned**; `main` and `MCP/main` are still refused; nothing staged and extra staged both
refuse `:git-staged-scope`; a real executable `pre-commit` hook that appends to a witness file
**never fired**; the push surface is still empty.

### One intentional behaviour change under read-only `.git`

Two lines differ from the b3dbd9e4 pass:

    b3dbd9e4:  :git-ref-updated false   ... g-recovery-exit 0,  ref updated after chmod u+w
    ebbf4389:  :git-ref-updated :unknown ... g-recovery-exit 1, :git-publication-recovery-required
    PROBE g-sidecar-after-readonly {:version 1, :id "M-1", ..., :status :uncertain}

A generic boundary failure now leaves an `:uncertain` sidecar, and every subsequent `commit` and
`undo` for that mission refuses until a human clears it. This is deliberate and documented
(`docs/plans/mission-git-ledger.md`): *"Generic boundary failure is not sufficient evidence of no
publication ... no automatic Git inverse or new recovery command is introduced."* It is the correct
fail-closed reading of an unknown ref outcome and I would not trade it away. **Finding 2
(non-blocking, operability):** a transient, provably-pre-ref failure (a read-only `.git`, a lock
contention, a full disk before `commit-tree`) now wedges the mission permanently, and neither the
refusal text nor `bin/mission help` names the manual step — the operator must know to delete
`<state>/missions/<id>.git-publication.edn` by hand. The refusal's `:decision` should name that file
path explicitly, or a `mission publication-clear <id>` verb should exist that refuses unless the
branch tip demonstrably lacks the mission's tree.

---

## Per-vector result

| # | Vector | Result |
|---|---|---|
| 1a | Exactly six `GIT_*` survive; `GIT_DIR`/`GIT_INDEX_FILE`/`GIT_CONFIG_*` stripped | **PASS** — executed with all eleven exported; injected index file never created, poisoned `GIT_DIR`/global config had no effect |
| 1b | Seat env beats a conflicting repo config | **PASS** — author *and* committer `forge-anvil <forge-anvil@anvil>` with repo config `Gene Kim`. Defect 1's reproduction no longer reproduces |
| 1c | Env absent → refuse, never fall back to config | **DIVERGES, BY DOCUMENTED DESIGN** — commits as `Gene Kim`. Finding 1, non-blocking |
| 2a | Undo after publication refuses typed, names the oid | **PASS** — exit 1, `mission-undo-after-git-publication`, `published-commit` present, `mutation_attempted false`. Defect 2 closed |
| 2b | Ledger carries the published oid | **PASS** — oid/tree/parent in both the ledger and an atomic forced sidecar; `next-action` nulled; `show` surfaces it |
| 2c | Metadata failure after `commit-tree` → consistent and typed | **PASS** — ref truthfully `true` with the real oid, `:git-publication-metadata-failed`, `metadata-recorded false`, sidecar independently `:published`, undo still refuses, retry refuses `:git-publication-recovery-required` |
| 3 | Staged gitlink hidden by `diff.ignoreSubmodules` / `.gitmodules ignore=all` | **PASS** — both hidings executed; default cached diff blind, `--ignore-submodules=none` sees `sub`; refuses `:git-staged-scope`, **no commit created**; removing the gitlink lets it proceed with exactly `["src/fixture/core.clj"]` |
| 4 | Nine git namespaces green; lanes enrolled | **PASS** — 34 tests / 263 assertions, 0 failures 0 errors; all nine in `lane-manifest`; `lane-manifest-test` 25/113 green standalone |
| 5 | Earlier probes: stages nothing, never pushes, escaping path = 0 git spawns, read-only `.git` consistent | **PASS** — no regression; one intentional fail-closed change under read-only `.git` (Finding 2) |

---

## VERDICT: GO

Both blocking defects from the b3dbd9e4 review are closed by execution, the third defect Astra's own
reviewer found is closed by execution against **both** hiding mechanisms, the housekeeping defect is
closed (nine namespaces, all enrolled, green), and none of the earlier safety properties regressed:
the seam still stages nothing, writes no source, runs no hook, cannot push, refuses `main`, refuses an
escaping path before spawning a single git process, refuses a replay, and now fails closed rather than
open when the ref outcome is unknowable.

The publication/undo repair is stronger than what was asked for: the durable sidecar survives a lost
ledger write, so the "ref updated but the record was lost" case — the one that would have produced a
dangling publication nobody could see — is the exact case I injected and it held.

Two non-blocking follow-ups, neither of which should hold the merge:

- **Finding 1 — identity fallback.** With the six variables absent, publication silently authors from
  repository config. Documented and intended, but in any shared clone whose config says `Gene Kim`
  that reproduces the *outcome* defect 1 was filed about, just via a different route. Suggested
  ratchet: compare the resolved `git var GIT_AUTHOR_IDENT` against the inherited env and refuse
  `:git-identity-unavailable` when the env supplied none, behind a flag if a config-authored mode is
  wanted; and a witness asserting the refusal.
- **Finding 2 — no named way out of `:uncertain`.** A transient generic boundary failure wedges the
  mission with no verb and no path in the refusal text. Suggested ratchet: name
  `<state>/missions/<id>.git-publication.edn` in the `:decision`, or add a `publication-clear` verb
  that refuses unless the branch tip demonstrably lacks the mission's tree.
