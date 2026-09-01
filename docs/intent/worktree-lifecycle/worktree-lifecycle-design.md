---
parent: high-level-design
prefix: WTL
---

# Worktree Lifecycle Controller

## Context and Design Philosophy

The repository creates many isolated worktrees for experiments, audits,
verification, and releases. Those worktrees are execution rooms, not the
durable record of what happened. A terminal room should be reclaimable only
after the useful result can still be found from a fresh checkout.

The controller closes one worktree at a time. It combines Git's exact checkout
state with Supacode's visible-workspace state, compiles a reviewable close plan,
and applies that same plan only if every relevant fact remains unchanged. It
never guesses an outcome from branch age or naming. The caller declares one of
three outcomes: `landed`, `negative-experiment`, or `parked`.

The controller removes present worktrees and resolves exact stale Git worktree
registrations whose checkout paths are already absent. It never deletes a
branch, tag, remote ref, observation, issue, archive, or retained result.

## Component Boundary

```text
Git worktree snapshot ─┐
Git remote snapshot ───┼──> pure lifecycle compiler ──> audit / close plan
Supacode snapshot ─────┤                                  |
outcome evidence ──────┘                                  v
                                                 reviewed plan hash
                                                          |
                              target-specific re-snapshot + exact comparison
                                                          |
                                      pending receipt -> Supacode archive
                                                          |
                                               Git worktree remove
                                                          |
                                      final receipt; branch remains intact
```

The pure compiler accepts closed data and returns classifications, reasons,
plans, or typed refusals. A thin I/O adapter owns subprocesses, canonical paths,
remote queries, receipt files, Supacode archival, Git removal, and issue
comments. Neither Git output nor Supacode output is interpreted by shell string
matching outside the adapter.

## Planned Repository Components

| Component | Responsibility | Boundary |
|---|---|---|
| `src/clj_surgeon/worktree_lifecycle.clj` | Closed schemas, pure classification, outcome validation, plan compilation, fingerprinting, and receipt compilation. | No filesystem, process, network, Git, Supacode, or Beads I/O. |
| `src/clj_surgeon/worktree_lifecycle_io.clj` | Capture exact authority snapshots, persist plans and receipts, execute one reviewed plan, and recover an interrupted apply. | Delegates every decision to the pure compiler. |
| `test/clj_surgeon/worktree_lifecycle_test.clj` | Exhaustive pure matrix and the registered field-failure fixtures. | Uses literal snapshots; no real worktrees. |
| `test/clj_surgeon/worktree_lifecycle_io_test.clj` | Temp-repository Git boundaries and fake Supacode, remote, and Beads adapters. | No shared worktree or live Supacode mutation. |
| `Makefile` | Discoverable `worktree-audit`, `finish-worktree`, and focused test targets. | Thin argument forwarding only. |

The implementation may use a small executable wrapper when required by the
Make target, but no wrapper may contain lifecycle policy.

## Authorities

| Fact | Authority | Non-authorities |
|---|---|---|
| Registered worktree path, HEAD, branch/detached state, lock, prunable reason | NUL-delimited `git worktree list --porcelain` from this repository's common Git directory | Directory names, Supacode labels, branch prefixes |
| Tree identity | `git rev-parse <commit>^{tree}` | Worktree modification time |
| Tracked and untracked dirt | `git status --porcelain=v2 -z --untracked-files=all` in the exact target | A clean diff of another checkout |
| Landing ancestry | `git merge-base --is-ancestor` against the explicit durable landing ref | Similar branch names or matching file contents |
| Remote durability | One bounded `git ls-remote` snapshot per configured remote, matched to the exact remote/ref/object tuple and peeled commit when applicable | A local branch, reflog, or stale remote-tracking name alone |
| Focused, pinned, archived, or authoritatively absent UI surface | A stable bracket of Supacode list, focused-worktree query, and repeated list for the exact target | Git state or a screenshot |
| Active agent lease | Git's worktree lock plus its owner/purpose reason | Process-name matching or worktree age |
| Negative-experiment meaning | A remote-published observation or Captain's Log plus its exact blob, experiment commit/tree pointer, and declared raw-evidence disposition | The experiment branch name alone |
| Parked ownership and next action | An open durable Beads issue plus the structured parking record appended by the controller | Chat history or an untracked TODO |
| Missing checkout | An exact no-follow filesystem lookup of the registered path, whose existing parent is canonical and contains no symlink component | Git's `prunable` prose, a missing Supacode tab, or a failed shell `cd` |
| Missing-registration preservation | The exact branch-backed registration HEAD and local full branch tip at that object, plus one unambiguous configured remote/effective-fetch-URL/full-ref advertisement whose commit equals or descends from that HEAD according to the selected proof kind | A detached HEAD, local branch, reflog, branch name, or similar file listing |
| Missing-registration ownership | Git worktree lock, lifecycle lease, unconsumed handoff, and exact Supacode focused, pinned, live, and `:main` facts for the identity | Process-name scans, worktree age, or an unpinned inactive tab |

If an authority is unavailable, malformed, duplicated, or contradictory, the
controller records `unknown` and refuses to classify the target as
`clean-safe`. It does not convert unavailable into false.

## Active Lease and Handoff Law

An agent-owned worktree is locked while work is active. The lock reason carries
at least the owner and purpose. The global Claude Code or Codex skill proposed
after repository validation must create or resume worktrees with that lease.
Process-name scans are not accepted as proof that an agent is or is not using a
worktree.

A compliant owner hands off in four explicit steps:

1. Compile an outcome request against the still-locked target.
2. Write a create-only handoff record under the common Git directory. It binds
   the canonical target, HEAD, tree, owner, outcome-request hash, and nonce.
3. Release the agent lock as the owner's final worktree action and leave the
   target session.
4. Let a controller in another worktree claim a create-only lifecycle lease
   for that exact handoff when it applies the reviewed close plan.

The lifecycle lease remains until completion, cancellation, or a typed recovery
transition. The controller releases it immediately before non-force Git
removal and reclaims it during recovery if removal did not happen. A handoff
record for different bytes, or more than one live handoff/plan for a target
fingerprint, refuses.

The controller also protects the primary worktree, its own control worktree,
the focused Supacode worktree, and every pinned worktree. An unlocked legacy
worktree has no inferred owner authority. It requires an explicit
single-target `legacy-handoff` declaration; that compatibility mode never
admits bulk apply and is recorded in the plan and receipt.

## Inventory Snapshot

`worktree-audit` captures one versioned snapshot over the union of Git and
Supacode identities. Every path has an absolute normalized lexical identity.
An existing path also has a canonical real identity; an absent path instead
binds its nearest existing canonical ancestor and stable filesystem identity.
Paths are compared as bytes after one percent-decoding of Supacode IDs. A
collision, symlink alias, relative path, or non-canonical request refuses
rather than selecting one identity. A Supacode-only identity whose checkout is
missing retains its strictly decoded lexical absolute path and remains
audit-only as `missing-prunable`.

The snapshot records:

```clojure
{:schema :clj-surgeon.worktree-lifecycle-snapshot/v1
 :captured-at instant
 :repository {:root canonical-absolute
              :common-git-dir canonical-absolute
              :primary-worktree canonical-absolute
              :object-format :sha1-or-sha256}
 :controller-worktree canonical-absolute
 :git-worktrees [{:path-lexical normalized-absolute
                  :path-state :present-or-absent-or-unknown
                  :path-real canonical-absolute-or-nil
                  :nearest-existing-parent
                  nil-or-{:path canonical-absolute
                          :device integer-or-nil
                          :inode integer-or-nil}
                  :head git-oid-matching-object-format
                  :tree git-oid-matching-object-format
                  :branch full-ref-or-nil
                  :detached boolean
                  :locked boolean
                  :lock-reason string-or-nil
                  :prunable string-or-nil
                  :status :clean-or-dirty-or-unknown-or-not-applicable
                  :removal-preflight
                  :not-applicable-or-{:eligible boolean
                                      :submodules :none-or-present-or-unknown
                                      :reasons [keyword]}}]
 :supacode {:available boolean
            :worktrees [{:id encoded-id
                         :path-lexical normalized-absolute
                         :path-state :present-or-absent-or-unknown
                         :path-real canonical-absolute-or-nil
                         :status :main-or-pinned-or-unpinned-or-archived
                         :focused boolean
                         :live boolean-or-unknown}]}
 :remotes {:available boolean
           :rows [{:remote string
                   :remote-url-sha256 hex64
                   :ref full-ref
                   :object git-oid
                   :peeled-object git-oid-or-nil}]}}
```

The exact output may add diagnostic fields under a versioned extension map.
Unknown top-level fields are not accepted by the pure compiler.

## Closed Classifications

The compiler returns exactly one classification per identity in the union:

- `active`: the primary worktree, controller worktree, focused worktree,
  pinned worktree, live worktree, contradictory `:main` surface, agent-locked
  worktree, or a worktree whose lifecycle lease belongs to a different plan.
- `dirty-blocked`: the path exists but tracked/untracked status is dirty, Git
  status is unavailable, identity aliases collide, or checkout linkage cannot
  be trusted. A known non-removable or unknown submodule state is also blocked
  before any Supacode mutation.
- `missing-prunable`: Supacode retains an identity with no Git registration, or
  Git registration/check-out linkage is missing or broken. This class has no
  ordinary close authority. A separate registration-prune request may select
  only the narrower Git-registered, path-absent case after proving durable
  content and no active ownership. A Git worktree that is authoritatively
  absent from a successful stable Supacode bracket is not missing; its UI state
  is `absent`.
- `needs-seal`: the checkout is clean and unprotected, but no still-valid
  reviewed close plan proves one terminal outcome.
- `clean-safe`: the checkout is clean and unprotected and one unconsumed plan
  still proves all outcome-specific evidence against the current snapshot.

Precedence is `active`, `dirty-blocked`, `missing-prunable`, `needs-seal`, then
`clean-safe`. A higher-safety classification cannot be hidden by a lower one.
Every non-safe classification carries stable reason keywords and the observed
facts needed to repair it.

For a Git-registered row whose path is authoritatively absent, status and
ordinary checkout-removal preflight are `:not-applicable`; their absence does
not reclassify the row as `dirty-blocked`. An unknown path state remains
blocked. A missing row can gain prune-plan eligibility only under the separate
contract below.

## Missing Registration Resolution

A registration-prune request is a second plan kind, not a fourth experimental
outcome. It removes stale Git administrative registration after the checkout
path is already absent; it does not claim that a `landed`,
`negative-experiment`, or `parked` close occurred. Supacode-only identities and
registrations whose lexical paths exist as any filesystem object remain
audit-only.

The request is closed data for exactly one target:

```clojure
{:schema :clj-surgeon.worktree-registration-prune-request/v1
 :target absolute-lexical-missing-path
 :preservation preservation-proof}

;; preservation-proof is exactly one of:
{:kind :branch-tip-on-remote
 :local-ref full-local-branch-ref
 :remote string
 :remote-url-sha256 hex64
 :ref full-ref
 :object git-oid
 :peeled-object nil}

{:kind :commit-on-remote
 :local-ref full-local-branch-ref
 :remote string
 :remote-url-sha256 hex64
 :ref full-ref
 :object git-oid
 :peeled-object git-oid-or-nil}
```

The preservation map is a tagged union whose `:kind` is exactly
`:branch-tip-on-remote` or `:commit-on-remote`. Both variants require a
branch-backed registration, registration branch equal to `:local-ref`, and the
local branch tip equal to registered HEAD. The first additionally requires the
advertised remote ref object to equal registered HEAD. The second requires the
registered HEAD to be an ancestor of the advertised remote endpoint, using the
peeled commit for an annotated tag. The endpoint must be a locally available
commit for the bounded proof. A detached registration is audit-only in this
slice.

Both proofs bind one unambiguous configured remote name, the digest of its
exact effective fetch URL bytes, full local and remote refs, advertised object,
and peeled object where applicable. Zero or multiple effective fetch URLs,
duplicate remote names, symbolic-only refs, non-commit endpoints, or movement
refuse. Request objects are expected values checked against fresh `ls-remote`
evidence, never caller authority. The proofs preserve Git content; neither
infers experiment meaning or deletes the preserved local branch.

Planning requires all of the following in one stable snapshot:

- one exact Git registration for the target and no identity collision;
- an absent final path proven by a no-follow filesystem lookup, with every
  existing ancestor canonical and non-symlink and the nearest existing parent
  captured;
- no Git worktree lock, lifecycle lease, unconsumed handoff, focused Supacode
  surface, pinned Supacode surface, live Supacode surface, or contradictory
  `:main` surface for the target;
- one current preservation proof; and
- a different clean controller worktree in the same common Git directory.

The plan binds the complete stale registration row, absent-path proof,
preservation proof, ownership facts, Supacode state, and controller identity.
Peer registrations are diagnostic only and are not plan authority. Apply uses
the same per-target OS lock, lifecycle lease, canonical plan file, monotone
journal, archive/restore boundary, immutable receipt, and recovery rules as
ordinary close. Immediately before the effect it repeats every target-specific
proof.

If the target Supacode surface exists and is unpinned and unfocused, apply
archives and verifies it. It then invokes exactly:

```text
git worktree remove <registered-missing-path>
```

The command runs without `--force`. The controller never invokes global
`git worktree prune` and never deletes or rewrites Git administrative files
directly. Git's exact remove command refuses when an unrelated directory has
reappeared without the registered worktree linkage; the controller also checks
path absence immediately before invoking it. A recreated path, changed
registration, new owner claim, moved preservation ref, unavailable Supacode
state, or live/focused/pinned UI refuses. The no-follow absence check binds the
nearest existing ancestor's canonical path and stable identity, walks every
existing component without following links, and repeats after Supacode archive
immediately before the closed Git command. Git's backlink validation is the
final guard against a filesystem object that reappears after that check.

The supported Git boundary is release-gated by executable fixtures on the
oldest supported Git version: an absent target removes only its registration;
an unrelated file, directory, or dangling symlink at the target refuses; a
locked registration refuses; and an exact peer remains registered. A Git
version without that witnessed behavior is unavailable authority, not a reason
to fall back to global prune or direct administration deletion.

Success requires the selected registration to be absent, the target path to
remain absent, the local branch and preservation ref to remain exact, and the
planned terminal Supacode state to hold. A planned absent surface remains
absent; a planned exact unpinned surface becomes that same identity archived,
not an `archived-or-absent` disjunction. Peer changes are recorded as
diagnostics but neither invalidate nor complete this target's plan. A boundary
witness must prove that the exact command never removes a peer.

Once journal state `prepared` durably records the exact selected row, later
authoritative absence of that row may satisfy the remove transition when path
absence, preservation, ownership, and exact UI postconditions still hold. The
receipt records `:registration-pruned` with
`:effect-observed :controller-or-external`; it does not claim controller
causation. Absence before `prepared` is typed `:already-resolved` and causes no
UI mutation. If removal fails after this invocation archived Supacode, the
controller restores the prior surface or returns a typed partial outcome.

One plan always owns one target. Batch cleanup is orchestration over independent
reviewed plans, locks, journals, rechecks, and receipts; a multi-target plan is
not part of this slice. One refusal stops only its target and cannot authorize
another target.

## Outcome Seals

### Landed

The caller supplies one full durable landing ref. The target HEAD must be an
ancestor of that exact ref, and the ref/object pair must appear in the captured
remote advertisement. A local branch, short hash, default branch assumption,
or tag name without its advertised object is insufficient.

#### Deferred rebase-landed proof

Ancestor proof correctly refuses work that reached the landing branch only
after rebase, squash, or cherry-pick changed its commit identity. A future
landed-seal variant may use an explicitly versioned patch-equivalence algorithm
over caller-supplied frozen target and landing ranges. `git cherry` and
`git patch-id --stable` are candidate mechanisms, not authorities. The design
must first define bases and ranges, squash/fixup, whitespace, tool-version
pinning, merges, empty commits, renames, mode changes, binary changes, reverts,
duplicate patches, attribution, and collision refusal. Until that contract is
ratified and witnessed, the controller continues to refuse non-ancestor landed
claims.

The motivating deferred field case is `cc-surgeon-create` at `c44ac759`, whose
content landed as main `64eac2ee` without ancestor identity. Those pointers are
a backlog witness, not current close authority.

### Negative experiment

The caller supplies:

- a repository-relative observation or Captain's Log path;
- the exact remote and remote-advertised ref that publish the document and
  contain the target worktree HEAD; and
- exactly one machine-readable negative-experiment seal embedded in the
  published document.

The seal is closed EDN between one named begin/end marker. It binds the exact
experiment commit/tree, the allowed observation/receipt paths between that
experiment and the terminal target HEAD, and exactly one raw-evidence
disposition: `none` or a content-addressed archive.

```clojure
{:schema :clj-surgeon.negative-experiment-seal/v1
 :experiment {:commit git-oid :tree git-oid}
 :allowed-terminal-paths ["docs/observations/example.md"]
 :raw-evidence {:kind :none}}
```

The archive variant replaces `:raw-evidence` with
`{:kind :archive :receipt-ref full-ref :receipt-path repo-relative
:archive-locator string :archive-sha256 hex64}`. The marker and seal grammar
are fixed in the public help; arbitrary Markdown is not interpreted as data.

The controller reads the document as one ordinary UTF-8 blob from the exact
advertised Git object. Absolute paths, `..`, non-canonical separators,
non-blobs, symlinks, submodules, malformed encoding, multiple seals, and
unknown seal fields refuse. The experiment commit must resolve to the supplied
tree and be reachable from the advertised breadcrumb ref. Every change from
the experiment commit through the target HEAD must be confined to the seal's
explicit observation/receipt path set. This proves that a later docs-only
audit commit did not hide unexplained product changes without requiring a
self-referential HEAD hash inside its own blob.

For `none`, the published seal itself declares that no external raw evidence is
needed to understand or reproduce the negative result. For an archive, the
seal binds a durable retention receipt reachable from the same advertised ref
or an allowlisted durable archive authority, a content-addressed locator
outside the target and ephemeral directories, and the archive SHA-256. The
controller validates the locator, receipt, and archive hash during planning,
again before Git removal, and again before finalization. It never opens or
copies raw content. The observation remains the human entry point; the plan and
close receipt preserve the exact machine pointers.

### Parked

The target must be branch-backed and clean. Its full local HEAD must equal the
advertised upstream HEAD for one exact remote/ref tuple so recreation cannot
silently select a different checkpoint. The caller supplies an open Beads
issue, its assigned owner, one non-empty single-line next action of at most 512
UTF-8 bytes, and a future ISO-8601 expiry. The plan binds the Beads store and
project identity, issue revision, status, owner, upstream tuple, and captured
clock.

Before UI or Git removal, apply appends an idempotent structured parking record
to the issue. That record binds the plan hash, target HEAD/tree, upstream ref,
owner, next action, and expiry. After successful removal, apply appends the
final close-receipt hash. The planned record remains truthful if a later step
fails: it states intent, not completion.

Every apply or recovery captures one controller clock and requires the expiry
to remain in the future immediately before Git removal. It also re-reads the
same Beads store and requires the issue to remain open with the same assigned
owner. The revision may advance only by the controller's byte-identical
idempotent parking append for this plan; any unrelated issue change refuses.
Closure, reassignment, remote movement, or expiry refuses before UI/Git
mutation when still possible and produces a typed partial outcome after an
earlier external step.

## Plan and Receipt Contracts

Dry-run writes a closed, canonical EDN plan under the common Git directory:

```text
<common-git-dir>/clj-surgeon/worktree-lifecycle/v1/plans/<plan-id>.edn
```

The plan declares exactly one operation kind: `:close-worktree` or
`:prune-missing-registration`. Both use the same storage, hashing, lease,
journal, receipt, privacy, and replay envelopes. Their target fingerprints and
postconditions remain operation-specific; a plan cannot change kinds during
recovery.

For `:close-worktree`, the plan binds the target's canonical present path,
status, and removal preflight. For `:prune-missing-registration`, it binds the
normalized lexical path, authoritative `:absent` state, nearest-existing-parent
identity, and `:not-applicable` checkout status and preflight. Both bind HEAD,
tree, branch or detached state as applicable, lock and lease facts, exact
Supacode identity and state, exact remote/ref/object tuples, active handoff,
lifecycle-lease prestate `absent`, the derived expected lease identity for this
plan, and the controller commit/tree plus exact controller artifact hashes.
An ordinary close plan also binds outcome-evidence blobs and hashes. The
controller worktree itself must be clean. Apply creates the expected lease only
after proving that the bound prestate is still absent; a foreign or different
lease is drift, not part of the plan.
Unrelated worktree creation or removal does not invalidate the plan. A change
to any target-specific authority does.

Canonical bytes use recursively key-sorted maps, ordered vectors, no public
sets, UTF-8, and one trailing newline. A random plan UUID identifies the file;
the plan SHA-256 covers every semantic byte except its own hash field. Atomic
create refuses an existing UUID with different bytes. Plan files and journals
use owner-only permissions and contain no source bodies, prompts, environment
values, remote URLs, or credentials.

Apply requires `PLAN=<absolute-path> APPLY=1`. It refuses loose outcome
arguments in apply mode and refuses `APPLY=1` without a previously written
plan. The plan has no time-based expiry; exact state equality is the authority.
One consumed plan cannot close another path or be applied twice as a new
operation. At most one unconsumed plan and lifecycle lease may exist for one
target fingerprint.

Apply holds a per-target operating-system file lock in the common Git directory
for every apply or recovery pass. Before any external mutation, it creates a
durable journal containing the plan bytes and hash. Journal transitions are
monotone:

```text
prepared
parking-intent-recorded
archive-commanded
archive-verified
remove-commanded
remove-verified
final-receipt-written
parking-completion-verified
```

Each transition uses atomic create/replace and synchronizes the file and parent
directory before the next external step. Supacode and Beads operations use the
plan hash as an idempotency key and verify exact postconditions. A divergent
same-key issue record refuses. Git removal is resumed only from observed exact
registration/path state. For a non-parked outcome, the two parking transitions
are still written with result `:not-applicable`; the journal never hides a
state by silently skipping it.

After success, apply atomically writes an immutable final receipt under:

```text
<common-git-dir>/clj-surgeon/worktree-lifecycle/v1/receipts/<plan-id>.edn
```

A parked apply then appends and verifies an issue completion record containing
the final-receipt hash and writes a separate immutable completion marker. This
avoids rewriting the final receipt to include a comment that itself cites the
receipt. Parked success is returned only after that marker exists.

A repeat invocation returns the existing terminal receipt or parked completion
marker only when the plan hash matches and every terminal postcondition still
holds. The target path and Git registration must remain absent; a recreated
path is `path-reused`, not idempotent success. An interrupted invocation uses
the journal to resume only the next incomplete idempotent transition. It never
recompiles a new outcome behind the same plan ID or adopts state from a
different plan.

## Public Commands

The repository exposes three paved Make targets:

```text
make worktree-audit [OUTPUT=/absolute/path.edn]

make handoff-worktree REQUEST=/absolute/close-request.edn

make finish-worktree \
  REQUEST=/absolute/close-request.edn

make finish-worktree PLAN=/absolute/path.edn APPLY=1
```

`handoff-worktree` validates the request against the locked owner worktree,
writes the create-only handoff record, and returns the exact final unlock
command; releasing the agent lock is a separate final owner action so the
command never silently revokes an active lease. A legacy request explicitly
sets `:handoff :legacy` and is accepted only one target at a time from a
different controller worktree.

Dry-run is the default for `finish-worktree`. It accepts either the ordinary
close-request schema or the registration-prune-request schema and emits the
corresponding one-target plan kind. Apply accepts only the persisted plan.
Standard output is one versioned EDN result; concise progress and remedies go
to standard error. Success exits zero. Refusal, unavailable authority, invalid
request, partial recovery, and execution failure use distinct nonzero exits
and stable `:error-type` values. `make help` shows both closed request schemas,
their evidence fields, and the no-global-prune guarantee. A request file
prevents Make or shell quoting from becoming an authority boundary for owner,
next-action, path, or ref values.

The audit command never archives, deletes, locks, unlocks, prunes, fetches,
pushes, creates issues, or changes focus. It may query remote advertisements
and local authorities. Handoff writes only the create-only handoff record. The
finish command never selects more than one target.

## Apply State Machine

```text
LOAD PLAN
  invalid / consumed / wrong repository -> REFUSE
  valid -> ACQUIRE PER-TARGET LOCK + LIFECYCLE LEASE

RE-SNAPSHOT TARGET
  any fingerprint or authority drift -> REFUSE, no mutation
  exact -> WRITE PREPARED JOURNAL

PARKING EVIDENCE
  parked and issue append fails -> REFUSE, no UI/Git mutation
  otherwise -> ARCHIVE UI

ARCHIVE UI
  Supacode unavailable or ambiguous -> REFUSE, no Git mutation
  target absent or already archived -> continue
  unarchived -> require stable list/focus/list bracket
  stable -> archive in background and verify archived/not-focused/not-pinned
  timeout or ambiguous completion -> PARTIAL; never remove

POST-ARCHIVE SAFETY GATE
  re-read Git registration, HEAD/tree/branch, status, handoff/lease,
  outcome authorities, remote refs, and parked issue/expiry
  compare non-UI facts to plan and UI facts to the planned archived state
  drift -> restore UI when this invocation archived it; refuse or PARTIAL

REMOVE GIT WORKTREE
  run from a different clean control worktree
  release only the matching lifecycle lease immediately before removal
  use git worktree remove without force
  failure -> restore prior UI state when this invocation archived it
  success -> verify path and registration absent, branch/ref/evidence unchanged,
             planned terminal UI state (:absent or :archived), and durable
             archive hash still exact

FINALIZE RECEIPT
  write final receipt atomically
  parked -> append and verify completion hash, then write completion marker
  branch, refs, tags, evidence and archives remain unchanged
```

If Git removal fails after this invocation archived the UI, the adapter attempts
to unarchive it. A failed restoration produces a typed partial receipt with the
exact remedy; it is never reported as a clean refusal. If Git removal succeeds
but finalization is interrupted, the pending receipt plus absent target allow a
repeat invocation to finish the receipt and parked completion marker.

`git worktree remove` is never called with `--force`. A locked, dirty,
submodule-bearing, malformed, or otherwise non-removable checkout remains for
human repair.

The registration-prune state machine uses the same transitions. Parking is
`:not-applicable`; archive transitions own an exact stale Supacode surface;
remove transitions own only `git worktree remove <registered-missing-path>`;
and finalization proves that the selected registration disappeared while the
path, branches, refs, preservation proof, and receipt remain exact. Peer rows
are diagnostic, because unrelated worktree changes remain outside this
one-target authority. Recovery never promotes an ordinary close plan into a
prune plan and may converge on externally observed absence only after
`prepared` under the exact rules above.

## Safety and Privacy

- The target registration must belong to the same Git common directory as the
  controller.
- The target may not equal the controller worktree or the primary worktree.
- An existing lexical absolute target must equal its canonical real path and
  contain no symlink component. A missing Supacode-only identity remains
  lexical and audit-only. Supacode IDs receive exactly one strict percent
  decode; malformed, double-encoded, NUL-containing, non-UTF-8, or
  byte-colliding identities refuse.
- Git object IDs are validated against the repository's advertised object
  format. Annotated tags bind both their advertised tag object and peeled
  commit; symbolic-only or ambiguous remote refs refuse.
- Unknown Git, remote, Supacode, or Beads state refuses before destructive
  action.
- Receipt data is limited to identities, hashes, paths, states, refs, issue
  IDs, owner, next action, expiry, commands, and outcomes. It never stores
  source bodies, prompts, transcripts, tokens, or environment dumps.
- Every subprocess receives an explicit working directory and closed argv.
  Shell interpolation is not an authority boundary.
- The controller never invokes branch deletion, global `git worktree prune`,
  force removal, direct Git-administration deletion, stash, reset,
  checkout-discard, remote deletion, or archive replacement.

## Global Skill Boundary

After the repository MVP survives the real corpus, a Claude Code skill and a
Codex skill reference may discover and invoke these Make targets. The global
layer may render results, request outcome evidence, and ensure active worktrees
are Git-locked. It may not reproduce classification, reachability, Supacode,
or apply policy. A repository without the command remains unsupported rather
than receiving a generic destructive fallback.

## Decisions & Alternatives

| Decision | Chosen | Alternatives Considered | Rationale |
|---|---|---|---|
| Lifecycle shape | Close one worktree, retain its branch | Delete worktree and branch together; archive Supacode only | Checkout reclamation and evidence retirement have different proof obligations. |
| Negative outcome name | `negative-experiment` | `rejected`, `failed-experiment`, `no-go`, `closed-negative` | The candidate did not earn adoption, but the experiment may have succeeded by disproving it. |
| Mutation boundary | Reviewed plan followed by exact-state apply | One command that plans and deletes; interactive confirmation | A saved plan makes drift and replay mechanically testable without depending on a terminal prompt. |
| Safe automation unit | One explicitly handed-off target | Bulk delete every apparently safe worktree | Existing unlocked rooms predate the lease rule; individual declaration limits blast radius. |
| Active-work authority | Agent lock -> explicit handoff -> controller lifecycle lease, plus current/focused/pinned/live protections | Process-name scans; age; open-tab count; treating unlock alone as consent | Explicit state transitions are stable and recoverable. The alternatives guess activity or classify stale UI as live forever. |
| Durable negative breadcrumb | Published human document plus exact experiment commit/tree and evidence disposition | Branch name alone; local receipt only; archive with no explanation | A future reader needs both the lesson and a machine-resolvable route to the exact experiment. |
| Parked breadcrumb | Pushed branch plus append-only structured Beads record | Chat reminder; local TODO; permanent worktree | The task ledger already owns durable work and can state who resumes what and when. |
| Operational receipt location | Fsynced journal and immutable receipts in the common Git directory, outside every removable worktree | Write into target; dirty the controller checkout; make a tag per close | Transition evidence survives target removal without changing source history or creating tag litter. |
| Missing registration handling | Separate one-target prune plan after exact absence, preservation, and ownership proofs | Global `git worktree prune`; force removal; direct deletion under `.git/worktrees`; treating missing as ordinary close success | A stale registration and a present worktree have different authorities. Git's exact non-force remove can retire one registration without granting branch or filesystem deletion authority. |
| Missing content preservation | Branch-backed target whose local ref stays at registered HEAD, plus exact tip equality to one advertised remote ref or target ancestry to one advertised remote commit | Detached HEAD; local branch alone; reflog; branch name; similar file listing | Cleanup must prove a durable remote route while retaining the local branch as a recovery anchor. |
| Batch cleanup | Compose independent one-target plans and receipts | One multi-target plan or one global prune command | Per-target review and recheck keep a stale row from expanding another target's authority. |
| Rebase-landed seal | Deferred versioned patch-equivalence variant; ancestor proof remains current | Treat equal-looking trees or commit messages as landed; make `git cherry` authoritative | Patch equivalence may recover valid rebases, but bases, ranges, squash, merge, binary, rename, revert, attribution, tooling, and collision semantics need a separate ratified contract. |
| Cross-client reuse | Global skills invoke the repository command | Copy shell policy into Claude and Codex skills | Repository policy evolves with its tests; global orchestration stays small and non-destructive. |

## Open Questions & Future Decisions

### Resolved

1. Worktree closure and branch retirement are separate operations.
2. Negative experiments retain a published explanation and exact commit/tree
   pointer; a hash without a durable ref is insufficient.
3. Apply is single-target, plan-bound, journaled, non-force, and executed from
   another clean worktree.
4. Active ownership transfers through an explicit handoff and lifecycle lease,
   not unlock state or process inference.
5. A missing Git registration is resolved only by a separate exact
   registration-prune plan; it is never ordinary close success.

### Deferred

1. Branch, tag, and remote-ref retirement after a separately measured corpus
   and design review.
2. Multi-target scheduling over independent registration-prune plans.
3. Rebase-landed proof using an explicitly versioned patch-equivalence
   algorithm; `git cherry` and stable patch IDs remain candidate mechanisms,
   with base/range, squash, merge, binary, rename, mode, revert,
   duplicate-patch, attribution, tool-version, and collision law unresolved.
4. Upload or central projection of local operational receipts beyond the
   repository's common Git directory.
5. Expired parked-work escalation and reporting across repositories.
6. Global skill publication until the repository command passes a real
   read-only audit and at least one deliberately selected close trial.

## References

- [High-Level Design](../../high-level-design.md#close-terminal-worktrees-without-erasing-experiments)
- [Testing Guidelines](../../testing-guidelines.md)
- [`bench/retain_benchmark_result.sh`](../../../bench/retain_benchmark_result.sh)
- [`git worktree` documentation](https://git-scm.com/docs/git-worktree.html)
- Supacode CLI worktree list, status, and archive contract
