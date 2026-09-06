1. Apparatus fence

- Expected HEAD: `d34ff7ff`
- Observed before any probe: `ba407bb8715c748b4750ea0920acf3932100e47d`
- Command: `git -C /home/forge/src/clj-surgeon-fence rev-parse HEAD`

2. Result

Apparatus fault. Per the binding rule, I stopped immediately without inspecting, executing, or changing anything. No LAND/HOLD verdict is issued.

> END RECEIPT (fence-run): worktree HEAD at review exit = ba407bb8715c748b4750ea0920acf3932100e47d, NOT the fenced sha ba407bb8 — this verdict may describe the wrong tree; re-run.
