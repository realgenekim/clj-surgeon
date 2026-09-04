## NO-GO

1. Baseline and review lineage are proven. `HEAD` is exactly `dda9fa29976bffd2f47cffb7f807ed0b96782d7c`, detached, and the worktree remained clean. I read the complete round-four, round-five, round-six, round-seven, and round-nine verdicts before ruling. The requested round-eleven range contains exactly the two RED/GREEN pairs, frozen-fixture commit, and observation commit claimed. Files: `repository:HEAD`; `docs/observations/study-ops-o2-round4-review-opus.md:1`; `docs/observations/study-ops-o2-round5-review-sol.md:1`; `docs/observations/study-ops-o2-round6-review-sol.md:1`; `docs/observations/study-ops-o2-round7-review-sol.md:1`; `docs/observations/study-ops-o2-round9-review-sol.md:1`.

   Exact commands:

   ```sh
   git rev-parse HEAD && git status --short --branch && git remote -v
   for f in study-ops-o2-round4-review-opus.md study-ops-o2-round5-review-sol.md study-ops-o2-round6-review-sol.md study-ops-o2-round7-review-sol.md study-ops-o2-round9-review-sol.md; do printf '%s: ' "$f"; git show "origin/MCP/main:docs/observations/$f" | sed -n '1p'; done; printf 'PRIOR_VERDICTS_EXIT_CODE=%s\n' "$?"
   git log --oneline e7bc588a..dda9fa29; printf 'ROUND11_LOG_EXIT_CODE=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   dda9fa29976bffd2f47cffb7f807ed0b96782d7c
   ## HEAD (no branch)
   origin	https://github.com/realgenekim/clj-surgeon.git (fetch)
   origin	https://github.com/realgenekim/clj-surgeon.git (push)
   study-ops-o2-round4-review-opus.md: ## NO-GO
   study-ops-o2-round5-review-sol.md: ## NO-GO
   study-ops-o2-round6-review-sol.md: ## NO-GO
   study-ops-o2-round7-review-sol.md: ## NO-GO
   study-ops-o2-round9-review-sol.md: ## NO-GO
   PRIOR_VERDICTS_EXIT_CODE=0
   dda9fa29 study-ops: O2r10 (§1, §2, §6) — the empty segment, the line-boundary class, and a frozen cost fixture
   eac53bbe study-ops: O2r10 (§6) — a cost witness that reads the code it measures is not a fixture
   6eccb717 study-ops: O2r10 GREEN (§2) — the whole Unicode line-boundary class, not the three that were named first
   73485f97 study-ops: O2r10 RED (§2) — a line boundary is whatever a splitter calls one
   31e7eb40 study-ops: O2r10 GREEN (§1) — the empty segment is spelled `~7`, so no position can be erased
   98c22e54 study-ops: O2r10 RED (§1) — a segment that renders as nothing erases the position it names
   ROUND11_LOG_EXIT_CODE=0
   ```

   I also inspected the diffs for every requested historical RED→GREEN transition. The requested first-parent histories were `515e8109..972cf4c2`, `972cf4c2..dafc7f37`, and `dafc7f37..dda9fa29`; the round-nine history was `f572e461..dda9fa29`. Their commit subjects matched the brief, including `e6d4416f` + `580e167a` and `8e385602`. The merge-parent test diffs retain both trunk and O2 witness families; I found no weakened trunk witness in those historical merges. Exact diff command and verbatim terminal result:

   ```sh
   for pair in '9bbe2bc1 aa8bfe5d' '532c76fb 0309f846' 'fe0a4a2e 8210e5c4' '28ae1897 42cff0ff' 'b410e31b 0362a4f9' '8b2a4aa5 468ca52e' '6bed20b0 dafc7f37' 'c7defa74 eee4283e' 'ac9cd03b eee4283e' 'd4739d3b c7b445c5' 'e04a243e c6446767' '98c22e54 31e7eb40' '73485f97 6eccb717'; do set -- $pair; git diff --stat "$1" "$2" >/dev/null || exit; done; printf 'RED_GREEN_DIFFS_EXIT_CODE=%s\n' "$?"
   ```

   ```text
   RED_GREEN_DIFFS_EXIT_CODE=0
   ```

2. **BLOCKING — the declared `:a`/`"a"` and `nil`/`""` residual is not safe when both keys occur in one map.** `segment-spelling` and `leaf-label` deliberately give each pair one pointer. With duplicate leaves separated in receipt order, `fact-block` can render the first leaf, declare the second dropped, and have `uncarried-leaves` treat the second as carried by the first leaf's identical line. The abstract keyword/string and nil/empty cases disagree at 102 and 100 allowances. More importantly, an ordinary deterministic fitted public result is 32,684 bytes—inside 32,768—and declares four dropped facts while its own audit finds three. Its single rendered `a: ...` line discharges two distinct in-memory leaves even while `dropped: ..., a` says one is absent. Files: `src/clj_surgeon/mcp_inspect.clj:577`, `src/clj_surgeon/mcp_inspect.clj:652`, `src/clj_surgeon/mcp_inspect.clj:677`, `src/clj_surgeon/mcp_inspect.clj:874`, `src/clj_surgeon/mcp_inspect.clj:920`, `src/clj_surgeon/mcp_inspect.clj:1072`, `src/clj_surgeon/mcp_inspect_tool.clj:2122`, `docs/intent/study-ops/study-ops-specs.md:120`.

   The residual explains the wire collapse accurately, but it is not an acceptable carriage exception. JSON emits duplicate object-member names and ordinary decoders keep only one; that explains why a caller cannot address both, but does not make the pre-wire declaration true and makes the structured face itself lossy on the wire. Reject or normalize colliding map keys at the public boundary, or encode a wire-surviving identity. A declared residual cannot waive the bright-line declaration/audit invariant.

   Exact commands:

   ```sh
   timeout 240s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r11-review-fx/attacks.clj; code=$?; echo ATTACKS_EXIT_CODE=$code
   timeout 240s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r11-review-fx/public-collision.clj; code=$?; echo PUBLIC_COLLISION_EXIT_CODE=$code
   ```

   Verbatim relevant output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   == simultaneous JSON-colliding keys: abstract fact-block ==
   keyword/string labels= ["a" "middle0" "middle1" "a"] json= {"a":"the-same-distinctive-value-rendered-twice","middle0":"mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm","middle1":"nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn","a":"the-same-distinctive-value-rendered-twice"} mismatch_count= 102 first= {:budget 158, :shown 1, :total 4, :declared 3, :audited 2, :section "  receipt facts · 1 of 4 rendered · the complete receipt is in structuredContent\n  dropped: middle0, middle1, a\n  a: the-same-distinctive-value-rendered-twice"}
   nil/empty labels= ["~7" "middle0" "middle1" "~7"] json= {"":"the-same-distinctive-value-rendered-twice","middle0":"mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm","middle1":"nnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn","":"the-same-distinctive-value-rendered-twice"} mismatch_count= 100 first= {:budget 160, :shown 1, :total 4, :declared 3, :audited 2, :section "  receipt facts · 1 of 4 rendered · the complete receipt is in structuredContent\n  dropped: middle0, middle1, ~7\n  ~7: the-same-distinctive-value-rendered-twice"}
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   target_structured= 32588 near_padding= 32251 near_structured= 32588
   mismatch_count= 85 first= {:omitted nil, :limit 314, :a-lines 1, :audited 3, :structured-bytes 32088, :padding 31751, :declared 4, :bytes 32684}
   text= "inspect_clojure\n  1 request · 0 files\n\n✓ all requests resolved\n✓ ordered snapshot\n✓ hashes attached\n! text abridged · read_complete=true · next action read_structured_content_or_narrow_request\n\n  receipt facts · 8 of 12 rendered · the complete receipt is in structuredContent\n  dropped: pad, ok, request_count, a\n  source_character_count=0\n  elapsed_ms=0.0\n  read_complete=true\n  file_count=0\n  text_evidence_limit=314\n  operation=inspect_clojure\n  mode=outline\n  a: the-same-distinctive-value-rendered-twice\n  0 source characters · 0.00 ms"
   deterministic= true
   PUBLIC_COLLISION_EXIT_CODE=0
   ```

3. The round-eleven empty-segment and full `\R` fixes otherwise hold. `~7` is unreachable from a non-empty segment because raw `~` becomes `~0`; the generated 16,275-path witness is injective; namespaced keywords preserve the slash in the keyword's name; the 29/29 pointer-valued rung agrees at every allowance; and the decoy/twin plants agree at every integer allowance 0–500, including budgets omitted by the checked-in sweep. U+0000 and the literal escape spellings remain distinct, and every tested boundary remains one Unicode-aware line. Files: `src/clj_surgeon/mcp_inspect.clj:594`, `src/clj_surgeon/mcp_inspect.clj:639`, `src/clj_surgeon/mcp_inspect.clj:652`, `src/clj_surgeon/mcp_inspect.clj:677`, `src/clj_surgeon/mcp_inspect.clj:789`, `src/clj_surgeon/mcp_inspect.clj:815`, `test/clj_surgeon/mcp_study_test.clj:4854`, `test/clj_surgeon/mcp_study_test.clj:4991`.

   A lone UTF-16 high surrogate is a separate non-blocking boundary gap: the internal line differs from `?`, but JSON/UTF-8 transport replaces the surrogate with `?`, making the two public pairs byte-identical. That is outside the specified Unicode scalar/`\R` family and does not create a text-vs-structured omission—both faces collapse together—but arbitrary JVM strings should either reject unpaired surrogates or define their wire spelling.

   Exact commands:

   ```sh
   review_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$review_cp" clojure.main /var/tmp/forge/o2r11-review-fx/run-vars.clj 'clj-surgeon.mcp-study-test/two-distinct-leaves-never-share-a-pointer' 'clj-surgeon.mcp-study-test/a-rendered-line-is-a-single-line-by-construction'; printf 'FOCUSED_TIP_EXIT_CODE=%s\n' "$?"
   timeout 240s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r11-review-fx/carriage-band.clj; code=$?; printf 'CARRIAGE_BAND_EXIT_CODE=%s\n' "$code"
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   Ran 2 tests containing 1506 assertions.
   0 failures, 0 errors.
   FOCUSED_TIP_EXIT_CODE=0
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   == pointer-valued 29/29 receipt at every allowance ==
   total= 29 allowances_tested= 944 complete_section_chars= 943 mismatch_count= 0 first= nil
   == decoy and twin at every integer budget 0..500 ==
   decoy budgets_tested= 501 mismatch_count= 0 first= nil
   twin budgets_tested= 501 mismatch_count= 0 first= nil
   == namespaced keyword whose name contains slash ==
   original= :src/dir/demo.clj namespace= src name= dir/demo.clj label= file_hashes.src~1dir~1demo~2clj wire_key= :src/dir/demo.clj wire_label= file_hashes.src~1dir~1demo~2clj same_label= true
   == pointer/value malformed and control edges ==
   lone-surrogate key_line_units= [32 32 55296 58 32 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 108 111 110 103] value_line_units= [32 32 107 61 55296] key_R_lines= 1 value_R_lines= 1
   question-mark key_line_units= [32 32 63 58 32 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 108 111 110 103] value_line_units= [32 32 107 61 63] key_R_lines= 1 value_R_lines= 1
   nul key_line_units= [32 32 0 58 32 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 108 111 110 103] value_line_units= [32 32 107 61 0] key_R_lines= 1 value_R_lines= 1
   literal-backslash-u0000 key_line_units= [32 32 92 92 117 48 48 48 48 58 32 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 108 111 110 103] value_line_units= [32 32 107 61 92 92 117 48 48 48 48] key_R_lines= 1 value_R_lines= 1
   line-separator key_line_units= [32 32 92 117 50 48 50 56 58 32 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 108 111 110 103] value_line_units= [32 32 107 61 34 92 117 50 48 50 56 34] key_R_lines= 1 value_R_lines= 1
   literal-backslash-u2028 key_line_units= [32 32 92 92 117 50 48 50 56 58 32 100 105 115 116 105 110 99 116 105 118 101 45 118 97 108 117 101 45 108 111 110 103] value_line_units= [32 32 107 61 92 92 117 50 48 50 56] key_R_lines= 1 value_R_lines= 1
   CARRIAGE_BAND_EXIT_CODE=0
   ```

4. The ordinary public budget path fits deterministically, never returns nil or an oversized candidate, and the declaration is the floor even when its own 103-character block exceeds allowances 0, 1, and 32. The frozen result and one-fact-larger result fit with 25 and 21 bytes headroom. The actual-cap 10,000-fact result returns a fitting typed refusal in 334 ms; 100,000 facts returns a fitting typed refusal in 3.22 s. A deliberately raised 220,000-byte cap makes the 10,000-fact accepted path take 10.54 s. The checked-in requirement is the real 32,768-byte public cap, so its 10k under-two-second witness holds; the lifted-cap result exposes scaling with a future larger public cap but is not a present public non-fit. Files: `src/clj_surgeon/mcp_inspect.clj:1047`, `src/clj_surgeon/mcp_inspect.clj:1072`, `src/clj_surgeon/mcp_inspect.clj:1146`, `src/clj_surgeon/mcp_inspect_tool.clj:2122`, `docs/intent/study-ops/study-ops-specs.md:116`, `test/clj_surgeon/mcp_study_test.clj:3849`, `test/clj_surgeon/mcp_study_test.clj:4560`.

   Exact command:

   ```sh
   review_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); timeout 240s ~/bin/suite-run java -cp "$review_cp" clojure.main /var/tmp/forge/o2r11-review-fx/budget-performance.clj; code=$?; printf 'BUDGET_PERFORMANCE_EXIT_CODE=%s\n' "$code"
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   == frozen two-file near-cap and one fact larger ==
   files= ["test-fixtures/study/cost-batch/cost_batch_inspect_tool.clj" "test-fixtures/study/cost-batch/cost_batch_inspect.clj"]
   ordinary= {:bytes 32743, :headroom 25, :text-chars 5549, :limit 5222, :omitted nil, :declared 1277, :audited 1277, :deterministic true}
   plus_one= {:bytes 32747, :headroom 21, :text-chars 5510, :limit 5188, :omitted nil, :declared 1279, :audited 1279, :deterministic true}
   == declaration floor and oversized dropped line ==
   allowance= 0 section_chars= 103 shown= 0 total= 2 declared= 2 audited= 2 section= "  receipt facts · 0 of 2 rendered · the complete receipt is in structuredContent\n  dropped: alpha, beta"
   allowance= 1 section_chars= 103 shown= 0 total= 2 declared= 2 audited= 2 section= "  receipt facts · 0 of 2 rendered · the complete receipt is in structuredContent\n  dropped: alpha, beta"
   allowance= 32 section_chars= 103 shown= 0 total= 2 declared= 2 audited= 2 section= "  receipt facts · 0 of 2 rendered · the complete receipt is in structuredContent\n  dropped: alpha, beta"
   allowance= 103 section_chars= 103 shown= 0 total= 2 declared= 2 audited= 2 section= "  receipt facts · 0 of 2 rendered · the complete receipt is in structuredContent\n  dropped: alpha, beta"
   == fit cost ==
   real_cap_10k= {:facts 10000, :cap 32768, :elapsed_ms 333.58742, :ok false, :error_type "inspect-output-limit", :text_omitted nil, :bytes 1525, :fits true}
   real_cap_100k= {:facts 100000, :cap 32768, :elapsed_ms 3218.347166, :ok false, :error_type "inspect-output-limit", :text_omitted nil, :bytes 1531, :fits true}
   fitted_cap_10k= {:facts 10000, :cap 220000, :elapsed_ms 10538.975115, :ok true, :error_type nil, :text_omitted nil, :bytes 219968, :fits true}
   BUDGET_PERFORMANCE_EXIT_CODE=0
   ```

5. Current-envelope ownership is safe, the live HTTP witness passes, and the CLI handles the typed `run-ls-tree` refusal correctly. `stamp-envelope` records construction metadata and the current top-level elapsed value wins correctly despite a forged nested `measured.elapsed_ms`; the live port-8151 response is fitting and declaration/audit-equal. `run-ls-tree` returns the typed map, while `-main` prints it and exits 1. Files: `src/clj_surgeon/mcp_operation.clj:27`, `src/clj_surgeon/mcp_operation.clj:54`, `src/clj_surgeon/mcp_operation.clj:83`, `src/clj_surgeon/mcp_operation.clj:112`, `src/clj_surgeon/core.clj:176`.

   There is a composition hazard for MEM-003: when the genuine envelope becomes nested under `:measured`, a forged domain top-level `:elapsed_ms` wins in `request-elapsed-ms` both before and after serialization. The MEM-003 conflict resolution must make the construction-identified/nested request clock win over a domain collision while keeping text equal to the published structured clock.

   Exact commands:

   ```sh
   timeout 240s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r11-review-fx/envelope-attack.clj; code=$?; printf 'ENVELOPE_ATTACK_EXIT_CODE=%s\n' "$code"
   ss -H -ltn '( sport = :8151 )' | sed -n '1p'; if ss -H -ltn '( sport = :8151 )' | rg -q .; then printf 'PORT_8151_NOT_FREE\n'; exit 2; else printf 'PORT_8151_FREE\n'; fi; timeout 240s ~/bin/suite-run clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r11-review-fx/http-witness.clj; code=$?; printf 'HTTP_WITNESS_EXIT_CODE=%s\n' "$code"; printf 'PORT_8151_STOPPED=%s\n' "$([ -z "$(ss -H -ltn '( sport = :8151 )')" ] && printf true || printf false)"; exit "$code"
   ~/bin/suite-run clojure -M -m clj-surgeon.core :op :ls-tree :dir /var/tmp/forge/o2r11-review-fx/no-such-directory :format :edn; printf 'CLI_EXIT_CODE=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   current_finalizer= {:envelope {:elapsed_ms 1.25}, :published_top 1.25, :published_nested 777.0, :request_elapsed_published 1.25, :wire_top 1.25, :wire_nested 777.0, :request_elapsed_wire 1.25}
   future_nested_envelope= {:envelope {:measured {:elapsed_ms 1.25, :request true}}, :published_top 888.0, :published_nested 1.25, :request_elapsed_published 888.0, :wire_top 888.0, :wire_nested 1.25, :request_elapsed_wire 888.0}
   ENVELOPE_ATTACK_EXIT_CODE=0
   PORT_8151_FREE
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   http_status= 200 session= true ok= true declared= 0 audited= 0 published_bytes= 2521 read_complete= true
   HTTP_WITNESS_EXIT_CODE=0
   PORT_8151_STOPPED=true
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:error
    ":ls-tree :dir must be an existing directory: \"/var/tmp/forge/o2r11-review-fx/no-such-directory\"",
    :error-type :workspace-root-not-a-directory,
    :dir "/var/tmp/forge/o2r11-review-fx/no-such-directory",
    :next-action "pass_an_existing_directory_path"}
   CLI_EXIT_CODE=1
   ```

6. **Landing also requires Gene's explicit product acceptance.** The increased read-side text is technically explained by the stronger carriage invariant, and the final public pair remains bounded, but this is still an intentional behavior change: one fact line became ten label lines, missing `read_complete` prints `null`, and the 25-file `ls-tree format=text` witness doubled from 4,334 to 8,796 characters. `MCP-OP-STUDY-051` explicitly supersedes `MCP-OP-STUDY-037`'s 8 KB pass-line, while the round-ten observation explicitly says acceptance is still outstanding. A 12 KB fixture “growth ratchet” is a regression tripwire for that fixture, not a public contract ceiling; outside that fixture the 32 KB pair budget is the real ceiling. Files: `docs/intent/study-ops/study-ops-specs.md:91`, `docs/intent/study-ops/study-ops-specs.md:118`, `test/clj_surgeon/mcp_study_test.clj:1881`, `test/clj_surgeon/mcp_study_test.clj:1901`, `docs/observations/2026-09-04-o2-round-ten-a-segment-that-renders-as-nothing-erases-the-position-it-names.md:86`.

   Exact command:

   ```sh
   sed -n '1901,1922p' test/clj_surgeon/mcp_study_test.clj; sed -n '75,90p' docs/observations/2026-09-04-o2-round-ten-a-segment-that-renders-as-nothing-erases-the-position-it-names.md
   ```

   Verbatim relevant output:

   ```text
        ;; @spec MCP-OP-STUDY-051 — a PRODUCT CHANGE, stated rather than
        ;; quietly relaxed. The 8 KB ceiling this line used to assert was a
        ;; rendering CONSTANT, which MCP-OP-STUDY-044 already forbids as an
        ;; allowance; and it was survivable in round six only because a
        ;; distinctive value found ANYWHERE in the text counted as carriage,
        ;; so every path, namespace, form name and hash in the structural
        ;; rows discharged its own receipt leaf. Under MCP-OP-STUDY-051 a
        ;; leaf is carried only by its own pointer line, so this text carries
        ;; the rows AND the receipt: measured 4,334 -> 8,796 characters on a
        ;; twenty-five file toy tree, and 11,546 here. The bound that is real
        ;; is the PUBLIC OUTPUT BUDGET; the 12 KB ceiling below is a ratchet
        ;; against further growth, not a contract.
   escaped=   {:text_chars 5549, :published_bytes 32749, :headroom 19, :limit 5216}
   unescaped= {:text_chars 5541, :published_bytes 32741, :headroom 27, :limit 5205}
   **The product-change flag is unchanged.** The `ls-tree format=text` doubling
   recorded in MCP-OP-STUDY-051 still awaits Gene's explicit acceptance; round ten
   neither widened it nor re-blessed a golden.
   ```

7. The declared prepared-wire flake did not reproduce: ten complete namespace runs, 30 tests / 270 assertions total, all passed; failure count 0; explicit port 8150 was stopped. Files: `test/clj_surgeon/mcp_prepared_wire_test.clj:205`.

   Exact command:

   ```sh
   review_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$review_cp" clojure.main /var/tmp/forge/o2r11-review-fx/prepared-ten.clj; rc=$?; printf 'PREPARED_TEN_EXIT_CODE=%s\n' "$rc"; ss -ltn '( sport = :8150 )' | sed -n '1,5p'; printf 'PORT_8150_STOPPED=%s\n' "$([ -z "$(ss -H -ltn '( sport = :8150 )')" ] && printf true || printf false)"; exit "$rc"
   ```

   Verbatim repeated output (runs 1–10 each emitted the same four-line result; terminal tail shown):

   ```text
   RUN=10

   Testing clj-surgeon.mcp-prepared-wire-test

   Ran 3 tests containing 27 assertions.
   0 failures, 0 errors.
   RUN_EXIT=0
   FAILURE_COUNT=0
   PREPARED_TEN_EXIT_CODE=0
   State Recv-Q Send-Q Local Address:Port Peer Address:Port
   PORT_8150_STOPPED=true
   ```

8. The new witnesses are load-bearing. On historical worktrees, `98c22e54` is RED with 7/152 and `31e7eb40` is GREEN with 0/152; `73485f97` is RED with 23/1354 and `6eccb717` is GREEN with 0/1354. On `git archive` copies of this exact tip, reverting only the empty-segment special case reproduces 7/152, and reverting only the extended line-boundary maps reproduces 23/1354. Files: `test/clj_surgeon/mcp_study_test.clj:4854`, `test/clj_surgeon/mcp_study_test.clj:4991`.

   Exact sabotage commands:

   ```sh
   cd /var/tmp/forge/o2r11-review-fx/sabotage-pointer && review_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$review_cp" clojure.main /var/tmp/forge/o2r11-review-fx/run-vars.clj 'clj-surgeon.mcp-study-test/two-distinct-leaves-never-share-a-pointer'; printf 'SABOTAGE_POINTER_EXIT_CODE=%s\n' "$?"
   cd /var/tmp/forge/o2r11-review-fx/sabotage-line && review_cp=$(clojure -Spath -M:clj-surgeon/mcp-test); ~/bin/suite-run java -cp "$review_cp" clojure.main /var/tmp/forge/o2r11-review-fx/run-vars.clj 'clj-surgeon.mcp-study-test/a-rendered-line-is-a-single-line-by-construction'; printf 'SABOTAGE_LINE_EXIT_CODE=%s\n' "$?"
   ```

   Verbatim terminal summaries:

   ```text
   Ran 1 tests containing 152 assertions.
   7 failures, 0 errors.
   SABOTAGE_POINTER_EXIT_CODE=1
   Ran 1 tests containing 1354 assertions.
   23 failures, 0 errors.
   SABOTAGE_LINE_EXIT_CODE=1
   ```

9. All requested gates pass, including two fresh full MCP-suite runs. Files: `test/run_all.clj:1`, `test/clj_surgeon/mcp_test_runner.clj:1`, `test/mcp_operation_contract_oracle.pl:1`, `src/clj_surgeon/mcp_intent_contract.clj:1`, `Makefile:1`.

   Exact commands:

   ```sh
   ~/bin/suite-run bb test/run_all.clj; code=$?; echo BB_EXIT_CODE=$code
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; code=$?; echo MCP_TEST_RUN_1_EXIT_CODE=$code
   ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; code=$?; echo MCP_TEST_RUN_2_EXIT_CODE=$code
   make mcp-operation-oracle; printf 'ORACLE_EXIT_CODE=%s\n' "$?"
   ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :spec-count :violations])))"; printf 'AUDIT_EXIT_CODE=%s\n' "$?"
   ```

   Verbatim terminal summaries:

   ```text
   Ran 854 tests containing 6882 assertions.
   0 failures, 0 errors.
   BB_EXIT_CODE=0
   Ran 895 tests containing 14407 assertions.
   0 failures, 0 errors.
   MCP_TEST_RUN_1_EXIT_CODE=0
   Ran 895 tests containing 14407 assertions.
   0 failures, 0 errors.
   MCP_TEST_RUN_2_EXIT_CODE=0
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   ORACLE_EXIT_CODE=0
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   {:ok true, :violations []}
   AUDIT_EXIT_CODE=0
   ```

10. Fixture accounting matches the round-eleven claim, with the historical qualification requested. At the round-six tip, `580e167a..dafc7f37` is clean and all 58 changed fixture paths in `972cf4c2..580e167a` came through the trunk merge. At the round-nine tip, `f572e461..e7bc588a` is clean. At this tip, `4480e3d..HEAD`, `580e167a..HEAD`, and `f572e461..HEAD` are non-zero because of those earlier trunk changes plus round ten. Relative to the round-eight observation base `e7bc588a`, round eleven adds exactly the two frozen cost-batch files and changes no existing fixture. Files: `test-fixtures/study/cost-batch/cost_batch_inspect.clj:1`, `test-fixtures/study/cost-batch/cost_batch_inspect_tool.clj:1`.

   Exact command:

   ```sh
   git diff --quiet 580e167a..dafc7f37 -- test-fixtures/; printf 'ROUND6_POST_MERGE_FIX_FIXTURES_EXIT_CODE=%s\n' "$?"; git diff --name-only 972cf4c2..580e167a -- test-fixtures/ | wc -l; git diff --quiet 972cf4c2..580e167a -- test-fixtures/; printf 'ROUND6_MERGE_FIXTURES_EXIT_CODE=%s\n' "$?"; git diff --quiet f572e461..e7bc588a -- test-fixtures/; printf 'ROUND9_FIXTURES_SINCE_R7_EXIT_CODE=%s\n' "$?"; git diff --quiet 4480e3d..HEAD -- test-fixtures/; printf 'FIXTURES_4480_EXIT_CODE=%s\n' "$?"; git diff --quiet f572e461..HEAD -- test-fixtures/; printf 'FIXTURES_SINCE_R7_EXIT_CODE=%s\n' "$?"; git diff --quiet e7bc588a..HEAD -- test-fixtures/; printf 'FIXTURES_SINCE_R8_EXIT_CODE=%s\n' "$?"; git diff --name-status e7bc588a..HEAD -- test-fixtures/; git diff --quiet 580e167a..HEAD -- test-fixtures/; printf 'FIXTURES_SINCE_MERGE_FIX_EXIT_CODE=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   ROUND6_POST_MERGE_FIX_FIXTURES_EXIT_CODE=0
   58
   ROUND6_MERGE_FIXTURES_EXIT_CODE=1
   ROUND9_FIXTURES_SINCE_R7_EXIT_CODE=0
   FIXTURES_4480_EXIT_CODE=1
   FIXTURES_SINCE_R7_EXIT_CODE=1
   FIXTURES_SINCE_R8_EXIT_CODE=1
   A	test-fixtures/study/cost-batch/cost_batch_inspect.clj
   A	test-fixtures/study/cost-batch/cost_batch_inspect_tool.clj
   FIXTURES_SINCE_MERGE_FIX_EXIT_CODE=1
   ```

11. The merge claims are stale against the current remote-tracking tips. After refreshing only `origin/MCP/main` and `origin/bridge/integration-2026-09-03-mem003`, this tip has **two conflicts** against current MCP/main `b916feb5` and **seven conflicts** against current MEM-003 `a2a15cc0`; it is not cleanly mergeable into either. Files: `src/clj_surgeon/core.clj:176`, `test/clj_surgeon/mcp_intent_contract_test.clj:1`, `src/clj_surgeon/mcp_inspect.clj:577`, `src/clj_surgeon/mcp_inspect_tool.clj:2122`, `src/clj_surgeon/mcp_operation.clj:27`, `test/clj_surgeon/core_discovery_test.clj:1`, `test/clj_surgeon/mcp_inspect_tool_test.clj:1`, `test/run_all.clj:1`.

   MCP/main absorption rule: take current trunk's census/security changes as the base in `core.clj`, reapply O2's typed `run-ls-tree` return and CLI exit contract, and union—not replace—the intent-contract witnesses. MEM-003 absorption rule: take O2's typed `run-ls-tree`, carriage, and budget behavior in `core.clj`, `mcp_inspect.clj`, and `mcp_inspect_tool.clj`; take MEM-003's nested envelope structure in `mcp_operation.clj` but reapply construction stamping and fix the forged-top-level elapsed precedence identified in finding 5; union both branches' discovery, inspect-tool, and runner witnesses. Thus the builder's direction remains right—MEM-003 should first absorb current trunk, then absorb this lane—but the seven-file conflict table requires semantic resolution and both witness sets.

   Exact command:

   ```sh
   printf 'MCP_MAIN='; git rev-parse origin/MCP/main; git merge-tree --write-tree HEAD origin/MCP/main 2>&1 | rg '^CONFLICT'; printf 'MCP_MAIN_EXIT_CODE=%s\n' "${PIPESTATUS[0]}"; printf 'MEM003='; git rev-parse origin/bridge/integration-2026-09-03-mem003; git merge-tree --write-tree HEAD origin/bridge/integration-2026-09-03-mem003 2>&1 | rg '^CONFLICT'; printf 'MEM003_EXIT_CODE=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim conflict summary from the command:

   ```text
   MCP_MAIN=b916feb56356cd7893ec42c7be0e9875f409160b
   CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
   CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_intent_contract_test.clj
   MCP_MAIN_EXIT_CODE=1
   MEM003=a2a15cc0f3f1192dca4221bda24562ac251f08a1
   CONFLICT (content): Merge conflict in src/clj_surgeon/core.clj
   CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect.clj
   CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_inspect_tool.clj
   CONFLICT (content): Merge conflict in src/clj_surgeon/mcp_operation.clj
   CONFLICT (content): Merge conflict in test/clj_surgeon/core_discovery_test.clj
   CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_inspect_tool_test.clj
   CONFLICT (content): Merge conflict in test/run_all.clj
   MEM003_EXIT_CODE=1
   ```

## NO-GO

This tip is not GO on its own for MCP/main because a fitting public result can disagree with its own dropped-fact audit (and the intentional read-side product change still needs Gene's acceptance), and it does not presently compose onto either current MCP/main or the MEM-003 second landing without the two- and seven-file semantic conflict resolutions above.
