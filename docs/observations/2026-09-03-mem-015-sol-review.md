# read-path-memory 61cb9b5 (MEM-015) — Sol executed review: GO-WITH-FIX (witness/contract corrections; implementation correct) — round 2 launched

GO-WITH-FIX — the implementation is correct, but the merge queue should hold for three witness/contract corrections: use five allocation samples, narrow MEM-015’s EARS wording, and make the differential independent of the refactored record builder while adding malformed-reader coverage.

1. [outline_differential_test.clj:21](test/clj_surgeon/outline_differential_test.clj:21) — the reconstruction exactly matches `origin/main`’s old `outline-source`, and an independent old-file overlay produced 160 files, 0 `pr-str` mismatches.

2. [outline_differential_test.clj:78](test/clj_surgeon/outline_differential_test.clj:78) — committed boundaries cover `.cljc` conditionals, local `ns`, and attached comments, but not forms following reader errors; executed probes covering unmatched delimiters, unterminated strings, and bad dispatch showed old/new outcome parity.

3. [outline_differential_test.clj:24](test/clj_surgeon/outline_differential_test.clj:24) — the test shares the newly refactored `top-level-form-records`, so its “cannot go tautological” claim is too strong; freeze the old builder or add fixed expected-record fixtures.

4. [outline_memory_test.clj:72](test/clj_surgeon/outline_memory_test.clj:72) — despite the documented “min of five,” the gate executes `min-allocation 3 3`; change the sample argument to five.

5. [outline_memory_test.clj:21](test/clj_surgeon/outline_memory_test.clj:21) — 980× is a historical constant tied to JDK/parser allocation behavior; rewrite-clj 1.2.50 is pinned, the JDK is not, and today’s five-sample minimum was 805.7×, so future upgrades require an explicit rebaseline rather than a silent ceiling bump.

6. [outline_memory_test.clj:68](test/clj_surgeon/outline_memory_test.clj:68) — reverting to the exact `origin/main` implementation in a scratch overlay raised allocation to 1363.1×; the checked-in allocation witness failed at 1337.8× and parse-count failed at exactly two calls.

7. [show_form.clj:360](src/clj_surgeon/show_form.clj:360) — all eight production callers of `top-level-form-records` omit the fourth argument and therefore retain exact `:source`; `show_form` consumes it at line 208 and transaction splice/requalification consumes it at [intent_transaction.clj:248](src/clj_surgeon/intent_transaction.clj:248), while extract safely uses `outline-source` plus the original source at [extract.clj:261](src/clj_surgeon/extract.clj:261).

8. [outline.clj:270](src/clj_surgeon/outline.clj:270) — every public `top-level-form-records` arity directly delegates once to `parse-and-build-records`; ordinary callers cannot bypass it, while `outline-source` intentionally uses the already-parsed root and private builder at line 322.

9. [2026-09-03-mem-015-single-parse.md:133](docs/observations/2026-09-03-mem-015-single-parse.md:133) — the receipt is interpreted correctly: retained outlines remain approximately 94 MB at 10,000 files, transient peak falls only 7.6–24.9 MB, and no pass line flips because live aggregate output dominates.

10. [2026-09-03-memory-design-sol-answer-2.md:230](docs/observations/2026-09-03-memory-design-sol-answer-2.md:230) — the residual owner is `MCP-OP-MEM-003`, streaming `ls-tree`; it should flatten `held_mb`, flip the `cli-ls-tree peak-scales-with-n` line, and likely clear its four peak-budget lines, while MEM-001/MEM-002 precede it for metering and admission.

11. [read-path-memory-specs.md:19](docs/intent/read-path-memory/read-path-memory-specs.md:19) — the EARS row overpromises: `include_string_symbols` deliberately builds form source and reparses it; qualify MEM-015 as the default/non-string-symbol projection, matching the boundary at [read-path-memory-design.md:53](docs/intent/read-path-memory/read-path-memory-design.md:53).

12. [2026-09-03-mem-015-single-parse.md:200](docs/observations/2026-09-03-mem-015-single-parse.md:200) — gates rerun once through the three `suite-run` lanes on `61cb9b5`: test-fast 702/5912/0, MCP 385/3971/0, oracle pass; checkout remains clean.