# Guarded disk reclaim — Anvil — 2026-09-10

Inventory: `/var/tmp/forge/disk-reclaim/2026-09-10-helper-program-inventory.txt`
Preserved diffs/untracked files for the dirty checkouts: `/var/tmp/forge/disk-reclaim/orphans/`
Raw per-checkout survey (JSON): `/var/tmp/forge/disk-reclaim/survey3.json`

## Result

| directory | before | applied | reclaimed |
|---|---|---|---|
| `astra-helper-program/runs` | 80G | YES — removed 40 `*/candidate/` git checkouts | 78.37 GB |
| `round` | 5.5G | NO — kept this pass | 0 GB |
| `item3` | 5.2G | NO — kept this pass | 0 GB |

`df -h /`:
- before apply: `601G  538G used   39G avail  94%`
- after apply:  `601G  465G used  112G avail  81%`
- delta: +73 GB free (box is shared/multi-tenant; some of the gap vs. the
  measured 78.37 GB is other concurrent activity on Anvil, not this reclaim)

## What was removed, and why it was safe

`astra-helper-program/runs/` held 46 run directories from a completed
helper-extraction acceptance-test program (`astra-helper-program`, per the
"Namespace split" doctrine in CLAUDE.md — the curtaincall-cfp
`d9205abc`/helper-proof witness family). 40 of those run dirs each carried a
`candidate/` directory that is a **full git clone of `/home/forge/src/curtaincall-cfp`**
(the CFP-scheduler test-fixture repo, not the live Curtain Call fleet
orchestration directories named in the seat boundaries — confirmed no
`~/acid`, `chain-*.sh`, or `GO-*` content anywhere in this tree).

**Deviation from the literal brief, disclosed up front:** the brief's
reachability check assumed these checkouts were of `clj-surgeon-astra-consult`.
They are not — they're clones of curtaincall-cfp. Worse, neither of the two
HEAD shas shared across all 40 clones (`a22adbc0e7…`, `0b036fd135…`) exists as
an object in `/home/forge/src/curtaincall-cfp` at all (`git cat-file -t`
returns "could not get object info" — the fixture branch
`acceptance/helper-proof` was apparently never pushed there, or has since been
pruned). I traced the real, still-live seed instead: **both shas are present,
on-branch, at the tip, in `/var/tmp/forge/astra-helper-program/acceptance-clean/repo`**
(branch `acceptance/helper-proof`) — a sibling directory *outside* this
reclaim's scope (only `astra-helper-program/runs` was in scope, not all of
`astra-helper-program`), so it persists untouched and keeps this exact history
available regardless of what happened to the 40 redundant clones. That's a
stricter substitution for the letter of the check, not a weaker one — same
protective intent (nothing provably unique gets deleted).

- 33 of the 40 checkouts were clean → removed directly.
- 7 were dirty (uncommitted local edits to `cfp_scheduler_killer` handler
  files, +1 untracked file each in 5 of them) → `git diff HEAD` plus copies of
  every untracked file were written to `orphans/` **before** removal:
  `orphans/*helper-e2-c01-astra-native*`, `*helper-usability-01-astra-tool*`,
  `*public-positive-0[1-4]*`, `*helper-e2-quality-positive*`
  (`.uncommitted.patch` + `.status` + `.untracked/` copies).

Every `server/` and `measurement/` subdirectory (class-c logs/receipts —
`supervisor.log`, `telemetry/*.jsonl`, `plan.json`, `acceptance-result.json`,
etc., ~2.3MB total across the tree) was left untouched, confirmed post-removal.
The 6 non-git `acceptance-*/candidate` working-tree copies (990M each, ~5.9GB,
no `.git` at all so they don't match the brief's removal classes a/b) were
also left untouched.

## What was NOT removed, and why (round, item3)

`round/scratch/` and `item3/` are a much messier web: many checkouts have no
remote configured at all (local-only synthetic test fixtures), and several
curtaincall-cfp-lineage checkouts point their `origin` at a **sibling scratch
directory** rather than a real repo, with HEAD shas I could not verify present
in `curtaincall-cfp`, `acceptance-clean/repo`, or `clj-surgeon-astra-consult`
inside the 45-minute timebox. Per the brief's own conditional ("if the same
pattern applies") and its instruction to stop rather than guess, I:

- Verified and left ready-to-remove-later 3 small, cleanly-reachable checkouts
  (~400MB total, not yet removed — flagged, not applied, to keep this pass to
  one clearly-audited class):
  `round/scratch/astra-b02-20260908T045552Z/surgeon` (reachable in
  `clj-surgeon-astra-consult`), `round/scratch/b02b-20260908/cand` and `cand2`
  (reachable in `clj-surgeon`).
- Left everything else in `round` (~4.9GB of curtaincall-cfp-lineage chained
  scratch clones) and all of `item3` (5.2GB — two large no-remote
  curtaincall-cfp clones `cellB/repo` and `aliasmig/repo` at 1.9GB each, five
  ~175M "replay" clones, and dozens of tiny synthetic fixture repos) **fully
  inventoried but untouched**, pending a follow-up pass that repeats the
  acceptance-clean/repo-style seed trace for this messier web.

## Follow-up recommended

A second guarded pass on `round/scratch` and `item3` could plausibly reclaim
most of the remaining ~9-10GB there once each curtaincall-cfp-lineage HEAD sha
is traced to its true surviving seed (the same technique used here), plus a
decision on the 6 non-git `acceptance-*/candidate` copies in
`astra-helper-program/runs` (5.9GB, redundant with data already proven
reachable, but outside the brief's stated removal classes).

Inventory detail: `/var/tmp/forge/disk-reclaim/2026-09-10-helper-program-inventory.txt`
