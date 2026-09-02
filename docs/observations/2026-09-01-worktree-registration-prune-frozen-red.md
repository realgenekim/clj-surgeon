# Worktree Registration Prune Frozen Red

The ratified stale-registration slice is frozen before product implementation.
No test in this phase applies against a real registration. The only executable
Git fixture creates and destroys its own temporary repository and worktrees.

- Candidate parent: `ba40d5d4953db8f123c29908c0dda76ca2df3612`
- Command: `make worktree-lifecycle-test`
- Expected result: `48 tests / 225 assertions / 9 failures / 0 errors`
- Test SHA-256:
  `18b9c4ba7cbc27f339e47d8fd73a99bbfae09083d7201ae57f4ce646d86479f6`
- Makefile SHA-256:
  `836086806cddb1fa6d269f34c9250d7ce7a70546281b2a2bb655850690140da3`

The one new test already green is the pre-existing conservative classifier:
the prototype correctly labels the absent registration `missing-prunable`.
The nine expected failures cover the missing closed request validator, ratified
snapshot row, remote preservation proof, outcome-free prune plan, distinct
plan validation, receipt, no-follow path proof, exact command/journal shape,
and controller-owned real Git compatibility matrix.

Any different count before implementation is drift and invalidates this red
receipt. Product code must not edit this record or weaken an existing
ordinary-close assertion to make the slice green.
