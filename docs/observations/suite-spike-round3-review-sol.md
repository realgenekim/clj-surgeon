## NO-GO

Final landing verdict at 2026-09-04T23:35:48Z. Read-only review began before the authorized window; focused JVM probes ran only after 23:20Z, all through `~/bin/suite-run`.

1. Checkout and capacity-rule preflight

   File: `CLAUDE.md:1`; review checkout `HEAD`.

   Exact command:

   ```sh
   date -u '+%Y-%m-%dT%H:%M:%SZ'
   git status --short --branch
   git rev-parse HEAD
   ```

   Verbatim output:

   ```text
   2026-09-04T22:42:08Z
   ## HEAD (no branch)
   105f4b6f716e6082ca88dcd677124c8e2d2e9d86
   ```

   The clone is detached at the requested tip. The repository-wide instruction to pull `MCP/main` is superseded here by the user's explicit prohibition on checking out anything else.

2. **BLOCKING — the mechanism that is supposed to discipline battery-only coverage is not on the reviewed tip or the landing gate.**

   Files: reviewed tree (missing `.github/workflows/mcp-main.yml`); `/home/forge/bin/land:17-20`; `Makefile:1027`.

   Exact command:

   ```sh
   git ls-remote origin refs/heads/MCP/main refs/heads/bridge/gha refs/heads/bridge/suite-spike
   printf 'HEAD workflows:\n'; git ls-tree -r --name-only HEAD .github
   printf 'GHA branch workflows:\n'; git ls-tree -r --name-only origin/bridge/gha .github
   nl -ba /home/forge/bin/land | sed -n '15,20p'
   ```

   Verbatim output:

   ```text
   e57a85d21781474e875c4793840624e33a8d153b refs/heads/MCP/main
   5ce8aaea17fbec3879db9c721ca437fc44b56c65 refs/heads/bridge/gha
   105f4b6f716e6082ca88dcd677124c8e2d2e9d86 refs/heads/bridge/suite-spike
   HEAD workflows:
   GHA branch workflows:
   .github/actions/clj-env/action.yml
   .github/scripts/lane_matrix.clj
   .github/workflows/mcp-main.yml
       15 m=$(git rev-parse --short HEAD); ok=1
       16 { echo "=== land $tip onto $base -> merge $m $(date -u +%H:%MZ) load $(cut -d' ' -f1 /proc/loadavg)"
       17   for g in "~/bin/suite-run clojure -M:clj-surgeon/mcp-test" "~/bin/suite-run bb test/run_all.clj" "make mcp-operation-oracle" "make repository-hygiene"; do
       18     echo "=== $g"; eval "$g" 2>&1 | grep -E '^Ran |failures|oracle|hygiene|ERROR in|FAIL in' | tail -6; rc=${PIPESTATUS[0]}; echo "RC=$rc"; [ "$rc" = 0 ] || ok=0; done
       19   echo "=== audit"; clojure -M -e "(require 'clj-surgeon.mcp-intent-contract)(prn (select-keys (clj-surgeon.mcp-intent-contract/audit-current-repository) [:ok :specs :violations]))" 2>&1 | tail -2 | tee /dev/stderr | grep -q ':ok true' || ok=0
       20   echo "=== done $(date -u +%H:%MZ) ok=$ok"; } > $L 2>&1
   ```

   Refresh at 23:34Z: `origin/MCP/main` advanced to `be94b8a3`; `git ls-tree -r --name-only origin/MCP/main .github` was still empty. The workflow-absence result is unchanged.

   `make battery-fresh` exists, but neither `land` nor `make mcp-test` invokes it, and `land` does not run `test-battery`. The CI workflow that fans out all eleven battery namespaces exists only on the separate `bridge/gha` tip, not in `105f4b6f` or current `MCP/main`. Thus the eleven namespaces removed from the merge gate are not mechanically required before this landing. This directly refutes “the rest disciplined off it” for the tree being reviewed.

3. **BLOCKING — a `:fast` namespace still writes outside its private per-run temp root.**

   Files: `test/clj_surgeon/lane_manifest.clj:133`; `test/clj_surgeon/scope_stream_test.clj:17-23`.

   Exact command:

   ```sh
   clj-surgeon :op :cat :file test/clj_surgeon/scope_stream_test.clj :contains CLJ_SURGEON_MEMORY_TMP
   ```

   Verbatim output (source field):

   ```clojure
   (defn- temp-root
     [label]
     (let [dir (io/file (or (System/getenv "CLJ_SURGEON_MEMORY_TMP") "/home/forge/tmp")
                        (str "clj-surgeon-scope-" label "-" (System/currentTimeMillis)
                             "-" (long (rand 1000000))))]
       (.mkdirs dir)
       (.getCanonicalPath dir)))
   ```

   The manifest promises that `:fast` performs no read outside its own `java.io.tmpdir` subtree and is N-wide safe. This helper ignores `java.io.tmpdir` and defaults to the seat-shared `/home/forge/tmp`. The builder disclosed this in the round-three report (`2026-09-04-suite-spike-round3.md:164-167`), so it is not a hypothetical reviewer concern; TEST-ISO-003 remains false at landing.

4. **BLOCKING, CONFIRMED by focused sabotage — the new exclusion ratchet proves only that a named target exists, not that it runs the excluded namespace.**

   File: `test/clj_surgeon/lane_manifest_test.clj:132-155`.

   Exact command:

   ```sh
   clj-surgeon :op :cat :file test/clj_surgeon/lane_manifest_test.clj :form every-exclusion-names-a-runner-that-actually-exists
   ```

   Verbatim output (operative lines):

   ```clojure
   (let [makefile (slurp (io/file "Makefile"))
         deps (slurp (io/file "deps.edn"))
         target? (fn [t] (or (re-find (re-pattern (str "(?m)^" (java.util.regex.Pattern/quote t) ":")) makefile)
                             (str/includes? makefile (str " " t " "))))
         alias? (fn [a] (str/includes? deps (str ":clj-surgeon/" a)))]
     (doseq [[s reason] lm/excluded]
       (let [targets (map second (re-seq #"`make ([a-z0-9\-]+)`" reason))
             aliases (map second (re-seq #":clj-surgeon/([a-z0-9\-]+)" reason))
             named (concat (filter target? targets) (filter alias? aliases))]
         (is (seq named) ...))))
   ```

   Any new orphan can therefore be put in `excluded` with the false reason “`make test-fast`” and pass this witness because that unrelated target exists. The implementation comment claims “the OTHER runner that runs it”; no membership check exists. The authorized-window archive-copy sabotage did exactly this and passed: `{:test 1, :pass 5, :fail 0, :error 0}`.

5. Coverage comparison answers Astra’s question: **no test was deleted from the overall three-lane corpus, but eleven complete namespaces—substantial product and security behavior—were removed from `make mcp-test`.**

   Files: `docs/observations/2026-09-04-suite-spike-round1-timing.md:11-59`; `test/clj_surgeon/lane_manifest.clj:96-153`; `deps.edn:59-70`.

   Exact command:

   ```sh
   awk 'FNR==NR { if (match($0, /clj-surgeon\.[a-z0-9.-]+-test[[:space:]]+:(fast|integration|battery)/)) { s=substr($0,RSTART,RLENGTH); split(s,a,/[[:space:]]+/); lane[a[1]]=substr(a[2],2) } next } /^\| [0-9]+ \| `clj-surgeon\./ { n=$0; sub(/^\| [0-9]+ \| `/,"",n); sub(/`.*/,"",n); l=lane[n]; c=(l=="fast"?"every-run":(l=="integration"?"merge-gate":"landing-and-nightly")); g=(l=="battery"?"no":"yes"); printf "| `%s` | `%s` | `%s` | %s |\n",n,l,c,g }' test/clj_surgeon/lane_manifest.clj docs/observations/2026-09-04-suite-spike-round1-timing.md
   ```

   The complete verbatim 49-row output is in the final coverage table below. Totals are 34 original namespaces in `:fast`, four in `:integration`, and eleven in `:battery`: **38/49 remain on the merge gate; 11/49 do not.** Calling this “the same coverage on the gate” is false. The overall corpus grew to 53 namespaces / 957 tests, but corpus retention and merge-gate coverage are different claims.

6. **BLOCKING from the direct execution path — the `:fast` lane contains a real subprocess drive hidden behind a production helper, exactly the blind spot the unbuilt TEST-ISO-002 witness was meant to close.**

   Files: `test/clj_surgeon/lane_manifest.clj:113`; `test/clj_surgeon/mcp_inspect_tool_test.clj:718-757`; `src/clj_surgeon/mcp_cold_verify.clj:102-210`.

   Exact command:

   ```sh
   clj-surgeon :op :cat :file test/clj_surgeon/mcp_inspect_tool_test.clj :contains Thread/sleep
   clj-surgeon :op :cat :file src/clj_surgeon/mcp_cold_verify.clj :form run-job!
   ```

   Verbatim operative output:

   ```clojure
   (let [launched (cold-verify/launch!
                    (.getPath project) "full"
                    {:command ["/bin/sh" "-c" "printf cold-ok"]
                     :timeout-ms 1000})
         job (:verification_job launched)]
     (loop [attempt 0]
       (when (and (not (:verification_complete
                         (cold-verify/status (.getPath project) job)))
                  (< attempt 100))
         (Thread/sleep 10)
         (recur (inc attempt))))
   ```

   and the helper’s execution path is:

   ```clojure
   (let [process (process-env/run-bounded!
                   {:command command
                    :cwd project-root
                    :timeout-ms timeout-ms
                    :merge-error? true
                    :visible-byte-limit max-output-characters
                    :on-start #(swap! job-store update id assoc :pid %)})
   ```

   Round one’s 40 ms sampler missed this very short `/bin/sh`, while the source scanner checks test-file spellings and therefore misses the production helper. This is a concrete counterexample to the manifest’s “`:fast` No child process” rule, not merely a theoretical scanner bypass. It also supplies the requested seventh timing site: a fast-lane test polls with `Thread/sleep 10` and a fixed ~1 s ceiling, then asserts the cold job passed. `census-pool-test` separately uses a 5 ms sleep to force scheduler distribution and a 2 s polling bound (`census_pool_test.clj:13-45`).

7. Current source arithmetic does close, but it is **53 namespaces**, not the superseded 51-namespace round-two figure.

   Exact command:

   ```sh
   # Static source census over each namespace named by manifest.
   # (Hyphens/dots are converted to the namespace's test path.)
   ```

   Verbatim output:

   ```text
   deftests fast=376 integration=71 battery=510 total=957 missing=0
   namespaces fast=38 integration=4 battery=11 total=53
   ```

   This agrees with the builder’s runtime summaries: 376 + 71 + 510 = 957 tests. The current exclusion count is five because `mcp-formatter-test` was adopted; the original “51 namespaces / six exclusions” claim describes the reviewed round-two predecessor, not `105f4b6f`.

8. COVERAGE COMPARISON — every namespace in round one’s 49, at `105f4b6f`.

   The cadence is the manifest’s declared cadence. “mcp-test?” follows the runner’s own alias: `:clj-surgeon/mcp-test` invokes `mcp-test-runner fast integration`, so every `:fast` and `:integration` namespace is included and every `:battery` namespace is excluded.

   | round-one namespace | lane now | cadence now | mcp-test? |
   |---|---|---|---|
   | `clj-surgeon.reader-eval-fence-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-relation-census-launcher-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-alias-migration-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-relation-census-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-prepared-wire-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.txn-journal-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-hot-verify-test` | `integration` | `merge-gate` | yes |
   | `clj-surgeon.admit-patch-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-compact-relations-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-tool-test` | `integration` | `merge-gate` | yes |
   | `clj-surgeon.outline-differential-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-process-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.core-discovery-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-http-server-test` | `integration` | `merge-gate` | yes |
   | `clj-surgeon.mcp-operation-registry-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-cold-verify-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.scope-stream-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-inspect-tool-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.outline-memory-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-change-buffer-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.workspace-onboarding-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.census-pool-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-inspect-contract-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-compact-location-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-create-files-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.repository-hygiene-test` | `battery` | `landing-and-nightly` | no |
   | `clj-surgeon.mcp-program-tool-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-intent-contract-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-prepared-request-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-extraction-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-contract-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-prepared-confirmation-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-relation-census-round20-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-write-refusal-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-combinable-transaction-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-extraction-plan-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.quoted-var-refs-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-server-test` | `integration` | `merge-gate` | yes |
   | `clj-surgeon.mcp-schema-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-recovery-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-telemetry-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-workspace-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-compact-edit-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-compact-edit-fields-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-read-request-normalization-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-operation-async-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-operation-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-semantic-client-test` | `fast` | `every-run` | yes |
   | `clj-surgeon.mcp-paths-test` | `fast` | `every-run` | yes |

   Totals for the original 49: **34 fast + 4 integration = 38 on `make mcp-test`; 11 battery = 11 off it.** The current expanded corpus is 38 fast + 4 integration + 11 battery = 53 namespaces. The battery-only list is precisely the eleven rows marked “no”; it covers reader-eval fencing, relation-census launcher and product behavior, alias migration, prepared-wire integration, transaction journals, patch admission, process management, discovery, cold verification, and repository hygiene. A regression in any of those namespaces can pass the local merge gate. This is a real merge-gate regression risk, even if a post-push CI battery catches it later.

   Therefore the literal claim **“same coverage on the gate, the rest disciplined off it” is refuted.** The gate does not have the same namespace coverage: 11/49 round-one namespaces and 510/957 current tests are outside it. A narrower, accurate claim is: **“same-or-larger total corpus, 38/49 original namespaces on the gate, with the 11 expensive namespaces assigned to a separate cadence.”**

   The discipline qualification depends on which tree is meant:

   - Tip `105f4b6f` has no `.github` workflow, `make mcp-test` does not run the battery, `/home/forge/bin/land` does not run `test-battery` or `battery-fresh`, and the ledger tripwire is not a prerequisite of a landing gate. On the reviewed tip alone, “the rest disciplined off it” is false.
   - `origin/bridge/gha` supplies `.github/workflows/mcp-main.yml`; its plan reads the battery lane from the manifest and fans all eleven namespaces into separate jobs on pushes to `MCP/main` and `bridge/**`, with a nightly schedule. `git ls-remote --symref origin HEAD` now reports `refs/heads/MCP/main`, so the workflow’s embedded warning that the schedule is dormant because `main` is default is stale: after this workflow is landed, the nightly trigger is eligible to run from the actual default branch. Thus the proposed *combined* landing would mechanically run the battery after each trunk push and nightly. It is post-merge detection, not pre-merge coverage.

9. Focused 23:20Z-window probes of the round-two closures.

   All fixtures were git-archive copies under `/var/tmp/forge/suite3-review-fx`; no historical suite was launched.

   - **Formatter adoption and count pin — CLOSED at the instance level.** A single focused JVM ran the three named lane-manifest witnesses plus every `mcp-formatter-test` var. Output: `{:test 6, :pass 33, :fail 0, :error 0}`. This proves the three formatter tests execute, the clean rename/exclusion witnesses pass, and the 957-test source arithmetic witness passes.
   - **Formatter exclusion class ratchet — OPEN/BLOCKING.** In the archive copy, I changed the already-excluded `clj-surgeon.analyzer-contract-test` reason to the deliberately false `"false redirection for sabotage -- `make test-fast`"`. The named `every-exclusion-names-a-runner-that-actually-exists` witness still exited 0: `{:test 1, :pass 5, :fail 0, :error 0}`. It checks only that the named target exists, not that the target runs the excluded namespace. This directly falsifies the implementation comment and round-three claim that the class was closed.
   - **Rename sweep and ratchet — CLOSED for its declared pattern.** The clean witness passed in the six-test bundle. Adding `The babashka lane is `make test-fast`.` to tracked living `README.md` in an archive copy made the named witness fail exactly: `README.md:3`, `{:test 1, :pass 0, :fail 1, :error 0}`. The documented blind spot remains: prose that names `test-fast` but contains no Babashka spelling cannot be classified by this scanner; that is non-blocking because the limitation is disclosed.
   - **Counts — CLOSED.** The source pin passes and the manifest arithmetic is fast 376 + integration 71 + battery 510 = **957 tests**, across 38 + 4 + 11 = **53 namespaces**. The builder’s separate runtime summaries report the same 957 total. The corrected historical round-two headline is 429 tests / 4,323 assertions; round three’s current combined gate is 447 / 4,408.
   - **Trunk merge — CLOSED for the named round-three base, stale by normal trunk motion.** Merge commit `8d184d65` has parents `2ecce8c4` and `9fceefe0`. After a read-only fetch during this review, tip `105f4b6f` is **88 commits behind** current `origin/MCP/main` (`be94b8a3`); the landing mechanism must merge current trunk and rerun gates. A read-only `git merge-tree` reports seven paths changed on both sides but no conflict markers. No merge was performed.
   - **GC sleeps — CLOSED for finding 8.** The two fixed `Thread/sleep 100` assertions were replaced with `await-cleared`, a reachability loop with a 10 s failure deadline and immediate success. The named `the-reader-drops-each-source-when-its-callback-returns` witness passed: `{:test 1, :pass 4, :fail 0, :error 0}`. This does not erase the separately disclosed 5 ms scheduler-forcing sleep in `census-pool-test` or the 10 ms cold-job polling site in `mcp-inspect-tool-test`.
   - **Battery ledger + tripwire — CLOSED at its named witnesses.** `~/bin/suite-run make battery-fresh` exited 0 and reported the newest receipt `2522bd95`, wall 721 s, 1.1 h old, one commit behind HEAD. The full `battery-ledger-test` refusal matrix waited behind another owner’s `make mcp-test`, then completed before cutoff: `{:test 12, :pass 53, :fail 0, :error 0}`, exit 0.

10. Verdict.

   **NO-GO. Do not land `105f4b6f` on `MCP/main`, with or without the GHA workflow.** The round-two instance defects are substantially repaired: formatter coverage is adopted, the rename sweep has a working sabotage witness, arithmetic closes, the named trunk merge exists, the GC witness is reachability-based, and the live battery receipt is fresh. But two branch-contract defects are blocking: (1) the advertised exclusion class ratchet demonstrably accepts an orphan redirected to an unrelated target; and (2) a declared `:fast` namespace still defaults fixtures outside its private `java.io.tmpdir` subtree. The fast-lane child-process counterexample is a third contract mismatch: `mcp-inspect-tool-test` is declared “No child process” while it drives `/bin/sh` through the production cold-verify helper. Separately, the GHA workflow is not in this tip; adding it in the same landing would provide valuable post-push/nightly battery discipline, but it does not repair those false manifest guarantees or restore the eleven namespaces to the pre-merge gate. Non-blocking items are the rename scanner’s disclosed semantic blind spot, stale workflow comments/counts, and the remaining bounded polling sleeps. Required fix round: make exclusion validation prove runner membership (with the false-`make test-fast` sabotage), make every fast fixture root descend from `java.io.tmpdir`, and either reclassify the cold-verify test or change the lane contract/witness so declared isolation matches execution.
