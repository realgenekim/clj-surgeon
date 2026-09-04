## NO-GO

1. **Checkout and review provenance are established.** The checkout is exactly `a0b0520`, remained clean, and the twelve requested commits are present. The undated round-one path named in the request was absent both in the checkout and at the literal fallback, so I read the dated round-one verdict at `/home/forge/src/clj-surgeon/docs/observations/2026-09-03-study-ops-o2-review-opus.md`, then the twelve diffs and `docs/observations/2026-09-04-o2r2-text-carries-the-receipt.md`. Relevant locations: `docs/observations/2026-09-04-o2r2-text-carries-the-receipt.md:1`, `/home/forge/src/clj-surgeon/docs/observations/2026-09-03-study-ops-o2-review-opus.md:1`.

   Exact command:

   ```sh
   mkdir -p /tmp/o2r2-fx && git rev-parse HEAD && git status --short && git log --oneline 26e4810..a0b0520
   ```

   Verbatim output:

   ```text
   a0b052061499daa2a4f729f285170f9e8981f2f2
   a0b05206 docs: O2 round 2 observation — measured before/after per mode, and what this round does not establish
   e9d2b169 study-ops: O2r2 (item 7) — the class ratchet: a new mode or refusal cannot ship text-blind
   127e96c7 study-ops: O2r2 (item 6) — rendered rows equal receipt rows, in order, and a mutation says so
   8ee8ef25 study-ops: O2r2 GREEN (item 5) — a session is (root × client run), and the entrances name it
   bc38e2bf study-ops: O2r2 RED (item 5) — session.start cannot tell two client runs on one root apart
   395361e8 study-ops: O2r2 GREEN (item 4) — one refusal renderer, and it names the cause
   97635d2d study-ops: O2r2 RED (item 4) — a refusal names a list it does not print, and never prints its cause
   1a146a10 study-ops: O2r2 (item 3) — match and xray evidence is pinned, and abridgement can never read as terminal
   4fa5e89b study-ops: O2r2 GREEN (item 2) — every mode's text carries its rows
   71cfa532 study-ops: O2r2 RED (item 2) — six modes render a row COUNT where the rows are
   9e7ab5c9 study-ops: O2r2 GREEN (item 1) — the public MCP result is bounded, text block included
   b44a26f8 study-ops: O2r2 RED (item 1) — the ls-tree public result crosses its own 32 KB budget unenforced
   ```

   Exact fallback-resolution command:

   ```sh
   if test -f docs/observations/study-ops-o2-review-opus.md; then printf '%s\n' docs/observations/study-ops-o2-review-opus.md; elif test -f /home/forge/src/clj-surgeon/docs/observations/study-ops-o2-review-opus.md; then printf '%s\n' /home/forge/src/clj-surgeon/docs/observations/study-ops-o2-review-opus.md; else printf '%s\n' /home/forge/src/clj-surgeon/docs/observations/2026-09-03-study-ops-o2-review-opus.md; fi; echo EXIT_CODE=$?
   ```

   Verbatim output:

   ```text
   /home/forge/src/clj-surgeon/docs/observations/2026-09-03-study-ops-o2-review-opus.md
   EXIT_CODE=0
   ```

2. **BLOCKER — `fit-public-result` does not implement the stated “truncate text first; refuse only if structured content alone is too large” contract, and two renderers can falsely read as terminal.** `src/clj_surgeon/mcp_inspect_tool.clj:1798-1808` reserves 1,024 bytes and says the refusal is reached only for structured-only overshoot; `src/clj_surgeon/mcp_inspect_tool.clj:1844-1863` instead refuses after four unsuccessful allowance halvings. `src/clj_surgeon/mcp_inspect.clj:1270-1288` counts the first row as shown even when it silently omits that row's body. `src/clj_surgeon/mcp_inspect_tool.clj:1043-1055` independently prints both an abridgement warning and `complete tree · read_complete=true`.

   The exact boundary is inclusive and works. At one byte over, however, structured content is only 32,558 bytes—below 32,768—yet the result is a typed refusal. A single 10,000-character form body is absent from text, no abridgement is declared, and the summary claims terminal evidence. On a 32-result batch, a structured receipt of 31,549 bytes is refused because the 512-character per-result floor (`src/clj_surgeon/mcp_inspect.clj:1101-1104,1331-1334`) prevents fitting the text.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r2-fx/adversarial_probe.clj
   ```

   Relevant verbatim output:

   ```text
   == BOUNDS ==
   exact_raw_bytes= 32768 exact_unchanged= true exact_ok= true
   plus_one_raw_bytes= 32769 plus_one_structured_bytes= 32558 plus_one_fit_ok= false plus_one_type= inspect-output-limit plus_one_fit_bytes= 968
   single_row_raw_public= 10731 fit_ok= true fit_public= 10731
   single_row_source_in_text= false abridged_notice= false terminal_claim= true
   inspect_clojure |   1 request · 1 file · 1 form |  | ✓ all requests resolved | ✓ ordered snapshot | ✓ hashes attached | ✓ terminal evidence · read_complete=true · next action none | 
   == 32-RESULT FLOOR ==
   payload=100 raw_ok=true structured=31549 public=37894 text_chars=6100 fit_ok=false fit_type=inspect-output-limit fit_public=968
   payload=200 raw_ok=true structured=34748 public=44293 text_chars=9300 fit_ok=false fit_type=inspect-output-limit fit_public=968
   payload=300 raw_ok=true structured=37949 public=50695 text_chars=12501 fit_ok=false fit_type=inspect-output-limit fit_public=968
   payload=400 raw_ok=true structured=41149 public=57095 text_chars=15701 fit_ok=false fit_type=inspect-output-limit fit_public=968
   ```

   Exact command for the independent `ls-tree` terminal contradiction:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r2-fx/terminal_probe.clj; rc=$?; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   fit_ok= true text_evidence_limit= 10254 abridged_notice= true complete_claim= true
   ! text abridged · 97 of 200 rows rendered · the complete receipt is in structuredContent
   ✓ complete tree · read_complete=true
   EXIT_CODE=0
   ```

   This violates checked requirements `MCP-OP-STUDY-040` and `MCP-OP-STUDY-041` at `docs/intent/study-ops/study-ops-specs.md:98-100`.

3. **BLOCKER — the requested strict criterion `content[0].text ⊇ every structuredContent leaf` fails in every mode, and even row-local leaf coverage fails for `forms`, `outline`, `match`, and `ls-deps`.** The implementation at `src/clj_surgeon/mcp_inspect.clj:1136-1200` deliberately projects selected row fields, rather than rendering all receipt fields. This is enough for the narrower row strings asserted by `MCP-OP-STUDY-041`, but it does not meet this review's title/acceptance test. `xray`, `deps`, `topo`, and `ls-extract` passed the row-local leaf check; all still omitted top-level/result metadata. `ls-tree` was public-fit abridged, so its complete `tree` value was not in text.

   Exact command (the probe JSON-round-trips the result, recursively enumerates every leaf, and tests each leaf value against the text):

   ```sh
   sed -n '/== MODE LEAF DIFFS ==/,/== NEXT CALL ==/p' /tmp/o2r2-fx/adversarial_probe.latest | rg '^==|^[a-z]|ROW-MISSING' | sed -n '1,90p'; rc=${PIPESTATUS[0]}; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   == MODE LEAF DIFFS ==
   forms ok=true public=1901 all_missing=19 row_missing=12
     ROW-MISSING [0 :source_anchor :file] = "src/clj_surgeon/analyze.clj"
     ROW-MISSING [0 :source_anchor :source_sha256] = "f75716c5d5948b873e5023a8bfe04db127a6c94a0f28befd6a4803b569aab621"
     ROW-MISSING [0 :source_anchor :range :start :line] = 36
     ROW-MISSING [0 :source_anchor :range :end :line] = 38
     ROW-MISSING [0 :source_anchor :range :end :character] = 52
     ROW-MISSING [0 :source_anchor :selection_range :start :line] = 36
     ROW-MISSING [0 :source_anchor :selection_range :end :line] = 36
     ROW-MISSING [0 :source_anchor :selection_range :end :character] = 19
     ROW-MISSING [0 :hash] = "8ed11d9afbd34c30e76dd19bec47607b492772c7278082efcba32a844f11d9b2"
     ROW-MISSING [0 :file_hash] = "f75716c5d5948b873e5023a8bfe04db127a6c94a0f28befd6a4803b569aab621"
     ROW-MISSING [0 :file] = "src/clj_surgeon/analyze.clj"
     ROW-MISSING [0 :platforms 0] = "clj"
   outline ok=true public=5598 all_missing=44 row_missing=28
     ROW-MISSING [0 :platforms 0] = "clj"
     ROW-MISSING [1 :platforms 0] = "clj"
     ROW-MISSING [2 :platforms 0] = "clj"
     ROW-MISSING [3 :platforms 0] = "clj"
     ROW-MISSING [4 :platforms 0] = "clj"
     ROW-MISSING [5 :platforms 0] = "clj"
     ROW-MISSING [6 :platforms 0] = "clj"
     ROW-MISSING [7 :platforms 0] = "clj"
     ROW-MISSING [8 :platforms 0] = "clj"
     ROW-MISSING [9 :platforms 0] = "clj"
     ROW-MISSING [10 :platforms 0] = "clj"
     ROW-MISSING [11 :platforms 0] = "clj"
   match ok=true public=10065 all_missing=29 row_missing=20
     ROW-MISSING [0 :address :preorder] = 52
     ROW-MISSING [0 :hash] = "8ed11d9afbd34c30e76dd19bec47607b492772c7278082efcba32a844f11d9b2"
     ROW-MISSING [0 :file_hash] = "f75716c5d5948b873e5023a8bfe04db127a6c94a0f28befd6a4803b569aab621"
     ROW-MISSING [1 :address :preorder] = 76
     ROW-MISSING [1 :hash] = "d9202bd49dba1ce7a29f5a08a0bd63461b7d54211863edb0684f9751cd223732"
     ROW-MISSING [1 :file_hash] = "f75716c5d5948b873e5023a8bfe04db127a6c94a0f28befd6a4803b569aab621"
     ROW-MISSING [2 :hash] = "923e9cce4f87f05edf032547b23efe9bab417cd90ac019b329b28dc3e83ec421"
     ROW-MISSING [2 :file_hash] = "f75716c5d5948b873e5023a8bfe04db127a6c94a0f28befd6a4803b569aab621"
     ROW-MISSING [3 :address :preorder] = 479
     ROW-MISSING [3 :hash] = "5ffcf2d844321d4a47ac5539d87cd9f9f26e7b3766c7848087d661de6c452060"
     ROW-MISSING [3 :file_hash] = "f75716c5d5948b873e5023a8bfe04db127a6c94a0f28befd6a4803b569aab621"
     ROW-MISSING [4 :address :preorder] = 582
   xray ok=true public=1635 all_missing=26 row_missing=0
   deps ok=true public=5389 all_missing=10 row_missing=0
   topo ok=true public=2538 all_missing=10 row_missing=0
   ls-deps ok=true public=5005 all_missing=28 row_missing=18
     ROW-MISSING [:leaf?] = false
     ROW-MISSING [:deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 0 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 0 :deps 0 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 2 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 2 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 2 :deps 0 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 2 :deps 0 :deps 0 :deps 0 :leaf?] = false
     ROW-MISSING [:deps 0 :deps 0 :deps 0 :deps 3 :leaf?] = false
   ls-extract ok=true public=1216 all_missing=11 row_missing=0
   ls-tree ok=true public=32515 all_missing=5 tree_in_text=false
   == NEXT CALL ==
   EXIT_CODE=0
   ```

4. **Order equality is genuinely ratcheted, but continuation executability is inconsistent.** `test/clj_surgeon/mcp_study_test.clj:2368-2423` makes reversed and phantom typed-result rows fail, and the direct mutation confirms it. A typed `deps` continuation at `src/clj_surgeon/mcp_inspect.clj:1241-1252` is JSON and replays verbatim. By contrast, `ls-tree` emits the custom key/value prose assembled at `src/clj_surgeon/mcp_inspect_tool.clj:925-946`; it is neither a JSON tool argument object nor a shell command, so it is retypeable guidance, not an executable continuation verbatim.

   Exact order-mutation command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r2-fx/order_probe.clj; rc=$?; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   baseline_agrees= true
   reversed_agrees= false
   phantom_agrees= false
   restored_agrees= true
   EXIT_CODE=0
   ```

   Exact typed-continuation command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r2-fx/adversarial_probe.clj
   ```

   Relevant verbatim output:

   ```text
   == NEXT CALL ==
   line=     → next call: inspect_clojure {"requests":[{"id":"request-1","operation":"deps","file":"src/clj_surgeon/analyze.clj","limit":16384}],"expect":{"requests":1,"files":1}}
   parsed= true replay_ok= true replay_truncated= nil replay_returned= 27
   ```

   Exact MCP-entrance `ls-tree` command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r2-fx/entrance_probe.clj; rc=$?; echo EXIT_CODE=$rc
   ```

   Relevant verbatim output:

   ```text
   == MCP ENTRANCE LS-TREE ==
   error?= false row_in_text= true row_in_structured= true truncated= true read_complete= false
   remedy_in_text= false next_call_in_text= true
   next_line= → next call: inspect_clojure mode=ls-tree dir=. format=text limit=16384
   next_line_is_json= false
   text_tail=

   ── total: 32 files; 2 shown, 30 omitted

   ! bounded receipt · 30 files omitted · read_complete=false
   → next call: inspect_clojure mode=ls-tree dir=. format=text limit=16384
   → raise_limit_or_narrow_scope
   ```

5. **The common refusal renderer closes the eight known dropped facts for the thirteen fixtures, but its test enumeration is not exhaustive and its cause string is unbounded.** `src/clj_surgeon/mcp_inspect_tool.clj:837-856` has the exclusion set, `:867-885` bounds default detail lines at 512 characters, but `:913-914` emits `:error` verbatim. All thirteen ratchet fixtures and all eight named facts passed. A synthetic 10,000-character path is bounded in the detail line but repeated in full in the cause, yielding 10,612 text characters. Separately, `src/clj_surgeon/mcp_inspect.clj` alone contains 22 reachable `refuse!` reasons while `test/clj_surgeon/mcp_study_test.clj:2525-2589` exercises thirteen heterogeneous cases; therefore the claim that the ratchet enumerates every reachable refusal kind is not established.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r2-fx/refusal_probe.clj; rc=$?; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   == THIRTEEN RATCHET REFUSALS ==
   missing-fields type=invalid-mcp-request reason=missing-fields cause=true next=true nonstruct=2 missing_detail=[]
   unknown-fields type=invalid-mcp-request reason=unknown-fields cause=true next=true nonstruct=2 missing_detail=[]
   expectation-mismatch type=invalid-mcp-request reason=aggregate-expectation-mismatch cause=true next=true nonstruct=3 missing_detail=[]
   unsupported-operation type=invalid-mcp-request reason=unknown-operation cause=true next=true nonstruct=3 missing_detail=[]
   invalid-xray type=invalid-xray-expression reason=null cause=true next=true nonstruct=2 missing_detail=[]
   form-not-found type=batch-form-selection-failed reason=null cause=true next=true nonstruct=9 missing_detail=[]
   study-form-not-found type=study-form-not-found reason=null cause=true next=true nonstruct=4 missing_detail=[]
   file-not-found type=source-file-not-found reason=null cause=true next=true nonstruct=1 missing_detail=[]
   match-expectation type=inspect-cardinality-mismatch reason=null cause=true next=true nonstruct=5 missing_detail=[]
   invalid-study-limit type=invalid-study-limit reason=null cause=true next=true nonstruct=0 missing_detail=[]
   invalid-format type=invalid-format reason=null cause=true next=true nonstruct=1 missing_detail=[]
   dir-not-found type=directory-not-found reason=null cause=true next=true nonstruct=0 missing_detail=[]
   unknown-parameter type=unknown-parameter reason=null cause=true next=true nonstruct=2 missing_detail=[]
   == EIGHT PREVIOUSLY DROPPED FACTS ==
   file_hash present=true detail_label=true value_in_text=true value="f75716c5d5948b873e5023a8bfe04db127a6c94a0f28befd6a4803b569aab621"
   form present=true detail_label=true value_in_text=true value="no-such-form-xyz"
   match_count present=true detail_label=true value_in_text=true value=7
   failure_count present=true detail_label=true value_in_text=true value=1
   resolved_form_count present=true detail_label=true value_in_text=true value=0
   available_form_count present=true detail_label=true value_in_text=true value=27
   requested_form_count present=true detail_label=true value_in_text=true value=1
   candidate_limit present=true detail_label=true value_in_text=true value=10
   == SYNTHETIC 10K DETAIL/CAUSE ==
   path_detail_bounded= true cause_unbounded= true text_chars= 10612
   EXIT_CODE=0
   ```

   Exact static coverage command:

   ```sh
   printf 'VALIDATION_REASONS\n'; rg -o 'refuse! :[a-z0-9-]+' src/clj_surgeon/mcp_inspect.clj | sed 's/.*refuse! //' | sort -u; printf 'RATCHET_CASE_LABELS\n'; sed -n '2525,2570p' test/clj_surgeon/mcp_study_test.clj | rg -o '\[:[a-z0-9-]+' | sed 's/^\[//' ; echo EXIT_CODE=$?
   ```

   Verbatim output:

   ```text
   VALIDATION_REASONS
   :aggregate-expectation-mismatch
   :boolean
   :duplicate-id
   :empty-snapshot-guards
   :expected-object
   :invalid-relative-source-path
   :invalid-snapshot-hash
   :invalid-study-limit
   :missing-fields
   :missing-snapshot-guards
   :mixed-request-ids
   :non-blank-string
   :non-empty-array
   :non-negative-integer
   :operation-required
   :positive-integer
   :request-expectation-mismatch
   :too-many-files
   :too-many-forms
   :too-many-requests
   :unknown-fields
   :unknown-operation
   RATCHET_CASE_LABELS
   :missing-fields
   :unknown-fields
   :expectation-mismatch
   :unsupported-operation
   :invalid-xray
   :form-not-found
   :study-form-not-found
   :file-not-found
   :match-expectation
   :invalid-study-limit
   :invalid-format
   :dir-not-found
   :unknown-parameter
   EXIT_CODE=0
   ```

6. **BLOCKER — the schema-mode ratchet goes red, but the refusal-class ratchet has a reproduced green escape.** The mode table at `test/clj_surgeon/mcp_study_test.clj:2435-2491` is genuinely derived from the published mode vocabulary. Adding a schema mode without a renderer produced three failures. The refusal witness at `test/clj_surgeon/mcp_study_test.clj:2575-2589`, however, treats every member of `refusal-structural-keys` as rendered without proving that any renderer actually consumes it. In a temp clone I added a top-level refusal fact, deliberately excluded it, and supplied no renderer; all 5,998 assertions stayed green. This directly falsifies “a new refusal cannot ship text-blind.”

   Exact mode-sabotage diff command:

   ```sh
   git diff -- src/clj_surgeon/mcp_inspect_tool.clj; echo DIFF_EXIT_CODE=$?
   ```

   Verbatim output from `/tmp/o2r2-fx/sabotage-mode`:

   ```diff
   diff --git a/src/clj_surgeon/mcp_inspect_tool.clj b/src/clj_surgeon/mcp_inspect_tool.clj
   index 433a3d9e..9f7f7381 100644
   --- a/src/clj_surgeon/mcp_inspect_tool.clj
   +++ b/src/clj_surgeon/mcp_inspect_tool.clj
   @@ -1071,7 +1071,8 @@
                          ;; pins its own const.
                          "mode"
                          {:type "string"
   -                       :enum ["prepare-change" "plan-extraction" "ls-tree"]})
   +                       :enum ["prepare-change" "plan-extraction" "ls-tree"
   +                              "sabotage-mode"]})
       :oneOf [{:required ["requests" "expect"]}
               {:properties {"mode" {:const "prepare-change"}}
                :required ["mode" "subject" "intent"]}
   DIFF_EXIT_CODE=0
   ```

   Exact mode-sabotage test command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test 2>&1 | rg -A6 'every-published-mode-renders|Ran [0-9]+ tests'; rc=${PIPESTATUS[0]}; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   FAIL in (every-published-mode-renders-its-rows-in-the-text) (mcp_study_test.clj:2490)
   every published mode is classified as witnessed here or named elsewhere
   expected: (= (schema-modes) (set (keys class-ratchet-mode-coverage)))
     actual: (not (= #{"prepare-change" "plan-extraction" "sabotage-mode" "ls-tree"} #{"prepare-change" "plan-extraction" "ls-tree"}))

   FAIL in (the-read-entrance-exposes-no-write-operation) (mcp_study_test.clj:1381)
   expected: (= #{"prepare-change" "plan-extraction" "ls-tree"} modes)
   --
   Ran 462 tests containing 5998 assertions.
   3 failures, 0 errors.
   EXIT_CODE=3
   ```

   Exact refusal-sabotage diff command:

   ```sh
   git diff -- src/clj_surgeon/mcp_inspect_tool.clj; echo DIFF_EXIT_CODE=$?
   ```

   Relevant verbatim output from `/tmp/o2r2-fx/sabotage-refusal`:

   ```diff
   @@ -853,7 +853,7 @@
        :available_owners_truncated :available_owners_omitted
        :failed_request :failures :selection_failures :form_candidates
        :candidates_truncated :hypotheses_truncated :continuation
   -    :file_hashes :results :dir :grep :ns_grep :format :limit})
   +    :file_hashes :results :dir :grep :ns_grep :format :limit :sabotage})
   @@ -1462,13 +1462,16 @@
    (defn- inspect-refusal
      [result]
      (let [normalized (inspect/json-data result)]
   -    (merge
   -      {:ok false
   -       :operation "inspect_clojure"
   -       :read_complete false
   -       :source_unchanged true
   -       :next_action "correct_request"}
   -      normalized)))
   +    (cond->
   +      (merge
   +        {:ok false
   +         :operation "inspect_clojure"
   +         :read_complete false
   +         :source_unchanged true
   +         :next_action "correct_request"}
   +        normalized)
   +      (= "missing-fields" (:reason normalized))
   +      (assoc :sabotage "HIDDEN-REFUSAL-FACT"))))
   ```

   Exact refusal-sabotage test command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test 2>&1 | rg -A2 'Ran [0-9]+ tests'; rc=${PIPESTATUS[0]}; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   Ran 462 tests containing 5998 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

7. **Telemetry behaves exactly as the new connection-level contract says, with an experimental caveat rather than an implementation failure.** `src/clj_surgeon/mcp_telemetry.clj:132-162` keys `session.start` on `[workspace-key client-run-id]`; `src/clj_surgeon/mcp_inspect_tool.clj:1933-1941` gets that id from the transport SDK session. Two SDK sessions on one root therefore produce two events with distinct ids, and a reconnect within one arm also splits that arm into two telemetry sessions. The observation explicitly admits this at `docs/observations/2026-09-04-o2r2-text-carries-the-receipt.md:62-65`; `MCP-OP-STUDY-043` at `docs/intent/study-ops/study-ops-specs.md:108` intentionally requires reconnects to leave separate events. Rejecting a request `client_run_id` is correct: it is forgeable, and the schema is closed at `src/clj_surgeon/mcp_inspect_tool.clj:1061-1064`. This does not corrupt a watch that counts `tool.call`, because every inspect call independently emits one at `src/clj_surgeon/mcp_telemetry.clj:263-266`; it does make `session.start` unsuitable as an arm count unless each arm retains exactly one SDK session.

   Exact telemetry command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r2-fx/telemetry_probe.clj
   ```

   Verbatim output:

   ```text
   session.start count= 2
   client_run_ids= ["arm-a-connection-1" "arm-a-connection-2"]
   workspace_keys= ["6629794dfeb9524a" "6629794dfeb9524a"]

   EXIT_CODE=0
   ```

   Exact request-forgery command:

   ```sh
   clojure -M:clj-surgeon/mcp -e "(require '[clj-surgeon.mcp-inspect-tool :as t]) (prn (select-keys (t/execute-inspect! {:project-root \"/home/forge/tmp/sol/o2r2-wt\"} {\"requests\" [{\"id\" \"r\" \"operation\" \"outline\" \"file\" \"src/clj_surgeon/analyze.clj\"}] \"expect\" {\"requests\" 1 \"files\" 1} \"client_run_id\" \"forged-arm\"}) [:ok :error_type :reason :unknown :next_action]))"; rc=$?; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   {:ok false, :error_type "invalid-mcp-request", :reason "unknown-fields", :unknown ["client_run_id"], :next_action "correct_request"}
   EXIT_CODE=0
   ```

8. **The source-free companion contract was intentionally reversed in EARS/tests/code, but not properly retired through the repository's required intent chain.** The three reversed assertion sites are now `test/clj_surgeon/mcp_inspect_tool_test.clj:304-311`, `test/clj_surgeon/mcp_http_server_test.clj:296-301`, and `test/clj_surgeon/mcp_inspect_contract_test.clj:257-268`. `MCP-OP-STUDY-041` at `docs/intent/study-ops/study-ops-specs.md:100` explicitly records the new intent, so the reversal was not wholly silent. However, the twelve commits changed only the EARS file in the design/intent surface: neither HLD nor an LLD changed. The original owning plan still promises “only a concise human summary” at `docs/plans/typed-mcp-inspect-entrance.md:142-145` and “concise source-free summaries” at `:304-310`; README still calls text an ordinary transcript summary at `README.md:972-979`. That is an incomplete retirement and violates the checkout's mandatory HLD → LLD → EARS ordering. It is a real behavioral regression for consumers that intentionally logged/displayed only the formerly concise, source-free text channel: that channel now includes source bodies (even though source was already available to structured-content consumers).

   Exact contract-history command:

   ```sh
   git grep -n -i 'source-free companion\|source-free.*summar' HEAD -- docs/intent docs/plans README.md src test; echo GREP_EXIT_CODE=$?; git log -S'source-free MCP text companion' --oneline -- src test docs/intent docs/plans; echo LOG_EXIT_CODE=$?
   ```

   Relevant verbatim output:

   ```text
   HEAD:docs/plans/typed-mcp-inspect-entrance.md:310:- deterministic normalization and concise source-free summaries.
   HEAD:test/clj_surgeon/mcp_inspect_contract_test.clj:259:    ;; reversed the "source-free companion" rule for every mode, because a
   HEAD:test/clj_surgeon/mcp_inspect_tool_test.clj:305:      ;; Reversed by O2 round 2: the text block is not a source-free companion
   GREP_EXIT_CODE=0
   4fa5e89b study-ops: O2r2 GREEN (item 2) — every mode's text carries its rows
   daedfbcd feat: integrate typed MCP inspection
   LOG_EXIT_CODE=0
   ```

   Exact intent-layer diff command:

   ```sh
   git diff --name-only 26e4810..a0b0520 -- docs/high-level-design.md docs/intent/study-ops docs/plans/typed-mcp-inspect-entrance.md README.md; echo EXIT_CODE=$?
   ```

   Verbatim output:

   ```text
   docs/intent/study-ops/study-ops-specs.md
   EXIT_CODE=0
   ```

9. **The round-one `ls-tree` blocker is closed at the MCP entrance and CLI, and all CLI goldens are unchanged.** `src/clj_surgeon/mcp_inspect_tool.clj:1012-1059` now carries payload rows and, when applicable, continuation/remedy. The live callback probe saw a row and continuation at a low limit; at the ceiling it saw a row and the exact remedy with no looping continuation. The CLI renders rows directly. The custom `ls-tree` continuation-executability defect is separately recorded in finding 4.

   Exact MCP ceiling-receipt command:

   ```sh
   sed -n '/== MCP ENTRANCE AT CEILING ==/,$p' /tmp/o2r2-fx/entrance_probe.latest; echo EXIT_CODE=$?
   ```

   Verbatim output:

   ```text
   == MCP ENTRANCE AT CEILING ==
   row_in_text= true remedy_in_text= true next_call_present= false
   → lower limit, narrow dir, or add a grep pattern so the complete result fits the public output budget
   ! bounded receipt · 54 files omitted · read_complete=false
   → The receipt is already at the maximum limit; scan a subdirectory or add a grep pattern.
   → narrow_scope
   EXIT_CODE=0
   ```

   Exact CLI command:

   ```sh
   bb -cp src -m clj-surgeon.core :op :ls-tree :dir /tmp/o2r2-fx/batch-3843554679460244926 :format :text | sed -n '1,8p'; rc=${PIPESTATUS[0]}; echo EXIT_CODE=$rc
   ```

   Verbatim output:

   ```text
   src/fixture/f0.clj  2 lines, 1 forms
     ns: fixture.f0
     2: defn f0 []

   src/fixture/f1.clj  2 lines, 1 forms
     ns: fixture.f1
     2: defn f1 []

   EXIT_CODE=0
   ```

   Exact golden/blob command:

   ```sh
   git diff --exit-code 4480e3d..HEAD -- test-fixtures/; diff_rc=$?; echo DIFF_EXIT_CODE=$diff_rc; for f in test-fixtures/study/ls-tree-existing-ops.golden.txt test-fixtures/study/ls-tree-existing-ops-edn.golden.txt test-fixtures/study/ls-tree-no-clojure-files.golden.txt test-fixtures/study/ls-tree-prune-target.golden.txt; do printf '%s 4480e3d=' "$f"; git rev-parse "4480e3d:$f"; printf '%s HEAD=' "$f"; git rev-parse "HEAD:$f"; done; echo EXIT_CODE=$?
   ```

   Verbatim output:

   ```text
   DIFF_EXIT_CODE=0
   test-fixtures/study/ls-tree-existing-ops.golden.txt 4480e3d=aa06049dc03534ac463ea01f38f7a19f89abfed8
   test-fixtures/study/ls-tree-existing-ops.golden.txt HEAD=aa06049dc03534ac463ea01f38f7a19f89abfed8
   test-fixtures/study/ls-tree-existing-ops-edn.golden.txt 4480e3d=e370c028074bd8288c6816f27216c1d2ddc6c452
   test-fixtures/study/ls-tree-existing-ops-edn.golden.txt HEAD=e370c028074bd8288c6816f27216c1d2ddc6c452
   test-fixtures/study/ls-tree-no-clojure-files.golden.txt 4480e3d=8e5a55a053642a2077eaa32ff1b1425b66a7ad76
   test-fixtures/study/ls-tree-no-clojure-files.golden.txt HEAD=8e5a55a053642a2077eaa32ff1b1425b66a7ad76
   test-fixtures/study/ls-tree-prune-target.golden.txt 4480e3d=d54a0617468badb642c05dc7c772c64bbe15bc68
   test-fixtures/study/ls-tree-prune-target.golden.txt HEAD=d54a0617468badb642c05dc7c772c64bbe15bc68
   EXIT_CODE=0
   ```

10. **All required current-head gates pass at the claimed counts.** Relevant entry points: `test/run_all.clj:1`, `test/clj_surgeon/mcp_test_runner.clj:1`, `test/mcp_operation_contract_oracle.pl:1`, `src/clj_surgeon/mcp_intent_contract.clj:1`.

    Exact command:

    ```sh
    ~/bin/suite-run bb test/run_all.clj
    ```

    Verbatim ran-lines and exit status:

    ```text
    Ran 731 tests containing 6023 assertions.
    0 failures, 0 errors.
    EXIT_CODE=0
    ```

    Exact command:

    ```sh
    ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
    ```

    Verbatim ran-lines and exit status:

    ```text
    Ran 462 tests containing 5998 assertions.
    0 failures, 0 errors.
    EXIT_CODE=0
    ```

    Exact command:

    ```sh
    make mcp-operation-oracle
    ```

    Verbatim output and exit status:

    ```text
    # @spec MCP-OP-ORACLE-001
    swipl -q -f test/mcp_operation_contract_oracle.pl
    mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
    EXIT_CODE=0
    ```

    Exact command:

    ```sh
    ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :violations])))"; rc=$?; echo EXIT_CODE=$rc
    ```

    Verbatim output:

    ```text
    {:ok true, :violations []}
    EXIT_CODE=0
    ```

11. **Every named RED commit is red at its own SHA and green at the following GREEN; no historical witness was never-red.** Relevant witness locations at current head: `test/clj_surgeon/mcp_study_test.clj:2021`, `:2132`, `:2245`, and `test/clj_surgeon/mcp_inspect_tool_test.clj:1138` (historical line numbers shown in the failures below).

    Exact command, run in `/tmp/o2r2-fx/redgreen`:

    ```sh
    for spec in b44a26f8:9e7ab5c9 71cfa532:4fa5e89b 97635d2d:395361e8 bc38e2bf:8ee8ef25; do red=${spec%:*}; green=${spec#*:}; for sha in "$red" "$green"; do git checkout --quiet "$sha"; receipt=$(mktemp /tmp/o2r2-fx/redgreen.XXXXXX); ~/bin/suite-run clojure -M:clj-surgeon/mcp-test >"$receipt" 2>&1; run_rc=$?; echo "SHA=$sha"; rg -m1 -A3 '^FAIL in' "$receipt" || true; rg '^Ran |^[0-9]+ failures' "$receipt"; echo "EXIT_CODE=$run_rc"; done; done
    ```

    Verbatim output:

    ```text
    SHA=b44a26f8
    FAIL in (ls-tree-public-result-is-bounded-by-the-declared-output-budget) (mcp_study_test.clj:2021)
    the enforced result was 34111 bytes
    expected: (<= (public-bytes fitted) inspect-tool/max-public-result-bytes)
      actual: (not (<= 34111 32768))
    Ran 439 tests containing 5641 assertions.
    5 failures, 0 errors.
    EXIT_CODE=5
    SHA=9e7ab5c9
    Ran 440 tests containing 5645 assertions.
    0 failures, 0 errors.
    EXIT_CODE=0
    SHA=71cfa532
    FAIL in (ls-deps-and-ls-extract-text-carry-their-trees) (mcp_study_test.clj:2132)
    the dependency tree's members, not a count of one row
    expected: (str/includes? tree-text "intra-ns-deps")
      actual: (not (str/includes? "inspect_clojure\n  1 request · 1 file\n\n✓ all requests resolved\n✓ ordered snapshot\n✓ hashes attached\n✓ terminal evidence · read_complete=true · next action none\n\n  request-1: ls-deps · 1 of 1 rows\n  23,811 source characters · 1.00 ms" "intra-ns-deps"))
    Ran 446 tests containing 5770 assertions.
    115 failures, 0 errors.
    EXIT_CODE=115
    SHA=4fa5e89b
    Ran 446 tests containing 5774 assertions.
    0 failures, 0 errors.
    EXIT_CODE=0
    SHA=97635d2d
    FAIL in (a-study-refusal-text-names-its-cause-and-its-owners) (mcp_study_test.clj:2245)
    the cause travels in the text, not only in structuredContent
    expected: (str/includes? text (:error response))
      actual: (not (str/includes? "inspect_clojure\n  refused · study-form-not-found · 1.00 ms\n\n  All listed owners are real snapshot evidence; ranking is non-authoritative. Semantic selection among them is allowed; the exact retry verifies the selection.\n\n→ correct_request" "No top-level form named no-such-form-xyz in src/clj_surgeon/analyze.clj"))
    Ran 452 tests containing 5851 assertions.
    37 failures, 0 errors.
    EXIT_CODE=37
    SHA=395361e8
    Ran 452 tests containing 5851 assertions.
    0 failures, 0 errors.
    EXIT_CODE=0
    SHA=bc38e2bf
    FAIL in (the-inspect-entrance-names-the-client-session-it-was-called-from) (mcp_inspect_tool_test.clj:1138)
    two client sessions on ONE root are two sessions, not one
    expected: (= 2 (count started))
      actual: (not (= 2 1))
    Ran 455 tests containing 5859 assertions.
    5 failures, 0 errors.
    EXIT_CODE=5
    SHA=8ee8ef25
    Ran 455 tests containing 5859 assertions.
    0 failures, 0 errors.
    EXIT_CODE=0
    ```

12. **The final checkout is still clean and unchanged.** I did not start a Surgeon server during this review. Relevant repository root: `.:1`.

    Exact command:

    ```sh
    git rev-parse HEAD; git status --short; echo EXIT_CODE=$?
    ```

    Verbatim output (the blank line between SHA and exit status is the empty status):

    ```text
    a0b052061499daa2a4f729f285170f9e8981f2f2
    EXIT_CODE=0
    ```

## NO-GO

Before landing, the mayor must verify fixes for silent single-row body loss and terminal wording, structured-below-budget refusals/the 32-result floor, strict text/structured evidence coverage or an explicitly narrowed approved contract, the refusal-ratchet green escape, and the incomplete source-free-contract retirement.
