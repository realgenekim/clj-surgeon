## NO-GO

1. `bench/fanout/fan_check.clj:46-173`, `bench/fanout/sabotage-FAN.sh:373-575` — **scope and provenance are correct.** The reviewed worktree stayed detached and clean at the requested tip. `68d1570` adds the two RED modes; `f4975d0` adds stderr rejection, `walk-src`, the base-tree cross-check, validated-walk reuse, and the named manifest error. The RED commit's scorer blob is byte-identical to `bc6021f`'s, so running the RED harness is a direct execution of the `bc6021f` scorer.

   Exact command:

   ```bash
   git rev-parse HEAD
   git status --short --branch
   git log --oneline bc6021f..f4975d0
   git rev-parse bc6021f:bench/fanout/fan_check.clj
   git rev-parse 68d1570:bench/fanout/fan_check.clj
   git rev-parse f4975d0:bench/fanout/fan_check.clj
   ```

   Verbatim output:

   ```text
   f4975d0b837e0526f9287c3c0a3a38118a3d9c51
   ## HEAD (no branch)
   f4975d0b bench/fanout: GREEN — CHECK 1 cross-checks git's listing against an independent filesystem walk of src/; every consumed listing rejects stderr
   68d15704 bench/fanout: RED — two self-tests for Sol round-2 finding 1 (successful-but-incomplete git listings, silently pruned filesystem walk)
   e350bbe43ecba364473acca30004043904df47f2
   e350bbe43ecba364473acca30004043904df47f2
   94a5e276c18ebd7b57c5cdaa7d3c942c0c5aeb55
   ```

2. `bench/fanout/fan_check.clj:153-172` — **BLOCKER: the new baseline is fetched through the same PATH-controlled `git`, so a coordinated shim can bless the omitted file and restore the full false `6/6`.** The exact requested attack, in which forged `ls-tree` and forged `ls-files` both omit the same base file, is caught by the independent walk. But if the same shim makes `ls-tree` claim that an omitted untracked extra was already in the base, `baseline-src` equals the walked set, `unreported` is empty, CHECK 1 sees only the 21 expected `git diff` paths, and the full gate passes. This directly contradicts the implementation's “independent completeness cross-check” claim under the PATH-shim threat model used by its own ratchets.

   Exact command for the requested both-omit attack:

   ```bash
   chmod 755 /tmp/fanout3-review-fx/bin-fake-both-base/git
   WT=/tmp/fanout3-review-fx/gate-k/k6-rescore/good
   BASE=$(git -C "$WT" rev-parse HEAD)
   printf 'real-ls-tree-hit='
   git -C "$WT" ls-tree -r --name-only -z "$BASE" -- src | tr '\0' '\n' | rg 'src/acid/fanout/ns_000.cljc' | wc -l
   printf 'forged-ls-tree-hit='
   PATH="/tmp/fanout3-review-fx/bin-fake-both-base:$PATH" git -C "$WT" ls-tree -r --name-only -z "$BASE" -- src | tr '\0' '\n' | rg 'src/acid/fanout/ns_000.cljc' | wc -l
   printf 'forged-ls-files-hit='
   PATH="/tmp/fanout3-review-fx/bin-fake-both-base:$PATH" git -C "$WT" ls-files -z --others --exclude-standard | tr '\0' '\n' | rg 'src/acid/fanout/ns_000.cljc' | wc -l
   PATH="/tmp/fanout3-review-fx/bin-fake-both-base:$PATH" bb bench/fanout/fan_check.clj "$WT" /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'fake-both-base-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   real-ls-tree-hit=1
   forged-ls-tree-hit=0
   forged-ls-files-hit=0
   CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/acid/fanout/ns_000.cljc"] (present on disk, absent from git's untracked listing)
   fake-both-base-pipeline-rc=1
   ```

   Exact command for the coordinated counterfeit that defeats the tip:

   ```bash
   chmod 755 /tmp/fanout3-review-fx/bin-bless-extra/git
   BASE=$(git -C /tmp/fanout3-review-fx/fake-both/repo rev-parse HEAD)
   printf 'real-untracked='
   git -C /tmp/fanout3-review-fx/fake-both/repo ls-files --others --exclude-standard | rg 'extra.clj'
   printf 'forged-ls-files-bytes='
   PATH="/tmp/fanout3-review-fx/bin-bless-extra:$PATH" git -C /tmp/fanout3-review-fx/fake-both/repo ls-files -z --others --exclude-standard | wc -c
   printf 'forged-ls-tree-extra-hits='
   PATH="/tmp/fanout3-review-fx/bin-bless-extra:$PATH" git -C /tmp/fanout3-review-fx/fake-both/repo ls-tree -r --name-only -z "$BASE" -- src | tr '\0' '\n' | rg 'extra.clj' | wc -l
   PATH="/tmp/fanout3-review-fx/bin-bless-extra:$PATH" bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/fake-both/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK [1-6]|^fan_check'
   rc1=${PIPESTATUS[0]}
   PATH="/tmp/fanout3-review-fx/bin-bless-extra:$PATH" FAN_FIXTURES=/tmp/fanout3-review-fx/gate-k/k6-a FAN_BASE="$BASE" bash bench/fanout/rescore-FAN.sh /tmp/fanout3-review-fx/fake-both/repo 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   rc2=${PIPESTATUS[0]}
   printf 'coordinated-fan-check-pipeline-rc=%d\ncoordinated-rescore-pipeline-rc=%d\n' "$rc1" "$rc2"
   ```

   Verbatim output:

   ```text
   real-untracked=src/acid/fanout/extra.clj
   forged-ls-files-bytes=0
   forged-ls-tree-extra-hits=1
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   rescore-FAN: worktree=/tmp/fanout3-review-fx/fake-both/repo n=21 base=07e4ec69e7f7a2fc0bcd2c782945fc07cb7251be fixtures=/tmp/fanout3-review-fx/gate-k/k6-a
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   coordinated-fan-check-pipeline-rc=0
   coordinated-rescore-pipeline-rc=0
   ```

   Required fix: obtain the base inventory through an authority the listing shim cannot counterfeit, or explicitly narrow and enforce the trusted-Git threat model. Add a causal ratchet in which `ls-files` omits an extra while `ls-tree` falsely includes it.

3. `bench/fanout/fan_check.clj:46-80,148-172,218-223`, `bench/fanout/sabotage-FAN.sh:498-575` — **BLOCKER: `walk-src` does not inventory directory symlink entries or entries it cannot classify, so successful-but-incomplete `ls-files` still false-PASSes.** `.isDirectory` follows links; only `.isFile` paths are recorded. A symlink to a nonempty directory therefore becomes child paths which disagree with Git's single symlink path. A symlink to an empty directory becomes no file path at all. With the exit-0/empty shim, the empty link disappears from both `changed-all` and `walked-src`, CHECK 6 reuses the incomplete walk, and the complete gate reports `6/6`.

   Exact command:

   ```bash
   mkdir -p /tmp/fanout3-review-fx/symlink-empty /tmp/fanout3-review-fx/symlink-empty/external-empty
   cp -a /tmp/fanout3-review-fx/gate-k/k6-rescore/good /tmp/fanout3-review-fx/symlink-empty/repo
   ln -s /tmp/fanout3-review-fx/symlink-empty/external-empty /tmp/fanout3-review-fx/symlink-empty/repo/src/linked-dir
   BASE=$(git -C /tmp/fanout3-review-fx/symlink-empty/repo rev-parse HEAD)
   printf 'symlink='; readlink /tmp/fanout3-review-fx/symlink-empty/repo/src/linked-dir
   printf 'stock-git-ls-files='; git -C /tmp/fanout3-review-fx/symlink-empty/repo ls-files --others --exclude-standard | rg 'src/linked-dir'
   git -C /tmp/fanout3-review-fx/symlink-empty/repo status --short | rg 'linked-dir'
   bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/symlink-empty/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'stock-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout3-review-fx/tip-incomplete/bin-empty:$PATH" bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/symlink-empty/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK [1-6]|^fan_check'
   printf 'shimmed-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout3-review-fx/tip-incomplete/bin-empty:$PATH" FAN_FIXTURES=/tmp/fanout3-review-fx/gate-k/k6-a FAN_BASE="$BASE" bash bench/fanout/rescore-FAN.sh /tmp/fanout3-review-fx/symlink-empty/repo 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   printf 'shimmed-rescore-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   symlink=/tmp/fanout3-review-fx/symlink-empty/external-empty
   stock-git-ls-files=src/linked-dir
   ?? src/linked-dir
   CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/linked-dir"]
   fan_check: FAILED CHECK 1 file-set
   stock-pipeline-rc=1
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   shimmed-pipeline-rc=0
   rescore-FAN: worktree=/tmp/fanout3-review-fx/symlink-empty/repo n=21 base=07e4ec69e7f7a2fc0bcd2c782945fc07cb7251be fixtures=/tmp/fanout3-review-fx/gate-k/k6-a
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   shimmed-rescore-pipeline-rc=0
   ```

   The nonempty-link control proves the walk follows the link while Git inventories the link itself.

   Exact command:

   ```bash
   BASE=$(git -C /tmp/fanout3-review-fx/symlink-nonempty/repo rev-parse HEAD)
   printf 'stock-git-path='; git -C /tmp/fanout3-review-fx/symlink-nonempty/repo ls-files --others --exclude-standard | rg 'linked-dir'
   PATH="/tmp/fanout3-review-fx/tip-incomplete/bin-empty:$PATH" bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/symlink-nonempty/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'nonempty-symlink-shimmed-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   stock-git-path=src/linked-dir
   CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/linked-dir/through-link.clj"] (present on disk, absent from git's untracked listing)
   nonempty-symlink-shimmed-pipeline-rc=1
   ```

   Required fix: enumerate with no-follow semantics, include symlinks as leaf entries exactly as Git does, refuse unknown/unclassifiable entries, and never recurse through a link. Add empty and nonempty directory-symlink ratchets under an incomplete `ls-files` shim.

4. `bench/fanout/fan_check.clj:69-80,148-152`, `bench/fanout/sabotage-FAN.sh:532-539` — **BLOCKER: `dirs-found` versus `dirs-entered` detects `chmod 000`, but not a directory which is readable and not searchable.** With mode `0400`, `.listFiles` returns names, so the directory is counted entered; `.isFile` and `.isDirectory` cannot stat the child, so that child is silently dropped. The measured counters are equal and `:pruned` is empty. Stock Git still lists the file and emits a warning, so the ordinary run fails closed; the exit-0/empty listing shim plus this walk blind spot reaches full `6/6`.

   Exact command for the count probe:

   ```bash
   D0=/tmp/fanout3-review-fx/nested-000/repo/src/probe/outer/blocked
   D4=/tmp/fanout3-review-fx/nested-400/repo/src/probe/outer/blocked
   chmod 000 "$D0"
   chmod 400 "$D4"
   trap 'chmod 755 "$D0" "$D4"' EXIT
   bb -e '(load-file "/tmp/fanout3-review-fx/walk-probe.clj") (prn :mode-000 (select-keys (fan-check/walk-src "/tmp/fanout3-review-fx/nested-000/repo") [:dirs-found :dirs-entered :pruned])) (prn :mode-400 (select-keys (fan-check/walk-src "/tmp/fanout3-review-fx/nested-400/repo") [:dirs-found :dirs-entered :pruned]))'
   chmod 755 "$D0" "$D4"
   trap - EXIT
   stat -c 'restored-mode=%a path=%n' "$D0" "$D4"
   ```

   Verbatim output:

   ```text
   :mode-000 {:dirs-found 6, :dirs-entered 5, :pruned ["src/probe/outer/blocked"]}
   :mode-400 {:dirs-found 6, :dirs-entered 6, :pruned []}
   restored-mode=755 path=/tmp/fanout3-review-fx/nested-000/repo/src/probe/outer/blocked
   restored-mode=755 path=/tmp/fanout3-review-fx/nested-400/repo/src/probe/outer/blocked
   ```

   Exact command for the tip false PASS:

   ```bash
   D=/tmp/fanout3-review-fx/nested-400/repo/src/probe/outer/blocked
   chmod 400 "$D"
   trap 'chmod 755 "$D"' EXIT
   BASE=$(git -C /tmp/fanout3-review-fx/nested-400/repo rev-parse HEAD)
   PATH="/tmp/fanout3-review-fx/tip-incomplete/bin-empty:$PATH" bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/nested-400/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK [1-6]|^fan_check'
   rc1=${PIPESTATUS[0]}
   PATH="/tmp/fanout3-review-fx/tip-incomplete/bin-empty:$PATH" FAN_FIXTURES=/tmp/fanout3-review-fx/gate-k/k6-a FAN_BASE="$BASE" bash bench/fanout/rescore-FAN.sh /tmp/fanout3-review-fx/nested-400/repo 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   rc2=${PIPESTATUS[0]}
   chmod 755 "$D"
   trap - EXIT
   printf 'mode-400-fan-check-pipeline-rc=%d\nmode-400-rescore-pipeline-rc=%d\n' "$rc1" "$rc2"
   stat -c 'restored-mode=%a path=%n' "$D"
   ```

   Verbatim output:

   ```text
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   rescore-FAN: worktree=/tmp/fanout3-review-fx/nested-400/repo n=21 base=07e4ec69e7f7a2fc0bcd2c782945fc07cb7251be fixtures=/tmp/fanout3-review-fx/gate-k/k6-a
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   mode-400-fan-check-pipeline-rc=0
   mode-400-rescore-pipeline-rc=0
   restored-mode=755 path=/tmp/fanout3-review-fx/nested-400/repo/src/probe/outer/blocked
   ```

   This is a stable regular file that the walk cannot classify; stock Git can see its name, but the successful-incomplete Git process in the stated threat model can omit it. Required fix: every name returned by a successful directory listing must produce exactly one classified inventory entry or a named error; equality of directory-entry counts alone is not a completeness proof. Add a `chmod 0400` nested-directory ratchet.

5. `bench/fanout/fan_check.clj:100-172`, `bench/fanout/sabotage-FAN.sh:373-575` — **the three round-two attacks are closed for ordinary regular files and `chmod 000`: empty output, a genuinely false-PASSing partial output, and stock Git's permission warning all change from false PASS at `bc6021f` to named nonzero errors at `f4975d0`.** The committed RED harness's own partial variant is not literally a false PASS at the old scorer: because it reveals one of two extras, old CHECK 1 returns ordinary `FAIL extras=1`. I therefore repeated the stronger partial attack which returns only a duplicate expected target and hides the actual extras; it false-PASSes `bc6021f` and is caught at the tip.

   Exact command and verbatim output for empty and `chmod 000` attacks:

   ```bash
   printf '%s\n' '--- bc6021f scorer / RED harness: empty listing ---'
   rg 'empty-output-shimmed: rc=|CHECK 1 file-set: PASS changed=21|full-gate empty-output-shimmed: rc=|rescore-FAN: 6/6|sabotage-FAN --selftest-incomplete' /tmp/fanout3-review-fx/red-incomplete.log
   printf '%s\n' '--- f4975d0: empty listing ---'
   rg 'empty-output-shimmed: rc=|CHECK 1 file-set: ERROR listing-incomplete unreported=2|full-gate empty-output-shimmed: rc=|rescore-FAN: FAILED|sabotage-FAN --selftest-incomplete' /tmp/fanout3-review-fx/tip-incomplete.log
   printf '%s\n' '--- bc6021f scorer / RED harness: chmod 000 ---'
   rg 'stock-git stderr:|fan_check: rc=|CHECK 1 file-set: PASS changed=21|full-gate: rc=|rescore-FAN: 6/6|sabotage-FAN --selftest-pruned' /tmp/fanout3-review-fx/red-pruned.log
   printf '%s\n' '--- f4975d0: chmod 000 ---'
   rg 'stock-git stderr:|fan_check: rc=|CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr|full-gate: rc=|rescore-FAN: FAILED|sabotage-FAN --selftest-pruned' /tmp/fanout3-review-fx/tip-pruned.log
   ```

   ```text
   --- bc6021f scorer / RED harness: empty listing ---
   SELFTEST-INCOMPLETE-LISTING empty-output-shimmed: rc=0
       CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   SELFTEST-INCOMPLETE-LISTING full-gate empty-output-shimmed: rc=0
       CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
       rescore-FAN: 6/6 checks passed
   sabotage-FAN --selftest-incomplete-listing: 1 passed, 3 failed
   --- f4975d0: empty listing ---
   SELFTEST-INCOMPLETE-LISTING empty-output-shimmed: rc=1
       CHECK 1 file-set: ERROR listing-incomplete unreported=2 ["src/acid/fanout/extra1.clj" "src/acid/fanout/extra2.clj"] (present on disk, absent from git's untracked listing)
   SELFTEST-INCOMPLETE-LISTING full-gate empty-output-shimmed: rc=1
       CHECK 1 file-set: ERROR listing-incomplete unreported=2 ["src/acid/fanout/extra1.clj" "src/acid/fanout/extra2.clj"] (present on disk, absent from git's untracked listing)
       rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   sabotage-FAN --selftest-incomplete-listing: 4 passed, 0 failed
   --- bc6021f scorer / RED harness: chmod 000 ---
   SELFTEST-PRUNED-WALK stock-git stderr: warning: could not open directory 'src/hidden/': Permission denied
   SELFTEST-PRUNED-WALK fan_check: rc=0
       CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   SELFTEST-PRUNED-WALK full-gate: rc=0
       CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
       rescore-FAN: 6/6 checks passed
   sabotage-FAN --selftest-pruned-walk: 0 passed, 2 failed
   --- f4975d0: chmod 000 ---
   SELFTEST-PRUNED-WALK stock-git stderr: warning: could not open directory 'src/hidden/': Permission denied
   SELFTEST-PRUNED-WALK fan_check: rc=1
       CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr: warning: could not open directory 'src/hidden/': Permission denied
   SELFTEST-PRUNED-WALK full-gate: rc=1
       CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr: warning: could not open directory 'src/hidden/': Permission denied
       rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   sabotage-FAN --selftest-pruned-walk: 2 passed, 0 failed
   ```

   Exact command and verbatim output for the stronger partial attack:

   ```bash
   chmod 755 /tmp/fanout3-review-fx/bin-partial-duplicate/git
   BASE=$(git -C /tmp/fanout3-review-fx/red-incomplete/repo rev-parse HEAD)
   PATH="/tmp/fanout3-review-fx/bin-partial-duplicate:$PATH" bb /tmp/fanout3-review-fx/export-bc6021f/bench/fanout/fan_check.clj /tmp/fanout3-review-fx/red-incomplete/repo /tmp/fanout3-review-fx/red-incomplete/gen/manifest-21.edn /tmp/fanout3-review-fx/red-incomplete/gen/canonical-21 "$BASE" 2>&1 | rg '^CHECK [1-6]|^fan_check'
   printf 'bc-partial-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout3-review-fx/bin-partial-duplicate:$PATH" bb /tmp/fanout3-review-fx/export-f4975d0/bench/fanout/fan_check.clj /tmp/fanout3-review-fx/red-incomplete/repo /tmp/fanout3-review-fx/red-incomplete/gen/manifest-21.edn /tmp/fanout3-review-fx/red-incomplete/gen/canonical-21 "$BASE" 2>&1 | rg '^CHECK [1-6]|^fan_check'
   printf 'tip-partial-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   ```text
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=102 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   bc-partial-pipeline-rc=0
   CHECK 1 file-set: ERROR listing-incomplete unreported=2 ["src/acid/fanout/extra1.clj" "src/acid/fanout/extra2.clj"] (present on disk, absent from git's untracked listing)
   tip-partial-pipeline-rc=1
   ```

6. `bench/fanout/fan_check.clj:46-80,218-228` — **the requested ordinary walk edge cases behave as intended, apart from findings 3 and 4.** A nested `chmod 000` directory produces the exact found/entered gap and a named error after Git stderr is hidden to force the walk guard. A `chmod 000` regular file is still inventoried (`unreadable-file-listed true`), Git lists it as an extra, and the scorer exits 1 when CHECK 6 tries to read it. It is a listing difference, not a pruned walk. The file error is an unhandled Babashka stack trace rather than a named CHECK error, but it is fail-closed. Every permission changed by the review was restored.

   Exact command and verbatim output for the nested directory:

   ```bash
   D=/tmp/fanout3-review-fx/nested-000/repo/src/probe/outer/blocked
   chmod 000 "$D"
   trap 'chmod 755 "$D"' EXIT
   BASE=$(git -C /tmp/fanout3-review-fx/nested-000/repo rev-parse HEAD)
   PATH="/tmp/fanout3-review-fx/bin-hide-lsfiles-stderr:$PATH" bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/nested-000/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   rc=${PIPESTATUS[0]}
   chmod 755 "$D"
   trap - EXIT
   printf 'nested-000-pipeline-rc=%d\n' "$rc"
   stat -c 'restored-mode=%a path=%n' "$D"
   ```

   ```text
   CHECK 1 file-set: ERROR listing-incomplete pruned dirs-found=6 dirs-entered=5 ["src/probe/outer/blocked"]
   nested-000-pipeline-rc=1
   restored-mode=755 path=/tmp/fanout3-review-fx/nested-000/repo/src/probe/outer/blocked
   ```

   Exact command and verbatim output for the unreadable file inventory:

   ```bash
   F=/tmp/fanout3-review-fx/unreadable-file/repo/src/probe/unreadable.clj
   chmod 000 "$F"
   trap 'chmod 644 "$F"' EXIT
   bb -e '(load-file "/tmp/fanout3-review-fx/walk-probe.clj") (let [w (fan-check/walk-src "/tmp/fanout3-review-fx/unreadable-file/repo")] (prn {:unreadable-file-listed (contains? (:files w) "src/probe/unreadable.clj") :dirs-found (:dirs-found w) :dirs-entered (:dirs-entered w) :pruned (:pruned w)}))'
   chmod 644 "$F"
   trap - EXIT
   stat -c 'restored-mode=%a path=%n' "$F"
   ```

   ```text
   {:unreadable-file-listed true, :dirs-found 4, :dirs-entered 4, :pruned []}
   restored-mode=644 path=/tmp/fanout3-review-fx/unreadable-file/repo/src/probe/unreadable.clj
   ```

   Exact scorer command:

   ```bash
   F=/tmp/fanout3-review-fx/unreadable-file/repo/src/probe/unreadable.clj
   chmod 000 "$F"
   trap 'chmod 644 "$F"' EXIT
   BASE=$(git -C /tmp/fanout3-review-fx/unreadable-file/repo rev-parse HEAD)
   printf 'stock-git-ls-files='
   git -C /tmp/fanout3-review-fx/unreadable-file/repo ls-files --others --exclude-standard 2>&1 | rg 'unreadable|warning' | tr '\n' ' '
   printf '\n'
   bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/unreadable-file/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" > /tmp/fanout3-review-fx/unreadable-file.out 2>&1
   rc=$?
   sed -n '1,16p' /tmp/fanout3-review-fx/unreadable-file.out
   chmod 644 "$F"
   trap - EXIT
   printf 'unreadable-file-rc=%d\n' "$rc"
   stat -c 'restored-mode=%a path=%n' "$F"
   ```

   ```text
   stock-git-ls-files=src/probe/unreadable.clj 
   CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/probe/unreadable.clj"]
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   ----- Error --------------------------------------------------------------------
   Type:     java.io.FileNotFoundException
   Message:  /tmp/fanout3-review-fx/unreadable-file/repo/src/probe/unreadable.clj (Permission denied)
   Location: /home/forge/tmp/sol/fanout-wt/bench/fanout/fan_check.clj:226:34

   ----- Context ------------------------------------------------------------------
   222:     (let [all-src (map #(io/file wt %)
   223:                         (filter #(re-find #"\.cljc?$" %) (:files walk)))
   224:           lib-re (re-pattern (str (str/replace (:lib (:old m)) "." "\\.") "(?![0-9A-Za-z_-])"))
   225:           lib-hits (for [f all-src
   226:                          :let [c (slurp f)]
                                       ^--- /tmp/fanout3-review-fx/unreadable-file/repo/src/probe/unreadable.clj (Permission denied)
   unreadable-file-rc=1
   restored-mode=644 path=/tmp/fanout3-review-fx/unreadable-file/repo/src/probe/unreadable.clj
   ```

   A case-insensitive collision is correctly caught even when Git is configured to hide it.

   Exact command:

   ```bash
   git -C /tmp/fanout3-review-fx/case-collision/repo config core.ignorecase true
   BASE=$(git -C /tmp/fanout3-review-fx/case-collision/repo rev-parse HEAD)
   printf 'core.ignorecase='; git -C /tmp/fanout3-review-fx/case-collision/repo config --get core.ignorecase
   printf 'git-ls-files-collision-hits='; git -C /tmp/fanout3-review-fx/case-collision/repo ls-files --others --exclude-standard | rg -i 'ns_003.clj' | wc -l
   bb bench/fanout/fan_check.clj /tmp/fanout3-review-fx/case-collision/repo /tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn /tmp/fanout3-review-fx/gate-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'ignorecase-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   ```text
   core.ignorecase=true
   git-ls-files-collision-hits=0
   CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/acid/fanout/NS_003.clj"] (present on disk, absent from git's untracked listing)
   ignorecase-pipeline-rc=1
   ```

7. `bench/fanout/fan_check.clj:62-68`, `bench/fanout/sabotage-FAN.sh:103-181` — **the backslash normalization is absent at the tip and the existing self-test remains causal.** The only relative-path expression is the raw POSIX `.relativize(...).toString()`; no `\` to `/` replacement remains. Reintroducing exactly that normalization in the scratch export makes the self-test fail on the two literal-backslash paths.

   Exact command:

   ```bash
   rg -n 'rel \(fn|relativize|str/replace.*\\\\|str/replace.*"/"|file-seq' /home/forge/tmp/sol/fanout-wt/bench/fanout/fan_check.clj
   cd /tmp/fanout3-review-fx/export-f4975d0
   bash bench/fanout/sabotage-FAN.sh --selftest-backslash 21 7 /tmp/fanout3-review-fx/backslash-mutant
   printf 'mutated-backslash-selftest-rc=%d\n' "$?"
   ```

   Verbatim output:

   ```text
   48:   (e.g. "src/acid/fanout/ns_003.clj"), by hand -- never `file-seq`, which
   66:        ;; `.relativize(...).toString()` already yields `/`-separated components
   68:        rel (fn [^java.io.File f] (.toString (.relativize wt-path (.toPath f))))]
   142:        ;; hand-rolled `walk-src` above -- never `file-seq` -- for "what src/
   221:    ;; second, unchecked `file-seq` call over src/ (Sol round-2 review, finding 4).
   224:          lib-re (re-pattern (str (str/replace (:lib (:old m)) "." "\\.") "(?![0-9A-Za-z_-])"))
   236:                                 want (re-pattern (str "\\[" (str/replace (:lib (:new m)) "." "\\.")
   237:                                                       "\\s+:as\\s+" (str/replace (:new-alias t) "-" "\\-")
   SELFTEST-BACKSLASH: CHECK 1 file-set: ERROR listing-incomplete vanished=2 ["src/acid/fanout/\\/ns_003.clj" "src/acid/fanout/\\/ns_005.clj"] (present at base, absent from the independent filesystem walk)
   SELFTEST-BACKSLASH: FAIL -- CHECK 1 misreads the backslash-named directory (want missing=0 extras=0)
   mutated-backslash-selftest-rc=1
   ```

8. `bench/fanout/fan_check.clj:40-173,218-223`, `bench/fanout/rescore-FAN.sh:38-50` — **seven-row listing-consumer accounting:** the named manifest error and all three Git path-listing exit/stderr guards work, the `chmod 000` walk guard works, and CHECK 6 really reuses the validated walk. Findings 2-4 show that the walk/base cross-check is not complete. The base resolver rejects an empty result, but it does not inspect `rev-list`'s exit code or stderr: a nonzero command which emits a SHA is accepted and the gate can report `6/6`.

   | # | Consumer | Current behavior |
   |---:|---|---|
   | 1 | Manifest `slurp` + EDN parse (`fan_check.clj:83-86`) | Named `CHECK 1 ... ERROR manifest unreadable`, rc 1. |
   | 2 | `git diff -z --name-only` (`fan_check.clj:100-112`) | Rejects nonzero exit and nonblank stderr. |
   | 3 | `git ls-files -z --others` (`fan_check.clj:118-126`) | Rejects nonzero exit and nonblank stderr; cross-check has findings 2-4. |
   | 4 | Java `walk-src` (`fan_check.clj:46-80,148-152`) | Rejects `.listFiles == nil`; misses symlink leaves and unclassifiable children. |
   | 5 | `git ls-tree -r --name-only -z` (`fan_check.clj:153-161`) | Rejects nonzero exit and nonblank stderr; same PATH authority can counterfeit the baseline. |
   | 6 | CHECK 6 source inventory (`fan_check.clj:218-223`) | No second listing; reuses row 4 and therefore inherits its blind spots. |
   | 7 | `git rev-list --max-parents=0` base fallback (`rescore-FAN.sh:38-42`) | Empty result rejected; exit and stderr suppressed/not checked. |

   Exact sabotage command:

   ```bash
   WT=/tmp/fanout3-review-fx/gate-k/k6-rescore/good
   MAN=/tmp/fanout3-review-fx/gate-k/k6-a/manifest-21.edn
   CAN=/tmp/fanout3-review-fx/gate-k/k6-a/canonical-21
   BASE=$(git -C "$WT" rev-parse HEAD)
   bb bench/fanout/fan_check.clj "$WT" /tmp/fanout3-review-fx/does-not-exist-manifest.edn "$CAN" "$BASE" 2>&1 | sed -n '1,4p'; printf 'manifest-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout3-review-fx/bin-diff-stderr:$PATH" bb bench/fanout/fan_check.clj "$WT" "$MAN" "$CAN" "$BASE" 2>&1 | sed -n '1,4p'; printf 'diff-stderr-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout3-review-fx/bin-lsfiles-stderr:$PATH" bb bench/fanout/fan_check.clj "$WT" "$MAN" "$CAN" "$BASE" 2>&1 | sed -n '1,4p'; printf 'lsfiles-stderr-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout3-review-fx/bin-lstree-stderr:$PATH" bb bench/fanout/fan_check.clj "$WT" "$MAN" "$CAN" "$BASE" 2>&1 | sed -n '1,4p'; printf 'lstree-stderr-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout3-review-fx/bin-revlist-empty:$PATH" env -u FAN_BASE bash bench/fanout/rescore-FAN.sh "$WT" 21 /tmp/fanout3-review-fx/gate-k/k6-a 2>&1 | sed -n '1,4p'; printf 'revlist-empty-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   CHECK 1 file-set: ERROR manifest unreadable: /tmp/fanout3-review-fx/does-not-exist-manifest.edn (No such file or directory)
   manifest-rc=1
   CHECK 1 file-set: ERROR listing-incomplete git diff stderr: simulated-diff-stderr
   diff-stderr-rc=1
   CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr: simulated-ls-files-stderr
   lsfiles-stderr-rc=1
   CHECK 1 file-set: ERROR listing-incomplete git ls-tree stderr: simulated-ls-tree-stderr
   lstree-stderr-rc=1
   rescore-FAN: FAIL cannot determine the base sha
   revlist-empty-rc=2
   ```

   CHECK 6's direct content sabotage is also causal in the plain gate below (`wrong-alias` and `one-site-missed`). The nested-000 output in finding 6 is the direct sabotage of the shared walk.

   Exact command demonstrating the base-resolution ceiling:

   ```bash
   chmod 755 /tmp/fanout3-review-fx/bin-revlist-nonzero-output/git
   WT=/tmp/fanout3-review-fx/gate-k/k6-rescore/good
   PATH="/tmp/fanout3-review-fx/bin-revlist-nonzero-output:$PATH" env -u FAN_BASE bash bench/fanout/rescore-FAN.sh "$WT" 21 /tmp/fanout3-review-fx/gate-k/k6-a 2>&1 | rg '^rescore-FAN|^CHECK [1-6]|^fan_check'
   printf 'revlist-nonzero-output-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   rescore-FAN: worktree=/tmp/fanout3-review-fx/gate-k/k6-rescore/good n=21 base=07e4ec69e7f7a2fc0bcd2c782945fc07cb7251be fixtures=/tmp/fanout3-review-fx/gate-k/k6-a
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   revlist-nonzero-output-pipeline-rc=0
   ```

9. `bench/fanout/sabotage-FAN.sh:1-575` — **all exposed tip gates pass, but they do not cover findings 2-4.** The request says “all seven self-test modes”; this tree exposes six, and `f4975d0`'s own commit message also says “all six self-test modes.” I ran all six plus the ordinary sabotage run, whose seven cases pass.

   Exact command identifying the available modes:

   ```bash
   rg -n '^if \[ "\$\{1:-\}" = "--selftest' bench/fanout/sabotage-FAN.sh
   ```

   Verbatim output:

   ```text
   41:if [ "${1:-}" = "--selftest-k" ]; then
   118:if [ "${1:-}" = "--selftest-backslash" ]; then
   200:if [ "${1:-}" = "--selftest-listing-failure" ]; then
   312:if [ "${1:-}" = "--selftest-whitespace-path" ]; then
   389:if [ "${1:-}" = "--selftest-incomplete-listing" ]; then
   511:if [ "${1:-}" = "--selftest-pruned-walk" ]; then
   ```

   Exact commands:

   ```bash
   bash bench/fanout/sabotage-FAN.sh --selftest-k 21 7 /tmp/fanout3-review-fx/gate-k
   bash bench/fanout/sabotage-FAN.sh --selftest-backslash 21 7 /tmp/fanout3-review-fx/gate-backslash
   bash bench/fanout/sabotage-FAN.sh --selftest-listing-failure 21 7 /tmp/fanout3-review-fx/gate-listing-failure
   bash bench/fanout/sabotage-FAN.sh --selftest-whitespace-path 21 7 /tmp/fanout3-review-fx/gate-whitespace
   bash bench/fanout/sabotage-FAN.sh --selftest-incomplete-listing 21 7 /tmp/fanout3-review-fx/tip-incomplete
   bash bench/fanout/sabotage-FAN.sh --selftest-pruned-walk 21 7 /tmp/fanout3-review-fx/tip-pruned
   bash bench/fanout/sabotage-FAN.sh /tmp/fanout3-review-fx/gate-k/k6-a 21 /tmp/fanout3-review-fx/gate-plain
   ```

   Verbatim output:

   ```text
   SELFTEST-K k=1 byte-identical: PASS two independent runs, repo-21 + canonical-21 + manifest-21.edn identical
   SELFTEST-K k=1: manifest :k=1 distinct-old-aliases=1 collisions=0 targets=21 histogram={"store" 21}
   SELFTEST-K k=1 witness: PASS distinct=1 collisions=0 targets=21
   SELFTEST-K k=1 rescore-FAN: PASS 6/6 on canonical-21
   SELFTEST-K k=2 byte-identical: PASS two independent runs, repo-21 + canonical-21 + manifest-21.edn identical
   SELFTEST-K k=2: manifest :k=2 distinct-old-aliases=2 collisions=10 targets=21 histogram={"st" 10, "store" 11}
   SELFTEST-K k=2 rescore-FAN: PASS 6/6 on canonical-21
   SELFTEST-K k=3 byte-identical: PASS two independent runs, repo-21 + canonical-21 + manifest-21.edn identical
   SELFTEST-K k=3: manifest :k=3 distinct-old-aliases=3 collisions=21 targets=21 histogram={"st" 7, "store" 7, "s" 7}
   SELFTEST-K k=3 witness: PASS distinct=3 targets=21
   SELFTEST-K k=3 rescore-FAN: PASS 6/6 on canonical-21
   SELFTEST-K k=6 byte-identical: PASS two independent runs, repo-21 + canonical-21 + manifest-21.edn identical
   SELFTEST-K k=6: manifest :k=6 distinct-old-aliases=6 collisions=30 targets=21 histogram={"st" 4, "db" 3, "s" 4, "store" 4, "repo" 3, "k" 3}
   SELFTEST-K k=6 witness: PASS distinct=6 targets=21 (today's shape)
   SELFTEST-K k=6 rescore-FAN: PASS 6/6 on canonical-21
   sabotage-FAN --selftest-k: 11 passed, 0 failed
   SELFTEST-BACKSLASH: CHECK 1 file-set: PASS changed=23 expected=23 missing=0 [] extras=0 []
   SELFTEST-BACKSLASH: PASS -- CHECK 1 correctly reads a directory literally named backslash (missing=0 extras=0)
   SELFTEST-LISTING-FAILURE control: CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/extra.clj"]
   SELFTEST-LISTING-FAILURE control real-git: PASS CHECK 1 correctly catches the untracked extra -- CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/extra.clj"]
   SELFTEST-LISTING-FAILURE ls-files-shimmed: rc=1
       CHECK 1 file-set: ERROR git ls-files exit=42 simulated-ls-files-failure
   SELFTEST-LISTING-FAILURE ls-files-shimmed fail-closed: PASS rc=1, no false PASS, named the ls-files exit
   SELFTEST-LISTING-FAILURE diff-shimmed: rc=1
       CHECK 1 file-set: FAIL git diff failed: simulated-diff-failure
   SELFTEST-LISTING-FAILURE diff-shimmed fail-closed: PASS rc=1, no false PASS, named the git diff failure
   SELFTEST-LISTING-FAILURE full-gate ls-files-shimmed: rc=1
       rescore-FAN: worktree=/tmp/fanout3-review-fx/gate-listing-failure/repo n=21 base=8dcdcfec6637fe102bfc591370c581aeb1360efb fixtures=/tmp/fanout3-review-fx/gate-listing-failure/gen
       CHECK 1 file-set: ERROR git ls-files exit=42 simulated-ls-files-failure
       CHECK 4 load: PASS namespaces=100 rc=0
       CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
       rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   SELFTEST-LISTING-FAILURE full-gate fail-closed: PASS rescore-FAN did not report 6/6 with a failing ls-files
   sabotage-FAN --selftest-listing-failure: 4 passed, 0 failed
   SELFTEST-WHITESPACE-PATH: CHECK 1 file-set: PASS changed=22 expected=22 missing=0 [] extras=0 []
   SELFTEST-WHITESPACE-PATH: PASS -- CHECK 1 correctly reads a legal path consisting solely of whitespace (missing=0 extras=0)
   SELFTEST-INCOMPLETE-LISTING control: CHECK 1 file-set: FAIL changed=23 expected=21 missing=0 [] extras=2 ["src/acid/fanout/extra1.clj" "src/acid/fanout/extra2.clj"]
   SELFTEST-INCOMPLETE-LISTING control real-git: PASS CHECK 1 correctly catches both untracked extras -- CHECK 1 file-set: FAIL changed=23 expected=21 missing=0 [] extras=2 ["src/acid/fanout/extra1.clj" "src/acid/fanout/extra2.clj"]
   SELFTEST-INCOMPLETE-LISTING empty-output-shimmed: rc=1
       CHECK 1 file-set: ERROR listing-incomplete unreported=2 ["src/acid/fanout/extra1.clj" "src/acid/fanout/extra2.clj"] (present on disk, absent from git's untracked listing)
   SELFTEST-INCOMPLETE-LISTING empty-output-shimmed fail-closed: PASS rc=1, no false PASS, named listing-incomplete
   SELFTEST-INCOMPLETE-LISTING partial-output-shimmed: rc=1
       CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/acid/fanout/extra2.clj"] (present on disk, absent from git's untracked listing)
   SELFTEST-INCOMPLETE-LISTING partial-output-shimmed fail-closed: PASS rc=1, no false PASS, named listing-incomplete
   SELFTEST-INCOMPLETE-LISTING full-gate empty-output-shimmed: rc=1
       rescore-FAN: worktree=/tmp/fanout3-review-fx/tip-incomplete/repo n=21 base=19a20c262aa78ad0477ed65130ef2cbdd6e2e20a fixtures=/tmp/fanout3-review-fx/tip-incomplete/gen
       CHECK 1 file-set: ERROR listing-incomplete unreported=2 ["src/acid/fanout/extra1.clj" "src/acid/fanout/extra2.clj"] (present on disk, absent from git's untracked listing)
       CHECK 4 load: PASS namespaces=100 rc=0
       CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
       rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   SELFTEST-INCOMPLETE-LISTING full-gate fail-closed: PASS rescore-FAN did not report 6/6 with an incomplete listing
   sabotage-FAN --selftest-incomplete-listing: 4 passed, 0 failed
   SELFTEST-PRUNED-WALK stock-git stderr: warning: could not open directory 'src/hidden/': Permission denied
   SELFTEST-PRUNED-WALK fan_check: rc=1
       CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr: warning: could not open directory 'src/hidden/': Permission denied
   SELFTEST-PRUNED-WALK fan_check fail-closed: PASS rc=1, no false PASS, named listing-incomplete
   SELFTEST-PRUNED-WALK full-gate: rc=1
       rescore-FAN: worktree=/tmp/fanout3-review-fx/tip-pruned/repo n=21 base=fa707f23388855e59496f15961eef2707378caf9 fixtures=/tmp/fanout3-review-fx/tip-pruned/gen
       CHECK 1 file-set: ERROR listing-incomplete git ls-files stderr: warning: could not open directory 'src/hidden/': Permission denied
       CHECK 4 load: PASS namespaces=100 rc=0
       CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
       rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   SELFTEST-PRUNED-WALK full-gate fail-closed: PASS rescore-FAN did not report 6/6 with an unreadable subdirectory
   sabotage-FAN --selftest-pruned-walk: 2 passed, 0 failed
   === positive control: the correct tree must be 6/6 GREEN ===
   POSITIVE CONTROL: GREEN 6/6
   === 1. wrong alias (policy says st2) ===
   SABOTAGE 1 wrong-alias: RED as designed -> CHECK 6 residue-and-alias: FAIL src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=1 ["src/acid/fanout/ns_003.clj"] shadowing=0 []
   === 2. one site missed ===
   SABOTAGE 2 one-site-missed: RED as designed -> CHECK 6 residue-and-alias: FAIL src-files=100 old-lib-hits=0 [] old-site-residue=1 ["src/acid/fanout/ns_003.clj"] wrong-or-missing-alias=0 [] shadowing=0 []
   === 3. one extra file touched (src/acid/fanout/ns_000.cljc) ===
   SABOTAGE 3 extra-file: RED as designed -> CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/ns_000.cljc"]
   === 4. corrupted docstring ===
   SABOTAGE 4 corrupted-docstring: RED as designed -> CHECK 3 protected-regions: FAIL regions=106 intact=105 manifest-sha-mismatch=0 damaged=1 [["src/acid/fanout/ns_003.clj" "docstring-token"]]
   === 5. reordered require ===
   SABOTAGE 5 reordered-require: RED as designed -> CHECK 2 form-equality: FAIL compared=21 equal=20 unparseable=0 [] unequal=1 ["src/acid/fanout/ns_003.clj"]
   === 6. unparseable file ===
   SABOTAGE 6 unparseable: RED as designed -> CHECK 2 form-equality: FAIL compared=21 equal=20 unparseable=1 [["src/acid/fanout/ns_003.clj" "Unexpected EOF. [at line 50, column 1]"]] unequal=0 []
   sabotage-FAN: 7 passed, 0 failed (1 positive control + 6 sabotages)
   ```

   Final permission and worktree audit, exact command:

   ```bash
   printf 'directories-without-owner-x:\n'
   find /tmp/fanout3-review-fx -type d ! -perm -u=x -print | sed -n '1,20p'
   printf 'files-without-owner-r:\n'
   find /tmp/fanout3-review-fx -type f ! -perm -u=r -print | sed -n '1,20p'
   printf 'head='; git rev-parse HEAD
   git status --short --branch
   ```

   Verbatim output:

   ```text
   directories-without-owner-x:
   files-without-owner-r:
   head=f4975d0b837e0526f9287c3c0a3a38118a3d9c51
   ## HEAD (no branch)
   ```

## NO-GO

This tip may not land on MCP/main until the base inventory cannot be counterfeited by the listing shim, the walk inventories symlink and unclassifiable directory entries fail-closed, and causal ratchets cover those false-`6/6` routes.
