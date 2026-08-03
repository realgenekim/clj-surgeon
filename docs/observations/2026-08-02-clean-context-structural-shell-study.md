# Clean-Context Structural-Shell Study

Date: 2026-08-02

## Question

Can a fresh coding agent use clj-surgeon as its primary lens for finding,
reading, and changing Clojure forms with fewer commands and less irrelevant
source than line-oriented tools?

The test is behavioral, not aspirational: give an agent a realistic task with
no conversation history, observe its commands, turn every avoidable detour into
instructions or regression coverage, and rerun from clean context.

## Tasks and observations

| Task | First clean-context behavior | Defect exposed | Repaired route |
|---|---|---|---|
| Read a known top-level form | `:ls`, then `:show-form` | Skill required outline as a universal preflight | One `:show-form :form` |
| Read the form containing a known line | `:ls`, then `:show-form` | Same preflight bias | One `:show-form :line` |
| Recover from guessed `:get` | Read local remedy, then `:show-form` | None | Two calls; no help |
| “Cat” a selected form | One `:cat :form` | None after alias contract | Canonical result remains `:show-form` |
| Find the form responsible for a distinctive help phrase | Full 40-form `:ls`, then `:show-form` | No routing rule from lexical clue to structural object | `rg -n`, then `:show-form :line` |
| Find nested syntax without knowing its parent | Opened help, then omitted `:inside` experimentally | Docs implied an already optional argument was mandatory | One file-wide `:grep-form` |
| Find a call inside `#(...)` | File-wide structural search returned zero despite visible source | Anonymous-function body is represented as sibling nodes, not a standalone list node | Added as an acceptance case for the general sibling-span lens in issue #21 |
| Change one `case` result | Guessed a synthetic clause list; recovered to the contained expression | Meaningful clause is an adjacent sibling pair | Target an independently readable expression; track general sibling-span lens in issue #21 |
| Apply that edit | Chained plan, apply, and verify with `&&` | Review could not affect application | Standalone plan command, observed review, separate apply |

## Output cost evidence

Measured against the 896-line `core.clj` during the experiment:

| Output | Bytes |
|---|---:|
| Global help | 2,621 |
| Complete `:ls` outline | 5,611 |
| `rg -n` line locator | 54 |
| Selected `:show-form` result | 1,684 |
| Bare `:cat` local refusal | 270 |
| Historical `:get` refusal with executable remedy | 675 |

The selected form is necessary task payload. The full outline is not when a
distinctive phrase can supply a line in 54 bytes. Likewise, printing 2,621
bytes of global help for a missing selector would replace a 270-byte local
contract with a wall of unrelated operations.

## Design conclusions

1. Use lexical search as a cheap coordinate provider, then cross into a
   structural object immediately.
2. Make the shortest honest Unix vocabulary available as aliases while
   preserving stable canonical machine operations.
3. Do not require agents to independently guess a capability and then guess
   how to invoke it. Put the file-wide example first when file-wide is valid.
4. Treat plan review as a control boundary, not prose. Planning and application
   must be separate process invocations.
5. Do not encode special knowledge for every Clojure macro. Add general lenses
   over Clojure's actual representation. The proposed sibling-span primitive
   covers `case`, `cond`, bindings, maps, and sibling syntax inside anonymous
   function literals mechanically.
6. Prefer the smallest sufficient error over automatic global help. Include
   local required arguments and an executable remedy only when confidence is
   high.

This is the operational meaning of a one-shot feature in this repository: a
clean agent chooses the narrow successful route, every refusal is actionable,
mutation remains fail-closed, and any observed confusion becomes permanent
documentation or test coverage without weakening an existing assertion.

## Follow-up

- General sibling-span lens: [GitHub issue #21](https://github.com/realgenekim/clj-surgeon/issues/21)
- Feature and verification plan: [show-form.md](../plans/show-form.md)
- Narrative experiment record: [Captain's Log](2026-08-02-captains-log-the-file-became-a-structural-shell.md)
