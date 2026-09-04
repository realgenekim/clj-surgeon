## NO-GO

1. `bench/fanout/fan_check.clj:48-55` — BLOCKER: `git ls-files` is not fail-closed, so an untracked extra can disappear and the full gate can false-PASS. Only `gd`'s exit is checked at lines 49-51; `untracked` is parsed regardless of `(:exit untracked)`. I fault-injected exit 42 only for `ls-files` while leaving `git diff` real and planted `src/acid/fanout/extra.clj`. With real git CHECK 1 catches it; with the failing subprocess, both the old scorer and this tip report structural success, and `rescore-FAN.sh` prints 6/6. This violates the file header's “cannot read is a FAIL” contract and is sufficient for NO-GO.

   Exact command:

   ```bash
   sed -n '43,55p' bench/fanout/fan_check.clj
   sed -n '1,20p' /tmp/fanout-review-fx/ls-files-error-1/bin/git
   ROOT=/tmp/fanout-review-fx/ls-files-error-1
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   FIX=/tmp/fanout-review-fx/selftest-k/k6-a
   printf '%s\n' 'real git:'
   bb bench/fanout/fan_check.clj "$ROOT/repo" "$FIX/manifest-21.edn" "$FIX/canonical-21" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf '%s\n' 'git wrapper exits 42 for ls-files:'
   PATH="$ROOT/bin:$PATH" bb bench/fanout/fan_check.clj "$ROOT/repo" "$FIX/manifest-21.edn" "$FIX/canonical-21" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   printf '%s\n' 'full six-check gate with that failing ls-files:'
   PATH="$ROOT/bin:$PATH" FAN_BASE="$BASE" FAN_FIXTURES="$FIX" bash bench/fanout/rescore-FAN.sh "$ROOT/repo" 21 2>&1 | rg '^CHECK [1-6]|^fan_check|^rescore-FAN'
   ```

   Verbatim output:

   ```text
           ;; -z / NUL-separated, raw bytes: `--name-only` (no -z) C-quotes any path
           ;; containing a backslash regardless of core.quotePath, so a legal POSIX
           ;; path with a literal "\" component never string-matches the manifest's
           ;; raw spelling (inb-9c18e2). -z disables that quoting entirely.
           gd (sh "git" "-C" wt "diff" "-z" "--name-only" base)
           untracked (sh "git" "-C" wt "ls-files" "-z" "--others" "--exclude-standard")
           _ (when-not (zero? (:exit gd))
               (println "CHECK 1 file-set: FAIL git diff failed:" (str/trim (:err gd)))
               (System/exit 1))
           split-nul (fn [s] (remove str/blank? (str/split s (re-pattern (str (char 0))))))
           changed (into #{} (split-nul (:out gd)))
           extra-untracked (into #{} (split-nul (:out untracked)))
           changed-all (into changed extra-untracked)]
   #!/usr/bin/env bash
   for arg in "$@"; do
     if [ "$arg" = ls-files ]; then echo simulated-ls-files-failure >&2; exit 42; fi
   done
   exec /usr/bin/git "$@"
   real git:
   CHECK 1 file-set: FAIL changed=22 expected=21 missing=0 [] extras=1 ["src/acid/fanout/extra.clj"]
   fan_check: FAILED CHECK 1 file-set
   git wrapper exits 42 for ls-files:
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   fan_check: 4/4 structural checks passed
   full six-check gate with that failing ls-files:
   rescore-FAN: worktree=/tmp/fanout-review-fx/ls-files-error-1/repo n=21 base=e53252477b61a7f32a05b5fe78379423acc943ad fixtures=/tmp/fanout-review-fx/selftest-k/k6-a
   CHECK 1 file-set: PASS changed=21 expected=21 missing=0 [] extras=0 []
   CHECK 2 form-equality: PASS compared=21 equal=21 unparseable=0 [] unequal=0 []
   CHECK 3 protected-regions: PASS regions=106 intact=106 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: PASS src-files=101 old-lib-hits=0 [] old-site-residue=0 [] wrong-or-missing-alias=0 [] shadowing=0 []
   fan_check: 4/4 structural checks passed
   CHECK 4 load: PASS namespaces=100 rc=0
   CHECK 5 behaviour: PASS FAN-TEST tests=21 assertions=147 failures=0 errors=0 (base count=21)
   rescore-FAN: 6/6 checks passed
   ```

   Required fix: check `(:exit untracked)` exactly as `gd` is checked, print its stderr, and exit nonzero before CHECK 1. Add a sabotage ratchet that injects an `ls-files` failure with an otherwise-green tracked migration plus one untracked extra and requires the overall scorer to fail.

2. `bench/fanout/fan_check.clj:52` — CHECK 1 still false-FAILs one legal POSIX name: a path consisting solely of whitespace. NUL splitting already creates only empty separators, so `str/blank?` is overbroad; it deletes the real path `" "`. The requested nonblank path with a trailing space passes (finding 3), but the implementation is not fully byte-faithful. Use `empty?`, not `str/blank?`. Independently, `clojure.java.shell/sh` exposes strings rather than arbitrary undecodable POSIX byte sequences, so the claim should be scoped to Java/EDN-representable paths.

   Exact command:

   ```bash
   ROOT=/tmp/fanout-review-fx/blank-path-1
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   printf 'raw changed path: '
   while IFS= read -r -d '' p; do printf '<%q> ' "$p"; done < <(git -C "$ROOT/repo" diff -z --name-only "$BASE")
   printf '\n'
   bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/manifest.edn" "$ROOT/canonical" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   ```

   Verbatim output:

   ```text
   raw changed path: <\ > 
   CHECK 1 file-set: FAIL changed=0 expected=1 missing=1 [" "] extras=0 []
   fan_check: FAILED CHECK 1 file-set
   ```

3. `bench/fanout/fan_check.clj:43-55` — The requested inb-9c18e2 fix itself is correct for backslash, embedded newline, leading dash, and trailing-space paths. At `aa95fc7`, the backslash and newline cases fail closed as missing+extras. At `8ed5893`, all 24 requested/ordinary owners match, and all four structural checks pass. A leading dash is data, never argv; a trailing space survives; NUL framing preserves the embedded newline.

   Exact command (newline, leading dash, trailing space; one correct migration containing all three):

   ```bash
   ROOT=/tmp/fanout-review-fx/path-bytes-1
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   git show aa95fc7:bench/fanout/fan_check.clj | bb /dev/stdin "$ROOT/repo" "$ROOT/manifest-24.edn" "$ROOT/canonical" "$BASE" 2>&1 | rg '^CHECK 1'
   bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/manifest-24.edn" "$ROOT/canonical" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   while IFS= read -r -d '' p; do case "$p" in *leading*|*new*|*trailing*) printf '%q\n' "$p";; esac; done < <(git -C "$ROOT/repo" diff -z --name-only "$BASE")
   ```

   Verbatim output:

   ```text
   CHECK 1 file-set: FAIL changed=24 expected=24 missing=1 ["src/acid/fanout/new\nline.clj"] extras=1 ["\"src/acid/fanout/new\\nline.clj\""]
   CHECK 1 file-set: PASS changed=24 expected=24 missing=0 [] extras=0 []
   fan_check: 4/4 structural checks passed
   -leading.clj
   $'src/acid/fanout/new\nline.clj'
   src/acid/fanout/trailing\ .cljc
   ```

   Exact command (backslash, old then new):

   ```bash
   ROOT=/tmp/fanout-review-fx/selftest-backslash
   BS_T5='src/acid/fanout/\/ns_005.clj'
   cp "$ROOT/canonical/$BS_T5" "$ROOT/repo/$BS_T5"
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   git show aa95fc7:bench/fanout/fan_check.clj | bb /dev/stdin "$ROOT/repo" "$ROOT/manifest-23.edn" "$ROOT/canonical" "$BASE" 2>&1 | rg '^CHECK 1'
   bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/manifest-23.edn" "$ROOT/canonical" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   ```

   Verbatim output:

   ```text
   CHECK 1 file-set: FAIL changed=23 expected=23 missing=2 ["src/acid/fanout/\\/ns_003.clj" "src/acid/fanout/\\/ns_005.clj"] extras=2 ["\"src/acid/fanout/\\\\/ns_003.clj\"" "\"src/acid/fanout/\\\\/ns_005.clj\""]
   CHECK 1 file-set: PASS changed=23 expected=23 missing=0 [] extras=0 []
   fan_check: 4/4 structural checks passed
   ```

   `bench/fanout/fan_check.clj:47-55` also counts rename and submodule records correctly. For a staged `R100`, `git diff --name-only` emits one record: the destination. CHECK 1 therefore rejects a rename when the manifest owns the original path, and accepts it only if the manifest owns the destination. For an unstaged move, `git diff` contributes the tracked deletion and `ls-files --others` contributes the untracked destination, so the union contains two paths. A changed gitlink contributes one submodule path; CHECK 1 counts it once, while CHECK 2/6 reject a manifest that pretends the directory is a Clojure owner.

   Exact command (staged rename):

   ```bash
   ROOT=/tmp/fanout-review-fx/rename-1
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   git -C "$ROOT/repo" diff --name-status "$BASE"
   while IFS= read -r -d '' p; do printf '<%q>\n' "$p"; done < <(git -C "$ROOT/repo" diff -z --name-only "$BASE")
   printf '%s\n' 'manifest expects original owner:'
   bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/manifest-old.edn" "$ROOT/canonical-old" "$BASE" 2>&1 | rg '^CHECK 1'
   printf '%s\n' 'manifest expects renamed owner:'
   bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/manifest-new.edn" "$ROOT/canonical-new" "$BASE" 2>&1 | rg '^CHECK 1'
   ```

   Verbatim output:

   ```text
   R100	old name.clj	new name.clj
   <new\ name.clj>
   manifest expects original owner:
   CHECK 1 file-set: FAIL changed=1 expected=1 missing=1 ["old name.clj"] extras=1 ["new name.clj"]
   manifest expects renamed owner:
   CHECK 1 file-set: PASS changed=1 expected=1 missing=0 [] extras=0 []
   ```

   Exact command (submodule):

   ```bash
   ROOT=/tmp/fanout-review-fx/submodule-2
   BASE=$(git -C "$ROOT/super" rev-parse HEAD)
   printf 'git diff -z --name-only: '
   while IFS= read -r -d '' p; do printf '<%q> ' "$p"; done < <(git -C "$ROOT/super" diff -z --name-only "$BASE")
   printf '\n'
   bb bench/fanout/fan_check.clj "$ROOT/super" "$ROOT/manifest.edn" "$ROOT/canonical" "$BASE" 2>&1 | rg '^CHECK 1|^fan_check'
   ```

   Verbatim output:

   ```text
   git diff -z --name-only: <vendor/sub\ module> 
   CHECK 1 file-set: PASS changed=1 expected=1 missing=0 [] extras=0 []
   fan_check: FAILED CHECK 2 form-equality, CHECK 6 residue-and-alias
   ```

4. `bench/fanout/sabotage-FAN.sh:114-175` — The new backslash self-test is not decoration for the specified omission. Its normal run passes. Replacing only the second backslash owner with its planted PRE bytes (equivalent to not copying canonical for that owner) makes CHECK 1 report the exact owner missing; CHECK 2 and CHECK 6 independently fail too, and the scorer exits 1.

   Exact command and verbatim output:

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-backslash 21 7 /tmp/fanout-review-fx/selftest-backslash
   SELFTEST-BACKSLASH: CHECK 1 file-set: PASS changed=23 expected=23 missing=0 [] extras=0 []
   SELFTEST-BACKSLASH: PASS -- CHECK 1 correctly reads a directory literally named backslash (missing=0 extras=0)
   ```

   Exact sabotage command:

   ```bash
   ROOT=/tmp/fanout-review-fx/selftest-backslash
   BS_T5='src/acid/fanout/\/ns_005.clj'
   cp "$ROOT/gen/repo-21/src/acid/fanout/ns_005.clj" "$ROOT/repo/$BS_T5"
   BASE=$(git -C "$ROOT/repo" rev-parse HEAD)
   bb bench/fanout/fan_check.clj "$ROOT/repo" "$ROOT/manifest-23.edn" "$ROOT/canonical" "$BASE"
   rc=$?
   printf 'rc=%d\n' "$rc"
   ```

   Verbatim output:

   ```text
   CHECK 1 file-set: FAIL changed=22 expected=23 missing=1 ["src/acid/fanout/\\/ns_005.clj"] extras=0 []
   CHECK 2 form-equality: FAIL compared=23 equal=22 unparseable=0 [] unequal=1 ["src/acid/fanout/\\/ns_005.clj"]
   CHECK 3 protected-regions: PASS regions=116 intact=116 manifest-sha-mismatch=0 damaged=0 []
   CHECK 6 residue-and-alias: FAIL src-files=102 old-lib-hits=1 ["/tmp/fanout-review-fx/selftest-backslash/repo/src/acid/fanout/\\/ns_005.clj"] old-site-residue=1 ["src/acid/fanout/\\/ns_005.clj"] wrong-or-missing-alias=1 ["src/acid/fanout/\\/ns_005.clj"] shadowing=0 []
   fan_check: FAILED CHECK 1 file-set, CHECK 2 form-equality, CHECK 6 residue-and-alias
   rc=1
   ```

5. `bench/fanout/fan_check.clj:67-138`, `bench/fanout/rescore-FAN.sh:52-80`, `bench/fanout/gen-fanout.clj:242-321`, `bench/fanout/sabotage-FAN.sh:52-53,79-83,126-193` — Path-listing inventory and the other five checks:

   - CHECK 1 has the only Git path-output consumers: `git diff -z --name-only` and `git ls-files -z --others --exclude-standard`. Their framing is faithful for the requested names, subject to findings 1 and 2.
   - CHECK 2 form equality and CHECK 3 protected regions iterate manifest `:targets` and construct `io/file` directly. They do not consume Git-quoted text or shell globs; the 24-path fixture's `4/4` proves backslash/newline/leading-dash/trailing-space lookup works.
   - CHECK 6 iterates manifest targets directly and discovers the residue scan with Java `file-seq`; it sees backslash/newline/space path components and uses no shell or Git text protocol.
   - CHECK 4 load and CHECK 5 behaviour do not list filesystem paths. `gen-fanout.clj:242-274` renders namespace symbols into `test/load_all.clj` and the generated test namespace; `rescore-FAN.sh` executes those fixed files. CHECK 1 owns detection of extra/omitted files.
   - `gen-fanout.clj` has no filesystem path-listing call: it derives `:file`, `:targets`, and `:non-targets` from in-memory specs and emits them directly.
   - `sabotage-FAN.sh` uses quoted `cp -r` directory traversal and `diff -rq`; `diff` output is discarded and only its exit status is used, so C-quoting cannot alter the equality decision. There is no `*` shell glob (`src/.` is a literal directory). Lines 191 and 193 extract one manifest path through `println` plus shell command substitution; that helper is not byte-faithful for a filename ending in newline because command substitution strips trailing newlines, but it drives only the ordinary-name sabotage mutations, not any acceptance check or the backslash self-test.

   Exact inventory command:

   ```bash
   rg -n 'git.*diff|ls-files|file-seq|diff -rq|T1=.*:file|NT=.*:non-targets|cp -r' bench/fanout/fan_check.clj bench/fanout/gen-fanout.clj bench/fanout/rescore-FAN.sh bench/fanout/sabotage-FAN.sh
   ```

   Verbatim output:

   ```text
   bench/fanout/sabotage-FAN.sh:52:    if diff -rq "$A/repo-$N" "$B/repo-$N" >/dev/null 2>&1 \
   bench/fanout/sabotage-FAN.sh:53:       && diff -rq "$A/canonical-$N" "$B/canonical-$N" >/dev/null 2>&1 \
   bench/fanout/sabotage-FAN.sh:79:    cp -r "$A/repo-$N" "$RS/base"
   bench/fanout/sabotage-FAN.sh:83:    rm -rf "$RS/good"; cp -r "$RS/base" "$RS/good"; cp -r "$A/canonical-$N/src/." "$RS/good/src/"
   bench/fanout/sabotage-FAN.sh:101:# Defect inb-9c18e2: `git diff --name-only` C-quotes any path containing a backslash
   bench/fanout/sabotage-FAN.sh:113:# Pre-fix (git diff --name-only, no -z) this goes RED with missing=2 extras=2.
   bench/fanout/sabotage-FAN.sh:126:  cp -r "$SCRATCH/gen/repo-$N/." "$SCRATCH/repo/"
   bench/fanout/sabotage-FAN.sh:139:  cp -r "$SCRATCH/gen/canonical-$N/." "$SCRATCH/canonical/"
   bench/fanout/sabotage-FAN.sh:161:  cp -r "$SCRATCH/canonical/src/." "$SCRATCH/repo/src/"
   bench/fanout/sabotage-FAN.sh:182:cp -r "$FIX/repo-$N" "$SCRATCH/base"
   bench/fanout/sabotage-FAN.sh:186:mk_good () { rm -rf "$1"; cp -r "$SCRATCH/base" "$1"; cp -r "$FIX/canonical-$N/src/." "$1/src/"; }
   bench/fanout/sabotage-FAN.sh:191:T1=$(bb -e "(println (:file (first (:targets (read-string (slurp \"$FIX/manifest-$N.edn\"))))))")
   bench/fanout/sabotage-FAN.sh:193:NT=$(bb -e "(println (first (:non-targets (read-string (slurp \"$FIX/manifest-$N.edn\")))))")
   bench/fanout/fan_check.clj:47:        gd (sh "git" "-C" wt "diff" "-z" "--name-only" base)
   bench/fanout/fan_check.clj:48:        untracked (sh "git" "-C" wt "ls-files" "-z" "--others" "--exclude-standard")
   bench/fanout/fan_check.clj:50:            (println "CHECK 1 file-set: FAIL git diff failed:" (str/trim (:err gd)))
   bench/fanout/fan_check.clj:103:                          (file-seq src))
   ```

6. `bench/fanout/gen-fanout.clj:323-356` — Retroactive answer: **the C-quoting defect itself could not make a published 6/6 into a false PASS; it failed closed.** Every `--n 21 --seed 7` target list for k=1..6 is the same 21 plain paths, and `bad=[]` for backslash, newline, or leading dash. Thus inb-9c18e2 caused no published-cohort false PASS and no false FAIL on these fixtures. However, the answer for the old scorer as a whole is **yes, it had a separate possible false-PASS route**: finding 1 is present in `aa95fc7` too, and published CHECK lines do not attest `ls-files` exit status. I found no evidence that a published run actually suffered that subprocess failure; this is a capability/assurance gap, not a claim that a published score was wrong.

   Exact commands:

   ```bash
   ROOT=/tmp/fanout-review-fx/published-paths-1
   mkdir -p "$ROOT"
   for k in 1 2 3 4 5 6; do bb bench/fanout/gen-fanout.clj --n 21 --seed 7 --k "$k" --out "$ROOT/k$k" >/dev/null; done
   bb -e '(require (quote clojure.string)) (doseq [k (range 1 7)] (let [m (read-string (slurp (str (first *command-line-args*) "/k" k "/manifest-21.edn"))) ps (mapv :file (:targets m)) bad (filterv #(or (clojure.string/includes? % "\\") (clojure.string/includes? % "\n") (clojure.string/starts-with? % "-")) ps)] (println (str "k=" k " paths=" (pr-str ps) " bad=" (pr-str bad)))))' "$ROOT"
   ```

   Verbatim output:

   ```text
   k=1 paths=["src/acid/fanout/ns_003.clj" "src/acid/fanout/ns_005.clj" "src/acid/fanout/ns_010.cljc" "src/acid/fanout/ns_013.clj" "src/acid/fanout/ns_017.clj" "src/acid/fanout/ns_023.clj" "src/acid/fanout/ns_025.clj" "src/acid/fanout/ns_026.clj" "src/acid/fanout/ns_027.clj" "src/acid/fanout/ns_029.clj" "src/acid/fanout/ns_035.clj" "src/acid/fanout/ns_039.clj" "src/acid/fanout/ns_043.clj" "src/acid/fanout/ns_055.clj" "src/acid/fanout/ns_058.clj" "src/acid/fanout/ns_066.clj" "src/acid/fanout/ns_068.clj" "src/acid/fanout/ns_078.clj" "src/acid/fanout/ns_083.clj" "src/acid/fanout/ns_091.clj" "src/acid/fanout/ns_098.clj"] bad=[]
   k=2 paths=["src/acid/fanout/ns_003.clj" "src/acid/fanout/ns_005.clj" "src/acid/fanout/ns_010.cljc" "src/acid/fanout/ns_013.clj" "src/acid/fanout/ns_017.clj" "src/acid/fanout/ns_023.clj" "src/acid/fanout/ns_025.clj" "src/acid/fanout/ns_026.clj" "src/acid/fanout/ns_027.clj" "src/acid/fanout/ns_029.clj" "src/acid/fanout/ns_035.clj" "src/acid/fanout/ns_039.clj" "src/acid/fanout/ns_043.clj" "src/acid/fanout/ns_055.clj" "src/acid/fanout/ns_058.clj" "src/acid/fanout/ns_066.clj" "src/acid/fanout/ns_068.clj" "src/acid/fanout/ns_078.clj" "src/acid/fanout/ns_083.clj" "src/acid/fanout/ns_091.clj" "src/acid/fanout/ns_098.clj"] bad=[]
   k=3 paths=["src/acid/fanout/ns_003.clj" "src/acid/fanout/ns_005.clj" "src/acid/fanout/ns_010.cljc" "src/acid/fanout/ns_013.clj" "src/acid/fanout/ns_017.clj" "src/acid/fanout/ns_023.clj" "src/acid/fanout/ns_025.clj" "src/acid/fanout/ns_026.clj" "src/acid/fanout/ns_027.clj" "src/acid/fanout/ns_029.clj" "src/acid/fanout/ns_035.clj" "src/acid/fanout/ns_039.clj" "src/acid/fanout/ns_043.clj" "src/acid/fanout/ns_055.clj" "src/acid/fanout/ns_058.clj" "src/acid/fanout/ns_066.clj" "src/acid/fanout/ns_068.clj" "src/acid/fanout/ns_078.clj" "src/acid/fanout/ns_083.clj" "src/acid/fanout/ns_091.clj" "src/acid/fanout/ns_098.clj"] bad=[]
   k=4 paths=["src/acid/fanout/ns_003.clj" "src/acid/fanout/ns_005.clj" "src/acid/fanout/ns_010.cljc" "src/acid/fanout/ns_013.clj" "src/acid/fanout/ns_017.clj" "src/acid/fanout/ns_023.clj" "src/acid/fanout/ns_025.clj" "src/acid/fanout/ns_026.clj" "src/acid/fanout/ns_027.clj" "src/acid/fanout/ns_029.clj" "src/acid/fanout/ns_035.clj" "src/acid/fanout/ns_039.clj" "src/acid/fanout/ns_043.clj" "src/acid/fanout/ns_055.clj" "src/acid/fanout/ns_058.clj" "src/acid/fanout/ns_066.clj" "src/acid/fanout/ns_068.clj" "src/acid/fanout/ns_078.clj" "src/acid/fanout/ns_083.clj" "src/acid/fanout/ns_091.clj" "src/acid/fanout/ns_098.clj"] bad=[]
   k=5 paths=["src/acid/fanout/ns_003.clj" "src/acid/fanout/ns_005.clj" "src/acid/fanout/ns_010.cljc" "src/acid/fanout/ns_013.clj" "src/acid/fanout/ns_017.clj" "src/acid/fanout/ns_023.clj" "src/acid/fanout/ns_025.clj" "src/acid/fanout/ns_026.clj" "src/acid/fanout/ns_027.clj" "src/acid/fanout/ns_029.clj" "src/acid/fanout/ns_035.clj" "src/acid/fanout/ns_039.clj" "src/acid/fanout/ns_043.clj" "src/acid/fanout/ns_055.clj" "src/acid/fanout/ns_058.clj" "src/acid/fanout/ns_066.clj" "src/acid/fanout/ns_068.clj" "src/acid/fanout/ns_078.clj" "src/acid/fanout/ns_083.clj" "src/acid/fanout/ns_091.clj" "src/acid/fanout/ns_098.clj"] bad=[]
   k=6 paths=["src/acid/fanout/ns_003.clj" "src/acid/fanout/ns_005.clj" "src/acid/fanout/ns_010.cljc" "src/acid/fanout/ns_013.clj" "src/acid/fanout/ns_017.clj" "src/acid/fanout/ns_023.clj" "src/acid/fanout/ns_025.clj" "src/acid/fanout/ns_026.clj" "src/acid/fanout/ns_027.clj" "src/acid/fanout/ns_029.clj" "src/acid/fanout/ns_035.clj" "src/acid/fanout/ns_039.clj" "src/acid/fanout/ns_043.clj" "src/acid/fanout/ns_055.clj" "src/acid/fanout/ns_058.clj" "src/acid/fanout/ns_066.clj" "src/acid/fanout/ns_068.clj" "src/acid/fanout/ns_078.clj" "src/acid/fanout/ns_083.clj" "src/acid/fanout/ns_091.clj" "src/acid/fanout/ns_098.clj"] bad=[]
   ```

7. `bench/fanout/sabotage-FAN.sh:37-95,114-175,178-258` — Required gates are green, including every claimed ran-line, but they do not exercise finding 1 or the whitespace-only case.

   Exact command and verbatim output:

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-k 21 7 /tmp/fanout-review-fx/selftest-k
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

   Exact command and verbatim output:

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh --selftest-backslash 21 7 /tmp/fanout-review-fx/selftest-backslash
   SELFTEST-BACKSLASH: CHECK 1 file-set: PASS changed=23 expected=23 missing=0 [] extras=0 []
   SELFTEST-BACKSLASH: PASS -- CHECK 1 correctly reads a directory literally named backslash (missing=0 extras=0)
   ```

   Exact command and verbatim output:

   ```bash
   $ bash bench/fanout/sabotage-FAN.sh /tmp/fanout-review-fx/selftest-k/k6-a 21 /tmp/fanout-review-fx/plain
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

8. `bench/fanout/fan_check.clj:47-54`, `bench/fanout/sabotage-FAN.sh:97-175` — Provenance is correct and the review clone remained unchanged. The requested history is exactly RED test then GREEN implementation; the RED commit changes only the sabotage script and the GREEN commit changes only `fan_check.clj`.

   Exact command:

   ```bash
   git rev-parse HEAD
   git status --short
   git log --oneline b62a501..8ed5893
   git diff --stat b62a501..8ed5893
   git diff --name-status b62a501..8ed5893
   ```

   Verbatim output (`git status --short` contributes no line):

   ```text
   8ed58934ed58112fd173e94010fe702562d77412
   8ed58934 bench/fanout: GREEN — fan_check.clj CHECK 1 reads git diff/ls-files with -z
   aa95fc7e bench/fanout: RED — CHECK 1 misreads a legal backslash-named directory
    bench/fanout/fan_check.clj   | 13 ++++---
    bench/fanout/sabotage-FAN.sh | 82 ++++++++++++++++++++++++++++++++++++++++++++
    2 files changed, 91 insertions(+), 4 deletions(-)
   M	bench/fanout/fan_check.clj
   M	bench/fanout/sabotage-FAN.sh
   ```

## NO-GO

This tip may not land on MCP/main until `ls-files` failure is fail-closed with a false-PASS ratchet; replace `str/blank?` with `empty?` in the same fix.
