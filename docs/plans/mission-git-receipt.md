# Verified mission to Git: restricted explicit-stage seam

The kernel's verified source mutation and a Git commit are separate events.
This first seam commits an already-staged, exact verified mission change. It
does not stage files, rewrite source, push, or run Git hooks. **`commit-tree`
deliberately skips ordinary `git commit` hooks and signing defaults.** An
independent successful mission proof is mandatory; this seam cannot claim those
ordinary commit semantics. The CLI must disclose this contract before invocation.

The pure planner receives a normalized trusted mission receipt plus observations
obtained independently by the integration boundary. Mission provenance contains
id, canonical workspace root, ledger digest, proof digest, and per-file before
and verified-after SHA-256 hashes. Observations contain canonical root, symbolic
branch, full HEAD oid, exact index tree oid, staged path set, and regular-file
HEAD/index/live hashes and modes. The boundary must reject symlinks and special
files before reading them, disable Git pathspec magic and environment overrides,
and resolve the actual Git top level rather than trusting a command-line root.

Admission requires a verified receipt with distinct successful gate and acceptance
evidence, exact current ledger digest, matching canonical root, a supported local
branch, and nonempty bounded changed paths. Both `main` and `MCP/main` refuse.
Every mission HEAD hash must equal its preimage; index and live hashes must equal
the verified postimage; staged paths must equal mission changed paths exactly.
Only modifications of regular existing files with unchanged mode are supported.
Creation, deletion, rename, submodules, executable-bit change, and detached HEAD
refuse. Unrelated unstaged files are permitted; unrelated staged paths refuse.

The generated commit body contains only bounded allowlisted provenance, never
source, prompts, intent text, provider output, or arbitrary receipt metadata.
The executor reobserves immediately before creation. It passes the generated
message on stdin to `git commit-tree TREE -p HEAD -F -`, then advances the named
branch using `git update-ref REF NEW OLD`. The old oid is a compare-and-swap
guard. Failure after object creation can leave an unreachable object, never a
success claim. The real adapter serializes cooperating calls with a lock in the
worktree Git directory, and repeats all observations before object creation and
ref advancement. External source/index/ledger/Git writers may ignore this lock.
The return explicitly distinguishes Git ref mutation from
kernel source mutation and declares skipped hooks.

This lane provides the pure planner, injected executor, and actual bounded argv,
Git/index/live observation and repository locking adapters. Root owns normalized
saved-ledger extraction, its reread/hash callback, CLI help and wiring. The seam
is not yet exposed as a production command. Git `write-tree` may refresh internal
index cache metadata; no staging or staged-content mutation occurs. Source blobs
must be UTF-8 and at most 1 MiB; every subprocess has a 10-second wall limit and
1 MiB output cap enforced while reading. Environment Git overrides are removed;
fsmonitor, hooks and automatic signing are disabled for these subprocesses.

An update-ref failure or timeout returns `:git-ref-updated :unknown` with a
`:possible-commit` oid. It does not claim the branch stayed unchanged: a process
could finish updating the ref before a timeout/error is observed. The caller
must inspect the named branch before retrying. Earlier refusals report false.

Root wiring calls `(commit! normalized-provenance ledger-current?)`. The callback
must reread the exact saved ledger and compare its digest to the normalized
`:ledger-sha256` on every invocation. Normalized provenance is:

```clojure
{:id "M-0001" :state :verified :workspace-root "/canonical/repository"
 :ledger-sha256 "<64 lowercase hex>" :receipt-sha256 "<64 lowercase hex>"
 :gate {:ok true :sha256 "<gate evidence digest>"}
 :acceptance {:ok true :sha256 "<distinct independent evidence digest>"}
 :files {"src/a.clj" {:before-sha256 "<HEAD/preimage digest>"
                      :after-sha256 "<verified live/index digest>"}}}
```

The adapter rejects paths containing whitespace, control characters, colon,
backslash, or dot traversal components. It also refuses non-regular blobs, file
creation/deletion, executable-mode changes and unsupported branch names. These
are deliberately narrow initial constraints, not claims that Git lacks them.

Validation matrix: happy plan and exact argv/stdin; missing/unverified receipt;
wrong root/ledger/hash; extra or missing staged files; invalid path/ref/oid;
creation/deletion/mode changes; both frozen branches; distinct proof evidence;
reobservation drift; commit-tree failure/invalid output; update-ref CAS failure;
success provenance and no source/staging operations. Scratch fixture commits are
authorized tests; root reviews before any user's mission is committed through
the feature.

Validation at implementation: 13 BB tests / 80 assertions green, including exact
real scratch Git commit/tree/body/source checks; unrelated staged path, stale
ledger/live source, missing source, symlink and frozen-branch refusals; output
cap; hooks skipped; uncertain ref result and metadata omission. Fixtures derive
from the mission-forms protected-comment/adjacent-owner shape and are removed
in finally. These are synthetic boundary tests, not live mission dogfood. A first
scratch run exposed BB's unsupported FileLockImpl.release method after a successful
ref update; releasing through FileChannel.close fixed that erroneous failure
report, and the real invocation regression now exercises the whole lifecycle.

Review hardening: stdin delivery now occurs asynchronously under the same
monotonic deadline as process exit/output capture. A nonreading Python child
with 2 MiB stdin times out at a 150 ms budget and is killed/reaped. Git author and
committer identity are preflighted; absence yields `:git-identity-unavailable`
with an explicit repository-local configuration next action, never guessed or
silently installed identity. Full seam: 15 tests / 85 assertions. Test lanes:
mission-git-test :fast (4); boundary :battery (4); fence :battery (5); process
:battery (2). Source and staged-content preservation remain unchanged.
