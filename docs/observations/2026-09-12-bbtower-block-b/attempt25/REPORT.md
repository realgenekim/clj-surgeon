# Block B attempt25 — GO for branch handoff

Sol round-five F1 is repaired. The probe derives canonical directory roots from
the running image's `java.class.path`, resolves namespace sources with
`RT/baseLoader` (the loader used by require), reloads local dependencies, and
counts distinct jar dependencies in `:external`. It refuses dependencies with
no resource or a source outside every root as `:probe-dependency-unresolved`,
carrying `:ns`, `:resolved-to`, `:roots` and zero reloads. Discovery completes
before execution. Successful and failed execution receipts expose the roots used.

The full restricted diagnostic and the real prewarm both passed on their first
runs. No budget, membership, runtime classification, test-target authorization,
or HTTP writer-boundary change was made. No push or merge was performed.

## Red, contract and witnesses

Commit **3f276210** contains Sol's exact failure shape: an authorized test
depends on a namespace in `dev/experiments`, loads value 1, writes value 2, then
probes with a valid identity. The fixture adds its temporary directories to
both the classloader and image classpath, restoring the property and removing
loaded fixture namespaces/files in finally. On old code, [red.log](red.log)
reports `:probe-passed`, loaded value 1, disk value 2, expected closure 1 and
only the test reloaded. The outer regression fails for that false green.

After repair, [focused-third.log](focused-third.log) records loaded value 2,
`:probe-failed`, one stale-value assertion failure, both namespaces reloaded,
closure count 2, and external count 1 for the actual clojure.test jar resource.
The same witness compares receipt roots with the canonical directory entries
of the image classpath. It now requires the actual dependency reload.

A second regression loads a dependency through a loader URL outside every
image classpath root, then also tests that loaded namespace after deleting its
source. Both cases assert the exact typed refusal, namespace, resolved URL or
nil, roots, empty receipt reload list, and zero observed reload calls. Round-four
prefix-list traversal, stale-dependency execution, unparsed-form refusal and
closure-count witnesses remain green. Their temporary projects now explicitly
declare their image classpath; no literal root fallback remains in production.

BB-PROBE-003 and its design describe this classpath contract. The refusal
registry names the prevented native failure: **a green verdict over code the
image never reloaded**. The reachable refusal census pins 164 kinds. Exactly
two test names were added to the derived census, with no removals. The existing
sleep stimulus moved to line 497 and retains its original reason and duration.

## Verification

| Check | Result | Evidence |
|---|---|---|
| Focused probe namespace | 20 tests / 158 assertions; zero failures/errors | focused-third.log |
| Probe, HTTP, parser compatibility, refusal completeness, receipt booleans | 212 / 4,885; zero failures/errors | focused-all.log |
| Sol's exact nine Vars | 9 / 1,135; zero failures/errors | sol-nine.log |
| Reachable refusal pin | 1 / 4; zero failures/errors | refusal-census.log |
| Corpus and sleep registration | 2 / 9; zero failures/errors; 2,582 registered tests | census-registration.log |
| Standalone fast suite | 1,244 / 12,560; zero failures/errors/isolation violations | checks/fast.log; fast-run/receipt.edn |
| Formatting and paved lint | Zero lint errors/warnings; two pre-existing informational findings in alias tests | format-final.log; lint-registration.log |

Inner stale-value failures printed by the regressions are intentional; the
enclosing suites pass. Initial test-authoring scope and loader-selection errors
are retained in focused-first.log and focused-second.log. The loader mismatch
was repaired by explicitly selecting RT/baseLoader, rather than io/resource's
default loader. See [dogfood.md](dogfood.md) for every source-edit mechanism.

The standalone fast sum was **45,292 / 60,000 ms**, with 28,175 ms makespan and
32 wrapper seconds. It ran once. The restricted diagnostic ran once and passed
all stages, with `:problems []`: **410,071 coordinator ms / 412 wrapper seconds**.
Its fast, integration and bb-runtime sums were 45,876, 67,405 and 243,130 ms.
Evidence: restricted-gate.md, restricted.edn and restricted-run/.

The real prewarm passed all stages, with `:problems []`:
**401,271 coordinator ms / 403 wrapper seconds**. No repair or retry was needed.

| Real prewarm pool | Tests / assertions | Outcome | Relevant sum / ceiling, ms |
|---|---|---|---|
| MCP | 1,422 / 16,511 | Zero failures/errors/isolation/leak violations | fast 46,275 / 60,000; integration 68,168 / 240,000 |
| bb | 902 / 8,029 | Zero failures/errors/isolation/leak violations | bb runtime 241,243 / 343,102 |
| Alias | 182 / 3,669 | Zero failures/errors/isolation/leak violations | battery cadence 85,726 / 1,800,000 |

These accounting dimensions overlap and are not added. The required serial bb
diagnostic also passed: 833 tests / 7,328 assertions, 226,024 ms. Recovery,
repository hygiene and intent audit stages all exited zero.

[gate.md](gate.md) is the verbatim complete real-prewarm transcript, including
every refusal-census and budget line. gate.sha256 proves equality with
checks/prewarm.log. Full lane and coordinator receipts are in prewarm-run/ and
prewarm.edn. [report.edn](report.edn) folds the retained receipts and check index.
Both whole gates tested **925a40d7d440069b3396b11690a740b38246e4a4**, with identical
source digest `b4f3ebcafa447e61e6f2c34c700bb561ae63a017007a251d17c792b2dae71659`.
The final evidence commit changes attempt25 artifacts only.
Git's staged whitespace check flags whitespace-only lines emitted by the raw
gate/install logs and context lines in source.patch. They are preserved verbatim;
the product-source diff itself passes the whitespace check.

## Commits and remaining authority

- ac001e20 preserves the pending battery row and run12 check-only artifacts.
- 3f276210 commits the motivating reproduction red.
- 925a40d7 implements classpath discovery, contract and witnesses, including pins.
- The commit containing this report commits the remaining attempt25 artifacts last.

[commands.md](commands.md) records entrances and subjects. The owned nREPL was
stopped before cold checks. Gates ran sequentially, with no workspace writes
during their isolation windows. No protected port or shared install was touched.

Independent Fable/Sol fence review and the shipping decision remain owed. The
historical fold, ceiling and accounting receipts were not remeasured; the nine
specified witnesses and requested gates exercised the continuing contracts.
No speed or independent acceptance claim follows from this correctness repair.
Least sure: independent review must still assess resource/classloader behavior
beyond these witnesses. Dynamically loaded code without a matching image root
intentionally refuses. There is no disagreement with the requested contract.
