# insert_forms v1 build plan

Authority: ../intent/insert-forms/contract.md. Design and atomic requirements:
../intent/insert-forms/design.md and insert-forms-specs.md. The behavior matrix is
contract sections 1–8; each of its eighteen named witnesses owns one namespace.

First commit typed stub and RED witnesses. Then implement CST planner, payload
splice, guarded snapshot publication and receipt projection, followed by the shared
MCP/CLI request-file entrances. Add an independent preservation oracle and negative
mutation probes. Preserve root forms, gaps, literal bytes, identity and modes.

Required acceptance: all eighteen namespaces; lint via ~/bin/clj-kondo; real
census-derived fixtures with source provenance; make test-fast; exactly one
~/bin/suite-run make landing-gate-prewarm. Every JVM is capped at 1 GiB, all
scratch/artifacts under /var/tmp/forge/insert-fx, no services or pushes.

Report commits, witness counts, amendments, remaining risks and three uncertain
choices in /var/tmp/forge/insert-fx/astra-build-report.md. A seam requiring more
than one paragraph of contract change stops the build for Fable's decision.

Fix round 1 (2026-09-10) supersedes the builder's prewarm requirement for this
handoff: fix F1, F2, F3 in that order, then the recovery docstring and witness.
Capture guard-deletion RED in `/var/tmp/forge/insert-fx/fix1-red.log`; amend the
single §3 gap paragraph and §5 golden; retain every original witness. Regenerate
and read the census after enrolling new namespaces, run all insert namespaces,
`make test-fast`, and lint via `~/bin/clj-kondo`. Do not run prewarm in this round.
Report named-file commits and every changed spacing expectation in
`/var/tmp/forge/insert-fx/astra-fix1-report.md`; never push.
