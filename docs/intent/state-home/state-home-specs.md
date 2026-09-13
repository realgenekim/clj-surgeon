# State-home identity requirements

These stable IDs extend the [warm-image state design](design.md). IDs are never reused or deleted.

- [x] **STATE-HOME-001**: When make warm starts in a clean checkout, its image identity publication shall leave git status empty after shutdown. Witness: state_home_warm.py, fresh committed copy and bounded exact-PID child.
- [x] **STATE-HOME-002**: When no image-file is supplied, startup and probe shall resolve the descriptor beneath the first configured state root in the declared precedence. Misreading: fallback to checkout state.
- [x] **STATE-HOME-003**: When distinct canonical workspace paths share a state root, their image descriptors shall have distinct content-addressed workspace directories. Misreading: key by HEAD or basename.
- [x] **STATE-HOME-004**: When image-file is supplied, probe shall read that location. Misreading: override only changes the receipt label.
- [x] **STATE-HOME-005**: When probe returns a receipt, it shall name its resolved descriptor path as image-file. Misreading: only successful receipts need provenance.
- [x] **STATE-HOME-006**: When the resolved descriptor is absent, probe shall refuse with probe-image-absent and its path before connecting. Misreading: every IOException means connection failure.
- [x] **STATE-HOME-007**: When identity publication encounters a non-writable state destination, it shall refuse with probe-state-not-writable, path and native errno category. Misreading: mkdirs returning false is success.
- [x] **STATE-HOME-008**: When identity publication resolves outside its declared envelope, it shall refuse before a write. Misreading: an environment path overrides envelope admission.

- [x] **STATE-HOME-009**: When the selected state root resolves inside the workspace, warm shall refuse with state-root-inside-workspace before creating state. Misreading: a writable checkout makes state publication acceptable. Boundaries: three precedence sources × direct/symlink/relative/empty × default/narrow envelope; witness configured-root-cartesian-matrix.
- [x] **STATE-HOME-010**: When the selected root resolves outside the pre-existing envelope, warm shall refuse with state-root-outside-envelope before creating state. Misreading: adding the selected root grants authority. Same Cartesian witness includes external positive controls.
- [x] **STATE-HOME-011**: When any descriptor publication stage fails, publication shall preserve the previous descriptor. Misreading: restoring after truncation is atomic. Boundaries: create, partial write, fsync, publish; witnesses publication-fault-matrix and native-efbig-preserves-descriptor also require zero temporary residue.
- [x] **STATE-HOME-012**: When native descriptor publication fails, its typed receipt shall name the bounded native errno category. Misreading: every IOException is unavailable. Boundaries: EFBIG, ENOSPC, EDQUOT, EACCES, EINTR, unknown short write; same publication witnesses.
- [x] **STATE-HOME-013**: When portability summaries are generated, their declared population shall equal the inventory population. Misreading: a constant header remains valid after an assignment is added. Witness generated-portability-census-agrees-with-all-inventories checks header, table, summary, controls, lane manifest and deftest census together.
- [x] **PROBE-RECEIPT-001**: When a pre-network descriptor filesystem stage fails, probe shall return a filesystem-typed refusal before attempting transport. Misreading: local IOExceptions are connection failures. Boundaries: resolution, absent, unreadable, directory, malformed, oversize, path too long and NUL; witness local-filesystem-failures-never-connect.
- [x] **PROBE-RECEIPT-002**: When probe returns a local filesystem refusal, the receipt shall carry the actual attempted path through bounded EDN encoding. Misreading: raw path text is safe to concatenate or an exception message supplies adequate provenance. Boundaries: newline/quote injection and oversized path diagnostics; witness local-filesystem-failures-never-connect.

## Refusal registration and repair reference

The vocabulary/remedy ledger is [probe/refusals.edn](../probe/refusals.edn).
Its rows carry the native failure, repair (`native_method`), owner and witness.
The independent frozen source census in `mcp-alias-migration-test` contains all
seven kinds below within its 176-kind pin. The vocabulary census witness is
`splice-envelope-test/bounded-input-path-encoding`; neither census substitutes
for the behavioral witnesses named in the ledger.

| Refusal kind | Native failure and repair | Existing intent |
|---|---|---|
| `probe-image-path-invalid` | Invalid/NUL/overlong path; correct the configured root or image-file path and retry. | PROBE-RECEIPT-001/002 |
| `probe-image-malformed` | Invalid descriptor EDN, shape or nesting; regenerate one bounded image descriptor with make warm. | PROBE-RECEIPT-001 |
| `probe-image-unreadable` | Access denial, directory or local read failure; restore readability at the reported path or select the intended file. | PROBE-RECEIPT-001 |
| `probe-image-too-large` | Descriptor exceeds its 8192-byte size or read bound; regenerate the small identity descriptor and correct a mistaken file override. | PROBE-RECEIPT-001 |
| `state-root-inside-workspace` | Canonical state root enters the checkout; select an external admitted root and correct symlink/relative targets. | STATE-HOME-009 |
| `state-root-outside-envelope` | State root exceeds existing write authority; select an external root already inside that envelope. | STATE-HOME-010 |
| `probe-state-not-writable` | Directory creation or publication fails; repair permissions, space or quota from the native errno, then retry make warm. | STATE-HOME-007/011/012 |

Registration checklist for a source-census refusal: name every missing kind and
its emitting owner; check both directions of the frozen enumeration and its
count; check the vocabulary/remedy EDN rows and owner membership; check this
repair reference and the owning intent; name the direct behavioral and census
witnesses and ensure the impact selection includes path-reading witnesses.


Round 7 supersedes the Round 5 oracle placement and cost allowance. The 72-cell
source × shape × envelope × destination class is enumerated by
state-home-admission-test/configured-root-cartesian-matrix in :fast against
state-root, canonicalization and admit-state-root!, using real temporary paths
and symlinks without child JVMs. The default envelope is represented by its
fixture runtime root; a narrow launcher envelope admits only runtime/allowed.
probe-state-test/configured-root-warm-cells runs five fresh committed Make copies:
two root refusal kinds once each and one valid destination per source, under
120 seconds total. The original four-mode warm witness remains unchanged.
The real-process helper stops the exact warm PID, waits for Make and bounded
process-group disappearance, then deletes. Cleanup retries once after 250 ms,
records the original error in the cell row, and propagates a second failure.
The complete battery must pass below 1,800,000 ms serial-equivalent with margin;
a namespace allowance or fixed-point impact run cannot establish that gate.
