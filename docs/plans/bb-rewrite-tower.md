# Babashka-first hybrid, block A

Authorized by Gene's September 11 build brief, on bb-rewrite-tower-local from
eae1e432. This is a hybrid, not a port: portable CLI/library/planner work and
tests use babashka; MCP and unavailable JVM dependencies retain the JVM.

The runtime inventory records actual isolated bb namespace loads, including
the first missing library/class. Load portability does not prove test parity.
TEST-ISO-001 keeps cadence separate from runtime: no existing namespace or
test Var may disappear when execution moves. A portable declaration whose
namespace cannot load must fail with that namespace's name.

The probe is an inner-loop verdict, never cold proof. One repository-owned
MCP image exposes a loopback probe endpoint. The bb CLI sends a namespace and
worktree/image identity. Startup identity binds canonical root, generation,
classpath configuration and server implementation. A mismatching image refuses
before reload or test execution. Ordinary source edits remain reloadable.
The existing hot-verification namespace owns reload and test execution.

Observable results: probe-passed or probe-failed, verification_complete false,
proof_pending [landing-gate], reloaded namespaces, test/assertion/failure
counts and elapsed_ms. Empty/missing test namespaces, malformed requests,
unreachable servers and stale identity cannot become passing verdicts.

Witness matrix: valid and malformed namespace; absent image; wrong root,
generation and classpath; red test, repaired test and elapsed time; runtime
membership completeness; named bb load failure; unchanged census coverage.
Expected verdicts are literals, independent of the runtime selector.

Validation: bb library, twenty verb groups, new test-fast, final-tip prewarm,
lint through ~/bin/clj-kondo and census diff. Resource-blocked checks remain
OWED rather than being bypassed. In particular the base coordinator invokes
Python for kernel slot admission, which the brief forbids; do not remove the
admission fence merely to obtain a timing. The before coordinator measurement
is OWED unless an authorized compatible entrance exists.

Meter: five real edit-to-verdict observations each for focused cold JVM,
focused cold bb and warm probe on the same portable namespace. Shell date
+%s.%N brackets edit completion through verdict. These observations are seat
measurements, not a statistically admitted performance claim.

Dogfood: insert_forms request files for top-level insertions, rename_alias for
alias changes, compact edit for matching replacements. Native patch is reserved
for file creation and non-Clojure/configuration edits or unsupported structures.
Every source edit and typed refusal is retained in the final report.
