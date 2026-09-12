# Classification of attempt22's twelve failing controls

The nine below have passing frozen JVM controls. All frozen complete controls
are under ../attempt22/controls/<namespace>-<runtime>-test.control.edn and the
adjacent .log preserves diagnostics. The manifest records these same reasons.

| Namespace (clj-surgeon prefix) | State | Capability and exact reason |
|---|---|---|
| mcp-cold-verify-test | bb-ineligible, JVM | sci-host-interop: FileLockImpl.close refused |
| mcp-namespace-split-test | bb-ineligible, JVM | bb-classpath-missing/nrepl.core: late helper-extraction resolution |
| mcp-relation-census-launcher-test | bb-ineligible, JVM | bb-hosted-jvm-launcher: enclosing java.class.path lacks test dependencies; native-image-reflection: StackOverflowError constructor |
| memory.journal-green-test | bb-ineligible, JVM | bb-hosted-jvm-launcher: nonexistent bin/java |
| memory.oom-reproduction-test | bb-ineligible, JVM | bb-hosted-jvm-launcher: nonexistent bin/java |
| mission-display-test | bb-ineligible, JVM | bb-classpath-missing/nrepl.core: late mission CLI resolution |
| ns-isolation-test | bb-ineligible, JVM | native-image-reflection: sci.lang.Var.getRawRoot unavailable |
| reader-eval-fence-test | bb-ineligible, JVM | bb-hosted-jvm-launcher: ClassNotFoundException clojure.main |
| worktree-lifecycle-recovery-test | bb-ineligible, JVM | sci-host-interop: actual release-file-lock! helper refuses FileLockImpl.release; retained lock causes replay refusal |
| cljc.merge-test | portable after repair | JVM reader accepts per-form non-splicing conditionals; both complete controls pass |
| cljc.split-test | portable after repair | JVM-readable split and alpha-normalized reader symbols; both complete controls pass |
| worktree-lifecycle-prune-test | bb-ineligible after reference repair | TMPDIR repaired; Git 2.53.0 admitted after actual compatibility matrix passed on both runtimes; JVM complete control passes, bb now exposes shared SCI FileLockImpl.release limitation |

Recovery's precise mechanism is newly established by recovery-lock-probe.log,
calling the actual private release helper with a real FileChannel lock. The
probe closes the channel and deletes the file in finally. No product recovery
behavior was changed to make this classification. No ninth reason is unknown.

The predicted 100 portable / 9 ineligible is not the observed result. The
corrected rule gives 99 portable / 10 ineligible / 0 refused. Prune's original
path errors had hidden both the unregistered Git version and its bb lock-release
limitation. The Git admission diff does not fake a version or bypass its gate:
it names the exact tested version, with the existing matrix as its witness.
