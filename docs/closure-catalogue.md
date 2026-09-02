# The closure catalogue

*Where, in three repos we own, one structural verb would replace many model returns of typing or
reading. Read-only survey, bridge seat, 2026-09-02. Nothing was edited in any repo.*

## The law being catalogued

**An agent's cost is its count of decisions, not its count of edits.** A verb wins where one
intent has many mechanical consequences, the consequences are computable from the forms, and
the model would otherwise have to type or read them. Native `apply_patch` batches unbounded
*edit sites* into one return, so site count alone never buys anything. Only two terms grow:
**files the agent must READ and cannot infer**, and repair returns once one-shot generation stops
being reliable. This survey measures the first term directly and ranks by it.

Measured anchor (rf1, this tree): nine forms, 16 external sites in three files plus seven
internal sites — **1.3 s of tool time against 141–152 s of native typing**, 9–10 model returns
against 1.

## Method and cost model

Every candidate is scored from clj-kondo's `:analysis` (`var-definitions`, `var-usages`,
`namespace-usages`), the measured route.

| term | definition |
|---|---|
| **S**, edit sites | require clauses + qualified var uses the intent touches |
| **P**, files patched | files whose bytes change |
| **R**, files-to-read | **distinct require *shapes*** (alias spelling × refer/plain) + 1 for the defining file. A file whose require shape is already known contributes nothing an agent must read. For class D, R = every caller file, because each call site is a judgment. |
| native returns | `R + ceil(P/2)` — rf1's ratio: ~1 return per file read, ~1 per 2 files patched |
| native seconds | `20 × returns` |
| tool seconds | `40` (one call + one receipt read) `+ 0.26 × P` (rf1's 1.3 s over 5 files) |
| ratio | native seconds ÷ tool seconds |
| rank | **R × S** |

**Calibration.** The model is not fitted; it is rf1's own arithmetic. Applied blind to rf1's own
task (`clj-surgeon.mcp-inspect/validate-inspect-params`, cluster 28, 3 caller files) it predicts
R=4, P=5, 7 returns, **140 s** — against the measured 141–152 s. The kondo-derived exclusive
closure also agrees with `:ls-extract` to within 1–3 forms on the Surgeon files where
`:ls-extract` runs (59 vs 60; 28 vs 31).

Repos: `clj-surgeon-main` @ `8e9a634` (159 ns), `marvin-voice-remote` @ `663703a` (58 ns),
`curtaincall-cfp` @ `d9afe8e9` on branch `staging` (375 ns). src + test both analysed.

---

## Ranked table — top 25 of 735 candidates

**Read the `closable` column first.** Rank is R×S as specified, and the ranking is dominated by
class D, which no verb can own.

| # | cls | repo | intent (target) | files | src/test | edit sites S | files-to-read R | native ret | native s | tool s | ratio | R×S | closable |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | D | cfp | `cfp:events/event-by-slug` — thread a new arg through every caller | 101 | 30/71 | 365 | 101 | 152 | 3040 | 66 | 45.9× | 36865 | **no** |
| 2 | D | cfp | `cfp:test-helpers/with-temp-store` | 123 | 0/123 | 247 | 123 | 185 | 3700 | 72 | 51.4× | 30381 | **no** |
| 3 | D | cfp | `cfp:server/create-app` | 94 | 1/93 | 249 | 94 | 141 | 2820 | 64 | 43.8× | 23406 | **no** |
| 4 | D | cfp | `cfp:store/snapshot` | 60 | 35/25 | 324 | 60 | 90 | 1800 | 56 | 32.4× | 19440 | **no** |
| 5 | D | cfp | `cfp:store/submission-by-id` | 66 | 21/45 | 253 | 66 | 99 | 1980 | 57 | 34.6× | 16698 | **no** |
| 6 | E | surgeon | `clojure.test` — swap the test lib | 84 | 0/84 | 7984 | 2 | 44 | 880 | 62 | 14.2× | 15968 | yes |
| 7 | E | cfp | `clojure.test` | 184 | 0/184 | 11883 | 1 | 93 | 1860 | 88 | 21.2× | 11883 | yes |
| 8 | A | cfp | `cfp:auth/issue-token!` — extract 4-form auth cluster | 81 | 3/78 | 122 | 82 | 124 | 2480 | 62 | 40.3× | 10004 | yes |
| 9 | D | cfp | `cfp:store/now-iso` | 55 | 32/23 | 176 | 55 | 83 | 1660 | 54 | 30.6× | 9680 | **no** |
| 10 | D | cfp | `cfp:auth/issue-token!` | 82 | 4/78 | 113 | 82 | 123 | 2460 | 61 | 40.1× | 9266 | **no** |
| 11 | D | cfp | `cfp:store/append!` | 55 | 25/30 | 142 | 55 | 83 | 1660 | 54 | 30.6× | 7810 | **no** |
| 12 | D | cfp | `cfp:auth/current-person` | 37 | 29/8 | 191 | 37 | 56 | 1120 | 50 | 22.6× | 7067 | **no** |
| 13 | D | cfp | `cfp:store/person-by-id` | 56 | 20/36 | 115 | 56 | 84 | 1680 | 55 | 30.8× | 6440 | **no** |
| 14 | D | cfp | `cfp:events/form-for-event` | 63 | 4/59 | 97 | 63 | 95 | 1900 | 56 | 33.7× | 6111 | **no** |
| 15 | D | cfp | `cfp:events/committees-for-event` | 63 | 11/52 | 93 | 63 | 95 | 1900 | 56 | 33.7× | 5859 | **no** |
| 16 | E | mvr | `clojure.test` | 29 | 0/29 | 5692 | 1 | 16 | 320 | 48 | 6.7× | 5692 | yes |
| 17 | D | cfp | `cfp:committees/add-member!` | 55 | 6/49 | 100 | 55 | 83 | 1660 | 54 | 30.6× | 5500 | **no** |
| 18 | D | cfp | `cfp:submissions/parse-answers` | 64 | 8/56 | 85 | 64 | 96 | 1920 | 57 | 33.9× | 5440 | **no** |
| 19 | D | cfp | `cfp:submissions/create-submission!` | 63 | 5/58 | 77 | 63 | 95 | 1900 | 56 | 33.7× | 4851 | **no** |
| 20 | D | cfp | `cfp:events/create-event!` | 57 | 2/55 | 77 | 57 | 86 | 1720 | 55 | 31.4× | 4389 | **no** |
| 21 | E | cfp | `clojure.string` | 272 | 129/143 | 4367 | 1 | 137 | 2740 | 111 | 24.8× | 4367 | yes |
| 22 | D | cfp | `cfp:store/read-events` | 38 | 1/37 | 111 | 38 | 57 | 1140 | 50 | 22.9× | 4218 | **no** |
| 23 | B | cfp | `cfp:store` — rename/move the event store ns | 170 | 68/102 | 2056 | 2 | 88 | 1760 | 84 | 20.8× | 4112 | yes |
| 24 | D | cfp | `cfp:submissions/parse-speaker` | 58 | 4/54 | 65 | 58 | 87 | 1740 | 55 | 31.6× | 3770 | **no** |
| 25 | D | cfp | `cfp:reviews/set-status!` | 38 | 5/33 | 95 | 38 | 57 | 1140 | 50 | 22.9× | 3610 | **no** |

**20 of the top 25 are class D and no verb can own any of them** (see "not in our favour").
The closable-only ranking is the one that matters:

| # | cls | repo | target | files | S | R | alias spellings | native s | tool s | ratio | R×S |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | A | cfp | `cfp:auth/issue-token!` | 81 | 122 | 82 | — | 2480 | 62 | 40.3× | 10004 |
| 2 | B | cfp | `cfp:store` | 170 | 2056 | 2 | `store` | 1760 | 84 | 20.8× | 4112 |
| 3 | B | cfp | `cfp:events` | 170 | 1166 | 3 | `:plain/events` | 1780 | 84 | 21.1× | 3498 |
| 4 | A | cfp | `cfp:views.organizer-layout/organizer-shell` | 29 | 79 | 30 | — | 920 | 48 | 19.1× | 2370 |
| 5 | B | cfp | `cfp:submissions` | 93 | 573 | 3 | `sub/submissions` | 1000 | 64 | 15.5× | 1719 |
| 6 | B | cfp | `cfp:auth` | 115 | 521 | 3 | `:plain/auth` | 1220 | 70 | 17.4× | 1563 |
| 7 | A | surgeon | `surg:mcp-contract/validate-tool-params` | 7 | 193 | 8 | — | 260 | 42 | 6.1× | 1544 |
| 8 | B | mvr | `mvr:channel` | 9 | 502 | 3 | `ch/channel` | 160 | 43 | 3.8× | 1506 |
| 9 | B | cfp | `cfp:web.http` | 28 | 465 | 3 | `http/web-http` | 360 | 48 | 7.6× | 1395 |
| 10 | B | cfp | `cfp:test-helpers` | 123 | 372 | 3 | `:plain/test-helpers` | 1300 | 72 | 18.0× | 1116 |
| 11 | C | cfp | `cfp:events/event-by-slug` | 101 | 365 | 3 | `:plain/events` | 1080 | 67 | 16.2× | 1095 |
| 12 | B | cfp | `cfp:reviews` | 63 | 482 | 2 | `reviews` | 680 | 57 | 12.0× | 964 |
| 13 | B | cfp | `cfp:schedule` | 29 | 477 | 2 | `schedule` | 340 | 48 | 7.1× | 954 |
| 14 | B | surgeon | `surg:structural-lens` | 39 | 282 | 3 | `lens/structural-lens` | 460 | 50 | 9.1× | 846 |
| 15 | A | surgeon | `surg:intent-transaction/compile-transaction` | 8 | 92 | 9 | — | 280 | 43 | 6.6× | 828 |

---

## Per-class summary

| class | surgeon | mvr | cfp | median S | median R | median ratio | verb |
|---|---|---|---|---|---|---|---|
| **A** extractions (cluster ≥4 forms, ≥3 external caller files) | 25 | 5 | 26 | 25 | 5 | 3.9× | `extract-with-rewire` (`bridge/rf2-extract-rewire`) — **covers it** |
| **B** namespace rename/move (≥3 requiring files) | 43 | 16 | 108 | 34 | 2 | 2.9× | `:rename-ns!` where the alias is uniform; `alias_migration` lib-only (`bridge/q5z-alias-migration`) where spellings are mixed |
| **C** var rename (≥10 sites, ≥5 files) | 25 | 11 | 139 | 22 | 2 | 3.3× | `alias_migration` (same-lib degenerate case). Wants a narrowed **`var_rename`** that omits the lib move |
| **D** parameter threading (≥8 callers, ≥4 files) | 61 | 36 | 218 | 16 | 6 | 4.3× | **none possible** — see below |
| **E** require migration (lib in ≥15 files) | 8 | 5 | 9 | 306 | 1 | 7.4× | `require_change` — **covers it, and is the measured winner** |
| **F** fan-out ≥20 sites across ≥8 files | 38 | 47 | 156 | — | — | — | 241 candidates; entirely a subset of A/B/C/D/E, no new shape |

Class F surfaced nothing structurally new: every ≥20-site/≥8-file candidate is already an
extraction, a rename, or a threading job. There is no sixth shape hiding in these repos.

---

## The top 5 real wins

Ranked among closable candidates that are work someone would plausibly want done.

1. **`cfp-scheduler-killer.store` → `event-store` (B, R×S 4112, ~29 min native → ~1.4 min).**
   170 requiring files, 2056 sites. This is the pinned anchor `R` of the slope experiment, and it
   is real: cfp is mid-migration to an event-store vocabulary. It is also the *adversarial* case —
   **170 of 170 files alias it `store`, identically** — so R collapses to 2 and a `sed` is nearly
   right. What makes it work and not a `sed` is the sibling trap: `store-pg`,
   `store-checkpoint`, `store*-test` share the prefix, and a regex stomps them.

2. **`cfp-scheduler-killer.events` rename (B, R×S 3498, ~30 min → ~1.4 min).** 170 files, 1166
   sites, and **two spellings** — 
   most files use `:as events`, some use the fully qualified plain form. A `sed` on `events/`
   silently misses the plain uses; a verb sees both. The most valuable *mixed-spelling* intent in
   any repo we own.

3. **`clj-surgeon.mcp-contract/validate-tool-params` extraction (A, R×S 1544, ~4.3 min → 42 s).**
   A 59-form exclusive closure inside a 1415-line file, 193 sites, 7 caller files — the rf1 shape
   at 6× the cluster size, on our own tree, in the layer we are actively carving. `:ls-extract`
   independently confirms the cluster (60 forms). This is the one candidate where the verb is
   already built, the repo is ours, and the work is on the roadmap.

4. **`cfp-scheduler-killer.web.http` rename (B, R×S 1395, 6 min → 48 s).** 28 files, 465 sites,
   spellings split `http` / `web-http`. Small enough to be done today, irregular enough that the
   native route must read both shapes, and cfp's `web.*` layer is being reorganised anyway.

5. **`marvin-voice-remote.channel` rename/split (B, R×S 1506, 2.7 min → 43 s).** Only 9 files but
   **502 sites**, spellings `ch` / `channel`. The ratio is the catalogue's weakest at 3.8×, and it
   is listed because it is the *only* live-product intent in the set: the channel namespace is a
   shipped surface under active change. It is the honest demonstration that on a 40-file app the
   verb buys minutes, not hours.

---

## Not in our favour

Four findings that say where **not** to spend effort.

**1. The biggest fan-out in our repos is in the one class no verb can own.** 315 of 735
candidates are class D, and they take 20 of the top 25 ranks — up to 123 caller files and 3,700 s
of native work. They are unreachable for exactly the reason the slope spec gave for rejecting
transformation (b): *what to pass at each call site is a judgment from that caller's scope, not a
mechanical closure.* A verb that guessed would produce compiling, wrong code. The only closable
sub-case is threading a value that is provably in scope under a known name at every site; nothing
in these three repos matches. **Do not build `param_thread`.**

**2. Our repos are alias-uniform, so the R term barely exists.** Across 364 class-B/C/E
candidates: 238 have **one** spelling, 107 have two, 15 have three, 4 have four. The maximum
anywhere is four (`cfp-scheduler-killer.domain.speakers`, aliased `domain` / `domain-speakers` /
`speaker-domain` / `speakers` across 10 files). Since R = distinct shapes + 1, **R is 2–3 for
almost every closable intent regardless of whether 9 files or 170 files are affected.** This is
the cfp finding of the slope spec (68 of 68 identical) generalised to three repos and to the test
trees. The measured consequence: median ratio for closable candidates is **3.4×**, and only 24%
clear 5×. The fan-out slope that would make the tool 10× **does not exist in real code we own**;
it must be synthesised, exactly as `sl1` does.

**3. The class with the largest sites is the class nobody would ever ask for.** `clojure.test`
(11,883 sites in cfp) and `clojure.string` (4,367 across 272 files) top class E on raw fan-out,
and swapping either is not a task any human wants. Real class-E work — `taoensso.timbre`
(64 files, cfp), `datastar-kit.ds`, `com.fulcrologic.guardrails.core` — sits at R=1–2 and
4.5–11.7×, and `require_change` already wins it in one call. **Class E is closed. It needs no new
verb.**

**4. The tool cannot currently read the repos where the fan-out lives.** On `main` (`8e9a634`),
`:ls` refuses with `:forward-reference-analysis-failed` and an **empty diagnostic** on:

| repo | `:ls` OK | refused | of the 10 biggest src files |
|---|---|---|---|
| clj-surgeon-main | 10 | 0 | 10 |
| marvin-voice-remote | 4 | 6 | 10 |
| curtaincall-cfp | **1** | **9** | 10 |

The exit codes are 2 and 3 — clj-kondo's *findings* codes, read as failure. This is the same root
cause `bridge/rf2-extract-rewire` already fixed for the `:ls` false refusal, still live on main.
**Class A on cfp is unreachable from the CLI today**, and every class-A number for cfp and mvr in
this catalogue was computed from kondo rather than from `:ls-extract` for that reason. The verb
succeeds 10/10 on the tree it was built in and fails 9/10 on the repo it would be sold into; that
asymmetry is invisible from inside Surgeon.

---

## Exact commands (reproducible)

```bash
# 1. analysis, per repo (src + test)
~/bin/clj-kondo --lint <REPO>/src <REPO>/test \
  --config '{:analysis true :output {:format :edn}}' > <repo>.edn
# exit 3 = findings present, not failure

# 2. classes B, C, D, E   (measure.clj — cost model + shape counting)
bb -cp . -m measure <repo>.edn <repo>          # -> <repo>-cands.edn

# 3. class A: exclusive-dependency closures from the same analysis
bb -cp . -m classa2 <repo>.edn <repo>          # -> <repo>-classA.edn

# 4. class A validation against the tool itself (run from clj-surgeon-main)
bb -m clj-surgeon.core :op :ls        :file <ABS PATH>
bb -m clj-surgeon.core :op :ls-extract :file <ABS PATH> :form <FORM>

# 5. the :ls refusal census
for p in $(find <REPO>/src -name '*.clj' -o -name '*.cljc' \
           | xargs wc -l | sort -rn | sed -n '2,11p' | awk '{print $2}'); do
  bb -m clj-surgeon.core :op :ls :file "$p" | head -1 | grep -q ':ns' \
    && echo OK || echo REFUSED
done
```

`measure.clj` and `classa2.clj` are ~120 lines total and live in the bridge seat's scratchpad;
they contain no repo state and can be regenerated from the model table above. Nothing in this
survey wrote to any repo other than this file.

*Bridge seat, 2026-09-02. 735 candidates over 592 namespaces and 183,704 var usages.*
