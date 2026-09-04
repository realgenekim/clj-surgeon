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


## UNPARKED (04:15Z) — Gene, verbatim: "It was intended for you. Mayor messgage for you. Go go to. Tell mayor too". The hold below is superseded; the study proceeds (script + E-THREAD, no verb).

## PARKED, superseded (04:13Z) — mayor 2350Z: HOLD; Gene replied "no send to anvil!" and the mayor cannot tell which reading he means. No builders were spent (the study is prose + one $0 Sol poll). Kept as notes; nothing further until the mayor confirms. The mayor's bead is inb-cf3866; this seat's duplicate capture inb-55e00e points at it.

## Appendix — Sol's independent position, verbatim (04:13Z)

# Independent position: cross-language feature threads

1. **Same square: square 3, with a relational output.** The new object is not five better searches; it is a declared JOIN from five independently found legs into one normalized thread, with provenance, ambiguity, and absence carried per leg. Five greps can supply the rows but do not themselves assert that those rows belong to one feature; the tool can make that join explicit. It may claim COMPLETENESS only relative to a frozen search root, five declared slot predicates, and untruncated searches—never semantic completeness across JavaScript—and “every thread touching X” is a separate scale claim that this proposal has not earned. E6-Q and E6-Q2 predict that this caller will perform the bounded join just as well by hand: native was exact on 18/18 big-file probes with three calls per run by windowing over `rg` hits, so file size and per-leg lookup do not create a product gap; those cohorts say nothing positive about semantic completeness or repo-wide thread enumeration.

2. **Yes, but narrowly:** a read-only relation survives the 2026-09-02 ruling because it leaves native editing untouched, although surviving the ruling is permission to measure it, not evidence that it wins.

3. **Use a RELATION over cheap, named evidence; do not add a JavaScript parser.** The inputs are identifier occurrences, exact route-string literals, repo-local role conventions, and a structurally parsed Clojure handler; each edge is labelled by which evidence supports it. A real JS parser is the scope trap: it begins a second language kernel and still cannot prove framework-level routing, generated wiring, or runtime registration. The cheap relation must downgrade to `ambiguous` or `partial` for aliased/renamed imports, computed or dynamic routes, template strings with interpolation, re-exports, indirect dispatch tables, generated code, macro/build-time code, and tests that reach the behavior without repeating either seed. The worse trap would be calling that cheap relation semantically complete; its honest maximum is **convention-complete over an enumerated search**.

4. **Refusal/receipt contract.** The public text must contain every structured leaf except an explicit frozen exclusion set—the night's text ⊇ structured rule—and a summary may never hide a partial result. This is the ten-line shape:

```text
receipt: feature-thread/v1
query: {seed, workspace_root, include_globs, exclude_globs, convention_version}
menu_caller: {state: evidence|absent|ambiguous, search_id: menu, hits: [file:line+excerpt_hash]}
js_function: {state: evidence|absent|ambiguous, search_id: js, hits: [file:line+excerpt_hash]}
http_route: {state: evidence|absent|ambiguous, search_id: route, hits: [file:line+excerpt_hash]}
clojure_handler: {state: evidence|absent|ambiguous, search_id: clj, hits: [file:line+form+source_hash]}
tests: {state: evidence|absent|ambiguous, search_id: tests, hits: [file:line+excerpt_hash]}
searches: {menu: exact_query, js: exact_query, route: exact_query, clj: exact_query, tests: exact_query, all_untruncated: bool}
complete: true iff all five states=evidence, all searches ran untruncated, and unresolved_candidates=[]
status: complete|partial|ambiguous; missing: [slot...]; unresolved_candidates: [...]; next_call: executable_search_or_null
```

A four-of-five thread is `complete:false`, `status:partial`, and names the missing slot plus its executed zero-hit search; it is never rendered as “thread found.” Even `complete:true` means only that the five-slot convention was satisfied inside the declared scope.

5. **The repo-local `bin/feature-thread <id>` script captures most of the possible value, but only as an experimental instrument.** Five fixed `rg` queries, a stable stitched printout, hashes, truncation detection, and typed zero-hit rows cost tens of lines and no parser, server surface, or new language ownership; a skill merely reminds an agent to do what E6-Q2 shows it already does, while shipping nothing is preferable to enshrining heuristics. Gene's tiebreak should keep the script repo-local and out of Surgeon: this is exactly a constellation of searches, and “the constellation is never the product” forbids promoting it unless measurement proves the joined receipt removes a caller decision.

6. **One-evening experiment: FT-SMW, in `social-media-writer` pinned and attested before arm 1.** Use three interleaved cells: **N**, unrestricted native search with no helper; **S**, mandated `bin/feature-thread`; **F**, the same helper available and described but optional. Run **n=3 per cell (9 isolated sessions)**; each session answers two counterbalanced fixtures: the real `formatDraft` ↔ `/api/transform/format` feature and a frozen real-repo-derived four-of-five variant with one leg removed, so false completeness is observable. The hard gate is exact agreement with a frozen human-validated five-slot oracle, correct `partial` on the ablation, valid file/line hashes, and zero omitted/extra owners; the load-immune primary meter is watcher-counted model returns from prompt to final receipt, with source-tool calls, bytes returned, duplicate searches, and F adoption as secondaries—no wall claim. Prediction: N **3/3 exact** on both fixtures, median **2 source calls and 3 returns**; S **3/3 exact**, **1 source call and 3 returns** (zero returns removed); F adoption **0/3**, matching the 0/19 free-choice prior, with F still 3/3 exact natively. Withdraw the product if N is 3/3 exact and S removes fewer than one median watcher-counted return, if F adopts in at most 1/3, or if either helper arm emits even one false `complete`; advance only if S removes at least one return in at least 2/3 matched sessions, F adopts in at least 2/3 without duplicate `rg`, and all 18 receipts pass the oracle.

7. **Do not build cross-language feature threads into clj-surgeon.** Build the disposable SMW script only to run FT-SMW, then keep it repo-local if humans like it; neither a JS parser nor a generic MCP relation is warranted by a caller that already joined the real feature immediately with `rg`. The number that changes my mind is **one full watcher-counted model return removed in at least 2/3 matched sessions**, with optional adoption at least 2/3 and zero false-complete receipts.

## 8. MEASURED (E-THREAD, 05:1xZ 2026-09-04, doc 2026-09-04-ethread-cohort.md) — the study closes: do not build the verb

30 arms, five threads (T1 plain / T2 aliased JS identifier / T3 template-assembled route on social-media-writer; T4 JS+CLJ and T5 JS+CLJ+Prolog mined from marvin-voice-remote commits), N (native) ×3 and S (told to run the script first) ×3 per thread. Every cell: 5.00 mean legs, 3/3 five-of-five, 0 FALSE_COMPLETE. The pre-registered withdrawal fired on its first branch: native 5/5 on all threads with zero false completes → "the skill is the product", no verb designed, no design round earned. The two predictions the verb depended on (native ≤4/5 on hidden-leg threads; native false-completes) missed 0 of 9 and 0.

Why native won: T2 — followed `const formatDraft = runDraftFormatter` into another file, 3/3 (the hop the script had to be taught); T3 — reasoned alias → constant → `(paths/api "transform" "format")` with no literal route in the tree; T5 — two legs invisible to any search on the identifier or route, and all three native arms found the intent-registry row by READING the `// INTENT: …` comment two lines above the function, which a search script cannot do and says so.

The finding that outranks the cohort: the first grading reported 8 false-completes — all eight were the frozen ORACLE's error (a slot with several real witnesses frozen to one, twice in the direction that would have earned the verb on a broken meter). "A frozen oracle that admits one right answer per slot measures itself, not its subject." 12 arms voided and re-run when constructed fixtures turned out to live only in scratch working trees (git clone carries commits, not dirt); the runner now refuses any arm whose clone lacks the thread's hidden-leg token.

Kept: on social-media-writer the receipt halved the search work (7.4 → 3.3 tool calls; 66 → 33 s) at identical quality; on the dictation app it bought nothing — keep the repo-local script where its config is tuned. Not tested, left open honestly: repo-wide SCALE (every thread touching X) and a GATE (every route has a handler and a test). Secondary for Gene's Prolog question: 0 of 6 T5 arms named the Prolog oracle in either cell — if agents should find `.pl` oracles, the lever is a link from code to oracle (the INTENT comment worked; a search tool did not).
