# Warm image identity outside the checkout

The warm image's identity is seat state, not project source. Incident inb-3c65d7
made every clean-checkout PROBE cell refuse after `make warm` wrote its descriptor.
This leaf extends the warm verification design: one canonical workspace path,
hashed with SHA-256, selects one descriptor under the declared seat state root.

Root precedence is CLJ_SURGEON_STATE_HOME, XDG_STATE_HOME/clj-surgeon,
then user.home/.local/state/clj-surgeon. A shared resolver serves startup and
probe; an explicit image-file remains authoritative. Publication admits the
resolved destination through the existing envelope before creating directories.
Native filesystem failures carry the path, exception class and errno category.
Probe receipts include the descriptor path even when reading or transport fails.

Misreadings: ignoring the checkout write is not a repair; hashing repository HEAD
would merge distinct checkouts; missing files are not dead connections; a requested
state path does not authorize bypassing a narrower destination envelope.

Boundary matrix: environment precedence, two workspace roots, override, missing
file, non-writable root, successful write/read, stale descriptor, refused envelope,
and actual Make startup followed by exact-process shutdown and empty git status.
Other .clj-surgeon consumers, telemetry policy and .gitignore are out of scope.

The registered build authorizes the red-first implementation cycle in this leaf.
Verification: named affected namespaces, serialized lint, fixed-point diff-impact
and each namespace it selects. No merge, push or performance claim is included.

- [ ] **STATE-HOME-001**: When make warm starts in a clean checkout, its image identity publication shall leave git status empty after shutdown. Witness: state_home_warm.py, fresh committed copy and bounded exact-PID child.
- [ ] **STATE-HOME-002**: When no image-file is supplied, startup and probe shall resolve the descriptor beneath the first configured state root in the declared precedence. Misreading: fallback to checkout state.
- [ ] **STATE-HOME-003**: When distinct canonical workspace paths share a state root, their image descriptors shall have distinct content-addressed workspace directories. Misreading: key by HEAD or basename.
- [ ] **STATE-HOME-004**: When image-file is supplied, probe shall read that location. Misreading: override only changes the receipt label.
- [ ] **STATE-HOME-005**: When probe returns a receipt, it shall name its resolved descriptor path as image-file. Misreading: only successful receipts need provenance.
- [ ] **STATE-HOME-006**: When the resolved descriptor is absent, probe shall refuse with probe-image-absent and its path before connecting. Misreading: every IOException means connection failure.
- [ ] **STATE-HOME-007**: When identity publication encounters a non-writable state destination, it shall refuse with probe-state-not-writable, path and native errno category. Misreading: mkdirs returning false is success.
- [ ] **STATE-HOME-008**: When identity publication resolves outside its declared envelope, it shall refuse before a write. Misreading: an environment path overrides envelope admission.
