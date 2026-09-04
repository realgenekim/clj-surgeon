## NO-GO

1. Provenance and review scope are correct and the worktree remained unchanged. I reviewed the round-two verdict first, the twelve commits and their diffs, and the round-three observation before probing. The server probe used only explicit port 8040 and stopped it. Relevant locations: `docs/observations/2026-09-04-o2r3-the-text-is-a-superset-of-the-receipt.md:1`, `CLAUDE.md:1`.

   Exact command:

   ```sh
   git rev-parse HEAD; git status --short; git log --oneline --reverse a0b0520..e258519; rg --files docs/observations | rg '2026-09-04-o2r3-.*\.md$'
   ```

   Verbatim output (`git status --short` emitted no line):

   ```text
   e258519e209ec28affb44377bba004a01c10bd23
   6587c9d2 test(mcp): expose false output-limit refusals
   2f094aa5 fix(mcp): fit text before refusing receipts
   ba6e29b4 test(mcp): expose receipt leaves missing from text
   30e5423c fix(mcp): carry every receipt leaf in text
   08e44904 test(mcp): expose structural-key text escape
   54c2f827 fix(mcp): make receipt facts structural-key-proof
   4b38202f test(mcp): expose non-executable next calls
   a08149af fix(mcp): publish executable next calls
   72843e65 test(mcp): expose refusal ratchet and cause gaps
   5b8b4a39 fix(mcp): derive refusal ratchet and bound cause
   34a4aa24 docs(mcp): retire source-free companion contract
   e258519e test(mcp): witness text superset over HTTP
   docs/observations/2026-09-04-o2r3-the-text-is-a-superset-of-the-receipt.md
   ```

   Exact final command:

   ```sh
   git rev-parse HEAD; git status --short; if lsof -nP -iTCP:8040 -sTCP:LISTEN; then echo PORT_8040_STILL_LISTENING; else echo PORT_8040_STOPPED; fi
   ```

   Verbatim output:

   ```text
   e258519e209ec28affb44377bba004a01c10bd23
   PORT_8040_STOPPED
   ```

2. **Blocker — the frozen exclusion set is not the whole exclusion mechanism.** `receipt-leaf-pairs` silently produces no leaf for an empty map/vector, and `leaf-rendered?` silently declares `nil` and blank strings rendered without their value or label appearing. Thus `text-excluded-leaf-keys = #{:workspace_root}` is not the claimed exhaustive set. The product predicate reports zero misses only because it shares these loopholes with the renderer. This violates MCP-OP-STUDY-044's “every structuredContent leaf” and “explicit, enumerated, frozen set” criterion. Files: `src/clj_surgeon/mcp_inspect.clj:433`, `src/clj_surgeon/mcp_inspect.clj:446`, `src/clj_surgeon/mcp_inspect.clj:463`, `test/clj_surgeon/mcp_study_test.clj:2910`, `test/clj_surgeon/mcp_study_test.clj:2991`, `docs/intent/study-ops/study-ops-specs.md:104`.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/fresh_probe.clj | sed -n '/== FRESH SPECIAL LEAF ATTACK ==/,/== MCP CALLBACK BOUNDARIES AND RESERVE ==/p'
   ```

   Verbatim output:

   ```text
   == FRESH SPECIAL LEAF ATTACK ==
   float spelling="1.25" present=true label_present=true
   empty_map spelling="{}" present=false label_present=false
   large_float spelling="1.0E20" present=true label_present=true
   nil_value spelling="null" present=false label_present=false
   blank spelling="" present=true label_present=false
   keyword spelling="violet" present=true label_present=true
   unicode spelling="雪☃" present=true label_present=true
   boolean spelling="false" present=true label_present=true
   empty_vector spelling="[]" present=false label_present=false
   fresh_missing= [[[:special :empty_map] {}] [[:special :nil_value] nil] [[:special :empty_vector] []] [[:results] []]]
   product_predicate_missing= []
   excluded_keys= #{:workspace_root}
   == MCP CALLBACK BOUNDARIES AND RESERVE ==
   ```

3. **Blocker — the claimed constructor-derived refusal enumeration already misses a reachable refusal.** The test scans only literal forms matching `(refuse! :reason`. `unique-strings!` accepts a reason argument, and the public forms validator passes `:duplicate-form`; that refusal is reachable but absent from the scanned 22. Therefore the source actually constructs at least 23 reachable reasons and MCP-OP-STUDY-046's exhaustive ratchet is not established. Files: `src/clj_surgeon/mcp_inspect.clj:67`, `src/clj_surgeon/mcp_inspect.clj:142`, `src/clj_surgeon/mcp_inspect.clj:149`, `src/clj_surgeon/mcp_inspect.clj:233`, `test/clj_surgeon/mcp_study_test.clj:3133`, `test/clj_surgeon/mcp_study_test.clj:3140`, `docs/intent/study-ops/study-ops-specs.md:108`.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/helper_reason_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim output:

   ```text
   scanned_count= 22
   scanned_reasons= #{"aggregate-expectation-mismatch" "boolean" "duplicate-id" "empty-snapshot-guards" "expected-object" "invalid-relative-source-path" "invalid-snapshot-hash" "invalid-study-limit" "missing-fields" "missing-snapshot-guards" "mixed-request-ids" "non-blank-string" "non-empty-array" "non-negative-integer" "operation-required" "positive-integer" "request-expectation-mismatch" "too-many-files" "too-many-forms" "too-many-requests" "unknown-fields" "unknown-operation"}
   helper_call_present= true
   helper_reason= duplicate-form included_in_scan= false refused= true
   cause_chars= 21 cause_in_text= true leaf_misses= []
   EXIT_CODE=0
   ```

4. **Blocker — a 10,000-character refusal is not text-superset-complete even when one complete rendering fits the 32 KiB public budget.** The cause itself is bounded, but the fixed receipt-fact allowance drops the full `error` and `path`, plus four other leaves. The text declares the drop, yet the measured hypothetical result containing one full copy is 31,869 bytes, below 32,768; this is not a drop forced by the public budget as MCP-OP-STUDY-044 states. It also disproves the builder's “text ⊇ structured on each” refusal claim. The requirements themselves are in tension: MCP-OP-STUDY-046's 10,000-character witness expects the cause to be dropped, whereas MCP-OP-STUDY-044 says only the output budget may force that exception. Files: `src/clj_surgeon/mcp_inspect.clj:513`, `src/clj_surgeon/mcp_inspect_tool.clj:958`, `src/clj_surgeon/mcp_inspect_tool.clj:1923`, `docs/intent/study-ops/study-ops-specs.md:104`, `docs/intent/study-ops/study-ops-specs.md:108`, `docs/intent/study-ops/study-ops-specs.md:161`.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/long_refusal_leaf_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim output:

   ```text
   WARNING: bytes already refers to: #'clojure.core/bytes in namespace: user, being replaced by: #'user/bytes
   error_type= invalid-source-path structured_bytes= 20504 text_chars= 1321 public_bytes= 21847
   full_cause_in_text= false full_path_in_text= false declares_drop= true
   uncarried_count= 6 uncarried_paths= [[:path] [:read_complete] [:inspection_elapsed_ms] [:source_unchanged] [:ok] [:error]]
   hypothetical_one_copy_public_bytes= 31869 hypothetical_fits= true
   EXIT_CODE=0
   ```

5. **The 64-byte publication reserve passes realistic clock-domain and exact boundary probes, but it is not invariant under the operation envelope's accepted numeric clock.** The callback entrance publishes within budget at pre-finalization sizes 32,768 and 32,769, and a `Long/MAX_VALUE`-scale elapsed rendering also fits. However, a finite `1.0E308` clock produces a 309-character elapsed value and accepted candidates publish at 32,860–33,023 bytes. This is a reproducible limit escape through `mcp-operation/invoke!`, not a suspicion, although the production `System/nanoTime` implementation cannot currently produce it. Files: `src/clj_surgeon/mcp_inspect_tool.clj:1880`, `src/clj_surgeon/mcp_inspect_tool.clj:1892`, `src/clj_surgeon/mcp_inspect_tool.clj:1923`, `src/clj_surgeon/mcp_operation.clj:49`.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/reserve_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim output:

   ```text
   payload= 400 structured= 21743 fit_ok= true fit_measure= 31554 limit= nil normal_published= 31571 huge_published= 31900 huge_bounded= true
   payload= 420 structured= 22383 fit_ok= true fit_measure= 32514 limit= nil normal_published= 32531 huge_published= 32860 huge_bounded= false
   payload= 430 structured= 22703 fit_ok= true fit_measure= 32495 limit= nil normal_published= 32512 huge_published= 32841 huge_bounded= false
   payload= 440 structured= 23023 fit_ok= true fit_measure= 32675 limit= 8852 normal_published= 32692 huge_published= 33021 huge_bounded= false
   payload= 460 structured= 23663 fit_ok= true fit_measure= 32677 limit= 8236 normal_published= 32694 huge_published= 33023 huge_bounded= false
   EXIT_CODE=0
   ```

   Exact MCP callback boundary command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/fresh_probe.clj | sed -n '/== MCP CALLBACK BOUNDARIES AND RESERVE ==/,/== EVERY MODE CONTINUATION/p'
   ```

   Verbatim output:

   ```text
   == MCP CALLBACK BOUNDARIES AND RESERVE ==
   target=32768 found=true raw_prefitted=32768 published=32619 bounded=true ok=true
   target=32769 found=true raw_prefitted=32769 published=32620 bounded=true ok=true
   long-domain-max elapsed_chars=19 published=32636 bounded=true
   over-64-byte-double elapsed_chars=309 published=32623 bounded=true
   row_rich_prefit= 27885 row_rich_fit_ok= true row_rich_text_limit= nil
   == EVERY MODE CONTINUATION THROUGH CALLBACK ENTRANCE ==
   ```

   The last `over-64-byte-double` line happens to remain below budget because that particular candidate has additional headroom; the targeted reserve probe above places the same clock shape against near-limit candidates and demonstrates the escape.

   I also simulated the MEM-003 wire transformation itself by relocating all four clock-derived fields under `measured` after this branch's fit, while holding the summary text constant. The wrapper costs 13 bytes and the current 64-byte reserve survives all four near-boundary candidates. This supports the normal re-composition shape; it does not repair the unbounded accepted clock representation above.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/measured_shape_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim output:

   ```text
   payload= 420 fit_ok= true limit= nil current_published= 32675 measured_published= 32688 shape_delta= 13 bounded= true
   payload= 430 fit_ok= true limit= nil current_published= 32656 measured_published= 32669 shape_delta= 13 bounded= true
   payload= 440 fit_ok= true limit= 8805 current_published= 32711 measured_published= 32724 shape_delta= 13 bounded= true
   payload= 460 fit_ok= true limit= 8176 current_published= 32715 measured_published= 32728 shape_delta= 13 bounded= true
   EXIT_CODE=0
   ```

6. **The HTTP entrance does honor the exact 32,768/32,769 boundary and the explicit server was stopped.** Files: `src/clj_surgeon/mcp_http_server.clj:218`, `src/clj_surgeon/mcp_inspect_tool.clj:2038`.

   Exact command:

   ```sh
   if lsof -nP -iTCP:8040 -sTCP:LISTEN; then echo PORT_8040_BUSY; exit 1; else echo PORT_8040_FREE; fi; clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/http_boundary_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc; if lsof -nP -iTCP:8040 -sTCP:LISTEN; then echo PORT_8040_STILL_LISTENING; else echo PORT_8040_STOPPED; fi
   ```

   Verbatim output:

   ```text
   PORT_8040_FREE
   server_port= 8040 session= true
   target= 32768 raw_prefitted= 32768 http_status= 200 published= 32625 bounded= true ok= true
   target= 32769 raw_prefitted= 32769 http_status= 200 published= 32625 bounded= true ok= true
   server_stopped_port= 8040
   EXIT_CODE=0
   PORT_8040_STOPPED
   ```

7. **The round-two §2 failures are otherwise fixed.** The old adversarial probe no longer finds an under-budget receipt refused, the 32×512 floor no longer forces refusal at its first case, dropped bodies are nonterminal and declared, and an abridged tree no longer claims completeness. Larger 32-result structured receipts still correctly refuse once structured content alone exceeds the budget. Files: `src/clj_surgeon/mcp_inspect_tool.clj:1892`, `src/clj_surgeon/mcp_inspect_tool.clj:1923`, `docs/intent/study-ops/study-ops-specs.md:98`.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/r2_adversarial_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim relevant output:

   ```text
   == BOUNDS ==
   exact_raw_bytes= 32885 exact_unchanged= false exact_ok= true
   plus_one_raw_bytes= 32886 plus_one_structured_bytes= 32558 plus_one_fit_ok= true plus_one_type= nil plus_one_fit_bytes= 32595
   single_row_raw_public= 11195 fit_ok= true fit_public= 11195
   single_row_source_in_text= false abridged_notice= true terminal_claim= false
   inspect_clojure |   1 request · 1 file · 1 form |  | ✓ all requests resolved | ✓ ordered snapshot | ✓ hashes attached | ! text abridged · read_complete=true · next action read_structured_content_or_narrow_request |
   == 32-RESULT FLOOR ==
   payload=100 raw_ok=true structured=31555 public=44293 text_chars=12423 fit_ok=true fit_type=null fit_public=32688
   payload=200 raw_ok=true structured=34755 public=50781 text_chars=15721 fit_ok=false fit_type=inspect-output-limit fit_public=1056
   payload=300 raw_ok=true structured=37956 public=54879 text_chars=16661 fit_ok=false fit_type=inspect-output-limit fit_public=1056
   payload=400 raw_ok=true structured=41157 public=57728 text_chars=16351 fit_ok=false fit_type=inspect-output-limit fit_public=1056
   ```

8. **Every mode that emitted a continuation in my fixture carried a verbatim JSON request and replayed successfully.** `deps`, `topo`, `ls-deps`, and `ls-tree` emitted continuations; all parsed and replayed, and the separate `ls-deps` check confirmed its raised limit and completion. No next call was emitted by the particular small `forms`, `outline`, `match`, `xray`, or `ls-extract` cases, so a claim about those non-emitting shapes is not needed. Files: `src/clj_surgeon/mcp_inspect_tool.clj:2038`, `test/clj_surgeon/mcp_study_test.clj:3086`, `docs/intent/study-ops/study-ops-specs.md:106`.

   Exact command:

   ```sh
   clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/fresh_probe.clj | sed -n '/== EVERY MODE CONTINUATION/,/== ABRIDGED DECLARATION ==/p'; clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/continuation_detail.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim output:

   ```text
   == EVERY MODE CONTINUATION THROUGH CALLBACK ENTRANCE ==
   forms emits_next=false
   outline emits_next=false
   match emits_next=false
   xray emits_next=false
   deps emits_next=true tool=inspect_clojure parsed=true verbatim=true replay_ok=true advances=true
   topo emits_next=true tool=inspect_clojure parsed=true verbatim=true replay_ok=true advances=true
   ls-deps emits_next=true tool=inspect_clojure parsed=true verbatim=true replay_ok=true advances=null
   ls-extract emits_next=false
   ls-tree emits_next=true tool=inspect_clojure parsed=true verbatim=true replay_ok=true advances=true
   == ABRIDGED DECLARATION ==
   first-top= {:read_complete false, :next_action "raise_limit_or_narrow_scope", :next_call {:tool "inspect_clojure", :arguments {:requests [{:id "r", :operation "ls-deps", :file "src/clj_surgeon/analyze.clj", :form "extraction-closure", :limit 2374}], :expect {:requests 1, :files 1}}}, :limit 200}
   first-result= {}
   replay= {:returned 1, :omitted 0, :truncated false, :limit 2374}
   same_arguments= false
   replay_ok= true
   EXIT_CODE=0
   ```

9. **Sabotage B and C fail as claimed, but a rung D exists and is already present in ordinary source.** B (adding `:sabotage` to the exclusion set) is caught. C (making `receipt-fact-lines` skip `:hash`) is caught by three witnesses, including the new HTTP witness. D replaces direct literal refusal reasons with equivalent computed expressions; the full suite stays green and the scan falls from 22 to 21. The current, unsabotaged helper-produced `:duplicate-form` in finding 3 is the real instance of the same escape. Files: `test/clj_surgeon/mcp_study_test.clj:2991`, `test/clj_surgeon/mcp_study_test.clj:3140`, `src/clj_surgeon/mcp_inspect.clj:149`.

   Exact B command:

   ```sh
   cd /tmp/o2r3-review-fx/sabotage-b && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; suite_rc=$?; echo EXIT_CODE=$suite_rc
   ```

   Verbatim terminal output:

   ```text
   FAIL in (the-excluded-leaf-set-is-frozen-and-every-member-carries-its-reason) (mcp_study_test.clj:2994)
   a new exclusion needs a reason in the docstring and an edit here
   expected: (= #{:workspace_root} inspect/text-excluded-leaf-keys)
     actual: (not (= #{:workspace_root} #{:sabotage :workspace_root}))

   Ran 476 tests containing 6235 assertions.
   1 failures, 0 errors.
   EXIT_CODE=1
   ```

   Exact C command:

   ```sh
   cd /tmp/o2r3-review-fx/sabotage-c && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; suite_rc=$?; echo EXIT_CODE=$suite_rc
   ```

   Verbatim terminal output:

   ```text
   FAIL in (http-protocol-exposes-four-tools-and-structured-read-evidence) (mcp_http_server_test.clj:313)
   receipt leaves absent from the text: [[:results 0 :hash] "71e4fed8ff88da6963a857117063573466da1f8d3eaf41d23117a60153ec73b6"]

   FAIL in (every-published-mode-text-carries-every-structured-content-leaf) (mcp_study_test.clj:2931)
   forms: 1 receipt leaves the text does not carry — [:results 0 :hash] = "79c1232317366da38ed13a97f98b5f8b33045b4e53d680a4444939cd0679193d"

   FAIL in (every-published-mode-text-carries-every-structured-content-leaf) (mcp_study_test.clj:2931)
   match: 1 receipt leaves the text does not carry — [:results 0 :hash] = "79c1232317366da38ed13a97f98b5f8b33045b4e53d680a4444939cd0679193d"

   Ran 476 tests containing 6235 assertions.
   3 failures, 0 errors.
   EXIT_CODE=3
   ```

   Exact D command:

   ```sh
   cd /tmp/o2r3-review-fx/sabotage-d && git diff -- src/clj_surgeon/mcp_inspect.clj; ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; suite_rc=$?; printf 'SCANNED='; rg -o '\(refuse! :[a-z0-9-]+' src/clj_surgeon/mcp_inspect.clj | sort -u | wc -l; echo EXIT_CODE=$suite_rc
   ```

   Verbatim terminal output:

   ```text
   diff --git a/src/clj_surgeon/mcp_inspect.clj b/src/clj_surgeon/mcp_inspect.clj
   index b84230e..1789795 100644
   --- a/src/clj_surgeon/mcp_inspect.clj
   +++ b/src/clj_surgeon/mcp_inspect.clj
   @@ -81,7 +81,7 @@
      [value path]
      (if (map? value)
        value
   -    (refuse! :expected-object path "an object" value)))
   +    (refuse! (identity :expected-object) path "an object" value)))
   @@ -126,7 +126,7 @@
      (if (or (nil? value) (boolean? value))
        value
        (refuse!
   -      :boolean path "a boolean" value)))
   +      (identity :boolean) path "a boolean" value)))

   Ran 476 tests containing 6235 assertions.
   0 failures, 0 errors.
   SCANNED=21
   EXIT_CODE=0
   ```

10. **The source-free companion contract is visibly retired through intent, tests, and prose; no orphaned marker was found.** MCP-OP-STUDY-036 is superseded in place, 044/045/046 are checked, and the repository audit is green. Files: `docs/intent/study-ops/study-ops-specs.md:90`, `docs/intent/study-ops/study-ops-specs.md:100`, `docs/intent/study-ops/study-ops-specs.md:104`, `docs/intent/study-ops/study-ops-specs.md:106`, `docs/intent/study-ops/study-ops-specs.md:108`, `docs/high-level-design.md:36`, `README.md:979`, `docs/plans/typed-mcp-inspect-entrance.md:144`, `test/clj_surgeon/mcp_study_test.clj:3226`.

   Exact command:

   ```sh
   rg -n -o 'MCP-OP-STUDY-(036|041|044|045|046)|RETIRES the "source-free companion" contract|RETIRED 2026-09-04|SUPERSEDES the `next call: <tool> <key>=<value> …` clause' docs/intent/study-ops/study-ops-specs.md docs/high-level-design.md README.md docs/plans/typed-mcp-inspect-entrance.md
   ```

   Verbatim output:

   ```text
   docs/plans/typed-mcp-inspect-entrance.md:144:RETIRED 2026-09-04
   docs/plans/typed-mcp-inspect-entrance.md:144:MCP-OP-STUDY-041
   docs/plans/typed-mcp-inspect-entrance.md:145:MCP-OP-STUDY-044
   docs/plans/typed-mcp-inspect-entrance.md:160:MCP-OP-STUDY-044
   docs/plans/typed-mcp-inspect-entrance.md:327:RETIRED 2026-09-04
   docs/plans/typed-mcp-inspect-entrance.md:328:MCP-OP-STUDY-041
   docs/plans/typed-mcp-inspect-entrance.md:328:MCP-OP-STUDY-044
   README.md:979:MCP-OP-STUDY-044
   README.md:983:RETIRED 2026-09-04
   README.md:983:MCP-OP-STUDY-041
   README.md:984:MCP-OP-STUDY-044
   docs/high-level-design.md:36:MCP-OP-STUDY-044
   docs/high-level-design.md:39:RETIRED 2026-09-04
   docs/high-level-design.md:39:MCP-OP-STUDY-041
   docs/high-level-design.md:40:MCP-OP-STUDY-044
   docs/intent/study-ops/study-ops-specs.md:90:MCP-OP-STUDY-036
   docs/intent/study-ops/study-ops-specs.md:90:MCP-OP-STUDY-045
   docs/intent/study-ops/study-ops-specs.md:98:MCP-OP-STUDY-036
   docs/intent/study-ops/study-ops-specs.md:100:MCP-OP-STUDY-041
   docs/intent/study-ops/study-ops-specs.md:100:RETIRES the "source-free companion" contract
   docs/intent/study-ops/study-ops-specs.md:100:MCP-OP-STUDY-044
   docs/intent/study-ops/study-ops-specs.md:104:MCP-OP-STUDY-044
   docs/intent/study-ops/study-ops-specs.md:104:MCP-OP-STUDY-041
   docs/intent/study-ops/study-ops-specs.md:106:MCP-OP-STUDY-045
   docs/intent/study-ops/study-ops-specs.md:106:SUPERSEDES the `next call: <tool> <key>=<value> …` clause
   docs/intent/study-ops/study-ops-specs.md:106:MCP-OP-STUDY-036
   docs/intent/study-ops/study-ops-specs.md:108:MCP-OP-STUDY-046
   docs/intent/study-ops/study-ops-specs.md:152:MCP-OP-STUDY-036
   docs/intent/study-ops/study-ops-specs.md:152:MCP-OP-STUDY-045
   docs/intent/study-ops/study-ops-specs.md:155:MCP-OP-STUDY-041
   docs/intent/study-ops/study-ops-specs.md:159:MCP-OP-STUDY-044
   docs/intent/study-ops/study-ops-specs.md:160:MCP-OP-STUDY-045
   docs/intent/study-ops/study-ops-specs.md:161:MCP-OP-STUDY-046
   ```

   Exact audit command:

   ```sh
   ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :spec-count :violations])))"; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   Verbatim output:

   ```text
   {:ok true, :violations []}
   EXIT_CODE=0
   ```

11. **Small-read wire size increased in every mode, but no measured small shape is remotely near the public budget.** Before/after public bytes are: forms 1,618→2,082; outline 1,367→1,645; match 1,994→2,615; xray 1,588→2,290; deps 1,260→1,525; topo 1,126→1,397; ls-deps 1,224→1,488; ls-extract 1,319→1,584; ls-tree 796→885. The added text is noticeably noisier (especially xray), but the absolute maximum here is 2,615 bytes, so this probe finds no small-read budget problem. Token cost remains unmeasured; byte cost is not a token measurement. Files: `src/clj_surgeon/mcp_inspect.clj:513`, `src/clj_surgeon/mcp_inspect_tool.clj:1923`.

   Exact before command:

   ```sh
   cd /tmp/o2r3-review-fx/history && git checkout --quiet --detach a0b0520 && git rev-parse --short HEAD && clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/mode_cost_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim output:

   ```text
   a0b05206
   forms ok= true text_bytes= 316 public_bytes= 1618
   outline ok= true text_bytes= 307 public_bytes= 1367
   match ok= true text_bytes= 401 public_bytes= 1994
   xray ok= true text_bytes= 239 public_bytes= 1588
   deps ok= true text_bytes= 330 public_bytes= 1260
   topo ok= true text_bytes= 309 public_bytes= 1126
   ls-deps ok= true text_bytes= 305 public_bytes= 1224
   ls-extract ok= true text_bytes= 336 public_bytes= 1319
   ls-tree ok= true text_bytes= 276 public_bytes= 796
   EXIT_CODE=0
   ```

   Exact after command:

   ```sh
   cd /tmp/o2r3-review-fx/history && git checkout --quiet --detach e258519 && git rev-parse --short HEAD && clojure -M:clj-surgeon/mcp /tmp/o2r3-review-fx/mode_cost_probe.clj; probe_rc=$?; echo EXIT_CODE=$probe_rc
   ```

   Verbatim output:

   ```text
   e258519e
   forms ok= true text_bytes= 771 public_bytes= 2082
   outline ok= true text_bytes= 578 public_bytes= 1645
   match ok= true text_bytes= 1011 public_bytes= 2615
   xray ok= true text_bytes= 927 public_bytes= 2290
   deps ok= true text_bytes= 587 public_bytes= 1525
   topo ok= true text_bytes= 573 public_bytes= 1397
   ls-deps ok= true text_bytes= 562 public_bytes= 1488
   ls-extract ok= true text_bytes= 593 public_bytes= 1584
   ls-tree ok= true text_bytes= 360 public_bytes= 885
   EXIT_CODE=0
   ```

12. **All tip gates pass, but they do not cover the blockers above.** Files: `Makefile:1`, `test/clj_surgeon/mcp_study_test.clj:2910`, `test/clj_surgeon/mcp_study_test.clj:3144`.

   Exact commands and verbatim outputs:

   ```sh
   ~/bin/suite-run bb test/run_all.clj; suite_rc=$?; echo EXIT_CODE=$suite_rc
   ```

   ```text
   Ran 731 tests containing 6023 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; suite_rc=$?; echo EXIT_CODE=$suite_rc
   ```

   ```text
   Ran 476 tests containing 6235 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   ```sh
   make mcp-operation-oracle; gate_rc=$?; echo EXIT_CODE=$gate_rc
   ```

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   EXIT_CODE=0
   ```

   ```sh
   git diff --exit-code 4480e3d..HEAD -- test-fixtures/; diff_rc=$?; echo EXIT_CODE=$diff_rc
   ```

   ```text
   EXIT_CODE=0
   ```

13. **The requested RED→GREEN lineage is present for every pair, with one environmental qualification.** The intended §2, §3, §4, §5, and §6 witnesses are red at 6587c9d, ba6e29b, 4b38202, 72843e6, and 08e4490 respectively, and green at the following fixes 2f094aa, 30e5423, a08149a, 5b8b4a3, and 54c2f82. JVM runs used `~/bin/suite-run`. The 6587c9d full run also had one unrelated admission-wait flake. The historical 5b8b4a full run was contaminated by `/tmp` inode exhaustion, so I do not mislabel that run; I reran its two exact witnesses through `suite-run`, and they were green. The inode incident interrupted the later full-history loop, so §6 is reported from its independently rerun focused witnesses rather than invented full-suite output.

   Exact historical pair command:

   ```sh
   for spec in 6587c9d2:2f094aa5 ba6e29b4:30e5423c 4b38202f:a08149af 72843e65:5b8b4a39; do red=${spec%%:*}; green=${spec##*:}; for rev in "$red" "$green"; do git -C /tmp/o2r3-review-fx/history checkout --quiet --detach "$rev"; echo REV=$(git -C /tmp/o2r3-review-fx/history rev-parse --short HEAD); (cd /tmp/o2r3-review-fx/history && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test); echo EXIT_CODE=$?; done; done
   ```

   Verbatim terminal summaries:

   ```text
   REV=6587c9d2
   Ran 467 tests containing 6016 assertions.
   6 failures, 0 errors.
   EXIT_CODE=6
   REV=2f094aa5
   Ran 467 tests containing 6016 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=ba6e29b4
   Ran 469 tests containing 6037 assertions.
   10 failures, 0 errors.
   EXIT_CODE=10
   REV=30e5423c
   Ran 470 tests containing 6044 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=4b38202f
   Ran 473 tests containing 6083 assertions.
   2 failures, 1 errors.
   EXIT_CODE=3
   REV=a08149af
   Ran 473 tests containing 6084 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=72843e65
   Ran 475 tests containing 6093 assertions.
   5 failures, 0 errors.
   EXIT_CODE=5
   REV=5b8b4a39
   Ran 475 tests containing 6093 assertions.
   61 failures, 37 errors.
   EXIT_CODE=98
   ```

   Exact focused 5b8b4a confirmation command:

   ```sh
   cd /tmp/o2r3-review-fx/history && git checkout --quiet --detach 5b8b4a39 && ~/bin/suite-run clojure -Sdeps '{:paths ["src" "test" "dev/experiments"]}' -M:clj-surgeon/mcp /tmp/o2r3-review-fx/focused_test_runner.clj the-refusal-ratchet-drives-every-reason-the-source-constructs a-refusal-cause-is-bounded-and-still-travels; suite_rc=$?; echo EXIT_CODE=$suite_rc
   ```

   Verbatim output:

   ```text
   COUNTERS {:test 2, :pass 9, :fail 0, :error 0}
   EXIT_CODE=0
   ```

   Exact focused §6 command:

   ```sh
   for rev in 08e44904 54c2f827; do git checkout --quiet --detach "$rev"; echo REV=$(git rev-parse --short HEAD); ~/bin/suite-run clojure -Sdeps '{:paths ["src" "test" "dev/experiments"]}' -M:clj-surgeon/mcp /tmp/o2r3-review-fx/focused_test_runner.clj a-key-named-in-the-structural-set-is-not-a-free-pass every-refusal-kind-text-carries-every-structured-content-leaf; suite_rc=$?; echo EXIT_CODE=$suite_rc; done
   ```

   Verbatim terminal summary (the RED run printed the 17 individual assertion failures before this counter):

   ```text
   REV=08e44904
   COUNTERS {:test 2, :pass 14, :fail 17, :error 0}
   EXIT_CODE=17
   REV=54c2f827
   COUNTERS {:test 2, :pass 31, :fail 0, :error 0}
   EXIT_CODE=0
   ```

## NO-GO

This tip is not GO on its own for MCP/main because the stated leaf-superset and exhaustive-refusal contracts are reproducibly false; recomposition onto MEM-003 landing `dd9d8b9` must re-fit and measure the final published envelope after the wire moves `elapsed_ms` to `measured.elapsed_ms`, rather than assuming the current 64-byte reserve survives the added/nested measured fields.
