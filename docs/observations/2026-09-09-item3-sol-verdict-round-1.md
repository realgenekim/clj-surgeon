NO-GO

# Sol fence review: preservation brief

- reviewed: sealed candidate `e8c36afbe00b98cb149f72c052740dcf869f9fe8`
- branch tip accepted by the brief: `1e00e17c8d956b81e6d803d0f2bc543e7171c8c0`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- review time: `2026-09-09 16:11:04Z`
- verification: the tool's own replay; an independently planted local-shadowing falsifier; a symlink confinement probe; tree-hash comparison; source inspection; preregistration arithmetic
- excluded as directed: `make test`

## Blocking findings

### PB-FENCE-001 — the owner canonicaliser certifies a changed binding as mechanically preserved

`bin/preservation-brief:172-221` canonicalises every bare token matching a unique base owner as that owner without establishing whether the token is a local, a `:refer`, syntax captured by a macro, or a Var reference. The limitation is disclosed at `bin/preservation-brief:1100`, but A2 nevertheless says every accepted reference still resolves to the same owner (`bin/preservation-brief:759-760`) and counts the body as mechanically preserved (`bin/preservation-brief:771-774`).

Falsifier, planted independently on the replay's real clean Cell C tree:

```clojure
;; base/clean candidate: local destructured binding
(form-edit-panel event editing edit-form)

;; planted candidate: the moved private Var, not the local binding
(form-edit-panel event editing cfp-scheduler-killer.views.portal/edit-form)
```

The source expressions resolve differently. Their canonical token streams compare equal because both `edit-form` tokens are rewritten to the old owner home. The checker still reported:

```text
moved=141
preserved_bodies=141
tiers={byte-identical 83, identical-modulo-requalification 58}
in_place_preserved=64
in_place_needs_review=0
```

Section B did not expose the mutation: `possible_unresolved` stayed at the clean row's value of 1 and named only the already-known bare destructuring symbol. This triggers the preregistered kill switch: a defect exists inside a body the checker declares preserved.

Required repair: make requalification proof resolution-aware, or conservatively refuse the preserved tier whenever an equivalence depends on a bare symbol that may be lexically bound, referred, or macro-captured. Permanently add this shadowing case to the replay. That repair changes `bin/`, which the ship-fix contract forbids a reviewer from patching, so it cannot be closed as `GO-WITH-FIX` here.

### PB-FENCE-002 — “reads only the two git trees” is false for symlink entries

The implementation extracts each tree, recursively follows `file-seq`, accepts `.isFile`, and `slurp`s the resulting path without resolving and checking it against the materialised-tree root (`bin/preservation-brief:71-76,89-97`).

Probe: a candidate tree added `src/leak.clj` as a symlink to `/var/tmp/forge/sol-proof-external.clj`; the external file defined `escaped-owner`. The brief reported the external definition as candidate evidence:

```text
scope_files=1
base_owners=0
cand_owners=1
new_owners=1
escaped-owner | def | escaped | src/leak.clj:3
```

Thus Section A can consume bytes absent from either git tree. Drop symlinks and require every walked file's real path to remain beneath the materialised root. The same entrance also has no pre-parse file/byte cap: `slurp` precedes all parsing bounds, contrary to the repository's fence contract for a new CLI surface.

### PB-FENCE-003 — B-only protected defects are not safe to consume as a reduced review entrance

The replay passed as implemented: all six planted rows changed a counter, with the wrong-namespace binding, dropped comment, and omitted owner caught in A; silent promotion and both load-order variants were caught only in B. However, the brief prints “Mechanically preserved bodies: 141 of 141” before a long A section. The load-order signals appear only starting at B4, and the machine summary has no overall unresolved/fail state. A reviewer can consume the preservation headline and skip the only signal.

Detected promotion, new forward-reference, require-order, and require-sort signals must be promoted to a top-of-brief hard-stop summary (and a machine-readable non-clear state). Calling them “decisions that remain” deep in B is not enough to ensure they cannot be skipped.

### PB-FENCE-004 — the preregistered co-primary is named but not honestly executable as written

Section 9 was written before any blind-review arm and correctly names per-class catch rate plus the strict rule that any class caught in C and missed in T rejects the bet. The allocation and arithmetic do not support that claimed analysis:

- The listed specimens are 8 clean and 8 defective, not the registered 9:7 ratio: `4 retained clean + 1 clean alias + 3 clean decoys = 8`; `5 protected + 1 alias defect + 2 defective decoys = 8`.
- Four reviewers seeing eight specimens each produce 32 observations, not the claimed 64.
- With one specimen per protected class and three arms C/T/N, the assignment does not specify the per-class denominators or guarantee each protected class appears in both C and T.

Freeze a corrected specimen ledger and assignment matrix, with per-class C/T denominators, before the first arm. The present text does not preregister an executable co-primary catch-rate comparison.

## Checks that passed

- Narrow code independence passed: none of the three `bin/` files requires `clj-surgeon.*`; the scanner only requires `clojure.string`; receipt data is read after the A/B derivations and rendered only in C. PB-FENCE-002 still defeats the broader two-tree independence claim.
- The replay is built from real retained trees, not synthetic replacements. Source and replay tree hashes matched exactly:

```text
d9205abc source/replay tree: 269dc8638eb697bd7df4aabfa64a4e3204fb705f
65ad613b source/replay tree: 1e8bbfc23069b2cef320ceddcd7849782824b715
```

- The unmodified replay completed successfully and reproduced all seven rows. Its original six defects all changed the advertised counters.

## Decision

Do not ship or use this candidate to shrink a reviewer's proof burden. PB-FENCE-001 is the prototype's own correctness kill switch, and PB-FENCE-002 invalidates its stated evidence boundary. Both require oracle changes under `bin/`, so this review leaves no ship-fix patch.
