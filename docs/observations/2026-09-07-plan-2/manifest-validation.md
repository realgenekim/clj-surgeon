# manifest-validation.md

Validator: /var/tmp/forge/plan2/split/manifest-work/validate.py
Manifest:  /var/tmp/forge/plan2/split/manifest.edn
Pre-split source sha: d9205abcc9b9f31d93bd4a76537b74fccd583ca6
Historical split commit: c4299615
Run at: 2026-09-07T22:17:10Z

## Check 0 -- manifest.edn is read by a real Clojure EDN reader (babashka)
  PASS  clojure.edn/read-string accepts manifest.edn
  PASS  bb reader agrees: 20 destinations, 141 forms, 141 distinct, 3 dispositions, 5 declares, 8 promotions :: got: 20 141 141 3 5 8

## Check 1 -- paren-aware parse of views.clj vs the manifest union
  PASS  source defs are distinct (no duplicate top-level def name) :: n=141
  PASS  source has exactly 141 top-level defs :: got 141
  PASS  the only non-def/non-declare top-level form is the ns form :: ['ns']
  PASS  every manifest form appears in exactly ONE destination :: []
  PASS  no source def is unmapped :: []
  PASS  no manifest form is absent from the source :: []
  PASS  SET EQUALITY source defs == manifest union :: |src|=141 |union|=141
  PASS  20 destinations :: got 20
  PASS  counts {:defs 141 :mapped 141 :unmapped 0} :: {'defs': 141, 'mapped': 141, 'unmapped': 0, 'destinations': 20, 'declare-forms': 3, 'declared-symbols': 5, 'promotions': 8}

## Check 2 -- every destination form exists BY NAME in c4299615:<dest>
  PASS  all forms of auth.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of avatar.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of committee.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of communications.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of dashboard.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of event_setup.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  NOTE  form_builder.clj: 1 form(s) not defined in the historical file: ['fb-tags']
  PASS  all forms of form_builder.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of form_controls.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of format.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of integrations.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of live_drafts.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of log.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of organizer_layout.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of people.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of portal.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of public_cfp.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of replay.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  NOTE  review.clj: 1 form(s) not defined in the historical file: ['submissions-page']
  PASS  all forms of review.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of schedule.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  all forms of shell.clj are in the historical file OR carry a recorded disposition :: absent-and-undisposed=[]
  PASS  historical destinations introduce no name absent from the source (no renames) :: []
  INFO  historical destinations define 139 of the 141 source names; 2 are recorded as dispositions.
  INFO  names the historical commit dropped: ['fb-tags', 'submissions-page']
  PASS  manifest placement matches the historical placement for all 139 historical names :: []

## Check 3 -- clj-kondo var-usage join: the private defs referenced across destinations
  PASS  kondo join yields exactly 8 promotions :: got 8: ['answer-input', 'datastar-script', 'field-error', 'field-errors', 'header', 'initials', 'not-blank', 'req-mark']
  PASS  promotion caller lists are correctly partitioned (cross-destination vs same-destination) :: []
  PASS  manifest :promotions-expected == kondo join :: manifest=['answer-input', 'datastar-script', 'field-error', 'field-errors', 'header', 'initials', 'not-blank', 'req-mark'] computed=['answer-input', 'datastar-script', 'field-error', 'field-errors', 'header', 'initials', 'not-blank', 'req-mark']
  PASS  ORACLE A: names c4299615 de-privatised == the 8 computed promotions :: historical=['answer-input', 'datastar-script', 'field-error', 'field-errors', 'header', 'initials', 'not-blank', 'req-mark']
  PASS  no source-public name became private in the historical split :: []
  PASS  ORACLE B: N1 mapping (141 names) identical to the manifest :: []
  PASS  ORACLE C: N2 mapping (141 names) identical to the manifest :: []
  PASS  ORACLE C2: N2 promotion set (its own regex parse + its own usage graph) == the 8 :: ['answer-input', 'datastar-script', 'field-error', 'field-errors', 'header', 'initials', 'not-blank', 'req-mark']

## Check 4 -- declares
  PASS  manifest :declares == the symbols in views.clj declare forms :: source=['cfp-note', 'datastar-script', 'portal-draft-status', 'row-controls*', 'time-travel-bar'] manifest=['cfp-note', 'datastar-script', 'portal-draft-status', 'row-controls*', 'time-travel-bar']
  PASS  3 declare FORMS (5 declared symbols) :: [210, 1304, 3059]
  PASS  declare time-travel-bar destination agrees with its def placement :: cfp-scheduler-killer.views.organizer-layout vs cfp-scheduler-killer.views.organizer-layout
  PASS  declare row-controls* destination agrees with its def placement :: cfp-scheduler-killer.views.review vs cfp-scheduler-killer.views.review
  PASS  declare datastar-script destination agrees with its def placement :: cfp-scheduler-killer.views.shell vs cfp-scheduler-killer.views.shell
  PASS  declare cfp-note destination agrees with its def placement :: cfp-scheduler-killer.views.live-drafts vs cfp-scheduler-killer.views.live-drafts
  PASS  declare portal-draft-status destination agrees with its def placement :: cfp-scheduler-killer.views.live-drafts vs cfp-scheduler-killer.views.live-drafts
  PASS  manifest form ORDER is topologically clean inside every destination (no declare needed after the split) :: []
  PASS  declare time-travel-bar :forward-reference-within-destination == recomputed :: manifest=False recomputed=False callers=['dev-strip', 'log-page']
  PASS  declare row-controls* :forward-reference-within-destination == recomputed :: manifest=False recomputed=False callers=['row-controls']
  PASS  declare datastar-script :forward-reference-within-destination == recomputed :: manifest=False recomputed=False callers=['cfp-page', 'portal-page']
  PASS  declare cfp-note :forward-reference-within-destination == recomputed :: manifest=False recomputed=False callers=['cfp-about-you', 'cfp-page', 'edit-form', 'profile-form']
  PASS  declare portal-draft-status :forward-reference-within-destination == recomputed :: manifest=False recomputed=False callers=['edit-form', 'profile-form']

## Result

**ALL CHECKS PASS.**

## Appendix A -- the 12 `(def ^:private ...)` names the earlier plan.md lost as `^:private` placeholders

plan.md printed the literal token `^:private` for these because its parser took the token after `def`.
They are recovered here from the historical destination files' actual contents (and cross-checked against
a paren-aware parse of views.clj). Destination counts match plan.md's placeholder counts exactly:
avatar 1, event-setup 5, format 4, public-cfp 2 = 12.

| # | symbol | views.clj line | destination |
|---|--------|----------------|-------------|
| 1 | `date-fmt` | 60 | `cfp-scheduler-killer.views.format` |
| 2 | `datetime-fmt` | 61 | `cfp-scheduler-killer.views.format` |
| 3 | `when-fmt` | 111 | `cfp-scheduler-killer.views.format` |
| 4 | `iso-date-fmt` | 149 | `cfp-scheduler-killer.views.format` |
| 5 | `default-start-date` | 589 | `cfp-scheduler-killer.views.event-setup` |
| 6 | `default-end-date` | 590 | `cfp-scheduler-killer.views.event-setup` |
| 7 | `example-name` | 591 | `cfp-scheduler-killer.views.event-setup` |
| 8 | `example-location` | 592 | `cfp-scheduler-killer.views.event-setup` |
| 9 | `example-website` | 593 | `cfp-scheduler-killer.views.event-setup` |
| 10 | `face-pool-size` | 878 | `cfp-scheduler-killer.views.avatar` |
| 11 | `speaker-inputs` | 3862 | `cfp-scheduler-killer.views.public-cfp` |
| 12 | `md-token` | 3901 | `cfp-scheduler-killer.views.public-cfp` |

## Appendix B -- the 8 promotions (private in views.clj, referenced from another destination)

Derived by joining clj-kondo `:var-usages` (intra-namespace edges, `from-var` -> `name`) with the
destination mapping. Independently confirmed three ways: (A) commit c4299615 removed `^:private` /
`defn-` from exactly these 8 and no others; (B) N1's plan; (C) N2's own regex parse + usage graph.

| symbol | form | destination (home) | referencing destinations | referencing vars |
|--------|------|--------------------|--------------------------|------------------|
| `answer-input` | `defn-` | `cfp-scheduler-killer.views.form-controls` | `form-builder`, `portal`, `public-cfp` | `cfp-page`, `edit-form`, `form-grid-region`, `form-preview-region` |
| `datastar-script` | `defn-` | `cfp-scheduler-killer.views.shell` | `portal`, `public-cfp` | `cfp-page`, `portal-page` |
| `field-error` | `defn-` | `cfp-scheduler-killer.views.form-controls` | `portal`, `public-cfp`, `review` | `capture-page`, `cfp-about-you`, `profile-form`, `speaker-input` |
| `field-errors` | `defn-` | `cfp-scheduler-killer.views.form-controls` | `committee`, `event-setup`, `form-builder` | `committee-card`, `field-form-fields`, `new-event-page` |
| `header` | `defn-` | `cfp-scheduler-killer.views.organizer-layout` | `committee`, `communications`, `dashboard`, `event-setup`, `form-builder`, `integrations`, `log`, `people`, `replay`, `review`, `schedule` | `board-page`, `capture-page`, `committee-page`, `comms-page`, `event-dashboard-page`, `event-details-page`, `events-list-page`, `exports-page`, `form-builder-page`, `inform-page`, `log-page`, `person-page`, `replay-page`, `schedule-page`, `settings-page`, `submission-detail-page`, `submissions-page` |
| `initials` | `defn-` | `cfp-scheduler-killer.views.avatar` | `review` | `submission-detail-page` |
| `not-blank` | `defn-` | `cfp-scheduler-killer.views.format` | `committee`, `dashboard`, `event-setup`, `integrations`, `people`, `public-cfp`, `review` | `board-qs`, `board-region`, `board-row`, `cfp-page`, `committee-card`, `dash-feed-talk`, `event-marquee`, `new-event-page`, `person-page`, `profile-links`, `settings-page`, `sort-chip`, `status-chip`, `submission-detail-page`, `track-chip` |
| `req-mark` | `defn-` | `cfp-scheduler-killer.views.form-controls` | `event-setup`, `form-builder`, `public-cfp` | `cfp-about-you`, `event-details-page`, `field-form-fields`, `field-row`, `speaker-input` |

The other 59 of the 67 private defs are referenced only inside their own destination and stay private.

## Appendix C -- declares

views.clj carries **3 `declare` forms naming 5 symbols**. All five exist only to satisfy the
single-file top-to-bottom load order; under the manifest's per-destination ordering none of them is a
forward reference inside its own destination, so **no destination file needs a `declare`** -- which is
what the historical split did too (zero `declare` forms in all 20 files at c4299615).

| declare line | symbol | destination | callers | caller destinations | forward ref within destination |
|---|---|---|---|---|---|
| 210 | `time-travel-bar` | `cfp-scheduler-killer.views.organizer-layout` | `dev-strip`, `log-page` | `log`, `organizer-layout` | **false** |
| 1304 | `row-controls*` | `cfp-scheduler-killer.views.review` | `row-controls` | `review` | **false** |
| 3059 | `datastar-script` | `cfp-scheduler-killer.views.shell` | `cfp-page`, `portal-page` | `portal`, `public-cfp` | **false** |
| 3059 | `cfp-note` | `cfp-scheduler-killer.views.live-drafts` | `cfp-about-you`, `cfp-page`, `edit-form`, `profile-form` | `portal`, `public-cfp` | **false** |
| 3059 | `portal-draft-status` | `cfp-scheduler-killer.views.live-drafts` | `edit-form`, `profile-form` | `portal` | **false** |

Note `row-controls*` (declared at views.clj:1304, defined at 1312) and `row-controls` (defined at 1306) are
two DISTINCT private vars, both in `cfp-scheduler-killer.views.review`. The earlier plan.md listed
`row-controls` twice, losing the trailing `*`.

## Appendix D -- dispositions (recorded once, here and in manifest.edn)

### `fb-tags` -> `cfp-scheduler-killer.views.form-builder`

Dropped by the historical split, not renamed: `(defn- fb-tags [f] ...)` at views.clj:3409 is dead code at d9205abc -- `git grep fb-tags d9205abc -- src test` returns only that definition line, and `git grep fb-tags c4299615 -- src test` returns nothing at all. This manifest preserves all 141 defs, so fb-tags goes to form-builder: it is defined inside the form-builder region between type-label and fb-post, and calls forms/locked? and forms/retired? like the rest of that namespace. It stays ^:private (no cross-destination caller).

### `submissions-page` -> `cfp-scheduler-killer.views.review`

Dropped by the historical split, not renamed: `(defn submissions-page ...)` at views.clj:997 is dead code at d9205abc -- `git grep submissions-page d9205abc -- src test` returns only that definition line, and `git grep submissions-page c4299615 -- src test` returns nothing. This manifest preserves all 141 defs, so submissions-page goes to review: it is the section '3b. Submissions list' that feeds the review board, it links to /capture and /board, and its only view calls are organizer-shell + header (organizer-layout) and fmt-date (format), both of which review already requires. It stays public (it was public in views.clj).

### `row-controls*` -> `cfp-scheduler-killer.views.review`

NOT a missing name -- a name the earlier derived plan lost. views.clj defines BOTH `row-controls` (line 1466, ^:private) and `row-controls*` (line 1435, ^:private); the earlier plan.md printed 'row-controls' twice because its parser dropped the trailing `*`. Both are distinct vars, both live in cfp-scheduler-killer.views.review in the historical file, and `row-controls*` is also one of the three declare targets (views.clj:1304). Recorded here so the doubling cannot recur.

## Appendix E -- doubts and caveats

- **The manifest is a superset of history by design.** c4299615 emitted 139 of the 141 defs; this manifest maps all 141. Anyone diffing a produced split against the historical files will see `fb-tags` and `submissions-page` as extra. That is intended (Astra's brief says map 141, and dropping code is not a split), but it means the historical files are not a byte-for-byte oracle -- only a name-and-placement oracle.
- **`submissions-page`'s destination is a judgement call, not a fact.** It is dead code, so no reference graph constrains it. `review` is chosen because the source section header reads `;; --- 3b. Submissions list ---` immediately before the review board and its only view calls (`organizer-shell`, `header`, `fmt-date`) are already required by review. `dashboard` would also compile. If a produced split puts it elsewhere and everything else matches, that is a placement disagreement, not a defect.
- **`submissions-page` is public in views.clj and stays public here.** Since it has no callers, its privacy is unconstrained; keeping it public preserves the source text verbatim, but a `defn-` would also be defensible and would avoid an unused-public-var lint.
- **Promotion detection is call-graph based and would miss a dynamic reference.** clj-kondo sees `(resolve 'views/foo)`, `(var views/foo)` spelled through a string, or a symbol built at runtime as nothing. I found none in views.clj, and the historical de-privatisation set agrees exactly, so the risk is closed for THIS file -- but the method, reused elsewhere, has that hole.
- **Privates are also referenced from OUTSIDE views.clj in principle.** This analysis only linted views.clj. Callers (`server.clj`, the four test namespaces) cannot legally reference a private var, so a same-file analysis is sufficient for privacy; it is NOT sufficient for the require/alias rewrite, which N1's `callers.py` handles separately and is out of this manifest's scope.
- **The `:forward-reference-within-destination` field is computed against the manifest's own form order** (which is the historical file order, with the two disposed names appended last). Reorder a destination's forms and the answer can change; the validator recomputes it rather than trusting the field.
- **Astra's brief says 'three missing-name dispositions'; only two names are actually missing** (`fb-tags`, `submissions-page`). The third recorded disposition is `row-controls*` -- the name plan.md lost by doubling `row-controls` rather than by the historical commit dropping it. It is recorded so the doubling cannot recur; if Astra meant a different third name, nothing in the source supports one: 141 = 139 historical + 2.
- **`face-pool-size`, `speaker-inputs`, `md-token` were never at risk but sit in the same class as the 12** -- they are `def ^:private` / `defn-` names plan.md either dropped or mis-rendered. All three are mapped and validated here (`face-pool-size` -> avatar, `speaker-inputs` and `md-token` -> public-cfp).
- **clj-kondo exits 2 on views.clj** (lint findings, not an analysis failure). The `:analysis` payload is complete: 146 var-definitions = 141 defs + 5 declares, matching the paren-aware parse exactly.
- **Reproduce:** `cd /var/tmp/forge/plan2/split/manifest-work && python3 validate.py` (exit 0 = all checks pass); appendices via `python3 appendices.py`.

