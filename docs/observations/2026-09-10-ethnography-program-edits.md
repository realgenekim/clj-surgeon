# Session mining: when agents wrote a PROGRAM to edit Clojure

**Corpus.** 1,570 session transcripts / 1.54 GB — `/home/forge/.codex/sessions` (827 Codex
rollouts) and `/home/forge/.claude/projects` (743 Claude sessions, including subagent
transcripts). Window: the files present on 2026-09-10; the dated hits run 2026-09-02 →
2026-09-10. Every line was parsed structurally (not grepped): Codex `custom_tool_call`
→ `tools.exec_command` / `tools.apply_patch`, and Claude `tool_use` → `Bash` / `Edit` /
`MultiEdit` / `Write` / `mcp__clj-surgeon__*`. Scripts and intermediates are in
`/var/tmp/forge/ethno/`.

**Counting rule.** One *call* = one unit, for programs and for editors alike. A program
edit is a shell command that both targets a `.clj/.cljc/.cljs/.edn` path **and** mutates
it — in-place regex (`sed -i`, `perl -i`, `ed -s`), a Python read-modify-write, a
heredoc/redirect over the file, an append, a `git apply`/`patch -p`, or a `bb`/`clojure -e`
script that `spit`s. Programs that only *read* Clojure (2,214 of them — `awk` counting
forms, `sed -n` printing a range) are excluded; that distinction matters, see §5.

---

## 1. Headline

**Across the corpus, Clojure source was mutated by a program 2.9× more often than by an
editor tool, and 28× more often than by clj-surgeon.**

| intent class | program-edit | editor-edit | surgeon | broke-the-file (confirmed) |
|---|---:|---:|---:|---:|
| test body edit | 1,920 | 484 | — | 46 |
| change require / alias | 1,876 | 683 | 52 | 29 |
| other / mixed | 834 | 439 | 144 † | 8 |
| insert or edit a defn | 628 | 366 | 24 | 17 |
| docstring / comment | 254 | 10 | — | 2 |
| config `.edn` | 210 | 58 | — | 1 |
| delete form | 208 | 44 | — | 2 |
| whole-file create/rewrite | 181 | 48 | — | 2 |
| bulk regex rename | 13 | 0 | — | 0 |
| **TOTAL** | **6,124** | **2,132** | **220** | **107** |

† `apply_clojure_changes` (119), `edit_clojure` (12) and `feature_thread` (13) are
general-purpose, so they cannot be attributed to one intent without per-call inspection;
they are pooled here. `alias_migration` (52) and `helper_extraction` (24) are verb-specific
and are attributed. `require_change` and `transform_clojure` and `namespace_split`: **zero
calls in the entire corpus.** Two further Surgeon calls are not edits and are excluded:
`admit_clojure_patch` 74 (a gate) and `inspect_clojure` 65 (a read).

### The three numbers that matter

| | program edit | editor edit |
|---|---:|---:|
| calls | 6,124 | 2,132 |
| verified within 8 events | **51%** | 37% |
| **`git diff` read afterwards** | **252 (4.1%)** | — |
| left the edited file unparseable | **107 (1.7%)** | **8 (0.4%)** |
| …as a share of *verified* edits | **3.4%** | **1.0%** |

Read the last two rows together. Program edits were verified *more* often than editor
edits and still broke the file **3.3× more often per verified edit**. And because
verification is the only way a breakage becomes visible, the 2,974 program edits with **no
verification in the window** are where the silent-wrong risk actually lives — the 107
confirmed breakages are a floor, not an estimate. The 4.1% `git diff` rate is the sharpest
finding in the corpus: an agent that writes a 30-line Python script to rewrite a namespace
almost never looks at what the script did.

### It is a model/harness trait far more than a task trait

| model | program | editor | program share |
|---|---:|---:|---:|
| claude-opus-5 | 4,843 | 552 | **90%** |
| claude-fable-5-1 | 286 | 0 | **100%** |
| gpt-6-astra (Codex) | 888 | 354 | 71% |
| claude-sonnet-5 | 84 | 417 | 17% |
| gpt-5.6-sol (Codex) | 23 | 787 | **3%** |

Same box, same repos, same week, same tasks: Sol reaches for `apply_patch` 97% of the time
and Opus-5 reaches for a heredoc 90% of the time. Whatever this behavior is, it is not
driven by the edit being hard.

**The bypass-permissions instruction amplifies it but does not cause it.** 107 of 743
Claude sessions carry a system instruction to "make file changes with sed, heredocs, or
short scripts, rather than using the dedicated Read, Edit, or Write tools" (it is injected
whenever bypass-permissions mode is active — including into *this* mining session):

| model | instruction present | program | editor | program share |
|---|---|---:|---:|---:|
| claude-opus-5 | no | 4,493 | 541 | 89% |
| claude-opus-5 | **yes** | 350 | 11 | **97%** |
| claude-sonnet-5 | no | 70 | 385 | 15% |
| claude-sonnet-5 | **yes** | 14 | 32 | **30%** |
| claude-fable-5-1 | **yes** | 286 | 0 | 100% |

The instruction roughly doubles Sonnet's program rate and pushes Opus from 89% to 97% —
but Opus was already at 89% without it.


---

## 2. The hits — 60 most instructive

Ordered by: confirmed breakage first, then blast radius, then absence of verification. Max two rows per session+intent so one noisy session cannot fill the table.

| # | session | when (UTC) | model | repo / target | intent | mechanism | verified? | diff? | went wrong? | program (1 line) |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `claude/-home-forge-src-marvin-voi/sub:agent-ab1b683085648f` | 2026-09-03T23:10 | claude-opus-5 | clj-surgeon-study / `mcp_telemetry_test.clj, mcp_telemetry_test.clj` | test body edit | py_read_modify_write+append+heredoc_whole_file | cat > /tmp/o2-fx/probe/run-tel.clj <<'EOF' ( | - | BROKE: compiling at (clj_surgeon/mcp_telemetry_test.clj:1 | `cat >> test/clj_surgeon/mcp_telemetry_test.clj <<'CLJEOF'` |
| 2 | `claude/-home-forge-src-marvin-voi/sub:agent-acffa772271027` | 2026-09-03T04:17 | claude-opus-5 | clj-surgeon-study / `mcp_study_test.clj, intent_transaction.clj` | test body edit | py_read_modify_write | cat > /home/forge/tmp/probe/probe.clj <<'EOF | - | BROKE: compiling at (clj_surgeon/mcp_study_test.clj:172 : | `cd /home/forge/src/clj-surgeon-study && python3 - <<'PY' import re p='test/clj_surgeon/mcp_study_test.clj' s=open(p).read() anchor='''(defn- write-clj` |
| 3 | `claude/-home-forge-src-marvin-voi/sub:agent-a3077ed9b282e1` | 2026-09-04T11:43 | claude-opus-5 | clj-surgeon-thread / `distillery.clj, mcp_feature_thread_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mcp_feature_thread. | `cd /home/forge/src/clj-surgeon-thread && git commit -q -m "$(cat <<'EOF' RED: a route entry that names its handler var UNQUALIFIED does not join quot` |
| 4 | `claude/-home-forge-src-marvin-voi/sub:agent-a38d8f6a97aa4e` | 2026-09-04T07:20 | claude-opus-5 | clj-surgeon-tmpleak / `tmp_leak_support.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/tmp_leak_support.clj:265 | `.replace('''(def ^:private reexec-sentinel "CLJ_SURGEON_TMPDIR_REEXEC")''', r'''(def ^:private reexec-sentinel "Env var the parent sets o` |
| 5 | `claude/-home-forge-src-marvin-voi/sub:agent-a4f713cb01afc0` | 2026-09-03T04:05 | claude-opus-5 | clj-surgeon-q5z / `mcp_alias_migration_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mcp_alias_migration_test | `cd /home/forge/src/clj-surgeon-q5z && python3 - <<'PY' import io p='test/clj_surgeon/mcp_alias_migration_test.clj' s=open(p).read() anchor=''' ;; ----` |
| 6 | `claude/-home-forge-src-marvin-voi/sub:agent-a4f713cb01afc0` | 2026-09-03T04:25 | claude-opus-5 | clj-surgeon-q5z / `mcp_alias_migration_test.clj, open.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mcp_alias_migration_test | `.replace(anchor,witness,1) open(p,` |
| 7 | `claude/-home-forge-src-marvin-voi/sub:agent-a4f713cb01afc0` | 2026-09-03T04:36 | claude-opus-5 | clj-surgeon-q5z / `mcp_alias_migration_test.clj, huge.clj` | change require alias | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mcp_alias_migration_test | `(spit oversized (str "(ns acid.fanout.huge)\\n;; " (String. (char-array (inc alias-mig` |
| 8 | `claude/-home-forge-src-marvin-voi/sub:agent-a4f713cb01afc0` | 2026-09-03T04:45 | claude-opus-5 | clj-surgeon-q5z / `repository_hygiene_test.clj, core.clj` | change require alias | heredoc_whole_file | **no** | - | BROKE: compiling at (clj_surgeon/repository_hygiene_test. | `cat > test/clj_surgeon/repository_hygiene_test.clj <<'CLJ'` |
| 9 | `claude/-home-forge-src-marvin-voi/sub:agent-ab2a2e6ec97220` | 2026-09-03T09:16 | claude-opus-5 | clj-surgeon-txn / `txn_journal.clj, txn_journal_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/txn_journal_test.clj:971 | `.replace(old,new) old = "clj-surgeon shall bound that window to a digest recheck, an identity recheck, one journal fsync and one rename," ` |
| 10 | `claude/-home-forge-src-marvin-voi/sub:agent-ab2a2e6ec97220` | 2026-09-03T09:17 | claude-opus-5 | clj-surgeon-txn / `txn_journal_test.clj` | change require alias | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/txn_journal_test.clj:971 | `.replace(old,new) anchor = ''';; @spec MCP-OP-MEM-006 ;; @spec MCP-OP-MEM-007 (deftest undo-refuses-a-target-another-writer-changed-after-` |
| 11 | `claude/-home-forge-src-marvin-voi/sub:agent-a8ed0d897e972c` | 2026-09-05T05:37 | claude-opus-5 | clj-surgeon-helperimpl / `mcp_helper_extraction_test.clj, mcp_helper_extraction_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mcp_helper_extraction_te | `.replace(old, new, 1) open(p,'w').write(s) print('ok') PY sed -n '143,152p' test/clj_surgeon/mcp_helper_extraction_test.clj` |
| 12 | `claude/-home-forge-src-marvin-voi/sub:agent-a8ed0d897e972c` | 2026-09-05T05:37 | claude-opus-5 | clj-surgeon-helperimpl / `mcp_helper_extraction_test.clj, mcp_helper_extraction_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mcp_helper_extraction_te | `.replace(old,new,1)) print('ok') PY sed -n '145,148p' test/clj_surgeon/mcp_helper_extraction_test.clj && export TMPDIR=/var/tmp/forge && CP` |
| 13 | `claude/-home-forge-src-marvin-voi/sub:agent-adb3860315b2a0` | 2026-09-04T03:13 | claude-opus-5 | clj-surgeon-gate / `admit_patch_test.clj, admit_patch_test.clj` | change require alias | py_read_modify_write+append+heredoc_whole_file | **no** | - | BROKE: compiling at (clj_surgeon/admit_patch_test.clj:401 | `.replace(old,new) open(p,'w').write(s) print("ok") PY cat >> test/clj_surgeon/admit_patch_test.clj <<'CLJ' ;; ----------------------------` |
| 14 | `claude/-home-forge-src-marvin-voi/sub:agent-a920b3d741bace` | 2026-09-04T06:04 | claude-opus-5 | clj-surgeon-study / `mcp_inspect.clj, p7_decoy.clj` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: at (clj_surgeon/mcp_inspect.clj:578 :: :1). Too ma | `cd /home/forge/src/clj-surgeon-study && python3 - <<'PY' p='src/clj_surgeon/mcp_inspect.clj' s=open(p).read() anchor=''';; @spec MCP-OP-STUDY-044 (def` |
| 15 | `claude/-home-forge-src-marvin-voi/sub:agent-ad9bdd07e688c0` | 2026-09-03T23:13 | claude-opus-5 | clj-surgeon-census / `mcp_relation_census_test.clj, inner.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mcp_relation_census | `cd /home/forge/src/clj-surgeon-census && python3 - <<'PY' p='test/clj_surgeon/mcp_relation_census_test.clj' s=open(p).read() old=''' (is (str/includes` |
| 16 | `claude/-home-forge-src-marvin-voi/sub:agent-ad9bdd07e688c0` | 2026-09-03T23:47 | claude-opus-5 | clj-surgeon-census / `mcp_relation_census_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mcp_relation_census | `cd /home/forge/src/clj-surgeon-census && python3 - <<'PY' p='test/clj_surgeon/mcp_relation_census_test.clj' s=open(p).read() old=''' (is (str/includes` |
| 17 | `claude/-home-forge-src-marvin-voi/sub:agent-a3908dd8408501` | 2026-09-04T22:29 | claude-opus-5 | clj-surgeon-gate16 / `mcp_admit_tool.clj, mcp_admit_tool.clj` | delete form | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mcp_admit_tool.clj: | `cd /home/forge/src/clj-surgeon-gate16 && python3 - <<'PY' p='src/clj_surgeon/mcp_admit_tool.clj' s=open(p).read() # (161b) unsupported-patch-target be` |
| 18 | `claude/-home-forge-src-marvin-voi/sub:agent-a3908dd8408501` | 2026-09-04T22:30 | claude-opus-5 | clj-surgeon-gate16 / `mcp_admit_tool.clj` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mcp_admit_tool.clj: | `.replace(old, new, 1) n = s.count(":verify_ok (nil? blocked)") assert n == 3, n s = s.replace(":verify_ok (nil? blocked)", ":verify_ok (ver` |
| 19 | `claude/-home-forge-src-marvin-voi/sub:agent-a5bc0a68020b59` | 2026-09-04T16:11 | claude-opus-5 | clj-surgeon-study / `mcp_study_test.clj, run-vars.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mcp_study_test.clj:5100  | `.replace(bad, good, 1) bad2 = ''' (count (str/split (first lines) #"\\R" -1)) " lines: " (pr-str (` |
| 20 | `claude/-home-forge-src-marvin-voi/sub:agent-af48925f2caace` | 2026-09-04T01:33 | claude-opus-5 | clj-surgeon-integ3 / `core.clj, core.clj` | other | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/core.clj:186 :: :68 | `cd /home/forge/src/clj-surgeon-integ3 && python3 - <<'PY' import re p='src/clj_surgeon/core.clj' s=open(p).read() pat=re.compile(r'<<<<<<< HEAD\n(.*?)` |
| 21 | `claude/-home-forge-src-marvin-voi/sub:agent-af48925f2caace` | 2026-09-04T01:33 | claude-opus-5 | clj-surgeon-integ3 / `core.clj` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/core.clj:184 :: :18). No | `.replace(old,new)); print('ok') PY timeout 300 clojure -M -e "(require 'clj-surgeon.core 'clj-surgeon.study 'clj-surgeon.mcp-inspect 'clj-s` |
| 22 | `claude/-home-forge-src-marvin-voi/sub:agent-aaa35fb0f121fd` | 2026-09-03T03:14 | claude-opus-5 | clj-surgeon-q5z / `mcp_alias_migration_test.clj, store.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mcp_alias_migration_test | `.replace(a,b,1) c = ''' (is (= (get (:pre corpus) (:defining-file lib-manifest)) (slurp (io/file (:retired_to (:` |
| 23 | `claude/-home-forge-src-marvin-voi/sub:agent-aad8ed5062cebb` | 2026-09-04T02:05 | claude-opus-5 | clj-surgeon-gate / `mcp_admit_tool.clj, mcp_admit_tool.clj` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: at (clj_surgeon/mcp_admit_tool.clj:748 :: :1). Too | `.replace(old,new,1) # 2. unverifiable analyzer error types + detectors-not-run, before verification-status old2 = ''';; @spec MCP-OP-ADMIT` |
| 24 | `claude/-home-forge-src-marvin-voi/sub:agent-aad8ed5062cebb` | 2026-09-04T02:06 | claude-opus-5 | clj-surgeon-gate / `mcp_admit_tool.clj` | other | py_read_modify_write | **no** | - | BROKE: at (clj_surgeon/mcp_admit_tool.clj:748 :: :1). Too | `.replace(old,new,1) # the verify != focused branch old2=''' {:verification_status :unverified ` |
| 25 | `claude/-home-forge-src-marvin-voi/sub:agent-a34337ca691518` | 2026-09-07T07:25 | claude-opus-5 | clj-surgeon-aliasfix / `alias_migration.clj, run-ns.clj` | change require alias | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/alias_migration.clj:1209 | `cd /home/forge/src/clj-surgeon-aliasfix && python3 - <<'PY' p='src/clj_surgeon/alias_migration.clj' s=open(p).read() old = """ metadata-vector? (and (` |
| 26 | `claude/-home-forge-src-marvin-voi/sub:agent-ad26fdc4bb3144` | 2026-09-07T22:07 | claude-opus-5 | cc-cellB-N2 / `commands.clj, exports.clj` | change require alias | py_read_modify_write | **no** | - | BROKE: reading source at (cfp_scheduler_killer/exports.cl | `.replace(REQ, NEW if mode=='drop' else REQ+'\n'+NEW) open(ROOT+f,'w').write(t) print(f,mode,n); total+=n print('total sites rewritt` |
| 27 | `claude/-home-forge-src-marvin-voi/sub:agent-aa14abf5d75ba4` | 2026-09-03T03:36 | claude-opus-5 | curtaincall-cfp-folddiff / `fold_diff_test.clj, db.edn` | test body edit | py_read_modify_write | **no** | - | BROKE: compiling at (cfp_scheduler_killer/fold_diff_test. | `cd /home/forge/src/curtaincall-cfp-folddiff && python3 - <<'PYEOF' p='test/cfp_scheduler_killer/fold_diff_test.clj' s=open(p,encoding='utf-8').read() ` |
| 28 | `claude/-home-forge-src-marvin-voi/sub:agent-a26c1a2d353219` | 2026-09-05T19:10 | claude-opus-5 | clj-surgeon-mission / `mission_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mission_test.clj:75 | `.replace(''' :decision "waiting on M-1"}] (mission/waiting-missions ms)))''',''' :decision "` |
| 29 | `claude/-home-forge-src-marvin-voi/sub:agent-a26c1a2d353219` | 2026-09-05T19:11 | claude-opus-5 | clj-surgeon-mission / `mission_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mission_test.clj:75 | `.replace(old,new) open(p,'w').write(s) PY nice -n 10 taskset -c 6-9 clojure -M:clj-surgeon/test-deps -e "(require 'clojure.test 'clj-surgeo` |
| 30 | `claude/-home-forge-src-marvin-voi/sub:agent-a26c1a2d353219` | 2026-09-05T19:15 | claude-opus-5 | clj-surgeon-mission / `mission_cli.clj, mission_cli.clj` | change require alias | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mission_cli.clj:164 :: : | `.replace('''(defn- parse-flags [args] (loop [[a b & more :as remaining] args acc {} positional []] (cond (empty? remaining) (` |
| 31 | `claude/-home-forge-src-marvin-voi/sub:agent-a26c1a2d353219` | 2026-09-05T19:16 | claude-opus-5 | clj-surgeon-mission / `mission_cli.clj, http.clj` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mission_cli.clj:164 :: : | `cd /home/forge/src/clj-surgeon-mission && python3 - <<'PY' p='src/clj_surgeon/mission_cli.clj' s=open(p).read() # 2. the closed-shape example + help, ` |
| 32 | `claude/-home-forge-src-marvin-voi/sub:agent-a26c1a2d353219` | 2026-09-05T19:17 | claude-opus-5 | clj-surgeon-mission / `mission_cli.clj, .clj-surgeon.edn` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: compiling at (clj_surgeon/mission_cli.clj:229 :: : | `.replace('''(defn- ledger-of''','''(defn admitted-profiles "@caller-probe. The profiles this call may prove a write with: the WORKSPAC` |
| 33 | `claude/-home-forge-src-marvin-voi/sub:agent-ae77088874780e` | 2026-09-05T05:50 | claude-opus-5 | clj-surgeon-helperimpl / `mcp_helper_extraction.clj, .clj-surgeon.edn` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mcp_helper_extracti | `cd /home/forge/src/clj-surgeon-helperimpl && python3 - <<'PY' # -*- coding: utf-8 -*- p='src/clj_surgeon/mcp_helper_extraction.clj' s=open(p, encoding` |
| 34 | `claude/-home-forge-src-marvin-voi/sub:agent-ae77088874780e` | 2026-09-05T05:50 | claude-opus-5 | clj-surgeon-helperimpl / `mcp_helper_extraction.clj` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: reading source at (clj_surgeon/mcp_helper_extracti | `.replace(anchor, block+anchor) open(p,'w',encoding='utf-8').write(s) PY timeout 600 java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" cl` |
| 35 | `codex/rollout-2026-09-09T00-45-03-01a083a0-1` | 2026-09-09T00:58 | gpt-6-astra | rows-sublime-5 [fx] / `compact-facts.clj, namespace_split.clj` | test body edit | py_read_modify_write+heredoc_whole_file | **no** | - | BROKE: compiling at (clj_surgeon/namespace_split_test.clj | `cat > /var/tmp/forge/rows-sublime-5/compact-facts.clj <<'CLJ'` |
| 36 | `claude/-home-forge-src-marvin-voi/sub:agent-a64653304f869e` | 2026-09-03T11:39 | claude-opus-5 | clj-surgeon-txn / `a2.clj, a2.clj` | insert or edit defn | py_read_modify_write | **no** | - | "oops" nearby | `.replace(""" (let [before (count @created) outcome""",""" (let [outcome""") s=s.replace(""" after @created pending` |
| 37 | `claude/-home-forge-src-marvin-voi/sub:agent-a64653304f869e` | 2026-09-03T11:42 | claude-opus-5 | clj-surgeon-txn / `item1-tests.clj, txn_journal_test.clj` | test body edit | py_read_modify_write+heredoc_whole_file | **no** | - | "oops" nearby | `cat > /tmp/txn5-fx/item1-tests.clj <<'CLJEOF'` |
| 38 | `claude/-home-forge-src-marvin-voi/sub:agent-a64653304f869e` | 2026-09-03T11:42 | claude-opus-5 | clj-surgeon-txn / `txn_journal_test.clj, txn_journal_test.clj` | test body edit | py_read_modify_write | **no** | - | "oops" nearby | `.replace("""(defn- create-lock-if-absent!""","""(defn- displaced-count "The kernel's running count of claims a break displaced and did no` |
| 39 | `claude/-home-forge-src-marvin-voi/sub:agent-a64653304f869e` | 2026-09-03T11:43 | claude-opus-5 | clj-surgeon-txn / `txn_journal.clj, txn_journal.clj` | insert or edit defn | py_read_modify_write | **no** | - | "oops" nearby | `cd /home/forge/src/clj-surgeon-txn && python3 - <<'PY' p='src/clj_surgeon/txn_journal.clj' s=open(p).read() # 1. counter + restore primitives, inserte` |
| 40 | `claude/-home-forge-src-marvin-voi/sub:agent-a64653304f869e` | 2026-09-03T11:44 | claude-opus-5 | clj-surgeon-txn / `txn_journal.clj, txn_journal.clj` | other | py_read_modify_write | **no** | - | "oops" nearby | `.replace(` |
| 41 | `claude/-home-forge-src-marvin-voi/sub:agent-a64653304f869e` | 2026-09-03T11:44 | claude-opus-5 | clj-surgeon-txn / `txn_journal.clj` | change require alias | py_read_modify_write | **no** | - | "oops" nearby | `.replace(old,new,1) old=''' (when-let [broken (:lock-broken lock)] ;; the break is durable evidence, not only` |
| 42 | `claude/-home-forge-src-marvin-voi/sub:agent-afe5eedc8f5241` | 2026-09-10T16:46 | claude-opus-5 | clj-surgeon-pubcand / `mcp_process.clj` | delete form | inplace_regex+py_read_modify_write | **no** | - | "oops" nearby | `sed -i 's/\*Bridge seat, 2026-09-02\./*Author seat, 2026-09-02./' docs/closure-catalogue.md ` |
| 43 | `claude/-home-forge-src-marvin-voi/sub:agent-a2d936e48f3423` | 2026-09-03T04:05 | claude-opus-5 | memcheck [fx] / `memprobe.clj, clj-surgeon.cljc` | change require alias | heredoc_whole_file | **no** | - | BROKE: at (memprobe.clj:78 :: :5). Unable to resolve var: | `cat > /home/forge/tmp/memcheck/dev/mem/memprobe.clj <<'EOF'` |
| 44 | `claude/-home-forge-src-marvin-voi/sub:agent-a2d936e48f3423` | 2026-09-03T04:07 | claude-opus-5 | memcheck [fx] / `memprobe.clj, memprobe.clj` | docstring comment | py_read_modify_write | **no** | - | BROKE: reading source at (memprobe.clj:77 :: :98). Unclos | `.replace(""" form (try (read {:eof eof :read-cond :preserve} rdr) (catch Exception _ eof))]""", """ ` |
| 45 | `claude/-home-forge-src-marvin-voi/sub:agent-a2d936e48f3423` | 2026-09-03T04:08 | claude-opus-5 | memcheck [fx] / `memprobe.clj` | insert or edit defn | py_read_modify_write | **no** | - | BROKE: reading source at (memprobe.clj:77 :: :98). Unclos | `.replace(""" (let [rdr (LineNumberingPushbackReader. (StringReader. source)) eof (Object.)] (binding [*read-eval* false ` |
| 46 | `claude/-home-forge-src-marvin-voi/sub:agent-a0cc860e3a46c0` | 2026-09-03T21:50 | claude-opus-5 | clj-surgeon-txn / `txn_journal.clj, txn9-fx-run5.clj` | delete form | py_read_modify_write | **no** | - | "oops" nearby | `.replace(old,new,1) old=''' :interrupted (long (count live))''' new=''' :interrupted-corroborated (long (get classes :corroborated` |
| 47 | `claude/-home-forge-src-marvin-voi/sub:agent-a80f3a65a389c5` | 2026-09-04T13:39 | claude-opus-5 | wt [fx] / `reader_eval_fence_test.clj` | test body edit | py_read_modify_write | **no** | - | BROKE: at (clj_surgeon/reader_eval_fence_test.clj:284 ::  | `.replace(old,new,1)) print("ok") PYEOF cd /home/forge/tmp/wt/census20 && CP=$(cat /var/tmp/forge/census24-fx/cp.txt) && timeout 1200 java -` |
| 48 | `claude/-home-forge-src-marvin-voi/sub:agent-a480d289fff70d` | 2026-09-03T08:50 | claude-sonnet-5 | curtaincall-cfp-folddiff / `defmacro.clj, declare.clj` | change require alias | heredoc_whole_file | **no** | - | "oops" nearby | `cat > /home/forge/tmp/folddiff6/item3/defmacro.clj <<'EOF'` |
| 49 | `claude/-home-forge-src-marvin-voi/sub:agent-a02963aa0720c5` | 2026-09-03T11:26 | claude-opus-5 | (cwd) / `pA2.clj, probes.clj` | change require alias | heredoc_whole_file | **no** | - | BROKE: reading source at (/tmp/txn4-fx/pA2.clj:39 :: :1). | `cat > /tmp/txn4-fx/pA2.clj <<'EOF'` |
| 50 | `claude/-home-forge-src-marvin-voi/sub:agent-afc15516741816` | 2026-09-04T03:50 | claude-opus-5 | clj-surgeon-q5z / `mcp_tool.clj, run2b.clj` | insert or edit defn | py_read_modify_write | **no** | - | "oops" nearby | `cd /home/forge/src/clj-surgeon-q5z && python3 - <<'PY' p='src/clj_surgeon/mcp_tool.clj' s=open(p).read() anchor=''';; @spec MCP-OP-ALIAS-059 (defn ref` |
| 51 | `claude/-home-forge-src-marvin-voi/sub:agent-ab2a2e6ec97220` | 2026-09-03T09:07 | claude-opus-5 | clj-surgeon-txn / `txn_journal.clj, state.edn` | insert or edit defn | py_read_modify_write | **no** | - | "oops" nearby | `.replace(anchor,helper) # 2. retained-transactions old = ''' :let [lease (read-edn-file (io/file d "lease.edn")) ` |
| 52 | `claude/-home-forge-src-marvin-voi/sub:agent-ab2a2e6ec97220` | 2026-09-03T09:08 | claude-opus-5 | clj-surgeon-txn / `txn_journal.clj, txn_journal_test.clj` | test body edit | py_read_modify_write | **no** | - | "oops" nearby | `.replace(old,new) old_row = "/ MEM-013 / \"A finished transaction's journal is garbage.\" A committed receipt is undoable only while its pr` |
| 53 | `claude/-home-forge-src-marvin-voi/sub:agent-ab2a2e6ec97220` | 2026-09-03T09:09 | claude-opus-5 | clj-surgeon-txn / `txn_lock_child.clj, txn_journal_test.clj` | change require alias | py_read_modify_write+heredoc_whole_file | **no** | - | "oops" nearby | `.replace(anchor, anchor + ''' (defn- hold-publish-lock! "Spawn a SEPARATE JVM holding this workspace's PUBLISH.lock for hold-ms. Re` |
| 54 | `claude/-home-forge-src-marvin-voi/sub:agent-ab86cef158e2f4` | 2026-09-07T23:40 | claude-opus-5 | plan2 [fx] / `s11.clj, server.clj` | change require alias | heredoc_whole_file | **no** | - | BROKE: reading source at (cfp_scheduler_killer/views_test | `cat > /var/tmp/forge/plan2/cellC/R5/s11.clj <<'EOF'` |
| 55 | `claude/-home-forge-src-marvin-voi/sub:agent-a8ed0d897e972c` | 2026-09-05T05:47 | claude-opus-5 | clj-surgeon-helperimpl / `helper_extraction_fixture.clj, l01.clj` | change require alias | py_read_modify_write | **no** | - | "oops" nearby | `cd /home/forge/src/clj-surgeon-helperimpl && python3 - <<'PY' p='test/clj_surgeon/helper_extraction_fixture.clj' s=open(p).read() anchor='''(def ^:pri` |
| 56 | `claude/-home-forge-src-marvin-voi/sub:agent-a5a7819c7046cd` | 2026-09-04T04:21 | claude-opus-5 | clj-surgeon / `-` | bulk regex rename | inplace_regex | **no** | - | - | `sed -i 's#^GLOBS_tests="test/\*\*/\*.clj test/\*\*/\*.js test/\*\*/\*.mjs"#GLOBS_tests="test/**/*.clj test/*.clj test/**/*.js test/*.js test/**/*.` |
| 57 | `claude/-home-forge-src-marvin-voi/sub:agent-a4753ab4c46fbe` | 2026-09-03T22:13 | claude-opus-5 | clj-surgeon-fanout / `gen-fanout.clj, n.clj` | insert or edit defn | py_read_modify_write+clj_script | **no** | - | "oops" nearby | `.replace('''(def fan-test-sh ''','''(def gitignore "target/\\n.cpcache/\\n.clj-kondo/.cache/\\n.lsp/\\n.cache/\\n*.log\\n") (def fan-test-s` |
| 58 | `claude/-home-forge-src-marvin-voi/sub:agent-a4753ab4c46fbe` | 2026-09-03T22:13 | claude-opus-5 | clj-surgeon-fanout / `fan_check.clj` | delete form | py_read_modify_write | **no** | - | "oops" nearby | `.replace(old,""" ;; ANY qualified use of the old var except the decoy ;; n` |
| 59 | `claude/-home-forge-src-marvin-voi/sub:agent-a2ec057c3a5cc7` | 2026-09-04T00:15 | claude-opus-5 | clj-surgeon-q5z / `red4.clj, mcp_workspace.clj` | test body edit | py_read_modify_write+heredoc_whole_file | cd /home/forge/src/clj-surgeon-q5z && /home/ | - | BROKE: compiling at (clj_surgeon/mcp_alias_migration_test | `cat > /tmp/q5z11-fx/red4.clj <<'CLJEOF'` |
| 60 | `claude/-home-forge-src-marvin-voi/sub:agent-a2a74f455184d6` | 2026-09-04T04:07 | claude-opus-5 | clj-surgeon-study / `mcp_study_test.clj, outline.clj` | test body edit | py_read_modify_write | cd /home/forge/src/clj-surgeon-study && pyth | - | BROKE: compiling at (clj_surgeon/mcp_study_test.clj:3296  | `.replace(old, new, 1) open(p,'w').write(s) print("fixture added") PY` |
---

## 3. Taxonomy of intents, by frequency — and the Surgeon verb that should have fired

**1. Test body edit — 1,920 program vs 484 editor vs ~0 dedicated Surgeon (46 breakages).**
The largest class and the most damaged one. Almost always a Python heredoc that anchors on
a triple-quoted blob of existing test source and `.replace()`s it, or a `cat >>` that
appends a new `deftest` to the end of the file. *Verb today:* `edit_clojure` /
`apply_clojure_changes` with `within` + `from`/`to` covers **replacing** a form.
**Missing:** appending a new `deftest` — the CLI has no insert operation at all, and
owner-scoped insertion through `apply_clojure_changes` is a measured loser (it re-prints
the whole owner). The `cat >> …_test.clj <<'CLJEOF'` idiom exists because nothing else does.

**2. Change require / alias — 1,876 program vs 683 editor vs 52 `alias_migration`
(29 breakages).** The most striking gap in the corpus. This is the one intent with a
dedicated, measured-winner verb on both sides (`alias_migration`, `require_change`), and
`require_change` was called **zero times**. Instead: `for f in $FILES; do sed -i
's#json/write-str#mjson/write#g' src/marvin_voice_remote/$f.clj; done`, followed by a
second `sed -i '/^[[:space:]]*\[clojure\.data\.json :as json\]$/d'` to delete the stale require —
two blind regexes doing exactly what one verb call does with an `expect` guard.

**3. Insert or edit a defn — 628 vs 366 vs 24 `helper_extraction` (17 breakages).**
Splits cleanly: *extracting* an existing helper has a verb (`helper_extraction`) and it
gets used; *inserting a new top-level form* has none, and is 100% program. This is the
single clearest missing-verb signal.

**4. Docstring / comment — 254 vs 10.** A 25:1 program ratio, the most lopsided class.
Docstrings are multi-line string literals, which is precisely where a Python heredoc's
escaping breaks: one agent's own next line was *"The Python heredoc left literal
backslashes in a Clojure docstring. Fixing."* *Verb:* `within` + `from`/`to` handles it
and is never used for it.

**5. Config `.edn` — 210 vs 58.** `deps.edn`, `bb.edn`, lane manifests. **No
Clojure-aware verb exists for `.edn`** — every Surgeon verb is namespace/Var-shaped. This
class is 100% programs by necessity.

**6. Delete form — 208 vs 44 (2 breakages).** Usually a Python line filter
(`'\n'.join(line for line in s.split('\n') if not (…))`) — deleting *lines* to remove a
*form*, which is the exact operation that produces `Unmatched delimiter`. **No delete
verb exists.**

**7. Whole-file create / rewrite — 181 vs 48.** `cat > src/foo/bar.clj <<'EOF'`. Creating
a new namespace has no verb (`namespace_split` writes files, but only as a split).
Largely legitimate — authoring a new file is not editing one.

**8. Bulk regex rename — 13 vs 0.** Rare but the highest blast radius:
`grep -rl "cc-split-T2\.src\.cfp-scheduler-killer" src test dev | xargs sed -i 's/…/…/'`.
`alias_migration` covers the alias case; a general **rename-symbol-across-repo** verb does
not exist (`:rename-ns!` covers namespaces only).

**Summary of the gap.** Of the eight classes, three have a matching verb that is
under-used (require/alias, form replace, helper extraction) and **five have no verb at
all**: insert a form, delete a form, rename a symbol repo-wide, edit `.edn`, create a
namespace. Those five account for **1,240 program edits** that had nowhere else to go.
The other ~4,900 did.

---

## 4. Three agents explaining, in their own words, why a program

**(a) The tool refused, so the agent hand-rolled the migration.** `claude-fable-5-1`,
main session, 2026-09-02T18:24:44Z — the intent was *exactly* `alias_migration`:

> "Confirmed the MCP path is blocked again by the same rf1 false refusal, so I'll close it
> out and rely on the mechanical fix instead — rewriting the 23 sites and four ns forms in
> one pass, then reloading the five namespaces to verify everything loads cleanly."

**(b) The best-practice case for a script — guards instead of a parser.**
`claude-opus-5` subagent, 2026-09-07T14:34:47Z:

> "Method note: four parallel reads (three ns headers + helper regions, plus one repo-wide
> grep) in a single batch, then one Python script that applied all nine edits (3 requires,
> 3 call sites, 3 helper deletions) with `assert count == 1` guards on every old string
> before writing — so a byte-drift in any of the three supposedly-identical helper bodies
> would have aborted rather than silently half-applied."

This is the corpus's strongest defense of the practice — and, measured, it is more common
than expected: of the 4,204 Python read-modify-write edits, **54% carry some `assert`** and
**19% carry an explicit `count(...) == n` guard** before writing. That cuts against the easy
story. Agents largely *are* guarding against the failure a script makes easy (matching the
wrong number of sites), and they still leave the file unparseable 3.3× more often than an
editor call does — because the guard checks *how many strings matched*, never *whether the
result is a form*. A count guard cannot catch a deleted line that took a closing paren with
it, and that is what the 107 breakages mostly are.

**(c) Batch economics, stated plainly.** `claude-sonnet-5`, ecaller N-3 worktree,
2026-09-04T02:57:44Z, immediately before a 21-file `sed`:

> "All uses of `store/` are `store/find-event`. Now applying the scripted edit across all
> 21 files with alias `store2` (uniform per the confirmed policy check — no file binds
> `store2`, `st2`, or `es`)."

**Two more worth recording.** The seat's own verdict on this exact question,
2026-09-10T18:33:20Z — note the *first* clause is the missing-verb finding, not a
preference:

> "**CLI has no insert operation.** Two of the three edits could not be done through it at
> all… So: Python is faster and it is the wrong tool for exactly the edit Astra made,
> because a string is not a form and a count has to be true."

And a 2026-09-04 review that found the healthy pattern — programs as *readers*, editors as
*writers*:

> "5 of 6 native arms invoked `sed`/`perl`/`awk` — every time as a *reader*; the one arm
> that used perl for the edit **printed a patch and fed it to `apply_patch`**."

---

## 5. Caveats — what this search cannot see

1. **A large share of the corpus is a benchmark whose arms were *told* to go native.**
   This box spent the week measuring native-vs-Surgeon. 790 of 1,570 sessions mention a
   native-only / no-Surgeon arm somewhere. Excluding every one of them, the program share
   is still **75%** (3,114 vs 1,063) — so the finding survives, but no single row can be
   assumed to be a free choice. Rows in §2 from `mvr-dogfood*`, `mvr-row2-*`, `cc-split-*`,
   `ecaller` and `*-fx` paths are especially likely to be instrumented arms.
2. **Fixture work is mixed in with product work.** 2,964 of 6,124 program edits target
   `/var/tmp/forge` or `/home/forge/tmp` scratch trees; 2,656 target real repo checkouts
   under `/home/forge/src`. Generating a fixture `.clj` with a heredoc is authoring, not
   editing, and inflates the `whole_file_create_or_rewrite` and `test_body_edit` classes.
3. **Self-contamination.** 162 hits are dated 2026-09-10 — this mining session's own
   transcript is being written into the corpus it is mining, and one candidate quote was
   an echo of my own intermediate output. Both quotes in §4 were re-verified against their
   original session files; the counts were not de-duplicated for this.
4. **One call ≠ one edit, and the bias runs one way.** A single Python heredoc routinely
   applies nine edits across three files; an `Edit` call applies one. Counting calls
   therefore *understates* the program side, probably by a wide margin. The 2.9× ratio is
   a floor.
5. **Breakage attribution is a floor, and its window is short.** A hit counts as "broke
   the file" only if a reader/compile error naming *that basename* appears within 8
   events. Damage found an hour later, in CI, by a human, or never, is invisible. Nothing
   here traces a later fix commit or a revert — that needs a git-history join this pass did
   not do.
6. **Reasoning is mostly not recorded.** Claude thinking blocks are summarized or absent
   and Codex reasoning is often redacted, so §4 has five quotes out of 6,124 edits.
   **The overwhelming finding about rationale is that there isn't one:** agents almost never
   say why they chose a program. It is not a deliberated choice, which is consistent with
   it being a model-level default (§1).
7. **Deleted and compacted sessions are gone**, and `--resume`d sessions may double-count
   replayed prefixes. Codex rollouts predating the `custom_tool_call` schema are parsed by
   a separate path (`function_call`/`exec_command`); if an older schema exists it was
   silently skipped.
8. **The classifier is regex-based.** Intent is inferred from the command text, so
   `change_require_alias` catches anything mentioning `:require`/`:as`, including edits
   that merely *touch* an ns form. The `other` class (834) is the honest residue.
9. **Not searched, by instruction:** everything outside the two session roots — no git
   history, no repo working trees, no `~/bin` scripts, no Codex/Claude sessions belonging
   to other seats or boxes.
