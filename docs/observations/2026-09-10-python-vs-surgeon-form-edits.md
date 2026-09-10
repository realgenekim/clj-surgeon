# Python string surgery vs clj-surgeon — measured on curtaincall-cfp @ 00e8f0fa

Run 2026-09-10 on forge@anvil. Specimen: `/home/forge/src/curtaincall-cfp` at `00e8f0fa`
(never committed to; every arm ran in a throwaway detached worktree under
`/var/tmp/forge/pyvs/<arm>`, all removed afterwards). Surgeon MCP on 127.0.0.1:7906.
Formatter `standard-clj` (Standard Clojure Style, npm `@chrisoakman/standard-clojure-style`)
was not on this box; installed to `~/.local/bin` for the run.

## Headline table

`churn` = `git diff --numstat` added/deleted lines. "edit only" = immediately after the edit,
before any formatter. "+ standard-clj" = after `standard-clj fix <file>` run twice.

| edit | arm | wall (mechanical) | churn edit-only | churn + standard-clj | correct? | verified? |
|---|---|---|---|---|---|---|
| E1 add `is` to first deftest | **P** python slice + `standard-clj fix` | 0.020 s edit, **0.402 s** total | 4/1 | 58/55 | yes | yes — kondo 0/0, fmt idempotent, 13 tests **101 assertions** 0 fail (was 100) |
| E1 | **S-MCP** `apply_clojure_changes` `insert_after` | **0.75 s** (server `elapsed_ms`) | 57/55 | 57/55 (already standard) | yes | yes — kondo 0/0, fmt idempotent, 13 tests **101 assertions** 0 fail |
| E1 | **S-CLI** | — | — | — | — | **op does not exist**: CLI `:change!` supports only `[:replace …]` and `[:delete true]`; no insertion action |
| E2 alias `events`→`ev` + 2 uses | **P** careful regex + `standard-clj fix` | 0.033 s edit, **1.53 s** total | 3/3 | 401/401 | yes | yes — kondo 4 warnings = baseline 4 |
| E2 | **S-MCP** `edit_clojure` `within`+`from`/`to` | **1.14 s** | **3/3** | 401/401 | yes | yes — kondo = baseline |
| E2 | **S-CLI** `:op :change!` | **0.72 s** (after 2 refusals, 0.21 s + 0.52 s) | **3/3** | 401/401 | yes | yes — kondo = baseline |
| E3 insert defn after `agenda-export-hint` | **P** index/splice + `standard-clj fix` | 0.020 s edit, **0.933 s** total | 5/0 | 402/398 | yes | yes — kondo = baseline |
| E3 | **S-MCP** `apply_clojure_changes` `insert_after` | **1.43 s** | 412/407 | 412/407 | yes | yes — kondo = baseline |
| E3 | **S-CLI** | — | — | — | — | **op does not exist** (same reason as E1) |
| **E4 RED** naive alias rewrite | **P** Astra-literal `s.replace('events/','ev/')` | 0.022 s edit, 0.675 s total | **32/32 — 29 of them wrong** | 413/413 | **NO** | **FALSE GREEN**: kondo 4 warnings (= baseline), `standard-clj fix` idempotent, file parses. Nothing caught it. |
| E4 | **S-MCP** same naive intent | 5.9 ms | **0 — refused** | — | refused | `invalid-intent-form`: `ev/` is not a complete Clojure form. `source unchanged` |
| E4 | **S-MCP** plausible naive intent (`from: events/day-hours, matches: 31`) | 157 ms | **0 — refused** | — | refused | `expect-count-mismatch`: "expected 31 matches, found 2". `source unchanged` |
| E4 | **S-MCP** correct intent (`matches: 2`) | **0.44 s** | **3/3** | — | yes | yes |

Shared cost, identical for every arm: focused test JVM `bin/kaocha unit --focus …` = **17–20 s**.
Baseline (unedited tree): 13 tests, 100 assertions, 0 failures; kondo 0 warnings on the test file,
4 warnings on `schedule.clj`.

Determinism cross-check: S-MCP-E2, S-MCP-E4 and S-CLI-E2 all read back the **same file hash**
`02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c` — three different entrances,
byte-identical result.

## Two findings that dominate everything else

**1. `standard-clj fix` is itself the biggest churn source on this repo.** On the *untouched*
specimen at `00e8f0fa`, `standard-clj fix` rewrites **398 lines of `schedule.clj` and 54 lines of
`placeholder_promotion_test.clj`** — 452 lines of reformatting on files nobody asked to reformat.
It is idempotent (second run = same diff), so it passes the gate; it just drags 450 unrelated lines
into every review. Any arm that ends with `standard-clj fix <file>` inherits that, python or not.
This repo is not currently in Standard Clojure Style.

**2. Surgeon splits into a zero-churn path and a formatting path.** `edit_clojure` with
`within` + `from`/`to` preserves every byte outside the replacement: **3 lines changed, exactly the
3 intended** (E2, both entrances). `apply_clojure_changes` with `insert_after` runs a post-edit
formatter over the whole file, producing the same class of collateral churn as `standard-clj`
(57/55 on the test file, 412/407 on `schedule.clj`) — and that output is already
standard-clj-clean, so `standard-clj fix` afterwards changes nothing. This matches the standing
house ruling: `within`+`from`/`to` is a measured winner, whole-owner/namespace-scoped rewrites are
a measured loser on churn.

## The E4 red case, in full

`schedule.clj` contains the string `events/` **31 times**. Exactly **2** are uses of the `events`
alias (`(events/day-hours event)`, inside `agenda-days` and `schedule-page`). The other **29** are
URL literals: `"/events/"`, `"/api/events/"`.

- **Python (Astra-literal `s.replace`)**: rewrote all 31. The file still parses, `standard-clj fix`
  is idempotent on it, and **clj-kondo reports exactly the baseline 4 warnings**. The routes are now
  `/ev/:slug/exports/calendar.ics` and `/api/ev/:slug/schedule/block-fill` — 19 broken hrefs and
  form actions, shipped green. No lint, no format, no parse check can see it; only an HTTP test
  that exercises those routes would.
- **Surgeon**: refused twice, in 5.9 ms and 157 ms, **source unchanged both times**. First because
  `ev/` is not a complete Clojure form (you cannot express a substring edit at all). Second — the
  much more interesting one — because the declared count was 31 and the derived count was 2. The
  count guard is precisely the control that turns "31 substring hits" into "2 real sites, and your
  intent disagrees with the code."

Note that the careful-python E2 arm *did* get this right, using
`re.subn(r'(?<![\w/.\-"])events/', 'ev/', s)`. That lookbehind is not obvious, it was written
knowing the trap existed, and nothing in the toolchain would have told the author it was needed.

## Exact commands

```bash
# worktrees (removed at the end)
git -C /home/forge/src/curtaincall-cfp worktree add -f --detach /var/tmp/forge/pyvs/<arm> HEAD

# P arms
python3 /var/tmp/forge/pyvs/_scripts/e{1,2,3,4}_py.py
standard-clj fix <file>          # run twice, second must change nothing

# verification, every arm
~/bin/clj-kondo --lint <file>
bin/kaocha unit --focus cfp-scheduler-killer.placeholder-promotion-test   # E1 only; -J-Xmx512m via bin/kaocha

# S-CLI (E2)
clj-surgeon :op :change! :spec-file - :receipt-out /var/tmp/forge/pyvs/scli-e2.receipt.edn <<'EDN'
{:changes [{:id :req :in ["src/cfp_scheduler_killer/views/schedule.clj"]
            :owner {:kind :namespace :name cfp-scheduler-killer.views.schedule}
            :find "[cfp-scheduler-killer.events :as events]"
            :do [:replace "[cfp-scheduler-killer.events :as ev]"] :expect {:matches 1}}
           {:id :uses :in ["src/cfp_scheduler_killer/views/schedule.clj"]
            :forms [agenda-days schedule-page] :find "events/day-hours"
            :do [:replace "ev/day-hours"] :expect {:matches 2 :each-form 1}}]
 :expect {:changes 2 :edits 3 :files 1}}
EDN

# S-MCP (E2/E4) — mcp__clj-surgeon__edit_clojure
{"workspace_root":"/var/tmp/forge/pyvs/smcp-e2",
 "edits":[{"file":"src/cfp_scheduler_killer/views/schedule.clj","within":{"namespace":true},
           "from":"[cfp-scheduler-killer.events :as events]","to":"[cfp-scheduler-killer.events :as ev]"},
          {"file":"src/cfp_scheduler_killer/views/schedule.clj","within":{"root":true},
           "from":"(events/day-hours event)","to":"(ev/day-hours event)","matches":2}]}

# S-MCP (E1/E3) — mcp__clj-surgeon__apply_clojure_changes, changes[].insert_after
```

## Caveats on the wall numbers

P's wall is pure script execution; it excludes the time to *write* the script, which for E1 meant
retyping the whole 30-line deftest body exactly (the Surgeon E1 call named two lines). Surgeon's
wall is the server's own `elapsed_ms` on an already-warm JVM; a cold `clj-surgeon` CLI start would
add several seconds, and S-CLI-E2 needed three attempts (two refusals: a rejected
`:workspace-root` key, then a wrong owner name for line 1112) totalling 1.45 s of tool wall plus
one extra lookup. Neither column includes agent thinking time. On this evidence, wall is not the
axis that separates these tools — both are well under two seconds and both are dwarfed by the 17–20 s
test JVM.

## For Gene

No — on raw speed we cannot beat hand-rolled python, and we should stop pretending that is the
contest. Python edited these files in 20–33 **milliseconds**; Surgeon took 0.4–1.4 **seconds**.
Both are noise next to the 17-second test JVM that has to run afterwards either way, so a second of
tool time buys nothing and costs nothing. The contest is what happens on the edit where the string
you searched for is not the thing you meant. On E4 — a real, unconstructed trap sitting in
`schedule.clj` today, where `events/` appears 31 times and exactly 2 of them are the alias — python
did what you told it, rewrote all 31, and produced a file that **parses, formats idempotently, and
lints identically to baseline** while 19 URLs now point at `/ev/`. Every gate in the pipeline said
green. Surgeon could not even express that edit (`ev/` is not a Clojure form, refused in 6 ms,
source untouched) and, when handed the plausible version of the same mistake, refused with
"expected 31 matches, found 2" — it made the author's wrong count collide with the code's real
count before anything was written. That is the whole product: not speed, but a machine that knows
the difference between text and a form, and a declared count that has to be true. The other thing
this run turned up is worth more than the comparison: `standard-clj fix` rewrites 452 lines of this
repo's untouched source, so Astra's final step is quietly the largest diff in every one of these
edits — that is a Curtain Call formatting decision to make on purpose, not a tool question.
