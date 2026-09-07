# Curtain Call LID gaps — batch 2

Source worktree: `/home/forge/src/cc-lid3`, branch `astra/lid-batch-2`, base `e36d9a73`.
Seven product registry rows plus one row in the owning canonical-agenda specs make
eight product promises. A separate `LID-TEST-TAG-001` registry row owns the new
contract guard. Every product row has one EARS outcome, at least three misreadings,
boundaries, a source-site `INTENT` marker and an `INTENT-TEST` marker immediately
before the existing deftest. Production behavior is unchanged; production source
changes are linkage comments plus the pure traceability scanner.

The two discretionary promises are agent content trust and block room occupancy:
one is an external trust boundary, the other drives conflict reporting across
mixed agenda occupants. Telemetry privacy remains explicit unlinked debt.

## Eight linked outcomes

All source paths below are relative to `src/cfp_scheduler_killer/`; tests are
relative to `test/cfp_scheduler_killer/`.

| ID | Source sites | Existing witness | Planted failures | Restored |
| --- | --- | --- | ---: | --- |
| `CSV-SAFE-001` | `review_scores_csv.clj` | `review_scores_csv_test.clj` / `csv-export-neutralizes-spreadsheet-formulas-test` | 16 | green |
| `EXPORT-RECEIPT-001` | `handlers/exports.clj; views/integrations.clj` | `exports_test.clj` / `taking-an-export-leaves-a-visible-receipt-test` | 3 | green |
| `TASK-REFUSAL-001` | `handlers/speaker_tasks.clj` | `content_management_test.clj` / `refused-task-creation-keeps-the-organizers-work` | 6 | green |
| `BLOB-DURABILITY-001` | `io/blob.clj` | `blob_durability_test.clj` / `shared-store-with-no-bucket-is-refused` | 1 | green |
| `REV-STEWARDSHIP-001` | `views/review.clj` | `board_test.clj` / `manage-speaker-is-never-offered-to-a-reviewer-test` | 3 | green |
| `TELEM-ASYNC-001` | `telemetry.clj` | `telemetry_test.clj` / `wrap-telemetry-returns-without-touching-the-network-test` | 4 | green |
| `AGENT-TRUST-001` | `exports.clj; agent/mcp.clj` | `agent_test.clj` / `agent-surfaces-say-speaker-text-is-data-not-instructions` | 5 | green |
| `AGENDA-ROOM-001` | `schedule.clj` | `schedule_test.clj` / `block-room-conflict-test` | 3 | green |

- **CSV-SAFE-001:** When either review CSV renderer exports a text cell beginning with equals, plus, minus, at, tab, or carriage return, the rendered cell shall contain an apostrophe prefix inside any CSV quoting.
- **EXPORT-RECEIPT-001:** When a chair downloads review-results.csv successfully, the Recent exports page shall display a receipt identifying that response by filename, UTF-8 byte count, and SHA-256 checksum.
- **TASK-REFUSAL-001:** When general-task creation fails form validation, the handler shall return a 422 deliverables form preserving the supported draft values alongside the refusal reason.
- **BLOB-DURABILITY-001:** When an upload targets a shared store without a configured bucket, the blob port shall refuse before any local write with a blob/durability-downgrade-refused error identifying the storage key.
- **REV-STEWARDSHIP-001:** When a reviewer opens an accepted submission detail, the page shall omit speaker-management affordances regardless of presenter visibility policy.
- **TELEM-ASYNC-001:** When request telemetry is enabled, the request wrapper shall complete its response or rethrow its handler error without invoking the telemetry sink, even when the queue is full.
- **AGENT-TRUST-001:** When an agent reads event llms.txt or MCP initialization instructions, the artifact shall label speaker-authored text as data rather than instructions, with suspected instruction attempts directed to the operator as findings.
- **AGENDA-ROOM-001:** When two scheduled occupants overlap in one named room on the same day, the conflict report shall identify their room double-booking regardless of whether either occupant is a block.

## Assertions read and strengthened

- CSV: retained helper examples; added literal encoded-cell expectations through
  BOTH real renderers, with actual submission title, speaker name and email. Covers
  all six leading characters, RFC-4180 quotes, comma, ordinary text, and nil cells.
- Receipt: computes UTF-8 byte count and SHA-256 independently from the returned
  HTTP body containing a non-ASCII session title, checks a new projected receipt, and requires filename, count and hash
  to appear in the same Recent exports table row. Full hash is checked in its title.
- Task refusal: a seven-case matrix reaches all five form-validation branches
  (including both missing and malformed due date), reads actual form controls via
  Jsoup, preserves supported values and selected speaker IDs, and compares the event
  log before/after. Unknown/cross-event speaker 404 is a separate authorization boundary.
- Durability: observes that the typed refusal happens before any local adapter
  write. Existing private-store and configured-bucket controls remain; the local
  fixture now uses the configured JVM temp directory and restores its system property.
- Reviewer: reads actual management hrefs for chair/reviewer across hidden, visible,
  and reveal-after-vote policies; retains the explicit Anonymous speaker check.
- Async: a 2x2 full/empty queue and success/throw matrix uses a bounded queue with
  daemon disabled; checks completion, exact response/error identity, zero sink calls,
  queue occupancy and drop metrics. The 2-second deadline detects blocking, not speed.
- Agent: obtains instructions from public MCP `handle-message` initialize output,
  instead of calling the private helper; keeps both llms and MCP trust statements.
- Room occupancy: retains block/block, block/talk, unroomed/different-room controls;
  adds partial overlap, adjacent half-open intervals and different-day cases through
  the public `schedule/conflicts` report.

## Red-first narrowing evidence

`plants.py` applies all eight independent source plants plus the scanner-disabled
plant, first runs the ORIGINAL HEAD versions of the eight witnesses, then restores
the strengthened test versions and reruns, then restores every source file from a
byte-preserving backup in `finally` and reruns green. This shows that the old tests
really survived the obvious narrowings, rather than merely claiming they would.
All plants are absent from the final source tree. The script and exact executed
patch are in this report directory.

```text
plants-old-witnesses:
8 tests, 65 assertions, 0 failures.
```

```text
plants-strengthened-red:
22 tests, 788 assertions, 51 failures.
```

```text
plants-restored-green:
22 tests, 788 assertions, 0 failures.
```

Exact planted source diffs (relative to the strengthened, clean source):

```diff
--- src/cfp_scheduler_killer/exports.clj
+++ src/cfp_scheduler_killer/exports.clj
@@ -53,7 +53,7 @@
   (let [bytes (.getBytes (str body) StandardCharsets/UTF_8)
         row {:id (store/new-id) :event-id (:id event) :kind kind :filename filename
              :person-id person-id :row-count (:submissions counts) :counts counts
-             :byte-count (alength bytes) :sha256 (sha256 body) :created-at (store/now-iso)}]
+             :byte-count (count body) :sha256 (sha256 body) :created-at (store/now-iso)}]
     (store/append! {:type "export.generated" :event-id (:id event) :actor actor :payload row})
     (log/info :export-generated :event-id (:id event) :kind kind :rows (:row-count row))
     row))
--- src/cfp_scheduler_killer/review_scores_csv.clj
+++ src/cfp_scheduler_killer/review_scores_csv.clj
@@ -119,7 +119,7 @@
   "RFC 4180-style CSV with CRLF line endings and a stable final newline."
   [event]
   (str (->> (cons header (rows event))
-            (map #(str/join "," (map csv-cell %)))
+            (map #(str/join "," (map str %)))
             (str/join "\r\n"))
        "\r\n"))
 
@@ -220,6 +220,6 @@
   "RFC 4180 CSV with one submission per row and nested review data in JSON cells."
   [event]
   (str (->> (cons review-results-header (review-results-rows event))
-            (map #(str/join "," (map csv-cell %)))
+            (map #(str/join "," (map str %)))
             (str/join "\r\n"))
        "\r\n"))
--- src/cfp_scheduler_killer/views/integrations.clj
+++ src/cfp_scheduler_killer/views/integrations.clj
@@ -209,8 +209,8 @@
                     [:tr [:td kind] [:td [:code filename]] [:td row-count]
                      [:td (when byte-count (str byte-count " bytes"))]
                      [:td (when sha256
-                            [:code.export-checksum {:title sha256}
-                             (subs (str sha256) 0 12)])]
+                            [:code.export-checksum {:title "0000000000000000000000000000000000000000000000000000000000000000"}
+                             "000000000000"])]
                      [:td created-at]])]]
          [:p.field-hint "No exports taken yet — every file you download is recorded here, with its row count and its checksum."])]]
 
--- src/cfp_scheduler_killer/handlers/speaker_tasks.clj
+++ src/cfp_scheduler_killer/handlers/speaker_tasks.clj
@@ -209,7 +209,8 @@
                       :task-name (some-> (get-in req [:params :task-name]) str)
                       :due-on (some-> (get-in req [:params :due-on]) str str/trim)
                       :task-kind (some-> (get-in req [:params :task-kind]) str str/trim)
-                      :instructions (some-> (get-in req [:params :instructions]) str)
+                      :instructions (when (= message "Tell the speaker what file to upload.")
+                                      (some-> (get-in req [:params :instructions]) str))
                       :submission-ids (mapv str (param-values
                                                  (get-in req [:params :submission-ids])))}}))
       (assoc :status 422)))
--- src/cfp_scheduler_killer/io/blob.clj
+++ src/cfp_scheduler_killer/io/blob.clj
@@ -45,6 +45,7 @@
       ;; Cloud Run local disk is also ephemeral and per-instance, so the same
       ;; write loses the bytes on the next revision.
       (when (shared-store?)
+        (local/put! (upload-root) source storage-key)
         (throw (ex-info
                 (str "Refusing to store an upload on local disk while writing "
                      "to the shared store: the file fact would be visible to "
--- src/cfp_scheduler_killer/views/review.clj
+++ src/cfp_scheduler_killer/views/review.clj
@@ -576,10 +576,10 @@
                               ;; The /manage route is declared :organizer and
                               ;; already refuses them — this removes the invitation.
                               ;; INTENT: REV-STEWARDSHIP-001
-                              (when (and chair? (= "Accepted" (:status row)))
+                              (when (= "Accepted" (:status row))
                                 [:a.ui.button {:href (str "/events/" (:slug event)
                                                           "/submissions/" (:id row) "/manage")}
-                                 "Manage speaker"]))
+                                 (if chair? "Manage speaker" "Steward")]))
 
      (list
       (content-status-control event row person)
--- src/cfp_scheduler_killer/telemetry.clj
+++ src/cfp_scheduler_killer/telemetry.clj
@@ -457,7 +457,8 @@
                       :route (:route event) :method (:method event)
                       :status 500 :duration-ms duration
                       :event-slug (:event-slug event) :ua-class (:ua-class event))
-            (enqueue! event))
+            (enqueue! event)
+            (flush-once!))
           (throw t))))))
 
 (defn wrap-route-template
--- src/cfp_scheduler_killer/agent/mcp.clj
+++ src/cfp_scheduler_killer/agent/mcp.clj
@@ -67,7 +67,7 @@
         method (:method message)]
     (case method
       "initialize"
-      (result-response id (server-capabilities))
+      (result-response id (dissoc (server-capabilities) "instructions"))
 
       "server/discover"
       (result-response id (server-capabilities))
--- src/cfp_scheduler_killer/schedule.clj
+++ src/cfp_scheduler_killer/schedule.clj
@@ -349,7 +349,8 @@
         ;; occupants cannot double-book a room; that is the whole point of letting
         ;; a placement have no room.
       (for [[a b] occupant-pairs
-            :when (and (:room-id a) (= (:room-id a) (:room-id b)) (overlap? a b))]
+            :when (and (:room-id a) (= (:room-id a) (:room-id b))
+                       (= (:start a) (:start b)) (overlap? a b))]
         {:type :room
          :severity :high
          :message (str "Room double-booked: " (room-name event-id (:room-id a))
--- src/cfp_scheduler_killer/intent_traceability.clj
+++ src/cfp_scheduler_killer/intent_traceability.clj
@@ -10,7 +10,7 @@
   [text]
   (into []
         (keep-indexed (fn [i line]
-                        (when (re-find #"^[ \t]*;;[ \t]*INTENT:" line)
+                        (when (re-find #"(?!)" line)
                           {:line (inc i) :text line})))
         (str/split-lines text)))
```

First rejecting assertion per strengthened product witness:

**CSV-SAFE-001 — Bypass csv-cell in both public renderers; the private helper remains correct.**

```text
FAIL in cfp-scheduler-killer.review-scores-csv-test/csv-export-neutralizes-spreadsheet-formulas-test (review_scores_csv_test.clj:141)
both public renderers neutralize actual title and presenter cells
rendered title/name/email cells: "=1+1"
expected: (str/includes? (render {:id "event-1"}) (str "\r\ntalk-1," (if (nil? input) "(untitled)" encoded) "," encoded "," encoded ",Pending,"))
  actual: (not (str/includes? "submission_id,title,speaker_name,speaker_email,decision_status,content_status,priority,review_count,mean_stars,rated,reviewer_name,reviewer_id,stars,rated_at\r\ntalk-1,=1+1,=1+1,=1+1,Pending,Draft,false,0,,false,,,,\r\n" "\r\ntalk-1,'=1+1,'=1+1,'=1+1,Pending,"))
```

**EXPORT-RECEIPT-001 — Count characters instead of UTF-8 bytes; replace the rendered checksum with zeroes while retaining the Checksum heading.**

```text
FAIL in cfp-scheduler-killer.exports-test/taking-an-export-leaves-a-visible-receipt-test (exports_test.clj:1524)
the receipt's exact bytes and hash belong to the new response
Expected:
  {:byte-count 828,
   :filename "export-test-review-results.csv",
   :sha256 "2d7c0b08ab3d48e8df4c84593dfa1ea63f4e918b67ed3d2f453b8a6e68411b6c"}
Actual:
  {:byte-count -828 +820,
   :filename "export-test-review-results.csv",
   :sha256 "2d7c0b08ab3d48e8df4c84593dfa1ea63f4e918b67ed3d2f453b8a6e68411b6c"}
```

**TASK-REFUSAL-001 — Preserve instructions only on the missing-instructions branch, losing them on other refusals.**

```text
FAIL in cfp-scheduler-killer.content-management-test/refused-task-creation-keeps-the-organizers-work (content_management_test.clj:796)
each validation refusal preserves all other supported form values
{"submission-ids" []} preserved instructions
Expected:
  "Keep these instructions"
Actual:
  -"Keep these instructions" +""
```

**BLOB-DURABILITY-001 — Write locally before throwing the existing typed durability refusal.**

```text
FAIL in cfp-scheduler-killer.blob-durability-test/shared-store-with-no-bucket-is-refused (blob_durability_test.clj:47)
typed refusal precedes any local write
throwing after writing is still a durability downgrade
expected: (empty? (clojure.core/deref writes))
  actual: (not (empty? [("/var/tmp/forge/lid-batch/blob-uploads" #object[java.io.BufferedInputStream 0x1423f969 "java.io.BufferedInputStream@1423f969"] "evt-1/file-1/version-1")]))
```

**REV-STEWARDSHIP-001 — Offer the management href to reviewers under a different label, Steward.**

```text
FAIL in cfp-scheduler-killer.board-test/manage-speaker-is-never-offered-to-a-reviewer-test (board_test.clj:1283)
the management href is chair-only across visibility policies
hidden chair? false
Expected:
  false
Actual:
  -false +true
```

**TELEM-ASYNC-001 — Flush synchronously only on the handler-exception branch.**

```text
FAIL in cfp-scheduler-killer.telemetry-test/wrap-telemetry-returns-without-touching-the-network-test (telemetry_test.clj:314)
errors and full queues also stay off the sink
full? false throws? true
expected: (zero? (clojure.core/deref calls))
  actual: (not (zero? 1))
```

**AGENT-TRUST-001 — Drop instructions from initialize while retaining the private capabilities helper.**

```text
FAIL in cfp-scheduler-killer.agent-test/agent-surfaces-say-speaker-text-is-data-not-instructions (agent_test.clj:478)
the MCP posture string states it too — that is what a client reads
expected: (str/includes? instructions "SPEAKER-AUTHORED")
  actual: (not (str/includes? "" "SPEAKER-AUTHORED"))
```

**AGENDA-ROOM-001 — Require identical start times, missing partially overlapping blocks.**

```text
FAIL in cfp-scheduler-killer.schedule-test/block-room-conflict-test (schedule_test.clj:1031)
partial overlap, adjacency, and day boundaries
different start times still overlap
Expected:
  1
Actual:
  -1 +0
```

## Prose comment ratchet and remaining debt

The gate now refuses anchored `;; INTENT:` comments under test/ (prose OR an ID
in the wrong dialect), reporting file and line. `INTENT-TEST` and `INTENT-NOTE`
remain disjoint. The real-gate injection and nine grammar cases go red when the
scanner is disabled. Documentation strings and incidental quoted examples are
not counted as actual comment markers.

The ledger's “25 / remaining 17” is not an exact census at this batch's base.
At HEAD there are **22 actual prose comment lines**, **one durability prose
namespace docstring**, and **one already-ID-bearing misplaced comment**
(`AGENT-URL-003` in a test helper). That is **23 distinct prose promises**;
the ledger table itself names 23. Eight are now linked, leaving **15 distinct
prose promises**. All retained comment prose is spelled `INTENT-NOTE`, including
explanations accompanying the eight linked witnesses. The existing ID-bearing
helper comment is also a note; its existing registered witness is untouched.
Quoted tag examples in the contract are examples, not missing promises.

The following 15 promises remain UNLINKED by this batch (line numbers are from
batch 1, preserved in `original-prose.txt`). Renaming them is not claiming coverage:

- test/cfp_scheduler_killer/reviewer_queue_views_test.clj:80: → INTENT-NOTE: a queue row must say whether THIS reviewer has weighed in. The row's
- test/cfp_scheduler_killer/board_test.clj:1188: → INTENT-NOTE: bulk export is a chair act, and the reviewer board must not offer it.
- test/cfp_scheduler_killer/telemetry_test.clj:177: → INTENT-NOTE: the sink mapping is PURE and privacy-bounded. The previous sink
- test/cfp_scheduler_killer/reviews_test.clj:726: → INTENT-NOTE: the Manifesto and the board cannot drift apart. refusals.clj has
- test/cfp_scheduler_killer/forms_test.clj:586: → INTENT-NOTE: a show-when rule keys on the option's TEXT. Rename an option on the
- test/cfp_scheduler_killer/schedule_test.clj:578: → INTENT-NOTE: the schedule opens on ROUGH BLOCKING (Gene, 2026-08-20).
- test/cfp_scheduler_killer/public_widgets_test.clj:864: → INTENT-NOTE: archiving is a fact, not a deletion — so the permalink must not rot,
- test/cfp_scheduler_killer/public_widgets_test.clj:904: → INTENT-NOTE: the program card moved from card.png to card.jpg, but a social
- test/cfp_scheduler_killer/public_widgets_test.clj:936: → INTENT-NOTE: /events/:slug/exports/agenda-embedded.html is a CONTRACT with
- test/cfp_scheduler_killer/public_widgets_test.clj:1155: → INTENT-NOTE: the sales page wants to bold the ORG the way our own speaker
- test/cfp_scheduler_killer/public_widgets_test.clj:1194: → INTENT-NOTE: a consumer deep-links a name to the speaker page. That is only
- test/cfp_scheduler_killer/public_widgets_test.clj:1249: → INTENT-NOTE: the one-name rule has to survive a session billed under TWO
- test/cfp_scheduler_killer/content_management_test.clj:636: → INTENT-NOTE: the editorial approval gate must never take a program off the public
- test/cfp_scheduler_killer/routes_contract_test.clj:395: → INTENT-NOTE: llms.txt is a PROMISE TO AN AGENT, and an unpinned promise is the
- test/cfp_scheduler_killer/exports_test.clj:1502: → INTENT-NOTE: a speaker who adds the CFP deadline to their calendar must be able

## Full required entry and lint

Every command inherits `TMPDIR`, `TMP`, `TEMP` and `-Djava.io.tmpdir` set to
`/var/tmp/forge/lid-batch/`; blob test storage is rooted there as well. No live
server or external data service was started. The Makefile's worktree test uses
only disposable local Git fixtures under that temp root (its local bare-fixture
push is not a project push).

### make-runtests-once — exit 0

```text
oracle: test/oracles/admin_event_slack_contract.pl
% All 9 tests passed in 0.011 seconds (0.011 cpu)
oracle: test/oracles/review_board_contract.pl
% All 6 tests passed in 0.009 seconds (0.009 cpu)
oracle: test/oracles/rough_drag_projection.pl
% All 5 tests passed in 0.009 seconds (0.009 cpu)
oracle: test/oracles/speaker_profile_edit_contract.pl
% All 8 tests passed in 0.009 seconds (0.009 cpu)
oracle: test/oracles/talk_agenda_contract.pl
% All 6 tests passed in 0.011 seconds (0.011 cpu)
# tests 18
# fail 0
PASS: fetched origin/main beats stale local main and refusals hold
```

```text
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
1027 tests, 12526 assertions, 0 failures.
```
### kaocha-unit — exit 0

```text
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
WARNING: reset! already refers to: #'clojure.core/reset! in namespace: cfp-scheduler-killer.replay, being replaced by: #'cfp-scheduler-killer.replay/reset!
1027 tests, 12526 assertions, 0 failures.
```

### Changed-file kondo

```text
test/cfp_scheduler_killer/public_widgets_test.clj:709:14: info: Single argument to str already is a string
test/cfp_scheduler_killer/public_widgets_test.clj:727:24: info: Single argument to str already is a string
test/cfp_scheduler_killer/public_widgets_test.clj:729:23: info: Single argument to str already is a string
test/cfp_scheduler_killer/public_widgets_test.clj:748:23: warning: unused binding banking
test/cfp_scheduler_killer/public_widgets_test.clj:748:38: warning: unused binding declined
test/cfp_scheduler_killer/reviewer_queue_views_test.clj:25:4: info: Single argument to str already is a string
test/cfp_scheduler_killer/reviews_test.clj:12:5: warning: namespace cfp-scheduler-killer.seed is required but never used
test/cfp_scheduler_killer/reviews_test.clj:240:9: warning: unused binding zero
test/cfp_scheduler_killer/reviews_test.clj:546:23: info: Single argument to str already is a string
linting took 787ms, errors: 0, warnings: 22
```

```text
Baseline findings: 22
Current findings: 22
New findings: {}
Retired findings: {}
```

Both current and baseline kondo exit 2 due to the identical 22 pre-existing
warnings; both have zero errors. Baseline files were read with `git show HEAD:path`
into the allowed temp root and linted with the same project configuration through
`~/bin/clj-kondo`. No new warning or error is introduced, and unrelated baseline
cleanup was not mixed into the eight promises.

## Changed files


```text
docs/high-level-design.md
docs/intent/canonical-agenda/canonical-agenda-design.md
docs/intent/canonical-agenda/canonical-agenda-specs.md
docs/intent/registry.edn
docs/intent/spec-traceability.md
src/cfp_scheduler_killer/agent/mcp.clj
src/cfp_scheduler_killer/exports.clj
src/cfp_scheduler_killer/handlers/exports.clj
src/cfp_scheduler_killer/handlers/speaker_tasks.clj
src/cfp_scheduler_killer/intent_traceability.clj
src/cfp_scheduler_killer/io/blob.clj
src/cfp_scheduler_killer/review_scores_csv.clj
src/cfp_scheduler_killer/schedule.clj
src/cfp_scheduler_killer/telemetry.clj
src/cfp_scheduler_killer/views/integrations.clj
src/cfp_scheduler_killer/views/review.clj
test/cfp_scheduler_killer/agent_test.clj
test/cfp_scheduler_killer/blob_durability_test.clj
test/cfp_scheduler_killer/board_test.clj
test/cfp_scheduler_killer/content_management_test.clj
test/cfp_scheduler_killer/exports_test.clj
test/cfp_scheduler_killer/forms_test.clj
test/cfp_scheduler_killer/intent_contract_test.clj
test/cfp_scheduler_killer/public_widgets_test.clj
test/cfp_scheduler_killer/review_scores_csv_test.clj
test/cfp_scheduler_killer/reviewer_queue_views_test.clj
test/cfp_scheduler_killer/reviews_test.clj
test/cfp_scheduler_killer/routes_contract_test.clj
test/cfp_scheduler_killer/schedule_test.clj
test/cfp_scheduler_killer/telemetry_test.clj
```

## Doubts and limits

- Linkage proves witnesses exist; it does not prove the intent is the right product
  policy. The existing product behavior remains the authority for this linkage batch.
- CSV checks cover the named six leaders and literal projection bytes, not a live
  spreadsheet execution or every spreadsheet's configurable import behavior.
- Export receipt proves response generation and displayed metadata, not that a
  browser saved the download. Other export formats remain outside this row.
- Task validation preserves supported fields; unknown action values are not added
  to the select menu. Cross-event or missing speakers remain the separate 404 fence.
- Telemetry's timed check detects queue blocking; it is not a latency benchmark or
  a test of production BigQuery. Agent trust labeling is not model compliance.
- Kondo's baseline 22 warnings remain. `bd prime` and search ran, but `bd create`
  refused because this worktree database lacks `issue_prefix`; no task was created
  or claimed and no database reinitialization was attempted.
- The project names `linked-intent-dev`, which is not installed in the discovered
  skill directories. The audit's available `linked-intent-testing/SKILL.md` and the
  user's explicit thin-linkage workflow were followed, with current HLD/LLD intent,
  EARS, edge cases, tests, then code linkage and the contract guard.
- No subagents, push, deployment, fleet/runtime directories, forbidden ports,
  `~/acid`, chain scripts, cohort locks, or GO files were used. The pre-existing
  `.codex/config.toml` deletion and disabled-file addition are excluded from staging.

## Commit and final state

```text
Author: forge-anvil <forge-anvil@anvil>
Commit: 450dc8faaa42eb035ec67a4553ff447c5d04e602

Link eight LID promises and reject prose test intent tags

Register CSV safety, export receipts, task refusal, blob durability,
reviewer stewardship, async telemetry and agent content trust. Link room
occupancy in the owning canonical-agenda specification. Strengthen each
existing witness and prove the original tests miss planted narrowings.

Reserve test INTENT-TEST comments for links and INTENT-NOTE for prose;
reject the source-only INTENT spelling in the normal test gate.

Co-Authored-By: Gene Kim <genek@itrevolution.com>
Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
```

Final worktree status (only pre-existing local configuration changes remain):

```text
 D .codex/config.toml
?? .codex/config.toml.disabled-by-sol-yolo
```

No project push was performed.
