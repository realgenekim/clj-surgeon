# O2 round seven — a leaf is carried by its own pointer line, and by nothing else

*Written 2026-09-04 13:07 UTC on the anvil seat, branch `bridge/study-ops-mcp`.*

Round six was NO-GO on two blocking findings, both instances of one mistake:
carriage was decided by looking for a leaf's characters ANYWHERE in the text.
Three coincidences resolved wrongly, and two of them made the rendering's own
DECLARATION disagree with an audit of the text it published.

## What was wrong, and what it is now

| coincidence | round six | round seven |
|---|---|---|
| a leaf whose value equals its POINTER | declared dropped, then found "carried" in the `dropped:` line naming it | the `dropped:` line is an address; the leaf is declared and audited alike |
| a value inside a longer decoy | `3 of 3 rendered` with no `target` line anywhere | rendered on its own pointer line, or declared dropped |
| one value at two pointers | `3 of 3` with `alpha` once and `beta` nowhere | two lines, each independently removable |

On the reviewer's public `name` rung, reproduced deterministically over twenty
pointer-valued leaves: **declared 29 / audited 25 before, 29 / 29 after.**

The repair is one predicate. `leaf-lines` emits the WHOLE LINES a leaf is
rendered as — `  <pointer>: <value>`, `  <pointer>=<value>`, or the pointer's
own indented block — and `leaf-carried?` asks whether the text's lines contain
them. The renderer's declaration and `uncarried-leaves` read that one rule, so
"this entry printed" and "the text carries this leaf" are the same fact rather
than two computations that have to agree. `carrier-indices` is gone: nothing
can carry another entry, so the dropped set is the tail itself.

## Two latent pointer defects the fix exposed

The moment carriage stopped accepting characters wherever they appeared, two
places that spelled a keyword with `name` became visible. A wire receipt key
`:src/demo.clj` — `file_hashes` keyed by a path, read back from JSON — audited
as the pointer `file_hashes.demo.clj` against a text printing
`file_hashes.src/demo.clj`. `json-key` and `leaf-label` now spell the complete
keyword. The HTTP wire witness was the one that caught it.

## What carriage costs — small reads on one fixed three-form fixture

Complete published pairs, in bytes:

| mode | `515e8109` (pre-carriage) | `dafc7f37` (round six) | this tip | round six -> here |
|---|---|---|---|---|
| outline | 1,769 | 2,962 | 2,984 | +22 (+0.7%) |
| deps | 1,592 | 2,376 | 2,398 | +22 (+0.9%) |
| topo | 1,423 | 2,020 | 2,042 | +22 (+1.1%) |
| ls-tree | 998 | 1,162 | 1,424 | +262 (+22.5%) |

The carriage RULE costs about 1% on a file read and 23% on `ls-tree`, on top of
the 16-59% the collidable-label rule cost in round five. `ls-tree` pays more
because its structural rows spell paths, namespaces, form names and hashes, and
under round six every one of those discharged its own receipt leaf by
coincidence. On a twenty-five file toy tree `format=text` went from 4,334 to
8,796 characters against a 4,376-character receipt: the text now carries the
rows AND the receipt.

That breaks MCP-OP-STUDY-037's 8 KB TEXT pass-line, and the intent says so
rather than the witness quietly relaxing. A fixed text ceiling is the rendering
constant MCP-OP-STUDY-044 already forbids as an allowance; the bound that is
real is the public output budget, and a 12 KB ratchet keeps the growth
witnessed.

## Sabotage receipts

Each witness was re-run against a `git archive` copy of the fixed tip with the
defect put back:

| sabotage | witnesses that went red |
|---|---|
| substring carriage restored in `leaf-carried?` | the pointer plant (1 assertion), the `name` rung (13) |
| + round six's cross-entry carriage in `fact-block` | the decoy plant (7), one value at two pointers (12) |
| a top-level receipt key spelled `dropped` | the residual witness (9) |
| `json-key` spelling a keyword with `name` | the HTTP wire witness (1) |

The first sabotage pass found a weak witness rather than a strong one: the decoy
plant stayed green because which budget renders the decoy and not the target
moves whenever the declaration changes size. Both plants now sweep the whole
band rather than sampling three budgets.

## Composition onto MEM-003 (`432268cf`, trunk-merged)

`git merge-tree --write-tree HEAD origin/bridge/integration-2026-09-03-mem003`
reports **ten** conflicts, not the seven round six saw, because this branch has
since absorbed the trunk (q5z's `alias_migration` landing and the temp-dir
hygiene gate) and MEM-003 has not. Three of the ten are MEM-003 against the
TRUNK and have nothing to do with O2 — the cheapest order is for MEM-003 to
absorb the trunk first, which should leave the seven.

| file | which side absorbs |
|---|---|
| `src/clj_surgeon/core.clj` | the TRUNK (q5z rewrote it); O2 carries no change here it did not merge |
| `src/clj_surgeon/mcp_server.clj` | the TRUNK |
| `src/clj_surgeon/mcp_tool.clj` | the TRUNK (the `alias_migration` verb) |
| `test/clj_surgeon/mcp_alias_migration_test.clj` | the TRUNK |
| `src/clj_surgeon/mcp_inspect.clj` | THIS TIP (1,346 lines of carriage and budget work against MEM-003's one-line touch); re-apply that line |
| `src/clj_surgeon/mcp_inspect_tool.clj` | THIS TIP; layer MEM-003's measured-domain fields onto the fitted result |
| `src/clj_surgeon/mcp_operation.clj` | MEM-003 for the nested `measured` wire shape; then re-apply this tip's construction-stamped envelope identity, which already reads either shape |
| `test/clj_surgeon/core_discovery_test.clj` | UNION — both sides add witnesses |
| `test/clj_surgeon/mcp_inspect_tool_test.clj` | UNION — independent witnesses, neither side deletes the other |
| `test/run_all.clj` | UNION the namespace registry |

## Residual, named rather than rediscovered

A fact line is `  <pointer><separator><spelling>`; the declaration lines are
`  receipt facts · …` and `  dropped: …`. The one way a declaration could still
be mistaken for a fact's own line is a TOP-LEVEL receipt key spelled `dropped`
or `receipt facts` — nested pointers always carry a `.` or a `[`, so they
cannot collide. Every top-level key is constructed inside
`clj-surgeon.mcp-inspect`, and a witness now holds that true across every
published operation and a refusal.

The widened decoy-substitution and removal audits are green but are NOT the
ratchet for structural carriage: removing a leaf also removes the structural row
that renders it, so the text changes either way. The plants and the
declared-equals-audited equalities are what hold that line.
