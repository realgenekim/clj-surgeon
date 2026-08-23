# Captain's Log: Refactoring paid in three currencies

**Date:** 2026-08-10

## Question

We invoked Kent Beck's rule repeatedly: make the change easy, then make the
easy change. Did those refactors merely improve the internal design, or did
they help us deliver useful behavior sooner, more safely, and with less
cognitive friction?

## Verdict

They paid back in all three currencies.

The decisive evidence is not a smaller helper or a cleaner namespace. A fresh
Codex session received one formatted Clojure file and 17 exact obsolete owner
names. It made one `apply_clojure_changes` call, deleted all 17 owners, ran the
fast verification profile, read the bytes back, and received an inverse
receipt. It used no structural preflight read, semantic query, marker form, or
native patch. Direct tool time was 1.216 seconds.

That behavior became easy only because several earlier refactors had moved
mechanics behind stable boundaries.

## The Beck sequence

| Make the change easy | Then make the easy change | Payoff |
|---|---|---|
| Extract one canonical MCP schema | Add `delete=true` to the direct contract | Published schema and validator cannot silently disagree |
| Keep one transaction kernel | Lower whole-owner deletion into existing edits | Parsing, hashes, atomic writes, rollback, verification, and receipts were reused |
| Establish exact-owner addressing | Accept a complete ordered owner set without cclsp | Known mechanical work no longer waits for a semantic graph |
| Project cclsp lifecycle from one pure function | Make health and semantic admission report the same state | Five lifecycle states are tested as plain data |
| Publish code and schema changes through hot reload | Dogfood each small batch immediately | The shared server did not restart |
| Keep one compact agent contract | Teach direct deletion once | A clean caller selected the intended route independently |

This is the important causal order. We did not start by adding a large
`delete-owners` subsystem. We first made schema truth singular, addressing
explicit, transaction execution reusable, and runtime state testable. The
feature that followed was therefore mostly a small contract and lowering
change.

## Sooner

The field refactor described in
`2026-08-10-captains-log-the-scalpel-was-fast-the-refactor-was-not-compiled.md`
made 118 Surgeon calls. The tool was an excellent microscope, but the caller
still carried the move through previews, reads, marker replacements, native
cleanup, and verification.

The new known-owner route is shorter:

```text
before
  decide owners
    -> inspect
    -> prepare
    -> replace with markers
    -> delete markers natively
    -> reconstruct aggregate state
    -> verify

after
  decide owners
    -> submit one guarded transaction
    -> receive one verified receipt
```

The 1.216-second measurement is direct tool time, not a controlled whole-task
speed comparison. The stronger claim is mechanistic: a complete decision no
longer creates several additional model/tool boundaries. We deleted work
rather than optimizing each call.

Development also became sooner. The deletion feature reused the compiler and
commit machinery instead of introducing a second writer. Focused MCP tests,
hot reload, and a clean-caller probe made the feedback loop small enough to
find product defects during implementation rather than after release.

## Safer

The safety improvement is stronger than the speed result:

- every named owner must resolve exactly once;
- expected edit and file counts are mandatory;
- every future file parses before the first write;
- writes are failure-atomic;
- written bytes are read back and hashed;
- optional verification failure rolls back the transaction;
- the inverse receipt is hash-fenced;
- semantic-provider availability is irrelevant when exact owners are known.

Dogfooding deletion found an equal-offset undo defect for adjacent owners. The
first undo refused rather than writing incorrect bytes. We retained original
source offsets in inverse edits, added the ordering regression, and then proved
byte-identical restoration.

The full suite later caught a different regression in our help text: a rewrite
had removed the exact executable token `[:replace SOURCE]`. We restored the
contract phrase and kept the test unchanged. That is the Beck payoff in its
most concrete form: the design lets tests protect behavior and language at the
smallest useful boundary.

Final evidence remained green:

- Surgeon: 634 tests, 5,412 assertions, zero failures;
- MCP: 136 tests, 1,102 assertions, zero failures;
- cclsp: 311 passed, five skipped, zero failed, 976 expectations;
- clj-kondo: zero errors and zero warnings on the focused files;
- Standard Clojure Style, TypeScript typecheck, Biome, and `git diff --check`
  passed.

## Happier

Happiness here is not aesthetic preference. It is a reduction in mechanical
state that the caller must carry.

```text
old experience
  remember 17 targets
  track which ones have moved
  preserve temporary markers
  remove markers later
  reconstruct what remains
  inspect the aggregate diff

new experience
  state 17 targets once
  receive one uneventful verified transaction
  continue thinking about architecture
```

The model spends less context on bookkeeping and more on judgment. A successful
call is compact and terminal. A refusal changes zero bytes and identifies the
violated contract. The interaction begins to feel like compiling an editor
macro rather than supervising a sequence of patches.

## Where the refactoring has not paid yet

The result is a local optimum, not completion of the vision.

`clj-surgeon-ws5` remains open because fast verification still compares a
transaction against an absolute clean baseline instead of a diagnostic delta.
`clj-surgeon-ss5` remains open because namespace extraction is not yet one MCP
transaction that creates the target, synthesizes requires, rewrites callers,
deletes owners, verifies, and emits one undo receipt.

Those two issues now have narrow, composable seams. Fix diagnostic-delta
verification first so a correct extraction is not rejected by historical
warnings. Then compile extraction through the same addressing, transaction,
and receipt machinery that made deletion small.

## Bottom line

The refactors did not merely make clj-surgeon easier to maintain. They changed
what an agent had to do.

```text
sooner  -> fewer model/tool boundaries and faster implementation feedback
safer   -> one guarded compiler and reversible atomic execution
happier -> architectural judgment stays in context; mechanics leave it
```

That is the standard for future refactoring here: the internal design earns
its keep only when the next valuable behavior becomes smaller, safer, or more
pleasant to deliver.
