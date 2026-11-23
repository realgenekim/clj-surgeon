# Census round 15 — Opus executed re-check of bridge/census-verb at e2d70d0 (2026-09-03T22:53Z; Sol refused the brief)

Verdict: **NO-GO**. Round-14 items 7 and 9 CLOSED on the surfaces round 15 drove; both ratchets real (broken on a scratch copy). OPEN: the CLI `:dir` walk reads discovered files WITHOUT the fence — a chmod-000 member → untyped `:invalid-arguments` (core.clj:532); a FIFO named `*.clj` found by `:dir` or named by `:file` blocks `slurp` forever (EXIT 124 at 30 s; the tool refuses in 2.6 ms); `:invalid-arguments` (core.clj:1811) is in neither enumeration set — the witnesses are blind to every refusal reaching the catch-all; check-then-read residual on MCP (4,607/20,000 mode flips → `census-adapter-failure`); a readable file under a denied parent → `:file-not-found` ("name a source that exists"; it exists); the constructor admits `[nil]`/`[42]`/`"x"` in `files` and the `:census-failed` fallback publishes `files [null]`; a denied parent's error is just the path. Items 1, 2, 5, 7 share one fix: resolve `:dir`-discovered paths through the same fence before reading. Gates 444/5452/0 ×2, 716/6057/0, oracle, 1/133/0. Round 16 launched.

## Opus verdict, verbatim

# NO-GO

Round fifteen closes both of round fourteen's blocking items on the surfaces it drove, and the
two new ratchets are real — I broke each of them on a scratch copy and watched them fail. But the
permission-denied class it set out to close is still open on the CLI's `:dir` walk, which is the
ordinary invocation, and the EARS clause commit `e2d70d0` added now asserts a property of "both
entrances" that only one entrance has. A spec that claims more than the code does is the failure
that authorizes the next round to move on.

Reviewer: Opus, executed, in `/home/forge/tmp/sol/census16-wt` at `e2d70d0`. Nothing committed,
stashed, or pushed; worktree verified clean at `e2d70d0` on exit. Live MCP server started by me on
port 7921 and stopped by me; ports 7888-7895 and 7906-7910 untouched. Fixtures under
`/tmp/census16-sol-fx` only; every `chmod` restored.

---

## Per item

### 1. Round-14 item 7, MCP entrance, stable chmod-000 — **CLOSED**

[mcp_paths.clj:88](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_paths.clj#L88) asks
`Files/isReadable` beside regularity, before `:ok true`.

Live, over HTTP against the running tool:

```
=== item7_good_denied http 200
  "error_type": "unreadable-source-path",
  "error": "Source file exists but this process may not read it (src/app/denied.clj)",
  "files_removed": ["src/app/denied.clj"], "files_removed_omitted": 0,
  "next_call": {"doors":["upsert-by"],"files":["src/app/good.clj"],"pool_size":1,
                "tool":"relation_census","workspace_root":"/tmp/census16-sol-fx/workspace"}
  [next_call UTF-8 bytes: 141]
```

`census-adapter-failure` is gone, `exhausted` is absent, the denied entry is removed, the readable
remainder survives, and `doors` and `pool_size` travel through. All-denied
(`item7_all_denied`) returns remedy-only with no `next_call` key at all.

### 2. Round-14 item 7, CLI `:file` — **CLOSED**

[core.clj:701](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L701).

```
$ bb -m clj-surgeon.core :op :relation-census :file /tmp/census16-sol-fx/workspace/src/app/denied.clj
{:ok false, :error-type :file-not-readable,
 :anchor {:kind :file, :given "…/denied.clj", :absolute "…/denied.clj"},
 :error "…/denied.clj exists but cannot be read",
 :file "…/denied.clj",
 :remedy "…exists but this process may not read it, and the one source this op was given IS the
          request… make the file readable, name a readable source with :file, or point :dir at a
          directory to census its tree."}
```

Its own type, distinct from `:file-not-found`, which the same op still returns for a genuinely
absent `:file`. Anchor, remedy, no continuation.

### 3. Round-14 item 9, the seven bypassing construction sites — **CLOSED**

[mcp_relation_census.clj:129](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L129)
is the one constructor;
[:170](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L170) is the byte
ceiling; `narrowing-continuation` at
[:615](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L615) is a thin
wrapper over it. The site I measured at 661 bytes last round:

```
=== item9_600char_root_unknown_field http 200
  "error_type": "invalid-mcp-request", "reason": "unknown-fields",
  "remedy": "The narrowest continuation this refusal can compute renders as 661 UTF-8 bytes,
             over the 512-byte ceiling a continuation must fit, so none is offered…"
  [no next_call key present: True]
```

Same for `doors-not-strings`, `pool-size-not-an-integer`. And the sites are *measured there*, not
merely near there: removing the ceiling from that one function on a scratch copy
(`:next-call (when rendered faithful)`) failed the witness at eight tool sites and seven shape
rows at once —

```
:unknown-door-symbol emitted a 781-byte continuation under a 512-byte ceiling
:unreadable-source-path emitted a 723-byte continuation …
:source-too-large 723 · :no-fold-arms-found 728 · :unparseable-file 728
:census-worker-failure 723 · :census-failed 712 · :too-many-candidate-files 703
[:unknown-fields :present] 711 · [:doors :container-type] 711 · [:files :empty] 711 …
```

`grep -n "next_call\|:next-call"` over the namespace shows no map reaching `refusal`'s
continuation argument from anywhere but the constructor.

### 4. (c) `mcp-refusal-types` declared = driven — **CLOSED**

[relation_census.clj:800](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/relation_census.clj#L800),
13 names, all 13 driven at
[mcp_relation_census_test.clj:4437](/home/forge/tmp/sol/census16-wt/test/clj_surgeon/mcp_relation_census_test.clj#L4437).
Adding `:sol-fake-refusal` to the declaration on a scratch copy:

```
FAIL in (every-continuation-either-entrance-emits-fits-the-byte-ceiling) (…:4562)
the probes cover every refusal the tool declares it can return
Ran 1 tests containing 76 assertions.  1 failures, 0 errors.
```

Exactly one assertion, and the constructor witness stayed green — the two ratchets are
independent.

### 5. (a) `isReadable` is a check-then-read — **OPEN**

[mcp_paths.clj:88](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_paths.clj#L88) answers the
question; [mcp_relation_census.clj:449](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L449)
performs the read, with nothing between them and no typed catch around it. Twenty thousand
in-process requests naming one file whose mode a second thread flipped:

```
RACE RESULTS: {"unreadable-source-path" 10085, :OK 5308, "census-adapter-failure" 4607}
```

and the payload of one of the 4,607 is round fourteen's rejected receipt, verbatim:

```
{:error_type "census-adapter-failure", :exhausted false, :files_read 0,
 :error "The census failed: java.io.FileNotFoundException …/race.clj (Permission denied)",
 :remedy "The census ran out of a runtime resource part-way through, so the walk's own aggregates
          were lost with it and this refusal can compute no narrower call: name at most 512 sources
          with files, or point workspace_root at a directory you know is smaller, and retry."}
```

This is not only a permission-bit race. The same window opens for an ordinary editor's atomic
rename — `spit` to `.tmp`, `renameTo`, `delete` — with no `chmod` anywhere:

```
DELETE-RACE RESULTS: {"unreadable-source-path" 19930, "census-adapter-failure" 38, :OK 32}
```

The docstring at [mcp_paths.clj:56-66](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_paths.clj#L56)
says "nothing this fn admits can fail in the caller's reader," and the new EARS clause says the
decision is made "never in a `try` around the reader." Moving the *check* earlier does not remove
the need for a typed catch at the *read*; the class narrowed, it did not close.

### 6. (b)/(c) the CLI `:dir` walk still crashes on an unreadable source — **OPEN, blocking**

[core.clj:532](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L532) — the discovery
walk's bare `(slurp p)`, with no fence resolution, no readability question, and no typed catch.
The exception lands at [core.clj:1811](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L1811),
the launcher's catch-all.

```
$ bb -m clj-surgeon.core :op :relation-census :dir /tmp/census16-sol-fx/workspace
{:error "/tmp/census16-sol-fx/workspace/src/app/denied.clj (Permission denied)",
 :error-type :invalid-arguments}
EXIT=1
```

Untyped, no anchor, no `files_removed`, no remedy, no continuation — and `:invalid-arguments` is
in **neither** `cli-refusal-types` nor `mcp-refusal-types`, so this is also the direct answer to
attack (c)'s second half: yes, there is a refusal the op can emit that no declared set contains,
and neither enumeration witness can see it.

The MCP entrance, given the *same tree* with the same traps and no `files`, answers:

```
=== b_walk_whole_tree_with_traps http 200
  "error_type": "unreadable-source-path", "file": "src/app/denied.clj",
  "remedy": "This path came from the workspace walk, not from the request, so there is no request
             to narrow… remove or repair src/app/denied.clj under …, or name the sources to
             census with files."
```

Round fifteen fixed the CLI's single-`:file` branch only. The witness at
[mcp_relation_census_test.clj:4013](/home/forge/tmp/sol/census16-wt/test/clj_surgeon/mcp_relation_census_test.clj#L4013)
drives the CLI through `:file` and a babashka `:file` subprocess, never through `:dir`, which is
why it is green over this.

### 7. (b) a directory named `x.clj` — **OPEN on the CLI**

```
$ bb -m clj-surgeon.core :op :relation-census :file …/src/app/dirfile.clj
{:error "…/src/app/dirfile.clj (Is a directory)", :error-type :invalid-arguments}
```

versus the tool, which names it and narrows around it:

```
=== b_dir_named_clj http 200
  "error_type": "unreadable-source-path", "error": "Source path is not a regular file (src/app/dirfile.clj)",
  "files_removed": ["src/app/dirfile.clj"],
  "next_call": {"files":["src/app/good.clj"],"pool_size":1,…}   [119 bytes]
```

### 8. (b) a FIFO named `x.clj` hangs the CLI forever — **OPEN, blocking**

Same line, [core.clj:532](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L532).
`fs/readable?` is true of a named pipe and `slurp` blocks on it with no writer:

```
$ timeout -s KILL 30 bb -m clj-surgeon.core :op :relation-census :file …/src/app/fifo.clj
EXIT=124   output bytes: 0

$ timeout -s KILL 30 bb -m clj-surgeon.core :op :relation-census :dir /tmp/census16-sol-fx/fifoonly
EXIT=124
```

Thirty seconds, zero bytes, no diagnostic. One named pipe anywhere under `:dir` wedges the census
with no timeout and nothing on stdout. The tool refuses the same path in 2.6 ms
(`Source path is not a regular file`). This is the highest-severity behavioral divergence between
the entrances that I found, and it shares its fix with items 6 and 7.

### 9. (b) a readable file under a denied PARENT — **PARTIAL**

The tool types it correctly but its `error` is the bare absolute path, because
`AccessDeniedException.getMessage` is the path and
[mcp_paths.clj:98](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_paths.clj#L98) passes it
through unlabelled:

```
"error_type": "unreadable-source-path",
"error": "/tmp/census16-sol-fx/workspace/src/app/locked/inner.clj (src/app/locked/inner.clj)"
```

The CLI is worse — it gets the *wrong type and the wrong remedy*, at
[core.clj:671](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L671):

```
{:ok false, :error-type :file-not-found,
 :error "…/src/app/locked/inner.clj does not exist",
 :remedy "…does not exist… name a source that exists with :file…"}
```

The file exists. `fs/exists?` returns false because it cannot stat through the `chmod 000` parent.
This is precisely the `file-not-found` / `file-not-readable` confusion commit `1038893` was written
to end — "the file is THERE, and what must change is what may read it" — reproduced by moving the
permission bit one directory up.

### 10. (d)/(c) the `:census-failed` fallback builds a schema-invalid continuation — **OPEN**

[mcp_relation_census.clj:988](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L988)
hands the constructor `{:files [(:file planned)]}`, and a plan failure that names no `:file` — the
documented reason this fallback exists — makes that `[nil]`. Driven from a short root, where the
ceiling cannot mask it:

```
census-failed error_type: census-failed
census-failed next_call: {"workspace_root":"…/workspace","files":[null],"tool":"relation_census"}
has remedy? false
```

The constructor's own contract, direct:

```
ctor empty files    -> {:candidate nil, :bytes nil, :next-call nil}          ; refused, correct
ctor nil-entry files-> {:next-call {… :files [nil] …}, :bytes 91}            ; admitted
ctor non-string     -> {:next-call {… :files [42]  …}, :bytes 89}            ; admitted
ctor files "x"      -> {:next-call {… :files "x"   …}, :bytes 88}            ; admitted
```

[mcp_relation_census.clj:163](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L163)
refuses an *empty* `files` because "the published schema declares `minItems 1`" — but the same
schema declares the items are strings, and replaying this continuation refuses
`invalid-mcp-request / file-not-a-string` (confirmed live). The 600-character ratchet is
structurally blind to it: at 600 characters the ceiling drops that continuation before any schema
question is asked, so the long root that closes item 9 hides this one.

### 11. (d) a request whose only files are all removed — **CLOSED on both entrances**

Tool, [mcp_relation_census.clj:851](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L851):
remedy-only, no `next_call` key, and it *does* say what was removed, by name, mixing the two
causes:

```
=== e_all_removed_denied_and_missing http 200
  "error_type": "unreadable-source-path",
  "files_removed": ["src/app/denied.clj","src/app/missing.clj"], "files_removed_omitted": 0,
  "remedy": "Every source this request named is unreadable through the project fence — missing,
             outside it, or there but not readable by this process — so the request minus them is
             not a request and no narrower call can be computed…"
  [no next_call key present: True]
```

CLI, [core.clj:704](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L704): the single
`:file` IS the whole request, so remedy-only naming the path, no `:next-command`,
no `:next-command-argv` (item 2 above).

### 12. Hostile-shape sweep — no untyped escape found at the tool

`files:[null]`, `doors:{…}`, `workspace_root:{…}`, `pool_size:{…}`, `files:{…}`, 60-deep nested
array, 513-entry `files`, empty request: every one returned a typed `invalid-mcp-request` (or
`invalid-workspace-root`) with a 90-byte continuation or a remedy. No transport error, no
uncaught throw. `mcp-operation/invoke!` has no catch of its own
([mcp_operation.clj:49](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_operation.clj#L49)),
so this is worth keeping an eye on, but I could not reach it.

---

## Gates — run once each, serially, under `suite-run`

```
mcp-test (run 1) — clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/mcp-test
Ran 444 tests containing 5452 assertions.
0 failures, 0 errors.

mcp-test (run 2)
Ran 444 tests containing 5452 assertions.
0 failures, 0 errors.

test-fast — bb test/run_all.clj
Ran 716 tests containing 6057 assertions.
0 failures, 0 errors.

oracle — swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]

schema battery — no-refusal-anywhere-puts-a-caption-in-an-argument-position
Ran 1 tests containing 133 assertions.
0 failures, 0 errors.
SCHEMA-BATTERY {:test 1, :pass 133, :fail 0, :error 0, :type :summary}
```

`make mcp-test` was never invoked. HEAD `e2d70d0`, `git status --porcelain` empty on exit.

---

# NO-GO for the mayor's merge queue

Items 6 and 8 block. They are one fix — the CLI's `:dir` walk must resolve each discovered path
through the same fence the tool uses before it reads it — and that one fix also closes 7 and the
CLI half of 9. Items 5 and 10 belong in the same round: 5 because the EARS clause this branch adds
states the property absolutely and the code holds it only for a bit that does not move, and 10
because it is the constructor's own stated invariant, unenforced, and hidden from the ratchet by
the very root length that makes the ratchet work.

1. [core.clj:532](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L532) — **blocking.**
   `:op :relation-census :dir <tree with a chmod-000 source>` returns
   `{:error "…/denied.clj (Permission denied)", :error-type :invalid-arguments}`; the tool answers
   the same tree `unreadable-source-path` with a remedy.
2. [core.clj:532](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L532) — **blocking.**
   A FIFO named `*.clj`, named by `:file` or found by `:dir`, blocks `slurp` forever: `EXIT=124`,
   zero output at 30 s; the tool refuses it in 2.6 ms.
3. [core.clj:1811](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L1811) —
   `:error-type :invalid-arguments` is in neither `cli-refusal-types` nor `mcp-refusal-types`, so
   both enumeration witnesses are blind to every refusal that reaches this catch-all.
4. [mcp_relation_census.clj:449](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L449) —
   check-then-read: 4,607/20,000 mode-flip runs and 38/20,000 atomic-rename runs returned
   `census-adapter-failure`, `exhausted false`, with the resource-exhaustion remedy.
5. [core.clj:671](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/core.clj#L671) — a readable file
   under a `chmod 000` parent is `:file-not-found` with "name a source that exists"; the file
   exists and the parent's bit is what must change.
6. [mcp_relation_census.clj:163](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_relation_census.clj#L163) —
   the constructor refuses an empty `files` but admits `[nil]`, `[42]`, and `"x"`; the
   `:census-failed` fallback publishes `next_call {"files":[null]}` on a short root.
7. [mcp_paths.clj:98](/home/forge/tmp/sol/census16-wt/src/clj_surgeon/mcp_paths.clj#L98) — a denied
   parent yields `error` "/abs/path (relative/path)" and nothing else, because
   `AccessDeniedException.getMessage` is the path.
8. [mcp_relation_census_test.clj:4013](/home/forge/tmp/sol/census16-wt/test/clj_surgeon/mcp_relation_census_test.clj#L4013) —
   the unreadable-source witness drives the CLI only through `:file`; add a `:dir` drive with an
   unreadable member, a FIFO, and a directory named `*.clj`, or items 1, 2 and 5 recur.
