# Item 3: the review consumes independently checked preservation evidence — bounded replay prototype

**Astra's order, table row 3** (`/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-astra-squares.md`):
*"Give the reviewer an independently checked account of which parts of a large change are mechanically
preserved, and an explicit list of the semantic decisions still requiring review… Build a bounded replay
prototype in 4–8 hours."* Her forecast: 120–240 s less review on an otherwise ~573 s move-heavy review,
0–30 s of projection/validation overhead. Her falsifier: blind reviews of retained clean changes plus
planted binding, promotion, comment, omitted-owner and load-order defects.

## 1. Versus native: nothing measured here, and that is the honest headline

**No native-versus-Surgeon wall was measured in this work, and none is claimed.** This prototype is not a
Surgeon capability. `bin/preservation-brief` takes two git shas and reads two git trees; it does not know or
care whether the candidate was produced by Surgeon, by `apply_patch`, or by hand. Astra's own note applies
unchanged: *"Incremental Surgeon advantage is unknown because native gets the same consumer."*

What *was* measured is the checker's own wall — the overhead half of her forecast — and the size of the
review obligation it removes and the size of what it leaves behind.

| specimen | checker wall | bodies mechanically preserved | obligations left for a reviewer |
|---|---|---|---|
| Cell C views split, real and landed (curtaincall-cfp `d9205abc` → `65ad613b`; 7,769 insertions / 4,863 deletions, 29 files) | **20.0 s** | **141/141 relocated + 64/64 caller** | 20 destination boundaries · 8 promotions · 11 lost comments · 1 unconfirmed bare reference · 7 new owners · 21 created namespaces · 9 macro-context owners · 2 non-source files |
| E3 Cell B split, real (`exports` → `exports.calendar`, from the retained candidate patch) | **19.8 s** | **25/25 relocated + 20/20 caller** | 1 destination boundary · 3 promotions · 0 lost comments |
| Alias migration, constructed (35 files repointed) | **33.2 s** | **100/103 caller bodies** | 3 bodies: two string literals and one comment my constructor also rewrote |
| clj-surgeon `571170cc` comment-edits landing, real | **10.1 s** | **0** | all 9 changed bodies · 3 lost comments · 12 non-source files |
| clj-surgeon `d2c3aa80` batch-5 landing, real | **14.9 s** | **0** | all 22 changed bodies · 5 lost comments · 13 non-source files |

Overhead sits inside Astra's 0–30 s budget on the two splits and 3 s outside it on the widest alias
migration. Against her ~573 s move-heavy review, the brief removes the reading of 205 relocated and caller
bodies and replaces it with eight named lists.

## 2. Top win

**The load-bearing insight is that byte-identity is nearly worthless on a split, and the fix is cheap.**

The first build scored Cell C at 96/141 bodies preserved. The reason is structural, not a bug: a split
*must* rewrite intra-namespace references, `(organizer-shell …)` becomes
`(organizer-layout/organizer-shell …)`, the head widens, and the formatter reindents the entire form. A
reviewer looking at that diff sees the whole body change and has to read all of it.

Comparing a **token stream in which every reference is canonicalised to the owner it named in the base
tree** turns 45 of those into a proven identity. Cell C went from 96/141 to **141/141** — and the strength
of the check went *up*, not down: the planted defect that requalifies a body to a namespace which does not
own the name still lands in `:changed`, because its canonical token is a different owner. That is the whole
prototype in one sentence: *accept a reference that follows its owner, refuse one that is repointed
anywhere else.*

**Second win: the counts corroborate the frozen manifests without touching a receipt.** Cell C's manifest
says 141 owners, 20 destinations, 87 static sites. The checker, reading only the two trees, re-derives
141, 20 and 87. Cell B's frozen A1/A3 oracle says 25 owners and 43 external references; the checker
re-derives 25 and 43. Those numbers were never read from a receipt — section A of the brief has no receipt
input at all.

**Third win: the negative controls refuse to help.** On the two real clj-surgeon feature landings the brief
preserves nothing and hands the reviewer 100% of the changed bodies. A tool that shrinks the reviewer's
burden on a behavioural change would be worse than useless; this one declines.

## 3. Top losses

**A `:require` reorder among requires the candidate ADDED escaped the first build entirely.** The order
check compared the subsequence of libs common to both trees — and a require the candidate added has no base
position, so swapping two of them was invisible. Found only because the replay planted it. Repaired by a
second, independent test: if the base file kept its requires sorted, the candidate must too. Both checks now
ship, and the planted swap is caught.

**A `defn-` → `defn` promotion made the matcher report the owner twice — once as omitted, once as new.**
Cell C read as 133 moved + 8 omissions + 8 phantom new owners. A checker whose *failure* signal fires on a
correct change is a checker nobody will read. Owners now match on the def family and the privacy change is
reported once, in section B, where a promotion belongs.

**Two of Astra's five defect classes are only caught in section B, not section A.** A silent promotion and a
load-order change are, mechanically, not body damage; the brief surfaces them as decisions the reviewer must
make. If the reviewer skips section B the defect ships. The falsifier below has to test that, which is why
its co-primary outcome is per-class catch rate and not aggregate defect count.

**The brief is static. It proves nothing about behaviour**, and section D says so on every run, at length,
including the specific things a namespace move can break that no text comparison sees: macro expansion,
dynamic resolution, generated class packages, load order as executed, and namespaces named as strings in
resources.

## 4. What was built

Three files under `bin/`, on branch `fable/proof-burden`, no edits to `src/`, `test/` or the Makefile.

- **`bin/preservation-brief <candidate-sha> <base-sha>`** — the checker. Sections **A** mechanically
  preserved, **B** decisions that remain, **C** disagreement with a producer receipt, **D** what it did not
  check. Reads only the two git trees, via `git archive`.
- **`bin/preservation_scan.clj`** — an independent Clojure source scanner: byte-exact top-level form spans,
  comment spans, `ns`-form parsing, and a token stream. It deliberately shares no code with clj-surgeon's
  `src/`, so its evidence cannot inherit a defect from the transform it is checking.
- **`bin/preservation-replay`** — builds a scratch repository holding the landed Cell C split plus six
  planted defects and runs the brief over every one.

What section A establishes, all of it re-derived:

| check | what it decides |
|---|---|
| A1 owner accounting | every base owner has exactly one candidate home, or it is named as omitted; every candidate owner has a base origin, or it is named as new |
| A2 body identity | four tiers per relocated owner: byte-identical · modulo whitespace · **modulo requalification** · code identical but comments differ · changed (with the diff) |
| A3 exactly once | no owner duplicated beyond its base multiplicity; no relocated owner left behind in its source file |
| A4 namespace-edit inventory | requires added, removed, reordered, alias-changed; imports; namespaces created and deleted — by diffing the `ns` forms |
| A5 static call sites | every site resolved through its own file's aliases, in its own tree: rewritten, stale, undefined-prefix, retargeted |
| A6 comment inventory | comment texts lost and added across the touched files — the comments *between* forms, which no body comparison can see |

What section B refuses to decide: destination boundaries · promotions · references the checker could not
confirm · load order (require order, require sort discipline, and new intra-file forward references) · macro
context and generated classes · prose edits · everything unclassified.

## 5. The tool's output on one real change, verbatim

Command:

```
bin/preservation-brief 65ad613b d9205abc \
  --repo /home/forge/src/curtaincall-cfp \
  --title "Cell C views split (curtaincall-cfp d9205abc -> 65ad613b)" \
  --receipt /var/tmp/forge/rows-sublime-5/cellC-after-receipt.edn
```

---

## Preservation brief — Cell C views split (curtaincall-cfp d9205abc -> 65ad613b)

- repository: `/home/forge/src/curtaincall-cfp`
- base: `d9205abcc9b9f31d93bd4a76537b74fccd583ca6`
- candidate: `65ad613baeb8d6ecc2676eafc6cbb3953f0f24eb`
- roots scanned: `src,test,dev,bin`
- Clojure source files parsed: 39 of 83 Clojure source files (every changed file, plus every file that textually mentions an owner defined in a changed file)
- change class, as re-derived by this checker: **namespace-split (owners relocated into new or out of deleted files)**
- evidence source: **the two git trees, read by this checker.** No receipt field was used in section A.

### A. Mechanically preserved (independently re-derived)

**A1 — owner accounting.**

| quantity | count |
|---|---|
| owners in the base tree, within the parsed scope | 678 |
| owners in the candidate tree, within the parsed scope | 685 |
| owners that stayed in their namespace | 537 |
| owners relocated to another namespace | 141 |
| owners in the base with NO candidate home (omitted) | 0 |
| owners in the candidate with no base origin (new) | 7 |
| owners whose match is ambiguous | 0 |


**Owners with no base origin — new code, not a relocation, and a full review obligation:**

| owner | kind | candidate namespace | file:line |
|---|---|---|---|
| view-prefix | def | cfp-scheduler-killer.view-architecture-test | test/cfp_scheduler_killer/view_architecture_test.clj:11 |
| expected-view-namespaces | def | cfp-scheduler-killer.view-architecture-test | test/cfp_scheduler_killer/view_architecture_test.clj:13 |
| foundation-namespaces | def | cfp-scheduler-killer.view-architecture-test | test/cfp_scheduler_killer/view_architecture_test.clj:20 |
| public-namespaces | def | cfp-scheduler-killer.view-architecture-test | test/cfp_scheduler_killer/view_architecture_test.clj:25 |
| clj-kondo-analysis | defn- | cfp-scheduler-killer.view-architecture-test | test/cfp_scheduler_killer/view_architecture_test.clj:28 |
| acyclic? | defn- | cfp-scheduler-killer.view-architecture-test | test/cfp_scheduler_killer/view_architecture_test.clj:39 |
| view-namespace-architecture-test | deftest | cfp-scheduler-killer.view-architecture-test | test/cfp_scheduler_killer/view_architecture_test.clj:51 |


**A2 — body identity of relocated owners.** Each tier is computed by comparing the exact bytes of the
top-level form in the base tree with the bytes of its candidate home.

| tier | owners | meaning |
|---|---|---|
| `:byte-identical` | 83 | identical bytes — nothing in the body to review |
| `:identical-modulo-whitespace` | 0 | identical after trailing-space and common-indent normalisation |
| `:identical-modulo-requalification` | 58 | identical once every reference is rewritten to the owner it named in the BASE tree — i.e. the only change is that references follow their owners, and each one still resolves to the same owner |
| `:code-identical-comments-differ` | 0 | code identical, COMMENT TEXT CHANGED — a prose review obligation |
| `:changed` | 0 | the body text differs — a FULL review obligation; diffs below |



The def head's privacy suffix (`defn-` vs `defn`) is normalised before comparison, so a promotion is never scored as a body change; every privacy change is reported in **B2** instead. Privacy changes in this candidate: **8**.

Mechanically preserved bodies (tiers 1–3): **141 of 141**.

Relocated owners that are not byte-identical:

| owner | tier | from | to |
|---|---|---|---|
| organizer-shell | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.organizer-layout |
| header | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.organizer-layout |
| not-blank | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.format |
| field-errors | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls |
| events-list-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| event-marquee | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| new-event-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| initials | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.avatar |
| member-row | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.committee |
| committee-card | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.committee |
| committee-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.committee |
| submissions-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| req-mark | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls |
| field-error | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls |
| answer-input | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls |
| board-row | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| sort-chip | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| status-chip | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| board-qs | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| track-chip | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| board-region | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| board-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| log-summary | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
| log-region | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
| log-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
| submission-detail-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| exports-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| api-docs-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| settings-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| schedule-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.schedule |
| agenda-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.schedule |
| inform-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.communications |
| capture-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| replay-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.replay |
| comms-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.communications |
| portal-submission | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| edit-form | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| profile-form | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| portal-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| landing-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.auth |

_18 further rows suppressed by --max-list 40._


**Owners that did NOT move, whose body text changed.** The same tiers apply: a caller whose only
change is that its references now follow the owners they name is mechanically preserved too.

| tier | owners |
|---|---|
| `identical-modulo-requalification` | 64 |


Of 64 in-place body changes, **64** are mechanically preserved and **0** need a reviewer.

| owner | namespace | tier |
|---|---|---|
| opinions-stars-ride-first-comment-only-test | cfp-scheduler-killer.views-test | `identical-modulo-requalification` |
| opinions-silent-raters-stay-named-test | cfp-scheduler-killer.views-test | `identical-modulo-requalification` |
| histogram-buckets-and-hover-test | cfp-scheduler-killer.views-test | `identical-modulo-requalification` |
| fmt-when-test | cfp-scheduler-killer.polish-test | `identical-modulo-requalification` |
| scrub-slider-wiring-test | cfp-scheduler-killer.polish-test | `identical-modulo-requalification` |
| handle-home | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-events-list | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-new-event | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-create-event | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| not-found-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| render-event-dashboard | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| render-committee-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-event-details | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-event-details-save | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-events-preview | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-sse-state | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-exports-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-login-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-login | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-auth-token | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-demo-login | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| board-fragment-html | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| dashboard-fragment-html | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| log-fragment-html | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-board | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-submission-detail | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| detail-page-response | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| push-board-updates! | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| push-notice! | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| reject-value! | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| render-portal | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-comms | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-capture-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-capture | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-inform-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-api-docs | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| settings-response | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| push-form-updates! | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-form-builder | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| with-form | cfp-scheduler-killer.server | `identical-modulo-requalification` |

_24 further rows suppressed by --max-list 40._


**A3 — exactly once.**

| check | result |
|---|---|
| owner ids duplicated in the candidate beyond their base multiplicity | pass — 0 |
| relocated owners still present in their source file | pass — 0 |


**A4 — namespace-edit inventory, re-derived by diffing the `ns` forms.**

| quantity | count |
|---|---|
| files whose `ns` form changed | 5 |
| namespaces created | 21 |
| namespaces deleted | 1 |
| files with a `:require` ORDER change | 0 |


| file | requires added | requires removed | order changed | alias changed | imports +/- |
|---|---|---|---|---|---|
| src/cfp_scheduler_killer/server.clj | cfp-scheduler-killer.views.auth cfp-scheduler-killer.views.committee cfp-scheduler-killer.views.communications cfp-scheduler-killer.views.dashboard cfp-scheduler-killer.views.event-setup cfp-scheduler-killer.views.form-builder cfp-scheduler-killer.views.format cfp-scheduler-killer.views.integrations cfp-scheduler-killer.views.live-drafts cfp-scheduler-killer.views.log cfp-scheduler-killer.views.people cfp-scheduler-killer.views.portal cfp-scheduler-killer.views.public-cfp cfp-scheduler-killer.views.replay cfp-scheduler-killer.views.review cfp-scheduler-killer.views.schedule cfp-scheduler-killer.views.shell | cfp-scheduler-killer.views | no | — | 0/0 |
| test/cfp_scheduler_killer/comms_test.clj | cfp-scheduler-killer.views.log | — | no | — | 0/0 |
| test/cfp_scheduler_killer/forms_test.clj | cfp-scheduler-killer.views.form-builder cfp-scheduler-killer.views.review | cfp-scheduler-killer.views | no | — | 0/0 |
| test/cfp_scheduler_killer/polish_test.clj | cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout | cfp-scheduler-killer.views | no | — | 0/0 |
| test/cfp_scheduler_killer/views_test.clj | cfp-scheduler-killer.views.review | cfp-scheduler-killer.views | no | — | 0/0 |


Namespaces created by this candidate:

| file | namespace | requires |
|---|---|---|
| src/cfp_scheduler_killer/views/auth.clj | cfp-scheduler-killer.views.auth | cfp-scheduler-killer.views.shell |
| src/cfp_scheduler_killer/views/avatar.clj | cfp-scheduler-killer.views.avatar | clojure.string |
| src/cfp_scheduler_killer/views/committee.clj | cfp-scheduler-killer.views.committee | cfp-scheduler-killer.committees cfp-scheduler-killer.views.avatar cfp-scheduler-killer.views.form-controls cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout |
| src/cfp_scheduler_killer/views/communications.clj | cfp-scheduler-killer.views.communications | cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout |
| src/cfp_scheduler_killer/views/dashboard.clj | cfp-scheduler-killer.views.dashboard | cfp-scheduler-killer.views.avatar cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout cfp-scheduler-killer.views.review datastar-kit.ds |
| src/cfp_scheduler_killer/views/event_setup.clj | cfp-scheduler-killer.views.event-setup | cfp-scheduler-killer.events cfp-scheduler-killer.views.form-controls cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout clojure.string datastar-kit.ds |
| src/cfp_scheduler_killer/views/form_builder.clj | cfp-scheduler-killer.views.form-builder | cfp-scheduler-killer.forms cfp-scheduler-killer.submissions cfp-scheduler-killer.views.form-controls cfp-scheduler-killer.views.organizer-layout clojure.string datastar-kit.ds |
| src/cfp_scheduler_killer/views/form_controls.clj | cfp-scheduler-killer.views.form-controls | clojure.string |
| src/cfp_scheduler_killer/views/format.clj | cfp-scheduler-killer.views.format | cfp-scheduler-killer.events clojure.string |
| src/cfp_scheduler_killer/views/integrations.clj | cfp-scheduler-killer.views.integrations | cfp-scheduler-killer.events cfp-scheduler-killer.exports cfp-scheduler-killer.submissions cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout cfp-scheduler-killer.views.review cfp-scheduler-killer.views.shell clojure.string |
| src/cfp_scheduler_killer/views/live_drafts.clj | cfp-scheduler-killer.views.live-drafts |  |
| src/cfp_scheduler_killer/views/log.clj | cfp-scheduler-killer.views.log | cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout cfp-scheduler-killer.views.review clojure.string |
| src/cfp_scheduler_killer/views/organizer_layout.clj | cfp-scheduler-killer.views.organizer-layout | cfp-scheduler-killer.committees cfp-scheduler-killer.events cfp-scheduler-killer.forms cfp-scheduler-killer.submissions cfp-scheduler-killer.views.shell clojure.string datastar-kit.ds hiccup.page hiccup2.core |
| src/cfp_scheduler_killer/views/people.clj | cfp-scheduler-killer.views.people | cfp-scheduler-killer.views.avatar cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/views/portal.clj | cfp-scheduler-killer.views.portal | cfp-scheduler-killer.portal cfp-scheduler-killer.submissions cfp-scheduler-killer.views.form-controls cfp-scheduler-killer.views.format cfp-scheduler-killer.views.live-drafts cfp-scheduler-killer.views.shell clojure.string datastar-kit.ds |
| src/cfp_scheduler_killer/views/public_cfp.clj | cfp-scheduler-killer.views.public-cfp | cfp-scheduler-killer.events cfp-scheduler-killer.submissions cfp-scheduler-killer.views.form-controls cfp-scheduler-killer.views.format cfp-scheduler-killer.views.live-drafts cfp-scheduler-killer.views.shell clojure.string datastar-kit.ds hiccup2.core |
| src/cfp_scheduler_killer/views/replay.clj | cfp-scheduler-killer.views.replay | cfp-scheduler-killer.views.organizer-layout datastar-kit.ds |
| src/cfp_scheduler_killer/views/review.clj | cfp-scheduler-killer.views.review | cfp-scheduler-killer.committees cfp-scheduler-killer.events cfp-scheduler-killer.forms cfp-scheduler-killer.reviews cfp-scheduler-killer.submissions cfp-scheduler-killer.views.avatar cfp-scheduler-killer.views.form-controls cfp-scheduler-killer.views.format cfp-scheduler-killer.views.organizer-layout clojure.data.json clojure.string datastar-kit.ds |
| src/cfp_scheduler_killer/views/schedule.clj | cfp-scheduler-killer.views.schedule | cfp-scheduler-killer.events cfp-scheduler-killer.schedule cfp-scheduler-killer.views.organizer-layout cfp-scheduler-killer.views.shell clojure.string datastar-kit.ds |
| src/cfp_scheduler_killer/views/shell.clj | cfp-scheduler-killer.views.shell | hiccup.page hiccup2.core |
| test/cfp_scheduler_killer/view_architecture_test.clj | cfp-scheduler-killer.view-architecture-test | clojure.data.json clojure.java.io clojure.java.shell clojure.set clojure.string clojure.test |


Namespaces deleted by this candidate:

| file | namespace |
|---|---|
| src/cfp_scheduler_killer/views.clj | cfp-scheduler-killer.views |


**A5 — static call sites.** Every site is resolved through the `ns` aliases of the file it lives in,
in the tree it lives in.

| quantity | count |
|---|---|
| base sites naming a relocated owner through its OLD namespace | 87 |
| candidate sites naming a relocated owner through its NEW namespace (rewritten) | 260 |
| candidate sites STILL naming the old namespace (stale unless a facade is retained) | 0 |
| candidate sites whose alias the checker could not resolve | 0 |
| candidate sites naming an alias the file's `ns` form does NOT define (undefined at load) | 0 |
| qualified references in touched files whose resolved namespace changed | 97 |


Rewritten sites:

| file:line | site | resolves to |
|---|---|---|
| src/cfp_scheduler_killer/server.clj:128 | `v-auth/landing-page` | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:136 | `event-setup/events-list-page` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:180 | `event-setup/new-event-page` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:217 | `event-setup/new-event-page` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:239 | `event-setup/new-event-page` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:260 | `shell/page-shell` | cfp-scheduler-killer.views.shell |
| src/cfp_scheduler_killer/server.clj:328 | `dashboard/event-dashboard-page` | cfp-scheduler-killer.views.dashboard |
| src/cfp_scheduler_killer/server.clj:340 | `committee/committee-page` | cfp-scheduler-killer.views.committee |
| src/cfp_scheduler_killer/server.clj:373 | `event-setup/event-details-page` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:398 | `event-setup/event-details-page` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:494 | `event-setup/event-marquee` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:500 | `event-setup/slug-status` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:560 | `event-setup/event-marquee` | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:576 | `integrations/exports-page` | cfp-scheduler-killer.views.integrations |
| src/cfp_scheduler_killer/server.clj:640 | `v-auth/login-page` | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:666 | `v-auth/login-page` | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:700 | `v-auth/login-page` | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:751 | `v-auth/login-page` | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:832 | `review/board-region` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:842 | `dashboard/event-dashboard-region` | cfp-scheduler-killer.views.dashboard |
| src/cfp_scheduler_killer/server.clj:852 | `v-log/log-region` | cfp-scheduler-killer.views.log |
| src/cfp_scheduler_killer/server.clj:927 | `review/board-page` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:938 | `review/submission-detail-page` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:952 | `review/submission-detail-page` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:973 | `review/board-row` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:976 | `review/coverage-bar` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1017 | `review/notice-region` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1041 | `review/board-page` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1213 | `v-portal/portal-page` | cfp-scheduler-killer.views.portal |
| src/cfp_scheduler_killer/server.clj:1282 | `communications/comms-page` | cfp-scheduler-killer.views.communications |
| src/cfp_scheduler_killer/server.clj:1298 | `review/capture-page` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1307 | `review/capture-page` | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1321 | `communications/inform-page` | cfp-scheduler-killer.views.communications |
| src/cfp_scheduler_killer/server.clj:1539 | `integrations/api-docs-page` | cfp-scheduler-killer.views.integrations |
| src/cfp_scheduler_killer/server.clj:1573 | `integrations/settings-page` | cfp-scheduler-killer.views.integrations |
| src/cfp_scheduler_killer/server.clj:1803 | `form-builder/form-grid-region` | cfp-scheduler-killer.views.form-builder |
| src/cfp_scheduler_killer/server.clj:1810 | `form-builder/form-builder-page` | cfp-scheduler-killer.views.form-builder |
| src/cfp_scheduler_killer/server.clj:1828 | `shell/page-shell` | cfp-scheduler-killer.views.shell |
| src/cfp_scheduler_killer/server.clj:1857 | `form-builder/form-builder-page` | cfp-scheduler-killer.views.form-builder |
| src/cfp_scheduler_killer/server.clj:1882 | `form-builder/form-builder-page` | cfp-scheduler-killer.views.form-builder |

_220 further rows suppressed by --max-list 40._


Retargeted references — same name, different namespace after the change:

| file:line | name | from | to |
|---|---|---|---|
| src/cfp_scheduler_killer/server.clj:128 | landing-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:136 | events-list-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:180 | new-event-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:217 | new-event-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:239 | new-event-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:260 | page-shell | cfp-scheduler-killer.views | cfp-scheduler-killer.views.shell |
| src/cfp_scheduler_killer/server.clj:328 | event-dashboard-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.dashboard |
| src/cfp_scheduler_killer/server.clj:340 | committee-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.committee |
| src/cfp_scheduler_killer/server.clj:373 | event-details-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:398 | event-details-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:494 | event-marquee | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:500 | slug-status | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:560 | event-marquee | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| src/cfp_scheduler_killer/server.clj:576 | exports-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| src/cfp_scheduler_killer/server.clj:640 | login-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:666 | login-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:700 | login-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:751 | login-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.auth |
| src/cfp_scheduler_killer/server.clj:759 | committees-for-event | cfp-scheduler-killer.events | cfp-scheduler-killer.store |
| src/cfp_scheduler_killer/server.clj:832 | board-region | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:842 | event-dashboard-region | cfp-scheduler-killer.views | cfp-scheduler-killer.views.dashboard |
| src/cfp_scheduler_killer/server.clj:852 | log-region | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
| src/cfp_scheduler_killer/server.clj:853 | log-for-event | cfp-scheduler-killer.store | cfp-scheduler-killer.events |
| src/cfp_scheduler_killer/server.clj:927 | board-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:938 | submission-detail-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:952 | submission-detail-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:973 | board-row | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:976 | coverage-bar | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1017 | notice-region | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1041 | board-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1213 | portal-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| src/cfp_scheduler_killer/server.clj:1282 | comms-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.communications |
| src/cfp_scheduler_killer/server.clj:1298 | capture-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1307 | capture-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| src/cfp_scheduler_killer/server.clj:1321 | inform-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.communications |
| src/cfp_scheduler_killer/server.clj:1539 | api-docs-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| src/cfp_scheduler_killer/server.clj:1573 | settings-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| src/cfp_scheduler_killer/server.clj:1803 | form-grid-region | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-builder |
| src/cfp_scheduler_killer/server.clj:1810 | form-builder-page | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-builder |
| src/cfp_scheduler_killer/server.clj:1828 | page-shell | cfp-scheduler-killer.views | cfp-scheduler-killer.views.shell |

_57 further rows suppressed by --max-list 40._


**A6 — comment inventory over the touched files.** Comments inside a relocated owner are covered by A2;
this covers the comments between forms, which no body comparison can see.

| quantity | count |
|---|---|
| comment occurrences in the base (touched files) | 715 |
| comment occurrences in the candidate (touched files) | 704 |
| distinct comment texts LOST | 11 |
| distinct comment texts ADDED | 0 |


**Comment texts present in the base and absent from the candidate:**

| n | comment |
|---|---|
| 1 | `;; explains them.` |
| 1 | `;; housekeeping pages sit in a quiet row at the bottom, where they belong.` |
| 1 | `;; below, and used by both surfaces: the portal's talk-edit form and the public` |
| 1 | `;; decide and tell people, run the show. It used to be an alphabet-soup list of` |
| 1 | `;; twelve peers, which said nothing about what to do next — and hid the exports` |
| 1 | `;; submission form must show the same feedback, computed by the same server` |
| 1 | `;; The nav is the LIFECYCLE, in order: open the call, review what arrives,` |
| 1 | `;; The speaker-facing LIVE LANE is defined once, in the Public CFP section` |
| 1 | `;; declared rather than moved, so the definitions stay beside the page that` |
| 1 | `;; entirely, which is how an integrator concluded we didn't have any. The three` |
| 1 | `;; code, or a speaker gets told "looks fine" here and refused there. Forward` |


### B. Decisions that remain for the reviewer

Nothing here is discharged by section A. These are the obligations the checker deliberately does not decide.

**B1 — destination boundaries.** The partition is a design judgement. The checker can say where every
owner went; it cannot say whether that is the right place.

| destination namespace | owners received | file |
|---|---|---|
| cfp-scheduler-killer.views.auth | 2 | src/cfp_scheduler_killer/views/auth.clj |
| cfp-scheduler-killer.views.avatar | 3 | src/cfp_scheduler_killer/views/avatar.clj |
| cfp-scheduler-killer.views.committee | 3 | src/cfp_scheduler_killer/views/committee.clj |
| cfp-scheduler-killer.views.communications | 3 | src/cfp_scheduler_killer/views/communications.clj |
| cfp-scheduler-killer.views.dashboard | 8 | src/cfp_scheduler_killer/views/dashboard.clj |
| cfp-scheduler-killer.views.event-setup | 10 | src/cfp_scheduler_killer/views/event_setup.clj |
| cfp-scheduler-killer.views.form-builder | 11 | src/cfp_scheduler_killer/views/form_builder.clj |
| cfp-scheduler-killer.views.form-controls | 4 | src/cfp_scheduler_killer/views/form_controls.clj |
| cfp-scheduler-killer.views.format | 15 | src/cfp_scheduler_killer/views/format.clj |
| cfp-scheduler-killer.views.integrations | 5 | src/cfp_scheduler_killer/views/integrations.clj |
| cfp-scheduler-killer.views.live-drafts | 3 | src/cfp_scheduler_killer/views/live_drafts.clj |
| cfp-scheduler-killer.views.log | 3 | src/cfp_scheduler_killer/views/log.clj |
| cfp-scheduler-killer.views.organizer-layout | 12 | src/cfp_scheduler_killer/views/organizer_layout.clj |
| cfp-scheduler-killer.views.people | 2 | src/cfp_scheduler_killer/views/people.clj |
| cfp-scheduler-killer.views.portal | 6 | src/cfp_scheduler_killer/views/portal.clj |
| cfp-scheduler-killer.views.public-cfp | 10 | src/cfp_scheduler_killer/views/public_cfp.clj |
| cfp-scheduler-killer.views.replay | 2 | src/cfp_scheduler_killer/views/replay.clj |
| cfp-scheduler-killer.views.review | 25 | src/cfp_scheduler_killer/views/review.clj |
| cfp-scheduler-killer.views.schedule | 10 | src/cfp_scheduler_killer/views/schedule.clj |
| cfp-scheduler-killer.views.shell | 4 | src/cfp_scheduler_killer/views/shell.clj |


**B2 — promotions and privacy changes.**

| owner | kind | from | to | change |
|---|---|---|---|---|
| header | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.organizer-layout | private -> public |
| not-blank | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.format | private -> public |
| field-errors | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls | private -> public |
| initials | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.avatar | private -> public |
| req-mark | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls | private -> public |
| field-error | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls | private -> public |
| answer-input | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls | private -> public |
| datastar-script | defn- | cfp-scheduler-killer.views | cfp-scheduler-killer.views.shell | private -> public |


Private→public relocations widen the public surface whether or not the transform's promotion policy demanded it. Count: **8**.

**B3 — references the checker could not confirm.**

| quantity | count |
|---|---|
| candidate sites with an unresolvable alias | 0 |
| candidate sites naming an alias the `ns` form does not define | 0 |
| bare references inside a relocated body naming an owner that now lives elsewhere | 1 |
| ambiguous owner matches | 0 |
| stale sites still naming the old namespace | 0 |


**Bare references inside relocated bodies that name an owner now in a different namespace.** Each is
either a local binding that shadows the name, a `:refer`, or a broken reference; the checker cannot
tell which without resolution, so each one is a review obligation:

| relocated owner | in file | bare symbol | that owner now lives in |
|---|---|---|---|
| form-builder-page | src/cfp_scheduler_killer/views/form_builder.clj | edit-form | cfp-scheduler-killer.views.portal |


**B4 — load order.**

| quantity | count |
|---|---|
| new require edges between namespaces in this repository | 74 |
| files whose require ORDER changed (`:require` order is load order) | 0 |
| namespaces created — their require order has NO base to compare against | 21 |
| namespaces deleted | 1 |
| NEW intra-file forward references (owner used before it is defined, no `declare`) | 0 |
| files whose `:require` list lost the sorted order the base kept | 0 |


| from namespace | now requires |
|---|---|
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.auth |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.committee |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.communications |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.dashboard |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.event-setup |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.form-builder |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.format |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.integrations |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.live-drafts |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.log |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.people |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.portal |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.public-cfp |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.replay |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.review |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.schedule |
| cfp-scheduler-killer.server | cfp-scheduler-killer.views.shell |
| cfp-scheduler-killer.comms-test | cfp-scheduler-killer.views.log |
| cfp-scheduler-killer.forms-test | cfp-scheduler-killer.views.form-builder |
| cfp-scheduler-killer.forms-test | cfp-scheduler-killer.views.review |
| cfp-scheduler-killer.polish-test | cfp-scheduler-killer.views.format |
| cfp-scheduler-killer.polish-test | cfp-scheduler-killer.views.organizer-layout |
| cfp-scheduler-killer.views-test | cfp-scheduler-killer.views.review |
| cfp-scheduler-killer.views.auth | cfp-scheduler-killer.views.shell |
| cfp-scheduler-killer.views.committee | cfp-scheduler-killer.views.avatar |
| cfp-scheduler-killer.views.committee | cfp-scheduler-killer.views.form-controls |
| cfp-scheduler-killer.views.committee | cfp-scheduler-killer.views.format |
| cfp-scheduler-killer.views.committee | cfp-scheduler-killer.views.organizer-layout |
| cfp-scheduler-killer.views.communications | cfp-scheduler-killer.views.format |
| cfp-scheduler-killer.views.communications | cfp-scheduler-killer.views.organizer-layout |
| cfp-scheduler-killer.views.dashboard | cfp-scheduler-killer.views.avatar |
| cfp-scheduler-killer.views.dashboard | cfp-scheduler-killer.views.format |
| cfp-scheduler-killer.views.dashboard | cfp-scheduler-killer.views.organizer-layout |
| cfp-scheduler-killer.views.dashboard | cfp-scheduler-killer.views.review |
| cfp-scheduler-killer.views.event-setup | cfp-scheduler-killer.events |
| cfp-scheduler-killer.views.event-setup | cfp-scheduler-killer.views.form-controls |
| cfp-scheduler-killer.views.event-setup | cfp-scheduler-killer.views.format |
| cfp-scheduler-killer.views.event-setup | cfp-scheduler-killer.views.organizer-layout |
| cfp-scheduler-killer.views.form-builder | cfp-scheduler-killer.views.form-controls |
| cfp-scheduler-killer.views.form-builder | cfp-scheduler-killer.views.organizer-layout |

_34 further rows suppressed by --max-list 40._


**B5 — macro context and generated classes.**

| quantity | count |
|---|---|
| relocated owners containing macro / class / dynamic-resolution constructs | 9 |
| candidate namespaces carrying `:gen-class` | 1 |


| relocated owner | new namespace | constructs found in the body |
|---|---|---|
| time-travel-bar | cfp-scheduler-killer.views.organizer-layout | load |
| render-markdown | cfp-scheduler-killer.views.public-cfp | requiring-resolve |
| board-region | cfp-scheduler-killer.views.review | load |
| login-page | cfp-scheduler-killer.views.auth | eval |
| datastar-script | cfp-scheduler-killer.views.shell | load |
| cfp-about-you | cfp-scheduler-killer.views.public-cfp | import |
| cfp-page | cfp-scheduler-killer.views.public-cfp | import |
| profile-links | cfp-scheduler-killer.views.people | import |
| person-page | cfp-scheduler-killer.views.people | import |


`:gen-class` files: src/cfp_scheduler_killer/server.clj — relocating one changes the generated class package.

**B6 — prose edits.**

Comment texts lost: **11**. Comment texts added: **0**. Relocated owners at `:code-identical-comments-differ`: **0**. Each is a prose change that must be separately authorised. A transform author may not declare its own prose edits outside review.

**B7 — everything the checker could not classify.**

| quantity | count |
|---|---|
| touched source files with no relocation or `ns` edit to explain them | 0 |
| changed files outside the scanned roots, or not Clojure source | 2 |


Changed non-source or out-of-root files. **This checker read none of them:**

| file |
|---|
| .clj-surgeon.edn |
| 00SERVER-LOGS.txt |


### C. Disagreement with the producer receipt

Receipt: `/var/tmp/forge/rows-sublime-5/cellC-after-receipt.edn`

Receipt claims the checker independently re-derived and **agrees with**:

| claim | receipt says | checker re-derived |
|---|---|---|
| caller sites | 87 | 87 |
| relocated owners | 141 | 141 |
| destination namespaces | 20 | 20 |
| promotions | 8 | 8 |


Agreement here means two derivations produced the same number. It does not transfer the receipt's
authority to anything else in it.

**The checker cannot confirm the following. Each is a review obligation, not a discrepancy to be
reconciled by re-reading the receipt.**

| claim | receipt says | checker re-derived | why it matters |
|---|---|---|---|
| candidate identity | content hash d3439cc314a47c2161a3bc71726e66befea721fccb316674a7d6b7e753cb8bb6 | 65ad613baeb8d6ecc2676eafc6cbb3953f0f24eb | the receipt carries no commit that binds it to this tree — UNVERIFIED, not agreement |
| verification_complete | false | not derivable here | the receipt is not proof; required checks are outstanding |
| state | committed-probe-only | not derivable here | not a finished committed state; a probe-only receipt is unfinished proof |
| proof_pending | ["/usr/bin/true"] | not derivable here | still pending — and the pending command is a NO-OP, so it proves nothing when it closes |


### D. What this brief did NOT check

This brief is a static, textual re-derivation over two git trees. It establishes nothing about behaviour.
It did not check:

- runtime behaviour of any kind. No namespace was loaded, no test was run, no process was started.
- cold process startup, classpath, load order as executed, or clean interning. A require-order table is not a load.
- macro expansion. A relocated macro's expansion sites, and any owner consumed by a macro, are unverified here.
- dynamic references: `resolve`, `requiring-resolve`, `ns-resolve`, `find-var`, `intern`, symbols built from strings, multimethod dispatch registration, data readers, and any var named by data rather than by code.
- reader conditionals. `.cljc` branches are compared as text; no platform was elaborated.
- protocol, record and type identity. A relocated `defrecord`/`deftype`/`definterface` changes its generated class package; AOT or serialised artefacts were not examined.
- references from resources, configuration, EDN data, documentation or any non-source file that names a namespace as a string.
- whether the destination partition is a good one, whether a promotion was authorised, or whether a prose edit was wanted.
- test coverage, lint delta, formatting policy, or any repository gate.
- custody. This brief proves nothing about who ran what, or that any claimed execution occurred. It is a re-derivation, not an attestation.
- anything outside the scanned roots, and any file whose extension is not .clj/.cljc/.cljs.
- trailing whitespace inside a string literal, which the `:identical-modulo-whitespace` tier normalises away.
- local bindings. The requalification tier canonicalises a bare name that matches an owner even when it is actually a local; B3 lists the cases where that could hide a broken reference.

_Checker wall: 20008.3 ms. Generated by `bin/preservation-brief`, which reads only the two git trees._

---

## 6. Replay table

Scratch repository `/var/tmp/forge/item3/planted`, rebuilt from scratch by `bin/preservation-replay`:
base = curtaincall-cfp `d9205abc`, clean = `65ad613b` (the landed Cell C split), then one commit per planted
defect on top of clean. Every row is one run of `bin/preservation-brief <row> <base>`.

| change | moved | preserved | omitted | new | promo | cmt-lost | req-reord | req-unsorted | fwd-ref | stale | undef-alias | bare? | inplace-rev | wall ms |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| clean | 141 | 141 | 0 | 7 | 8 | 11 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 20408 |
| wrong-binding | 141 | **140** | 0 | 7 | 8 | 11 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 20597 |
| silent-promotion | 141 | 141 | 0 | 7 | **9** | 11 | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 20147 |
| dropped-comment | 141 | **140** | 0 | 7 | 8 | **12** | 0 | 0 | 0 | 0 | 0 | 1 | 0 | 20207 |
| omitted-owner | **140** | 140 | **1** | 7 | 8 | 11 | 0 | 0 | 0 | 0 | 0 | 1 | 2 | 20433 |
| load-order-forms | 141 | 141 | 0 | 7 | 8 | 11 | 0 | 0 | **2** | 0 | 0 | 1 | 0 | 20821 |
| load-order-requires | 141 | 141 | 0 | 7 | 8 | 11 | 0 | **1** | 0 | 0 | 0 | 1 | 0 | 20581 |

### Planted-defect verdict: **6/6 caught, 0 escaped**

| planted defect | what was planted | the signal the brief raised |
|---|---|---|
| wrong binding | `avatar/pool-face` → `organizer-layout/pool-face` inside a relocated body: requalified to a namespace that does not own the name | body tier `:changed` 0 → 1; preserved bodies 141 → 140; rewritten sites 260 → 259; the diff is printed inline |
| silent promotion | `(defn- committee-card` → `(defn committee-card` on a relocated owner | **B2** promotions 8 → 9, listed by name with `private -> public` |
| dropped comment | one `;;` line deleted from inside the relocated `event-marquee` body | tier `:code-identical-comments-differ` 0 → 1 **and** comment inventory lost 11 → 12, with the exact text |
| omitted owner | `log-summary` deleted from its destination file | **A1** omitted 0 → 1 named as a preservation FAILURE; moved 141 → 140; base call sites 87 → 85 |
| load-order swap (forms) | two top-level forms swapped inside a destination so `committee-card` now names `member-row`, defined below it | **B4** new intra-file forward references 0 → 2, both named |
| load-order swap (requires) | two `:require` lines swapped in a caller's `ns` form | **B4** require sort discipline broken 0 → 1, with the candidate's require order printed |

The require reorder **escaped the first build** and was repaired in the same session; see §3.

### The alias-migration specimen and its own planted defect

| change | in-place bodies preserved | undefined-alias sites |
|---|---|---|
| alias migration, clean | 100/103 | **0** |
| alias migration + one site left on the retired alias | 99/102 | **1**, named `file:line` with the undefined prefix |

The three in-place bodies the clean run refused to clear are worth naming, because they are a real finding
about the constructor and not noise. My alias rewriter was a regex without string/comment masking, and it
edited three things that are not references: the string literals `"exports/sessions.json"` and
`"exports/calendar.ics"` — genuine URL paths, a behavioural break — and one comment. The brief isolated
exactly those three out of 103 changed bodies. A checker that separates 100 correct requalifications from 3
pieces of collateral damage is doing the job the reviewer was doing by hand.

## 7. Checker wall per change

Every figure is the checker's own `wall_ms`, printed at the foot of each brief.

| change | source files in tree | files parsed | owners parsed | wall |
|---|---|---|---|---|
| Cell C split | 83 | 39 | 678 base / 685 candidate | 20.0 s |
| Cell B split | 473 | 12 | 382 / 382 | 19.8 s |
| alias migration (clean) | 472 | 35 | — | 33.2 s |
| alias migration (defect) | 472 | 35 | — | 34.8 s |
| clj-surgeon `571170cc` | 344 | 5 | — | 10.1 s |
| clj-surgeon `d2c3aa80` | 344 | 12 | — | 14.9 s |
| each of the 7 replay rows | 83 | 39 | ~685 | 20.1–20.8 s |
| null change (`d9205abc` against itself) | 83 | **0** | 0 | 0.9 s |

The first build took **136 s** on the Cell B specimen because it parsed the whole repository. Two-stage
scoping — parse the changed files, then only the files that textually mention a name that moved, was
dropped or was added — brought it to **19.8 s** with byte-identical output. Nothing else can hold a call
site or a duplicate for a changed owner, so the narrower scope is not a weaker claim; it is the same claim
computed without reading 460 irrelevant files. This is a Babashka script with no tuning beyond that.

The null row is a standing sanity witness: a candidate compared against itself must parse nothing, move
nothing, lose no comment and report no non-source change. A non-zero figure on that row means the checker
is manufacturing findings.

## 8. Section C: cross-examining a real producer receipt

Run against the retained Cell C receipt `/var/tmp/forge/rows-sublime-5/cellC-after-receipt.edn`. The
checker **agrees** with four counts it derived independently — 87 caller sites, 141 relocated owners, 20
destination namespaces, 8 promotions — and **refuses four things**:

| claim | receipt says | checker | why it matters |
|---|---|---|---|
| candidate identity | content hash `d3439cc3…` | `65ad613b…` | the receipt carries no commit binding it to this tree — UNVERIFIED, not agreement |
| `verification_complete` | `false` | not derivable here | the receipt is not proof; required checks are outstanding |
| `state` | `committed-probe-only` | not derivable here | a probe-only receipt is unfinished proof |
| `proof_pending` | `["/usr/bin/true"]` | not derivable here | still pending — and the pending command is a **no-op**, so it proves nothing when it closes |

This is the shape the Frame 7 consumer contract asks for: agreement on what two derivations both computed,
a typed refusal on everything else, and no laundering of a producer's claim through a checker that only
re-read it.

## 9. Preregistration — the blind-review falsifier (design only; nothing was run)

Registered before any arm, per Astra's §7 discipline. **No review was conducted for this report.** The
prototype's own numbers above are the checker's derivations, not evidence about reviewers.

### 9.1 Hypothesis and forecast, printed before the first arm

**H1 (time).** A reviewer given the preservation brief alongside the complete diff reaches a verdict faster
than a reviewer given the complete diff alone, on large mechanical changes.
Pre-registered point forecast, taken from Astra: **−120 to −240 s on a ~573 s move-heavy review**, with
0–30 s of brief-generation overhead already charged inside the treatment arm's clock. **Forecast on a small
behavioural change: zero**, and the two clj-surgeon landings are in the portfolio precisely to test that
null.

**H2 (safety).** The treatment arm's per-class defect catch rate is **not lower** than the control arm's on
any protected class.

**H3 (non-substitution).** The treatment reviewer does not simply re-read the whole diff anyway.

### 9.2 Arms

| arm | what the reviewer receives |
|---|---|
| **C** control | the complete diff, the base and candidate trees, and the task statement |
| **T** treatment | everything in C, **plus** the preservation brief for that exact pair |
| **N** native-produced treatment | identical to T, on candidates produced by a native patch with no Surgeon receipt at all |

Arm N is required, not optional: Astra's condition is *"The same projection must be available to native when
its evidence qualifies."* The checker already takes two git shas and cannot tell the arms apart, so N costs
nothing but the reviewers.

### 9.3 Specimens

Sixteen specimens, each one an ordered pair of shas in a scratch repository built by
`bin/preservation-replay` or its successor. Registered before assignment, hashes frozen.

- **4 retained clean changes**: Cell C split · Cell B split · clj-surgeon `571170cc` · clj-surgeon `d2c3aa80`.
- **5 planted defects, one per protected class**, on the Cell C clean candidate: wrong binding · silent
  promotion · dropped comment · omitted owner · load-order swap.
- **2 alias-migration specimens**: clean, and one site left on the retired alias.
- **5 decoys**: three further clean changes and two changes carrying a *non-protected* defect (a genuine
  logic edit inside an owner that also moved, and a changed string literal). Decoys exist so that "the brief
  said preserved" cannot be learned as "there is no defect".

The clean:defective ratio (9:7) is registered so a reviewer cannot infer a base rate. Specimen identity,
defect class and defect location are withheld from every reviewer in every arm.

### 9.4 Blinding and assignment

- An **independent runner** — not the builder of the checker, not a reviewer — freezes the specimen set,
  assigns arms, and holds the answer key. The answer key is sealed with a digest published before the first
  arm.
- **Counterbalanced within-reviewer design.** Each reviewer sees 8 of the 16 specimens, half in C and half
  in T/N, in a Latin-square order. No reviewer ever sees the same specimen in two arms.
- Reviewers are told the portfolio contains both clean and defective changes and that a clean change is a
  legitimate GO. They are not told the ratio.
- Model reviewers run in a fresh session per specimen with no memory of previous specimens. Human reviewers
  are not used in the same session as model reviewers on the same specimen.

### 9.5 Clocks, per the frozen Frame 7 protocol

- `t0` = the runner's **prospective** release of the specimen, recorded durably before the reviewer receives
  any bytes. The brief is generated **after** `t0` and its wall is charged to arm T/N. A brief generated
  before `t0` would smuggle solved work outside the clock and voids the specimen.
- `review-end` = the runner's receipt of a submitted verdict record.
- **Primary outcome: `R = review-end − t0`**, per specimen, per arm.
- Charge everything: orientation, reading the brief, reading the diff, any tool the reviewer runs, every
  revision of the verdict. No subtraction for "time spent reading the brief".
- Timeout 1,800 s per specimen; a timeout is recorded as a timeout, never dropped.

### 9.6 The verdict record (the reviewer's deliverable)

A structured record, required in every arm, so that H2 and H3 are measurable rather than asserted:

1. `decision` ∈ {GO, NO-GO, UNVERIFIED} — UNVERIFIED is a legitimate, non-penalised answer.
2. `findings[]`: for each, a free-text claim **plus** a `file:line` locator. A NO-GO with no locator scores
   as a miss, not a catch.
3. `obligations_checked[]`: which of the eight named decision classes the reviewer actually examined.
4. `artifacts_read[]`: logged by the harness, not self-reported — which files and hunks the reviewer opened,
   and how many bytes of diff were fetched.

### 9.7 Outcomes and their pre-registered decision rules

| outcome | measure | rule |
|---|---|---|
| **primary** | median and paired per-specimen `R`, T vs C, on the mechanical specimens | the bet holds if the paired median saving is ≥ 60 s with a 90% bootstrap interval excluding 0 |
| **co-primary safety** | per-class catch rate, T vs C, on the 5 protected classes | **any protected class caught in C and missed in T rejects the bet outright**, regardless of the time saving |
| **unsupported GO** | a GO on a defective specimen | any increase in T over C rejects the bet |
| **non-substitution** | bytes of diff fetched, T vs C | if T's median is ≥ 90% of C's, the reviewer is still reconstructing the whole diff and the bet is rejected even if `R` fell |
| **null check** | `R` on the two clj-surgeon landings | a saving there is a red flag, not a bonus: it means the brief is being trusted where it preserved nothing |
| **native parity** | `R` and catch rate, N vs T | if N ≈ T, the benefit belongs to the platform and must not be reported as a Surgeon advantage |
| **overhead** | checker `wall_ms`, already inside T's clock | reported, never subtracted |

### 9.8 What would falsify the bet, stated as commitments

1. Any protected defect class missed under T that was caught under C.
2. An increase in unsupported GO under T.
3. `R` under T not distinguishable from C on the mechanical specimens.
4. Diff bytes read under T ≈ C: the reviewer kept reconstructing the moves.
5. A saving on the behavioural null specimens — evidence the brief buys unearned confidence.
6. Any specimen where the checker declared a body preserved and a defect was in fact inside that body. This
   is the **kill switch**: a correctness failure suspends the route immediately, not after a wall analysis.

### 9.9 Registered threats

- **Shared blind spot.** The checker and a model reviewer may both be blind to the same class. Mitigated by
  including at least one non-model reviewer, and by the decoy with a genuine logic edit.
- **The brief leaks the answer.** A brief on a defective specimen differs visibly from one on a clean
  specimen (that is the point), so a reviewer could learn "long section A anomaly list = defect". The decoys
  and the 9:7 ratio blunt it; the analysis additionally reports catch rate conditioned on whether the brief
  flagged anything at all.
- **Learning across specimens.** Latin-square order, fresh sessions, and no specimen repeated per reviewer.
- **The builder is not neutral.** I built the checker and I planted the defects in this prototype. The
  falsifier's planted defects must be planted by someone else, and the answer key sealed before any arm.
- **Sample size.** Sixteen specimens across, say, four reviewers gives 64 observations and a paired design;
  it is powered for a 120 s effect on a 573 s review, and it is **not** powered for a 20 s effect. That is
  registered in advance so a null is not later reinterpreted as "underpowered".

### 9.10 What this preregistration does not authorise

No arm, no cohort, no installation, no routing change, and no claim that item 3's qualified slice has run.
It is the document that must exist before the slice can.

## 10. Boundaries and provenance

- Worktree `/home/forge/src/clj-surgeon-item3`, branch `fable/proof-burden`, created by
  `~/bin/worktree-add` from the **fetched** `origin/MCP/main` = `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`;
  the new HEAD equalled that base.
- Commits, author `forge-anvil <forge-anvil@anvil>`, both carrying the required trailers:
  - `f7d7a059` — the prototype.
  - **`1e00e17c`** — the repairs the replay forced, and the receipt cross-examination. **This is the tip.**
- **Nothing was pushed.** The branch has no upstream.
- No edits under `src/`, `test/`, or the Makefile. Three new files, all under `bin/`, 1,736 lines.
- `/home/forge/src/clj-surgeon-item1` and `/var/tmp/forge/ship-v3.7` were never touched.
- No server calls. No connection to ports 7888, 7890, 7894, 7895, or 8300–8339. No `pkill`/`pgrep -f`.
- All temporary state under `/var/tmp/forge/item3/`, never `/tmp`. No JVM was started, so no `-Xmx` was
  needed; the checker is Babashka.
- Read-only against `/home/forge/src/curtaincall-cfp` and `/home/forge/src/clj-surgeon` (`git archive`,
  `git rev-parse`, `git diff --name-only`). Scratch repositories for the E3 Cell B reconstruction, the
  alias-migration specimen and the planted defects were created fresh under `/var/tmp/forge/item3/`.
- Interim written at `/var/tmp/forge/plan2/cellC/opus-item3-interim.md`.

### Retrievability of the specimens the brief asked for

- **Cell C split** — retrievable and used: `d9205abc` → `65ad613b` in `/home/forge/src/curtaincall-cfp`.
- **`astra/comment-edits` 571170cc, batch-5 `d2c3aa80`** — retrievable and used, in
  `/home/forge/src/clj-surgeon`. Neither is a relocation; both are used as anti-overclaim controls.
- **The alias migrations in `/var/tmp/forge/rows-sublime-5/` and `row5-adopt-4/`** — **the trees are gone.**
  `rows-sublime-5` retains patches and receipts but no working tree, and `row5-adopt-4` holds the E3/E4
  grading apparatus, not trees. What *was* recoverable is `rows-sublime-5/adoption-candidate.patch`, whose
  base blob still exists in curtaincall-cfp at `92a7ca14`; that reconstruction is the E3 Cell B specimen
  above, and it applied clean. The alias-migration specimen in the table is therefore **constructed by me**,
  labelled as such everywhere, and is not retained evidence.

## 11. Learnings → ratchets

| learning | ratchet |
|---|---|
| An order check that compares only the common subsequence cannot see a reorder among entries the candidate added. | The sort-discipline test now ships alongside it, and the replay carries `load-order-requires` permanently so the gap cannot silently reopen. |
| A checker whose failure signal fires on a *correct* change (the `defn-`/`defn` phantom omission) will be ignored within a day. | Owner matching is on the def family; privacy is reported once, as a decision, never as damage. The clean Cell C row asserting `omitted = 0` is the witness. |
| Body byte-identity is the wrong primitive for a namespace split, and adopting it would have made the whole bet look false. | The canonical token stream, and the `wrong-binding` replay row proving the looser tier did not become a weaker check. |
| A receipt that agrees on every count it shares with you can still be unfinished proof. | Section C separates *agreement* from *authority*, and refuses candidate-unbound receipts, `verification_complete: false`, `committed-probe-only`, and a `proof_pending` whose only command is `/usr/bin/true`. |
| A tool that reports "preserved" must be able to report "nothing preserved". | The two clj-surgeon landings are permanent portfolio members; if either ever reports a non-zero preserved count, something is wrong with the checker, not with the change. |

## 12. What is next

1. **Someone other than me plants the defects**, seals an answer key, and runs §9. I built the checker and I
   planted these six; that is a demonstration, not a falsification.
2. **Widen the planted-defect set to the classes section D admits it cannot see**: a relocated `defrecord`
   whose generated class package moved, a namespace named as a string in a resource, a `requiring-resolve`
   on a moved owner. If a blind reviewer misses those under T and catches them under C, the brief is
   *creating* a blind spot and must be narrowed.
3. **Wire it into the review entrance** so the brief is generated at the `candidate-submitted` boundary and
   arrives with the diff, rather than being run by hand.
4. **Charge it honestly.** Under Frame 7 the brief's 20 s is inside `L`. It buys review time only if the
   review was actually reading bodies; on a review that was already skipping them it buys nothing, and the
   §9.7 null check is what will say so.
