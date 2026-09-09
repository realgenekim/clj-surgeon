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
| Cell C views split, real and landed (curtaincall-cfp `d9205abc` → `65ad613b`; 7,769 insertions / 4,863 deletions, 29 files) | **20.3 s** | **136/141 relocated + 54/64 caller** | 5 + 10 changed bodies · 20 destination boundaries · 8 promotions · 11 lost comments · 1 unconfirmed bare reference · 3 unmodelled macros · 7 new owners · 21 created namespaces · 2 non-source files |
| E3 Cell B split, real (`exports` → `exports.calendar`, from the retained candidate patch) | **11.6 s** | **25/25 relocated + 19/20 caller** | 1 changed body · 1 destination boundary · 3 promotions · 1 unmodelled macro · 2 refused tree entries |
| Alias migration, constructed (35 files repointed) | **25.0 s** | **81/103 caller bodies** | 22 changed bodies · 3 unmodelled macros · 2 lost comments |
| clj-surgeon `571170cc` comment-edits landing, real | **9.7 s** | **0** | all 9 changed bodies · 3 lost comments · 12 non-source files · 4 refused tree entries |
| clj-surgeon `d2c3aa80` batch-5 landing, real | **14.9 s** | **0** | all 22 changed bodies · 5 lost comments · 13 non-source files · 4 refused tree entries |
| null change (`d9205abc` against itself) | **0.15 s** | — | none; the **only** `clear: true` row in the portfolio |

Overhead sits inside Astra's 0–30 s budget on every specimen. Against her ~573 s move-heavy review, the
brief removes the reading of 190 of the 205 changed bodies in Cell C and replaces the rest with a hard-stop
list the reviewer cannot skip.

**These figures are lower than this report's first draft, and deliberately so.** Sol's fence review found
that the canonicaliser certified a body in which a local binding had been replaced by a Var of the same
name. Making it resolution-aware cost 5 relocated and 10 caller bodies on Cell C — they now say `:changed`
and print their diffs, because their equivalence depended on a bare symbol the scanner could not prove was
a Var. That is the correct answer, and the previous 141/141 was not.

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

**Fourth win, and it is Sol's, not mine: the conservative refusal is cheap.** Requiring the scanner to prove
a bare token is a Var before certifying it cost 15 of 205 bodies on the real Cell C split — about 7%. The
bet survives the correct rule comfortably, which is a much better place to be than defending the incorrect
one.

## 3. Top losses

**The canonicaliser certified a changed binding, and an independent reviewer found it, not me.** Sol planted
a use of a local destructured `edit-form` replaced by the moved Var
`cfp-scheduler-killer.views.portal/edit-form`. Both sides canonicalised to the same owner; the brief said
preserved. That is a defect inside a body the checker declared preserved — this prototype's own
preregistered kill switch. The repair is a structural node reader plus a lexical scope analysis; a bare
token is now canonicalised only when the scanner has established it is a Var reference, and anything inside
a quote, a `case` test, an interop form, or a macro whose semantics are not modelled is frozen and the macro
is named. Sol's exact candidate is now a permanent replay row and lands in A as `:changed`.

The general lesson is the uncomfortable one: **my own six planted defects all passed, and the seventh, which
someone else designed, did not.** A builder planting his own falsifiers tests the failures he already
imagined.

**"Reads only the two git trees" was false.** The first build extracted each tree and walked it with
`file-seq`, so a `src/leak.clj` symlink pointing at `/var/tmp/…` was followed and an external definition
became section-A evidence. Nothing is materialised now: `git ls-tree` enumerates and `git cat-file --batch`
streams blobs, only regular blobs are read, and symlinks, non-blobs and oversized blobs are reported as
refused entries. A stated evidence boundary that the implementation did not enforce is worse than no
boundary, because the report repeats it.

**A `:require` reorder among requires the candidate ADDED escaped the first build entirely.** The order
check compared the subsequence of libs common to both trees — and a require the candidate added has no base
position, so swapping two of them was invisible. Found only because the replay planted it. Repaired by a
second, independent test: if the base file kept its requires sorted, the candidate must too. Both checks now
ship, and the planted swap is caught.

**A `defn-` → `defn` promotion made the matcher report the owner twice — once as omitted, once as new.**
Cell C read as 133 moved + 8 omissions + 8 phantom new owners. A checker whose *failure* signal fires on a
correct change is a checker nobody will read. Owners now match on the def family and the privacy change is
reported once, in section B, where a promotion belongs.

**Two of Astra's five defect classes are caught only in section B, and the first build printed
"141 of 141 preserved" above them.** A silent promotion and a load-order change are not body damage, so they
surface as decisions — but a headline a reviewer can consume and stop on defeats the whole point. Every
obligation is now hoisted into a HARD STOP block **above** section A, the machine summary carries `clear`,
and the process exits 3 when the brief is not clear. Every specimen in this portfolio is `clear: false`; the
only clear row in the whole report is a candidate compared against itself.

**The brief is static. It proves nothing about behaviour**, and section D says so on every run, at length,
including the specific things a namespace move can break that no text comparison sees: macro expansion,
dynamic resolution, generated class packages, load order as executed, and namespaces named as strings in
resources.

## 4. What was built

Three files under `bin/`, on branch `fable/proof-burden`, no edits to `src/`, `test/` or the Makefile.

- **`bin/preservation-brief <candidate-sha> <base-sha>`** — the checker. A **HARD STOP** block, then
  sections **A** mechanically preserved, **B** decisions that remain, **C** disagreement with a producer
  receipt, **D** what it did not check. It reads the two git trees out of the **object database**
  (`git ls-tree` + one `git cat-file --batch`): nothing is materialised, no path is walked, no symlink is
  followed, and blobs above a 2 MiB cap are refused rather than parsed. Exit 3 when the brief is not clear.
- **`bin/preservation_scan.clj`** — an independent Clojure source scanner: byte-exact top-level form spans,
  comment spans, `ns`-form parsing, a flat token stream, and a structural node tree. It deliberately shares
  no code with clj-surgeon's `src/`, so its evidence cannot inherit a defect from the transform it is
  checking.
- **`bin/preservation-replay`** — builds a scratch repository holding the landed Cell C split plus eight
  planted defects, six mine and two Sol's, and runs the brief over every one.

What section A establishes, all of it re-derived:

| check | what it decides |
|---|---|
| A1 owner accounting | every base owner has exactly one candidate home, or it is named as omitted; every candidate owner has a base origin, or it is named as new |
| A2 body identity | four tiers per relocated owner: byte-identical · modulo whitespace · **modulo requalification** · code identical but comments differ · changed (with the diff) |
| A3 exactly once | no owner duplicated beyond its base multiplicity; no relocated owner left behind in its source file |
| A4 namespace-edit inventory | requires added, removed, reordered, alias-changed; imports; namespaces created and deleted — by diffing the `ns` forms |
| A5 static call sites | every site resolved through its own file's aliases, in its own tree: rewritten, stale, undefined-prefix, retargeted |
| A6 comment inventory | comment texts lost and added across the touched files — the comments *between* forms, which no body comparison can see |

A tier is only awarded when the scanner has **proved** each reference is a Var. A lexical scope analysis
over the node tree collects every name a binding form could bind (including destructuring), and freezes
every token inside a quote, a `case` test constant, an interop form, and any macro form whose binding
semantics the scanner does not model — naming that macro in the brief. A frozen or possibly-bound bare token
is left alone, so a candidate that changed it falls to `:changed` and lands in front of a reviewer.

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
- evidence source: **the two git trees, read from the object database by this checker.** No
  filesystem path is walked, no symlink is followed, and no receipt field is used in section A.

> ## ⛔ HARD STOP — this candidate is NOT clear. `clear: false`, exit 3.
>
> **Do not consume the preservation figures below and stop.** The brief raised the following, and
> every one of them needs a human. A signal that lives in section B is not a weaker signal.
>
> - **5** relocated bodies whose text CHANGED — read the diffs (A2)
> - **10** in-place bodies that changed and are not preserved (A2)
> - **11** comment texts LOST (A6)
> - **8** PRIVACY CHANGES, including private->public promotions (B2)
> - **1** bare references the checker could NOT confirm (B3)
> - **3** macro forms whose binding semantics this scanner does not model, so their tokens were left UNRESOLVED: with-as-of, with-etag, with-viewer-session
> - **7** owners with no base origin — new code, not a relocation (A1)
> - **2** changed files outside the scanned roots or not Clojure source — UNREAD (B7)
> - **4** receipt claims the checker could not confirm (C)

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
| `:identical-modulo-requalification` | 53 | identical once every reference is rewritten to the owner it named in the BASE tree — i.e. the only change is that references follow their owners, and each one still resolves to the same owner |
| `:code-identical-comments-differ` | 0 | code identical, COMMENT TEXT CHANGED — a prose review obligation |
| `:changed` | 5 | the body text differs — a FULL review obligation; diffs below |



The def head's privacy suffix (`defn-` vs `defn`) is normalised before comparison, so a promotion is never scored as a body change; every privacy change is reported in **B2** instead. Privacy changes in this candidate: **8**.

Mechanically preserved bodies (tiers 1–3): **136 of 141**.

Relocated owners that are not byte-identical:

| owner | tier | from | to |
|---|---|---|---|
| event-marquee | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| member-row | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.committee |
| board-region | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| log-region | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
| form-preview-region | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-builder |
| organizer-shell | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.organizer-layout |
| header | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.organizer-layout |
| not-blank | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.format |
| field-errors | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls |
| events-list-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| new-event-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| initials | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.avatar |
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
| board-page | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| log-summary | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
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

_18 further rows suppressed by --max-list 40._


<details><summary>diff — event-marquee (changed)</summary>

```diff
--- base/event-marquee
+++ candidate/event-marquee
@@ -12,7 +12,7 @@
    ;; Never a degenerate address. With no name there is no slug, and the line
    ;; says so in the same ghost idiom as the headline rather than inventing
    ;; something like /cfp/2026 out of the dates.
-   (if-let [s (not-blank slug)]
+   (if-let [s (format/not-blank slug)]
      [:div.marquee-url
       [:span.url-text (str host "/cfp/" s)]
       ;; Clipboard is one of the few things the browser owns (global CLAUDE.md)
```

</details>

<details><summary>diff — member-row (changed)</summary>

```diff
--- base/member-row
+++ candidate/member-row
@@ -4,7 +4,7 @@
    morph); Open is a plain link to the person page."
   [event-slug m]
   [:div.member-row {:key (str (:membership-id m))}
-   [:img.member-avatar-img {:src (pool-face (:person-id m)) :alt (:name m)}]
+   [:img.member-avatar-img {:src (avatar/pool-face (:person-id m)) :alt (:name m)}]
    [:div.member-who
     [:span.member-name (:name m)]
     [:span.member-role-pill {:class (:role m)} (:role m)]
```

</details>

<details><summary>diff — board-region (changed)</summary>

```diff
--- base/board-region
+++ candidate/board-region
@@ -12,13 +12,13 @@
    [:div.board-toprow
     [:form.board-controls {:method "get" :action (str "/events/" (:slug event) "/board")}
      [:input {:type "hidden" :name "sort" :value sort-key}]
-     (when (not-blank status) [:input {:type "hidden" :name "status" :value status}])
-     (when (not-blank track) [:input {:type "hidden" :name "track" :value track}])
+     (when (format/not-blank status) [:input {:type "hidden" :name "status" :value status}])
+     (when (format/not-blank track) [:input {:type "hidden" :name "track" :value track}])
      [:input {:type "search" :name "q" :value (or q "")
               :placeholder "Search title, speaker, org…"
               :style "padding:0.35em 0.6em; width:18em;"}]
      [:button.ui.mini.button {:type "submit"} "Search"]
-     (when (not-blank q)
+     (when (format/not-blank q)
        [:a.chip {:href (str "/events/" (:slug event) "/board?sort=" sort-key)} "clear"])]
     ;; ALL submissions over the call's life, never the filtered view (a
     ;; filter is a lens, the sparkline is the weather).
```

</details>

<details><summary>diff — log-region (changed)</summary>

```diff
--- base/log-region
+++ candidate/log-region
@@ -7,7 +7,7 @@
       [:div.empty-state "Nothing recorded yet."]
       (for [e (reverse log-entries)]
         [:div.log-row
-         [:div.log-when (or (fmt-when (:at e) (:tz event)) (:at e))]
+         [:div.log-when (or (format/fmt-when (:at e) (:tz event)) (:at e))]
          [:div.log-type (:type e)]
          [:div.log-what (log-summary e)]
          [:div.log-actor (:actor e)]]))]
```

</details>

<details><summary>diff — form-preview-region (changed)</summary>

```diff
--- base/form-preview-region
+++ candidate/form-preview-region
@@ -17,11 +17,11 @@
       (fn [i f]
         [:div.pv-item {:key (str "pv-" i)}
          [:span.pv-num (inc i)]
-         [:div.pv-field (answer-input f {} {})]])
+         [:div.pv-field (form-controls/answer-input f {} {})]])
       (submissions/session-fields (forms/active-fields fields)))
      (when ghost
        [:div.fb-ghost {:key "ghost"}
-        (answer-input ghost {} {})
+        (form-controls/answer-input ghost {} {})
         [:div.field-hint "Not added yet — appears here when you press Add question."]])
      [:div.cfp-section-title "About you"]
      [:div.field-hint
```

</details>

**Owners that did NOT move, whose body text changed.** The same tiers apply: a caller whose only
change is that its references now follow the owners they name is mechanically preserved too.

| tier | owners |
|---|---|
| `identical-modulo-requalification` | 54 |
| `changed` | 10 |


Of 64 in-place body changes, **54** are mechanically preserved and **10** need a reviewer.

| owner | namespace | tier |
|---|---|---|
| opinions-stars-ride-first-comment-only-test | cfp-scheduler-killer.views-test | `changed` |
| opinions-silent-raters-stay-named-test | cfp-scheduler-killer.views-test | `changed` |
| board-fragment-html | cfp-scheduler-killer.server | `changed` |
| dashboard-fragment-html | cfp-scheduler-killer.server | `changed` |
| log-fragment-html | cfp-scheduler-killer.server | `changed` |
| handle-board | cfp-scheduler-killer.server | `changed` |
| handle-api-docs | cfp-scheduler-killer.server | `changed` |
| handle-event-log | cfp-scheduler-killer.server | `changed` |
| dev-render-mode-test | cfp-scheduler-killer.comms-test | `changed` |
| capture-test | cfp-scheduler-killer.comms-test | `changed` |
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
| settings-response | cfp-scheduler-killer.server | `identical-modulo-requalification` |

_24 further rows suppressed by --max-list 40._


<details><summary>diff — opinions-stars-ride-first-comment-only-test (in place, changed)</summary>

```diff
--- base/opinions-stars-ride-first-comment-only-test
+++ candidate/opinions-stars-ride-first-comment-only-test
@@ -6,7 +6,7 @@
                          :body "First thought." :at t0}
                         {:id "c2" :person-id "p1" :person-name "Ann"
                          :body "Second thought." :at t0}]}
-        html (render (#'views/opinions-block row))]
+        html (render (#'review/opinions-block row))]
     (testing "both comments render"
       (is (str/includes? html "First thought."))
       (is (str/includes? html "Second thought.")))
```

</details>

<details><summary>diff — opinions-silent-raters-stay-named-test (in place, changed)</summary>

```diff
--- base/opinions-silent-raters-stay-named-test
+++ candidate/opinions-silent-raters-stay-named-test
@@ -4,7 +4,7 @@
                        {:person-id "p2" :person-name "Gene" :stars 3.0 :at t0}]
              :comments [{:id "c1" :person-id "p1" :person-name "Ann"
                          :body "A comment." :at t0}]}
-        html (render (#'views/opinions-block row))]
+        html (render (#'review/opinions-block row))]
     (is (str/includes? html "also rated"))
     (is (str/includes? html "Gene"))
     ;; his stars render as a real span, not merely in the histogram tooltip
```

</details>

<details><summary>diff — board-fragment-html (in place, changed)</summary>

```diff
--- base/board-fragment-html
+++ candidate/board-fragment-html
@@ -9,4 +9,4 @@
   (let [tt (time-travel-context req event (str "/events/" (:slug event) "/board"))]
     (with-as-of (:cutoff tt)
       (let [past-event (or (events/event-by-slug (:slug event)) event)]
-        (str (h/html (views/board-region past-event (board-state req past-event))))))))
\ No newline at end of file
+        (str (h/html (review/board-region past-event (board-state req past-event))))))))
\ No newline at end of file
```

</details>

<details><summary>diff — dashboard-fragment-html (in place, changed)</summary>

```diff
--- base/dashboard-fragment-html
+++ candidate/dashboard-fragment-html
@@ -6,7 +6,7 @@
     (with-as-of (:cutoff tt)
       (let [past-event (or (events/event-by-slug (:slug event)) event)]
         (str (h/html
-              (views/event-dashboard-region
-               (request-host req)
-               past-event
-               (dashboard-state req past-event nil))))))))
\ No newline at end of file
+              (dashboard/event-dashboard-region
+                   (request-host req)
+                   past-event
+                   (dashboard-state req past-event nil))))))))
\ No newline at end of file
```

</details>

<details><summary>diff — log-fragment-html (in place, changed)</summary>

```diff
--- base/log-fragment-html
+++ candidate/log-fragment-html
@@ -3,5 +3,5 @@
   (let [tt (time-travel-context req event (str "/events/" (:slug event) "/log"))]
     (with-as-of (:cutoff tt)
       (let [past-event (or (events/event-by-slug (:slug event)) event)]
-        (str (h/html (views/log-region past-event
+        (str (h/html (v-log/log-region past-event
                                        (events/log-for-event (:id past-event)))))))))
\ No newline at end of file
```

</details>

<details><summary>diff — handle-board (in place, changed)</summary>

```diff
--- base/handle-board
+++ candidate/handle-board
@@ -7,6 +7,6 @@
           ;; had a different name, or not existed at all.
           (let [past-event (or (events/event-by-slug slug) event)]
             (html-response
-             (views/board-page past-event
-                               (assoc (board-state req past-event) :time-travel tt))))))
+             (review/board-page past-event
+                                (assoc (board-state req past-event) :time-travel tt))))))
       (not-found-page slug))))
\ No newline at end of file
```

</details>

<details><summary>diff — handle-api-docs (in place, changed)</summary>

```diff
--- base/handle-api-docs
+++ candidate/handle-api-docs
@@ -6,6 +6,6 @@
   (let [slug (get-in req [:path-params :slug])]
     (if-let [event (events/event-by-slug slug)]
       (with-etag req
-        (-> (html-response (views/api-docs-page (request-host req) event))
+        (-> (html-response (integrations/api-docs-page (request-host req) event))
             (assoc-in [:headers "Access-Control-Allow-Origin"] "*")))
       (json-response 404 {"error" "no such event" "slug" slug}))))
\ No newline at end of file
```

</details>

<details><summary>diff — handle-event-log (in place, changed)</summary>

```diff
--- base/handle-event-log
+++ candidate/handle-event-log
@@ -7,7 +7,7 @@
       (let [tt (time-travel-context req event (str "/events/" slug "/log"))]
         (with-as-of (:cutoff tt)
           (let [past-event (or (events/event-by-slug slug) event)]
-            (html-response (views/log-page past-event
+            (html-response (v-log/log-page past-event
                                            (events/log-for-event (:id past-event))
                                            (auth/current-person req)
                                            tt)))))
```

</details>

<details><summary>diff — dev-render-mode-test (in place, changed)</summary>

```diff
--- base/dev-render-mode-test
+++ candidate/dev-render-mode-test
@@ -25,4 +25,4 @@
     (testing "the Log narrates it as 'Would send'"
       (let [e (first (filter #(= "comms.rendered" (:type %))
                              (store/log-for-event (:id event))))]
-        (is (str/includes? (@#'cfp-scheduler-killer.views/log-summary e) "Would send"))))))
\ No newline at end of file
+        (is (str/includes? (@#'log/log-summary e) "Would send"))))))
\ No newline at end of file
```

</details>

<details><summary>diff — capture-test (in place, changed)</summary>

```diff
--- base/capture-test
+++ candidate/capture-test
@@ -57,7 +57,7 @@
                                   (str/starts-with? (str (get-in % [:payload :source]))
                                                     "on-behalf-of"))
                             (store/log-for-event (:id event))))]
-        (is (str/includes? (@#'cfp-scheduler-killer.views/log-summary e)
+        (is (str/includes? (@#'log/log-summary e)
                            "Captured on behalf of"))))
 
     (testing "and it all survives a reload"
```

</details>

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
| tree entries the checker REFUSED to read (symlink, non-blob, oversized) | 0 |
| macro forms whose binding semantics the scanner does not model | 3 |


**Macro forms whose binding semantics this scanner does not model.** Every token inside one of these
was treated as UNRESOLVED, so any body that depended on one for its equivalence is reported as
changed rather than preserved:

| macro form |
|---|
| `with-as-of` |
| `with-etag` |
| `with-viewer-session` |


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

_Checker wall: 20268.5 ms. Generated by `bin/preservation-brief`, which reads only the two git trees._

---

## 6. Replay table

Scratch repository `/var/tmp/forge/item3/planted`, rebuilt from scratch by `bin/preservation-replay`:
base = curtaincall-cfp `d9205abc`, clean = `65ad613b` (the landed Cell C split), then one commit per planted
defect on top of clean. Every row is one run of `bin/preservation-brief <row> <base>`. Rows 7 and 8 are
**Sol's**, planted independently of this builder during the fence review.

| change | clear | moved | preserved | omitted | new | promo | cmt-lost | req-unsorted | fwd-ref | undef-alias | refused | wall ms |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| clean | false | 141 | 136 | 0 | 7 | 8 | 11 | 0 | 0 | 0 | 0 | 20065 |
| wrong-binding | false | 141 | **135** | 0 | 7 | 8 | 11 | 0 | 0 | 0 | 0 | 20506 |
| silent-promotion | false | 141 | 136 | 0 | 7 | **9** | 11 | 0 | 0 | 0 | 0 | 20612 |
| dropped-comment | false | 141 | 136 | 0 | 7 | 8 | **12** | 0 | 0 | 0 | 0 | 20383 |
| omitted-owner | false | **140** | **135** | **1** | 7 | 8 | 11 | 0 | 0 | 0 | 0 | 20327 |
| load-order-forms | false | 141 | 136 | 0 | 7 | 8 | 11 | 0 | **2** | 0 | 0 | 19982 |
| load-order-requires | false | 141 | 136 | 0 | 7 | 8 | 11 | **1** | 0 | 0 | 0 | 20065 |
| **local-shadow** (Sol) | false | 141 | **135** | 0 | 7 | 8 | 11 | 0 | 0 | 0 | 0 | 20219 |
| **symlink-escape** (Sol) | false | 141 | 136 | 0 | 7 | 8 | 11 | 0 | 0 | 0 | **1** | 20311 |

### Planted-defect verdict: **8/8 caught, 0 escaped**

| planted defect | what was planted | the signal the brief raised |
|---|---|---|
| wrong binding | `(organizer-layout/header "Create review committee"` → `(format/header …)` inside the relocated `committee-page`: requalified to a namespace that is aliased in that file and does not own the name | body tier `:changed` 5 → 6; preserved 136 → 135; rewritten sites 260 → 259; the diff is printed inline |
| silent promotion | `(defn- committee-card` → `(defn committee-card` on a relocated owner | hard stop and **B2**: privacy changes 8 → 9, listed by name as `private -> public` |
| dropped comment | one `;;` line deleted from inside the relocated `event-marquee` body | comment inventory lost 11 → 12, with the exact text |
| omitted owner | `log-summary` deleted from its destination file | **A1** omitted 0 → 1, named as a preservation FAILURE; moved 141 → 140; base call sites 87 → 85 |
| load-order swap (forms) | two top-level forms swapped inside a destination so `committee-card` now names `member-row`, defined below it | **B4** new intra-file forward references 0 → 2, both named |
| load-order swap (requires) | two `:require` lines swapped in a caller's `ns` form | **B4** require sort discipline broken 0 → 1, with the candidate's require order printed |
| **local-shadow substitution** (Sol, PB-FENCE-001) | `(form-edit-panel event editing edit-form)` → `(form-edit-panel event editing cfp-scheduler-killer.views.portal/edit-form)`: the local destructured binding replaced by the moved private Var of the same name | body tier `:changed` 5 → 6 (`form-builder-page` joins the list); preserved 136 → 135; rewritten sites 260 → **261** |
| **symlink escape** (Sol, PB-FENCE-002) | `src/leak.clj` added as a symlink to `/var/tmp/forge/item3/sol-proof-external.clj`, which defines `escaped-owner` | refused entries 0 → 1, `src/leak.clj` named with `symlink (mode 120000) — never followed`; **`escaped-owner` appears nowhere in the brief** and the candidate owner count is unchanged |

Two of these escaped an earlier build and were repaired in the same session: the require reorder (§3) and
Sol's local-shadow substitution (§3). The wrong-binding row was also re-targeted after the scope repair:
its original host body had itself become `:changed`, which made the catch a site-count delta rather than a
tier delta, so the plant was moved to `committee-page`, which the clean row certifies as preserved.

### The alias-migration specimen and its own planted defect

| change | in-place bodies preserved | undefined-alias sites |
|---|---|---|
| alias migration, clean | 81/103 | **0** |
| alias migration + one site left on the retired alias | 80/102 | **1**, named `file:line` with the undefined prefix |

Three of the 22 in-place bodies the clean run refuses to clear are worth naming, because they are a real
finding about the constructor and not noise. My alias rewriter was a regex without string or comment
masking, and it edited three things that are not references: the string literals `"exports/sessions.json"`
and `"exports/calendar.ics"` — genuine URL paths, a behavioural break — and one comment. The brief isolated
exactly those. The remaining 19 fall to `:changed` under the scope-aware rule because their equivalence
depended on a bare symbol inside `with-event` / `with-etag` forms, macros the scanner does not model; the
brief names those macros rather than certifying past them.

## 7. Checker wall per change

Every figure is the checker's own `wall_ms`, printed at the foot of each brief.

| change | source files in tree | files parsed | wall |
|---|---|---|---|
| Cell C split | 83 | 39 | 20.3 s |
| Cell B split | 473 | 12 | 11.6 s |
| alias migration (clean) | 472 | 35 | 25.0 s |
| alias migration (defect) | 472 | 35 | 25.0 s |
| clj-surgeon `571170cc` | 344 | 5 | 9.7 s |
| clj-surgeon `d2c3aa80` | 344 | 12 | 14.9 s |
| each of the 9 replay rows | 83 | 39 | 20.0–20.6 s |
| null change (`d9205abc` against itself) | 62 | **0** | **0.15 s** |

The first build took **136 s** on the Cell B specimen because it parsed the whole repository. Two-stage
scoping — parse the changed files, then only the files that textually mention a name that moved, was
dropped or was added — brought it to 20 s with byte-identical output, and reading blobs from the object
database instead of extracting trees took it to **11.6 s**. Nothing else can hold a call site or a duplicate
for a changed owner, so the narrower scope is not a weaker claim; it is the same claim computed without
reading 460 irrelevant files. This is a Babashka script with no tuning beyond that.

The null row is a standing sanity witness, and after PB-FENCE-003 it is also the portfolio's only
`clear: true` row: a candidate compared against itself must parse nothing, move nothing, lose no comment and
raise no obligation. A non-zero figure there means the checker is manufacturing findings.

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

Registered before any arm. **No review was conducted for this report.** The prototype's numbers above are
the checker's own derivations, not evidence about reviewers.

Rewritten after Sol's fence review (PB-FENCE-004): the first draft named a per-class catch-rate co-primary
that its own allocation could not compute — the specimen counts did not sum to the stated ratio, the
observation count did not follow from reviewers × specimens, and nothing guaranteed a protected class
appeared in both arms. The ledger and matrix below are arithmetic, and the co-primary is restated as the
comparison the design can actually support.

### 9.1 Hypotheses and forecasts, printed before the first arm

**H1 (time).** A reviewer given the preservation brief alongside the complete diff reaches a verdict faster
than a reviewer given the diff alone, on large mechanical changes. Pre-registered point forecast, from
Astra: **−120 to −240 s on a ~573 s move-heavy review**, with the checker's own wall (≈20 s) already
charged inside the treatment arm's clock.
**Forecast on a small behavioural change: zero**, and the two clj-surgeon landings are in the portfolio to
test that null.

**H2 (safety).** No protected defect class is caught in the control arm and missed in the treatment arm.

**H3 (non-substitution).** The treatment reviewer does not re-read the whole diff anyway.

### 9.2 Arms

| arm | what the reviewer receives |
|---|---|
| **C** control | the complete diff, both trees, the task statement |
| **T** treatment | everything in C **plus** the preservation brief for that exact sha pair |
| **N** native-produced treatment | identical to T, on candidates produced by a native patch with no producer receipt at all |

Arm N is required, not optional: Astra's condition is *"the same projection must be available to native when
its evidence qualifies."* The checker takes two git shas and cannot tell the arms apart, so N costs only
reviewer time.

### 9.3 Specimen ledger — the counts sum

Five **hosts**, each a real retained change: `H1` Cell C split · `H2` Cell B split · `H3` alias migration ·
`H4` clj-surgeon `571170cc` · `H5` clj-surgeon `d2c3aa80`.

Six **protected classes**, each of which the current checker claims to raise:

`P1` wrong binding (requalified to a namespace that does not own the name) · `P2` silent promotion ·
`P3` dropped comment · `P4` omitted owner · `P5` load-order change (form order or require order) ·
`P6` local-shadow substitution (a local replaced by a Var of the same name — Sol's PB-FENCE-001 case).

| block | construction | specimens |
|---|---|---|
| clean | one per host, unmodified | 5 |
| protected defects | 6 classes × 2 hosts each (the two hosts on which the class is applicable) | 12 |
| decoys, defective | 2: a genuine logic edit inside a relocated body; a changed string literal that is not a reference | 2 |
| decoys, clean | 2: a whitespace-only reformat; an authorised prose edit with its authorisation attached | 2 |
| **total** | | **21** |

**Clean = 5 + 2 = 7. Defective = 12 + 2 = 14. Ratio 7:14, i.e. one in three is clean.** The ratio is
registered so it cannot be re-described afterwards, and reviewers are told a clean change is a legitimate
GO but not what the ratio is. The decoys exist so that "the brief flagged nothing" cannot be learned as
"there is no defect", and so that "the brief flagged something" cannot be learned as "there is one".

### 9.4 Assignment matrix — the observation count follows

Every specimen is reviewed **once in each arm, by three different reviewers**.

- cells = 21 specimens × 3 arms = **63 observations**
- reviewers = **9**, each reviewing **7** specimens: 9 × 7 = **63** ✔
- no reviewer sees a specimen more than once, in any arm
- each reviewer's 7 cells are 3 of one arm and 2 of each other, rotated across reviewers so that each arm
  receives 21 of the 63 cells ✔

Per-class denominators, which the first draft did not state:

| | specimens per protected class | observations per class **per arm** |
|---|---|---|
| P1 … P6 | 2 | **2** (one in C, one in T, one in N, for each of the 2 specimens) |

**This is the honest consequence, and it changes the co-primary.** Two observations per class per arm can
support a **strict dominance rule** — *any* protected class caught in C and missed in T rejects the bet —
but it **cannot estimate a catch rate**, and no rate will be reported. If a rate is wanted, the ledger must
grow to at least 8 specimens per class (48 protected specimens, 144 observations); that is a different,
larger study and is not preregistered here.

The runner freezes the concrete 9 × 7 matrix, with a published digest, before the first arm. Order within a
reviewer is randomised under a fixed seed that is published with the matrix.

### 9.5 Clocks, per the frozen Frame 7 protocol

- `t0` = the runner's **prospective** release of the specimen, recorded durably before the reviewer receives
  any bytes. **The brief is generated after `t0` and its wall is charged to arm T/N.** A brief generated
  before `t0` smuggles solved work outside the clock and voids that specimen.
- `review-end` = the runner's receipt of a submitted verdict record.
- **Primary outcome `R = review-end − t0`**, per specimen, per arm.
- Charge everything: orientation, reading the brief, reading the diff, any tool the reviewer runs, every
  revision of the verdict. No subtraction for time spent reading the brief.
- Timeout 1,800 s; a timeout is recorded as a timeout and never dropped.

### 9.6 The verdict record

Required in every arm, so H2 and H3 are measured rather than asserted:

1. `decision` ∈ {GO, NO-GO, UNVERIFIED}; UNVERIFIED is legitimate and unpenalised.
2. `findings[]`: each a claim **plus** a `file:line` locator. A NO-GO with no locator scores as a miss.
3. `obligations_checked[]`: which named decision classes the reviewer actually examined.
4. `artifacts_read[]`: logged by the harness, never self-reported — which files and hunks were opened and
   how many bytes of diff were fetched.

### 9.7 Outcomes and their decision rules

| outcome | measure | rule |
|---|---|---|
| **primary (time)** | paired per-specimen `R`, T vs C, over the 21 specimens | the bet holds if the paired median saving is ≥ 60 s with a 90% bootstrap interval excluding 0 |
| **co-primary (safety), strict dominance** | per protected class, all C observations vs all T observations | **any protected class caught in C and missed in T rejects the bet**, whatever the time saving. No rate is reported; n=2 per class per arm does not support one |
| unsupported GO | a GO on a defective specimen | any increase in T over C rejects the bet |
| non-substitution | diff bytes fetched, T vs C | if T's median is ≥ 90% of C's, the reviewer is still reconstructing the diff; reject even if `R` fell |
| null check | `R` on `H4`/`H5`, where the brief preserves nothing | a saving there is a red flag, not a bonus |
| native parity | `R` and catch outcomes, N vs T | if N ≈ T the benefit belongs to the platform and must not be reported as a Surgeon advantage |
| hard-stop compliance | did the T reviewer address every hard-stop line? | a T reviewer who issues GO without addressing a hard-stop line is recorded; ≥ 2 such cases rejects PB-FENCE-003's remedy |
| overhead | checker `wall_ms`, already inside T's clock | reported, never subtracted |

**Power, stated so a null cannot be reinterpreted later.** 21 paired specimens is powered for a 120 s effect
on a ~573 s review only if the within-specimen T-vs-C standard deviation is below ≈180 s. The runner
publishes that calculation with the frozen matrix; if the observed variance is larger, the primary is
reported as **inconclusive**, not as a null and not as a win.

### 9.8 What would falsify the bet, as commitments

1. Any protected class caught in C and missed in T.
2. An increase in unsupported GO under T.
3. `R` under T not distinguishable from C on the mechanical specimens.
4. Diff bytes read under T ≈ C: the reviewer kept reconstructing the moves.
5. A saving on the behavioural null specimens — the brief buying unearned confidence.
6. Any specimen where the checker declared a body preserved and a defect was in fact inside that body.
   This is the **kill switch**; it fired once already, on Sol's PB-FENCE-001 falsifier, and suspended the
   route until the canonicaliser was made resolution-aware.

### 9.9 Registered threats

- **Shared blind spot.** The checker and a model reviewer may be blind to the same class. Mitigated by at
  least one non-model reviewer and by the logic-edit decoy.
- **The brief leaks the answer.** A brief on a defective specimen looks different from one on a clean
  specimen — that is the point — so "long hard-stop list = defect" is learnable. The decoys and the 7:14
  ratio blunt it; the analysis additionally reports catch outcomes conditioned on whether the brief's
  hard-stop block was non-empty at all. Note that after PB-FENCE-003 **every** specimen in this portfolio,
  clean ones included, produces a non-empty hard-stop block, which weakens the leak considerably.
- **Learning across specimens.** Randomised order under a published seed, fresh sessions, no specimen
  repeated per reviewer.
- **The builder is not neutral.** I built the checker and planted six of the eight replay defects; Sol
  planted the other two. The falsifier's specimens must be planted by someone who did not build the
  checker, and the answer key sealed before the first arm.

### 9.10 What this preregistration does not authorise

No arm, no cohort, no installation, no routing change, and no claim that item 3's qualified slice has run.
It is the document that must exist before the slice can.

## 10. Boundaries and provenance

- Worktree `/home/forge/src/clj-surgeon-item3`, branch `fable/proof-burden`, created by
  `~/bin/worktree-add` from the **fetched** `origin/MCP/main` = `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`;
  the new HEAD equalled that base.
- Commits, author `forge-anvil <forge-anvil@anvil>`, all carrying the required trailers:
  - `f7d7a059` — the prototype.
  - `1e00e17c` — the repairs my own replay forced, and the receipt cross-examination. **Sol's fence review
    returned NO-GO on this commit**, verdict at
    `/var/tmp/forge/ship/20260909T160254Z-1e00e17c8d95/verdict-1.md`.
  - **`8751ed9e`** — all four fence findings repaired. **This is the tip.**
- **Nothing was pushed.** The branch has no upstream.
- No edits under `src/`, `test/`, or the Makefile. Three new files, all under `bin/`.
- The brief no longer materialises anything: it reads blobs from the object database. The only path it
  writes is a temp directory for `diff` invocations, under `/var/tmp/forge/item3/`.
- `/home/forge/src/clj-surgeon-item1` and `/var/tmp/forge/ship-v3.7` were never touched.
- No server calls. No connection to ports 7888, 7890, 7894, 7895, or 8300–8339. No `pkill`/`pgrep -f`.
- All temporary state under `/var/tmp/forge/item3/`, never `/tmp`. No JVM was started, so no `-Xmx` was
  needed; the checker is Babashka. Pre-parse caps: 2 MiB per blob, 64 MiB per tree, refusing with exit 4
  above the total.
- Read-only against `/home/forge/src/curtaincall-cfp` and `/home/forge/src/clj-surgeon` (`git ls-tree`,
  `git cat-file --batch`, `git rev-parse`, `git diff --name-only`). Scratch repositories for the E3 Cell B reconstruction, the
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
| **A builder's own planted defects test only the failures he already imagined.** My six all passed; the seventh, designed by someone else, broke the tool. | Sol's two probes are permanent replay rows, and §9.9 now requires that the falsifier's specimens be planted by someone who did not build the checker, with the answer key sealed before the first arm. |
| **A bare symbol is not a Var until something proves it is.** Canonicalising by name alone equates a local binding with the Var that shadows it. | A structural node reader plus a lexical scope analysis; a token inside a binding form, a quote, a `case` test, an interop form or an unmodelled macro is frozen, and the unmodelled macro is named in the brief. The permanent `local-shadow` row is the witness. |
| **A stated evidence boundary that the implementation does not enforce is worse than none**, because the report repeats it. | The brief reads the object database, never the filesystem; symlinks, non-blobs and oversized blobs are counted as refused entries and named. The permanent `symlink-escape` row is the witness. |
| **A headline a reviewer can consume and stop on defeats a checker whose real value is in section B.** | Every obligation is hoisted above section A into a hard-stop block, the summary carries `clear`, and the exit code is 3 when not clear. The null change is the only clear row in the portfolio. |
| A preregistration whose arithmetic does not close cannot be executed, however good its rules sound. | §9 now carries a specimen ledger whose counts sum, an assignment matrix whose observation count follows from reviewers × specimens, stated per-class denominators, and a co-primary restated as strict dominance because n=2 per class cannot support a rate. |

## 12. What is next

1. **Someone other than me plants the defects**, seals an answer key, and runs §9. Sol's fence review is the
   proof that this matters and not a formality: two of the eight replay rows are his, and one of them broke
   the tool.
2. **Widen the planted-defect set to the classes section D admits it cannot see**: a relocated `defrecord`
   whose generated class package moved, a namespace named as a string in a resource, a `requiring-resolve`
   on a moved owner, and a binding introduced by a macro the scanner *does* model but models wrongly. If a blind reviewer misses those under T and catches them under C, the brief is
   *creating* a blind spot and must be narrowed.
3. **Wire it into the review entrance** so the brief is generated at the `candidate-submitted` boundary and
   arrives with the diff, rather than being run by hand.
4. **Reduce the unmodelled-macro surface deliberately, and measure what each addition buys.** Three repo
   macros currently freeze the bodies that use them, and 19 of the alias-migration specimen's 22
   unpreserved bodies are `with-event`/`with-etag` frozen regions. Modelling a macro's binding form is a
   small change with a measurable return in preserved bodies — and a correctness risk if modelled wrongly,
   so each one needs its own planted-defect row before it ships.
5. **Charge it honestly.** Under Frame 7 the brief's 20 s is inside `L`. It buys review time only if the
   review was actually reading bodies; on a review that was already skipping them it buys nothing, and the
   §9.7 null check is what will say so.
