## NO-GO

1. `bench/fanout/fan_check.clj:47-69`, `bench/fanout/sabotage-FAN.sh:180-296` — **BLOCKER: the exit-42 defect is fixed, but CHECK 1 still trusts a successful, incomplete `git ls-files` listing and can false-PASS the full gate.** The reviewed tree is the requested detached tip, and the only commits after `8ed5893` are the RED ratchets and GREEN implementation:

   Exact command:

   ```bash
   git status --short --branch
   git rev-parse HEAD
   git --version
   git log --oneline 8ed5893..bc6021f
   ```

   Verbatim output:

   ```text
   ## HEAD (no branch)
   bc6021f5d6bf92891f0f654b25340a80f45af404
   git version 2.53.0
   bc6021f5 bench/fanout: GREEN — fan_check.clj CHECK 1 fails closed on ls-files and reads empty? not blank?
   079edcb2 bench/fanout: RED — CHECK 1 false-PASSes a failing ls-files and misreads a whitespace-only path
   ```

   The round-one exit-42 injection reproduces as a false PASS and `6/6` at `8ed5893`; at `bc6021f`, it emits the required `ERROR`, exits nonzero, and propagates to `rescore-FAN: FAILED 1 group(s)`.

   Exact command:

   ```bash
   ROOT=/tmp/fanout2-review-fx/selftest-listing-failure
   OLD=/tmp/fanout2-review-fx/export-8ed5893
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   PATH="$ROOT/bin-ls-files:$PATH" bb "$OLD/bench/fanout/fan_check.clj" "$ROOT/repo" "$ROOT/gen/manifest-21.edn" "$ROOT/gen/canonical-21" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'old-fan-check-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="$ROOT/bin-ls-files:$PATH" FAN_FIXTURES="$ROOT/gen" FAN_BASE="$BASE" bash "$OLD/bench/fanout/rescore-FAN.sh" "$ROOT/repo" 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   printf 'old-rescore-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="$ROOT/bin-ls-files:$PATH" bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/gen/manifest-21.edn" "$ROOT/gen/canonical-21" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'tip-fan-check-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="$ROOT/bin-ls-files:$PATH" FAN_FIXTURES="$ROOT/gen" FAN_BASE="$BASE" bash bench/fanout/rescore-FAN.sh "$ROOT/repo" 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   printf 'tip-rescore-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   fan_check: 4/4 structural checks passed
   old-fan-check-pipeline-rc=0
   rescore-FAN: worktree=/tmp/fanout2-review-fx/selftest-listing-failure/repo n=21 base=79522d75ba808941c25c18178f15208192c56ff8 fixtures=/tmp/fanout2-review-fx/selftest-listing-failure/gen
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   old-rescore-pipeline-rc=0
   CHECK 1 file-set: ERROR git ls-files exit=42 simulated-ls-files-failure
   tip-fan-check-pipeline-rc=1
   rescore-FAN: worktree=/tmp/fanout2-review-fx/selftest-listing-failure/repo n=21 base=79522d75ba808941c25c18178f15208192c56ff8 fixtures=/tmp/fanout2-review-fx/selftest-listing-failure/gen
   CHECK 1 file-set: ERROR git ls-files exit=42 simulated-ls-files-failure
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   tip-rescore-pipeline-rc=1
   ```

   The requested lower-layer attack still false-PASSes. The shim exits 0 and writes nothing only for `ls-files`; the real listing is 26 bytes because `src/acid/fanout/extra.clj` exists untracked.

   PATH shim, verbatim:

   ```bash
   #!/usr/bin/env bash
   for arg in "$@"; do
     if [ "$arg" = ls-files ]; then exit 0; fi
   done
   exec /usr/bin/git "$@"
   ```

   Exact command:

   ```bash
   chmod +x /tmp/fanout2-review-fx/bin-ls-files-empty/git
   ROOT=/tmp/fanout2-review-fx/selftest-listing-failure
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   printf 'real listing bytes='
   git -C "$ROOT/repo" ls-files -z --others --exclude-standard | wc -c
   PATH="/tmp/fanout2-review-fx/bin-ls-files-empty:$PATH" git -C "$ROOT/repo" ls-files -z --others --exclude-standard
   printf 'shim-ls-files-rc=%d shim-listing-bytes=0\n' "$?"
   PATH="/tmp/fanout2-review-fx/bin-ls-files-empty:$PATH" bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/gen/manifest-21.edn" "$ROOT/gen/canonical-21" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'fan-check-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   PATH="/tmp/fanout2-review-fx/bin-ls-files-empty:$PATH" FAN_FIXTURES="$ROOT/gen" FAN_BASE="$BASE" bash bench/fanout/rescore-FAN.sh "$ROOT/repo" 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   printf 'rescore-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   real listing bytes=26
   shim-ls-files-rc=0 shim-listing-bytes=0
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   fan_check: 4/4 structural checks passed
   fan-check-pipeline-rc=0
   rescore-FAN: worktree=/tmp/fanout2-review-fx/selftest-listing-failure/repo n=21 base=79522d75ba808941c25c18178f15208192c56ff8 fixtures=/tmp/fanout2-review-fx/selftest-listing-failure/gen
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   rescore-pipeline-rc=0
   ```

   This is reachable with stock Git, not merely a lying shim. When an untracked subtree is unreadable, Git 2.53.0 prints a warning on stderr but returns 0 and an empty stdout listing. `fan_check.clj:57-60` checks only the exit code and ignores stderr on success. In the same case, `file-seq` at `fan_check.clj:116-117` silently prunes the unreadable `src/hidden` subtree, including an extra `.clj` containing forbidden `acid.fanout.store` residue. The complete gate therefore false-PASSes both CHECK 1 and CHECK 6.

   Exact command:

   ```bash
   mkdir -p /tmp/fanout2-review-fx/permission-src-real
   cp -a /tmp/fanout2-review-fx/selftest-k/k6-rescore/good /tmp/fanout2-review-fx/permission-src-real/repo
   mkdir -p /tmp/fanout2-review-fx/permission-src-real/repo/src/hidden
   printf '(ns hidden.extra)\n(require (quote acid.fanout.store))\n' > /tmp/fanout2-review-fx/permission-src-real/repo/src/hidden/extra.clj
   BASE=$(git -C /tmp/fanout2-review-fx/permission-src-real/repo rev-parse HEAD)
   chmod 000 /tmp/fanout2-review-fx/permission-src-real/repo/src/hidden
   GIT_OUT=$(git -C /tmp/fanout2-review-fx/permission-src-real/repo ls-files -z --others --exclude-standard 2> /tmp/fanout2-review-fx/permission-src-real/stderr.txt)
   GIT_RC=$?
   printf 'git-ls-files rc=%d stdout-bytes=%d stderr=' "$GIT_RC" "${#GIT_OUT}"
   sed -n '1p' /tmp/fanout2-review-fx/permission-src-real/stderr.txt
   FAN_FIXTURES=/tmp/fanout2-review-fx/selftest-k/k6-a FAN_BASE="$BASE" bash bench/fanout/rescore-FAN.sh /tmp/fanout2-review-fx/permission-src-real/repo 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   printf 'rescore-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   chmod 755 /tmp/fanout2-review-fx/permission-src-real/repo/src/hidden
   ```

   Verbatim output:

   ```text
   git-ls-files rc=0 stdout-bytes=0 stderr=warning: could not open directory 'src/hidden/': Permission denied
   rescore-FAN: worktree=/tmp/fanout2-review-fx/permission-src-real/repo n=21 base=4581d7544b8a0b8f0c3c02a12b7d8e8dbf69ff0f fixtures=/tmp/fanout2-review-fx/selftest-k/k6-a
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   rescore-pipeline-rc=0
   ```

   Other practical exit-0/empty routes are a PATH wrapper/instrumentation bug and a concurrent filesystem race. Ordinary “no untracked files” is legitimately exit 0/empty; Git built-in aliases cannot override `ls-files`. The demonstrated unreadable-directory warning is the concrete stock-Git failure mode. Required fix: reject nonempty stderr from both Git path listings (at minimum, this closes the demonstrated stock-Git case), make the `file-seq` walk surface unreadable/pruned directories as an error, and add a ratchet for exit 0 + empty stdout with a real untracked extra. If the contract must defend against a shim that silently lies without stderr, CHECK 1 needs an independent inventory rather than trusting the same listing process.

2. `bench/fanout/fan_check.clj:61-69`, `bench/fanout/sabotage-FAN.sh:299-369` — **the `empty?` fix is correct for Java/EDN-representable whitespace-only POSIX names.** The new self-test proves a single-space target is counted. NUL itself cannot occur in a POSIX pathname; the NUL-adjacent legal cases requested here, a filename consisting only of one tab and one newline, are both retained by the splitter and counted as extras.

   Exact command and verbatim output for the single-space target:

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-whitespace-path 21 7 /tmp/fanout2-review-fx/selftest-whitespace-path
   SELFTEST-WHITESPACE-PATH: CHECK 1 file-set: PASS changed=22 expected=22 missing=0 [] extras=0 []
   SELFTEST-WHITESPACE-PATH: PASS -- CHECK 1 correctly reads a legal path consisting solely of whitespace (missing=0 extras=0)
   ```

   Exact command:

   ```bash
   mkdir -p /tmp/fanout2-review-fx/odd-whitespace
   cp -a /tmp/fanout2-review-fx/selftest-k/k6-rescore/good /tmp/fanout2-review-fx/odd-whitespace/repo
   printf '(ns tab-only)\n' > /tmp/fanout2-review-fx/odd-whitespace/repo/$'\t'
   printf '(ns newline-only)\n' > /tmp/fanout2-review-fx/odd-whitespace/repo/$'\n'
   ROOT=/tmp/fanout2-review-fx/odd-whitespace/repo
   BASE=$(git -C "$ROOT" rev-parse HEAD)
   printf 'raw untracked paths: '
   while IFS= read -r -d '' p; do printf '<%q> ' "$p"; done < <(git -C "$ROOT" ls-files -z --others --exclude-standard)
   printf '\n'
   bb bench/fanout/fan_check.clj "$ROOT" /tmp/fanout2-review-fx/selftest-k/k6-a/manifest-21.edn /tmp/fanout2-review-fx/selftest-k/k6-a/canonical-21 "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf 'fan-check-pipeline-rc=%d\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   raw untracked paths: <$'\t'> <$'\n'> 
   CHECK 1 file-set: FAIL changed=23 expected=21 missing=0 [] extras=2 ["\t" "\n"]
   fan_check: FAILED CHECK 1 file-set
   fan-check-pipeline-rc=1
   ```

3. `bench/fanout/sabotage-FAN.sh:180-369`, `bench/fanout/fan_check.clj:47-69` — **both new self-tests are causal ratchets for their stated GREEN hunks, not decoration.** I used scratch exports of RED `079edcb` (which contains the self-tests but not the GREEN implementation) and ran each self-test unchanged. The listing test turns red in the exact two places the old false PASS propagates; the whitespace test turns red on the missing single-space owner.

   Exact command for the reverted listing fix:

   ```bash
   cd /tmp/fanout2-review-fx/red-listing
   bash bench/fanout/sabotage-FAN.sh --selftest-listing-failure 21 7 /tmp/fanout2-review-fx/mutation-listing
   printf 'selftest-rc=%d\n' "$?"
   ```

   Verbatim output:

   ```text
   SELFTEST-LISTING-FAILURE control: CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/extra.clj"]
   SELFTEST-LISTING-FAILURE control real-git: PASS CHECK 1 correctly catches the untracked extra -- CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/extra.clj"]
   SELFTEST-LISTING-FAILURE ls-files-shimmed: rc=0
       CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
       CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
       CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
       CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
       fan_check: 4/4 structural checks passed
   SELFTEST-LISTING-FAILURE ls-files-shimmed fail-closed: FAIL rc=0 -- want nonzero rc, an ERROR/FAIL line naming ls-files exit=42, and no PASS line
   SELFTEST-LISTING-FAILURE diff-shimmed: rc=1
       CHECK 1 file-set: FAIL git diff failed: simulated-diff-failure
   SELFTEST-LISTING-FAILURE diff-shimmed fail-closed: PASS rc=1, no false PASS, named the git diff failure
   SELFTEST-LISTING-FAILURE full-gate ls-files-shimmed: rc=0
       rescore-FAN: worktree=/tmp/fanout2-review-fx/mutation-listing/repo n=21 base=533a32ebb7df99255782e3e0992d4b52af076924 fixtures=/tmp/fanout2-review-fx/mutation-listing/gen
       CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
       CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
       CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
       CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
       fan_check: 4/4 structural checks passed
       CHECK 4 load: PASS namespaces=100 rc=0
       CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
       rescore-FAN: 6/6 checks passed
   SELFTEST-LISTING-FAILURE full-gate fail-closed: FAIL rescore-FAN reported 6/6 (or rc=0) with a failing ls-files -- false PASS reached the gate
   sabotage-FAN --selftest-listing-failure: 2 passed, 2 failed
   selftest-rc=1
   ```

   Exact command for the reverted whitespace fix:

   ```bash
   cd /tmp/fanout2-review-fx/red-whitespace
   bash bench/fanout/sabotage-FAN.sh --selftest-whitespace-path 21 7 /tmp/fanout2-review-fx/mutation-whitespace
   printf 'selftest-rc=%d\n' "$?"
   ```

   Verbatim output:

   ```text
   SELFTEST-WHITESPACE-PATH: CHECK 1 file-set: FAIL changed=21 expected=22 missing=1 [" "] extras=0 []
   SELFTEST-WHITESPACE-PATH: FAIL -- CHECK 1 misreads the whitespace-only path (want missing=0 extras=0)
   selftest-rc=1
   ```

   The listing ratchet is causal for nonzero exits, but finding 1 shows its coverage ceiling: it stays green at the tip while the success-with-incomplete-output class remains open.

4. `bench/fanout/fan_check.clj:47-69,114-152`, `bench/fanout/rescore-FAN.sh:38-42` — **listing inventory: not every consumed listing is fail-closed.** There is no shell `find` and no shell glob in the scorer. Manifest targets are direct paths, and CHECKS 4/5 execute fixed files. The four discovered listing/base-resolution sites are:

   | Consumer | Purpose and framing | Failure behavior at `bc6021f` | Fail-closed? |
   |---|---|---|---|
   | `git diff -z --name-only BASE` (`fan_check.clj:47`) | Tracked changed-path set, NUL framed | Nonzero exit is rejected at lines 48-50. Empty output makes all 21 expected targets missing. In the unreadable tracked-file attack, Git reports the path as changed/deleted, so CHECK 1 rejects it. | Yes for observed Git failures; arbitrary exit-0 selective omission is not independently authenticated. |
   | `git ls-files -z --others --exclude-standard` (`fan_check.clj:56`) | Untracked-path set, NUL framed | Nonzero exit is now rejected at lines 57-60. Exit 0 with a warning/incomplete stdout is consumed as authoritative. | **No — blocker, finding 1.** |
   | Java `file-seq src` (`fan_check.clj:116-117`) | CHECK 6 `.clj`/`.cljc` residue inventory | An unreadable descendant is silently pruned; no error or completeness witness is produced. | **No — blocker, finding 1.** |
   | `git rev-list --max-parents=0 HEAD` (`rescore-FAN.sh:41`) | Default base SHA, not a path listing | Empty result is rejected by `[ -n "$BASE" ]` at line 42; stderr is suppressed. | Yes for a missing result; not part of the path set. |
   | shell `find` / shell glob | None found in `fan_check.clj` or `rescore-FAN.sh` | Not applicable. | Not consumed. |

   Exact inventory command:

   ```bash
   rg -n 'gd \(sh|untracked \(sh|\(file-seq src\)|rev-list --max-parents' bench/fanout/fan_check.clj bench/fanout/rescore-FAN.sh
   ```

   Verbatim output:

   ```text
   bench/fanout/rescore-FAN.sh:41:else BASE=$(git -C "$WT" rev-list --max-parents=0 HEAD 2>/dev/null); fi
   bench/fanout/fan_check.clj:47:        gd (sh "git" "-C" wt "diff" "-z" "--name-only" base)
   bench/fanout/fan_check.clj:56:        untracked (sh "git" "-C" wt "ls-files" "-z" "--others" "--exclude-standard")
   bench/fanout/fan_check.clj:117:                          (file-seq src))
   ```

5. `bench/fanout/sabotage-FAN.sh:27-369,371-449` — **all requested tip gates pass verbatim, including the ordinary seven-case sabotage gate.** These establish no regression in the existing scorer and validate the two exact GREEN changes, but they do not override the false `6/6` in finding 1.

   Exact command and verbatim output:

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-k 21 7 /tmp/fanout2-review-fx/selftest-k
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
   ```

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-backslash 21 7 /tmp/fanout2-review-fx/selftest-backslash
   SELFTEST-BACKSLASH: CHECK 1 file-set: PASS changed=23 expected=23 missing=0 [] extras=0 []
   SELFTEST-BACKSLASH: PASS -- CHECK 1 correctly reads a directory literally named backslash (missing=0 extras=0)
   ```

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-listing-failure 21 7 /tmp/fanout2-review-fx/selftest-listing-failure
   SELFTEST-LISTING-FAILURE control: CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/extra.clj"]
   SELFTEST-LISTING-FAILURE control real-git: PASS CHECK 1 correctly catches the untracked extra -- CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/extra.clj"]
   SELFTEST-LISTING-FAILURE ls-files-shimmed: rc=1
       CHECK 1 file-set: ERROR git ls-files exit=42 simulated-ls-files-failure
   SELFTEST-LISTING-FAILURE ls-files-shimmed fail-closed: PASS rc=1, no false PASS, named the ls-files exit
   SELFTEST-LISTING-FAILURE diff-shimmed: rc=1
       CHECK 1 file-set: FAIL git diff failed: simulated-diff-failure
   SELFTEST-LISTING-FAILURE diff-shimmed fail-closed: PASS rc=1, no false PASS, named the git diff failure
   SELFTEST-LISTING-FAILURE full-gate ls-files-shimmed: rc=1
       rescore-FAN: worktree=/tmp/fanout2-review-fx/selftest-listing-failure/repo n=21 base=79522d75ba808941c25c18178f15208192c56ff8 fixtures=/tmp/fanout2-review-fx/selftest-listing-failure/gen
       CHECK 1 file-set: ERROR git ls-files exit=42 simulated-ls-files-failure
       CHECK 4 load: PASS namespaces=100 rc=0
       CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
       rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   SELFTEST-LISTING-FAILURE full-gate fail-closed: PASS rescore-FAN did not report 6/6 with a failing ls-files
   sabotage-FAN --selftest-listing-failure: 4 passed, 0 failed
   ```

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-whitespace-path 21 7 /tmp/fanout2-review-fx/selftest-whitespace-path
   SELFTEST-WHITESPACE-PATH: CHECK 1 file-set: PASS changed=22 expected=22 missing=0 [] extras=0 []
   SELFTEST-WHITESPACE-PATH: PASS -- CHECK 1 correctly reads a legal path consisting solely of whitespace (missing=0 extras=0)
   ```

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh /tmp/fanout2-review-fx/selftest-k/k6-a 21 /tmp/fanout2-review-fx/plain-sabotage
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

## NO-GO

This tip may not land on MCP/main until successful-but-incomplete Git listings and silently pruned filesystem walks fail the scorer closed and the stock-Git permission-warning case has a causal regression ratchet.
