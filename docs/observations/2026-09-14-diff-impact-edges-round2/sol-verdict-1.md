NO-GO
CLASS: Every nonempty diff with a nonempty `:unmatched-files` inventory that can exit 0 without a conservative fallback; oracle: `fixed-point-unmatched-files-must-refuse-or-fallback`, quantified over every such inventory.

Finding F1 — the gate fails open. In [test/diff_impact.clj](/home/forge/src/clj-surgeon-fence/test/diff_impact.clj:104), `:nothing-selected` writes a receipt, launches nothing, and naturally exits 0. No caller in `Makefile`, `/home/forge/bin/ship`, or `/home/forge/bin/lib` interprets the status or chooses a fallback (`rg -n "diff-impact|nothing-selected|results-fixed-point" ...` returned no matches outside the launcher).

The executable Makefile-only fixture command returned:

```clojure
{:process-exit 0
 :status :nothing-selected
 :results {:changed-files ["Makefile"]
           :unmatched-files [{:file "Makefile"
                              :reason :no-dependency-edge}]
           :namespaces []}}
```

That is not a valid green gate. Non-list modes must either run an explicitly conservative fallback lane or exit nonzero/HOLD whenever a nonempty diff has unmatched files. The repair also requires changing DIFF-IMPACT-004 and its test, which currently require exit 0; because ship forbids a repair deleting/replacing assertions under `test/`, this cannot be a compliant `GO-WITH-FIX`.

Pressure-point results:

1. `bb --classpath src:test:libs/clj-splice/src -e '<five run-fixture cases>'` produced:

   - path constant in `src/`: missed, `:no-dependency-edge`
   - path read from EDN config: missed, `:no-dependency-edge`
   - relative `io/resource`: missed, `:no-dependency-edge`
   - scan helper under `test/`: transitively caught
   - scan helper under `src/`: missed and misleadingly classified `:no-test-dependent`

   The misses are recorded, not absent from inventory, but exit 0 converts the uncertainty into a false green.

2. The historical `df0c9e1c` probe returned exactly `72` selected namespaces, `55` source-scan reasons, and only `1` selected namespace with a root narrower than `src/`: `splice-envelope-test` via `src/clj_surgeon`. A boundary probe returned `{:test-root [], :narrow-src-root [{:file "src/clj_surgeon/mcp/handler.clj", ...}]}`; `probe.clj` was not selected. Root bounding passes.

3. No fallback exists today; the fixed-point Makefile fixture exited 0 after running nothing. This is the blocking finding.

4. In scratch, adding `:when source?` to remove data-file edges made both runtimes agree: `9 tests / 66 assertions / 7 failures / 0 errors`. Every failure concerned data-file behavior, although it spans three test vars: the direct reader cell, the data cases in the pure matrix, and printed data reasons. Source-scan and require behavior stayed green.

5. The portability-map comparison returned:

```clojure
{:old-count 162
 :new-count 163
 :added #{clj-surgeon.diff-impact-test}
 :removed #{}
 :changed-existing #{}}
```

   Both JVM and bb command/control pairs reported `:argv-equal true` and `:hash-map-equal true`. Independent `sha256sum` matched all three recorded tip hashes exactly.

No `make test` was run. The scratch worktree was removed; `git diff HEAD --numstat` is empty. The pre-existing untracked `docs/observations/diff-impact-edges.md.codex.log` remains untouched.

> END RECEIPT (fence-run): worktree HEAD at review exit = f78ba442cc0ee9b401c3e2ca73f715e706f7ab8f = fenced sha.
