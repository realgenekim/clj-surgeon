# Astra: make the existing patch gate usable with supplied proof

Status: jointly reviewed and exercised on one real-source scratch fixture; the existing API and verification law are unchanged. Tool-description guidance is implemented on the branch; live catalog exposure and portable example packaging remain unfinished.
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

## Joint-review additions and the next usage meter

Fable approved this slice and requested these additions before use: put the
same recovery sentence in the refusal itself, reconcile obsolete precedence
and unverified-commit prose in the owning specification, and use an existing
real Maven suite for the executable example. Fable owns the canonical refusal
construction; this branch owns the profile example and documentation. A valid
server profile is sufficient, so recovery must not imply that every workspace
must create a repository profile.

For each task, record whether the caller invoked the gate. Among tasks with a
gate invocation, report how many first gate calls returned a complete,
candidate-bound verification receipt without a preceding gate refusal or
profile repair in that attempt. Also report mode, committed status, number of
later calls and total task correctness. A verified preview is not a commit;
a native-only caller has no gate attempt and is not a failed gate call. A
prepared integration demonstration is not free-choice adoption.

The frozen pilot therefore has one attempted first gate call, zero first-call
complete receipts and zero tool commits, while both tasks completed correctly.
Future successful setup cannot retroactively change that row. Charge the
adapter and proof-binding preparation separately and expose how many later
uses would be needed to recover it.

The example remains a restricted existing-file modification: recording-query's
real test namespace in the frozen Maven closure. Its adapter must overlay the
candidate onto a private copy and produce per-namespace counts from actual
clojure.test results. The supplied ABI does not encode deletion tombstones;
do not generalize the example to deletion or arbitrary workspace reconstruction.

## Actual integration outcome

The originally suggested diagnostic task was completed earlier through the installed guarded CLI and landed. The subsequent proof-profile use named a shared text-length bound in real Maven transcript code. Candidate/live polarity checks demonstrated the runner loaded the candidate; the first public commit still refused because the adapter rejected two gate-created control files. The exact two-path repair preserved source/test/dependency checks, and the same patch then received a complete verified commit in 2.145 seconds. At that first-task checkpoint, first-call complete receipts were 0/1 and successful public calls 1/2. A second useful helper extraction then succeeded on its first call in 2.069 seconds using the unchanged v3 profile: aggregate first-call-complete tasks 1/2 and successful public calls 2/3. [Full evidence](../observations/2026-09-06-astra-real-profile-utility.md).

The restricted adapter and reports are retained experiment artifacts, not a portable profile for arbitrary repositories. No new ABI or weaker commit policy was needed. Preparation amortization and first-call usability remain open; do not count the mechanism demonstration as shipped adoption. The additional recovery sentence owned by the refusal-text branch remains parked with that branch, and the branch tool description now carries the explicit focused-profile guidance already in the skill reference. The live catalog still needs a separately authorized refresh before claiming callers saw it.
