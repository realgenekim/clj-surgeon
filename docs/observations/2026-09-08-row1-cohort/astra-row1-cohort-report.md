| Arm (fresh replication) | Wall: request → closure + accepted review | Accepted | Stale |
|---|---:|:---:|:---:|
| W1 | 109.6 s | Yes | No |
| C1 | 118.3 s | Yes | No |
| C2 | Not reached (return 35.9 s) | No | No |
| W2 | 100.8 s | Yes | No |
| W3 | Invalidated (110.3 s first) | No | No |
| C3 | 114.1 s | Yes | No |
| C4 | 116.0 s | Yes | No |
| W4 | 105.6 s | Yes | No |
| W5 | 100.0 s | Yes | No |
| C5 | 117.4 s | Yes | No |
| C6 | Invalidated (120.2 s first) | No | No |
| W6 | 104.3 s | Yes | No |

**NO ADMISSION.** Registered median paired W/C = ∞ (required ≤0.75); accepted W 5/6 and C 4/6; stale closures 0.

**Learning:** W callers read the receipt during proof in 6/6 arms, but 0/6 finished an accepted note before closure. Accepted primary medians were 104.3 s W and 116.7 s C; the finite accepted-pair median W/C was 0.911 (sensitivity only). Caller action counts are retained per arm (W3 used four because it revised its note); the single required status read and caller termination are retained as secondary timings. Useful overlap did not by itself establish the registered 25% complete-task saving.

**Caveat:** The original twelve-arm cohort was invalidated by my runner rejecting W1's expected `committed-probe-only` state and allowing its detached gate to overlap C1; its strict token-boundary oracle also rejected worktree-qualified caller paths. All original results, including W1's randomized `store-test/sink-filtering-test` closure failure, are retained. The table above is one complete fresh replication, preregistered before any of its runs, using corrected state handling and the requested literal-string presence oracle; no individual failures were replaced. W3 and C6 revised notes after first acceptance, invalidating their endpoints; C6's revision also removed three owner names (138/141 remain), which the registered minimal presence oracle does not reject. Replication C2's cold unit suite failed `exports-test/schedule-version-test` (seed 658765265), and the tool rolled back; its missing endpoint is scored as +∞ for that pair, with no retry. This remains a mandated Sol/Cell-C result with warm preload outside the request clock.

- Replication freeze: `/var/tmp/forge/row1-cohort/replication/freeze.edn`.
- Replication results: `/var/tmp/forge/row1-cohort/replication/results.edn` (JSON mirror: `/var/tmp/forge/row1-cohort/replication/results.json`).
- Original freezes/results: `/var/tmp/forge/row1-cohort/freeze.edn`, `/var/tmp/forge/row1-cohort/freeze-v2.edn`, `/var/tmp/forge/row1-cohort/results.edn`.
- Preregistration: `/var/tmp/forge/plan2/cellC/astra-row1-cohort-prereg.md`.
