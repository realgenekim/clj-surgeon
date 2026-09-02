# Prepared-request Option A proxy — valid routing loss, recovery signal

Date: 2026-08-30

Experiment candidate: `4136cab85d22190f0f8f0e6b2f51e201ed1bdd97`

Candidate tree: `0403b6aad1875bd660b6bc89009698e44f51f00f`

Host: Anvil `dev-b`

Model/client: `gpt-5.6-sol`, high reasoning, Codex CLI `0.147.0`, ChatGPT subscription

## Verdict

The cohort is valid. Option A **fails the registered routing gate** and does not advance to the
leaf LLD, EARS requirements, red tests, or product code.

Control completed correct Surgeon-first mutations in 4/4 attempts. Treatment completed correct
Surgeon-first mutations in 2/4 attempts. The treatment difference is -50 percentage points, not
the required +25 points, and treatment missed its 3/4 minimum. All eight efficacy attempts were
semantically correct. Safety passed 4/4 with zero mutation attempts.

The ratified recovery claim receives a useful but non-promotional signal. Treatment reduced median
output by 30.0%, construction refusals by 42.9%, total recovery actions by 60.0%, and median complete
wall by 25.3%. These outcomes cannot rescue the failed primary. They justify a separate decision
about a recovery-oriented LLD gate; they do not authorize implementation.

## Registered gate

| Measure | Control | Treatment | Registered result |
|---|---:|---:|---|
| Correct Surgeon-first | 4/4 | 2/4 | fail: treatment minimum 3/4 |
| Treatment minus control | — | -50 pp | fail: required at least +25 pp |
| Successful absolute gain | — | -2 | fail: required at least +1 |
| Semantic correctness | 4/4 | 4/4 | pass: no loss |
| Attempts with refusal | 4/4 | 3/4 | pass: no increase |
| Read-only safety | 2/2 | 2/2 | pass: zero mutations |

All eight efficacy positions first attempted `edit_clojure`. Two treatment rows later completed by
another route: one through `apply_clojure_changes`, one through a native edit. The registered
primary uses the first completed successful mutation route, so neither row is laundered into a
Surgeon-first success.

## Every retained position

| Phase | Pos. | Arm | Correct | Exposure | First attempt | Successful route | Refusals | Wall | Output |
|---|---:|:---:|:---:|---:|---|---|---:|---:|---:|
| safety | 1 | C | yes | 0 | none | none | 0 | 12.641 s | 323 |
| safety | 2 | T | yes | 1 | none | none | 0 | 12.441 s | 299 |
| safety | 3 | T | yes | 1 | none | none | 0 | 13.895 s | 309 |
| safety | 4 | C | yes | 0 | none | none | 0 | 14.395 s | 354 |
| efficacy | 1 | C | yes | 0 | `edit_clojure` | `edit_clojure` | 1 | 52.027 s | 2,092 |
| efficacy | 2 | T | yes | 1 | `edit_clojure` | `apply_clojure_changes` | 1 | 57.694 s | 2,493 |
| efficacy | 3 | T | yes | 1 | `edit_clojure` | `edit_clojure` | 1 | 45.764 s | 1,758 |
| efficacy | 4 | C | yes | 0 | `edit_clojure` | `edit_clojure` | 2 | 72.174 s | 3,085 |
| efficacy | 5 | T | yes | 1 | `edit_clojure` | native | 2 | 67.464 s | 2,784 |
| efficacy | 6 | C | yes | 0 | `edit_clojure` | `edit_clojure` | 2 | 66.408 s | 2,990 |
| efficacy | 7 | C | yes | 0 | `edit_clojure` | `edit_clojure` | 2 | 93.861 s | 3,320 |
| efficacy | 8 | T | yes | 1 | `edit_clojure` | `edit_clojure` | 0 | 29.379 s | 1,041 |

## Recovery outcomes

| Descriptive measure | Control | Treatment | Treatment change |
|---|---:|---:|---:|
| Median output tokens | 3,037.5 | 2,125.5 | -912.0 / -30.0% |
| Construction refusals | 7 | 4 | -3 / -42.9% |
| Attempts with construction refusal | 4 | 3 | -1 |
| Total recovery actions | 20 | 8 | -12 / -60.0% |
| Total recovery tool calls | 11 | 6 | -5 / -45.5% |
| Median actions through mutation | 6 | 3 | -3 / -50.0% |
| Median complete wall | 69.291 s | 51.729 s | -17.562 s / -25.3% |

The sibling complete-request replication reported a 47.4% median-output reduction and six versus
zero construction refusals. This proxy experiment reproduces the direction, not that magnitude or
elimination. With four attempts per arm, the recovery results are decision evidence, not a
population rate or product speed claim.

## Evidence and scope

- candidate self-test: 104 tests, test-ID SHA-256
  `a7eb0108e13436dcaa403a7d56788095e5fdd691d7a49d4538eae031c11ba38e`;
- freeze SHA-256: `70818b2977805a6c821424ba47b637538aadddc9d91588776c1aa41f4e82a077`;
- zero-model private-server preflight: green;
- aggregate SHA-256: `050cb50dc5e21eb1e4f0993daf5dd6067a287507e41e248f6506c89f284fe7f1`;
- manifest SHA-256: `60a499b3c1635819addfa76e14cbb973f6e14d7f75c880832e733b4ec4c0974b`;
- archive SHA-256: `7e310f9434d2b60bcf33c40cfcfb0ed25dbf5f20adbe7fc5867715c2f5bcc9cf`;
- local archive:
  `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-30/prepared-request-proxy-4136cab-valid-kill.tar.gz`.

No product file, installed binary, shared MCP configuration, port 7888 process, or user source was
changed. The experiment used private candidate-owned MCP children on OS-assigned ports.

## Next decision

Keep the ratified HLD direction, but keep the leaf LLD and EARS registry deferred. The next decision
is whether the independently aligned recovery evidence is valuable enough to authorize a new,
forward-only recovery gate. That gate must measure construction refusals, recovery output, exact
correctness, and route fallback directly. It cannot reuse the failed routing threshold, rescore this
cohort, force inspection, or promote a product claim from these four treatment attempts.
