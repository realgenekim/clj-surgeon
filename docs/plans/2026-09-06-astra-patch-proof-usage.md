# Astra: make the existing patch gate usable with supplied proof

Status: design for joint review; no API or verification change implemented.
Evidence: [fresh-caller pilot](../observations/2026-09-06-astra-recovery-pilot-result.md).

## Problem and observed behavior

The encouraged caller authored its native patch, called `admit_clojure_patch`
with `mode=commit, verify=none`, received `verification-incomplete`, then applied
that same patch natively and ran the supplied proof. Both callers finished
correctly: native 67.262 seconds, encouraged 87.680 seconds. These are literal
pilot rows, not a powered speed comparison. Zero sites were tool-committed.

The tool's refusal was correct. ADMIT-120 makes `verify=none` unwaivable for
commit. The existing ADMIT-126 exception requires explicit `allow_partial`,
focused verification, partial status, an observed absent profile and a clean
analyzer reading. This work must neither broaden nor recommend that waiver.

The setup supplied external gate/candidate commands but did not demonstrate
that those commands were usable by the admission gate. It prohibited creating
workspace helper files. The proof script accepts a workspace, uses its proof
classpath, launches the isolated JVM, and returns its exit code; it does not
accept an admission report path. Its script witness also does not follow the
ordinary `test/<namespace>.clj` discovery layout. Supplying a passing shell
command is therefore not the same as supplying a usable focused-test profile.
The unexecuted `verify=focused` counterfactual remains unknown.

## Existing contract to use

`mcp_admit_tool/resolve-focused-test` merges repository and server configuration
per key, repository first: `:command`, `:timeout-ms`, `:namespaces`. The repository
location is `.clj-surgeon/focused-test.edn`. A namespaces-only repository profile
can use the server command. Receipts identify each source. Earlier prose in the
owning specification still describes older whole-map precedence and historical
commit behavior; those passages need reconciliation with ADMIT-110/120/126,
not another appended contradictory paragraph.

`run-focused-tests` requires a nonempty argv vector with literal `{snapshot}`
and `{report}` arguments. `{namespaces}` expands to individual namespace args.
The runner must actually evaluate the candidate and write attributable results
to the supplied report. `{snapshot}` contains changed, nondeleted post-images,
not a whole checkout. The runner must resolve candidate files before unchanged
workspace dependencies, suites and resources, while honoring deletions if its
accepted task permits them. The process starts in the live project root;
working directory alone is not the snapshot contract. `focused-namespace-plan` must resolve the declared
suite files. A missing mapping, command, report or suite is not a passing test.

## Smallest useful slice

1. Add concise caller guidance on the existing development MCP reference and
   its owning tool description: commit uses focused verification; none belongs
   to preview. No mandatory preview round-trip when the caller already has the
   exact patch and sufficient authority.
2. Document the existing profile ABI with one executable example from a real
   repository's existing suite. Reuse an existing suitable runner if available.
   Do not invent coverage mappings, synthesize passing counts, parse a printed
   sentence as proof, or add a second proof framework.
3. Validate that runner on a known-good candidate and a behavioral negative,
   with snapshot-only changes proving it did not silently test the live tree.
   A complete commit receipt must identify the actual suites and lint evidence;
   refusal must leave source bytes unchanged.
4. Only then try the next needed task. Fable offered inb-e68905: an existing
   two-form replacement refusal should name the expected/actual cardinality.
   Keep this a diagnostic fix; sequence-site lowering is outside this slice.

The pilot's frozen files and results stay unchanged. A new binding experiment
must use a separately named fixture and preserve the same proof for native and
Surgeon. This proposal does not claim that the old script can be plugged into
`:command` unchanged. If a substantial adapter is required, record that cost
and stop to judge whether a native patch plus the existing proof is simpler.

## Falsifier and accounting

Success is one real change admitted with a complete, candidate-bound receipt,
without an extra source inspection or repeated external suite needed to trust
that receipt. Correctness alone is not a wall-time win. Charge profile setup,
orientation, candidate generation, refusals, proof, cleanup and landing in
separate visible fields. Give the native arm identical reusable proof; freeze
the accepted task and protocol before either timed arm. Preserve losses.

Stop this slice if a competent caller still cannot reach the gate without
inventing a new test integration, or if that integration costs more than the
actual reuse can amortize. Do not turn a failed adoption attempt into a mandate
to use a second editor. The intended product is the model's existing patch
plus trustworthy evidence, with fewer decisions to finish the task.
