# Census round 16 — Opus executed re-check of bridge/census-verb at 42df064 (2026-09-04T00:52Z)

Reviewer: Opus, executed, in `/home/forge/tmp/sol/census17-wt`, verified at
`42df0641a6e83bec56491356728408242cd693a6` on entry and `git status --porcelain` empty on
exit. Nothing committed, stashed or pushed. Live MCP server started by me on port **7933**
and stopped by me (`ss` confirms 7933 free); ports 7888–7895 and 7906–7910 untouched.
Fixtures under `/tmp/census17-sol-fx` only; every `chmod` restored. `make mcp-test` never
invoked. FIFO probes under `timeout -s KILL 30`.

---

# NO-GO

Round sixteen is the strongest round of this program: seven of my eight round-fifteen items
are CLOSED, driven by me at both entrances, and the ratchets behind them are real —
neutering `census-source-refusal` on a scratch copy failed
`every-path-the-census-reads-passes-one-fence-before-any-open` at seven assertions at once,
and it failed them by producing the very answer this review is about
(`:census-adapter-failure`).

It is NO-GO for one reason, and it is last round's reason with the entrances swapped: **the
fix for item 5 landed at one entrance.** Commit `5d50d9d` put a typed catch at the MCP read
and left the CLI's read a bare `(slurp p)`. That is the exact sentence commit `06b81bd` was
written under — *"a rule that lives in one branch is a rule the other branches break"* —
recurring inside the round that wrote it. The EARS text `42df064` adds states the rule with
no entrance qualifier, in a requirement whose own opening reads *"no refusal from any
entrance, the `relation_census` tool and the `:relation-census` CLI op alike."* A spec that
claims more than the code does is what authorizes the next round to move on.

The second blocking finding is the same shape: `d0bd778` closed the raw-exception-text leak
for `AccessDeniedException` by matching its class NAME, and the sentence it added to the
spec is global — *"A refusal that names a path shall not publish the raw text of the
exception that produced it as its explanation"* — while its sibling `FileSystemException`
(a symlink loop, a name too long) still walks into the generic catch and publishes the
server's absolute root.

---

## Per item — my round-fifteen NO-GO list

### 1. `core.clj:532`, the `:dir` walk on a chmod-000 member — **CLOSED**

The fence is [core.clj:503](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L503)
(`census-source-refusal`), called on every discovered member at
[core.clj:616](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L616).

```
$ bb -m clj-surgeon.core :op :relation-census :dir /tmp/census17-sol-fx/workspace
{:ok false, :error-type :file-not-readable,
 :anchor {:kind :dir, :given "…/workspace", :absolute "…/workspace"},
 :error "…/src/app/denied.clj exists but cannot be read",
 :file "src/app/denied.clj",
 :remedy "src/app/denied.clj came from the workspace walk, not from the request, so there
          is no request to narrow and no narrower command can be computed: remove or
          repair it under …, or name a readable regular file with :file.",
 :files-scanned 0, :cause :permission-denied}
EXIT=1
```

Typed, anchored, project-relative `:file`, a remedy, no continuation, and a `:cause`. The
tool answers the same tree `unreadable-source-path` with the same remedy shape.

### 2. `core.clj:532`, a FIFO by `:file` and by `:dir` — **CLOSED**

Regularity is asked before any open, FOLLOWING links, at
[core.clj:550](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L550).

```
:file …/src/app/fifo.clj   -> :file-not-a-regular-file    elapsed 0.312 s
:dir  …/fifoonly           -> :file-not-a-regular-file    elapsed 0.310 s
```

Against 30 s / EXIT=124 / zero bytes last round. The `:dir` case refuses at the FIRST
refusable member in the walk's sorted order and names it project-relative.

### 3. `:invalid-arguments` in neither declared set — **CLOSED**

`:census-adapter-failure` and `:census-resource-exhausted` are now in
[relation_census.clj:808-815](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/relation_census.clj#L808),
and [core.clj:1093](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L1093) catches
`Throwable` at the op's own entrance. Attack (c), my own drive, injecting a `proxy`-based
`Error` subclass the op has never heard of:

```
site1 census-discovery/discover        -> :census-adapter-failure    declared-cli? true  remedy? true  next? false
site2 census/defines-arms? (read loop) -> :census-adapter-failure    declared-cli? true  remedy? true  next? false
site3 census/plan (plan phase)         -> :census-adapter-failure    declared-cli? true  remedy? true  next? false
site4 census/cli-anchor (the refusal builder ITSELF)
                                       -> :census-adapter-failure    declared-cli? true  remedy? true  next? false
StackOverflowError at discover         -> :census-resource-exhausted declared-cli? true
AssertionError at plan                 -> :census-adapter-failure    declared-cli? true
core/run + custom Error at plan        -> :census-adapter-failure    declared-cli? true
mcp discover / plan / defines-arms?    -> census-adapter-failure     declared-mcp? true  remedy? true
```

Never `:invalid-arguments`, never a throw, always in the declared set, always a remedy,
never a continuation. Injecting into the refusal builder itself still lands typed.

### 4. `mcp_relation_census.clj:449`, check-then-read — **PARTIAL, blocking**

**MCP: CLOSED.** Twenty thousand in-process requests naming one file whose mode a second
thread flipped, and twenty thousand more under an editor's atomic save:

```
MODE-FLIP   RESULTS: {:OK 14502, "unreadable-source-path" 5498}
RENAME-RACE RESULTS: {:OK 20000}
```

Zero `census-adapter-failure`, against 4,607 and 38 last round. The typed catch is
[mcp_relation_census.clj:509](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L509)
and the tripped entry joins the removals at
[:902](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L902).

**CLI: OPEN.** [core.clj:618](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L618)
is a bare `(slurp p)` with no `try`. The identical storm through
`core/run-relation-census {:file …}`:

```
CLI MODE-FLIP RESULTS: {:file-not-readable 16523, :census-adapter-failure 1623, :OK 1854}
CLI SAMPLE:
 {:error-type :census-adapter-failure,
  :error "The census failed: java.io.FileNotFoundException
          /tmp/census17-sol-fx/cli-race/src/app/race.clj (Permission denied)",
  :remedy "The census stopped part-way through, so the walk's own aggregates were lost with
           it and this refusal can compute no narrower command: point :dir at a directory
           you know is smaller, or census one :file at a time, and retry.",
  :exhausted false, :files-read 0}
```

**1,623 in 20,000.** That payload is round fourteen's rejected receipt, and it is the
receipt the spec added at `42df064` names as the defect, word for word:

> *"a permission failure that escapes the reader arrives at the census's catch-all and is
> published as `census-adapter-failure` with `exhausted` false and a resource-exhaustion
> remedy: a receipt that blames the adapter for a permission bit, invites a smaller retry
> against a bound that would refuse identically at any size"*

and the rule it states without qualifying the entrance:

> *"a read that fails AFTER the fence admitted the path — a mode flipped by another process
> between the check and the read, or an ordinary editor's atomic save — shall be caught at
> the READ and answered exactly as a path the fence refused is answered, with the same type
> and the same narrowing"*

The remedy is also wrong on its face: this request named ONE file and is told to point
`:dir` at a smaller directory. The witness `1f39b5f` added
([mcp_relation_census_test.clj:4653](/home/forge/tmp/sol/census17-wt/test/clj_surgeon/mcp_relation_census_test.clj#L4653))
drives `census-tool/execute-request!` only — the MCP entrance — which is why it is green
over this: the same blindness as the round-fourteen witness that drove only `:file`.

### 5. `core.clj:671`, a readable file under a denied PARENT at the CLI — **CLOSED**

[core.clj:481](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L481)
(`denied-ancestor`) is asked before `:file-not-found` is concluded.

```
$ bb … :file …/src/app/locked/inner.clj
{:ok false, :error-type :file-not-readable,
 :error "…/inner.clj cannot be read: the directory …/src/app/locked may not be read by this process",
 :remedy "…/inner.clj is there, and the directory …/locked is what this process may not
          read… make …/locked readable, name a reachable source with :file, or point :dir
          at a directory to census its tree.",
 :cause :parent-denied, :parent "…/src/app/locked"}
```

The directory is named, the cause is named, and the type is no longer `:file-not-found`.

### 6. `mcp_relation_census.clj:163`, the constructor's `files` rule — **CLOSED**

Driven directly against `mcp-relation-census/continuation`:

```
empty-files      next-call? false bytes=       files=<none>
nil-entry        next-call? false bytes=       files=<none>
int-entry        next-call? false bytes=       files=<none>
string-files     next-call? false bytes=       files=<none>
blank-entry      next-call? false bytes=       files=<none>
set-files        next-call? false bytes=       files=<none>
good             next-call? true  bytes=70     files=["src/a.clj"]
```

`[nil]`, `[42]`, `"x"`, `["   "]` and a set are all refused, and — the part that matters
for the wording — a candidate dropped for SHAPE carries **no** `bytes`, so
`continuation-refused-remedy`
([:205](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L205))
prints "there was no call to make" rather than the measurement of something never
measured. The `:census-failed` fallback at
[:1069](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L1069) now
omits a null `:file` instead of publishing `files [null]`.

### 7. `mcp_paths.clj:98`, the denied-parent message at the tool — **CLOSED**

```
=== denied-parent at tool ===
  error_type = "unreadable-source-path"
  error = "Source file cannot be reached: the directory src/app/locked may not be read by
           this process (src/app/locked/inner.clj)"
  files_removed = ["src/app/locked/inner.clj"]
```

The directory, project-relative, and the cause — not the path twice.

### 8. `mcp_relation_census_test.clj:4013`, the witness drove only `:file` — **CLOSED**

`f4c5494` adds CLI `:dir` drives for a denied member and a FIFO under deadlines, tool
drives for a directory named `*.clj` and a file under a denied parent
([:4405-4450](/home/forge/tmp/sol/census17-wt/test/clj_surgeon/mcp_relation_census_test.clj#L4405)),
and — the honest part — asserts rather than assumes the one shape the walk cannot produce
(`census-discovery` descends a directory named `*.clj` and never yields it as a candidate).
**Ratchet proved real:** neutering `census-source-refusal` on a scratch copy under
`/tmp/census17-sol-fx/scratch`:

```
FAIL (…:4322) expected :file-not-readable       actual :census-adapter-failure
FAIL (…:4325) expected "src/app/denied.clj"     actual nil
FAIL (…:4358) expected :file-not-a-regular-file actual :census-adapter-failure
… seven assertions, across the two fence witnesses
```

Note what the broken build answers: `:census-adapter-failure`. The crash-catch is the CLI's
*only* backstop for a read — which is item 4 stated as a mechanism.

---

## Round-sixteen attacks

### (a) the class-NAME match for `AccessDeniedException` — **a sibling slips**

[mcp_paths.clj:134](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_paths.clj#L134)
tests `(= "java.nio.file.AccessDeniedException" (.getName (class error)))`. A subclass
misses, and so does every other `FileSystemException`, which falls to
[mcp_paths.clj:142](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_paths.clj#L142) and
publishes `(.getMessage error)`. A symlink loop — `a.clj -> b.clj -> a.clj`, a shape that
occurs in real repositories:

```
  error_type = "unreadable-source-path"
  error = "/tmp/census17-sol-fx/workspace/src/app/loopa.clj: Too many levels of symbolic
           links or unable to access attributes of symbolic link (src/app/loopa.clj)"
```

The server's absolute root, published to the caller — precisely what `unreadable-ancestor`'s
own docstring at
[mcp_paths.clj:44](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_paths.clj#L44) says a
refusal must not do (*"a refusal that leaks the server's absolute root tells the caller a
fact about the box instead of a fact about their request"*), and what the EARS sentence
added in this round forbids **globally**, not for one exception class. The
matching-by-name workaround is correct and well reasoned; the *predicate* is one class too
narrow. Widening it to `java.nio.file.FileSystemException`, tested by name the same way, is
the fix.

A wrapped `UncheckedIOException` I could not reach at this site — `toRealPath` throws the
cause directly — but the same one-class-wide predicate would miss that too.

### (b) refuse-at-the-first-refusable-member — **both entrances AGREE; the contract is right, with one inconsistency**

A tree of 500 readable arms plus one denied member is not the discriminating case, because
the CLI has no multi-file request. The discriminating question is **provenance**, and on
that the two entrances now agree exactly:

- **path from the WALK** — both REFUSE the whole census, name the member project-relative,
  and offer the `:file` remedy. CLI: *"src/app/denied.clj came from the workspace walk, not
  from the request… or name a readable regular file with :file."* Tool: *"This path came
  from the workspace walk, not from the request… or name the sources to census with
  files."*
- **path from the REQUEST** — the tool NARROWS, dropping the entry and carrying `doors` and
  `pool_size` through; the CLI's one `:file` IS the request, so removing it leaves no
  request and it refuses with a remedy.

That is the right contract: a census is a completeness claim, and a silently skipped source
makes `read_complete` a lie. **But the contract disagrees with itself one level up.**
[census_discovery.clj:220](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/census_discovery.clj#L220)
swallows an unreadable DIRECTORY with `:continue` and no counter:

```
tree: src/app/ok.clj (an arm) + src/app/hidden/ (chmod 000, holding another arm)
CLI  :dir  -> ok, sites 1, "review the raw sites…"
MCP  walk  -> ok = true, files_scanned = 1, files = 1, arms = 1
```

One unreadable FILE refuses the entire census; one unreadable DIRECTORY that can hide a
thousand files is invisible and the receipt still says `read_complete true`. Whichever way
the project decides, both should decide the same way, and an unwalkable subtree belongs in
the receipt beside `oversized_skipped`, `skipped_outside_root` and `duplicates`.

### (c) an exception type never seen, at three sites — **typed every time**

See item 3: four CLI sites, three MCP sites, a `proxy` `Error` subclass and a
`StackOverflowError`. Typed, declared, remedied, no continuation, at every one.

### (d) a FIFO that gets a writer mid-test — **refused before open**

Regularity is a `stat` question, not an `open` question, so a live writer changes nothing.
With a shell writer holding the pipe open and dribbling bytes for 60 s:

```
:file …/fifowriter/src/app/slow.clj -> :file-not-a-regular-file   elapsed 0.313 s
:dir  …/fifowriter                  -> :file-not-a-regular-file   elapsed 0.310 s
```

Refused before open, both entrances, deadline never approached.

### (e) the schema item rule at the constructor — **the item rule is asked; `maxItems` and length are not**

```
10001-char   next-call? false bytes=10062   (dropped for LENGTH, not for shape)
513-entries  next-call? false bytes=5591    (dropped for LENGTH, not for shape)
nul-byte     next-call? TRUE  bytes=76      files=["src/a<NUL>.clj"]
```

Three observations, none blocking, all the same shape as the item that produced
round-fifteen's item 10:

1. **`files` is not length-capped at the constructor.** The schema declares `:maxItems 512`
   ([mcp_relation_census.clj:59](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L59));
   `publishable-files?`
   ([:171](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L171))
   asks `sequential?`, `seq` and `every? string?` and never asks the count. 513 entries is
   refused only because 5,591 bytes exceeds the 512-byte ceiling — the *same masking by the
   ceiling* that hid the `items` half until this round. The request validator does enforce
   it (`too-many-files`, driven), so it is unreachable today; it is unreachable by
   accident, not by construction.
2. **A NUL-byte entry is admitted into a continuation** that the tool's own entrance then
   refuses: replaying it returns *"Expected a project-relative .clj, .cljs, .cljc, or .edn
   path without parent traversal."* JSON Schema calls it a string, so the schema question
   passes; the tool rejects it. I could not construct a request in which it survives into a
   narrowing (the entry is itself unreadable and gets removed), so this is latent.
3. **No refusal field is length-bounded.** A 10,001-character `files` entry:

   ```
   MCP:  error / file / files_removed each echo the 10,001-char name  ≈ 30 KB refusal,
         and `error` is again the raw exception text — the absolute root
   CLI:  :file-not-found, typed correctly, 50,612 bytes of output
   ```

   Receipts are capped at 4,096 bytes and continuations at 512; refusals are capped at
   nothing. This is also the second entrance divergence I found: the CLI types it
   `:file-not-found` (via `fs/exists?`), the tool types it `unreadable-source-path` with
   the raw exception text.

### (f) gates — run once each, serially, under `suite-run`

```
mcp-test — /home/forge/bin/suite-run clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test
Ran 449 tests containing 5574 assertions.
0 failures, 0 errors.

test-fast — /home/forge/bin/suite-run bb test/run_all.clj
Ran 716 tests containing 6057 assertions.
0 failures, 0 errors.

oracle — swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]

named witnesses incl. the schema battery
 (no-refusal-anywhere-puts-a-caption-in-an-argument-position,
  every-path-the-census-reads-passes-one-fence-before-any-open,
  a-named-pipe-is-refused-before-any-open-on-both-entrances,
  a-read-that-fails-after-the-fence-is-never-an-adapter-crash,
  nothing-thrown-on-the-census-path-escapes-the-op-untyped)
RATCHET-RESULT {:test 5, :pass 215, :fail 0, :error 0}
```

Every figure matches the builder's claim (mcp-test 449/5574/0, test-fast 716/6057/0, oracle
pass). `make mcp-test` was never invoked.

---

# NO-GO for the mayor's merge queue

Items 1 and 2 block; they are two instances of one habit — a rule proved at one entrance
and a spec sentence written for both. Items 3–7 should ride the same round, because each is
a bound this round asserted and did not measure.

1. [core.clj:618](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L618) —
   **blocking.** The CLI's read after the fence has no typed catch: 20,000 `:file` requests
   under a mode-flip returned `{:file-not-readable 16523, :census-adapter-failure 1623,
   :OK 1854}`, the adapter-failure payload carrying the resource-exhaustion remedy telling a
   one-file request to point `:dir` somewhere smaller; the MCP entrance under the identical
   storm returned 0.
2. [mcp_paths.clj:142](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_paths.clj#L142) —
   **blocking.** Every `FileSystemException` that is not `AccessDeniedException` still
   publishes `(.getMessage error)`: a symlink loop returns `error
   "/tmp/census17-sol-fx/workspace/src/app/loopa.clj: Too many levels of symbolic links…
   (src/app/loopa.clj)"`, the absolute root this round's own spec sentence and docstring
   forbid.
3. [mcp_relation_census_test.clj:4653](/home/forge/tmp/sol/census17-wt/test/clj_surgeon/mcp_relation_census_test.clj#L4653) —
   the read-after-fence witness drives `census-tool/execute-request!` only; add the CLI
   storm through `core/run-relation-census` or item 1 recurs at whichever entrance is left
   out next.
4. [census_discovery.clj:220](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/census_discovery.clj#L220) —
   an unreadable DIRECTORY is swallowed with no counter: a tree whose `src/app/hidden` is
   `chmod 000` and holds an arm returns `ok true, files 1, read_complete true` at both
   entrances, while one unreadable FILE refuses the whole census.
5. [mcp_relation_census.clj:187](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L187) —
   `continuation-overflow-remedy` asserts a cause it never measured: 500 named files plus
   one denied returns *"The REQUEST is not the problem, the length of the workspace path in
   it is: retry with workspace_root reaching the same tree by a shorter path"* on a
   24-character root.
6. [mcp_relation_census.clj:171](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_relation_census.clj#L171) —
   `publishable-files?` asks the schema's `items` rule and not its `maxItems 512`: a
   513-entry candidate is dropped for LENGTH (5,591 bytes), masked by the byte ceiling
   exactly as `items` was before this round; and a NUL-byte entry is admitted into a
   76-byte continuation the tool's own entrance refuses.
7. [mcp_paths.clj:142](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/mcp_paths.clj#L142)
   and [core.clj:684](/home/forge/tmp/sol/census17-wt/src/clj_surgeon/core.clj#L684) — no
   refusal field is length-bounded while receipts are capped at 4,096 bytes: a
   10,001-character `files` entry yields a ~30 KB tool refusal and a 50,612-byte CLI
   refusal, and the two entrances type it differently (`unreadable-source-path` with raw
   exception text vs `:file-not-found`).
