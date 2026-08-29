# Emission Compression Composition Screen

Date: 2026-08-29

Lane: SURGEON2, pure oracle only

Base receipt: 93dd258b647aad831f7aef69ad7c7358797cfd4a

Product authority: SURGEON1

## Result

Two pairs made only from individually sub-20% shapes clear the 20% compact
argument gate:

1. file_index + replacement_groups: 6,409 to 4,892 characters,
   saving 1,517 (23.67%);
2. file_groups + replacement_groups: 6,409 to 4,894 characters,
   saving 1,515 (23.64%).

The first pair is the launch candidate. The second retains the earlier
file_groups correctness loss and is not eligible. No model cohort was
launched.

The launch candidate is not a free win. replacement_groups alone saves 1,189
characters. The file table adds only 328 new saved characters, which projects
to 1.930 seconds under the descriptive emission slope. It crosses the payload
threshold, but it also introduces numeric cross-references between one file
table, grouped sites, and retained edits. Predicted payload is not predicted
correctness.

## Oracle

The screen uses the same frozen 51-edit request:

- 6,409 compact JSON characters;
- 33 edit rows and 37 declared matches;
- nine files;
- 14 owner deletions;
- exact future defined by owner-aware-symbol-migration/oracle-request.

Seven already-ranked pure shapes are composed:

~~~text
O  omit matches=1
I  file index
F  file groups
G  replacement groups
R  closed relations
Q  closed relations plus require delta
T  positional tuples
~~~

The composer uses a deterministic set of operators. Both orderings of each
pair produce the identical compact shape and canonical edit multiset. All 42
ordered pairs round-trip to the same 33 rows, 37 matches, nine files,
workspace root, and owner deletions.

File indexing applies only to edit carriers. It does not silently expand its
authority to delete_owners.

## All ordered pairs

The matrix is symmetric because every forward and reverse pair was asserted
byte-equal. Each off-diagonal cell therefore covers both ordered pairs.
Values are combined compact-character reduction. MISS means below 20%.

| first / second | O | I | F | G | R | Q | T |
|---|---:|---:|---:|---:|---:|---:|---:|
| O | — | 13.04% MISS | 18.16% MISS | 18.55% MISS | 22.80% | 42.96% | 21.47% |
| I | 13.04% MISS | — | 16.88% MISS | 23.67% | 25.17% | 45.33% | 31.99% |
| F | 18.16% MISS | 16.88% MISS | — | 23.64% | 22.52% | 42.67% | 27.57% |
| G | 18.55% MISS | 23.67% | 23.64% | — | 22.80% | 42.96% | 27.38% |
| R | 22.80% | 25.17% | 22.52% | 22.80% | — | 42.96% | 23.12% |
| Q | 42.96% | 45.33% | 42.67% | 42.96% | 42.96% | — | 43.28% |
| T | 21.47% | 31.99% | 27.57% | 27.38% | 23.12% | 43.28% | — |

There are 34 passing ordered pairs and eight misses. The eight misses are the
two directions of O+I, O+F, O+G, and I+F. They remain in the result.

Most passing pairs do not establish composition. R, Q, or T already clears
20% alone. Only I+G and F+G cross the gate while both individual members remain
below it.

## Overlap and interference

For each unique pair:

~~~text
overlap = saved(left) + saved(right) - saved(combined)
increment = saved(combined) - max(saved(left), saved(right))
~~~

| Pair | Saved | Overlap | Increment over best member | Class |
|---|---:|---:|---:|---|
| O+I | 836 | 360 | 0 | sub-additive; I already omits defaults |
| O+F | 1,164 | 360 | 0 | sub-additive; F already omits defaults |
| O+G | 1,189 | 360 | 0 | sub-additive; G already omits defaults |
| O+R | 1,461 | 360 | 0 | sub-additive |
| O+Q | 2,753 | 360 | 0 | sub-additive |
| O+T | 1,376 | 360 | 0 | sub-additive |
| I+F | 1,082 | 918 | -82 | interfering |
| I+G | 1,517 | 508 | +328 | sub-additive; earned candidate |
| I+R | 1,613 | 684 | +152 | sub-additive |
| I+Q | 2,905 | 684 | +152 | sub-additive |
| I+T | 2,050 | 162 | +674 | sub-additive |
| F+G | 1,515 | 838 | +326 | sub-additive; retained F loss |
| F+R | 1,443 | 1,182 | -18 | interfering |
| F+Q | 2,735 | 1,182 | -18 | interfering |
| F+T | 1,767 | 773 | +391 | sub-additive |
| G+R | 1,461 | 1,189 | 0 | R consumes every G-eligible row |
| G+Q | 2,753 | 1,189 | 0 | Q consumes every G-eligible row |
| G+T | 1,755 | 810 | +379 | sub-additive, high legibility risk |
| R+Q | 2,753 | 1,461 | 0 | Q supersedes R |
| R+T | 1,482 | 1,355 | +21 | almost complete overlap |
| Q+T | 2,774 | 1,355 | +21 | almost complete overlap |

No pair is additive. Every pair overlaps, and three unique pairs interfere.
Adding individual percentages would overstate every result.

## Best triples

The absolute smallest pure shape is:

~~~text
I + Q + T
6409 to 3477
saved 2932 (45.75%)
projected emission saving 17.261 s
~~~

This is not a product candidate. Q is an older experimental representation;
the current relation facade is already smaller than this triple. Adding numeric
indexes plus positional tuples increases ambiguity for only 179 characters
beyond Q.

The best triple that does not reuse either relation shape is:

~~~text
I + G + T
6409 to 4200
saved 2209 (34.47%)
projected emission saving 13.004 s
~~~

It is exact but high risk. A caller must coordinate a file table, group sites,
retained positional rows, tuple arity, and default omission. This compounds the
legibility failure mode that invalidated earlier cohorts. Do not launch it
before the simpler I+G pair.

## Pure compiler boundary for I+G

~~~text
request
  files: [path0 path1 ...]
  replacement_groups:
    - from / to / matches
    - sites: [{file_index, forms}]
  edits:
    - unique edit with file_index
        |
        v
Phase A1: validate and resolve file indexes
  - files is ordered, unique, nonempty strings
  - every index is an in-range nonnegative integer
  - no path guessing, normalization authority, or fuzzy match
        |
        v
Phase A2: expand replacement groups
  - closed keys and types
  - at least two sites per group
  - no duplicate decision, site, or collision with edits
        |
        v
existing canonical validation and transaction
~~~

The file table and every index remain caller-supplied authority. Phase A is
pure and source-blind. Source capture, path policy, stale guards, atomic
mutation, rollback, and exact verification stay in the existing canonical
engine.

Joint schema risk is medium-high:

- an index is less self-describing than a path;
- one wrong but in-range index can name the wrong file;
- the model must reuse the table consistently in two branches;
- a validation refusal can erase the predicted emission gain.

Required permanent falsifiers before product work:

1. duplicate or empty file-table entry;
2. negative, noninteger, or out-of-range index;
3. index in a group site and in an ordinary edit;
4. missing file table;
5. duplicate/colliding group site;
6. mixed invalid member refuses the complete request;
7. expansion is exactly 33 rows, 37 matches, nine files, and the frozen future;
8. stale hash and exact-verifier failure preserve rollback law.

## Frozen model protocol

No launch is authorized by this receipt. If forge authorizes it, run on Anvil
dev-a, not Skiff.

Use one exact candidate, catalog, model/effort, task, scorer generation, and
kernel. Both grammars are present in the same tool surface.

~~~text
Block 1: F C C F
Block 2: C F F C

F = canonical flat request
C = file_index + replacement_groups
~~~

Each row gets a fresh isolated workspace and process identity. No retry is
allowed. Record every attempt.

Admission gates for every row:

- first actionable item is exactly one apply_clojure_changes call;
- zero inspect, shell, file, refusal, fallback, or recovery action before it;
- route_adherent is reported separately from semantic correctness;
- pure lowering yields 51 edits, nine files, and the exact future;
- one atomic transaction and exact verifier complete;
- compact argument characters, output tokens, T_emit, server wall,
  result-to-final wall, and complete verified wall have bounded provenance.

Promotion gates:

- C is 4/4 correct, first-call, and route-adherent;
- F is 4/4 correct and first-call;
- C emits at least 20% fewer compact argument characters in both blocks and
  pooled;
- C lowers T_emit in both blocks and by at least 20% pooled;
- C lowers complete verified wall in both blocks and by at least 20% pooled;
- no loss, refusal, incomplete row, or position effect is removed;
- a prediction failure remains a published result.

The pure oracle predicts 23.67%, not correctness. Any schema refusal or wrong
index is a NO-GO even if a retry later reaches the exact future.

## Verification

New experiment-only files:

- dev/experiments/request_shape_composition_screen.clj
- dev/experiments/request_shape_composition_screen_test.clj

Bounded standalone nREPL, PID 55192, CWD
/Users/genekim/src.local/clj-surgeon, -Xmx512m:

~~~text
3 tests
57 assertions
0 failures
0 errors
~~~

No model token, Anvil worker, product file, install, reload, shared port, or
pre-existing process was changed.
