# txn-journal 1cece9a (B1 kernel) — Sol executed review: GO-WITH-FIX, do not merge HEAD (6 blockers: recheck→rename race, count-only membership, `..` normalised away, identity not pinned, pre-image lifetime, battery ignores the accountant) — round 2 launched

B1 proves the memory architecture, but current HEAD is not merge-ready. The disk journal eliminates the OOM and keeps retention flat; however, the implementation overstates its serializability, misses two identity/membership conflicts, accepts normalized `..`, cannot yet preserve pre-images for durable undo, and is not wired into the battery’s reservation meter.

## Executed receipt

`make memory-red` was run exactly once under `flock /home/forge/tmp/suite.lock`. Exit 0:

```text
clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/memory-test

Testing clj-surgeon.memory.oom-reproduction-test
CONTROL scope: {:root /home/forge/tmp/clj-surgeon-memory-control-1788418045775, :files 8, :bytes 4196920}
CONTROL receipt: {:arm :frozen-read, :files 8, :bytes 4196920, :result-hash-count 8, :tree-hash f1ff28cb792b72d27c6b91e884a55487ef9fb435b28f807bbd4724859c637ae6, :memory {:xmx-mb 256.0, :heap-used-start-mb 33.44873046875, :heap-used-peak-mb 254.75948333740234, :heap-used-end-mb 12.335853576660156, :heap-after-gc-peak-mb 218.5738067626953, :heap-retained-peak-mb 0.0, :wall-ms 2627}}
RED scope: {:root /home/forge/tmp/clj-surgeon-memory-frozen-1788418050749, :files 600, :bytes 314772270}
RED exit: 3
RED err:
RED out: Terminating due to java.lang.OutOfMemoryError: Java heap space

Testing clj-surgeon.memory.journal-green-test
GREEN scope: {:root /home/forge/tmp/clj-surgeon-green-journal-1788418053847, :files 600, :bytes 314772270}
GREEN reference receipt: {:arm :frozen-read, :files 600, :bytes 314772270, :result-hash-count 600, :tree-hash 55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440, :memory {:xmx-mb 2048.0, :heap-used-start-mb 36.210174560546875, :heap-used-peak-mb 2046.9753112792969, :heap-used-end-mb 12.321968078613281, :heap-after-gc-peak-mb 1747.0968322753906, :heap-retained-peak-mb 0.0, :wall-ms 146187}}
GREEN journal receipt: {:tree-hash 55423110f805a112cd6b353252ccd5183e035dfb8fe4b50da52e5f310a762440, :arm :journal, :work {:walk-entries 604, :files-discovered 600, :files-read 600, :source-bytes 314772270, :largest-file-bytes 524621, :receipt-records 0, :receipt-bytes 0}, :memory {:xmx-mb 256.0, :heap-used-start-mb 40.095481872558594, :heap-used-peak-mb 253.5648651123047, :heap-used-end-mb 10.885749816894531, :heap-after-gc-peak-mb 234.92581939697266, :heap-retained-peak-mb 14.399391174316406, :wall-ms 167504}, :read-set-files 600, :commit-error nil, :committed true, :refusals [], :files 600, :reserved {:staged-files 600, :aggregate-bytes 314772270, :heap-reserved-peak-bytes 29378776, :journal-bytes-max 2147483648, :staged-files-max 5000, :aggregate-bytes-max 1073741824, :journal-bytes-peak 629544540, :journal-bytes 629544540, :work-budget-bytes 201326592, :parse-factor 56}, :files-written 600}
GREEN err:
FLATNESS 60 {:xmx-mb 256.0, :heap-used-start-mb 26.4300537109375, :heap-used-peak-mb 252.63892364501953, :heap-used-end-mb 12.910751342773438, :heap-after-gc-peak-mb 229.76739501953125, :heap-retained-peak-mb 13.976226806640625, :wall-ms 18606}
FLATNESS 600 {:xmx-mb 256.0, :heap-used-start-mb 27.42583465576172, :heap-used-peak-mb 254.45913696289062, :heap-used-end-mb 10.839279174804688, :heap-after-gc-peak-mb 234.45994567871094, :heap-retained-peak-mb 14.300285339355469, :wall-ms 168260}

Ran 4 tests containing 25 assertions.
0 failures, 0 errors.
```

This reproduces RED→GREEN, three-way digest parity, ~2,047 MB reference peak, and flat retention. Normal GC variance explains the small differences from the filed 14.19 MB and 13.95→14.15 MB figures.

The RED wording “every ceiling admits” needs qualification:

| Ceiling | Run value | Observed | Result |
|---|---:|---:|---|
| Walk entries | 200,000 | 604 | admits |
| Depth | 40 | 4 | admits |
| Per-file bytes | 2,097,152 | 524,621 | admits |
| Aggregate bytes | 1,073,741,824 | 314,772,270 | admits; default 512 MiB also admits |
| Work reserve | 201,326,592 | 29,378,776 | admits |
| Read-set files | 20,000 | 600 | admits |
| Staged files | 5,000 | 600 | admits; default 2,000 also admits |
| Receipt | 1,000 / 64 KiB | 0 / 0 | admits |
| Journal bytes | **2 GiB override** | 629,544,540 | admits; **default 512 MiB does not** |

Therefore the raw scope fits every read/scope default, but the complete pin+stage workload does not fit the published default journal quota. The RED test itself asserts only the 2 MiB file and 2,000-file claims.

## Instrument ruling

I uphold the builder’s choice. My earlier sampled-peak hard line was wrong for small-heap G1: the healthy 8-file control peaked at 254.76 MB while ending around 12.34 MB, and GREEN similarly peaked at 253.56 MB while retaining 14.40 MB. Sampled used heap is a valuable process-wide trend, not an attributable gate.

The hard evidence should be:

- no OOM at the declared `-Xmx`;
- forced-full-GC retention;
- cross-N retention flatness;
- attributable reservation within the work budget;
- output parity.

But this changes the existing MEM-011 instrument contract. The merge must amend that requirement through LID rather than letting `docs/txn-journal.md` silently contradict the battery specification.

The ordinary suites reproduced the disclosed old-main baseline:

- `test-fast`: 718 tests, 5 failures.
- `mcp-test`: 407 tests, 1 failure.
- All journal and scope-stream tests passed.
- `origin/main` is ahead and reported green; B1’s merge base is `ba0ffea`, so these 5+1 failures are not B1 fixes.

## Merge-queue findings

1. **BLOCKER — undetected recheck→rename race:** [txn_journal.clj:620](src/clj_surgeon/txn_journal.clj:620) — an injected writer placed `THEIRS` after the H0 check and before `publish-file!`; commit returned `{:ok true :committed true}` and final bytes were `OURS`. Atomic rename prevents tearing but is not compare-and-swap, and the window includes copying staging into the target directory.

2. **BLOCKER — scope membership is count-only:** [txn_journal.clj:529](src/clj_surgeon/txn_journal.clj:529) — planned membership `[a b]`, rewalked membership `[c d]`, equal count; commit succeeded and wrote one file. The sealed membership digest is never compared.

3. **FIX — deleted read-set file behaves correctly:** [txn_journal.clj:529](src/clj_surgeon/txn_journal.clj:529) — deleting the final read-only file produced `:txn-conflict`, `:actual-hash nil`, and `:files-written 0`.

4. **BLOCKER — lexical `..` is normalized away:** [txn_journal.clj:403](src/clj_surgeon/txn_journal.clj:403) — `/root/src/../src/in.clj` was successfully pinned and staged because `getCanonicalPath` removed `..` before `resolve-source-path` could reject it.

5. **BLOCKER — path identity is not pinned:** [txn_journal.clj:432](src/clj_surgeon/txn_journal.clj:432) — after pinning a regular file, replacing it with a symlink to identical bytes passed revalidation and committed, replacing the link. Record and recheck `NOFOLLOW_LINKS` type/file identity, not only content hash.

6. **PASS — direct escapes refuse before target bytes:** [txn_journal_test.clj:314](test/clj_surgeon/txn_journal_test.clj:314) — both an outside path and a symlink resolving outside returned `:txn-path-outside-workspace`; no staged file or live byte resulted.

7. **PASS — matching scope symlinks refuse before callback:** [scope_stream.clj:94](src/clj_surgeon/scope_stream.clj:94) — the witness returned `:scope-symlink-refused` with an empty callback ledger. This fail-closed default is correct; legitimate vendored links need a separately canonicalized source root/explicit opt-in contract, not implicit following.

8. **PASS — quota Q and Q+1:** [txn_journal_test.clj:245](test/clj_surgeon/txn_journal_test.clj:245) — exact Q admitted every pin/stage and restored all H0 bytes after the final injected write failure; one byte beyond Q refused before any live write.

9. **PASS — crash witnesses no longer pass vacuously:** [txn_journal_test.clj:448](test/clj_surgeon/txn_journal_test.clj:448) — recovery asserts zero restored paths before any rename and exactly two verified restored paths when killed after the second rename.

10. **BLOCKER FOR ADOPTION — pre-image lifetime is absent:** [txn_journal.clj:595](src/clj_surgeon/txn_journal.clj:595) — successful commit and failed rollback/recovery delete the transaction directory; the receipt is not undoable and no CAS lease/refcount prevents eviction. B2 must retain referenced objects, and failed restoration must preserve its journal instead of deleting the only recovery material.

11. **FIX — reservation is per-operation but incomplete:** [scope_stream.clj:198](src/clj_surgeon/scope_stream.clj:198) — `reserved-peak` is a local invocation accumulator, not a process sampler, but it accounts only for the largest parser reservation; `collect-entries` still retains and sorts every matching path.

12. **BLOCKER FOR MEM-001/011 — battery ignores the accountant:** [memory_battery_runner.clj:207](src/clj_surgeon/memory_battery_runner.clj:207) — `aggregate` hard-codes `:heap-reserved-peak-mb nil`; adopting the kernel unchanged would still report the reservation line UNMEASURED.

13. **FIX — MEM-001 has two authorities:** [memory-transaction-specs.md:28](docs/intent/memory/memory-transaction-specs.md:28) and [memory-boundedness-specs.md:33](docs/intent/memory-boundedness/memory-boundedness-specs.md:33) — merge must keep one canonical, unchecked MEM-001 in the battery specification and replace the kernel copy with a clause/backlink until an adopted verb returns the block and the battery consumes it.

14. **FIX — reflection/boxing remains in hot paths:** [txn_journal.clj:414](src/clj_surgeon/txn_journal.clj:414) and [scope_stream.clj:123](src/clj_surgeon/scope_stream.clj:123) — fresh reflection-enabled loading emitted **11 reflection warnings**: 10 journal and 1 scope-stream, plus 8 primitive/autoboxing warnings at scope line 324. Journal confinement runs twice per staged file and cleanup reflects per journal artifact; the scope boxing loop runs once per admitted file.

**GO-WITH-FIX — do not merge current HEAD; the memory design is sound enough to retain, but findings 1, 2, 4, 5, 10, and 12 must be closed or explicitly narrowed before the kernel enters the queue or is adopted by a verb.**