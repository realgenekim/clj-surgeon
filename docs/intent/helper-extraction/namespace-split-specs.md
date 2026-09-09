# Whole namespace split compiler

Ratified scope: docs/plans/namespace-split.md and Gene's two-batch build brief.
One source snapshot, one final graph, one guarded publication and inverse.

- [x] **NS-SPLIT-001**: When given a complete seam mapping, the compiler shall assign every definition identity to exactly one destination.
- [x] **NS-SPLIT-002**: When references cross mapped seams, the compiler shall derive the final namespace edges from resolved reference identities with source witnesses.
- [x] **NS-SPLIT-003**: When a private definition is referenced across a seam, the compiler shall promote it only under promote-required or an explicitly authorized named set.
- [x] **NS-SPLIT-004**: When lowering resolved references, the compiler shall allocate aliases from each destination's policy without colliding with external bindings.
- [x] **NS-SPLIT-005**: When a forward reference remains within a destination, the compiler shall emit a declaration before its first use.
- [x] **NS-SPLIT-006**: When callers under bounded roots reference moved Vars, the compiler shall rewrite those references and their requires in the same future source set.
- [x] **NS-SPLIT-007**: When the final graph contains a cycle or a declared architectural violation, publication shall refuse before writing.
- [x] **NS-SPLIT-008**: When namespace identity, mapping, snapshot guards or promotion authorization is invalid, publication shall refuse before writing.
- [x] **NS-SPLIT-009**: When publishing a split, the shared extraction kernel shall commit source retirement, all destination creations and all caller edits as one failure-atomic file set.
- [x] **NS-SPLIT-010**: When publication or required verification fails, the shared guarded inverse shall restore the captured files or report recovery required.
- [x] **NS-SPLIT-011**: When analysis is requested, plan-only shall return the nonmutating projection of the same compiler, including unknown coverage and grouped blockers.
- [x] **NS-SPLIT-012**: When a split completes, its receipt shall report state, counts, destination libraries, promotions, final graph coverage and named checks with exits and durations.
- [x] **NS-SPLIT-013**: When invoked through MCP, namespace_split shall accept the closed input schema and return text containing the entire structured receipt.
- [x] **NS-SPLIT-014**: When invoked through CLI :split-ns!, the tool shall accept the same EDN request and plan-only flag with truthful refusal exit status.
- [x] **NS-SPLIT-015**: When capturing the compiler input, the boundary shall enforce finite confined roots and analyze the captured sources once.

Misreadings: twenty extraction calls constitute one compiler; source order proves
absence of forward references; textual mentions identify callers; a private crossing
authorizes any visibility change; old source require edges describe the final graph;
successful writes prove application behavior; a partial undo is a refusal without
mutation; a dry run may leave generated source files; test callers may be ignored.

Boundaries: aliases, first-class Vars, quoted Vars, locally shadowed symbols,
declarations, def metadata, external imports, external alias collisions, missing and
duplicate owners, cycles through callers, source-root mismatch, drift at publication,
proof failures and guarded undo. Unsupported reader/macro/dynamic cases are bounded
unknowns and refuse rather than imply universal static closure. State-space witnesses
exercise mapping and rollback alternatives; no retained second logical runtime.


Round 3 registered amendments (registry: namespace-split-papercuts.edn):

- [x] **NS-SPLIT-028**: When retiring a caller require, the compiler shall replace its exact position with the sorted destination block.
- [x] **NS-SPLIT-029**: While a live workspace nREPL is available, verification shall run the affected reload/test probe before cold commands and roll back on probe failure.
- [x] **NS-SPLIT-030**: When a profile selects warm proof, success shall report committed-probe-only with incomplete verification and all skipped cold commands pending.

- [x] **NS-SPLIT-032**: When a call head changes width, the compiler shall shift every subsequent line within its structural span by that delta at every nesting depth, except lines inside multiline string literals.
- [x] **NS-SPLIT-033**: When replacing the retired views require, the compiler shall splice only the sorted new block at its exact span, preserving other host bytes.

## B01: round-5 debt registration

These append-only promises register the behavior and existing witnesses in
14c0501f..1f0646e9. Earlier identifiers remain resolvable; 034 and 035 are the
current registry rulings for 032 and 033. This registration changes no compiler
behavior and does not claim a new fail-first experiment.

- [x] **NS-SPLIT-034**: When a changed call head has a width delta, the compiler shall shift every subsequent line owned by that call form by the head delta, with ownership determined by paren structure at every nesting depth and multiline string contents never moved.
- [x] **NS-SPLIT-035**: When replacing the retired views require in the pinned d9205abc forms/polish headers, the compiler shall replace only that span with the sorted destination block, preserving every other host byte.
- [x] **NS-SPLIT-036**: When the full MCP profile lists its public tools, it shall include namespace_split exactly once between helper_extraction and admit_clojure_patch.

B07 preregistered requirements:

- [x] **NS-SPLIT-037**: When source.retain is true, the compiler shall leave unmapped owners in the source and include that file in the guarded inverse.
- [x] **NS-SPLIT-038**: When a reference crosses a retained or moved seam, the compiler shall qualify it through a required alias and promote only authorized necessary private owners.
- [x] **NS-SPLIT-039**: When removal of moved invocations in source comment forms is authorized, the compiler shall remove only the offending invocation and its sole print wrapper.
- [x] **NS-SPLIT-040**: When facts are requested, the compiler shall return snapshot-bound pre-emission facts identical to those consumed by full execution.
- [x] **NS-SPLIT-041**: When CLI facts-only or MCP plan_only facts is requested, the shared boundary shall return a read-only facts receipt through the closed request grammar.
- [D] **NS-SPLIT-042**: When the Cell B oracle encounters an unresolved diagnostic in any letter case, A8 shall fail.
- [x] **NS-SPLIT-043**: When Cell B partial-extraction preservation is checked, the oracle shall reject missing retained qualification, redirected retained callers and the source comment cycle.

- [x] **NS-SPLIT-044**: When captured analysis exceeds source-byte size, the boundary shall apply a separate finite 64 MiB analysis-output cap and refuse truncation before parsing.

- [x] **NS-SPLIT-045**: When partial extraction retires the last use of a source alias or short imported class, the compiler shall remove that newly unused header entry while preserving mixed and load-only dependencies.

- [x] **NS-SPLIT-046**: When A8 compares a candidate with an independently analyzed archive of the exact frozen base, the oracle shall reject every new or increased unresolved finding in any letter case while retaining matching baseline findings as explicit evidence.

NS-SPLIT-046 supersedes the absolute NS-SPLIT-042 check after the latter exposed
five findings already present in byte-protected baseline code. The exact fixture
and all ownership, behavior, architecture and protected-content gates are unchanged.

- [x] **NS-SPLIT-047**: When verification.profile-file or CLI :profile-file supplies an absolute external EDN profile configuration, the split shall resolve it without adding configuration to the workspace, refusing invalid paths or data before mutation.
- [x] **NS-SPLIT-048**: When a cold profile executes only true commands, the split shall publish incomplete verification with cold-suite pending and the executed command evidence.
- [x] **NS-SPLIT-049**: When a selected profile has commands [], verification admission shall refuse with a specific empty-profile reason before writing.

## Rows sublime batch 3

- [x] **NS-SPLIT-050**: When warm proof selects a background gate, the verb shall return an immutable pending receipt bound to a detached worker pid.
- [x] **NS-SPLIT-051**: When background proof closes, status shall report complete only for all successful commands over the unchanged committed snapshot.
- [x] **NS-SPLIT-052**: When a split commits, its receipt shall contain bounded encoded review facts for every destination and retained owner.
- [x] **NS-SPLIT-053**: When facts-only targets a committed request, it shall answer from the committed snapshot or name its closure receipt in a typed refusal.
- [x] **NS-SPLIT-054**: When facts print a manifest, those bytes shall be a valid plan-only request including verification.
- [x] **NS-SPLIT-055**: When proof status reads a closure, it shall accept it only when the original receipt hash, worker pid, worker start identity, and launched worker argv all match.
- [x] **NS-SPLIT-056**: When a background proof worker is admitted, its `java.io.tmpdir` shall be an existing absolute descendant of `/var/tmp`; `/tmp`, relative, missing, and root-only paths shall refuse before publication.
- [x] **NS-SPLIT-057**: When a receipt is bounded, both its UTF-8 EDN bytes and escaped-JSON transport bytes shall fit the ceiling; hostile newline and bidi filename text shall remain encoded.
- [x] **NS-SPLIT-058**: When facts print a replay manifest, it shall bind the captured snapshot hash so a one-byte hash change returns the typed snapshot-drift refusal.

## Sol delta fence for 0956951b, ruling (a)

- [x] **NS-SPLIT-059**: When a background proof refuses an unsafe `java.io.tmpdir`, the public receipt shall carry an executable `next_call` naming the `/var/tmp` repair before any mutation, and `:split-ns!` help shall state both the Babashka `TMPDIR=` and the JVM/MCP `-Djava.io.tmpdir=` form.

## Rows sublime batch 4: candidate negative evidence

- [x] **NS-SPLIT-060**: When a split receipt describes candidate comment changes, it shall publish an owner-and-content identity diff with explicit moved, changed, added and deleted evidence and the applied policy; line shifts alone shall be omitted, and content surviving anywhere in the candidate shall never be labeled deleted.
- [x] **NS-SPLIT-061**: When a split publishes stale-reference facts, it shall scan every captured candidate file across the declared roots for retired qualified symbols and aliases with explicit exclusions.
- [x] **NS-SPLIT-062**: When a split publishes facade facts, it shall enumerate retained source forwarding definitions with explicit retention-policy expectations.
- [x] **NS-SPLIT-063**: When a split publishes exactly-once facts, it shall count each moved owner in its assigned destination and elsewhere across candidate roots.
- [x] **NS-SPLIT-064**: When a split publishes body preservation facts, it shall compare each original owner after authorized reference, alignment and promotion replay with actual destination bytes using SHA-256 evidence while reporting raw byte equality separately.
- [x] **NS-SPLIT-065**: When candidate negative evidence contradicts the split contract, publication shall refuse before any mutation with the violating facts.
- [x] **NS-SPLIT-066**: When a committed split has incomplete verification, its receipt shall explain proof tier and pending status with a proof-status next_call without changing verification_complete.

## Rows sublime batch 5: residual review facts

- [x] **NS-SPLIT-067**: When candidate namespace headers differ, the receipt shall publish exact require/import entry occurrence edits with original and candidate order.
- [x] **NS-SPLIT-068**: When a candidate footprint is reported, the receipt shall classify whole-file byte identity across the captured roots, including files outside moved owner files.
- [x] **NS-SPLIT-069**: When candidate lint has executed, receipt facts shall expose signed error/warning deltas with baseline counts and introduced error count.
- [x] **NS-SPLIT-070**: When a warm probe executes, receipt facts shall name successfully loaded namespaces and the namespace whose require failed.
- [x] **NS-SPLIT-071**: When a split text receipt is rendered, committed shall precede proof and its defined verification_complete boolean.
- [x] **NS-SPLIT-072**: When byte-identity facts capture source files, the boundary shall refuse malformed UTF-8 before mutation rather than compare lossy decoded strings.
