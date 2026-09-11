# rename_alias v1

Parent: ../../high-level-design.md. Authority: [frozen contract](contract.md).

A separate request-file verb renames one fixed library alias and its complete
syntactic reference set. The functional core consumes a sorted snapshot and a
closed request, resolves reader roles, produces disjoint prefix splices, and
independently validates the candidate. The shell reuses insertion's bounded EDN,
guards, confined paths, publication lock, staged atomic replacement, shared
transaction engine and durable receipt store. It extends those seams with a
complete inspected snapshot and per-file recovery observations.

The public contract deliberately counts quoted symbols and excludes discards.
No library loading, evaluation, formatting, verification profile or automatic
retry is permitted. Byte preservation is not behavioral verification.

Requirements and permanent misreadings are in rename-alias-specs.md. Section 7
of the contract fixes the RED witness inventory, including the exact E4 blob.
The independent oracle reuses the insertion oracle's independent inventory and
hashing; its role selection is separately authored. Both share rewrite-clj,
which remains a shared-parser limitation. Implementation gates are the focused
witnesses, make test-fast, serialized lint 0/0 and an E4 request-to-oracle replay.

Fix round 1 replaces plan-derived receipt claims with a projector over fresh disk
observations. Declared site ordinals partition changed and protected root forms;
raw CST form hashes remain independent of namespace resolution. Candidate-role,
form-preservation, inverse and disk-read guards each have an injected-fault witness
that fails when that guard alone is deleted. The shared
[receipt boolean registry](../receipt-booleans/registry.edn) prevents a future
versioned verb or nested boolean field from escaping false-witness coverage.
Section 3 carries Fable's selected-namespace/comment-ancestry amendment.
