## NO-GO

1. `repository:HEAD` — the review checkout is the requested immutable tip and began clean.

   Exact command:

   ```text
   git status --short --branch && git rev-parse HEAD
   ```

   Verbatim output:

   ```text
   ## HEAD (no branch)
   3dfe08956b58c2ac2780e036640ee5f88b266c77
   ```

2. `src/clj_surgeon/mcp_server.clj:54` — the isolated live server is on the explicitly allowed port 8126 and advertises exactly seven tools, including `feature_thread`. Its description no longer says “Before any edit, re-hash each leg's range”; it says the SHA-256 covers exactly what `sed -n '<from>,<to>p'` prints and that `admit_clojure_patch` is the authoritative whole-file pre-image gate.

   Exact command:

   ```text
   sed -n 's/^data: //p' /var/tmp/forge/ft9-review-fx/tools.json > /var/tmp/forge/ft9-review-fx/tools.body.json && jq -r '.result.tools | "count=\(length) names=\(map(.name)|join(","))"' /var/tmp/forge/ft9-review-fx/tools.body.json; sed -n '1p' /var/tmp/forge/ft9-review-fx/ready.edn
   ```

   Verbatim output:

   ```text
   count=7 names=inspect_clojure,apply_clojure_changes,edit_clojure,transform_clojure,alias_migration,admit_clojure_patch,feature_thread
   {:transport :streamable-http, :project-root "/home/forge/tmp/sol/ft1-wt", :server :clj-surgeon, :port 8126, :host "127.0.0.1", :pid 1632863, :ok true, :verification-profile-source :built-in, :url "http://127.0.0.1:8126/mcp"}
   ```

3. **BLOCKING — `src/clj_surgeon/mcp_feature_thread.clj:1302-1317`: a prose mention which merely LOOKS like a call inside a string is promoted to FOUND and receives an insertion anchor.** `literal-mask` correctly marks the occurrence as inside a string, but `string-shape` declares every match immediately followed by `(` to be a call. The admitted fixture `(def note "call formatDraft() later")` therefore returns five FOUND use legs, all pointing at the prose string and all carrying `anchor=after:L3`. A multiline Clojure prose string with `formatDraft()` repeats the false green. This violates the tool description’s promise that a hit inside a string which only mentions the subject is CANDIDATE, and it publishes edit authority against a non-owner.

   Exact command:

   ```text
   fx=/var/tmp/forge/ft9-review-fx/string-attacks; base='{"repo-label":"ft9-string-attack","legs":[{"id":"leg-0","kind":"use","globs":["GLOB"]},{"id":"leg-1","kind":"use","globs":["GLOB"]},{"id":"leg-2","kind":"use","globs":["GLOB"]},{"id":"leg-3","kind":"use","globs":["GLOB"]},{"id":"leg-4","kind":"use","globs":["GLOB"]}]}'; for row in 'call|formatDraft|src/call.clj' 'multiline|formatDraft|src/multiline.clj'; do IFS='|' read -r label subject glob <<< "$row"; cfg=${base//GLOB/$glob}; args=$(jq -nc --arg s "$subject" --arg root "$fx" --argjson c "$cfg" '{subject:$s,scope:{workspace_root:$root},config:$c,budget_bytes:32768}'); /var/tmp/forge/ft9-review-fx/mcp-call.sh 20 "$args" "/var/tmp/forge/ft9-review-fx/$label"; jq -r --arg l "$label" '.result.structuredContent as $r | "\($l) overall=\($r.status) " + ([$r.legs[]?|"\(.id)=\(.status):anchor=\(.anchor // "nil"):weak=\(.weak_reason // "-")"]|join(" | "))' "/var/tmp/forge/ft9-review-fx/$label.json"; done
   ```

   Verbatim output:

   ```text
   call overall=INCOMPLETE (5 of 6) leg-0=FOUND:anchor=after:L3:weak=- | leg-1=FOUND:anchor=after:L3:weak=- | leg-2=FOUND:anchor=after:L3:weak=- | leg-3=FOUND:anchor=after:L3:weak=- | leg-4=FOUND:anchor=after:L3:weak=- | implementation=CANDIDATE:anchor=nil:weak=the hit is a comment mention, not code
   multiline overall=INCOMPLETE (5 of 6) leg-0=FOUND:anchor=after:L5:weak=- | leg-1=FOUND:anchor=after:L5:weak=- | leg-2=FOUND:anchor=after:L5:weak=- | leg-3=FOUND:anchor=after:L5:weak=- | leg-4=FOUND:anchor=after:L5:weak=- | implementation=CANDIDATE:anchor=nil:weak=evidence identifier is a fallback search, not this leg's own shape
   ```

   The automatic implementation row prevents the overall header from saying COMPLETE in those two identifier calls, but that does not mitigate the false FOUND legs or their anchors. The same classifier’s unconditional `route` branch produces the explicit blocking form: a route-shaped subject present only in prose, `(def note "call /fake() later; this is prose, not a route table")`, makes all five accepted use roles FOUND and returns COMPLETE (5 of 5).

   Exact command:

   ```text
   cfg=$(jq -nc '{"repo-label":"ft9-route-shaped-prose",legs:[range(0;5)|{"id":("leg-"+(.|tostring)),"kind":"use","globs":["src/routecall.clj"]}]}'); args=$(jq -nc --argjson c "$cfg" '{subject:"/fake",scope:{workspace_root:"/var/tmp/forge/ft9-review-fx/string-attacks"},config:$c,budget_bytes:32768}'); /var/tmp/forge/ft9-review-fx/mcp-call.sh 700 "$args" /var/tmp/forge/ft9-review-fx/route-shaped-prose; jq -r '.result.structuredContent as $r | "overall=\($r.status) complete=\($r.complete) legs_found=\($r.legs_found)", ($r.legs[]|"\(.id) \(.status) \(.file // "-"):L\(.from // "-")-L\(.to // "-") anchor=\(.anchor // "nil") weak=\(.weak_reason // "-")")' /var/tmp/forge/ft9-review-fx/route-shaped-prose.json
   ```

   Verbatim output:

   ```text
   overall=COMPLETE (5 of 5) complete=true legs_found=5
   leg-0 FOUND src/routecall.clj:L3-L3 anchor=after:L3 weak=-
   leg-1 FOUND src/routecall.clj:L3-L3 anchor=after:L3 weak=-
   leg-2 FOUND src/routecall.clj:L3-L3 anchor=after:L3 weak=-
   leg-3 FOUND src/routecall.clj:L3-L3 anchor=after:L3 weak=-
   leg-4 FOUND src/routecall.clj:L3-L3 anchor=after:L3 weak=-
   implementation N/A -:L--L- anchor=nil weak=-
   ```

4. **BLOCKING under the review’s explicit path rule — `src/clj_surgeon/mcp_feature_thread.clj:1037-1068`: the typed conventions-symlink refusal publishes the resolved out-of-workspace target path.** The closure successfully refuses before reading the canary bytes, but its `resolved_target` and error text expose `/var/tmp/forge/ft9-review-fx/conventions/escape-outside.edn`, which is outside the supplied workspace root. The brief makes “a path published outside [the workspace root]” blocking, independently of whether file contents were read.

   Exact command:

   ```text
   args=$(jq -nc '{subject:"genuine",scope:{workspace_root:"/var/tmp/forge/ft9-review-fx/conventions/escape-root"},budget_bytes:32768}'); /var/tmp/forge/ft9-review-fx/mcp-call.sh 44 "$args" /var/tmp/forge/ft9-review-fx/conv-escape; jq -r '.result as $w | $w.structuredContent as $r | "isError=\($w.isError // false) error=\($r.error_type // "none") workspace_root=\($r.workspace_root // "-") conventions_source=\($r.conventions_source // "-") resolved_target=\($r.resolved_target // "-") canary_in_text=\((($w.content[0].text // "")+($r|tostring))|contains("OUTSIDE-CONVENTIONS-CANARY-FT9"))"' /var/tmp/forge/ft9-review-fx/conv-escape.json
   ```

   Verbatim output:

   ```text
   isError=true error=feature-thread-conventions-file-escapes-workspace workspace_root=/var/tmp/forge/ft9-review-fx/conventions/escape-root conventions_source=.clj-surgeon/feature-thread.edn resolved_target=/var/tmp/forge/ft9-review-fx/conventions/escape-outside.edn canary_in_text=false
   ```

   The root-itself symlink is accepted because both root and convention file resolve beneath the same real root; a hard-linked conventions inode is likewise accepted because it is reached through an in-root directory entry and has no distinct “real path.” EDN conventions have no include semantics: an `:include` key was ignored and its outside canary was not read. TOCTOU replacement between validation and read remains out of scope as directed.

5. **GO-WITH-FIX independently — `src/clj_surgeon/mcp_feature_thread.clj:66` and `:2573`: the documented 11,264-byte ranges-only floor no longer admits the named receipt.** Three warm live calls at exactly 11,264 bytes all returned typed `feature-thread-budget-exceeded`; the computed minimum was 11,266 bytes. At floor+50 (11,314), the delivered receipt is 11,266 bytes and its declared/delivered counts agree. The refusal is honest and bounded, so this is not a false receipt, but the builder’s “18 bytes of headroom” claim is false at this tip and fixture path.

   Exact command:

   ```text
   for i in 1 2 3; do args='{"subject":"formatDraft","also":["/api/transform/format","mechanical-format"],"scope":{"workspace_root":"/var/tmp/forge/ft9-review-fx/smw-dequote"},"budget_bytes":11264}'; /var/tmp/forge/ft9-review-fx/mcp-call.sh "$((60+i))" "$args" "/var/tmp/forge/ft9-review-fx/floor-$i"; jq -r --arg i "$i" '.result as $w | $w.structuredContent as $r | "run=\($i) isError=\($w.isError // false) error=\($r.error_type // "none") text=\($r.text_bytes // "-") would=\($r.would_be_text_bytes // "-") elapsed=\($r.elapsed_ms)"' "/var/tmp/forge/ft9-review-fx/floor-$i.json"; done
   ```

   Verbatim output:

   ```text
   run=1 isError=true error=feature-thread-budget-exceeded text=- would=11266 elapsed=340.255451
   run=2 isError=true error=feature-thread-budget-exceeded text=- would=11266 elapsed=405.40696
   run=3 isError=true error=feature-thread-budget-exceeded text=- would=11266 elapsed=306.733576
   ```

   Exact floor+50 command:

   ```text
   args=$(jq -nc '{subject:"formatDraft",also:["/api/transform/format","mechanical-format"],scope:{workspace_root:"/var/tmp/forge/ft9-review-fx/smw-dequote"},budget_bytes:11314}'); /var/tmp/forge/ft9-review-fx/mcp-call.sh 11314 "$args" /var/tmp/forge/ft9-review-fx/fx-budget-11314; declared=$(jq -r '.result.structuredContent.text_bytes' /var/tmp/forge/ft9-review-fx/fx-budget-11314.json); actual=$(jq -j '.result.content[0].text' /var/tmp/forge/ft9-review-fx/fx-budget-11314.json | wc -c); jq -r --arg d "$declared" --arg a "$actual" '.result.structuredContent as $r | "budget=\($r.budget_bytes) status=\($r.status) declared=\($d) delivered=\($a) headroom=\(($r.budget_bytes)-($d|tonumber)) cuts=\([$r.elided[]?.leg]|join(","))"' /var/tmp/forge/ft9-review-fx/fx-budget-11314.json
   ```

   Verbatim output:

   ```text
   budget=11314 status=COMPLETE (6 of 6) declared=11266 delivered=11266 headroom=48 cuts=peers,sibling,after-context,verify-detail,governance-template,next-call,menu-caller,route,tests(js),tests,implementation,js-function,handler
   ```

6. `src/clj_surgeon/mcp_feature_thread.clj:741,1604,3608-3643` — the named SMW receipt otherwise reproduces exactly. It is COMPLETE (6 of 6), has the expected ranges, keeps implementation distinct from the handler, reports all bodies in text, and reports exact text/structured/sum byte counts. Independent `sed` hashes match every published range SHA, and extraction of each visible BODY block is byte-identical to that same file slice, including the final LF.

   Exact live command:

   ```text
   args='{"subject":"formatDraft","also":["/api/transform/format","mechanical-format"],"scope":{"workspace_root":"/var/tmp/forge/ft9-review-fx/smw-dequote"}}'; /var/tmp/forge/ft9-review-fx/mcp-call.sh 500 "$args" /var/tmp/forge/ft9-review-fx/t1-fx; jq -r '.result.structuredContent as $r | "status=\($r.status) legs_found=\($r.legs_found) text_bytes=\($r.text_bytes) structured_bytes=\($r.structured_bytes) receipt_bytes=\($r.receipt_bytes)", ($r.legs[] | "\(.id) \(.status) \(.file // "-"):\(.from // "-")-\(.to // "-") sha=\(.sha256 // "-") body_in_text=\(.body_in_text // false) boundary=\(.boundary // "-")")' /var/tmp/forge/ft9-review-fx/t1-fx.json
   ```

   Verbatim output:

   ```text
   status=COMPLETE (6 of 6) legs_found=6 text_bytes=27814 structured_bytes=15096 receipt_bytes=42910
   menu-caller FOUND src/writer/views/components.clj:102-113 sha=4aff0ec67c8a1dcbbf2e82bf723eae8ef69cd9743b543a88d536340b620c1fca body_in_text=true boundary=form(parsed, member of L92-L165 top-tabs)
   js-function FOUND resources/public/js/editor-commands.js:389-454 sha=f947ad9aeb506110b3309028a9ba9695617a45b903e2ee39b60d64c45b656b59 body_in_text=true boundary=brace-window(lexed,closed)
   route FOUND src/writer/routes.clj:2148-2148 sha=7499febc64daf1e0e4c1c9674fe40e4a56f53d2c26dda696ee7a6e66e81362b9 body_in_text=true boundary=form(parsed, member of L2083-L2376 make-routes)
   handler FOUND src/writer/handlers/transform.clj:606-680 sha=1a35e3b23abe8d132354c11d69e2e7f206e9a7adbee766805fbc2dfa183f3a9a body_in_text=true boundary=form(parsed)
   tests FOUND test/writer/handlers/transform_apply_test.clj:349-384 sha=bd9c92a6187c70b3dcbd7d9ca250cb19f127fbfaf769d713765b2a787f2f4248 body_in_text=true boundary=form(parsed)
   implementation FOUND src/writer/handlers/transform.clj:81-132 sha=e0946275b528ff2aa6d00b676a8b016c09ae7f16eb8db6fcdfdd0caf1c41bb70 body_in_text=true boundary=form(parsed)
   ```

   Exact independent hash/body command:

   ```text
   root=/var/tmp/forge/ft9-review-fx/smw-dequote; for id in menu-caller js-function route handler tests implementation; do row=$(jq -r --arg id "$id" '.result.structuredContent.legs[]|select(.id==$id)|[.file,.from,.to,.sha256]|@tsv' /var/tmp/forge/ft9-review-fx/t1-fx.json); IFS=$'\t' read -r file from to claimed <<< "$row"; full=$(sed -n "${from},${to}p" "$root/$file" | sha256sum | cut -d' ' -f1); body=$(jq -r '.result.content[0].text' /var/tmp/forge/ft9-review-fx/t1-fx.json | LEG="$id" perl -0777 -ne 'my $id=$ENV{"LEG"}; if (/^leg \Q$id\E  .*?^  BODY<<\n(.*?)^  >>\n/ms) {print $1}' | sha256sum | cut -d' ' -f1); printf '%s sha_match=%s body_match=%s\n' "$id" "$([ "$claimed" = "$full" ] && printf yes || printf no)" "$([ "$body" = "$full" ] && printf yes || printf no)"; done
   ```

   Verbatim output:

   ```text
   menu-caller sha_match=yes body_match=yes
   js-function sha_match=yes body_match=yes
   route sha_match=yes body_match=yes
   handler sha_match=yes body_match=yes
   tests sha_match=yes body_match=yes
   implementation sha_match=yes body_match=yes
   ```

   All six `after_context` blocks were also byte-identical (`L114-117`, `L455-458`, `L2149-2152`, `L681-684`, `L385-388`, `L133-136`). Every `next_call.expect_pre_sha256` matched `sha256sum` of its whole file. The printed range assertion is advisory; `admit_clojure_patch` plus these whole-file digests is the actual stale-preimage control and returns a refusal on mismatch.

7. `src/clj_surgeon/mcp_feature_thread.clj:451-571,1212-1317,1797-1835` — apart from finding 3, the lexer and candidate boundaries hold. All keyword-aware regex contexts, division cases, regex character classes, nested template objects and comment markers in strings either close at the actual brace or downgrade explicitly. Postfix division ambiguity, an unterminated block comment and the 408-line function are CANDIDATE line windows with no anchor—never wrong closed ranges.

   Exact command:

   ```text
   for row in 'returnRegex|attacks.js' 'typeofRegex|attacks.js' 'caseRegex|attacks.js' 'yieldRegex|attacks.js' 'divisionAssign|attacks.js' 'divisionPostfix|attacks.js' 'divisionParen|attacks.js' 'divisionBracket|attacks.js' 'asiRegex|attacks.js' 'slashSlashRegex|attacks.js' 'classSlash|attacks.js' 'classNegSlash|attacks.js' 'classEscapedBracket|attacks.js' 'nestedTemplate|attacks.js' 'blockMarkerString|attacks.js' 'unterminatedComment|broken.js' 'overCeiling|long.js'; do IFS='|' read -r subject file <<< "$row"; jq -r --arg s "$subject" '.result.structuredContent.legs[0] | "\($s) status=\(.status) range=\(.from // "-")-\(.to // "-") boundary=\(.boundary // "-") anchor=\(.anchor // "nil")"' "/var/tmp/forge/ft9-review-fx/lex-$subject.json"; done; wc -l /var/tmp/forge/ft9-review-fx/lexer/long.js
   ```

   Verbatim output:

   ```text
   returnRegex status=FOUND range=1-1 boundary=brace-window(lexed,closed) anchor=after:L1
   typeofRegex status=FOUND range=2-2 boundary=brace-window(lexed,closed) anchor=after:L2
   caseRegex status=FOUND range=3-3 boundary=brace-window(lexed,closed) anchor=after:L3
   yieldRegex status=FOUND range=4-4 boundary=brace-window(lexed,closed) anchor=after:L4
   divisionAssign status=FOUND range=5-5 boundary=brace-window(lexed,closed) anchor=after:L5
   divisionPostfix status=CANDIDATE range=1-19 boundary=line-window(+/-40, unclosed at L6) anchor=nil
   divisionParen status=FOUND range=7-7 boundary=brace-window(lexed,closed) anchor=after:L7
   divisionBracket status=FOUND range=8-8 boundary=brace-window(lexed,closed) anchor=after:L8
   asiRegex status=FOUND range=9-12 boundary=brace-window(lexed,closed) anchor=after:L12
   slashSlashRegex status=FOUND range=13-13 boundary=brace-window(lexed,closed) anchor=after:L13
   classSlash status=FOUND range=14-14 boundary=brace-window(lexed,closed) anchor=after:L14
   classNegSlash status=FOUND range=15-15 boundary=brace-window(lexed,closed) anchor=after:L15
   classEscapedBracket status=FOUND range=16-16 boundary=brace-window(lexed,closed) anchor=after:L16
   nestedTemplate status=FOUND range=17-17 boundary=brace-window(lexed,closed) anchor=after:L17
   blockMarkerString status=FOUND range=18-18 boundary=brace-window(lexed,closed) anchor=after:L18
   unterminatedComment status=CANDIDATE range=1-5 boundary=line-window(+/-40, unclosed at L1) anchor=nil
   overCeiling status=CANDIDATE range=1-41 boundary=line-window(+/-40, unclosed at L1) anchor=nil
   408 /var/tmp/forge/ft9-review-fx/lexer/long.js
   ```

   The other literal attacks resolve as follows: a route literal in a handler docstring is CANDIDATE under a proper route leg; a subject inside a JS regex is CANDIDATE; a string inside a Clojure `(comment …)` form is CANDIDATE; the multiline Clojure string is the false FOUND in finding 3; `${formatDraft()}` in a JS template interpolation is correctly unmasked as code but conservatively remains CANDIDATE because the use-hit cannot establish a closed owner boundary. No CANDIDATE row has an anchor.

8. `src/clj_surgeon/mcp_feature_thread.clj:741-761,1604` — THREAD-052 closes the final-LF digest defect across the requested byte shapes. The range digest equals the exact `sed` output for CRLF, EOF with no final LF, a last line with LF, and a UTF-8 BOM.

   Exact command:

   ```text
   for label in crlf no-final-lf last-line-lf bom; do rowout=$(jq -r '.result.structuredContent.legs[0]|[.status,.file,.from,.to,.sha256,.boundary]|@tsv' "/var/tmp/forge/ft9-review-fx/digest-$label.json"); IFS=$'\t' read -r status gotfile from to claimed boundary <<< "$rowout"; full=$(sed -n "${from},${to}p" "/var/tmp/forge/ft9-review-fx/digest/$gotfile" | sha256sum | cut -d' ' -f1); printf '%s status=%s range=%s-%s boundary=%s match=%s\n' "$label" "$status" "$from" "$to" "$boundary" "$([ "$claimed" = "$full" ] && printf yes || printf no)"; done
   ```

   Verbatim output:

   ```text
   crlf status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes
   no-final-lf status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes
   last-line-lf status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes
   bom status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes
   ```

9. `src/clj_surgeon/mcp_feature_thread.clj:2995-3036` — THREAD-012/046 close the delivered-face hole. An independent JSON-wire walk over 58 live receipts with independently spelled convention IDs found every scalar leaf in the text. Nested collections are traversed by JSON path; keyword/symbol values arrive as JSON strings and are therefore checked as strings; null has no occurrence in these delivered faces. No object carries the internal `elide` key.

   Exact command:

   ```text
   for seed in $(seq 0 57); do jq -r -f /var/tmp/forge/ft9-review-fx/check-leaves.jq "/var/tmp/forge/ft9-review-fx/leaf-$seed.json"; done | awk 'BEGIN{n=0;bad=0} {n++; if ($0 !~ /missing_leaf_count=0/) bad++} END{printf "receipts=%d receipts_with_missing_leaves=%d\n",n,bad}'; jq -r -f /var/tmp/forge/ft9-review-fx/check-leaves.jq /var/tmp/forge/ft9-review-fx/leaf-0.json; jq -r '.result.structuredContent | [..|objects|select(has("elide"))]|"objects_with_elide=\(length)"' /var/tmp/forge/ft9-review-fx/leaf-0.json
   ```

   Verbatim output:

   ```text
   receipts=58 receipts_with_missing_leaves=0
   leaf_count=338 missing_leaf_count=0 missing=[]
   objects_with_elide=0
   ```

10. `src/clj_surgeon/mcp_feature_thread.clj:174,305-350,891-1110,3643-3740` — admission and confinement refusals remain typed and bounded. A chmod-000 handler produces INCOMPLETE (4 of 5), one deduplicated unreadable row, and no COMPLETE header. Source-file symlinks are neither read nor published. Missing root, 10,001-character subject, 33 `also` entries, malformed EDN, escaping glob, `scope.paths` escape, 1-byte budget, non-number budget and above-cap budget all refuse with the expected type; every refusal passes the text-superset walk.

   Exact command:

   ```text
   for label in unreadable symlink-source bad-root long-subject also33 malformed escape-glob escape-scope budget1 budget-string budget40000; do jq -r --arg l "$label" '.result as $w | $w.structuredContent as $r | "\($l) isError=\($w.isError // false) error=\($r.error_type // "none") status=\($r.status // "-") budget=\($r.budget_bytes // $r.requested_budget_bytes // "-") field=\($r.field // "-") glob=\($r.glob // "-")"' "/var/tmp/forge/ft9-review-fx/ref-$label.json"; done
   ```

   Verbatim output:

   ```text
   unreadable isError=false error=none status=INCOMPLETE (4 of 5) budget=32768 field=- glob=-
   symlink-source isError=false error=none status=INCOMPLETE (0 of 5) budget=32768 field=- glob=-
   bad-root isError=true error=invalid-workspace-root status=- budget=- field=- glob=-
   long-subject isError=true error=feature-thread-subject-too-long status=- budget=- field=subject glob=-
   also33 isError=true error=feature-thread-also-too-many status=- budget=- field=also glob=-
   malformed isError=true error=feature-thread-conventions-invalid status=- budget=- field=- glob=-
   escape-glob isError=true error=feature-thread-conventions-escaping-glob status=- budget=- field=legs[0].globs glob=../outside/*.js
   escape-scope isError=true error=feature-thread-scope-path-escapes-workspace status=- budget=- field=scope.paths glob=-
   budget1 isError=true error=feature-thread-budget-exceeded status=INCOMPLETE (3 of 5) budget=1 field=- glob=-
   budget-string isError=true error=feature-thread-invalid-budget status=- budget=- field=- glob=-
   budget40000 isError=true error=feature-thread-budget-above-cap status=- budget=40000 field=- glob=-
   ```

   The symlink source probe reported `symlink_canary=false published_symlink=false`. The unreadable handler was `{"status":"ABSENT","absent_cause":"searched-and-absent","unreadable":[{"file":"src/writer/handlers/transform.clj","reason":"unreadable"}]}` and `complete5_in_text=false`.

11. `test/clj_surgeon/mcp_feature_thread_test.clj:2248-2440` — the live recall table is identical on the fixture and the read-only real SMW repository at `2df99c98`. `saveDraft` uses the real route at L2121, not the docstring; `expound` finds `app-safe.js:L342-L368` but honestly remains INCOMPLETE (2 of 5) with no route, handler, or tests. T1 also carries the JS co-primary, `export=none`, governance/verify data, matching request contract, whole-file `by_leg` digests and the computed `admit_clojure_patch` next call.

   Exact command/output summary from the live calls:

   ```text
   git -C /home/forge/tmp/replay/smw-base rev-parse HEAD
   2df99c989e2dc1963161c13f7a341847c16b4deb
   fixture/formatDraft status=COMPLETE (6 of 6) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L389-L454 route=FOUND:src/writer/routes.clj:L2148-L2148 handler=FOUND:src/writer/handlers/transform.clj:L606-L680 tests=FOUND:test/writer/handlers/transform_apply_test.clj:L349-L384 implementation=FOUND:src/writer/handlers/transform.clj:L81-L132
   fixture/saveDraft status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L98-L101 js-function=FOUND:resources/public/js/editor-controller.js:L505-L571 route=FOUND:src/writer/routes.clj:L2121-L2121 handler=FOUND:src/writer/handlers/book_workshop.clj:L1922-L1985 tests=FOUND:test/js/editor_conflict_quarantine_test.js:L185-L199 implementation=N/A:-:L--L-
   fixture/openTransformFromSelection status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L332-L344 route=FOUND:src/writer/routes.clj:L2144-L2144 handler=FOUND:src/writer/handlers/chat.clj:L324-L352 tests=FOUND:test/writer/spa_lint_test.clj:L509-L521 implementation=N/A:-:L--L-
   fixture/expound status=INCOMPLETE (2 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/app-safe.js:L342-L368 route=ABSENT:-:L--L- handler=ABSENT:-:L--L- tests=ABSENT:-:L--L- implementation=N/A:-:L--L-
   real/formatDraft status=COMPLETE (6 of 6) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L389-L454 route=FOUND:src/writer/routes.clj:L2148-L2148 handler=FOUND:src/writer/handlers/transform.clj:L606-L680 tests=FOUND:test/writer/handlers/transform_apply_test.clj:L349-L384 implementation=FOUND:src/writer/handlers/transform.clj:L81-L132
   real/saveDraft status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L98-L101 js-function=FOUND:resources/public/js/editor-controller.js:L505-L571 route=FOUND:src/writer/routes.clj:L2121-L2121 handler=FOUND:src/writer/handlers/book_workshop.clj:L1922-L1985 tests=FOUND:test/js/editor_conflict_quarantine_test.js:L185-L199 implementation=N/A:-:L--L-
   real/openTransformFromSelection status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L332-L344 route=FOUND:src/writer/routes.clj:L2144-L2144 handler=FOUND:src/writer/handlers/chat.clj:L324-L352 tests=FOUND:test/writer/spa_lint_test.clj:L509-L521 implementation=N/A:-:L--L-
   real/expound status=INCOMPLETE (2 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/app-safe.js:L342-L368 route=ABSENT:-:L--L- handler=ABSENT:-:L--L- tests=ABSENT:-:L--L- implementation=N/A:-:L--L-
   ```

12. `test/clj_surgeon/mcp_feature_thread_test.clj:2691-3031` — all five requested sabotage controls bite on independent `git archive HEAD` exports whose source SHA changed from the common original `2b8516c4…`: deleting the string-mention branch gives 17 failures; disabling conventions containment 9; hashing the joined body 20; restoring `elide` while blinding keyword leaves 20; trimming text bodies 3.

   Exact command:

   ```text
   for d in sab-string sab-conventions sab-digest sab-face sab-trim; do printf '%s ' "$d"; tr -d '\000' < "/var/tmp/forge/ft9-review-fx/$d.log" | rg '^(Ran |[0-9]+ failures, [0-9]+ errors)' | tail -2; done
   ```

   Verbatim output:

   ```text
   sab-string Ran 67 tests containing 2174 assertions.
   17 failures, 0 errors.
   sab-conventions Ran 67 tests containing 2184 assertions.
   9 failures, 0 errors.
   sab-digest Ran 67 tests containing 2184 assertions.
   20 failures, 0 errors.
   sab-face Ran 67 tests containing 2184 assertions.
   20 failures, 0 errors.
   sab-trim Ran 67 tests containing 2185 assertions.
   3 failures, 0 errors.
   ```

13. `src/clj_surgeon/mcp_server.clj:54`, `src/clj_surgeon/workspace_onboarding.clj:297`, and `test/clj_surgeon/workspace_onboarding_test.clj:131` — the thirteen six→seven witnesses remain honest exact count/vector/set changes; no assertion was weakened. The managed onboarding block intentionally contains six enabled tools and excludes only `admit_clojure_patch`: `inspect_clojure`, `apply_clojure_changes`, `edit_clojure`, `transform_clojure`, `alias_migration`, `feature_thread`.

14. `Makefile:188-244`, `src/clj_surgeon/mcp_intent_contract.clj:159`, and `test/clj_surgeon/mcp_feature_thread_test.clj:1` — all gates are green and the two JVM runs are identical. The round-nine suite claims reproduce exactly: 832/12371 twice, 825/6770, namespace 67/2184, oracle pass, seven-tool smoke, hygiene, tmp-leak ratchet, and intent audit 417 specs / 0 violations with 52 THREAD specs (`001` through `052`). These gates do not expose findings 3–5.

   Exact commands:

   ```text
   env TMPDIR=/var/tmp/forge/ft9-review-fx JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft9-review-fx ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   env TMPDIR=/var/tmp/forge/ft9-review-fx JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft9-review-fx ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   env TMPDIR=/var/tmp/forge/ft9-review-fx ~/bin/suite-run bb test/run_all.clj
   make mcp-operation-oracle
   make mcp-smoke
   make repository-hygiene
   env TMPDIR=/var/tmp/forge/ft9-review-fx bb -cp src:test -e "(require 'clj-surgeon.mcp-intent-contract) (let [r (clj-surgeon.mcp-intent-contract/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (:violations r)}))"
   ```

   Verbatim terminal summaries:

   ```text
   Ran 832 tests containing 12371 assertions.
   0 failures, 0 errors.

   Ran 832 tests containing 12371 assertions.
   0 failures, 0 errors.

   Ran 825 tests containing 6770 assertions.
   0 failures, 0 errors.

   Ran 67 tests containing 2184 assertions.
   0 failures, 0 errors.

   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   {:ok true, :operation :mcp-stdio-smoke, :server "clj-surgeon", :tools ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure" "alias_migration" "admit_clojure_patch" "feature_thread"], :response-count 3, :wall-ms 7698.564234}
   repository hygiene: no machine-local build cache is tracked at any depth
   tmp-leak ratchet witness passed
   {:ok true, :specs 417, :violations []}
   thread_specs=52
   first=MCP-OP-THREAD-001
   last=MCP-OP-THREAD-052
   ```

15. `repository:merge-tree` — the local current trunk ref is `e6a11a7f9967fd40623524ed02639889a3ff9bec`, and the dry-run merge is conflict-free. The review checkout remains clean.

   Exact command:

   ```text
   git rev-parse origin/MCP/main && git merge-tree --write-tree HEAD origin/MCP/main; printf 'MERGE_TREE_EXIT=%s\n' "$?"; git status --short --branch
   ```

   Verbatim output:

   ```text
   e6a11a7f9967fd40623524ed02639889a3ff9bec
   888c0b9bbfde26d099fd333aa9cc049159feaa54
   MERGE_TREE_EXIT=0
   ## HEAD (no branch)
   ```

## NO-GO

This tip is not GO on its own for MCP/main: the merge-tree is clean, but it can still publish `COMPLETE (5 of 5)` with five false FOUND/anchor rows sourced only from a prose string, and its symlink refusal publishes an out-of-workspace path; the 11,264-byte floor claim is also stale.
