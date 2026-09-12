# Named non-portability findings

The census retains each complete runtime control in `controls/`. A failure here
is not repaired by the existing assignment to the other runtime. No runtime or
cadence has been changed. These findings remain portability refusals until fixed.

- `clj-surgeon.cljc.merge-test`: JVM control has one error in
  `unmatched-body-counts-emit-strict-split`; bb passes. The JVM tools.reader
  rejects a top-level splicing reader conditional with `cond-splice not in list`.
  The witness explicitly expects that strict-split output shape. This is a
  separate CLJC output/reader contract, not the xray exception wrapper.
- `clj-surgeon.cljc.split-test`: JVM control has one error and one failure;
  bb passes. `double-round-trip-converges` hits the same top-level-splice
  refusal. `round-trip-unmatched-counts` compares separately read anonymous
  functions whose JVM-generated argument symbols differ. The latter is a
  test-oracle normalization issue, independently of the reader refusal.
- `clj-surgeon.mcp-cold-verify-test`: JVM passes; bb has one error in
  `cold-clj-kondo-admission-timeout-is-unverified`. SCI refuses `close` on
  `sun.nio.ch.FileLockImpl`. This is a runtime capability failure in the
  admission-timeout witness, independently of xray evaluation.
- `clj-surgeon.mcp-namespace-split-test`: JVM passes; bb has one error in
  `empty-profile-refuses-with-a-specific-reason`. Its late `requiring-resolve`
  of helper-extraction needs `nrepl.core`, absent from the configured bb
  classpath. Initial namespace loading succeeds, so this is retained as a
  failed complete bb control rather than hidden among initial-load exclusions.
- `clj-surgeon.mcp-relation-census-launcher-test`: JVM passes; bb reports
  38 failures and one error. JVM-launcher assertions receive empty output;
  their launcher builds `java -cp` from the enclosing process's
  `java.class.path`, which is not a JVM test-dependency classpath under bb.
  Independently, `the-launchers-last-resort-catch-is-over-throwable` cannot
  invoke `StackOverflowError`'s constructor in the bb native image. The
  constructor error is directly logged; the classpath diagnosis follows the
  launcher's recorded source expression and empty-output failures.
- `clj-surgeon.memory.journal-green-test`: JVM passes its two tests and 18
  assertions, including the 600-file reference/journal and flatness controls.
  bb has two errors because the child launcher resolves Java to `bin/java`,
  which does not exist in that runtime context.
- `clj-surgeon.memory.oom-reproduction-test`: JVM passes; bb has two errors
  from the same `bin/java` launcher failure. The census did execute the
  dedicated memory namespaces; it did not infer their status from cadence.
- `clj-surgeon.mission-display-test`: JVM passes; bb has three errors from
  late mission-CLI resolution requiring `nrepl.core`. This is a completed
  failed bb test control despite successful initial namespace loading.
- `clj-surgeon.ns-isolation-test`: JVM passes; bb has three errors because
  the native image cannot invoke `sci.lang.Var.getRawRoot`. This is an
  instrumentation capability mismatch in complete tests, not an initial-load
  failure, and remains visible despite the namespace's existing JVM assignment.
- `clj-surgeon.reader-eval-fence-test`: JVM passes all 13 tests / 77 assertions;
  bb has eight failures. The bb-hosted JVM launchers print
  `ClassNotFoundException: clojure.main`, so the required real-launcher output
  is missing. The full JVM control was retained; its wall was 460,155 ms.
- `clj-surgeon.worktree-lifecycle-prune-test`: both runtimes have five errors
  in 24 tests. The fixtures explicitly create directories under `/private/tmp`,
  which is absent on this Linux host. This is a common environment failure,
  not an asymmetric runtime result; neither control certifies portability.
- `clj-surgeon.worktree-lifecycle-recovery-test`: JVM passes; bb has five
  failures and nine errors. Replayed crash-window operations report
  `:lifecycle-target-locked`, and expected removal counts remain zero.
  The precise lock-release mechanism is not established by this census.

Configured bb load incompatibilities are listed individually, including their
cause messages, in `portability-census.md`. They are accounted for as unsupported
on the configured bb classpath, not counted as passing tests. A namespace that
loads and then fails tests is not placed in that category.
