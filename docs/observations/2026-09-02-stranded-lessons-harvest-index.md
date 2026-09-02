# Stranded-lessons harvest — index

Built 2026-09-02 by the bridge seat, under Gene's instruction: "make sure we're
getting all lessons (i fear some maybe stranded on branches???)".

**What this branch is.** `bridge/harvest-stranded-lessons-2026-09-02` is a
documentation-only harvest branched from `origin/main` at
`64eac2ee37278d1aa2f51a012e5e6e8d81a9ef51`. It collects every captain's log,
intent doc, decision record, plan, and evidence artifact under `docs/` that
today exists ONLY on some other branch — i.e. is absent from `origin/main` —
so the mayor can pull this one branch into main without any lesson staying
stranded on a branch nobody merges. Nothing outside `docs/` was touched: no
source, no tests, no Makefile.

Every branch in the ledger below (`obs_docs_only_on_branch > 0`) was walked in
`last-date ascending, then name` order. For each branch, files ADDED relative
to `origin/main` under `docs/` were copied byte-for-byte via `git show
BRANCH:PATH`. When two branches added the *same path* with *identical*
content, the second (and later) copy was recorded as a corroborating "same"
event and no duplicate file was written. When two branches added the same
path with *different* content, the later branch's version was written
alongside the first under a `.from-<branch-slug>` suffix so both survive —
neither the mayor nor a future reader has to guess which is authoritative.

## Totals

- Branches scanned (obs_docs_only_on_branch > 0): **76**
- Unique doc paths harvested (canonical, one per path): **670**
- Content variants (same path, different content, second+ branch): **24**
- Total files landed on disk (670 + 24, excluding this index): **694**
  - Markdown reports/logs/plans/decisions (`.md`): **148**
  - Raw evidence artifacts (`.jsonl` 179, `.json` 121, `.txt` 81, `.toml` 41,
    `.clj` 40, `.edn` 39, `.stderr` 38, `.sha256` 5, `.mjs` 2 = **546**)
  - Total bytes: ~11.5 MB (largest single file ~926 KiB)
- Corroborating "same content" events not written as new files: **359**
  (i.e. 359 additional (branch, path) pairs across the ledger where a later
  branch added byte-identical content to a path another branch had already
  placed — nothing lost, nothing duplicated)
- Docs files MODIFIED (not added) relative to `origin/main` across these
  branches, NOT harvested by this pass (a merge would still change these):
  **28** — see the per-branch table below.

**Known ledger discrepancy:** `origin/bench/threeway-acid-20260831` is listed
in the input ledger with `obs_docs_only_on_branch=1`, but a direct
`git diff --name-only --diff-filter=A origin/main...BRANCH -- docs` (filtered
to paths absent from `origin/main`) found **4** branch-only doc files, all
genuinely absent from main. All 4 were harvested per the algorithm (which
computes its own file list from git, not from the ledger count). The ledger
column is a hint for triage, not a correctness authority.

## Per-branch harvest

| Branch | Author | Last date | Commits ahead | Not-upstream (patch-id) | Files harvested | Files modified (not harvested) |
|---|---|---|---|---|---|---|
| `origin/codex/telemetry-ratchets-20260829` | sol | 2026-08-29 | 2 | 2 | 1 | 0 |
| `origin/docs/captains-logs-2026-08-29` | fable | 2026-08-29 | 11 | 8 | 5 | 0 |
| `origin/experiment/claude-route-result-price-tag-20260829` | route-hint | 2026-08-29 | 1 | 1 | 1 | 0 |
| `origin/experiment/codex-catalog-floor-sweep` | surgeon1 | 2026-08-29 | 9 | 9 | 2 | 1 |
| `origin/experiment/emission-compression-screen` | surgeon2 | 2026-08-29 | 324 | 5 | 2 | 5 |
| `origin/experiment/three-arm-request-shape-prereq` | Gene Kim | 2026-08-29 | 251 | 10 | 4 | 5 |
| `origin/audit/adoption-gap-attribution-20260830` | surgeon2 | 2026-08-30 | 5 | 5 | 4 | 0 |
| `origin/audit/eligibility-characterization-20260831` | surgeon2 | 2026-08-30 | 2 | 2 | 1 | 0 |
| `origin/audit/eligibility-characterization-fourth-audit-20260831` | surgeon2 | 2026-08-30 | 6 | 6 | 1 | 0 |
| `origin/audit/eligibility-characterization-reaudit-20260831` | surgeon2 | 2026-08-30 | 3 | 3 | 1 | 0 |
| `origin/audit/eligibility-characterization-third-audit-20260831` | surgeon2 | 2026-08-30 | 4 | 4 | 1 | 0 |
| `origin/audit/prepared-confirm-affinity-7e0300fe-20260831` | surgeon1 | 2026-08-30 | 7 | 7 | 3 | 4 |
| `origin/audit/prepared-confirm-preview-05f5a196-20260830` | surgeon2 | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/audit/prepared-confirm-session-affinity-20260831` | surgeon2 | 2026-08-30 | 2 | 2 | 2 | 0 |
| `origin/audit/substantiation-telemetry-independent-20260830` | surgeon2 | 2026-08-30 | 4 | 4 | 5 | 0 |
| `origin/audit/substantiation-telemetry-second-successor-20260830` | surgeon2 | 2026-08-30 | 7 | 7 | 5 | 0 |
| `origin/audit/substantiation-telemetry-successor-20260830` | surgeon2 | 2026-08-30 | 5 | 5 | 5 | 0 |
| `origin/audit/substantiation-telemetry-w1-rebase-20260831` | surgeon2 | 2026-08-30 | 9 | 9 | 6 | 0 |
| `origin/audit/write-refusal-001-independent-20260830` | surgeon2 | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/audit/write-refusal-completeness-20260830` | sol | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/bench/acid-crossover-ladder-20260830` | sol | 2026-08-30 | 4 | 4 | 3 | 0 |
| `origin/docs/acejump-designs-20260830` | sol | 2026-08-30 | 4 | 4 | 4 | 0 |
| `origin/docs/acejump-postmortem-20260830` | sol | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/docs/conn-review-20260830` | sol | 2026-08-30 | 7 | 7 | 7 | 0 |
| `origin/docs/dreamlist-designs-20260830` | sol | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/docs/embedded-elaborator-ratification-20260830` | surgeon2 | 2026-08-30 | 3 | 3 | 3 | 1 |
| `origin/docs/fast-typist-designs-20260831` | sol | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/docs/grave-revisit-update-20260830` | sol | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/docs/prepared-confirm-preview-ratification-20260830` | surgeon2 | 2026-08-30 | 2 | 1 | 1 | 3 |
| `origin/docs/prepared-request-ratification-20260830` | surgeon1 | 2026-08-30 | 1 | 1 | 2 | 0 |
| `origin/docs/prepared-request-recovery-lld-20260830` | surgeon1 | 2026-08-30 | 12 | 12 | 9 | 0 |
| `origin/docs/splice-reference-ratification-20260830` | surgeon2 | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/docs/substantiation-telemetry-ratification-20260830` | surgeon1 | 2026-08-30 | 5 | 5 | 4 | 0 |
| `origin/docs/sweep-lane1-complete-refusal-ab` | sol | 2026-08-30 | 16 | 13 | 6 | 0 |
| `origin/experiment/consumption-gap-20260830` | sol | 2026-08-30 | 2 | 2 | 4 | 0 |
| `origin/experiment/differential-routing-interview-20260829` | sol | 2026-08-30 | 12 | 9 | 6 | 0 |
| `origin/experiment/embedded-spark-probe-20260830` | sol | 2026-08-30 | 1 | 1 | 5 | 0 |
| `origin/experiment/external-corpus-shape-census` | surgeon1 | 2026-08-30 | 4 | 4 | 3 | 0 |
| `origin/experiment/fromto-overlap-study-20260830` | sol | 2026-08-30 | 3 | 3 | 2 | 0 |
| `origin/experiment/fuel-table-completion-20260831` | sol | 2026-08-30 | 3 | 3 | 1 | 0 |
| `origin/experiment/native-prelanding-gate-audit` | surgeon2 | 2026-08-30 | 4 | 4 | 3 | 0 |
| `origin/experiment/ordinal-refusal-screen-20260830` | sol | 2026-08-30 | 2 | 2 | 1 | 0 |
| `origin/experiment/prepared-confirm-affinity-live-measure-20260831` | surgeon1 | 2026-08-30 | 10 | 10 | 4 | 4 |
| `origin/experiment/prepared-confirm-preview-live-token-measurement` | surgeon1 | 2026-08-30 | 4 | 4 | 2 | 0 |
| `origin/experiment/prepared-request-proxy-screen-20260830` | surgeon1 | 2026-08-30 | 11 | 11 | 9 | 0 |
| `origin/experiment/prepared-request-replication-20260830` | sol | 2026-08-30 | 15 | 12 | 7 | 0 |
| `origin/experiment/read-normalization-live-token-measurement` | surgeon1 | 2026-08-30 | 3 | 3 | 2 | 0 |
| `origin/experiment/redesign-exemplars-20260830` | Gene Kim | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/experiment/rename-verb-screen-20260830` | sol | 2026-08-30 | 4 | 4 | 2 | 0 |
| `origin/experiment/spark-caller-screen-20260830` | mayor | 2026-08-30 | 4 | 4 | 2 | 0 |
| `origin/experiment/spark-isolation-screen-20260830` | sol | 2026-08-30 | 3 | 3 | 37 | 0 |
| `origin/experiment/splice-adversarial-replication-20260830` | sol | 2026-08-30 | 7 | 7 | 505 | 0 |
| `origin/experiment/splice-reference-screen-20260830` | sol | 2026-08-30 | 4 | 4 | 262 | 0 |
| `origin/experiment/substantiation-overhead-20260830` | surgeon1 | 2026-08-30 | 6 | 6 | 5 | 0 |
| `origin/experiment/substantiation-overhead-w1-rebase-20260831` | surgeon1 | 2026-08-30 | 14 | 14 | 8 | 0 |
| `origin/experiment/turn-waste-mining-20260830` | sol | 2026-08-30 | 2 | 2 | 1 | 0 |
| `origin/experiment/verb-algebra-census-20260830` | sol | 2026-08-30 | 2 | 2 | 4 | 0 |
| `origin/experiment/walls-of-text-exhibit-20260830` | sol | 2026-08-30 | 1 | 1 | 2 | 0 |
| `origin/experiment/warm-executor-screen-20260830` | sol | 2026-08-30 | 4 | 4 | 4 | 0 |
| `origin/experiment/write-refusal-001-live-token-measurement` | surgeon1 | 2026-08-30 | 5 | 5 | 3 | 0 |
| `origin/feature/embedded-elaborator-20260830` | sol | 2026-08-30 | 8 | 8 | 39 | 1 |
| `origin/feature/prepared-confirm-affinity-guidance-20260831` | surgeon2 | 2026-08-30 | 6 | 6 | 2 | 4 |
| `origin/feature/prepared-request-first-slice-20260830` | surgeon1 | 2026-08-30 | 3 | 3 | 3 | 0 |
| `origin/feature/substantiation-telemetry-w1-rebase-20260831` | surgeon1 | 2026-08-30 | 8 | 8 | 5 | 0 |
| `origin/feature/write-refusal-completeness-20260830` | surgeon1 | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/prep/substantiation-w1-witness-20260830` | surgeon1 | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/release/prepared-request-published-20260830` | surgeon1 | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/release/read-normalization-published-20260830` | surgeon1 | 2026-08-30 | 2 | 2 | 1 | 0 |
| `origin/release/write-refusal-001-published-20260830` | surgeon1 | 2026-08-30 | 1 | 1 | 1 | 0 |
| `origin/audit/eligibility-legibility-refactor-20260831` | surgeon2 | 2026-08-31 | 7 | 7 | 1 | 0 |
| `origin/bench/threeway-acid-20260831` | sol | 2026-08-31 | 6 | 6 | 4 | 0 |
| `origin/docs/captains-log-2026-08-30-install-night` | mayor | 2026-08-31 | 9 | 9 | 9 | 0 |
| `origin/docs/fleet-opines-20260831` | sol | 2026-08-31 | 1 | 1 | 1 | 0 |
| `origin/docs/speed-confirm-20260831` | sol | 2026-08-31 | 1 | 1 | 1 | 0 |
| `origin/docs/transcript-audit-20260831` | sol | 2026-08-31 | 1 | 1 | 1 | 0 |
| `origin/bridge/captains-log-wall-clock-ideal` | forge-bridge | 2026-09-02 | 2 | 2 | 1 | 0 |

## Full provenance

One row per file landed on disk (`placed` = first branch to add this path;
`variant` = a later branch that added the same path with *different*
content, written as `<name>.from-<branch-slug><ext>`). "Also identical on"
counts additional branches whose commit added byte-identical content to the
same path (recorded, not duplicated on disk).

| File on disk | Status | Source branch (added it) | Commit | Author | Also identical on |
|---|---|---|---|---|---|
| `docs/experiments/2026-08-31-threeway-acid-wall-battery-prereg.md` | placed | `origin/bench/threeway-acid-20260831` | 57ea69e | sol | — |
| `docs/experiments/2026-08-31-w1-product-cohort-correction-prereg.md` | placed | `origin/bench/threeway-acid-20260831` | 6017ac0 | sol | — |
| `docs/experiments/2026-08-31-w1-product-cohort-prereg.md` | placed | `origin/bench/threeway-acid-20260831` | 093f44e | sol | — |
| `docs/intent/2026-08-30-prepared-request-ratification/prepared-request-design.from-docs--prepared-request-recovery-lld-20260830.md` | variant | `origin/docs/prepared-request-recovery-lld-20260830` | 0228fbe | surgeon1 | — |
| `docs/intent/2026-08-30-prepared-request-ratification/prepared-request-design.from-experiment--prepared-request-proxy-screen-20260830.md` | variant | `origin/experiment/prepared-request-proxy-screen-20260830` | 0228fbe | surgeon1 | — |
| `docs/intent/2026-08-30-prepared-request-ratification/prepared-request-design.md` | placed | `origin/docs/prepared-request-ratification-20260830` | 0228fbe | surgeon1 | — |
| `docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs.from-docs--prepared-request-recovery-lld-20260830.md` | variant | `origin/docs/prepared-request-recovery-lld-20260830` | 0228fbe | surgeon1 | — |
| `docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs.from-experiment--prepared-request-proxy-screen-20260830.md` | variant | `origin/experiment/prepared-request-proxy-screen-20260830` | 0228fbe | surgeon1 | — |
| `docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs.md` | placed | `origin/docs/prepared-request-ratification-20260830` | 0228fbe | surgeon1 | — |
| `docs/intent/2026-08-30-splice-reference-ratification/README.md` | placed | `origin/docs/splice-reference-ratification-20260830` | f5504e6 | surgeon2 | — |
| `docs/intent/embedded-elaborator/embedded-elaborator-design.md` | placed | `origin/docs/embedded-elaborator-ratification-20260830` | d6a334f | surgeon2 | 1 other branch(es) |
| `docs/intent/embedded-elaborator/embedded-elaborator-specs.from-feature--embedded-elaborator-20260830.md` | variant | `origin/feature/embedded-elaborator-20260830` | 9601c5f | surgeon2 | — |
| `docs/intent/embedded-elaborator/embedded-elaborator-specs.md` | placed | `origin/docs/embedded-elaborator-ratification-20260830` | d6a334f | surgeon2 | — |
| `docs/intent/embedded-elaborator/frozen-red-declaration.md` | placed | `origin/feature/embedded-elaborator-20260830` | 2145b75 | sol | — |
| `docs/intent/substantiation-telemetry/README.from-audit--substantiation-telemetry-second-successor-20260830.md` | variant | `origin/audit/substantiation-telemetry-second-successor-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/README.from-audit--substantiation-telemetry-w1-rebase-20260831.md` | variant | `origin/audit/substantiation-telemetry-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/README.from-docs--substantiation-telemetry-ratification-20260830.md` | variant | `origin/docs/substantiation-telemetry-ratification-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/README.from-experiment--substantiation-overhead-20260830.md` | variant | `origin/experiment/substantiation-overhead-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/README.from-experiment--substantiation-overhead-w1-rebase-20260831.md` | variant | `origin/experiment/substantiation-overhead-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/README.from-feature--substantiation-telemetry-w1-rebase-20260831.md` | variant | `origin/feature/substantiation-telemetry-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/README.md` | placed | `origin/audit/substantiation-telemetry-independent-20260830` | 4831b8a | surgeon1 | 1 other branch(es) |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-design.from-audit--substantiation-telemetry-second-successor-20260830.md` | variant | `origin/audit/substantiation-telemetry-second-successor-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-design.from-audit--substantiation-telemetry-w1-rebase-20260831.md` | variant | `origin/audit/substantiation-telemetry-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-design.from-docs--substantiation-telemetry-ratification-20260830.md` | variant | `origin/docs/substantiation-telemetry-ratification-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-design.from-experiment--substantiation-overhead-20260830.md` | variant | `origin/experiment/substantiation-overhead-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-design.from-experiment--substantiation-overhead-w1-rebase-20260831.md` | variant | `origin/experiment/substantiation-overhead-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-design.from-feature--substantiation-telemetry-w1-rebase-20260831.md` | variant | `origin/feature/substantiation-telemetry-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-design.md` | placed | `origin/audit/substantiation-telemetry-independent-20260830` | 4831b8a | surgeon1 | 1 other branch(es) |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-frozen-red.md` | placed | `origin/audit/substantiation-telemetry-independent-20260830` | 4831b8a | surgeon1 | 7 other branch(es) |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-audit--substantiation-telemetry-second-successor-20260830.md` | variant | `origin/audit/substantiation-telemetry-second-successor-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-audit--substantiation-telemetry-w1-rebase-20260831.md` | variant | `origin/audit/substantiation-telemetry-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-docs--substantiation-telemetry-ratification-20260830.md` | variant | `origin/docs/substantiation-telemetry-ratification-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-experiment--substantiation-overhead-20260830.md` | variant | `origin/experiment/substantiation-overhead-20260830` | 4831b8a | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-experiment--substantiation-overhead-w1-rebase-20260831.md` | variant | `origin/experiment/substantiation-overhead-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-feature--substantiation-telemetry-w1-rebase-20260831.md` | variant | `origin/feature/substantiation-telemetry-w1-rebase-20260831` | a27b0b0 | surgeon1 | — |
| `docs/intent/substantiation-telemetry/substantiation-telemetry-specs.md` | placed | `origin/audit/substantiation-telemetry-independent-20260830` | 4831b8a | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-29-captains-log-independent-adoption-census.md` | placed | `origin/audit/adoption-gap-attribution-20260830` | 286aa20 | surgeon1 | 2 other branch(es) |
| `docs/observations/2026-08-29-captains-log-six-designs-died-and-the-tool-was-barely-used.md` | placed | `origin/docs/captains-logs-2026-08-29` | dd897b5 | mayor | 3 other branch(es) |
| `docs/observations/2026-08-29-captains-log-the-490-ms-tax-became-41-ms.md` | placed | `origin/experiment/codex-catalog-floor-sweep` | 87a53ec | surgeon1 | — |
| `docs/observations/2026-08-29-captains-log-the-predictions-that-failed.md` | placed | `origin/docs/captains-logs-2026-08-29` | cfcafdb | mayor | 3 other branch(es) |
| `docs/observations/2026-08-29-captains-log-what-the-six-withdrawals-mean.md` | placed | `origin/docs/captains-logs-2026-08-29` | b3ee1ee | mayor | 3 other branch(es) |
| `docs/observations/2026-08-29-claude-inspect-result-price-tag-routing-experiment.md` | placed | `origin/experiment/claude-route-result-price-tag-20260829` | bada0b4 | route-hint | — |
| `docs/observations/2026-08-29-codex-catalog-floor-sweep-protocol.md` | placed | `origin/experiment/codex-catalog-floor-sweep` | 56055b1 | surgeon1 | — |
| `docs/observations/2026-08-29-complete-owner-vocabulary-causal-ab.md` | placed | `origin/docs/sweep-lane1-complete-refusal-ab` | 2126368 | sol | — |
| `docs/observations/2026-08-29-emission-compression-composition-screen.md` | placed | `origin/experiment/emission-compression-screen` | fbe65ef | surgeon2 | — |
| `docs/observations/2026-08-29-emission-compression-option-portfolio.md` | placed | `origin/experiment/emission-compression-screen` | 93dd258 | surgeon2 | — |
| `docs/observations/2026-08-29-external-corpus-shape-census-preregistration.md` | placed | `origin/audit/adoption-gap-attribution-20260830` | 502f70a | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-29-measurements-and-how-to-repeat-them.md` | placed | `origin/docs/captains-logs-2026-08-29` | d3ab026 | mayor | 3 other branch(es) |
| `docs/observations/2026-08-29-native-prelanding-parse-gate-prize-audit.md` | placed | `origin/experiment/native-prelanding-gate-audit` | a02368a | surgeon2 | — |
| `docs/observations/2026-08-29-three-arm-request-shape-zero-model-receipt.md` | placed | `origin/experiment/three-arm-request-shape-prereq` | 7844bc6 | Gene Kim | — |
| `docs/observations/2026-08-30-acejump-docs-postmortem.md` | placed | `origin/docs/acejump-postmortem-20260830` | a78c660 | sol | — |
| `docs/observations/2026-08-30-acid-crossover-ladder-result.md` | placed | `origin/bench/acid-crossover-ladder-20260830` | f6ac0b5 | sol | — |
| `docs/observations/2026-08-30-acid-regression-crossover-preregistration.md` | placed | `origin/bench/acid-crossover-ladder-20260830` | 62785e1 | sol | — |
| `docs/observations/2026-08-30-acid-regression-gate-result.md` | placed | `origin/bench/acid-crossover-ladder-20260830` | 00a0c6b | sol | — |
| `docs/observations/2026-08-30-adoption-gap-attribution.md` | placed | `origin/audit/adoption-gap-attribution-20260830` | 628e6d1 | surgeon2 | — |
| `docs/observations/2026-08-30-captains-log-a-second-mind-in-the-server.md` | placed | `origin/docs/conn-review-20260830` | 1154027 | mayor | 1 other branch(es) |
| `docs/observations/2026-08-30-captains-log-spark-and-the-inverse-surprise.md` | placed | `origin/docs/conn-review-20260830` | 92f47f9 | mayor | 1 other branch(es) |
| `docs/observations/2026-08-30-captains-log-the-day-the-routing-story-died-twice.md` | placed | `origin/docs/conn-review-20260830` | 5d28b13 | mayor | 1 other branch(es) |
| `docs/observations/2026-08-30-captains-log-the-night-the-arrow-walked-itself.md` | placed | `origin/docs/conn-review-20260830` | 12541d0 | mayor | 1 other branch(es) |
| `docs/observations/2026-08-30-captains-log-the-night-the-verifiers-ran-the-ship.md` | placed | `origin/docs/captains-log-2026-08-30-install-night` | d267c49 | mayor | — |
| `docs/observations/2026-08-30-captains-log-the-storyboard-that-earned-a-wow.md` | placed | `origin/docs/conn-review-20260830` | 21b2409 | mayor | 1 other branch(es) |
| `docs/observations/2026-08-30-captains-log-warm-executor-paid-for-itself.md` | placed | `origin/experiment/warm-executor-screen-20260830` | 9b6c970 | sol | — |
| `docs/observations/2026-08-30-conn-rulings-adversarial-review.md` | placed | `origin/docs/conn-review-20260830` | 2f39190 | sol | — |
| `docs/observations/2026-08-30-consumption-gap-preregistration.md` | placed | `origin/experiment/consumption-gap-20260830` | 2ef7f7e | sol | — |
| `docs/observations/2026-08-30-consumption-gap-result.md` | placed | `origin/experiment/consumption-gap-20260830` | 1648d5d | sol | — |
| `docs/observations/2026-08-30-differential-routing-interview-and-ablations.md` | placed | `origin/experiment/differential-routing-interview-20260829` | a9afcd1 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-dream-list-fulfillment-designs.md` | placed | `origin/docs/dreamlist-designs-20260830` | 7c937a4 | sol | — |
| `docs/observations/2026-08-30-embedded-spark-elaborator-probe.md` | placed | `origin/experiment/embedded-spark-probe-20260830` | 44a5bac | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-embedded-spark-elaborator-probe/app-server-transcript.jsonl` | placed | `origin/experiment/embedded-spark-probe-20260830` | 44a5bac | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-embedded-spark-elaborator-probe/installed-cli-recon.json` | placed | `origin/experiment/embedded-spark-probe-20260830` | 44a5bac | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-embedded-spark-elaborator-probe/probe-app-server.mjs` | placed | `origin/experiment/embedded-spark-probe-20260830` | 44a5bac | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-embedded-spark-elaborator-probe/probe-receipt.json` | placed | `origin/experiment/embedded-spark-probe-20260830` | 44a5bac | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-external-corpus-write-shape-census.md` | placed | `origin/audit/adoption-gap-attribution-20260830` | 28ee81f | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-from-to-double-repetition-study.md` | placed | `origin/experiment/fromto-overlap-study-20260830` | 0a0a65f | sol | — |
| `docs/observations/2026-08-30-gpt-5-3-spark-caller-screen.md` | placed | `origin/experiment/spark-caller-screen-20260830` | 2b2a417 | sol | — |
| `docs/observations/2026-08-30-grave-revisit-plan.from-docs--grave-revisit-update-20260830.md` | variant | `origin/docs/grave-revisit-update-20260830` | f9eafb3 | sol | — |
| `docs/observations/2026-08-30-grave-revisit-plan.md` | placed | `origin/docs/captains-logs-2026-08-29` | 8ac7cde | fable | 3 other branch(es) |
| `docs/observations/2026-08-30-label-addressed-write-designs.md` | placed | `origin/docs/acejump-designs-20260830` | 9c57fa7 | sol | — |
| `docs/observations/2026-08-30-ordinal-refusal-recovery-screen.md` | placed | `origin/experiment/ordinal-refusal-screen-20260830` | 00bac4d | sol | — |
| `docs/observations/2026-08-30-prepared-confirm-preview-live-route-protocol.md` | placed | `origin/experiment/prepared-confirm-preview-live-token-measurement` | f88c672 | surgeon1 | — |
| `docs/observations/2026-08-30-prepared-confirm-preview-live-route-token-measurement.md` | placed | `origin/experiment/prepared-confirm-preview-live-token-measurement` | db6b0fb | surgeon1 | — |
| `docs/observations/2026-08-30-prepared-confirm-preview-repaired-successor-independent-go.md` | placed | `origin/audit/prepared-confirm-preview-05f5a196-20260830` | 7370c96 | surgeon2 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-installed-route.md` | placed | `origin/release/prepared-request-published-20260830` | ff2e8ab | surgeon1 | — |
| `docs/observations/2026-08-30-prepared-request-live-route-attempt1-invalid.md` | placed | `origin/docs/acejump-designs-20260830` | d426126 | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-live-route-protocol.md` | placed | `origin/docs/acejump-designs-20260830` | 919ae8a | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-live-route-token-measurement.md` | placed | `origin/docs/acejump-designs-20260830` | 9f2b1ba | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-option-a-proxy-invalid-safety-stop.md` | placed | `origin/docs/prepared-request-recovery-lld-20260830` | 64f8661 | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-option-a-proxy-matched-refusal-stop.md` | placed | `origin/docs/prepared-request-recovery-lld-20260830` | 4136cab | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-option-a-proxy-protocol.md` | placed | `origin/docs/prepared-request-recovery-lld-20260830` | 709f5ef | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-option-a-proxy-safety-root-stop.md` | placed | `origin/docs/prepared-request-recovery-lld-20260830` | 693cadf | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-option-a-proxy-safety-shape-stop.md` | placed | `origin/docs/prepared-request-recovery-lld-20260830` | 5303552 | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-option-a-proxy-stdin-stop.md` | placed | `origin/docs/prepared-request-recovery-lld-20260830` | 71d8300 | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-option-a-proxy-valid-verdict.md` | placed | `origin/docs/prepared-request-recovery-lld-20260830` | ab5759e | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-prepared-request-replication.md` | placed | `origin/experiment/prepared-request-replication-20260830` | 6277e06 | sol | — |
| `docs/observations/2026-08-30-production-parse-gate-reopening-screen.md` | placed | `origin/experiment/native-prelanding-gate-audit` | e9d2201 | surgeon2 | — |
| `docs/observations/2026-08-30-read-normalization-live-route-protocol.md` | placed | `origin/experiment/read-normalization-live-token-measurement` | 5294ce9 | surgeon1 | — |
| `docs/observations/2026-08-30-read-normalization-live-route-token-measurement.md` | placed | `origin/experiment/read-normalization-live-token-measurement` | abb70ae | surgeon1 | — |
| `docs/observations/2026-08-30-read-request-normalization-install-receipt.md` | placed | `origin/release/read-normalization-published-20260830` | 91cdda4 | surgeon1 | — |
| `docs/observations/2026-08-30-rename-verb-screen-results.md` | placed | `origin/experiment/rename-verb-screen-20260830` | ec66ed5 | sol | — |
| `docs/observations/2026-08-30-spark-isolation-screen/DESIGN.md` | placed | `origin/experiment/spark-isolation-screen-20260830` | e97ba83 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/RESULT.md` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/hardening.config.toml` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-kill-1-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-kill-1-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-kill-2-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-kill-2-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-kill-3-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-kill-3-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-1-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-1-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-2-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-2-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-3-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-3-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-restart-1-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-restart-1-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-restart-2-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-restart-2-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-restart-3-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/lifecycle-restart-3-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-E-B-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-E-B-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-E-S-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-E-S-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-H-B-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-H-B-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-H-S-observer.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/matrix-H-S-transcript.jsonl` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/screen-app-server.mjs` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/screen-receipt.json` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-spark-isolation-screen/sha256-manifest.txt` | placed | `origin/experiment/spark-isolation-screen-20260830` | 3c2cc19 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-splice-reference-adversarial-replication-preregistration.md` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 65b2164 | sol | — |
| `docs/observations/2026-08-30-splice-reference-adversarial-replication-result.md` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/2026-08-30-splice-reference-screen-preregistration.md` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 65679e5 | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-splice-reference-screen-result.md` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/2026-08-30-substantiation-telemetry-independent-no-go.md` | placed | `origin/audit/substantiation-telemetry-independent-20260830` | 316a564 | surgeon2 | — |
| `docs/observations/2026-08-30-substantiation-telemetry-overhead-screen.md` | placed | `origin/experiment/substantiation-overhead-20260830` | 7dcf1fa | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-substantiation-telemetry-second-successor-independent-go.md` | placed | `origin/audit/substantiation-telemetry-second-successor-20260830` | 9e7c51f | surgeon2 | — |
| `docs/observations/2026-08-30-substantiation-telemetry-successor-independent-no-go.md` | placed | `origin/audit/substantiation-telemetry-successor-20260830` | 01529db | surgeon2 | — |
| `docs/observations/2026-08-30-the-dream-session-north-star.md` | placed | `origin/docs/conn-review-20260830` | 7fe1c38 | mayor | 1 other branch(es) |
| `docs/observations/2026-08-30-transform-verb-expressibility-census.md` | placed | `origin/experiment/verb-algebra-census-20260830` | abaf7e6 | sol | — |
| `docs/observations/2026-08-30-transformation-redesign-exemplars.md` | placed | `origin/experiment/redesign-exemplars-20260830` | a5d8267 | Gene Kim | — |
| `docs/observations/2026-08-30-turn-waste-mining.md` | placed | `origin/experiment/turn-waste-mining-20260830` | 367470c | sol | — |
| `docs/observations/2026-08-30-walls-of-text-exhibit.md` | placed | `origin/experiment/walls-of-text-exhibit-20260830` | 5d3834e | sol | — |
| `docs/observations/2026-08-30-warm-executor-screen-preregistration.md` | placed | `origin/experiment/warm-executor-screen-20260830` | d1ce1b2 | sol | — |
| `docs/observations/2026-08-30-warm-executor-screen-recovery-addendum.md` | placed | `origin/experiment/warm-executor-screen-20260830` | 2855471 | sol | — |
| `docs/observations/2026-08-30-warm-executor-screen-replacement-addendum.md` | placed | `origin/experiment/warm-executor-screen-20260830` | 2ee7f0f | sol | — |
| `docs/observations/2026-08-30-write-refusal-001-first-green.md` | placed | `origin/experiment/write-refusal-001-live-token-measurement` | 4bb5783 | surgeon1 | 1 other branch(es) |
| `docs/observations/2026-08-30-write-refusal-001-independent-verification.md` | placed | `origin/audit/write-refusal-001-independent-20260830` | 29e417c | surgeon2 | — |
| `docs/observations/2026-08-30-write-refusal-001-installed-release.md` | placed | `origin/docs/embedded-elaborator-ratification-20260830` | 4ec9394 | surgeon1 | 3 other branch(es) |
| `docs/observations/2026-08-30-write-refusal-001-live-route-protocol.md` | placed | `origin/experiment/write-refusal-001-live-token-measurement` | 1d59315 | surgeon1 | — |
| `docs/observations/2026-08-30-write-refusal-001-live-route-token-measurement.md` | placed | `origin/experiment/write-refusal-001-live-token-measurement` | b2274cb | surgeon1 | — |
| `docs/observations/2026-08-30-write-side-refusal-completeness-audit.md` | placed | `origin/audit/write-refusal-completeness-20260830` | e418c85 | sol | — |
| `docs/observations/2026-08-31-caller-transcript-emission-audit.md` | placed | `origin/docs/transcript-audit-20260831` | 6fd50f5 | sol | — |
| `docs/observations/2026-08-31-captains-log-the-nine-hour-watch.md` | placed | `origin/docs/captains-log-2026-08-30-install-night` | 74190f6 | mayor | — |
| `docs/observations/2026-08-31-elaborator-fuel-table.md` | placed | `origin/experiment/fuel-table-completion-20260831` | c390afb | sol | — |
| `docs/observations/2026-08-31-eligible-descriptor-legibility-refactor-audit.md` | placed | `origin/audit/eligibility-legibility-refactor-20260831` | ecae7b1 | surgeon2 | — |
| `docs/observations/2026-08-31-embedded-elaborator-production-verification/RESULT.md` | placed | `origin/feature/embedded-elaborator-20260830` | 1062d41 | sol | — |
| `docs/observations/2026-08-31-embedded-elaborator-production-verification/d1-receipt.json` | placed | `origin/feature/embedded-elaborator-20260830` | 1062d41 | sol | — |
| `docs/observations/2026-08-31-embedded-elaborator-production-verification/idle-receipt.json` | placed | `origin/feature/embedded-elaborator-20260830` | 3f5fb45 | sol | — |
| `docs/observations/2026-08-31-fast-typist-utilization-designs.md` | placed | `origin/docs/fast-typist-designs-20260831` | 9fc3110 | sol | — |
| `docs/observations/2026-08-31-openrouter-fast-elaborator-shootout.md` | placed | `origin/docs/captains-log-2026-08-30-install-night` | 3872330 | mayor | — |
| `docs/observations/2026-08-31-prepared-confirm-affinity-live-route-protocol.md` | placed | `origin/experiment/prepared-confirm-affinity-live-measure-20260831` | f508017 | surgeon1 | — |
| `docs/observations/2026-08-31-prepared-confirm-affinity-live-route-token-measurement.md` | placed | `origin/experiment/prepared-confirm-affinity-live-measure-20260831` | 5fb3445 | surgeon1 | — |
| `docs/observations/2026-08-31-prepared-confirmation-affinity-independent-go.md` | placed | `origin/audit/prepared-confirm-affinity-7e0300fe-20260831` | 9529f70 | surgeon1 | — |
| `docs/observations/2026-08-31-prepared-confirmation-affinity-repair.md` | placed | `origin/audit/prepared-confirm-affinity-7e0300fe-20260831` | 7e0300f | surgeon2 | 2 other branch(es) |
| `docs/observations/2026-08-31-prepared-confirmation-session-affinity-field-audit.md` | placed | `origin/audit/prepared-confirm-session-affinity-20260831` | c8b0716 | surgeon2 | — |
| `docs/observations/2026-08-31-prepared-request-eligibility-characterization-audit.md` | placed | `origin/audit/eligibility-characterization-20260831` | c5fed62 | surgeon2 | — |
| `docs/observations/2026-08-31-prepared-request-eligibility-fourth-audit.md` | placed | `origin/audit/eligibility-characterization-fourth-audit-20260831` | 1386f8d | surgeon2 | — |
| `docs/observations/2026-08-31-prepared-request-eligibility-successor-reaudit.md` | placed | `origin/audit/eligibility-characterization-reaudit-20260831` | 394a262 | surgeon2 | — |
| `docs/observations/2026-08-31-prepared-request-eligibility-third-audit.md` | placed | `origin/audit/eligibility-characterization-third-audit-20260831` | ff7d17a | surgeon2 | — |
| `docs/observations/2026-08-31-speed-claims-confirmation-audit.md` | placed | `origin/docs/speed-confirm-20260831` | 01d9647 | sol | — |
| `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-audit.md` | placed | `origin/audit/substantiation-telemetry-w1-rebase-20260831` | fe45faa | surgeon2 | — |
| `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-overhead-protocol.md` | placed | `origin/experiment/substantiation-overhead-w1-rebase-20260831` | 7da2fad | surgeon1 | — |
| `docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-overhead-screen.md` | placed | `origin/experiment/substantiation-overhead-w1-rebase-20260831` | 3dde767 | surgeon1 | — |
| `docs/observations/2026-08-31-the-fleet-opines-on-the-magic.md` | placed | `origin/docs/fleet-opines-20260831` | f261fa9 | sol | — |
| `docs/observations/2026-08-31-threeway-acid-wall-battery.md` | placed | `origin/bench/threeway-acid-20260831` | f904ed9 | sol | — |
| `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md` | placed | `origin/bridge/captains-log-wall-clock-ideal` | d7b3b72 | forge-bridge | — |
| `docs/observations/evidence/2026-08-30-from-to-double-repetition-receipt.json` | placed | `origin/experiment/fromto-overlap-study-20260830` | b51f802 | sol | — |
| `docs/observations/evidence/2026-08-30-walls-of-text-exhibit-receipt.json` | placed | `origin/experiment/walls-of-text-exhibit-20260830` | 5d3834e | sol | — |
| `docs/observations/evidence/consumption-gap-20260830/input-locator.json` | placed | `origin/experiment/consumption-gap-20260830` | 1648d5d | sol | — |
| `docs/observations/evidence/consumption-gap-20260830/phase1-coverage.json` | placed | `origin/experiment/consumption-gap-20260830` | 1648d5d | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/mcp-telemetry/fe07de7c-6347-4a0d-ab4c-cc143f28f970.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/workspace/.receipts/fa46e3e6-5517-488f-8d70-a74e25fd2bf5.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/mcp-telemetry/96e6d987-686c-40ba-b90a-221d6a9b77a4.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/workspace/.receipts/ef134243-2071-4662-bbc4-f6dca941e76b.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort-summary-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort-summary.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/mcp-telemetry/0889b925-aea7-48ea-ac3d-e1024cfb7d20.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/workspace/.receipts/bade8304-8cd8-413b-a178-862cf9b23c96.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/mcp-telemetry/2c2ec76e-8606-4671-a322-802709a5ceca.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/workspace/.receipts/c9845fd6-9931-4ce6-8022-a681fc1c8445.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/mcp-telemetry/8e99c196-2dd2-46c2-a131-0c0dfaaa87a2.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/workspace/.receipts/265fdb06-0646-40c9-b2a2-7d1d4a34a782.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/mcp-telemetry/0beef7f8-4389-402b-91b3-d4c32ca269f2.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/workspace/.receipts/06d2efcf-ae67-4c2c-adf9-ed263d7c9bd0.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/mcp-telemetry/552e62ba-1ffd-4595-b257-d4674c128827.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/workspace/.receipts/62b7d8ab-1a40-457b-95bc-1c1fa79980a8.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/mcp-telemetry/49becfa6-5d9f-4d16-a50e-29877c000426.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/workspace/.receipts/9f496d70-aaef-477c-8611-897172c43d00.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/mcp-telemetry/3a17fc36-b383-4a2a-831b-db8c723280cc.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/workspace/.receipts/d4155add-82cb-4df9-9f8b-19bd5de48d8b.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/mcp-telemetry/69fefb84-9c48-4734-b9dc-6645929d60b9.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/workspace/.receipts/476fcb4e-bebb-422d-8620-ac61a5eab392.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/mcp-telemetry/9df02015-b692-4c30-91f9-325221cc2e4a.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/workspace/.receipts/c12418fc-ee24-460c-9a3b-b1f3e692cc1e.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/mcp-telemetry/5fe32e5a-26e0-42d5-9969-ce08675be424.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/workspace/.receipts/5b991280-6097-4c83-b05d-cbc3e649c56b.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/mcp-telemetry/2f8dd4a4-cd41-4cd3-8c70-ee383319d899.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/workspace/.receipts/310bffe7-b7e8-4399-8571-9afb8e97a613.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/mcp-telemetry/320fdbda-5120-4b97-99ad-a3dd1d06c6b0.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/workspace/.receipts/65980079-b584-44d4-af5f-6737e4910b2d.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/mcp-telemetry/fc94bc3e-60ce-4f6a-b1f7-84ac8d5fe63b.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/workspace/.receipts/04d0ce2e-0340-48bf-8611-d9a2f88f22b2.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/mcp-telemetry/f42480be-9595-4a3f-9a99-d96f9b9f71e2.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/workspace/.receipts/f95606c5-0bea-47e5-880a-ee485f5c349f.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/mcp-telemetry/b4a63c10-8d30-448c-9a3f-a8eaa7136be3.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/workspace/.receipts/aa353666-fd36-45c5-8638-4917087b46d8.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/mcp-telemetry/243c09cc-4f0d-4af2-a0ab-9b244e28bb05.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/score-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/workspace/.receipts/dad396b4-6846-488a-b45a-7aaf9dadcac0.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/manifest-original.sha256` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/manifest.sha256` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/run-config.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/spark-bonus-summary-rescored.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/spark-bonus-summary.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | b0432c2 | sol | — |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/manifest.sha256` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot-summary.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/01-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/pilot/02-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v2/run-config.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | da139e1 | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort-summary.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/mcp-telemetry/631b0093-44a3-4108-a138-efee10332dfa.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/workspace/.receipts/49fef249-f10c-4c33-b5a2-497e714ac43f.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/mcp-telemetry/dd9d969a-998e-43c1-a6ef-b10b49783458.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/workspace/.receipts/224c87a7-982a-4975-9926-76c027b7b848.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/mcp-telemetry/b662352c-1f96-4587-9863-c4e493693ae1.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/workspace/.receipts/871ef471-39a3-417f-aa0e-f083f35863a7.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/mcp-telemetry/9e9dd764-1de9-4d69-8b9d-c6c3b105b44c.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/workspace/.receipts/8aca7aae-f74d-4a75-af7f-6522b6aa4cd8.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/mcp-telemetry/98fed944-3cbd-47c2-bd41-451f1549082c.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/workspace/.receipts/dee05002-9f5d-4ac6-bfbc-93de6a529374.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/mcp-telemetry/78cf0429-4398-437b-b73a-b41c426554f6.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/workspace/.receipts/6c23fbbf-b00d-440f-803b-6558cad552a6.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/mcp-telemetry/f5899530-e8ef-4a94-902a-f09eaae9a9b0.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/workspace/.receipts/4823707e-7aec-4ff3-b50d-1d7dc569cec4.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/mcp-telemetry/527fef05-635b-4c35-b019-13c0418165da.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/workspace/.receipts/200943b8-2460-4c6b-9f5e-ef74a4d04763.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/mcp-telemetry/852cb1a4-8694-4301-9617-bb972e5d8939.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/workspace/.receipts/33fea9d4-aaca-49fc-a174-341cb1c358f5.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/mcp-telemetry/df5e371f-2ccc-4623-9e4d-56760fe7dc39.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/workspace/.receipts/e3beacb5-1183-45c8-b67b-c9e2375cddcd.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/mcp-telemetry/7e6bd83c-afc0-4329-855d-fe533fefa2db.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/workspace/.receipts/88b3271c-6a67-425f-b600-196ec9488095.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/mcp-telemetry/14bf3259-ce10-4fc3-b4a8-d34f2e3bf545.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/workspace/.receipts/00e8fdf1-3f83-4adf-97cd-300c82e9a276.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/mcp-telemetry/eada42cc-a797-4688-9cd1-8e96d6242f0e.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/workspace/.receipts/5328df6a-8443-403d-9526-a7ff6a7eb5bd.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/mcp-telemetry/441e93a6-78f0-4167-ab8f-78ef9033be8e.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/workspace/.receipts/876482d4-7b17-4a2b-8c28-041aa29b5b70.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/mcp-telemetry/18e273c0-e3b5-409d-946c-60ecadecd13b.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/workspace/.receipts/12531712-4363-45fd-9d40-9f1b68c01df2.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/mcp-telemetry/c4d22dc8-4ed0-453f-875b-1ba9a8cf2951.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/workspace/.receipts/0397b9be-c334-45df-988c-22e83206b690.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/manifest.sha256` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot-summary.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/mcp-telemetry/b39cd118-6730-4df5-88c2-54e8011db4e8.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/workspace/.receipts/df1ebb11-25aa-480e-b935-62e0a855540a.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/mcp-child.stderr` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/mcp-telemetry/c6e01d64-04b3-46ff-9458-fc869283f8dc.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/proxy-receipts.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/proxy-stream.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/workspace/.receipts/34e3ecf7-9bbd-40be-9df8-58f3e5770c63.edn` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw-v3/run-config.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 588893f | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/manifest.sha256` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot-summary.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/01-Q/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/01-Q/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/01-Q/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/01-Q/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/01-Q/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/01-Q/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/01-Q/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/codex-config.toml` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/episode.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/events.jsonl` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/prompt.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/score.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/stderr.txt` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/workspace/src/splice_reference/fixture.clj` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/splice-reference-screen-20260830/raw/run-config.json` | placed | `origin/experiment/splice-adversarial-replication-20260830` | 1e4d60e | sol | 1 other branch(es) |
| `docs/observations/evidence/three-arm-request-shape-6328db5.edn` | placed | `origin/experiment/three-arm-request-shape-prereq` | 1b8c28c | Gene Kim | — |
| `docs/observations/evidence/three-arm-request-shape-a9a9d5d.edn` | placed | `origin/experiment/three-arm-request-shape-prereq` | 7844bc6 | Gene Kim | — |
| `docs/observations/evidence/three-arm-request-shape-b298b8b.edn` | placed | `origin/experiment/three-arm-request-shape-prereq` | 2a55fc9 | Gene Kim | — |
| `docs/observations/evidence/verb-algebra-census-20260830/preregistration.md` | placed | `origin/experiment/verb-algebra-census-20260830` | a8a9037 | sol | — |
| `docs/observations/evidence/verb-algebra-census-20260830/receipt.json` | placed | `origin/experiment/verb-algebra-census-20260830` | abaf7e6 | sol | — |
| `docs/observations/evidence/verb-algebra-census-20260830/splice-family-join.json` | placed | `origin/experiment/verb-algebra-census-20260830` | abaf7e6 | sol | — |
| `docs/plans/2026-08-30-gpt-5-3-spark-caller-screen.md` | placed | `origin/experiment/spark-caller-screen-20260830` | 065a51e | sol | — |
| `docs/plans/2026-08-30-rename-verb-screen.md` | placed | `origin/experiment/rename-verb-screen-20260830` | 47fa7a6 | sol | — |
| `docs/plans/prepared-confirm-affinity-guidance-frozen-red-20260831.md` | placed | `origin/audit/prepared-confirm-affinity-7e0300fe-20260831` | a393504 | surgeon2 | 2 other branch(es) |
| `docs/plans/substantiation-w1-cross-feature-witness-20260830.md` | placed | `origin/audit/substantiation-telemetry-w1-rebase-20260831` | 74d3e29 | surgeon1 | 3 other branch(es) |
| `docs/plans/telemetry-coverage-and-workspace-ratchets.md` | placed | `origin/codex/telemetry-ratchets-20260829` | 643157f | ratchet | — |

## Source ledger (verbatim)

```
branch	author	last	commits_ahead	not_upstream_by_patch	obs_docs_only_on_branch	nondocs_files
origin/audit/adoption-gap-attribution-20260830	surgeon2	2026-08-30	5	5	4	9
origin/audit/eligibility-characterization-20260831	surgeon2	2026-08-30	2	2	1	3
origin/audit/eligibility-characterization-fourth-audit-20260831	surgeon2	2026-08-30	6	6	1	4
origin/audit/eligibility-characterization-reaudit-20260831	surgeon2	2026-08-30	3	3	1	4
origin/audit/eligibility-characterization-third-audit-20260831	surgeon2	2026-08-30	4	4	1	4
origin/audit/eligibility-legibility-refactor-20260831	surgeon2	2026-08-31	7	7	1	5
origin/audit/prepared-confirm-affinity-7e0300fe-20260831	surgeon1	2026-08-30	7	7	3	8
origin/audit/prepared-confirm-preview-05f5a196-20260830	surgeon2	2026-08-30	1	1	1	0
origin/audit/prepared-confirm-session-affinity-20260831	surgeon2	2026-08-30	2	2	2	0
origin/audit/substantiation-telemetry-independent-20260830	surgeon2	2026-08-30	4	4	5	17
origin/audit/substantiation-telemetry-second-successor-20260830	surgeon2	2026-08-30	7	7	5	17
origin/audit/substantiation-telemetry-successor-20260830	surgeon2	2026-08-30	5	5	5	17
origin/audit/substantiation-telemetry-w1-rebase-20260831	surgeon2	2026-08-30	9	9	6	18
origin/audit/write-refusal-001-independent-20260830	surgeon2	2026-08-30	1	1	1	0
origin/audit/write-refusal-completeness-20260830	sol	2026-08-30	1	1	1	0
origin/bench/acid-crossover-ladder-20260830	sol	2026-08-30	4	4	3	46
origin/bench/prefill-decode-ratio	opus-bench	2026-08-29	5	2	0	57
origin/bench/threeway-acid-20260831	sol	2026-08-31	6	6	1	23
origin/bridge/captains-log-wall-clock-ideal	forge-bridge	2026-09-02	2	2	1	0
origin/codex/telemetry-ratchets-20260829	sol	2026-08-29	2	2	1	2
origin/docs/acejump-designs-20260830	sol	2026-08-30	4	4	4	90
origin/docs/acejump-postmortem-20260830	sol	2026-08-30	1	1	1	0
origin/docs/acid-test-doctrine-20260830	mayor	2026-08-30	2	2	0	3
origin/docs/captains-log-2026-08-30-install-night	mayor	2026-08-31	9	9	9	0
origin/docs/captains-logs-2026-08-29	fable	2026-08-29	11	8	5	0
origin/docs/conn-review-20260830	sol	2026-08-30	7	7	7	0
origin/docs/dreamlist-designs-20260830	sol	2026-08-30	1	1	1	0
origin/docs/embedded-elaborator-ratification-20260830	surgeon2	2026-08-30	3	3	3	14
origin/docs/fast-typist-designs-20260831	sol	2026-08-30	1	1	1	0
origin/docs/fleet-opines-20260831	sol	2026-08-31	1	1	1	0
origin/docs/grave-revisit-update-20260830	sol	2026-08-30	1	1	1	0
origin/docs/prepared-confirm-preview-ratification-20260830	surgeon2	2026-08-30	2	1	1	14
origin/docs/prepared-request-ratification-20260830	surgeon1	2026-08-30	1	1	2	0
origin/docs/prepared-request-recovery-lld-20260830	surgeon1	2026-08-30	12	12	9	19
origin/docs/readme-economics-20260830	sol	2026-08-30	1	1	0	1
origin/docs/speed-confirm-20260831	sol	2026-08-31	1	1	1	0
origin/docs/splice-reference-ratification-20260830	surgeon2	2026-08-30	1	1	1	0
origin/docs/substantiation-telemetry-ratification-20260830	surgeon1	2026-08-30	5	5	4	17
origin/docs/sweep-lane1-complete-refusal-ab	sol	2026-08-30	16	13	6	0
origin/docs/transcript-audit-20260831	sol	2026-08-31	1	1	1	0
origin/docs/worktree-lifecycle-hld-20260831	surgeon1	2026-08-31	1	1	0	0
origin/experiment/claude-route-result-price-tag-20260829	route-hint	2026-08-29	1	1	1	0
origin/experiment/codex-catalog-floor-sweep	surgeon1	2026-08-29	9	9	2	9
origin/experiment/consumption-gap-20260830	sol	2026-08-30	2	2	4	3
origin/experiment/differential-routing-interview-20260829	sol	2026-08-30	12	9	6	0
origin/experiment/elaborator-fallback-battery-20260831	sol	2026-08-31	3	3	0	208
origin/experiment/embedded-spark-probe-20260830	sol	2026-08-30	1	1	5	0
origin/experiment/emission-compression-screen	surgeon2	2026-08-29	324	5	2	178
origin/experiment/external-corpus-shape-census	surgeon1	2026-08-30	4	4	3	6
origin/experiment/fromto-overlap-study-20260830	sol	2026-08-30	3	3	2	2
origin/experiment/fuel-table-completion-20260831	sol	2026-08-30	3	3	1	155
origin/experiment/mcp-catalog-screen-6128b9b	Gene Kim	2026-08-27	144	0	0	94
origin/experiment/mcp-catalog-screen-clean-a953e41	Gene Kim	2026-08-28	151	0	0	96
origin/experiment/mcp-catalog-screen-verdict-e4c37eb	Gene Kim	2026-08-28	152	0	0	96
origin/experiment/multisite-headtohead-20260830	sol	2026-08-30	6	6	0	459
origin/experiment/native-prelanding-gate-audit	surgeon2	2026-08-30	4	4	3	4
origin/experiment/ordinal-refusal-screen-20260830	sol	2026-08-30	2	2	1	19
origin/experiment/prepared-confirm-affinity-live-measure-20260831	surgeon1	2026-08-30	10	10	4	86
origin/experiment/prepared-confirm-preview-live-token-measurement	surgeon1	2026-08-30	4	4	2	111
origin/experiment/prepared-request-proxy-screen-20260830	surgeon1	2026-08-30	11	11	9	19
origin/experiment/prepared-request-replication-20260830	sol	2026-08-30	15	12	7	22
origin/experiment/read-normalization-live-token-measurement	surgeon1	2026-08-30	3	3	2	80
origin/experiment/redesign-exemplars-20260830	Gene Kim	2026-08-30	1	1	1	0
origin/experiment/rename-verb-screen-20260830	sol	2026-08-30	4	4	2	26
origin/experiment/result-decision-chord-screen	surgeon1	2026-08-29	329	0	0	176
origin/experiment/routing-adoption-live-config-20260830	surgeon2	2026-08-30	9	9	0	55
origin/experiment/routing-tranche-20260830	sol	2026-08-30	6	6	0	48
origin/experiment/spark-caller-screen-20260830	mayor	2026-08-30	4	4	2	26
origin/experiment/spark-isolation-screen-20260830	sol	2026-08-30	3	3	37	0
origin/experiment/splice-adversarial-replication-20260830	sol	2026-08-30	7	7	505	8
origin/experiment/splice-reference-screen-20260830	sol	2026-08-30	4	4	262	8
origin/experiment/steering-ab-20260831	sol	2026-08-31	3	3	0	128
origin/experiment/submission-row-counterfactual	Gene Kim	2026-08-27	71	0	0	48
origin/experiment/substantiation-overhead-20260830	surgeon1	2026-08-30	6	6	5	38
origin/experiment/substantiation-overhead-w1-rebase-20260831	surgeon1	2026-08-30	14	14	8	56
origin/experiment/three-arm-request-shape-prereq	Gene Kim	2026-08-29	251	10	4	149
origin/experiment/turn-waste-mining-20260830	sol	2026-08-30	2	2	1	0
origin/experiment/verb-algebra-census-20260830	sol	2026-08-30	2	2	4	1
origin/experiment/walls-of-text-exhibit-20260830	sol	2026-08-30	1	1	2	0
origin/experiment/warm-executor-screen-20260830	sol	2026-08-30	4	4	4	255
origin/experiment/write-refusal-001-live-token-measurement	surgeon1	2026-08-30	5	5	3	76
origin/feature/create-files-transaction	builder	2026-08-31	2	0	0	10
origin/feature/embedded-elaborator-20260830	sol	2026-08-30	8	8	39	31
origin/feature/prepared-confirm-affinity-guidance-20260831	surgeon2	2026-08-30	6	6	2	7
origin/feature/prepared-request-first-slice-20260830	surgeon1	2026-08-30	3	3	3	90
origin/feature/substantiation-telemetry-w1-rebase-20260831	surgeon1	2026-08-30	8	8	5	18
origin/feature/write-refusal-completeness-20260830	surgeon1	2026-08-30	1	1	1	0
origin/fix/cold-verifier-deterministic-clock-20260830	surgeon2	2026-08-30	1	1	0	1
origin/fix/collector-telemetry-root	forge-bridge	2026-09-02	1	1	0	2
origin/prep/substantiation-w1-witness-20260830	surgeon1	2026-08-30	1	1	1	1
origin/refactor/eligibility-legible-checks-20260831	mayor	2026-08-31	7	7	0	5
origin/release/prepared-request-published-20260830	surgeon1	2026-08-30	1	1	1	0
origin/release/read-normalization-published-20260830	surgeon1	2026-08-30	2	2	1	23
origin/release/write-refusal-001-published-20260830	surgeon1	2026-08-30	1	1	1	14
origin/test/eligibility-characterization-20260831	mayor	2026-08-30	5	5	0	4
```
