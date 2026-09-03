# rf2 (5e6cdd2) NO-GO and q5z (2753f23) GO-WITH-FIX — executed red-team of the two write verbs (2026-09-03T02:30Z)

Fences: diffed explicitly on both branches — mcp_paths, mcp_workspace(_sources), file_ops, the SCI
allowlist/`:classes` in edit_dsl and forms `sci-opts`, mcp_process: all UNTOUCHED. Reader: every read of
repo bytes is `clojure.edn/read-string`; q5z adds no reader sites. Subprocess: rf2's compile check is
argv (`ProcessBuilder`) and uses `clojure -Spath -A:` deliberately so `:main-opts`/`:exec-fn` never run
(witnessed with a hostile alias); the branch got that right.

## rf2 — NO-GO (write confinement)

| # | sev | finding (witnessed) | fix |
|---|---|---|---|
| 1 | HIGH | caller-rewire writes `(io/file file)` for every caller plan (extract.clj:1308-1309); paths come from raw `file-seq` (:1177-1184) with no realpath/root check. `proj/vendor -> ../outside`: `atomic-write!` rewrote `outside/other.clj`. (A symlinked FILE is contained; the DIRECTORY symlink escapes.) The discovery walk is pre-existing; rf2 turns the read set into a write set. | route every caller path through `mcp-paths/resolve-source-path` at discovery AND at the write; refuse, never drop silently |
| 2 | HIGH | the receipt's `:command` (extract.clj:459-461, emitted at :626 dry-run and :537 remedy) is a SHELL string with the repo-controlled alias interpolated verbatim, described as "the exact command a reader can run, matching what the apply runs" — it does not match and it executes (`x; touch PWNED; echo` created PWNED when pasted). `namespaces` spliced inside `-e "…"` likewise. | publish `:command` as the argv vector actually executed, or shell-quote every token; delete the false claim |
| 3 | MED | the compile check runs untrusted workspace code by default (`(require 'target.ns)` in a subprocess, default on at :1323) with no stated trust boundary; a chosen alias's `:extra-paths` lands first on the classpath and can shadow the required ns | state the boundary in docstring + receipt; opt-in for repos the operator did not author |
| 4 | MED | compile aliases can come from OUTSIDE the workspace: `forms/find-config-file` (forms.clj:181-194) walks to `/` with no stop | bound at the project root; record `:config-file` in the receipt |
| 5 | MED | compile-failure attribution by basename (:442-443): a foreign `core.clj` in the error text flips `:unverified` to `:ok false` — the receipt then tells the agent to undo correct work | match project-relative paths |
| 6 | LOW | unbounded discovery: symlink-cycle recursion, no file cap, whole-tree text retained, only `/.git/` excluded | caps before slurp; skip build dirs; walk without FOLLOW_LINKS |
| 7 | LOW | `:verified {:parsed true :atomic-write true :read-back true}` are literals (:1368-1376) | derive them |
| 8 | LOW | `alias` validated only as non-blank; `(symbol (str alias "/" v))` writes garbage tokens | symbol pattern |

Reachability note: caller rewiring + compile check are CLI-only (`:extract!`); the MCP extract pins
`:rewire-callers false`. The threat model is an agent running `:extract!` in a repo it did not write —
exactly the acid arms.

## q5z — GO-WITH-FIX (confinement correct; hygiene and bounds)

Every discovered path re-enters `resolve-source-path`; edits address the canonical path; the rename
destination goes through `resolve-new-source-path` with typed refusals; `retire-path` composes from a
proven-relative path; routing honours the REQUEST root (no EXTRACT-023 repeat); `verify` refuses at three
layers rather than skipping; the receipt is genuinely O(1) with tests.

| # | sev | finding | fix |
|---|---|---|---|
| 1 | MED blocker | 14 `.cpcache/*` files COMMITTED (foreign absolute paths `/home/genek-forge/.m2/…`); `.cpcache` not gitignored | `git rm --cached`, ignore |
| 2 | MED | `file-seq` follows symlinked dirs; the realpath gate runs only after `expand-scope` returns (cycle DoS before confinement) | walk without FOLLOW_LINKS or a visited-realpath set with depth bound |
| 3 | MED | no file-count or size cap; whole scope slurped and retained; `expect.files` checked post-plan | caps before slurp; per-file byte ceiling |
| 4 | LOW | retire moves the LINK (`(io/file project-root relative)` + `Files/move`) while edits hit the real file; restore restores only the link | retire the resolved real path, or refuse on a symlinked defining file |
| 5 | LOW | receipt `:ok true :committed true` and the ✓ summary lines are literals though `commit` computes `:committed` — and the tool tells agents not to re-read | propagate and derive |
| 6 | LOW | retire-failure rollback leaves the undo receipt; the verification-failure branch deletes it | delete on both |
| 7 | LOW | `:retired_to` leaks an absolute server path; per-run detail files accumulate with no retention | project-relative; retention policy |

Fix rounds launched on `~/src/clj-surgeon-rf2` and `~/src/clj-surgeon-q5z`; both re-reviewed before re-entering the queue.
