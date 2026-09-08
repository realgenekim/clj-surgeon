# Astra B01 report

Written: 2026-09-08T03:58:42+00:00. **B01 registration and evidence freeze delivered. B02 apparatus
build may start; row 1 remains a friction-medium ceiling, not friction-low.**
No push or merge. No edits to `~/bin/round`, the cold-start harness, installed
routing or any cc-split/coldstart worktree. Both designated branches are clean.

## Commits

| Deliverable | Commit | Worktree / branch |
|---|---|---|
| Round-5 LID registration | `4856f5aa48716f6c4c149655f2e5616424edb941` | `/home/forge/src/clj-surgeon-split` / `astra/namespace-split` |
| Tree correction and dated log/resume pointers | `7ab11965c379c523742fea36c39cf6bec17104e2` | `/home/forge/src/clj-surgeon-records` / `records/MCP-main` |
| Frozen row-1 ledger and log/resume pointers | `e0b365e4346895ad42db86692814bd5121aaf085` | `/home/forge/src/clj-surgeon-records` / `records/MCP-main` |

Each is authored by `forge-anvil <forge-anvil@anvil>` with
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`.
This report is also archived as its own records commit at
`docs/observations/2026-09-08-astra-B01-report.md`; its identity is appended to the
requested external report after commit, avoiding a self-referential commit SHA.

## Registration and evidence

NS-SPLIT-034 records the structural ruling: every line owned by the changed call
form shifts by the head delta; ownership follows paren structure at every depth;
multiline string contents never move. NS-SPLIT-035 preserves the exact in-place
forms/polish require pin. NS-SPLIT-036 registers namespace_split in the exact
full-profile tool list. Earlier IDs remain resolvable; 034/035 explicitly
supersede 032/033 in the append-only registry. Code and existing behavioral
witnesses carry both @spec and INTENT marker pairs; census 190→193 and the local
bidirectional scan now includes the catalog/witness paths. No compiler behavior
changed and no new fail-first run is claimed. The original red provenance remains
in the design/registry and prior round records.

The local `linked-intent-dev` skill was absent after search; the available
self-contained `linked-intent-testing` skill was read and applied to this
explicitly authorized retrospective registration. No new feature phase or new
cross-leaf behavior was implemented.

Direct audit expression (executed from the code worktree):

```sh
clojure -M -e "(require 'clj-surgeon.mcp-intent-contract)(prn (select-keys (clj-surgeon.mcp-intent-contract/audit-current-repository) [:ok :violations]))"
```

```clojure
{:ok true, :violations []}
```

Exact runner output, exit 0:

```text
ROUND b01: tip 4856f5aa→4856f5aa build=ok state=committed in_call=34483ms PAPERCUTS=0 oracles=4/4 witnesses=35/428/0 intents=ok new_intent_markers=n/a new_spec_markers=n/a total=57s
```

The literal requested command
`~/bin/round /home/forge/src/clj-surgeon-split /dev/null b01 --no-build`
first exited 3 because it mistook this active B01 Codex session for another
builder. Original receipt: `/var/tmp/forge/plan2/cellC/b01-round-first-refusal.log`.
The tool subprocess has a separate process group from its owner session.
The successful bounded retry used only existing options:

```sh
flock -n /home/forge/src/clj-surgeon-split/.round.lock /home/forge/bin/round /home/forge/src/clj-surgeon-split /dev/null b01 --no-build --adopt 520384
```

520384 was this B01 session, confirmed from PID/parent/argv/root evidence, not an
unowned builder. The explicit outer normal lock held ownership through grading;
`--no-build` skips the builder/adopt execution branch, so no builder was started,
waited on or killed. This does not certify the script's separate adopt lock.
Both inactive B01 lock files were removed after checking they had no holders.
The only grading fixture created was `/var/tmp/forge/plan2/build/round-b01-fx`.
The code tip was committed before grading and unchanged afterward.
`new_intent_markers=n/a` is correct for no-build; the code commit supplies the new
links. 57 s measures grade-only work, not a complete fix or timing cohort.

Validation: 35 tests / 428 assertions / 0 failures / 0 errors in the no-build
round, paper cuts 0 and four fixture oracles passed. Six focused warm witnesses
separately passed 68 assertions. An initial selective invocation incorrectly
ran admit's whole-namespace refusal-census fixture without the refusal drivers
(three fixture failures); the direct registry Var plus the other five witnesses
passed with no behavior changes. `~/bin/clj-kondo` found zero errors and five
warnings, exactly matching the pre-change admit-test baseline. Diff checks pass.

**Full landing gate remains pending.** Additional `make test` passed the recovery
battery 3/3, then stopped at `battery-fresh`: the existing receipt for
`4eaabdeaa17277406abd96924b41a6d7f5ca58cf` is 31 counted commits behind HEAD,
exceeding the maximum 30 (36 raw, five archive-only ignored). Exit 2; the remaining
landing gate did not run. Log: `/var/tmp/forge/plan2/cellC/b01-make-test.log`.
No full-suite pass, new reviewer approval or landing is claimed. The receipt
threshold and apparatus were not changed to hide this failure.

## B02 must consume

The canonical handoff is
`/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-row1-split-ledger.md`.
It freezes the exact Cell C request/grammar and exclusions, artifact digests,
four pilot-2 input cells with pinned CC/MVR SHAs, observed model identities and
inline+P delivery (retaining Claude's extra T exposure), six fresh native floors
plus six pairs in two waves per row-1 entrance, all missing gate receipts, and
the route/escape/kill examples plus exact proposed ROW1_RECEIPT line format.

B02 must qualify explicit-session resume; one external canonical-worktree/fixture
lease across normal/adopt and final receipt; actual child terminal exits; parsed
exact-candidate checks and pending cold proof; and actual descendant exec/end,
filesystem/network and eval observation. Preserve the newly observed no-build
self-owner refusal as a regression input. A child printing PASS/committed then
exiting nonzero or omitting cold proof must fail; a hidden-child probe must be
observed. Close with a real disposable ready-image red→warm-green→one-final-cold
smoke, or CONTINUE with the missing receipt and certification/cohort stop retained.

The saved pilot tasks use missing-Var load errors and caller-created tests, not
precommitted intended assertion-red body-only seeds. Their cold gate spelling
also needs reconciliation with full repo prerequisites. Their input freeze is
not certification of the running pilot. Do not alter live slots or retroactively
claim pure P, a full model/repo matrix, or a performance comparison.

Row 1 keeps its ≥30% and >2SD primary gate in both fresh waves. Row 5 keeps
Astra's registered primary (25% + ≥90 s + >2SD; all six accepted, ≥5/6 without
repair/fallback), with 30% fixed as stricter secondary. Rankings remain hypotheses.
Fan-out suspension is prepared only for the measured 3- and 21-site informed
cases; installation receipts remain absent. B03 consumes qualified apparatus
hashes for observed pilot/route/refusal receipts; B05 consumes the fresh-control
allocation. The full battery freshness gate still needs its own current receipt
before full landing approval.
