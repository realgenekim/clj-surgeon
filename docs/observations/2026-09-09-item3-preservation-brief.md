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
| Cell C views split, real and landed (curtaincall-cfp `d9205abc` → `65ad613b`; 7,769 insertions / 4,863 deletions, 29 files) | **20.2 s** | **102/141 relocated + 32/64 caller** | 6 changed bodies · 33 + 32 uncertified for frozen call heads · 59 frozen heads named · 20 destination boundaries · 8 promotions · 11 lost comments · 7 new owners · 21 created namespaces |
| E3 Cell B split, real (`exports` → `exports.calendar`, from the retained candidate patch) | **11.4 s** | **20/25 relocated + 8/20 caller** | 4 changed bodies · 3 cross-tree kind disagreements · 7 frozen heads · 3 promotions · 2 refused tree entries |
| Alias migration, constructed (35 files repointed) | **25.4 s** | **39/103 caller bodies** | 64 uncertified or changed · 21 frozen heads · 2 lost comments |
| clj-surgeon `571170cc` comment-edits landing, real | **10.0 s** | **0** | all 9 changed bodies · 3 lost comments · 12 non-source files · 4 refused tree entries |
| clj-surgeon `d2c3aa80` batch-5 landing, real | **15.0 s** | **0** | all 22 changed bodies · 5 lost comments · 13 non-source files · 4 refused tree entries |
| null change (`d9205abc` against itself) | **0.18 s** | — | none; the **only** `clear: true` row in the portfolio |

Overhead sits inside Astra's 0–30 s budget on every specimen. Against her ~573 s move-heavy review, the
brief now removes the reading of 135 of the 205 changed bodies in Cell C and hands the reviewer the rest
under a hard-stop list.

### These figures fell twice, and both falls were correct

| Cell C | relocated bodies preserved | caller bodies preserved |
|---|---|---|
| first draft | 141/141 | 64/64 |
| after Sol's round 1 (resolution-aware canonicaliser) | 136/141 | 54/64 |
| after Sol's round 2 (allowlist, not denylist) | 103/141 | 32/64 |
| after Sol's round 3 (every table entry carries a falsifier) | 103/141 | 32/64 |
| **after Sol's round 4 (resolution precedes every table)** | **102/141** | **32/64** |

Sol reviewed the prototype four times and broke it four times — seven planted specimens, seven false
certifications, every one of them the same shape: a *bare symbol* that was not the Var the canonicaliser
assumed. Round 1: a local destructured binding replaced by a moved Var of the same name. Round 2: the same
substitution, but bound by `compojure.core/GET` — a binding macro from a library, defined in neither tree
and spelled neither `with-*` nor `def*`, so the denylist of "macros I do not model" never saw it. Round 3:
three more, and this time the leak was not in the rule but in the **tables the rule consults** — `are`
declared non-binding when `clojure.test/are` binds its argv; a merged def-head index letting a candidate
`defn` overwrite a base `defmacro`; and an uppercase require alias slipping past the "that's a Java class"
heuristic because the heuristic ran before alias resolution.

Round 4 found the deepest one: the tables were keyed by **spelling**, and `head-status` matched a bare name
before it ever read the file's `ns` form. A file that refers a binding macro of its own named `testing` was
handed `clojure.test/testing`'s semantics. Every table is now keyed by a fully qualified Var, and a head is
resolved through the file's own namespace — refer, then alias, then a definition in the two trees, then the
core default, and only when nothing shadows it — before any table is consulted.

**Round 3 cost nothing on Cell C** (tightened tables and a dozen macros moved to frozen cancelled out);
**round 4 cost one more body and named 18 more frozen heads**, 41 → 59. Cell B moved 21 → 20 and 9 → 8, and
now reports three real cross-tree kind disagreements it had been silently resolving in the candidate's
favour.

**Spelling is not identity.** A table keyed by a bare name speaks for whatever that name happens to mean in
the file being read, which is not what the table's author meant. Tables are keyed by fully qualified Var and
resolution precedes every lookup.

**A denylist over syntax is never conservative.** The rule is inverted: a bare token is canonicalised only
when every enclosing list head is a modelled core form, a non-macro `def`/`defn` this checker can actually
find in the two trees, or a clojure.core function from a shipped allowlist. Everything else is frozen, the
head is named and counted, and the body cannot reach a tier that depends on canonicalisation.

**And an allowlist is only as good as its audit — and an audit of spellings audits nothing.**
`bin/preservation-tables-test` refuses to let a table entry exist without its falsifier: **202** allowlist
functions machine-resolved in their stated namespace and proved non-macro, **74** modelled and frozen Vars
resolved, **45 executed** witnesses run against the real scope analysis, and — the rung that would have
caught round 4 before it shipped — **276 GENERATED shadow witnesses, one per table entry, produced by the
test rather than hand-written**: for every table Var, a specimen that refers a same-named binding macro from
another namespace must flip that head to `unmodelled`, with a matching control asserting the head keeps its
meaning when nothing shadows it. It reports PASS, and it is the artefact I would keep if I kept nothing else.

**83 of the surviving 103 are byte-identical** and need no canonicalisation at all, so the loss lands
entirely in the requalification tier — which is exactly the tier that made this bet interesting. That is the
state of the evidence, not a reason to soften the rule. The recovery path is in §12 and it is real work:
widen the resolver by reading definitions it currently misses (`>defn` alone hides real functions from it),
each widening carrying its own planted-defect row.

## 2. Top win

**The load-bearing insight is that byte-identity is nearly worthless on a split, and requalification-aware
comparison is the whole idea — but only where it can be proved.**

The first build scored Cell C at 96/141. The reason is structural, not a bug: a split *must* rewrite
intra-namespace references, `(organizer-shell …)` becomes `(organizer-layout/organizer-shell …)`, the head
widens, and the formatter reindents the entire form. A reviewer looking at that diff sees the whole body
change and has to read all of it.

Comparing a **token stream in which every reference is canonicalised to the owner it named in the base
tree** recovers those bodies: *accept a reference that follows its owner, refuse one that is repointed
anywhere else.* The naive version of that rule reached 141/141 and was wrong twice; the version that only
canonicalises a token it has proved is a Var reaches **103/141**, and the three planted substitutions all
land in `:changed`. **The idea survived both falsifications; the naive implementation did not.**

**Second win: the counts corroborate the frozen manifests without touching a receipt.** Cell C's manifest
says 141 owners, 20 destinations, 87 static sites. The checker, reading only the two trees, re-derives
141, 20 and 87. Cell B's frozen A1/A3 oracle says 25 owners and 43 external references; the checker
re-derives 25 and 43. Those numbers were never read from a receipt — section A of the brief has no receipt
input at all.

**Third win: the negative controls refuse to help.** On the two real clj-surgeon feature landings the brief
preserves nothing and hands the reviewer 100% of the changed bodies. A tool that shrinks the reviewer's
burden on a behavioural change would be worse than useless; this one declines.

**Fourth win, and it is Sol's, not mine: the tool now fails in the safe direction, and says so loudly.**
Two independent falsifiers, two false certifications, two repairs. What survives is a checker whose
preserved count is a claim it can defend — 83 byte-identical bodies that need no argument at all, plus 20
whose every reference the scanner resolved through heads it can name. The 70 it now refuses to certify are
listed, with the call heads that caused the refusal, so a reviewer can see precisely why — and that list is
itself the work plan for recovering them (§12).

## 3. Top losses

**The denylist was the design error, and it took two rounds to see it.** Round 1 froze macros defined in the
scanned trees, plus any unrecognised `with-*`/`def*` head. That is a denylist over *spelling*, and Sol
defeated it in one line with `compojure.core/GET` — a real binding macro from a real library, matching none
of those patterns. The lesson generalises past this tool: **an enumeration of the constructs you distrust
can never be complete when anyone may define a new one; only an enumeration of the constructs you have
positively verified can be.** Inverting the rule cost 33 more relocated and 22 more caller bodies on Cell C,
and it is the only version of this checker whose section-A claim is defensible.

**The canonicaliser certified a changed binding, and an independent reviewer found it, not me.** Sol planted
a use of a local destructured `edit-form` replaced by the moved Var
`cfp-scheduler-killer.views.portal/edit-form`. Both sides canonicalised to the same owner; the brief said
preserved. That is a defect inside a body the checker declared preserved — this prototype's own
preregistered kill switch. The repair is a structural node reader plus a lexical scope analysis; a bare
token is now canonicalised only when the scanner has established it is a Var reference, and anything inside
a quote, a `case` test, an interop form, or a macro whose semantics are not modelled is frozen and the macro
is named. Sol's exact candidate is now a permanent replay row and lands in A as `:changed`.

The general lesson is the uncomfortable one: **my own six planted defects all passed; both of the ones
someone else designed broke the tool.** A builder planting his own falsifiers tests the failures he already
imagined, which is the set that is already handled.

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
- **`bin/preservation-replay`** — builds a scratch repository holding the landed Cell C split plus twelve
  planted defects, six mine and seven Sol's, and runs the brief over every one. Six are **pairs**: their
  probe exists in both trees.
- **`bin/preservation-tables-test`** — the ratchet over the brief's own allowlist tables. It refuses to let
  a table entry exist without a falsifier, and it is what makes the tables an audited claim rather than a
  list somebody typed. Rungs R1–R5 are described in its header — including 276 generated shadow witnesses —
  and it exits nonzero on any unsupported claim.

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
> - **6** relocated bodies whose text CHANGED — read the diffs (A2)
> - **33** relocated bodies NOT certified because they call a head this scanner cannot vouch for (A2)
> - **16** in-place bodies NOT certified because they call a head this scanner cannot vouch for (A2)
> - **16** in-place bodies that changed and are not preserved (A2)
> - **11** comment texts LOST (A6)
> - **8** PRIVACY CHANGES, including private->public promotions (B2)
> - **1** bare references the checker could NOT confirm (B3)
> - **59** call heads whose tokens were FROZEN — unresolvable, or a macro whose binding grammar the scanner deliberately does not walk: /, answer-input (shadowed by a local), cond->, curl, curl (shadowed by a local), deftest, ds/bind -> datastar-kit.ds/bind, ds/copy-nearest-text -> datastar-kit.ds/copy-nearest-text
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
| `:identical-modulo-requalification` | 19 | identical once every reference is rewritten to the owner it named in the BASE tree — i.e. the only change is that references follow their owners, and each one still resolves to the same owner |
| `:code-identical-comments-differ` | 0 | code identical, COMMENT TEXT CHANGED — a prose review obligation |
| `:unmodelled-macro-context` | 33 | the canonical streams matched, but the body called a head this scanner cannot vouch for, so the match is NOT evidence — read the body |
| `:changed` | 6 | the body text differs — a FULL review obligation; diffs below |



The def head's privacy suffix (`defn-` vs `defn`) is normalised before comparison, so a promotion is never scored as a body change; every privacy change is reported in **B2** instead. Privacy changes in this candidate: **8**.

Mechanically preserved bodies (tiers 1–3): **102 of 141**.

Relocated owners that are not byte-identical:

| owner | tier | from | to |
|---|---|---|---|
| organizer-shell | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.organizer-layout |
| event-marquee | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| member-row | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.committee |
| board-region | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| log-region | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
| form-preview-region | `changed` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-builder |
| not-blank | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.format |
| field-errors | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls |
| events-list-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| new-event-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| initials | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.avatar |
| committee-card | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.committee |
| submissions-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| field-error | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-controls |
| sort-chip | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| status-chip | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| board-qs | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| track-chip | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| board-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| log-summary | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.log |
| submission-detail-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| exports-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| api-docs-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| settings-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.integrations |
| schedule-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.schedule |
| agenda-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.schedule |
| capture-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.review |
| replay-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.replay |
| edit-form | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| profile-form | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| portal-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.portal |
| field-form-fields | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-builder |
| form-builder-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.form-builder |
| cfp-closed-notice | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.public-cfp |
| cfp-about-you | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.public-cfp |
| cfp-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.public-cfp |
| event-details-page | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.event-setup |
| dash-feed-talk | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.dashboard |
| event-dashboard-region | `unmodelled-macro-context` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.dashboard |
| header | `identical-modulo-requalification` | cfp-scheduler-killer.views | cfp-scheduler-killer.views.organizer-layout |

_18 further rows suppressed by --max-list 40._


<details><summary>diff — organizer-shell (changed)</summary>

```diff
--- base/organizer-shell
+++ candidate/organizer-shell
@@ -11,20 +11,20 @@
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
       [:title title]
-      [:link {:rel "icon" :href favicon-data-uri}]
+      [:link {:rel "icon" :href shell/favicon-data-uri}]
       [:link {:rel "stylesheet"
               :href "https://cdn.jsdelivr.net/npm/fomantic-ui@2.9.3/dist/semantic.min.css"}]
       [:script {:src "https://code.jquery.com/jquery-3.6.0.min.js"}]
       [:script {:src "https://cdn.jsdelivr.net/npm/fomantic-ui@2.9.3/dist/semantic.min.js"}]
-      [:script {:src (versioned "/js/datastar-kit.js")}]
-      [:script {:src (versioned "/js/keyboard.js") :defer true}]
-      [:script {:src (versioned "/js/ghost-fill.js") :defer true}]
+      [:script {:src (shell/versioned "/js/datastar-kit.js")}]
+      [:script {:src (shell/versioned "/js/keyboard.js") :defer true}]
+      [:script {:src (shell/versioned "/js/ghost-fill.js") :defer true}]
         ;; Some pages need Datastar for one-shot actions without owning a
         ;; persistent stream. Keep runtime loading separate from SSE mounting so
         ;; live scrub cannot consume a browser connection for the page lifetime.
       (when (or (:datastar? nav) (:sse? nav))
-        [:script {:type "module" :src (versioned "/vendor/datastar-aliased.js")}])
-      [:link {:rel "stylesheet" :href (versioned "/css/app.css")}]]
+        [:script {:type "module" :src (shell/versioned "/vendor/datastar-aliased.js")}])
+      [:link {:rel "stylesheet" :href (shell/versioned "/css/app.css")}]]
      [:body (when-let [attrs (:body-attrs nav)] attrs)
         ;; The sidebar owns the top of the viewport (Gene, 2026-08-10: no
         ;; wasted band above it) — whoami rides the content column's first
```

</details>

<details><summary>diff — not-blank (unmodelled-macro-context)</summary>

```diff
--- base/not-blank
+++ candidate/not-blank
@@ -1 +1 @@
-(defn- not-blank [s] (when-not (str/blank? s) s))
\ No newline at end of file
+(defn not-blank [s] (when-not (str/blank? s) s))
\ No newline at end of file
```

</details>

<details><summary>diff — field-errors (unmodelled-macro-context)</summary>

```diff
--- base/field-errors
+++ candidate/field-errors
@@ -1,3 +1,3 @@
-(defn- field-errors [errors k]
+(defn field-errors [errors k]
   (when-let [msgs (get errors k)]
     [:div.ui.pointing.red.basic.label (str/join " " msgs)]))
\ No newline at end of file
```

</details>

_36 further body diffs suppressed by --diffs 3._

**Owners that did NOT move, whose body text changed.** The same tiers apply: a caller whose only
change is that its references now follow the owners they name is mechanically preserved too.

| tier | owners |
|---|---|
| `identical-modulo-requalification` | 32 |
| `unmodelled-macro-context` | 16 |
| `changed` | 16 |


Of 64 in-place body changes, **32** are mechanically preserved and **32** need a reviewer.

| owner | namespace | tier |
|---|---|---|
| opinions-stars-ride-first-comment-only-test | cfp-scheduler-killer.views-test | `changed` |
| opinions-silent-raters-stay-named-test | cfp-scheduler-killer.views-test | `changed` |
| histogram-buckets-and-hover-test | cfp-scheduler-killer.views-test | `changed` |
| fmt-when-test | cfp-scheduler-killer.polish-test | `changed` |
| scrub-slider-wiring-test | cfp-scheduler-killer.polish-test | `changed` |
| handle-sse-state | cfp-scheduler-killer.server | `changed` |
| board-fragment-html | cfp-scheduler-killer.server | `changed` |
| dashboard-fragment-html | cfp-scheduler-killer.server | `changed` |
| log-fragment-html | cfp-scheduler-killer.server | `changed` |
| handle-board | cfp-scheduler-killer.server | `changed` |
| handle-event-log | cfp-scheduler-killer.server | `changed` |
| dev-render-mode-test | cfp-scheduler-killer.comms-test | `changed` |
| capture-test | cfp-scheduler-killer.comms-test | `changed` |
| retire-hides-without-erasing-test | cfp-scheduler-killer.forms-test | `changed` |
| editing-the-form-never-rewrites-an-existing-submission-test | cfp-scheduler-killer.forms-test | `changed` |
| form-page-renders-the-real-public-renderer-test | cfp-scheduler-killer.forms-test | `changed` |
| handle-create-event | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-event-details-save | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-events-preview | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-demo-login | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| reject-value! | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-capture | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| with-form | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-form-add | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-form-update | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-form-preview | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| with-schedule | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| with-replay | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| render-cfp | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-cfp-draft | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-portal-draft | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-cfp-import-live | cfp-scheduler-killer.server | `unmodelled-macro-context` |
| handle-home | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-events-list | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-new-event | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| not-found-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| render-event-dashboard | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| render-committee-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-event-details | cfp-scheduler-killer.server | `identical-modulo-requalification` |
| handle-exports-page | cfp-scheduler-killer.server | `identical-modulo-requalification` |

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

<details><summary>diff — histogram-buckets-and-hover-test (in place, changed)</summary>

```diff
--- base/histogram-buckets-and-hover-test
+++ candidate/histogram-buckets-and-hover-test
@@ -2,10 +2,10 @@
   ;; Second ruling (Gene, 2026-08-10): histograms have BARS; five buckets,
   ;; halves folding down. Every bucket renders (empty ones marked), and the
   ;; hover title names every rater precisely.
-  (let [html (render (views/star-histogram
-                      [{:person-name "Ann" :stars 4.0}
-                       {:person-name "Gene" :stars 4.5}
-                       {:person-name "Alex" :stars 2.0}]))]
+  (let [html (render (review/star-histogram
+                       [{:person-name "Ann" :stars 4.0}
+                        {:person-name "Gene" :stars 4.5}
+                        {:person-name "Alex" :stars 2.0}]))]
     (testing "five buckets, three empty (1, 3, 5)"
       (is (= 5 (count (re-seq #"hbar" html))))
       (is (= 3 (count (re-seq #"empty" html)))))
```

</details>

_29 further in-place diffs suppressed by --diffs 3._

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
| call heads whose tokens were frozen (unresolvable, or a deliberately unwalked macro) | 59 |


**Call heads whose tokens were frozen.** Either the scanner could not resolve the head to a
non-macro definition it can read, or the head is a macro whose binding grammar it deliberately does
not walk. Every token inside such a form is left UNRESOLVED, so a body that depended on one for its
equivalence is reported as not certified rather than preserved:

| macro form |
|---|
| `/` |
| `answer-input (shadowed by a local)` |
| `cond->` |
| `curl` |
| `curl (shadowed by a local)` |
| `deftest` |
| `ds/bind -> datastar-kit.ds/bind` |
| `ds/copy-nearest-text -> datastar-kit.ds/copy-nearest-text` |
| `ds/keydown-expr -> datastar-kit.ds/keydown-expr` |
| `ds/on-meta -> datastar-kit.ds/on-meta` |
| `ds/post-action* -> datastar-kit.ds/post-action*` |
| `ds/sse-mount -> datastar-kit.ds/sse-mount` |
| `ds/sse-mount-url -> datastar-kit.ds/sse-mount-url` |
| `err` |
| `err (shadowed by a local)` |
| `events/create-event! -> cfp-scheduler-killer.events/create-event!` |
| `events/update-event-details! -> cfp-scheduler-killer.events/update-event-details!` |
| `export` |
| `export (shadowed by a local)` |
| `f` |
| `f (shadowed by a local)` |
| `form-controls/answer-input (shadowed by a local)` |
| `forms/add-field! -> cfp-scheduler-killer.forms/add-field!` |
| `forms/update-field! -> cfp-scheduler-killer.forms/update-field!` |
| `h/html -> hiccup2.core/html` |
| `java.net.URLEncoder/encode` |
| `java.time.Instant/now` |
| `json/write-str -> clojure.data.json/write-str` |
| `keyword (shadowed by a local)` |
| `log/debug -> taoensso.timbre/debug` |
| `log/info -> taoensso.timbre/info` |
| `log/warn -> taoensso.timbre/warn` |
| `name (shadowed by a local)` |
| `note` |
| `note (shadowed by a local)` |
| `prose` |
| `prose (shadowed by a local)` |
| `sequential?` |
| `sig` |
| `sig (shadowed by a local)` |

_19 further rows suppressed by --max-list 40._


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

_Checker wall: 20164.8 ms. Generated by `bin/preservation-brief`, which reads only the two git trees._

---

## 6. Replay table

Scratch repository `/var/tmp/forge/item3/planted`, rebuilt from scratch by `bin/preservation-replay`:
base = curtaincall-cfp `d9205abc`, clean = `65ad613b` (the landed Cell C split), then one commit per planted
defect. Six rows are **Sol's**, planted independently of this builder across three fence reviews. Five rows
are **pairs** — their probe exists in both trees — so their counters are read against their own base.

| change | clear | moved | preserved | omitted | new | promo | cmt-lost | req-unsorted | fwd-ref | bare? | refused | wall ms |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| clean | false | 141 | 102 | 0 | 7 | 8 | 11 | 0 | 0 | 1 | 0 | 20586 |
| wrong-binding | false | 141 | **101** | 0 | 7 | 8 | 11 | 0 | 0 | 1 | 0 | 20674 |
| silent-promotion | false | 141 | 102 | 0 | 7 | **9** | 11 | 0 | 0 | 1 | 0 | 20161 |
| dropped-comment | false | 141 | 102 | 0 | 7 | 8 | **12** | 0 | 0 | 1 | 0 | 20379 |
| omitted-owner | false | **140** | 102 | **1** | 7 | 8 | 11 | 0 | 0 | 1 | 0 | 20061 |
| load-order-forms | false | 141 | 102 | 0 | 7 | 8 | 11 | 0 | **2** | 1 | 0 | 20292 |
| load-order-requires | false | 141 | 102 | 0 | 7 | 8 | 11 | **1** | 0 | 1 | 0 | 20150 |
| **local-shadow** (Sol r1) | false | 141 | 102 | 0 | 7 | 8 | 11 | 0 | 0 | 1 | 0 | 20161 |
| **symlink-escape** (Sol r1) | false | 141 | 102 | 0 | 7 | 8 | 11 | 0 | 0 | 1 | **1** | 20343 |
| **external-macro** (Sol r2, pair) | false | 141 | **101** | 0 | 7 | 8 | 11 | 0 | 0 | **2** | 0 | 20189 |
| **are-binding** (Sol r3, pair) | false | 141 | **101** | 0 | 7 | 8 | 11 | 0 | 0 | **2** | 0 | 20645 |
| **uppercase-alias** (Sol r3, pair) | false | 141 | **101** | 0 | 7 | 8 | 11 | **1** | 0 | **2** | 0 | 20569 |
| **kind-collision** (Sol r3, pair) | false | 141 | **101** | **1** | **8** | 8 | 11 | **1** | 0 | 1 | 0 | 19971 |
| **referred-testing** (Sol r4, pair) | false | 141 | **101** | 0 | 7 | 8 | 11 | 0 | 0 | **2** | 0 | 20148 |

Body tiers, which is where seven of the thirteen catches land. Every one of Sol's seven moves
`committee-page` or `form-builder-page` out of a certified tier and into `:changed`, diff printed:

| change | `:byte-identical` | `:identical-modulo-requalification` | `:unmodelled-macro-context` | `:changed` |
|---|---|---|---|---|
| clean | 83 | 19 | 33 | 6 |
| wrong-binding | 83 | **18** | 33 | **7** |
| local-shadow | 83 | 19 | **32** | **7** |
| external-macro | 83 | **18** | 33 | **7** |
| are-binding | 83 | **18** | 33 | **7** |
| uppercase-alias | 83 | **18** | 33 | **7** |
| kind-collision | 83 | **18** | 33 | **7** |
| referred-testing | 83 | **18** | 33 | **7** |

### Planted-defect verdict: **13/13 caught, 0 escaped**

| planted defect | what was planted | the signal the brief raised |
|---|---|---|
| wrong binding | `(organizer-layout/header …)` → `(format/header …)` inside the relocated `committee-page`: requalified to a namespace aliased in that file that does not own the name | tier `:changed` 6 → 7, `:identical-modulo-requalification` 20 → 19; preserved 103 → 102; diff printed |
| silent promotion | `(defn- committee-card` → `(defn committee-card` | hard stop and **B2**: privacy changes 8 → 9, listed by name |
| dropped comment | one `;;` line deleted from inside the relocated `event-marquee` body | comment inventory lost 11 → 12, with the exact text |
| omitted owner | `log-summary` deleted from its destination file | **A1** omitted 0 → 1 as a preservation FAILURE; moved 141 → 140 |
| load-order swap (forms) | two top-level forms swapped so `committee-card` names `member-row`, defined below it | **B4** new intra-file forward references 0 → 2 |
| load-order swap (requires) | two `:require` lines swapped in a caller's `ns` | **B4** require sort discipline broken 0 → 1 |
| **local-shadow substitution** (Sol r1, PB-FENCE-001) | the local destructured `edit-form` replaced by the moved private Var of the same name | `form-builder-page` → `:changed`; tier `:changed` 6 → 7 |
| **symlink escape** (Sol r1, PB-FENCE-002) | `src/leak.clj` as a symlink to a file outside the repository, defining `escaped-owner` | refused entries 0 → 1, named `symlink (mode 120000) — never followed`; **`escaped-owner` appears nowhere** |
| **external binding macro** (Sol r2, PB-FENCE-005, pair) | `(compojure.core/GET "/probe/:header" [header] header)` in the base, the bound use replaced by `organizer-layout/header` in the candidate | `committee-page` → `:changed`; preserved 103 → 102; `compojure.core/GET` named as a frozen head |
| **`are` argv binding** (Sol r3, PB-FENCE-006, pair) | `(are [header] (= header :probe) :probe)` in the base, the bound use replaced in the candidate. `clojure.test/are` binds the symbols in its argv and sat in the "introduces no binding" table | `committee-page` → `:changed`; preserved 103 → 102; `are` named as a frozen head. **`bin/preservation-tables-test` R3 would now reject that table entry before it could ship** |
| **uppercase require alias** (Sol r3, PB-FENCE-008, pair) | `[compojure.core :as Route]` + `Route/GET`, exploiting that the interop heuristic ran before alias resolution | `committee-page` → `:changed`; preserved 103 → 102; `Route/GET` named |
| **cross-tree kind collision** (Sol r3, PB-FENCE-007, pair) | the same `[ns name]` defined `defmacro` in the base and `defn` in the candidate, so the merged index vouched for a macro call as a function call | `committee-page` → `:changed`; preserved 102 → 101; **`kind_disagreements` 0 → 1** |
| **referred binding macro named for a table entry** (Sol r4, PB-FENCE-009, pair) | a `defmacro testing` added to a namespace both trees already require, referred as `testing`; the base names its local binding, the candidate the relocated Var. The tables matched the bare spelling before reading the file's `ns` form | `committee-page` → `:changed`; preserved 102 → 101; `testing` named as a frozen head. **R5's generated shadow witnesses would now reject that table entry before it could ship** |

Seven of these escaped an earlier build and were repaired in the same session. Each is now permanent.

### The tables self-test

`bin/preservation-tables-test` is the rung that ends the sequence. It reports **PASS**:

```
R1  core-fn-vars: 202 entries
R2  modelled/frozen Vars: 74
R3  claims needing a witness: 37
R3  running 45 executed witnesses
R5  generated shadow witnesses: 276
R4  required replay rows: 7

PASS — every table entry carries its falsifier, including a generated shadow witness.
```

- **R1** resolves all 202 allowlist entries **in their stated namespace** and fails on any macro. That
  resolution *is* the falsifier — a function's arguments are evaluated and cannot bind. It removed `cast`,
  the typo `when-first?`, and `clojure.core//`, whose name cannot be split on `/`.
- **R2** resolves all 74 modelled and frozen Vars in their stated namespace, requires a Var in the
  non-binding table to actually be a macro, and refuses any table collision. It removed `io!`, `gen-class`,
  `import`, `definline`, `definterface` and `clojure.core/deftest` — none of which resolve where the table
  claimed. Dropping an unresolvable entry loses nothing: a head no table names is unmodelled, which freezes
  it just the same.
- **R3** runs 45 **executed** witnesses against the real scope analysis, through a new `--probe-scope`
  entry point on the brief: 31 binding heads must report their binder bound, 4 destructuring shapes must
  too, and 12 macros declared non-binding must report nothing bound.
- **R5** is **generated, one row per table entry** — 276 of them. For every table Var it plants a specimen
  that refers a same-named binding macro from another namespace and requires the head to flip to
  `unmodelled`, with a matching control requiring the head to keep its meaning when nothing shadows it. This
  is the rung that would have caught PB-FENCE-009 before it shipped, and it means a new table entry cannot
  arrive without proof that it speaks for a Var rather than a spelling.
- **R4** requires each of the seven historic false-certification classes to name a planted replay row.

`frozen-heads` needs no witness, and that asymmetry is the design: **freezing is the absence of a claim**, so
anything unproved goes there. `are`, `condp`, `cond->`, `while`, `assert`, `comment`, `time`, `delay`,
`future`, `lazy-seq`, `locking`, `dosync`, `declare`, `defmulti` and `deftest` all moved there in this round.

### The alias-migration specimen and its own planted defect

| change | in-place bodies preserved | undefined-alias sites |
|---|---|---|
| alias migration, clean | 39/103 | **0** |
| alias migration + one site left on the retired alias | 38/102 | **1**, named `file:line` with the undefined prefix |

Most of the 64 bodies the clean run refuses to clear are refused because they call a head the scanner cannot
vouch for. Among the genuinely changed ones is a real finding about my own constructor: the alias rewriter
was a regex without string or comment masking, and it edited the string literals `"exports/sessions.json"`
and `"exports/calendar.ics"` — genuine URL paths, a behavioural break — and one comment.

## 7. Checker wall per change

Every figure is the checker's own `wall_ms`, printed at the foot of each brief.

| change | source files in tree | files parsed | wall |
|---|---|---|---|
| Cell C split | 83 | 39 | 20.2 s |
| Cell B split | 473 | 12 | 11.4 s |
| alias migration (clean / defect) | 472 | 35 | 25.4 / 25.1 s |
| clj-surgeon `571170cc` | 344 | 5 | 9.7 s |
| clj-surgeon `d2c3aa80` | 344 | 12 | 14.8 s |
| each of the 14 replay rows | 83 | 39 | 20.0–20.7 s |
| null change (`d9205abc` against itself) | 62 | **0** | **0.20 s** |
| `bin/preservation-tables-test` (276 resolutions + 45 executed witnesses + 552 generated shadow probes) | — | — | **~11 s** |

The first build took **136 s** on the Cell B specimen because it parsed the whole repository. Two-stage
scoping brought it to 20 s with byte-identical output, and reading blobs from the object database instead of
extracting trees took it to **11.7 s**. None of the three soundness rounds moved the wall measurably: the
scope walk, the per-tree def-head indexes and the table audit are all cheap next to parsing.

The null row is a standing sanity witness and the portfolio's only `clear: true` row: a candidate compared
against itself must parse nothing, move nothing, lose no comment and raise no obligation.

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
  - `8751ed9e` — all four round-1 fence findings repaired. **Sol's round-2 review returned NO-GO on this
    commit** with one blocker, PB-FENCE-005, verdict at
    `/var/tmp/forge/ship/20260909T163340Z-8751ed9e4d4c/verdict-1.md`; it also verified all four round-1
    repairs and the replay's tree-hash provenance.
  - `1cb98b5e` — PB-FENCE-005 repaired: the denylist inverted to an allowlist. **Sol's round-3 review
    returned NO-GO on this commit** with three blockers, PB-FENCE-006/007/008, verdict at
    `/var/tmp/forge/ship/20260909T171132Z-1cb98b5e2a59/verdict-1.md`.
  - `77126c8d` — round-3 repairs and `bin/preservation-tables-test`. **Sol's round-4 review returned NO-GO
    on this commit** with one blocker, PB-FENCE-009, verdict at
    `/var/tmp/forge/ship/20260909T175507Z-77126c8d81a7/verdict-1.md`.
  - **`83c13814`** — PB-FENCE-009 repaired: every table keyed by fully qualified Var, resolution before
    every lookup, and 276 generated shadow witnesses. **This is the tip.**
- **Nothing was pushed.** The branch has no upstream.
- No edits under `src/`, `test/`, or the Makefile. Four new files, all under `bin/`.
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
| **A denylist over syntax is never conservative.** Round 1 froze macros defined in the scanned trees plus any `with-*`/`def*` head — an enumeration of distrusted *spellings*, which one library call defeated. Anyone can define a construct that is not on your list; nobody can add one to the list of constructs you have positively verified. | The rule is inverted: canonicalise only inside heads that are a modelled core form, a non-macro `def`/`defn` findable in the two trees, or a shipped clojure.core **function** allowlist. Everything else freezes, is named, is counted, and blocks certification. Cost stated in §1: 136/141 → 103/141. The permanent `external-macro` row is the witness, and each future allowlist entry must carry its own planted row. |
| **A stated evidence boundary that the implementation does not enforce is worse than none**, because the report repeats it. | The brief reads the object database, never the filesystem; symlinks, non-blobs and oversized blobs are counted as refused entries and named. The permanent `symlink-escape` row is the witness. |
| **A headline a reviewer can consume and stop on defeats a checker whose real value is in section B.** | Every obligation is hoisted above section A into a hard-stop block, the summary carries `clear`, and the exit code is 3 when not clear. The null change is the only clear row in the portfolio. |
| **A builder cannot review his own oracle, and one round of outside review is not enough either.** Round 1 found four findings; the round-2 probe reproduced the same false-certification class one abstraction level out. | Sol's three specimens are permanent replay rows. §9.9 requires the falsifier's specimens be planted by someone who did not build the checker, and §12 now names the next unseen classes to probe rather than declaring the set complete. |
| **Spelling is not identity; resolution precedes every table.** A table keyed by a bare name speaks for whatever that name means in the file being read. A referred macro named `testing` was handed `clojure.test/testing`'s semantics and A certified a changed binding. | Every table is keyed by a fully qualified Var; `head-info` resolves through the file's own `ns` form — refer, alias, definition in the two trees, then the core default, and only when nothing shadows it — before any lookup. The ratchet's R5 generates a shadow witness per table entry, so a new entry cannot arrive without proof that a same-named refer flips it to unmodelled. The permanent `referred-testing` pair row is the witness. |
| **A table entry is a soundness claim, and a claim with no falsifier is how a checker certifies a defect.** Round 3's three blockers were all entries somebody typed and nobody audited: `are` in the non-binding table, a merged def-head index, an interop heuristic ordered before alias resolution. | `bin/preservation-tables-test`: 203 machine-resolved allowlist entries proved non-macro, 48 heads proved special-form-or-Var with no unexamined macro, 45 executed binding witnesses through a `--probe-scope` entry point, and a required replay row per historic failure class. It exits nonzero on any unsupported claim. **`frozen-heads` needs no witness — freezing is the absence of a claim — so anything unproved goes there.** |
| A preregistration whose arithmetic does not close cannot be executed, however good its rules sound. | §9 now carries a specimen ledger whose counts sum, an assignment matrix whose observation count follows from reviewers × specimens, stated per-class denominators, and a co-primary restated as strict dominance because n=2 per class cannot support a rate. |

## 12. What is next

1. **Someone other than me plants the defects**, seals an answer key, and runs §9. Sol's fence review is the
   proof that this matters and not a formality: two of the eight replay rows are his, and one of them broke
   the tool.
2. **Extend the tables test from "every entry has a falsifier" to "every entry's falsifier is adversarial."**
   R5's 276 shadow witnesses are generated, which is the right shape — a new table entry cannot arrive
   without one. R3's 45 binding witnesses are still hand-written by the same person who wrote the table.
   Generate those too, from each Var's published arglist, or have the next reviewer plant them.
3. **Widen the planted-defect set to the classes section D admits it cannot see**: a relocated `defrecord`
   whose generated class package moved, a namespace named as a string in a resource, a `requiring-resolve`
   on a moved owner, and a binding introduced by a macro the scanner *does* model but models wrongly. If a blind reviewer misses those under T and catches them under C, the brief is
   *creating* a blind spot and must be narrowed.
4. **Wire it into the review entrance** so the brief is generated at the `candidate-submitted` boundary and
   arrives with the diff, rather than being run by hand.
4. **Reduce the unmodelled-macro surface deliberately, and measure what each addition buys.** Three repo
   macros currently freeze the bodies that use them, and 19 of the alias-migration specimen's 22
   unpreserved bodies are `with-event`/`with-etag` frozen regions. Modelling a macro's binding form is a
   small change with a measurable return in preserved bodies — and a correctness risk if modelled wrongly,
   so each one needs its own planted-defect row before it ships.
5. **Widen the resolver, because that is where the value went.** 48 heads are unmodelled on Cell C and they
   cost 33 relocated bodies. Three fixable causes, in order of return: (i) definitions the regex resolver
   cannot see — `>defn` from guardrails/malli defines real functions and currently reads as unresolvable;
   (ii) `clojure.string`, `clojure.set` and `clojure.walk`, which are pure-function namespaces whose members
   could join the shipped allowlist; (iii) the repo's own three `with-*` macros, whose binding forms are
   small and modellable. **Each addition is a soundness assertion and must ship with its own planted-defect
   row**, because that is precisely the kind of claim Sol has now falsified twice.
6. **Charge it honestly.** Under Frame 7 the brief's 20 s is inside `L`. It buys review time only if the
   review was actually reading bodies; on a review that was already skipping them it buys nothing, and the
   §9.7 null check is what will say so.
