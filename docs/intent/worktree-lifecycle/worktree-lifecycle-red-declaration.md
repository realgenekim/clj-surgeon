---
parent: worktree-lifecycle-design
requirements: worktree-lifecycle-specs
---

# Worktree Lifecycle Frozen Red Declaration

The MVP red gate is frozen before product code.

| Stratum | Tests | Assertions | Expected failures | Expected errors |
|---|---:|---:|---:|---:|
| Pure compiler | 21 | 128 | 25 | 0 |
| Fixture-only I/O and apply | 13 | 66 | 12 | 0 |
| CLI and Make boundary | 4 | 21 | 3 | 0 |
| **Total red** | **38** | **215** | **40** | **0** |
| **Green target** | **38** | **215** | **0** | **0** |

The 40 failures correspond one-for-one to the 40 active `WTL-*`
requirements. Deferred `WTL-CLI-004` is outside this gate. Fixture tests own
their temporary roots and never apply to a pre-existing worktree. The first
real close trial remains a separate Gene-gated phase.
