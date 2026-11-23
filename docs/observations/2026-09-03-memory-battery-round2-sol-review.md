# memory-battery c6a2264 — Sol executed round-2 review: GO-WITH-FIX as tooling (verifier symlink/directory holes; reference hand-spoofable; root guard) — round 3 launched

GO-WITH-FIX as tooling. The original blocking fixes work, but round two introduced two corpus-verifier holes and the “attestation” is not tamper-resistant. Main’s three hard `held_mb` failures are expected product RED, not an apparatus objection. Checkout remains clean at `c6a2264`.

Self-test summary, verbatim:

```text
generate_tree verification self-test: ok
generate_tree self-test: ok

Testing clj-surgeon.memory-battery-test

Ran 24 tests containing 138 assertions.
0 failures, 0 errors.
```

`make test-fast` also passed: 726 tests, 6,050 assertions, zero failures/errors.

The one authorized fresh-root full run used `/home/forge/tmp/membat-sol2` under the exclusive lock. Its verdict line, verbatim:

```text
verdict: FAIL (INCOMPLETE)   exit 1
```

Receipt: [20260903T064701.069493358Z-battery.edn](/home/forge/tmp/membat-sol2/receipts/20260903T064701.069493358Z-battery.edn:1).

### Round-two findings

The giant/nested peaks are real transient, shape-driven allocation, but their magnitude is strongly controlled by G1 scheduling and available heap—not live retention:

| Heap | Giant peak | Nested peak | Held / grow |
|---|---:|---:|---:|
| 512m targeted reruns | 352.7–391.4 MiB | 260.2–311.1 MiB | giant ≈3.1 MiB / 0; nested 0 / 0 |
| 1g targeted reruns | 453.3–495.5 MiB | 363.4–471.6 MiB | unchanged |
| 4g fresh reference | 762.2 MiB | 1006.8 MiB | unchanged |

Thus 386.4/285.7 MiB was not a bogus sample, but neither number is the object’s retained cost. The higher heap allows more garbage to accumulate before collection. The allocation source is the double parser in [outline.clj:275](src/clj_surgeon/outline.clj:275), reached through the parallel materializer at [core.clj:321](src/clj_surgeon/core.clj:321).

Ownership splits cleanly:

- Nested/token-dense input belongs to `MCP-OP-MEM-005`: lexical node and depth admission before full rewrite-clj construction.
- The giant file first belongs to `MCP-OP-MEM-002`’s per-file byte admission/worker reservation; if admitted by bytes, MEM-005 remains the second parser-shape guard. Opus’s 2 MiB ceiling is indeed too large for one 192 MiB work budget; lower it toward ~512 KiB or reserve and serialize it explicitly.

Corpus probes:

- Identical bytes with mtime changed from `1788417009` to `978307200`: correctly remained `verified`.
- Unlisted symlink: typed `unexpected-files`, exit 2.
- Expected path replaced by an outside-root symlink containing identical bytes: incorrectly reported `verified`; `.isFile` follows it at [generate_tree.clj:300](bench/memory_battery/generate_tree.clj:300).
- Expected path replaced by a directory: detected initially as missing, then regeneration threw an untyped `FileNotFoundException (Is a directory)` at the write in [generate_tree.clj:406](bench/memory_battery/generate_tree.clj:406).

Attestation probes:

- Another corpus digest correctly produced `stale-reference` with `:fields [:corpus-digests]`.
- All of `:src-digest`, `:generator-digest`, `:ops-digest`, and `:jvm` are spoofable by hand-editing `reference-hashes.edn`; so are `:ops`, `:corpus-digests`, and the parity hashes.
- A hand-written reference with current attestation fields and `:hashes {:forged "arbitrary"}` passed `memory-battery-attest`. The reference bytes have no separately anchored digest or signature; [run-attest!:507](src/clj_surgeon/memory_battery_runner.clj:507) only compares editable fields. This is useful stale-cache binding, not adversarial attestation.

The silent-filter idiom has no executable duplicate in battery code. The only `(#{:default nil} nil)` match is the explanatory comment at [memory_battery.clj:275](src/clj_surgeon/memory_battery.clj:275).

### Mayor merge-queue verdict: GO-WITH-FIX

1. **CLOSED — peak disposition and 248 MiB arithmetic.** [memory_battery.clj:123](src/clj_surgeon/memory_battery.clj:123), [memory-battery.md:117](docs/memory-battery.md:117) — my peak-only probe returned `:status :pass`, exit 0, with `:peak-scales-with-n` only under `:trends`; Xmx reruns demonstrated the expected drift.

2. **CLOSED — retention semantics split.** [memory_battery_runner.clj:179](src/clj_surgeon/memory_battery_runner.clj:179), [memory_battery.clj:409](src/clj_surgeon/memory_battery.clj:409) — the full table separately printed `held_mb`, `excl_mb`, and `grow_mb`; adversarial `grow_mb` remained zero.

3. **CLOSED — INCOMPLETE is terminal.** [memory_battery.clj:65](src/clj_surgeon/memory_battery.clj:65), [memory_battery_test.clj:99](test/clj_surgeon/memory_battery_test.clj:99) — direct rerun returned `{:status :incomplete, :pass? false, :complete? false, :exit 4}`; adding OOM returned `:fail`, exit 1.

4. **CLOSED — exact held cross-N gate.** [memory_battery.clj:21](src/clj_surgeon/memory_battery.clj:21), [memory_battery_test.clj:249](test/clj_surgeon/memory_battery_test.clj:249) — the 1.0→9.8 witness failed at limit 3.0; the full battery failed the same line for three operations.

5. **PARTIAL — corpus shapes.** [generate_tree.clj:132](bench/memory_battery/generate_tree.clj:132), [memory_battery.clj:90](src/clj_surgeon/memory_battery.clj:90) — CLJC, giant, and nested arms ran with parity, but the 17 KiB-mean and 450×1.9 MiB admission arms remain explicitly unimplemented.

6. **CLOSED — fast-gate isolation.** [Makefile:865](Makefile:865), [memory_battery_test.clj:578](test/clj_surgeon/memory_battery_test.clj:578) — `make -n` found no full-battery invocation from `test`, `test-fast`, `mcp-test`, or `runtests`; both executed suites passed.

7. **PARTIAL — corpus/reference trust.** [generate_tree.clj:304](bench/memory_battery/generate_tree.clj:304), [memory_battery_runner.clj:309](src/clj_surgeon/memory_battery_runner.clj:309) — deletion printed `generated:missing-file`, digest tampering printed `generated:digest-mismatch`, and stale corpus identity was typed; expected-path symlinks, directory collisions, and hand-spoofable reference fields remain.

8. **PARTIAL — execution/root safety.** [Makefile:829](Makefile:829), [Makefile:855](Makefile:855) — battery code contains no socket or port reference and contacted no forbidden port, but the shared arbitrary `MEMBAT_ROOT` still lacks a marker/realpath guard, and a fresh run automatically launches the 4g reference.