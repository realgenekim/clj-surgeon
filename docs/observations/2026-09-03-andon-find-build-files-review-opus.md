# ANDON fix — Opus adversarial review of bridge/andon-find-build-files-argv at 811f4b0 (2026-09-03T21:07Z)

Verdict: **GO-WITH-FIX — merge to main, with items 1 and 2 in the same merge.** Injection reproduced three ways at 11413f2 (find-build-files, run-ls-tree, bb CLI — canary each time), not reproducible any way at 811f4b0; strace: caller value one token, zero shell execve; entrance refusal fires with `find execve count: 0`; NUL delimitation survives seven path shapes; witnesses red at the vulnerable sha (10 fail / 1 error of 12). REQUIRED same merge: (1) a source-level test that no `"sh" "-c"` / `"bash" "-c"` literal exists under src/ — the intent audit is marker-presence only (a reintroduced site with markers intact stayed `OK= true`); (2) rename the `format` binding shadowing clojure.core/format at core.clj:539 — empty-result branch throws `ArityException … :edn` (pre-existing, reachable from a symlinked, unreadable, or `..` root, and any empty tree). RECOMMENDED same merge: `-H` on both find argvs (gate follows symlinks, `find -P` does not descend a symlinked start); `fs/create-temp-dir` instead of `/tmp/andon-fx`; `catch Exception`. FOLLOW-UP: `find-start-token` handles only a leading `-`; `(`, `)`, `!`, `,` are also expression starts (a dir named `(` yields `[]`). Environment: /usr/bin/find on Anvil is bfs 4.1.1, not GNU.

## Opus verdict, verbatim

# Adversarial review — Andon fix `bridge/andon-find-build-files-argv` @ 811f4b0

Reviewer: Opus, forge@anvil seat. Worktree `/home/forge/tmp/sol/andon-wt` @ `811f4b0`
(clean, four commits on `origin/main` `11413f2`). Fixtures under `/tmp/andon-sol-fx`.
Nothing committed, stashed, or pushed. All canaries planted were checked and removed.

Environment fact that colours several results: **`/usr/bin/find` on Anvil is `bfs 4.1.1`,
not GNU findutils** (`find --version` -> `bfs 4.1.1`). See finding G6.

---

## 1. The original injection: reproduced at 11413f2, absent at 811f4b0

Vulnerable tree created with `git worktree add /tmp/andon-sol-fx/vuln 11413f2`
(`git stash` not used; worktree removed at the end of the review).

Fixture: `/tmp/andon-sol-fx/repro1/H` — a real `deps.edn` project.

### 1a. Through `find-build-files` (the private helper)

```
$ cd /tmp/andon-sol-fx/vuln && bb /tmp/andon-sol-fx/repro-fbf.clj \
    '/tmp/andon-sol-fx/repro1/H; touch /tmp/andon-sol-fx/repro1/PWNED ; echo z'
DIR= "/tmp/andon-sol-fx/repro1/H; touch /tmp/andon-sol-fx/repro1/PWNED ; echo z"
RESULT= ["/tmp/andon-sol-fx/repro1/H" ".../H/deps.edn" ".../H/src" ".../H/src/core.clj"
         "z ( -name .shadow-cljs -o ... ) -prune -o ( -name deps.edn ... ) -print"]
$ ls -la /tmp/andon-sol-fx/repro1/PWNED
-rw-r--r-- 1 forge forge 0 Sep  3 20:55 /tmp/andon-sol-fx/repro1/PWNED
```

Canary created. Note the last element of `RESULT`: the injected `echo z` output was
folded back into the returned path vector — arbitrary command execution, confirmed
twice over (side effect + stdout capture).

### 1b. Through `run-ls-tree` (the public op), `$(...)` form

```
$ cd /tmp/andon-sol-fx/vuln && bb /tmp/andon-sol-fx/repro-lstree.clj \
    '/tmp/andon-sol-fx/repro1/H$(touch /tmp/andon-sol-fx/repro1/PWNED2)'
RESULT= [{:ns core, :file "../../../../../H/src/core.clj", ...}]
$ ls -la /tmp/andon-sol-fx/repro1/PWNED2
-rw-r--r-- 1 forge forge 0 Sep  3 20:55 /tmp/andon-sol-fx/repro1/PWNED2
```

### 1c. Through the bb CLI

```
$ cd /tmp/andon-sol-fx/vuln && bb -m clj-surgeon.core :op :ls-tree :dir \
    '/tmp/andon-sol-fx/repro1/H; touch /tmp/andon-sol-fx/repro1/PWNED3 ; echo z'
{:error "'other' is different type of Path", :error-type :invalid-arguments}
$ ls -la /tmp/andon-sol-fx/repro1/PWNED3
-rw-r--r-- 1 forge forge 0 Sep  3 20:55 /tmp/andon-sol-fx/repro1/PWNED3
```

The op *errored* and the payload still ran — the shell fired before the error.

### 1d. All three, at 811f4b0

```
### A) find-build-files, semicolon
clj-surgeon: skipping project discovery; not an existing directory: "...; touch .../FIXED1 ; echo z"
RESULT= []
### B) run-ls-tree, $()
RESULT= {:error ":ls-tree :dir must be an existing directory: \"...$(touch .../FIXED2)\"",
         :error-type :workspace-root-not-a-directory, :dir "...", :next-action "pass_an_existing_directory_path"}
### C) bb CLI, semicolon
CLI-EXIT=1
{:error ":ls-tree :dir must be an existing directory: ...", :error-type :workspace-root-not-a-directory, ...}
### canaries: FIXED1 FIXED2 FIXED3 — none created
```

CLI exit 1 confirms the comment at `src/clj_surgeon/core.clj:545-547`; the mechanism is
`src/clj_surgeon/core.clj:1387-1388` (`(when (and (map? result) (:error result)) (System/exit 1))`).

**Verdict: the Andon defect is closed on all three entrances.**

---

## 2. Argument injection: `:dir` values that `find` could read as options

### 2a. The stakes are real (raw `find`, sacrificial tree)

```
$ cd /tmp/andon-sol-fx/sac && find . -mindepth 1 | sort
./a
./a/b
./a/b/victim.txt
./top.txt
$ find -delete ; echo exit=$?
exit=0
$ find /tmp/andon-sol-fx/sac -mindepth 1 | sort
              <-- empty: the whole tree was destroyed
```

A bare `-delete` reaching `find` as a positional token destroys the working tree.

### 2b. Existing directories with hostile names, passed RELATIVE to `find-build-files`

`cd /tmp/andon-sol-fx/argv` (each name is a real project directory):

| `:dir` | RESULT | option interpreted? |
|---|---|---|
| `-delete` | `["./-delete/deps.edn"]` | no |
| `-exec`   | `["./-exec/deps.edn"]`   | no |
| `-D`      | `["./-D/deps.edn"]`      | no |
| `--`      | `["./--/deps.edn"]`      | no |
| `-`       | `["./-/deps.edn"]`       | no |
| `-name`   | `["./-name/deps.edn"]`   | no |
| `(`       | **`[]`**                 | **yes — mis-parsed, see G2** |
| `!`       | **`[]`**                 | **yes — mis-parsed, see G2** |

Fixtures all intact afterwards; `/tmp/andon-sol-fx/out` (the `-fprint` target) never created.

### 2c. strace proof of the argv and its order

```
$ strace -f -e trace=execve -s 500 bb .../repro-fbf.clj '-delete'
execve(".../find", ["find", "./-delete", "(", "-name", ".clj-kondo", "-o", ... ")",
  "-prune", "-o", "(", "-name", "deps.edn", "-o", "-name", "project.clj",
  "-o", "-name", "bb.edn", ")", "-print0"], ...)
```

`dir` is exactly one token, `./`-prefixed by `find-start-token`
(`src/clj_surgeon/core.clj:170-175`). **Zero `execve` of any shell** in the whole trace
(8 `execve` calls total; `grep -E '"/bin/sh"|"sh"|bash'` -> no match).

### 2d. Does the entrance refusal fire BEFORE `find`?

Non-existent hostile `:dir` values, traced:

```
:dir = "-fprint /tmp/andon-sol-fx/out"       -> RESULT= []   find execve count: 0
:dir = "-delete"                              -> RESULT= []   find execve count: 0
:dir = "-exec rm -rf /tmp/andon-sol-fx/argv ;"-> RESULT= []   find execve count: 0
```

**`find` is never executed at all when the directory does not exist.** Ordering proven
(`src/clj_surgeon/core.clj:204` in `find-build-files`; `:526-537` + `:545` for the
`:ls-tree` entrance).

### 2e. Through the public op (absolutized)

`run-ls-tree` on `/tmp/andon-sol-fx/argv/{-delete,-exec,--,-,(,!}` returns the correct
project outline for every one. Absolutization removes the leading-`-` class entirely at
the public entrance; `find-start-token` is the belt for direct/relative callers.

---

## 3. `grep-tree`: option interpretation

### 3a. ripgrep branch (`rg` 15.1.0 on PATH)

```
pattern "needle"                    -> #{"/tmp/andon-sol-fx/gt/proj/src/core.clj"}
pattern "-e x"                      -> #{}
pattern "--include=*"               -> #{}
pattern "-f /etc/passwd"            -> #{}
pattern "--pre=/bin/sh"             -> #{}
pattern "-x"                        -> #{}
```

Decisive RCE probe — `--pre` is ripgrep's *run a preprocessor command* flag:

```
$ cat /tmp/andon-sol-fx/pre.sh
#!/bin/sh
touch /tmp/andon-sol-fx/PRE-PWNED
cat "$1"
$ bb .../repro-grep.clj '--pre=/tmp/andon-sol-fx/pre.sh' /tmp/andon-sol-fx/gt
RESULT= #{}
canary PRE-PWNED: ls: cannot access '/tmp/andon-sol-fx/PRE-PWNED': No such file or directory
```

Decisive pattern-file probe (`patfile.txt` contains `needle`, which WOULD match
`core.clj` if `-f` were honoured):

```
rg branch:   '-f /tmp/andon-sol-fx/patfile.txt' -> #{}     (not honoured)
grep branch: '-f /tmp/andon-sol-fx/patfile.txt' -> #{}     (not honoured)
```

strace, rg branch:

```
execve(".../rg", ["rg", "-li", "--null", "-g", "*.clj", ..., "-g", "bb.edn",
  "-e", "--pre=/tmp/andon-sol-fx/pre.sh", "--", "/tmp/andon-sol-fx/argv/-delete"], ...)
```

### 3b. system-grep fallback branch (rg removed from PATH; GNU grep 3.12)

PATH restricted to `/tmp/andon-sol-fx/nopath` (symlinks to bb/find/grep/sh/timeout, no rg).

```
WARNING: ripgrep (rg) not found. Falling back to grep (much slower).
pattern "needle"             -> #{"/tmp/andon-sol-fx/gt/proj/src/core.clj"}
pattern "-e x"               -> #{}
pattern "--include=*"        -> #{}
pattern "-f /etc/passwd"     -> #{}
pattern "--devices=recurse"  -> #{}
dir "/tmp/andon-sol-fx/argv/-delete", pattern "defn"
                             -> #{"/tmp/andon-sol-fx/argv/-delete/src/core.clj"}
```

strace, grep branch:

```
execve(".../grep", ["grep", "-rliZE", "--include=*.clj", ..., "--exclude-dir", ".shadow-cljs",
  "-e", "-f /tmp/andon-sol-fx/patfile.txt", "--", "/tmp/andon-sol-fx/argv/-delete"], ...)
```

**No option interpretation left on either branch, for pattern or for dir.** A dir starting
with `-` is handled by `--` (`src/clj_surgeon/core.clj:340,349`), not by `find-start-token`.

---

## 4. `-print0` parsing

Tree `/tmp/andon-sol-fx/nul` with seven project directories whose names sit directly
against the NUL delimiter:

```
dollar$name   endnl\n   lead space   nl\nname   quote"name   tab\tname   trail<space>
```

`find-build-files`:

```
RESULT= ["/tmp/andon-sol-fx/nul/dollar$name/deps.edn" "/tmp/andon-sol-fx/nul/endnl\n/deps.edn"
         "/tmp/andon-sol-fx/nul/lead space/deps.edn"  "/tmp/andon-sol-fx/nul/nl\nname/deps.edn"
         "/tmp/andon-sol-fx/nul/quote\"name/deps.edn" "/tmp/andon-sol-fx/nul/tab\tname/deps.edn"
         "/tmp/andon-sol-fx/nul/trail /deps.edn"]
```

All seven survive intact — including the **trailing newline** and **trailing space** cases,
which a `str/trim`-based parser would have eaten. `nul-separated-paths`
(`src/clj_surgeon/core.clj:178-188`) correctly does not trim.

End to end, both `:ls-tree` paths return all seven:

```
full scan : "dollar$name/src/core.clj" "endnl\n/src/core.clj" "lead space/src/core.clj"
            "nl\nname/src/core.clj" "quote\"name/src/core.clj" "tab\tname/src/core.clj" "trail /src/core.clj"
:grep path: (identical set)
```

Empty output: `find-build-files "/tmp/andon-sol-fx/emptytree"` -> `RESULT= []`
(`str/split` of `""` yields `[""]`, dropped by the `remove #(= "" %)`). No off-by-one.

Embedded NUL *in the argument string* (the case `existing-directory?` claims never throws):

```
:dir = "/tmp/andon-sol-fx/ref/real\0/x"
RESULT= {:error ":ls-tree :dir must be an existing directory: \"...real^@/x\"",
         :error-type :workspace-root-not-a-directory, ...}
```

Typed refusal, no throw. `src/clj_surgeon/core.clj:163-168` holds.

---

## 5. The typed refusal: edge cases

| `:dir` | Result | Anything execute? |
|---|---|---|
| dangling symlink | `{:error-type :workspace-root-not-a-directory ...}` | no |
| 4118-char path | `{:error-type :workspace-root-not-a-directory ...}` | no |
| embedded NUL | `{:error-type :workspace-root-not-a-directory ...}` | no |
| **symlink -> directory** | **throws `ArityException: Wrong number of args (3) passed to: :edn`** | `find` runs, returns nothing |
| **directory `chmod 000`** | **same ArityException** | `find` runs, permission-denied |
| **`..` escaping to `/etc`** | **same ArityException** | `find` runs over `/etc` |

Diagnosis (finding G3): three separate causes converge on one crash.

```
$ find /tmp/andon-sol-fx/ref/link \( -name .git \) -prune -o \( -name deps.edn \) -print0
              <-- empty; find (-P default) does not descend a symlinked START POINT
$ find -H /tmp/andon-sol-fx/ref/link \( -name deps.edn \) -print0
/tmp/andon-sol-fx/ref/link/deps.edn
$ bb -e '(fs/directory? "/tmp/andon-sol-fx/ref/link")'
:directory? true
```

So `existing-directory?` (`core.clj:163`, `Files.isDirectory`, follows links) says yes and
`find` says no — a composite state with two disagreeing authorities. Zero projects then
reaches `run-ls-tree`'s empty branch, where the destructured key `format`
(`core.clj:539`) shadows `clojure.core/format`, so `(format "No Clojure files found under %s%s" ...)`
at `core.clj:556` calls the **keyword `:edn` as a function with 3 args**:

```
$ bb -m clj-surgeon.core :op :ls-tree :dir /tmp/andon-sol-fx/ref/link :format :edn
CLI-EXIT=1
{:error "Wrong number of args (3) passed to: :edn", :error-type :invalid-arguments}
```

**Pre-existing** — the same three probes crash identically at `11413f2`, so this is not a
regression introduced by the fix. But it is verbatim the falsifier wording of
MCP-OP-SHELL-ARGV-002 ("reaches discovery, and reports an untyped error (or a stack trace)
instead of a refusal"), landing one predicate outside the new refusal's reach.

---

## 6. The intent leaf

```
$ bb .../audit2.clj .            # clj-surgeon.mcp-intent-contract/audit-current-repository
OK= true
violations= []
specs        [["MCP-OP-SHELL-ARGV-001" :implemented] ["MCP-OP-SHELL-ARGV-002" :implemented]
              ["MCP-OP-SHELL-ARGV-003" :implemented]]
impl-witness ["MCP-OP-SHELL-ARGV-001" "MCP-OP-SHELL-ARGV-002" "MCP-OP-SHELL-ARGV-003"]
test-witness ["MCP-OP-SHELL-ARGV-001" "MCP-OP-SHELL-ARGV-002" "MCP-OP-SHELL-ARGV-003"]
```

Green, wired at `src/clj_surgeon/mcp_intent_contract.clj:114-115`, three implemented
requirements each carrying both an implementation and a test witness.

**Non-vacuity, positive control** (on a copy at `/tmp/andon-sol-fx/auditcopy`, never the
worktree): deleting the `;; @spec MCP-OP-SHELL-ARGV-002` marker line turns it red —

```
OK= false
violations= [{:type :missing-implementation-witness, :intent "MCP-OP-SHELL-ARGV-002",
              :source-kind :implementation}]
```

**Non-vacuity, negative control — this is finding G1.** On the same copy I reintroduced a
string-interpolating `sh -c` discovery site while leaving every marker intact:

```clojure
;; @spec MCP-OP-SHELL-ARGV-001
(defn- find-build-files-legacy [dir]
  (babashka.process/shell {:out :string :continue true} "sh" "-c"
                          (format "find %s -name deps.edn -print" (str dir))))
```

```
OK= true
violations= []
```

The audit **did not notice**. `audit-contract` (`src/clj_surgeon/mcp_intent_contract.clj:40-81`)
checks only that an `@spec <ID>` marker exists in some implementation source and some test
source. It is a marker-presence audit, not a structural one, so MCP-OP-SHELL-ARGV-001's
own words — "the promise is **structural**: there is no shell" — are not enforced by it.

**Falsifier / Misreadings quality:** the falsifier table
(`docs/intent/shell-argv-safety/shell-argv-safety-design.md`) names three witnesses, and all
three exist in `test/clj_surgeon/core_discovery_test.clj` with those exact names. The
"Misreadings" section **does** forbid escaping-as-a-fix, explicitly and first:
*"'Escape or quote the directory before interpolating it.' A filter is weakenable by the
next refactor and was already the shape of the defect. MCP-OP-SHELL-ARGV-001 forbids the
command string, not a character class."* It also forbids entrance-check-only, newline-safety
assumptions, and empty-result-as-refusal. That section is correct and load-bearing.

**Red-first verified independently.** I copied the committed test namespace into the
`11413f2` worktree and ran it there:

```
$ cd /tmp/andon-sol-fx/vuln && bb .../redcheck.clj
Ran 3 tests containing 12 assertions.
10 failures, 1 errors.
RED-CHECK at 11413f2 => {:test 3, :pass 1, :fail 10, :error 1, :type :summary}
```

The witnesses genuinely falsify. They are not decoration.

The Prolog oracle contains **zero** `shell`/`argv` terms
(`grep -ci 'shell\|argv' test/mcp_operation_contract_oracle.pl` -> `0`); it passes but does
not model this invariant. That is fine — a relational shadow oracle is not the right rung
here — but it should not be cited as coverage.

---

## 7. The builder's two flagged choices

### 7a. `/tmp/andon-fx` hardcoded in a committed test — **WRONG, fix it**

`test/clj_surgeon/core_discovery_test.clj:21-23`:

```clojure
(def ^:private andon-fixture-root "/tmp/andon-fx")
```

- The repo idiom is `fs/create-temp-dir`: **55** call sites across `test/`.
- The path already exists on this box holding the builder's *unrelated* scratch
  (`green2-bb.txt`, `mcp.txt`, `oracle.txt`, `red-bb.txt`) — the committed suite now writes
  into a human scratch directory.
- The root is **never cleaned**. `fs/delete-tree` removes only the per-test uuid subdir
  (verified: no residue after both suites), so `/tmp/andon-fx` is left behind permanently
  with the creator's ownership. On a shared box a foreign-owned `/tmp/andon-fx` with
  restrictive perms fails this suite for every other user — a cross-user test break with no
  code change.
- The namespace now runs in **both** `test/run_all.clj:91` and
  `test/clj_surgeon/mcp_test_runner.clj:46`, which the fleet runs concurrently. The
  `random-uuid` subdir is the only thing preventing collision; the fixed root removes the
  isolation that `create-temp-dir` would have given for free.

One-line fix: `(fs/create-temp-dir {:prefix "andon-shell-safety"})` per test.

### 7b. `catch Throwable` around `run-ls-tree` in the e2e witness — **RIGHT, with a nit**

`test/clj_surgeon/core_discovery_test.clj:76,92`. The worry is that it masks a regression.
It does not: the catch rewraps into `{:threw ...}`, and the load-bearing assertion
`(= :workspace-root-not-a-directory (:error-type result))` then **fails loudly**. Proven by
the red run at `11413f2`, where both the canary assertion and the `:error-type` assertion
reported as named FAILs rather than one ERROR aborting the deftest and hiding the canary
check. Keeping the two assertions independently reportable is the correct call for a
security witness. Nit only: `catch Exception` would be tighter — `Throwable` also swallows
`Error`/`StackOverflowError`, which are never the expected outcome here. Not a blocker.

---

## 8. Gates (all under `/home/forge/bin/suite-run`, this branch off `11413f2`)

| Gate | Command | Result |
|---|---|---|
| bb suite | `suite-run bb test/run_all.clj` | `Ran 705 tests containing 5925 assertions. 0 failures, 0 errors.` EXIT=0 |
| mcp-test | `suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 380 tests containing 3962 assertions. 0 failures, 0 errors.` EXIT=0 |
| oracle | `swipl -q -f test/mcp_operation_contract_oracle.pl` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` EXIT=0 |

Independently reproduced; matches the builder's reported baselines exactly (705/5925/0,
380/3962/0, oracle pass). `make mcp-test` was not used. Worktree `git status` clean at
`811f4b0` after the review.

---

# VERDICT: GO-WITH-FIX — merge to main, with items 1 and 2 in the same merge

The Andon defect is genuinely and structurally closed. I reproduced arbitrary command
execution three ways at `11413f2` and could not reproduce it any way at `811f4b0`; strace
shows the caller value as exactly one token with no shell in the process tree; the
entrance refuses before `find` is executed at all; NUL delimitation is correct against
seven adversarial path shapes and against empty output; the witnesses go red at the
vulnerable commit; all three gates are green at the builder's numbers. The
release-lane freeze can lift on this merge.

1. **Add the class-level ratchet the leaf's own text promises (REQUIRED, same merge).**
   The intent audit is marker-presence only: I reintroduced a `format`+`sh -c`
   discovery site in `src/` with all `@spec` markers intact and
   `audit-current-repository` stayed `OK= true, violations= []`. MCP-OP-SHELL-ARGV-001
   says "the promise is structural: there is no shell", but nothing enforces that on a
   *new* site — the two canary tests only pin `find-build-files`. Add one source-level
   test asserting no `"sh" "-c"` / `"bash" "-c"` argv literal appears anywhere under
   `src/` (allowlist by name if one is ever legitimate). It is a single test and it is
   the rung the house-rules ratchet ladder asks for on load-bearing code.

2. **Rename the `format` binding that shadows `clojure.core/format` (REQUIRED, same merge,
   two lines).** `src/clj_surgeon/core.clj:539` destructures `format`, so the empty-result
   branch at `core.clj:556` calls the keyword `:edn` as a function and throws
   `ArityException: Wrong number of args (3) passed to: :edn`. Reachable today from a
   symlinked root, a `chmod 000` root, a `..`-escaping root, and any genuinely empty
   tree. Pre-existing at `11413f2` — not a regression — but it is the exact untyped-error
   outcome MCP-OP-SHELL-ARGV-002's falsifier forbids, sitting one predicate outside the
   new refusal, and this is the commit that claims to have typed that entrance.

3. **Make the gate and the executor agree on "directory" (RECOMMENDED, same merge).**
   `existing-directory?` (`core.clj:163`) follows symlinks; `find` (`-P` default) does not
   descend a symlinked start point. A symlinked workspace root therefore passes the gate
   and silently discovers nothing. Add `-H` to both `find` argvs (`core.clj:216`, `:265`),
   or resolve with `fs/real-path` before the check. One authority, one answer.

4. **Widen `find-start-token` beyond `-` (RECOMMENDED, follow-up bead).** It handles only
   leading `-`; `find`/`bfs` also treat `(`, `)`, `!`, `,` as expression starts. A relative
   dir literally named `(` yields `[]` from `find-build-files` (raw find: `bfs: error:
   Expected a ).`) while `./(` yields the deps.edn — a silently missing project, the same
   symptom class MCP-OP-SHELL-ARGV-003 exists to forbid. Not reachable through
   `run-ls-tree` (absolutized) but reachable through `rename/plan`'s `:root`
   (`src/clj_surgeon/rename.clj:118-121`). Fix: `./`-prefix every relative start point,
   not just `-`-leading ones. Also correct the design doc, which currently claims
   "Argument injection is CLOSED at every discovery site" — it is closed for the
   `-`-leading class only.

5. **Replace the hardcoded `/tmp/andon-fx` with `fs/create-temp-dir` (RECOMMENDED,
   same merge).** `test/clj_surgeon/core_discovery_test.clj:23`. Against a repo idiom of
   55 `create-temp-dir` sites; the root is never cleaned; a foreign-owned `/tmp/andon-fx`
   breaks this suite for every other user on a shared box; and the namespace now runs in
   both concurrently-executed runners.

6. **Tighten `catch Throwable` to `catch Exception` (NIT).**
   `test/clj_surgeon/core_discovery_test.clj:76,92`. The rewrap-into-a-map pattern itself
   is correct and should stay — it keeps the canary assertion and the `:error-type`
   assertion independently reportable, which I verified at `11413f2`.

7. **Do not cite the Prolog oracle as coverage for this leaf.** It passes, but
   `grep -ci 'shell\|argv' test/mcp_operation_contract_oracle.pl` -> `0`. It models nothing
   here, and the design doc rightly does not claim otherwise — keep it that way.

8. **Record the environment fact (INFORMATIONAL).** `/usr/bin/find` on Anvil is
   **`bfs 4.1.1`**, not GNU findutils. Every `find` behaviour measured above is bfs's. The
   argv shape is compatible with both, but `-print0`, parenthesised `-name` alternation,
   and the `(`/`!` mis-parse have been verified here against bfs only. Worth one line in
   the design doc so the next reviewer does not mistake this for a findutils receipt.
