# Frozen screen: minimal easy-path schema

Hypothesis: Codex routes native because the structural schema appears expensive
to construct even for a trivial bounded edit.

A exposes the general structural schema (`operation`, one `changes` entry with
file/within/from/to/matches, and verification options). B exposes only `file`,
`old`, `new`, and optional `within_form`; the local adapter derives the same
operation and guards. Name, description, position, mutation semantics, result,
prompt, fixture, and scorer are unchanged.

Prediction: A 25% and B 50% structural-first, a +25pp lift. Kill if lift is
below 15pp, or if B-A invalid-call or wrong-owner-edit incidence increases by
more than 5pp. Wrong-subject must be 0. The frozen 2-run A pilot must first be
valid and sub-ceiling.
