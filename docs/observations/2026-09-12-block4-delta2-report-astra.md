NO-GO

The ratified oracle adjustment and both rounds of source changes are committed.
The requested history and leakage witnesses pass. The broader verification remains
**blocked**: N1 has its expected packet Landlock denial, and fresh scoped-circuit
proof for this payload remains owed. Independent acceptance stays external.

Manifest: `5880584605962d87f592661b0f8c246ee1436f0aabdca00b6a2981d9d91bdaea`.
Base: detached `286f3f9259dc8c9ed9ed57ced6a756031ae70d6a`.

1. `lib/measure_store.py` retains the round-1 per-process UUID epoch, with explicit
   `LEDGER_EPOCH` precedence. All 15 measurement tests pass on this delta, including
   process separation, repeated emissions and the linked-intent contract.
2. `lib/production_content.py` applies Fable's ratified proposal: journaled event
   IDs and epochs each detect contamination; a shared source-history run ID alone
   does not. The test entry point delegates to this implementation, and the
   installer payload and staged manifest include it. No production history changed.
3. All six literal cases are executable in `test/production-content-witness.py`.
   The retained exact 75 source rows reproduce 75 run-ID overlaps and zero epoch
   or event-ID overlaps against a scratch ledger holding the actual 00:48Z imports.
   Replay passes. A copied fixture event retaining only its journaled epoch is
   rejected; a copied event with an unrelated epoch and journaled event ID is also
   rejected. Restoring the scratch ledger passes. The fixture source hash is pinned.
   The old run-ID-only negative is replaced by these two witnesses, and the active
   `PRODUCTION-CONTENT-001` registry row quotes Fable's ratification verbatim.
4. The sealed old oracle fails both the run-only matrix case and exact replay
   ([fresh old-oracle control](.epoch-evidence/round2/old-oracle.log)); the ratified
   implementation passes ([new witnesses](.epoch-evidence/round2/production-content.log)).
   These tests run through the existing production-content suite entry.
5. `bash test/run.sh` exits 1 after 42.910 seconds at N1,
   following passing measurements, packet tests, refusals and Fix 1
   ([full log](.epoch-evidence/round2/full-suite.log),
   [clock/argv/exit](.epoch-evidence/round2/full-suite.json)).
   The continuation keeps each later exit explicit
   ([results](.epoch-evidence/round2/continuation.log)). The circuit witness requires
   fresh Claude/Codex subscription receipts for the changed closure; historical
   circuit receipts cannot satisfy it. Its oracle and launch setting were preserved.
6. A frozen scratch source copy passes installed-byte measurements, reaches the
   same N1 denial, reports `INSTALL NOT PROVEN`, and rolls back all added files
   ([installer](.epoch-evidence/round2/install.log)). Fable owns external N1,
   circuit qualification, installation and independent acceptance. The current
   manifest declares only the witness suite; no JVM gates or model launches ran.

The intentional `FAIL production ledger content` lines are asserted negative
witnesses on scratch files. Actual production isolation checks pass. No live ledger,
shared install, server or packet evidence was modified.

Round-1 logs remain historical in `.epoch-evidence/`; its complete fixture tree,
including nested Git metadata, is committed as an archive with a file-hash index
([archive instructions](.epoch-evidence/README.md)). Fresh evidence is confined to
`round2/`. [Validation](.epoch-evidence/round2/validation.log) and
[subject file hashes](.epoch-evidence/round2/subject.json) identify the reviewed bytes.
Raw retained logs include original trailing whitespace; evidence bytes were not cleaned.
The pre-existing two modified tracked Python cache files were left untouched and
are excluded from these commits.

Commits recorded before the final report commit:

- `8382ceb5c6a852f55c90610159a23ca28e37999d`
- `ac39ab974a18d465e57ab1e4b8346f44227afdfc`
- `7c2bab6c680bc884cc74af851d285b255a7decd4`

The final report commit is the detached HEAD printed with delivery; its own hash
cannot be embedded in its contents. `report.edn` records the commits preceding it.

Least sure: fresh circuit qualification of the changed payload has not been
observed. Linked-intent traceability proves witnessed intent, not independent
acceptance. No disagreement with the ratified identity rule remains.
