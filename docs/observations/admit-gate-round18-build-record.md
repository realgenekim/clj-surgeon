# clj-surgeon admit gate — ROUND EIGHTEEN build record (CHECKPOINT, 2026-09-04T22:56Z)

Branch `bridge/admit-gate-r16`, built from `2ac33278` (round seventeen's tip).
Reviewer's verdict this round answers: `docs/observations/admit-gate-round17-review-opus.md` — **NO-GO**,
three blocking findings, four non-blocking.

**Status at this checkpoint: all three blockers RED -> GREEN and pushed; the four
non-blocking findings closed; the full gate set NOT yet re-run.** A fleet-wide
SUSPEND-ALL (Astra, backed by Gene) came in while `make mcp-test` was draining on
a fresh clone, so this record is written at the suspension point rather than at
the end of the round. Nothing below is claimed that was not run.

## What changed, per blocker

| # | intent | RED sha | GREEN sha | mechanism |
|---|---|---|---|---|
| 1 | MCP-OP-ADMIT-158 | `5c603a3a` | `60d1aac6` | the overlay walks with NOFOLLOW_LINKS, derives every destination from the WORKSPACE-RELATIVE path, refuses any entry resolving outside the root under a new typed kind `:inline-verify-overlay-escape` (in EVERY mode, `propose` included), and recreates in-workspace links as links with their targets rewritten to stay inside the overlay |
| 2 | MCP-OP-ADMIT-159 | `75d39d4b` | `c58ccbc3` | `Files/copy` carries COPY_ATTRIBUTES, so modes and mtimes survive; a command that cannot be LAUNCHED gets status `did-not-start`, its own reason, its own blocking kind, and the launcher's text attributed to the launcher rather than published as the command's last lines |
| 3 | MCP-OP-ADMIT-160 | `52873795` | `280493d5` | `file-evidence` takes an anchor and seeks; the two admit sites that publish for a READER (inline commands, repository-declared runner) read the capture's TAIL at a 262,144-byte window, and every inline row carries `output_bytes`, `output_omitted_bytes` and `output_truncated` |
| 4 | MCP-OP-ADMIT-161/162 | `d71b9d42` | `c420f45a` | a `next_call` is never the refused call; `unsupported-patch-target` names the native files AND the admitted ones; the missing-profile affordance is a sendable `[["make","test"]]`; `verify_ok` is null when `verify` is "none"; the overlay skips node_modules/target/.cpcache/.venv/dist/build (configurable); the description says the fence is a working directory and NOT a filesystem sandbox, and that a setsid child survives the deadline |

The RED for blocker 1, verbatim, in all three modes:

```
mode preview: the overlay wrote outside its own root: victim bytes 30 -> 0
mode propose: the overlay wrote outside its own root: victim bytes 30 -> 0
mode commit:  the overlay wrote outside its own root: victim bytes 30 -> 0
every file beneath the linked directory is untouched: secret-0.txt is 0 bytes
Ran 1 tests containing 29 assertions.  25 failures, 0 errors.
```

## Sabotage — every blocker's witness goes red when its fix is reverted

Each on its own copy under `/var/tmp/forge/gate18-fx`, never in a working tree.

| # | sabotage | result |
|---|---|---|
| S1 | `git archive 5c603a3a` (the MCP-OP-ADMIT-158 fix absent) | **25 failures / 29**, `victim bytes 30 -> 0` in preview, propose AND commit |
| S2 | `Files/copy` reverted to `io/copy`; the `did-not-start` branch and reason removed | **10 failures / 98**, including `Cannot run program "./bin/check" … error: 13 (Permission denied)`, copy perms without OWNER_EXECUTE, `source 1000000000000 copy 1788562145809`, and `status "did-not-finish"` on a launch failure |
| S3 | the tail anchor and the truncation fields reverted | **6 failures + 1 error / 98**, `output_tail` first line `noise line 218 …` last line `noise line 257 padding padding `, no truncation flag, and the TEXT face carrying the same head |

## The figure correction round seventeen asked for

The claim **197 tests / 4570 assertions** for the two admit namespaces was carried in the
body of commit `2853370c` and does not reproduce. The reviewer measured **175 / 4425 / 0**
on a fresh clone at `2ac33278`, in both precondition states. **175/4425/0 is the correct
figure for that tip**; the commit message cannot be corrected without rewriting history,
so the correction lives here. On the current tip the same two namespaces plus the new
round-eighteen leaf measure **177 / 4469 / 0** (1 precondition skipped, the battery receipt).

## Gates — what has run at this tip, and what has not

RUN, on the builder's worktree at the tip, verbatim:

```
Ran 235 tests containing 5072 assertions.
0 failures, 0 errors.  1 precondition skipped.
(clj-surgeon.admit-patch-test, admit-patch-round16-test, admit-patch-round18-test,
 mcp-intent-contract-test, mcp-contract-test, core-discovery-test)
```

That set contains the linked-intent audit (`mcp-intent-contract-test`, green, so
MCP-OP-ADMIT-158..162 each carry an implementation and a test witness), the refusal-kind
enumeration (the new kind is DRIVEN through the entrance, not merely enumerated), the
parser node ceiling and the structural shell-argv scan.

NOT YET RUN at this tip — the remaining list:

1. `make mcp-test` on a fresh clone (`/var/tmp/forge/gate18-fx/clone`, at `c420f45a`,
   clean). This branch uses **the trunk's monolithic lane** — `mcp-test: mcp-operation-oracle`
   then one `clojure -M:clj-surgeon/mcp-test` — the lane manifest has NOT landed here.
   The run was draining when SUSPEND-ALL arrived and was stopped; no JVM survived it.
2. `~/bin/suite-run bb test/run_all.clj` (the babashka lane)
3. `make mcp-operation-oracle` standalone
4. `make repository-hygiene`
5. `make admit-transaction-recovery-battery`
6. the two admit namespaces alone, on the fresh clone
7. `git merge-tree --write-tree HEAD origin/MCP/main` against the trunk sha
8. fixture removal under `/var/tmp/forge/gate18-fx` with an `ls` proof

Fixtures are DELIBERATELY left in place at the suspension point so the remaining gates
can resume without rebuilding them: `/var/tmp/forge/gate18-fx/{clone,sab1,sab2,sab3,jtmp,cp.txt}`.
They are removed when the round finishes.

## Constraints honoured

No server started on any port. No contact with 7888/7890/7894/7895/8171/8173. No sudo.
No `git stash`, no `git add -A` — every commit adds by name. Nothing merged; nothing
pushed to `main`. Every fixture under `/var/tmp/forge/gate18-fx`, never `/tmp`. Suites
were routed through `~/bin/suite-run`, one at a time, from the capacity order onward.
