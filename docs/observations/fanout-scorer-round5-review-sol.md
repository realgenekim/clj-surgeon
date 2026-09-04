## GO

1. `bench/fanout/rescore-FAN.sh:39-83`, `bench/fanout/fan_check.clj:68,183-203`, and `/home/forge/tmp/arms/eafford/gate-eafford.sh:15-17` — the round-four required fix is closed. The scorer validates the selected value with `^[0-9a-f]{40}$` after all three source branches, uses `FAN_GIT` or a fixed absolute candidate for fallback `rev-list`, checks its exit status, reports its stderr on failure, and prints `base-from=`. The only two residual-looking cases are trust-boundary cases, not false-PASS routes opened by the scorer: a missing arm `base.sha` can intentionally use the documented root-commit fallback and advertises that fact as `base-from=rev-list`; a caller that deliberately supplies an adversarial but valid commit has replaced the trusted baseline input itself.

   HEAD and the reviewed series were proved before testing. Exact command:

   ```bash
   cd /home/forge/tmp/sol/fanout-wt
   git status --short --branch
   git rev-parse HEAD
   git log --oneline a3a367c0..f2fa8be9
   ```

   Verbatim output:

   ```text
   ## HEAD (no branch)
   f2fa8be9bdaf3b39580aab0055265d323aa61339
   f2fa8be9 docs: document the walk's non-UTF-8 filename limit (finding 9, non-blocking)
   f5f003eb build: add make fanout-selftests, iterating the self-test roster as a gate
   6291f1c8 fix: widen the self-test roster grep to match elif and digit/underscore mode names
   296108d4 GREEN: rescore-FAN.sh validates the base sha as 40-hex on every branch, resolves git absolutely
   886668bc RED: sabotage-FAN --selftest-base-resolution witnesses finding 1 (fanout4-opus-review.md)
   ```

   The ratchet is causal at the named commits. Exact RED command:

   ```bash
   cd /var/tmp/forge/fanout5-review-fx/base-red-export
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   bash bench/fanout/sabotage-FAN.sh --selftest-base-resolution 21 7 /var/tmp/forge/fanout5-review-fx/base-red-run 2>&1 | rg '1a-short-sha: (rc|FAIL)|1a-literal-HEAD: (rc|FAIL)|1c-revlist-nonzero: (rc|FAIL)|1b-(shim-not-invoked|fail-closed): (FAIL|PASS)|sabotage-FAN --selftest-base-resolution'
   printf 'red-selftest-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output at `886668bc0d79409fcda66e3e6f5bfa448e6740d1`:

   ```text
   SELFTEST-BASE-RESOLUTION 1a-short-sha: rc=0
   SELFTEST-BASE-RESOLUTION 1a-short-sha: FAIL want the typed 40-hex refusal naming '143207f' and no 6/6, got rc=0
   SELFTEST-BASE-RESOLUTION 1a-literal-HEAD: rc=0
   SELFTEST-BASE-RESOLUTION 1a-literal-HEAD: FAIL want the typed 40-hex refusal naming 'HEAD' and no 6/6, got rc=0
   SELFTEST-BASE-RESOLUTION 1c-revlist-nonzero: rc=0
   SELFTEST-BASE-RESOLUTION 1c-revlist-nonzero: FAIL want a named refusal citing rev-list's failure and no 6/6, got rc=0
   SELFTEST-BASE-RESOLUTION 1b-shim-not-invoked: FAIL the PATH shim ran -- git is still being resolved through PATH
   SELFTEST-BASE-RESOLUTION 1b-fail-closed: FAIL want rc!=0, no 6/6, a named CHECK 1 FAIL/ERROR -- got rc=0
   sabotage-FAN --selftest-base-resolution: 0 passed, 7 failed
   red-selftest-rc=1
   ```

   Exact GREEN command:

   ```bash
   cd /var/tmp/forge/fanout5-review-fx/base-green-export
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   bash bench/fanout/sabotage-FAN.sh --selftest-base-resolution 21 7 /var/tmp/forge/fanout5-review-fx/base-green-run 2>&1 | rg '1a-short-sha: (rc|PASS)|1a-literal-HEAD: (rc|PASS)|1c-revlist-nonzero: (rc|PASS)|1b-(shim-not-invoked|fail-closed): (FAIL|PASS)|sabotage-FAN --selftest-base-resolution'
   printf 'green-selftest-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output at `296108d4b8fc872d0d8e4a8dbfeacb4dbfe9e091`:

   ```text
   SELFTEST-BASE-RESOLUTION 1a-short-sha: rc=2
   SELFTEST-BASE-RESOLUTION 1a-short-sha: PASS a 7-char FAN_BASE is refused by name, exit=2
   SELFTEST-BASE-RESOLUTION 1a-literal-HEAD: rc=2
   SELFTEST-BASE-RESOLUTION 1a-literal-HEAD: PASS the literal HEAD is refused by name, exit=2
   SELFTEST-BASE-RESOLUTION 1c-revlist-nonzero: rc=2
   SELFTEST-BASE-RESOLUTION 1c-revlist-nonzero: PASS a failing rev-list is refused by name, exit=2, no 6/6
   SELFTEST-BASE-RESOLUTION 1b-shim-not-invoked: PASS the PATH shim never ran -- git was resolved absolutely, never through PATH
   SELFTEST-BASE-RESOLUTION 1b-fail-closed: PASS rc=1, no 6/6, the honest base was scored and the planted file was caught
   sabotage-FAN --selftest-base-resolution: 7 passed, 0 failed
   green-selftest-rc=0
   ```

   Fresh 1a–1c/1b reproduction at the reviewed tip used a new generated fixture. Exact command:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   bash bench/fanout/sabotage-FAN.sh --selftest-base-resolution 21 7 /var/tmp/forge/fanout5-review-fx/base-resolution-report 2>&1 | rg '1a-short-sha: (rc|PASS)|1a-literal-HEAD: (rc|PASS)|1c-revlist-nonzero: (rc|PASS)|1b-path-shim-crafted-base: rc|base-from=rev-list|CHECK 1 file-set: ERROR listing-incomplete|1b-(shim-not-invoked|fail-closed): PASS|sabotage-FAN --selftest-base-resolution'
   printf 'base-resolution-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   SELFTEST-BASE-RESOLUTION 1a-short-sha: rc=2
   SELFTEST-BASE-RESOLUTION 1a-short-sha: PASS a 7-char FAN_BASE is refused by name, exit=2
   SELFTEST-BASE-RESOLUTION 1a-literal-HEAD: rc=2
   SELFTEST-BASE-RESOLUTION 1a-literal-HEAD: PASS the literal HEAD is refused by name, exit=2
   SELFTEST-BASE-RESOLUTION 1c-revlist-nonzero: rc=2
   SELFTEST-BASE-RESOLUTION 1c-revlist-nonzero: PASS a failing rev-list is refused by name, exit=2, no 6/6
   SELFTEST-BASE-RESOLUTION 1b-path-shim-crafted-base: rc=1
       rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/base-resolution-report/case1b n=21 base=dd75b8348af6c8cc3deefbbcc982cb2a783b152c base-from=rev-list fixtures=/var/tmp/forge/fanout5-review-fx/base-resolution-report/gen
       CHECK 1 file-set: ERROR listing-incomplete unreported=1 ["src/acid/fanout/extra.clj"] (present on disk, absent from git's untracked listing)
   SELFTEST-BASE-RESOLUTION 1b-shim-not-invoked: PASS the PATH shim never ran -- git was resolved absolutely, never through PATH
   SELFTEST-BASE-RESOLUTION 1b-fail-closed: PASS rc=1, no 6/6, the honest base was scored and the planted file was caught
   sabotage-FAN --selftest-base-resolution: 7 passed, 0 failed
   base-resolution-rc=0
   ```

   A successful empty `rev-list` and a failing `rev-list` both refuse at exit 2; the failing command's stderr is preserved. Exact command:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   ROOT=/var/tmp/forge/fanout5-review-fx
   WT="$ROOT/base-resolution/case1c"
   FAN_FIXTURES="$ROOT/base-resolution/gen" FAN_GIT="$ROOT/bin-revlist-empty/git" bash bench/fanout/rescore-FAN.sh "$WT" 21 2>&1
   printf 'empty-rev-list-rc=%s\n' "$?"
   FAN_FIXTURES="$ROOT/base-resolution/gen" FAN_GIT="$ROOT/base-resolution/bin-revlist-nonzero/git" bash bench/fanout/rescore-FAN.sh "$WT" 21 2>&1
   printf 'failing-rev-list-rc=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   rescore-FAN: FAIL base must be a 40-hex sha (got <empty>)
   empty-rev-list-rc=2
   rescore-FAN: FAIL base must be a 40-hex sha (got <rev-list exit=7 stderr=simulated-rev-list-failure>)
   failing-rev-list-rc=2
   ```

   The `base.sha` branch intentionally removes whitespace before applying the common lower-case 40-hex check. Thus the normal newline, a trailing space, and CRLF all normalize to the same valid SHA. Exact command:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   ROOT=/var/tmp/forge/fanout5-review-fx
   REAL_BASE=$(git -C "$ROOT/base-resolution/ctrlB/worktree" rev-parse HEAD)
   for kind in newline space crlf; do
     D="$ROOT/base-$kind"
     cp -a "$ROOT/base-resolution/ctrlB" "$D"
     case "$kind" in
       newline) printf '%s\n' "$REAL_BASE" > "$D/base.sha" ;;
       space) printf '%s \n' "$REAL_BASE" > "$D/base.sha" ;;
       crlf) printf '%s\r\n' "$REAL_BASE" > "$D/base.sha" ;;
     esac
     OUT=$(FAN_FIXTURES="$ROOT/base-resolution/gen" bash bench/fanout/rescore-FAN.sh "$D/worktree" 21 2>&1)
     RC=$?
     printf '%s\n' "variant=$kind"
     printf '%s\n' "$OUT" | rg '^rescore-FAN:'
     printf 'variant-rc=%s\n' "$RC"
   done
   ```

   Verbatim output:

   ```text
   variant=newline
   rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/base-newline/worktree n=21 base=e848caf5219ea564856a9a7fbe4a0c872496731a base-from=base.sha fixtures=/var/tmp/forge/fanout5-review-fx/base-resolution/gen
   rescore-FAN: 6/6 checks passed
   variant-rc=0
   variant=space
   rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/base-space/worktree n=21 base=e848caf5219ea564856a9a7fbe4a0c872496731a base-from=base.sha fixtures=/var/tmp/forge/fanout5-review-fx/base-resolution/gen
   rescore-FAN: 6/6 checks passed
   variant-rc=0
   variant=crlf
   rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/base-crlf/worktree n=21 base=e848caf5219ea564856a9a7fbe4a0c872496731a base-from=base.sha fixtures=/var/tmp/forge/fanout5-review-fx/base-resolution/gen
   rescore-FAN: 6/6 checks passed
   variant-rc=0
   ```

   Uppercase hexadecimal is refused because the promised and implemented canonical form is lower-case (`[0-9a-f]{40}`), matching the exact round-four requested guard. Exact command and verbatim output:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   ROOT=/var/tmp/forge/fanout5-review-fx
   WT="$ROOT/base-resolution/ctrlA"
   BASE=$(git -C "$WT" rev-parse HEAD)
   UPPER=$(printf '%s' "$BASE" | tr '[:lower:]' '[:upper:]')
   printf 'uppercase-base=%s\n' "$UPPER"
   FAN_FIXTURES="$ROOT/base-resolution/gen" FAN_BASE="$UPPER" bash bench/fanout/rescore-FAN.sh "$WT" 21 2>&1
   printf 'uppercase-base-rc=%s\n' "$?"
   ```

   ```text
   uppercase-base=E848CAF5219EA564856A9A7FBE4A0C872496731A
   rescore-FAN: FAIL base must be a 40-hex sha (got E848CAF5219EA564856A9A7FBE4A0C872496731A)
   uppercase-base-rc=2
   ```

   A lower-case 40-hex object that is not a commit passes the Bash shape check but is refused by the structural scorer. Exact command and verbatim output:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   ROOT=/var/tmp/forge/fanout5-review-fx
   WT="$ROOT/base-resolution/ctrlA"
   BLOB=$(printf 'not a commit\n' | git -C "$WT" hash-object -w --stdin)
   printf 'unrelated-object=%s type=%s\n' "$BLOB" "$(git -C "$WT" cat-file -t "$BLOB")"
   FAN_FIXTURES="$ROOT/base-resolution/gen" FAN_BASE="$BLOB" bash bench/fanout/rescore-FAN.sh "$WT" 21 2>&1 | rg '^rescore-FAN:|^CHECK 1'
   printf 'noncommit-base-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   ```text
   unrelated-object=90db16de6c0119c0c924c80d206b1e80bc3d2331 type=blob
   rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/base-resolution/ctrlA n=21 base=90db16de6c0119c0c924c80d206b1e80bc3d2331 base-from=FAN_BASE fixtures=/var/tmp/forge/fanout5-review-fx/base-resolution/gen
   CHECK 1 file-set: FAIL git diff failed: usage: git diff [<options>] [<commit>] [--] [<path>...]
   rescore-FAN: FAILED 1 group(s): structural(1,2,3,6)
   noncommit-base-rc=1
   ```

   The direct crafted-commit attack does reach 6/6. Exact command and verbatim output:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   ROOT=/var/tmp/forge/fanout5-review-fx
   WT="$ROOT/base-resolution/case1b"
   HONEST=$(git -C "$WT" rev-list --max-parents=0 HEAD)
   TREE=$(git -C "$WT" write-tree)
   CRAFTED=$(git -C "$WT" commit-tree "$TREE" -p "$HONEST" -m 'direct crafted base')
   printf 'honest-base=%s crafted-base=%s resolved=%s\n' "$HONEST" "$CRAFTED" "$(git -C "$WT" rev-parse --verify "$CRAFTED^{commit}")"
   FAN_FIXTURES="$ROOT/base-resolution/gen" FAN_BASE="$CRAFTED" bash bench/fanout/rescore-FAN.sh "$WT" 21 2>&1 | rg '^rescore-FAN:|^CHECK 1|^CHECK 6|^fan_check:'
   printf 'direct-crafted-base-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   ```text
   honest-base=36601cc56feb0a33b4acb8a2afbefbd286a3f61c crafted-base=ae1821a849707f2abdb1408174064127a4d7edd6 resolved=ae1821a849707f2abdb1408174064127a4d7edd6
   rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/base-resolution/case1b n=21 base=ae1821a849707f2abdb1408174064127a4d7edd6 base-from=FAN_BASE fixtures=/var/tmp/forge/fanout5-review-fx/base-resolution/gen
   fan_check: git=/usr/bin/git exec-path=/usr/lib/git-core version=git version 2.53.0 resolution=absolute-candidate
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   rescore-FAN: 6/6 checks passed
   direct-crafted-base-rc=0
   ```

   This is out of the scorer's stated adversary boundary, not a regression in the fix. `fan_check.clj:68` explicitly defines the base SHA as “an INPUT — from the caller/fixtures,” and the cohort contract at `docs/observations/2026-09-04-e3-e6-prestaged.md:184` defines it as the pinned SHA from which the worktree was cut. The guard at `fan_check.clj:183-203` proves that Git resolves the supplied 40-hex address to that same commit and that the returned object bytes hash to it; it cannot prove that the trusted caller selected the right commit. Authenticating `FAN_BASE` would require an independent pin or attestation and is a different contract. The former vulnerability did not require replacement of that trusted input: an untrusted PATH binary silently substituted it.

   Case 1d does not exit 2 when only the copied arm's `base.sha` is absent: the documented third branch obtains the same root commit, and the header now makes that fallback unambiguous. The shell redirection order also means the missing-file diagnostic is visible despite the apparent `2>/dev/null`. Exact command and verbatim output:

   ```bash
   cp -a /var/tmp/forge/fanout5-review-fx/published-copy /var/tmp/forge/fanout5-review-fx/missing-base
   mv /var/tmp/forge/fanout5-review-fx/missing-base/base.sha /var/tmp/forge/fanout5-review-fx/missing-base/base.sha.removed
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   A=/var/tmp/forge/fanout5-review-fx/missing-base
   FAN_FIXTURES=/home/forge/tmp/arms/ereg/fanout-k1 FAN_BASE="$(tr -d '[:space:]' < "$A/base.sha" 2>/dev/null)" bash bench/fanout/rescore-FAN.sh "$A/worktree" 21 2>&1 | rg '^rescore-FAN:|^CHECK 1'
   printf 'missing-base-gate-form-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   ```text
   /bin/bash: line 5: /var/tmp/forge/fanout5-review-fx/missing-base/base.sha: No such file or directory
   rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/missing-base/worktree n=21 base=65fe39a9071083f478ed091ab64ebdf05c02abbd base-from=rev-list fixtures=/home/forge/tmp/arms/ereg/fanout-k1
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   rescore-FAN: 6/6 checks passed
   missing-base-gate-form-rc=0
   ```

   So the precise answer is: the gate is loud about the missing file and the scorer is loud about provenance, but it does not refuse when the safe fallback successfully supplies a valid base. That is consistent with `rescore-FAN.sh:10` and with round four's expressly allowed “if the fallback stays” resolution; it no longer creates the PATH-controlled false 6/6.

2. `bench/fanout/sabotage-FAN.sh:33-50,1149-1237` — prior finding 10 is closed, and the closure is causal against `296108d4`. The widened expression recognizes `if`/`elif` and `[A-Za-z0-9_-]`; the content-verified injection test proves both roster inclusion and real dispatch.

   Exact pre-fix command, using a content export of `296108d4` with only the requested `elif --selftest-inj3ct_2` branch injected:

   ```bash
   RED=/var/tmp/forge/fanout5-review-fx/roster-red/bench/fanout/sabotage-FAN.sh
   printf 'source-commit=%s\n' "$(git rev-parse 296108d4)"
   bash "$RED" --selftest-list | head -1
   bash "$RED" --selftest-inj3ct_2
   printf 'injected-mode-rc=%s\n' "$?"
   ```

   Verbatim output:

   ```text
   source-commit=296108d4b8fc872d0d8e4a8dbfeacb4dbfe9e091
   sabotage-FAN: self-test modes (10): --selftest-k --selftest-backslash --selftest-listing-failure --selftest-whitespace-path --selftest-incomplete-listing --selftest-pruned-walk --selftest-shimmed-baseline --selftest-symlink-entries --selftest-unsearchable-dir --selftest-base-resolution 
   SELFTEST-ROSTER-INJECTION: the injected elif mode ran
   injected-mode-rc=0
   ```

   Exact reviewed-tip command:

   ```bash
   bash bench/fanout/sabotage-FAN.sh --selftest-roster-injection /var/tmp/forge/fanout5-review-fx/roster-green | tee /var/tmp/forge/fanout5-review-fx/roster-green.out
   printf 'roster-green-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   SELFTEST-ROSTER-INJECTION export-sha256=1c118e284867e9d45c1081f10db07c9099d07f6e2ec66f422558132da33ec0a9 source-sha256=1c118e284867e9d45c1081f10db07c9099d07f6e2ec66f422558132da33ec0a9
   SELFTEST-ROSTER-INJECTION export-verified: PASS scratch copy content-identical to the running script
   SELFTEST-ROSTER-INJECTION before: sabotage-FAN: self-test modes (11): --selftest-k --selftest-backslash --selftest-listing-failure --selftest-whitespace-path --selftest-incomplete-listing --selftest-pruned-walk --selftest-shimmed-baseline --selftest-symlink-entries --selftest-unsearchable-dir --selftest-base-resolution --selftest-roster-injection 
   SELFTEST-ROSTER-INJECTION after: sabotage-FAN: self-test modes (12): --selftest-inj3ct_2 --selftest-k --selftest-backslash --selftest-listing-failure --selftest-whitespace-path --selftest-incomplete-listing --selftest-pruned-walk --selftest-shimmed-baseline --selftest-symlink-entries --selftest-unsearchable-dir --selftest-base-resolution --selftest-roster-injection 
   SELFTEST-ROSTER-INJECTION roster-count-rises: PASS roster went from 11 to 12 and names --selftest-inj3ct_2
   SELFTEST-ROSTER-INJECTION injected-mode-dispatchable: PASS running --selftest-inj3ct_2 actually executes the injected elif branch
   sabotage-FAN --selftest-roster-injection: 3 passed, 0 failed
   roster-green-rc=0
   ```

   Prior finding 9 was also handled exactly as promised: `bench/fanout/fan_check.clj:245-260` documents the non-UTF-8 filename limitation and expressly preserves its fail-closed status; `f2fa8be9` changes only that docstring.

3. `Makefile:55,871-890` — prior finding 11 is closed. `fanout-selftests` takes its list from `--selftest-list`, runs every listed token, records each return code, and exits non-zero if any mode does. Because the repository's historical self-tests contain default paths outside this review's mandated fixture root, the exact gate ran in a content export of `f2fa8be9` whose only changes were the eleven default scratch-directory literals; the Makefile and all test logic were unchanged. The reviewed clone itself remained untouched.

   The unpiped gate was run exactly as required:

   ```bash
   make fanout-selftests
   ```

   It returned 0. This exact evidence command prints the gate's verbatim per-mode result lines and preserves Make's return code:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   make fanout-selftests 2>&1 | rg '^=== fanout-selftests: --selftest-.* PASS ===$|^fanout-selftests:'
   printf 'make-fanout-selftests-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   === fanout-selftests: --selftest-k PASS ===
   === fanout-selftests: --selftest-backslash PASS ===
   === fanout-selftests: --selftest-listing-failure PASS ===
   === fanout-selftests: --selftest-whitespace-path PASS ===
   === fanout-selftests: --selftest-incomplete-listing PASS ===
   === fanout-selftests: --selftest-pruned-walk PASS ===
   === fanout-selftests: --selftest-shimmed-baseline PASS ===
   === fanout-selftests: --selftest-symlink-entries PASS ===
   === fanout-selftests: --selftest-unsearchable-dir PASS ===
   === fanout-selftests: --selftest-base-resolution PASS ===
   === fanout-selftests: --selftest-roster-injection PASS ===
   fanout-selftests: all 11 modes passed
   make-fanout-selftests-rc=0
   ```

   A scratch export then forced the listed `--selftest-backslash` mode to print one line and exit 1. Exact command:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   make fanout-selftests 2>&1 | rg 'SABOTAGE|fanout-selftests: --selftest-backslash|fanout-selftests: FAILED'
   printf 'sabotaged-make-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   === fanout-selftests: --selftest-backslash ===
   SABOTAGE: forced --selftest-backslash exit 1
   === fanout-selftests: --selftest-backslash FAIL (rc=1) ===
   fanout-selftests: FAILED (11 modes run)
   sabotaged-make-rc=2
   ```

   Make maps the recipe's exit 1 to its own exit 2, which is still the required non-zero failure. It also ran all eleven modes instead of stopping after the sabotaged one.

4. `bench/fanout/rescore-FAN.sh:83-129` and `bench/fanout/sabotage-FAN.sh:1239-1328` — retroactive scoring and both requested gates are green. The published `/home/forge/tmp/arms/eafford/eafford-T-T-1` was copied read-only into the permitted fixture root; no command wrote under `/home/forge/tmp/arms`.

   Exact rescore command:

   ```bash
   FAN_FIXTURES=/home/forge/tmp/arms/ereg/fanout-k1 FAN_BASE="$(tr -d '[:space:]' < /var/tmp/forge/fanout5-review-fx/published-copy/base.sha)" bash bench/fanout/rescore-FAN.sh /var/tmp/forge/fanout5-review-fx/published-copy/worktree 21 2>&1 | tee /var/tmp/forge/fanout5-review-fx/published-rescore.out
   printf 'published-rescore-rc=%s\n' "${PIPESTATUS[0]}"
   ```

   Verbatim output:

   ```text
   rescore-FAN: worktree=/var/tmp/forge/fanout5-review-fx/published-copy/worktree n=21 base=65fe39a9071083f478ed091ab64ebdf05c02abbd base-from=FAN_BASE fixtures=/home/forge/tmp/arms/ereg/fanout-k1
   fan_check: git=/usr/bin/git exec-path=/usr/lib/git-core version=git version 2.53.0 resolution=absolute-candidate
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=100 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   published-rescore-rc=0
   ```

   Exact plain sabotage gate:

   ```bash
   export TMPDIR=/var/tmp/forge/fanout5-review-fx/tmp
   bash bench/fanout/sabotage-FAN.sh /home/forge/tmp/arms/ereg/fanout-k1 21 /var/tmp/forge/fanout5-review-fx/plain-sabotage
   ```

   Verbatim output:

   ```text
   On branch main
   nothing to commit, working tree clean
   === positive control: the correct tree must be 6/6 GREEN ===
   POSITIVE CONTROL: GREEN 6/6
   === 1. wrong alias (policy says store2) ===
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
   SABOTAGE 6 unparseable: RED as designed -> CHECK 2 form-equality: FAIL compared=21 equal=20 unparseable=1 [["src/acid/fanout/ns_003.clj" "Unexpected EOF. [at line 49, column 1]"]] unequal=0 []
   sabotage-FAN: 7 passed, 0 failed (1 positive control + 6 sabotages)
   ```

   Final audit exact command:

   ```bash
   find /var/tmp/forge/fanout5-review-fx -type d ! -perm -u=x -print
   find /var/tmp/forge/fanout5-review-fx -type f ! -perm -u=r -print
   find /var/tmp/forge/fanout5-review-fx -depth -delete
   if [ -e /var/tmp/forge/fanout5-review-fx ]; then printf '%s\n' fixture-root-still-exists; else printf '%s\n' fixture-root-removed; fi
   git rev-parse HEAD
   git status --short --branch
   git -C /home/forge/tmp/arms/eafford/eafford-T-T-1/worktree status --short | wc -l
   ```

   Verbatim output (the two permission searches were empty; the published arm retained its original 21-file worktree state):

   ```text
   fixture-root-removed
   f2fa8be9bdaf3b39580aab0055265d323aa61339
   ## HEAD (no branch)
   21
   ```

## GO

`f2fa8be9` may land on MCP/main: the round-four blocker is closed, the two non-blocking harness findings have causal ratchets, all eleven roster-derived self-tests and the seven-case sabotage gate pass, and the published arm remains 6/6 under the fixed scorer.
