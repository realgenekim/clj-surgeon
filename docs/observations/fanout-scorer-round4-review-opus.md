## GO-WITH-FIX

Reviewer: Opus (independent round-four reviewer; Sol's content filter refused this brief, same brief
executed verbatim). Clone `/home/forge/tmp/sol/fanout-wt`, detached and clean at
`a3a367c0254234728c791113cd2ff8655f9e7471` before and after. Fixtures under
`/home/forge/tmp/fanout4-review-fx`, removed at the end. Every chmod restored. No Surgeon server; no
commit, push, stash or `git add`; no process signalled that I did not start.

**One-line verdict:** the three round-three BLOCKERS (shimmed baseline, symlink/unclassifiable
entries, unenterable directories) are genuinely closed and survive fresh attack; the roster is real
and the self-tests are causal; all nine published cohort arms rescore byte-identically. The one thing
this round did **not** close — the base-sha fallback the builder itself flagged as residual —
still reaches a **full false `6/6` on a tree containing a real planted source file**, so the tip must
not land as it stands. Finding 1 below is the required fix; findings 2-8 are the closures verified,
and findings 9-11 are non-blocking accuracy items.

---

### 1. `bench/fanout/rescore-FAN.sh:39-42` — **REQUIRED FIX: the base sha is the one input still decided by a PATH-resolved, exit-ignored, stderr-suppressed command, and that route reaches a false `6/6` with a real injected file on disk.**

`fan_check.clj` now resolves git absolutely and rebuilds the base inventory by content address — that
part is sound (finding 2 below). But `rescore-FAN.sh` chooses **which commit** to hand it, and its
third branch is:

```
41:else BASE=$(git -C "$WT" rev-list --max-parents=0 HEAD 2>/dev/null); fi
42:[ -n "$BASE" ] || { echo "rescore-FAN: FAIL cannot determine the base sha" >&2; exit 2; }
```

`git` here is PATH-resolved, the exit code is discarded, and stderr is thrown away. The only guard is
non-emptiness. `fan_check.clj:86` asserts the opposite in prose — *"when the caller passes a full
40-hex base (rescore-FAN.sh always does) even that is closed"* — and there is no 40-hex validation
anywhere in either file.

**1a. The claim "rescore-FAN.sh always [passes] a full 40-hex base" is false.** Exact command:

```bash
cd /home/forge/tmp/sol/fanout-wt
WTD=/home/forge/tmp/fanout4-review-fx/mine/attack/blobrepo
BASE=$(git -C "$WTD" rev-parse HEAD); SHORT=${BASE:0:7}
FAN_FIXTURES=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a FAN_BASE="$SHORT" \
  bash bench/fanout/rescore-FAN.sh "$WTD" 21 2>&1 | grep -E '^rescore-FAN|^CHECK 1|^fan_check:'
echo "short-base-rc=${PIPESTATUS[0]}"
FAN_FIXTURES=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a FAN_BASE="HEAD" \
  bash bench/fanout/rescore-FAN.sh "$WTD" 21 2>&1 | grep -E '^rescore-FAN|^CHECK 1'
echo "ref-base-rc=${PIPESTATUS[0]}"
```

Verbatim output:

```text
rescore-FAN: worktree=/home/forge/tmp/fanout4-review-fx/mine/attack/blobrepo n=21 base=31b46b0 fixtures=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a
fan_check: git=/usr/bin/git exec-path=/usr/lib/git-core version=git version 2.53.0 resolution=absolute-candidate
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
short-base-rc=0
rescore-FAN: worktree=/home/forge/tmp/fanout4-review-fx/mine/attack/blobrepo n=21 base=HEAD fixtures=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
ref-base-rc=0
```

A 7-character sha and the literal string `HEAD` both flow through unvalidated. With a non-40-hex base
the self-resolution guard at `fan_check.clj:189-191` (`(when (and (re-matches #"[0-9a-f]{40}" base)
(not= full base)) die)`) never fires, so the residual freedom the comment declares closed — *which*
commit the binary hands back — is open.

**1b. When the fallback is taken, a PATH shim reaches a false `6/6` on a defective tree.** The
worktree carries a real injected source file the manifest does not own — the exact round-3 defect
class. Exact command:

```bash
cd /home/forge/tmp/sol/fanout-wt
A=/home/forge/tmp/fanout4-review-fx/mine/attack
R="$A/shimbase"; rm -rf "$R"; cp -a /home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-rescore/good "$R"
BASE=$(git -C "$R" rev-parse HEAD)
printf '(ns acid.fanout.extra)\n(def planted true)\n' > "$R/src/acid/fanout/extra.clj"
git -C "$R" add src/acid/fanout/extra.clj
TREE=$(git -C "$R" write-tree); X=$(git -C "$R" commit-tree "$TREE" -p "$BASE" -m "planted base")
echo "real-base=$BASE  crafted-base=$X"
# control: the honest base
FAN_FIXTURES=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a FAN_BASE="$BASE" \
  bash bench/fanout/rescore-FAN.sh "$R" 21 2>&1 | grep -E '^rescore-FAN|^CHECK 1'
echo "control-rc=${PIPESTATUS[0]}"
# attack: no FAN_BASE, no ../base.sha, a PATH shim answering only rev-list
mkdir -p "$A/bin-revlist-crafted"
cat > "$A/bin-revlist-crafted/git" <<EOF
#!/usr/bin/env bash
for a in "\$@"; do if [ "\$a" = "rev-list" ]; then echo "$X"; exit 0; fi; done
exec /usr/bin/git "\$@"
EOF
chmod 755 "$A/bin-revlist-crafted/git"
PATH="$A/bin-revlist-crafted:$PATH" env -u FAN_BASE \
  FAN_FIXTURES=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a \
  bash bench/fanout/rescore-FAN.sh "$R" 21 2>&1 | grep -E '^rescore-FAN|^CHECK|^fan_check:'
echo "crafted-base-rc=${PIPESTATUS[0]}"
```

Verbatim output:

```text
real-base=31b46b02569a75fb99f6d5745fab4a1e0e3476a1  crafted-base=f7f964647956b7597b357584d17eb5bf39efd9da

--- control: honest run with the REAL base ---
rescore-FAN: worktree=/home/forge/tmp/fanout4-review-fx/mine/attack/shimbase n=21 base=31b46b02569a75fb99f6d5745fab4a1e0e3476a1 fixtures=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a
CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/acid/fanout/extra.clj"] (present on disk, absent from git's untracked listing)
rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
control-rc=1

--- attack: NO FAN_BASE, NO ../base.sha, PATH shim answers rev-list with the crafted commit ---
rescore-FAN: worktree=/home/forge/tmp/fanout4-review-fx/mine/attack/shimbase n=21 base=f7f964647956b7597b357584d17eb5bf39efd9da fixtures=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a
fan_check: git=/usr/bin/git exec-path=/usr/lib/git-core version=git version 2.53.0 resolution=absolute-candidate
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
fan_check: 4/4 structural checks passed
CHECK 4 load: PASS namespaces=100 rc=0
CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
rescore-FAN: 6/6 checks passed
crafted-base-rc=0
```

Note `CHECK 6 ... src-files=101`: the scorer **counted the planted file and passed anyway**. `git` was
resolved absolutely (`resolution=absolute-candidate`) — the shim never touched `fan_check.clj`. It
only had to answer one `rev-list`.

**1c. A failing `rev-list` is accepted silently.** This is round-3 finding 8's row 7, unchanged
(`git diff --stat f4975d0b..a3a367c0` touches only `fan_check.clj` and `sabotage-FAN.sh`). Exact
command and verbatim output:

```bash
cat > "$A/bin-revlist-nonzero/git" <<EOF
#!/usr/bin/env bash
if [ "\$3" = "rev-list" ] || [ "\$1" = "rev-list" ]; then
  echo "$REAL"; echo "simulated-rev-list-failure" >&2; exit 7
fi
exec /usr/bin/git "\$@"
EOF
PATH="$A/bin-revlist-nonzero:$PATH" env -u FAN_BASE FAN_FIXTURES=.../gate-k/k6-a \
  bash bench/fanout/rescore-FAN.sh "$WTD" 21 2>&1 | grep -E '^rescore-FAN|^CHECK 1'
```

```text
rescore-FAN: worktree=/home/forge/tmp/fanout4-review-fx/mine/attack/blobrepo n=21 base=31b46b02569a75fb99f6d5745fab4a1e0e3476a1 fixtures=/home/forge/tmp/fanout4-review-fx/mine/gate-k/k6-a
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
rescore-FAN: 6/6 checks passed
revlist-nonzero-rc=0
```

**1d. Reachability in a real cohort run — the answer the brief asked for: yes.** The shipped gate is
`/home/forge/tmp/arms/eafford/gate-eafford.sh:15-17`:

```bash
  gout=$(FAN_FIXTURES="$FIX" FAN_BASE="$(tr -d '[:space:]' < "$A/base.sha" 2>/dev/null)" \
         flock -x /home/forge/tmp/suite.lock \
         bash "$FANOUT/bench/fanout/rescore-FAN.sh" "$A/worktree" 21 2>&1)
```

If `base.sha` is missing or unreadable, `2>/dev/null` swallows the error, `FAN_BASE` is the empty
string, `rescore-FAN.sh:39` falls through, `rescore-FAN.sh:40`'s `$WT/../base.sha` **is the same
missing file**, and line 41 fires. Verbatim:

```text
--- if base.sha is absent, what does the gate export? ---
/bin/bash: line 9: /home/forge/tmp/does-not-exist-base.sha: No such file or directory
FAN_BASE-from-missing-file=[]
```

So the gate **fails loudly on a missing manifest or canonical** (`rescore-FAN.sh:35`, `exit 2`) and
**fails open on a missing base**, substituting a PATH command's discarded-exit output — and the
`rescore-FAN:` header prints the base without its provenance, so the two runs are indistinguishable
in the log. (All nine published arms do carry a 40-hex `base.sha`, so nothing published is affected —
see finding 8.)

**Required fix, four lines and one ratchet:**
1. Validate the base at the boundary: `[[ $BASE =~ ^[0-9a-f]{40}$ ]]` or `exit 2`, in `rescore-FAN.sh`
   — for **all three** branches, so the prose at `fan_check.clj:86` becomes true;
2. treat a missing base like a missing fixture: `exit 2`, not a fallback — or, if the fallback stays,
   capture `rev-list`'s stderr and exit and refuse on either, exactly as the three listings inside
   `fan_check.clj` already do;
3. resolve `git` in `rescore-FAN.sh` the way `fan_check.clj` does (`$FAN_GIT` else the absolute
   candidate list), so no git call in the pipeline is PATH-resolved;
4. print the base's **provenance** on the `rescore-FAN:` header (`base-from=FAN_BASE|base.sha|rev-list`);
5. **ratchet**: a tenth self-test, `--selftest-base-resolution`, replaying exactly 1b — a planted
   untracked-then-staged file, no `FAN_BASE`, no `../base.sha`, a PATH shim answering `rev-list` with
   an adversary-crafted commit — asserting nonzero rc and no `6/6`. It must be RED at `a3a367c0` and
   GREEN after the fix.

---

### 2. `bench/fanout/fan_check.clj:96-201` — **finding 2's object-store half is closed. A hostile git that lies about a BLOB is unreachable: the scorer never reads one.**

The brief asks whether CHECK 1 needs blob content, and whether a lying blob is reachable given
CHECK 2/6 read the working tree. I instrumented every git invocation with a logging `FAN_GIT` relay.
Exact command:

```bash
cd /home/forge/tmp/sol/fanout-wt
A=/home/forge/tmp/fanout4-review-fx/mine/attack
mkdir -p "$A/logging-git"
cat > "$A/logging-git/git" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$FANLOG"
exec /usr/bin/git "$@"
EOF
chmod 755 "$A/logging-git/git"
BASE=$(git -C "$A/blobrepo" rev-parse HEAD); : > "$A/git-calls.log"
FANLOG="$A/git-calls.log" FAN_GIT="$A/logging-git/git" \
  bb bench/fanout/fan_check.clj "$A/blobrepo" .../manifest-21.edn .../canonical-21 "$BASE" \
  2>&1 | grep -E '^CHECK|^fan_check'
awk '{print $3}' "$A/git-calls.log" | sort | uniq -c | sort -rn
grep -c 'cat-file blob\|cat-file -p\| show ' "$A/git-calls.log"
grep -o 'cat-file [a-z]*' "$A/git-calls.log" | sort | uniq -c
```

Verbatim output:

```text
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
fan_check: 4/4 structural checks passed
--- distinct subcommands the scorer invoked ---
      5 cat-file
      2 
      1 rev-parse
      1 ls-tree
      1 ls-files
      1 diff
--- any blob read? ---
0
--- cat-file lines by type ---
      1 cat-file commit
      4 cat-file tree
```

**CHECK 1 needs no blob content at all** — the inventory is paths, taken from commit and tree objects
only, each hash-verified in-process (`verified-object`, `fan_check.clj:145-160`). CHECK 2, 3 and 6
read the working tree through `io/file wt` / `slurp` (`fan_check.clj:469-470, 487, 509-510, 522`),
never through git. There is no channel by which a forged blob can influence a verdict. This half of
the independence argument holds as written.

### 3. `bench/fanout/fan_check.clj:98-128` — **the `FAN_GIT` install-prefix attack is moot in both directions, and the prefix check is decorative.**

The brief asks for an `FAN_GIT` proxy whose `--exec-path` is a real directory under a fake install
prefix. Built it; it is unnecessary, because `FAN_GIT` **skips the prefix check entirely**
(`fan_check.clj:119-127` takes the WARNING branch). Exact command and verbatim output:

```bash
mkdir -p "$A/fakeprefix/bin" "$A/fakeprefix/libexec/git-core"
# proxy: answers --exec-path with a real dir under its own fake prefix, relays everything else
FAN_GIT="$A/fakeprefix/bin/git" bb bench/fanout/fan_check.clj "$A/blobrepo" ... "$BASE"
```

```text
proxy---exec-path=/home/forge/tmp/fanout4-review-fx/mine/attack/fakeprefix/libexec/git-core
fan_check: git=/home/forge/tmp/fanout4-review-fx/mine/attack/fakeprefix/bin/git exec-path=/home/forge/tmp/fanout4-review-fx/mine/attack/fakeprefix/libexec/git-core version=git version 2.53.0 resolution=FAN_GIT-override
fan_check: WARNING FAN_GIT override in use -- the install-prefix check is skipped; the content-addressed base inventory below is what defends this run
CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
fan_check: 4/4 structural checks passed
```

That PASS is correct — the proxy is an honest relay — and a *dishonest* `FAN_GIT` is caught by the
content-addressed inventory (`--selftest-shimmed-baseline`, finding 6, and my own runs in finding 5).
In the other direction the prefix check is unreachable without root:

```text
/usr/bin/git exists=yes writable-by-me=no
/bin/git exists=yes writable-by-me=no
/usr/local/bin/git exists=no writable-by-me=no
/opt/homebrew/bin/git exists=no writable-by-me=no
can-I-write-/usr/bin: no
am-I-root: no (uid=1011)
```

**Assessment:** part (1) of the independence argument is load-bearing for exactly one reason —
`git` is never resolved through PATH — and that reason is real and correct *inside `fan_check.clj`*.
The `--exec-path`-under-own-prefix test adds nothing an attacker in the stated model can reach; it
should be described as belt-and-braces rather than as a defence. SHA-1 collision is explicitly out of
scope, as the brief directs; nothing here depends on preimage resistance beyond the tree/commit
verification the builder already names.

### 4. `bench/fanout/fan_check.clj:236-333, 446` — **finding 3 is closed. Five fresh symlink/hard-link attacks all fail closed, with and without an incomplete `ls-files`.**

`walk-src` classifies every entry NOFOLLOW; symlinks become leaves in `:entries`; `walked-src` is
`:entries`, not `:files` (`fan_check.clj:446`). Exact commands (one copy of the good tree per case,
`--probe-walk` then the full gate, then the full gate again under a `FAN_GIT` whose `ls-files` exits
0 with empty stdout — the round-3 shim, now delivered through the seam since PATH no longer works):

```bash
ln -s b "$A/loop/src/a"; ln -s a "$A/loop/src/b"                     # B1 symlink loop a->b->a
ln -s /etc/passwd "$A/outside/src/outside-link.clj"                   # B2 target outside the workspace
ln "$A/hard/src/acid/fanout/ns_000.cljc" "$A/hard/src/acid/fanout/hardlink.cljc"  # B3 hard link
bb bench/fanout/fan_check.clj --probe-walk "$A/<case>"
FAN_FIXTURES=.../k6-a FAN_BASE="$B" bash bench/fanout/rescore-FAN.sh "$A/<case>" 21
FAN_GIT="$A/git-empty-lsfiles/git" FAN_FIXTURES=.../k6-a FAN_BASE="$B" \
  bash bench/fanout/rescore-FAN.sh "$A/<case>" 21
```

Verbatim output:

```text
--- B1-symlink-loop : probe-walk ---
WALK-PROBE dirs-found=3 dirs-entered=3 entries-seen=104 files=100 symlinks=2 errors=0
WALK-PROBE symlink src/a -> dangling
WALK-PROBE symlink src/b -> dangling
CHECK 1 file-set: FAIL changed=23 expected=21 missing=0 [] extras=2 ["src/a" "src/b"]
B1-symlink-loop-rc=1
--- B2-symlink-outside : probe-walk ---
WALK-PROBE symlink src/outside-link.clj -> file
CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/outside-link.clj"]
B2-symlink-outside-rc=1
--- B3-hard-link : probe-walk ---
WALK-PROBE dirs-found=3 dirs-entered=3 entries-seen=103 files=101 symlinks=0 errors=0
CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/hardlink.cljc"]
B3-hard-link-rc=1

--- the same three, under a FAN_GIT whose ls-files is exit-0/EMPTY ---
CHECK 1 file-set: ERROR listing-incomplete unreported=2 ["src/a" "src/b"] (present on disk, absent from git's untracked listing)
loop-shimmed-rc=1
CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/outside-link.clj"] (present on disk, absent from git's untracked listing)
outside-shimmed-rc=1
CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/acid/fanout/hardlink.cljc"] (present on disk, absent from git's untracked listing)
hard-shimmed-rc=1
```

The **loop terminates and is never traversed** — both arms are labelled `dangling` by
`link-target-kind` (`fan_check.clj:279-287`), which is the only place a link is resolved and only to
label it. The **outside target is never opened**: CHECK 6 reads `:files`, not `:entries`
(`fan_check.clj:509-510`), so `/etc/passwd` is never slurped. The **hard link is a regular file, and
that is correct** — git treats it identically; there is no filesystem-level distinction to make and
none is needed, because the extra path is caught as an extra by set difference either way.

Two further cases from the brief:

**Trailing newline in a filename** — handled, escaped in the report, fail-closed both ways:

```text
WALK-PROBE file src/acid/fanout/trailing$
CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/trailing\n.clj"]
nl-rc=1
CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/acid/fanout/trailing\n.clj"] (present on disk, absent from git's untracked listing)
nl-shimmed-rc=1
```

**The listing/classification race** — the brief's "a directory entry that disappears between listing
and classification". `walk-src` forces the whole `DirectoryStream` inside `with-open`
(`fan_check.clj:300-303`) and then stats each name, so the window is real. I forced it: 4,000 files
in `src/churn`, a background deleter racing one `--probe-walk`. Caught on the **first** attempt:

```text
attempt=1 :: WALK-PROBE dirs-found=4 dirs-entered=4 entries-seen=4093 files=4077 symlinks=0 errors=13
WALK-PROBE error :unclassifiable-entry src/churn/f0010.clj NoSuchFileException: .../src/churn/f0010.clj
WALK-PROBE error :unclassifiable-entry src/churn/f0013.clj NoSuchFileException: .../src/churn/f0013.clj
race-caught-a-vanishing-entry=1
```

Thirteen typed, named, path-bearing errors and `System/exit 1` — the race is a refusal, not a silent
drop. This is the strongest single piece of evidence that finding 3's rule ("every name a successful
listing returns becomes exactly one classified entry or one named error") is enforced and not merely
documented.

### 5. `bench/fanout/fan_check.clj:274-313` — **finding 4 is closed. The two attacks the brief names both fail closed, and one of them is not constructible at all.**

**A readable-and-searchable directory whose ancestor is not searchable, reached deeper:** the walk
always descends from `<wt>/src`, so "given directly" reduces to an unenterable ancestor. Exact
command and verbatim output (chmod restored and re-`stat`ed):

```bash
mkdir -p "$A/b6/src/probe/outer/deep"; printf ';; x\n' > "$A/b6/src/probe/outer/deep/leaf.clj"
chmod 755 "$A/b6/src/probe/outer/deep"; chmod 0600 "$A/b6/src/probe/outer"
trap 'chmod 755 "$A/b6/src/probe/outer"' EXIT
bb bench/fanout/fan_check.clj --probe-walk "$A/b6"
FAN_FIXTURES=.../k6-a FAN_BASE="$B" bash bench/fanout/rescore-FAN.sh "$A/b6" 21
chmod 755 "$A/b6/src/probe/outer"; trap - EXIT; stat -c 'restored-mode=%a %n' "$A/b6/src/probe/outer"
```

```text
ancestor mode=600 .../b6/src/probe/outer
stat: cannot stat '.../b6/src/probe/outer/deep': Permission denied (os error 13)
can-I-cd-to-deep-directly: no
WALK-PROBE dirs-found=5 dirs-entered=4 entries-seen=104 files=100 symlinks=0 errors=1
WALK-PROBE error :unenterable-dir src/probe/outer readable=true executable=false perms=rw------- -- a directory that cannot be entered is an error at any mode
CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr: warning: unable to access 'src/probe/outer/.gitignore': Permission denied
rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
b6-rc=1
restored-mode=755 .../b6/src/probe/outer
```

Two independent refusals fire (the walk's typed error and git's stderr guard). Also verified: a
**missing `src/` altogether** is one typed error, not an empty inventory —
`WALK-PROBE error :unenterable-dir src readable=false executable=false perms=unreadable-mode(NoSuchFileException)`.

**The ACL-denied directory at mode 755 is not constructible for the process that owns it**, and I
report that rather than claiming a pass I did not earn. Exact command and verbatim output:

```bash
chmod 755 "$A/b7/src/probe/acldir"; setfacl -m u:$(id -un):--- "$A/b7/src/probe/acldir"
stat -c 'mode=%a %n' "$A/b7/src/probe/acldir"; getfacl -p "$A/b7/src/probe/acldir"
ls "$A/b7/src/probe/acldir" >/dev/null 2>&1 && echo yes || echo no
```

```text
acl-set
mode=755 .../b7/src/probe/acldir
# owner: forge
user::rwx
user:forge:---
group::r-x
shell-can-list: yes
WALK-PROBE dirs-found=5 dirs-entered=5 entries-seen=105 files=101 symlinks=0 errors=0
CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/probe/acldir/leaf.clj"]
b7-rc=1
```

A POSIX named-user ACL entry is inert against the file's **owner** (`user::` governs), so this seat
cannot build the case; a directory owned by another user is not available without sudo. On the code
path, both guards would fire: `Files/isReadable` and `isExecutable` go through `access(2)`, which
honours ACLs, and even if they did not, the attempted `newDirectoryStream` is inside the `try` at
`fan_check.clj:298-308` whose `AccessDeniedException` becomes the same typed `:unenterable-dir`. I
verified that second path really is the one that fires for mode 0200/0000 in finding 7 below.
**Residual, stated honestly:** the ACL case is argued, not measured.

### 6. `bench/fanout/sabotage-FAN.sh:33-46` — **the roster is computed and equals the dispatch; nine modes.**

Exact command and verbatim output (roster compared against an *independent* grep of the same file):

```bash
grep -n '^if \[ "\${1:-}" = "--selftest' bench/fanout/sabotage-FAN.sh | sed 's/;.*//'
diff <(bash bench/fanout/sabotage-FAN.sh --selftest-list | grep '^--selftest-' | sort) \
     <(grep -o '^if \[ "\${1:-}" = "\(--selftest-[a-z-]*\)"' bench/fanout/sabotage-FAN.sh \
       | sed 's/.*"\(--selftest-[a-z-]*\)"/\1/' | grep -v -- '--selftest-list$' | sort) \
  && echo IDENTICAL || echo DIFFER
```

```text
43:if [ "${1:-}" = "--selftest-list" ]
61:if [ "${1:-}" = "--selftest-k" ]
139:if [ "${1:-}" = "--selftest-backslash" ]
222:if [ "${1:-}" = "--selftest-listing-failure" ]
334:if [ "${1:-}" = "--selftest-whitespace-path" ]
415:if [ "${1:-}" = "--selftest-incomplete-listing" ]
538:if [ "${1:-}" = "--selftest-pruned-walk" ]
631:if [ "${1:-}" = "--selftest-shimmed-baseline" ]
778:if [ "${1:-}" = "--selftest-symlink-entries" ]
912:if [ "${1:-}" = "--selftest-unsearchable-dir" ]
sabotage-FAN: self-test modes (9): --selftest-k --selftest-backslash --selftest-listing-failure --selftest-whitespace-path --selftest-incomplete-listing --selftest-pruned-walk --selftest-shimmed-baseline --selftest-symlink-entries --selftest-unsearchable-dir
diff-roster-vs-dispatch: IDENTICAL
```

### 7. **Each of the three new modes is CAUSAL — the GREEN hunk was sabotaged on a scratch export of `a3a367c0` and each mode goes red.**

The export is content-verified against the reviewed blob before any edit:

```text
export-head-blob-fan_check=463db2ef07f7b737a99500cabd4f117c3afeb4dd
export-copy-sha256=c6cae83761f370b5  git-blob-content-sha256=c6cae83761f370b5
```

**7a. `--selftest-shimmed-baseline` witnesses `fan_check.clj:432` (the object-store baseline).**
Sabotage: `baseline-src (:files store-inv)` → `baseline-src (into #{} (split-nul (:out base-tree)))`
— i.e. the round-3 behaviour, baseline from `ls-tree`. Verbatim:

```text
432c432
<         baseline-src (:files store-inv)
---
>         baseline-src (into #{} (split-nul (:out base-tree)))
SELFTEST-SHIMMED-BASELINE fan-git-shimmed: rc=0
SELFTEST-SHIMMED-BASELINE fan-git-shimmed fail-closed: FAIL rc=0 -- want a 'ERROR base-inventory mismatch' line and no PASS line
SELFTEST-SHIMMED-BASELINE full-gate fan-git-shimmed: rc=0
SELFTEST-SHIMMED-BASELINE full-gate fan-git-shimmed: FAIL rescore-FAN reported 6/6 (or rc=0) -- the coordinated shim reached a false GREEN
sabotage-FAN --selftest-shimmed-baseline: 4 passed, 2 failed
```

**7b. `--selftest-symlink-entries` witnesses `fan_check.clj:446` (symlink leaves in the reconciled
set).** Sabotage: `walked-src (:entries walk)` → `(:files walk)`. Verbatim:

```text
446c446
<         walked-src (:entries walk)
---
>         walked-src (:files walk)
SELFTEST-SYMLINK-ENTRIES A linked-empty fail-closed: FAIL rc=0 -- want nonzero rc, a line matching 'src/linked-empty' ... got: CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
SELFTEST-SYMLINK-ENTRIES C linked-file fail-closed: FAIL rc=0 -- ... got: CHECK 1 file-set: PASS ...
SELFTEST-SYMLINK-ENTRIES D dangling fail-closed: FAIL rc=0 -- ... got: CHECK 1 file-set: PASS ...
sab-symlink-rc=1
```

Note the probe assertions still PASS while the gate assertions FAIL — the classification is right and
only the reconciliation was broken. That is a well-aimed test.

**7c. `--selftest-unsearchable-dir` witnesses `fan_check.clj:289` (the explicit enterability
assertion), and precisely at mode 0400.** Sabotage: `names (if-not (and readable executable)` →
`names (if-not true`, so the assertion is never taken. Verbatim:

```text
289c289
<                     names (if-not (and readable executable)
---
>                     names (if-not true
SELFTEST-UNSEARCHABLE-DIR 0400-readable-not-searchable probe: FAIL want 'WALK-PROBE error :unenterable-dir src/probe/outer/blocked'; got: WALK-PROBE dirs-found=6 dirs-entered=6 entries-seen=106 files=100 symlinks=0 errors=1|WALK-PROBE error :unclassifiable-entry src/probe/outer/blocked/residue.clj AccessDeniedException: ...
SELFTEST-UNSEARCHABLE-DIR 0400-readable-not-searchable fail-closed: FAIL rc=1 -- want a typed 'unenterable-dir' error naming src/probe/outer/blocked and no CHECK 1 PASS line
SELFTEST-UNSEARCHABLE-DIR 0200-not-readable fail-closed: PASS rc=1, typed unenterable-dir error naming src/probe/outer/blocked, no false PASS
SELFTEST-UNSEARCHABLE-DIR 0000-no-access fail-closed: PASS rc=1, typed unenterable-dir error naming src/probe/outer/blocked, no false PASS
SELFTEST-UNSEARCHABLE-DIR 0400-one-level-up fail-closed: FAIL rc=1 -- want a typed 'unenterable-dir' error naming src/probe and no CHECK 1 PASS line
sabotage-FAN --selftest-unsearchable-dir: 4 passed, 4 failed
```

Two things worth recording. (i) The mode is causal exactly on the round-3 defect (0400 —
readable-but-not-searchable) and correctly indifferent at 0200/0000, where the attempted
`newDirectoryStream` supplies the same typed error. (ii) Even with the assertion removed the gate
**still fails closed** (`rc=1`, `:unclassifiable-entry` with the child's path) — finding 4's fix is
defence-in-depth over finding 3's per-entry rule, and the self-test asserts the *typed reason*, not
merely a nonzero exit. That is the right strictness.

### 8. **Gates, verbatim, and the retroactive re-score: no published verdict moved.**

All nine self-tests and the plain sabotage run, at `a3a367c0`, from the clean clone:

```text
list-rc=0
--selftest-k-rc=0                       SELFTEST-K: 11 passed, 0 failed
--selftest-backslash-rc=0               SELFTEST-BACKSLASH: PASS
--selftest-listing-failure-rc=0         4 passed, 0 failed
--selftest-whitespace-path-rc=0         PASS
--selftest-incomplete-listing-rc=0      4 passed, 0 failed
--selftest-pruned-walk-rc=0             2 passed, 0 failed
--selftest-shimmed-baseline-rc=0        6 passed, 0 failed
--selftest-symlink-entries-rc=0         (A..E) 0 failed
--selftest-unsearchable-dir-rc=0        8 passed, 0 failed
plain-rc=0                              sabotage-FAN: 7 passed, 0 failed (1 positive control + 6 sabotages)
```

Plain run, verbatim:

```text
=== positive control: the correct tree must be 6/6 GREEN ===
POSITIVE CONTROL: GREEN 6/6
SABOTAGE 1 wrong-alias: RED as designed -> CHECK 6 residue-and-alias: FAIL src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=1 ["src/acid/fanout/ns_003.clj"] shadowing=0 []
SABOTAGE 2 one-site-missed: RED as designed -> CHECK 6 residue-and-alias: FAIL src-files=100 old-lib-hits=0 [] old-site-residue=1 ["src/acid/fanout/ns_003.clj"] wrong-or-missing-alias=0 [] shadowing=0 []
SABOTAGE 3 extra-file: RED as designed -> CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/ns_000.cljc"]
SABOTAGE 4 corrupted-docstring: RED as designed -> CHECK 3 protected-regions: FAIL regions=106 intact=105 manifest-sha-mismatch=0 damaged=1 [["src/acid/fanout/ns_003.clj" "docstring-token"]]
SABOTAGE 5 reordered-require: RED as designed -> CHECK 2 form-equality: FAIL compared=21 equal=20 unparseable=0 [] unequal=1 ["src/acid/fanout/ns_003.clj"]
SABOTAGE 6 unparseable: RED as designed -> CHECK 2 form-equality: FAIL compared=21 equal=20 unparseable=1 [["src/acid/fanout/ns_003.clj" "Unexpected EOF. [at line 50, column 1]"]] unequal=0 []
sabotage-FAN: 7 passed, 0 failed (1 positive control + 6 sabotages)
```

**Retroactive, all nine published `eafford` arms** (not only `eafford-T-T-1`), rescored with the
round-4 scorer using the identical invocation shape as `gate-eafford.sh`, and each arm's CHECK lines
diffed against its published `gate.log`:

```bash
for A in /home/forge/tmp/arms/eafford/eafford-*-*-*; do
  new=$(FAN_FIXTURES=/home/forge/tmp/arms/ereg/fanout-k1 \
        FAN_BASE="$(tr -d '[:space:]' < "$A/base.sha")" \
        bash bench/fanout/rescore-FAN.sh "$A/worktree" 21 2>&1 | grep -E '^CHECK|^rescore-FAN:|^fan_check:')
  diff <(grep -E '^CHECK|^rescore-FAN:|^fan_check:' "$A/gate.log") \
       <(printf '%s\n' "$new" | grep -v '^fan_check: git=')
done
```

```text
eafford-Nd-N-1: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-Nd-N-2: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-Nd-N-3: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-Ns-N-1: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-Ns-N-2: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-Ns-N-3: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-T-T-1: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-T-T-2: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
eafford-T-T-3: round4-verdict=rescore-FAN: 6/6 checks passed published=rescore-FAN: 6/6 checks passed compare=IDENTICAL
```

Every arm's `base.sha` is 40 hex (`eafford-T-T-1`: `65fe39a9071083f478ed091ab64ebdf05c02abbd`, len 40),
so the fallback of finding 1 was **not** taken for any published verdict. **A stricter scorer flipped
nothing, and nothing published rests on the open route.**

---

### Non-blocking accuracy items

### 9. `bench/fanout/fan_check.clj:270` — **the walk cannot round-trip a non-UTF-8 filename: it undercounts, though it still fails closed.**

Java decodes directory names with `sun.jnu.encoding=UTF-8`, so two distinct raw byte names collapse
to one JVM string and one set element. Exact command and verbatim output:

```bash
python3 -c "import os; d=os.fsencode('$A/collapse/src')
for b in (b'\xff', b'\xfe'): open(d+b'/x'+b+b'y.clj','wb').write(b';; planted\n')"
ls -b "$A/collapse/src" | grep -c '^x'
git -C "$A/collapse" ls-files --others --exclude-standard -z | tr '\0' '\n' | LC_ALL=C grep -ac '^src/x'
bb bench/fanout/fan_check.clj --probe-walk "$A/collapse" | LC_ALL=C grep -c 'WALK-PROBE file src/x'
FAN_GIT="$A/git-empty-lsfiles/git" FAN_FIXTURES=.../k6-a FAN_BASE="$B" \
  bash bench/fanout/rescore-FAN.sh "$A/collapse" 21
```

```text
raw-entries-on-disk=2
git-ls-files-planted=2
walk-planted=1
CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/xM-oM-?M-=y.clj"] (present on disk, absent from git's untracked listing)
collapse-shimmed-rc=1
```

Two planted files, one reported. The gate still refuses (so this is not a false-PASS route), but the
count in the error line is wrong and the reported path is not the path on disk. The same decode
applies to `clojure.java.shell/sh`'s handling of `ls-files -z` output and to `parse-tree`'s
`String. ... "UTF-8"` (`fan_check.clj:169`), so both sides mangle consistently, which is why it stays
closed. If exactness ever matters, compare raw bytes (`java.nio.file.Path` → `toString` on the
default provider is lossy; `Files.newDirectoryStream` names would need
`sun.jnu.encoding=ISO-8859-1` or a byte-level comparison). One line in the walk's docstring naming
this limit would be honest.

### 10. `bench/fanout/sabotage-FAN.sh:36` — **the roster's own grep can miss a dispatchable mode, so "the roster IS the dispatch" is true only for `[a-z-]` names.**

`sed -n 's/^if \[ "\${1:-}" = "\(--selftest-[a-z-]*\)" \];.*/\1/p'` excludes digits, uppercase and
underscores, and only matches a line beginning `if` (not `elif`). Exact command and verbatim output
(on a scratch export, a real dispatchable mode injected):

```bash
# inject:  if [ "${1:-}" = "--selftest-mode2" ]; then echo ...; exit 0; fi   before --selftest-k
bash bench/fanout/sabotage-FAN.sh --selftest-list | head -1
bash bench/fanout/sabotage-FAN.sh --selftest-mode2; echo "mode2-rc=$?"
```

```text
roster-says: sabotage-FAN: self-test modes (9): --selftest-k --selftest-backslash --selftest-listing-failure --selftest-whitespace-path --selftest-incomplete-listing --selftest-pruned-walk --selftest-shimmed-baseline --selftest-symlink-entries --selftest-unsearchable-dir
but the mode runs: SELFTEST-MODE2: I am a real, dispatchable mode
mode2-rc=0
```

Cheap ratchet: widen to `[A-Za-z0-9_-]`, match `^\(el\)\?if`, and add a self-test that injects a mode
and asserts the roster count rises. This is the same class as the "typed count" defect the comment at
`sabotage-FAN.sh:33-34` says it already paid for once.

### 11. **Nothing in the tree runs the roster.** `--selftest-list` is referenced only inside
`sabotage-FAN.sh` itself — no Makefile target, bb task or CI step runs all nine. The roster is a
printed list, not a gate, so a mode can be added and never executed. Whoever lands this should add
one target that iterates `--selftest-list` and fails on any nonzero rc (the four-line loop I used is
in `/home/forge/tmp/fanout4-review-fx/mine/run-gates.sh`, which is removed with the fixtures — it is
reproduced in the exact command of finding 8).

---

### Final audit

```bash
find /home/forge/tmp/fanout4-review-fx -type d ! -perm -u=x -print
find /home/forge/tmp/fanout4-review-fx -type f ! -perm -u=r -print
getfacl -R -s /home/forge/tmp/fanout4-review-fx
git -C /home/forge/tmp/sol/fanout-wt rev-parse HEAD
git -C /home/forge/tmp/sol/fanout-wt status --short --branch
```

```text
directories-without-owner-x:
files-without-owner-r:
non-trivial ACLs:
a3a367c0254234728c791113cd2ff8655f9e7471
## HEAD (no branch)
```

Every chmod restored, the one ACL removed, no fixture left with a stripped permission, the clone
detached and clean at the reviewed tip, and every published arm's worktree untouched (each still
shows its own 21 modified files and nothing else). Fixtures removed after this audit.

## GO-WITH-FIX

`a3a367c0` may land on MCP/main **only after finding 1 lands with it** — validate the base as 40 hex
in all three branches of `rescore-FAN.sh:39-41`, refuse a missing base instead of falling back,
resolve `git` there the way `fan_check.clj` does, print the base's provenance, and add the
`--selftest-base-resolution` ratchet that is RED at `a3a367c0` and GREEN after — because until then
one PATH-resolved, exit-ignored command still decides the single input every one of the six checks is
measured against, and I have shown that route producing `rescore-FAN: 6/6 checks passed` on a
worktree carrying a planted source file.
