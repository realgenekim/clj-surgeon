## NO-GO

1. **Checkout identity and cleanliness — PASS.** `/home/forge/tmp/sol/ft1-wt:HEAD`

   Exact command:

   ```text
   pwd && git status --short --branch && git rev-parse HEAD && git rev-parse --verify bb3b6360^{commit}
   ```

   Verbatim output:

   ```text
   /home/forge/tmp/sol/ft1-wt
   ## HEAD (no branch)
   bb3b63605605778ec545b0c126371dd505ddf4ab
   bb3b63605605778ec545b0c126371dd505ddf4ab
   ```

2. **BLOCKING — a conventions file may name and publish paths outside the workspace instead of receiving a typed refusal.** `src/clj_surgeon/mcp_feature_thread.clj:841` validates only that each glob is a string; `src/clj_surgeon/mcp_feature_thread.clj:853` accepts that convention set, and `src/clj_surgeon/mcp_feature_thread.clj:996` renders the caller-controlled glob into the receipt. The bounded walk at `src/clj_surgeon/mcp_feature_thread.clj:299` did not read the external files, but the review contract explicitly makes either a non-refusal or an out-of-root published path blocking. The conventions file used here is `/var/tmp/forge/ft5-review-fx/outsidecfg/.clj-surgeon/feature-thread.edn`; its five globs are `/etc/passwd` or `../outside/*.clj`.

   Exact command:

   ```text
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj text '{:subject "safeSubject" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/outsidecfg"}}' | sed -n '1,18p'
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   receipt feature-thread/v2  subject=safeSubject  root=/var/tmp/forge/ft5-review-fx/outsidecfg  repo=outside-path-attack  text=2945B (budget 28672B)  structured=3766B (trunk cap 32640B)  total=6711B  status=INCOMPLETE (0 of 5) — legs, not bytes
   leg menu  ABSENT cause=searched-and-absent
     searched: identifier-or-route: rg -n -e '\QsafeSubject\E' -g '/etc/passwd'
   leg js  ABSENT cause=searched-and-absent
     searched: definition-shaped: rg -n -e '\(defn?-? +(?:\QsafeSubject\E)\b|(?:async +)?function +(?:\QsafeSubject\E)\b|(?:const|let|var) +(?:\QsafeSubject\E)\s*=|(?:window|globalThis)\.(?:\QsafeSubject\E)\s*=|\b(?:\QsafeSubject\E)\s*[:=]\s*(?:async\s*)?(?:function|\()' -g '../outside/*.clj'
     searched: identifier: rg -n -e '\QsafeSubject\E' -g '../outside/*.clj'
   leg route  ABSENT cause=no-seed-of-this-leg-kind
     reason: this leg searches for a route and the request named none
     remedy: Pass the route as `subject` or in `also`. It is still counted as missing: the verb cannot tell an unnamed one from an absent one.
   leg handler  ABSENT cause=searched-and-absent
     searched: identifier-or-route: rg -n -e '\QsafeSubject\E' -g '../outside/*.clj'
   leg tests  ABSENT cause=searched-and-absent
     searched: identifier-route-or-tail: rg -n -e '\QsafeSubject\E' -g '/etc/passwd'
   leg implementation  N/A · implementation: n/a (no seed names a definition) — not counted in the leg status
     searched: definition-shaped: rg -n -e '\(defn?-? +(?:\QsafeSubject\E)\b|(?:async +)?function +(?:\QsafeSubject\E)\b|(?:const|let|var) +(?:\QsafeSubject\E)\s*=|(?:window|globalThis)\.(?:\QsafeSubject\E)\s*=|\b(?:\QsafeSubject\E)\s*[:=]\s*(?:async\s*)?(?:function|\()' -g 'src/**/*.clj' -g 'src/**/*.cljc'
     searched: identifier: rg -n -e '\QsafeSubject\E' -g 'src/**/*.clj' -g 'src/**/*.cljc'
   sibling ABSENT rule=adjacent-route-entry reason=no sibling resolved by rule adjacent-route-entry; pass mirror to name one explicitly
   rules durable_path=[] refusal_statuses=[] intents=[]
   ```

   Required fix: reject absolute globs and any glob with a `..` path segment during convention admission, before the workspace walk, with a stable typed refusal naming the offending field and without echoing an out-of-root path.

3. **BLOCKING — the real-repository `saveDraft` recall is falsely COMPLETE because the route leg is the handler’s docstring, not the route entry.** `src/clj_surgeon/mcp_feature_thread.clj:1556` turns the first literal hit into the primary route member without checking that it is a route-table entry. On the required read-only clone at `2df99c989e2dc1963161c13f7a341847c16b4deb`, the live receipt labels `src/writer/routes.clj:L392-L445` `route-literal`; that form is `(defn handle-save ...)`, whose docstring merely says `POST /api/save`. The actual route entry is `src/writer/routes.clj:L2121`. This directly meets the brief’s blocking rule: `COMPLETE (5 of 5)` with a wrong leg.

   Exact command:

   ```text
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj recall-one '{:root "/home/forge/tmp/replay/smw-base" :subject "saveDraft" :also ["/api/save"]}' && sed -n '392,398p' /home/forge/tmp/replay/smw-base/src/writer/routes.clj && sed -n '2119,2123p' /home/forge/tmp/replay/smw-base/src/writer/routes.clj
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   status= COMPLETE (5 of 5)
   route= {:status "FOUND", :file "src/writer/routes.clj", :from 392, :to 445, :evidence "route-literal", :boundary "form(parsed)", :sha256 "efcb360969282e0bacb7b80539c46b6a7d84fd8c093ab4f5ffc7af93830f3296"}
   (defn handle-save
     "POST /api/save — one compound command carrying the visible editor snapshot.
      Sync validation, state commit, timestamped archive, book-node settle, and
      durable acknowledgement are serialized. A stale snapshot returns 409 and
      performs NO save. An explicit force-conflict choice may keep browser text,
      but only for the same editor identity and displayed server revision."
     [request]
      ["/api/clear" {:post {:handler #'nav-handlers/handle-clear}}]
      ["/api/clear-all" {:post {:handler #'nav-handlers/handle-clear-all}}]
      ["/api/save" {:post {:handler #'handle-save}}]
      ["/api/draft/accept-server" {:post {:handler #'handle-accept-server-draft}}]
      ["/api/save-draft" {:post {:handler #'handle-save-draft}}]
   ```

   Required fix: a `:route` FOUND must structurally prove the hit is a route entry (and select that member/range), not merely an occurrence of the route literal anywhere in a routes-globbed file. Add the real `saveDraft` recall as a witness asserting route `L2121`, then sabotage it with the docstring occurrence retained.

4. **The seven-tool catalog and the named `formatDraft` six-leg receipt reproduce; every delivered body, range digest, anchor context, and whole-file admission digest is exact.** `src/clj_surgeon/mcp_feature_thread.clj:1414`, `src/clj_surgeon/mcp_feature_thread.clj:1556`, `src/clj_surgeon/mcp_feature_thread.clj:2011`, `src/clj_surgeon/mcp_feature_thread.clj:2071`, `src/clj_surgeon/mcp_feature_thread.clj:2265`, `src/clj_surgeon/mcp_feature_thread.clj:2609`.

   Exact command:

   ```text
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj list && clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj t1 /home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   count= 7
   tools= ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure" "alias_migration" "admit_clojure_patch" "feature_thread"]
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   status= COMPLETE (6 of 6) text_actual= 27246 text_declared= 27246 structured= 30713 receipt= 57959
   menu-caller FOUND src/writer/views/components.clj L102-L113 body= true sha= true text= true after= true anchor= "after:L113 in-form:L92-L165"
   js-function FOUND resources/public/js/editor-commands.js L389-L454 body= true sha= true text= true after= true anchor= "after:L454"
   route FOUND src/writer/routes.clj L2148-L2148 body= true sha= true text= true after= true anchor= "after:L2148 in-form:L2083-L2376"
   handler FOUND src/writer/handlers/transform.clj L606-L680 body= true sha= true text= true after= true anchor= "after:L680"
   tests FOUND test/writer/handlers/transform_apply_test.clj L349-L384 body= true sha= true text= true after= true anchor= "after:L384"
    co-primary js test/js/browser_runtime_classic_script_test.js L63-L94 body= true sha= true
   implementation FOUND src/writer/handlers/transform.clj L81-L132 body= true sha= true text= true after= true anchor= "after:L132"
   peers= [{:identifier "openTransformFromSelection", :status "FOUND", :file "resources/public/js/editor-commands.js", :from 332, :to 344, :evidence "co-menu-item"} {:identifier "expound", :status "ABSENT"} {:identifier "bulletize", :status "ABSENT"}]
   export= none (classic script; functions are globals)
   governance= [{:line 240, :anchor "unparsed"} {:line 268, :anchor "unparsed"} {:line 270, :form_start 270, :form_end 299, :anchor "after:L299"} {:line 299, :form_start 270, :form_end 299, :anchor "after:L299"} {:line 311, :form_start 301, :form_end 330, :anchor "after:L330"} {:line 382, :form_start 382, :form_end 400, :anchor "after:L400"}]
   request_contract= {:route "/api/transform/format", :handler_reads ["sync"], :js_posts ["sync"], :agree? true, :only_in_js [], :only_in_handler []}
   verify= [{:target "runtests-once", :line 279, :command "clojure -M:test:run-tests unit fast 2>&1 | tee 00TESTLOG.txt", :for "test/writer/handlers/transform_apply_test.clj", :evidence "alias"} {:target "runtests-unit", :line 283, :command "clojure -M:test:run-tests unit", :for "test/writer/handlers/transform_apply_test.clj", :evidence "alias"} {:target "runtests-integration", :line 287, :command "clojure -M:test:run-tests integration 2>&1 | tee 00TESTLOG-integration.txt", :for "test/writer/handlers/transform_apply_test.clj", :evidence "alias"} {:target "runtests-component", :line 291, :command "clojure -M:test:run-tests component 2>&1 | tee 00TESTLOG-component.txt", :for "test/writer/handlers/transform_apply_test.clj", :evidence "alias"} {:target "test-js", :line 233, :command "node --test test/js/browser_runtime_classic_script_test.js", :for "test/js/browser_runtime_classic_script_test.js", :evidence "names-the-file", :make_prefix "@"}]
   by_leg= (:menu-caller :js-function :route :handler :tests :implementation)
   whole_file_hashes= true
   ```

   The `rules.assert` is now honest: the range hashes are advisory, this read-only verb enforces nothing, and the separate `admit_clojure_patch` call binds the selectable whole-file digest subset. A printed instruction alone is not a control; the emitted `next_call.by_leg` plus the admission verb is the control, and `sha256sum` independently matched all six files.

5. **Round-three blockers B3, B1′, and B2′ are closed under live attack.** The automatic-leg globs are unioned before the walk at `src/clj_surgeon/mcp_feature_thread.clj:3225`; the lexer’s regex-character-class state is at `src/clj_surgeon/mcp_feature_thread.clj:474`; and whole-file comment/discard classification is at `src/clj_surgeon/mcp_feature_thread.clj:1226`. Regex/division ambiguity either returned the true closing brace or the labelled `CANDIDATE` downgrade; no wrong range was labelled closed. CANDIDATE route/comment legs carried no anchor and did not count toward COMPLETE.

   Exact commands:

   ```text
   rg -n "defn mechanical-format" /var/tmp/forge/ft5-review-fx/falsefx/src && clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj summary '{:subject "formatDraft" :also ["/api/transform/format" "mechanical-format"] :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/falsefx"}}' | rg '^(isError|status=|leg |  body_|structured_leaf)'
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj lex '{:root "/var/tmp/forge/ft5-review-fx/jsfx" :subjects ["returnRegex" "typeofRegex" "caseRegex" "yieldRegex" "divisionAssign" "divisionPostfix" "divisionParen" "divisionBracket" "asiRegex" "slashSlashRegex" "classSlash" "classNegSlash" "classEscapedBracket" "nestedTemplate" "blockMarkerString" "unterminatedComment"]}'
   wc -l /var/tmp/forge/ft5-review-fx/jsfx/resources/public/js/long.js && clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj lex '{:root "/var/tmp/forge/ft5-review-fx/jsfx" :subjects ["overCeiling"]}'
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj summary '{:subject "formatDraft" :also ["/api/transform/format" "mechanical-format"] :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/commentfx"}}' | rg '^(isError|status=|leg (menu-caller|js-function|route|handler|tests|implementation)|structured_leaf)'
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj summary '{:subject "formatDraft" :also ["/api/transform/format" "mechanical-format"] :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/routefx"}}' | rg '^(isError|status=|leg (route|handler)|structured_leaf)'
   ```

   Verbatim outputs (in the same order):

   ```text
   /var/tmp/forge/ft5-review-fx/falsefx/src/writer/other/dup.clj:4:(defn mechanical-format
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   isError= false error_type= nil
   status= COMPLETE (6 of 6)
   leg menu-caller FOUND src/writer/views/components.clj 102 113 evidence= identifier-or-route boundary= form(parsed, member of L92-L165 top-tabs) anchor= "after:L113 in-form:L92-L165"
     body_bytes= 707 body_eq_slice= true sha_eq_slice= true body_in_text= true after_eq_slice= true
   leg js-function FOUND resources/public/js/editor-commands.js 389 454 evidence= identifier(def) boundary= brace-window(lexed,closed) anchor= "after:L454"
     body_bytes= 2973 body_eq_slice= true sha_eq_slice= true body_in_text= true after_eq_slice= true
   leg route FOUND src/writer/routes.clj 2148 2148 evidence= route-literal boundary= form(parsed, member of L2083-L2376 make-routes) anchor= "after:L2148 in-form:L2083-L2376"
     body_bytes= 73 body_eq_slice= true sha_eq_slice= true body_in_text= true after_eq_slice= true
   leg handler FOUND src/writer/handlers/transform.clj 553 627 evidence= handler-join boundary= form(parsed) anchor= "after:L627"
     body_bytes= 4049 body_eq_slice= true sha_eq_slice= true body_in_text= true after_eq_slice= true
   leg tests FOUND test/writer/handlers/transform_apply_test.clj 349 384 evidence= form(deftest,CALLS-handle-format) boundary= form(parsed) anchor= "after:L384"
     body_bytes= 1997 body_eq_slice= true sha_eq_slice= true body_in_text= true after_eq_slice= true
   leg implementation FOUND src/writer/other/dup.clj 4 45 evidence= identifier(def) boundary= form(parsed) anchor= "after:L45"
     body_bytes= 2640 body_eq_slice= true sha_eq_slice= true body_in_text= true after_eq_slice= true
   structured_leaf_count= 337 missing_leaf_count= 0 missing= ()
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   returnRegex status= FOUND range= 1-3 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L3"
   typeofRegex status= FOUND range= 5-7 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L7"
   caseRegex status= FOUND range= 9-11 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L11"
   yieldRegex status= FOUND range= 13-16 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L16"
   divisionAssign status= FOUND range= 18-21 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L21"
   divisionPostfix status= CANDIDATE range= 1-63 boundary= line-window(+/-40, unclosed at L23) body_eq_slice= true body_in_text= true anchor= nil
   divisionParen status= FOUND range= 28-31 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L31"
   divisionBracket status= FOUND range= 33-36 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L36"
   asiRegex status= FOUND range= 38-41 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L41"
   slashSlashRegex status= FOUND range= 43-45 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L45"
   classSlash status= FOUND range= 47-49 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L49"
   classNegSlash status= FOUND range= 51-53 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L53"
   classEscapedBracket status= FOUND range= 55-57 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L57"
   nestedTemplate status= FOUND range= 59-61 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L61"
   blockMarkerString status= FOUND range= 63-65 boundary= brace-window(lexed,closed) body_eq_slice= true body_in_text= true anchor= "after:L65"
   unterminatedComment status= CANDIDATE range= 27-72 boundary= line-window(+/-40, unclosed at L67) body_eq_slice= true body_in_text= true anchor= nil
   ```

   ```text
   409 /var/tmp/forge/ft5-review-fx/jsfx/resources/public/js/long.js
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   overCeiling status= CANDIDATE range= 1-41 boundary= line-window(+/-40, unclosed at L1) body_eq_slice= true body_in_text= true anchor= nil
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   isError= false error_type= nil
   status= INCOMPLETE (5 of 6)
   leg menu-caller FOUND src/writer/views/components.clj 102 113 evidence= identifier-or-route boundary= form(parsed, member of L92-L165 top-tabs) anchor= "after:L113 in-form:L92-L165"
   leg js-function FOUND resources/public/js/editor-commands.js 389 454 evidence= identifier(def) boundary= brace-window(lexed,closed) anchor= "after:L454"
   leg route FOUND src/writer/routes.clj 2148 2148 evidence= route-literal boundary= form(parsed, member of L2083-L2376 make-routes) anchor= "after:L2148 in-form:L2083-L2376"
   leg handler CANDIDATE src/writer/handlers/transform.clj 842 844 evidence= handler-join boundary= form(parsed) anchor= nil
   leg tests FOUND test/js/browser_runtime_classic_script_test.js 63 94 evidence= test(js) boundary= brace-window(lexed,closed), test-call at L63 anchor= "after:L94"
   leg implementation FOUND src/writer/handlers/transform.clj 81 132 evidence= identifier(def) boundary= form(parsed) anchor= "after:L132"
   structured_leaf_count= 319 missing_leaf_count= 1 missing= ("  (let [before \"authoritative current prose\"]")
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   isError= false error_type= nil
   status= INCOMPLETE (4 of 5)
   leg route CANDIDATE src/writer/routes.clj 2139 2163 evidence= route-assembled boundary= line-window(in oversized form L2083-L2377, ceiling 200 lines) anchor= nil
   leg handler FOUND src/writer/handlers/transform.clj 81 132 evidence= identifier-or-route boundary= form(parsed) anchor= "after:L132"
   structured_leaf_count= 310 missing_leaf_count= 1 missing= ("  (let [before \"authoritative current prose\"]")
   ```

   The `missing_leaf_count=1` lines are a limitation of this review helper’s raw-substring check: that co-primary `after_context` leaf is present in the receipt’s `structured-only` row using EDN escaping. It is not a missing or altered body; the direct body and SHA checks above are authoritative.

6. **Budget admission, self-counting, and edit-aware elision reproduce; the structured-cap explanation is visible but its refetch advice is misleading at the hard cap.** `src/clj_surgeon/mcp_feature_thread.clj:41`, `src/clj_surgeon/mcp_feature_thread.clj:74`, `src/clj_surgeon/mcp_feature_thread.clj:79`, `src/clj_surgeon/mcp_feature_thread.clj:207`, `src/clj_surgeon/mcp_feature_thread.clj:2609`, `src/clj_surgeon/mcp_feature_thread.clj:2976`. At ten requested budgets, every successful receipt’s declared `text_bytes` equals delivered bytes and stays within the caller’s text budget; 10,240 and 1 byte refuse and quote the caller’s number; a string and 40,000 refuse by type/cap. The default is 28,672 and the ranges-only floor is 11,264. The max-budget edit-basis call below has `text=28141 < 32768` yet cuts `sibling` and `peers` solely to satisfy the structured cap, labels the reason only `public-budget`, and tells the caller to “re-run ... with a larger budget_bytes” even though 32,768 is the hard maximum. This is non-blocking because the cuts are explicit and no counted leg is missing, but the remedy should name `mode=locations` or the structured cap rather than an impossible larger budget.

   Exact commands:

   ```text
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj budgets '{:root "/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote" :budgets [:default 32768 28672 24576 20000 11264 10240 1 "lots" 40000]}'
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj summary '{:subject "dequoteFormatSelection" :also ["/api/transform/format" "mechanical-format"] :mirror "formatDraft" :budget_bytes 32768 :scope {:workspace_root "/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote-after"}}' | rg '^(isError|status=|bytes actual|elided=|leg |structured_leaf)'
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   budget= :default isError= false error= nil status= COMPLETE (6 of 6) actual= 27246 declared= 27246 structured= 30713 receipt= 57959 quoted_budget= 28672 would_be= nil cuts= []
     header= receipt feature-thread/v2  subject=formatDraft also=/api/transform/format,mechanical-format  root=/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote  repo=social-media-writer  text=27246B (budget 28672B)  structured=30713B (trunk cap 32640B)  total=57959B  status=COMPLETE (6 of 6) — legs, not bytes
   budget= 32768 isError= false error= nil status= COMPLETE (6 of 6) actual= 27246 declared= 27246 structured= 30713 receipt= 57959 quoted_budget= 32768 would_be= nil cuts= []
     header= receipt feature-thread/v2  subject=formatDraft also=/api/transform/format,mechanical-format  root=/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote  repo=social-media-writer  text=27246B (budget 32768B)  structured=30713B (trunk cap 32640B)  total=57959B  status=COMPLETE (6 of 6) — legs, not bytes
   budget= 28672 isError= false error= nil status= COMPLETE (6 of 6) actual= 27246 declared= 27246 structured= 30713 receipt= 57959 quoted_budget= 28672 would_be= nil cuts= []
     header= receipt feature-thread/v2  subject=formatDraft also=/api/transform/format,mechanical-format  root=/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote  repo=social-media-writer  text=27246B (budget 28672B)  structured=30713B (trunk cap 32640B)  total=57959B  status=COMPLETE (6 of 6) — legs, not bytes
   budget= 24576 isError= false error= nil status= COMPLETE (6 of 6) actual= 24017 declared= 24017 structured= 26679 receipt= 50696 quoted_budget= 24576 would_be= nil cuts= ["sibling" "peers" "after-context" "governance-template" "peer-rows"]
     header= receipt feature-thread/v2  subject=formatDraft also=/api/transform/format,mechanical-format  root=/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote  repo=social-media-writer  text=24017B (budget 24576B)  structured=26679B (trunk cap 32640B)  total=50696B  status=COMPLETE (6 of 6) — legs, not bytes
   budget= 20000 isError= false error= nil status= COMPLETE (6 of 6) actual= 19822 declared= 19822 structured= 21876 receipt= 41698 quoted_budget= 20000 would_be= nil cuts= ["sibling" "peers" "after-context" "governance-template" "peer-rows" "next-call" "menu-caller" "route" "tests(js)" "tests"]
     header= receipt feature-thread/v2  subject=formatDraft also=/api/transform/format,mechanical-format  root=/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote  repo=social-media-writer  text=19822B (budget 20000B)  structured=21876B (trunk cap 32640B)  total=41698B  status=COMPLETE (6 of 6) — legs, not bytes
   budget= 11264 isError= false error= nil status= COMPLETE (6 of 6) actual= 10410 declared= 10410 structured= 12132 receipt= 22542 quoted_budget= 11264 would_be= nil cuts= ["sibling" "peers" "after-context" "governance-template" "peer-rows" "next-call" "menu-caller" "route" "tests(js)" "tests" "implementation" "js-function" "handler"]
     header= receipt feature-thread/v2  subject=formatDraft also=/api/transform/format,mechanical-format  root=/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote  repo=social-media-writer  text=10410B (budget 11264B)  structured=12132B (trunk cap 32640B)  total=22542B  status=COMPLETE (6 of 6) — legs, not bytes
   budget= 10240 isError= true error= feature-thread-budget-exceeded status= COMPLETE (6 of 6) actual= 526 declared= nil structured= nil receipt= nil quoted_budget= 10240 would_be= 10410 cuts= []
     header= feature_thread refused · feature-thread-budget-exceeded
   budget= 1 isError= true error= feature-thread-budget-exceeded status= COMPLETE (6 of 6) actual= 518 declared= nil structured= nil receipt= nil quoted_budget= 1 would_be= 10406 cuts= []
     header= feature_thread refused · feature-thread-budget-exceeded
   budget= lots isError= true error= feature-thread-invalid-budget status= nil actual= 367 declared= nil structured= nil receipt= nil quoted_budget= nil would_be= nil cuts= []
     header= feature_thread refused · feature-thread-invalid-budget
   budget= 40000 isError= true error= feature-thread-budget-above-cap status= nil actual= 331 declared= nil structured= nil receipt= nil quoted_budget= 40000 would_be= nil cuts= []
     header= feature_thread refused · feature-thread-budget-above-cap
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   isError= false error_type= nil
   status= COMPLETE (6 of 6)
   bytes actual/text/structured/receipt= 28141 28141 31462 59603 budget= 32768 would_be= nil
   elided= [{:leg "sibling", :bytes 0, :reason "public-budget", :from 0, :to 0, :sha256 "n/a", :refetch "feature_thread subject=formatDraft"} {:leg "peers", :bytes 0, :reason "public-budget; 2 co-menu-item peer bodies dropped", :from 0, :to 0, :sha256 "n/a", :refetch "re-run feature_thread with a larger budget_bytes"}]
   leg menu-caller FOUND src/writer/views/components.clj 102 115 evidence= identifier-or-route boundary= form(parsed, member of L92-L167 top-tabs) anchor= "after:L115 in-form:L92-L167" also= []
   leg js-function FOUND resources/public/js/editor-commands.js 459 523 evidence= identifier(def) boundary= brace-window(lexed,closed) anchor= "after:L523" also= []
   leg route FOUND src/writer/routes.clj 2148 2148 evidence= route-literal boundary= form(parsed, member of L2083-L2376 make-routes) anchor= "after:L2148 in-form:L2083-L2376" also= []
   leg handler FOUND src/writer/handlers/transform.clj 622 696 evidence= handler-join boundary= form(parsed) anchor= "after:L696" also= []
   leg tests FOUND test/writer/handlers/transform_apply_test.clj 349 384 evidence= form(deftest,CALLS-handle-format) boundary= form(parsed) anchor= "after:L384" also= [{:file "test/writer/handlers/transform_apply_test.clj", :from 386, :to 424, :evidence "form(deftest,CALLS-handle-format)"} {:file "test/js/browser_runtime_classic_script_test.js", :from 152, :to 176, :evidence "test(js)"}]
   leg implementation FOUND src/writer/handlers/transform.clj 81 132 evidence= identifier(def) boundary= form(parsed) anchor= "after:L132" also= [{:file "src/writer/handlers/transform.clj", :from 134, :to 148, :evidence "identifier(def)"}]
   structured_leaf_count= 333 missing_leaf_count= 0 missing= ()
   ```

7. **Unreadable, symlink, missing-root, malformed-config, and seed-ceiling attacks are bounded and typed, except for the out-of-root-glob blocker in finding 2.** `src/clj_surgeon/mcp_feature_thread.clj:84`, `src/clj_surgeon/mcp_feature_thread.clj:299`, `src/clj_surgeon/mcp_feature_thread.clj:853`, `src/clj_surgeon/mcp_feature_thread.clj:3071`. A chmod-000 handler yields exactly one absent leg, `INCOMPLETE (4 of 5)`, and never prints `COMPLETE (5 of 5)`. An external-file symlink produces no source/body/path canary. The 10,001-character subject and 33-item `also` vector refuse at the advertised 512/32 ceilings before scanning.

   Exact command:

   ```text
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj attacks '[{:label "unreadable" :args {:subject "formatDraft" :also ["/api/transform/format" "mechanical-format"] :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/unreadablefx"}} :canaries []} {:label "symlink-outside" :args {:subject "OUTSIDE_CANARY_FT5" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/pathfx"}} :canaries ["outside.secret" "\"formatDraft mechanical-format\"" "/var/tmp/forge/ft5-review-fx/outside"]} {:label "bad-root" :args {:subject "formatDraft" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/does-not-exist"}} :canaries []} {:label "long-subject" :long-subject 10001 :args {:subject "x" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/pathfx"}} :canaries []} {:label "also-33" :also-count 33 :args {:subject "formatDraft" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/pathfx"}} :canaries []} {:label "malformed-config" :args {:subject "formatDraft" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/malformed"}} :canaries []}]' | rg '^(unreadable|symlink-outside|bad-root|long-subject|also-33|malformed-config|  legs=|  canaries=)'
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   unreadable isError= false error= nil status= INCOMPLETE (4 of 5) text_bytes= 18578 complete5= false
     legs= [{:id "menu-caller", :status "FOUND", :file "src/writer/views/components.clj", :anchor "after:L113 in-form:L92-L165"} {:id "js-function", :status "FOUND", :file "resources/public/js/editor-commands.js", :anchor "after:L454"} {:id "route", :status "FOUND", :file "src/writer/routes.clj", :anchor "after:L2148 in-form:L2083-L2376"} {:id "handler", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "tests", :status "FOUND", :file "test/writer/handlers/transform_apply_test.clj", :anchor "after:L384"} {:id "implementation", :status "N/A", :reason "the definition of formatDraft is already a leg of this receipt (resources/public/js/editor-commands.js:L389-L454)"}]
     canaries= {}
   symlink-outside isError= false error= nil status= INCOMPLETE (0 of 5) text_bytes= 3727 complete5= false
     legs= [{:id "menu-caller", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "js-function", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "route", :status "ABSENT", :absent_cause "no-seed-of-this-leg-kind", :reason "this leg searches for a route and the request named none", :remedy "Pass the route as `subject` or in `also`. It is still counted as missing: the verb cannot tell an unnamed one from an absent one."} {:id "handler", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "tests", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "implementation", :status "N/A", :reason "no seed names a definition"}]
     canaries= {"outside.secret" false, "\"formatDraft mechanical-format\"" false, "/var/tmp/forge/ft5-review-fx/outside" false}
   bad-root isError= true error= invalid-workspace-root status= nil text_bytes= 350 complete5= false
     legs= []
     canaries= {}
   long-subject isError= true error= feature-thread-subject-too-long status= nil text_bytes= 331 complete5= false
     legs= []
     canaries= {}
   also-33 isError= true error= feature-thread-also-too-many status= nil text_bytes= 289 complete5= false
     legs= []
     canaries= {}
   malformed-config isError= true error= feature-thread-conventions-invalid status= nil text_bytes= 384 complete5= false
     legs= []
     canaries= {}
   ```

8. **The real-repository recall otherwise matches the builder’s table, and the implementation-leg fixes hold.** `src/clj_surgeon/mcp_feature_thread.clj:2164`, `src/clj_surgeon/mcp_feature_thread.clj:2222`, `src/clj_surgeon/mcp_feature_thread.clj:2341`. The required clone is clean at `2df99c989e2dc1963161c13f7a341847c16b4deb`; `formatDraft` is 6/6, `openTransformFromSelection` is 5/5, and `expound` is honestly 2/5 with a searched-and-absent route. `saveDraft`’s aggregate count matches the claim but is invalid for finding 3’s reason. A test-file stub is not selected as implementation, and two definitions in two source files are both named.

   Exact commands:

   ```text
   git -C /home/forge/tmp/replay/smw-base rev-parse HEAD && git -C /home/forge/tmp/replay/smw-base status --short --branch
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj recall /home/forge/tmp/replay/smw-base
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj summary '{:subject "formatDraft" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/testimpl"}}' | rg '^(status=|leg (tests|implementation|route|handler))'
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj summary '{:subject "formatDraft" :also ["/api/transform/format" "mechanical-format"] :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/twodef"}}' | rg '^(status=|leg implementation)'
   ```

   Verbatim output:

   ```text
   2df99c989e2dc1963161c13f7a341847c16b4deb
   ## HEAD (no branch)
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   formatDraft status= COMPLETE (6 of 6) legs= [{:id "menu-caller", :status "FOUND", :file "src/writer/views/components.clj", :from 102, :to 113, :evidence "identifier-or-route"} {:id "js-function", :status "FOUND", :file "resources/public/js/editor-commands.js", :from 389, :to 454, :evidence "identifier(def)"} {:id "route", :status "FOUND", :file "src/writer/routes.clj", :from 2148, :to 2148, :evidence "route-literal"} {:id "handler", :status "FOUND", :file "src/writer/handlers/transform.clj", :from 606, :to 680, :evidence "handler-join"} {:id "tests", :status "FOUND", :file "test/writer/handlers/transform_apply_test.clj", :from 349, :to 384, :evidence "form(deftest,CALLS-handle-format)"} {:id "implementation", :status "FOUND", :file "src/writer/handlers/transform.clj", :from 81, :to 132, :evidence "identifier(def)"}]
   saveDraft status= COMPLETE (5 of 5) legs= [{:id "menu-caller", :status "FOUND", :file "src/writer/views/components.clj", :from 98, :to 101, :evidence "identifier-or-route"} {:id "js-function", :status "FOUND", :file "resources/public/js/editor-controller.js", :from 505, :to 571, :evidence "identifier(def)"} {:id "route", :status "FOUND", :file "src/writer/routes.clj", :from 392, :to 445, :evidence "route-literal"} {:id "handler", :status "FOUND", :file "src/writer/handlers/book_workshop.clj", :from 1922, :to 1985, :evidence "handler-join"} {:id "tests", :status "FOUND", :file "test/js/editor_conflict_quarantine_test.js", :from 185, :to 199, :evidence "test(js)"} {:id "implementation", :status "N/A"}]
   openTransformFromSelection status= COMPLETE (5 of 5) legs= [{:id "menu-caller", :status "FOUND", :file "src/writer/views/components.clj", :from 102, :to 113, :evidence "identifier-or-route"} {:id "js-function", :status "FOUND", :file "resources/public/js/editor-commands.js", :from 332, :to 344, :evidence "identifier(def)"} {:id "route", :status "FOUND", :file "src/writer/routes.clj", :from 2144, :to 2144, :evidence "route-literal"} {:id "handler", :status "FOUND", :file "src/writer/handlers/chat.clj", :from 324, :to 352, :evidence "handler-join"} {:id "tests", :status "FOUND", :file "test/writer/spa_lint_test.clj", :from 509, :to 521, :evidence "form(deftest,string-assert)"} {:id "implementation", :status "N/A"}]
   expound status= INCOMPLETE (2 of 5) legs= [{:id "menu-caller", :status "FOUND", :file "src/writer/views/components.clj", :from 102, :to 113, :evidence "identifier-or-route"} {:id "js-function", :status "FOUND", :file "resources/public/js/app-safe.js", :from 342, :to 368, :evidence "identifier(def)"} {:id "route", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "handler", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "tests", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "implementation", :status "N/A"}]
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   status= INCOMPLETE (3 of 5)
   leg route ABSENT nil nil nil evidence= nil boundary= nil anchor= nil also= []
   leg handler ABSENT nil nil nil evidence= nil boundary= nil anchor= nil also= []
   leg tests FOUND test/js/browser_runtime_classic_script_test.js 63 94 evidence= test(js) boundary= brace-window(lexed,closed), test-call at L63 anchor= "after:L94" also= [{:file "test/js/ghost_def_test.js", :from 1, :to 3, :evidence "identifier"}]
   leg implementation N/A nil nil nil evidence= nil boundary= nil anchor= nil also= []
   ```

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   status= COMPLETE (6 of 6)
   leg implementation FOUND src/writer/handlers/transform.clj 81 132 evidence= identifier(def) boundary= form(parsed) anchor= "after:L132" also= [{:file "src/writer/other/dup.clj", :from 4, :to 45, :evidence "identifier(def)"}]
   ```

9. **Non-blocking contract inconsistency: alias-only is ABSENT, not the addendum/THREAD-024’s CANDIDATE, and candidate-only string/comment hits make the automatic N/A row falsely say “the definition ... is already a leg.”** `docs/intent/feature-thread/feature-thread-specs.md:61` requires alias-only ABSENT while `docs/intent/feature-thread/feature-thread-specs.md:166` requires the same evidence to be CANDIDATE; the live implementation follows THREAD-007. `src/clj_surgeon/mcp_feature_thread.clj:2222` treats an excluded CANDIDATE range as if it proved a definition. These do not yield COMPLETE—the count remains 0/5—and no CANDIDATE carries an anchor, so they do not independently trigger this review’s blocking threshold. Reconcile the specs and change the N/A reason to “the only occurrence is already a CANDIDATE leg” unless a definition-shaped FOUND leg actually exists.

   Exact command:

   ```text
   clojure -M /var/tmp/forge/ft5-review-fx/live_client.clj attacks '[{:label "common-prefix" :args {:subject "formatDraftX" :scope {:workspace_root "/home/forge/tmp/sol/ft1-wt/test-fixtures/feature-thread/smw-dequote"}} :canaries []} {:label "string-only" :args {:subject "ghostOnly" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/jsfx"}} :canaries []} {:label "comment-only" :args {:subject "ghostComment" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/jsfx"}} :canaries []} {:label "alias-only" :args {:subject "aliasOnly" :scope {:workspace_root "/var/tmp/forge/ft5-review-fx/jsfx"}} :canaries []}]' | rg '^(common-prefix|string-only|comment-only|alias-only|  legs=)'
   ```

   Verbatim output:

   ```text
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   common-prefix isError= false error= nil status= INCOMPLETE (0 of 5) text_bytes= 3651 complete5= false
     legs= [{:id "menu-caller", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "js-function", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "route", :status "ABSENT", :absent_cause "no-seed-of-this-leg-kind", :reason "this leg searches for a route and the request named none", :remedy "Pass the route as `subject` or in `also`. It is still counted as missing: the verb cannot tell an unnamed one from an absent one."} {:id "handler", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "tests", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "implementation", :status "N/A", :reason "no seed names a definition"}]
   string-only isError= false error= nil status= INCOMPLETE (0 of 5) text_bytes= 4325 complete5= false
     legs= [{:id "menu-caller", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "js-function", :status "CANDIDATE", :file "resources/public/js/attacks.js", :evidence "identifier", :weak_reason "the boundary is not a parsed form or a closed brace window: line-window(+/-40, unclosed at L67)"} {:id "route", :status "ABSENT", :absent_cause "no-seed-of-this-leg-kind", :reason "this leg searches for a route and the request named none", :remedy "Pass the route as `subject` or in `also`. It is still counted as missing: the verb cannot tell an unnamed one from an absent one."} {:id "handler", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "tests", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "implementation", :status "N/A", :reason "the definition of ghostOnly is already a leg of this receipt (resources/public/js/attacks.js:L27-L76)"}]
   comment-only isError= false error= nil status= INCOMPLETE (0 of 5) text_bytes= 4545 complete5= false
     legs= [{:id "menu-caller", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "js-function", :status "CANDIDATE", :file "resources/public/js/attacks.js", :evidence "identifier", :weak_reason "the hit is a comment mention, not code"} {:id "route", :status "ABSENT", :absent_cause "no-seed-of-this-leg-kind", :reason "this leg searches for a route and the request named none", :remedy "Pass the route as `subject` or in `also`. It is still counted as missing: the verb cannot tell an unnamed one from an absent one."} {:id "handler", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "tests", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "implementation", :status "N/A", :reason "the definition of ghostComment is already a leg of this receipt (resources/public/js/attacks.js:L28-L76)"}]
   alias-only isError= false error= nil status= INCOMPLETE (0 of 5) text_bytes= 3183 complete5= false
     legs= [{:id "menu-caller", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "js-function", :status "ABSENT", :evidence "alias-only"} {:id "route", :status "ABSENT", :absent_cause "no-seed-of-this-leg-kind", :reason "this leg searches for a route and the request named none", :remedy "Pass the route as `subject` or in `also`. It is still counted as missing: the verb cannot tell an unnamed one from an absent one."} {:id "handler", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "tests", :status "ABSENT", :absent_cause "searched-and-absent"} {:id "implementation", :status "N/A", :reason "no seed names a definition"}]
   ```

10. **The round-five witness ratchets are real, but the builder's focused-count claim is stale.** `test/clj_surgeon/mcp_feature_thread_test.clj:1976` is the new two-face byte-identity witness and `src/clj_surgeon/mcp_feature_thread.clj:2557` is the sabotaged renderer. On a `git archive bb3b6360` export, changing only the text face to `(str/trim (:body leg))` produces four failures (two byte-identity assertions and two text-superset assertions), not the claimed two. The clean tip has 53 tests / 1372 assertions, not 52 / 1338. A broader archive sabotage replacing the feature-thread implementation with the B2' RED pre-image produced 100 failures, demonstrating the accumulated witness set is not decorative.

    Exact commands:

    ```text
    export FT5_CP="$(clojure -Spath -M:clj-surgeon/mcp-test)" && set -o pipefail && ~/bin/suite-run java -cp "$FT5_CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (System/exit (+ (:fail r) (:error r))))" 2>&1 | rg '^(FAIL in|Ran |[0-9]+ failures)'
    ```

    Verbatim output from the two-face archive sabotage:

    ```text
    FAIL in (every-body-is-byte-for-byte-identical-in-both-faces) (mcp_feature_thread_test.clj:1986)
    FAIL in (every-body-is-byte-for-byte-identical-in-both-faces) (mcp_feature_thread_test.clj:1986)
    FAIL in (the-text-block-is-a-superset-of-the-structured-receipt) (mcp_feature_thread_test.clj:640)
    FAIL in (the-text-block-is-a-superset-of-the-structured-receipt) (mcp_feature_thread_test.clj:640)
    Ran 53 tests containing 1372 assertions.
    4 failures, 0 errors.
    ```

    Verbatim output from the same command on clean `bb3b6360`:

    ```text
    Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
    Testing clj-surgeon.mcp-feature-thread-test
    Ran 53 tests containing 1372 assertions.
    0 failures, 0 errors.
    ```

    Exact command for the broader RED-pre-image sabotage:

    ```text
    mkdir -p /var/tmp/forge/ft5-review-fx/b2-current-red-source && git archive bb3b6360 | tar -x -C /var/tmp/forge/ft5-review-fx/b2-current-red-source && git archive 1cef5e25 src/clj_surgeon/mcp_feature_thread.clj | tar -x -C /var/tmp/forge/ft5-review-fx/b2-current-red-source
    export FT5_CP="$(clojure -Spath -M:clj-surgeon/mcp-test)"; ~/bin/suite-run java -cp "$FT5_CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (prn r) (System/exit (+ (:fail r) (:error r))))"; echo EXIT=$?
    ```

    Verbatim terminal summary:

    ```text
    Ran 53 tests containing 1227 assertions.
    100 failures, 0 errors.
    {:test 53, :pass 1127, :fail 100, :error 0, :type :summary}
    EXIT=100
    ```

11. **All named gates are green at `bb3b6360`; the current counts supersede the older claims.** `Makefile:180`, `Makefile:184`, `Makefile:219`, `test/clj_surgeon/mcp_test_runner.clj:1`. Every listed command returned process exit 0. The JVM suite is 764/9815/0, the Babashka suite is 814/6724/0, smoke reports seven tools, and the intent audit reports 392 specifications, zero violations, and THREAD-001 through THREAD-042 implemented. The audit is a marker/coherence gate; its green result did not detect findings 2 or 3.

    Exact commands:

    ```text
    make mcp-operation-oracle && make repository-hygiene; echo EXIT=$?
    make mcp-smoke
    ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
    ~/bin/suite-run bb test/run_all.clj
    ~/bin/suite-run clojure -M -e "(require 'clj-surgeon.mcp-intent-contract)(let [a (clj-surgeon.mcp-intent-contract/audit-current-repository)] (prn {:ok (:ok a) :spec-count (:spec-count a) :violations (count (:violations a)) :thread (into (sorted-map) (filter (fn [[k _]] (clojure.string/starts-with? k \"MCP-OP-THREAD-\")) (:status-by-id a)))}))"
    ```

    Verbatim outputs:

    ```text
    # @spec MCP-OP-ORACLE-001
    swipl -q -f test/mcp_operation_contract_oracle.pl
    mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
    # @spec MCP-OP-ALIAS-036
    # @spec MCP-OP-ALIAS-053
    repository hygiene: no machine-local build cache is tracked at any depth
    EXIT=0
    ```

    ```text
    bb test/mcp_stdio_smoke.clj
    {:ok true, :operation :mcp-stdio-smoke, :server "clj-surgeon", :tools ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure" "alias_migration" "admit_clojure_patch" "feature_thread"], :response-count 3, :wall-ms 8621.411204}
    Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
    clj-surgeon MCP: ready — telemetry off
    ```

    ```text
    Ran 764 tests containing 9815 assertions.
    0 failures, 0 errors.
    ```

    ```text
    Ran 814 tests containing 6724 assertions.
    0 failures, 0 errors.
    ```

    ```text
    Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
    {:ok true, :spec-count 392, :violations 0, :thread {"MCP-OP-THREAD-001" :implemented, "MCP-OP-THREAD-002" :implemented, "MCP-OP-THREAD-003" :implemented, "MCP-OP-THREAD-004" :implemented, "MCP-OP-THREAD-005" :implemented, "MCP-OP-THREAD-006" :implemented, "MCP-OP-THREAD-007" :implemented, "MCP-OP-THREAD-008" :implemented, "MCP-OP-THREAD-009" :implemented, "MCP-OP-THREAD-010" :implemented, "MCP-OP-THREAD-011" :implemented, "MCP-OP-THREAD-012" :implemented, "MCP-OP-THREAD-013" :implemented, "MCP-OP-THREAD-014" :implemented, "MCP-OP-THREAD-015" :implemented, "MCP-OP-THREAD-016" :implemented, "MCP-OP-THREAD-017" :implemented, "MCP-OP-THREAD-018" :implemented, "MCP-OP-THREAD-019" :implemented, "MCP-OP-THREAD-020" :implemented, "MCP-OP-THREAD-021" :implemented, "MCP-OP-THREAD-022" :implemented, "MCP-OP-THREAD-023" :implemented, "MCP-OP-THREAD-024" :implemented, "MCP-OP-THREAD-025" :implemented, "MCP-OP-THREAD-026" :implemented, "MCP-OP-THREAD-027" :implemented, "MCP-OP-THREAD-028" :implemented, "MCP-OP-THREAD-029" :implemented, "MCP-OP-THREAD-030" :implemented, "MCP-OP-THREAD-031" :implemented, "MCP-OP-THREAD-032" :implemented, "MCP-OP-THREAD-033" :implemented, "MCP-OP-THREAD-034" :implemented, "MCP-OP-THREAD-035" :implemented, "MCP-OP-THREAD-036" :implemented, "MCP-OP-THREAD-037" :implemented, "MCP-OP-THREAD-038" :implemented, "MCP-OP-THREAD-039" :implemented, "MCP-OP-THREAD-040" :implemented, "MCP-OP-THREAD-041" :implemented, "MCP-OP-THREAD-042" :implemented}}
    ```

12. **The round-five history is 15 explicit RED/GREEN pairs plus the final byte-identity witness; the managed onboarding block honestly excludes the write admission tool.** `docs/onboarding-template.md:297`, `test/clj_surgeon/workspace_onboarding_test.clj:120`, `src/clj_surgeon/mcp_server.clj:52`. The server advertises seven tools, while the managed automatic-onboarding list deliberately contains the six read/edit tools and omits `admit_clojure_patch`; the checked witness asserts that exact string. No six-to-seven assertion was weakened: the live smoke and the complete JVM/Babashka suites exercise the seven-tool catalog.

    Exact commands:

    ```text
    git log --format='%h %s' 9139b2c5..bb3b6360
    git log --format='%s' 9139b2c5..bb3b6360 | awk 'BEGIN{r=0;g=0} /^RED:/{r++} /^GREEN:|^Witness:/{g++} END{print "RED=" r " GREEN_OR_WITNESS=" g}'
    sed -n '294,300p' docs/onboarding-template.md && sed -n '116,123p' test/clj_surgeon/workspace_onboarding_test.clj
    ```

    Verbatim output:

    ```text
    bb3b6360 Witness: the text face renders every body byte-for-byte (MCP-OP-THREAD-012)
    34b1d682 GREEN: an unqualified `#'handler` in a route table joins (MCP-OP-THREAD-042)
    9c90e29b RED: a route entry that names its handler var UNQUALIFIED does not join
    00410edf GREEN: the two recall defects (MCP-OP-THREAD-040, MCP-OP-THREAD-041)
    88d54a84 RED: the real-repo recall found two defects — a test-file "implementation", and an untyped absence
    f2d3d5ee GREEN: round-three spec 1 — co-menu-item peers (MCP-OP-THREAD-039)
    cce34124 RED: round-three spec 1 — the use leg names no co-menu-item peers
    238c919e GREEN: round-three spec 3 — `absent` negative evidence and the `probe` param (MCP-OP-THREAD-038)
    3ccebe21 RED: round-three spec 3 — the receipt carries no negative evidence
    50d11b7b GREEN: round-three spec 2 — rules.request_contract (MCP-OP-THREAD-037)
    e569f955 RED: round-three spec 2 — rules carries no request_contract row
    32cc17f7 GREEN: addendum — an anchor carries after_context (MCP-OP-THREAD-036)
    3b9adc7a RED: addendum — an anchor names an insertion point and not one line of what is there
    422fb6be GREEN: round-three spec 4 — a script leg states its export (MCP-OP-THREAD-035)
    147661d9 RED: round-three spec 4 — a script leg makes no statement about export
    82747509 GREEN: 3.2 — next_call emits per-leg digests and says "the subset your patch touches"
    e4f44fb2 RED: 3.2 — next_call is not executable for the normal edit
    6970989e GREEN: 3.1 — the clock is out of the superset haystack; a refusal says would_be_text_bytes
    ba075b6a RED: 3.1 — the clock's digits swallow a structured leaf; a refusal's text_bytes describes nothing delivered
    4c1ffbe9 GREEN: a CANDIDATE leg carries no insertion anchor (MCP-OP-THREAD-034)
    aa49f028 RED: a CANDIDATE leg offers an insertion anchor it does not vouch for
    059d5629 GREEN: 3.3 + 3.4 — verify rows are runnable; one unreadable entry per file per leg
    14647101 RED: 3.3 + 3.4 — a make recipe prefix shipped as a shell command; unreadable listed twice
    e7e3293c GREEN: 3.5 — separator in the walk's containment test, distinct paths (MCP-OP-THREAD-033)
    823577d2 RED: 3.5 — the walk's containment test has no separator and lists a file twice
    6539a76b GREEN: B2' — (comment …), #_ and /* … */ are comment mentions (MCP-OP-THREAD-032)
    1cef5e25 RED: B2' — a (comment …) form, an #_ discard and a /* … */ block are read as code
    0e5effc0 GREEN: B1' — track character-class state in the script lexer's :regex mode
    8781f516 RED: B1' — an unescaped `/` inside a regex character class ends the regex in the lexer
    dd99ccb6 GREEN: B3 — union the automatic leg's globs into the walk; N/A names its seed; UNSCANNED is counted
    066aa1d9 RED: B3 — the automatic implementation leg is walked over a file set bounded before it existed
    RED=15 GREEN_OR_WITNESS=16
      enabled_tools = ["inspect_clojure", "apply_clojure_changes", "edit_clojure", "transform_clojure", "alias_migration", "feature_thread"]
    enabled = true
    startup_timeout_sec = 20.0
    tool_timeout_sec = 120.0
                         (str/starts-with? line "<!-- END CLJ-SURGEON -->"))
                       (str/starts-with? line "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"alias_migration\", \"feature_thread\"]"))
                       (str/starts-with? line "enabled_tools ="))
              "managed block keeps the exact enabled MCP tool names")
    ```

13. **Current-trunk dry-run merge has one content conflict.** `test/clj_surgeon/mcp_intent_contract_test.clj:20`. `origin/MCP/main` was `44e70af5a551d208b1748ade022e8f8404b10146` at review time. The only conflict is both sides editing the same two audited-path lists: this branch adds the feature-thread spec and current trunk adds the temp-leak spec. It is mechanically resolvable by retaining both, but the tip does not merge conflict-free and the behavioral blockers still make it ineligible.

    Exact command:

    ```text
    git rev-parse origin/MCP/main && git merge-tree --write-tree HEAD origin/MCP/main; echo EXIT=$?
    ```

    Verbatim output:

    ```text
    44e70af5a551d208b1748ade022e8f8404b10146
    49070765a56f3f8a48a5d225bbe720a95fd14c9e
    100644 a3e28eb398c3722d6cd4551112742e949eb7dcfc 1	test/clj_surgeon/mcp_intent_contract_test.clj
    100644 dd67f61ce12d14dd81b5964fc8355d12dbcf499e 2	test/clj_surgeon/mcp_intent_contract_test.clj
    100644 f4c0f3cc2a9d32405db59ce7ac210d5506c245f7 3	test/clj_surgeon/mcp_intent_contract_test.clj

    Auto-merging src/clj_surgeon/mcp_server.clj
    Auto-merging src/clj_surgeon/mcp_tool.clj
    Auto-merging test/clj_surgeon/mcp_intent_contract_test.clj
    CONFLICT (content): Merge conflict in test/clj_surgeon/mcp_intent_contract_test.clj
    Auto-merging test/clj_surgeon/mcp_test_runner.clj
    Auto-merging test/clj_surgeon/workspace_onboarding_test.clj
    EXIT=1
    ```

14. **Operational constraints were observed.** `src/clj_surgeon/mcp_server.clj:294`. The live process used explicit port 8126, was interrupted after the calls, no listener remains, the repository is still clean/detached, and the sole fixture root was deleted.

    Exact commands:

    ```text
    clojure -X:clj-surgeon/mcp :port 8126 :nrepl-port :none :telemetry :off
    # after all live calls: send SIGINT to that exact foreground process
    review_fx=/var/tmp/forge/ft5-review-fx; realpath -m "$review_fx"; test "$(realpath -m "$review_fx")" = /var/tmp/forge/ft5-review-fx && chmod -R u+rwX "$review_fx" && find /var/tmp/forge/ft5-review-fx -depth -delete; test ! -e /var/tmp/forge/ft5-review-fx && echo 'fixture cleanup: removed /var/tmp/forge/ft5-review-fx'
    git status --short --branch && ss -ltn 'sport = :8126' | sed -n '1,5p'; test ! -e /var/tmp/forge/ft5-review-fx && echo 'fixtures_absent=true'
    ```

    Verbatim outputs:

    ```text
    Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
    clj-surgeon MCP: persistent server ready on http://127.0.0.1:8126/mcp
    ^C
    /var/tmp/forge/ft5-review-fx
    fixture cleanup: removed /var/tmp/forge/ft5-review-fx
    ## HEAD (no branch)
    State Recv-Q Send-Q Local Address:Port Peer Address:Port
    fixtures_absent=true
    ```

## NO-GO

This tip is **not GO on its own for MCP/main**: current `origin/MCP/main` has one benign audited-path-list conflict, and findings 2 and 3 are independently blocking even after that conflict is resolved.
