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

Numbers, always with the artifact each one was read from. Nothing appears here without a
source; the header of the canonical section carries the identical table.

| Meter | Result | Where it comes from | Qualifier |
|---|---|---|---|
| Median complete command wall, four prepared-change pairs | 3.05x | Astra forms cohort: `/var/tmp/forge/astra-forms-cohort-fx/summary.json` (written 2026-09-06 02:19:03Z); write-up `docs/observations/2026-09-06-astra-forms-cohort-result.md`, measurement stamped 2026-09-06T02:20:24Z, cohort engine `0a49f012` | Tool 3/4 verified against native Sol 4/4 verified. Latency only, bought at LOWER reliability. It is NOT an Astra-caller-versus-Sol-caller comparison. n=4. |
| Codex `apply_patch` V4A payloads refused by a unified-diff-only gate | 69-75% of admit calls | `docs/observations/2026-09-02-resume-here-bridge-program.md`, UPDATE 15:41Z | Why a structured spec shape is used instead of a diff string. A different gate, not this executor. |
| The 2026-09-02 native-default ruling | ~2x wall and ~2x actions for a tool-mandated agent | `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`; 81 arm-runs, verified servers, two blind judges | Measured on the Sol caller, on the build before `13c12401`, and on the MCP per-form editing grammar. |
| Executor-first in the pilot | not a measurement | this document; the experiment row of the canonical table | MANDATED by the experiment, not chosen by any agent. No adoption signal, no speedup claim. |

**The 11x bench row has been REMOVED.** It was a single unreplicated harness reading with
no retained receipt naming its scope. It is not carried on any surface.

The raw pilot is not a replicated speedup. The EXPERIMENTAL ENGINE is named separately from
production routing: the `bin/mission` `owner_forms` executor, PROTOTYPE dated 2026-09-05,
cohort engine `0a49f012`; its kernel source commit is not yet a git commit on any published
branch. Production routing does not depend on it.

## 2. Canonical text

ONE canonical section: `skills/clj-surgeon/SKILL.md`, heading
**"Edit routing (policy revision 1, 2026-09-06)"**. That working-tree file is the SOURCE;
installed skill mirrors follow it through `make install-claude-skill` and
`make install-codex-skill`.

**The copies are made BY HAND. Nothing generates them.** No templating step, no include, no
substitution: each surface below holds its own literal bytes of the table, typed in by whoever
last edited it. The guard against drift is therefore a CHECK, not a generator ---
`bb bin/check-routing-parity.clj` asserts that each of the FIVE hand-copied table renderings
(`skill.md`, `skills/safe-refactor/SKILL.md`, `CLAUDE.md`, `AGENTS.md`, and this document) is
byte-identical to the canonical section's table, that the plate's pointer heading exists in
the canonical file, and that every document the plate cites exists on disk. It exits 2 and
names the first differing row. `make check-agent-routing` AND `make install-agent-routing`
both run it before touching the managed blocks, so the actual write entrance cannot bypass
the guard.

**The plate is not one of those five.** `resources/clj-surgeon-agent-routing.md` is a REVIEWED
SUMMARY that points at the canonical section — compact prose bullets, deliberately not a
byte-parity rendering of the table. No byte comparison is made against it; the check reaches
only its pointer heading and its citations. That its summary still means what the canonical
section says is established by human review, and nothing on this branch gates it.

The table:

| Situation | Route |
|---|---|
| Owner and line already known | Direct bounded read: `:op :cat :file F :form NAME`, or `sed -n 'A,Bp'` on the known range. No outline. |
| Owner unknown in a large file | One outline or one search (`:op :ls`, or `rg`), then read the named form. |
| Source already held in context | No reread. |
| Known small literal change in one region | Native `rg` plus `apply_patch`. This stays a legitimate production default. |
| Bounded mechanical edit (rename across call sites, move helpers, thread a parameter) | Choose native or a deterministic Surgeon route by COMPLETE VERIFIED TASK COST. There is no executor-first rule in production. |
| Extraction to a new namespace; namespace rename; a require added or changed across namespaces; a surgical edit inside one known form | The earned deterministic Surgeon routes: `:extract!`, `:rename-ns!`, `require_change`, `within` plus `from`/`to`. Kept from the 2026-09-02 ruling: no native equivalent, or measured zero churn. |
| Complete reference discovery required | Surgeon semantic preparation. `rg` is not a closure proof. |
| New code, new tests, prose, non-Clojure | Native. Ineligible for the experimental executor on this build; not forbidden territory. |
| Under the mandated dogfood EXPERIMENT only, explicitly opted into, an eligible bounded mechanical edit | Try the `bin/mission` executor FIRST, then write one ledger line. Executor-first is the experiment's rule; it does not govern production routing. |
| Fan-out via per-form MCP writes; `apply_clojure_changes` with a namespace owner; forms-scoped `find`+`replace` for insertion | Do not use. Measured losers 2026-09-02, not re-measured since. |

Reads: owner and line known — direct bounded read, no outline. Owner unknown in a large file
— one outline or one search, then the form. Source already held — no reread.

## 3. Surface inventory

| # | Surface | Where the bytes live | Installer | Checker | Tripwire |
|---|---|---|---|---|---|
| 1 | Managed block `CLJ-SURGEON ROUTING v:2` in `~/.claude/CLAUDE.md` | `resources/clj-surgeon-agent-routing.md` | `make install-agent-routing` | `make check-agent-routing` | `~/bin/check-prompt-plate.sh` (bridge, hourly cron) |
| 2 | Same block in `~/.codex/AGENTS.md` | same file | `make install-agent-routing` | `make check-agent-routing` | same |
| 3 | Claude skill `clj-surgeon` | `skills/clj-surgeon/SKILL.md` | `make install-claude-skill` (via `prepare-skill-package`) | `make check-clj-surgeon-skill-mirrors`; `bb bin/check-routing-parity.clj` (canonical source) | none — **gap, see §6** |
| 4 | Codex skill `clj-surgeon` | same file | `make install-codex-skill` | same | none — **gap** |
| 5 | Skill `safe-refactor` | `skills/safe-refactor/SKILL.md` | **none on this branch — outstanding: no Makefile target installs `skills/safe-refactor`** | `bb bin/check-routing-parity.clj` (table only) | none |
| 6 | Repository `CLAUDE.md` | repo root | git checkout (read in-repo) | `bb bin/check-routing-parity.clj` (table only) | none |
| 7 | Repository `AGENTS.md` | repo root | git checkout (read in-repo) | `bb bin/check-routing-parity.clj` (table only) | none |
| 8 | Seat header `~/.claude/CLAUDE.md` prose (outside the managed block) | forge@anvil seat file | hand-edited by the seat owner | none | none |
| 9 | Shared doctrine `~/opt/claude-skills/_doctrine/house-rules.md` | claude-skills repo | `git pull` on each seat | none | none |
| 10 | Delegation briefs | each launching agent's prompt text | the launching agent | none | none |
| 11 | Legacy root `skill.md` | repo root | not installed (`SKILL_SOURCE` is `skills/clj-surgeon`) | `bb bin/check-routing-parity.clj` (table only) | none |

`SKILL_SOURCE` in the Makefile is `skills/clj-surgeon` only. Rows 3 and 4 are the ONLY
skill directory with an installer. Rows 5 and 8-10 are hand-carried surfaces; they are where
drift will appear first.

## 4. Install and verify procedure

Run in this order, from a clean checkout of the merged branch.

1. **Regenerate / confirm the plate.**
   `sha256sum resources/clj-surgeon-agent-routing.md`
   The plate was rewritten for policy revision 1; do NOT carry a hash from an earlier draft
   here. `make install-agent-routing` prints `:block-hash` computed over the installed bytes;
   quote THAT value in the captain's log, because it is the installer's own receipt.
2. **Prove the parity check is green and the checker fails on the CURRENT installed v:1
   block, before installing.** `make check-agent-routing` runs
   `bb bin/check-routing-parity.clj` first; a hand-copied table that drifted fails here.
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
> literal change.** For any other bounded mechanical edit, production chooses native or one of
> the earned deterministic Surgeon routes (`:extract!`, `:rename-ns!`, `require_change`,
> `within` plus `from`/`to`) by complete verified task cost. **There is no executor-first rule
> in production**; trying the `bin/mission` executor first is the rule of the explicitly
> opted-in dogfood experiment alone. A typed refusal is read, not defeated: retry only when new
> evidence or a concrete supported correction lifts the stated reason; otherwise finish
> natively and record the provenance. The tie-break is complete verified task cost, not the
> existence of a receipt.
>
> **Reassess at each Gene report. Nothing expires silently.** Each reassessment records the
> measured build, the route taken per edit class, any counterexample, and the explicit
> decision delta — including "no change".

Delegation-brief paragraph, for every builder prompt:

> Clojure edit routing: the `CLJ-SURGEON ROUTING` block in your instructions carries the
> routing summary and is sufficient for an ordinary edit — do NOT load a skill per edit. Read
> the `clj-surgeon` skill's "Edit routing (policy revision 1, 2026-09-06)" section only when
> the route is not already decided, or for an advanced workflow. Owner and line known — read
> the form directly, no outline. Owner unknown in a large file — one outline or one search,
> then the form. Known small literal change — native `rg` plus `apply_patch`. Any other
> bounded mechanical edit — choose native or a deterministic Surgeon route (`:extract!`,
> `:rename-ns!`, `require_change`, `within` plus `from`/`to`) by complete verified task cost;
> there is no executor-first rule in production. On a typed refusal, read the code wherever it
> sits — `:error_type` strings beginning `mission-`, `:error-type` keywords such as
> `:forms-protected-syntax`, or a nested diagnostic under `:candidates`/`:proof`/`:decision` —
> retry only on new evidence, otherwise finish natively and say so. New code, tests, and prose
> are native — ineligible for the experimental executor on this build, not forbidden. Never use
> per-form MCP writes for fan-out, `apply_clojure_changes` with a namespace owner, or
> forms-scoped `find`+`replace` for insertion. Do not start an MCP server for an ordinary edit.
> **Only if this brief explicitly opts you into the dogfood experiment:** try `bin/mission`
> first on an eligible edit (`run --workspace WS --spec-file spec.edn`, or `propose` then
> `apply`; schema in `docs/mission-typist.md`), do not invent a profile to force eligibility,
> and record one ledger line per edit:
> `dogfood | <edit class> | <route> | <refusal type or -> | <wall seconds>`.

## 6. Gaps this draft does not close

1. **No installer for `skills/safe-refactor`.** `SKILL_SOURCE` is `skills/clj-surgeon`
   alone. The safe-refactor text ships only to agents reading the working tree.
2. ~~No checker binds the skill text to the plate.~~ **PARTLY CLOSED.**
   `bb bin/check-routing-parity.clj` fails if the canonical heading the plate names is
   missing, if any of the five hand-copied table renderings differs by a byte, or if a
   document the plate cites does not exist. `make check-agent-routing` and
   `make install-agent-routing` both depend on it, so the write entrance is gated too. It
   does NOT check rows 8-10 below, or the prose around each table --- only the table bytes,
   the pointer, and the citations. **The plate's own wording remains unchecked:** it is a
   reviewed summary, not a byte-parity rendering, so no gate can tell whether its bullets
   still agree with the canonical section. Only review does.
3. **Rows 6-10 have no checker at all.** Repo `CLAUDE.md`/`AGENTS.md`, the seat header,
   shared house rules, and delegation briefs are verified by reading, not by a gate.
4. **The plate does not carry the doctrine commit it was derived from.** The generator is an
   installer over a static file; it substitutes nothing. It names the source DOCUMENT
   (`docs/observations/2026-09-06-clojure-edit-routing-rule.md`) instead. Templating a commit
   into the block would need a generator change and a test.
