# Stable CLI runtime package

- [ ] **CLI-PACKAGE-001**: While running outside the source checkout, when the installed CLI receives an empty insert-forms or rename-alias request, it shall return the typed invalid-request refusal.
- [ ] **CLI-PACKAGE-002**: When bundled clj-splice source bytes change, the CLI package identity shall change.

Misreadings: a worktree bb.edn proving dispatch also proves the installed launcher;
copying Surgeon src alone closes its runtime; a library edit can reuse a prior package.
Boundaries: both lazy-loaded write verbs, ls positive control, destination spaces,
caller outside the checkout, immutable versioned library source, fixed source commit.
Regression: stable eae1e432 on 2026-09-11 omitted clj-splice from the launcher.
Direct witnesses live in clj-surgeon.install-test and run in the ordinary bb suite.
