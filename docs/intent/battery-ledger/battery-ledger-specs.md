---
parent: battery-ledger-design
prefix: BATTERY-LEDGER
---

# Battery evidence specifications

- [x] **BATTERY-LEDGER-001**: When the battery receipt writer runs, it shall append its candidate entry only when BATTERY_LEDGER_APPEND equals `1`; otherwise it shall print the candidate marked `not appended (BATTERY_LEDGER_APPEND unset)` without changing ledger bytes. Pass and fail use the same decision. Freshness arithmetic is unchanged.
- [x] **BATTERY-LEDGER-002**: When the battery records namespace walls, it shall write them under the shared seat state root at `battery/namespace-walls.edn`, leaving the tracked seed unchanged. CLJ_SURGEON_STATE_HOME precedes XDG_STATE_HOME/clj-surgeon and the user-home default.
- [x] **BATTERY-LEDGER-003**: While the state walls file is absent, when the battery schedules its next run, it shall read namespace and per-Var estimates from the tracked seed. Existing state takes precedence.
- [x] **BATTERY-LEDGER-004**: When `make census-regenerate` is invoked, it shall print the census difference as +N/-M and exit nonzero without writing when a name is removed, including a rename. Ordinary make gates shall remain unable to regenerate their oracle.
