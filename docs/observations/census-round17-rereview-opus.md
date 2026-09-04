## NO-GO

# Census round 17 — Opus executed re-check of bridge/census-verb at fb7f3b1 (2026-09-04T02:20Z)

Reviewer: Opus, executed (Sol's content filter refused this brief; same brief, same
substitutions). Clone `/home/forge/tmp/sol/census17-wt2`, verified
`fb7f3b136c12a3f2da338aed25144b6b6550027e` on entry and `git status --porcelain` EMPTY on
exit. Nothing committed, stashed, added or pushed. **No Surgeon server started** — every
drive is in-process against the clone's own classpath, so ports 7985–7987 were never bound
and 7888/7894/7895/7906–7910/7941–7982 were never touched (`ss -ltnp | grep ':798[567]'`
empty). Fixtures under `/tmp/census17-fx` only; every `chmod 000` restored (`chmod 755` on
the five directories, `chmod 644` on the one file). Scratch trees for the historical checks
are `git archive` exports under `/tmp/census17-fx/`, never worktrees or checkouts of the
clone. `make mcp-test` never invoked; `make check-agent-routing` never counted. No process
I did not start was signalled.

---

# NO-GO

Round seventeen closes five of Opus round-sixteen's seven items with real ratchets, and the
two that matter most are closed convincingly: the CLI read-after-fence storm now answers
**zero** `census-adapter-failure` in 20,000 racing requests where it answered 1,623 last
round, and the symlink-loop / name-too-long leak that was round sixteen's second blocking
finding is gone from the resolver, the tool and the CLI alike. The `bound-refusal`
exemption ratchet is genuinely falsifiable — I neutered `refusal-continuation-keys` to
`#{}` on a scratch copy and got exactly the builder's two failures back.

It is NO-GO for two reasons, and both are the same sentence this program has now written
three rounds running, with the axis rotated once more. Rounds fifteen and sixteen were
**one entrance proved, both entrances claimed**. This round is **one refusal SHAPE proved,
every refusal shape claimed** — and it is the two newest requirements, the two this round
added to the EARS text itself, that fail on the shape their own witness does not drive:

1. `execute-request!` publishes a **10,007-character unbounded field** in a 10,540-byte
   refusal, hours after the spec sentence "*NO FIELD of any refusal either entrance emits
   shall be unbounded*" landed — because the MCP bound is applied at the `refusal`
   CONSTRUCTOR, which is precisely the placement that same sentence forbids ("*the bound
   shall be applied at each entrance's LAST step rather than at the sites that build the
   strings*"). The CLI, which does apply it at the last step, is bounded on every shape I
   could drive.
2. `overflow-measurement` still names a cause it never measured, reachable through
   `execute-request!`: **"workspace_root alone measures 19 of those bytes"** on a 723-byte
   continuation, where the 19 is 2.6% and `doors` is the rest. That is round-sixteen item
   5's defect verbatim, in a variable part the fix did not weigh.

Neither is a security-boundary breach: no refusal published an absolute path *outside* the
workspace, and no read escaped the MCP fence. Both are the round's own new spec sentences
being false in the field on the first shape outside their witness.

---

## Verified claims — driven at BOTH entrances

### Claim 1 — the CLI read after the fence has a typed catch: **VERIFIED**

`/tmp/census17-fx/drv/p4.clj`, one daemon thread flipping `race.clj` between `---------`
and `rw-r--r--` (5,458,054 flips over the run, so the racer won constantly):

```
MCP  MODE-FLIP 20000 -> {"unreadable-source-path" 15064, :OK 4936}
CLI  MODE-FLIP 20000 -> {:file-not-readable 18312, :OK 1688}
CLI  DIR-WALK  10000 -> {:OK 2681, :file-not-readable 7319}
MCP  DIR-WALK  10000 -> {"unreadable-source-path" 7857, :OK 2143}
```

**Zero `census-adapter-failure` in 60,000 requests**, against 1,623 in 20,000 at the CLI
last round. I drove the `:dir` WALK as well as `:file`, which the brief did not ask for and
which is where the round-fifteen fix had leaked: also zero, at both entrances. No remedy
naming a wrong cause survived; `core/census-read-refusal` ([core.clj:570](src/clj_surgeon/core.clj#L570))
publishes `:cause :read-failed-after-fence` and the class NAME only, never the
`FileNotFoundException` message that carries the absolute path.

### Claim 2 — no refusal publishes raw exception text or an absolute root: **VERIFIED for the class it names** (one exception, finding 4)

Every shape I could raise through the resolver, the tool and the CLI. Fixtures
`/tmp/census17-fx/{loop,longws,odd,compfile}`:

```
resolver loopa (a.clj->b.clj->a.clj)
  :error "Source path does not resolve to a file: Too many levels of symbolic links
          or unable to access attributes of symbolic link"   :cause "not-found"
resolver ENAMETOOLONG (1,212-char relative)
  :error "Source path does not resolve to a file: File name too long"   :cause "not-found"
resolver ENOTDIR (src/app/afile.clj/x.clj)
  :error "Source path does not resolve to a file: Not a directory"      :cause "not-found"
resolver FIFO
  :error "Source path is not a regular file"            :cause "not-a-regular-file"
TOOL files=[loopa]   :error "Source path does not resolve to a file: Too many levels of
                             symbolic links… (src/app/loopa.clj)"
```

No absolute path in any of them. `filesystem-reason`
([mcp_paths.clj:91](src/clj_surgeon/mcp_paths.clj#L91)) reads `getReason` reflectively and
withholds any reason containing `/` — the right guard, since a JDK wording change is the
same defect through a different door. `exception-class-names` asks the whole hierarchy, so
a subclass cannot slip. The dangling-chain, FIFO, and path-component-is-a-file shapes all
land typed. The `workspace_root` an MCP refusal echoes is the request's own root and is
published in every receipt by design, so it is not this leak.

### Claim 3 — the read-after-fence witness now drives the CLI: **VERIFIED, 11 assertions**

`git archive 86fe30cc^` (= 42df064) + `git show 86fe30cc:test/…`, whole suite run:

```
FAIL a-read-that-fails-after-the-fence-is-never-an-adapter-crash (…:4806)
  expected: (zero? (get counts :census-adapter-failure 0))   actual: (not (zero? 177))
FAIL (…:4836) expected :file-not-readable   actual :census-adapter-failure
FAIL (…:4840) expected :read-failed-after-fence   actual nil
FAIL (…:4843) expected the named file        actual nil
FAIL (…:4846) expected (not (contains? result :exhausted))   actual (not (not true))
FAIL (…:4849) remedy must not say "directory you know is smaller"   actual (not (not true))
FAIL (…:4853) remedy must name the file — actual remedy: "The census stopped part-way
      through… point :dir at a directory you know is smaller, or census one :file at a
      time, and retry."
FAIL (…:4880) (…:4884) (…:4887) (…:4890)  — the :dir walk half, same shape
```

Exactly 11, exactly the claim, and red for the right reason. Green at fb7f3b1 (below).

### Claim 4 — an unreadable DIRECTORY refuses at both entrances: **VERIFIED**

Three trees: `denieddir` (`src/app/hidden` 000), `nested` (`src/app/one` 000, two levels
above the arm), `dirlink` (`src/app/hidden -> ../real-hidden`, the target 000).

```
TOOL denieddir  error_type "unreadable-source-path"  cause "directory-denied"
                directory "src/app/hidden"  no next_call
CLI  denieddir  error-type :file-not-readable        cause :directory-denied
                directory "src/app/hidden"  no continuation
TOOL/CLI nested   -> directory "src/app/one"     (the walk stops at the OUTERMOST denied dir)
TOOL/CLI dirlink  -> directory "real-hidden"     (project-relative, the real dir)
```

Both entrances, same cause, same project-relative name, never the absolute one, no
continuation. **The reuse attack found no misroute:** `rg` over `src/` shows no consumer
branching on `unreadable-source-path` or `:file-not-readable` other than the declared
enumerations `census/cli-refusal-types` and `census/mcp-refusal-types`
([relation_census.clj:759,818](src/clj_surgeon/relation_census.clj#L759)), so the type reuse
routes nothing anywhere. The only shape difference is that the directory refusal carries
`:directory` where a file refusal carries `:file`/`files_removed`, which is the honest
distinction. A named `files` request under the same tree still succeeds (`files_scanned 1`)
— correct: the walk is what could not be completed, and a named request is not the walk.

Red at `e19b445a^`: 12 assertions, including
`expected (= ["src/app/hidden"] (:unreadable-directories discovered)) actual nil` and
`expected (false? (:ok result)) actual (not (false? true))`.

### Claim 6 — `publishable-files?` applies maxItems 512 and the item pattern: **VERIFIED, and the property holds**

Driven directly against `#'ct/continuation`, short root, so the byte ceiling masks nothing:

```
:512               admitted?=true   bytes=6604   overflow=:entry-count   entrance-rule=true
:513               admitted?=false  bytes=null   overflow=nil            entrance-rule=true
:nul               admitted?=false  bytes=null
:absolute          admitted?=false  bytes=null
:traversal         admitted?=false  bytes=null
:badext            admitted?=false  bytes=null
:emptyseg          admitted?=false  bytes=null
:trailing-space    admitted?=false  bytes=null
:blank-seg-space   admitted?=false  bytes=null
```

512 admitted for SHAPE and left to the byte ceiling; 513 refused for shape with **no
measured byte length** — the masking is gone. My three added attacks:

- **a 512-entry list whose bytes exceed the ceiling**: admitted for shape,
  `bytes 29644, overflow {:cause :entry-count, :measured 28050, :entries 512}` — the right
  cause, the right figure.
- **unicode normalisation**: NFC (74 bytes) and NFD (75 bytes) both admitted, both accepted
  by `mcp-paths/relative-source-path?`. No divergence — the constructor and the entrance
  apply one predicate, which is the stated contract; they cannot disagree by construction.
- **trailing space / CR / LF / tab**: trailing space refused (the extension becomes
  `"clj "`); CR, LF and tab INSIDE a segment admitted — and admitted by the entrance too,
  so the promise still executes. The property "*every entry of every candidate the
  constructor admits passes the entrance's own path rule*" held on all 15 inputs.

Red at `e664a6a4^`: 20 failures, matching the builder's `:fail 20`, including
`:absolute published an entry this tool's entrance refuses: "/src/a.clj"`.

### Claim 7, the exemption half — **VERIFIED, and the ratchet is real**

`git archive fb7f3b1` to `/tmp/census17-fx/neuter`, `refusal-continuation-keys` edited to
`#{}` ([relation_census.clj:1022](src/clj_surgeon/relation_census.clj#L1022)), full suite:

```
FAIL every-refusal-field-is-length-bounded-at-both-entrances (…:5553)
  expected: (= call (:next_call bounded))
FAIL (…:5555) expected: (= argv (:next-command-argv bounded))
Ran 454 tests containing 5734 assertions.
2 failures, 0 errors.
```

Exactly the builder's two, and they are the assertions that FALSIFY the exemption rather
than merely observing it is harmless. I could not drive a hostile input into an unbounded
continuation: `continuation` returns `:next-call` only when `within-next-call-bytes?`, so a
continuation is ≤512 bytes by construction, a quarter of the field bound; and the CLI's
`:next-command`/`:next-command-argv` are built only by
`census/cli-next-command-argv` ([relation_census.clj:1088](src/clj_surgeon/relation_census.clj#L1088)),
never spelled at a site. The exemption is safe.

### The corrected item-7 witness, red at 42df064 for the RIGHT reason — **CONFIRMED**

`git archive 42df064` + `git show 503d334f:test/…`, **17 assertions red**, and the first is
the one that matters:

```
FAIL every-refusal-field-is-length-bounded-at-both-entrances (…:5453)
  expected: (not= over (file-field over))
  actual:   (not (not= "dddd…dddd.clj" "dddd…dddd.clj"))
       — i.e. a field one character over the ceiling came back byte-identical
FAIL (…:5455) (…:5463 ×3) (…:5466) (…:5470) (…:5472) (…:5483 ×5) (…:5486) (…:5489)
     (…:5493) (…:5495)
```

The 1,025-character drive is built from 24-character segments
([test:5427](test/clj_surgeon/mcp_relation_census_test.clj#L5427)) so that no component is
itself ENAMETOOLONG — the disclosed "10009 on a 10,013-char entry" mistake is gone and the
witness now fails on the absence of truncation, which is the requirement. The near-miss (a
field exactly AT 1,024 must NOT be truncated) is asserted beside it, so the bound cannot
become a censor.

---

## Findings

### 1. `mcp_relation_census.clj:1292` — an MCP refusal field is UNBOUNDED. **BLOCKING**

The bound is applied inside `refusal`
([mcp_relation_census.clj:124](src/clj_surgeon/mcp_relation_census.clj#L124)). Two refusals
this entrance emits are not built by `refusal`: the workspace-router failure at
[mcp_relation_census.clj:1292](src/clj_surgeon/mcp_relation_census.clj#L1292), which
`assoc`s onto `routed` and `dissoc`s `:next_call`, and never touches `census/bound-refusal`.

```clojure
(ct/execute-request! {:project-root "/tmp/census17-fx/ws"}
                     {:workspace_root (str "/nope/" (apply str (repeat 10001 \a)))})
```
```
ok false  error_type invalid-workspace-root
longest field = 10007   total json bytes = 10540   truncation-marker? false
UNBOUNDED :workspace_root len 10007 head: /nope/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
```

Three reachable inputs, all through `execute-request!`:

| input | error_type | longest field | refusal bytes | marker |
|---|---|---|---|---|
| `:workspace_root "/nope/<10001×a>"` | `invalid-workspace-root` | **10,007** | 10,540 | none |
| `:workspace_root "<10001×a>"` (relative) | `invalid-workspace-root` | **10,001** | 10,495 | none |
| `:workspace_root "<root>/src/app/arm.clj<9000×a>"` | `invalid-workspace-root` | **9,035** | 9,568 | none |
| CLI `:dir "/nope/<10001×a>"` | `:census-resource-exhausted` | 1,055 | 2,616 | **present** |

The CLI is right and the tool is wrong, and the spec sentence this round added says why:

> *"the bound shall be applied at each entrance's LAST step rather than at the sites that
> build the strings, for the reason this requirement already gives about the continuation
> constructor"*

`core/run-relation-census` ([core.clj:1241](src/clj_surgeon/core.clj#L1241)) does exactly
that — `(if (false? (:ok result)) (relation-census/bound-refusal result) result)` — and I
could not find a CLI shape that escapes it. The MCP entrance does the thing the sentence
forbids, and the field that escapes is the *caller's own bad input echoed back*, which is
the harm the requirement names ("*at the moment they are least able to read it*"). The
refusal is 2.6× the 4,096-byte receipt cap the witness asserts refusals must not dwarf
(`(is (< (count (json/generate-string tool)) 8192))`,
[test:5460](test/clj_surgeon/mcp_relation_census_test.clj#L5460)).

**Why the witness is green over it:** `every-refusal-field-is-length-bounded-at-both-entrances`
drives exactly one MCP shape — `{:files [long-name]}`
([test:5436](test/clj_surgeon/mcp_relation_census_test.clj#L5436)) — which goes through
`refusal`. It never drives `workspace_root`. Same blindness as the round-fourteen witness
that drove only `:file` and the round-sixteen witness that drove only the tool.

**Fix:** wrap `execute-request!`'s return in `census/bound-refusal` the way
`run-relation-census` does, so the bound is a property of the ENTRANCE and not of one
constructor; then extend the witness to every refusal shape reachable from a hostile
`workspace_root`, `doors`, `pool_size` and unknown-field name, not one `files` entry.

### 2. `mcp_relation_census.clj:136` — the overflow remedy still names an unmeasured cause. **BLOCKING**

`overflow-measurement` weighs two of the candidate's variable parts, `workspace_root` and
`files`. The candidate carries a third: the `:refusal loaded` branch builds it as
`(assoc (dissoc params :files) :workspace_root canonical :files remaining)`
([mcp_relation_census.clj:1045](src/clj_surgeon/mcp_relation_census.clj#L1045)), so every
caller-supplied option rides through — `doors` included. With `root-bytes = 19` and
`entry-bytes = 19`, the `(>= root-bytes entry-bytes)` branch wins and the remedy blames the
workspace path:

```clojure
(ct/execute-request! {:project-root "/tmp/census17-fx/ws"}
  {:files ["src/app/arm.clj" "src/app/missing.clj"]
   :doors (mapv (fn [i] (str "d" i (apply str (repeat 150 \x)))) (range 4))})
```
```
error_type = "unreadable-source-path"   next_call? = false
REMEDY: The narrowest continuation this refusal can compute renders as 723 UTF-8 bytes,
over the 512-byte ceiling a continuation must fit, so none is offered. The REQUEST is not
the problem, the length of the workspace path in it is: workspace_root alone measures 19
of those bytes — retry with workspace_root reaching the same tree by a shorter path, and
fix what this refusal named.
```

Nineteen bytes of 723 — **2.6%** — named as the cause, on a 19-character root. Round
sixteen's item 5 was "*a remedy blaming the workspace path on a twenty-four-character
root*". This is that receipt, from the same function, one round later. Reproduced at
`doors = 4×150`, `8×80`, `2×300` and `16×40` (723 / 783 / 713 / 829 bytes); every one says
"workspace_root alone measures 19". A caller who follows it shortens the one thing that is
not the problem and receives the identical refusal — the loop-with-a-receipt
MCP-OP-CENSUS-014 forbids a continuation, arriving in a remedy, which is the exact wording
the round's own spec addition uses.

The spec says "*It shall measure the candidate's variable parts*". `doors` is a variable
part of the candidate and nothing measures it.

**Why the witness is green over it:** the three item-5 fixtures the EARS names — 500 short
entries on a short root, one 609-byte entry, a 647-byte root — vary only the two parts the
fix weighs. A witness whose fixtures are the fix's own two dimensions cannot see a third.

**Fix:** measure the candidate as a whole — subtract `workspace_root` and `files` from the
rendered byte count and, when the remainder dominates, name the option that carries it (or,
minimally, refuse to name a cause whose `measured` is under some fraction of `bytes` and
fall back to a cause-free sentence). Add a fixture whose bulk is `doors`.

### 3. `core.clj:481` — `denied-ancestor` calls a readable regular FILE a denied directory, and the two entrances disagree about it

The docstring says "*the nearest EXISTING ancestor DIRECTORY*"; the code never asks
`fs/directory?`:

```clojure
(loop [dir (fs/parent (fs/absolutize path))]
  (when dir
    (if (fs/exists? dir)
      (when-not (and (fs/readable? dir) (fs/executable? dir))
        (str dir))
      (recur (fs/parent dir)))))
```

A mode-644 regular file is readable and NOT executable, so it satisfies the predicate.
`src/app/afile.clj` is an ordinary source file; `src/app/afile.clj/x.clj` is ENOTDIR:

```
CLI  :error  "…/afile.clj/x.clj cannot be read: the directory …/afile.clj may not be
              read by this process"
     :remedy "…make …/afile.clj readable, name a reachable source with :file…"
     :cause  :parent-denied      :error-type :file-not-readable
TOOL :error  "Source path does not resolve to a file: Not a directory (…)"
     :cause  "not-found"         :error_type "unreadable-source-path"
```

Three defects in one: the refusal states a falsehood (that file is readable, and it is not
a directory); the remedy cannot be followed (it is already readable, and making it more
readable changes nothing); and the two entrances publish **different causes for the same
observation** — which is precisely what the shared `source-refusal-causes` vocabulary was
added this round to make impossible, and what the new witness asserts for the loop and the
long name but not for this shape.

Full parity enumeration, ten shapes, both entrances:

| case | TOOL cause | CLI cause | agree |
|---|---|---|---|
| missing | `not-found` | `:not-found` | ✅ |
| denied file | `permission-denied` | `:permission-denied` | ✅ |
| denied parent | `parent-denied` | `:parent-denied` | ✅ |
| directory named `*.clj` | `not-a-regular-file` | `:not-a-regular-file` | ✅ |
| FIFO | `not-a-regular-file` | `:not-a-regular-file` | ✅ |
| symlink loop | `not-found` | `:not-found` | ✅ |
| name too long | `not-found` | `:not-found` | ✅ |
| **ENOTDIR component** | **`not-found`** | **`:parent-denied`** | ❌ |
| `../escape.clj` | `not-a-relative-source-path` | `:not-found` | ❌ (by design¹) |
| `a.txt` | `not-a-relative-source-path` | `:not-found` | ❌ (by design¹) |

¹ the CLI's `:file` is an absolute operator-named path with no lexical fence, so these two
are the entrances answering different questions rather than the same one differently. The
ENOTDIR row is not: both entrances stat the same path and one of them is wrong about what
it saw.

**Fix:** add `(fs/directory? dir)` to the `denied-ancestor` predicate and fall through to
`:not-found` otherwise; add an ENOTDIR row to the parity witness. Note
`mcp_paths/unreadable-ancestor` ([mcp_paths.clj:112](src/clj_surgeon/mcp_paths.clj#L112))
has the identical missing test — it is unreachable today only because ENOTDIR arrives as
`FileSystemException` and never as `AccessDeniedException` — and should be fixed in the
same edit, or it becomes this finding again the day the JDK changes which exception it
throws.

### 4. `mcp_paths.clj:126` — `unreadable-ancestor` publishes the server's ABSOLUTE root when the unreadable ancestor IS the root

```clojure
(let [shown (.toString (.relativize root dir))]
  (if (str/blank? shown) (.toString dir) shown))
```

When the workspace root itself is `chmod 000`, `relativize` yields `""` and the fallback
prints the absolute path. Fixture `/tmp/census17-fx/rootdenied` (mode 000):

```
resolver: :error "Source file cannot be reached: the directory
                  /tmp/census17-fx/rootdenied may not be read by this process"
TOOL:     :error "Source file cannot be reached: the directory
                  /tmp/census17-fx/rootdenied may not be read by this process
                  (src/app/arm.clj)"
```

This is the class round sixteen called blocking, from the namespace whose own docstring
([mcp_paths.clj:44](src/clj_surgeon/mcp_paths.clj#L44)) forbids it, and this round's spec
says a refusal "*shall publish the workspace-relative path*". I am **not** calling it
blocking, and the reason is honest rather than generous: the tool publishes
`workspace_root` as an absolute path in every refusal and every receipt by design, so this
discloses nothing the same map does not already disclose. It is a contract violation and a
wrong subject, not a new disclosure. Line unchanged by this round.

**Fix:** when the unreadable ancestor is the root, say so by name ("the workspace root")
rather than by path.

### 5. `core.clj:659` / `mcp_relation_census.clj:996` — a root the walk cannot enter yields a refusal that names NOTHING

New this round, at both entrances, same fixture:

```
TOOL: :directory ""
      :error  "the directory  may not be read or traversed by this process, so this
               census cannot claim to have read the tree"
      :remedy " came from the workspace walk, not from the request, … make  readable
               under the workspace root, remove it, …"
CLI:  :directory ""   — identical two-space sentences
```

`census-discovery` records `rel-dir` for the failing directory; for the root that is `""`,
and both entrances interpolate it into three sentences. A receipt that names no subject is
the class House-rule 20 exists for. The two entrances agree, which is the good news.

**Fix:** render `""` as "the workspace root" at both interpolation sites; assert it in the
unreadable-directory witness, which today only drives `src/app/hidden`.

### 6. `install_test.clj:50` — a gate that can false-RED any builder who commits mid-run

```clojure
(def ^:private source-commit                       ; evaluated at NAMESPACE LOAD
  (let [{:keys [exit out]} @(proc/process ["git" "rev-parse" "HEAD"] {:dir project-root …})]
    (if (zero? exit) (str/trim out) "unknown")))
(def ^:private stable-copy-stamp
  (str "\nStable copy installed from commit " source-commit ".\n" …))
```

and [Makefile:16](Makefile#L16) `SOURCE_COMMIT := $(shell git -C "$(CLJ_SURGEON_HOME)" rev-parse HEAD …)`,
stamped into the installed `SKILL.md` at [Makefile:537](Makefile#L537), asserted equal at
[install_test.clj:321](test/clj_surgeon/install_test.clj#L321).

**Yes — it is such a gate.** Two independent reads of one mutable ambient value at two
different times: the JVM's read when the namespace loads, and the sub-`make`'s read when
the test body runs. Any commit landing between them fails the assertion with a stamp
mismatch that has nothing to do with the change under test — and a TDD red/green loop
commits between suite runs by construction, which is why the builder hit it. (Mechanism
read from source and from the Makefile; I did not re-drive `make install`, so the field
evidence is the builder's own disclosure, not mine.)

**Fix, cheapest rung that closes it:** have the test OWN the value instead of sharing an
ambient reading — read HEAD once inside the test body and pass it down,
`(run-make "--silent" "install" (str "SOURCE_COMMIT=" head) …)`, building
`stable-copy-stamp` from the same `head`. Second-best: assert against `:source-commit` in
the `install-receipt.edn` the installer just wrote ([Makefile:538](Makefile#L538)), which
is a record of what executed rather than a second guess at it.

### 7. The CLI's `:file` follows a symlink out of the tree; its own `:dir` walk does not

Fixture `/tmp/census17-fx/esc`: `src/app/escape.clj -> ../../../outside/secret.clj`, whose
arm is named `SECRET-OUTSIDE`.

```
TOOL walk         ok true, files_scanned 1, skipped_outside_root 3   (never read)
TOOL files=[escape.clj]  refused, cause "outside-project"
TOOL files=[hosts.clj]   refused, cause "outside-project"   (-> /etc/hosts)
CLI  walk         ok true, files-scanned 1, skipped-outside-root 3   (never read)
CLI  :file escape ok true, arm "SECRET-OUTSIDE", files-scanned 1     ← READ IT
```

The MCP fence holds on every escape I could build, including a symlinked DIRECTORY out of
the tree — **no read escaped the tool's workspace**, so the brief's blocking condition is
not met. The CLI's `:file` is by contract an absolute operator-named path with no project
root, so this is not a fence breach either. It is worth recording anyway because the CLI
disagrees with **itself**: its `:dir` walk counts that same link as `skipped-outside-root`
and refuses to read it, while its `:file` reads it. Not blocking; not this round's work.

### 8. The `{:test 13 :pass 540}` battery figure does not reproduce — composition, not correctness

My thirteen (the five round-sixteen named witnesses, the five added this round, the
constructor witness, `a-shape-refusal-on-a-long-root-measures-its-continuation`, and
`the-cli-entrance-validates-every-field-before-it-loads-any-config`) give
`{:test 13, :pass 444, :fail 0, :error 0}`. I could not recover which thirteenth member
yields 540, and the difference is which vars are in the set, not whether they pass — the
full suite is green at 454/5734/0, which contains all of them. Flagging it only so the
number is not quoted onward as reproduced. **Unreproduced; suspicion of nothing.**

---

## Gates — run once each, serially, verbatim

```
mcp-test  — /home/forge/bin/suite-run clojure -J-Xms64m -J-Xmx1g -M:clj-surgeon/mcp-test
            Ran 454 tests containing 5734 assertions.
            0 failures, 0 errors.
            EXIT=0                                        [claim 454/5734/0 — MATCHES]

test-fast — /home/forge/bin/suite-run bb test/run_all.clj
            Ran 716 tests containing 6057 assertions.
            0 failures, 0 errors.
            EXIT=0                                        [claim 716/6057/0 — MATCHES]

oracle    — swipl -q -f test/mcp_operation_contract_oracle.pl
            mcp-operation oracle: pass;
              legacy counterexamples=[verification_failed,verification_pending]
            EXIT=0                                        [claim pass — MATCHES]

battery   — 13 named witnesses + schema drive, via `clojure -Spath -M:clj-surgeon/mcp-test`
            MISSING: []
            :BATTERY-RESULT {:test 13, :pass 444, :fail 0, :error 0}
                                                          [claim 13/540/0/0 — see finding 8]

neutered  — refusal-continuation-keys -> #{} on a scratch export of fb7f3b1
            Ran 454 tests containing 5734 assertions.
            2 failures, 0 errors.                         [ratchet PROVED REAL]
```

`make mcp-test` was never invoked. `make check-agent-routing` was never run and is not
counted.

---

# NO-GO for the mayor's merge queue

Two blocking items, both this round's own new spec sentences failing on the first refusal
shape outside their witness's single drive:

1. [mcp_relation_census.clj:1292](src/clj_surgeon/mcp_relation_census.clj#L1292) —
   **blocking.** `execute-request!`'s workspace-router refusal bypasses
   `census/bound-refusal` entirely: a 10,001-character `workspace_root` returns a
   10,540-byte refusal with an unbounded 10,007-character field and no truncation marker,
   against a spec sentence written this round that says no field of any refusal either
   entrance emits shall be unbounded, and against a CLI that gets it right by applying the
   bound at the entrance's last step as that same sentence prescribes.
2. [mcp_relation_census.clj:136](src/clj_surgeon/mcp_relation_census.clj#L136) —
   **blocking.** `overflow-measurement` weighs `workspace_root` and `files` and not the
   `doors` the continuation also carries: a 723-byte continuation on a 19-character root is
   refused with "*the length of the workspace path in it is: workspace_root alone measures
   19 of those bytes — retry with workspace_root reaching the same tree by a shorter
   path*". That is round-sixteen item 5's rejected receipt, from the function written to
   replace it, reachable through `execute-request!`.

Non-blocking, and they should ride the same round because each is a bound or a name this
round asserted and did not measure: **3** `denied-ancestor` calling a readable regular file
a denied directory (a false statement, an unfollowable remedy, and the only genuine
cross-entrance cause disagreement in a ten-shape enumeration); **4** `unreadable-ancestor`
printing the absolute root when the root is the unreadable ancestor; **5** a refusal that
names `""` at both entrances when the walk cannot enter the root; **6** the `install_test`
stamp comparing two independent reads of `git rev-parse HEAD` taken at different times,
which false-REDs any builder who commits mid-run; **7** the CLI's `:file` reading through a
symlink its own `:dir` walk skips; **8** the `540` battery figure, unreproduced.

**What the mayor must verify before this joins the queue:** that the refusal bound is moved
to `execute-request!`'s own return value and the overflow measurement weighs the whole
candidate — and then that the two witnesses are re-driven on the shapes that caught them
(a hostile `workspace_root`, and a continuation whose bulk is `doors`), because both of
these passed a green suite of 454 tests while being false in the field.
