# Frequent friction patterns

Ranked by call count; timeout-only patterns enter only when they also have fallback. Examples are literal retained excerpts with paths redacted; coverage is a keyword lead, not proof that an entry fixes the case.

## 1. `cat` / success / native-edit

Count: 90

covers: NS-SPLIT-021 (profile, verification); NS-SPLIT-030 (profile, verification); NS-SPLIT-036 (catalog, profile); NS-SPLIT-041 (profile, verification, require); NS-SPLIT-048 (profile, verification); NS-SPLIT-061 (comment, require)

Example 1: codex, `rollout-2026-09-03T06-47-32-01a06605-d069-7a23-af53-26f46b22a873.jsonl`, `codex:05537e27d8e5:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/intent_transactio","workdir":"~/tmp/sol/q5z6-wt","yield_time_ms":10000,"max_output_tokens":12000}
{"name":"apply_patch","input":"<unresolved:patch>"}
```

Example 2: codex, `rollout-2026-09-04T06-09-55-01a06b09-bbf3-7ad3-8b02-0b7e5a57dab4.jsonl`, `codex:e415b6e9f627:1`, 2026-09-04

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/mcp_feature_threa","workdir":"~/tmp/sol/ft1-wt","yield_time_ms":10000,"max_output_tokens":30000}
{"name":"apply_patch","input":"<unresolved:patch>"}
```

## 2. `cat` / refusal / surgeon-same

Count: 53

covers: NS-SPLIT-024 (continuation); NS-SPLIT-025 (continuation); NS-SPLIT-032 (continuation)

Example 1: codex, `rollout-2026-09-03T04-50-49-01a0659a-f42d-7c23-8fa6-842413c18d35.jsonl`, `codex:52289a4f8ae1:1`, 2026-09-03

```text
{"cmd":"printf '%s\\n' '{:reads [{:file \"src/clj_surgeon/memory_batte","workdir":"~/tmp/sol/membat-wt","yield_time_ms":30000,"max_output_tokens":60000}
Script completed Wall time 0.2 seconds Output:  {:operation :show-form,  :selector {:spec :cross-file},  :error "Declared read counts do not match the manifest",  :error-type :read-expectation-mismatch,  :expected {:file-count 3, :form-count 46},  :actual {:file-count 3, :form-count 47}} 
```

Example 2: codex, `rollout-2026-09-03T04-51-48-01a0659b-dcc2-7e63-ba4d-66677b50f842.jsonl`, `codex:891374dcccb7:1`, 2026-09-03

```text
{"cmd":"printf '%s\\n' '{:reads [{:file \"src/clj_surgeon/mcp_alias_mi","workdir":"~/tmp/sol/q5z-wt","yield_time_ms":30000,"max_output_tokens":50000}
Script completed Wall time 0.9 seconds Output:  {:operation :show-form,  :selector {:spec :cross-file},  :error "Declared read counts do not match the manifest",  :error-type :read-expectation-mismatch,  :expected {:file-count 1, :form-count 18},  :actual {:file-count 1, :form-count 17}} {:operation
```

## 3. `cat` / success / verify

Count: 47

covers: NS-SPLIT-021 (profile, verification); NS-SPLIT-030 (profile, verification); NS-SPLIT-041 (profile, verification); NS-SPLIT-048 (profile, verification); NS-SPLIT-066 (comment, verification)

Example 1: codex, `rollout-2026-09-03T07-11-29-01a0661b-c0be-7f03-b5d6-a855c3a70a85.jsonl`, `codex:f6c607cad3a0:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file test/clj_surgeon/mcp_http_server_","workdir":"~/tmp/sol/census5-wt","yield_time_ms":10000,"max_output_tokens":30000}
{"name":"exec_command","input":{"cmd":"clojure -X:clj-surgeon/mcp :port 7908","workdir":"~/tmp/sol/census5-wt","yield_time_ms":1000,"max_output_tokens":12000,"tty":true}}
```

Example 2: codex, `rollout-2026-09-03T07-11-29-01a0661b-c0be-7f03-b5d6-a855c3a70a85.jsonl`, `codex:f6c607cad3a0:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/census_discovery.","workdir":"~/tmp/sol/census5-wt","yield_time_ms":10000,"max_output_tokens":12000}
{"name":"exec_command","input":{"cmd":"cd /tmp/census5-fx/deep/root\nCENSUS_DEPTH=0\nwhile [ \"$CENSUS_DEPTH\" -lt 1000 ]; do cd d; CENSUS_DEPTH=$((CENSUS_DEPTH + 1)); done\nrm bottom.clj\nwhile [ \"$CENSUS_DEPTH\" -lt 1700 ]; do mkdir d; cd d; CENSUS_DEPTH=$((CENSUS_DEPTH + 1)); done\ncp /tmp/censu
```

## 4. `cat` / success / other

Count: 35

covers: NS-SPLIT-021 (profile, verification); NS-SPLIT-030 (profile, verification); NS-SPLIT-041 (profile, verification, require); NS-SPLIT-048 (profile, verification); NS-SPLIT-061 (comment, require); NS-SPLIT-066 (comment, verification)

Example 1: codex, `rollout-2026-09-03T06-07-55-01a065e1-8b2f-7d13-a5f1-2c8d438de0e6.jsonl`, `codex:774ae41b161a:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/mcp_alias_migrati","workdir":"~/tmp/sol/q5z5-wt","yield_time_ms":30000,"max_output_tokens":10000}
{"name":"exec_command","input":{"cmd":"mkdir -p /tmp/q5z5-fx","workdir":"~/tmp/sol/q5z5-wt","yield_time_ms":10000,"max_output_tokens":2000}}
```

Example 2: codex, `rollout-2026-09-03T11-42-59-01a06714-5159-7123-91ec-d184ae2f031b.jsonl`, `codex:094ad7435b45:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file test/clj_surgeon/mcp_relation_cen","workdir":"~/tmp/sol/census11-wt","yield_time_ms":10000,"max_output_tokens":50000}
{"name":"exec_command","input":{"cmd":"if [ -e /tmp/census11-sol-fx ]; then find /tmp/census11-sol-fx -maxdepth 2 -printf '%y %p\\n' | sort; else echo absent; fi","workdir":"~/tmp/sol/census11-wt","yield_time_ms":10000,"max_output_tokens":10000}}
```

## 5. `cat` / refusal / verify

Count: 22

covers: none found

Example 1: codex, `rollout-2026-09-03T04-31-28-01a06589-3f26-70f0-ad94-736134dc620c.jsonl`, `codex:27182edca971:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/mcp_test_runner.c","workdir":"~/tmp/sol/ratchets-wt","yield_time_ms":10000,"max_output_tokens":15000}
Script completed Wall time 0.4 seconds Output:  {:operation :show-form,  :file "src/clj_surgeon/mcp_test_runner.clj",  :selector {:contains "defn -main"},  :error  "Cannot read source file: src/clj_surgeon/mcp_test_runner.clj (No such file or directory)",  :error-type :file-read-failed} {:operation 
```

Example 2: codex, `rollout-2026-09-03T04-31-28-01a06589-3f26-70f0-ad94-736134dc620c.jsonl`, `codex:27182edca971:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/mcp_test_runner.c","workdir":"~/tmp/sol/ratchets-wt","yield_time_ms":10000,"max_output_tokens":15000}
Script completed Wall time 0.4 seconds Output:  {:operation :show-form,  :file "src/clj_surgeon/mcp_test_runner.clj",  :selector {:contains "defn -main"},  :error  "Cannot read source file: src/clj_surgeon/mcp_test_runner.clj (No such file or directory)",  :error-type :file-read-failed} {:operation 
```

## 6. `split-ns!` / unknown / native-read

Count: 22

covers: none found

Example 1: claude, `agent-a00712ad5f157c4ca.jsonl`, `claude:a969bd5a11ff:1`, 2026-09-08

```text
{"command":"cd ~/src/cc-row5-X4 && TMPDIR=/var/tmp/forge ~/bin/clj-surge","description":"Run the single split-ns! call","timeout":600000}
{"name":"Bash","input":{"command":"cd /var/tmp/forge/row5-adopt-2/X4 && head -c 3000 e2-receipt.edn; echo; echo \"=== GREP ===\"; grep -o ':state \"[^\"]*\"' e2-receipt.edn | head; grep -o ':committed [a-z]*' e2-receipt.edn | head; grep -o ':ok [a-z]*' e2-receipt.edn | head; grep -o ':mutation_attem
```

Example 2: claude, `agent-a0572a1d9a9efc5f4.jsonl`, `claude:396db963208f:1`, 2026-09-08

```text
{"command":"cd ~/src/cc-row5-E1 && TMPDIR=/var/tmp/forge ~/bin/clj-surge","description":"Run single split-ns! call","timeout":600000}
{"name":"Bash","input":{"command":"cd /var/tmp/forge/row5-adopt/E1 && head -c 1200 e-receipt.edn; echo; echo \"=== grep state ===\"; grep -o ':state \"[^\"]*\"' e-receipt.edn | head; grep -o ':ok [a-z]*' e-receipt.edn | head; grep -o ':committed [a-z]*' e-receipt.edn | head; grep -o ':mutation_attem
```

## 7. `cat` / unknown / native-edit

Count: 19

covers: none found

Example 1: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:55`, 2026-09-06

```text
{"cmd":"~/bin/clj-surgeon :op :cat :file src/clj_surgeon/mission_typ","workdir":"/var/tmp/forge/astra-typist-route-fx","max_output_tokens":2800}
{"name":"apply_patch","input":"*** Begin Patch\n*** Update File: /var/tmp/forge/astra-typist-route-fx/test/clj_surgeon/mission_typist_test.clj\n@@\n-(ns clj-surgeon.mission-typist-test\n+(ns clj-surgeon.mission-typist-test\n@@\n-            [clojure.test :refer [deftest is testing]]))\n+            
```

Example 2: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:55`, 2026-09-06

```text
{"cmd":"~/bin/clj-surgeon :op :cat :file src/clj_surgeon/mission_typ","workdir":"/var/tmp/forge/astra-typist-route-fx","max_output_tokens":2800}
{"name":"apply_patch","input":"*** Begin Patch\n*** Update File: /var/tmp/forge/astra-typist-route-fx/test/clj_surgeon/mission_typist_test.clj\n@@\n-(ns clj-surgeon.mission-typist-test\n+(ns clj-surgeon.mission-typist-test\n@@\n-            [clojure.test :refer [deftest is testing]]))\n+            
```

## 8. `cat` / unknown / verify

Count: 19

covers: none found

Example 1: codex, `rollout-2026-09-03T06-21-22-01a065ed-db0c-7662-a67d-61cbb25557ce.jsonl`, `codex:5184cc7be300:1`, 2026-09-03

```text
{"cmd":"rg -n --glob '*.clj' --glob '*.cljc' --glob '*.cljs' 'defn.*","workdir":"~/tmp/sol/readpath-wt","yield_time_ms":30000,"max_output_tokens":30000}
{"name":"exec_command","input":{"cmd":"nl -ba docs/intent/read-path-memory/read-path-memory-design.md; nl -ba docs/intent/read-path-memory/read-path-memory-specs.md; nl -ba test/clj_surgeon/outline_memory_test.clj; nl -ba test/clj_surgeon/outline_differential_test.clj","workdir":"~/tmp/sol/readpath-
```

Example 2: codex, `rollout-2026-09-03T21-28-39-01a0692c-81b7-7e03-be48-d03c108e0a0a.jsonl`, `codex:9f91a16e28ba:1`, 2026-09-03

```text
{"cmd":"printf '%s\\n' '{:reads [{:file \"src/clj_surgeon/relation_cen","workdir":"~/tmp/sol/census15-wt","yield_time_ms":30000,"max_output_tokens":40000}
{"name":"exec_command","input":{"cmd":"command -v clj-surgeon || true\nrg --files bin | sort | head -80\nrg -n \"clj-surgeon\" deps.edn Makefile | head -80","workdir":"~/tmp/sol/census15-wt","yield_time_ms":10000,"max_output_tokens":10000}}
```

## 9. `cat` / error / surgeon-same

Count: 18

covers: none found

Example 1: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:69`, 2026-09-06

```text
{"cmd":"nice -n 10 ~/bin/clj-surgeon :op :cat :file src/clj_surgeon/","workdir":"/var/tmp/forge/astra-typist-route-fx","max_output_tokens":3500}
{"name":"exec_command","input":{"cmd":"nice -n 10 ~/bin/clj-surgeon :op :cat :file test/clj_surgeon/mission_typist_test.clj :forms '[admission-and-rate-boundaries dossier-is-frozen-source-evidence]'; nice -n 10 ~/bin/clj-surgeon :op :cat :file test/clj_surgeon/mission_typist_executor_test.clj :form 
```

Example 2: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:69`, 2026-09-06

```text
{"cmd":"nice -n 10 ~/bin/clj-surgeon :op :cat :file src/clj_surgeon/","workdir":"/var/tmp/forge/astra-typist-route-fx","max_output_tokens":3500}
{"name":"exec_command","input":{"cmd":"nice -n 10 ~/bin/clj-surgeon :op :cat :file test/clj_surgeon/mission_typist_test.clj :forms '[admission-and-rate-boundaries dossier-is-frozen-source-evidence]'; nice -n 10 ~/bin/clj-surgeon :op :cat :file test/clj_surgeon/mission_typist_executor_test.clj :form 
```

## 10. `cat` / refusal / surgeon-other

Count: 18

covers: none found

Example 1: codex, `rollout-2026-09-03T04-31-28-01a06589-3f26-70f0-ad94-736134dc620c.jsonl`, `codex:27182edca971:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/mcp_tool.clj :for","workdir":"~/tmp/sol/ratchets-wt","yield_time_ms":10000,"max_output_tokens":40000}
Script completed Wall time 0.2 seconds Output:  {:remedies  {:list-forms   {:operation :ls,    :reason "List available top-level forms and line ranges",    :command "clj-surgeon :op :ls :file src/clj_surgeon/mcp_tool.clj",    :command-args    ["clj-surgeon"     ":op"     ":ls"     ":file"     "src/c
```

Example 2: codex, `rollout-2026-09-03T04-31-28-01a06589-3f26-70f0-ad94-736134dc620c.jsonl`, `codex:27182edca971:1`, 2026-09-03

```text
{"cmd":"clj-surgeon :op :cat :file src/clj_surgeon/mcp_tool.clj :for","workdir":"~/tmp/sol/ratchets-wt","yield_time_ms":10000,"max_output_tokens":40000}
Script completed Wall time 0.2 seconds Output:  {:remedies  {:list-forms   {:operation :ls,    :reason "List available top-level forms and line ranges",    :command "clj-surgeon :op :ls :file src/clj_surgeon/mcp_tool.clj",    :command-args    ["clj-surgeon"     ":op"     ":ls"     ":file"     "src/c
```

## 11. `cat` / unknown / other

Count: 16

covers: none found

Example 1: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:58`, 2026-09-06

```text
{"cmd":"nice -n 10 ~/bin/clj-surgeon :op :cat :file src/clj_surgeon/","workdir":"/var/tmp/forge/astra-typist-route-fx","max_output_tokens":2500}
{"name":"send_message","input":{"target":"model_adapter","message":"gAAAAABqnLsL1_LwJO6duG-QGBJqTyhLbbn6cZWKvlqqD5voT9vi8xo-2av2h9xn1PgzXEW8TNSvtfmeuBfd2OU1vQPZiwSy1NucaQzyJDeTEYh3OEqCSU9ZL6A0UBk5s5f9fOIjFldSYCB4qoz0UmOnURY8bfo6KbsiPpa3aohk5n4EruLeslh9Zr9TXRs60RiVqiIOZRZDyXwBU9yCLKwDlKQ3Z2RQHYRsw9DH
```

Example 2: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:58`, 2026-09-06

```text
{"cmd":"nice -n 10 ~/bin/clj-surgeon :op :cat :file src/clj_surgeon/","workdir":"/var/tmp/forge/astra-typist-route-fx","max_output_tokens":1000}
{"name":"write_stdin","input":{"session_id":3168,"chars":"","yield_time_ms":1000,"max_output_tokens":1000}}
```

## 12. `admit_clojure_patch` / refusal / surgeon-same

Count: 12

covers: NS-SPLIT-021 (profile, verification); NS-SPLIT-030 (profile, verification); NS-SPLIT-041 (profile, verification); NS-SPLIT-048 (profile, verification)

Example 1: codex, `rollout-2026-09-04T08-22-51-01a06b83-7065-7700-ae3f-5e2e406b8bb7.jsonl`, `codex:98149bbbf90d:1`, 2026-09-04

```text
"<unresolved-object>"
Script completed Wall time 0.0 seconds Output:  admit_clojure_patch refused · unsupported-patch-target · 6.64 ms The gate admits Clojure and EDN sources only; apply these natively: resources/public/js/editor-commands.js, test/js/browser_runtime_classic_script_test.js source unchanged
```

Example 2: codex, `rollout-2026-09-04T08-22-51-01a06b83-7065-7700-ae3f-5e2e406b8bb7.jsonl`, `codex:98149bbbf90d:1`, 2026-09-04

```text
"<unresolved-object>"
Script completed Wall time 1.4 seconds Output:  admit_clojure_patch refused · verification-incomplete · 1367.07 ms Verification did not complete (partial: no-focused-test-profile); nothing was written. Run mode preview to see the same receipt without a write, and repair no-focused-test-profile befor
```

## 13. `admit_clojure_patch` / refusal / other

Count: 11

covers: NS-SPLIT-066 (comment, verification)

Example 1: codex, `rollout-2026-09-04T08-22-51-01a06b83-7065-7700-ae3f-5e2e406b8bb7.jsonl`, `codex:98149bbbf90d:1`, 2026-09-04

```text
"<unresolved-object>"
Script completed Wall time 0.0 seconds Output:  admit_clojure_patch refused · invalid-admit-request · 9.87 ms expect_pre_sha256 must name exactly the files the patch touches. This patch touches docs/intent/registry.edn, src/writer/handlers/transform.clj, src/writer/routes.clj, src/writer/views/compo
```

Example 2: codex, `rollout-2026-09-04T19-53-17-01a06dfb-8f7f-7b91-8d36-0d49f8ec8580.jsonl`, `codex:44dbe13ade9d:1`, 2026-09-04

```text
"<unresolved-object>"
Script completed Wall time 1.3 seconds Output:  admit_clojure_patch refused · verification-incomplete · 6000.42 ms Verification did not complete (unverified: verification-runner-failed); nothing was written. Run mode preview to see the same receipt without a write, and repair verification-runner-fai
```

## 14. `apply_clojure_changes` / success / verify

Count: 11

covers: none found

Example 1: claude, `agent-a1e88804dd2c2c620.jsonl`, `claude:a7a9381cfead:1`, 2026-09-07

```text
{"workspace_root":"~/src/cc-cellB-M1","extraction":{"file":"src/cfp_scheduler_killer/exports.clj","to":"src/cfp_scheduler_killer/exports/calendar.clj","forms":["calendar-prodid","ical-domain","ics-escape","fold-line","ics-date","ics-stamp","ics-uid","ics-sequence","ics-local-datetime","absolute-link
{"name":"Bash","input":{"command":"cd ~/src/cc-cellB-M1 && head -40 src/cfp_scheduler_killer/exports/calendar.clj && echo \"=== tail requires ===\" && sed -n '20,60p' src/cfp_scheduler_killer/exports/calendar.clj && echo \"=== exports.clj ns ===\" && sed -n '20,45p' src/cfp_scheduler_killer/exports.
```

Example 2: claude, `agent-a27500dd74762b9a2.jsonl`, `claude:d59385655a18:1`, 2026-09-07

```text
{"workspace_root":"~/src/cc-split-T2","extraction":{"file":"src/cfp_scheduler_killer/views.clj","to":"src/cfp_scheduler_killer/views/schedule.clj","forms":["day-tab","schedule-status-bar","conflict-chips","room-options","placed-card","block-card","schedule-grid","place-form","schedule-page","agenda-
{"name":"Bash","input":{"command":"cd ~/src/cc-split-T2 && export DOGFOOD_LOG=/var/tmp/forge/plan2/T2/stopwatch.log && /var/tmp/forge/dogfood-stamp.sh moves-complete && wc -l src/cfp_scheduler_killer/views.clj && cat src/cfp_scheduler_killer/views.clj && ls src/cfp_scheduler_killer/views/","descript
```

## 15. `inspect_clojure` / success / native-edit

Count: 11

covers: none found

Example 1: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:100`, 2026-09-06

```text
{"workspace_root":"/var/tmp/forge/astra-typist-route-fx","requests":[{"id":"routing","operation":"forms","file":"src/clj_surgeon/mission_typist.clj","forms":["route","candidate-count"],"expect":{"forms":2}}],"expect":{"requests":1,"files":1}}
{"name":"exec_command","input":{"cmd":"python3 - <<'PY'\nfrom pathlib import Path\nfrom datetime import datetime,timezone\nnow=datetime.now(timezone.utc).strftime('%H:%MZ');q=Path('/var/tmp/forge/quiet-window.md')\nassert not q.exists(),q.read_text() if q.exists() else ''\nq.write_text('owner=astra 
```

Example 2: claude, `agent-a1e88804dd2c2c620.jsonl`, `claude:a7a9381cfead:1`, 2026-09-07

```text
{"workspace_root":"~/src/cc-cellB-M1","mode":"plan-extraction","file":"src/cfp_scheduler_killer/exports.clj","to":"src/cfp_scheduler_killer/exports/calendar.clj","require_policy":"minimal","forms":["calendar-prodid","ical-domain","ics-escape","fold-line","ics-date","ics-stamp","ics-uid","ics-sequenc
{"name":"Bash","input":{"command":"cd /var/tmp/forge/plan2/cellB/M1 && cat > response-1.json <<'EOF'\n(saved inline in transcript; see response-1-full.json)\nEOF\ncat > request-2.json <<'EOF'\n{\n  \"workspace_root\": \"~/src/cc-cellB-M1\",\n  \"op\": \"helper_extraction\",\n  \"from\": {\"file\": \
```

## 16. `cat` / error / verify

Count: 10

covers: none found

Example 1: codex, `rollout-2026-09-04T22-07-14-01a06e76-314b-7ca1-8a26-347576dac6b7.jsonl`, `codex:4ccca23478cb:80`, 2026-09-06

```text
{"cmd":"mkdir -p /var/tmp/forge/outline-lane-split\n~/bin/clj-surgeon","workdir":"/var/tmp/forge/astra-typist-transport-fx","max_output_tokens":2600}
{"name":"exec_command","input":{"cmd":"rg --files docs | rg 'outline|test-isolation'\ncat /var/tmp/forge/outline-lane-split/witness.edn\ncat /var/tmp/forge/outline-lane-split/source-files.edn\ncat /var/tmp/forge/outline-lane-split/legacy.edn\nrg -n -A 8 -B 2 'test-deps|integration-test|test-fast' de
```

Example 2: codex, `rollout-2026-09-04T22-07-14-01a06e76-314b-7ca1-8a26-347576dac6b7.jsonl`, `codex:4ccca23478cb:80`, 2026-09-06

```text
{"cmd":"mkdir -p /var/tmp/forge/outline-lane-split\n~/bin/clj-surgeon","workdir":"/var/tmp/forge/astra-typist-transport-fx","max_output_tokens":2600}
{"name":"exec_command","input":{"cmd":"rg --files docs | rg 'outline|test-isolation'\ncat /var/tmp/forge/outline-lane-split/witness.edn\ncat /var/tmp/forge/outline-lane-split/source-files.edn\ncat /var/tmp/forge/outline-lane-split/legacy.edn\nrg -n -A 8 -B 2 'test-deps|integration-test|test-fast' de
```

## 17. `cat` / unknown / native-read

Count: 10

covers: none found

Example 1: codex, `rollout-2026-09-03T12-47-04-01a0674e-fcc6-7f33-9540-1fd3045c949b.jsonl`, `codex:02bb73bd63c6:1`, 2026-09-03

```text
{"cmd":"printf '%s\\n' '{:reads [{:file \"src/clj_surgeon/relation_cen","workdir":"~/tmp/sol/census12-wt","yield_time_ms":10000,"max_output_tokens":50000}
{"name":"exec_command","input":{"cmd":"find /tmp/census11-sol-fx -maxdepth 2 -type f -printf '%P\\n' | sort | head -200","workdir":"~/tmp/sol/census12-wt","yield_time_ms":10000,"max_output_tokens":20000}}
```

Example 2: codex, `rollout-2026-09-04T21-56-42-01a06e6c-8bff-7cc3-8402-14d16b3eca60.jsonl`, `codex:b3142519c37b:58`, 2026-09-06

```text
{"cmd":"nice -n 10 ~/bin/clj-surgeon :op :cat :file src/clj_surgeon/","workdir":"/var/tmp/forge/astra-typist-route-fx","max_output_tokens":5000}
{"name":"exec_command","input":{"cmd":"tail -16 /var/tmp/forge/fable-to-astra.md","max_output_tokens":2500}}
```

## 18. `split-ns!` / unknown / native-edit

Count: 10

covers: none found

Example 1: claude, `agent-a1b348c92898bd708.jsonl`, `claude:ef007a0d7202:1`, 2026-09-08

```text
{"command":"cd ~/src/cc-row5-E4 && TMPDIR=/var/tmp/forge ~/bin/clj-surge","description":"Run the single split-ns! call","timeout":600000}
{"name":"Bash","input":{"command":"cd /var/tmp/forge/row5-adopt/E4 && python3 - <<'PY'\nimport re\ns=open('e-receipt.edn').read()\nfor k in [':state',':ok ',':committed',':mutation_attempted',':verification_complete',':proof_pending',':counts',':unexpected_paths']:\n    for m in re.finditer(re.escap
```

Example 2: claude, `agent-a1d94f2f26685b405.jsonl`, `claude:a119a8536e9a:1`, 2026-09-08

```text
{"command":"cd ~/src/cc-row5-Y1 && TMPDIR=/var/tmp/forge ~/bin/clj-surge","description":"Run the single split-ns! call","timeout":420000}
{"name":"Bash","input":{"command":"cd /var/tmp/forge/row5-adopt-3/Y1 && python3 - <<'EOF'\nimport re\ns=open('e3-receipt.edn').read()\nfor k in [':state',':ok ',':committed',':mutation_attempted',':verification_complete',':proof_pending',':status']:\n    for m in re.finditer(re.escape(k), s):\n     
```

## 19. `apply_clojure_changes` / success / native-edit

Count: 9

covers: none found

Example 1: codex, `rollout-2026-09-06T10-36-13-01a0764a-4432-7430-9409-668fde2fecc9.jsonl`, `codex:5791745588ef:1`, 2026-09-06

```text
"<unresolved-expression>"
{"name":"apply_patch","input":"*** Begin Patch\n*** Update File: /var/tmp/forge/cell-prep/runner-b/cohort/D3/workspace/src/maven/db.clj\n@@\n-(def instance \"postgresql-483717:us-central1:postgres1\")\n+(defn execute! [& args]\n+  (apply jdbc/execute! args))\n+\n+(def instance \"postgresql-483717:us
```

Example 2: claude, `agent-a27500dd74762b9a2.jsonl`, `claude:d59385655a18:1`, 2026-09-07

```text
{"workspace_root":"~/src/cc-split-T2","extraction":{"file":"src/cfp_scheduler_killer/views.clj","to":"src/cfp_scheduler_killer/views/organizer_layout.clj","forms":["event-resume-path","event-setup-done?","sb-link","sb-out","sb-group","sidebar","dev-strip","breadcrumb","whoami-strip","organizer-shell
{"name":"Bash","input":{"command":"cd ~/src/cc-split-T2 && grep -rho \"views/[a-zA-Z0-9!?*<>=+.-]*\" src/cfp_scheduler_killer/server.clj test/cfp_scheduler_killer/{polish_test,forms_test,comms_test,views_test}.clj | sed 's|views/||' | sort -u > /var/tmp/forge/plan2/T2/external-refs.txt; cd /var/tmp/
```

## 20. `cat` / error / other

Count: 9

covers: none found

Example 1: codex, `rollout-2026-09-04T22-07-14-01a06e76-314b-7ca1-8a26-347576dac6b7.jsonl`, `codex:4ccca23478cb:63`, 2026-09-06

```text
{"cmd":"~/bin/clj-surgeon :op :cat :file src/clj_surgeon/mission_eve","workdir":"/var/tmp/forge/astra-typist-transport-fx","max_output_tokens":3000}
{"name":"write_stdin","input":{"session_id":72348,"chars":"","yield_time_ms":1000,"max_output_tokens":100}}
```

Example 2: codex, `rollout-2026-09-04T22-07-14-01a06e76-314b-7ca1-8a26-347576dac6b7.jsonl`, `codex:4ccca23478cb:63`, 2026-09-06

```text
{"cmd":"~/bin/clj-surgeon :op :cat :file src/clj_surgeon/mission_eve","workdir":"/var/tmp/forge/astra-typist-transport-fx","max_output_tokens":3000}
{"name":"write_stdin","input":{"session_id":72348,"chars":"","yield_time_ms":1000,"max_output_tokens":100}}
```
