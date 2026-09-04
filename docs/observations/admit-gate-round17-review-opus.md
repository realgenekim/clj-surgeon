## NO-GO

Independent adversarial review — clj-surgeon admit gate **ROUND SEVENTEEN**, tip `2ac33278`
(branch `bridge/admit-gate-r16`). Reviewer: **Opus fallback** (Sol's content filter refused this
review). Review clone `/home/forge/tmp/sol/gate4-wt`, detached at `2ac33278`, `git status
--porcelain` empty, never checked out. All suites and fixtures ran in real `git clone`s under
`/var/tmp/forge/gate17-opus-fx`. No commit, stage, stash, push, or merge. No server started; no
contact with 7888/7890/7894/7895/8171/8173.

**Three blocking findings, and none of them is in the round's new contract as designed — all three
are in the six-line copy loop the new contract stands on.** `overlay-snapshot!` (a) follows symbolic
links and derives each copy's destination from the link's *canonical* target, so a workspace holding
a symlink to an outside file makes the gate write **outside the overlay root** and truncate that
file to zero, in `preview` and `propose`, on a receipt that says `committed=false,
mutation_attempted=false, ok=true`; and (b) does not preserve file modes, so an executable script
the caller's verify command runs becomes 0644 in the overlay and the verification fails with
`Permission denied` against a tree that does not exist anywhere. Separately, (c) `output_tail`
publishes the last 40 lines of the **first 12 KB** of a command's output rather than its tail, with
no truncation flag — which makes MCP-OP-ADMIT-156's headline claim false for every real test suite.
Everything else in items 1–8 either holds or is a named, non-blocking defect.

---

# 1. BLOCKING — the overlay copy escapes its own root and destroys files outside the workspace

**Where:** `src/clj_surgeon/mcp_admit_tool.clj:1081-1120` (`overlay-snapshot!`), with
`src/clj_surgeon/mcp_admit_tool.clj:1049-1079` (`overlay-source-files`).

**Mechanism, read from the source:**

```clojure
;; overlay-source-files: a symlink to a file is a file; a symlink to a directory is a directory
(.isDirectory current)   ; java.io.File.isDirectory FOLLOWS links
:else (recur rest-pending (conj files current) ...)

;; overlay-snapshot!: the destination is derived from the link's RESOLVED target
source-path (.toPath (.getCanonicalFile source-root))
relative    (str (.relativize source-path (.toPath (.getCanonicalFile file))))
target      (io/file root relative)
(io/copy file target)
```

`(.getCanonicalFile file)` resolves the symlink to its target *outside* the workspace, so
`relativize` yields a `../…` path, `target` lands outside `root`, and `io/copy` opens the same
inode for read and write — truncating the victim to zero bytes. Nothing checks that `target` is
under `root`.

**Reproduction** (`/var/tmp/forge/gate17-opus-fx/attacks/symlink.clj`, run in the fresh clone at
`2ac33278`):

```
cd /var/tmp/forge/gate17-opus-fx/clone1 && java -cp "$(cat ../cp.txt)" clojure.main \
    /var/tmp/forge/gate17-opus-fx/attacks/symlink.clj
```

Verbatim output:

```
BEFORE: victim bytes = 30  content = "PRECIOUS-CONTENT-DO-NOT-TOUCH\n"
gate: ok= true  op= :admit-patch-proposed  committed= false  mutation_attempted= false
AFTER : victim bytes = 0  content = ""
D1 VERDICT: file outside the workspace was DESTROYED

D2 preview: ok= true
D2 victim2 after = "" bytes= 0
```

On-disk confirmation after the JVM exited (no fixture teardown touched these paths):

```
$ ls -la /var/tmp/forge/gate17-opus-fx/esc/
-rw-r--r-- 1 forge forge   22 Sep  4 21:33 other-source.txt
-rw-r--r-- 1 forge forge    0 Sep  4 21:33 victim.txt
-rw-r--r-- 1 forge forge    0 Sep  4 21:33 victim2.txt
```

The workspace itself was a fresh temp dir containing only `src/app/core.clj`,
`src/app/util.clj` and one symlink named `innocent-link.txt`.

A symlink to a *directory* outside the workspace is worse: `.isDirectory` follows the link, the
walk descends into it, and **every file underneath is truncated in place**. Reproduced in the
earlier `ceiling.clj` run (case C6): the command's `find . -name secret.txt` found nothing inside
the overlay, and the file's bytes were gone from the outside directory.

**Why this is blocking, not a fix-later:**

1. **It destroys data outside the declared blast radius.** The gate's entire proposition is that it
   is safer than `apply_patch`. This is a path where it is strictly less safe than the tool it
   replaces.
2. **It fires in the modes that promise no write.** `propose` published
   `mutation_attempted=false, committed=false, ok=true` on the same call that zeroed a file. That
   is the exact class house rule 20 names: *a receipt that names a subject it was blind to.* A
   false green terminates investigation.
3. **No attacker is required.** Symlinks into a shared cache, a `.venv`, a vendored checkout, a
   `node_modules/.bin` entry, a linked `resources` directory, or a dotfile link are ordinary in the
   real repositories this round exists to serve. The field repo that motivated the round is a
   JavaScript/Clojure tree.
4. **It is reachable only through the code this round added.** `overlay-snapshot!` is new on this
   branch; the profile path never copied the workspace.

**The fix is small and belongs in the same commit** (and, per the house ratchet ladder, at the
typed-refusal rung, because it makes the bad state unrepresentable rather than detected):

- walk with `java.nio.file.Files/walkFileTree` **without** `FOLLOW_LINKS` (or test
  `Files/isSymbolicLink` and skip / recreate the link rather than dereference it);
- derive `relative` from the **non-canonical** path, so the destination is a function of the tree's
  own shape;
- and, as the belt-and-braces invariant, refuse any `target` whose normalized path is not under
  `root` — a typed, counted refusal, not a silent skip.

A witness must go red on an unfixed tree: a workspace with one symlink to an outside file, asserting
that file's bytes are untouched after `preview`, `propose` and `commit`.

*(Non-finding, checked and clean: a symlink **cycle** does not hang the walk — `D3 result = ok=true
… overlay_files=2`. The walk terminates.)*

---

# 2. BLOCKING — the overlay does not preserve file modes, so the verify runs against a tree that does not exist

*(Raised by the coordinator from live arm G3 on the real repo, where the gate admitted twice and the
overlay changed executable CLI fixtures from 0755 to 0644, forcing the agent to add a Makefile
mode-normalization workaround — a product change the gate imposed on the caller. Confirmed at the
tip, with a control.)*

**Where:** the same copy loop, `src/clj_surgeon/mcp_admit_tool.clj:1105-1111`. `io/copy` writes a
**new** file through an output stream; it carries no attributes, so every copied file lands at the
process umask (0644 here) regardless of its source mode.

**Reproduction** (`/var/tmp/forge/gate17-opus-fx/attacks/modes.clj`), verbatim:

```
WORKSPACE bin/check mode = [GROUP_EXECUTE, OTHERS_READ, OWNER_EXECUTE, OTHERS_EXECUTE, OWNER_READ, OWNER_WRITE, GROUP_READ]

===== E1 verify runs the repo's own executable script =====
ok= false  op= :admit-patch-refused  et= :verification-failed  committed= false
error= "Snapshot verification failed (inline-verify-command-failed); nothing was written; its last
        lines were:\nCannot run program \"./bin/check\" (in directory
        \"/var/tmp/forge/clj-surgeon-admit-overlay2265689057566235321\"):
        Exec failed, error: 13 (Permission denied) "
row= {:command ["./bin/check"], :argv ["./bin/check"], :exit nil, :finished false,
      :status "did-not-finish", ...}

===== E2 modes across the overlay copy =====
source  bin/check perms = [GROUP_EXECUTE, OTHERS_READ, OWNER_EXECUTE, OTHERS_EXECUTE, OWNER_READ, OWNER_WRITE, GROUP_READ]  canExecute= true
overlay bin/check perms = [OTHERS_READ, OWNER_READ, OWNER_WRITE, GROUP_READ]  canExecute= false
source  mtime = 1788558335146
overlay mtime = 1788558335147
E2 VERDICT: overlay preserves the executable bit?  false

===== E3 CONTROL: same script in the WORKSPACE =====
exit= 0  out= "CHECK-OK\n"
```

The control matters: the identical script, run from the identical workspace, exits 0 and prints
`CHECK-OK`. It fails **only** inside the overlay, and only because the gate changed it.

**Ranking: blocking for inline verify, and I agree with the coordinator's expectation.** The intent's
own justification for the overlay is that *"a green here is a green about the bytes the gate is
about to write and never about the bytes already on disk."* File modes are part of the tree a verify
command runs against. A gate that silently rewrites the tree it is verifying is verifying a
different tree — and unlike finding 1 this is not a corner case: `./bin/test`, `script/test`,
`./gradlew`, `./mvnw`, `node_modules/.bin/jest` are the ordinary spelling of a repository's own
verify command. Every such repository gets a **guaranteed false refusal**, and the field's only
recourse was to change the product to suit the gate.

Two aggravating details:

1. **The failure is misreported as a deadline.** `:exit nil`, `:finished false`,
   `:status "did-not-finish"` — the same triple a timeout produces — and the refusal sentence omits
   the `"; the command exited N"` clause entirely. A reader sees "verification did not finish" and
   reasonably concludes the suite hung. The real cause survives only inside `output_tail`. A launch
   failure deserves its own kind (see also finding 7(c), the nonexistent-`cwd` case, which is the
   same conflation).
2. **mtimes are not preserved either** (source `…146`, overlay `…147`; in general the copy stamps
   "now"). Any verify that is incremental — `make`, a build cache, a test runner that skips
   unchanged files — sees a tree in which *every* file is newer than every artifact. That does not
   fail loudly; it silently changes what the verification actually did.

**Fix, and it is the same edit as finding 1:** replace the `io/copy` loop with
`java.nio.file.Files/copy` carrying `COPY_ATTRIBUTES` and `NOFOLLOW_LINKS`, recreating symlinks as
symlinks, and refuse any destination that normalizes outside the overlay root. One loop, three
defects. The witness: a workspace with a 0755 script and a symlink, asserting the overlay copy is
executable, the link is still a link, and the outside file is untouched.

---

# 3. BLOCKING FOR ITS OWN INTENT — `output_tail` is not the tail, and nothing says so

**MCP-OP-ADMIT-156** is carried as implemented: *"the LAST 40 lines of its merged output shall reach
the caller VERBATIM."* It does not, for any command whose output exceeds **12,000 bytes** — which is
every real test suite, i.e. exactly the workload the round was written for.

**Mechanism.** `run-inline-commands!` (`mcp_admit_tool.clj:1124`) calls the 3-arity
`change-buffer/run-process!`, which defaults `visible-byte-limit` to
`exact-verification-visible-bytes` = **12000** (`mcp_change_buffer.clj:29,1288`). `file-evidence`
(`mcp_process.clj`) then reads the **first** `visible-byte-limit` bytes of the capture file:

```clojure
(let [byte-count (.length ^java.io.File file)
      visible-count (min byte-count (long visible-byte-limit))
      visible (byte-array (int visible-count))]
  ... (.read input visible offset ...)      ; from the HEAD of the file
```

`inline-output-tail` then takes the last 40 lines **of that head**. The receipt therefore publishes
the last 40 lines of the first 12 KB and calls them `output_tail`.

**Reproduction** — a command that prints 4,000 noise lines, then the failing assertion, then exits 5:

```
==========  A10b LARGE FAILING OUTPUT -- is the real tail delivered?  ==========
exit= 5  verify_ok= false
output_tail FIRST line: "noise line 218 padding padding padding padding"
output_tail LAST line: "noise line 257 padding padding "
output_tail contains the real failing assertion?  false
output_tail chars= 1864  lines= 40
row keys= (:argv :command :elapsed_ms :exit :finished :output_tail :status)
any truncation flag on the row/receipt?  {}
```

Three things are wrong at once, and the third is what makes it blocking rather than cosmetic:

1. the failing assertion — the one fact 156 says "makes the next edit possible" — is **absent**;
2. the last line is cut **mid-word** (`"noise line 257 padding padding "`), so the caller is handed
   a corrupted fragment presented as verbatim output;
3. `run-bounded!` computes `:output-truncated` and `:output-bytes`, and `run-inline-commands!`
   **carries neither onto the row**. The row's key set is
   `(:argv :command :elapsed_ms :exit :finished :output_tail :status)`. There is no way for a caller
   to know it is looking at the middle of the output.

The field consequence is precisely the one the round set out to remove: the agent reads 40 lines of
noise, learns nothing, and re-runs the suite outside the gate. The small-output case is genuinely
good (finding 7 below), which is what makes the silence dangerous — the shape looks right.

**Fix:** read the capture file's **tail** (seek to `length - N`), raise the inline limit well above a
receipt budget since the receipt publishes only 40 lines of it anyway, and carry
`output_truncated` / `output_bytes` on every row. The same path serves the repository-declared
runner (`runner-output-tail`, line 850), so fix it once at `run-process!`. A witness must assert the
assertion survives at, say, 200 KB of preceding output.

---

# 4. The coordinator's first attack — an `unsupported-patch-target` refusal hands back the call that just refused

Reproduced exactly, on a fixture with one `.clj` and one `.js` in the same patch:

```
==========  A14 mixed clj+js patch  ==========
ok= false  error-type= :unsupported-patch-target
error= "The gate admits Clojure and EDN sources only; apply these natively: resources/public/js/editor.js"
next_call= {:tool "admit_clojure_patch",
            :arguments {:mode "preview", :verify "focused", :workspace_root "…"},
            :patch_field "patch",
            :patch_sha256 "a2dd0279ac12bcb07557b70b525a628763a1b37614322058c6e59f5d5887a858",
            :note "resend the same patch text in the patch field; it is deliberately not echoed here",
            :blocked_by :unsupported-patch-target}
REQUEST WAS: mode= preview  verify= "focused"
next_call.arguments == request shape?  true
second identical call identical refusal?  true
```

The kind and the remedy sentence are right. The `next_call` is the **identical call** — same mode,
same verify, same patch digest, and a note instructing the caller to resend the same patch text. An
agent that follows `next_call` loops forever, which is what the live arm did twice.

**This is structural, not local to this kind.** `refusal` (`mcp_admit_tool.clj:394-402`) ends every
typed refusal with `(next-call context "preview" error-type)`, and `next-call`
(`mcp_admit_tool.clj:179-212`) always appends *"resend the same patch text."* So **any** refusal
raised from a `preview` call with a `focused`/`none` verify hands back a byte-identical call. Only
the refusals that build their own `next_call` (verification-incomplete, verification-failed,
hazards) escape it. `:unsupported-patch-target` is simply the kind an agent meets most on a real
mixed-language repository.

**Ruling: non-blocking, must fix before this contract is mandated as a write path again.** It cannot
corrupt anything and it cannot be reached from an otherwise-admissible patch — the patch is refused
on its own shape before any snapshot, hazard analysis or write. But it is a *liveness* defect in the
one field the round added to end exactly this behaviour, and the round's own thesis is that a
refusal's remedy must name a call the agent can act on. The rule to write down, in the intent:

> A `next_call` published on a refusal shall never be a call whose `arguments` and `patch_sha256`
> equal the call that was refused. Where no different call can lift the refusal, `next_call` shall
> be `nil` and the receipt shall say what to do outside the gate.

For this kind specifically, either remedy works: `next_call` naming the reduced patch (apply the
named non-Clojure files natively, resend with only the admitted files), or — better, and it is the
r18 ask already on the board — the gate splits the patch itself, admits the clj/edn part, and names
the js files as native-apply on the receipt. The second is the one that removes the round trip.

---

# 5. The same class again — the missing-profile `next_call` is itself refused (the naive-reader probe)

MCP-OP-ADMIT-154 requires the remedy to be *"spelled as JSON a caller can send."* The refusal
sentence is excellent (finding 6). The `next_call` is not sendable:

```
===== B7 naive reader sends next_call VERBATIM =====
next_call.verify = {:commands ["<your project's test command, e.g. make test>"]}
result ok= false  et= :invalid-admit-request
error= "every verify command must be a plain command line ("make test") or an array of arguments
        (["make", "test"]); command 1 is neither. clj-surgeon never hands a string to a shell, so a
        command carrying a shell metacharacter (| & ; < > $ ` ( ) quotes globs) or a leading
        NAME=value assignment must be sent as an array …"
```

The placeholder contains `<` and `>`, which are in `inline-shell-metacharacters`, so the gate refuses
its own suggestion — with a message about shells that has nothing to do with the caller's mistake.
A naive reader following the affordance lands on a second, more confusing refusal.
`mcp_admit_tool.clj:2082-2087`. **Non-blocking; one-line fix** (`["make" "test"]`, matching the
prose remedy, which already says `"commands": ["make test"]`).

---

# 6. `verify_ok: true` on a call that verified nothing

`:verify_ok (nil? blocked)` (`mcp_admit_tool.clj:2419, 2483, 2531`) is `true` whenever no check
*failed*, including when no check ever *ran*:

```
==========  A12 propose with verify none -- what does verify_ok say  ==========
ok= true  op= :admit-patch-proposed  verify_ok= true  committed= false
tests.commands= nil
```

MCP-OP-ADMIT-157 says `verify_ok` "carries the verification's own verdict." With `verify: "none"`
there is no verdict, and `true` is the reading a caller will act on. `verification_complete` is
`false` on the same receipt, so the two fields disagree in the direction that flatters the patch.
`verify_ok` is present on all three published shapes (`preview`, `propose`, `commit`), so it did
join the closed receipt key set as commit `b5251ef9` claims — the key is right, the value is not.
**Non-blocking; `verify_ok` should be absent or `null` when `verify` is `"none"`.**

---

# 7. What holds — item by item

### Claim 1, inline verify (MCP-OP-ADMIT-153/154)

| attack | result |
|---|---|
| `"make a && make b"`, `\|`, `;`, `$HOME`, backticks, `NAME=value`, `>`, `*.clj`, newline, tab | **typed refusal**, `:invalid-admit-request`, message names the array form *and* `["sh","-c","…"]` — 10/10 |
| `["sh","-c","echo SHELL-RAN; id -u"]` explicit array | **runs**, exit 0, `output_tail "SHELL-RAN\n1011"` — the caller's own argv, as the intent says |
| overlay write (`echo CLOBBERED > src/app/util.clj; rm -f src/app/core.clj`) | exits 0 inside the overlay; **workspace byte-identical** |
| `verify.cwd` `/etc`, `../..` | typed refusal: *"cwd must be a relative path inside the snapshot, with no parent segments"* |
| never-exiting `sleep 400`, `timeout_ms 4000` | stopped at 4,141 ms; `finished false`, `status "did-not-finish"`, reason `:inline-verify-command-did-not-finish`; nothing written |
| 20,001-file workspace | refused **by name**, `:inline-verify-workspace-too-large`, in **154 ms** — a walk, not a copy |
| 300 MB single file | same refusal in **21 ms**; the byte ceiling works too |
| workspace-not-snapshot check (builder's own) | a command grepping the *pre-image* text fails, proving the venue is the patched overlay |
| missing-profile refusal | first words are `this workspace has no verification profile`; the sentence carries the exact JSON call; **both faces** carry it |

Two named caveats on the fence, neither blocking:

**(a) The fence is a working directory, not a jail.** `["cat","/etc/hostname"]` ran and returned
`anvil-server` on the receipt; `ls ..` listed the sibling overlay roots of other runs;
`cat /etc/passwd` returned root's line. Intent 153 says *"the overlay root as the only venue"* — the
implementation makes it the only **cwd**. This is defensible (the caller supplies the argv, exactly
as a repository-declared profile does, and the intent disclaims a network sandbox), but the intent
sentence and the tool description both read as containment to a naive reader. Say it plainly:
*the overlay is the working directory and the venue for the patched bytes; it is not a filesystem
sandbox, and the command runs with the server's own authority.* Note the second-order case while
you are there: overlays of concurrent calls are siblings in the same temp root and one call's
command can write into another's.

**(b) `destroy-process-tree!` does not reach a reparented child.** `.descendants()` is read at kill
time, so a `setsid`/double-forked grandchild escapes:

```
row: ... :status "did-not-finish", :elapsed_ms 4039.19, :output_tail "MARK-ORPHAN-3842909"
ORPHAN CHECK (sleep 400 still alive?):
3842911 sleep 400
```

The command itself was killed and the call refused correctly; a `sleep 400` outlived it. This is the
profile path's fence unchanged, so it satisfies 153's letter ("no weaker"), but "the process tree
destroyed on expiry" should read "the descendant tree observed at expiry."

**(c) minor:** `#` and `%` are not in the metacharacter set, so `"true #ignored"` runs as
`["true" "#ignored"]` rather than refusing (harmless — no shell — but a silent difference from what
a caller writing a shell comment expects). And a **nonexistent `cwd`** reports
`:inline-verify-command-did-not-finish` with `finished false`, conflating a launch failure with a
deadline expiry; the real cause survives only in `output_tail`
(`"Cannot run program \"true\" … No such file or directory"`). A distinct kind would be honest.

### Claim 2, harness-native patch (MCP-OP-ADMIT-155) — holds cleanly

| attack | result |
|---|---|
| V4A `*** Begin Patch`, zero digests, `mode commit` | `:admit-patch!`, `committed=true`, **`pre_image_binding "derived"`**, file written |
| `diff --git` + `index`/extended headers, zero digests | identical, `derived` |
| tree changed after the diff was cut (hook mutates the file mid-admission) | **`:source-hash-mismatch`**, *"The workspace changed while this admission was being verified: src/app/core.clj"*, `drifted` names expected and actual hashes, `source-unchanged=true`, nothing written |
| context does not match | `:patch-does-not-apply`, *"Hunk 0 of src/app/core.clj does not match the current file at line 5"* |
| `../escape.clj`, `/etc/evil.clj`, `src/../../outside.clj` | all three `:invalid-relative-source-path`; no file created outside |
| CRLF source | admitted; `\r\n` preserved byte-for-byte in the post-image |
| NUL bytes in a `.clj` string | admitted, form identity computed, `changed ["x"]` |

### Claim 3, failing verify output (MCP-OP-ADMIT-156) — holds for small outputs, fails for real ones

Small output, both faces:

```
error= "Snapshot verification failed (inline-verify-command-failed); nothing was written;
        the command exited 3; its last lines were:
        FAIL in (handle-tick-test) expected 1 actual 2"
failing_command_exit= 3
TEXT FACE contains assertion?  true
TEXT FACE contains exit code 3?  true
```

The failing assertion's own line is present on both faces, and the repository-declared runner's RED
side is witnessed by the builder's own test at
`test/clj_surgeon/admit_patch_round16_test.clj:198-221`. **Finding 3 above is the exception, and it
is the one that matters in the field.**

### Claim 4, propose mode (MCP-OP-ADMIT-157) — holds

- failing patch: `ok=true`, `operation :admit-patch-proposed`, `verify_ok=false`,
  `committed=false`, `mutation_attempted=false`, exit code and lines on the receipt, tree
  byte-identical before and after (the builder's witness hashes every file; my own runs confirm the
  workspace sources are unchanged in every propose case above);
- passing patch: same shape with `verify_ok=true`, still nothing written;
- **structural hazards still refuse in propose** — a duplicate top-level definition returns
  `ok=false, :duplicate-definition, "Top-level symbol handle-tick is defined 2 times for one
  reader"`, which is correct: that is a fact about the patch, not a check result;
- every verification-failed refusal proposes `propose` as the next hop, in **both** `preview` and
  `commit`:
  `A11e … mode preview → next_call.mode= "propose"`, `A11e … mode commit → next_call.mode= "propose"`,
  and the inline plan travels back whole as a sendable `verify` value.

Caveat: finding 6 (`verify_ok` true when nothing ran), and **no write path is reachable from
`propose` — except through finding 1**, which is the whole reason this review is NO-GO. The
propose-writes-nothing witness hashes the *workspace*; it cannot see a write the overlay makes
outside it.

### Claim 5, the builder's three self-caught defects — all three confirmed

**(a) The `["sh" "-c"]` mapping.** No file under `src/` builds one
(`grep -rn '"sh"' src/` returns one negative-assertion line in `worktree_lifecycle_io.clj`). The
structural scan `no-source-file-hands-a-command-string-to-a-shell`
(`test/clj_surgeon/core_discovery_test.clj:345`) is present **and non-vacuous** — its companion
`the-source-scan-is-not-vacuous` plants the exact shape and proves the scanner sees it. When I
reintroduced the forbidden mapping (sabotage S1), the scan went red and named the file and the
literal:

```
FAIL in (no-source-file-hands-a-command-string-to-a-shell) (core_discovery_test.clj:348)
  src/clj_surgeon/mcp_admit_tool.clj: argv literal hands a command string to a shell interpreter: ["sh" "-c" trimmed]
```

That is the claimed "+1", exactly.

**(b) The parser-ceiling split.** `clj-surgeon.admit-patch-round16-test` is in the JVM lane
(`test/clj_surgeon/mcp_test_runner.clj:5,72`) and **not** in bb's `test/run_all.clj`
(`grep -n "admit" test/run_all.clj` → no output). `make test-fast` (the bb lane) ran green, so bb
does not attempt to load it.

**(c) The false-green `cmd | tail; echo EXIT=$?`.** I cannot audit the builder's shell history, but I
can state the standard I held myself to: **every figure in this review comes from a process exit
code or a `clojure.test` summary map I read directly**, not from a pipeline whose `$?` is `tail`'s.
The `make` targets I ran report their own exit status, quoted below.

### Claim 7, the gates — mostly reproduce; one figure does not

| gate | claimed | measured on a fresh clone at `2ac33278` |
|---|---|---|
| `make test-fast` (bb) | 840 / 6919 / 0 | **840 / 6919 / 0** ✅ exact |
| mcp-operation oracle | pass | **pass** — `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| `make repository-hygiene` | green | **exit 0** — *"repository hygiene: no machine-local build cache is tracked at any depth"* |
| `make admit-transaction-recovery-battery` | 3/3 | **3/3**, `verdict :passed`, `kinds #{:transaction-recovery-required}`, arms n=8/32/64 |
| admit namespaces | 197 / 4570 / 0 | **175 / 4425 / 0** ❌ — 0 failures, but the figure does not reproduce |
| `make mcp-test` | 926 / 15323 / 0 | **926 / 15323 / 0** ✅ exact, `0 failures, 0 errors`, and `grep -cE "^(FAIL|ERROR) in"` on the log returns **0**; the lane also ran the hygiene self-test and the kernel warning check (`2 namespace(s), 0 warning(s)`) green |

The admit-namespace figure was measured twice, with and without the battery receipt present (the one
`precondition-skipped` test), and both give 175/4425/0. There are only two admit test namespaces in
the tree (`admit_patch_test.clj`, `admit_patch_round16_test.clj`). Zero failures either way, and the
full lane that contains them reproduces to the assertion — so this is a **figure-accuracy** finding,
not a correctness one. But the house standard is that a reported number comes from a run somebody
watched, and this one does not reproduce in either precondition state.

**Also exercised and clean — `profile_from_receipt` (the second half of MCP-OP-ADMIT-153), which
neither the brief nor the builder's own leaf attacks in anger:** string rows, map rows spelled
`:command`, and map rows spelled `"argv"` all resolve and run (`P1`–`P3`); nine commands is refused
by name (*"may name at most 8 commands; this call sent 9"*), an empty array is refused by name, and a
non-zero exit **stops the sequence with every skipped command still named** rather than omitted:

```
P8 rows= [{:command "true", :exit 0, :status "passed"}
          {:command ["sh" "-c" "exit 4"], :exit 4, :status "failed"}
          {:command "true", :status "not-run-after-failure"}
          {:command "true", :status "not-run-after-failure"}]
```

That is the intent's sentence implemented exactly. Two nits: a receipt row that names no command at
all (`{:notacommand 1}`) is refused with the *shell-metacharacter* message, which has nothing to do
with the caller's actual mistake; and a call sending both `commands` and `profile_from_receipt`
silently honours `commands` and drops the other without a word on the receipt.

### Claim, sabotage — every class is pinned and goes loudly red

Five sabotages of my own construction, each on its own clone of `2ac33278`, each reverting one
intent's mechanism, running the two admit namespaces:

| # | sabotage | failures |
|---|---|---|
| S1 | `inline-command-argv` string form → the forbidden `["sh" "-c" line]` | **16** in the admit lane **+ 1** in the structural shell scan |
| S2 | the missing-profile branch of `verification-incomplete-error` disabled | **2** |
| S3 | `base-binding` `"derived"` → `"unbound"` | **4** |
| S4 | `verification-failure-detail` returns `{}` | **7** |
| S5 | `propose` mode removed from the receipt branch | **10** |

The claimed sequence was 21+1 / 4 / 5 / 7 / 2. My reverts are not the builder's reverts, so the
counts are not expected to match line for line — position 4 matches exactly (7), and the "+1"
matches exactly. **What matters holds: no sabotage stays green, every one names its own class, and
the structural fence witness fires independently of the admit lane.**

### Requested for the record: G3's refused calls — UNVERIFIED from this seat

The coordinator asked how many admit calls G3 was refused before the two that committed, with each
refusal's first words quoted. **I cannot answer that, and I will not estimate it.** I have no G3
rollout in reach: `docs/observations/` on the branch and on `origin/MCP/main` carries the G/GN and
G2/GN2 records but no G3 arm; `/home/forge/tmp/sol/` holds no G3 log; and the arm's server is on a
port this seat is forbidden to contact. The honest state is `:unverified`.

Two artifacts would settle it without guessing, and both already exist by construction. The gate
emits one telemetry event per call (`telemetry-event`, `mcp_admit_tool.clj:2554`, carrying
`request_shape` with `patch_sha256`, `mode` and `verify`, alongside the receipt's `error-type`), so
a count of refusals by kind and their `error` strings is a filter over that stream. Failing that,
the arm's own transcript names every call. Whoever holds either can fill this in; the two findings
above stand on their own reproductions and do not depend on it.

### Claim 8, merge-tree

```
$ git rev-parse origin/MCP/main
8be766c5432fc076fcc44bc963a80eb1ab192004
$ git merge-tree --write-tree --name-only origin/MCP/main 2ac33278
7c436244538e1ddb0dc7032603a19cedd2f2981e
EXIT=0
```

**Clean, no conflicts, against the current tip `8be766c5`** (two commits newer than the `07c5a167`
the review clone knew; both are `docs/observations` additions only, and the branch does not touch
those files' changed regions). Merge base `717c25ff`.

---

# 8. Item 6 — the overlay copy cost, and my ruling on hardlink/reflink

Measured on this box (ext4, load 11–35 throughout — treat these as upper bounds):

| workspace | files | wall of a full inline-verify admit |
|---|---:|---:|
| baseline (2 files) | 2 | 151 ms |
| synthetic | 2,002 | 237 ms |
| synthetic | 10,002 | 910 ms |
| 20,001 files (ceiling) | — | **154 ms**, refused by name |
| 300 MB single file (byte ceiling) | — | **21 ms**, refused by name |

And the honest field number — `overlay-snapshot!` on the clj-surgeon repository itself, three runs:

```
run 0: ok=true files=2642 bytes=39336432 wall_ms=219
run 1: ok=true files=2642 bytes=39336432 wall_ms=214
run 2: ok=true files=2642 bytes=39336432 wall_ms=223
```

**2,642 files / 39 MB in ~220 ms, stable.** Against a verify command that runs a test suite — seconds
to minutes — the copy is noise. It is roughly 76 µs/file, so even at the 20,000-file ceiling the copy
is ~1.5 s.

**Ruling: keep the byte copy. Do not switch to hardlinks. Reflink only as a best-effort fast path,
and only after finding 1 is fixed.**

- **Hardlinks are wrong here, and dangerously so.** The overlay exists so a command can run against
  the *patched* bytes without touching the workspace. A hardlink farm shares inodes, so any command
  that writes **in place** — `>>`, `sed -i` without a rename, a compiler emitting into a source tree,
  a test that rewrites a fixture — silently mutates the real workspace through the link. That
  converts the gate's central safety property into a coin flip decided by how each tool happens to
  write. Finding 1 is proof this code base is already not careful about path aliasing; adding
  deliberate aliasing on top would be the wrong direction.
- **Reflink (`--reflink=auto`, COW) is safe and nearly free — where the filesystem supports it.** It
  is btrfs/XFS-with-reflink/APFS; **this box is ext4, where it is unavailable**, so it can only ever
  be an opportunistic fast path with a full-copy fallback, i.e. a second code path guarding a cost
  that measures at 220 ms. That is not worth its own failure modes today.
- **The ceiling's *shape* is right and I would not change it.** It refuses after a **walk**, not a
  copy — 154 ms and 21 ms above — so an oversized tree costs a stat pass, and both dimensions
  (files and bytes) are enforced with one named refusal that offers two remedies. That is the
  correct design.
- **The ceiling's *reach* is the real adoption risk, and it is not the copy cost.** The overlay
  deliberately excludes only `.git`, for a stated and correct reason. A repository with
  `node_modules` or a populated `target/` blows 20,000 files without being a large project; the
  field repo that motivated this round is a JavaScript/Clojure tree. My prediction is that the
  ceiling, not the wall, is what agents will meet there — and its remedy ("run the commands
  yourself, or declare a focused-test profile") sends them straight back to the behaviour of arm G.
  Worth measuring before the next mandated arm: count files in the target repo first.

---

# 9. Verdict

**NO-GO**, on three findings that share one six-line loop and one field. The round's new *contract*
is good work and I want to say so plainly: inline verify refuses ten shell shapes by name and admits
the explicit `["sh","-c"]` array the intent promised, with the structural src-scan proven
non-vacuous; `profile_from_receipt` resolves all three row spellings, caps at eight commands, and
names every command a failure skipped; the harness's own V4A and `diff --git` grammars are admitted
with **zero** caller digests as `pre_image_binding: derived`, while a tree that moves mid-admission
still refuses `source-hash-mismatch` naming the file and the two hashes, and three spellings of an
out-of-workspace path are refused; the missing-profile refusal begins with the required words and
carries a real call on both faces; `propose` publishes a verdict instead of a refusal, writes
nothing, and still refuses structural hazards; the overlay ceiling refuses **by name after a walk**
in 154 ms and 21 ms; `make mcp-test` reproduces to the assertion at **926/15323/0**, `make
test-fast` at **840/6919/0**, with the oracle, hygiene and a 3/3 recovery battery green; all five
sabotages go loudly red (16+1 / 2 / 4 / 7 / 10); and merge-tree is clean against the current
`8be766c5`.

**What blocks it is `overlay-snapshot!`, which is new on this branch and is the foundation every one
of those wins now stands on.** It follows symbolic links and derives each copy's destination from
the link's canonical target, so an ordinary symlink to an outside file makes the gate write outside
the overlay root and truncate that file to zero — under `preview` and `propose`, on a receipt
publishing `mutation_attempted=false`. It does not preserve file modes, so a repository whose verify
command is `./bin/test` gets a guaranteed false refusal against a tree that exists nowhere, reported
with a timeout's status word; that one already cost a live arm a product change. And `output_tail`
publishes the last 40 lines of the first 12 KB with no truncation flag and a mid-word cut, which
makes MCP-OP-ADMIT-156 — carried as implemented — false for every real test suite, in the one
respect the round was written to fix. The first is uninstructed data destruction outside the
declared blast radius reported as a no-op; the second and third are the gate lying about what it
verified. None can be a follow-up.

The good news is the repair is small and concentrated: findings 1 and 2 are the same edit
(`Files/copy` with `COPY_ATTRIBUTES` + `NOFOLLOW_LINKS`, symlinks recreated as symlinks, and a typed
refusal for any destination normalizing outside the root), and finding 3 is a seek-to-tail plus
carrying `output_truncated`/`output_bytes` onto the row. The remaining four — the
`unsupported-patch-target` `next_call` that repeats the refused call verbatim, the missing-profile
`next_call` placeholder the gate itself refuses, `verify_ok: true` on a call that verified nothing,
and the 175/4425 vs 197/4570 figure — are **non-blocking** and should ride the same repair round,
along with the three honest-wording items (the fence is a working directory and not a jail; the
process tree is the descendant set observed at expiry; a launch failure is not a deadline). Fix the
three blockers with witnesses in the JVM lane, re-run the two admit namespaces plus
`clj-surgeon.core-discovery-test`, and I expect this lane to be a clean GO — the contract underneath
it is sound.

---

*Method note, for the record.* Every figure above came from a process exit code or a
`clojure.test` summary map read directly; no figure was taken from a pipeline whose `$?` belongs to
`tail`. Load on this box ran 11–35 for the whole review (other seats), so the wall figures in
section 8 are upper bounds; the three-run stability of the 220 ms real-repo overlay measurement is
the reason I trust its order of magnitude. Fixtures lived only under `/var/tmp/forge/gate17-opus-fx`
and are removed. The review clone was never checked out, staged, committed, or pushed.
