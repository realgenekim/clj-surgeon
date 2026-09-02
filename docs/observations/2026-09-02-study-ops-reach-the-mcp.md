# Study operations reach the MCP read entrance (clj-surgeon-0me)

**Written:** 2026-09-02T23:18:20Z · seat: forge-bridge (Buster) · branch `bridge/study-ops-mcp`

Gene, relayed by the mayor: *"Nudge bridge: really think this needs to be done
sooner rather than later"* — on the finding that `:ls-tree`, `:ls-deps`,
`:deps`, `:topo`, and `:ls-extract` lived only in `core.clj` and appeared
nowhere in the MCP surface. `docs/vision.md` names "questions grep answers
wrong" as one of the four squares we still compete on and `:ls-tree` as its
foundation; the measured caller of Surgeon is the MCP, not the CLI, and "the
CLI as an MCP substitute" is already a closed, measured loser.

## The entrance decision

`inspect_clojure`, not a new tool. `docs/plans/one-compiler-two-entrances.md`
is binding: Public Contract "Keep exactly `inspect_clojure` and
`apply_clojure_changes`", Safety Invariant 10 "The number of public Surgeon MCP
tools remains two", Definition of Done "no third Surgeon MCP tool". A
`study_clojure` tool would add a fifth public tool for read-only work the read
tool already owns.

Inside `inspect_clojure` the split fell out of the existing contract, not out
of taste. Every `requests` item is keyed by one project-relative `file`, is
counted by `expect.files`, and must be covered by `snapshot_guards`. Four
study operations are file-scoped and drop straight in with no rule changed.
`ls-tree` is *directory*-scoped: making it a request item would require
`expect.files` to accept zero and the snapshot-guard completeness rule to
become conditional — a contract change to an existing verb, paid by every
existing caller. It therefore took the shape the contract already reserves for
a whole-project read: a top-level `mode`, exactly like `plan-extraction`.

## One kernel

New `clj-surgeon.study`: five pure functions, data in, data out. The CLI is
kernel plus print; the MCP is kernel plus receipt. The `:ls-tree` discovery and
formatting pipeline moved verbatim out of `core.clj`. Nine of ten frozen CLI
invocations came back byte-identical.

## The tenth invocation, and what it found

`:ls-tree` on a directory with no Clojure sources printed
`{:error nil, :error-type :invalid-arguments}` and exited 1 — never the
documented message. Cause: `run-ls-tree` destructured `format` as a local,
shadowing `clojure.core/format`, so the "No Clojure files found under …"
branch called `nil` and threw a NullPointerException that `-main` caught and
reported with a nil message. Extracting the kernel removed the shadow. The
refusal message is now reachable, and
`ls-tree-refusal-message-is-reachable` is the regression test that keeps it so.
This is the one place where the CLI bytes deliberately changed.

## Two things I had to widen, both reported rather than assumed

1. **`babashka/fs` and `babashka/process` moved from a test alias into base
   `:deps`.** The shipped MCP server could not load the kernel without them,
   and `ls-tree` genuinely needs directory walking and ripgrep. Both are small
   and transitively clean.
2. **`find-build-files` no longer builds a `sh -c` string.** `ls-tree` is the
   first read path that runs subprocesses on behalf of an MCP caller, and the
   old code interpolated the scanned directory into a shell command. It is an
   argv vector now, so no caller-influenced path reaches a shell. The three
   ls-tree goldens stayed byte-identical across that change.

I also added `resolve-directory-path` to `mcp_paths.clj` — purely additive,
mirroring `resolve-source-path` line for line. No existing function changed and
no check was relaxed.

## Receipts

Bounded to 4096 payload characters by default, `limit` up to 16384. Over
budget: whole rows only, `truncated=true`, `returned`/`omitted`,
`read_complete=false`, `next_action="raise_limit_or_narrow_scope"`, and an
executable `next_call`. `ls-tree` truncates at whole-file granularity and
renders the kept files through the same formatter the CLI uses, so a truncated
tree is still a valid tree. The bound is not decoration: `:ls-tree :dir .` over
this repository is **221,018 characters** and **161 files**; the default
receipt returns 3 of them in 3,623 characters and hands back the call that
widens it.

`read_complete` is false whenever any study row was bounded away. A truncated
receipt that claims terminal evidence is a false green, and a false green
terminates investigation.

Hand-driving the wire found the defect the unit tests did not: replaying the
truncated `next_call` at the maximum limit returned that same call again — a
continuation with no way to advance. The rule now is the plan's own: an
executable `next_call` only while raising `limit` can still help; at the
ceiling the receipt says `narrow_scope` with a remedy and serves no call,
because the narrower `dir`, `grep`, or `form` is a caller judgment, not a
deterministic projection of proved facts.

## Evidence

- Real wire, own server on 127.0.0.1:7931, exact caller JSON over streamable
  HTTP: success and refusal for every operation. `ls-tree` over the whole
  repository, `elapsed_ms` **2351.84**.
- `study-ops-both-entrances-call-one-kernel`: for each operation, on the same
  bytes, MCP receipt payload = `json-data` of the kernel result = the CLI
  handler's return.
- `docs/intent/study-ops/` carries MCP-OP-STUDY-001..010 with falsifiers.

## Owed

`:mv`, `:rename-ns!`, and `:fix-declares!` stay CLI- and gate-only, asserted by
`the-read-entrance-exposes-no-write-operation`. The E6 free-choice adoption
cohort for `:ls-tree` through MCP is still owed: the feature is built, and
under this repository's own test doctrine a feature whose optional-arm adoption
is unmeasured has not shipped a claim. `tech-tree.md` records it as
"BUILT, adoption unmeasured", not WON.
