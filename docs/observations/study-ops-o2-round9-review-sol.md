## NO-GO

1. **BLOCKING — MCP-OP-STUDY-052 is still false: the empty-string segment is not encoded, so two distinct leaves render the same pointer and a fitting public result declares 32 dropped while its own audit finds 31.** The distinct paths `["" 0]` (an array below the empty top-level JSON key) and `[0]` (the numeric top-level JSON key, serialized as object key `"0"`) both render as `[0]`. This is not only an abstract collision: at padding 31,100, `fit-public-result` deterministically returns a fitting 32,731-byte ordinary result whose text renders one `[0]` line, names the other `[0]` as dropped, and thereby treats both structured leaves as carried. The committed alphabet omits `""`, so its 5,219-path witness cannot see the defect. Files: `src/clj_surgeon/mcp_inspect.clj:577`, `src/clj_surgeon/mcp_inspect.clj:626`, `src/clj_surgeon/mcp_inspect.clj:639`, `src/clj_surgeon/mcp_inspect.clj:812`, `src/clj_surgeon/mcp_inspect.clj:822`, `test/clj_surgeon/mcp_study_test.clj:4847`, `docs/intent/study-ops/study-ops-specs.md:120`.

   Exact command:

   ```sh
   timeout 120s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r9-review-fx/repo/pointer-public-attack.clj; code=$?; echo POINTER_PUBLIC_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   abstract_paths= [["" 0] [0]]
   abstract_labels= ["[0]" "[0]"] injective= false
   public_mismatch_count= 2
   first_public_mismatch= {:padding 31100, :bytes 32731, :limit 237, :omitted nil, :declared 32, :audited 31, :labels ["[0]" "[0]"], :text "inspect_clojure\n  1 request · 0 files\n\n✓ all requests resolved\n✓ ordered snapshot\n✓ hashes attached\n! text abridged · read_complete=true · next action read_structured_content_or_narrow_request\n\n  receipt facts · 1 of 33 rendered · the complete receipt is in structuredContent\n  dropped: [0], tail7, tail17, source_character_count, tail4, tail12, elapsed_ms, read_complete (+24 more)\n  [0]: the-same-distinctive-value-rendered-twice\n  0 source characters · 0.00 ms"}
   POINTER_PUBLIC_EXIT_CODE=0
   ```

2. The rest of the requested pointer/value matrix is mostly sound, but MCP-OP-STUDY-053 leaves Unicode line/paragraph separators raw. The 29-leaf pointer-valued receipt is declaration-equal-to-audit at all 944 integer allowances; the decoy and twin plants are clean at all 501 budgets, including the unplanted gaps; the namespaced-keyword slash, nested boolean, numeric string `"42"`, decimal string versus index, and trailing backslash all pass. `\r\n`, `\n`, `\r`, and a literal backslash-`n` map to distinct value lines. However U+2028 and U+2029 remain raw, and a Unicode-aware `\R` splitter sees two lines where `text-line-index` sees one. That is a real “single line by construction” contract gap, although not a second bright-line omission: the characters remain present and the product audit still finds the leaf. Files: `src/clj_surgeon/mcp_inspect.clj:590`, `src/clj_surgeon/mcp_inspect.clj:750`, `src/clj_surgeon/mcp_inspect.clj:777`, `docs/intent/study-ops/study-ops-specs.md:122`, `test/clj_surgeon/mcp_study_test.clj:4908`.

   Exact command:

   ```sh
   edge_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); timeout 120s ~/bin/suite-run java -cp "$edge_cp" clojure.main /var/tmp/forge/o2r9-review-fx/edge-attacks.clj; code=$?; echo EDGE_ATTACKS_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   pointer_value_total= 29 allowances_tested= 944 complete_section_chars= 943 mismatch_count= 0 first= nil
   decoy budgets_tested= 501 mismatch_count= 0 first= nil
   twin budgets_tested= 501 mismatch_count= 0 first= nil
   namespaced_keyword_path= [:file_hashes :src/dir/demo.clj] label= "file_hashes.src~1dir~1demo~2clj"
   nested_and_numeric_section= "  receipt facts · 2 of 2 rendered\n  nested[0][0]=true\n  numeric_string=42" uncarried= []
   value_inputs= ["a\r\nb" "a\nb" "a\rb" "a\\nb" "a b" "a b"]
   value_lines= ["  k=a\\r\\nb" "  k=a\\nb" "  k=a\\rb" "  k=a\\\\nb" "  k=a b" "  k=a b"]
   value_lines_injective= true unicode_line_separator_raw= true unicode_paragraph_separator_raw= true
   numeric-string/index paths= [["0"] [0]] labels= ["0" "[0]"] injective= true
   trailing-backslash paths= [["a\\"] ["a"]] labels= ["a\\\\" "a"] injective= true
   keyword/string paths= [[:a] ["a"]] labels= ["a" "a"] injective= false
   nil/empty paths= [[nil] [""]] labels= ["" ""] injective= false
   empty-leading-index paths= [["" 0] [0]] labels= ["[0]" "[0]"] injective= false
   EDGE_ATTACKS_EXIT_CODE=0
   ```

   Exact Unicode-split command:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp -e '(require (quote [clj-surgeon.mcp-inspect :as i]) (quote [clojure.string :as str])) (doseq [v ["a b" "a b"] :let [line (first (i/leaf-lines [:k] v))]] (println "value=" (pr-str v) "rendered=" (pr-str line) "clojure_split_lines=" (count (str/split-lines line)) "unicode_R_lines=" (count (str/split line #"\R" -1))))'
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   value= "a b" rendered= "  k=a b" clojure_split_lines= 1 unicode_R_lines= 2
   value= "a b" rendered= "  k=a b" clojure_split_lines= 1 unicode_R_lines= 2
   ```

3. The round-eight RED→GREEN history is honest for its stated fixtures, and the disclosed two-part sabotage receipts reproduce. `d4739d3b` is 26/133 red and `c7b445c5` is 0/133; `e04a243e` is 122/563 red and `c6446767` is 0/563. At the tip the same witnesses are 0/133 and 0/563. Replacing `escape-pointer-segment` with identity is 26/133. The advertised 151/563 is **not** the result of reverting the second escape function alone: `escape-line-breaks -> identity` alone is 61/563; 151/563 requires also removing `\n`/`\r`/`\t` from the pointer map. The round-eight observation discloses that two-part mutation, but the addendum's “revert each escape fn to identity” shorthand does not. The sabotage is effective against the defects it names, but its alphabet omission explains why it remains green over finding 1. Files: commits `d4739d3b`, `c7b445c5`, `e04a243e`, `c6446767`; `test/clj_surgeon/mcp_study_test.clj:4847`, `test/clj_surgeon/mcp_study_test.clj:4908`.

   Exact history commands (each run from the matching `git archive` directory):

   ```sh
   history_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$history_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj two-distinct-leaves-never-share-a-pointer; code=$?; echo EXIT_CODE=$code
   history_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$history_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj a-rendered-line-is-a-single-line-by-construction; code=$?; echo EXIT_CODE=$code
   ```

   Verbatim terminal summaries in revision order:

   ```text
   REV=d4739d3b
   Ran 1 tests containing 133 assertions.
   26 failures, 0 errors.
   EXIT_CODE=1
   REV=c7b445c5
   Ran 1 tests containing 133 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   REV=e04a243e
   Ran 1 tests containing 563 assertions.
   122 failures, 0 errors.
   EXIT_CODE=1
   REV=c6446767
   Ran 1 tests containing 563 assertions.
   0 failures, 0 errors.
   EXIT_CODE=0
   ```

   Exact tip-witness command:

   ```sh
   review_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$review_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj two-distinct-leaves-never-share-a-pointer; code=$?; echo POINTER_WITNESS_EXIT_CODE=$code; ~/bin/suite-run java -cp "$review_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj a-rendered-line-is-a-single-line-by-construction; code=$?; echo LINE_WITNESS_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   Ran 1 tests containing 133 assertions.
   0 failures, 0 errors.
   POINTER_WITNESS_EXIT_CODE=0
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   Ran 1 tests containing 563 assertions.
   0 failures, 0 errors.
   LINE_WITNESS_EXIT_CODE=0
   ```

   Exact sabotage commands (from the two archive copies described above):

   ```sh
   sabotage_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$sabotage_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj two-distinct-leaves-never-share-a-pointer; code=$?; echo POINTER_SABOTAGE_EXIT_CODE=$code
   sabotage_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$sabotage_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj a-rendered-line-is-a-single-line-by-construction; code=$?; echo LINE_SABOTAGE_EXIT_CODE=$code
   ```

   Verbatim terminal summaries:

   ```text
   Ran 1 tests containing 133 assertions.
   26 failures, 0 errors.
   POINTER_SABOTAGE_EXIT_CODE=1
   Ran 1 tests containing 563 assertions.
   151 failures, 0 errors.
   LINE_SABOTAGE_EXIT_CODE=1
   ```

   Exact function-only line sabotage command:

   ```sh
   set -o pipefail; line_only_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$line_only_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj a-rendered-line-is-a-single-line-by-construction 2>&1 | tail -4; code=${PIPESTATUS[0]}; echo LINE_ONLY_SABOTAGE_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
     actual: (not (= 0 1))

   Ran 1 tests containing 563 assertions.
   61 failures, 0 errors.
   LINE_ONLY_SABOTAGE_EXIT_CODE=1
   ```

4. The earlier public-budget mechanism remains bounded and deterministic on the ordinary two-file result; finding 1 is a carriage/declaration failure inside a fitting candidate, not a non-fitting return. At this checkout the ordinary result is 32,740 bytes with 28 bytes of headroom, and one additional fact is 32,760 bytes with 8 bytes of headroom; both are stable across three fits and declaration equals audit. Ten thousand facts refuse typed in 392.45 ms and the 1,525-byte refusal fits. One hundred thousand also refuses typed and fits, but takes 3,220.47 ms, so it does not meet a blanket two-second affordability claim. The deliberately lifted-cap fitted 10,000-fact path remains expensive at 10,200.20 ms; that is not a shipped 32-KB-path correctness blocker, but MCP-OP-STUDY-050 should not be read as a general fact-count complexity claim. Files: `src/clj_surgeon/mcp_inspect.clj:1020`, `src/clj_surgeon/mcp_inspect_tool.clj:2122`, `test/clj_surgeon/mcp_study_test.clj:4564`.

   Exact near-cap command:

   ```sh
   cap_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); timeout 120s ~/bin/suite-run java -cp "$cap_cp" clojure.main /var/tmp/forge/o2r9-review-fx/near-cap.clj; code=$?; echo NEAR_CAP_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   ordinary= {:bytes 32740, :headroom 28, :limit 5440, :omitted nil, :declared 1266, :audited 1266, :deterministic true}
   plus_one= {:bytes 32760, :headroom 8, :limit 5417, :omitted nil, :declared 1268, :audited 1268, :deterministic true}
   NEAR_CAP_EXIT_CODE=0
   ```

   Exact performance command:

   ```sh
   perf_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); timeout 60s ~/bin/suite-run java -cp "$perf_cp" clojure.main /var/tmp/forge/o2r9-review-fx/performance.clj; code=$?; echo PERFORMANCE_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   real_cap_10k= {:facts 10000, :cap 32768, :elapsed_ms 392.449585, :ok false, :error_type "inspect-output-limit", :text_omitted nil, :bytes 1525, :fits true}
   real_cap_100k= {:facts 100000, :cap 32768, :elapsed_ms 3220.474864, :ok false, :error_type "inspect-output-limit", :text_omitted nil, :bytes 1531, :fits true}
   fitted_cap_10k= {:facts 10000, :cap 220000, :elapsed_ms 10200.199941, :ok true, :error_type nil, :text_omitted nil, :bytes 219880, :fits true}
   PERFORMANCE_EXIT_CODE=0
   ```

5. The envelope gate and real HTTP witness are green at this tip, and `run-ls-tree` still returns a typed map that the CLI turns into exit 1. The two envelope witnesses pass 17 assertions; the real HTTP carriage witness passes 21 assertions on explicit port 8151 and stops it. There is nevertheless a semantic composition hazard for MEM-003: if the future finalizer adds nested `:measured.elapsed_ms` while a domain receipt carries a top-level `:elapsed_ms`, current `request-elapsed-ms` prefers the forged top-level value (888.0) over the construction-stamped nested envelope clock (1.25), both before and after JSON. Thus the seven-file MEM resolution must change the clock-selection rule; simply taking MEM-003's nested envelope and reapplying this tip's current helper does not compose safely. Files: `src/clj_surgeon/mcp_operation.clj:27`, `src/clj_surgeon/mcp_operation.clj:54`, `src/clj_surgeon/mcp_operation.clj:73`, `src/clj_surgeon/core.clj:176`, `src/clj_surgeon/core.clj:533`.

   Exact focused-envelope command:

   ```sh
   env_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$env_cp" clojure.main /var/tmp/forge/o2r9-review-fx/repo/run-vars.clj the-envelope-is-whatever-the-finalizer-added a-domain-key-spelled-like-the-envelope-is-not-the-envelope; code=$?; echo ENVELOPE_TEST_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

   Ran 2 tests containing 17 assertions.
   0 failures, 0 errors.
   ENVELOPE_TEST_EXIT_CODE=0
   ```

   Exact nested-envelope composition attack:

   ```sh
   ~/bin/suite-run clojure -M:clj-surgeon/mcp -e '(require (quote [clj-surgeon.mcp-operation :as o]) (quote [cheshire.core :as json])) (let [domain {:elapsed_ms 888.0 :measured {:elapsed_ms 777.0 :domain true}} published (o/stamp-envelope domain {:measured {:elapsed_ms 1.25 :request true}}) wire (json/parse-string (json/generate-string published) true)] (println "envelope=" (pr-str (o/envelope published)) "published_top=" (:elapsed_ms published) "published_nested=" (get-in published [:measured :elapsed_ms]) "request_elapsed_published=" (o/request-elapsed-ms published) "wire_top=" (:elapsed_ms wire) "wire_nested=" (get-in wire [:measured :elapsed_ms]) "request_elapsed_wire=" (o/request-elapsed-ms wire)))'
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   envelope= {:measured {:elapsed_ms 1.25, :request true}} published_top= 888.0 published_nested= 1.25 request_elapsed_published= 888.0 wire_top= 888.0 wire_nested= 1.25 request_elapsed_wire= 888.0
   ```

   Exact HTTP command:

   ```sh
   http_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); if lsof -nP -iTCP:8151 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8151_BUSY; exit 1; else echo PORT_8151_FREE; fi; ~/bin/suite-run java -cp "$http_cp" clojure.main /var/tmp/forge/o2r9-review-fx/http-witness.clj; code=$?; echo HTTP_WITNESS_EXIT_CODE=$code; if lsof -nP -iTCP:8151 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8151_STILL_LISTENING; else echo PORT_8151_STOPPED; fi
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

   Exact CLI command:

   ```sh
   rg -n "run-ls-tree" --glob '!docs/**' .; bb -cp src -m clj-surgeon.core :op :ls-tree :dir /var/tmp/forge/o2r9-review-fx/no-such-dir :format :edn; code=$?; echo CLI_EXIT_CODE=$code
   ```

   Verbatim production hits and result:

   ```text
   ./src/clj_surgeon/core.clj:176:(defn run-ls-tree
   ./src/clj_surgeon/core.clj:533:    :ls-tree          {:handler   run-ls-tree
   ./src/clj_surgeon/memory_battery_runner.clj:52:    :entrance "clj-surgeon.core/run-ls-tree {:dir root :format :edn}"
   ./src/clj_surgeon/memory_battery_runner.clj:56:           ((requiring-resolve 'clj-surgeon.core/run-ls-tree)
   {:error
    ":ls-tree :dir must be an existing directory: \"/var/tmp/forge/o2r9-review-fx/no-such-dir\"",
    :error-type :workspace-root-not-a-directory,
    :dir "/var/tmp/forge/o2r9-review-fx/no-such-dir",
    :next-action "pass_an_existing_directory_path"}
   CLI_EXIT_CODE=1
   ```

6. The six-character escape cost reproduces, but the “fixed” two-file fixture is self-referential and therefore not a stable cost witness. This checkout measures 5,777 escaped versus 5,771 unescaped; headroom is 28 rather than the claimed 29 because the absolute review path differs by one byte. Reading this repository's changing source is useful as a live regression canary, but it is not acceptable as the retained fixed fixture for a byte-cost claim or as justification for repeatedly moving a text floor: every edit to either source changes both the structured receipt and the room left for text. A generated or retained external fixture is needed for that claim.

   The product change still needs Gene's explicit acceptance. The 25-file toy is 11,144 text characters here: above MCP-OP-STUDY-037's 8-KB line but below the 12-KB test. The 12-KB assertion is a ceiling only for that one fixture; it is not a public ceiling and therefore is a growth licence up to the 32-KB pair budget elsewhere. The round-eight observation itself says acceptance is still outstanding. The `Co-Authored-By` trailer is authorship, not an acceptance decision. Standing alone this is GO-WITH-FIX pending explicit acceptance and an unambiguous intent/golden decision; finding 1 makes the overall verdict NO-GO. Files: `docs/intent/study-ops/study-ops-specs.md:118`, `docs/intent/study-ops/study-ops-specs.md:122`, `test/clj_surgeon/mcp_study_test.clj:1881`, `test/clj_surgeon/mcp_study_test.clj:3870`, `docs/observations/2026-09-04-o2-round-eight-a-pointer-that-cannot-be-decoded-is-not-an-address.md:79`.

   Exact escape-cost command:

   ```sh
   cost_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); timeout 120s ~/bin/suite-run java -cp "$cost_cp" clojure.main /var/tmp/forge/o2r9-review-fx/cost.clj; code=$?; echo COST_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   escaped= {:text_chars 5777, :published_bytes 32740, :headroom 28, :limit 5440, :omitted nil}
   unescaped= {:text_chars 5771, :published_bytes 32734, :headroom 34, :limit 5440, :omitted nil}
   text_delta= 6
   COST_EXIT_CODE=0
   ```

   Exact toy-size command:

   ```sh
   size_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); timeout 150s ~/bin/suite-run java -cp "$size_cp" clojure.main /var/tmp/forge/o2r9-review-fx/ls-tree-size.clj; code=$?; echo SIZE_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   files= 10 read_complete= true returned= 10 limit= 8192 text_chars= 4609 structured_chars= 2469 public_bytes= 7262 under_8k= true under_12k= true
   files= 25 read_complete= true returned= 25 limit= 8192 text_chars= 11144 structured_chars= 5789 public_bytes= 17327 under_8k= false under_12k= true
   SIZE_EXIT_CODE=0
   ```

   Exact acceptance-record command:

   ```sh
   git show -s --format='%H%n%B' f572e461; sed -n '74,83p' docs/observations/2026-09-04-o2-round-eight-a-pointer-that-cannot-be-decoded-is-not-an-address.md
   ```

   Verbatim relevant output:

   ```text
   f572e461c218733e8f64788c25dd1183dd0c8792
   study-ops: O2r7 (§6, §10) — state the byte cost, the product change, and the MEM-003 absorption

   The carriage rule costs about 1% on a file read (22 bytes) and 23% on
   ls-tree, measured at three revisions on one fixed fixture, on top of the
   16-59% round five's collidable-label rule cost. The text is no longer a
   summary containing the receipt; it is the receipt with the rows in front of
   it, and MCP-OP-STUDY-037's 8 KB text pass-line is superseded by the public
   output budget plus a growth ratchet. All of that is now in the intent.

   Co-Authored-By: Gene Kim <genek@itrevolution.com>

   **The product-change flag is unchanged.** The `ls-tree format=text` doubling
   recorded in MCP-OP-STUDY-051 still awaits Gene's explicit acceptance; round
   eight neither widened it nor re-blessed a golden.
   ```

7. The declared `stop-child!` flake did not reproduce: 0 failing namespace runs out of 10, totaling 30 tests / 270 assertions. The wrapper changed only a child command's `:port 0` to explicit port 8150; the port was free before and stopped afterward. Files: `test/clj_surgeon/mcp_prepared_wire_test.clj:30`, `test/clj_surgeon/mcp_prepared_wire_test.clj:205`.

   Exact command:

   ```sh
   prepared_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_BUSY; exit 1; else echo PORT_8150_FREE; fi; timeout 600s ~/bin/suite-run java -cp "$prepared_cp" clojure.main /var/tmp/forge/o2r9-review-fx/prepared-ten.clj; code=$?; echo PREPARED_EXIT_CODE=$code; if lsof -nP -iTCP:8150 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8150_STILL_LISTENING; else echo PORT_8150_STOPPED; fi
   ```

   Verbatim output summary for all ten runs:

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

8. Provenance, lineage, and fixture scope are correct. I read the round-four, round-five, round-six, and round-seven verdicts first; inspected the requested histories and named RED→GREEN diffs; and reviewed this tip from a fresh detached clone. The round-eight first-parent delta is exactly two REDs, their two GREENs, and the observation. No source, test, committed fixture, index, stash, commit, branch, or remote was changed. Round eight changes no `test-fixtures`; the current merge-base comparison is nonzero only for the eight earlier-round ls-tree golden/prune-target paths listed below. Files: `docs/observations/study-ops-o2-round4-review-opus.md:1`, `docs/observations/study-ops-o2-round5-review-sol.md:1`, `docs/observations/study-ops-o2-round6-review-sol.md:1`, `docs/observations/study-ops-o2-round7-review-sol.md:1`, commits `d4739d3b..e7bc588a`.

   Exact provenance/log command:

   ```sh
   git rev-parse HEAD && git status --short --branch && git log --oneline f572e461..e7bc588a
   ```

   Verbatim output:

   ```text
   e7bc588a27768e3fd90c40af7ed18d55672c9bd6
   ## HEAD (no branch)
   e7bc588a study-ops: O2r8 (§10) — the six-character cost, the sabotage receipts, and the MEM-003 composition
   c6446767 study-ops: O2r8 GREEN (§3) — one leaf, one line, escaped so nothing can split it
   e04a243e study-ops: O2r8 RED (§3) — a rendered line is a single line, or the text lies about being complete
   c7b445c5 study-ops: O2r8 GREEN (§2) — every delimiter this pointer syntax spends is escaped inside a segment
   d4739d3b study-ops: O2r8 RED (§2) — a pointer is an injective encoding of the path it names
   ```

   Exact fixture-history command:

   ```sh
   git diff --exit-code 4480e3d..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_4480_EXIT_CODE=$code; git diff --exit-code 580e167a..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_POST_R6_MERGE_EXIT_CODE=$code; git diff --exit-code 972cf4c2..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_SINCE_R5_EXIT_CODE=$code; echo FIXTURES_SINCE_R5_COUNT=$(git diff --name-only 972cf4c2..HEAD -- test-fixtures/ | wc -l); git diff --exit-code f572e461..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_SINCE_F572_EXIT_CODE=$code; base=$(git merge-base HEAD origin/MCP/main); echo MERGE_BASE=$base; git diff --exit-code "$base"..HEAD -- test-fixtures/ >/dev/null; code=$?; echo FIXTURES_MERGE_BASE_EXIT_CODE=$code; echo FIXTURES_MERGE_BASE_COUNT=$(git diff --name-only "$base"..HEAD -- test-fixtures/ | wc -l); git diff --name-only "$base"..HEAD -- test-fixtures/
   ```

   Verbatim output:

   ```text
   FIXTURES_4480_EXIT_CODE=1
   FIXTURES_POST_R6_MERGE_EXIT_CODE=0
   FIXTURES_SINCE_R5_EXIT_CODE=1
   FIXTURES_SINCE_R5_COUNT=58
   FIXTURES_SINCE_F572_EXIT_CODE=0
   MERGE_BASE=49c5baa57e4d755df03970eb7f268aa99619d1bb
   FIXTURES_MERGE_BASE_EXIT_CODE=1
   FIXTURES_MERGE_BASE_COUNT=8
   test-fixtures/study/ls-tree-existing-ops-edn.golden.txt
   test-fixtures/study/ls-tree-existing-ops.golden.txt
   test-fixtures/study/ls-tree-no-clojure-files.golden.txt
   test-fixtures/study/ls-tree-prune-target.golden.txt
   test-fixtures/study/prune-target/core.clj
   test-fixtures/study/prune-target/src/app/core.clj
   test-fixtures/study/prune-target/src/app/target/bar.clj
   test-fixtures/study/prune-target/target/foo.clj
   ```

9. All requested executable gates are green. The disclosed six-failure `mcp-test` event did not reproduce in either of two fresh-clone runs: both are exactly 895 tests / 13,597 assertions / 0 failures / 0 errors. The Babashka gate is 854 / 6,882 / 0; the oracle passes; the intent audit is clean. These gates do not cover finding 1's empty-segment collision. Files: `Makefile:1`, `test/run_all.clj:1`, `test/clj_surgeon/mcp_test_runner.clj:1`, `test/clj_surgeon/mcp_study_test.clj:4847`.

   Exact commands:

   ```sh
   ~/bin/suite-run bb test/run_all.clj; code=$?; echo BB_EXIT_CODE=$code
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; code=$?; echo MCP_TEST_RUN_1_EXIT_CODE=$code
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; code=$?; echo MCP_TEST_RUN_2_EXIT_CODE=$code
   make mcp-operation-oracle; code=$?; echo ORACLE_EXIT_CODE=$code
   ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :spec-count :violations])))"; code=$?; echo AUDIT_EXIT_CODE=$code
   ```

   Verbatim terminal results:

   ```text
   Ran 854 tests containing 6882 assertions.
   0 failures, 0 errors.
   BB_EXIT_CODE=0

   Ran 895 tests containing 13597 assertions.
   0 failures, 0 errors.
   MCP_TEST_RUN_1_EXIT_CODE=0

   Ran 895 tests containing 13597 assertions.
   0 failures, 0 errors.
   MCP_TEST_RUN_2_EXIT_CODE=0

   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   ORACLE_EXIT_CODE=0

   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :violations []}
   AUDIT_EXIT_CODE=0
   ```

10. Mergeability: this tip textually merges cleanly into the clone's `origin/MCP/main` at `e3f09b8d`. It does not merge into current MEM-003 at `a2a15cc0`: seven conflicts, exactly the genuine O2/MEM overlap. The earlier instruction that MEM-003 absorb trunk first was correct and has removed the three stale-trunk conflicts seen in round seven. For the seven remaining files: start from this tip's typed `run-ls-tree` behavior in `core.clj` and its witness in `core_discovery_test.clj`, then layer MEM's measured fields; retain this tip's carriage/budget code in `mcp_inspect.clj` and `mcp_inspect_tool.clj`, then layer MEM's measured-domain shape; start from MEM's nested envelope in `mcp_operation.clj`, reapply construction stamping, and fix the forged-top-level clock priority demonstrated in finding 5; union both witness sets in `mcp_inspect_tool_test.clj`; union the namespace registry in `test/run_all.clj`. Neither test side should absorb the other by deletion. Finding 1 must be fixed before this lane is landed anywhere. Files: the seven conflict paths below; `docs/observations/2026-09-04-o2-round-eight-a-pointer-that-cannot-be-decoded-is-not-an-address.md:91`.

   Exact command:

   ```sh
   set -o pipefail; echo MCP_MAIN=$(git rev-parse origin/MCP/main); git merge-tree --write-tree HEAD origin/MCP/main 2>&1 | rg '^[0-9a-f]{40}$|^CONFLICT'; code=${PIPESTATUS[0]}; echo MCP_MAIN_EXIT_CODE=$code; echo MEM003=$(git rev-parse origin/bridge/integration-2026-09-03-mem003); git merge-tree --write-tree HEAD origin/bridge/integration-2026-09-03-mem003 2>&1 | rg '^[0-9a-f]{40}$|^CONFLICT'; code=${PIPESTATUS[0]}; echo MEM003_EXIT_CODE=$code
   ```

   Verbatim output:

   ```text
   MCP_MAIN=e3f09b8db88bdccd4cca49a681ea7c7a7cb30d82
   990704d9870ccddfadadb7bc68962b24fb3494cb
   MCP_MAIN_EXIT_CODE=0
   MEM003=a2a15cc0f3f1192dca4221bda24562ac251f08a1
   0eb9ecf7b3f6b33b7a43b3270e9e708e223d26e7
   CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
   CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
   CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
   CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_operation.clj
   CONFLICT (content): Merge conflict in test/clj_surgeon/core_discovery_test.clj
   CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
   CONFLICT (content): Merge conflict in test/run_all.clj
   MEM003_EXIT_CODE=1
   ```

11. Review cleanup is complete. The isolated fixture/clone tree and the three Clojure error reports produced by review probes were removed, and none of the only permitted review ports is listening.

   Exact command:

   ```sh
   review_fx=/var/tmp/forge/o2r9-review-fx; test "$review_fx" = /var/tmp/forge/o2r9-review-fx || exit 1; find "$review_fx" -depth -delete; for report in /var/tmp/forge/clojure-1833806094625988200.edn /var/tmp/forge/clojure-2931632474997650832.edn /var/tmp/forge/clojure-5067126496501024758.edn; do test ! -e "$report" || find "$report" -maxdepth 0 -delete; done; test ! -e "$review_fx" && echo FIXTURES_REMOVED; for report in /var/tmp/forge/clojure-1833806094625988200.edn /var/tmp/forge/clojure-2931632474997650832.edn /var/tmp/forge/clojure-5067126496501024758.edn; do test ! -e "$report" || exit 1; done; echo REVIEW_ERROR_REPORTS_REMOVED; for port in 8150 8151 8152; do if lsof -nP -iTCP:$port -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_${port}_LISTENING; exit 1; else echo PORT_${port}_STOPPED; fi; done
   ```

   Verbatim output:

   ```text
   FIXTURES_REMOVED
   REVIEW_ERROR_REPORTS_REMOVED
   PORT_8150_STOPPED
   PORT_8151_STOPPED
   PORT_8152_STOPPED
   ```

## NO-GO

This tip is not GO on its own for MCP/main because the empty-segment pointer collision yields an undeclared public omission and the product change still lacks Gene's explicit acceptance; it merge-trees cleanly into MCP/main, but it does not yet compose onto the MEM-003 second landing because seven textual conflicts and the nested-clock priority defect require deliberate absorption.
