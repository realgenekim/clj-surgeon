## NO-GO

# Round-nineteen review — clj-surgeon `bridge/census-verb` at 563c300d

Independent reviewer: Opus, taking this brief after Sol's content filter refused it.
Same brief, substituted paths. Reviewed read-only in `/home/forge/tmp/sol/census18-wt`.
No commit, push, stash, index write, or source edit was made anywhere. Fixtures were
confined to `/var/tmp/forge/census19-review-fx/opus` and removed at the end; every mode I
changed was restored. No listed port was contacted. I signalled no process I did not
start. The only remote operation was one `git fetch origin` in the clone, required for
the `origin/MCP/main` dry-run at the end; it updates remote-tracking refs only and left
the worktree clean, and it is disclosed here rather than omitted.

**Two BLOCKING findings.** Both are the round-eighteen blocking findings surviving one
frame over — not new territory, the same two promises, reached through a door the
round-nineteen witnesses do not open.

Entry proof:

```text
$ git rev-parse HEAD && git status --porcelain=v1 && git log --oneline 3b7904a..563c300d
563c300d50cd6e93fd1d5cc93a12b304a6a5681f

563c300d docs(census): CENSUS-014/018 state round nineteen's four rules
9ac92f69 green: both dispatch refusals leave through the launcher's one bounded exit
38af8f41 red: declaring the launcher's set found a THIRD unbounded launcher refusal
72587ecb green: a refusal's PROSE names the workspace root once, by its one name (Sol round-18 item 4)
25eb0993 red: "no absolute root" held at the three sites the last reviewer listed (Sol round-18 item 4)
ad212510 green: all ten shapes agree, and the enumeration prints its own derivation (Sol round-18 item 3)
634a3aba red: "ten shapes, all agreeing" was a sentence, not a computed fact (Sol round-18 item 3)
1acfcc04 green: no census READS a source whose real path leaves the workspace (Sol round-18 item 2)
ca739828 red: a source whose REAL path leaves the workspace is READ at the CLI (Sol round-18 item 2)
a16b9e3f green: the LAUNCHER has ONE bounded exit, the op's own (Sol round-18 item 1)
c56ee1d4 red: the real LAUNCHER publishes unbounded refusals (Sol round-18 item 1)
```

The blank line after the hash is the verbatim empty `git status --porcelain=v1`.

**Procedural disclosure, recorded because a reviewer's workspace is evidence.** The
fixture directory this brief assigned me, `/var/tmp/forge/census19-review-fx`, was
ALREADY POPULATED when I arrived, timestamped 05:18–05:20 on 2026-09-04, minutes before
this review began, with outputs named for exactly the item-1 attacks this brief asks me
to run — `bb-bad-help.out`, `bb-unknown-op.out`, `jvm-malformed-edn.out`,
`jvm-after-dispatch.out`, `launcher-after-dispatch/` — one of them a classpath failure
(`Could not locate clj_surgeon/core__init.class`). I did not read them as results, I
removed the directory, and every receipt below is from my own drives. Recording it
because a review that inherits someone else's fixtures is not independent, and the next
reviewer should be given a directory nobody has written to.

---

## 1. BLOCKING — the launcher's bound is still not total: a NON-STRING argument value is unbounded at both real launchers

`src/clj_surgeon/relation_census.clj:1179` (`bound-refusal`), `:1195` (the postwalk),
`src/clj_surgeon/core.clj:2210` (`parse-val`), `src/clj_surgeon/core.clj:2091`
(`print-launcher-refusal!`), witness `test/clj_surgeon/mcp_relation_census_test.clj:7187`.

`print-launcher-refusal!` really is the one printing site, and it really does call
`census/bound-refusal`. But `bound-refusal` bounds only strings:

```clojure
;; src/clj_surgeon/relation_census.clj:1193-1196
(walk/postwalk
  #(if (string? %) (bound-refusal-text %) %)
  v)
```

and `core/parse-val` (core.clj:2210) turns a CLI value beginning with `:` into a
**keyword** and one beginning with `[` into whatever `read-string` makes of it. So the
caller controls a non-string that rides straight through the bound. `parse-args` then
puts those values, verbatim, into `:values` of the `:duplicate-argument` refusal
(core.clj:2262-2265) — a name **declared in `census/launcher-refusal-types`**.

Driven through the REAL launchers as subprocesses, not a fn call, with a 10,001-character
argument, the same length the previous two rounds drove:

```text
$ BIG=$(python3 -c "print('a'*10001)")
$ java -cp "$CP" clojure.main -m clj-surgeon.core :op :relation-census :doors ":$BIG" :doors ":$BIG" > a1-jvm-dup-keyword.out 2>&1; echo "EXIT=$?"
EXIT=1
$ bb -cp "$PWD/src" -m clj-surgeon.core :op :relation-census :doors ":$BIG" :doors ":$BIG" > a1-bb-dup-keyword.out 2>&1; echo "EXIT=$?"
EXIT=1
$ for f in a1-jvm-dup-keyword a1-bb-dup-keyword; do b=$(wc -c < $f.out); m=$(rg -o 'a+' $f.out | awk '{if(length>m)m=length} END{print m+0}'); k=$(rg -o 'truncated' $f.out | wc -l); t=$(rg -o ':error-type :[a-z-]+' $f.out | head -1); echo "$f BYTES=$b MAX_A_RUN=$m MARKERS=$k $t"; done
a1-jvm-dup-keyword BYTES=20287 MAX_A_RUN=10001 MARKERS=0 :error-type :duplicate-argument
a1-bb-dup-keyword  BYTES=20226 MAX_A_RUN=10001 MARKERS=0 :error-type :duplicate-argument
```

Compare the receipt round eighteen reported as the defect, and this round reports as
repaired: `duplicate EXIT=1 BYTES=20228 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument`.
**20,228 → 20,287 bytes.** Same launcher, same declared type, same 10,001-character run,
same zero truncation markers, one colon of difference in the caller's argument. Verbatim
head and tail of the JVM receipt:

```text
{:error-type :duplicate-argument,
 :argument ":doors",
 :occurrences 2,
 :values
 [:aaaaaaaaaaaaaaaa … 10,001 characters … aaaaaaaa],
 :error
 ":doors was given 2 times; every clj-surgeon argument is given at most once, and a repeated one would be silently dropped"}
```

The `:error` string is bounded. The `:values` keywords are not.

**The same root cause is live at the OP's own entrance exit, not only the launcher's.**
`:doors-not-a-string` is declared in `census/cli-refusal-types` and is therefore inside
the round-eighteen enumeration that this round left in place:

```text
$ java -cp "$CP" clojure.main -m clj-surgeon.core :op :relation-census :dir . :doors "[:$BIG]"
kw-door-vector EXIT=1 BYTES=11683 MAX_A_RUN=10001 MARKERS=1 :error-type :doors-not-a-string
$ bb -cp "$PWD/src" -m clj-surgeon.core :op :relation-census :dir . :doors "[:$BIG]"
bb-kw-door-vector BYTES=11622 MAX_A_RUN=10001
$ java -cp "$CP" clojure.main -m clj-surgeon.core :op :relation-census :dir . :doors "[$BIG]"   # symbol, not keyword
sym-door-vector BYTES=11682 MAX_A_RUN=10001 :error-type :doors-not-a-string
```

Field by field, so the claim is exact rather than impressionistic:

```text
:ok                    len=     11 max_a_run=1     truncated_marker=False
:error-type            len=     34 max_a_run=1     truncated_marker=False
:error                 len=   1068 max_a_run=963   truncated_marker=True
:anchor                len=    140 max_a_run=1     truncated_marker=False
:value                 len=  10015 max_a_run=10001 truncated_marker=False
:next-command          len=    160 max_a_run=1     truncated_marker=False
:next-command-argv     len=    192 max_a_run=1     truncated_marker=False
```

`:error` was bounded correctly. `:value` — a 10,015-character field with no marker — was
not, because it holds a keyword.

**Why the round-nineteen witness is green over it.** `launcher-drives`
(test:7143-7161) builds every hostile argument from `(hostile-argument)`, which is
`(apply str (repeat 10001 \a))` — a **string**, always. And the bound assertion itself
(test:7187-7188) is

```clojure
(let [longest (->> (tree-seq coll? seq parsed)
                   (filter string?)
                   (map count)
                   (reduce max 0))]
```

so even had a keyword been driven, the length assertion could not see it. The `re-find
#"a{10001}"` assertion on the raw bytes two lines later WOULD have caught it — the drive
simply never produces one. This is the round-eighteen lesson repeating exactly: an
enumeration that describes a subset of what an entrance emits is green over the rest.
Round eighteen's subset was the launcher's names; round nineteen's is the launcher's
*types* — the bound is enforced on strings and the enumeration drives only strings, so
the two agree with each other and neither is checked against what the caller can send.

Ruling: **BLOCKING**, at the same severity and for the same reason Sol ruled round
eighteen's item 1 blocking. The CENSUS-014 launcher clause added this round says the
bound "shall be applied to every refusal the public CLI entrance can PRINT." A refusal
the public CLI entrance prints at 20,287 bytes with a 10,001-character run and no marker
falsifies that sentence. "It is a keyword, not a string" is the same shape of exemption
as "it belongs to no op," and the reviewer's ruling on that one applies unchanged.

The other launcher attacks this brief names all hold. Recorded so the finding is scoped
and not overstated:

```text
after-dispatch-config EXIT=1 BYTES=289  MAX_A_RUN=1    MARKERS=0 :error-type :invalid-arguments
up-huge               EXIT=1 BYTES=123  MAX_A_RUN=1    MARKERS=0 :error-type :invalid-arguments
recover-huge          EXIT=1 BYTES=123  MAX_A_RUN=1    MARKERS=0 :error-type :invalid-arguments
reportfailure-huge    EXIT=1 BYTES=1164 MAX_A_RUN=1024 MARKERS=1 :error-type :invalid-arguments
version-extra         EXIT=1 BYTES=528  MAX_A_RUN=1    MARKERS=0 :error-type :unknown-operation
odd-args              EXIT=1 BYTES=139  MAX_A_RUN=1    MARKERS=0 :error-type :invalid-arguments
kw-unknown-flag       EXIT=1 BYTES=1566 MAX_A_RUN=986  MARKERS=1 :error-type :unknown-arguments
kw-pool               EXIT=1 BYTES=1547 MAX_A_RUN=973  MARKERS=1 :error-type :invalid-pool-size
```

`after-dispatch-config` is the brief's "refusal raised in the launcher AFTER dispatch
begins but before the op's exit" — a malformed `.clj-surgeon.edn` with a 10,001-character
alias, loaded by `run-op`'s `forms/init-from-file!`. It throws, `-main`'s catch bounds it,
289 bytes. Correct.

And the three field receipts the brief claims reproduce exactly at the JVM launcher:

```text
duplicate  EXIT=1 BYTES=2340 MAX_A_RUN=1024 MARKERS=2 :error-type :duplicate-argument
invalid    EXIT=1 BYTES=1103 MAX_A_RUN=1007 MARKERS=1 :error-type :invalid-arguments
unknown-op EXIT=1 BYTES=1136 MAX_A_RUN=1012 MARKERS=1 :error-type :unknown-operation
```

(20228→2340, 10064→1103, 10468→1136, all with markers, as claimed. At babashka the
`invalid` receipt is 1196 rather than 1103 — a message-text difference, still bounded.)

**Scope, stated so the finding is not read wider than it is: this is CLI-only.** At the
MCP entrance every leaf arrives as a JSON scalar, strings are bounded by the same
`bound-refusal`, and Jackson's own `StreamReadConstraints` refuses a number literal longer
than 1,000 characters before the tool ever sees it (I drove a 10,001-digit `doors`,
`pool_size`, `files`, `workspace_root` and an unknown field; the parser refuses them all).
The hole exists because the CLI, uniquely, has a `parse-val` that manufactures unbounded
non-strings out of operator text.

**The fix is one predicate, and it should be at `bound-refusal`, not at the drive.** A
bound that asks `string?` is a bound on the type the author happened to picture. Bound
the RENDERED length of every leaf a refusal carries — the thing a caller reads is
`pr-str` output, not a Java type — and add a keyword and a symbol drive to
`launcher-drives` so the ratchet can see the class rather than the instance.

---

## 2. BLOCKING — the containment fence FAILS OPEN, and the CLI then reads and censuses a source outside every tree the request named

`src/clj_surgeon/core.clj:520-553` (`census-workspace`, `(catch Exception _ nil)` at
`:553`), `src/clj_surgeon/core.clj:556-574` (`escaping-source`, `(when workspace` at
`:569`), `src/clj_surgeon/core.clj:723` (the one `workspace` binding).

`census-workspace` swallows every exception to `nil`. `escaping-source` opens with `(when
workspace …)`, so a `nil` workspace answers "not escaping" for every path. The fence is
not merely absent in that case — it affirmatively reports containment it never tested.

An unresolvable `:dir` is enough to reach it, and an unresolvable `:dir` is an ordinary
operator typo that this op refuses nowhere on the `:file` path:

```text
$ java -cp "$CP" clojure.main probe2b.clj
=== ROW 8 FULL RECEIPT: :dir <nonexistent> :file <symlink escaping the workspace> ===
{:counts {:door 0, :set 0, :guarded 0, :raw 0, :unknown 0},
 :pool-size 1,
 :unknown [],
 :sites 0,
 :next-action "none",
 :guarded [],
 :files-scanned 1,
 :by-file
 {"../ws/src/app/escape.clj"
  {:arms 1, :outside-arms 0, :counts {…}, :sites 0}},
 :census-version 1,
 :ok true,
 :files 1,
 :read-complete true,
 :arms 1}
=== CONTROL: the identical :file, with :dir = the real workspace ===
{:ok false, :error-type :file-outside-workspace, :cause :outside-project}
=== census-workspace values ===
nonexistent-dir -> nil
real ws         -> #object[sun.nio.fs.UnixPath "…/fx/ws"]
```

The link is `fx/ws/src/app/escape.clj -> ../../../outside/secret.clj`. With a resolvable
`:dir` it is refused. With `:dir` naming a directory that does not exist, the same link is
**READ**: one file scanned, one `defmethod fold-event` arm counted out of a file outside
the workspace, `:ok true`, `:read-complete true` — a completeness claim over a tree the
request never named.

It does not need a symlink at all. An absolute outside path with an unresolvable `:dir`
is read the same way, and the receipt publishes an escaping relative path for it:

```text
=== :dir <nonexistent> :file /var/tmp/forge/census19-review-fx/opus/fx/outside/secret.clj ===
{:ok true, :files-scanned 1, :arms 1}
:by-file {"../outside/secret.clj" {:arms 1, :outside-arms 0, …}}
```

The MCP entrance refuses the identical request, so this is also a parity failure on the
shape the two entrances were just made to agree about:

```text
=== TOOL, nonexistent project-root, files = the escaping link ===
{:ok false, :error_type "invalid-workspace-root", :error "workspace_root must be an existing directory"}
```

Ruling: **BLOCKING**, under this review's explicit rule ("any read outside it is
BLOCKING") and under CENSUS-018's own round-nineteen sentence: *"NO CENSUS READS A SOURCE
WHOSE REAL PATH LEAVES THE WORKSPACE, at either entrance, however the source was
reached."* The source was reached; it was read; the receipt says `:ok true`. This is
worse than round eighteen's version of the same defect, because round eighteen's was
DECLARED — the rule said the CLI's `:file` reads what the operator typed, and a reader
could at least know. This one is a silent fail-open in a fence whose EARS text now
promises the opposite, and its symptom is a green receipt, which is the failure class
that terminates investigation.

**The fix is a fail-CLOSED default.** `escaping-source` returning `nil` must mean "I
tested and it is inside," never "I could not test." A workspace that does not resolve is
a typed refusal about the workspace — which is exactly what the tool already does — not a
licence to read. Concretely: `census-workspace` should return a typed failure rather than
`nil`, and `census-source-refusal` should refuse when it is handed one, before existence
is asked.

**What this finding is NOT.** The rest of item 2 is real and reproduces at both entrances.
Every ordinary escape route the brief names is closed:

```text
1-CLI-dir+file-escape   {:ok false, :type :file-outside-workspace, :cause :outside-project, :read-secret? false}
1-TOOL-escape           {:ok false, :type "unreadable-source-path", :cause "outside-project", :read-secret? false}
2-CLI-file-only-escape  {:ok false, :type :file-outside-workspace, :cause :outside-project, :read-secret? false}
3-CLI-dir+chain         {:ok false, :type :file-outside-workspace, :cause :outside-project, :read-secret? false}   ; hop1 -> inside/hop2 -> outside
3-TOOL-chain            {:ok false, :type "unreadable-source-path", :cause "outside-project", :read-secret? false}
3-CLI-file-only-chain   {:ok false, :type :file-outside-workspace, :cause :outside-project, :read-secret? false}
4-CLI-inside-link       {:ok true,  :files 1}      ; the CONTROL: an inside link is FOLLOWED
4-TOOL-inside-link      {:ok true,  :files 1}
6-CLI-dir+linkdir       {:ok false, :type :file-outside-workspace, :cause :outside-project}   ; via a DIRECTORY link that leaves
6-TOOL-linkdir          {:ok false, :type "unreadable-source-path", :cause "outside-project"}
9-CLI-dir+abs-outside   {:ok false, :type :file-outside-workspace, :cause :outside-project}
10-CLI-toroot           {:ok false, :type :file-not-a-regular-file, :cause :not-a-regular-file} ; link -> the workspace root itself
8c-CLI-dir-is-a-file    {:ok false, :type :file-outside-workspace}
```

The two-hop chain is refused, which is the case a one-hop containment test passes. The
refusal names the link as the request spelled it and never the target, at both entrances —
I checked the whole refusal, not just `:error`:

```text
:error  "…/fx/ws/src/app/escape.clj resolves outside the workspace this census is over, so reading it would answer about a tree the request did not name"
:remedy "…/fx/ws/src/app/escape.clj resolves outside the workspace … name the tree that source really lives in with :dir, or name a source whose real path stays inside the tree you are censusing."
NAMES-TARGET? false
TOOL :error  "Source symlink resolves outside the configured project root (src/app/escape.clj)"
NAMES-TARGET? false
```

And the EARS text does state the supersession explicitly rather than dropping the removed
witness silently — the brief's check on that passes. Verbatim from the round-nineteen
CENSUS-018 line:

> "This REPLACES the earlier "naming is not walking" exemption, under which the CLI's
> `:file` read what the operator typed because it had no project root: that rule was
> stated, witnessed, and AUTHORISED a census to publish the bytes of a file outside the
> tree it was censusing, and a "no path escapes" claim false at one entrance is false."

**Two rulings the brief asks for.**

*The hard link.* `ws/src/app/hard.clj` is a hard link to `outside/secret.clj`; both
entrances read it (`{:ok true, :files 1}`). **This is not a hole and should not be
closed.** A hard link has no target path — the inode has two equally real names, and one
of them is inside the workspace. `toRealPath` is correct to call it inside, and any test
that called it outside would have to be an inode-identity test, which would then refuse
ordinary deduplicated trees. What CENSUS-018 owes here is one sentence saying so, because
"REAL path" is doing work a reader cannot check: the rule is *path* containment, and a
hard link is inside by construction.

*A `:file` named by an absolute path inside the workspace.* Read, correctly, with and
without `:dir` (`{:ok true, :files-scanned 1, :arms 1}` both ways). No finding.

*One scoping note, non-blocking.* With no `:dir` at all, `census-workspace` resolves every
link ABOVE the final component, so `:file ws/src/linkdir/inner.clj` — reached through a
directory link that leaves the workspace — is READ (`6-CLI-file-only-linkdir {:ok true,
:files 1}`). That is the declared rule, stated in the EARS and in the docstring, and it is
defensible: a request naming one source with no tree has no tree for the target to be
outside of. I flag it only because the sentence "no census reads a source whose real path
leaves the workspace" reads, to anyone who has not read `census-workspace`, as stronger
than what is implemented. The workspace definition is doing the load-bearing work and
should be quoted next to the rule wherever the rule is quoted.

---

## 3. VERIFIED — the parity enumeration is a computed fact with a printed derivation, and the extension set is genuinely shared

`test/clj_surgeon/mcp_relation_census_test.clj:6669`, `:6704-6711` (the print),
`:6721-6733` (the empty-disagreement assertion), `src/clj_surgeon/mcp_paths.clj:9`.

The witness prints one line per shape with the agreement computed per row, asserts the
printed set is the compared set, asserts there are ten of them, and asserts the
disagreeing set is `#{}`. No exemption table survives. From the battery's own stdout:

```text
PARITY-ENUMERATION:
  missing              expected not-found                    tool "not-found"                  cli :not-found                   agree true
  denied-file          expected permission-denied            tool "permission-denied"          cli :permission-denied           agree true
  denied-parent        expected parent-denied                tool "parent-denied"              cli :parent-denied               agree true
  dir-named-clj        expected not-a-regular-file           tool "not-a-regular-file"         cli :not-a-regular-file          agree true
  fifo                 expected not-a-regular-file           tool "not-a-regular-file"         cli :not-a-regular-file          agree true
  symlink-loop         expected not-found                    tool "not-found"                  cli :not-found                   agree true
  name-too-long        expected not-found                    tool "not-found"                  cli :not-found                   agree true
  enotdir-component    expected not-found                    tool "not-found"                  cli :not-found                   agree true
  escape               expected outside-project              tool "outside-project"            cli :outside-project             agree true
  wrong-extension      expected not-a-relative-source-path   tool "not-a-relative-source-path" cli :not-a-relative-source-path  agree true
```

The two rows round eighteen carried as declared divergences now agree, and they agree by
being closed rather than re-declared. `mcp-paths/supported-source-extensions` is the same
object as `census/named-source-extensions`, not a copy that agrees:

```text
SHARED-SET-IDENTICAL? true #{"cljc" "cljs" "clj" "edn"}
```

**The eleventh-shape attack the brief asks for — three shapes disagree, none blocking.**
I drove seven shapes the enumeration does not contain, at both entrances, the same way
the witness drives (tool: `{:project-root ws} {:files [p]}`; CLI: `{:file (str ws "/" p)}`):

```text
src/app/arm.clj            tool :READ                        cli :READ                        agree true
src/app/mod.cljc           tool :READ                        cli :READ                        agree true
src/app/data.edn           tool :READ                        cli :READ                        agree true
src/app/noext              tool not-a-relative-source-path   cli :not-a-relative-source-path  agree true
src/app/UP.CLJ             tool not-a-relative-source-path   cli :not-a-relative-source-path  agree true
src/app/a.clj.txt          tool not-a-relative-source-path   cli :not-a-relative-source-path  agree true
src/app/dirnamed.cljc      tool not-a-regular-file           cli :not-a-regular-file          agree true
src/app/missing.cljc       tool not-found                    cli :not-found                   agree true
src/app/../app/arm.clj     tool not-a-relative-source-path   cli :READ                        agree FALSE
./src/app/arm.clj          tool not-a-relative-source-path   cli :READ                        agree FALSE
src/app//arm.clj           tool not-a-relative-source-path   cli :READ                        agree FALSE
```

`.cljc`, a directory named `x.cljc`, a file with no extension, an uppercase extension and
a double extension all agree — the shared set and the shared vocabulary do their job. The
three that disagree are PATH FORMS, not source kinds: the tool's `relative-source-path?`
forbids `.`, `..` and empty segments because a relative path with them can escape a root,
and the CLI takes an absolute operator path where they are just normalisation. None
authorises a read outside the workspace (each resolves to a file inside it), so this is
non-blocking. But it IS the class the brief names — a shape the enumeration does not
contain — and it should be either declared in the EARS as a path-form divergence with its
reason, or covered by three more rows.

*Also recorded:* `src/clj_surgeon/mcp_contract.clj:68` keeps a THIRD, private copy of the
set — `(def ^:private supported-source-extensions #{"clj" "cljs" "cljc" "edn"})` — the
exact "two sets that agree until they do not" that `mcp_paths.clj:9`'s own docstring was
written to prevent. Non-blocking today because the values match. It should read the shared
one.

---

## 4. The absolute root is out of the CLI's prose — and still in the MCP entrance's, on a shape neither round-nineteen witness can see

`src/clj_surgeon/mcp_relation_census.clj:1264-1271`; witnesses
`test/clj_surgeon/mcp_relation_census_test.clj:7302-7305` and `:7355-7378`.

The CLI half of the repair is real. `census/directory-repair-phrase` exists, is used by
both entrances, and the near-miss the brief names is closed:

```text
REPAIR-PHRASE-root: "make <workspace_root> itself readable"
REPAIR-PHRASE-sub : "make src/app readable under <workspace_root>"

CLI  remedy (chmod 000 ROOT): "<workspace_root> came from the workspace walk … make <workspace_root> itself readable, remove it, or name the sources to census with :file. …"
TOOL remedy (chmod 000 ROOT): "<workspace_root> came from the workspace walk … make <workspace_root> itself readable, remove it, or name the sources to census with files. …"
```

But driving every refusal shape I could reach at both entrances and scanning all prose
against the enumerated exemptions found one leak, at the tool:

```text
CLI-denied-root    ok=false type=:file-not-readable      ABS-ROOT-IN-PROSE=none
TOOL-denied-root   ok=false type=unreadable-source-path  ABS-ROOT-IN-PROSE=none
CLI-denied-subdir  ok=false type=:file-not-readable      ABS-ROOT-IN-PROSE=none
TOOL-denied-subdir ok=false type=unreadable-source-path  ABS-ROOT-IN-PROSE=none
CLI-no-arms        ok=false type=:no-fold-arms-found     ABS-ROOT-IN-PROSE=none
TOOL-no-arms       ok=false type=no-fold-arms-found      ABS-ROOT-IN-PROSE=["Nothing under /var/tmp/forge/census19-review-fx/opus/p4/empty defines defmethod fold-event arms (0 file(s) scanned), so no narrower call can be computed: point workspace_root at a directory whose sour…"]
CLI-too-large      ok=false type=:source-too-large       ABS-ROOT-IN-PROSE=none
TOOL-too-large     ok=false type=source-too-large        ABS-ROOT-IN-PROSE=none
CLI-missing        ok=false type=:file-not-found         ABS-ROOT-IN-PROSE=none
TOOL-missing       ok=false type=unreadable-source-path  ABS-ROOT-IN-PROSE=none
CLI-unknown-door   ok=false type=:unknown-door-symbol    ABS-ROOT-IN-PROSE=none
TOOL-unknown-door  ok=false type=unknown-door-symbol     ABS-ROOT-IN-PROSE=none
```

The site is `mcp_relation_census.clj:1264-1271`:

```clojure
(empty? named)
(str "Nothing under " canonical
     " defines defmethod fold-event arms ("
     scanned-count " file(s) scanned), so no "
     "narrower call can be computed: point "
     "workspace_root at a directory whose "
     "sources define fold arms, or name the "
     "sources to census with files.")
```

The CLI twin at `core.clj:1294-1300` does it correctly, with
`relation-census/workspace-root-token`. So the round-nineteen CENSUS-018 clause — *"no
`error`, `remedy`, or `directory` field shall render the workspace root absolutely, **at
either entrance**"* — is false at the MCP entrance for `no-fold-arms-found`.

**Both round-nineteen witnesses are structurally blind to it, and the way they are blind
is the more useful finding.**

*The drive checks the wrong subject.* At test:7299-7305 the MCP enumeration runs every
drive and checks each result's leaks against **one** root:

```clojure
(doseq [{:keys [label drive]} (mcp-refusal-drives {:arms arms :bare bare :broken broken})]
  (let [result (drive)
        leaks (names-the-root-itself result (.getCanonicalPath arms))]
```

but `mcp-refusal-drives` runs `:no-fold-arms-found` on **`bare`** (test:5981-5984) and
`:unparseable-file` on **`broken`** (test:5986-5988). A refusal that names `bare`
absolutely contains no occurrence of `arms`, so the check returns `[]`. Demonstrated
directly:

```text
$ java -cp "$CP" clojure.main probe4b.clj
no-fold-arms-found  type=no-fold-arms-found  leaks-vs-ARMS(what the witness checks)=[]  leaks-vs-OWN-ROOT=["Nothing under /var/tmp/forge/census19-review-fx/opus/p4b/bare defines defmethod fold-event arms (0 file(s) scanned), so no narrower call can be comput…"]
unparseable-file    type=unparseable-file    leaks-vs-ARMS(what the witness checks)=[]  leaks-vs-OWN-ROOT=[]
```

The verifier was blind to its own subject: it proved that no refusal names a root that
refusal never had.

*And the last CLI-only block names the exact shape and drives only half of it.*
test:7343-7351 is titled "no arm-bearing tree is refused with its root in the prose", its
comment says `no-fold-arms-found` "is the shape whose remedy names what it SCANNED, and it
named it absolutely" — and then it drives `core/run-relation-census` alone. The author
identified the shape, fixed the CLI, wrote the witness for the CLI, and the tool's twin was
never asked.

*The source scan cannot cover for it either.* test:7371-7372 matches a whitespace-bearing
string literal adjacent to a binding literally named `root` (or `(census-root dir)`). The
leaking site's binding is named `canonical`. The scan is green over it by construction, and
would be green over any future site that names its root anything else.

**The brief's own check on this item lands as a finding.** "Confirm the enumerated set is
asserted, not listed twice." It is listed twice, and the enumeration is dead code:

```text
$ rg -n 'root-carrying-fields' test/ src/
test/clj_surgeon/mcp_relation_census_test.clj:7244:(def ^:private root-carrying-fields
```

One definition, zero references. `names-the-root-itself` re-lists the same six keys
literally at test:7265-7266. Adding a field to `root-carrying-fields` changes nothing; the
"set that shall be enumerated rather than discovered" cannot influence the only place that
uses it.

Ruling: **not blocking.** The path published is the workspace's own, not a path outside it,
so it does not trip this review's disclosure blocker, and Sol treated the identical class
at the CLI as non-blocking in round eighteen; I hold the same line rather than moving it.
But claim 4 as written in the brief is false, both witnesses that were built to make it
total are blind on the same shape, and the enumeration is not enumerated. All three should
be fixed together — check each drive against ITS OWN root, drive the tool half of the
`no-fold-arms-found` block, and make `names-the-root-itself` read `root-carrying-fields`.

---

## 5. VERIFIED — both ratchets fire at exactly the reviewer's counts, and are green on the clean tree

`test/clj_surgeon/mcp_relation_census_test.clj:6242` (bound), `:6400` (weights).

On a `git archive 563c300d` export with `entrance-bounded`
(`src/clj_surgeon/mcp_relation_census.clj:143-159`) replaced by identity:

```text
$ cd ratchet/bound && ~/bin/suite-run java -cp "$CP" -Dtarget=every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance clojure.main run-one.clj
FAIL_LINES=18
RATCHET-RESULT {:test 1, :pass 126, :fail 18, :error 0}
```

On a separate export with `candidate-field-weights`
(`src/clj_surgeon/mcp_relation_census.clj:163-182`) restricted to `[:workspace_root
:files]`:

```text
$ cd ratchet/weight && ~/bin/suite-run java -cp "$CP" -Dtarget=the-overflow-remedy-names-the-heaviest-field-it-measured clojure.main run-one.clj
FAIL_LINES=14
RATCHET-RESULT {:test 1, :pass 22, :fail 14, :error 0}
```

Controls on the unmodified clone, so the 18 and the 14 are the sabotage and not the export:

```text
RATCHET-RESULT {:test 1, :pass 144, :fail 0, :error 0}
RATCHET-RESULT {:test 1, :pass  36, :fail 0, :error 0}
```

18 and 14 exactly, as claimed.

---

## 6. VERIFIED — the battery reproduces twice, byte-identical, at the claimed counts and hash

```text
$ ~/bin/suite-run java -cp "$CP" clojure.main -m census-witness-battery > battery-1.out 2>&1   # EXIT1=0
$ ~/bin/suite-run java -cp "$CP" clojure.main -m census-witness-battery > battery-2.out 2>&1   # EXIT2=0
$ cmp -s battery-1.out battery-2.out; echo CMP_EXIT=$?
CMP_EXIT=0
$ sha256sum battery-1.out battery-2.out
9822515cea7b1e44fe4af0592b8df2899ac136f71fdfb94b617485a14cbeccc4  battery-1.out
9822515cea7b1e44fe4af0592b8df2899ac136f71fdfb94b617485a14cbeccc4  battery-2.out
```

`{:test 20, :pass 1173, :fail 0, :error 0}` and sha256 `9822515c…`, exactly as claimed.
Verbatim composition:

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
  :r16   the-constructors-are-the-only-continuation-construction-sites            pass   71  fail 0  error 0
  :r18   every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance     pass  144  fail 0  error 0
  :r18   every-declared-refusal-shape-carries-no-field-over-the-ceiling           pass  334  fail 0  error 0
  :r18   the-overflow-remedy-names-the-heaviest-field-it-measured                 pass   36  fail 0  error 0
  :r18   the-two-entrances-name-the-same-cause-for-the-same-observation           pass   55  fail 0  error 0
  :r18   a-refusal-whose-subject-is-the-root-names-the-root                       pass   20  fail 0  error 0
  :r19   no-census-reads-a-source-whose-real-path-leaves-the-workspace            pass   94  fail 0  error 0
  :r19   every-refusal-the-launcher-itself-prints-is-bounded-at-its-exit          pass   57  fail 0  error 0
  :r19   no-refusal-names-the-workspace-root-in-its-prose                         pass   46  fail 0  error 0
  :r19   no-refusal-SITE-renders-a-raw-workspace-root-into-prose                  pass    1  fail 0  error 0
:BATTERY-RESULT {:test 20, :pass 1173, :fail 0, :error 0}
```

`naming-a-source-is-not-walking-a-tree` is gone from the composition, and the EARS text
states its rule as REPLACED rather than dropping it silently. That half of the brief's
item-2 check passes.

---

## 7. RULING on the declared-open item: `:unresolvable-source-path` has no parity row

`src/clj_surgeon/mcp_paths.clj:265`, witness exclusion at
`test/clj_surgeon/mcp_relation_census_test.clj:6690-6698`.

**Acceptable as declared-open. Non-blocking, and I concur with round eighteen.** The cause
is the `:else` of the resolver's exception analysis; no ordinary path provokes it, the
witness `disj`s it with the reason written down beside the other two exclusions, and the
receipt it produces when injected is bounded, typed, relative and safe. What makes it
acceptable is that the exclusion is DERIVED — the assertion is `(= (disj vocabulary …)
(set (map :cause rows)))`, so a cause added to the vocabulary without a row still fails.
The totality hole is in the three named exclusions, not in the enumeration's structure.

Recommended, not required: give the `:else` a drivable seam so the exclusion list shrinks
to two. An unreachable branch is a branch nobody has read the output of.

---

## 8. Gates, verbatim, with exits

```text
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
Ran 463 tests containing 6537 assertions.
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

All three match the brief's claims exactly (463/6537/0 and 717/6061/0). The battery is at
item 6.

---

## 9. RED commits: red at their shas for the stated reason, green at the following fix

RED_GREEN_TABLE_PLACEHOLDER

---

## Final cleanliness and mode audit

```text
CLEANLINESS_PLACEHOLDER
```

---

## NO-GO

This tip is **not GO on its own for MCP/main**, on its own merits and separately on its
mergeability. On merits: the launcher's bound is enforced on strings while the CLI hands
callers a keyword, so a declared launcher refusal still prints 20,287 bytes with a
10,001-character run and no truncation marker at both real launchers; and the containment
fence fails OPEN when the workspace does not resolve, so `:dir <typo> :file <escaping
link>` reads a source outside every tree the request named and returns `:ok true`. On
mergeability: `git merge-tree --write-tree HEAD origin/MCP/main` exits 1 with content
conflicts in 12 files — `src/clj_surgeon/core.clj`, `mcp_intent_contract.clj`,
`mcp_server.clj`, `mcp_tool.clj`, `workspace_onboarding.clj`, and the test files
`mcp_http_server_test.clj`, `mcp_operation_registry_test.clj`, `mcp_server_test.clj`,
`mcp_test_runner.clj`, `workspace_onboarding_test.clj`, `test/mcp_stdio_smoke.clj`,
`test/run_all.clj` — so it cannot land unassisted regardless of the verdict.
