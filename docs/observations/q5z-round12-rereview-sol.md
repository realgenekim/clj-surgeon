## NO-GO

# Round-twelve adversarial review — `bridge/q5z-alias-migration` at `61dd334`

The review worktree was not changed, staged, committed, stashed, or pushed. All review
fixtures and scratch clones were under `/tmp/q5z12-review-fx`. Source was read with
`rg`/`sed`; no Surgeon MCP read was used. The only live tool call was the FAN gate's one
`alias_migration` call on explicit port 7975; that server was stopped afterward.

Entry and exit proof (the output between HEAD and the socket header is empty: clean status,
empty stash list):

```text
$ git rev-parse HEAD; git status --porcelain; git stash list; ss -ltn 'sport = :7975'
61dd334d56f693a7be59003c7bc942ada112ce27
State Recv-Q Send-Q Local Address:Port Peer Address:Port
EXIT=0
```

## Findings

1. **BLOCKING — a legal POSIX backslash directory is dropped from `scope.paths ["**"]`, so the verb commits a partial migration while claiming complete discovery.** `src/clj_surgeon/mcp_alias_migration.clj:195-197` converts every `\` in a Unix project-relative filename into `/`; `:530-568` then derives remedies from that corrupted whole-tree scan. This violates `docs/intent/alias-migration/alias-migration-specs.md:20` (`MCP-OP-ALIAS-004`, discover every requiring namespace). The escape table at `mcp_alias_migration.clj:488-503` cannot repair a filename that `relative-path` already discarded. The two-owner fixture had one ordinary owner and one owner under the top-level directory whose exact name is `\`. The operation committed the ordinary owner and left the second owner requiring the old lib.

   Exact command:

   ```text
   $ mv /tmp/q5z12-review-fx/backslash-partial /tmp/q5z12-review-fx/backslash-partial-second && ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /tmp/q5z12-review-fx/backslash_partial.clj'
   ```

   Verbatim output:

   ```text
   receipt => {:ok true, :committed true, :files 1, :sites 1}
   normal file migrated? => true
   backslash file still old? => true
   scan => {:ok true, :files [src/a.clj]}

   EXIT:0
   ```

   The narrower hostile-name probe also showed the alleged escaped pattern is parser-valid
   but selects nothing: `"\\" => "\\\\/**" parse-error=nil selected=[]`; a nested name
   ending in backslash behaved identically. By contrast, `?`, `**`, `a?`, `[x]`, `{a,b}`,
   `*`, `a{b`, spaces, and Unicode were escaped, parsed, and matched exactly themselves.
   This is not merely a remedy-quality defect: the successful one-file commit above breaks
   the feature's N-owner closure guarantee.

2. **GO-WITH-FIX — the “bounded” no-`next_call` root listing can push the visible refusal past its stated 4,096-character ceiling.** `src/clj_surgeon/mcp_alias_migration.clj:645-653` bounds `root-sizes` only by item count, while `:975-990` embeds its `pr-str` whole in `:remedy`; `:775-782` states 4,096 as the whole-text ceiling but does not enforce it. Seven legal root names, each consisting of a digit and 246 quotation marks, make the six-item list 3,019 JSON characters and the visible refusal 6,609 characters. This falsifies `MCP-OP-ALIAS-059` at `docs/intent/alias-migration/alias-migration-specs.md:106` and the builder's root-list attack, although the round-11 “phantom resend” itself is fixed.

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /tmp/q5z12-review-fx/root_blowout.clj'
   ```

   Verbatim output:

   ```text
   error_type => alias-migration-scope-matches-nothing
   next_call => nil
   root count/listed => 7 6
   root list count/json length/max item => 6 3019 254
   error/remedy/text lengths => 1928 3446 6609
   text ceiling => 4096

   EXIT:0
   ```

   The ordinary long-root and replay controls held: with a 1,145-character shortest call,
   `next_call` was nil, the remedy did not say `Resend the next_call`, and it named `1145 /
   512`; with roots `alpha`, `root with spaces`, and `ρίζα`, the published call replayed and
   reached discovery with `scanned_files => 3`. Thus the “selects all” prose is backed by
   replay in the covered case; the remaining failure is the unbounded rendering above.

3. **GO-WITH-FIX — the advertised independent 36-kind enumeration misses a live refusal forwarded dynamically from a helper namespace.** `test/clj_surgeon/mcp_alias_migration_test.clj:1051-1098` scans six fixed sources. `src/clj_surgeon/mcp_alias_migration.clj:2236-2240` forwards `(:error-type baseline)`, but `src/clj_surgeon/mcp_change_buffer.clj:1445` constructs `:invalid-diagnostic-output` and is not scanned. A configured `clj-kondo` command returning `not-edn` reaches that refusal before write. The current generic renderer happens to show the kind, but the claimed source-derived witness does not cover it.

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /tmp/q5z12-review-fx/helper_refusal.clj'
   ```

   Verbatim output:

   ```text
   enumerated count => 36
   enumerated contains invalid-diagnostic-output? => false
   live error_type => "invalid-diagnostic-output"
   live source_unchanged => true

   EXIT:0
   ```

4. **GO-WITH-FIX — only NUL is typed; other invisible/control path spellings fall through to the false tree claim `scope-matches-nothing`.** `src/clj_surgeon/mcp_alias_migration.clj:233` special-cases only `U+0000`, and `:457-469` constructs only the NUL refusal. `U+0001`, `U+007F`, and an unpaired high surrogate all compiled and were reported as a tree with no match, contrary to the requested typed-path boundary.

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /tmp/q5z12-review-fx/control_paths.clj'
   ```

   Verbatim output:

   ```text
   NUL => "alias-migration-scope-path-refused"
   SOH => "alias-migration-scope-matches-nothing"
   DEL => "alias-migration-scope-matches-nothing"
   LONE-HIGH-SURROGATE => "alias-migration-scope-matches-nothing"

   EXIT:0
   ```

   **Suspicion, not a live-MCP reproduction:** overlong UTF-8 cannot exist as a JVM string,
   so I did not spend the one permitted live call on it. The exact Jackson version on this
   classpath accepts the overlong `C0 AF` encoding of `/` and normalizes it to `/`, meaning
   the verb cannot type it after decoding. Whether the SDK's HTTP layer uses the same byte
   path was not reproduced.

   ```text
   $ ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /tmp/q5z12-review-fx/overlong_utf8.clj'
   parsed => #object[java.util.LinkedHashMap 0x4390f46e {path=src//x}]

   EXIT:0
   ```

5. **Non-blocking — `#_` reader-discarded strings are counted as exact mention sites, contrary to ALIAS-034's reader-defined wording.** `src/clj_surgeon/alias_migration.clj:1113-1133` recursively descends every rewrite-clj node and does not stop at `:uneval`, although the migration walker itself explicitly stops at `:uneval` at `:424`. `docs/intent/alias-migration/alias-migration-specs.md:75` says a string literal is what the reader says one is and the count is exact. The real reader drops `#_"..."`; the mention scan keeps it. A string containing regex syntax, a regex, and a semicolon comment were correctly excluded; a string inside `(comment ...)`, a docstring, and an ordinary string were correctly counted.

   Exact commands and verbatim outputs:

   ```text
   $ ~/bin/suite-run bash -lc 'exec java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /tmp/q5z12-review-fx/string_shapes.clj'
   sites => ["src/shapes.clj:3" "src/shapes.clj:4" "src/shapes.clj:5" "src/shapes.clj:6"]
   inside-string/comment-form/discard/docstring/ordinary/regex/comment => [false true true true true false false]

   EXIT:0

   $ ~/bin/suite-run clojure -M -e '(println (read-string "[#_\"old.store/find-event\" :sentinel]"))'
   [:sentinel]

   EXIT:0
   ```

6. **GO-WITH-FIX — the 9a and claim-3 future-regression witnesses have false negatives under the requested sabotage.** `test/clj_surgeon/mcp_alias_migration_test.clj:1203-1249` recognizes only literal `(refusal :kind` and literal `:error-type :kind` shapes in fixed files. `:1310-1400` derives a key name from source but tests every discovered key with the synthetic string value `"probe-value"`; the production renderer at `src/clj_surgeon/mcp_tool.clj:1280-1310` deliberately omits maps. On a scratch copy I added (a) a dynamically spelled refusal constructor and (b) a nested-map fact to the live exhausted-policy refusal. Both advertised witnesses stayed green; neither scan saw the dynamic kind, and the live text omitted the nested key. This directly contradicts “fails on the day it is written.”

   Exact command:

   ```text
   $ ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -A:clj-surgeon/mcp-test); java -cp "$cp" clojure.main /tmp/q5z12-review-fx/historical_counts.clj the-refusal-enumeration-contains-every-kind-the-entrance-constructs every-refusal-key-the-verb-constructs-appears-in-its-text-block; printf "WITNESS_EXIT=%s\n" "$?"; java -cp "$cp" clojure.main /tmp/q5z12-review-fx/sabotage_probe.clj; printf "PROBE_EXIT=%s\n" "$?"'
   ```

   Verbatim output:

   ```text
   {:test 2, :pass 49, :fail 0, :error 0}
   WITNESS_EXIT=0
   dynamic sabotage enumerated? => false
   dynamic sabotage constructor-scanned? => false
   live sabotage key present structurally? => true
   live sabotage key named in text? => false
   text =>
    alias_migration
     refused · alias-migration-alias-policy-exhausted · 1.00 ms

   ✓ source unchanged
   → Every alias_policy entry is already bound in src/demo.clj
   facts · alias_policy=["store2"] · collided_bindings=["store2"] · file="src/demo.clj" · mutation_attempted=false · next_action="correct_request" · source_unchanged=true · write_authority=false
   remedy · to.alias_policy is exhausted for src/demo.clj: every one of its 1 entries — ["store2"] — is already bound to another namespace in that file's ns form. No next_call is composed, because any alias this verb could propose would be outside the policy you sent and your own request forbids it. Add an alias that file does not bind to to.alias_policy, or exclude src/demo.clj through scope.exclude, and resend.
   next_call · none — this refusal has no mechanically composable correction; the remedy above names what only the caller can decide
   PROBE_EXIT=0

   EXIT:0
   ```

7. **Resolved at this tip, but a finding class as requested: exactly two pre-existing witnesses pinned the 9b defect.** At `9728073`, the planner suite required “the next_call appends one more policy entry,” and the MCP suite required `["store2" "store2-2"]`. Both were corrected by `61dd334`; no other assertion pins a policy-external alias. The only other `next_call` size assertions require `<= 512`, so I found no other assertion pinning an unbounded call.

   Exact command and verbatim output:

   ```text
   $ for f in test/clj_surgeon/alias_migration_test.clj test/clj_surgeon/mcp_alias_migration_test.clj; do git show 9728073:$f; done | rg -n -C 1 'the next_call appends one more policy entry|\["store2" "store2-2"\]'; rg -n 'is \(= .*store-2-2|is \(= .*store2-2|the next_call appends one more policy entry|next_call is .*characters|<= \(count rendered\) 512' test/clj_surgeon/alias_migration_test.clj test/clj_surgeon/mcp_alias_migration_test.clj
   342-           (get-in plan [:next_call "to" "alias_policy"]))
   343:        "the next_call appends one more policy entry")))
   344-
   --
   2362-        (is (= ["store2"] (:collided_bindings result)))
   2363:        (is (= ["store2" "store2-2"] (get-in result [:next_call "to" "alias_policy"]))))
   2364-      (finally
   test/clj_surgeon/mcp_alias_migration_test.clj:2292:            (is (<= (count rendered) 512)
   test/clj_surgeon/mcp_alias_migration_test.clj:2293:                (str "next_call is " (count rendered) " characters"))
   test/clj_surgeon/mcp_alias_migration_test.clj:2330:            (is (<= (count rendered) 512)
   test/clj_surgeon/mcp_alias_migration_test.clj:2331:                (str "next_call is " (count rendered) " characters"))

   EXIT:0
   ```

## Adversarial checks that held

- Numeric mention ranking held across 27 sites in `src/f1.clj`, `src/f10.clj`, and
  `src/f2.clj`: each file sorted lexically and its lines sorted numerically (`2..10`), and
  the first 20 were the expected prefix.
- Caller-input bounds held on a 10,001-character malformed path: path/pattern/cause were
  `228/228/228`, error `565`, visible text `1918`; the path marker named `10001` and the
  parser-cause marker honestly named its own `20031`. A 389-character depth-refusal path
  retained the first 200 characters and named `389`.
- Sixteen facts rendered with no truncation claim; seventeen rendered the first sixteen
  followed by `+1 more in structuredContent`.
- Confinement held. `../outside/**`, an absolute outside glob, `link/**` where `link` was a
  symlink outside, `src/../../outside/**`, and `..` each selected `[]`; the control `src/**`
  selected only `src/in.clj`. No scope selected a path outside the workspace, and no refusal
  published a server-derived absolute outside path. There is therefore no separate automatic
  blocking finding under the review's explicit outside-root rule.
- The copied E-PREWRITE fixture reproduced 9b exactly, and one-entry/duplicate/empty policy
  edges behaved consistently.

Exact policy-edge command:

```text
$ ~/bin/suite-run bash -lc 'java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main /tmp/q5z12-review-fx/probes.clj | sed -n "/=== P9b one-entry/,\$p"'
```

Verbatim output:

```text
=== P9b one-entry duplicate and empty policies ===
["only"] => {:ok false, :error_type alias-migration-alias-policy-exhausted, :file src/demo.clj, :collided_bindings [only], :next_call nil, :remedy to.alias_policy is exhausted for src/demo.clj: every one of its 1 entries — ["only"] — is already bound to another namespace in that file's ns form. No next_call is composed, because any alias this verb could propose would be outside the policy you sent and your own request forbids it. Add an alias that file does not bind to to.alias_policy, or exclude src/demo.clj through scope.exclude, and resend.}
["dup" "dup"] => {:ok false, :error_type alias-migration-alias-policy-exhausted, :file src/demo.clj, :collided_bindings [dup dup], :next_call nil, :remedy to.alias_policy is exhausted for src/demo.clj: every one of its 2 entries — ["dup" "dup"] — is already bound to another namespace in that file's ns form. No next_call is composed, because any alias this verb could propose would be outside the policy you sent and your own request forbids it. Add an alias that file does not bind to to.alias_policy, or exclude src/demo.clj through scope.exclude, and resend.}
empty through validation => {:ok false, :error_type invalid-mcp-request, :error to.alias_policy must be one non-empty array of names, :path [to alias_policy]}

=== P9b copied E-PREWRITE fixture ===
error_type => "alias-migration-alias-policy-exhausted"
file => "src/acid/fanout/ns_100.clj"
collided_bindings => ["store2" "st2" "es" "store-2"]
next_call => nil
remedy => to.alias_policy is exhausted for src/acid/fanout/ns_100.clj: every one of its 4 entries — ["store2" "st2" "es" "store-2"] — is already bound to another namespace in that file's ns form. No next_call is composed, because any alias this verb could propose would be outside the policy you sent and your own request forbids it. Add an alias that file does not bind to to.alias_policy, or exclude src/acid/fanout/ns_100.clj through scope.exclude, and resend.

EXIT:0
```

## RED→GREEN history

Each RED witness was run at its own commit and the same test var(s) at the immediately
following fix. Exact command:

```text
$ cd /tmp/q5z12-review-fx/history-wt && ~/bin/suite-run /tmp/q5z12-review-fx/redgreen_counts.sh
```

Verbatim output:

```text
{:test 2, :pass 6, :fail 9, :error 0}
9eeb454 RED EXIT=1
{:test 2, :pass 15, :fail 0, :error 0}
0228692 GREEN EXIT=0
{:test 1, :pass 19, :fail 9, :error 0}
2f9872f RED EXIT=1
{:test 1, :pass 28, :fail 0, :error 0}
b066a66 GREEN EXIT=0
{:test 1, :pass 4, :fail 4, :error 0}
ce35745 RED EXIT=1
{:test 1, :pass 8, :fail 0, :error 0}
790b6e8 GREEN EXIT=0
{:test 2, :pass 3, :fail 4, :error 0}
da38105 RED EXIT=1
{:test 2, :pass 7, :fail 0, :error 0}
d252b8e GREEN EXIT=0
{:test 1, :pass 2, :fail 11, :error 0}
5fe66b3 RED EXIT=1
{:test 1, :pass 16, :fail 0, :error 0}
2872b06 GREEN EXIT=0
{:test 1, :pass 1, :fail 1, :error 0}
751b664 RED EXIT=1
{:test 1, :pass 2, :fail 0, :error 0}
06892e0 GREEN EXIT=0
{:test 1, :pass 2, :fail 3, :error 0}
0cb69e8 RED EXIT=1
{:test 1, :pass 5, :fail 0, :error 0}
707cee0 GREEN EXIT=0
{:test 1, :pass 3, :fail 15, :error 0}
d78362c RED EXIT=1
{:test 1, :pass 18, :fail 0, :error 0}
21e4e4e GREEN EXIT=0
{:test 1, :pass 35, :fail 5, :error 0}
e87de0e RED EXIT=1
{:test 1, :pass 40, :fail 0, :error 0}
e6166b4 GREEN EXIT=0
{:test 1, :pass 3, :fail 4, :error 0}
9728073 RED EXIT=1
{:test 1, :pass 7, :fail 0, :error 0}
61dd334 GREEN EXIT=0

EXIT:0
```

The disclosed round-5 correction is genuine. A scratch tree at `2872b06` retained its
corrected test while both production source files were restored from `5fe66b3`;
`git diff --quiet 5fe66b3 -- <both source files>` returned `OLD_SOURCE_DIFF_EXIT=0`. The
corrected witness then remained red, and in fact found three more failures than the original
wording because `:cause` correctly names its own 20,031-character length:

```text
$ cd /tmp/q5z12-review-fx/reproof-wt && ~/bin/suite-run bash -lc 'cp=$(clojure -Spath -A:clj-surgeon/mcp-test); java -cp "$cp" clojure.main /tmp/q5z12-review-fx/historical_counts.clj a-caller-sized-scope-entry-cannot-grow-the-refusal-text; printf "EXIT=%s\n" "$?"'
{:test 1, :pass 2, :fail 14, :error 0}
EXIT=1

EXIT:0
```

## Release gates

```text
$ ~/bin/suite-run bb test/run_all.clj
Ran 737 tests containing 6275 assertions.
0 failures, 0 errors.

EXIT:0

$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 479 tests containing 5912 assertions.
0 failures, 0 errors.

EXIT:0

$ make mcp-operation-oracle
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]

EXIT:0

$ make repository-hygiene
# @spec MCP-OP-ALIAS-036
# @spec MCP-OP-ALIAS-053
repository hygiene: no machine-local build cache is tracked at any depth

EXIT:0
```

## FAN gate

Fresh generation:

```text
$ bb /tmp/q5z12-review-fx/fan/tools/bench/fanout/gen-fanout.clj --n 21 --seed 7 --k 6 --out /tmp/q5z12-review-fx/fan/gen
gen-fanout: n=21 seed=7 k=6 namespaces=100 targets=21 out=/tmp/q5z12-review-fx/fan/gen
gen-fanout: alias histogram {"st2" 5, "store-2" 5, "es" 5, "store2" 6}
gen-fanout: old-alias histogram {"st" 4, "db" 3, "s" 4, "store" 4, "repo" 3, "k" 3} collisions=30
```

The server command was
`clojure -X:clj-surgeon/mcp :project-dir '"/tmp/q5z12-review-fx/fan/wt"' :port 7975`.
Startup output:

```text
clj-surgeon MCP: embedded nREPL on 43819 ( /tmp/q5z12-review-fx/fan/wt/.nrepl-port )
clj-surgeon MCP: persistent server ready on http://127.0.0.1:7975/mcp
```

Exactly one live tool call, via `/tmp/q5z12-review-fx/fan/call.sh`:

```text
--- ONE alias_migration call ---
id: a15475d9-255f-419a-a4bd-32c293b37148
event: message
data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"alias_migration\n  21 files · 63 sites · aliases {\"es\" 5, \"st2\" 5, \"store-2\" 5, \"store2\" 6} · 30 collisions resolved · 669.06 ms\n\n✓ atomic commit complete\n✓ written bytes read back and verified\n✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-625f8127-6682-4d3a-82fb-0e7490dd6a41.edn (best-effort retention)"}],"isError":false,"structuredContent":{"details_retained":20,"workspace_root":"/tmp/q5z12-review-fx/fan/wt","committed":true,"kondo_delta":{"status":"not-requested"},"alias_histogram":{"es":5,"st2":5,"store-2":5,"store2":6},"sites":63,"string_mention_sites_shown":0,"details_retention":"best-effort","focused_test":{"status":"not-requested"},"string_mentions":0,"string_mention_sites":[],"lib_renamed":null,"elapsed_ms":669.057625,"next_action":"none","refer_sites":0,"files":21,"collisions_resolved":30,"receipt_hash":"afc05ab6dc4eff4730c4fed3a4c6704388a4ce6c8f8774c854e95f2f56139f68","ok":true,"operation":"alias_migration","undo_receipt":"/home/forge/.local/state/clj-surgeon/workspaces/3fdc996aff5f3268dd5d1b003ba89c8f98f485dd3fb9f4d2dc542a5105875a3f/receipts/fcbe9077-d40a-4efe-b86c-b2185d56a6ed.edn","details_path":".clj-surgeon/alias-migration/detail-625f8127-6682-4d3a-82fb-0e7490dd6a41.edn"}}}

EXIT:0
```

The server was stopped with Ctrl-C (`EXIT:130`); `ss -ltn 'sport = :7975'` then printed only
its header. The first scorer run correctly caught the server's generated `.nrepl-port` as
one extra changed file; checks 2–6 passed. Verbatim first-run checks:

```text
CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 [".nrepl-port"]
CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
fan_check: FAILED CHECK 1 file-set
CHECK 4 load: PASS namespaces=100 rc=0
CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)

EXIT:1
```

I retained that generated marker as `/tmp/q5z12-review-fx/fan/nrepl-port.generated` outside
the scored worktree and reran the scorer. Final command and every check line verbatim:

```text
$ FAN_BASE=6ea79f7a70964532cda34a05e849e5bb373a49e9 ~/bin/suite-run /tmp/q5z12-review-fx/fan/tools/bench/fanout/rescore-FAN.sh /tmp/q5z12-review-fx/fan/wt 21 /tmp/q5z12-review-fx/fan/gen
rescore-FAN: worktree=/tmp/q5z12-review-fx/fan/wt n=21 base=6ea79f7a70964532cda34a05e849e5bb373a49e9 fixtures=/tmp/q5z12-review-fx/fan/gen
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
fan_check: 4/4 structural checks passed
CHECK 4 load: PASS namespaces=100 rc=0
CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
rescore-FAN: 6/6 checks passed

EXIT:0

$ diff -r --exclude=.git --exclude=.clj-surgeon /tmp/q5z12-review-fx/fan/wt /tmp/q5z12-review-fx/fan/gen/canonical-21

EXIT:0
```

## NO-GO

The mayor must verify a RED→GREEN fix that preserves literal POSIX backslashes (and proves
two-owner closure), hard-bounds the root-list refusal, covers dynamically forwarded helper
refusals and non-scalar facts, and types invisible path spellings before this joins the
queue; round 10's GO-WITH-FIX and round 11's GO-WITH-FIX are both superseded, and this tip is
**not GO on its own**.
