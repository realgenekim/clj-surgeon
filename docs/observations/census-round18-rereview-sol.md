## NO-GO

# Round-eighteen review — clj-surgeon bridge/census-verb at 3b7904a

Reviewed read-only in `/home/forge/tmp/sol/census18-wt`. No commit, push, stash,
index write, or source edit was made. Manual fixtures and git-archive exports were
confined to `/tmp/census18-fx`; every mode I changed was restored. No listed port was
contacted. The MCP suite itself opened only its ephemeral test nREPLs (45295, 36541,
36399).

Entry proof:

```text
$ git rev-parse HEAD && git status --porcelain=v1 && git log --oneline fb7f3b1..3b7904a
3b7904a277f5f5e1306ea85774a27e862e16297e
3b7904a2 docs(census): CENSUS-014/018 state round seventeen's five rules
5ed325ca ratchet: the witness battery is a COMMITTED composition that prints itself (Opus round-17 item 8)
17efac62 fix: install_test OWNS the commit it asserts, instead of reading it twice (Opus round-17 item 6)
7b33698c pin: naming a source is not walking a tree — the symlink rule, stated and driven (Opus round-17 item 7)
7bc886a5 green: the workspace root has ONE name, at both entrances (Opus round-17 items 4 and 5)
5bd6fbaa red: a refusal whose subject is the workspace root names it absolutely, or names nothing (Opus round-17 items 4 and 5)
cf2b94e7 green: a denied ANCESTOR must be a directory, at both entrances (Opus round-17 item 3)
6aff6965 red: the two entrances name a readable regular file in a path prefix two different things (Opus round-17 item 3)
23b59493 green: the overflow remedy WALKS the candidate's fields and names the heaviest (Opus round-17 item 2)
c27cb1a2 red: the overflow remedy names a cause it never measured, on a variable part the fix did not weigh (Opus round-17 item 2)
194c57af green: the refusal bound is a property of each ENTRANCE, not of one constructor (Opus round-17 item 1)
3daf6c2e red: a refusal shape outside one witness's single drive is unbounded at BOTH entrances (Opus round-17 item 1)
```

The blank line after the commit hash is the verbatim empty `git status --porcelain=v1`.

1. **BLOCKING — the real CLI launcher still publishes unbounded refusals.**
   `src/clj_surgeon/core.clj:2052`, `src/clj_surgeon/core.clj:2085`,
   `src/clj_surgeon/core.clj:2193`.

   `core/run-relation-census` and `core/run` now bound the current census shapes, but
   `parse-args` throws before either. `-main` prints the exception data and message
   directly. A repeated 10,001-character value is repeated in `:values`; an invalid
   numeric token is repeated in the reader exception. Neither carries a truncation
   marker.

```text
$ set +e
$ printf -v census_big '%*s' 10001 ''
$ census_big=${census_big// /a}
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main -m clj-surgeon.core :op :relation-census :doors "$census_big" :doors "$census_big" > /tmp/census18-fx/sol/launcher-duplicate.edn 2>&1
$ dup_exit=$?
$ invalid="[1${census_big}]"
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main -m clj-surgeon.core :op :relation-census :doors "$invalid" > /tmp/census18-fx/sol/launcher-invalid.edn 2>&1
$ inv_exit=$?
$ for pair in "duplicate:$dup_exit:/tmp/census18-fx/sol/launcher-duplicate.edn" "invalid:$inv_exit:/tmp/census18-fx/sol/launcher-invalid.edn"; do label=${pair%%:*}; rest=${pair#*:}; status=${rest%%:*}; file=${rest#*:}; bytes=$(wc -c < "$file"); maxrun=$(rg -o 'a+' "$file" | awk '{ if (length > m) m=length } END { print m+0 }'); type=$(rg -o ':error-type :[^,}]+' "$file" | sed -n '1p'); markers=$(rg -o 'truncated' "$file" | wc -l); echo "$label EXIT=$status BYTES=$bytes MAX_A_RUN=$maxrun MARKERS=$markers $type"; done
duplicate EXIT=1 BYTES=20228 MAX_A_RUN=10001 MARKERS=0 :error-type :duplicate-argument
invalid EXIT=1 BYTES=10064 MAX_A_RUN=10001 MARKERS=0 :error-type :invalid-arguments
```

   “Belongs to no op” is not a valid bound exemption. It can explain why these names
   do not belong in `cli-refusal-types`, but it cannot exempt the public CLI entrance
   from the global CENSUS-014 promise that no refusal field is unbounded. The exact
   command already contains `:op :relation-census`; only the parser's placement keeps
   the last-step bound from seeing it. The launcher needs its own last-step bound (or
   a bounded exception projection) regardless of which op is eventually known.

2. **BLOCKING — CLI `:file` reads through an escaping symlink; the brief's “no
   longer reads” claim is false.** `test/clj_surgeon/mcp_relation_census_test.clj:6748`,
   `test/clj_surgeon/mcp_relation_census_test.clj:6781`,
   `docs/intent/relation-census/relation-census-specs.md:44`.

   Fixture: `ws/src/app/escape.clj -> ../../../outside/secret.clj`, with the outside
   arm uniquely containing `SECRET-OUTSIDE`.

```text
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/symlink.clj
MCP-WALK {:ok true, :type nil, :cause nil, :skipped 1, :files 1, :read-secret? false}
MCP-NAMED {:ok false, :type "unreadable-source-path", :cause "outside-project", :skipped nil, :files 0, :read-secret? false}
CLI-WALK {:ok true, :type nil, :cause nil, :skipped 1, :files 1, :read-secret? false}
CLI-NAMED {:ok true, :type nil, :cause nil, :skipped nil, :files 1, :read-secret? true}
CLI-RUN-NAMED {:ok true, :type nil, :cause nil, :skipped nil, :files 1, :read-secret? true}
```

   The rule actually pinned in both the witness and EARS is **NAMING IS NOT
   WALKING**: MCP named files are fenced; both walks are confined; CLI `:file` has no
   fence and “reads what the operator typed.” That is the opposite of the builder
   claim in this brief. Under this review's explicit rule that any outside read is
   blocking, this is independently NO-GO. The EARS text does contain the implemented
   rule, but that rule authorises the forbidden read.

3. **The “ten-shape parity enumeration all agreeing” claim is false, and
   `:unresolvable-source-path` still has no parity row.**
   `test/clj_surgeon/mcp_relation_census_test.clj:6597`,
   `test/clj_surgeon/mcp_relation_census_test.clj:6617`,
   `src/clj_surgeon/mcp_paths.clj:265`.

```text
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/parity.clj
missing expected not-found tool not-found cli not-found agree true
denied-file expected permission-denied tool permission-denied cli permission-denied agree true
denied-parent expected parent-denied tool parent-denied cli parent-denied agree true
dir-named-clj expected not-a-regular-file tool not-a-regular-file cli not-a-regular-file agree true
fifo expected not-a-regular-file tool not-a-regular-file cli not-a-regular-file agree true
symlink-loop expected not-found tool not-found cli not-found agree true
name-too-long expected not-found tool not-found cli not-found agree true
enotdir-component expected not-found tool not-found cli not-found agree true
escape expected outside-project tool outside-project cli nil agree false
wrong-extension expected not-a-relative-source-path tool not-a-relative-source-path cli nil agree false
RESTORED true true
```

   Eight comparable rows agree, including the repaired ENOTDIR row. Two rows are
   expressly declared divergences; at this tip they are successful CLI reads, not
   equal refusals. Calling all ten “agreeing” is therefore inaccurate.

   The resolver's `:else` is reachable without publishing exception text:

```text
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/probe.clj | sed -n '/UNRESOLVABLE-INJECTION/,$p'
UNRESOLVABLE-INJECTION
{:ok false, :error_type "invalid-source-path", :error "Source path could not be resolved (IllegalStateException)", :path "src/app/arm.clj", :source_unchanged true, :remedy "Use an existing project-relative source path inside the configured project root.", :cause "unresolvable-source-path"}
```

   The parity witness explicitly `disj`s this cause because no ordinary path provokes
   it. That is a real, declared-not-closed totality hole against CENSUS-014's “every
   cause” language, although the injected receipt itself is bounded, relative, typed,
   and safe. I treat it as non-blocking beside the two executed blockers above.

4. **The root-name repair works at the three subject sites, but “no absolute root”
   is too broad for the CLI receipt.** `src/clj_surgeon/mcp_paths.clj:144`,
   `src/clj_surgeon/mcp_relation_census.clj:1084`, `src/clj_surgeon/core.clj:1083`,
   `src/clj_surgeon/core.clj:1098`.

```text
$ chmod 000 /tmp/census18-fx/sol/denied
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/root-name.clj
$ status=$?
$ chmod 755 /tmp/census18-fx/sol/denied
$ echo EXIT=$status
$ find /tmp/census18-fx/sol/denied -maxdepth 0 -printf 'RESTORED=%m %p\n'
MCP-NAMED {:ok false, :type "unreadable-source-path", :directory nil, :root-token? true, :absolute-root-in-subject? false, :double-space? false}
MCP-WALK {:ok false, :type "unreadable-source-path", :directory "<workspace_root>", :root-token? true, :absolute-root-in-subject? false, :double-space? false}
CLI-DIRECT {:ok false, :type :file-not-readable, :directory "<workspace_root>", :root-token? true, :absolute-root-in-subject? true, :double-space? false}
CLI-RUN {:ok false, :type :file-not-readable, :directory "<workspace_root>", :root-token? true, :absolute-root-in-subject? true, :double-space? false}
EXIT=0
RESTORED=755 /tmp/census18-fx/sol/denied
```

   The CLI's absolute match is in its remedy, not `:directory` or `:error`:

```text
$ chmod 000 /tmp/census18-fx/sol/denied
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main -e '(require (quote [clj-surgeon.core :as c])) (binding [*out* (java.io.StringWriter.)] (binding [*out* *err*] (prn (select-keys (c/run-relation-census {:dir "/tmp/census18-fx/sol/denied"}) [:directory :error :remedy :anchor]))))'
$ chmod 755 /tmp/census18-fx/sol/denied
{:directory "<workspace_root>", :error "the directory <workspace_root> may not be read or traversed by this process, so this census cannot claim to have read the tree", :remedy "<workspace_root> came from the workspace walk, not from the request, so there is no request to narrow and no narrower command can be computed: make <workspace_root> readable under /tmp/census18-fx/sol/denied, remove it, or name the sources to census with :file. A census is a completeness claim, and a subtree this process may not enter cannot be counted as read.", :anchor {:kind :dir, :given "/tmp/census18-fx/sol/denied", :absolute "/tmp/census18-fx/sol/denied"}}
```

   The empty subject and double-space defects are closed, and the MCP resolver no
   longer leaks the absolute root in its subject. The CLI still prints its absolute
   anchor in `:anchor` by older contract and repeats it in the remedy. This is the
   workspace itself, not an outside path, so it does not trigger the brief's outside-
   disclosure blocker; it does mean the literal “no absolute root” claim is not true
   of the whole refusal.

5. **The current entrance-bound and field-weight repairs reproduce.**
   `src/clj_surgeon/mcp_relation_census.clj:143`,
   `src/clj_surgeon/mcp_relation_census.clj:163`,
   `test/clj_surgeon/mcp_relation_census_test.clj:6242`.

   Current fields at MCP, direct CLI, and `core/run` are bounded. The refusal raised
   between the request exit and handler exit (`server-not-initialized`) is also
   bounded and has a 26-byte continuation:

```text
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/probe.clj
BOUND-MATRIX
workspace-root/dir MCP {:ok false, :type "invalid-workspace-root", :reason "invalid-workspace-root", :max-string 1055, :json-bytes 1590, :marker? true} DIRECT {:ok false, :type :census-adapter-failure, :reason nil, :max-string 1055, :json-bytes 2623, :marker? true} RUN {:ok false, :type :census-adapter-failure, :reason nil, :max-string 1055, :json-bytes 2623, :marker? true}
files MCP {:ok false, :type "unreadable-source-path", :reason nil, :max-string 1055, :json-bytes 3830, :marker? true} DIRECT {:ok false, :type :file-not-found, :reason nil, :max-string 1055, :json-bytes 5483, :marker? true} RUN {:ok false, :type :file-not-found, :reason nil, :max-string 1055, :json-bytes 5483, :marker? true}
doors MCP {:ok false, :type "unknown-door-symbol", :reason nil, :max-string 1055, :json-bytes 2594, :marker? true} DIRECT {:ok false, :type :unknown-door-symbol, :reason nil, :max-string 1055, :json-bytes 2704, :marker? true} RUN {:ok false, :type :unknown-door-symbol, :reason nil, :max-string 1055, :json-bytes 2704, :marker? true}
pool-size MCP {:ok false, :type "invalid-mcp-request", :reason "pool-size-out-of-range", :max-string 34, :json-bytes 297, :marker? false} DIRECT {:ok false, :type :invalid-pool-size, :reason nil, :max-string 1055, :json-bytes 1408, :marker? true} RUN {:ok false, :type :invalid-pool-size, :reason nil, :max-string 1055, :json-bytes 1408, :marker? true}
unknown-fields MCP {:ok false, :type "invalid-mcp-request", :reason "unknown-fields", :max-string 1055, :json-bytes 2419, :marker? true} DIRECT {:ok false, :type :unknown-arguments, :reason nil, :max-string 1055, :json-bytes 1429, :marker? true} RUN {:ok false, :type :unknown-arguments, :reason nil, :max-string 1055, :json-bytes 1429, :marker? true}
BETWEEN-EXITS
{:ok false, :type "server-not-initialized", :reason nil, :max-string 41, :json-bytes 258, :marker? false} next-call-bytes 26
```

   The current schema has arrays of strings, not an array of objects; the 10,001-
   character values are placed in the `files` and `doors` leaves. Adding an unknown
   nested field to a redefined schema does not silently green: the derived equality
   fails before pretending the field was driven. It does not auto-generate a nested
   leaf drive; a developer must add that drive to make the equality green.

```text
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/nested-schema.clj

FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6270)
the hostile drives cover every request field the tool declares
declared: #{"workspace_root" "files" "future_items" "doors" :clj-surgeon.mcp-relation-census-test/unknown-field "pool_size"}; driven: #{"workspace_root" "files" "doors" :clj-surgeon.mcp-relation-census-test/unknown-field "pool_size"}
expected: (= (declared-mcp-request-fields) (set (keys mcp-drives)))
  actual: (not (= #{"workspace_root" "files" "future_items" "doors" :clj-surgeon.mcp-relation-census-test/unknown-field "pool_size"} #{"workspace_root" "files" "doors" :clj-surgeon.mcp-relation-census-test/unknown-field "pool_size"}))
nil
```

   The reviewer's exact 723/783/713 receipts now name `doors`, and a nested-map
   synthetic field is weighed by its top-level actionable field name:

```text
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/exact-doors.clj
723 names-doors true "The narrowest continuation this refusal can compute renders as 723 UTF-8 bytes, over the 512-byte ceiling a continuation must fit, so none is offered. The workspace path is not the problem, the length of the doors in it is: doors alone measures 631 of those bytes — retry with a shorter doors, and fix what this refusal named."
783 names-doors true "The narrowest continuation this refusal can compute renders as 783 UTF-8 bytes, over the 512-byte ceiling a continuation must fit, so none is offered. The workspace path is not the problem, the length of the doors in it is: doors alone measures 691 of those bytes — retry with a shorter doors, and fix what this refusal named."
713 names-doors true "The narrowest continuation this refusal can compute renders as 713 UTF-8 bytes, over the 512-byte ceiling a continuation must fit, so none is offered. The workspace path is not the problem, the length of the doors in it is: doors alone measures 621 of those bytes — retry with a shorter doors, and fix what this refusal named."
```

```text
$ java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/probe.clj | rg '^synthetic-'
synthetic-vector {:bytes 828, :entries 1, :field "synthetic_option", :cause :field-length, :measured 758} "The narrowest continuation this refusal can compute renders as 828 UTF-8 bytes, over the 512-byte ceiling a continuation must fit, so none is offered. The workspace path is not the problem, the length of the synthetic_option in it is: synthetic_option alone measures 758 of those bytes — retry with a shorter synthetic_option, and fix what this refusal named."
synthetic-nested-map {:bytes 811, :entries 1, :field "future_options", :cause :field-length, :measured 741} "The narrowest continuation this refusal can compute renders as 811 UTF-8 bytes, over the 512-byte ceiling a continuation must fit, so none is offered. The workspace path is not the problem, the length of the future_options in it is: future_options alone measures 741 of those bytes — retry with a shorter future_options, and fix what this refusal named."
```

6. **The install test now owns the HEAD it asserts.**
   `test/clj_surgeon/install_test.clj:50`,
   `test/clj_surgeon/install_test.clj:306`.

   I made two commits before the run in a git-archive scratch repository, pointed
   HEAD at the first, loaded the namespace, then moved the ref to the second before
   starting the assertion. No commit occurred while the targeted test ran.

```text
$ java -cp "$race_cp" clojure.main /tmp/census18-fx/sol/install-race/race.clj
NAMESPACE-LOADED HEAD= d568cecdd4b9f952096e74a3cd1fcfcba21a80b8
ASSERTION-START HEAD= c908b5eb1fc8d35d0bda046a9cc468c892ba1475
RESULT {:test 1, :pass 130, :fail 0, :error 0}
PROCESS_EXIT=0
HEAD_BEFORE=d568cecdd4b9f952096e74a3cd1fcfcba21a80b8
HEAD_AFTER=c908b5eb1fc8d35d0bda046a9cc468c892ba1475
```

7. **Both requested ratchets are real at the claimed counts.**
   `test/clj_surgeon/mcp_relation_census_test.clj:6242`,
   `test/clj_surgeon/mcp_relation_census_test.clj:6400`.

   On a `git archive 3b7904a` export, `entrance-bounded` replaced by identity:

```text
$ set +e
$ ~/bin/suite-run java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/ratchet-bound.clj > /tmp/census18-fx/sol/ratchet-bound.out 2>&1
$ status=$?
$ rg '^FAIL in|^RATCHET-RESULT' /tmp/census18-fx/sol/ratchet-bound.out
$ echo EXIT=$status
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6233)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6237)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6233)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6237)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6233)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6237)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6229)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6233)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6237)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6301)
FAIL in (every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance) (mcp_relation_census_test.clj:6305)
RATCHET-RESULT {:test 1, :pass 126, :fail 18, :error 0}
EXIT=1
```

   On a separate export, `candidate-field-weights` restricted to
   `[:workspace_root :files]`:

```text
$ set +e
$ ~/bin/suite-run java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main /tmp/census18-fx/sol/ratchet-weight.clj > /tmp/census18-fx/sol/ratchet-weight.out 2>&1
$ status=$?
$ rg '^FAIL in|^RATCHET-RESULT' /tmp/census18-fx/sol/ratchet-weight.out
$ echo EXIT=$status
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6423)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6426)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6423)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6426)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6423)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6426)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6438)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6441)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6443)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6456)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6456)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6456)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6504)
FAIL in (the-overflow-remedy-names-the-heaviest-field-it-measured) (mcp_relation_census_test.clj:6504)
RATCHET-RESULT {:test 1, :pass 22, :fail 14, :error 0}
EXIT=1
```

8. **The committed battery reproduces twice, prints its composition, and is
   byte-identical.** `test/census_witness_battery.clj:32`.

```text
$ ~/bin/suite-run java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main -m census-witness-battery > /tmp/census18-fx/sol/battery-run-1.out 2>&1
$ ~/bin/suite-run java -cp "$(clojure -Spath -M:clj-surgeon/mcp-test)" clojure.main -m census-witness-battery > /tmp/census18-fx/sol/battery-run-2.out 2>&1
$ cmp -s /tmp/census18-fx/sol/battery-run-1.out /tmp/census18-fx/sol/battery-run-2.out; echo CMP_EXIT=$?
$ sha256sum /tmp/census18-fx/sol/battery-run-{1,2}.out
CMP_EXIT=0
7eb5bfc00d3e05f68ad060bbed4c5f4c3201a757c301a7267d994f086940ecef  /tmp/census18-fx/sol/battery-run-1.out
7eb5bfc00d3e05f68ad060bbed4c5f4c3201a757c301a7267d994f086940ecef  /tmp/census18-fx/sol/battery-run-2.out
```

Verbatim composition (both runs):

```text
MISSING: []
COMPOSITION:
  :r15   the-cli-entrance-validates-every-field-before-it-loads-any-config        pass   40  fail 0  error 0
  :r15   the-constructor-refuses-a-files-list-the-published-schema-rejects        pass   46  fail 0  error 0
  :r16   a-read-that-fails-after-the-fence-is-never-an-adapter-crash              pass   27  fail 0  error 0
  :r16   no-refusal-publishes-the-raw-text-of-the-exception-that-produced-it      pass   16  fail 0  error 0
  :r16   an-unreadable-directory-refuses-the-census-on-both-entrances             pass   18  fail 0  error 0
  :r16   a-continuation-over-the-ceiling-names-the-cause-it-measured              pass   11  fail 0  error 0
  :r16   a-continuation-over-the-ceiling-on-a-long-root-names-the-root            pass    4  fail 0  error 0
  :r16   every-refusal-field-is-length-bounded-at-both-entrances                  pass   70  fail 0  error 0
  :r16   a-shape-refusal-on-a-long-root-measures-its-continuation                 pass    7  fail 0  error 0
  :r16   every-continuation-either-entrance-emits-fits-the-byte-ceiling           pass   76  fail 0  error 0
  :r16   the-constructors-are-the-only-continuation-construction-sites            pass   67  fail 0  error 0
  :r18   every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance     pass  144  fail 0  error 0
  :r18   every-declared-refusal-shape-carries-no-field-over-the-ceiling           pass  320  fail 0  error 0
  :r18   the-overflow-remedy-names-the-heaviest-field-it-measured                 pass   36  fail 0  error 0
  :r18   the-two-entrances-name-the-same-cause-for-the-same-observation           pass   46  fail 0  error 0
  :r18   a-refusal-whose-subject-is-the-root-names-the-root                       pass   20  fail 0  error 0
  :r18   naming-a-source-is-not-walking-a-tree                                    pass   14  fail 0  error 0
:BATTERY-RESULT {:test 17, :pass 962, :fail 0, :error 0}
```

9. **Required gates.**

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 460 tests containing 6314 assertions.
0 failures, 0 errors.
EXIT=0
```

```text
$ ~/bin/suite-run make test-fast
Ran 717 tests containing 6061 assertions.
0 failures, 0 errors.
EXIT=0
```

```text
$ swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
EXIT=0
```

   One earlier exact `test-fast` attempt reported four assertions in
   `lens_query_test.clj:502` after the plan succeeded but its apply subprocess exited
   1. The identical public plan/apply command then passed, and the exact full gate
   rerun above passed 717/6061/0. That first red is an unreproduced transient suspicion,
   not a census finding; it is disclosed rather than silently discarded.

Final cleanliness and mode audit:

```text
$ git rev-parse HEAD
3b7904a277f5f5e1306ea85774a27e862e16297e
$ git status --porcelain=v1
$ find /tmp/census18-fx/sol -type d ! -perm -u=rwx -printf 'BAD-DIR %m %p\n'
$ find /tmp/census18-fx/sol -type f ! -perm -u=rw -printf 'BAD-FILE %m %p\n'
$ find /tmp/census18-fx/sol -type l -printf 'SYMLINK %p -> %l\n'
SYMLINK /tmp/census18-fx/sol/ws/src/app/escape.clj -> ../../../outside/secret.clj
```

## NO-GO

This tip is not GO on its own for MCP/main: the MCP census entrance repairs are real, but main still ships unbounded pre-dispatch CLI refusals and a CLI `:file` path that demonstrably reads outside the censused tree.
