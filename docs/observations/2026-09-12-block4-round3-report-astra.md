NO-GO

Both check-only tooling defects are fixed in `b096c6ec7a9eaa788fcba320b8c1ba3d036438c2`.
Overall verification remains **blocked** on the expected N1 packet-envelope denial
and fresh scoped circuits. Independent acceptance remains external.

Execution manifest: `b300a73fee4b60b4870262335741be79cba4b794b471edd07a4ed40a799f8aa7`.
Starting subject: detached `a51f2024a2ae686c348e75922e75cf3280c5079a`.

1. **State access.** Check-only packets recognize the origin basename, falling back
   to the worktree basename when no origin exists. Names starting with
   `clj-surgeon` receive exactly the passwd home's `.local/state/clj-surgeon`
   directory as an additional write root, created when absent. Both
   `environment.edn` and the sealed manifest record it. Build write roots retain
   their previous envelope. The witness substitutes an owned passwd home and
   exercises actual Landlock child writes: check-only succeeds; build, sibling
   and redirected-HOME writes are denied. See the retained
   [check manifest](.check-only-evidence/check-packet/manifest.edn) and
   [build manifest](.check-only-evidence/build-packet/manifest.edn).
2. **Report delivery.** Check-only execution creates missing EDN and Markdown
   parents, writes keyword `:pass`/`:fail`, and retains check rows and log paths.
   Separate absent directories pass for both check exits 0 and 2. The exit-2
   packet returns 4 and leaves [report.edn](.check-only-evidence/red-report.edn)
   and [REPORT.md](.check-only-evidence/red-REPORT.md). The same test against the
   original subject reproduces the writer's FileNotFoundException in both cases
   ([control](.check-only-evidence/baseline-report.log)).
3. **Witnesses and packaging.** All eight check-only/reviewer tests and all 15
   measurement/traceability tests pass. Two linked intents cover the new promises.
   Native registry lint reports zero errors and warnings. The staged manifest is
   regenerated and verifies. Protected launcher, SHIP, installer, ledger and
   prewarm-admission files are byte-identical to the supplied subject
   ([validation](.check-only-evidence/source-validation.json)). Exact-tip/tree
   mismatch refusals remain exercised by the prewarm witness.
4. **Required suite.** `bash test/run.sh` exits 1 after 52.435 seconds at the
   unchanged N1 attempt to create a directory directly under
   `/var/tmp/forge/packets` ([log](.check-only-evidence/full-suite.log),
   [clock and argv](.check-only-evidence/full-suite.json)). N2–N9 and N11 pass
   separately. Publication, ledger, lifecycle, ship, board, compatibility,
   production-content, the real 61-second heartbeat, and fixture checks all pass
   ([continuation](.check-only-evidence/continuation.log)). Scoped circuits exit 1
   with an explicit OWED message. The installer reaches the same N1 denial on
   installed bytes, emits `INSTALL NOT PROVEN`, and successfully rolls back
   ([installer log](.check-only-evidence/install.log)). No other continuation
   entrance is red.

No model or JVM was launched, no product repository or shared installation was
changed, and no server or publication was started outside local test fixtures.
The current sealed assignment admits only the zero-JVM witness suite; prewarm
and battery remain owed to an eligible executor. Fresh real-home product checks,
N1 outside this packet, scoped circuits, installation and independent acceptance
remain owed to their named owners in [report.edn](report.edn).

Least sure: real passwd-home product execution is not witnessed inside this build
envelope. Traceability proves witnessed intent, not independent acceptance.
Setup mistakes and corrected fixture placement are recorded in the
[evidence notes](.check-only-evidence/README.md); neither changed an oracle.
The final report commit's own SHA is printed with delivery rather than embedded
in its contents.
