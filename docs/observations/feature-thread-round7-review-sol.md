## NO-GO

1. `repository:HEAD` — the review checkout is the requested immutable tip and began clean.

   Exact command:

   ```text
   git rev-parse HEAD && git status --short --branch
   ```

   Verbatim output:

   ```text
   529755f06d66a4ab62d9e1a98e6ad7e8c9005c01
   ## HEAD (no branch)
   ```

2. `src/clj_surgeon/mcp_server.clj:54` — the live pinned server started on the explicitly allowed port 8126 and advertised exactly seven tools, including `feature_thread`.

   Exact command (after the HTTP initialize handshake):

   ```text
   sed -n 's/^data://p' /var/tmp/forge/ft7-review-fx/tools.body | jq -r '.result.tools | "count=\(length) names=\(map(.name)|join(","))"'; cat /var/tmp/forge/ft7-review-fx/ready.edn
   ```

   Verbatim output:

   ```text
   count=7 names=inspect_clojure,apply_clojure_changes,edit_clojure,transform_clojure,alias_migration,admit_clojure_patch,feature_thread
   {:transport :streamable-http, :project-root "/home/forge/tmp/sol/ft1-wt", :server :clj-surgeon, :port 8126, :host "127.0.0.1", :pid 2873015, :ok true, :verification-profile-source :built-in, :url "http://127.0.0.1:8126/mcp"}
   ```

3. **BLOCKING — `src/clj_surgeon/mcp_feature_thread.clj:1600`: a subject present only inside a Clojure string is promoted to FOUND, receives insertion anchors, and can make a valid five-leg convention report `COMPLETE (5 of 5)`.** `leg-strength` rejects comments, weak boundaries, and fallback evidence, but a parsed Clojure form reached by `identifier-or-route` is unconditionally strong; unlike the script fallback, it never asks whether the match is inside a string. This violates the requested string-only closure and the rule that only real owners count. The fixture contains only `(def note "stringOnly appears only in this string")`; the inline convention has the required five declared legs and is accepted by the public schema.

   Exact command:

   ```text
   jq -n --arg root /var/tmp/forge/ft7-review-fx/attack '{subject:"stringOnly",scope:{workspace_root:$root},budget_bytes:32768,config:{"repo-label":"string-only-false-green",legs:[range(0;5)|{"id":("leg-"+(.|tostring)),"kind":"use",globs:["src/*.clj"]}]}}' > /var/tmp/forge/ft7-review-fx/string-false-green.args.json && sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/string-false-green.args.json /var/tmp/forge/ft7-review-fx/string-false-green 41 && jq -r '.result.structuredContent | "error=\(.error_type // "none") status=\(.status) complete=\(.complete) legs_found=\(.legs_found)", (.legs[]|"\(.id) \(.status) \(.file // "-"):\(.from // "-")-\(.to // "-") evid=\(.evidence // "-") anchor=\(.anchor // "nil")")' /var/tmp/forge/ft7-review-fx/string-false-green.json
   ```

   Verbatim output:

   ```text
   error=none status=COMPLETE (5 of 5) complete=true legs_found=5
   leg-0 FOUND src/own.clj:18-18 evid=identifier-or-route anchor=after:L18
   leg-1 FOUND src/own.clj:18-18 evid=identifier-or-route anchor=after:L18
   leg-2 FOUND src/own.clj:18-18 evid=identifier-or-route anchor=after:L18
   leg-3 FOUND src/own.clj:18-18 evid=identifier-or-route anchor=after:L18
   leg-4 FOUND src/own.clj:18-18 evid=identifier-or-route anchor=after:L18
   implementation N/A -:--- evid=- anchor=nil
   ```

4. **BLOCKING — `src/clj_surgeon/mcp_feature_thread.clj:1007`: the conventions file itself may be a symlink outside the workspace and is read.** `load-conventions` tests `.isFile` and then `slurp`s the path without a no-follow/realpath containment check. A workspace-local `.clj-surgeon/feature-thread.edn` symlink to `../../outside/conventions.edn` caused the live verb to publish the out-of-root canary as `repo_label`. This directly meets the brief’s blocking condition “any read outside the workspace root”; the no-follow bounded source walk does not protect this earlier read.

   Exact command:

   ```text
   readlink /var/tmp/forge/ft7-review-fx/convsymlink/.clj-surgeon/feature-thread.edn && sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/convsymlink.args.json /var/tmp/forge/ft7-review-fx/convsymlink-call 60 && jq -r '.result as $w | $w.structuredContent | "isError=\($w.isError // false) error=\(.error_type // "none") repo_label=\(.repo_label // "-") conventions_source=\(.conventions_source // "-") status=\(.status // "-") outside_canary_in_text=\(($w.content[0].text|contains("OUTSIDE_CONVENTIONS_CANARY_FT7")))"' /var/tmp/forge/ft7-review-fx/convsymlink-call.json
   ```

   Verbatim output:

   ```text
   ../../outside/conventions.edn
   isError=false error=none repo_label=OUTSIDE_CONVENTIONS_CANARY_FT7 conventions_source=.clj-surgeon/feature-thread.edn status=INCOMPLETE (0 of 5) outside_canary_in_text=true
   ```

5. **BLOCKING — `src/clj_surgeon/mcp_feature_thread.clj:1690` and `test/clj_surgeon/mcp_feature_thread_test.clj:161`: every T1 range digest excludes the range’s final LF.** Recomputing SHA-256 over the actual inclusive line slice produced by `sed -n '<from>,<to>p'` disagrees for all six legs; each receipt digest matches only after deleting the slice’s final byte. The witness constructs its “slice” with `split`/`join`, which silently removes that LF. The visible BODY block reintroduces an LF as renderer framing and therefore `cmp`s equal, but that does not make the published `sha256` a digest of the file slice the receipt names. The brief explicitly makes a wrong range digest blocking.

   Exact command:

   ```text
   for id in menu-caller js-function route handler tests implementation; do claimed=$(jq -r --arg id "$id" '.result.structuredContent.legs[] | select(.id==$id) | .sha256' /var/tmp/forge/ft7-review-fx/t1.json); full=$(sha256sum /var/tmp/forge/ft7-review-fx/${id}.slice | cut -d' ' -f1); minus=$(head -c -1 /var/tmp/forge/ft7-review-fx/${id}.slice | sha256sum | cut -d' ' -f1); printf '%s claimed=%s full=%s without_final_LF=%s matches=%s\n' "$id" "$claimed" "$full" "$minus" "$([ "$claimed" = "$minus" ] && printf without_final_LF || printf neither)"; done
   ```

   Verbatim output:

   ```text
   menu-caller claimed=679b2b275f0b9ad929ff59a23252c4c38bb3a83fa4432eefce621392a7583b3f full=4aff0ec67c8a1dcbbf2e82bf723eae8ef69cd9743b543a88d536340b620c1fca without_final_LF=679b2b275f0b9ad929ff59a23252c4c38bb3a83fa4432eefce621392a7583b3f matches=without_final_LF
   js-function claimed=ab3318a09d0910c410fb0369519f72f6d54b43193378f6db9cc1bb9c9c805612 full=f947ad9aeb506110b3309028a9ba9695617a45b903e2ee39b60d64c45b656b59 without_final_LF=ab3318a09d0910c410fb0369519f72f6d54b43193378f6db9cc1bb9c9c805612 matches=without_final_LF
   route claimed=9bd0ce1e388ff364ba8eacdfbf5c13c9befbe51e50ab29db59f39b193bc9fe4f full=7499febc64daf1e0e4c1c9674fe40e4a56f53d2c26dda696ee7a6e66e81362b9 without_final_LF=9bd0ce1e388ff364ba8eacdfbf5c13c9befbe51e50ab29db59f39b193bc9fe4f matches=without_final_LF
   handler claimed=db6e58198a23fa5d1fc4fca7857a85330870d4fbd68c1b0e47be9adf23f0d9dd full=1a35e3b23abe8d132354c11d69e2e7f206e9a7adbee766805fbc2dfa183f3a9a without_final_LF=db6e58198a23fa5d1fc4fca7857a85330870d4fbd68c1b0e47be9adf23f0d9dd matches=without_final_LF
   tests claimed=3f09f5fff4e4c944dd5424b2b4cffc31f5bdcc4800a2cc103356784ddad70058 full=bd9c92a6187c70b3dcbd7d9ca250cb19f127fbfaf769d713765b2a787f2f4248 without_final_LF=3f09f5fff4e4c944dd5424b2b4cffc31f5bdcc4800a2cc103356784ddad70058 matches=without_final_LF
   implementation claimed=007315c59652811d7c1c058bcdb2e2458ad01e0fb65cb4d74926dbfb19ca98a3 full=e0946275b528ff2aa6d00b676a8b016c09ae7f16eb8db6fcdfdd0caf1c41bb70 without_final_LF=007315c59652811d7c1c058bcdb2e2458ad01e0fb65cb4d74926dbfb19ca98a3 matches=without_final_LF
   ```

6. **BLOCKING — `src/clj_surgeon/mcp_feature_thread.clj:2705` and `:2827`: text is not a superset of delivered structured content for accepted custom conventions.** Each delivered leg contains an internal `elide` leaf, but a five-use-leg receipt contains no spelling of its value `menu` anywhere in text. The named SMW case passes only because its conventional words occur coincidentally elsewhere. Four of 58 live receipts failed the leaf check (`elide="menu"` or `elide="js-function"`/`"tests"`); bodies were correctly absent from structured content, so this is not the expected body split. This violates MCP-OP-THREAD-012 and the round-seven explicit every-leaf attack.

   Exact command:

   ```text
   bb /var/tmp/forge/ft7-review-fx/check-superset.clj /var/tmp/forge/ft7-review-fx/string-false-green.json
   ```

   Verbatim output:

   ```text
   leaf_count=142 missing_leaf_count=5 structured_body_fields=0 declared_text_bytes=5093 actual_text_bytes=5093
   MISSING [:legs 0 :elide] "menu"
   MISSING [:legs 1 :elide] "menu"
   MISSING [:legs 2 :elide] "menu"
   MISSING [:legs 3 :elide] "menu"
   MISSING [:legs 4 :elide] "menu"
   ```

7. `src/clj_surgeon/mcp_feature_thread.clj:3722` — apart from the digest defect in finding 5, the named T1 topology reproduces live: `COMPLETE (6 of 6)`, the five requested owners and automatic implementation have the claimed ranges, every structured leg says its body is in text, the structured face contains zero body fields, delivered `text_bytes` is exact, every four-line `after_context` is byte-identical to the corresponding file slice, and all six whole-file `next_call.expect_pre_sha256` values match `sha256sum`.

   Exact commands:

   ```text
   sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/t1.args.json /var/tmp/forge/ft7-review-fx/t1-live 93
   jq -r '.result.structuredContent | "status=\(.status) legs_found=\(.legs_found) text_bytes=\(.text_bytes) structured_bytes=\(.structured_bytes) receipt_bytes=\(.receipt_bytes)", (.legs[] | "\(.id) \(.status) \(.file // "-"):\(.from // "-")-\(.to // "-") sha=\(.sha256 // "-") body_in_text=\(.body_in_text // false) boundary=\(.boundary // "-")")' /var/tmp/forge/ft7-review-fx/t1-live.json
   bb /var/tmp/forge/ft7-review-fx/check-superset.clj /var/tmp/forge/ft7-review-fx/t1-live.json
   ```

   Verbatim output:

   ```text
   status=COMPLETE (6 of 6) legs_found=6 text_bytes=27814 structured_bytes=15208 receipt_bytes=43022
   menu-caller FOUND src/writer/views/components.clj:102-113 sha=679b2b275f0b9ad929ff59a23252c4c38bb3a83fa4432eefce621392a7583b3f body_in_text=true boundary=form(parsed, member of L92-L165 top-tabs)
   js-function FOUND resources/public/js/editor-commands.js:389-454 sha=ab3318a09d0910c410fb0369519f72f6d54b43193378f6db9cc1bb9c9c805612 body_in_text=true boundary=brace-window(lexed,closed)
   route FOUND src/writer/routes.clj:2148-2148 sha=9bd0ce1e388ff364ba8eacdfbf5c13c9befbe51e50ab29db59f39b193bc9fe4f body_in_text=true boundary=form(parsed, member of L2083-L2376 make-routes)
   handler FOUND src/writer/handlers/transform.clj:606-680 sha=db6e58198a23fa5d1fc4fca7857a85330870d4fbd68c1b0e47be9adf23f0d9dd body_in_text=true boundary=form(parsed)
   tests FOUND test/writer/handlers/transform_apply_test.clj:349-384 sha=3f09f5fff4e4c944dd5424b2b4cffc31f5bdcc4800a2cc103356784ddad70058 body_in_text=true boundary=form(parsed)
   implementation FOUND src/writer/handlers/transform.clj:81-132 sha=007315c59652811d7c1c058bcdb2e2458ad01e0fb65cb4d74926dbfb19ca98a3 body_in_text=true boundary=form(parsed)
   leaf_count=365 missing_leaf_count=0 structured_body_fields=0 declared_text_bytes=27814 actual_text_bytes=27814
   ```

   Exact independent after-context command produced:

   ```text
   menu-caller after=L114-L117 byte_identical=yes
   js-function after=L455-L458 byte_identical=yes
   route after=L2149-L2152 byte_identical=yes
   handler after=L681-L684 byte_identical=yes
   tests after=L385-L388 byte_identical=yes
   implementation after=L133-L136 byte_identical=yes
   ```

   Exact whole-file digest command produced:

   ```text
   resources/public/js/editor-commands.js whole_file_sha_match=yes
   src/writer/handlers/transform.clj whole_file_sha_match=yes
   src/writer/routes.clj whole_file_sha_match=yes
   src/writer/views/components.clj whole_file_sha_match=yes
   test/js/browser_runtime_classic_script_test.js whole_file_sha_match=yes
   test/writer/handlers/transform_apply_test.clj whole_file_sha_match=yes
   ```

8. `src/clj_surgeon/mcp_feature_thread.clj:360` and `:1600` — the round-one/r3 lexer and CANDIDATE repairs otherwise hold. All requested regex/division/character-class/template/string hazards either closed at the actual brace or downgraded; the 408-line function, postfix ambiguity, and unterminated comment downgraded with labelled windows and no anchors. Comment-only, `(comment …)`, `#_`, block-comment, and alias-only hits were CANDIDATE with no anchor; a common prefix found nothing. The exception is the Clojure string false-green in finding 3.

   Exact command:

   ```text
   for subject in returnRegex typeofRegex caseRegex yieldRegex divisionAssign divisionPostfix divisionParen divisionBracket asiRegex slashSlashRegex classSlash classNegSlash classEscapedBracket nestedTemplate blockMarkerString unterminatedComment overCeiling; do sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/$subject.args.json /var/tmp/forge/ft7-review-fx/$subject 20; jq -r --arg s "$subject" '.result.structuredContent as $r | ($r.legs[] | select(.id=="js-function")) | "\($s) status=\(.status) range=\(.from // "-")-\(.to // "-") boundary=\(.boundary // "-") anchor=\(.anchor // "nil")"' /var/tmp/forge/ft7-review-fx/$subject.json; done; wc -l /var/tmp/forge/ft7-review-fx/attack/js/long.js
   ```

   Verbatim output:

   ```text
   returnRegex status=FOUND range=13-13 boundary=brace-window(lexed,closed) anchor=after:L13
   typeofRegex status=FOUND range=14-14 boundary=brace-window(lexed,closed) anchor=after:L14
   caseRegex status=FOUND range=15-15 boundary=brace-window(lexed,closed) anchor=after:L15
   yieldRegex status=FOUND range=16-16 boundary=brace-window(lexed,closed) anchor=after:L16
   divisionAssign status=FOUND range=17-17 boundary=brace-window(lexed,closed) anchor=after:L17
   divisionPostfix status=CANDIDATE range=1-38 boundary=line-window(+/-40, unclosed at L18) anchor=nil
   divisionParen status=FOUND range=19-19 boundary=brace-window(lexed,closed) anchor=after:L19
   divisionBracket status=FOUND range=20-20 boundary=brace-window(lexed,closed) anchor=after:L20
   asiRegex status=FOUND range=21-24 boundary=brace-window(lexed,closed) anchor=after:L24
   slashSlashRegex status=FOUND range=25-25 boundary=brace-window(lexed,closed) anchor=after:L25
   classSlash status=FOUND range=26-26 boundary=brace-window(lexed,closed) anchor=after:L26
   classNegSlash status=FOUND range=27-27 boundary=brace-window(lexed,closed) anchor=after:L27
   classEscapedBracket status=FOUND range=28-28 boundary=brace-window(lexed,closed) anchor=after:L28
   nestedTemplate status=FOUND range=29-29 boundary=brace-window(lexed,closed) anchor=after:L29
   blockMarkerString status=FOUND range=30-30 boundary=brace-window(lexed,closed) anchor=after:L30
   unterminatedComment status=CANDIDATE range=1-38 boundary=line-window(+/-40, unclosed at L34) anchor=nil
   overCeiling status=CANDIDATE range=1-41 boundary=line-window(+/-40, unclosed at L1) anchor=nil
   408 /var/tmp/forge/ft7-review-fx/attack/js/long.js
   ```

   Exact route-shape command:

   ```text
   for label in route-doc route-macro route-def-vector; do sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/$label.args.json /var/tmp/forge/ft7-review-fx/$label 30; jq -r --arg l "$label" '.result.structuredContent as $r | ($r.legs[]|select(.id=="route")) | "\($l) overall=\($r.status) route=\(.status) evidence=\(.evidence // "-") range=\(.from // "-")-\(.to // "-") anchor=\(.anchor // "nil") weak=\(.weak_reason // "-")"' /var/tmp/forge/ft7-review-fx/$label.json; done
   ```

   Verbatim output:

   ```text
   route-doc overall=INCOMPLETE (2 of 5) route=CANDIDATE evidence=route-literal range=14-14 anchor=nil weak=the route literal is a string inside `route-doc`, not a route-table entry
   route-macro overall=INCOMPLETE (2 of 5) route=CANDIDATE evidence=route-literal range=6-7 anchor=nil weak=the route literal is a string inside `macro-table`, not a route-table entry
   route-def-vector overall=INCOMPLETE (3 of 5) route=FOUND evidence=route-literal range=9-10 anchor=after:L10 weak=-
   ```

   A macro-built real route is conservatively CANDIDATE (a recall loss, not a false FOUND); a `def` vector referenced by the table is accepted as the parsed vector entry. CANDIDATE rows retain a SHA/refetch locator but have no insertion anchor and do not count toward status.

9. `test/clj_surgeon/mcp_feature_thread_test.clj:2248` — the round-seven recall table is identical on the fixture and the read-only real repository. The real repository is exactly `2df99c98`; `saveDraft` resolves the real route-table row at L2121 rather than its L392–445 handler docstring, and `expound` resolves `app-safe.js:L342-L368` while honestly remaining `INCOMPLETE (2 of 5)` with route/handler/tests ABSENT. The fixture also carries the formerly omitted `bulletize` peer at `app-safe.js:L175-L206` (finding 12).

   Exact command:

   ```text
   git -C /home/forge/tmp/replay/smw-base rev-parse HEAD; git -C /home/forge/tmp/replay/smw-base status --short --branch
   for rootlabel in fixture real; do if [ "$rootlabel" = fixture ]; then root=/var/tmp/forge/ft7-review-fx/smw-dequote; else root=/home/forge/tmp/replay/smw-base; fi; for subject in formatDraft saveDraft openTransformFromSelection expound; do sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/recall-$rootlabel-$subject.args.json /var/tmp/forge/ft7-review-fx/recall-$rootlabel-$subject 80; jq -r --arg label "$rootlabel/$subject" '.result.structuredContent as $r | "\($label) status=\($r.status) " + ([$r.legs[]|"\(.id)=\(.status):\(.file // "-"):L\(.from // "-")-L\(.to // "-")"]|join(" "))' /var/tmp/forge/ft7-review-fx/recall-$rootlabel-$subject.json; done; done
   ```

   Verbatim output:

   ```text
   2df99c989e2dc1963161c13f7a341847c16b4deb
   ## HEAD (no branch)
   fixture/formatDraft status=COMPLETE (6 of 6) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L389-L454 route=FOUND:src/writer/routes.clj:L2148-L2148 handler=FOUND:src/writer/handlers/transform.clj:L606-L680 tests=FOUND:test/writer/handlers/transform_apply_test.clj:L349-L384 implementation=FOUND:src/writer/handlers/transform.clj:L81-L132
   fixture/saveDraft status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L98-L101 js-function=FOUND:resources/public/js/editor-controller.js:L505-L571 route=FOUND:src/writer/routes.clj:L2121-L2121 handler=FOUND:src/writer/handlers/book_workshop.clj:L1922-L1985 tests=FOUND:test/js/editor_conflict_quarantine_test.js:L185-L199 implementation=N/A:-:L--L-
   fixture/openTransformFromSelection status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L332-L344 route=FOUND:src/writer/routes.clj:L2144-L2144 handler=FOUND:src/writer/handlers/chat.clj:L324-L352 tests=FOUND:test/writer/spa_lint_test.clj:L509-L521 implementation=N/A:-:L--L-
   fixture/expound status=INCOMPLETE (2 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/app-safe.js:L342-L368 route=ABSENT:-:L--L- handler=ABSENT:-:L--L- tests=ABSENT:-:L--L- implementation=N/A:-:L--L-
   real/formatDraft status=COMPLETE (6 of 6) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L389-L454 route=FOUND:src/writer/routes.clj:L2148-L2148 handler=FOUND:src/writer/handlers/transform.clj:L606-L680 tests=FOUND:test/writer/handlers/transform_apply_test.clj:L349-L384 implementation=FOUND:src/writer/handlers/transform.clj:L81-L132
   real/saveDraft status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L98-L101 js-function=FOUND:resources/public/js/editor-controller.js:L505-L571 route=FOUND:src/writer/routes.clj:L2121-L2121 handler=FOUND:src/writer/handlers/book_workshop.clj:L1922-L1985 tests=FOUND:test/js/editor_conflict_quarantine_test.js:L185-L199 implementation=N/A:-:L--L-
   real/openTransformFromSelection status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L332-L344 route=FOUND:src/writer/routes.clj:L2144-L2144 handler=FOUND:src/writer/handlers/chat.clj:L324-L352 tests=FOUND:test/writer/spa_lint_test.clj:L509-L521 implementation=N/A:-:L--L-
   real/expound status=INCOMPLETE (2 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/app-safe.js:L342-L368 route=ABSENT:-:L--L- handler=ABSENT:-:L--L- tests=ABSENT:-:L--L- implementation=N/A:-:L--L-
   ```

10. `src/clj_surgeon/mcp_feature_thread.clj:41`, `:3180`, and `:3392` — budget behavior reproduces with the current contract: default text budget 28,672, floor 11,264, hard cap 32,768, structured cap 32,640. At 11,264 every cut is labelled and handler is last; delivered text is 11,246 bytes. Requests for 10,240 and 1 byte refuse and quote that exact budget; a string and 40,000 refuse by type/cap. At 32,768 the named case fits with no cuts (`text=30218`, `structured=14994`), so the old structured-cap binding is gone. All six budget/refusal receipts had exact text counts when present and zero missing structured leaves; finding 6 concerns accepted custom conventions, not these named receipts.

   Exact command:

   ```text
   for label in b11264 b10240 b1 bstring b40000 b32768; do sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/$label.args.json /var/tmp/forge/ft7-review-fx/$label 50; jq -r --arg l "$label" '.result as $wire | $wire.structuredContent as $r | "\($l) isError=\($wire.isError // false) error=\($r.error_type // "none") status=\($r.status // "-") budget=\($r.budget_bytes // $r.requested_budget_bytes // "-") text=\($r.text_bytes // "-") structured=\($r.structured_bytes // "-") cuts=\([$r.elided[]?.leg]|join(",")) remedy=\($r.remedy // "-")"' /var/tmp/forge/ft7-review-fx/$label.json; bb /var/tmp/forge/ft7-review-fx/check-superset.clj /var/tmp/forge/ft7-review-fx/$label.json | sed 's/^/  /'; done
   ```

   Verbatim output:

   ```text
   b11264 isError=false error=none status=COMPLETE (6 of 6) budget=11264 text=11246 structured=13316 cuts=peers,sibling,after-context,verify-detail,governance-template,next-call,menu-caller,route,tests(js),tests,implementation,js-function,handler remedy=-
     leaf_count=367 missing_leaf_count=0 structured_body_fields=0 declared_text_bytes=11246 actual_text_bytes=11246
   b10240 isError=true error=feature-thread-budget-exceeded status=COMPLETE (6 of 6) budget=10240 text=- structured=- cuts= remedy=Raise budget_bytes (hard cap 32768) or narrow scope.paths.
     leaf_count=10 missing_leaf_count=0 structured_body_fields=0 declared_text_bytes= actual_text_bytes=526
   b1 isError=true error=feature-thread-budget-exceeded status=COMPLETE (6 of 6) budget=1 text=- structured=- cuts= remedy=Raise budget_bytes (hard cap 32768) or narrow scope.paths.
     leaf_count=10 missing_leaf_count=0 structured_body_fields=0 declared_text_bytes= actual_text_bytes=518
   bstring isError=true error=feature-thread-invalid-budget status=- budget=- text=- structured=- cuts= remedy=Pass a positive integer, or omit it for the default.
     leaf_count=9 missing_leaf_count=0 structured_body_fields=0 declared_text_bytes= actual_text_bytes=367
   b40000 isError=true error=feature-thread-budget-above-cap status=- budget=40000 text=- structured=- cuts= remedy=Request at most 32768 bytes.
     leaf_count=9 missing_leaf_count=0 structured_body_fields=0 declared_text_bytes= actual_text_bytes=331
   b32768 isError=false error=none status=COMPLETE (6 of 6) budget=32768 text=30218 structured=14994 cuts= remedy=-
     leaf_count=358 missing_leaf_count=0 structured_body_fields=0 declared_text_bytes=30218 actual_text_bytes=30218
   ```

11. `src/clj_surgeon/mcp_feature_thread.clj:305`, `:891`, and `:3477` — the ordinary confinement/admission attacks hold, distinct from the conventions-file symlink blocker. A chmod-000 handler is one unreadable ABSENT and yields `INCOMPLETE (4 of 5)`; out-of-root source-file and directory symlinks are neither read nor published; missing root, 10,001-character subject, 33 `also` seeds, malformed EDN, `../` convention glob, and `scope.paths` escape are bounded typed refusals. The escaping convention refusal names `legs[0].globs` and the glob as spelled. Per THREAD-043, an inside-spelled glob reached through a symlink is not shape-refused, but its target is safely absent.

   Exact command:

   ```text
   for label in unreadable bad-root long-subject also33 malformed escape-glob escape-scope; do sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/$label.args.json /var/tmp/forge/ft7-review-fx/$label 70; jq -r --arg l "$label" '.result as $w | $w.structuredContent as $r | "\($l) isError=\($w.isError // false) error=\($r.error_type // "none") status=\($r.status // "-") legs=\([$r.legs[]?|"\(.id):\(.status):\(.absent_cause // "-")"]|join(",")) field=\($r.field // "-") glob=\($r.glob // "-")"' /var/tmp/forge/ft7-review-fx/$label.json; done
   for label in symlink-file symlink-dir; do sh /var/tmp/forge/ft7-review-fx/mcp-call.sh /var/tmp/forge/ft7-review-fx/$label.args.json /var/tmp/forge/ft7-review-fx/$label 71; jq -r --arg l "$label" '.result as $w | $w.structuredContent as $r | "\($l) isError=\($w.isError // false) status=\($r.status) secret_content_read=\((($w.content[0].text // "") + ($r|tostring))|contains("READ_CANARY_SECRET_FT7")) published_symlink_path=\((($w.content[0].text // "") + ($r|tostring))|contains("js/secret.js"))"' /var/tmp/forge/ft7-review-fx/$label.json; done
   ```

   Verbatim output:

   ```text
   unreadable isError=false error=none status=INCOMPLETE (4 of 5) legs=menu-caller:FOUND:-,js-function:FOUND:-,route:FOUND:-,handler:ABSENT:searched-and-absent,tests:FOUND:-,implementation:N/A:- field=- glob=-
   bad-root isError=true error=invalid-workspace-root status=- legs= field=- glob=-
   long-subject isError=true error=feature-thread-subject-too-long status=- legs= field=subject glob=-
   also33 isError=true error=feature-thread-also-too-many status=- legs= field=also glob=-
   malformed isError=true error=feature-thread-conventions-invalid status=- legs= field=- glob=-
   escape-glob isError=true error=feature-thread-conventions-escaping-glob status=- legs= field=legs[0].globs glob=../outside/*.js
   escape-scope isError=true error=feature-thread-scope-path-escapes-workspace status=- legs= field=scope.paths glob=-
   symlink-file isError=false status=INCOMPLETE (0 of 5) secret_content_read=false published_symlink_path=false
   symlink-dir isError=false status=INCOMPLETE (0 of 5) secret_content_read=false published_symlink_path=false
   ```

   The unreadable row’s exact structured detail was:

   ```text
   {"status":"ABSENT","absent_cause":"searched-and-absent","unreadable":[{"file":"src/writer/handlers/transform.clj","reason":"unreadable"}]}
   ```

12. `src/clj_surgeon/mcp_feature_thread.clj:210`, `:2242`, `:2451`, and `:2795` — the new edit-basis metadata holds on the named case and peer stress. The body-free structured face retains locator/refetch data; all three real peers (including fixture-restored `expound` and `bulletize`) have ranges, SHA, anchors, and refetch. A 12-command menu returns all 11 peer rows at the 11,264-byte floor, never elides a row, and names the definition outside the declared JS glob as ABSENT. A second `mechanical-format` definition is retained under `implementation.also`. Governance honestly reports two lossy rows as `unparsed`; request keys agree; verify names the test namespace and whether each suite picks it up; `next_call` carries one clock repeated in its note and correct whole-file digests. A printed range assertion remains advisory; `admit_clojure_patch` is the actual whole-file pre-image control.

   Exact command:

   ```text
   jq -r '.result.structuredContent | "request_contract="+(.rules.request_contract|tojson), "governance="+([.rules.governance[]|"\(.line):\(.form_start // "-")-\(.form_end // "-"):\(.anchor)"]|join(",")), "next_call=\(.next_call.tool) computed_at=\(.next_call.computed_at)"' /var/tmp/forge/ft7-review-fx/t1.json
   jq -r '.result.structuredContent.legs[] | select(.id=="menu-caller") | .peers[] | "peer=\(.identifier) status=\(.status) locator=\(.file // "-"):L\(.from // "-")-L\(.to // "-") sha=\(.sha256 // "-") anchor=\(.anchor // "nil") refetch=\(.refetch // "-")"' /var/tmp/forge/ft7-review-fx/t1.json
   jq -r '.result.structuredContent as $r | ($r.legs[]|select(.id=="menu-caller")) as $m | "status=\($r.status) text=\($r.text_bytes) peers=\(($m.peers // [])|length) absent_peers=\([($m.peers // [])[]|select(.status=="ABSENT")|.identifier]|join(",")) peer_cut=\([$r.elided[]?|select(.leg=="peers")]|length)"' /var/tmp/forge/ft7-review-fx/peers-min.json
   jq -r '.result.structuredContent as $r | ($r.legs[]|select(.id=="implementation")) | "status=\($r.status) implementation=\(.status):\(.file):L\(.from)-L\(.to) also=\([.also[]?|"\(.file):L\(.from)-L\(.to):\(.status // "FOUND")"]|join(","))"' /var/tmp/forge/ft7-review-fx/twodef-call.json
   jq -r '.result.structuredContent.legs[] | select(.id=="tests") | "primary=\(.file):L\(.from)-L\(.to):\(.boundary)", (.co_primaries[]|"co_primary=\(.language):\(.file):L\(.from)-L\(.to):\(.boundary):anchor=\(.anchor // "nil")")' /var/tmp/forge/ft7-review-fx/t1.json
   jq -r '.result.structuredContent.legs[]|select(.id=="js-function")|"export="+.export' /var/tmp/forge/ft7-review-fx/t1.json
   ```

   Verbatim output:

   ```text
   request_contract={"route":"/api/transform/format","handler_reads":["sync"],"js_posts":["sync"],"agree?":true,"only_in_js":[],"only_in_handler":[]}
   governance=240:---:unparsed,268:---:unparsed,270:270-299:after:L299,299:270-299:after:L299,311:301-330:after:L330,382:382-400:after:L400
   next_call=admit_clojure_patch computed_at=2026-09-04T14:22:36.308738969Z
   peer=openTransformFromSelection status=FOUND locator=resources/public/js/editor-commands.js:L332-L344 sha=35de8c8e14f3b86a80353df8a1abc547963e4f0496be9673d38c6a7fa2fd795f anchor=after:L344 refetch=nl -ba resources/public/js/editor-commands.js | sed -n '332,344p'
   peer=expound status=FOUND locator=resources/public/js/app-safe.js:L342-L368 sha=90a22cdb1ff1eda9328ea93895bf9cf51854afef58cf6285f959bcd1776412fc anchor=after:L368 refetch=nl -ba resources/public/js/app-safe.js | sed -n '342,368p'
   peer=bulletize status=FOUND locator=resources/public/js/app-safe.js:L175-L206 sha=1a4c915693dfbafc38f5c5a00ada8af387a50c7c99a7da9822c4259a85e370c1 anchor=after:L206 refetch=nl -ba resources/public/js/app-safe.js | sed -n '175,206p'
   status=INCOMPLETE (3 of 5) text=9075 peers=11 absent_peers=outsidePeer peer_cut=0
   status=COMPLETE (6 of 6) implementation=FOUND:src/writer/handlers/transform.clj:L81-L132 also=src/writer/other/dup.clj:L3-L4:FOUND
   primary=test/writer/handlers/transform_apply_test.clj:L349-L384:form(parsed)
   co_primary=js:test/js/browser_runtime_classic_script_test.js:L63-L94:brace-window(lexed,closed), test-call at L63:anchor=after:L94
   export=none (classic script; functions are globals)
   ```

13. `test/clj_surgeon/mcp_server_test.clj:39`, `test/clj_surgeon/mcp_http_server_test.clj:260`, and `test/mcp_stdio_smoke.clj:93` — the thirteen catalog witnesses were strengthened honestly: exact ordered vectors/sets gained `feature_thread`, the exact count moved 6→7, hot add/remove arithmetic moved 7→8 and 6→7, the deftest/message changed six→seven, and the oracle gained receipt/refusal outcomes. No assertion was replaced by a loose containment check. The later onboarding fix removed only `admit_clojure_patch` from the managed client allowlist; source and byte-identical test now agree on the intended six enabled tools.

   Exact commands:

   ```text
   git show --format= --unified=0 02e823e7 -- test/mcp_stdio_smoke.clj test/clj_surgeon/mcp_server_test.clj test/clj_surgeon/mcp_http_server_test.clj test/clj_surgeon/admit_patch_test.clj test/clj_surgeon/mcp_operation_registry_test.clj test/clj_surgeon/workspace_onboarding_test.clj test/mcp_operation_contract_oracle.pl src/clj_surgeon/mcp_tool.clj src/clj_surgeon/mcp_server.clj src/clj_surgeon/workspace_onboarding.clj | rg '^[-+].*(feature_thread|exactly six|exactly seven|tool-count [678]|\(count tools\)|enabled_tools)'
   rg -n 'enabled_tools = |admit_clojure_patch' src/clj_surgeon/workspace_onboarding.clj test/clj_surgeon/workspace_onboarding_test.clj
   ```

   Verbatim output:

   ```text
   +   "feature_thread"
   -       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"alias_migration\"]\n"
   +       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"alias_migration\", \"admit_clojure_patch\", \"feature_thread\"]\n"
   +            "feature_thread"]
   +                "admit_clojure_patch" "feature_thread"]
   -                :tool-count 7
   +                :tool-count 8
   +                 "feature_thread"
   -                :tool-count 6
   +                :tool-count 7
   +                 "admit_clojure_patch" "feature_thread"}
   +   "feature_thread"
   +          "feature_thread"]
   -    (is (= 6 (count tools)))
   +    (is (= 7 (count tools)))
   +            "feature_thread"]
   -                       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"alias_migration\"]"))
   +                       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"alias_migration\", \"admit_clojure_patch\", \"feature_thread\"]"))
   +required_outcome(feature_thread, receipt).
   +required_outcome(feature_thread, typed_refusal).
   +                       "admit_clojure_patch" "feature_thread"]
   -                   "MCP must expose exactly six structural tools" {:tools tools})
   +                   "MCP must expose exactly seven structural tools"
   +                       "admit_clojure_patch" "feature_thread"]
   test/clj_surgeon/workspace_onboarding_test.clj:131:                       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"alias_migration\", \"feature_thread\"]"))
   src/clj_surgeon/workspace_onboarding.clj:297:       "enabled_tools = [\"inspect_clojure\", \"apply_clojure_changes\", \"edit_clojure\", \"transform_clojure\", \"alias_migration\", \"feature_thread\"]\n"
   ```

14. `Makefile:180`, `Makefile:184`, `Makefile:219`, `test/clj_surgeon/mcp_test_runner.clj:1`, and `test/clj_surgeon/mcp_feature_thread_test.clj:1` — every named gate is green at the pinned tip. The JVM lane was independently run twice and both runs returned 827 tests / 12,263 assertions / 0 failures / 0 errors. Babashka returned 825 / 6,770 / 0. The isolated feature-thread namespace returned the claimed 62 / 2,076 / 0. Oracle, seven-tool smoke, repository hygiene, tmp-leak ratchet, and intent audit all exited zero. The audit sees 414 total specs and all 49 THREAD specs, THREAD-001 through THREAD-049. These gates do not expose blockers 3–6.

   Exact commands:

   ```text
   make mcp-operation-oracle; printf 'EXIT=%s\n' "$?"
   env TMPDIR=/var/tmp/forge/ft7-review-fx JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft7-review-fx ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; printf 'EXIT=%s\n' "$?"
   env TMPDIR=/var/tmp/forge/ft7-review-fx JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft7-review-fx ~/bin/suite-run clojure -M:clj-surgeon/mcp-test; printf 'EXIT=%s\n' "$?"
   env TMPDIR=/var/tmp/forge/ft7-review-fx ~/bin/suite-run bb test/run_all.clj; printf 'EXIT=%s\n' "$?"
   make mcp-smoke; printf 'EXIT=%s\n' "$?"
   set -o pipefail; make repository-hygiene 2>&1 | tail -n 8; printf 'EXIT=%s\n' "${PIPESTATUS[0]}"
   set -o pipefail; sh test/tmp_leak_ratchet_test.sh 2>&1 | tee /var/tmp/forge/ft7-review-fx/tmp-leak.log | tail -n 3; printf 'GATE_EXIT=%s\n' "${PIPESTATUS[0]}"
   env TMPDIR=/var/tmp/forge/ft7-review-fx bb -cp src:test -e "(require 'clj-surgeon.mcp-intent-contract) (let [r (clj-surgeon.mcp-intent-contract/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (count (:violations r))}))"; printf 'EXIT=%s\n' "$?"
   FT7_CP="$(clojure -Spath -M:clj-surgeon/mcp-test)"; env TMPDIR=/var/tmp/forge/ft7-review-fx JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft7-review-fx ~/bin/suite-run java -cp "$FT7_CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (System/exit (+ (:fail r) (:error r))))"; printf 'EXIT=%s\n' "$?"
   rg -o 'MCP-OP-THREAD-[0-9]{3}' docs/intent/feature-thread/feature-thread-specs.md | sort -u > /var/tmp/forge/ft7-review-fx/thread-specs.txt; printf 'thread_specs='; wc -l < /var/tmp/forge/ft7-review-fx/thread-specs.txt; printf 'first='; sed -n '1p' /var/tmp/forge/ft7-review-fx/thread-specs.txt; printf 'last='; sed -n '$p' /var/tmp/forge/ft7-review-fx/thread-specs.txt; printf 'EXIT=%s\n' "$?"
   ```

   Verbatim terminal outputs, in command order:

   ```text
   # @spec MCP-OP-ORACLE-001
   swipl -q -f test/mcp_operation_contract_oracle.pl
   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   EXIT=0

   Ran 827 tests containing 12263 assertions.
   0 failures, 0 errors.
   EXIT=0

   Ran 827 tests containing 12263 assertions.
   0 failures, 0 errors.
   EXIT=0

   Ran 825 tests containing 6770 assertions.
   0 failures, 0 errors.
   EXIT=0

   bb test/mcp_stdio_smoke.clj
   {:ok true, :operation :mcp-stdio-smoke, :server "clj-surgeon", :tools ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure" "alias_migration" "admit_clojure_patch" "feature_thread"], :response-count 3, :wall-ms 8704.612543}
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
   clj-surgeon MCP: ready — telemetry off
   EXIT=0

   # @spec MCP-OP-ALIAS-036
   # @spec MCP-OP-ALIAS-053
   repository hygiene: no machine-local build cache is tracked at any depth
   EXIT=0

   --- SELF_TEST_TMP with TMPDIR=/var/tmp/forge/clj-surgeon-tmpleak-witness.A0B0kl -> /var/tmp/forge/clj-surgeon-tmpleak-witness.A0B0kl ---
   --- SELF_TEST_TMP with TMPDIR=/var/tmp/forge -> /var/tmp/forge ---
   tmp-leak ratchet witness passed
   GATE_EXIT=0

   {:ok true, :specs 414, :violations 0}
   EXIT=0

   Testing clj-surgeon.mcp-feature-thread-test

   Ran 62 tests containing 2076 assertions.
   0 failures, 0 errors.
   EXIT=0

   thread_specs=49
   first=MCP-OP-THREAD-001
   last=MCP-OP-THREAD-049
   EXIT=0
   ```

15. `test/clj_surgeon/mcp_feature_thread_test.clj:2622` — the sabotage ratchets bite on `git archive` exports. On a current-tip archive, changing only the text-face renderer to trim each body produces three current failures (the old “two failures” count is stale). On the historically matching `bb3b6360` export, replacing only `mcp_feature_thread.clj` with the B2′ RED pre-image at `1cef5e25` exactly reproduces the accumulated round-two ratchet: 53 tests, 1,227 assertions, 100 failures. The source SHA was checked byte-for-byte before running, avoiding a no-op sabotage; this is stronger than merely counting the original fourteen closures.

   Exact commands:

   ```text
   set -o pipefail
   FT7_CP="$(clojure -Spath -M:clj-surgeon/mcp-test)"
   env TMPDIR=/var/tmp/forge/ft7-review-fx JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft7-review-fx ~/bin/suite-run java -cp "$FT7_CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (System/exit (+ (:fail r) (:error r))))" 2>&1 | tee /var/tmp/forge/ft7-review-fx/two-face-sabotage-full.log | rg '^(FAIL in|Ran |[0-9]+ failures)'
   printf 'SUITE_EXIT=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   FAIL in (the-use-leg-names-its-co-menu-item-peers) (mcp_feature_thread_test.clj:2206)
   FAIL in (the-bodies-live-in-the-text-face-and-the-structured-face-says-so) (mcp_feature_thread_test.clj:2622)
   FAIL in (the-bodies-live-in-the-text-face-and-the-structured-face-says-so) (mcp_feature_thread_test.clj:2622)
   Ran 62 tests containing 2077 assertions.
   3 failures, 0 errors.
   SUITE_EXIT=3
   ```

   Exact historical archive preparation and checksum commands:

   ```text
   mkdir -p /var/tmp/forge/ft7-review-fx/sabotage-round2
   git archive bb3b6360 | tar -x -C /var/tmp/forge/ft7-review-fx/sabotage-round2
   git archive 1cef5e25 src/clj_surgeon/mcp_feature_thread.clj | tar -x -C /var/tmp/forge/ft7-review-fx/sabotage-round2
   sha256sum /var/tmp/forge/ft7-review-fx/sabotage-round2/src/clj_surgeon/mcp_feature_thread.clj
   printf 'expected='
   git show 1cef5e25:src/clj_surgeon/mcp_feature_thread.clj | sha256sum
   ```

   Verbatim output:

   ```text
   1b5750749fa8bd109f6887d47328467c5995dbc4bcb376c17b5d138770de60e0  /var/tmp/forge/ft7-review-fx/sabotage-round2/src/clj_surgeon/mcp_feature_thread.clj
   expected=1b5750749fa8bd109f6887d47328467c5995dbc4bcb376c17b5d138770de60e0  -
   ```

   Exact sabotage test command:

   ```text
   set -o pipefail
   FT7_CP="$(clojure -Spath -M:clj-surgeon/mcp-test)"
   env TMPDIR=/var/tmp/forge/ft7-review-fx JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft7-review-fx ~/bin/suite-run java -cp "$FT7_CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (prn r) (System/exit (+ (:fail r) (:error r))))" 2>&1 | tee /var/tmp/forge/ft7-review-fx/round2-sabotage-full.log | rg '^(Ran |[0-9]+ failures|\{:test)'
   printf 'SUITE_EXIT=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   Ran 53 tests containing 1227 assertions.
   100 failures, 0 errors.
   {:test 53, :pass 1127, :fail 100, :error 0, :type :summary}
   SUITE_EXIT=100
   ```

16. `repository:HEAD` — the dry-run merge is conflict-free against the current `origin/MCP/main` at `856f7756`, producing tree `efada82b`; that syntactic result does not cure the four blocking contract defects. Teardown stopped the explicit server, restored the chmod fixture before disposal, removed `/var/tmp/forge/ft7-review-fx`, removed the one untracked hot-verify port file left by an interrupted extra diagnostic run, and ended at the same detached commit with a clean worktree. Nothing was committed, staged, stashed, pushed, or changed in tracked repository files.

   Exact merge-tree command:

   ```text
   git rev-parse origin/MCP/main
   git merge-tree --write-tree HEAD origin/MCP/main
   git status --short --branch
   printf 'EXIT=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   856f7756f555ea11e54dc706a584b34154ea2990
   efada82bfc6d7135786cf1a8e60740218cbf576c
   ## HEAD (no branch)
   EXIT=0
   ```

   Exact teardown verification commands:

   ```text
   if ss -ltn '( sport = :8126 )' | rg -q ':8126'; then printf 'port_8126=LISTENING\n'; else printf 'port_8126=stopped\n'; fi
   if kill -0 2873015 2>/dev/null; then printf 'server_pid_2873015=alive\n'; else printf 'server_pid_2873015=stopped\n'; fi
   chmod 600 /var/tmp/forge/ft7-review-fx/unreadablefx/src/writer/handlers/transform.clj
   gio trash /var/tmp/forge/ft7-review-fx
   if test -e /var/tmp/forge/ft7-review-fx; then printf 'fixture_root=present\n'; else printf 'fixture_root=removed\n'; fi
   gio trash /home/forge/tmp/sol/ft1-wt/.hot-verify-test-84953071-2003-4f77-a3bb-b16f77eb48e0.port
   git status --short --branch
   git rev-parse HEAD
   ```

   Verbatim output:

   ```text
   port_8126=stopped
   server_pid_2873015=stopped
   fixture_root=removed
   ## HEAD (no branch)
   529755f06d66a4ab62d9e1a98e6ad7e8c9005c01
   ```

## NO-GO

This tip is not GO on its own for MCP/main: the dry-run merge is conflict-free against `origin/MCP/main@856f7756`, but findings 3–6 are blocking.
