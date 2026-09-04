## GO-WITH-FIX

1. **GO-WITH-FIX — the whole refusal is bounded correctly, but the root-listing marker is appended after the 512-character budget and breaks that advertised sub-bound.** `src/clj_surgeon/mcp_alias_migration.clj:829-862` budgets only retained entries, then `cond->` appends `… [+N more roots …]` without charging its rendered length. Six ordinary 112-ish-character patterns retain four entries; the marker becomes a fifth and makes `(pr-str listing)` 528 characters. This falsifies the root-listing clause of `MCP-OP-ALIAS-059` (`docs/intent/alias-migration/alias-migration-specs.md:108`), but it is not a whole-receipt escape: `src/clj_surgeon/mcp_tool.clj:1362-1380` includes its truncation marker inside the 4,096-character budget. The formal promise is **characters**, not UTF-8 bytes. Thus the 4,096/4,097 edge holds, while even the otherwise-ASCII fixture is 4,107/4,109 bytes because the renderer contains Unicode glyphs, and a lambda payload reaches 7,935 bytes. If “4 KB” was intended as a transport-byte ceiling, that is a different, currently unmet contract; the EARS text says character ceiling.

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/probe.clj' | sed -n '/=== root-list/,/=== code-point/p'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   === root-list marker and whole-text ceilings ===
   root-list items/rendered/ceiling => 5 528 512
   root-list marker? => true
   root-list within ceiling? => false
   at chars/bytes/truncated => 4096 4107 false
   past chars/bytes/truncated => 4096 4109 true
   unicode chars/bytes/truncated => 4096 7935 true

   === code-point boundary ===
   EXIT=0
   ```

2. **GO-WITH-FIX — “every value shape is elided” bounds the returned text, but not the work needed to produce it: `pr-str` realizes the complete value before truncation, and an infinite lazy sequence hangs. The helper-built live witness also has one key-specific blind spot.** `src/clj_surgeon/mcp_tool.clj:1318-1325` calls unbounded `pr-str` before measuring 160 characters. A function renders safely; a 100,000-item lazy sequence and a 10,000-entry nested map render a bounded text only after fully materializing their printed representation; `(iterate inc 0)` never reaches the ceiling gate and timed out. This does not expose a current partial write—the live receipts at this tip carry finite realized values—but it contradicts the new shape-independent/constant-bound claim and can strand the MCP request if a future helper returns a lazy fact.

   Exact commands:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/probe.clj' | sed -n '/=== finite arbitrary/,/=== source-derived/p'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -A:clj-surgeon/mcp-test); exec timeout 10s java -cp "$cp" clojure.main /tmp/q5z13-review-fx/infinite_fact.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim outputs:

   ```text
   === finite arbitrary fact shapes ===
   function => chars 352 named true elided true ms 0.9
   lazy-100k => chars 449 named true elided true ms 15.4
   nested-map-10k => chars 449 named true elided true ms 6.4

   === source-derived enumeration ===
   EXIT=0

   before infinite render
   EXIT=124
   ```

   The broad sabotage requested by the builder is genuinely red: restoring a map-dropping predicate makes the new any-shape witness fail four assertions. But the dedicated helper-live witness at `test/clj_surgeon/mcp_alias_migration_test.clj:1524` still filters asserted values at `:1193-1206`; and the ten-receipt list at `:4448-4525` omits the workspace-router refusal. On a scratch copy I added `:helper_sabotage_detail {:nested "value"}` to `mcp_workspace/refusal` and made only that key disappear in the renderer. The source-key witness, dedicated live workspace witness, ten-live-receipts witness, and any-shape witness all stayed green—259 assertions. This is a witness defect, not a defect in the current generic renderer.

   Exact commands and verbatim outputs:

   ```text
   $ (cd /tmp/q5z13-review-fx/render-sabotage-wt && ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/render_sabotage_runner.clj'; rc=$?; echo EXIT=$rc) 2>&1 | rg '^dedicated|^any-shape|^FAIL|^\{:test|^EXIT'
   dedicated helper-live witness:
   {:test 1, :pass 14, :fail 0, :error 0}
   any-shape witness:
   FAIL in (a-fact-of-any-shape-is-named-in-the-text-block) (mcp_alias_migration_test.clj:4586)
   FAIL in (a-fact-of-any-shape-is-named-in-the-text-block) (mcp_alias_migration_test.clj:4586)
   FAIL in (a-fact-of-any-shape-is-named-in-the-text-block) (mcp_alias_migration_test.clj:4598)
   FAIL in (a-fact-of-any-shape-is-named-in-the-text-block) (mcp_alias_migration_test.clj:4600)
   {:test 1, :pass 5, :fail 4, :error 0}
   EXIT=1

   $ cd /tmp/q5z13-review-fx/helper-gap-wt && ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -A:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/helper_gap_runner.clj'; rc=$?; echo EXIT=$rc
   WARNING: Use of :main-opts with -A is deprecated. Use -M instead.
   helper-specific sabotage witnesses => {:test 4, :pass 259, :fail 0, :error 0}
   EXIT=0
   ```

3. **GO-WITH-FIX — closing the required-namespace graph to 125 kinds is a sound, deliberately cheap over-approximation, but holding out `mcp_tool` leaves exactly the future-regression hole the requirement says is closed.** `test/clj_surgeon/mcp_alias_migration_test.clj:1078-1110` excludes `clj-surgeon.mcp-tool`; `refusal-kinds-in-source` only scrapes alias-prefixed spellings from that held-out file. The 125 set contains clearly unreachable kinds such as `unknown-buffer-site` and the reachable `invalid-diagnostic-output`. That cost is right: 89 extra synthetic renderer assertions are much cheaper than missing a live kind. The boundary is not complete, however. In a scratch copy I added a `defmulti`/`defmethod` in `mcp_tool`, routed it from `handle-alias-migration` (`src/clj_surgeon/mcp_tool.clj:1441`), and dynamically composed `heldout-protocol-kind`. All four enumeration/dynamic-spelling witnesses stayed green (24 assertions), while the live entrance returned a kind absent from the set. Thus both requested attacks—held-out construction and a multimethod-produced kind—reproduce in one case. The current renderer still shows the kind; this is a future witness failure, not a current missing visible fact.

   Exact patch proof:

   ```text
   $ git -C /tmp/q5z13-review-fx/sabotage-wt diff --unified=1 -- src/clj_surgeon/mcp_tool.clj | sed -n '1,120p'
   diff --git a/src/clj_surgeon/mcp_tool.clj b/src/clj_surgeon/mcp_tool.clj
   index 3b2af441..9cfb0ab6 100644
   --- a/src/clj_surgeon/mcp_tool.clj
   +++ b/src/clj_surgeon/mcp_tool.clj
   @@ -1439,2 +1439,12 @@
    
   +(defmulti heldout-alias-refusal :kind)
   +
   +(defmethod heldout-alias-refusal :alias [_]
   +  {:ok false
   +   :operation "alias_migration"
   +   :error_type (str "heldout-" "protocol-kind")
   +   :error "held-out mcp_tool refusal"
   +   :source_unchanged true
   +   :next_call nil})
   +
    ;; @spec MCP-OP-ALIAS-001
   @@ -1447,3 +1457,5 @@
          (let [normalized (json/parse-string (json/generate-string params) true)]
   -         (if-not @runtime-config
   +         (if (= "HELDOUT" (:workspace_root normalized))
   +           (heldout-alias-refusal {:kind :alias})
   +           (if-not @runtime-config
              {:ok false
   @@ -1469,3 +1481,3 @@
                              (:params routed))
   -                        :workspace_root (:workspace-root routed))))))))
   +                        :workspace_root (:workspace-root routed)))))))))
        :summarize alias-migration-summary
   ```

   Exact execution and verbatim output:

   ```text
   $ cd /tmp/q5z13-review-fx/sabotage-wt && ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -A:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/sabotage_runner.clj'; rc=$?; echo EXIT=$rc
   enumeration witnesses => {:test 4, :pass 24, :fail 0, :error 0}
   held-out live kind => heldout-protocol-kind
   held-out kind enumerated? => false
   held-out visible kind? => true
   EXIT=0
   ```

4. **The round-twelve BLOCKING is closed at this tip.** `src/clj_surgeon/mcp_alias_migration.clj:197-213` takes the separator from the candidate `Path`'s filesystem, and `src/clj_surgeon/mcp_paths.clj:10-35` uses the host filesystem separator. `independent-scope-count` at `mcp_alias_migration.clj:373-429` shares the glob patterns, `source-file-name?`, skipped-directory policy, and a second `walkFileTree`; it does **not** share the first walk's string path arithmetic or its exclusion representation. That is narrower than “fully independent,” but it is exactly the independence MCP-OP-ALIAS-060 states.

   The proposed static attacks did not make both counts wrong. A symlinked directory is intentionally not followed under ALIAS-037; a directory with a trailing space is counted; NFC and NFD names remain two distinct paths. The five selected owners produced equal counts and a complete five-file commit. The RED→GREEN pair separately proves a forced disagreement yields `alias-migration-discovery-incomplete`, both counts, `source_unchanged true`, `mutation_attempted false`, and no writes. I found no partial write or workspace escape.

   Exact command and verbatim output:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/probe.clj' | sed -n '/=== discovery independence/,/=== root-list/p'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   === discovery independence adversaries ===
   scan => {:ok true, :files ["é/owner.clj" "real/owner.clj" "src/plain.clj" "trail /owner.clj" "é/owner.clj"]}
   second count => 5
   symlink dir followed? => false
   NFC/NFD both distinct? => true
   receipt => {:ok true, :committed true, :files 5, :sites 5}
   all five migrated? => true

   === root-list marker and whole-text ceilings ===
   EXIT=0
   ```

   **Suspicion, not reproduced:** because the witness compares counts rather than path sets and the directory inventory is not transaction-fenced, equal-cardinality churn between the two walks could theoretically substitute one file for another. I did not reproduce a partial commit from that race, and ordinary operation assumes a quiescent source inventory.

5. **The control-character and reader-defined mention rules hold, with one transport nuance.** `src/clj_surgeon/mcp_alias_migration.clj:218-278` refuses BOM at index 0, RTL override, U+FFFD, the twelve specified controls/invisibles, and unpaired surrogates. A lone combining mark is printable data and a valid surrogate pair denotes one valid supplementary code point; both are legal Linux filenames and were selected. “Paired surrogate not refused” is correct.

   Exact command and verbatim output:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/probe.clj' | sed -n '/=== code-point boundary/,/=== finite arbitrary/p'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   === code-point boundary ===
   BOM-at-start => U+FEFF index=0
   RTL-override => U+202E index=4
   combining-alone => allowed
   paired-surrogate => allowed
   replacement-character => U+FFFD index=0
   combining scan => {:ok true, :files ["́/one.clj"]}
   paired scan => {:ok true, :files ["😀/two.clj"]}

   === finite arbitrary fact shapes ===
   EXIT=0
   ```

   U+FFFD is in the refused class, but the actual Jackson version does not turn the overlong `C0 AF` spelling of slash into U+FFFD; it normalizes it to `/` before the verb sees it. This does not bypass confinement—the verb receives an ordinary visible separator—but a strict “malformed UTF-8 becomes U+FFFD” transport claim would be false.

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/overlong_probe.clj'; rc=$?; echo EXIT=$rc
   parsed path => "src/x/**"
   code points => [115 114 99 47 120 47 42 42]
   path gate => nil
   EXIT=0
   ```

   `src/clj_surgeon/alias_migration.clj:1113-1140` now stops at `:uneval` and still descends `(comment …)`. This is consistent with ALIAS-034's reader boundary: `#_` prevents the form from entering the reader's value; `comment` is an ordinary macro whose body the reader constructs and evaluation later ignores. A grep-minded operator may reasonably expect **both** spellings to be stale-source hits—grep sees both—so the product choice is debatable, but the EARS text at `docs/intent/alias-migration/alias-migration-specs.md:76` now says the choice plainly and explains it without relying on this review.

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /tmp/q5z13-review-fx/mentions_probe.clj'; rc=$?; echo EXIT=$rc
   reader => [:sentinel]
   sites => ["src/shapes.clj:3" "src/shapes.clj:4"]
   EXIT=0
   ```

6. **Both FAN runs are byte-identical; the reported run-2 failure belongs to the round-twelve scorer, not the verb.** The generator ran with `--n 21 --seed 7 --k 6`. Run 1's single live call on port 8035 committed 21 files, 63 sites, and 30 collisions; the scorer passed 6/6 and `diff -r` passed. Run 2 added byte copies of `ns_003.clj` and `ns_005.clj` beneath the literal `src/acid/fanout/\` directory, extended the manifest to 23 targets, and made one live call on port 8036. It committed 23 files, 69 sites, and 34 collisions. Both servers were stopped immediately after their call.

   Generator and run-1 exact commands/output:

   ```text
   $ /tmp/q5z13-review-fx/prepare-fan.sh; rc=$?; echo EXIT=$rc
   gen-fanout: n=21 seed=7 k=6 namespaces=100 targets=21 out=/tmp/q5z13-review-fx/fan/gen
   gen-fanout: alias histogram {"st2" 5, "store-2" 5, "es" 5, "store2" 6}
   gen-fanout: old-alias histogram {"st" 4, "db" 3, "s" 4, "store" 4, "repo" 3, "k" 3} collisions=30
   d9e3a26419c12452277c6d2ca77dd4ada5a48aa8
   71ba1c3b877e717c65f8f6b113ae6407f81aba55
   EXIT=0

   $ /tmp/q5z13-review-fx/mcp-call.sh 8035 /tmp/q5z13-review-fx/fan-run1-args.json; rc=$?; echo EXIT=$rc
   SESSION=8ba1b4a8-25d4-4c18-b11a-3fdc568bdbd1
   --- ONE alias_migration call ---
   id: 8ba1b4a8-25d4-4c18-b11a-3fdc568bdbd1
   event: message
   data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"alias_migration\n  21 files · 63 sites · aliases {\"es\" 5, \"st2\" 5, \"store-2\" 5, \"store2\" 6} · 30 collisions resolved · 858.44 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified\n✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-d7fe6cca-8673-4cfc-9e98-23dd6683bd5e.edn (best-effort retention)"}],"isError":false,"structuredContent":{"details_retained":20,"workspace_root":"/tmp/q5z13-review-fx/fan/run1","committed":true,"kondo_delta":{"status":"not-requested"},"alias_histogram":{"es":5,"st2":5,"store-2":5,"store2":6},"sites":63,"string_mention_sites_shown":0,"details_retention":"best-effort","focused_test":{"status":"not-requested"},"string_mentions":0,"string_mention_sites":[],"lib_renamed":null,"elapsed_ms":858.435596,"next_action":"none","refer_sites":0,"files":21,"collisions_resolved":30,"receipt_hash":"25494530a957b7ee121815fcd4d357f8a41258c4236c554488325a6301356ed4","ok":true,"operation":"alias_migration","undo_receipt":"/home/forge/.local/state/clj-surgeon/workspaces/503d749a2ecce6a500947cbcd7f4e7853cea090288441a0ea24b5421b4cf0770/receipts/13595bda-dadf-408a-97d7-1b648fe61bef.edn","details_path":".clj-surgeon/alias-migration/detail-d7fe6cca-8673-4cfc-9e98-23dd6683bd5e.edn"}}}


   EXIT=0

   $ FAN_BASE=d9e3a26419c12452277c6d2ca77dd4ada5a48aa8 ~/bin/suite-run /tmp/q5z13-review-fx/fan/tools/bench/fanout/rescore-FAN.sh /tmp/q5z13-review-fx/fan/run1 21 /tmp/q5z13-review-fx/fan/gen; rc=$?; echo EXIT=$rc
   rescore-FAN: worktree=/tmp/q5z13-review-fx/fan/run1 n=21 base=d9e3a26419c12452277c6d2ca77dd4ada5a48aa8 fixtures=/tmp/q5z13-review-fx/fan/gen
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   EXIT=0
   ```

   Run-2 live call, exact command/output:

   ```text
   $ /tmp/q5z13-review-fx/mcp-call.sh 8036 /tmp/q5z13-review-fx/fan-run2-args.json; rc=$?; echo EXIT=$rc
   SESSION=0786c67e-1260-427b-a9e6-a9796e8b6dad
   --- ONE alias_migration call ---
   id: 0786c67e-1260-427b-a9e6-a9796e8b6dad
   event: message
   data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"alias_migration\n  23 files · 69 sites · aliases {\"es\" 5, \"st2\" 6, \"store-2\" 6, \"store2\" 6} · 34 collisions resolved · 776.48 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified\n✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-40107a5d-ea79-429e-8584-c9577b8755f7.edn (best-effort retention)"}],"isError":false,"structuredContent":{"details_retained":20,"workspace_root":"/tmp/q5z13-review-fx/fan/run2","committed":true,"kondo_delta":{"status":"not-requested"},"alias_histogram":{"es":5,"st2":6,"store-2":6,"store2":6},"sites":69,"string_mention_sites_shown":0,"details_retention":"best-effort","focused_test":{"status":"not-requested"},"string_mentions":0,"string_mention_sites":[],"lib_renamed":null,"elapsed_ms":776.480008,"next_action":"none","refer_sites":0,"files":23,"collisions_resolved":34,"receipt_hash":"7db8cf6b27c8581ddc1e0bc8b0e1c7241ca7407e79d5362a6071422a1e5104e1","ok":true,"operation":"alias_migration","undo_receipt":"/home/forge/.local/state/clj-surgeon/workspaces/82e157af15376c3d763fc946199a6f2168b410ea0edb104b9ae739534207cdd8/receipts/6b64f72e-3db6-4626-b7c2-8dd2dcde8ec7.edn","details_path":".clj-surgeon/alias-migration/detail-40107a5d-ea79-429e-8584-c9577b8755f7.edn"}}}


   EXIT=0
   ```

   The scorer at `aa95fc7` (the round-twelve behavior) C-quotes exactly the two backslash paths and fails only CHECK 1:

   ```text
   $ FAN_BASE=71ba1c3b877e717c65f8f6b113ae6407f81aba55 ~/bin/suite-run /tmp/q5z13-review-fx/fan/old-tools/bench/fanout/rescore-FAN.sh /tmp/q5z13-review-fx/fan/run2 21 /tmp/q5z13-review-fx/fan/run2-fixtures; rc=$?; echo EXIT=$rc
   rescore-FAN: worktree=/tmp/q5z13-review-fx/fan/run2 n=21 base=71ba1c3b877e717c65f8f6b113ae6407f81aba55 fixtures=/tmp/q5z13-review-fx/fan/run2-fixtures
   CHECK 1 file-set: FAIL changed=23 expected=23 missing=2 ["src/acid/fanout/\\/ns_003.clj" "src/acid/fanout/\\/ns_005.clj"] extras=2 ["\"src/acid/fanout/\\\\/ns_003.clj\"" "\"src/acid/fanout/\\\\/ns_005.clj\""]
   CHECK 2 form-equality: PASS compared=23 equal=23 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=116 intact=116 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=102 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: FAILED CHECK 1 file-set
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   EXIT=1
   ```

   NUL-delimited Git output contains the raw names, the fixed scorer passes 6/6, and both recursive byte diffs are empty. This confirms `inb-9c18e2`'s attribution.

   ```text
   $ git -C /tmp/q5z13-review-fx/fan/run2 diff -z --name-only 71ba1c3b877e717c65f8f6b113ae6407f81aba55 | python3 -c 'import sys; p=[x.decode() for x in sys.stdin.buffer.read().split(b"\0") if x]; print("NUL paths/count =>", repr([x for x in p if chr(92) in x]), len(p))'; echo EXIT=$?
   NUL paths/count => ['src/acid/fanout/\\/ns_003.clj', 'src/acid/fanout/\\/ns_005.clj'] 23
   EXIT=0

   $ FAN_BASE=71ba1c3b877e717c65f8f6b113ae6407f81aba55 ~/bin/suite-run /tmp/q5z13-review-fx/fan/tools/bench/fanout/rescore-FAN.sh /tmp/q5z13-review-fx/fan/run2 21 /tmp/q5z13-review-fx/fan/run2-fixtures; score_rc=$?; echo SCORE_EXIT=$score_rc; diff -r --exclude=.git --exclude=.clj-surgeon /tmp/q5z13-review-fx/fan/run1 /tmp/q5z13-review-fx/fan/gen/canonical-21; echo RUN1_DIFF_EXIT=$?; diff -r --exclude=.git --exclude=.clj-surgeon /tmp/q5z13-review-fx/fan/run2 /tmp/q5z13-review-fx/fan/run2-fixtures/canonical-21; echo RUN2_DIFF_EXIT=$?
   rescore-FAN: worktree=/tmp/q5z13-review-fx/fan/run2 n=21 base=71ba1c3b877e717c65f8f6b113ae6407f81aba55 fixtures=/tmp/q5z13-review-fx/fan/run2-fixtures
   CHECK 1 file-set: PASS changed=23 expected=23 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=23 equal=23 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=116 intact=116 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=102 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   SCORE_EXIT=0
   RUN1_DIFF_EXIT=0
   RUN2_DIFF_EXIT=0
   ```

7. **All six claimed RED→GREEN pairs and all release gates reproduce; the reviewed checkout remained clean and ports 8035–8037 are stopped.** The witness locations are `test/clj_surgeon/mcp_alias_migration_test.clj:4006`, `:4117`, `:4232`, `:4306`, `:4382`, and `:4527`; Make targets are `Makefile:175` and `:179`.

   Exact RED→GREEN command and verbatim output:

   ```text
   $ /tmp/q5z13-review-fx/redgreen.sh 2>&1 | rg '^\{:test|^[0-9a-f]{7}'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   {:test 2, :pass 1, :fail 13, :error 0}
   32b4115 RED EXIT=1
   {:test 2, :pass 23, :fail 0, :error 0}
   a002cb3 GREEN EXIT=0
   {:test 2, :pass 5, :fail 6, :error 0}
   59e2f75 RED EXIT=1
   {:test 2, :pass 13, :fail 0, :error 0}
   e57af95 GREEN EXIT=0
   {:test 3, :pass 19, :fail 4, :error 0}
   b70223f RED EXIT=1
   {:test 3, :pass 23, :fail 0, :error 0}
   8fc1004 GREEN EXIT=0
   {:test 1, :pass 19, :fail 44, :error 0}
   fe91606 RED EXIT=1
   {:test 1, :pass 63, :fail 0, :error 0}
   ddfcaef GREEN EXIT=0
   {:test 1, :pass 2, :fail 3, :error 0}
   8da1e62 RED EXIT=1
   {:test 1, :pass 5, :fail 0, :error 0}
   98e6d31 GREEN EXIT=0
   {:test 2, :pass 51, :fail 8, :error 0}
   0f94b10 RED EXIT=1
   {:test 2, :pass 59, :fail 0, :error 0}
   12bc539 GREEN EXIT=0
   EXIT=0
   ```

   Release gates, exact commands and verbatim terminal outputs:

   ```text
   $ ~/bin/suite-run bb test/run_all.clj; rc=$?; echo EXIT=$rc
   Ran 737 tests containing 6275 assertions.
   0 failures, 0 errors.
   EXIT=0

   $ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; rc=$?; echo EXIT=$rc
   Ran 490 tests containing 6867 assertions.
   0 failures, 0 errors.
   EXIT=0

   $ make mcp-operation-oracle; rc=$?; echo EXIT=$rc
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   EXIT=0

   $ make repository-hygiene; rc=$?; echo EXIT=$rc
   # @spec MCP-OP-ALIAS-036
   # @spec MCP-OP-ALIAS-053
   repository hygiene: no machine-local build cache is tracked at any depth
   EXIT=0
   ```

   Entry/exit proof (the empty region after HEAD is clean status plus an empty stash list):

   ```text
   $ git rev-parse HEAD; git status --porcelain; git stash list; ss -ltn '( sport = :8035 or sport = :8036 or sport = :8037 )'
   12bc53909be6db5f5413ae151e29e04a050a4934
   State Recv-Q Send-Q Local Address:Port Peer Address:Port
   ```

## GO-WITH-FIX

This tip is **not GO on its own for MCP/main**: the prior partial-migration blocker is closed and no path escaped, but the 512-character root-list marker accounting, bounded printing of arbitrary fact values, and the two held-out/helper witness gaps should be fixed before treating the tip itself as GO.
