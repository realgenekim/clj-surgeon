## GO

1. HEAD identity — VERIFIED.

   File: `.git/HEAD` (detached worktree metadata)

   Exact command:

   ```text
   git rev-parse HEAD
   ```

   Verbatim output:

   ```text
   508f26f5531ff1bd359c5fb221c0558fd3fbc2f9
   ```

2. `test-fixtures/feature-thread/smw-dequote/.clj-surgeon/feature-thread.edn:1` and `src/clj_surgeon/mcp_feature_thread.clj:4229` — LIVE catalog and named T1 receipt VERIFIED. The detached tip's HTTP server on explicit port 8126 advertises exactly eight tools (the seven feature-thread-era tools plus trunk's `relation_census`). T1 is honestly COMPLETE 6/6 after the automatic implementation leg; all six line-range hashes equal the exact `sed` slices, all six rendered BODY blocks are byte-identical to those slices, all six `after_context` blocks are byte-identical, and every whole-file pre-image digest matches `sha256sum`.

   Exact commands:

   ```text
   env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp clojure -J-Xms64m -J-Xmx512m -X:clj-surgeon/mcp :project-dir '"/home/forge/tmp/sol/ft1-wt"' :port 8126 :telemetry :full :telemetry-dir '"/var/tmp/forge/ft12-review-fx/server/telemetry-live"' :run-id '"ft12-review"' :port-file '"/var/tmp/forge/ft12-review-fx/server/nrepl-live-port"' :ready-file '"/var/tmp/forge/ft12-review-fx/server/ready-live.edn"' :log-file '"/var/tmp/forge/ft12-review-fx/server/live.log"' :nrepl-port :none
   curl -sS --max-time 20 -X POST http://127.0.0.1:8126/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H 'Mcp-Session-Id: 44e24c32-b46c-4c94-86ce-cccc98f09676' --data '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
   payload=$(jq -nc --arg root /var/tmp/forge/ft12-review-fx/smw '{jsonrpc:"2.0",id:3,method:"tools/call",params:{name:"feature_thread",arguments:{subject:"formatDraft",also:["/api/transform/format","mechanical-format"],scope:{workspace_root:$root}}}}'); curl -sS --max-time 120 -X POST http://127.0.0.1:8126/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H 'Mcp-Session-Id: 44e24c32-b46c-4c94-86ce-cccc98f09676' --data "$payload" | sed -n 's/^data: //p'
   root=/var/tmp/forge/ft12-review-fx/smw; for id in menu-caller js-function route handler tests implementation; do row=$(jq -r --arg id "$id" '.result.structuredContent.legs[]|select(.id==$id)|[.file,.from,.to,.sha256]|@tsv' /var/tmp/forge/ft12-review-fx/t1.json); IFS=$'\t' read -r file from to claimed <<< "$row"; full=$(sed -n "${from},${to}p" "$root/$file" | sha256sum | cut -d' ' -f1); body=$(jq -r '.result.content[0].text' /var/tmp/forge/ft12-review-fx/t1.json | LEG="$id" perl -0777 -ne 'my $id=$ENV{"LEG"}; if (/^leg \Q$id\E  .*?^  BODY<<\n(.*?)^  >>\n/ms) {print $1}' | sha256sum | cut -d' ' -f1); printf '%s sha_match=%s body_match=%s\n' "$id" "$([ "$claimed" = "$full" ] && printf yes || printf no)" "$([ "$body" = "$full" ] && printf yes || printf no)"; done
   ```

   Verbatim output:

   ```text
   clj-surgeon MCP: persistent server ready on http://127.0.0.1:8126/mcp
   count=8 names=inspect_clojure,apply_clojure_changes,edit_clojure,transform_clojure,relation_census,alias_migration,admit_clojure_patch,feature_thread
   status=COMPLETE (6 of 6) legs=6/6 text_bytes=27807 structured_bytes=15089 receipt_bytes=42896 budget=28672
   menu-caller FOUND src/writer/views/components.clj:102-113 sha=4aff0ec67c8a1dcbbf2e82bf723eae8ef69cd9743b543a88d536340b620c1fca boundary=form(parsed, member of L92-L165 top-tabs) anchor=after:L113 in-form:L92-L165 body_in_text=true
   js-function FOUND resources/public/js/editor-commands.js:389-454 sha=f947ad9aeb506110b3309028a9ba9695617a45b903e2ee39b60d64c45b656b59 boundary=brace-window(lexed,closed) anchor=after:L454 body_in_text=true
   route FOUND src/writer/routes.clj:2148-2148 sha=7499febc64daf1e0e4c1c9674fe40e4a56f53d2c26dda696ee7a6e66e81362b9 boundary=form(parsed, member of L2083-L2376 make-routes) anchor=after:L2148 in-form:L2083-L2376 body_in_text=true
   handler FOUND src/writer/handlers/transform.clj:606-680 sha=1a35e3b23abe8d132354c11d69e2e7f206e9a7adbee766805fbc2dfa183f3a9a boundary=form(parsed) anchor=after:L680 body_in_text=true
   tests FOUND test/writer/handlers/transform_apply_test.clj:349-384 sha=bd9c92a6187c70b3dcbd7d9ca250cb19f127fbfaf769d713765b2a787f2f4248 boundary=form(parsed) anchor=after:L384 body_in_text=true
   implementation FOUND src/writer/handlers/transform.clj:81-132 sha=e0946275b528ff2aa6d00b676a8b016c09ae7f16eb8db6fcdfdd0caf1c41bb70 boundary=form(parsed) anchor=after:L132 body_in_text=true
   menu-caller sha_match=yes body_match=yes
   js-function sha_match=yes body_match=yes
   route sha_match=yes body_match=yes
   handler sha_match=yes body_match=yes
   tests sha_match=yes body_match=yes
   implementation sha_match=yes body_match=yes
   after:menu-caller range=114-117 byte_match=yes
   after:js-function range=455-458 byte_match=yes
   after:route range=2149-2152 byte_match=yes
   after:handler range=681-684 byte_match=yes
   after:tests range=385-388 byte_match=yes
   after:implementation range=133-136 byte_match=yes
   whole-file resources/public/js/editor-commands.js match=yes
   whole-file src/writer/handlers/transform.clj match=yes
   whole-file src/writer/routes.clj match=yes
   whole-file src/writer/views/components.clj match=yes
   whole-file test/js/browser_runtime_classic_script_test.js match=yes
   whole-file test/writer/handlers/transform_apply_test.clj match=yes
   ```

3. `src/clj_surgeon/mcp_feature_thread.clj:451` and `test/clj_surgeon/mcp_feature_thread_test.clj:3081` — round-ten whole-literal/evidence closure VERIFIED LIVE. A prose string that looks like a call, multiline string, line comment, `(comment ...)`, `#_`, route in a docstring, and regex-only mention are CANDIDATE without anchors. A whole-literal call and whole route literal are FOUND. An assembled route with no literal is ABSENT. `formatDraftX` yields only a fallback CANDIDATE for `formatDraft`, never FOUND. Template interpolation is correctly unmasked as code; because this use hit is not itself a closed owner, it conservatively remains a boundary CANDIDATE rather than a string-literal candidate. This is the right answer: `openTransformFromSelection` losing the assertion-string-only leg is honest, because assertion prose is not executable ownership.

   Exact command:

   ```text
   root=/var/tmp/forge/ft12-review-fx/literals; n=50; while IFS='|' read -r label subject kind file; do legs=$(jq -nc --arg k "$kind" --arg f "$file" '[range(0;5) as $i | {id:("leg-"+($i|tostring)),kind:$k,globs:[$f]}]'); args=$(jq -nc --arg root "$root" --arg s "$subject" --arg label "$label" --argjson legs "$legs" '{subject:$s,budget_bytes:32768,scope:{workspace_root:$root},config:{"repo-label":$label,legs:$legs}}'); /var/tmp/forge/ft12-review-fx/mcp-call.sh "$n" "$args" "/var/tmp/forge/ft12-review-fx/lit-$label.json"; jq -r --arg label "$label" '.result.structuredContent as $r | ($r.legs[]|select(.id=="leg-0")) as $l | "\($label) thread=\($r.status) leg=\($l.status) evidence=\($l.evidence // "-") boundary=\($l.boundary // "-") anchor=\($l.anchor // "nil") weak=\($l.weak_reason // "-")"' "/var/tmp/forge/ft12-review-fx/lit-$label.json"; n=$((n+1)); done
   ```

   Verbatim output:

   ```text
   prose thread=INCOMPLETE (0 of 6) leg=CANDIDATE evidence=identifier-or-route boundary=form(parsed, member of L3-L3 note) anchor=nil weak=the hit is inside a string literal that only MENTIONS the subject -- not a call spelling and not a route literal
   comment thread=INCOMPLETE (0 of 6) leg=CANDIDATE evidence=identifier-or-route boundary=line-window(no-enclosing-top-level-form) anchor=nil weak=the hit is a comment mention, not code
   commentform thread=INCOMPLETE (0 of 6) leg=CANDIDATE evidence=identifier-or-route boundary=form(parsed, member of L3-L4 form) anchor=nil weak=the hit is a comment mention, not code
   discard thread=INCOMPLETE (0 of 6) leg=CANDIDATE evidence=identifier-or-route boundary=line-window(no-enclosing-top-level-form) anchor=nil weak=the hit is a comment mention, not code
   wholecall thread=INCOMPLETE (5 of 6) leg=FOUND evidence=identifier-or-route boundary=form(parsed, member of L3-L3 command) anchor=after:L3 weak=-
   routedoc thread=INCOMPLETE (0 of 5) leg=CANDIDATE evidence=route-literal boundary=form(parsed, member of L3-L6 unrelated) anchor=nil weak=the hit is inside a string literal that only MENTIONS the subject -- not a call spelling and not a route literal
   routewhole thread=COMPLETE (5 of 5) leg=FOUND evidence=route-literal boundary=form(parsed, member of L3-L3 routes) anchor=after:L3 weak=-
   assembled thread=INCOMPLETE (0 of 5) leg=ABSENT evidence=- boundary=- anchor=nil weak=-
   prefix thread=INCOMPLETE (0 of 6) leg=CANDIDATE evidence=identifier boundary=form(parsed) anchor=nil weak=evidence identifier is a fallback search, not this leg's own shape
   regex thread=INCOMPLETE (0 of 6) leg=CANDIDATE evidence=identifier-or-route boundary=line-window(+/-40, unclosed at L2) anchor=nil weak=the hit is inside a string literal that only MENTIONS the subject -- not a call spelling and not a route literal
   template thread=INCOMPLETE (0 of 6) leg=CANDIDATE evidence=identifier-or-route boundary=line-window(+/-40, unclosed at L2) anchor=nil weak=the boundary is not a parsed form or a closed brace window: line-window(+/-40, unclosed at L2)
   ```

4. `src/clj_surgeon/mcp_feature_thread.clj:1212` and `test/clj_surgeon/mcp_feature_thread_test.clj:1376` — lexer attacks VERIFIED LIVE. Every named regex/division/ASI/character-class/template/string case either closes at the actual brace or downgrades LABELLED with no anchor; none publishes a wrong closed range. The conservative `a++ / 2` ambiguity downgrades. The 408-line function demonstrates the 400-line ceiling.

   Exact command:

   ```text
   root=/var/tmp/forge/ft12-review-fx/lexer; n=70; while IFS='|' read -r subject file; do legs=$(jq -nc --arg f "$file" '[range(0;5) as $i | {id:("leg-"+($i|tostring)),kind:"def",globs:[$f]}]'); args=$(jq -nc --arg root "$root" --arg s "$subject" --argjson legs "$legs" '{subject:$s,budget_bytes:32768,scope:{workspace_root:$root},config:{"repo-label":"lexer",legs:$legs}}'); /var/tmp/forge/ft12-review-fx/mcp-call.sh "$n" "$args" "/var/tmp/forge/ft12-review-fx/lex-$subject.json"; jq -r --arg s "$subject" '.result.structuredContent.legs[]|select(.id=="leg-0")|"\($s) status=\(.status) range=\(.from // "-")-\(.to // "-") boundary=\(.boundary // "-") anchor=\(.anchor // "nil")"' "/var/tmp/forge/ft12-review-fx/lex-$subject.json"; n=$((n+1)); done; wc -l /var/tmp/forge/ft12-review-fx/lexer/js/long.js
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
   408 /var/tmp/forge/ft12-review-fx/lexer/js/long.js
   ```

5. `src/clj_surgeon/mcp_feature_thread.clj:2995` and `test/clj_surgeon/mcp_feature_thread_test.clj:3243` — budgets and two-face containment VERIFIED LIVE. The current default is 28,672 B, hard cap 32,768 B. At 10,240 the honest outcome is now a typed ranges-floor refusal, superseding round one's older successful-elision expectation. At every delivered size, `text_bytes` equals the exact UTF-8 bytes delivered and every structured scalar leaf is spelled in the text. Elisions are ordered and labelled; handler is last. Refusals quote the caller's budget. On the named checked-in fixture (the path used by the derived witness), 11,266 admits exactly and 11,265 refuses with `would_be_text_bytes=11266`; +50 admits. The structured face is body-free but retains every locator/refetch; `receipt_bytes` consistently means text plus structured bytes.

   Exact command:

   ```text
   root=/var/tmp/forge/ft12-review-fx/smw; n=100; for b in default 10240 11265 11266 11316 16384 24576 28672 32768 1 string 32769; do if [ "$b" = default ]; then args=$(jq -nc --arg root "$root" '{subject:"formatDraft",also:["/api/transform/format","mechanical-format"],scope:{workspace_root:$root}}'); elif [ "$b" = string ]; then args=$(jq -nc --arg root "$root" '{subject:"formatDraft",also:["/api/transform/format","mechanical-format"],scope:{workspace_root:$root},budget_bytes:"wide"}'); else args=$(jq -nc --arg root "$root" --argjson b "$b" '{subject:"formatDraft",also:["/api/transform/format","mechanical-format"],scope:{workspace_root:$root},budget_bytes:$b}'); fi; /var/tmp/forge/ft12-review-fx/mcp-call.sh "$n" "$args" "/var/tmp/forge/ft12-review-fx/budget-$b.json"; delivered=$(jq -j '.result.content[0].text' "/var/tmp/forge/ft12-review-fx/budget-$b.json" | wc -c); jq -r --arg b "$b" --arg delivered "$delivered" '.result as $w|$w.structuredContent as $r|"budget=\($b) isError=\($w.isError // false) error=\($r.error_type // "none") status=\($r.status // "-") declared=\($r.text_bytes // "-") delivered=\($delivered) request_budget=\($r.budget_bytes // "-") elisions=\([$r.elisions[]?|(.section+":"+(.leg // ""))]|join(","))"' "/var/tmp/forge/ft12-review-fx/budget-$b.json"; jq -r -f /var/tmp/forge/ft12-review-fx/check-leaves.jq "/var/tmp/forge/ft12-review-fx/budget-$b.json"; n=$((n+1)); done
   n=120; for b in 11264 11265 11266 11267; do args=$(jq -nc --argjson b "$b" '{subject:"formatDraft",also:["/api/transform/format","mechanical-format"],scope:{workspace_root:"test-fixtures/feature-thread/smw-dequote"},budget_bytes:$b}'); /var/tmp/forge/ft12-review-fx/mcp-call.sh "$n" "$args" "/var/tmp/forge/ft12-review-fx/floor-$b.json"; done
   ```

   Verbatim output:

   ```text
   budget=default isError=false error=none status=COMPLETE (6 of 6) declared=27807 delivered=27807 request_budget=28672 elisions=peers:
   missing_leaf_count=0 missing=[]
   budget=10240 isError=true error=feature-thread-budget-exceeded status=COMPLETE (6 of 6) declared=- delivered=526 request_budget=10240 elisions=
   missing_leaf_count=0 missing=[]
   budget=11265 isError=false error=none status=COMPLETE (6 of 6) declared=11259 delivered=11259 request_budget=11265 elisions=peers:,sibling:,after-context:,verify-detail:,governance-template:,next-call:,menu-caller:,route:,tests(js):,tests:,implementation:,js-function:,handler:
   missing_leaf_count=0 missing=[]
   budget=11316 isError=false error=none status=COMPLETE (6 of 6) declared=11259 delivered=11259 request_budget=11316 elisions=peers:,sibling:,after-context:,verify-detail:,governance-template:,next-call:,menu-caller:,route:,tests(js):,tests:,implementation:,js-function:,handler:
   missing_leaf_count=0 missing=[]
   budget=16384 isError=false error=none status=COMPLETE (6 of 6) declared=15040 delivered=15040 request_budget=16384 elisions=peers:,sibling:,after-context:,verify-detail:,governance-template:,next-call:,menu-caller:,route:,tests(js):,tests:,implementation:,js-function:
   missing_leaf_count=0 missing=[]
   budget=24576 isError=false error=none status=COMPLETE (6 of 6) declared=23687 delivered=23687 request_budget=24576 elisions=peers:,sibling:,after-context:,verify-detail:,governance-template:,next-call:
   missing_leaf_count=0 missing=[]
   budget=28672 isError=false error=none status=COMPLETE (6 of 6) declared=27807 delivered=27807 request_budget=28672 elisions=peers:
   missing_leaf_count=0 missing=[]
   budget=32768 isError=false error=none status=COMPLETE (6 of 6) declared=30211 delivered=30211 request_budget=32768 elisions=
   missing_leaf_count=0 missing=[]
   budget=1 isError=true error=feature-thread-budget-exceeded status=COMPLETE (6 of 6) declared=- delivered=518 request_budget=1 elisions=
   missing_leaf_count=0 missing=[]
   budget=string isError=true error=feature-thread-invalid-budget status=- declared=- delivered=367 request_budget=- elisions=
   missing_leaf_count=0 missing=[]
   budget=32769 isError=true error=feature-thread-budget-above-cap status=- declared=- delivered=331 request_budget=32769 elisions=
   missing_leaf_count=0 missing=[]
   budget=11264 error=feature-thread-budget-exceeded status=COMPLETE (6 of 6) declared=- would_be=11266 delivered=526 root=-
   budget=11265 error=feature-thread-budget-exceeded status=COMPLETE (6 of 6) declared=- would_be=11266 delivered=526 root=-
   budget=11266 error=none status=COMPLETE (6 of 6) declared=11266 would_be=- delivered=11266 root=test-fixtures/feature-thread/smw-dequote
   budget=11267 error=none status=COMPLETE (6 of 6) declared=11266 would_be=- delivered=11266 root=test-fixtures/feature-thread/smw-dequote
   ```

6. `src/clj_surgeon/mcp_feature_thread.clj:891` and `test/clj_surgeon/mcp_feature_thread_test.clj:2791` — confinement/refusal attacks VERIFIED LIVE. chmod 000 makes only the handler ABSENT and the receipt INCOMPLETE (4 of 5), with no false COMPLETE text; the same unreadable file is listed once in each affected leg. Missing root, 10,001-character subject, 33 seeds, malformed EDN, escaping glob, and `scope.paths` escape are typed. A source-file symlink and an in-spelling glob reached through an escaping directory symlink are neither read nor published. A symlinked conventions file refuses using only `.clj-surgeon/feature-thread.edn`, echoes the caller's root spelling, has no `resolved_target`, leaks neither target substring nor canary, and says it was not read. A symlinked workspace root is valid and remains spelled as the caller gave it while containment uses its resolved root. A hard link is correctly treated as an in-root file: hard links have no independently resolvable target path, so the in-root directory entry is the file and no out-of-root path is traversed or published. The conventions EDN grammar has no include facility. TOCTOU between containment check and read remains explicitly outside this audit's scope.

   Exact command:

   ```text
   chmod 000 /var/tmp/forge/ft12-review-fx/unreadable/src/writer/handlers/transform.clj
   # each row below was one HTTP tools/call to feature_thread through /var/tmp/forge/ft12-review-fx/mcp-call.sh
   for label in unreadable missing long also malformed convlink hardlink root-link escapeglob scope symlinkglob sourcelink; do jq -r --arg l "$label" '.result as $w|$w.structuredContent as $r|"\($l) isError=\($w.isError // false) error=\($r.error_type // "none") status=\($r.status // "-") root=\($r.workspace_root // "-") repo=\($r.repo_label // "-") unreadable=\([$r.legs[]?.unreadable[]?]|length) complete_text=\(($w.content[0].text|contains("COMPLETE (5 of 5)"))) leak=\(((($w.content[0].text//"")+($r|tostring))|contains("LEAKMARK-FT12"))) outside_path=\(((($w.content[0].text//"")+($r|tostring))|contains("/var/tmp/forge/ft12-review-fx/confinement/outside")))"' "/var/tmp/forge/ft12-review-fx/ref-$label.json"; done
   jq -r '.result.structuredContent | {error_type,error,workspace_root,conventions_source,resolved_target}' /var/tmp/forge/ft12-review-fx/ref-convlink.json
   ```

   Verbatim output:

   ```text
   unreadable isError=false error=none status=INCOMPLETE (4 of 5) root=/var/tmp/forge/ft12-review-fx/unreadable repo=social-media-writer unreadable=2 complete_text=false leak=false outside_path=false
   missing isError=true error=invalid-workspace-root status=- root=/var/tmp/forge/ft12-review-fx/no-such-root repo=- unreadable=0 complete_text=false leak=false outside_path=false
   long isError=true error=feature-thread-subject-too-long status=- root=- repo=- unreadable=0 complete_text=false leak=false outside_path=false
   also isError=true error=feature-thread-also-too-many status=- root=- repo=- unreadable=0 complete_text=false leak=false outside_path=false
   malformed isError=true error=feature-thread-conventions-invalid status=- root=/var/tmp/forge/ft12-review-fx/confinement/malformed repo=- unreadable=0 complete_text=false leak=false outside_path=false
   convlink isError=true error=feature-thread-conventions-file-escapes-workspace status=- root=/var/tmp/forge/ft12-review-fx/confinement/convlink repo=- unreadable=0 complete_text=false leak=false outside_path=false
   hardlink isError=false error=none status=COMPLETE (5 of 5) root=/var/tmp/forge/ft12-review-fx/confinement/hardlink repo=OUTSIDE-CONVENTIONS-CANARY-FT12 unreadable=0 complete_text=true leak=false outside_path=false
   root-link isError=false error=none status=COMPLETE (5 of 5) root=/var/tmp/forge/ft12-review-fx/confinement/root-link repo=INSIDE-OK unreadable=0 complete_text=true leak=false outside_path=false
   escapeglob isError=true error=feature-thread-conventions-escaping-glob status=- root=/var/tmp/forge/ft12-review-fx/confinement/escapeglob repo=- unreadable=0 complete_text=false leak=false outside_path=false
   scope isError=true error=feature-thread-scope-path-escapes-workspace status=- root=- repo=- unreadable=0 complete_text=false leak=false outside_path=false
   symlinkglob isError=false error=none status=INCOMPLETE (0 of 5) root=/var/tmp/forge/ft12-review-fx/confinement/symlinkglob repo=symlinkglob unreadable=0 complete_text=false leak=false outside_path=false
   sourcelink isError=false error=none status=INCOMPLETE (0 of 5) root=/var/tmp/forge/ft12-review-fx/confinement/source-link repo=sourcelink unreadable=0 complete_text=false leak=false outside_path=false
   {
     "error_type": "feature-thread-conventions-file-escapes-workspace",
     "error": "the convention set at .clj-surgeon/feature-thread.edn resolves outside the workspace root. It was NOT read. The resolved location is deliberately NOT named here: it is outside the workspace this call was scoped to.",
     "workspace_root": "/var/tmp/forge/ft12-review-fx/confinement/convlink",
     "conventions_source": ".clj-surgeon/feature-thread.edn",
     "resolved_target": null
   }
   ```

7. `src/clj_surgeon/mcp_feature_thread.clj:741` and `test/clj_surgeon/mcp_feature_thread_test.clj:2925` — exact range-byte semantics VERIFIED LIVE for CRLF, no trailing LF, final line with LF, and UTF-8 BOM. In every case the published digest equals `sed -n 'from,to p'` byte-for-byte.

   Exact command:

   ```text
   perl -pi -e 's/\n/\r\n/g' /var/tmp/forge/ft12-review-fx/digest/js/crlf.js
   truncate -s -1 /var/tmp/forge/ft12-review-fx/digest/js/no-final-lf.js
   sed -i '1s/^/\xEF\xBB\xBF/' /var/tmp/forge/ft12-review-fx/digest/js/bom.js
   root=/var/tmp/forge/ft12-review-fx/digest; n=220; printf '%s\n' 'crlf|digestCRLF|js/crlf.js' 'no-final-lf|digestNoLF|js/no-final-lf.js' 'last-line-lf|digestLastLF|js/last-line-lf.js' 'bom|digestBOM|js/bom.js' | while IFS='|' read -r label subject file; do legs=$(jq -nc --arg f "$file" '[range(0;5) as $i|{id:("leg-"+($i|tostring)),kind:"js-function",globs:[$f]}]'); args=$(jq -nc --arg root "$root" --arg subject "$subject" --argjson legs "$legs" '{subject:$subject,budget_bytes:32768,scope:{workspace_root:$root},config:{"repo-label":"digest",legs:$legs}}'); /var/tmp/forge/ft12-review-fx/mcp-call.sh "$n" "$args" "/var/tmp/forge/ft12-review-fx/digest-$label.json"; row=$(jq -r '.result.structuredContent.legs[]|select(.id=="leg-0")|[.status,.file,.from,.to,.sha256,.boundary]|@tsv' "/var/tmp/forge/ft12-review-fx/digest-$label.json"); IFS=$'\t' read -r status gotfile from to claimed boundary <<< "$row"; actual=$(sed -n "${from},${to}p" "$root/$gotfile" | sha256sum | cut -d' ' -f1); printf '%s status=%s range=%s-%s boundary=%s match=%s claimed=%s actual=%s\n' "$label" "$status" "$from" "$to" "$boundary" "$([ "$claimed" = "$actual" ] && printf yes || printf no)" "$claimed" "$actual"; n=$((n+1)); done
   ```

   Verbatim output:

   ```text
   crlf status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes claimed=14ecb5debd83becfc88f3596e72cf2f4ae7565182f154b5cf2a6eafa2b35097a actual=14ecb5debd83becfc88f3596e72cf2f4ae7565182f154b5cf2a6eafa2b35097a
   no-final-lf status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes claimed=15621f2557c0bedb2225d1e88f2104dcc15a1808af473652322055f7d1332c4c actual=15621f2557c0bedb2225d1e88f2104dcc15a1808af473652322055f7d1332c4c
   last-line-lf status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes claimed=65b88bbd1fb0831308fade060bc408044ac5ff1edb2344ce22b184f0481c7eaf actual=65b88bbd1fb0831308fade060bc408044ac5ff1edb2344ce22b184f0481c7eaf
   bom status=FOUND range=1-3 boundary=brace-window(lexed,closed) match=yes claimed=f466044439d888e3b4d0ed07161556fcb76eedb6d11d7bc8f9e74832103486ed actual=f466044439d888e3b4d0ed07161556fcb76eedb6d11d7bc8f9e74832103486ed
   ```

8. `src/clj_surgeon/mcp_feature_thread.clj:2995` and `test/clj_surgeon/mcp_feature_thread_test.clj:3025` — the every-leaf backstop VERIFIED on 58 independent live HTTP receipts with independently renamed convention IDs. Every scalar leaf, including nested collection values and false values, is in the text; keyword/symbol values are JSON strings on the wire; null is not a scalar leaf. The internal `elide` key reaches neither face.

   Exact command:

   ```text
   for seed in $(seq 0 57); do config=$(jq -nc --argjson base "$base" --arg s "$seed" '$base | .["repo-label"]=("zqrepo"+$s) | .legs |= (to_entries|map(.value + {id:("zq"+$s+"leg"+(.key|tostring))}))'); args=$(jq -nc --argjson config "$config" '{subject:"formatDraft",also:["/api/transform/format","mechanical-format"],config:$config,budget_bytes:32768,scope:{workspace_root:"test-fixtures/feature-thread/smw-dequote"}}'); /var/tmp/forge/ft12-review-fx/mcp-call.sh "$((400+seed))" "$args" "/var/tmp/forge/ft12-review-fx/leaf-$seed.json"; done
   for seed in $(seq 0 57); do jq -r -f /var/tmp/forge/ft12-review-fx/check-leaves.jq "/var/tmp/forge/ft12-review-fx/leaf-$seed.json"; done | awk 'BEGIN{n=0;bad=0} {n++; if ($0 !~ /missing_leaf_count=0/) bad++} END{printf "receipts=%d receipts_with_missing_leaves=%d\n",n,bad}'
   jq -r '.result.structuredContent | [..|objects|select(has("elide"))]|"objects_with_elide=\(length)"' /var/tmp/forge/ft12-review-fx/leaf-0.json
   ```

   Verbatim output:

   ```text
   receipts=58 receipts_with_missing_leaves=0
   missing_leaf_count=0 missing=[]
   objects_with_elide=0
   ```

9. `src/clj_surgeon/mcp_feature_thread.clj:3527` and `test/clj_surgeon/mcp_feature_thread_test.clj:1280` — the implementation, peers, governance, verification, and write-control enrichments are VERIFIED LIVE. The automatic implementation walk unions its own globs before scanning, finds a seed definition outside declared leg globs, names both definitions when duplicated, and dedupes definitions already used by a handler/script leg as N/A. Tests are co-primary by language with the JS `test(...)` window. Governance rows compute parsed `form_end`/anchors and honestly say `unparsed` for the fixture's two lossy registry fragments. Verify rows are exact Makefile recipes with `@` split into `make_prefix`, and namespace pickup is derived from `tests.edn`. Request client/server fields agree. Classic script export is `none`. `next_call` is `admit_clojure_patch` with whole-file hashes and a computed-at clock.

   A 12-command menu (subject plus 11 peers) fits even at 11,266 B: every peer row survives with file/range/hash/refetch, ten tiny peer bodies ride, and the peer whose only definition is outside the definition glob is explicitly ABSENT by name. The structured-only client receives zero bodies but every locator/refetch, and every structured leaf is still in text.

   Exact commands:

   ```text
   for label in two-defs no-seed dedupe; do jq -r --arg label "$label" '.result.structuredContent as $r|($r.legs[]|select(.id=="implementation")) as $l|"\($label) thread=\($r.status) implementation=\($l.status) primary=\($l.file // "-"):L\($l.from // "-")-L\($l.to // "-") reason=\($l.reason // "-") also=\([$l.also[]?|(.file+":L"+(.from|tostring)+"-L"+(.to|tostring))]|join(","))"' "/var/tmp/forge/ft12-review-fx/impl-$label.json"; done
   jq -r '.result.structuredContent as $r | "co_primaries=" + (($r.legs[]|select(.id=="tests")|.co_primaries|map(.language+":"+.file+":L"+(.from|tostring)+"-L"+(.to|tostring)))|join(",")), "request_contract="+($r.rules.request_contract|tostring), "next_tool="+$r.next_call.tool+" computed_at="+$r.next_call.computed_at' /var/tmp/forge/ft12-review-fx/t1.json
   for b in default 11266; do jq -r --arg b "$b" '.result.structuredContent as $r|"budget=\($b) error=\($r.error_type // "none") status=\($r.status) text_bytes=\($r.text_bytes) peer_count=\($r.rules.peers|length) peer_bodies=\([$r.rules.peers[]|select(.body_in_text==true)]|length) absent=\([$r.rules.peers[]|select(.status=="ABSENT")|.name]|join(",")) rows_complete=\([$r.rules.peers[]|select(.status=="FOUND")|select((has("file") and has("from") and has("to") and has("sha256") and has("refetch"))|not)]|length==0)"' "/var/tmp/forge/ft12-review-fx/peer-$b.json"; done
   jq -r '.result.structuredContent | "structured_objects_with_body="+([..|objects|select(has("body"))]|length|tostring)+" located_without_refetch="+([..|objects|select((.status?=="FOUND") and (has("file")))|select((has("from") and has("sha256") and has("refetch"))|not)]|length|tostring)' /var/tmp/forge/ft12-review-fx/peer-default.json
   ```

   Verbatim output:

   ```text
   two-defs thread=COMPLETE (6 of 6) implementation=FOUND primary=src/writer/handlers/transform.clj:L81-L132 reason=- also=src/writer/other/dup.clj:L3-L4
   no-seed thread=INCOMPLETE (3 of 5) implementation=N/A primary=-:L--L- reason=the definition of formatDraft is already a leg of this receipt (resources/public/js/editor-commands.js:L389-L454) also=
   dedupe thread=INCOMPLETE (2 of 5) implementation=N/A primary=-:L--L- reason=the definition of handle-format is already a leg of this receipt (src/writer/handlers/transform.clj:L606-L680) also=
   co_primaries=js:test/js/browser_runtime_classic_script_test.js:L63-L94
   request_contract={"route":"/api/transform/format","handler_reads":["sync"],"js_posts":["sync"],"agree?":true,"only_in_js":[],"only_in_handler":[]}
   next_tool=admit_clojure_patch computed_at=2026-09-04T20:55:07.847617780Z
   budget=default error=none status=COMPLETE (5 of 5) text_bytes=8781 peer_count=11 peer_bodies=10 absent=hiddenCmd rows_complete=true
   budget=11266 error=none status=COMPLETE (5 of 5) text_bytes=8781 peer_count=11 peer_bodies=10 absent=hiddenCmd rows_complete=true
   structured_objects_with_body=0 located_without_refetch=0
   missing_leaf_count=0 missing=[]
   ```

10. `src/clj_surgeon/mcp_feature_thread.clj:4229` and `src/clj_surgeon/mcp_admit_patch.clj:1` — the printed range assertion is advisory, while the returned `admit_clojure_patch` whole-file pre-images are the actual control. A deliberately stale all-zero digest was submitted through the live server in commit mode: the gate refused `source-hash-mismatch`, attempted no mutation, committed false, and the fixture's whole-file SHA stayed byte-identical. Thus the control is enforced, not merely printed.

   Exact command:

   ```text
   before=$(sha256sum /var/tmp/forge/ft12-review-fx/smw/src/writer/routes.clj | cut -d' ' -f1); args=$(jq -nc --arg root /var/tmp/forge/ft12-review-fx/smw --arg patch "$patch" '{workspace_root:$root,patch:$patch,mode:"commit",verify:"none",expect_pre_sha256:{"src/writer/routes.clj":"0000000000000000000000000000000000000000000000000000000000000000"}}'); payload=$(jq -nc --argjson args "$args" '{jsonrpc:"2.0",id:240,method:"tools/call",params:{name:"admit_clojure_patch",arguments:$args}}'); curl -sS --max-time 120 -X POST http://127.0.0.1:8126/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H 'Mcp-Session-Id: 44e24c32-b46c-4c94-86ce-cccc98f09676' --data "$payload" | sed -n 's/^data: //p'; after=$(sha256sum /var/tmp/forge/ft12-review-fx/smw/src/writer/routes.clj | cut -d' ' -f1)
   ```

   Verbatim output:

   ```text
   admit_clojure_patch refused · source-hash-mismatch · 194.91 ms
   The workspace moved since the preview that authorized this commit: src/writer/routes.clj
   source unchanged
   error-type=source-hash-mismatch · mutation_attempted=false · committed=false
   expected-hash=0000000000000000000000000000000000000000000000000000000000000000
   actual-hash=6befbc7077ed62698e939f5461b8c972fc60ce23f57e9af68ba25763421491c9
   before=6befbc7077ed62698e939f5461b8c972fc60ce23f57e9af68ba25763421491c9 after=6befbc7077ed62698e939f5461b8c972fc60ce23f57e9af68ba25763421491c9 unchanged=true
   ```

11. `/home/forge/tmp/replay/smw-base:HEAD` and `test/clj_surgeon/mcp_feature_thread_test.clj:2248` — real-repository recall VERIFIED LIVE and byte-for-byte identical in status/file/range to the fixture at SMW `2df99c98`. `saveDraft` selects the real parsed route entry at L2121, never its handler docstring. `expound` finds the restored `app-safe.js` function but honestly has no route/handler/tests (2/5). `openTransformFromSelection` is correctly 4/5: the only test occurrences are assertion strings, which are useful CANDIDATE leads but cannot establish ownership or completion.

   Exact command:

   ```text
   git -C /home/forge/tmp/replay/smw-base rev-parse HEAD
   config=$(bb -e '(require (quote [cheshire.core :as json]) (quote [clojure.edn :as edn])) (print (json/generate-string (edn/read-string (slurp "test-fixtures/feature-thread/smw-dequote/.clj-surgeon/feature-thread.edn"))))'); n=600; printf '%s\n' 'fixture|/var/tmp/forge/ft12-review-fx/smw' 'real|/home/forge/tmp/replay/smw-base' | while IFS='|' read -r where root; do printf '%s\n' 'formatDraft|["/api/transform/format","mechanical-format"]' 'saveDraft|["/api/save"]' 'openTransformFromSelection|["/api/transform/apply"]' 'expound|[]' | while IFS='|' read -r subject alsojson; do args=$(jq -nc --arg root "$root" --arg subject "$subject" --argjson also "$alsojson" --argjson config "$config" '{subject:$subject,also:$also,budget_bytes:32768,scope:{workspace_root:$root},config:$config}'); /var/tmp/forge/ft12-review-fx/mcp-call.sh "$n" "$args" "/var/tmp/forge/ft12-review-fx/recall-$where-$subject.json"; jq -r --arg where "$where" '.result.structuredContent as $r|"\($where)/\($r.subject) status=\($r.status) "+([$r.legs[]|(.id+"="+.status+":"+(.file // "-")+":L"+((.from // "-")|tostring)+"-L"+((.to // "-")|tostring))]|join(" "))' "/var/tmp/forge/ft12-review-fx/recall-$where-$subject.json"; n=$((n+1)); done; done
   ```

   Verbatim output:

   ```text
   2df99c989e2dc1963161c13f7a341847c16b4deb
   fixture/formatDraft status=COMPLETE (6 of 6) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L389-L454 route=FOUND:src/writer/routes.clj:L2148-L2148 handler=FOUND:src/writer/handlers/transform.clj:L606-L680 tests=FOUND:test/writer/handlers/transform_apply_test.clj:L349-L384 implementation=FOUND:src/writer/handlers/transform.clj:L81-L132
   fixture/saveDraft status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L98-L101 js-function=FOUND:resources/public/js/editor-controller.js:L505-L571 route=FOUND:src/writer/routes.clj:L2121-L2121 handler=FOUND:src/writer/handlers/book_workshop.clj:L1922-L1985 tests=FOUND:test/js/editor_conflict_quarantine_test.js:L185-L199 implementation=N/A:-:L--L-
   fixture/openTransformFromSelection status=INCOMPLETE (4 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L332-L344 route=FOUND:src/writer/routes.clj:L2144-L2144 handler=FOUND:src/writer/handlers/chat.clj:L324-L352 tests=CANDIDATE:test/writer/spa_lint_test.clj:L509-L521 implementation=N/A:-:L--L-
   fixture/expound status=INCOMPLETE (2 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/app-safe.js:L342-L368 route=ABSENT:-:L--L- handler=ABSENT:-:L--L- tests=ABSENT:-:L--L- implementation=N/A:-:L--L-
   real/formatDraft status=COMPLETE (6 of 6) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L389-L454 route=FOUND:src/writer/routes.clj:L2148-L2148 handler=FOUND:src/writer/handlers/transform.clj:L606-L680 tests=FOUND:test/writer/handlers/transform_apply_test.clj:L349-L384 implementation=FOUND:src/writer/handlers/transform.clj:L81-L132
   real/saveDraft status=COMPLETE (5 of 5) menu-caller=FOUND:src/writer/views/components.clj:L98-L101 js-function=FOUND:resources/public/js/editor-controller.js:L505-L571 route=FOUND:src/writer/routes.clj:L2121-L2121 handler=FOUND:src/writer/handlers/book_workshop.clj:L1922-L1985 tests=FOUND:test/js/editor_conflict_quarantine_test.js:L185-L199 implementation=N/A:-:L--L-
   real/openTransformFromSelection status=INCOMPLETE (4 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/editor-commands.js:L332-L344 route=FOUND:src/writer/routes.clj:L2144-L2144 handler=FOUND:src/writer/handlers/chat.clj:L324-L352 tests=CANDIDATE:test/writer/spa_lint_test.clj:L509-L521 implementation=N/A:-:L--L-
   real/expound status=INCOMPLETE (2 of 5) menu-caller=FOUND:src/writer/views/components.clj:L102-L113 js-function=FOUND:resources/public/js/app-safe.js:L342-L368 route=ABSENT:-:L--L- handler=ABSENT:-:L--L- tests=ABSENT:-:L--L- implementation=N/A:-:L--L-
   ```

12. `test/clj_surgeon/mcp_feature_thread_test.clj:2932` and `src/clj_surgeon/mcp_feature_thread.clj:1344` — the round-ten landing closures are causal, not decorative. On independent `git archive` exports, reversing only each GREEN implementation hunk makes the current 69-test feature-thread namespace fail by exactly 33 (whole-literal strength), 6 (path-as-spelled/redaction), and 5 (derived floor) while the unmodified namespace is 69/2266/0.

   Exact history commands:

   ```text
   git log --oneline 3dfe0895..508f26f5 -- src/clj_surgeon/mcp_feature_thread.clj test/clj_surgeon/mcp_feature_thread_test.clj docs/intent/feature-thread/feature-thread-specs.md
   git show origin/MCP/main:docs/observations/feature-thread-round9-review-sol.md | sed -n '1p;$p'
   ```

   Verbatim output:

   ```text
   4d51ebc3 fix(feature-thread): GREEN — the ranges-only floor is 11266, measured from the named fixture (round-9 finding 5)
   3bebc9e8 test(feature-thread): RED — round-9 finding 5, the documented ranges-only floor refuses the receipt it names
   ea71847f fix(feature-thread): GREEN — a refusal names the path AS SPELLED, never where it resolves to (round-9 finding 4)
   fba27341 test(feature-thread): RED — round-9 finding 4, the conventions-symlink refusal publishes the resolved out-of-workspace target
   b60631ce fix(feature-thread): GREEN — a string literal is strong only when the WHOLE literal is code (round-9 finding 3)
   dca95885 test(feature-thread): RED — round-9 finding 3, prose inside a string literal is promoted to FOUND
   ## NO-GO
   This tip is not GO on its own for MCP/main: the merge-tree is clean, but it can still publish `COMPLETE (5 of 5)` with five false FOUND/anchor rows sourced only from a prose string, and its symlink refusal publishes an out-of-workspace path; the 11,264-byte floor claim is also stale.
   ```

   Exact commands (run once for each named export, with `D` set as shown):

   ```text
   D=/var/tmp/forge/ft12-review-fx/sab-string; cd "$D"; CP=$(clojure -Spath -M:clj-surgeon/mcp-test); env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run java -cp "$CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (System/exit (+ (:fail r) (:error r))))"
   D=/var/tmp/forge/ft12-review-fx/sab-path; cd "$D"; CP=$(clojure -Spath -M:clj-surgeon/mcp-test); env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run java -cp "$CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (System/exit (+ (:fail r) (:error r))))"
   D=/var/tmp/forge/ft12-review-fx/sab-floor; cd "$D"; CP=$(clojure -Spath -M:clj-surgeon/mcp-test); env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run java -cp "$CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (System/exit (+ (:fail r) (:error r))))"
   ```

   Verbatim terminal summaries:

   ```text
   exit=33
   Ran 69 tests containing 2266 assertions.
   33 failures, 0 errors.
   exit=6
   Ran 69 tests containing 2266 assertions.
   6 failures, 0 errors.
   exit=5
   Ran 69 tests containing 2248 assertions.
   5 failures, 0 errors.
   ```

13. `src/clj_surgeon/mcp_tool.clj:1964`, `src/clj_surgeon/workspace_onboarding.clj:297`, `test/clj_surgeon/mcp_http_server_test.clj:273`, and `test/mcp_stdio_smoke.clj:94` — the round-eleven merge unions are intact. The remerge reconstruction names exactly eight conflicted files; the current source and all catalog/onboarding/HTTP/admit/intent/server/stdio witnesses retain both trunk's `relation_census` and the lane's `feature_thread`. The two silent HTTP `:tool-count` literals are 9 during the deliberately injected tool and 8 after restoration. The managed onboarding block intentionally exposes seven tools and excludes only the write gate `admit_clojure_patch`. No assertion was weakened: exact ordered vectors/sets and counts replaced the former literals. A fresh detached clone at the reviewed SHA advertises the canonical eight names over stdio.

   Exact commands:

   ```text
   git show --remerge-diff --stat --oneline 1e108647
   git show --stat --oneline 2ae1b0c9
   git clone --no-local /home/forge/tmp/sol/ft1-wt /var/tmp/forge/ft12-review-fx/fresh-clone
   git -C /var/tmp/forge/ft12-review-fx/fresh-clone checkout --detach 508f26f5531ff1bd359c5fb221c0558fd3fbc2f9
   git -C /var/tmp/forge/ft12-review-fx/fresh-clone rev-parse HEAD
   git -C /var/tmp/forge/ft12-review-fx/fresh-clone status --short --branch
   cd /var/tmp/forge/ft12-review-fx/fresh-clone && env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp make mcp-smoke
   ```

   Verbatim output:

   ```text
   1e108647 merge: origin/MCP/main (c3ab91c909e5e43789af735506e9bc65654414df) into bridge/feature-thread-verb
    src/clj_surgeon/mcp_tool.clj                   |  6 +-----
    src/clj_surgeon/workspace_onboarding.clj       |  6 +-----
    test/clj_surgeon/admit_patch_test.clj          |  7 +------
    test/clj_surgeon/mcp_http_server_test.clj      | 14 ++------------
    test/clj_surgeon/mcp_intent_contract_test.clj  |  5 +----
    test/clj_surgeon/mcp_server_test.clj           | 18 ++++--------------
    test/clj_surgeon/workspace_onboarding_test.clj |  6 +-----
    test/mcp_stdio_smoke.clj                       | 20 ++++----------------
    8 files changed, 15 insertions(+), 67 deletions(-)
   2ae1b0c9 fix(merge): the live-tool-sync :tool-count literals the merge left stale
    test/clj_surgeon/mcp_http_server_test.clj | 4 ++--
    1 file changed, 2 insertions(+), 2 deletions(-)
   508f26f5531ff1bd359c5fb221c0558fd3fbc2f9
   ## HEAD (no branch)
   bb test/mcp_stdio_smoke.clj
   {:ok true, :operation :mcp-stdio-smoke, :server "clj-surgeon", :tools ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure" "relation_census" "alias_migration" "admit_clojure_patch" "feature_thread"], :response-count 3, :wall-ms 9633.39485}
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp
   Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp
   clj-surgeon MCP: ready — telemetry off
   ```

14. `Makefile:189`, `Makefile:193`, `Makefile:248`, `Makefile:957`, and `test/clj_surgeon/mcp_feature_thread_test.clj:1` — all landing gates are GREEN. Two complete full JVM executions produce the claimed 988/17487/0 plus the one explicitly counted trunk-owned precondition skip; a preliminary heavily contended run was killed with exit 143 before any terminal summary and is not represented as a pass. The focused namespace is 69/2266/0, Babashka is 840/6919/0, the committed census battery is 27/1336/0, the oracle and hygiene pass, and the audit is 478 specifications / zero violations with THREAD-001 through THREAD-052 present. The feature-thread description no longer instructs a pre-edit range re-hash; it says the per-leg assertion is advisory and the write-time whole-file gate binds.

   Exact commands:

   ```text
   env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
   FT12_CP=$(clojure -Spath -M:clj-surgeon/mcp-test); env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run java -cp "$FT12_CP" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-feature-thread-test)(let [r (clojure.test/run-tests 'clj-surgeon.mcp-feature-thread-test)] (System/exit (+ (:fail r) (:error r))))"
   env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run bb test/run_all.clj
   env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp make mcp-operation-oracle
   env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp make repository-hygiene
   env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp bb -cp src:test -e "(require 'clj-surgeon.mcp-intent-contract) (let [r (clj-surgeon.mcp-intent-contract/audit-current-repository)] (prn {:ok (:ok r) :specs (count (:specs r)) :violations (:violations r)}))"
   rg -o 'MCP-OP-THREAD-[0-9]{3}' docs/intent/feature-thread/feature-thread-specs.md | sort -u | tee /var/tmp/forge/ft12-review-fx/thread-spec-ids.txt | tail -3
   wc -l /var/tmp/forge/ft12-review-fx/thread-spec-ids.txt
   FT12_CP=$(clojure -Spath -M:clj-surgeon/mcp-test); env TMPDIR=/var/tmp/forge/ft12-review-fx/tmp JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/ft12-review-fx/tmp ~/bin/suite-run java -cp "$FT12_CP" clojure.main -m census-witness-battery
   ```

   Verbatim terminal summaries:

   ```text
   Ran 988 tests containing 17487 assertions.
   0 failures, 0 errors.
   1 preconditions skipped.
     SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   0 preconditions failed.

   Ran 988 tests containing 17487 assertions.
   0 failures, 0 errors.
   1 preconditions skipped.
     SKIPPED · no battery receipt at target/admit-transaction-recovery-battery-receipt.edn · run `make admit-transaction-recovery-battery` to prove :transaction-recovery-required by execution rather than by the structural checks alone
   0 preconditions failed.

   Ran 69 tests containing 2266 assertions.
   0 failures, 0 errors.

   Ran 840 tests containing 6919 assertions.
   0 failures, 0 errors.

   mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
   repository hygiene: no machine-local build cache is tracked at any depth
   {:ok true, :specs 478, :violations []}
   MCP-OP-THREAD-050
   MCP-OP-THREAD-051
   MCP-OP-THREAD-052
   52 /var/tmp/forge/ft12-review-fx/thread-spec-ids.txt
   :BATTERY-RESULT {:test 27, :pass 1336, :fail 0, :error 0}
   ```

15. `repository:merge-tree` — after fetching the actual current remote, `origin/MCP/main` is `5b531d3b709b64fcae0dfcc9398942fe795da145`, newer than the round-eleven comparison tip, and the required dry-run merge is still clean. The reviewed checkout is still detached and source-clean. The explicit server on 8126 was stopped; no listener remains on 8126–8128.

   Exact commands:

   ```text
   git fetch origin MCP/main
   git rev-parse origin/MCP/main
   git merge-tree --write-tree HEAD origin/MCP/main
   printf 'MERGE_TREE_EXIT=%s\n' "$?"
   git status --short --branch
   ss -ltn '( sport >= :8126 and sport <= :8128 )'
   ```

   Verbatim output:

   ```text
   From https://github.com/realgenekim/clj-surgeon
    * branch              MCP/main   -> FETCH_HEAD
      98d6ba46..5b531d3b  MCP/main   -> origin/MCP/main
   5b531d3b709b64fcae0dfcc9398942fe795da145
   82e24a272ff36b77bba28460aa943710efe63172
   MERGE_TREE_EXIT=0
   ## HEAD (no branch)
   State Recv-Q Send-Q Local Address:Port Peer Address:PortProcess
   ```

16. `/var/tmp/forge/ft12-review-fx` — the review fixture tree was resolved to the exact authorized target and removed after the server and all gates completed. The source checkout remains clean.

   Exact command:

   ```text
   realpath /var/tmp/forge/ft12-review-fx
   test "$(realpath /var/tmp/forge/ft12-review-fx)" = /var/tmp/forge/ft12-review-fx
   find /var/tmp/forge/ft12-review-fx -depth -delete
   if [ -e /var/tmp/forge/ft12-review-fx ]; then printf 'fixture_removed=no\n'; else printf 'fixture_removed=yes\n'; fi
   git status --short --branch
   ```

   Verbatim output:

   ```text
   /var/tmp/forge/ft12-review-fx
   fixture_removed=yes
   ## HEAD (no branch)
   ```

## GO

This tip is GO on its own for MCP/main: every live receipt and refusal attack held, every landing gate is green, and `git merge-tree --write-tree HEAD origin/MCP/main` is conflict-free against current trunk `5b531d3b`.
