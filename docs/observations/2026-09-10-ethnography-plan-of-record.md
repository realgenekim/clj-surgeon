# Plan of record: from programs to contracts (2026-09-10) — written 2026-09-10T20:09Z

Gene's ruling (verbatim, 2026-09-10): "I don't care how good you are at Python; come on, hand-rolling these Python regex changes is insanity to me. Even if we're three times slower, we're still in seconds. It's fine." "I do think that the CLI needs a native insert operation." "Even though we're slower, I think it's better; it's less dangerous." "Ultimately, all of this is in service of creating the perfect tools for LLMs to write great clojure code."

## Evidence
- Spike: 2026-09-10-python-vs-surgeon-form-edits.md (E4: 19 routes broken with gates green; Surgeon refused expected 31 found 2).
- Census: 2026-09-10-ethnography-program-edits.md (1,570 sessions on Anvil: 6,124 program edits / 2,132 editor / 220 Surgeon; 107 confirmed broken files as a floor; program share is a MODEL trait: sol 3%, sonnet 17%, astra 71%, opus 90%, fable 100%; require_change 0 calls in the corpus; 1,876 require/alias edits went through sed although the verb exists).
- Consult: 2026-09-10-astra-ethnography.md.

## Settled
1. Wall is a diagnostic column, never the verdict. The meter for a route is: independent correctness (gate-green defects counted separately from gate-detected), complete cost through independent acceptance, refusals split warranted/unwarranted with recovery cost, preservation (unintended sites), and evidence coverage. (Astra Q2; Fable agrees; the 09-10 wall table is withdrawn as a verdict.)
2. "Slower is better" is not the sentence. The sentence is: a string has no contract; a form edit's contract is a count OF FORMS; a relevant enforced contract is worth a seconds-scale premium. (Astra Q3, Gene, Fable.)
3. The gap is 20% verbs, 80% adoption. Missing verbs (insert form, delete form, repo-wide rename, edit .edn, create ns) explain ~1,240 of 6,124 program edits; the rest had a verb nobody reached for. (Census; overrides Astra's "premature census" worry, which the 12-minute census settled.)
4. False refusals are the adoption killer. The one recorded rationale in 6,124 edits is Fable bypassing Surgeon after a refusal it judged false. Every refusal class gets a warranted/unwarranted count. (Astra Q6 "refusal romanticism"; census Q4.)
5. Guards in programs do not protect: 54% carry an assert, 19% a count guard, and they still break files 3.3x more often per verified edit than editor edits. A count of strings is not a count of forms. (Census.)

## Next 4 hours (Astra's order, accepted)
1. Reproduce E4 in an isolated fixture with an INDEPENDENT oracle (intended alias references + preserved routes), not the repo's gates.
2. Three matched arms on the same intent: the Python program, a reviewed native patch, Surgeon. Separate mistaken-request robustness from correct-request completion. Report on the settled meter (item 1).
3. Write the insertion contract: before/after a top-level form and at a boundary inside a named deftest body; unique anchors; stale-source refusal; comment handling; multi-form insertion.
4. The harness one-liner: Claude sessions on this seat carry "make file changes with sed, heredocs, or short scripts rather than Read/Edit/Write"; add the Clojure exception (structural editor or Surgeon for .clj/.cljc/.cljs/.edn). Fable is the 100% seat.

## Next 40 hours (Astra's allocation, accepted)
| h | deliverable |
|---:|---|
| 8 | cross-caller mining audit (skiff via mayor inb-9be58c), double-coded subset, evidence ledger |
| 6 | reviewed insertion + request-envelope contract (ordering, snapshot check, preview, mutation status) |
| 14 | narrow insertion implementation reusing the existing edit engines; CLI gains the op |
| 8 | matched acceptance trials incl. ambiguous anchors, stale inputs, strings and comments |
| 4 | executable examples, caller pilot, adoption + recovery review |

## Meter of success
Program share per intent class per model, re-mined monthly from the same roots; target is Sol's 3%. Plus warranted/unwarranted refusal counts. "Python disappeared" alone is not success (Astra).
