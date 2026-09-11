---
parent: rename-alias-design
prefix: RENAME-ALIAS
status: implemented
---

# Fixed alias rename promises

The frozen contract is normative; IDs are permanent. Each boundary retains the full §7 witness matrix.

- [x] **RENAME-ALIAS-001**: While the exact E4 source is guarded, when two references are requested, the operation shall produce SHA-256 02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c.

  Witness: `rename-alias-e4-verbatim`. Misreading: Treat 31 textual occurrences as 31 alias references. Boundary: 31→2 refusal evidence; 35 forms, three changed forms, 29 literals and 22 route templates.

- [x] **RENAME-ALIAS-002**: When an alias is renamed, the selector shall match the complete symbol namespace part.

  Witness: `rename-alias-role-selection`. Misreading: Match ev as a prefix of event/x. Boundary: Arbitrary local Var spellings; bare symbols stay unchanged.

- [x] **RENAME-ALIAS-003**: When effective syntax refers through the alias, the operation shall rewrite exactly its reader-role prefix.

  Witness: `rename-alias-role-selection`. Misreading: Ignore quoted symbols or treat literal keywords and tagged-literal tag names as aliases. Boundary: Auto keywords/maps, quotes, syntax quote/unquote, metadata and destructuring.

- [x] **RENAME-ALIAS-004**: When opaque lexical content resembles an alias reference, the operation shall preserve its exact bytes.

  Witness: `rename-alias-role-selection`. Misreading: Use a source-wide substring replacement. Boundary: Strings, regex, characters, docstrings and semicolon comments; comment macro bodies remain syntax.

- [x] **RENAME-ALIAS-005**: When a reader-discard subtree contains alias spellings, the selector shall exclude the entire subtree.

  Witness: `rename-alias-role-selection`. Misreading: Rename dormant references or silently accept malformed discarded syntax. Boundary: Nested discards, discarded ns lookalikes and global reader restrictions.

- [x] **RENAME-ALIAS-006**: When alias binding selection is ambiguous or would capture existing syntax, the operation shall refuse before publication.

  Witness: `rename-alias-binding-matrix`. Misreading: Treat a same-named local as an alias collision or allow duplicate library aliases. Boundary: as/as-alias, absent/wrong library, duplicate options, refer/rename declarations, unused alias and no-op.

- [x] **RENAME-ALIAS-007**: When a request claims a snapshot cardinality, the operation shall require the exact inspected inventory and reference counts.

  Witness: `rename-alias-stale-and-counts`. Misreading: Guard only changed files or silently retry a stale guard. Boundary: All scopes, zero-count skips, per-file/total counts, portable receipts, membership drift and complete true-site evidence.

- [x] **RENAME-ALIAS-008**: When source violates the supported path, encoding or reader boundary, the operation shall refuse without changing target bytes.

  Witness: `rename-alias-parse`. Misreading: Follow symlinks, ignore unsupported discovered files or evaluate reader syntax. Boundary: UTF-8, LF/CRLF, BOM, hardlinks, confinement, limits, namespace shape and dynamic mutations.

- [x] **RENAME-ALIAS-009**: When publication fails, the operation shall report its verified per-file mutation outcome without overwriting external bytes.

  Witness: `rename-alias-publication-evidence`. Misreading: Report no-write after an attempted replacement or replay a published candidate. Boundary: Staging, second replacement, read-back, restoration, receipt failure, final snapshot and crash-window recovery.

- [x] **RENAME-ALIAS-010**: When a receipt claims preservation, every recorded span and form digest shall agree with independently captured source and candidate bytes.

  Witness: `rename-alias-preservation-address`. Misreading: Trust a true preservation flag or use production selection as its own oracle. Boundary: Complete ordinal inventories, inverse identity, bounded summaries/full detail, corrupt neighbors and forged digests.

- [x] **RENAME-ALIAS-011**: When either request-file CLI spelling or MCP is invoked, the entrance shall expose the same domain result and mutation state.

  Witness: `rename-alias-cli-mcp-parity`. Misreading: Turn a domain refusal into a transport error or report exit zero without a verified commit. Boundary: Closed bounded EDN, help, stdout, 0/2/1 exits, success/refusal/recovery parity and terminal omission.

- [x] **RENAME-ALIAS-012**: When candidate roles, preservation, inverse identity, parsing, or replacement read-back disagree, the operation shall report the exact candidate or I/O refusal and preserve or restore original disk bytes.

  Witnesses: `rename-alias-candidate-integrity`, `rename-alias-candidate-integrity`, `rename-alias-candidate-integrity`, `rename-alias-parse`, `rename-alias-publication-evidence`. Each load-bearing guard is independently deletion-tested; faults enter through injected functions, never source edits inside a witness.

- [x] **RENAME-ALIAS-013**: When a receipt is projected, preservation shall compare every form outside the declared change set against namespace-independent hashes of read-back disk bytes; per-file and summary read-back hashes shall hash that same observation, and detail-section claims shall come from the published artifact. Every boolean in a receipt must have a witness in which it is false.

  Witnesses: `rename-alias-publication-evidence`, `rename-alias-publication-evidence`, `rename-alias-publication-evidence`. A corrupted observation is retained in a recovery receipt, never promoted to committed proof or overwritten.

- [x] **RENAME-ALIAS-014**: When a site begins at column one, its line and preorder shall identify that node, including the first line and refusal evidence.

  Witness: `rename-alias-preservation-address`. Line starts are inclusive; end offsets remain exclusive.

- [x] **RENAME-ALIAS-015**: When planning the generated 4,000-line ordinary-form fixture under a 1 GiB heap, the planner shall complete in under five seconds. Original parser rows and cached UTF-16/UTF-8 indexing shall replace repeated prefix scans.

  Witness: `four-thousand-line-planning-under-five-seconds` (battery lane; machine wall bound, no native-control claim).

- [x] **RENAME-ALIAS-016**: When repository scope selects namespaces, eligibility shall ignore non-Clojure symlinks and skipped-file duplicate bindings; namespace-mutation tripwires shall apply only to selected namespaces outside comment-macro ancestry, while alias references inside comment bodies remain selected.

  Witnesses: `rename-alias-scope`, `rename-alias-scope`, `rename-alias-scope`, `rename-alias-scope`. Fable's one-paragraph §3 amendment authorizes the mutation exception; selected code symlinks still refuse and selected duplicate bindings remain ambiguous.
