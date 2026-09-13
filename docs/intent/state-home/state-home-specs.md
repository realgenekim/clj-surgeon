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
