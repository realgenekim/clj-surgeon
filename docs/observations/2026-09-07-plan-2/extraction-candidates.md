# Cross-namespace extraction candidates — native-vs-tool timed experiment
Discovery date: 2026-09-07. Read-only. No edits, commits, or checkouts made.

Repos surveyed (as checked out, HEAD unchanged):
- `/home/forge/src/curtaincall-cfp` @ `00e8f0fa` branch `main`
- `/home/forge/src/clj-surgeon-records` @ `27176297` branch `records/MCP-main`
  (this is the branch present in the working copy; the request said `origin/MCP/main`)

Test entrances:
- cfp: `make runtests-focus FOCUS=<test-ns>` → `bin/kaocha unit --focus <ns> --fail-fast`;
  full unit lane `bin/kaocha unit --fail-fast`
- surgeon: `make test-fast` (JVM FAST lane), `make test-battery`, `make test` (landing gate)

---

## Repo 1 — curtaincall-cfp

| # | source ns | forms to move | intra-cluster deps | caller namespaces | call sites | tests covering | native difficulty | suggested destination |
|---|---|---|---|---|---|---|---|---|
| **1 (BEST)** | `cfp-scheduler-killer.exports` (`src/cfp_scheduler_killer/exports.clj`, 1879 lines; calendar block lines 704–1053) | 23: `ics-escape` `fold-line` `ics-date` `ics-stamp` `ics-uid` `ics-sequence` `ics-local-datetime` `absolute-link` `vevent` `snapshot-published?` `last-publication-snapshot` `ics-sequence-at` `cancellation-vevent` `cancellation-for` `hold-the-date-uid` `hold-the-date-sequence` `hold-the-date-vevent` `cfp-deadline-uid` `cfp-deadline-sequence` `cfp-deadline-ics` `submission-ics` `calendar-ics-for` `calendar-ics` (+ carry `calendar-prodid`, `ical-domain`) | **42 edges** (deepest: `vevent`→7, `hold-the-date-vevent`→7, `cfp-deadline-ics`→7, `calendar-ics`→5) | **9** (6 src + 3 test): `agent.commands`, `handlers.exports`, `handlers.public-cfp`, `handlers.public-widgets`, `inform`, `session-invites`, `exports-test`, `comms-test`, `schedule-test` | **43** | `exports-test`: `ics-test`, `ics-escaping-test`, `issued-invite-uid-survives-title-amendment-test`, `unroomed-session-calendar-uses-event-location`, `cfp-deadline-ics-amends-and-never-duplicates-test`, `published-gate-test`, `explicit-content-readiness-gates-machine-publication-not-organizer-work`, `llms-txt-test`; `schedule-test`: `exports-reflect-placement-test`, `published-session-removal-emits-a-snapshot-cancellation-test`, `conflict-withheld-from-public-test`; `comms-test`: `ics-attachment-matches-the-feed-test` | **HIGH.** 9 `:require` additions + 1 new ns form. **4 private vars must become public or be duplicated**: `speaker-names`, `communicated?`, `content-publishable?`, `calendar-prodid` (`defn-`/`^:private`). 6 more same-file public vars become cross-ns calls: `public-answers`, `published?`, `publishable-sessions`, `room-name`, `date-range-string`, `ical-domain`. New ns needs `store`, `events`, `clojure.string` + `java.time` imports (`LocalDate` `ZoneId` `Instant` `DateTimeFormatter`). **Back-reference**: `exports.clj:1241` (a `(comment …)` REPL block) calls `calendar-ics` → either delete, or `exports` must require the new ns (no cycle: new ns requires `store`/`events`, not `exports`, if the 10 shared vars move or are re-homed). | `cfp-scheduler-killer.exports.calendar` (file `src/cfp_scheduler_killer/exports/calendar.clj`) |
| 2 | `cfp-scheduler-killer.schedule` (`src/cfp_scheduler_killer/schedule.clj`, 1129 lines; lines 38–121) | 11: `slot-granularity-minutes` `parse-time` `minutes->hhmm` `minutes->display` `time-range-display` `parse-day` `event-days` `day-label` `in-bounds?` `default-durations` `duration-for` | 5 edges (`time-range-display`→`minutes->display`; `day-label`→`event-days`,`parse-day`; `in-bounds?`→`event-days`; `duration-for`→`default-durations`) | **11** (9 src + 2 test): `views.schedule`, `handlers.schedule`, `views.public-widgets`, `views.portal`, `schedule-suggestions`, `inform`, `seed-demo`, `embed-widget`, `cli.judge-sandbox`, `schedule-test`, `public-widgets-test` | **73** | `schedule-test` (18 sites), `public-widgets-test` (2), `schedule-ghost-test` (`in-bounds?`) | **MEDIUM.** Widest caller fan-out in the repo (11 requires, 73 rewrites) but **zero** same-file external deps and **zero** private→public promotions — pure `java.time` + `str` helpers. `schedule` itself keeps ~40 callers of the family, so `schedule.clj` also gains a require. Good "many callers, easy semantics" control arm. | `cfp-scheduler-killer.schedule.time` |
| 3 | `cfp-scheduler-killer.schedule` (rough/ghost lanes; lines 519–570 + 852–916) | 12: `rough-labels` `rough-label-keys` `block-labels` `rough-labels-for` `label-visibilities` `rename-label!` `rough-parking-day` `rough-lane-meter` `parked-ghosts` `placeholder-label-parts` `promote-url` (+ `schedule-shaping-fact-types` optional) | 7 edges (`rough-labels`→`block-labels`; `rough-label-keys`→`rough-labels`; `rough-labels-for`→`rough-labels`; `rename-label!`→`label-visibilities`,`rough-label-keys`; `parked-ghosts`→`rough-parking-day`; `promote-url`→`placeholder-label-parts`) | **5**: `views.schedule`, `handlers.schedule`, `schedule-ghost-test`, `placeholder-promotion-test`, (`schedule-test`) | **65** | `schedule-ghost-test`, `placeholder-promotion-test` | **MEDIUM-HIGH.** Non-contiguous (two blocks), so a native move needs two cut sites. Needs `ensure-unlocked!` and `event-days` (private/`>defn` guards) — `ensure-unlocked!` is `defn-` → private→public promotion, and `rename-label!` is a `>defn` (guardrails) whose spec travels with it. Likely **declare needed** if only part of the label family moves. | `cfp-scheduler-killer.schedule.rough-lane` |

---

## Repo 2 — clj-surgeon-records

| # | source ns | forms to move | intra-cluster deps | caller namespaces | call sites | tests covering | native difficulty | suggested destination |
|---|---|---|---|---|---|---|---|---|
| **1 (BEST)** | `clj-surgeon.structural-lens` (`src/clj_surgeon/structural_lens.clj`, 1360 lines; query engine lines 142–676) | 24 (14-form core in bold): `navigation-steps` `where-keys` `invalid-query!` **`parse-query`** `location-key` `unique-items` `outermost-items` `semantic-span` `semantic-partitions` `raw-span-source` `raw-between` `span-gaps` `platform-context-for-file` `source-index` `subtree-items` `matching-descendants` `where-match?` `navigate` `def-initializer` **`apply-query-step`** `query-match` `query-error-result` **`evaluate-query`** **`find-subforms`** | **32 edges** (`apply-query-step`→10, `evaluate-query`→6) | **15** (6 src + 9 test): `edit-dsl`, `mcp-program-tool`, `mcp-inspect`, `mcp-change-buffer`, `intent-transaction`, `core`, `lens-query-test`, `partition-all-test`, `outermost-test`, `structural-lens-test`, `platform-selector-test`, `xray-test`, `parser-admission-test`, `edit-test`, `edit-dsl-test` | **90** | `lens-query-test` (35), `partition-all-test` (15), `outermost-test` (12), `structural-lens-test` (9), `platform-selector-test` (4), `xray-test`, `parser-admission-test`, `edit-test`, `edit-dsl-test` | **VERY HIGH.** **14 same-file vars the cluster still needs** and would have to be promoted or carried: `defining-form-name` `enclosing-form-name` `inside-range` `semantic-path` `one-complete-form` `wildcard-match?` `within-range?` `top-level-locations` `zipper-locations` (all `defn-` → **9 private→public promotions**) plus `max-query-steps` `query-result-limit` `supported-query-steps` `supported-platform-file-extensions` `source-hash`. `structural-lens` keeps `evaluate-lens`/`plan-replacement`/`edit-file*` which call `evaluate-query`, so **the source ns must require the new ns** (one-way; `outline` does NOT require `structural-lens`, so no cycle is created). Two test files use alias `lens`, thirteen use `structural-lens` — the caller edit is **alias-heterogeneous**, which is exactly the case a `from`/`to` fan-out gets wrong. | `clj-surgeon.lens-query` |
| 2 | `clj-surgeon.relation-census` (`src/clj_surgeon/relation_census.clj`, 2225 lines; lines 18–282) | 21: `census-version` `max-pool-size` `max-requested-files` `max-doors` `max-source-bytes` `max-scanned-files` `max-walk-entries` `skipped-directories` `max-listed-files` `max-next-call-bytes` `workspace-root-token` `shown-directory` `directory-repair-phrase` `discovery-fact-keys` `discovery-facts` `source-name-pattern` `named-source-extensions` `named-source-extension?` `coerce-pool-size` `effective-pool-size` `default-doors` | 10 edges | **8** (5 src + 3 test): `core`, `mcp-relation-census`, `census-discovery`, `census-pool`, `mcp-paths`, `mcp-relation-census-test`, `cli-dispatch-test`, `census-pool-test` | **155** | `mcp-relation-census-test` (`:lane :battery`, 82 sites), `cli-dispatch-test` (7), `census-pool-test` (`:lane :fast`, 1), `relation-census-test` | **MEDIUM.** Highest **site density** in either repo (155 rewrites over 8 files). Only 1 same-file external dep (`normalise-request`, used by `discovery-facts`). Two caller aliases in play: `census` (9 files) and `relation-census` (2 files). `relation-census` keeps ~30 internal uses of these bounds → source ns must require the new ns. Ns docstring calls these "one kernel for both entrances", so the cut is semantically pre-blessed. | `clj-surgeon.census-bounds` |
| 3 | `clj-surgeon.relation-census` (refusal/continuation kernel; lines 1057–1420) | 17: `cli-anchor` `shell-safe-token` `shell-quote` `render-command` `utf8-byte-count` `max-refusal-field-chars` `refusal-continuation-keys` `bound-refusal-text` `refusal-identity-keys` `bound-refusal-leaf` `bound-refusal` `within-next-call-bytes?` `cli-command-argv` `cli-next-command-argv` `cli-continuation-overflow-remedy` `cli-continuation` `validate-cli-request-shape` | **24 edges** (`bound-refusal`↔`bound-refusal-leaf` mutual → **a `declare` is required** whichever order they land in) | **5** (2 src + 3 test): `core`, `mcp-relation-census`, `mcp-relation-census-test`, `mcp-relation-census-launcher-test`, `mcp-relation-census-round20-test` | **52** | `mcp-relation-census-test`, `mcp-relation-census-launcher-test`, `mcp-relation-census-round20-test` (all `:lane :battery`) | **HIGH.** The only cluster in either repo with a **true mutual recursion** (`bound-refusal` ⇄ `bound-refusal-leaf`) — a native move must emit a `declare` or reorder; a tool that moves forms one at a time will produce a transiently unresolvable ns. Needs 5 same-file vars back: `known-door-list` (private), `max-next-call-bytes`, `normalise-request`, `request-shape-rules`, `shape-rules`. `shell-safe-token` is `^:private`. Battery-lane tests are minutes-scale — slower oracle. | `clj-surgeon.census-refusal` |

---

## Best candidate, fully specified (no re-discovery needed)

**Task:** move the calendar/ICS family out of `cfp-scheduler-killer.exports` into a new
`cfp-scheduler-killer.exports.calendar` (`src/cfp_scheduler_killer/exports/calendar.clj`),
fix every caller, keep the suite green.

### Forms to move — exports.clj, in file order (line : form)

Carried constants (currently outside the block, both needed by the cluster):
```
41  (def ^:private calendar-prodid …)     ; PRIVATE → must be promoted or moved
78  (def ical-domain "cfp-scheduler-killer.local")
```
The block itself (`;; --- calendar.ics ---` at line 704 through line 1053):
```
706  (defn  ics-escape                [s])
718  (defn  fold-line                 [line])
729  (defn- ics-date                  [^LocalDate d])
732  (defn- ics-stamp                 [])
736  (defn  ics-uid                   [submission])
743  (defn  ics-sequence              [submission])
752  (defn- ics-local-datetime        [^LocalDate d minutes])
768  (defn- absolute-link             [base-url path])
774  (defn  vevent                    [event submission])
815  (defn- snapshot-published?       [state submission])
821  (defn- last-publication-snapshot [event])
831  (defn- ics-sequence-at           [event-id submission-id cutoff])
838  (defn- cancellation-vevent       [{:keys [event submission state sequence]}])
872  (defn  cancellation-for          [event submission])
890  (defn  hold-the-date-uid         [event])
897  (defn  hold-the-date-sequence    [event])
905  (defn  hold-the-date-vevent      ([event] [event base-url]))
942  (defn  cfp-deadline-uid          [event])
950  (defn  cfp-deadline-sequence     [event])
957  (defn  cfp-deadline-ics          ([event] [event base-url]))
998  (defn  submission-ics            [event submission])
1017 (defn  calendar-ics-for          [event sessions])
1030 (defn  calendar-ics              ([event] [event base-url]))
```

### Intra-cluster call graph (42 edges)
```
vevent              -> ics-uid ics-stamp ics-sequence ics-local-datetime ics-date ics-escape fold-line
cancellation-vevent -> ics-uid ics-stamp ics-local-datetime ics-date ics-escape fold-line
cancellation-for    -> last-publication-snapshot snapshot-published? ics-sequence ics-sequence-at
hold-the-date-vevent-> hold-the-date-uid hold-the-date-sequence ics-stamp ics-date ics-escape absolute-link fold-line
cfp-deadline-uid    -> hold-the-date-uid            (docstring reference only)
cfp-deadline-ics    -> cfp-deadline-uid cfp-deadline-sequence ics-stamp ics-date ics-escape absolute-link fold-line
submission-ics      -> cancellation-for cancellation-vevent vevent
calendar-ics-for    -> vevent ics-escape
calendar-ics        -> cancellation-for cancellation-vevent hold-the-date-vevent vevent ics-escape
```

### What the move forces in `exports.clj` (the native-difficulty payload)
Vars left behind in `exports` that the moved cluster still calls:

| var | line | visibility | required action |
|---|---|---|---|
| `speaker-names` | 366 | `defn-` **private** | promote to `defn` (or move) |
| `communicated?` | 209 | `defn-` **private** | promote to `defn` |
| `content-publishable?` | 224 | `defn-` **private** | promote to `defn` |
| `calendar-prodid` | 41 | `def ^:private` **private** | move with the cluster, or promote |
| `public-answers` | 278 | public | becomes `exports/public-answers` |
| `published?` | 235 | public | becomes `exports/published?` |
| `publishable-sessions` | 250 | public | becomes `exports/publishable-sessions` |
| `room-name` | 402 | public | becomes `exports/room-name` |
| `date-range-string` | 312 | public | becomes `exports/date-range-string` |
| `ical-domain` | 78 | public | move with the cluster |

New ns needs: `[cfp-scheduler-killer.store :as store]`, `[cfp-scheduler-killer.events :as events]`,
`[cfp-scheduler-killer.exports :as exports]`, `[clojure.string :as str]`,
`(:import (java.time LocalDate ZoneId Instant) (java.time.format DateTimeFormatter))`.

**Cycle check / the trap:** `exports.clj:1241` is inside a `(comment …)` REPL block and calls
`calendar-ics`. If left in place, `exports` must require `…exports.calendar`, which requires
`exports` back → **circular require, load failure**. The correct native move deletes that
comment line (or moves the whole `(comment …)`), or re-homes the shared helpers so the new ns
does not require `exports`. This is the single failure the grep oracle should be built around.

### Every caller — file:line (43 sites, 9 files)

Production (6 files, 7 sites):
```
src/cfp_scheduler_killer/agent/commands.clj:134        exports/calendar-ics
src/cfp_scheduler_killer/handlers/exports.clj:121      exports/calendar-ics
src/cfp_scheduler_killer/handlers/public_cfp.clj:703   exports/cfp-deadline-ics
src/cfp_scheduler_killer/handlers/public_widgets.clj:549 exports/calendar-ics-for
src/cfp_scheduler_killer/inform.clj:349                exports/submission-ics
src/cfp_scheduler_killer/session_invites.clj:16         exports/submission-ics
src/cfp_scheduler_killer/session_invites.clj:27         exports/cancellation-for
```
Tests (3 files, 36 sites):
```
test/cfp_scheduler_killer/comms_test.clj:138           exports/submission-ics
test/cfp_scheduler_killer/comms_test.clj:139           exports/calendar-ics
test/cfp_scheduler_killer/comms_test.clj:140           exports/ics-uid

test/cfp_scheduler_killer/exports_test.clj:206         exports/calendar-ics
test/cfp_scheduler_killer/exports_test.clj:243         exports/calendar-ics
test/cfp_scheduler_killer/exports_test.clj:393         exports/calendar-ics
test/cfp_scheduler_killer/exports_test.clj:394         exports/ics-uid
test/cfp_scheduler_killer/exports_test.clj:400         exports/calendar-ics
test/cfp_scheduler_killer/exports_test.clj:401         exports/calendar-ics-for
test/cfp_scheduler_killer/exports_test.clj:402         exports/submission-ics
test/cfp_scheduler_killer/exports_test.clj:424         exports/ics-sequence
test/cfp_scheduler_killer/exports_test.clj:430         exports/calendar-ics
test/cfp_scheduler_killer/exports_test.clj:431         exports/ics-sequence
test/cfp_scheduler_killer/exports_test.clj:442         exports/submission-ics
test/cfp_scheduler_killer/exports_test.clj:452         exports/submission-ics
test/cfp_scheduler_killer/exports_test.clj:463         exports/ics-escape
test/cfp_scheduler_killer/exports_test.clj:464         exports/ics-escape
test/cfp_scheduler_killer/exports_test.clj:465         exports/ics-escape
test/cfp_scheduler_killer/exports_test.clj:467         exports/ics-escape
test/cfp_scheduler_killer/exports_test.clj:474         exports/fold-line
test/cfp_scheduler_killer/exports_test.clj:483         exports/calendar-ics
test/cfp_scheduler_killer/exports_test.clj:1514        exports/cfp-deadline-ics
test/cfp_scheduler_killer/exports_test.clj:1515        exports/cfp-deadline-ics
test/cfp_scheduler_killer/exports_test.clj:1544        exports/cfp-deadline-ics

test/cfp_scheduler_killer/schedule_test.clj:482        exports/calendar-ics
test/cfp_scheduler_killer/schedule_test.clj:488        exports/ics-uid
test/cfp_scheduler_killer/schedule_test.clj:489        exports/ics-sequence
test/cfp_scheduler_killer/schedule_test.clj:492        exports/calendar-ics
test/cfp_scheduler_killer/schedule_test.clj:493        exports/ics-sequence
test/cfp_scheduler_killer/schedule_test.clj:509        exports/calendar-ics
test/cfp_scheduler_killer/schedule_test.clj:510        exports/ics-uid
test/cfp_scheduler_killer/schedule_test.clj:511        exports/ics-sequence
test/cfp_scheduler_killer/schedule_test.clj:514        exports/cancellation-for
test/cfp_scheduler_killer/schedule_test.clj:515        exports/calendar-ics
test/cfp_scheduler_killer/schedule_test.clj:528        exports/cancellation-for
test/cfp_scheduler_killer/schedule_test.clj:862        exports/calendar-ics
```
Plus, in the source file itself: `src/cfp_scheduler_killer/exports.clj:1241` (`(comment …)`).
All 9 caller files bind the alias `exports` (single spelling — verified: every
`cfp-scheduler-killer.exports :as exports`).

### Oracles
1. **Suite green**: `make runtests-focus FOCUS=cfp-scheduler-killer.exports-test`,
   then `…schedule-test`, `…comms-test`, then the full `bin/kaocha unit --fail-fast`.
2. **Grep oracle** (must be 0 after the move):
   `rg -n 'exports/(ics-escape|fold-line|ics-uid|ics-sequence|vevent|cancellation-for|hold-the-date-uid|hold-the-date-sequence|hold-the-date-vevent|cfp-deadline-uid|cfp-deadline-sequence|cfp-deadline-ics|submission-ics|calendar-ics-for|calendar-ics|ical-domain)\b' src test bin`
3. **Count oracle**: the same pattern rewritten to the new alias must total **43** sites
   across exactly **9** files.
4. **Load oracle**: `clojure -M -e "(require 'cfp-scheduler-killer.exports.calendar)"` —
   catches the `(comment …)` cycle.

### Smaller sub-variant (if 8–15 forms is a hard bound)
Move only lines 704–878 (the 14-form ICS core: `ics-escape` `fold-line` `ics-date`
`ics-stamp` `ics-uid` `ics-sequence` `ics-local-datetime` `absolute-link` `vevent`
`snapshot-published?` `last-publication-snapshot` `ics-sequence-at` `cancellation-vevent`
`cancellation-for`). 17 intra-cluster edges; **4 caller files / 17 sites**
(`session_invites.clj:27`, `exports_test` ×8, `schedule_test` ×7, `comms_test` ×1) plus
`exports.clj` itself, which then requires the new ns for the 5 remaining assemblers.
Same 4 private→public promotions. Lower fan-out, same structural difficulty.

---

## Raw commands used

```bash
# repo survey
find . -name '*.clj*' | grep -v '/\.git/' | xargs wc -l | sort -rn | head -40

# how many files require a given ns
rg -l 'cfp-scheduler-killer\.<ns>\b' src test bin

# alias spellings a caller binds
rg -o 'cfp-scheduler-killer\.exports :as ([a-z-]+)' -r '$1' src test bin --no-filename | sort | uniq -c
rg -o 'clj-surgeon\.<ns> :as ([a-z0-9-]+)' src test bench bin

# top-level forms with line numbers
rg -n '^\((defn-?|def|>defn-?) ' src/cfp_scheduler_killer/exports.clj

# per-var external callers (files + sites), skipping the defining file
#   /var/tmp/forge/plan2/varcount.sh <src-file> '<alias|alias>'
rg -l "(exports)/ics-uid([^-a-zA-Z0-9?!*><=+]|$)" src test bin
rg -c "(exports)/ics-uid([^-a-zA-Z0-9?!*><=+]|$)" src test bin

# union of a whole cluster (files + total sites)
#   /var/tmp/forge/plan2/union.sh <src-file> '<alias regex>' var1 var2 …

# intra-cluster call graph + same-file external deps for a line range
#   /var/tmp/forge/plan2/edges.sh <src-file> <lo> <hi>
/var/tmp/forge/plan2/edges.sh src/cfp_scheduler_killer/exports.clj 704 1053

# reverse references from outside the block (cycle detection)
for v in ics-escape fold-line … calendar-ics; do
  rg -n "\b$v\b" src/cfp_scheduler_killer/exports.clj | awk -F: '$1<704 || $1>1053'
done

# covering tests
rg -n '^\(deftest' test/cfp_scheduler_killer/exports_test.clj
```
Helper scripts live at `/var/tmp/forge/plan2/{varcount,union,edges}.sh`.
