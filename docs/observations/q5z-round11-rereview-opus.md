## GO-WITH-FIX

# q5z round 11 — independent Opus review of `bridge/q5z-alias-migration` at `656a0a3c`

Executed in a fresh clone at `/home/forge/tmp/sol/q5z11-wt`. HEAD proved:

```
$ git clone -q https://github.com/realgenekim/clj-surgeon.git /home/forge/tmp/sol/q5z11-wt
$ cd /home/forge/tmp/sol/q5z11-wt && git checkout -q 656a0a3c871d13fac1ea34b874fdc44e1ec5e9ea && git rev-parse HEAD
656a0a3c871d13fac1ea34b874fdc44e1ec5e9ea
$ git status --porcelain      # empty at entry AND at exit
$ git stash list | wc -l
0
```

Nothing committed, stashed, pushed, or `git add`ed on any tree. One server started, on
port **7947** (explicit, above 7940), stopped after the single call; ports 7888 / 7894 /
7895 never contacted. No `~/acid` path, no `chain-*.sh`, no fleet directory touched. No
Surgeon MCP tool used for reading — `rg`/`sed`/`grep` only. All fixtures under
`/tmp/q5z11-review-fx` (trusted before use). JVM work through `~/bin/suite-run`;
`clojure -M:clj-surgeon/mcp-test` run directly, never `make mcp-test`. No process
signalled that I did not start (the one `kill` was the server I launched, PID 1352963).

---

## Gates — one run each, verbatim ran-lines and exit codes

| gate | command | result | builder's claim |
|---|---|---|---|
| bb suite | `~/bin/suite-run bb test/run_all.clj` | `Ran 737 tests containing 6273 assertions.` / `0 failures, 0 errors.` / `EXIT=0` | 737 / 6273 / 0 — **match** |
| mcp suite | `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 467 tests containing 5658 assertions.` / `0 failures, 0 errors.` / `EXIT=0` | 467 / 5658 / 0 — **match** |
| oracle | `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` / `EXIT=0` | pass — match |
| hygiene | `make repository-hygiene` | `repository hygiene: no machine-local build cache is tracked at any depth` / `EXIT=0` | green — match |

## The FAN gate, re-run independently

Bench scripts copied from `origin/bridge/fanout-fixtures-in-git` (`bench/fanout/gen-fanout.clj`,
`rescore-FAN.sh`, `fan_check.clj`); fixtures generated fresh:

```
$ bb gen-fanout.clj --n 21 --seed 7 --k 6 --out /tmp/q5z11-review-fx/fan/gen
gen-fanout: n=21 seed=7 k=6 namespaces=100 targets=21
gen-fanout: alias histogram {"st2" 5, "store-2" 5, "es" 5, "store2" 6}
gen-fanout: old-alias histogram {"st" 4, "db" 3, "s" 4, "store" 4, "repo" 3, "k" 3} collisions=30
```

`repo-21` copied to a git-initialised worktree, base `6ea79f7a70964532cda34a05e849e5bb373a49e9`.
One live server (`clojure -X:clj-surgeon/mcp :project-dir '"/tmp/q5z11-review-fx/fan/wt"' :port 7947 …`),
**one** `tools/call alias_migration` over Streamable HTTP with the cohort's exact spelling
(`scope.paths ["src"]`, `expect.files 21`, `refer_policy preserve-refer`):

```
{"content":[{"type":"text","text":"alias_migration\n  21 files · 63 sites ·
 aliases {\"es\" 5, \"st2\" 5, \"store-2\" 5, \"store2\" 6} · 30 collisions resolved · 631.20 ms\n\n
 ✓ atomic commit complete\n✓ written bytes read back and verified\n
 ✓ terminal evidence · per-file detail at .clj-surgeon/alias-migration/detail-b1c74796-….edn"}],
 "isError":false, "structuredContent":{…"committed":true,"sites":63,"files":21,
 "collisions_resolved":30,"string_mentions":0,"string_mention_sites":[],"elapsed_ms":631.197543…}}
```

Then rescored (server stopped first, port freed):

```
$ FAN_BASE=$(cat base.sha) ~/bin/suite-run ./rescore-FAN.sh /tmp/q5z11-review-fx/fan/wt 21 /tmp/q5z11-review-fx/fan/gen
rescore-FAN: worktree=/tmp/q5z11-review-fx/fan/wt n=21 base=6ea79f7a70964532cda34a05e849e5bb373a49e9 fixtures=/tmp/q5z11-review-fx/fan/gen
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
fan_check: 4/4 structural checks passed
CHECK 4 load: PASS namespaces=100 rc=0
CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
rescore-FAN: 6/6 checks passed

$ diff -r --exclude=.git --exclude=.clj-surgeon wt gen/canonical-21 && echo "BYTE-IDENTICAL to canonical-21"
BYTE-IDENTICAL to canonical-21
```

**The E3-P clause-1 result reproduces exactly: one call, 21/63/30, 6/6, byte-identical.**

## Every RED commit is genuinely red

Each pair run with `clojure.test/run-tests 'clj-surgeon.mcp-alias-migration-test` at the
exact commit (`/tmp/q5z11-review-fx/redcheck.sh <sha>`, under `suite-run`):

| red commit | at red | at the following fix | fix commit |
|---|---|---|---|
| `ae60c8c6` | `{:test 75, :pass 1397, :fail 7, :error 0}` | `{:test 75, :pass 1406, :fail 0, :error 0}` | `518ba104` |
| `078f93e8` | `{:test 76, :pass 1412, :fail 10, :error 0}` | `{:test 76, :pass 1422, :fail 0, :error 0}` | `9b8f4133` |
| `47f0a355` | fail (7 assertions in `a-malformed-scope-glob-is-a-typed-refusal-not-an-adapter-failure`) | `{:test 77, :pass 1557, :fail 0, :error 0}` | `bcfce43d` |
| `ddeae5e7` | fail (7 across `the-refusal-enumeration-covers-every-kind-the-entrance-can-emit` and `an-adapter-failure-renders-through-the-tools-own-summary`) | `{:test 81, :pass 1628, :fail 0, :error 0}` | `9802d6c8` |
| `a97a391a` | `{:test 83, :pass 1630, :fail 5, :error 0}` | `{:test 83, :pass 1635, :fail 0, :error 0}` | `656a0a3c` |

Load-bearing failing assertions, verbatim, from `ae60c8c6`:

```
FAIL in (the-derived-remedy-never-drops-a-source-root-in-silence) (mcp_alias_migration_test.clj:487)
the remedy dropped the root holding 100 of the 118 sources
expected: (contains? (set (:suggested_paths result)) "src/**")
  actual: (not (contains? #{"r3/**" "r5/**" "r0/**" "r1/**" "r4/**" "r2/**"} "src/**"))
FAIL in (the-derived-remedy-never-drops-a-source-root-in-silence) (mcp_alias_migration_test.clj:497)
the remedy's next_call selects fewer files than the walk saw
expected: (= 118 (:scanned_files replayed))
  actual: (not (= 118 14))
```

**No witness in this round was decorative.**

## Claim-by-claim

**Claim 1 — derived remedy no longer drops source roots: HOLDS on ranking and completion,
breaks in two reachable corners (findings 1 and 2).** Ties are deterministic and the
completing `**` makes the replay complete:

```
=== nine roots, all TIED at 3 files each ===
suggested-scope-paths => {:source-files 27, :roots 9, :roots-listed 6, :truncated? true,
                          :paths ["a/**" "b/**" "c/**" "d/**" "e/**" "f/**" "**"]}
replay selects 27 of 27
=== 20 roots (src holds 40) ===
suggested => {:source-files 78, :roots 20, :roots-listed 6, :truncated? true,
              :paths ["r00/**" … "r04/**" "src/**" "**"]}
replay selects 78
=== exactly 6 roots — at the bound, NOT truncated ===
suggested => {:source-files 12, :roots 6, :roots-listed 6, :truncated? false,
              :paths ["r0/**" … "r5/**"]}
replay selects 12
```
A root name that sorts after `**` is a non-issue: `completing-scope-path` is appended last
and matcher order is irrelevant (patterns are OR'd in `scan-parsed-scope`).

**Claim 2 — subtree glob built from the normalized entry: HOLDS, and confinement holds.**
Thirty spellings driven through the real `scope-glob-patterns` / `scan-scope` / `expand-scope`
against a root holding `src/a.clj`, `src/sub/b.clj`, plus two symlinks (`lnk`, `src/lnk2`)
pointing at an outside tree containing `secret.clj`:

```
"src"                patterns=["src" "src/**"]            selected=["src/a.clj" "src/sub/b.clj"]
"./src" "src/." "src/" "src//" "./src/" ".///src" "./src/." "src/./"   — all identical to "src"
"src/../src"         patterns=["src/../src" "src/**"]     selected=["src/a.clj" "src/sub/b.clj"]
"."  "./"  ".."  "../"  "../../"  "src/../.."             selected=[]
"src/../../outside"  "../outside"  "../outside/**"        selected=[]
"/tmp/q5z11-review-fx/conf/outside"  and its "/**" form   selected=[]
"lnk" "lnk/**" "lnk/deep" "src/lnk2" "src/lnk2/**"        selected=[]
"**"                 selected=["src/a.clj" "src/sub/b.clj"]
"../**"  "**/../**"                                       selected=[]

=== did any outside file get selected anywhere? ===
done          # nothing printed — no ESCAPE line for any probe
```
**No path resolving outside the workspace root selected a byte.** `..`, `src/../..`,
absolute entries and both symlinks contribute no subtree pattern, and the raw caller
pattern that is still carried forward cannot match a project-relative path. The refusal
of `.` / `./` is ALIAS-057's stated boundary and is now asserted, not assumed.

**Claim 3 — malformed globs are a typed refusal: HOLDS.** All eight round-10 escapees are
typed before the first visited entry, and nested groups are caught too:

```
"["                parse-error="Missing '] near index 0"
"{a,b"             parse-error="Missing '} near index 3"
"**/{"             parse-error="Missing '} near index 3"
"src/{**"          parse-error="Missing '} near index 6"
"src/[a-"          parse-error="Missing '] near index 7"
"src/**\\"         parse-error="No character to escape near index 6"
"\\"               parse-error="No character to escape near index 0"
"a{b"              parse-error="Missing '} near index 2"
"{a,{b,c}}"        parse-error="Cannot nest groups near index 3"
"{{}}"             parse-error="Cannot nest groups near index 1"
"src/**"           parse-error=nil          # controls still reach the walk
"src/{clj,cljs}/**" parse-error=nil
```
`**/**/**/**` over a 40-branch × 10-deep tree: `selected 40 in 15 ms` — no explosion.
See finding 5 for the length bound and finding 8 for NUL.

**Claim 4 — refusal enumeration derives from the whole entrance: FALSIFIED (finding 3).**
The adapter-failure half is sound (see below); the derivation is still incomplete.

**Claim 5 — `string_mentions` real in var mode: HOLDS on the needle, breaks on the bound
(finding 4).** The surviving-lib rule is stated in `MCP-OP-ALIAS-034` (specs line 75:
*"a var migration leaves the lib and its other vars standing and a string naming the
survivor is not stale work"*), witnessed by `the-string-needle-follows-what-the-migration-retires`,
and reproduced: `var needle on a bare-lib string => []`.

---

# Findings

### 1 — BLOCKING-adjacent (fix before merge). The remedy says "Resend the next_call" on a receipt whose `next_call` is `nil`, in the same text block that says there is none.

`src/clj_surgeon/mcp_alias_migration.clj:751` (`:next_call (planner/rescoping-call request paths)`)
and `:771-776` (the non-truncated remedy branch); `src/clj_surgeon/alias_migration.clj:719`
(`(when (and (seq paths) (within-next-call-bound? call)) call)` — nil past 512 characters).

Six top-level source roots with ordinary long names blow the next_call bound. Command:

```
$ ~/bin/suite-run java -cp "$(cat /tmp/q5z11-review-fx/cp.txt)" clojure.main /tmp/q5z11-review-fx/p7.clj
```

Verbatim output (trimmed only where the six root names repeat):

```
error_type    => "alias-migration-scope-matches-nothing"
next_call     => nil
remedy        => Resend the next_call: it replaces scope.paths with ["a-very-long-but-entirely-
ordinary-source-root-name-0/**" … "…-5/**"], every one of the 6 source roots this tree holds,
selecting all 6 of its sources. expect.files declared 1 and is left as declared, because no file
was read.

--- rendered text block ---
…
remedy · Resend the next_call: it replaces scope.paths with [… six roots …], every one of the 6
source roots this tree holds, selecting all 6 of its sources. …
next_call · none — this refusal has no mechanically composable correction; the remedy above names
what only the caller can decide
```

Two adjacent lines of one text block tell the caller to resend a next_call and that no
next_call exists. This is the exact defect class commit `9802d6c8` fixed on the other
side of the same renderer — *"the stated absence shall NOT name a remedy the receipt does
not carry"* — inverted: the remedy names a `next_call` the receipt does not carry. It also
qualifies the round's headline claim: the replayed call selects the whole selection **only
while the call fits**, and past the bound there is no call at all. Fix: make the remedy
prose a function of `(:next_call …)` — when it is nil, say the scope.paths to send rather
than "resend the next_call", or truncate the listing further until the call fits (the
completing `**` already keeps the selection complete at any listing size).

### 2 — BLOCKING-adjacent (fix before merge). The derived remedy can itself be an unparseable glob, refused by this same round's new ALIAS-051 check.

`src/clj_surgeon/mcp_alias_migration.clj:518-528` (`suggested-scope-paths` builds
`"<dirname>/**"` from the raw directory name) against `:454-472` (`scan-scope`'s new
`glob-parse-error` gate, added by `bcfce43d`).

A top-level directory whose name contains a glob metacharacter — `{`, `[`, or a trailing
`\` — is legal on POSIX and makes the published correction unexecutable.

```
$ ~/bin/suite-run java -cp "$(cat /tmp/q5z11-review-fx/cp.txt)" clojure.main /tmp/q5z11-review-fx/p1.clj
=== P1b: root whose NAME is a glob metacharacter group ===
suggested-scope-paths => {:source-files 6, :roots 3, :roots-listed 3, :truncated? false,
                          :paths ["a{b/**" "src/**" "zz/**"]}
glob-parse-error on each derived pattern:
   "a{b/**" -> "Missing '} near index 5\na{b/**\n     ^"
   "src/**" -> nil
   "zz/**" -> nil
REPLAY through scan-scope: {:ok false, :error-type :alias-migration-scope-path-refused,
                            :path "a{b/**", :pattern "a{b/**",
                            :cause "Missing '} near index 5\na{b/**\n     ^"}
```

End to end through `execute!`, two calls, the second replaying the first's published
`next_call` after a JSON round-trip (the real client path):

```
$ ~/bin/suite-run java -cp "$(cat /tmp/q5z11-review-fx/cp.txt)" clojure.main /tmp/q5z11-review-fx/p5.clj
=== call 1: a scope that matches nothing ===
error_type => "alias-migration-scope-matches-nothing"
suggested_paths => ["a{b/**" "src/**"]
source_roots => 2  roots_listed => 2
remedy => Resend the next_call: it replaces scope.paths with ["a{b/**" "src/**"], every one of the
2 source roots this tree holds, selecting all 2 of its sources. expect.files declared 1 and is
left as declared, because no file was read.
next_call => {"op":"alias_migration",…,"scope":{"paths":["a{b/**","src/**"]},"expect":{"files":1},…}

=== call 3: replay via JSON round-trip (the real client path) ===
replaying scope.paths => ["a{b/**" "src/**"]
error_type => "alias-migration-scope-path-refused"
ok => false  committed => nil
error => scope.paths entry "a{b/**" is not a parseable glob: Missing '} near index 5
a{b/**
     ^. No file was visited, so what the scope contains is not known.
```

The remedy asserts "selecting all 2 of its sources"; executing it selects zero and earns a
second refusal — from a check this same round added. Neither commit's witness covers the
intersection. Fix: escape glob metacharacters when deriving the root pattern (Java's glob
syntax accepts `\{`, `\[`, `\\`), and add a witness that every pattern
`suggested-scope-paths` returns satisfies `(nil? (glob-parse-error p))`.

### 3 — GO-WITH-FIX. A refusal kind reachable from `handle-alias-migration` that the "derived from the whole entrance" enumeration does not contain.

`test/clj_surgeon/mcp_alias_migration_test.clj:698-703` (`refusal-kinds-in-source`) against
`src/clj_surgeon/mcp_alias_migration.clj:2165` — `(refusal :unknown-verification-profile …)`
inside `execute-migration!`, the verb's own execution path — and `:1917`, the same kind on
the undo path.

The alias-side regex is prefix-locked to `alias-migration-`
(`#"[:\"](alias-migration-[a-z-]+)[\"\s\)\}]"`), and the keyword-spelled `:error-type :…`
regex is applied only to `intent_transaction.clj` and `file_ops.clj`, so a non-prefixed
kind minted by `mcp_alias_migration.clj` itself falls through both.

```
$ ~/bin/suite-run java -cp "$(cat /tmp/q5z11-review-fx/cp.txt)" clojure.main /tmp/q5z11-review-fx/p4.clj
=== P4a: the enumeration the round-11 witness derives ===
count = 33
contains? unknown-verification-profile => false
contains? invalid-mcp-elapsed-time => false
contains? invalid-mcp-operation-result => false
contains? invalid-workspace-root => true
contains? mcp-adapter-failure => true

=== P4b: is unknown-verification-profile actually reachable from the verb? ===
error_type => "unknown-verification-profile"
structured keys => (:configured_profiles :error :error_type :mutation_attempted :next_action :ok
                    :operation :remedy :source_unchanged :verify :write_authority)
--- text block ---
alias_migration
  refused · unknown-verification-profile · 1.00 ms

✓ source unchanged
→ Unknown verification profile: no-such-profile
facts · configured_profiles=["focused"] · verify="no-such-profile"
remedy · Name a profile this workspace configures, or omit verify.
next_call · none — this refusal has no mechanically composable correction; the remedy above names
what only the caller can decide
--- end ---
```

The live receipt renders correctly today only because `refusal-fact-line` is generic; the
gate that is supposed to fail "on the day a kind is written" would not have noticed. The
enumeration is still *"a listed set wearing a derivation's clothes"* — narrower than the
docstring at `:679-689` claims. `mcp_operation.clj`'s `:invalid-mcp-operation-result` and
`:invalid-mcp-elapsed-time` (thrown from `invoke!`, which `handle-alias-migration` calls
directly) are also unseen; those surface as `mcp-adapter-failure`, so I record them as a
lesser instance of the same gap. Fix: scrape `:error-type :…` from `mcp_alias_migration.clj`,
`alias_migration.clj`, `mcp_tool.clj` and `mcp_operation.clj` too, not only the two kernel
files — i.e. apply BOTH spellings to ALL of the entrance's sources.

The rest of claim 4 reproduces. `mcp-adapter-failure` asserts neither fact and renders
through the tool's own summarizer:

```
=== P6b: adapter-failure receipt + its rendered text ===
receipt keys = (:cause :error :error_type :next_call :ok :operation :remedy)
contains source_unchanged? => false
contains mutation_attempted? => false
--- text ---
alias_migration
  refused · mcp-adapter-failure · 1.00 ms

⚠ source state requires structured receipt review
→ boom mid-write
facts · cause="clojure.lang.ExceptionInfo"
remedy · This failed before alias_migration published a receipt, so the operation's own answer —
including whether it wrote anything — does not exist. Inspect the workspace before resending
rather than assuming it is untouched, …
next_call · none — this refusal has no mechanically composable correction; the remedy above names
what only the caller can decide
```

### 4 — GO-WITH-FIX. `string_mention_sites` is truncated on a LEXICOGRAPHIC ordering of `file:line`, so the bound drops low line numbers and keeps high ones.

`src/clj_surgeon/alias_migration.clj:1093` (`(vec (sort (mapcat …)))` over `"file:line"`
strings) and `src/clj_surgeon/mcp_alias_migration.clj:1649`
(`(vec (take max-string-mention-sites …))`).

```
$ ~/bin/suite-run java -cp "$(cat /tmp/q5z11-review-fx/cp.txt)" clojure.main /tmp/q5z11-review-fx/p6.clj
=== P6a: the bounded string_mention_sites list, ordered ===
string_mentions (total) = 26
string_mention_sites   = ["src/z.clj:10" "src/z.clj:11" "src/z.clj:12" "src/z.clj:13"
 "src/z.clj:14" "src/z.clj:15" "src/z.clj:16" "src/z.clj:17" "src/z.clj:18" "src/z.clj:19"
 "src/z.clj:2" "src/z.clj:20" "src/z.clj:21" "src/z.clj:22" "src/z.clj:23" "src/z.clj:24"
 "src/z.clj:25" "src/z.clj:26" "src/z.clj:27" "src/z.clj:3"]
lines actually named   = (2 3 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27)
lines DROPPED          = (4 5 6 7 8 9)
```

This is the same class as round-10 finding 1 — *a bound applied to the wrong ranking* —
reintroduced by this round's own fix commit `656a0a3c`, and the docstring
(`alias_migration.clj:1088-1090`: *"a file is where the caller must go, a line is where they
must look"*) is what makes the ordering load-bearing. Note also that nothing in the receipt
states the truncation; the caller must infer it by comparing `string_mentions` (26, exact)
against `(count string_mention_sites)` (20). ALIAS-034's own text asks only for "a bounded
list", so I rate the missing statement a nit and the ordering the defect. Fix: sort by
`[file line]` with a numeric line, and state the truncation the way ALIAS-058's remedy now
does.

### 5 — Non-blocking. The scope-path refusal is unbounded in the caller's own input: a 10,001-character entry yields a 30,831-character text block.

`src/clj_surgeon/mcp_alias_migration.clj:657-684` (the `:error` and `:remedy` strings embed
`(:path scan)`, `(:pattern scan)` and `(:cause scan)`); `src/clj_surgeon/mcp_tool.clj:1244`
(`max-refusal-fact-characters` 160) bounds only the `facts ·` line — `:error` and `:remedy`
are envelope keys and are rendered whole.

```
$ ~/bin/suite-run java -cp "$(cat /tmp/q5z11-review-fx/cp.txt)" clojure.main /tmp/q5z11-review-fx/p3.clj
=== P3c: 10,000-char pattern -> refusal field length ===
error-type :alias-migration-scope-path-refused
path length = 10001  pattern length = 10001  cause length = 20031

=== P3e: text block for the live scope-path refusal (10k pattern) ===
text block length = 30831 characters
```

The parser's message echoes the pattern twice, so the cause is 2× the input before the
`:error` and `:remedy` sentences quote it again. `MCP-OP-ALIAS-059` asks for a text
"bounded in count and in per-field length"; the bound reads on tree size, not caller-input
size, so this is a gap in the requirement as much as in the code. Not a regression in kind
— at `0085db8` the same input became an `mcp-adapter-failure` whose raw-JSON text carried
the same 20 KB message — but this round moved the class inside the verb's own
constant-size-receipt contract without bounding it. Fix: elide `:path`/`:pattern`/`:cause`
at a stated ceiling inside the refusal constructor, not only in the fact renderer.

### 6 — Non-blocking. `string-mentions` counts comments and regex literals as string-literal sites.

`src/clj_surgeon/alias_migration.clj:1092-1098` — the scan is `str/includes?` of
`"\"<needle>\""` per line, with no reader.

```
=== P4c: string_mentions with 25 sites; comment / regex / string ===
total sites = 27
comment line 2 counted? => true
regex line 3 counted? => true
```

25 string literals + one `; comment mentioning "acid.fanout.store/find-event"` + one
`#"acid.fanout.store/find-event"` = 27. `MCP-OP-ALIAS-034` says "STRING LITERAL sites".
False positives on a go-look-here list are cheap, so this is a wording-or-scan choice to
settle rather than a defect to block on — but the count is published as exact and it is
not exactly what the requirement names.

### 7 — Non-blocking, latent. `refusal-fact-line` drops facts past 12 without saying so.

`src/clj_surgeon/mcp_tool.clj:1249-1252` (`max-refusal-facts` 12) and `:1289` (`take`).

```
=== P6c: refusal-fact-line bound — a refusal with 15 facts ===
facts published = 15  · facts rendered = 12
dropped fields = (:f12 :f13 :f14)
does the text say it truncated? => false
```

No live `alias_migration` refusal carries 12 discriminating facts today (the widest,
`scope-matches-nothing`, carries seven), and the enumeration witness seeds far fewer, so
the bound has never fired. It is the same silent-truncation class the round just paid for
twice; a one-line "· 3 more in structuredContent" closes it and makes the witness able to
see it.

### 8 — Suspicion (reproduced, but I cannot show it harms anything). A NUL inside a `scope.paths` entry is neither refused nor parse-errored.

`src/clj_surgeon/mcp_alias_migration.clj:224` skips the subtree derivation for an entry
containing ` `, and `getPathMatcher` accepts it, so the entry silently selects nothing
and the caller gets `scope-matches-nothing` rather than `scope-path-refused`.

```
=== P3b: NUL entry through scan-scope ===
{:ok true, :files []}      ; entry "src/ x"
{:ok true, :files []}      ; entry " "
```

No escape and no data loss; the caller is simply told a spelling cause it cannot see. I
label it a suspicion because a NUL cannot name a real path and the downstream refusal is
already fail-closed.

---

# Verdict

**GO-WITH-FIX.**

Every gate matches the builder's numbers exactly (bb 737/6273/0, mcp-test 467/5658/0,
oracle pass, hygiene pass). The FAN gate reproduces independently at one call — 21 files,
63 sites, 30 collisions, 6/6 checks, byte-identical to `canonical-21`. All five RED
commits are genuinely red at their own commit and green at the following fix; not one
witness in this round is decorative. Confinement is intact under thirty hostile spellings
including two symlinks out of the root: **nothing escaped, nothing outside the workspace
was selected, no byte was lost.** Claims 2 and 3 hold as stated, and the
`mcp-adapter-failure` half of claim 4 holds as stated.

What stops this being a clean GO is that two of the five claims are over-stated in
precisely the way a caller pays for. Findings 1 and 2 both publish an *executable*
correction that cannot be executed — one because the `next_call` is `nil` while the remedy
says "resend the next_call", the other because the derived glob is refused by this same
round's new parser gate — and both are reachable from ordinary trees (six long directory
names; one directory named `a{b`). That is the class ALIAS-058's own text calls out: *"a
silent truncation published as an executable correction is … worse, because the caller
executes it."* Finding 3 falsifies claim 4 as written: `unknown-verification-profile`, minted
by the verb's own namespace through the verb's own `refusal` constructor, is not in the
"derived from the whole entrance" set. Finding 4 reintroduces round-10's wrong-ranking-
under-a-bound defect inside this round's own fix.

None of the four is a data-safety defect and none regresses `f51ceae` or the pinned FAN
result, so they are fixes to land on this branch rather than a reason to stop the line.
Findings 5–8 are merge-neutral and should be filed, not blocked on.
