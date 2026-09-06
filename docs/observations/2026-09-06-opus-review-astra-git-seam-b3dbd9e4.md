# Independent executed review — b3dbd9e4 "Expose saved-proof Git publication through mission commit"

Reviewer: independent seat (Sol's review failed at the service level; no evidence supplied).
Worktree: /home/forge/src/clj-surgeon-review-b3dbd9e4 (detached review/b3dbd9e4 @ b3dbd9e4df251258eec869d54d037f6541e7dc5c)
Scratch: /var/tmp/forge/review-b3dbd9e4-fx. No source edits, no commit, no push, no model calls, no forbidden ports.
Method: execution. Every claim below is backed by a quoted command output.

---

## 1. What the Git seam actually does

`git show --stat b3dbd9e4`:

    bin/mission                                  |  22 +++++-
    docs/mission-typist.md                       |  21 ++++++
    docs/plans/mission-git-ledger.md             |  34 ++++++++-
    docs/plans/mission-git-receipt.md            |   2 +-
    src/clj_surgeon/mission.clj                  |   1 +
    src/clj_surgeon/mission_cli.clj              |  31 +++++++-
    test/clj_surgeon/mission_commit_cli_test.clj | 108 +++++++++++++++++++++++++++

The commit is only the **CLI exposure**. The seam itself (`mission_git.clj`, `mission_git_ledger.clj`,
`mission_git_process.clj`) already existed on the branch; this commit adds `commit-options` / `commit!`
in `mission_cli.clj`, routes `commit` through `bin/mission`, and adds help text.

**Which git commands run, and how.** `mission-git/run-git!` prefixes every invocation:

    git --no-optional-locks -c core.fsmonitor=false -c core.hooksPath=/dev/null -c commit.gpgSign=false <argv>

`mission-git-process/run-process!` uses `ProcessBuilder` with an argv vector — **no shell**, no string
command. `.directory` = the mission's canonical `workspace-root`. It **removes every `GIT_*` env var**,
sets `GIT_TERMINAL_PROMPT=0` and `LC_ALL=C`, caps output at 1 MiB, and enforces one 10 s monotonic
deadline covering stdin, wait and capture.

The argv set is closed: `rev-parse --show-toplevel` / `--verify HEAD` / `--absolute-git-dir`,
`symbolic-ref -q HEAD`, `write-tree`, `diff --cached --name-only -z`, `ls-tree`, `ls-files --stage`,
`show`, `var GIT_AUTHOR_IDENT|GIT_COMMITTER_IDENT`, `commit-tree <tree> -p <head> -F -`, `update-ref`.

- **Staging: none.** There is no `git add` and no pathspec. The seam *refuses* unless the index
  already contains exactly the mission's owned file set (`plan` requires
  `(= (set staged-paths) (set (keys files)))`). The commit object is built from `write-tree`, i.e. the
  index as the operator staged it.
- **Message:** built only from the mission id and four SHA-256s plus one `Verified-File:` line per file.
  No free-text field from the receipt reaches it.
- **Push: never.** No `push`, `remote`, `fetch` or URL appears anywhere in the three namespaces.
- **Hooks/signing: disabled twice over** — `core.hooksPath=/dev/null` *and* `commit-tree`, which runs no
  hooks by construction. `commit.gpgSign=false`.
- **Branch guard:** `branch?` refuses `refs/heads/main` and `refs/heads/MCP/main` outright.
- **Concurrency:** a `FileChannel.tryLock` on `<git-dir>/mission-commit.lock`; the observation is taken,
  re-taken after `commit-tree`, and any drift throws `:git-observation-drift`.
- **Identity:** `git var GIT_AUTHOR_IDENT` / `GIT_COMMITTER_IDENT` must be non-blank, else
  `:git-identity-unavailable` with a decision string. See defect 1.

---

## 2. Safety probes (all executed)

Harness: `/var/tmp/forge/review-b3dbd9e4-fx/probe.clj` and `probe-g.clj`, run as
`~/bin/suite-run clojure -M:clj-surgeon/test-deps -i <probe>` from the review worktree, building a
**real** saved verified mission (`cli/propose!` + `cli/apply!` over the repo's own typist fixture) in a
throwaway git repo, then driving the **public** `bin/mission commit`.

    PROBE fixture-verified             :verified
    PROBE h-escape-refusal             :git-invalid-provenance
    PROBE h-git-calls-made             0
    PROBE h-path?-dotdot               false
    PROBE h-path?-abs                  false
    PROBE h-path?-dotgit               false
    PROBE a-refuses-when-nothing-staged [1 :git-staged-scope]
    PROBE b-refuses-extra-staged       [1 :git-staged-scope]
    PROBE commit-exit                  [0 true false false false false]
    PROBE b-committed-paths            ["src/fixture/core.clj"]
    PROBE b-untracked-still-untracked  true
    PROBE b-unrelated-blob-unchanged   true
    PROBE c-author                     "Gene Kim <genek@itrevolution.com>"
    PROBE c-committer                  "Gene Kim <genek@itrevolution.com>"
    PROBE c-env-GIT_AUTHOR_NAME        "forge-anvil"
    PROBE c-env-GIT_AUTHOR_EMAIL       "forge-anvil@anvil"
    PROBE d-has-mission-id             true
    PROBE d-has-ledger-sha             true
    PROBE d-has-receipt-sha            true
    PROBE d-provider-named             false
    PROBE d-key-shaped-present         false
    PROBE replay-second-commit         [1 :git-staged-scope]
    PROBE e-undo-exit                  0
    PROBE e-git-head-unchanged-by-undo true
    PROBE e-source-reverted            true
    PROBE e-git-tree-still-has-new-name true
    PROBE f-branch-refuses-main        false      (branch? => false means REFUSED)
    PROBE f-branch-refuses-mcp-main    false      (refused)
    PROBE f-branch-allows-other        true

Clean re-run of probe (g), on a still-`:verified` mission with a real `pre-commit` hook installed that
appends to a witness file and exits 1:

    PROBE state                          :verified
    PROBE g-readonly-exit                1
    PROBE g-readonly-result              {:error-type :git-boundary-failed, :git-ref-updated false,
                                          :index-staging false, :source-mutation-attempted false,
                                          :hooks-run false}
    PROBE g-head-unchanged               true
    PROBE g-source-unchanged             true
    PROBE g-index-still-staged           true
    PROBE g-recovery-exit                0
    PROBE g-recovery-ref                 true
    PROBE g-hook-fired                   ""

The full commit message written by the seam:

    Record verified mission M-1

    Mission: M-1
    Ledger-SHA256: 044b1084825b00fdcfc4ccaef5553899f637bc30df0ae00c0b236a60a202b8d7
    Receipt-SHA256: 21aeab453a62896f8ca9645d2a93957968073e6f7867042bb2565d792fe8682c
    Gate-SHA256: 1a44e85a2f335a1a1823c6f2d0d354053a90779cd939b12a11190f98abcc3d76
    Acceptance-SHA256: bf708a5bb53fcc5df0171e8ce11215aa5447e6f8930a866a2d39fdbb0ae71af1
    Hooks: skipped (commit-tree)
    Signing: not requested
    Verified-File: src/fixture/core.clj 2a8d074d338dbeb2393923137e38892ed2d198e940a9442638f8d4f6ea147a87

### Per-probe result

| # | Probe | Result |
|---|---|---|
| a | Refuses when the mission's files are not staged (tree dirty elsewhere) | **PASS** — exit 1, `:git-staged-scope`. Note: an unrelated *unstaged* modification does not block the commit, which is correct — the commit is `write-tree` over the index, so that file's blob is unchanged (`b-unrelated-blob-unchanged true`). |
| b | Stages only the mission's files | **PASS** — an extra *staged* file is refused `:git-staged-scope`; the resulting commit's `diff-tree` is exactly `["src/fixture/core.clj"]`; the untracked file stayed untracked; nothing was ever added by the seam. |
| c | Author/committer is the seat identity, never "Gene Kim" | **FAIL — defect 1.** Env carried `GIT_AUTHOR_NAME=forge-anvil`; the commit landed `Gene Kim <genek@itrevolution.com>`. |
| d | Message carries the receipt; no key-like string leaks | **PASS on secrets, PARTIAL on content.** id + ledger/receipt/gate/acceptance SHA-256 + per-file hashes are present; `sk-…`/`AKIA…`/`ghp_…` cannot appear because no free-text receipt field reaches the message. **But no provider/executor is named** (`d-provider-named false`). |
| e | Undo reverts the git commit or refuses with a typed reason | **FAIL — defect 2.** `bin/mission undo` exits 0, reverts the source, and leaves the published commit as HEAD. Dangling state confirmed: working tree back to `old-name`, git tree at HEAD still `new-name`. Neither reverted nor refused. |
| f | Never pushes | **PASS** — no `push`/`remote`/`fetch`/URL in the seam; closed argv set; `GIT_TERMINAL_PROMPT=0`; `refs/heads/main` and `MCP/main` refused. |
| g | Git failure → typed refusal, consistent state; hooks cannot run | **PASS** — read-only `.git` gives exit 1 and `:git-boundary-failed` with `git-ref-updated false, index-staging false, source-mutation-attempted false`; HEAD, source and index all unchanged; the same command succeeds after write is restored. The installed `pre-commit` hook **never fired**. |
| h | Escaping path refuses before any git call | **PASS** — `:git-invalid-provenance` with **0 git subprocesses spawned** (`run-git!` redefined to count). `path?` rejects `..`, absolute paths and `.git`. |
| — | Replay | **PASS (bonus)** — a second `commit` of the same mission refuses `:git-staged-scope`; no double publication. |

---

## 3. Babashka native routing / fallback

`bin/mission` sends read verbs (`show list ready blocked help`) and **unknown** verbs to
`bb --classpath src bin/mission-read.clj`; write verbs (now including `commit`) fall through to the JVM.
This commit rewrote the verb-finding loop so global option *pairs* before the verb are skipped, matching
`mission-cli/parse-flags` (flag + non-flag token = pair; flag + flag = boolean).

Executed routing parity — three argv shapes that must all reach the JVM identically, plus the bb path:

    [commit --help]                               ms=110   -> bb (help text)
    [--workspace /nope commit M-9]                ms=5113  -> JVM, {:error-type :git-ledger-artifact-unavailable, :contract "Git ref p…
    [commit M-9 --workspace /nope]                ms=5013  -> JVM, {:error-type :git-ledger-artifact-unavailable, :contract "Git ref p…
    [--state-home /h commit M-9 --workspace /nope] ms=5013 -> JVM, {:error-type :git-ledger-artifact-unavailable, :contract "Git ref p…
    [commit]                                      ms=5113  -> JVM, {:error-type :mission-commit-options, :contract "Git ref publicatio…

    $ bin/mission commit M-9 --workspace /nope >/dev/null 2>&1; echo exit=$?
    exit=1
    $ bin/mission commit --help | grep -c "stages nothing"   -> 1
    $ bin/mission help commit  | grep -c "stages nothing"    -> 1

**Parity holds and the fallback is not silent.** All three global-option orderings produce byte-identical
JVM refusals at ~5 s; the bb path answers help in 110 ms. I could not make bb take over a write verb:
the `case` allowlist sends only the five read verbs to bb, `commit` is explicitly in the JVM arm, and an
unknown verb reaches bb only to print help and exit 2. Refusals exit 1, not 0.

One cosmetic divergence: the `--help` pre-scan tests `$1` rather than the parsed verb, so
`bin/mission --workspace R commit --help` prints the general help instead of the commit-specific help.
No safety consequence.

---

## 4. Tests

**Namespaces this commit touched**, forced to run (see defect 3 for why they had to be forced):

    $ ~/bin/suite-run clojure -M:clj-surgeon/test-deps -e "(run-tests 'clj-surgeon.mission-commit-cli-test
        'clj-surgeon.mission-git-test 'clj-surgeon.mission-git-ledger-test
        'clj-surgeon.mission-git-boundary-test 'clj-surgeon.mission-git-fence-test
        'clj-surgeon.mission-git-process-test)"

    Ran 22 tests containing 156 assertions.
    0 failures, 0 errors.

That matches the commit message's "Six Git namespaces:22 tests/156 assertions" exactly.

**Fast lane:**

    $ ~/bin/suite-run clojure -M:clj-surgeon/test-fast
    Ran 486 tests containing 4802 assertions.
    6 failures, 0 errors.
    test-isolation: 0 violations across 46 namespace(s)

All six failures are in `clj-surgeon.lane-manifest-test`:

    FAIL in (every-test-namespace-on-disk-is-accounted-for) (lane_manifest_test.clj:88)
    expected: (empty? unaccounted)
      actual: (not (empty? (clj-surgeon.mission-commit-cli-test clj-surgeon.mission-git-boundary-test
                            clj-surgeon.mission-git-fence-test clj-surgeon.mission-git-ledger-test
                            clj-surgeon.mission-git-process-test clj-surgeon.mission-git-test)))
    FAIL in (the-corpus-only-ever-grows-and-the-arithmetic-is-shown) (lane_manifest_test.clj:481)
      actual: (not (= 7 9))
    ... (= 343 adopted) -> 345 ;  (= 1264 total) -> 1266

And the lane runner refuses to run them at all:

    lane-refused: clj-surgeon.mission-commit-cli-test carries no lane declaration. Every JVM test
    namespace must appear in clj-surgeon.lane-manifest/manifest ... (TEST-ISO-001)

**Baseline check** — the parent already carried five of the six unaccounted:

    $ git show b3dbd9e4^:test/clj_surgeon/lane_manifest.clj | grep -n "mission-git\|mission-commit"
    (no output)

So the red fast lane is **pre-existing on this branch**; b3dbd9e4 adds a sixth orphan and does not fix it.

---

## 5. Defects

**1. The commit is authored as Gene Kim, not the seat.** `run-process!` deletes *every* `GIT_*`
environment variable, so `git var GIT_AUTHOR_IDENT` falls back to `git config user.name/user.email`.
Executed: with `GIT_AUTHOR_NAME=forge-anvil` / `GIT_AUTHOR_EMAIL=forge-anvil@anvil` live in the seat
environment, the published commit is `Gene Kim <genek@itrevolution.com>` for both author and committer.
This defeats the fleet's *only* seat-identity mechanism, which is `GIT_AUTHOR_*`/`GIT_COMMITTER_*` env
vars precisely because seats share clones and a repo-level `user.email` cannot distinguish them. The
house rule is explicit: agent seats never author commits as Gene. A machine-published "verified mission"
commit is exactly the artifact whose actor identity must be unrecoverable-if-not-written-at-commit-time.
Note the seam's stripping is otherwise sound (it must neutralise `GIT_DIR`, `GIT_INDEX_FILE`,
`GIT_CONFIG_*`); the bug is that the allowlist is empty rather than that the blocklist exists.

*Fix:* preserve or re-inject an explicit identity — either allowlist
`GIT_AUTHOR_NAME/EMAIL/DATE` + `GIT_COMMITTER_NAME/EMAIL/DATE` through `run-process!`, or pass
`-c user.name=… -c user.email=…` from a required seat-identity option and refuse when it is absent
(the existing `:git-identity-unavailable` refusal is the right shape to extend). Add a witness that
sets the env to a seat identity, sets repo config to a *different* name, and asserts
`git log -1 --format='%an <%ae>'` is the seat.

**2. `undo` leaves a published ref standing, silently.** After a successful `mission commit`,
`bin/mission undo` exits 0, reverts the source file, and does not touch, mention, or refuse on account
of the git commit. Executed: HEAD unchanged, working tree back to `old-name`, HEAD tree still
`new-name`. The result is a git ref asserting "Record verified mission M-1" with `Verified-File` hashes
for a state that no longer exists, and nothing in the ledger records that a ref was published.

*Fix:* record the published commit oid on the mission ledger at `git/execute!` success, and have `undo!`
either (a) refuse with a typed reason — e.g. `:mission-undo-after-git-publication` naming the oid — or
(b) proceed but report the published oid in the undo receipt so the divergence is visible. Silent
success is the one option that should not remain.

**3. This commit's own test runs in no lane.** `mission-commit-cli-test` carries `{:lane :battery}` in
its ns metadata but is absent from `clj-surgeon.lane-manifest/manifest`, so the runner refuses it and
`lane-manifest-test` fails on it. Its 22 tests/156 assertions pass when forced, but no lane the merge
gate runs will ever execute them, and the fast lane is red. Pre-existing for the other five namespaces;
this commit extends it. *Fix:* add all six to the lane manifest (and update the corpus arithmetic the
test pins), in this commit or an immediate follow-up.

**4. (minor) The message names no provider/executor.** The brief's receipt contract is id + proof +
provider; the message carries id and proof hashes but nothing identifying the executor
(`:executor :typist`) or the candidate provider. This is a defensible tradeoff — the empty free-text
surface is exactly why no key can leak — so if provider is added it should be a closed enumerated
value, not a passthrough string.

---

## VERDICT: GO-WITH-FIX

The seam itself is genuinely well built and survived every adversarial probe I could execute: it stages
nothing, commits exactly and only the mission's verified files, cannot run hooks, cannot sign, cannot
push, refuses `main`, refuses an escaping path before spawning a single git process, refuses a replay,
and fails typed and state-consistent under a read-only `.git`. No shell, closed argv, bounded output,
locked, drift-checked. Routing parity between the bb and JVM paths is exact and the fallback is never
silent.

Two fixes must land before this is used on a real repository:

- **Fix 1 (blocking): author/committer identity.** Allowlist `GIT_AUTHOR_*`/`GIT_COMMITTER_*` through
  `run-process!`, or require an explicit seat identity via `-c user.name/user.email`. As shipped, this
  seam authors machine commits as Gene Kim.
- **Fix 2 (blocking): undo after publication.** Record the published oid and make `undo` either refuse
  with a typed reason or report the divergence. It currently exits 0 and leaves a dangling ref.

Plus **Fix 3 (housekeeping):** register the six git test namespaces in the lane manifest so the fast lane
is green and this commit's evidence actually runs.

No counterexample rises to HOLD: nothing I could execute made the seam write source, stage a file, run a
hook, push, or commit anything outside the mission's owned set.
