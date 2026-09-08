# Namespace split paper cuts, round 2

Recorded 2026-09-08T00:15:34.498415+00:00 on `astra/namespace-split`, based on `9b205fd4`.

All six registered promises are implemented in the namespace-split leaf. Each
destination accepts a nonblank `:doc`; otherwise a deterministic generated doc names
its source, form count and first three public forms in source order. Private metadata,
private attribute maps and required promotions are accounted for. The original
monolith doc goes into **none** of the destinations. NS-SPLIT-022 supersedes the
round-1 NS-SPLIT-019 cloning promise without deleting or renumbering its registry row.

Imports now use located captured kondo class usages and distinguish short class
names from fully qualified tokens. Destination requires form one sorted block at
the source continuation indent, defaulting to three spaces. Caller additions join
the existing library group, preserving unrelated entries, comments and indentation;
source and test callers follow the same rule. Ordinary and anonymous calls shift
only continuation prefixes equal to the old argument column. Shortening and nested
heads on the same line are covered; strings and unaligned lines remain untouched.

The new `:unrequired_qualified_refs` receipt rows are advisory candidate file/line
references. They add no requires and do not block publication. The final fixture
reports store/now-inst at dashboard.clj:44 and review.clj:421, plus the pre-existing
quoted markdown.core/md-to-html-string in public_cfp.clj:119. The latter is used by
requiring-resolve: this advisory describes missing requires, not runtime failure.
Compiler-derived provenance docstrings are excluded from stale-prose advisories.

## Intent and failing witnesses

The append-only registry is
[namespace-split-papercuts.edn](../intent/helper-extraction/namespace-split-papercuts.edn).
INTENT and INTENT-TEST links remain bidirectionally checked in the normal suite.

| Intent | Behavioral witness |
|---|---|
| NS-SPLIT-022 | `destination-docstrings-describe-the-destination`: overrides, escaping, generated summaries, private metadata/attribute maps, first three public names; replaces superseded cloning witness |
| NS-SPLIT-023 | `destination-imports-follow-short-class-usage`: short/FQ static tokens, class values, constructors and type hints; excludes unused imports |
| NS-SPLIT-024 | `qualified-call-continuations-track-head-width`: caller/destination × ordinary/anonymous × shorter/longer × aligned/unaligned; nested same-line heads, multiline string contents and head-only lines |
| NS-SPLIT-025 | `destination-requires-share-source-layout`: mixed external/sibling requires, inline first entry, inherited spaces/tabs and three-space default |
| NS-SPLIT-026 | `caller-require-groups-remain-in-place`: core/external/project grouping in source and test callers; existing NS-SPLIT-017 witnesses retain first/middle/last retired positions and trivia |
| NS-SPLIT-027 | `unrequired-qualified-refs-are-advisory`: exact candidate coordinates, required namespaces, aliases, Java classes and prose exclusions; no added require |

Initial retained red: 21 tests / 190 assertions, 23 failures, zero errors. The
private-metadata doc edge subsequently earned three failing assertions, nested
alignment one, anonymous calls four, and a core-only caller group two, each before its repair. The final focused
compiler + boundary run passes **28 tests / 275 assertions**, zero failures/errors.
The corpus census grows from 16 to 21 tests in the pure split namespace: six new
witnesses replace one explicitly superseded witness. No active intent is retired
to make a failing test green.

## Fresh fixture and four independent oracle lines

Baseline tool: a detached `9b205fd4` checkout at
`/var/tmp/forge/plan2/build/surgeon-papercuts2-baseline`. Baseline application:
`cc-papercuts2-baseline`; final application: `cc-papercuts2-final`, both fresh
curtaincall-cfp `d9205abc` worktrees under the same build directory. D1-request.edn
is changed only at workspace_root. The supplied architecture test is copied from
the previous retained proof fixture. Both profiles run the owner oracle first,
then the whole Kaocha unit suite. No cc-split-* worktree is changed.

Captured source identity, equal in both calls:
`43b15c857ebcc69b0a4bb8c051d13a319b6eed823a6330a382191e1bb762f938`.

```sh
bb --classpath /var/tmp/forge/plan2/build/surgeon-papercuts2-baseline/src -m clj-surgeon.core :op :split-ns! :request-file /var/tmp/forge/plan2/build/papercuts2-evidence/baseline-request.edn
bb --classpath /home/forge/src/clj-surgeon-split/src -m clj-surgeon.core :op :split-ns! :request-file /var/tmp/forge/plan2/build/papercuts2-evidence/final-request.edn
```

Both exit 0, committed, verification_complete true. Final counts:
141 forms, 20 destinations, five caller files, 87 caller sites, 26 changed files.
The unit and owner-oracle outputs are extracted from the executed in-call proof;
source absence and the focused architecture command are independently checked
again after the final call. Four lines verbatim, with ANSI color bytes removed:

```text
231 tests, 2258 assertions, 0 failures.
views.clj gone: PASS
PASS: views.clj gone; all 141 owners occur exactly once in their 20 listed destination files.
1 tests, 7 assertions, 0 failures.
```

The final layout oracle independently checks all 20 generated headers, short-name
imports, caller grouping, the anonymous-call 51 → 57 indentation, and receipt
coordinates. Its script and output are retained beside the complete staged fixture
diff. The final fixture was restored through the verified 26-file guarded inverse
before the last replay; the earlier receipt and inverse result are retained.

## Lint: preserve the distinction between new findings and repeated old causes

The in-call isolated whole-snapshot comparison is baseline 108 errors / 195 warnings
and candidate 108 / 195: **delta 0 errors / 0 warnings**, introduced errors 0.
The baseline's errors are unchanged static findings, not a clean application lint
claim. All four application acceptance checks pass.

The requested direct `~/bin/clj-kondo --lint src/cfp_scheduler_killer/views` run
reports **0 errors / 15 warnings**, versus **0 errors / 14 warnings** for the original
monolith with the same project configuration. There are **0 NEW warning identities**
after mapping moved Var names to their original identities. The extra raw row is
the pre-existing unrequired store namespace now reported in both dashboard and
review. Both sites existed at monolith lines 1458 and 4346; kondo reported that
namespace only once in the old file. The unchanged unused fb-tags warning follows
its Var into form-builder. The raw outputs and independent lineage comparison are
retained; no warning is suppressed and no require is added to conceal this count.
Thus a raw view-only warning-count delta of zero is not claimed.

Touched compiler, boundary, witness and census files: `~/bin/clj-kondo` reports
0 errors / 0 warnings. Standard Clojure Style formats the implementation and
witness files; `git diff --check` passes.

## Requested source evidence

Actual server require-block diff (three-space indentation, sorted insertion):

```diff
@@ -25,7 +25,23 @@
    [cfp-scheduler-killer.sse :as sse]
    [cfp-scheduler-killer.store :as store]
    [cfp-scheduler-killer.submissions :as submissions]
-   [cfp-scheduler-killer.views :as views]
+   [cfp-scheduler-killer.views.auth :as v-auth]
+   [cfp-scheduler-killer.views.committee :as committee]
+   [cfp-scheduler-killer.views.communications :as communications]
+   [cfp-scheduler-killer.views.dashboard :as dashboard]
+   [cfp-scheduler-killer.views.event-setup :as event-setup]
+   [cfp-scheduler-killer.views.form-builder :as form-builder]
+   [cfp-scheduler-killer.views.format :as format]
+   [cfp-scheduler-killer.views.integrations :as integrations]
+   [cfp-scheduler-killer.views.live-drafts :as live-drafts]
+   [cfp-scheduler-killer.views.log :as v-log]
+   [cfp-scheduler-killer.views.people :as people]
+   [cfp-scheduler-killer.views.portal :as v-portal]
+   [cfp-scheduler-killer.views.public-cfp :as public-cfp]
+   [cfp-scheduler-killer.views.replay :as v-replay]
+   [cfp-scheduler-killer.views.review :as review]
+   [cfp-scheduler-killer.views.schedule :as v-schedule]
+   [cfp-scheduler-killer.views.shell :as shell]
    [clojure.data.json :as json]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
```

Actual multiline call-site diff, including the aligned continuation lines:

```diff
@@ -198,11 +214,11 @@
         errors (events/validation-errors draft)]
     (if errors
       (do (log/info :event-create-rejected :fields (vec (keys errors)))
-          (html-response 422 (views/new-event-page (request-host req)
-                                                   ;; keep what they typed, but show the slug we derived
-                                                   (assoc params :slug (or (:slug draft) (:slug params)))
-                                                   errors
-                                                   (auth/current-person req))))
+          (html-response 422 (event-setup/new-event-page (request-host req)
+                                                         ;; keep what they typed, but show the slug we derived
+                                                         (assoc params :slug (or (:slug draft) (:slug params)))
+                                                         errors
+                                                         (auth/current-person req))))
       (try
         (let [event (events/create-event! draft (or (:email person) "organizer"))]
           ;; Whoever creates a conference joins its Program Committee as chair —
```

The anonymous-call field regression is also visible in the real diff:

```diff
@@ -475,13 +491,13 @@
         ;; Elements first, signals second (CLAUDE.md #9) — a signal patch that
         ;; lands before its target exists fires an effect against nothing.
         (sse/push-to-person! sse/new-event-channel (:id person) "#event-marquee"
-                             #(views/event-marquee (request-host req) typed slug
-                                                   (when-not trim? found) open?))
+                             #(event-setup/event-marquee (request-host req) typed slug
+                                                         (when-not trim? found) open?))
         ;; The URL field's own line answers "is this address free?" live, from
         ;; the same derivation the marquee just used and the same owner lookup
         ;; the create-time refusal uses.
         (sse/push-to-person! sse/new-event-channel (:id person) "#slug-status"
-                             #(views/slug-status slug (events/slug-owner-display slug)))
+                             #(event-setup/slug-status slug (events/slug-owner-display slug)))
         ;; Signals AFTER elements (CLAUDE.md #9): the ghost in the URL box
         ;; follows the derivation, so it never disagrees with the green line.
         (sse/push-signals-to-person! sse/new-event-channel (:id person)
```

Final format.clj and dashboard.clj namespace headers:

```clojure
(ns cfp-scheduler-killer.views.format
  "Split from cfp-scheduler-killer.views: 15 forms — ->local-date, fmt-date, fmt-date-range…"
  (:require
   [cfp-scheduler-killer.events :as events]
   [clojure.string :as str])
  (:import (java.time LocalDate ZoneId) (java.time.format DateTimeFormatter)))

(ns cfp-scheduler-killer.views.dashboard
  "Split from cfp-scheduler-killer.views: 8 forms — alert-rows-partial, event-dashboard-region, event-dashboard-page."
  (:require
   [cfp-scheduler-killer.views.avatar :as avatar]
   [cfp-scheduler-killer.views.format :as format]
   [cfp-scheduler-killer.views.organizer-layout :as organizer-layout]
   [cfp-scheduler-killer.views.review :as review]
   [datastar-kit.ds :as ds]))
```

## Wall and verification

| Same request and proof profile | In-call wall | Kaocha unit | Candidate lint |
|---|---:|---:|---:|
| Before, 9b205fd4 | 35.392597 s | 23.101582 s | 3.431775 s |
| After, final code | 33.390705 s | 21.196349 s | 3.219563 s |

The observed total reduction is 2.002 s; Kaocha varies by 1.905 s.
Non-Kaocha time is 12.291 s before and 12.194 s after. This meets the
33–38 s in-call constraint without removing verification. It is one functional
before/after replay, not a new speedup estimate or matched native cohort. Gene's
prior fresh-caller 80–151 s versus native 408/486 s remains prior-wave evidence;
this repair does not rerun or broaden that comparative claim. Earlier successful
repair replays are retained, including 34.943 s and the pre-anonymous 34.954 s call.
A preliminary launcher receipt lacking the expected round-1 checks was excluded
from the comparison; the explicit isolated 9b205fd4 invocation above is the baseline.

Final `make test` exited 0 on the frozen implementation: JVM fast/integration
**781 tests / 9,850 assertions**, Babashka **873 tests / 7,511 assertions**, zero
failures/errors. The operation oracle, sentinel intent audit, isolation, recovery,
temp-path checks and repository hygiene pass. The standing battery-fresh gate
accepts its recorded full battery; this repair does not claim a fresh full battery.
The four touched Clojure files match `final-source-sha256.txt`, captured before the
final gate, and all four were formatted and linted clean. The final focused
namespace split run is 28 tests / 275 assertions. Gate log: `make-test-final.log`.

All raw receipts, red/green logs, output hashes, independent oracles, lint findings,
source diffs, requests and inverse evidence are under
`/var/tmp/forge/plan2/build/papercuts2-evidence/`. The final fixture is retained at
`/var/tmp/forge/plan2/build/cc-papercuts2-final` for inspection. Work lands only on
`astra/namespace-split`, with forge-anvil identity and Gene's co-author trailer;
no push or main change.
