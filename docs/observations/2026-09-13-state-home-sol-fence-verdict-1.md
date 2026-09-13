NO-GO

CLASS: SECOND RECURRENCE — envelope writers whose configured state root resolves into the checkout or widens the pre-existing destination envelope. Oracle: a Cartesian state-root matrix over all three precedence sources, direct/symlink/relative/empty paths, and default/narrow envelopes, running `make warm` in fresh committed copies and asserting both canonical destination exclusion and clean Git status.

`warm!` grants the selected root authority by adding it to the envelope before admission ([probe_state.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/probe_state.clj:88)). The adversarial resolver/writer probe produced:

```clojure
{:case :absolute-state-home-in-checkout,
 :path ".../checkout/workspaces/09e523.../probe.edn",
 :exists true}
```

Then:

```text
git -C .../checkout status --porcelain
?? workspaces/
```

A symlinked state root printed the same canonical checkout destination with `:inside-checkout true`. This recreates the incident through `CLJ_SURGEON_STATE_HOME`; equivalent XDG and `user.home` shapes remain in the class. The Landlock scan cannot detect this member because the checkout is deliberately writable.

CLASS: descriptor publication failures after the final file is opened or truncated. Oracle: fault-inject every write-stage errno/short-write class—at least EFBIG, ENOSPC, EDQUOT, EACCES and interruption—and require preservation of the prior descriptor, no temporary residue, and the correct errno.

[write-image!](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/probe_state.clj:64) writes directly to the final file with `Files/write`. Under `ulimit -f 1`, writing a 10,000-byte descriptor returned:

```clojure
{:error-type :probe-state-not-writable,
 :errno :unavailable,
 :native-class "java.io.IOException",
 :native-message "File too large"}
```

`wc -c` then reported `1024` bytes, and the file began `{:payload "xxxxx...`. Publication therefore destroyed the previous complete descriptor, left partial state, and failed to name EFBIG.

CLASS: local descriptor-resolution/read failures classified as transport failures or escaping the receipt boundary. Oracle: exercise every pre-network filesystem stage—canonicalization, absent, EACCES, directory, malformed EDN and oversize—and assert one bounded, encoded, filesystem-typed receipt containing the actual path and zero network attempts.

The path resolver runs outside `cli!`’s exception boundary ([probe.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/probe.clj:195)). A generated long `:image-file` produced an uncaught `java.io.IOException: Filename too long` stack trace and no receipt. Separate probes returned:

```clojure
{:case :malformed,  :error-type :probe-connection-failed, :error "EOF while reading"}
{:case :unreadable, :error-type :probe-connection-failed, :error "... (Permission denied)"}
```

Both occurred before any network attempt, contradicting the newly narrowed `probe-connection-failed` contract.

CLASS: generated census summaries whose declared population diverges from their machine-readable inventory. Oracle: compare the Markdown header/table/summary, controls map, lane manifest, and deftest census in one generation check.

Parsing `portability-controls.edn` printed:

```clojure
{:entries 160,
 :classes {:bb-load-incompatible 50, :portable 97, :non-portable 13}}
```

But [portability-census.md](/home/forge/src/clj-surgeon-fence/docs/observations/2026-09-12-bbtower-block-b/attempt22/portability-census.md:3) still claims “All 159 assigned namespaces are listed.”

Other reviewed pressure points did not produce blockers: workspace keys hash the canonical workspace path; `:battery`/`:jvm` registration and the 6/28 JVM versus four bb launcher failures are measured; the witness uses a fresh committed copy and exact exec-preserved PID; and stripping `:telemetry_dropped` from cross-runtime equality is appropriate because the test separately asserts JVM `16` versus bb absence before comparing operation-contract fields.

No `make test` was run and no patch was left. The temporary adversarial fixture was moved to trash.

> END RECEIPT (fence-run): worktree HEAD at review exit = 561307096b879ff57909b42d954bd3db8275b74b = fenced sha.
