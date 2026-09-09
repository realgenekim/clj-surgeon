NO-GO

# Sol fence review, round 2: preservation brief

- reviewed: sealed candidate `a11f465504bf887ae89a483b3f200dfa10520133`
- branch tip accepted by the brief: `8751ed9e4d4cca169b98d748cdba8e92e28e87ba`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- review time: `2026-09-09 16:43:01Z`
- verification: repaired replay; the two round-1 probes; an independently planted external-library-macro shadowing falsifier; tree-hash comparison; source inspection; preregistration arithmetic
- excluded as directed: `make test`

## Blocking finding

### PB-FENCE-005 — an external binding macro still lets A certify a changed binding

The scope walker freezes a macro only when its name is defined in one of the two scanned trees or its
unrecognised head begins with `with-`/`def*` (`bin/preservation-brief:245-255`). Every other unknown list
head falls through to a recursive walk that establishes no binding (`bin/preservation-brief:339`). The bare
token can then still be canonicalised to the unique base owner (`bin/preservation-brief:371-379`). This is
not conservative for binding macros supplied by libraries.

I made corresponding copies of the real Cell C base and candidate trees. In the base `committee-page` I
planted:

```clojure
(compojure.core/GET "/probe/:header" [header] header)
```

In the candidate I changed only the bound use to the moved Var:

```clojure
(compojure.core/GET "/probe/:header" [header] organizer-layout/header)
```

`compojure.core/GET` is an external macro and is defined in neither tree. The two expressions resolve
differently: the base use is the route binding, while the candidate use is the moved `header` Var. The
brief nevertheless left `committee-page` at `:identical-modulo-requalification`, reported the same
`preserved_bodies=136` and tier counts as the unmodified repaired row, and listed only the three existing
`with-*` forms under `unmodelled_macros`; `compojure.core/GET` was absent.

The B3 heuristic did increase `possible_unresolved` from 1 to 2 and therefore kept `clear:false`. That does
not repair A's false statement that this body is mechanically preserved and that each canonicalised
reference still resolves to the same owner. Section 9.8's explicit kill switch is exactly “a defect inside
a body the checker declared preserved,” so the hard stop elsewhere cannot excuse this certification.

Required repair: external macro calls must not contribute canonicalised bare-token equivalence unless the
scanner has positive knowledge that their argument grammar introduces no bindings. Conservatively freezing
all unmodelled macro calls requires a dependable way to distinguish them from ordinary function calls; the
new falsifier must become a permanent replay row. This changes `bin/`, which the ship-fix contract forbids a
reviewer from patching, so this review cannot close as `GO-WITH-FIX`.

## Round-1 repairs verified

- **PB-FENCE-001, named replay row:** `local-shadow` moved one body from preserved to changed
  (`136 -> 135`) and the hard stop exposed six changed relocated bodies.
- **PB-FENCE-002:** `symlink-escape` reported one refused entry; `escaped-owner` appeared in neither its
  Markdown nor JSON output. Source inspection confirms tree reads use `git ls-tree` and bounded regular
  blobs via `git cat-file --batch`; no extracted tree is walked.
- **PB-FENCE-003:** every real replay specimen emitted `clear:false`; the promotion, form-order and
  require-order signals appeared in the hard-stop block above A, in machine `hard_stops`, and the brief
  exited 3.
- **PB-FENCE-004:** section 9 now closes arithmetically: 5 clean + 12 protected + 4 decoys = 21 specimens;
  21 x 3 arms = 63 observations; 9 reviewers x 7 = 63; each protected class has two observations per arm.
  The co-primary is honestly strict dominance rather than a rate, and independent planting plus a sealed
  answer key is required before the first arm.
- **Independence and replay provenance:** none of the three candidate files requires `clj-surgeon.*`.
  Receipt reading begins only after the A/B derivations and is consumed in C. The replay archives the real
  retained `d9205abc` and `65ad613b` trees; their source/replay tree hashes match byte-for-byte:
  `269dc8638eb697bd7df4aabfa64a4e3204fb705f` and
  `1e8bbfc23069b2cef320ceddcd7849782824b715`.
- **Delta confinement:** `1e00e17c..8751ed9e` changes only `bin/preservation-brief`,
  `bin/preservation-replay`, and `bin/preservation_scan.clj`; `git diff --check` is clean.

## Decision

Do not ship this candidate as a proof-burden reducer. The four round-1 findings are materially repaired,
but the requested unseen binding-macro probe reproduces PB-FENCE-001's core false-certification class at an
external macro boundary and triggers the preregistered kill switch.


> END RECEIPT (fence-run): worktree HEAD at review exit = a11f465504bf887ae89a483b3f200dfa10520133 = fenced sha.
