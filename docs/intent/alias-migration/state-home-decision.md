# State-home substitution is bounded invocation context

The parent contract is the high-level design's operation authority, refined by
[receipt artifact isolation](receipt-artifacts-specs.md), DATACODE-ENV-001/002/003/005.
This decision implements the round-9 launcher contract and its explicit
outside-policy negative witness.

A state-home passed to an internal journal/workspace entrance is trusted
launcher context. Admit its resolved path against the invocation's existing
roots, then register that path as a root in a new envelope for this invocation.
The binding declares a place inside policy; it does not authorize arbitrary
outside destinations. A supplied narrower envelope stays narrower. Nil leaves
the current envelope unchanged. Neither registration nor return changes the
process's initialized envelope. The journal's opened transaction retains the
admission envelope as evidence, not as a grant for a later invocation.

Workspace state derivation and journal creation admit their final destinations.
An admitted substitute whose `.local` descendant is a symlink outside the roots
still refuses. An absent outside parent is not created. Public operation
requests carrying state-home or destination-envelope remain unknown arguments;
internal options do not become request authority.

The motivating test chose `/home/forge/tmp` while the battery admitted only its
fresh suite TMPDIR, the passwd state root, and the invocation workspace. The
right fixture default is java.io.tmpdir. Registering an outside substitute to
turn that failure green would contradict the required negative witness.

Witnesses: `state-home-substitution-is-bounded-launcher-context` and
`state-home-substitution-preserves-narrow-context-and-final-target-checks` in
receipt-artifacts-boundary-test; all-test scratch-default census in
txn-journal-test; complete narrow diff-impact and the counted battery. Coverage
remains the shared artifact boundary and these internal entrances, not every
writer or adversarial filesystem races.
