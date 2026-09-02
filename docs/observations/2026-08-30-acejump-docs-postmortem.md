# AceJump after the measurements: a postmortem of the 2026-08-29 label and golf proposals

Date: 2026-08-30 PT

Author: `sol <sol@skiff>`

Method: documentary review and zero-model judgment; no model cohort or model-call experiment

## Verdict

The AceJump metaphor contained one durable product insight and one bad product conclusion.

The durable insight was: **a read can cheaply return enough structured evidence to make a later
write easier to construct.** That survives, and the prepared-request recovery results now give it
two independent directional receipts.

The bad conclusion was: **the model should write back a short label in place of the subject.** That
is dead. Opaque labels, readable prefixes, structural paths, ordinals, gutter codes, and homerow
chords all have the same authority defect when they select a mutation target: another valid token
is another valid subject. Constrained decoding makes the token well formed; it does not make it the
one the caller intended. A snapshot proves that the selected subject exists and is current; it does
not prove that the caller meant that subject.

The surviving product shape is therefore not “paint code with tiny write handles.” It is:

> On an already-required exact read, return a non-executable, snapshot-grounded write descriptor
> that repeats full typed subject identity and guards, leaves only caller-owned decision holes, and
> submits through the ordinary guarded write path after those holes are filled.

That is prepared-request recovery, not AceJump mutation authority. It saves assembly and recovery;
it does not improve routing. The routing story changed sign across fixtures and is dead.

The rest of the old portfolio divides cleanly:

- **SURVIVES:** closed semantic operations that compress repeated structure; server-owned read IDs
  plus a structurally implied `forms` operation; rich read evidence; exact full-identity prepared
  descriptors; turn deletion as the performance objective.
- **DEAD:** opaque or mnemonic write authority; guards meant to rehabilitate those labels; broad
  declared-intent compression; wrapped EDN carriage; the proposed single-anchor splice; catalog
  shrinking; “add a terminal signal”; pre-composed menus whose choice can change the subject.
- **TRANSFORMED:** AceJump overlays become read-only navigation; URL-like identifiers become display
  strings over typed identity; diff syntax becomes either an existing semantic operation or a
  bounded exact literal edit; lavish read annotations become piggybacked prepared evidence, never
  an extra discovery turn; terminality moves from one tool result to the client/task contract.

## Evidence rules used for this review

The old documents mixed three epistemic kinds. This review keeps them separate.

1. **Measured:** derived from retained requests, rollouts, source, or controlled trials with a
   replayable receipt.
2. **Projected:** a deterministic byte/token re-encoding or a priced consequence of a measured
   slope. It is not observed task speed.
3. **Relayed:** a coordinator or brief repeated another agent's interpretation without independently
   re-deriving the number or checking the named mechanism. A decimal point does not promote this to
   measurement.

The laws that bind the verdicts are:

| Law | Evidence status | Consequence here |
|---|---|---|
| Write emission is `3.5237 ms/byte` over the within-format production corpus (`R²=0.9807`, `n=59`, 76.3× spread). | Measured at `935cc0d`; later retained as a descriptive within-format price. | Byte reductions can be priced, but format changes must be token- and wall-measured. |
| Copying is not cheaper than composing: `0.96×`; predictable copy was also `0.96×`. | Controlled `n=9` arms at `dd36336`; real-Clojure carriage later agreed. | “The model can just echo the label/call/source” receives no emission discount. |
| One turn costs about 222 output tokens at the raw floor and about 620 at the working floor. | Measured floor and working-route arithmetic. | A sub-turn byte win cannot compete with a removed continuation. Do not add a read turn to establish labels. |
| Production JSON tool arguments are schema-constrained at the sampler. | Three probes, 5/5 each, at `93d9918`. | JSON syntax errors are prevented. A valid wrong label remains valid, while EDN inside a string forfeits the guarantee. |
| The flagship request repeats structure, not subjects: 47 write occurrences, 44 distinct subjects. | Zero-model corpus fold at `dacf768` / synthesis at `ecef504`. | Subject dictionaries have almost nothing to amortize; structural relations do. |
| External owner-visible repetition is 8/60 = 13.3% in the observable subset. | Preregistered external census `28ee81f4`; only 19.4% of hunks exposed owner identity. | Guarded-label economics hold. File repetition alone does not reopen owner-label authority. |
| IDs are safe to omit only when they are presentation-only and ownership is all-supplied or all-omitted. | Buildability audit `96baa2ad`; installed closed normalization `c55de22`. | Call-local read IDs may be compressed. Mutation subjects may not be replaced by IDs. |
| Text that merely resembles an owner is not identity; the returned selection must parse as one top-level form whose structural owner-token range equals the selection range. | Prepared-request correction `b445a8c`. | A label, matching substring, comment, or metadata occurrence cannot prove the write owner. |
| Routing depends on whether the decision is discovered or supplied, not on change count alone. | Acid crossover ladder `f6ac0b5`. | Native won discovery through 32 changes/6 files; Surgeon wins the large class only when the exact decision is already supplied. |
| Prepared evidence reduced recovery in two independent screens but did not improve routing. | `6277e06` and `ab5759e`; sign-flip synthesis `5d28b13`. | Preserve the recovery product claim; kill the routing claim. |

## The relayed-Opus telephone game

The main historical failure was not that the 29 August program failed to measure. It measured a
great deal. The failure was that an Opus coordinator turned first-pass interpretations into facts
while relaying them to Gene and into later briefs. The raw `sol-*.txt` sessions are not independent
sources when they quote those briefs; they often reproduce the same claim because the coordinator
put it in their prompt.

| Relayed claim | What direct review found | Judgment |
|---|---|---|
| “191 mechanically derivable corrections.” | The server had candidate vocabulary, not selection authority. The claim confused a hint with a correction. | **WITHDRAWN.** Complete vocabulary survives; auto-selection does not. |
| Candidate-list truncation created a 33.5-minute recovery prize; later, “137/137 lists were complete.” | The first mechanism claim was wrong; the later correction was also wrong. Independent review found only 25/191 complete lists. Complete vs truncated reread was 48.0% vs 80.7%. | **WITHDRAWN twice.** This is the clearest telephone-game example. |
| `21.2 h` of ceremony, `812×` the write-grammar prize. | Counts reproduce: 279 turns, 5,499 items, median 12, zero ending at the last decision. One 0.46%-covered idle turn contributed 11.52 h. Measured union wall is 5.68 h; outlier-excluded wall is 9.74 h; ratio 218–373×. | **Magnitude dead; direction survives.** |
| “Add an explicit terminal signal.” | `verification_complete=true`, `next_action=none`, and `terminal_response` already shipped. Only 3/277 turns ended their last decision with Surgeon; none ended at the decision. | **Proposal dead at this layer.** |
| `+490 ms` of catalog tax per turn. | A 98-position controlled screen measured about 40.75 ms fixed and no byte slope; hiding tools creates an under-declaration failure. | **WITHDRAWN; catalog shrinking dead.** |
| Wrapped EDN was an 18.4% no-brainer. | The comparison omitted the mandatory outer JSON string. Full-corpus replay saved 4.931% write bytes but added 1.389% write tokens; reads grew 2.306% in bytes and 12.253% in tokens. | **DEAD.** |
| Declared intent costs about 13% of the mnemonic prize and nets about 20.9%. | The flagship has 44 subjects for 47 writes. The optimistic declaration made the request 1,626 bytes larger; 178/190 requests (93.684%) lost bytes. | **DEAD.** Wrong sign. |
| Copying a rendered call/source should be cheaper than composing it. | Copy/compose was 0.96×, and predictability did not help. | **DEAD as a price assumption.** |
| “Read results can be as lavish as you like.” | Prefill is extremely cheap, but standing context is re-prefilled and an establishing read costs a turn. | **TRANSFORMED:** enrich an already-required result under an output cap; never buy a turn just to save write bytes. |
| About 17% of production writes silently corrupted bodies. | The harness emitted free text; it did not call MCP or write files. A direct MCP bisection committed 5/5 sensitive strings exactly and refused malformed source. | **WITHDRAWN.** Model content error remains possible; the claimed tool defect did not exist. |

The measured results should not be demoted because they passed through the same evening. The
`3.5237 ms/byte` slope, `0.96×` copy result, constrained-decoding probe, mnemonic size arithmetic,
wrong-valid-label mutation, guard cost, EDN token sign, read-ID fold, and splice corpus counts all
have direct receipts. The table above identifies the interpretations that did not.

## Proposal-by-proposal judgments

### 1. AceJump overlay labels on every Clojure symbol

**TRANSFORMED.** As read-only navigation or a visual cross-reference, an overlay is harmless and
may be useful. As the sole selector in a write, it is dead.

The original five renderings were opaque AceJump codes, a gutter rail, name-derived mnemonics,
structural paths, and two-stroke homerow chords. Four are plainly opaque. The fifth is readable but
still abbreviates identity. In all five, substituting another valid code creates another valid
request. The wrong-valid-label witness changed `beta` while the stated intent named `alpha`, returned
`ok=true`, and reported `verification_complete=true`.

Constrained decoding sharpens rather than repairs the problem: it can force the model to emit one
member of the label enum, making garbage impossible while leaving every wrong valid member
available. That changes obvious refusal into silent subject substitution.

**Promotion evidence:** none for direct write authority; the deterministic impossibility result is
terminal. A read-only overlay could be promoted only by a navigation/usability study showing fewer
actions without any label entering a mutation request.

### 2. Name-derived labels such as `~alph` and `~render-h`

**TRANSFORMED.** The size result survives; the authority claim does not.

The frozen nine-file/37-owner basis round-tripped exactly and shrank the 2,871-byte request by 697
bytes (24.277%). That was a real representation result, not boofarama. It was also conditional on a
shared basis: carrying the explicit table made the request 801 bytes larger than baseline.

Readable prefixes improve salience, but `~alpha` versus `~beta` is still a caller choice. Full file
and typed owner identity must remain authoritative. The external census does not reopen the case:
file identities repeated 52.0%, but owner-visible repetition was only 13.3% in a low-coverage
subset. There is still no demonstrated owner-level amortization and no new authority mechanism.

The surviving use is a cosmetic label next to canonical identity, scoped to one frozen result,
collision-lengthened, exact-match-only, and echoed with resolved identity in receipts.

**Promotion evidence:** for cosmetic navigation, a no-mutation action-reduction screen. There is no
promotion path to sole mutation authority absent an independent canonical effect that already owns
the subject—in which case the label is no longer the authority and supplies little value.

### 3. Guarded labels and declaration-first intent

**DEAD.** This was the most thoroughly falsified family.

A per-label content guard is rung 4: it catches a mismatched pair but accepts a coherent wrong label
plus that label's correct guard. The favorable request saved 982 bytes before guards; a 22-character
guard plus envelope returned 975, consuming 99.3% of the prize, while 14/14 deletions had no guard.

A declaration of the complete subject set is semantically stronger but economically worse. In the
flagship, only three of 44 subjects repeat. Even an impossible zero-syntax declaration cost at least
1,796 bytes against 170 bytes of mnemonic savings in that fold, net `-1,626`. Across the corpus,
93.684% of complete requests lost bytes. Counts never bind identity, and an exact subject set still
does not bind heterogeneous effects to the right subjects.

The all-or-none-ID law does not rehabilitate labels. It is safe precisely because generated read
IDs are presentation-only and the complete subject selectors remain. Likewise, owner-token proof
does not make a label authoritative; it proves that the full returned source and range denote one
owner before a descriptor repeats that owner in the ordinary write grammar.

### 4. URL-like `path#owner` identifiers

**TRANSFORMED.** The `path#owner` proposal was not opaque: it concatenated readable identity and
saved 8,109 bytes (1.29%) across 195 writes. But applying the same spelling to reads repeated paths
at 2.05 forms per subrequest and added 91,372 bytes (20.07%). The optimization is fan-out-specific,
not a universal format principle.

The later owner-token law also makes the string insufficient as a general typed identity. A
multimethod implementation needs dispatch identity; namespace forms use a different address; a
separator needs unambiguous escaping. Keep URL-like text as a display rendering over a typed object,
or for a closed one-owner write variant whose parser can reconstruct the exact typed identity. Do
not make it a universal ID or a cross-call handle.

**Promotion evidence:** a pure encode/decode proof over every public owner kind, zero collisions,
identical canonical subjects, token savings above a material threshold, then a matched task-class
cohort. At 1.29% corpus savings it is not currently worth that surface.

### 5. Read-request golf: omit IDs and default `forms`

**SURVIVES—and was built.** This is the clean positive control for the whole review.

The 1,242-call fold removed 84,263 of 455,185 bytes (18.512%). Two tokenizers retained a
16.43–16.99% reduction. The fields are protocol and closed grammar, not subject identity. The safe
rules are all IDs supplied or all omitted; mixed ownership refuses before reading. `operation` may
be omitted only when the remaining fields structurally prove a complete `forms` request. Other
operation-less shapes refuse.

The user-supplied short SHA `96aa2ad` is a transposition. The actual buildability commit is
`96baa2ad954b157ca9404708faee356fa81f023e`; the normalization later shipped at `c55de22`.

**Promotion evidence:** the representation and safety claim is already installed. A speed headline
still requires a matched Anvil comparison on a named read task class; the 296.918-second corpus
figure is a slope-based projection, not observed task wall.

### 6. Server-owned guards and whole corrected calls

**TRANSFORMED.** The original suggestion that a snapshot hash could replace `from` was too broad.
The splice audit established that the server-computed file hash guards compile-to-commit drift,
whereas `from` carries model-belief-to-compile evidence. Removing `from` from replacements weakens
the contract.

What survived is server preparation without guard deletion. An eligible exact read can return the
full old owner source, `matches=1`, full file and owner identity, and null replacement holes. The
descriptor is `executable=false` and `write_authority=false`; after the caller fills every hole, the
ordinary writer recaptures and rechecks everything. This is the prepared-request design proved by
`b445a8c`.

Rendering a complete corrected call for the model to retype is not a byte win because copy costs the
same as compose. Returning a non-executable prepared object can still reduce construction mistakes
and recovery, which two independent screens observed. Auto-execution remains lawful only when no
caller decision remains and canonical subject/effect identity is unchanged.

**Promotion evidence:** the product-shaped recovery gate in the ratified design: exact correctness
in both arms, fewer construction refusals and recovery actions in both blocks and pooled, at least
25% median output reduction, and no wall/fallback regression. Routing is not an admissible rescue
metric.

### 7. Prepared candidate menus and selection instead of authorship

**DEAD when selection can change identity; SURVIVES only as prepared recovery with full identity.**

A menu of short candidate IDs is AceJump with the table moved into the result. A constrained decoder
can guarantee membership, not intent. Ranked hypotheses are `authority=false`. Mixed or partial ID
ownership must refuse. The prepared-request implementation deliberately contains no request ID,
basis ID, continuation ID, site ID, plan ID, or other opaque write reference.

The original routing rationale is dead: the three relevant fixtures produced `+50 pp`, `0 pp`, and
`-50 pp`. The last two independent cohorts nevertheless agreed on recovery direction: median output
fell 47.4% with six construction refusals eliminated in one, and 30.0% with refusals 7→4 and recovery
actions 20→8 in the other. That is a different mechanism and a different product claim.

### 8. Wrapped EDN and “edit Clojure with Clojure”

**DEAD as public carriage; TRANSFORMED into closed semantic lowering.**

Wrapped EDN round-tripped exactly but lost on the priced unit. It reduced write bytes 4.931% while
increasing write tokens 1.389%; reads grew 2.306% in bytes and 12.253% in tokens. Direct generation
confirmed that time followed token count and gave no Clojure-domain coherence discount. JSON tool
arguments also retain a sampler-enforced structural guarantee that EDN inside a string loses.

The deeper idea—express code operations as inert Clojure data in a closed vocabulary—survives as an
internal compiler design and as existing semantic operations such as symbol migration and require
change. It should not become a second public wire grammar for a 382-byte residual over the already
compact relation request.

**Promotion evidence:** only a new non-byte advantage could reopen a public grammar: materially
better decision coverage, fewer turns/refusals, exact effect parity, and no loss of constrained
decoding. Smaller serialized bytes alone are insufficient.

### 9. Single-anchor splice and two-anchor diff syntax

**Single-anchor splice: DEAD. Two-anchor exact edit: TRANSFORMED into a low-priority hypothesis.**

The proposed splice failed three independent gates:

- only 7.38% of insertion-reachable calls cleared 1,300 echo bytes;
- whitespace-insensitive matching converted 2/63 no-match refusals (3.2%), below the 5% kill line;
- `after` already meant new text in the shipped `before`/`after` alias pair, so the proposed anchor
  spelling created a silent replace-the-anchor path.

It also targeted the minority shape. Insertions were 28.22% of `from`/`to` pairs; true replacements
were 68.46% and held 83.17% of their bytes. Existing `find` plus `insert_after`/`insert_before`
already covered 941 operations.

The broader observation survives: 42.5% of retained `from`+`to` bytes were shared prefix/suffix, and
65.2% of that echo lived inside true replacements. A lawful two-anchor form must retain full typed
subject identity, reconstruct the exact old bytes, preserve exact matching, use unclaimed field
names, and lower into the ordinary guarded transaction. The independent diff review measured only
362 bytes saved in the flat request and 58 in the relation request—below a turn even before a new
grammar's authorability cost.

**Promotion evidence:** an external corpus with many distinct large replacements, a zero-model
token saving above a whole-turn threshold on a material share of eligible tasks, exact old/new
reconstruction, and then a matched complete-wall cohort. Positive bytes in the old fixture are not
enough.

### 10. `replacement_groups`, file tables, and general emission composition

**TRANSFORMED.** `replacement_groups` correctly compresses repeated decisions and is on the right
side of the law. Alone it saved 1,189/6,409 characters (18.55%), below its registered 20% gate. Its
compositions that crossed 20% did so by adding file indexes or file groups; the former produced a
silent wrong-file mutation and the latter retained a correctness loss. No composition was additive.

The durable mechanism is the closed relation: state one semantic relationship, preserve full
subjects, and lower it to exact effects in the server. Do not generalize this into a bag of
independent compression tricks or resurrect numeric cross-references.

**Promotion evidence:** a new closed relation must be injective, preserve readable/typed subjects,
clear its preregistered payload gate on a frozen real task, then win a matched supplied-decision
cohort. Discovery-required performance cannot be borrowed from a supplied-decision receipt.

### 11. Goal-shaped transactions and deterministic suffix execution

**TRANSFORMED by the discovered-versus-supplied axis.** The old proposal to fuse inspect, prepare,
apply, format, and verify was too broad. The acid ladder measured native faster at every
discovery-required rung through 32 changes and six files. Surgeon incurred 19 discovery failures;
the atomic writes themselves were fast.

The idea survives when the exact decision is already supplied or mechanically closed. Then one
guarded transaction can delete actions and turns, as the 51-effect/nine-file relation receipt did.
When the task requires discovery, native discovery remains the measured route for this class; a
later exact decision may still cross to Surgeon.

**Promotion evidence:** separate matched cohorts for discovered and supplied decisions. Never use
change count alone, and never combine the `>32` discovery lower bound with the 51-edit supplied
receipt as though they bracket one crossover.

### 12. Rich terminal results and post-decision turn deletion

**TRANSFORMED.** The objective survives; the original actuator does not.

The 5,499-item count, median 12, and zero-of-279 terminal behavior are measured. The 21.2-hour wall
and 812× headline are not. The shipped terminal signal could reach only a tiny fraction of last
decisions because 274/277 ended with native file changes. Returning more source may replace some
recovery reads, but it cannot discharge formatter, lint, tests, documentation, Git, or unfinished
task obligations—and availability does not prove the caller will consume the evidence.

Move terminality to the client/task contract: track remaining obligations across all write routes,
accept terminal evidence from both native and Surgeon paths, and end only when the task-level list is
empty. Within doctrine, the measured safe scoping prize is about 359 actions/10.2%, led by 254 pure
Git inspections already forbidden; formatter and linter can batch, while tests and commit cadence
retain their safety purpose.

**Promotion evidence:** first a zero-model classification of which tail actions the new contract can
actually discharge; then a matched client-level cohort. A louder Surgeon-only flag has already
failed structurally and should not be tested again.

### 13. Leased or lazy tool catalogs

**DEAD as a latency lever.** The needle packet ranked catalog leasing second by multiplying a
relayed `490 ms` per-continuation estimate across 5,499 tail items. The controlled 98-position
screen later measured about 40.75 ms of fixed request overhead and no response-time slope with
catalog bytes. It also exposed the product cost: an absent catalog is not a cheaper equivalent
request; it withholds the operation contract the caller needs.

This does not prohibit ordinary client caching or demand-driven UI help. It kills the claim that
hiding or lazily declaring the schema is a measured way to buy back model wall. The sampler's JSON
constraint is another reason to preserve, not obscure, the exact operation schema at the point of
use.

**Promotion evidence:** only a transport change that preserves complete operation discoverability
and constrained decoding, plus an interleaved end-to-end latency win on otherwise identical model
requests. Catalog byte counts and the withdrawn `490 ms` multiplier cannot promote it.

## What was boofarama

The boofarama was not the AceJump analogy itself. It was the unpriced jump from analogy to authority:

- assuming a label that was easy to see was safe to write;
- assuming a checksum proved intent rather than consistency;
- assuming read cheapness made an extra read turn free;
- assuming copied bytes emitted faster than composed bytes;
- assuming bytes remained the priced unit across a carriage change;
- assuming a tool-local terminal flag could end a task whose last write was almost always native;
- assuming a supplied-decision win generalized to discovery-required work; and
- treating advisor answers that repeated the coordinator's brief as independent confirmation.

The measurements did their job. Every dangerous direct-label route was stopped before product
promotion. The error was in the story told between measurements.

## Artifact inventory

### Repository records carrying original proposals or direct judgments

- `935cc0d` — `docs/observations/2026-08-29-write-side-emission-and-read-side-encoding-study.md`:
  corpus ledger,
  `3.5237 ms/byte`, read annotations, URL-like `path#owner`, server-owned guards, read golf, and the
  initial ceremony headline.
- `ace26fe` — `docs/observations/2026-08-29-read-request-emission-audit.md`: independent 18.512%
  replay, fan-out, batching stop, and hint-versus-selection correction.
- `96baa2ad` —
  `docs/observations/2026-08-29-read-id-and-default-forms-buildability-audit.md`: closed all-or-none
  IDs and structurally implied `forms` design. This corrects the brief's `96aa2ad` typo.
- `93dd258` — `docs/observations/2026-08-29-emission-compression-option-portfolio.md`; `fbe65ef`
  and `52ed39c` — `docs/observations/2026-08-29-emission-compression-composition-screen.md` and
  `docs/observations/2026-08-29-wrong-index-ended-emission-composition.md`: the general option
  portfolio, composition screen, and wrong-index stop.
- `2d40036` — `docs/observations/2026-08-29-mnemonic-label-content-guard-screen.md`; `0119cbe` —
  `docs/observations/2026-08-29-name-derived-mnemonic-screen.md`; `dacf768` —
  `docs/observations/2026-08-29-declaration-first-subject-guard-screen.md`: the guarded-label,
  readable-label, and declaration-first screens.
- `d0c11a59` — `docs/observations/2026-08-29-edn-form-carriage-token-screen.md`. This is the named
  snapshot in the `experiment/emission-compression-screen` chain.
- `db937f5` on `screen/splice-edit-grammar-zero-model` —
  `docs/observations/2026-08-29-splice-edit-grammar-zero-model-screens.md`: splice screens and alias
  inversion.
- `7682abf` on `screen/ceremony-attribution` —
  `docs/observations/2026-08-29-ceremony-attribution-and-terminal-signal-screen.md`: re-derived wall,
  attribution, terminal-signal reach, and safe scoping.
- `dd36336` / `93d9918` on `bench/prefill-decode-ratio`: copy/compose, transcription, constrained
  decoding, and carriage generation-time controls in
  `bench/results/2026-08-29-prefill-decode-ratio/*/SUMMARY.md`,
  `bench/results/2026-08-29-carriage-json-vs-edn/*/SUMMARY.md`, and
  `docs/why-reading-is-cheap-and-writing-is-expensive.md`.
- The secondary chronicles read were
  `docs/observations/2026-08-29-captains-log-three-ways-to-name-the-wrong-thing.md` (`3d84110`),
  `docs/observations/2026-08-29-captains-log-the-ledger-had-two-sides.md` (`57873c3`),
  `docs/observations/2026-08-29-captains-log-six-designs-died-and-the-tool-was-barely-used.md`
  (`2d01e88`), `docs/observations/2026-08-29-captains-log-the-predictions-that-failed.md`
  (`cfcafdb`), `docs/observations/2026-08-29-captains-log-what-the-six-withdrawals-mean.md`
  (`b3ee1ee`), and `docs/observations/2026-08-29-measurements-and-how-to-repeat-them.md`
  (`ecef504`). Where a chronicle conflicts with a direct receipt, the receipt wins.

### Later evidence used to judge the old proposals

- `28ee81f4` — external-corpus write-shape census: owner-visible repetition 13.3%, with coverage
  limitation stated.
- `c55de22` — installed read-request normalization and its all-or-none ID boundary.
- `b445a8c` — prepared-request structural owner-token identity proof.
- `f6ac0b5` — discovery crossover lower bound and discovered-versus-supplied routing axis.
- `6277e06`, `ab5759e`, `5d28b13` — prepared-request routing death and twice-replicated recovery
  direction.

### `/tmp/brainfleet` records

All readable top-level `.md` and `.txt` files dated 2026-08-29 PT were searched for
`acejump|labels?|golf`. The literal search returned 32 files:

```text
adoption-brief.md                 adoption-experiments.md
briefing.md                       clj-brief.md
diff-brief.md                     doctrine-brief.md
expect-brief.md                   faster-doctrine.md
lid-task.md                       needle-brief.md
ratchets-task.md                  sol-adopt-exp.txt
sol-adoption.txt                  sol-answer.txt
sol-clj.txt                       sol-diff.txt
sol-doctrine.txt                  sol-expect.txt
sol-faster.txt                    sol-impl-A.txt
sol-impl-A2.txt                   sol-impl-B.txt
sol-lens-constraints.txt          sol-lens-metrology.txt
sol-lens-skeptic.txt              sol-lid.txt
sol-mayor-review.txt              sol-needle.txt
sol-ratchets.txt                  sol-readside.txt
sol-route-hint.txt                sol-topology.txt
```

The proposal-bearing packets are `briefing`/`sol-answer`, `diff-brief`/`sol-diff`,
`clj-brief`/`sol-clj`, `expect-brief`/`sol-expect`, and `needle-brief`/`sol-needle`. The adoption,
read-side, ratchet, doctrine, LID, mayor, and topology packets mostly restate, audit, or route those
claims. The implementation transcripts add no new label mechanism. Raw `sol-*.txt` files include
echoed prompts, repository search output, and repeated final answers; a hit in them is not an
independent observation. Files created on 2026-08-30 for this review/design (`acejump-*-brief.md`,
`sol-acejump-*.txt`) were read as task provenance but excluded from the 29 August source count.

## Final decision

Do not reopen “AceJump labels for mutation.” Keep labels cosmetic. Keep complete typed identity and
exact old-source guards in the write. Continue the prepared-request recovery product because it is
the legitimate descendant of read annotation: it piggybacks on an already-required exact read,
repeats full identity, carries no write authority, and has replicated recovery direction. Never
cite it as a routing lever.

Treat new write grammars as guilty until they prove that they compress repeated structure, clear a
whole-turn threshold on a material task class, preserve canonical subject/effect identity, and win
complete verified wall in the correct discovered-or-supplied stratum. The 29 August corpus already
paid for that law. There is no reason to buy it again.
