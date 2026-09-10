NO-GO

# Seat-path defaults fence, round 3 — sealed candidate `1b077cb787839dbf3f6ddaf54449c27301fdfe0c`

## Findings

### SPF-004 — artifact-root validation writes before refusal and fails open when filesystem proof is unavailable

`validate-artifact-root!` calls `mkdirs` before its ownership and RAM-backed checks. Executed against the sealed tree, both `/tmp/codex-seat-path-r3-probe` and `/dev/shm/codex-seat-path-r3-probe` were created and then rejected as `:artifact-root-ram-backed`. A default rooted below a `$HOME/.local/state` symlink into `/dev/shm` behaved the same way: it was rejected, but only after creating `clj-surgeon/artifacts` on tmpfs. This contradicts ALIAS-MIGRATION-001's requirement that validation occur before any write.

The self-contained RAM predicate is also not equivalent to the documented fail-closed `base-refusal`. With `findmnt` unavailable, `validate-artifact-root!` accepted `/run/user/1011/codex-seat-path-r3-probe`; an independent `findmnt` call proved that path is on `tmpfs`. `base-refusal` has an authoritative mounts-table/Darwin fallback and refuses an unknown filesystem type, while the production duplicate catches every lookup failure and treats it as non-RAM. The candidate therefore cannot substantiate “real disk (never RAM-backed).” A correct cross-platform repair needs production-available fail-closed filesystem classification and pre-write resolution of the nearest existing/canonical ancestor; it is not a safe fence one-liner.

### SPF-005 — alias telemetry bypasses the validator

`clj-surgeon.mcp-alias-migration/append-telemetry!` constructs its directory directly from `artifacts/*artifact-root*` and immediately calls `mkdirs`; it never calls `validate-artifact-root!` or `artifacts/directory`. With `CLJ_SURGEON_ARTIFACT_ROOT=/dev/shm/codex-seat-path-r3-telemetry`, the executed entrance wrote `alias-migration-receipts/ledger.edn` on tmpfs. Thus “called by every writer” is false, and the parity override is not accepted only through the same predicate.

This is residue of SPF-001, but closing it together with SPF-004 requires consolidating the production path-classification contract and covering every writer. No fix patch is attached; a partial one-line guard on telemetry would leave the fail-open validator and write-before-refusal defect intact.

## Checks that passed

- HEAD is the sealed merge `1b077cb787839dbf3f6ddaf54449c27301fdfe0c` (base `13635f748c58767aa37d261ebb98b9e0a03465f5`, branch tip `3dd61167e0a975c8b201f7768b11febca7b6fe81`).
- With both selectors unset, the derived root lands at `$HOME/.local/state/clj-surgeon/artifacts`. A real-disk symlinked `$HOME` is accepted. `/tmp`, `/dev/shm`, and a tmpfs reached through a path under `$HOME` are recognized when `findmnt` succeeds. `/var/tmp/forge` is accepted by `validate-artifact-root!`.
- The in-flight parity receipt uses the exact variable `CLJ_SURGEON_ARTIFACT_ROOT=/var/tmp/forge`; the candidate's name and selected-root semantics match that invocation.
- `test/clj_kondo_admission_path_test.sh` passed. Its Python fallback witness executed Python, and its nested fresh-worktree comparison remained clean; the round-3 `__pycache__` repair is effective.
- Each other added witness was run from its own fresh detached worktree after classpath prewarm. Alias-root tests: 8 tests / 16 assertions / 0 failures; pressure-status test: 1 / 2 / 0; MEMBAT source witness: 1 / 3 / 0. Each before/after `git status --porcelain --ignored` comparison was unchanged.
- The repaired placement assertion is substantive: planting `/definitely/wrong-root` made `receipt-directories-are-deterministic-and-workspace-isolated` red with exactly 1 failure (2 assertions still passed).
- No `make test` was run.

At review exit, the only pre-existing worktree dirt is `docs/observations/seat-path-defaults-fence.md.codex.log`; this verdict file is the sole review output.


> END RECEIPT (fence-run): worktree HEAD at review exit = 1b077cb787839dbf3f6ddaf54449c27301fdfe0c = fenced sha.
