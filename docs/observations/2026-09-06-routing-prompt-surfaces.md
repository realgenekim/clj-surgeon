# Routing prompt surfaces — inventory, canonical text, install and verify procedure

Written 2026-09-06 03:26Z on branch `fable/routing-prompts` (cut from `astra/typist-route` e6331bfe).
DRAFT for joint review. **Nothing in this document has been installed.**

Governing rule: "doctrine cannot silently disagree with the prompt" (house rules,
2026-09-02). A doctrine change that affects what agents do at boot is not done until every
managed block and prompt file it must change is named, regenerated, installed on every
account that carries it, verified by a checker bound to the CURRENT intent, and watched by a
tripwire afterwards.

## 1. Why there is a v:2 at all

The installed `CLJ-SURGEON ROUTING v:1` block says: "Native `rg` plus a native patch is
the default route for reading and editing Clojure. Do not reach for clj-surgeon for ordinary
edits." That ruling was measured 2026-09-02 on the Sol caller, the pre-`13c12401` build, and
the MCP per-form editing grammar. The mission executor is a different mechanism and a
different caller. A rule measured on the first does not govern the second — and it does not
license the second beyond what has been measured either.

Numbers, always with their qualifiers:

| Meter | Result | Qualifier |
|---|---|---|
| Executor adoption in the pilot | executor-first | MANDATED, not chosen. Not an adoption signal. |
| Terminal latency, Astra caller, complete CLI | 3.05x | Came with a reliability LOSS: 3/4 versus 4/4. |
| Bench harness wall | 11x | Single harness, not replicated. |
| Unified-diff route acceptance | 0 of 20 | Why the executor spec shape is used at all. |

The raw pilot is not a replicated speedup. The mission executor is a PROTOTYPE dated
2026-09-05; its kernel source commit is not yet a git commit on any published branch.

## 2. Canonical text

ONE canonical section: `skills/clj-surgeon/SKILL.md`, heading
**"Edit routing (measured 2026-09-06, build >= 13c12401)"**. Every other surface renders the
identical decision table, verbatim, not a paraphrase:

| Situation | Route |
|---|---|
| Owner and line already known | Direct bounded read: `:op :cat :file F :form NAME`, or `sed -n 'A,Bp'` on the known range. No outline. |
| Owner unknown in a large file | One outline or one search (`:op :ls`, or `rg`), then read the named form. |
| Source already held in context | No reread. |
| Known small literal change in one region | Native `rg` plus `apply_patch`. This stays a legitimate production default. |
| Bounded mechanical edit (rename across call sites, move helpers, thread a parameter, add a require across namespaces) **and** scope, proof profile, provider permission, and measured admission facts already fit | Try the `bin/mission` executor first. Do not invent a profile or a prior to force eligibility. |
| Complete reference discovery required | Surgeon semantic preparation. `rg` is not a closure proof. |
| New code, new tests, prose, non-Clojure | Native. Ineligible for this executor on this build; not forbidden territory. |
| Tonight's mandated dogfood experiment, eligible edit | Executor first, then one ledger line. |
| Fan-out via per-form MCP writes; `apply_clojure_changes` with a namespace owner; forms-scoped `find`+`replace` for insertion | Do not use. Measured losers 2026-09-02, not re-measured since. |

Reads: owner and line known — direct bounded read, no outline. Owner unknown in a large file
— one outline or one search, then the form. Source already held — no reread.

## 3. Surface inventory

| # | Surface | Where the bytes live | Installer | Checker | Tripwire |
|---|---|---|---|---|---|
| 1 | Managed block `CLJ-SURGEON ROUTING v:2` in `~/.claude/CLAUDE.md` | `resources/clj-surgeon-agent-routing.md` | `make install-agent-routing` | `make check-agent-routing` | `~/bin/check-prompt-plate.sh` (bridge, hourly cron) |
| 2 | Same block in `~/.codex/AGENTS.md` | same file | `make install-agent-routing` | `make check-agent-routing` | same |
| 3 | Claude skill `clj-surgeon` | `skills/clj-surgeon/SKILL.md` | `make install-claude-skill` (via `prepare-skill-package`) | `make check-clj-surgeon-skill-mirrors` | none — **gap, see §6** |
| 4 | Codex skill `clj-surgeon` | same file | `make install-codex-skill` | same | none — **gap** |
| 5 | Skill `safe-refactor` | `skills/safe-refactor/SKILL.md` | **none on this branch — outstanding: no Makefile target installs `skills/safe-refactor`** | none | none |
| 6 | Repository `CLAUDE.md` | repo root | git checkout (read in-repo) | none | none |
| 7 | Repository `AGENTS.md` | repo root | git checkout (read in-repo) | none | none |
| 8 | Seat header `~/.claude/CLAUDE.md` prose (outside the managed block) | forge@anvil seat file | hand-edited by the seat owner | none | none |
| 9 | Shared doctrine `~/opt/claude-skills/_doctrine/house-rules.md` | claude-skills repo | `git pull` on each seat | none | none |
| 10 | Delegation briefs | each launching agent's prompt text | the launching agent | none | none |

`SKILL_SOURCE` in the Makefile is `skills/clj-surgeon` only. Rows 3 and 4 are the ONLY
skill directory with an installer. Rows 5 and 8-10 are hand-carried surfaces; they are where
drift will appear first.

## 4. Install and verify procedure

Run in this order, from a clean checkout of the merged branch.

1. **Regenerate / confirm the plate.**
   `sha256sum resources/clj-surgeon-agent-routing.md`
   Current block hash on this branch: `589799bdba1bd2832d65bd07edab0cd65558711aadb2fdbb26462d1d2ba34a11`
   (`make install-agent-routing` prints `:block-hash` computed over the same bytes; quote
   THAT value in the captain's log, not this one, because it is the installer's own receipt.)
2. **Prove the checker fails on the CURRENT installed v:1 block, before installing.**
   `make check-agent-routing` — expected: `:error-type :agent-routing-stale-version`,
   `:expected-version 2`, one target per account with `:previous-state :stale`,
   `:stale-version 1`. Quote it.
3. **Install on every account that carries the block.** Named accounts, with evidence status:
   - `forge@anvil`: `~/.claude/CLAUDE.md`, `~/.codex/AGENTS.md` — **outstanding: not
     installed by this task (drafting seat is forbidden to write installed surfaces).**
   - `mayor@anvil`: **outstanding: installer not run; no evidence accessible from this seat.**
   - `forge@buster` / bridge seats: **outstanding: no evidence accessible from this seat.**
   - skiff seats: **outstanding: no evidence accessible from this seat.**
   No blanket "installed everywhere" claim is made. Each row above must be replaced with a
   quoted `make install-agent-routing` receipt before the change is called landed.
4. **Re-run `make check-agent-routing` on every account.** Green means the installed bytes
   equal the plate byte-for-byte.
5. **Install the skills:** `make install-claude-skill` and `make install-codex-skill`.
   Quote the printed `source-commit` and `source-hash`.
   Then `make check-clj-surgeon-skill-mirrors`.
6. **Arm the tripwire.** `~/bin/check-prompt-plate.sh` fetches main, compares the installed
   block against the current plate, logs, and files a maven inbox item on drift. Confirm it is
   in cron on each box.
7. **Quote the block hash in the captain's log entry** for this change, per the doctrine rule.

## 5. Text for Gene and the mayor to paste

House-rules doctrine sentences (replace the "Surgeon is not the default route for Clojure
edits" paragraph):

> **Routing rules name the build and the caller they were measured on.** When either changes,
> the rule is re-measured before it is applied, and the measurement comes from the tool's own
> ledger, not from a prompt experiment. The canonical Clojure edit-routing text is the
> `clj-surgeon` skill's "Edit routing" section; the managed `CLJ-SURGEON ROUTING` block is a
> pointer at it, and every other prompt surface renders its decision table verbatim.
>
> **Native `rg` plus `apply_patch` remains a legitimate production default for a known small
> literal change.** The mission executor is tried first only when scope, proof profile,
> provider permission, and measured admission facts already fit. A typed refusal is read, not
> defeated: retry only when new evidence or a concrete supported correction lifts the stated
> reason; otherwise finish natively and record the provenance. The tie-break is complete
> verified task cost, not the existence of a receipt.
>
> **Reassess at each Gene report. Nothing expires silently.** Each reassessment records the
> measured build, the route taken per edit class, any counterexample, and the explicit
> decision delta — including "no change".

Delegation-brief paragraph, for every builder prompt:

> Clojure edit routing: read the `clj-surgeon` skill's "Edit routing" section before your
> first Clojure edit. Owner and line known — read the form directly, no outline. Owner unknown
> in a large file — one outline or one search, then the form. Known small literal change —
> native `rg` plus `apply_patch`. Bounded mechanical edit (rename across call sites, move
> helpers, thread a parameter, add a require across namespaces) where scope, proof profile and
> provider permission already fit — try `bin/mission` first (`run --spec-file -`, or
> `propose` then `apply`); do not invent a profile to force eligibility. On a typed refusal,
> read `:error_type`, retry only on new evidence, otherwise finish natively and say so. New
> code, tests, and prose are native — ineligible for this executor on this build, not
> forbidden. Never use per-form MCP writes for fan-out, `apply_clojure_changes` with a
> namespace owner, or forms-scoped `find`+`replace` for insertion. Do not start an MCP server
> for an ordinary edit. If this is a dogfood run, record one ledger line per edit:
> `dogfood | <edit class> | <route> | <refusal type or -> | <wall seconds>`.

## 6. Gaps this draft does not close

1. **No installer for `skills/safe-refactor`.** `SKILL_SOURCE` is `skills/clj-surgeon`
   alone. The safe-refactor text ships only to agents reading the working tree.
2. **No checker binds the skill text to the plate.** `check-agent-routing` compares the
   managed block only. If the skill's "Edit routing" heading is renamed, the plate's pointer
   dangles and nothing goes red. A one-line addition to
   `check-clj-surgeon-skill-mirrors` (grep the heading) would close it.
3. **Rows 6-10 have no checker at all.** Repo `CLAUDE.md`/`AGENTS.md`, the seat header,
   shared house rules, and delegation briefs are verified by reading, not by a gate.
4. **The plate does not carry the doctrine commit it was derived from.** The generator is an
   installer over a static file; it substitutes nothing. It names the source DOCUMENT
   (`docs/observations/2026-09-06-clojure-edit-routing-rule.md`) instead. Templating a commit
   into the block would need a generator change and a test.
