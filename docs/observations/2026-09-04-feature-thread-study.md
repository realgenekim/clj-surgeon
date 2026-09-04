# Study: should Surgeon read and relate non-Clojure files? The cross-language feature thread

*Requested by Gene (dictated, relayed by the mayor 2340Z, received on the anvil seat 03:59Z 2026-09-04). Filed as inb-55e00e. Written 2026-09-04T04:05Z by Fable (the anvil seat); Sol's independent position appended when it lands. Design-phase only; nothing is built until Gene ratifies.*

## 0. The request, verbatim

Gene: "Look in the curtain call repo. I had asked for a change that required five searches, both in JavaScript and closure. I had asked why didn't you surgeon and had such an articulate answer is that its search spanned the need to search JavaScript enclosure. And I think this is actually an extraordinary use case for surgeon. It's like an ethnographic study of a problem that need to be solved, and I think surgeon should allow us to potentially search files outside of closure files. in other words, let's go to where the problem is versus what the tool is currently capable of fetch that chat interaction, the whole thing, including my original request … Capture that whole thing and send it to Anvil for it to analyze and study."

Provenance (mayor, verified against the rollout): repo social-media-writer, codex session 01a0678b-d807-7e42-ac73-db3bd41ca674, 2026-09-03T06:53:33Z, turns 8–16. The exchange that matters:

> [13] GENE: "qq: does surgeon cli :ls-tree help you?  if not, what would?"
> [14] ASSISTANT: "`qq:` No—`:ls-tree` doesn't help on this change. `rg` immediately found the four exact owners: menu item, JS command bridge, HTTP handler/route, and focused tests. The tool I'd want here is a cross-language "feature thread" query: given `formatDraft` or `/api/transform/format`, return the menu caller, JS function, route, handler, and tests in one compact receipt. `:ls-tree` is useful for namespace-level structural study, not this narrow JS↔Clojure path."

The five-leg receipt (menu caller, JS function, route, handler, tests) is the specification hiding in that sentence; "four" is what one seat happened to grep. The mayor is right not to correct Gene to four.

## 1. Same square or new?

Not new. It is square 3 ("questions grep answers wrong") generalised by one question. Grep answers each of the five leg questions correctly — the transcript says so: "rg immediately found the four exact owners." What grep does not answer is the sixth question: *is this the whole thread?* Three candidate things a tool could return that five greps cannot:

| candidate | what grep lacks | what tonight's evidence says |
|---|---|---|
| the JOIN (one receipt) | stitching | E6-Q/E6-Q2: this caller stitches three calls in its head and scores 54/54; a join it does anyway is a convenience, not a capability |
| the COMPLETENESS claim ("all five legs, or a named absence") | a claim, not a search | grep cannot make it; a receipt can. This is the only leg of the candidate that is a new object |
| SCALE (every thread touching X across the repo) | a relation, not a lookup | untested; square 2's shape (one intent, N owners) on the read side, and square 2 is the program's one win — bounded, but real |

So the finding the mayor suspected is right, and narrower than it sounds: the new object is the completeness receipt, and the square it belongs to was withdrawn twice tonight for this caller at the size of one question. It can come back only where the join is wide (many threads) or where completeness is the deliverable (a gate: "every route has a handler and a test").

## 2. The 2026-09-02 ruling

It survives. The ruling removed Surgeon as the default EDIT route; every measured win tonight was on the read side or the fan-out write. A cross-language read that changes nothing about how the agent edits is exactly what the ruling leaves open. It also inherits the ruling's lesson: adoption under free choice is 0/19 tonight, so if built it must be routed (a skill that runs it, or a gate that requires it), never offered.

## 3. The honest cost

Surgeon's power is a Clojure parser and a structural index. A JavaScript "function" is not a Clojure form. Two designs:

- **A JS parser.** Real (tree-sitter or an ES parser), heavy, a second language to keep correct, and the same class of refusal work the census and alias lanes spent nineteen rounds on tonight — for one leg of five. This is the trap: the moment the tool claims to *parse* JS it owes JS-grade correctness, and the program has no JS witnesses.
- **A relation over cheap evidence.** Identifier occurrence (a byte search, no claims), route-string literal match, file role by path convention (`menu*`, `*.js`, `routes`, `handlers`, `*_test`), and ONLY the Clojure leg parsed structurally (the handler form, the route-table entry as a form, the test var). The receipt never says "the JS function"; it says "an identifier occurrence in a file whose role is JS command bridge, at these lines." Cheaper, honest, and it names its own evidence.

What breaks the cheap relation, and must be typed absences rather than silent misses: aliased identifiers (`const fmt = formatDraft`), routes built from template strings or constants, generated code, a handler registered by a macro, a test that references the route only through a fixture. Each of these is a leg the relation cannot see, and the receipt must say "not found by <search>", never "none".

## 4. The refusal contract (the receipt shape)

```
feature-thread  subject=formatDraft  (also matched: /api/transform/format)
  menu-caller   FOUND   resources/public/js/menu.js:112   evidence=identifier
  js-function   FOUND   resources/public/js/editor.js:340  evidence=identifier(def)
  route         FOUND   src/smw/routes.clj:58              evidence=route-literal, form=(POST "/api/transform/format" …)
  handler       FOUND   src/smw/handlers.clj:210           evidence=form  handle-format-draft
  tests         ABSENT  searched: test/**/*_test.clj for "format-draft" | "/api/transform/format" | formatDraft — 0 hits
  status        INCOMPLETE (4 of 5)   next: name the test file, or confirm none exists
```

Rules: five named slots, always rendered; each slot is FOUND with evidence kind and location, or ABSENT with the exact search that was run; `status` is COMPLETE only when every slot is FOUND or explicitly declared empty by the caller; a four-of-five thread is always INCOMPLETE and says which leg; text ⊇ structured (every slot in the text block). A receipt that presented four legs as the thread would be tonight's receipt-blindness failure in a new coat — the mayor's phrase, and correct.

## 5. The cheaper thing

A repo-local script, `bin/feature-thread <identifier-or-route>`, in social-media-writer itself: five rg queries keyed on the repo's own conventions, printed as the receipt above, with named ABSENT slots. No parser, no server, no MCP, an evening of work, and it lives in the repo whose conventions it encodes (which is where a thread's file roles are actually known). A skill can tell any agent to run it and answer the sixth question. This captures the join and the completeness receipt — the two objects §1 found — for the one repo Gene works in. It does not capture scale (every thread touching X) or a cross-repo gate; those are the only reasons left to build a verb, and neither is asked for yet. Gene's tiebreak ("the constellation is never the product") picks the script.

## 6. The experiment that decides it in one evening

**E-THREAD**, on social-media-writer (real repo, real feature, the actual conversation as the task). Cells: N (native, rg + hand-stitched answer) ×3, S (native told to run `bin/feature-thread` first) ×3, no verb cell until a verb exists. Task: three threads chosen by hand — one plain (`formatDraft`), one with an aliased identifier, one with a template-built route — each with a frozen truth of five legs. Meters, load-immune: legs found (0–5) and FALSE-COMPLETE claims (the arm says "these are all" when a leg is missing); secondary emitted chars and tool calls; wall descriptive. Predictions to write before arm 1: N finds 5/5 on the plain thread (90%), ≤4/5 on the aliased and template threads (60%), and claims completeness anyway in ≥1 of those arms (50%); S finds the same legs and never claims completeness on a missing one (80%). Withdrawal: if N is 5/5 on all three threads and never false-completes, the study closes as "the skill is the product" and no verb is designed; if N false-completes in ≥2 of the 6 hard-thread arms and S does not, the completeness receipt has a measured value and a verb design round is earned (scale + gate), still no parser.

## 7. Committed recommendation for Gene

Do not build the verb yet; the evidence from tonight says it would lose to this caller on one thread, exactly as square 3 did twice. Build the cheap thing first — the repo-local feature-thread script with named absent legs — and run E-THREAD in one evening, meter completeness and false-complete claims, not wall. The number that changes my mind is a native arm that reports four legs as the whole thread on a hidden-leg case while the relation names the fifth: two of six and the verb earns a design round, as a relation over cheap evidence with a completeness receipt, never a JavaScript parser.
