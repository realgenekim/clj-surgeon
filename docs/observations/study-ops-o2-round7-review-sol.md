## NO-GO

1. Review subject and repository state. I reviewed the round-four, round-five, and round-six verdicts before testing, inspected both requested commit ranges and their diffs, and kept the repository detached and unchanged throughout.

   Exact command:

   ```text
   git rev-parse HEAD && git status --short --branch
   ```

   Verbatim output:

   ```text
   f572e461c218733e8f64788c25dd1183dd0c8792
   ## HEAD (no branch)
   ```

   Final cleanup/re-proof command:

   ```sh
   review_fx=/var/tmp/forge/o2r7-review-fx; if [ "$review_fx" = /var/tmp/forge/o2r7-review-fx ] && [ -d "$review_fx" ]; then find "$review_fx" -depth -delete; fi; if [ -e "$review_fx" ]; then echo FIXTURES_REMAIN; else echo FIXTURES_REMOVED; fi; git rev-parse HEAD; git status --short --branch; for port in 8150 8151 8152; do if lsof -nP -iTCP:$port -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_${port}_STILL_LISTENING; else echo PORT_${port}_STOPPED; fi; done
   ```

   Verbatim output:

   ```text
   FIXTURES_REMOVED
   f572e461c218733e8f64788c25dd1183dd0c8792
   ## HEAD (no branch)
   PORT_8150_STOPPED
   PORT_8151_STOPPED
   PORT_8152_STOPPED
   ```

2. **BLOCKING — two distinct receipt leaves can still render the identical pointer/value line, so the public declaration can count one as dropped while the audit calls it carried.** `leaf-label` concatenates unescaped dotted segments, hence top-level key `"a.b"` and nested path `[:a :b]` both become `a.b`; `text-line-index` is a set, so one rendered `a.b: value` line discharges both leaves. Across the allowance band there are 587 declaration/audit disagreements; the first is 19 declared versus 18 audited. This directly violates MCP-OP-STUDY-047/051 and the review's blocking rule. Files: `src/clj_surgeon/mcp_inspect.clj:594`, `src/clj_surgeon/mcp_inspect.clj:705`, `src/clj_surgeon/mcp_inspect.clj:724`, `src/clj_surgeon/mcp_inspect.clj:738`, `src/clj_surgeon/mcp_inspect.clj:936`.

   Exact command:

   ```sh
   timeout 60s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r7-review-fx/attacks.clj skip-100k; code=$?; echo ATTACK_EXIT_CODE=$code
   ```

   Verbatim output (collision section):

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   == two distinct leaves with identical pointer+value ==
   labels= [tail7 tail17 a.b tail4 tail12 tail5 tail13 tail11 tail6 tail10 tail18 tail1 tail15 tail0 tail3 tail9 tail16 tail8 tail2 tail14 a.b tail19]
   mismatch_count= 587
   first_mismatch= {:budget 286, :shown 3, :total 22, :declared 19, :audited 18, :section "  receipt facts · 3 of 22 rendered · the complete receipt is in structuredContent\n  dropped: tail4, tail12, tail5, tail13, tail11, tail6, tail10, tail18 (+11 more)\n  tail7: a-distinctive-tail-value-7\n  tail17: a-distinctive-tail-value-17\n  a.b: the-same-distinctive-value-rendered-twice"}
   ```

3. **BLOCKING — an unescaped newline in a pointer makes a public text face claim `10 of 10 rendered` while its structured face contains one uncarried, undeclared leaf.** The complete public pair fits at 761 bytes and is returned normally; the emitted fact is split into `bad` and `key: …`, while `leaf-carried?` searches for the unsplit whole string. A colon-only segment stays internally self-consistent, although its `bad:key: value` syntax remains ambiguous. Files: `src/clj_surgeon/mcp_inspect.clj:576-603`, `src/clj_surgeon/mcp_inspect.clj:705-756`, `src/clj_surgeon/mcp_inspect.clj:784-795`.

   Exact command: the command in finding 2.

   Verbatim output (pointer section):

   ```text
   == public result with newline and colon pointer segments ==
   newline leaf_label= "bad\nkey" bytes= 761 declared= 0 audited= 1 uncarried= [[["bad\nkey"] "the-distinctive-pointer-value"]]
   text= "inspect_clojure\n  1 request · 1 file\n\n✓ all requests resolved\n✓ ordered snapshot\n✓ hashes attached\n✓ terminal evidence · read_complete=true · next action none\n\n  receipt facts · 10 of 10 rendered\n  source_character_count=0\n  elapsed_ms=0.0\n  read_complete=true\n  file_count=1\n  operation=inspect_clojure\n  next_action=none\n  ok=true\n  request_count=1\n  bad\nkey: the-distinctive-pointer-value\n  results=[]\n  0 source characters · 0.00 ms"
   colon leaf_label= "bad:key" bytes= 759 declared= 0 audited= 0 uncarried= []
   text= "inspect_clojure\n  1 request · 1 file\n\n✓ all requests resolved\n✓ ordered snapshot\n✓ hashes attached\n✓ terminal evidence · read_complete=true · next action none\n\n  receipt facts · 10 of 10 rendered\n  source_character_count=0\n  elapsed_ms=0.0\n  read_complete=true\n  bad:key: the-distinctive-pointer-value\n  file_count=1\n  operation=inspect_clojure\n  next_action=none\n  ok=true\n  request_count=1\n  results=[]\n  0 source characters · 0.00 ms"
   ```

4. The named round-six failures are otherwise closed. The real two-source result and a one-fact-larger result both deterministically select fitting ordinary candidates, with declaration equal to audit; at this larger round-seven source tip the ordinary candidate is 32,766 bytes (two bytes of headroom), not the earlier claimed 13. The pointer-valued fixture is 29/29 at every one of 964 integer allowances in its complete band; the decoy and twin plants are clean at all 501 budgets tested, including the budgets omitted by the committed step-four sweep. Zero allowance and allowance exactly equal to the 103-character declaration preserve the declaration. Pointer-syntax-in-a-value, nested-vector boolean, numeric string `"42"`, and a namespaced keyword whose name itself contains a slash also pass.

   Files: `src/clj_surgeon/mcp_inspect.clj:497-603`, `src/clj_surgeon/mcp_inspect.clj:705-795`, `src/clj_surgeon/mcp_inspect.clj:801-843`, `src/clj_surgeon/mcp_inspect.clj:903-1046`, `src/clj_surgeon/mcp_inspect_tool.clj:2122-2203`.

   Exact command:

   ```sh
   cp_value=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$cp_value" clojure.main /var/tmp/forge/o2r7-review-fx/batch-metrics.clj; code=$?; echo BATCH_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   files= ["src/clj_surgeon/mcp_inspect_tool.clj" "src/clj_surgeon/mcp_inspect.clj"]
   ordinary= {:bytes 32766, :headroom 2, :limit 5920, :omitted nil, :text_chars 6248, :declared 1235, :audited 1235, :deterministic true}
   plus_one= {:bytes 32742, :headroom 26, :limit 5844, :omitted nil, :text_chars 6176, :declared 1238, :audited 1238, :deterministic true}
   BATCH_EXIT_CODE=0
   ```

   Exact command: the command in finding 2.

   Verbatim output (remaining carriage attacks):

   ```text
   == namespaced keyword whose name itself contains slash ==
   original= :src/dir/demo.clj namespace= src name= dir/demo.clj label= file_hashes.src/dir/demo.clj
   normalized= :src/dir/demo.clj namespace= src name= dir/demo.clj equal= true same_spelling= true

   == every integer allowance for pointer-valued leaves ==
   allowances_tested= 964 complete_section_chars= 943 mismatch_count= 0 first_mismatch= nil

   == decoy and twin at every budget, including outside planted sweep ==
   decoy_budgets_tested=501 mismatch_count= 0 first= nil
   twin_budgets_tested=501 mismatch_count= 0 first= nil
   ```

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp -e '(require (quote [clj-surgeon.mcp-inspect :as i])) (let [r {:alpha (apply str (repeat 100 "a")) :beta (apply str (repeat 100 "b"))} d (i/fact-block "structural" r 0) s (i/fact-section d) exact (count s) e (i/fact-block "structural" r exact)] (println "allowance=0" "section_chars=" (count s) "shown=" (:shown d) "declared=" (count (:dropped-labels d)) "audited=" (count (i/uncarried-leaves (str "structural\n" s) r)) "section=" (pr-str s)) (println "allowance=declaration_length" "allowance=" exact "section_chars=" (count (i/fact-section e)) "shown=" (:shown e) "declared=" (count (:dropped-labels e)) "audited=" (count (i/uncarried-leaves (str "structural\n" (i/fact-section e)) r))))'; code=$?; echo FLOOR_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   allowance=0 section_chars= 103 shown= 0 declared= 2 audited= 2 section= "  receipt facts · 0 of 2 rendered · the complete receipt is in structuredContent\n  dropped: alpha, beta"
   allowance=declaration_length allowance= 103 section_chars= 103 shown= 0 declared= 2 audited= 2
   FLOOR_EXIT_CODE=0
   ```

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp -e '(require (quote [clj-surgeon.mcp-inspect :as i])) (let [r {:decoy "probe.target=42" :probe {:target 42} :nested [[true]] :numeric_string "42"} x (i/fact-block "" r i/unbounded-evidence) s (i/fact-section x) bad (for [b (range 0 501) :let [q (i/fact-block "" r b) t (or (i/fact-section q) "") d (count (:dropped-labels q)) a (count (i/uncarried-leaves t r))] :when (not= d a)] [b d a])] (println "complete_section=" (pr-str s)) (println "budgets_tested=501 mismatch_count=" (count bad) "first=" (pr-str (first bad))))'; code=$?; echo POINTER_SYNTAX_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   complete_section= "  receipt facts · 4 of 4 rendered\n  decoy=probe.target=42\n  probe.target=42\n  nested[0][0]=true\n  numeric_string=42"
   budgets_tested=501 mismatch_count= 0 first= nil
   POINTER_SYNTAX_EXIT_CODE=0
   ```

5. The production-cap performance path is bounded but the broader affordability claim is not. Ten thousand tiny facts refuse with a fitting 1,525-byte typed result in 425.59 ms. One hundred thousand facts also refuse fitting, but take 3,308.55 ms, over the requested two-second comparison. More importantly, forcing 10,000 facts onto a genuinely fitted, non-refusal path by raising the cap to 220,000 takes 9,128.15 ms. That lifted cap is not the shipped 32 KB contract, so this is not an additional public-cap correctness blocker; it does show that the committed witness measures only the cheap refusal path and cannot support a general “affordable in number of facts” claim. Files: `src/clj_surgeon/mcp_inspect.clj:936-1046`, `src/clj_surgeon/mcp_inspect_tool.clj:2122-2203`, `docs/intent/study-ops/study-ops-specs.md:115`.

   Exact commands:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r7-review-fx/perf100k.clj 10000; code=$?; echo PERF10K_EXIT_CODE=$code
   timeout 15s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r7-review-fx/perf100k.clj; code=$?; echo PERF100K_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   facts= 10000 elapsed_ms= 425.59 ok= false error_type= inspect-output-limit bytes= 1525 fits= true
   PERF10K_EXIT_CODE=0
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   facts=100000 elapsed_ms= 3308.55 ok= false error_type= inspect-output-limit bytes= 1531 fits= true
   PERF100K_EXIT_CODE=0
   ```

   Exact command for the fitted 10,000-fact path: the command in finding 2.

   Verbatim output:

   ```text
   == 10k fitted path and 100k real-cap path ==
   {:facts 10000, :cap 220000, :elapsed_ms 9128.151964, :ok true, :error_type nil, :text_omitted nil, :bytes 219880, :fits true}
   100k=SKIPPED_IN_THIS_PASS
   ```

6. The round-six envelope and CLI-call-site fixes survive. A domain receipt carrying both envelope key names cannot forge the request clock across JSON serialization: the published and wire top-level clock is 1.25 while the nested domain value remains 888888.0. An oversized domain `measured` becomes a fitting 1,521-byte typed refusal, and the real HTTP carriage witness passes on explicit port 8151. `run-ls-tree` still has one production registry caller plus the valid-root memory battery, and the CLI launcher exits 1 with bounded typed EDN for an invalid root. Files: `src/clj_surgeon/mcp_operation.clj:27-112`, `src/clj_surgeon/mcp_operation.clj:136-169`, `src/clj_surgeon/mcp_inspect_tool.clj:2043-2113`, `src/clj_surgeon/core.clj:176`, `src/clj_surgeon/core.clj:533`, `src/clj_surgeon/core.clj:1043`.

   Exact command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r7-review-fx/envelope-wire.clj; code=$?; echo ENVELOPE_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   == forged domain clocks across the wire ==
   envelope_keys= #{:measured :elapsed_ms} published_top_elapsed= 1.25 published_nested_elapsed= 888888.0 wire_top_elapsed= 1.25 wire_nested_elapsed= 888888.0 request_elapsed_wire= 1.25 wire_finalized= false bytes= 784
   == receipt carrying every envelope key name and an oversized domain measured ==
   ok= false error_type= inspect-output-limit domain_measured_preserved= false published_bytes= 1521 budget= 32768 fits= true
   ENVELOPE_EXIT_CODE=0
   ```

   Exact command:

   ```sh
   cp_value=$(clojure -Spath -M:clj-surgeon/mcp-test); if lsof -nP -iTCP:8151 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8151_BUSY; exit 1; else echo PORT_8151_FREE; fi; ~/bin/suite-run java -cp "$cp_value" clojure.main /var/tmp/forge/o2r7-review-fx/run-http-witness.clj 8151; code=$?; echo HTTP_WITNESS_EXIT_CODE=$code; if lsof -nP -iTCP:8151 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8151_STILL_LISTENING; else echo PORT_8151_STOPPED; fi
   ```

   Verbatim output:

   ```text
   PORT_8151_FREE
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Ran 1 tests containing 21 assertions.
   0 failures, 0 errors.
   HTTP_WITNESS_EXIT_CODE=0
   PORT_8151_STOPPED
   ```

   Exact command:

   ```sh
   rg -n "run-ls-tree" --glob '!docs/**' .; bb -cp src -m clj-surgeon.core :op :ls-tree :dir /var/tmp/forge/o2r7-review-fx/no-such-dir :format :edn; code=$?; echo CLI_EXIT_CODE=$code
   ```

   Verbatim output (production hits and CLI result):

   ```text
   ./src/clj_surgeon/core.clj:176:(defn run-ls-tree
   ./src/clj_surgeon/core.clj:533:    :ls-tree          {:handler   run-ls-tree
   ./src/clj_surgeon/memory_battery_runner.clj:52:    :entrance "clj-surgeon.core/run-ls-tree {:dir root :format :edn}"
   ./src/clj_surgeon/memory_battery_runner.clj:56:           ((requiring-resolve 'clj-surgeon.core/run-ls-tree)
   {:error
    ":ls-tree :dir must be an existing directory: \"/var/tmp/forge/o2r7-review-fx/no-such-dir\"",
    :error-type :workspace-root-not-a-directory,
    :dir "/var/tmp/forge/o2r7-review-fx/no-such-dir",
    :next-action "pass_an_existing_directory_path"}
   CLI_EXIT_CODE=1
   ```

7. The text-growth change is mechanically tolerable only if it buys the carriage invariant; findings 2–3 show it still does not. On the current committed CI toy, ten files remain inside MCP-OP-STUDY-037's literal requirement at 4,771 text characters, while the 25-file witness is 11,546 characters—over 8 KB but below the new 12 KB test. Thus the branch did not actually falsify STUDY-037's stated ten-file/30-form EARS sentence; it relaxed a second 25-file witness annotated to that ID, while STUDY-051 claims to supersede STUDY-037 without editing the older active requirement. The “growth ratchet” is a concrete CI ceiling for this one fixture, not a general product ceiling, and calling it “not a contract” makes it inadequate as a replacement public guarantee.

   The fixed-fixture cost table (outline 2,984 / deps 2,398 / topo 2,042 / ls-tree 1,424; +22.5% ls-tree over round six) is declared but its external fixture is not retained, so I can verify the product direction and the committed 25-file measurement, not reconstruct those exact four numbers from repository artifacts. Doubling `ls-tree format=text` from 4,334 to 8,796, relaxing its golden, and changing `read_complete`/fact labels are intentional public behavior changes. I find no explicit Gene acceptance in the reviewed record. The `Co-Authored-By: Gene Kim` trailer records authorship, not an acceptance decision. Standing alone this is **GO-WITH-FIX pending Gene's explicit acceptance and an unambiguous intent edit**; with findings 2–3 the tip remains NO-GO. Files: `docs/intent/study-ops/study-ops-specs.md:91`, `docs/intent/study-ops/study-ops-specs.md:118`, `test/clj_surgeon/mcp_study_test.clj:1881-1918`.

   Exact command:

   ```sh
   cp_value=$(clojure -Spath -M:clj-surgeon/mcp-test); timeout 150s ~/bin/suite-run java -cp "$cp_value" clojure.main /var/tmp/forge/o2r7-review-fx/ls-tree-size.clj; code=$?; echo SIZE_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   files= 10 read_complete= true returned= 10 limit= 8192 text_chars= 4771 structured_chars= 2486 public_bytes= 7431 under_8k= true under_12k= true
   files= 25 read_complete= true returned= 25 limit= 8192 text_chars= 11546 structured_chars= 5806 public_bytes= 17721 under_8k= false under_12k= true
   SIZE_EXIT_CODE=0
   ```

   Exact command:

   ```sh
   git show -s --format='%H%n%B' f572e461
   ```

   Verbatim output:

   ```text
   f572e461c218733e8f64788c25dd1183dd0c8792
   study-ops: O2r7 (§6, §10) — state the byte cost, the product change, and the MEM-003 absorption

   The carriage rule costs about 1% on a file read (22 bytes) and 23% on
   ls-tree, measured at three revisions on one fixed fixture, on top of the
   16-59% round five's collidable-label rule cost. The text is no longer a
   summary containing the receipt; it is the receipt with the rows in front of
   it, and MCP-OP-STUDY-037's 8 KB text pass-line is superseded by the public
   output budget plus a growth ratchet. All of that is now in the intent.

   MEM-003 (432268cf) dry-runs to TEN conflicts from this tip rather than seven,
   because this branch absorbed the trunk and MEM-003 has not; three of the ten
   are MEM-003 against the trunk. Per-file absorption recorded.

   Co-Authored-By: Gene Kim <genek@itrevolution.com>
   ```

8. The history is honest. The round-seven first-parent lineage is the two REDs, one GREEN, the sweep repair, the trunk merge, and the observation. The round-seven REDs fail for the stated pointer/declaration and decoy/twin reasons and pass at `eee4283e`. I also re-ran the round-five and round-six focused lineage: `9bbe2bc1` 11/20 red → `aa8bfe5d` 0/20; `532c76fb` 16/24 red → `0309f846` 0/24; `fe0a4a2e` 1 error → `8210e5c4` 0/11; `28ae1897` 3/8 red → `42cff0ff` 0/8; `b410e31b` 11 failures + 2 errors/17 → `0362a4f9` 0/19; `8b2a4aa5` 4/6 red → `468ca52e` 0/6; `6bed20b0` 18,015.99 ms and 1/4 red → `dafc7f37` 0/4. The two textual conflicts in merge `8e385602` are true unions: both spec-doc entries remain in `mcp_intent_contract_test.clj`, and both the O2 explicit-port recipe checks and trunk child-heap execution check remain in `mcp_heap_config_test.sh`; I found no weakened trunk witness. Files: commits `9bbe2bc1..f572e461`, `test/clj_surgeon/mcp_study_test.clj:4580-4825`, `test/clj_surgeon/mcp_intent_contract_test.clj:204-238`, `test/mcp_heap_config_test.sh:60-127`.

   Exact command:

   ```sh
   git log --oneline --first-parent dafc7f37..f572e461
   ```

   Verbatim output:

   ```text
   f572e461 study-ops: O2r7 (§6, §10) — state the byte cost, the product change, and the MEM-003 absorption
   8e385602 merge: origin/MCP/main into bridge/study-ops-mcp (O2 round seven)
   c9a783d7 study-ops: O2r7 (§3) — sweep the plant witnesses across the budget band
   eee4283e study-ops: O2r7 GREEN (§2, §3) — a leaf is carried by its own pointer line and by nothing else
   ac9cd03b study-ops: O2r7 RED (§3) — a substring of a decoy is not a rendering of a fact
   c7defa74 study-ops: O2r7 RED (§2) — a pointer spells the leaf's address, never its value
   ```

   Exact command (round-seven focused lineage, run in the disposable history clone):

   ```sh
   for spec in c7defa74:pointer eee4283e:pointer ac9cd03b:plants eee4283e:plants; do rev=${spec%%:*}; group=${spec##*:}; git checkout --quiet --detach "$rev"; cp_value=$(clojure -Spath -M:clj-surgeon/mcp-test); echo REV=$(git rev-parse --short HEAD) GROUP=$group; if [ "$group" = pointer ]; then vars='a-dropped-pointer-is-not-carriage-of-the-value-it-names the-name-rung-declares-exactly-what-its-own-audit-finds'; else vars='a-value-inside-a-longer-decoy-is-not-a-rendered-fact one-value-at-two-pointers-is-two-independently-removable-facts'; fi; ~/bin/suite-run java -cp "$cp_value" clojure.main /var/tmp/forge/o2r7-review-fx/run-vars.clj $vars; code=$?; echo EXIT_CODE=$code; done
   ```

   Verbatim summaries:

   ```text
   REV=c7defa74 GROUP=pointer
   Ran 2 tests containing 26 assertions.
   14 failures, 0 errors.
   EXIT_CODE=1
   REV=eee4283e GROUP=pointer
   Ran 2 tests containing 26 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=ac9cd03b GROUP=plants
   Ran 2 tests containing 17 assertions.
   4 failures, 0 errors.
   EXIT_CODE=1
   REV=eee4283e GROUP=plants
   Ran 2 tests containing 18 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   The first round-seven RED's verbatim failure includes:

   ```text
   the block declares 1 dropped while the audit finds 0 uncarried: the `dropped:` pointer satisfied the value's own carriage test; section "  receipt facts · 0 of 1 rendered · the complete receipt is in structuredContent\n  dropped: abcdefghijklmnop"
   the name rung declares 29 omitted facts while its own audit finds 25; the leaves it counts as carried are named by the `dropped:` line and rendered nowhere: ("pointervalue0004" "pointervalue0010" "pointervalue0017" "pointervalue0019")
   ```

   The second round-seven RED's verbatim failure includes:

   ```text
   budget 90: 1 leaves counted as rendered have no pointer line of their own — ["target"]; section "  receipt facts · 3 of 3 rendered\n  ok=true\n  decoy: XXabcdefghijklmnopYY"
   budget 100: 1 leaves counted as rendered have no pointer line of their own — ["beta"]; section "  receipt facts · 3 of 3 rendered\n  ok=true\n  alpha: the-same-distinctive-value-rendered-twice"
   ```

   Exact merge-resolution inspection:

   ```sh
   git show --remerge-diff --format= 8e385602 -- test/clj_surgeon/mcp_intent_contract_test.clj test/mcp_heap_config_test.sh | rg -n 'study-ops-specs|temp-dir-hygiene-specs|default 7888|EXECUTION|MCP heap configuration regression passed|MCP-OP-STUDY-001..051'
   ```

   Verbatim output:

   ```text
   11:    "docs/intent/study-ops/study-ops-specs.md"
   13:    "docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md"
   25:-   "docs/intent/study-ops/study-ops-specs.md"])
   27:+   ;; through `inspect_clojure` (MCP-OP-STUDY-001..051).
   28:+   "docs/intent/study-ops/study-ops-specs.md"
   29:    "docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md"])
   45: # default 7888 regardless of MCP_PORT and failed "Address already in use"
   52: # EXECUTION, not printed recipe text. Every assertion above reads `make -n`
   60: echo "MCP heap configuration regression passed"
   ```

9. The declared `stop-child!` flake did not reproduce: 0 failing namespace runs out of 10, 30 tests / 270 assertions total. The wrapper changed only child command `:port` values to explicit port 8150; the port was free before and stopped afterward. Files: `test/clj_surgeon/mcp_prepared_wire_test.clj:30-48`, `test/clj_surgeon/mcp_prepared_wire_test.clj:205-246`.

   Exact command:

   ```sh
   cp_value=$(clojure -Spath -M:clj-surgeon/mcp-test); if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_BUSY; exit 1; else echo PORT_8150_FREE; fi; ~/bin/suite-run java -cp "$cp_value" clojure.main /var/tmp/forge/o2r7-review-fx/run-prepared-ns.clj 8150 10; code=$?; echo PREPARED_EXIT_CODE=$code; if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_STILL_LISTENING; else echo PORT_8150_STOPPED; fi
   ```

   Verbatim output:

   ```text
   PORT_8150_FREE
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   RUN=1
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=2
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=3
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=4
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=5
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=6
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=7
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=8
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=9
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   RUN=10
   Testing clj-surgeon.mcp-prepared-wire-test
   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   FAILURE_COUNT=0
   PREPARED_EXIT_CODE=0
   PORT_8150_STOPPED
   ```

10. All requested repository gates are green. The fixture history matches round six: `4480e3d..HEAD` is nonzero because the first trunk merge brought exactly 58 fixture paths; `580e167a..HEAD` is zero; `972cf4c2..HEAD` differs only by those same 58 names; round seven itself changes no fixture. Files: `Makefile:1`, `test/run_all.clj:1`, `test/clj_surgeon/mcp_test_runner.clj:1`, `test-fixtures/`.

    Exact command:

    ```sh
    ~/bin/suite-run bb test/run_all.clj; code=$?; echo BB_EXIT_CODE=$code
    ```

    Verbatim terminal output:

    ```text
    Ran 854 tests containing 6882 assertions.
    0 failures, 0 errors.
    BB_EXIT_CODE=0
    ```

    Exact command:

    ```sh
    ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; code=$?; echo MCP_TEST_EXIT_CODE=$code
    ```

    Verbatim terminal output:

    ```text
    Ran 893 tests containing 12901 assertions.
    0 failures, 0 errors.
    MCP_TEST_EXIT_CODE=0
    ```

    Exact commands:

    ```sh
    make mcp-operation-oracle; code=$?; echo ORACLE_EXIT_CODE=$code
    ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :spec-count :violations])))"; code=$?; echo AUDIT_EXIT_CODE=$code
    ```

    Verbatim output:

    ```text
    # @spec MCP-OP-ORACLE-001
    swipl -q -f test/mcp_operation_contract_oracle.pl
    mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
    ORACLE_EXIT_CODE=0
    Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
    Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
    {:ok true, :violations []}
    AUDIT_EXIT_CODE=0
    ```

    Exact command:

    ```sh
    git diff --exit-code 4480e3d..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_4480_EXIT_CODE=$code; git diff --exit-code 580e167a..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_POST_R6_MERGE_EXIT_CODE=$code; git diff --exit-code 972cf4c2..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_SINCE_R5_EXIT_CODE=$code; echo FIXTURES_SINCE_R5_COUNT=$(git diff --name-only 972cf4c2..HEAD -- test-fixtures/ | wc -l); echo FIXTURES_FIRST_MERGE_COUNT=$(git diff --name-only 972cf4c2..580e167a -- test-fixtures/ | wc -l); diff -u <(git diff --name-only 972cf4c2..HEAD -- test-fixtures/) <(git diff --name-only 972cf4c2..580e167a -- test-fixtures/); code=$?; echo FIXTURE_NAMESETS_DIFF_EXIT_CODE=$code; git diff --exit-code dafc7f37..HEAD -- test-fixtures/ >/dev/null; code=$?; echo ROUND7_FIXTURES_EXIT_CODE=$code
    ```

    Verbatim output:

    ```text
    FIXTURES_4480_EXIT_CODE=1
    FIXTURES_POST_R6_MERGE_EXIT_CODE=0
    FIXTURES_SINCE_R5_EXIT_CODE=1
    FIXTURES_SINCE_R5_COUNT=58
    FIXTURES_FIRST_MERGE_COUNT=58
    FIXTURE_NAMESETS_DIFF_EXIT_CODE=0
    ROUND7_FIXTURES_EXIT_CODE=0
    ```

11. Mergeability: this tip merges cleanly into current `origin/MCP/main` at `38f0f95c`. It does not compose cleanly onto current MEM-003 at `432268cf`: ten conflicts, matching the builder's table. MEM-003 should absorb trunk first because four current conflicts (`core.clj`, `mcp_server.clj`, `mcp_tool.clj`, `mcp_alias_migration_test.clj`) are its stale-trunk problem; after that, resolve the O2/MEM overlap as follows: take this tip's carriage/budget implementation in `mcp_inspect.clj`; take this tip in `mcp_inspect_tool.clj` and layer MEM-003's measured-domain fields; take MEM-003's nested `measured` wire shape in `mcp_operation.clj` and reapply construction-stamped envelope identity; union both witness sets in `core_discovery_test.clj` and `mcp_inspect_tool_test.clj`; union `test/run_all.clj`. The brief's older exact `694f538d` is no longer the remote tip and produces twelve conflicts, adding `memory_battery_runner.clj` and `mcp_process_test.clj`. Files: the conflict names below and `docs/observations/2026-09-04-o2-round-seven-carriage-by-the-leafs-own-line.md:76-100`.

    Exact command:

    ```sh
    set -o pipefail; echo MCP_MAIN=$(git rev-parse origin/MCP/main); git merge-tree --write-tree HEAD origin/MCP/main 2>&1 | rg '^[0-9a-f]{40}$|^CONFLICT'; code=${PIPESTATUS[0]}; echo MCP_MAIN_EXIT_CODE=$code; echo MEM003=$(git rev-parse origin/bridge/integration-2026-09-03-mem003); git merge-tree --write-tree HEAD origin/bridge/integration-2026-09-03-mem003 2>&1 | rg '^[0-9a-f]{40}$|^CONFLICT'; code=${PIPESTATUS[0]}; echo MEM003_EXIT_CODE=$code; echo MEM003_BRIEF=$(git rev-parse 694f538d); git merge-tree --write-tree HEAD 694f538d 2>&1 | rg '^[0-9a-f]{40}$|^CONFLICT'; code=${PIPESTATUS[0]}; echo MEM003_694F_EXIT_CODE=$code
    ```

    Verbatim output:

    ```text
    MCP_MAIN=38f0f95cf28b564464bf2e5d97825a10c15c43a9
    81d5e93fb027bec2dfb61fdbc8d888af245f671f
    MCP_MAIN_EXIT_CODE=0
    MEM003=432268cf40997ef7694ad44d8a6f7ff06e18ce35
    a18f3ddd3295da885f3405e68bb463371101c91b
    CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_operation.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_server.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_tool.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/core_discovery_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_alias_migration_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
    CONFLICT (content): Merge conflict in test/run_all.clj
    MEM003_EXIT_CODE=1
    MEM003_BRIEF=694f538d235c7bd5f9bab4153a313d15d0f867ab
    ee6a13b66a7bdc4b43916a54150b41565ae70452
    CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_operation.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_server.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_tool.clj
    CONFLICT (content): Merge conflict in src/clj_surgeon/memory_battery_runner.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/core_discovery_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_alias_migration_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_process_test.clj
    CONFLICT (content): Merge conflict in test/run_all.clj
    MEM003_694F_EXIT_CODE=1
    ```

## NO-GO
This tip is not GO on its own for MCP/main because two distinct structured leaves can collapse to one unescaped dotted pointer line and a newline-bearing pointer produces a fitting public result with an undeclared leaf; it also does not yet compose onto MEM-003 without the ten explicit conflict resolutions above.
