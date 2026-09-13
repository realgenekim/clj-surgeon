NO-GO

CLASS: create-stage failure outcomes `{throw-before-create, throw-after-create}`. Oracle: inject every registered errno both before and after the real `:create` side effect, asserting prior descriptor preservation, typed refusal, and zero `.probe-*` residue.

Finding SH-ROUND2-01: [write-image!](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/probe_state.clj:84) sets `created?` only after `publish-stage! :create` returns. If creation succeeds but reports interruption afterward, cleanup is skipped.

Focused command:

```text
clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps -
```

It invoked the real create operation, then threw `IOException("Interrupted system call")`. Output:

```clojure
{:throwable "clojure.lang.ExceptionInfo",
 :error-type :probe-state-not-writable,
 :errno "EINTR",
 :prior "{:prior true}\n",
 :entries [".probe-32cc4c98-645c-4d13-ae30-5b305e36caae.tmp"
           "probe.edn"]}
```

The prior descriptor survives, but temporary residue remains, violating STATE-HOME-011. The existing [publication-fault-matrix](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/probe_state_test.clj:108) throws before calling the selected stage, so every `:create` cell covers only `throw-before-create` and cannot detect this member.

The 240,000 ms namespace override is a measured approximately 2× declaration under TEST-ISO-007; the independent 1,800,000 ms battery ceiling remains unchanged. Targeted inspection found no additional outside-envelope writer. Stripping `:telemetry_dropped` from runtime-contract equality is appropriate because the same test separately asserts JVM value `16` and bb absence before comparison.

No `make test` was run. No patch was left; the pre-existing untracked review log remains untouched.

> END RECEIPT (fence-run): worktree HEAD at review exit = 3cd911930ba759a67434c264ff84c9ee43aa225d = fenced sha.
