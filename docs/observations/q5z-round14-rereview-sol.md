## GO-WITH-FIX

1. **GO-WITH-FIX — `bounded-pr-str` still cannot bound work performed inside an arbitrary value's `toString`.** `src/clj_surgeon/mcp_tool.clj:1281-1342` bounds writes made *after* `print-method` receives the value, but `print-method` for an otherwise ordinary object invokes that object's `toString` before any characters reach `ceiling-writer`. A custom `deftype` whose `toString` does not return therefore hangs the renderer. This falsifies the literal “whatever the value”/bounded-work claim in `docs/intent/alias-migration/alias-migration-specs.md:108`, although it does not expose a present live MCP receipt: JSON cannot carry a JVM object, and none of the current internally constructed refusal fields has this shape. The practical fix is to admit only closed data shapes before printing (and render an unsupported-value marker for anything else), because no `Writer` can pre-empt arbitrary code inside `toString`.

   Exact command:

   ```text
   $ timeout 15s ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/tostring_hang.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   before custom toString
   EXIT=124
   ```

2. **GO-WITH-FIX — the new source guard misses a live refusal kind derived with `keyword`/`name` from an enclosing parameter.** `test/clj_surgeon/mcp_alias_migration_test.clj:1292-1324` treats any dynamic `:error-type` expression containing any parameter of its enclosing function as the `refusal` constructor's legitimate forwarded argument. That exemption is much broader than the constructor it intends to exempt. In the requested sabotage, keyword literals in a `case` were enumerated, and a namespace imported only with `:refer` was reached; but `:error_type (name (keyword (:dynamic_kind params)))` in a router helper was neither enumerated nor reported as unscannable. Routing it from `handle-alias-migration` returned live `runtime-data-kind`, absent from the set, while both advertised guard witnesses stayed green. This is the same future-regression class as round thirteen, not a missing kind in the unmodified tip, so it is GO-WITH-FIX rather than NO-GO. Narrow `constructor-site?` to the actual constructor entry (or remove the heuristic and require an explicit forwarding marker).

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/enumeration_probe.clj'; probe_rc=$?; echo PROBE_EXIT=$probe_rc; ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/test_vars.clj no-reachable-namespace-spells-a-refusal-kind-dynamically the-enumeration-reaches-the-routers-entrance-slice-and-every-spelling'; test_rc=$?; echo TEST_EXIT=$test_rc
   ```

   Verbatim output:

   ```text
   enumerated-count= 145
   case-branch-kind enumerated= true
   cond-branch-kind enumerated= true
   fallback-kind enumerated= true
   refer-only-kind enumerated= true
   refer-namespace-reached= true
   unscannable-sites= []
   live-kind= runtime-data-kind
   live-kind-enumerated= false
   PROBE_EXIT=0
   {:test 2, :pass 9, :fail 0, :error 0}
   TEST_EXIT=0
   ```

3. **Round-thirteen item 1 is closed.** `src/clj_surgeon/mcp_alias_migration.clj:829-873` constructs the final vector including the marker and measures `(count (pr-str ...))` while shrinking. The exact 512-character rendering is whole; the 513-character rendering is cut to 436 and names `+1 more roots`; and a case where the first marker cannot fit in the residual budget shrinks an additional entry and names `+2 more roots`. The contract at `docs/intent/alias-migration/alias-migration-specs.md:108` promises **characters**, not transport bytes. Concretely this is Java/Clojure UTF-16 code-unit count: BMP multibyte root names rendered as 493 characters and 957 UTF-8 bytes. Supplementary code points are counted conservatively as two units. There is no 512-byte promise.

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/probe.clj' | sed -n '/=== root listing edges ===/,/=== bounded nested/p'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   === root listing edges ===
   exact-512 items= 4 chars= 512 bytes= 512 marker= nil
   one-past-513 items= 4 chars= 436 bytes= 438 marker= … [+1 more roots, complete in structuredContent]
   marker-exceeds-remainder items= 4 chars= 406 bytes= 408 marker= … [+2 more roots, complete in structuredContent]
   multibyte items= 4 chars= 493 bytes= 957 within-char-ceiling= true within-byte-ceiling= false
   === bounded nested endless ===
   EXIT=0
   ```

4. **The intended round-thirteen item 2 shapes are closed, and the key-specific sabotage now has direct witnesses.** `src/clj_surgeon/mcp_tool.clj:1281-1386` bounded an endless sequence nested in a map at 55 realizations and an `APersistentMap` with an endless entry/key sequence at 20. The committed deep-value and 10 MB witnesses are green in the 498-test gate. A corpus of ordinary receipts is byte-for-byte identical before (`6cbcbd48`) and after (`1cc5990b`). Replanting exactly `:helper_sabotage_detail {:nested "value"}` in the live workspace-router refusal and dropping maps in the renderer makes both the dedicated live assertion and the all-live-receipts assertion fail.

   Exact commands:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/probe.clj' | sed -n '/=== bounded nested endless ===/,/=== ordinary corpus ===/p'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   $ ~/bin/suite-run bash -lc 'diff -u <(cd /home/forge/tmp/q5z14-review-fx/pre-bounded && cp=$(clojure -Spath -M:clj-surgeon/mcp-test) && java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/corpus.clj) <(cd /home/forge/tmp/sol/q5z14-wt && cp=$(clojure -Spath -M:clj-surgeon/mcp-test) && java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/corpus.clj)'; rc=$?; echo DIFF_EXIT=$rc
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/test_vars.clj the-live-invalid-workspace-root-refusal-renders-cause-remedy-and-next-call every-live-refusal-renders-every-key-its-receipt-carries' 2>&1 | rg 'helper_sabotage_detail|^\{:test'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   ```

   Verbatim outputs:

   ```text
   === bounded nested endless ===
   nested chars= 176 realisations= 55 elided= true
   endless-key-map chars= 185 realisations= 20 elided= true
   === ordinary corpus ===
   EXIT=0
   ```

   ```text
   DIFF_EXIT=0
   ```

   ```text
   {:file "mcp_alias_migration_test.clj", :line 1454, :type :fail, :expected (str/includes? text (name field)), :actual (not (str/includes? "alias_migration\n  refused · invalid-workspace-root · 1.00 ms\n\n✓ source unchanged\n→ workspace_root must be absolute\nfacts · next_action=\"pass_an_existing_absolute_workspace_root\" · path=[\"workspace_root\"] · reason=\"invalid-workspace-root\" · source_unchanged=true · workspace_root_given=\"\\\"relative/path\\\"\"\nremedy · Resend with workspace_root set to an absolute path naming a directory that already exists; \"relative/path\" is not one. No next_call is composed because only the caller knows which workspace it meant.\nnext_call · none — this refusal has no mechanically composable correction; the remedy above names what only the caller can decide" "helper_sabotage_detail")), :message "invalid-workspace-root · the text block drops the discriminating field helper_sabotage_detail"}
   {:file "mcp_alias_migration_test.clj", :line 4813, :type :fail, :expected (empty? missing), :actual (not (empty? ["helper_sabotage_detail"])), :message "invalid-workspace-root · the text block drops [\"helper_sabotage_detail\"], which structuredContent carries"}
   {:test 2, :pass 68, :fail 2, :error 0}
   EXIT=1
   ```

5. **Round-thirteen item 3 is closed for the committed shapes, subject to finding 2.** `test/clj_surgeon/mcp_alias_migration_test.clj:1129-1431` reads 14 of 56 router defs and reads non-literal `:error-type` values structurally. At the §3 fix commit `67526f64`, the enumeration is exactly 138 and contains both `no-match` and `ambiguous-match` minted at current `src/clj_surgeon/mcp_change_buffer.clj:1077-1081`; the tip is 139 because §4 subsequently added `diagnostic-output-truncated`. The requested `case` and `:refer` attacks succeed, as item 2's output shows. The `keyword`/`name` attack is the remaining guard defect.

   Exact commands:

   ```text
   $ (cd /home/forge/tmp/q5z14-review-fx/hist/67526f64 && ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/enumeration_current.clj'); rc=$?; echo EXIT=$rc
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/enumeration_current.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim outputs:

   ```text
   enumerated= 138
   router-slice-defs= 14 router-total-defs= 56
   no-match= true ambiguous-match= true
   EXIT=0
   ```

   ```text
   enumerated= 139
   router-slice-defs= 14 router-total-defs= 56
   no-match= true ambiguous-match= true
   EXIT=0
   ```

6. **The E-CALLER `verify:fast` defect is closed, including its ceiling and replay behavior.** `src/clj_surgeon/mcp_change_buffer.clj:29-45,1440-1486` gives diagnostic documents their own 4,194,304-byte capture. Exactly 4,194,304 bytes parses; 4,194,305 refuses as `diagnostic-output-truncated`, names measured size and budget, and is never parsed as a truncated EDN document. The original E-CALLER T2/T/2 tree at `65fe39a9071083f478ed091ab64ebdf05c02abbd`, served as the server's project root, no longer fails its baseline as `invalid-diagnostic-output`. Its later Standard Clojure formatter check refuses, rolls back all 21 files, and publishes a `next_call` that drops `verify`; replaying that exact structured call commits 21 files and 63 sites. `src/clj_surgeon/mcp_alias_migration.clj:2490-2512,2642-2665,2721-2726` applies the same correction on pre- and post-write branches.

   The class ruling is: a human-visible evidence cap should never be reused as detector input. Every actual use of `exact-verification-visible-bytes` is now evidence-bound: `src/clj_surgeon/mcp_change_buffer.clj:1280-1282` supplies the default; `:1382-1406` decides exact verification from exit/process outcome and retains the bounded output only as evidence; `:1408-1426` likewise decides a non-diagnostic check from completion/exit and retains output only on failure. The diagnostic parser at `:1440-1486` does **not** use that cap; its separate document-input ceiling may refuse, but it does so before parsing and with a typed measured result. No other source/test use exists.

   Exact ceiling command:

   ```text
   $ printf 'exact-bytes='; /home/forge/tmp/q5z14-review-fx/exact/clj-kondo | wc -c; printf 'over-bytes='; /home/forge/tmp/q5z14-review-fx/over/clj-kondo | wc -c; ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/diagnostic_probe.clj'; rc=$?; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   exact-bytes=4194304
   over-bytes=4194305
   at-ceiling {:ok true, :error-type nil}
    check= {:ok true}
   over-ceiling {:ok false, :error-type :diagnostic-output-truncated}
    check= {:ok false, :error-type :diagnostic-output-truncated, :output-bytes 4194305, :diagnostic_byte_budget 4194304}
   EXIT=0
   ```

   Exact E-CALLER fixture/request proof command:

   ```text
   $ sed -n '1,80p' /home/forge/tmp/q5z14-review-fx/ecaller-exact.json; git -C /home/forge/tmp/arms/ecaller/ecaller-T2-T-2/worktree rev-parse HEAD; git clone --quiet /home/forge/tmp/arms/ecaller/ecaller-T2-T-2/worktree /home/forge/tmp/q5z14-review-fx/ecaller-replay && git -C /home/forge/tmp/q5z14-review-fx/ecaller-replay rev-parse HEAD
   ```

   Verbatim output:

   ```text
   {
     "from": {"lib": "acid.fanout.store", "var": "find-event"},
     "to": {"lib": "acid.fanout.store2", "var": "fetch-event",
            "alias_policy": ["store2", "st2", "es", "store-2"]},
     "scope": {"paths": ["src"]},
     "expect": {"files": 21},
     "verify": "fast"
   }
   65fe39a9071083f478ed091ab64ebdf05c02abbd
   65fe39a9071083f478ed091ab64ebdf05c02abbd
   ```

   Exact E-CALLER replay command:

   ```text
   $ first=$(/home/forge/tmp/q5z14-review-fx/mcp-call.sh 8076 alias_migration /home/forge/tmp/q5z14-review-fx/ecaller-exact.json); data=$(printf '%s\n' "$first" | sed -n 's/^data: //p'); printf '%s\n' "$data" | jq -c '{first:{ok:.result.structuredContent.ok,error_type:.result.structuredContent.error_type,source_unchanged:.result.structuredContent.source_unchanged,rolled_back:.result.structuredContent.rolled_back,files_restored:.result.structuredContent.files_restored,files_still_migrated:.result.structuredContent.files_still_migrated,next_call_has_verify:(.result.structuredContent.next_call|has("verify")),next_call:.result.structuredContent.next_call}}'; next=$(printf '%s\n' "$data" | jq -c '.result.structuredContent.next_call'); second=$(/home/forge/tmp/q5z14-review-fx/mcp-call.sh 8076 alias_migration <(printf '%s\n' "$next")); printf '%s\n' "$second" | sed -n 's/^data: //p' | jq -c '{replay:{ok:.result.structuredContent.ok,committed:.result.structuredContent.committed,files:.result.structuredContent.files,sites:.result.structuredContent.sites}}'; rc=${PIPESTATUS[0]}; echo EXIT=$rc
   ```

   Verbatim output:

   ```text
   {"first":{"ok":false,"error_type":"verification-failed","source_unchanged":true,"rolled_back":true,"files_restored":21,"files_still_migrated":0,"next_call_has_verify":false,"next_call":{"op":"alias_migration","from":{"lib":"acid.fanout.store","var":"find-event"},"to":{"lib":"acid.fanout.store2","var":"fetch-event","alias_policy":["store2","st2","es","store-2"],"refer_policy":"preserve-refer"},"scope":{"paths":["src"]},"expect":{"files":21}}}}
   {"replay":{"ok":true,"committed":true,"files":21,"sites":63}}
   EXIT=0
   ```

7. **Both FAN gates pass with byte-identical output under the latest fail-closed scorer.** At fetched commit `f4975d0b`, `bench/fanout/rescore-FAN.sh:43-88` drives the structural, load, and behavior checks and requires all six groups. The full fetched tip was `f4975d0b837e0526f9287c3c0a3a38118a3d9c51`. One live call on port 8075 transformed the standard seed-7/K-6 tree to 21 files, 63 sites, 30 collisions. A second live call on the same explicit port transformed the literal-backslash tree to 23 files, 69 sites, 34 collisions. The latest scorer passes all six checks for both, including its independent filesystem-vs-Git path-list cross-check, and recursive byte diffs against both canonical trees are empty. Servers on 8075 and 8076 were stopped; 8075–8077 have no listeners.

   Exact fetch command and output:

   ```text
   $ git fetch origin bridge/fanout-fixtures-in-git && git rev-parse FETCH_HEAD && git log -1 --oneline FETCH_HEAD
   From https://github.com/realgenekim/clj-surgeon
    * branch              bridge/fanout-fixtures-in-git -> FETCH_HEAD
   f4975d0b837e0526f9287c3c0a3a38118a3d9c51
   f4975d0b bench/fanout: GREEN — CHECK 1 cross-checks git's listing against an independent filesystem walk of src/; every consumed listing rejects stderr
   ```

   Standard live-call output:

   ```text
   $ /home/forge/tmp/q5z14-review-fx/mcp-call.sh 8075 alias_migration /home/forge/tmp/q5z14-review-fx/fan-run1.json; rc=$?; echo EXIT=$rc
   SESSION=9b84c7df-be7e-4b1a-8af8-44da45501e7f
   id: 9b84c7df-be7e-4b1a-8af8-44da45501e7f
   event: message
   data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"alias_migration\n  21 files · 63 sites · aliases {\"es\" 5, \"st2\" 5, \"store-2\" 5, \"store2\" 6} · 30 collisions resolved · 399.95 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified\n✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-edaf1b44-9f3e-4d3e-a6d0-277059b297a5.edn (best-effort retention)"}],"isError":false,"structuredContent":{"committed":true,"alias_histogram":{"es":5,"st2":5,"store-2":5,"store2":6},"sites":63,"files":21,"collisions_resolved":30,"ok":true,"operation":"alias_migration"}}}
   EXIT=0
   ```

   Standard scorer and byte-diff output:

   ```text
   $ FAN_BASE=1a3243716ca1304f86c68ca84512ee86c9970819 ~/bin/suite-run /home/forge/tmp/q5z14-review-fx/fan-tools/bench/fanout/rescore-FAN.sh /home/forge/tmp/q5z14-review-fx/fan/run1 21 /home/forge/tmp/q5z14-review-fx/fan/generated; score_rc=$?; echo SCORE_EXIT=$score_rc; diff -r --exclude=.git --exclude=.clj-surgeon --exclude=.cpcache --exclude=target /home/forge/tmp/q5z14-review-fx/fan/run1 /home/forge/tmp/q5z14-review-fx/fan/generated/canonical-21; diff_rc=$?; echo BYTE_DIFF_EXIT=$diff_rc
   rescore-FAN: worktree=/home/forge/tmp/q5z14-review-fx/fan/run1 n=21 base=1a3243716ca1304f86c68ca84512ee86c9970819 fixtures=/home/forge/tmp/q5z14-review-fx/fan/generated
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   SCORE_EXIT=0
   BYTE_DIFF_EXIT=0
   ```

   Backslash live-call output:

   ```text
   $ /home/forge/tmp/q5z14-review-fx/mcp-call.sh 8075 alias_migration /home/forge/tmp/q5z14-review-fx/fan-run2.json; rc=$?; echo EXIT=$rc
   SESSION=81f3fc7e-e8a6-4ed6-ae33-f3d54adc0891
   id: 81f3fc7e-e8a6-4ed6-ae33-f3d54adc0891
   event: message
   data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"alias_migration\n  23 files · 69 sites · aliases {\"es\" 5, \"st2\" 6, \"store-2\" 6, \"store2\" 6} · 34 collisions resolved · 475.16 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified\n✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-6357d24f-f3e6-4e93-a9c0-3bf9fd67f7e8.edn (best-effort retention)"}],"isError":false,"structuredContent":{"committed":true,"alias_histogram":{"es":5,"st2":6,"store-2":6,"store2":6},"sites":69,"files":23,"collisions_resolved":34,"ok":true,"operation":"alias_migration"}}}
   EXIT=0
   ```

   Backslash scorer and byte-diff output:

   ```text
   $ FAN_BASE=7f4cf26d2e93eb05f3f1db647cc53e8933827f53 ~/bin/suite-run /home/forge/tmp/q5z14-review-fx/fan-tools/bench/fanout/rescore-FAN.sh /home/forge/tmp/q5z14-review-fx/fan/run2 21 /home/forge/tmp/q5z14-review-fx/fan/backslash-fixtures; score_rc=$?; echo SCORE_EXIT=$score_rc; diff -r --exclude=.git --exclude=.clj-surgeon --exclude=.cpcache --exclude=target /home/forge/tmp/q5z14-review-fx/fan/run2 /home/forge/tmp/q5z14-review-fx/fan/backslash-fixtures/canonical-21; diff_rc=$?; echo BYTE_DIFF_EXIT=$diff_rc
   rescore-FAN: worktree=/home/forge/tmp/q5z14-review-fx/fan/run2 n=21 base=7f4cf26d2e93eb05f3f1db647cc53e8933827f53 fixtures=/home/forge/tmp/q5z14-review-fx/fan/backslash-fixtures
   CHECK 1 file-set: PASS changed=23 expected=23 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=23 equal=23 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=116 intact=116 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=102 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   SCORE_EXIT=0
   BYTE_DIFF_EXIT=0
   ```

8. **All four RED→GREEN pairs reproduce, and all current gates are green.** `Makefile:175-191` owns the oracle, hygiene, and focused MCP entry points, while `deps.edn:30-41` binds the direct MCP test runner used below. The RED witnesses fail at `8fa9a0fa`, `6cbcbd48`, `bde96387`, and `4cc96eab`; the same focused witnesses pass at immediate fixes `524dd21d`, `c5e63e69`, `67526f64`, and `52123fa9`. The current tip is clean and detached at the requested SHA. The inspected delta is nine commits, seven files, 839 insertions and 39 deletions.

   Exact HEAD/delta command:

   ```text
   $ git rev-parse HEAD; git status --short --branch; git log --oneline 12bc539..1cc5990b; git diff --stat 12bc539..1cc5990b
   ```

   Verbatim output:

   ```text
   1cc5990b596290adc69768492fe09b059d46a0e2
   ## HEAD (no branch)
   1cc5990b q5z-r14-4b: the post-write verification refusal gets the same rule, found by replaying the E-CALLER shape live (ALIAS-028, ALIAS-059)
   52123fa9 q5z-r14-4: the diagnostic baseline reads its analyzer's answer whole, and a baseline refusal names the one change that works (ALIAS-028, ALIAS-059)
   4cc96eab q5z-r14-red-4: the verify:fast baseline reads a truncated EDN document and calls it invalid output, then prescribes the request that just failed (ALIAS-028, ALIAS-059)
   67526f64 q5z-r14-3: the router is read as the entrance's own slice, and a kind minted inside a non-literal value is read with the reader (ALIAS-059)
   bde96387 q5z-r14-red-3: the enumeration holds out a file instead of constructing a reachable set, and misses two kinds minted inside a non-literal value (ALIAS-059)
   c5e63e69 q5z-r14-2: the fact renderer prints through a writer that stops at the ceiling, so an endless, deep or huge value costs bounded work (ALIAS-059)
   6cbcbd48 q5z-r14-red-2: the fact renderer bounds its output and not its work, and the witness that should have seen it filtered by the shape it polices (ALIAS-059)
   524dd21d q5z-r14-1: the root listing's drop marker is charged against the same 512-character ceiling, measured on the rendered listing (ALIAS-058, ALIAS-059)
   8fa9a0fa q5z-r14-red-1: the root-listing marker is appended after the 512-character budget, so the item announcing the bound is the one that breaks it (ALIAS-058, ALIAS-059)
    .../alias-migration/alias-migration-specs.md       |   4 +-
    src/clj_surgeon/alias_migration.clj                |  16 +
    src/clj_surgeon/mcp_alias_migration.clj            |  83 ++-
    src/clj_surgeon/mcp_change_buffer.clj              |  56 +-
    src/clj_surgeon/mcp_cold_verify.clj                |   3 +
    src/clj_surgeon/mcp_tool.clj                       |  77 ++-
    test/clj_surgeon/mcp_alias_migration_test.clj      | 639 ++++++++++++++++++++-
    7 files changed, 839 insertions(+), 39 deletions(-)
   ```

   Exact RED→GREEN command (each invocation prints its complete final counter line):

   ```text
   $ run_hist() { label=$1; dir=$2; shift 2; echo "HIST=$label HEAD=$(git -C "$dir" rev-parse --short=8 HEAD)"; out=$(cd "$dir" && ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -M:clj-surgeon/mcp-test); exec java -cp "$cp" clojure.main /home/forge/tmp/q5z14-review-fx/test_vars.clj "$@"' bash "$@" 2>&1); rc=$?; printf '%s\n' "$out" | tail -n 1; echo "EXIT=$rc"; }; run_hist RED-1 /home/forge/tmp/q5z14-review-fx/hist/8fa9a0fa the-root-listing-charges-its-own-marker-against-its-own-ceiling; run_hist GREEN-1 /home/forge/tmp/q5z14-review-fx/hist/524dd21d the-root-listing-charges-its-own-marker-against-its-own-ceiling; run_hist RED-2 /home/forge/tmp/q5z14-review-fx/pre-bounded the-fact-renderer-costs-bounded-work-whatever-the-value the-fact-renderer-survives-a-deeply-nested-value the-fact-renderer-does-not-read-a-huge-value-whole; run_hist GREEN-2 /home/forge/tmp/q5z14-review-fx/hist/c5e63e69 the-fact-renderer-costs-bounded-work-whatever-the-value the-fact-renderer-survives-a-deeply-nested-value the-fact-renderer-does-not-read-a-huge-value-whole; run_hist RED-3 /home/forge/tmp/q5z14-review-fx/hist/bde96387 the-enumeration-reaches-the-routers-entrance-slice-and-every-spelling; run_hist GREEN-3 /home/forge/tmp/q5z14-review-fx/hist/67526f64 the-enumeration-reaches-the-routers-entrance-slice-and-every-spelling; run_hist RED-4 /home/forge/tmp/q5z14-review-fx/hist/4cc96eab a-diagnostic-baseline-parses-output-larger-than-the-visible-budget a-baseline-refusal-never-prescribes-re-sending-the-same-request; run_hist GREEN-4 /home/forge/tmp/q5z14-review-fx/hist/52123fa9 a-diagnostic-baseline-parses-output-larger-than-the-visible-budget a-baseline-refusal-never-prescribes-re-sending-the-same-request
   ```

   Verbatim output:

   ```text
   HIST=RED-1 HEAD=8fa9a0fa
   {:test 1, :pass 4, :fail 4, :error 0}
   EXIT=1
   HIST=GREEN-1 HEAD=524dd21d
   {:test 1, :pass 8, :fail 0, :error 0}
   EXIT=0
   HIST=RED-2 HEAD=6cbcbd48
   {:test 3, :pass 2, :fail 1, :error 2}
   EXIT=1
   HIST=GREEN-2 HEAD=c5e63e69
   {:test 3, :pass 10, :fail 0, :error 0}
   EXIT=0
   HIST=RED-3 HEAD=bde96387
   {:test 1, :pass 5, :fail 3, :error 0}
   EXIT=1
   HIST=GREEN-3 HEAD=67526f64
   {:test 1, :pass 8, :fail 0, :error 0}
   EXIT=0
   HIST=RED-4 HEAD=4cc96eab
   {:test 2, :pass 2, :fail 4, :error 0}
   EXIT=1
   HIST=GREEN-4 HEAD=52123fa9
   {:test 2, :pass 6, :fail 0, :error 0}
   EXIT=0
   ```

   Exact current gate commands and verbatim terminal output:

   ```text
   $ ~/bin/suite-run bb test/run_all.clj; rc=$?; echo EXIT=$rc
   Ran 737 tests containing 6275 assertions.
   0 failures, 0 errors.
   EXIT=0

   $ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; rc=$?; echo EXIT=$rc
   Ran 498 tests containing 7008 assertions.
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

## GO-WITH-FIX

No: this tip is not GO on its own for MCP/main until findings 1 and 2 are fixed and witnessed.
