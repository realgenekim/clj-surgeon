# Astra: prevent Node compile-cache leaks at the shared test boundary

Two otherwise green batteries failed temp hygiene because npm's implicit Node
compile cache survived. Earlier causal evidence is retained at
/var/tmp/forge/node-cache-repro-66s5ynly/REPORT.md; this repair preserves those
failed receipts. It changes the shared test helper, not the production formatter,
seat wrappers, leak exemptions or JVM manifest.

The child environment fixes NODE_DISABLE_COMPILE_CACHE to1, including when its
parent supplies0 or nothing. An authenticated re-exec child missing that exact
value refuses before tests and before registering a destructive sweep. Existing
HOME, real-disk/root ownership, heap, argv and descendant-temp rules remain.
The temp-hygiene design and MCP-OP-TMPHYG-005 specify this contract.

## Executed evidence before normal gate

Artifacts: /var/tmp/forge/astra-nodecache-ratchet-fx/gates/.

- Old helper: new pure witness fails exactly two flag assertions. Real BB child
  and descendant retain an unset flag; forged own-root/sentinel with stripped
  flag exits0, runs tests, and removes the precious marker. These are faithful
  RED receipts, not predictions (`pure-red-corrected.log`, `bb-unset-red.log`,
  `stripped-red.json`).
- Repaired helper: BB unset/0 parents both yield child/descendant1; stripped
  child exits97, never reaches tests, and preserves marker (`bb-green-matrix.json`).
- Owning pure namespace:12tests50assertions,0failures/errors, with explicit BB
  real-disk startup property (`pure-green-disk.log`). Delta is +1BBtest/+6assertions;
  namespace count11→12. It belongs to test/run_all.clj, outside the1353 JVM
  manifest. No namespace enrollment or pinned JVM count changes.
- `make tmp-leak-ratchet-self-test` exited0 at08:15:08Z under slot-t/suite-run,
  own temp/events, and env-unset AFTER the wrapper. Existing JVM heap/argv and
  BB argv arms now witness inheritance; one additional BB arm witnesses
  stripped-flag refusal. No new JVM launch was added (`owning-green.log`).
- Actual cached npx standard-clj0.29.0, offline/no-install: raw unset control
  leaves exactly node-compile-cache; guarded child receives1, exits0 with leak0,
  and yields identical formatted bytes (`raw-formatter-red.json`,
  `formatter-guard.log`). Owned raw cache removed after recording. No package
  installation, provider call, runtime speedup or production-cache claim.
- Changed Clojure files formatted. Lint via ~/bin/clj-kondo passes with the
  existing with-temp-dir macro declared let-like for analysis,0errors/warnings.
  Initial unconfigured lint reported existing macro-binding unresolved symbols;
  its log is retained. Formatting aligns the changed secure-tmpdir! arity.

The first pure RED command had malformed quoting; its reader error is retained
but is not RED evidence. A first direct BB namespace run omitted the explicit
-D property: suite-run does not set BB's native startup property, and that run
created then cleaned fixtures under/tmp. The corrected disk-backed12/50 gate
is the reported receipt. This was an invocation error, not a bypass added to
production or the test runner.

The normal gate is separately captured as gates/normal.log and its terminal
receipt as gates/normal-status.json. This observation is frozen before that
run; it does not predeclare its result. All requested source changes used the
current native-default route. The patch removes a repeated hygiene failure;
it adds no formatter feature or performance claim.
