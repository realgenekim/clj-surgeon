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

## 9. Next step, on Gene's reframe (tool calls are the meter) — Fable's position, written before the fleet's answers (05:26Z)

Gene: "I suggest we build it, and see if any agents use it … bring back the forms? … reading is fast, but don't want to swamp context window … if we can save tool calls, we rack up gains."

**The call sequence today, for the exact SMW request** (from the transcript and the E-THREAD N arms, ~7.4 calls): one or two searches for the identifier and the route; a read of the menu file; a read of the JS bridge; a read of the route table and handler; a read of the sibling command (Format Draft, the thing the new command must mirror); a read of the tests; then the writes. With the current receipt (~3.3 calls): the receipt, then the reads of the bodies it pointed at, then the writes. **The remaining calls are reads of the forms the receipt located.** So the next receipt carries the forms.

**Shape, ten lines:**
```
feature_thread subject=formatDraft also=/api/transform/format budget=12288
  menu-caller  FOUND js/menu.js:108-118       sha=…  body: <11 lines>
  js-function  FOUND js/editor.js:340-362     sha=…  body: <23 lines>
  route        FOUND routes.clj:58            sha=…  form: (POST "/api/transform/format" … handle-format-draft)
  handler      FOUND handlers.clj:210-241     sha=…  form: (defn handle-format-draft …)   ; parsed, exact
  tests        FOUND test/…_test.clj:77-112   sha=…  form: (deftest format-draft-… )
  sibling      format-draft → the same five legs, bodies ELIDED to line ranges (budget)
  rules        editor-path=durable,conflict-aware  selection=required-for-Dequote/Format  intent=SMW-EDIT-00x
  status       COMPLETE (5/5)   bytes=9,812/12,288   elided: sibling bodies (ranges given)
```
Per leg: location, content hash (so the later edit can assert its pre-image), the body. Clojure legs are parsed forms with exact ranges (this is where Surgeon's parser earns the call). JS legs: a brace-matched body from the definition line, falling back to a fixed line window, always labelled which method produced it. The sibling row is the one native cannot know to ask for: "make one like Format Draft" needs Format Draft's five legs too. The rules row is the second receipt type — how X is wired AND what its rules are — and is the only row a grep script cannot produce.

**Budget:** the five bodies for this feature are small (a menu entry, a ~20-line JS function, a route line, a ~30-line handler, a test); estimate 6–10 KB. Proposed default budget 12 KB (~3k tokens), hard cap 32 KB (the trunk's public-result budget); over budget → bodies become line ranges in a stated order (tests first, sibling next, never the handler), with `elided:` naming what was cut. Never a silent cut; never more than the budget. That answers "don't swamp the window": one receipt costs less than the two file reads it replaces.

**Adoption experiment (pre-registered, tool calls primary):** the exact Dequote/Format task (and one mirror task on the dictation app), cells N (native, 7.4 calls baseline), F (verb described, free choice), M (mandated first call), K (a skill line: "for a change spanning JS and Clojure, call feature_thread first"); n = 3; primary = tool calls to a correct, gate-green edit; secondary returns, wall. Predictions: M and K 2–3 calls (receipt + write + test) vs N 7–8; F adoption ≤ 1/3 (the night's 0/19); correctness equal. Withdrawal: if M does not cut calls by ≥ 2× vs N on both tasks, the verb is not built into the product (the script stays); if F adoption is 0/3, routing is by skill or mandate, never by description.

**Name:** `feature_thread` (the seat's own coinage; `trace_feature` if a verb is wanted).

**One sentence for Gene:** build the verb with bodies + sibling + rules under a 12 KB budget, measure tool calls to a correct edit on your exact request; ≥ 2× fewer calls when mandated says it works, 0/3 free-choice adoption says route it by skill, and < 1.5× says stop.

## 10. Fleet poll on the next step — Sol, verbatim (05:34Z)

# Fleet poll: save tool calls by returning the editable pre-image

1. **The native call sequence is search, reopen, disambiguate; the current receipt removes search but not reopen.** The excerpt preserved in study §0 does not contain the original rollout's tool-call records, so it proves the five owners and the eventual write/test sequence, but not an exact original-call count. The exact reproducible trace is the frozen E-THREAD T1 native arms: **11, 6, and 6 calls; mean 7.7**. T1-N-1 did: (1) read `CLAUDE.md`; (2) repo-wide `rg` for `formatDraft|/api/transform/format`; (3) search handler/test vocabulary while reading `src/writer/views/components.clj`, `resources/public/js/editor-commands.js`, `src/writer/routes.clj`, and `src/writer/handlers/transform.clj`; (4) read `spa_lint_test.clj`, `format_conflict_payload_test.clj`, `handlers/transform_apply_test.clj`, and `editor_durable_ack_test.js`; (5–7) inspect more JS tests and re-search the thread; (8–11) inspect git history, the original Format commit, mechanical-format tests, blame, and the later fencing commit to choose the canonical witness. T1-N-2 compressed that to: instructions → one repo `rg` → one batched read of the four production owners plus `spa_lint_test.clj` and a test search → `transform_apply_test.clj` → history → originating/fencing commits: **6 calls**. T1-N-3 used instructions → `rg` → the four production bodies → test search → handler tests → browser JS test: **6 calls**. Across all three SMW threads N was **7.4 calls and 66 s**. With the current location-only receipt, T1-S was **2/3/2 calls**: receipt → one batched read of the reported production bodies and candidate tests → sometimes one extra test search. Across T1–T3 S was **3.3 calls and 33 s**. The remaining calls are principally **reading the forms that must be understood or edited**; secondarily they choose among 27 test hits, read repo instructions, and follow the constructed alias/assembled-route hop. They are not five more owner-discovery calls. This is exactly why adding bodies is the right next experiment.

2. **Build an edit-basis receipt, not a richer locator.** Ten-line shape:

```text
receipt: feature-thread/edit-basis-v1; status: complete|partial|ambiguous; root; commit
query: {seeds:[formatDraft,/api/transform/format], requested_change, sibling:formatDraft}
budget: {total:16384, per_leg:{menu:512,js:5120,route:512,handler:5632,tests:4096}, used}
menu-caller: {role:sibling, file, lines:[110,111], sha256_utf8, bytes, body}
js-function: {role:sibling, file, lines:[389,454], boundary:heuristic|window, sha256_utf8, bytes, body}
route: {role:sibling, file, lines:[2148,2148], parsed:true, sha256_utf8, bytes, body}
handler: {role:sibling, var:writer.handlers.transform/handle-format, lines, parsed:true, sha256_utf8, bytes, body}
tests: [{role:wiring|behavior, file, owner, lines, parsed, sha256_utf8, bytes, body}]
siblings: [{seed, relation:durable-path|selection-precedent, legs:[...] }]; contracts:[{id,file,lines,sha256,requirements:[...]}]
elisions:[{leg,reason:leg_budget|total_budget, full_lines, full_bytes, full_sha256}]; next_write:{guards:[...]}
```

The hash is over the exact UTF-8 bytes represented by the inclusive line range, including its final newline; a later write asserts that pre-image. For an insertion, also guard the parsed parent owner or the adjacent sibling so line drift cannot retarget it. The **smallest straight-to-write receipt** is one Format Draft sibling body per production leg, the one wiring test plus the one direct handler/JS behavior witness that will be cloned, the insertion relation (`after`/`before`), and the two rule links. Do not return every occurrence or history. For Dequote/Format, the new command has no body yet: these sibling bodies *are* the editable basis.

3. **Use a 16 KiB total receipt cap; 8 KiB is too small for this real sibling.** The cohort worktrees were removed, so these are estimates from the actual line-numbered source printed in the frozen T1 logs, not byte-exact measurements: menu item, 2 lines, **~0.12 KB / 30 tokens**; route entry, 1 line, **~0.08 KB / 20 tokens**; `formatDraft`, about 66 lines (389–454), **~3.8–4.2 KB / 1.0–1.1k tokens**; `handle-format`, roughly 75–85 lines from 606, **~4.3–5.0 KB / 1.1–1.3k tokens**; the wiring test plus two focused behavior forms, **~3.5–4.5 KB / 0.9–1.2k tokens**. The useful five-leg payload is therefore **~12–14 KB, about 3.1–3.6k tokens**; hashes, ranges, status, and contract links bring it near **15–16 KB**. Set both per-leg caps and the 16,384-byte overall cap before reading. Over budget, drop the lowest-priority whole body to `{full_lines, full_bytes, full_sha256}` and add a typed elision naming exactly what was cut. Never return an unlabelled prefix, never call a line window a complete body, and never silently omit a leg. Small whole route/menu entries survive; oversized test collections degrade first to selected witness forms, then to ranges only.

4. **For JS, return a brace-matched *candidate* with a hard fallback, not a claimed parse.** Anchor at an unambiguous `function name(`, `async function name(`, or `const name = ... => {`; scan braces with lexical states for comments, quotes, and template interpolation; label the result `boundary: heuristic`. A raw brace counter breaks on braces in strings/comments, template literals and nested `${...}`, and regex literals; distinguishing regex `/.../` from division needs syntactic context. Nested ordinary functions/arrows are fine only if their braces are counted; expression-bodied arrows have no closing function brace to find. My recommendation is **lexically aware brace matching, bounded to 96 lines, then fail closed to a 96-line window labelled `complete:false`**. For this SMW body the 66-line function fits. A line-range-only answer is safe but forfeits the tool-call saving; a silently confident brace matcher is worse than native search.

5. **Name the verb `feature_thread`.** That is the phrase the observed agent invented, it states the returned object, and it does not falsely promise a runtime trace (`trace_feature`) or only ownership (`owners_of`). Pre-register a paired, interleaved **4-cell × n=6 = 24-arm** cohort over three frozen real cross-language feature additions (Dequote/Format plus two comparable JS↔Clojure changes, two reps each): **N**, native; **F**, verb available and its edit-basis behavior described, free choice; **M**, prompt mandates `feature_thread` first; **K**, an installed skill says “for any change spanning JS and Clojure, call `feature_thread` first.” Same caller, harness, commits, acceptance tests, and write/verification permissions. Primary meter is watcher-counted **tool calls from prompt to the first acceptance-green edit**; failures are failures, not removed from the denominator. Then report watcher-counted total model returns; wall is descriptive. Classify every call as discovery, body-read, contract-read, write, or verify, and count refusals/fallbacks. Predictions: adoption **F 0/6, M 6/6, K 6/6** (the 0/19 prior says description will not route; mandate/skill will); correct edits **6/6 each**; median calls **N 10, F 10, M 4, K 4**; median returns **11, 11, 5, 5**; descriptive wall **~95, ~95, ~45, ~45 s**. The mechanism claim is at least **3 median calls removed**, specifically body/contract reads, with no correctness loss. Withdraw the form-bearing product if pooled M+K saves **<2 median tool calls** versus N, if either routed cell has lower acceptance than N, or if a stale/misbounded body causes one wrong edit. If F adopts in **≤1/6**, withdraw free-choice delivery—not the routed verb—and record the expected adoption null.

6. **The one receipt must carry rules that search hits cannot establish; model it as a second receipt mode.** I would want: (a) the Format Draft durable-path contract—single-flight `beginEditorCommand`, snapshot required, editor read-only latch preserved even when the journal is dead, `{sync}` committed through `fold-editor-snapshot-and-tx!`, 409 raised as a resolvable conflict, visible bytes unchanged on refusal, authoritative frame applied only after success, durable receipt acknowledged, cleanup always ends the command; (b) the selection contract—non-empty **half-open range**, its coordinate system and snapshot identity, bounds validation on both client and server, and the explicit law “no selection refuses; it never expands to the whole document”; (c) the precise intent/spec identifier and owning design/spec lines, plus direct witness tests. There are really two sibling relations: `formatDraft` supplies durability/conflict behavior, while the existing selection command supplies range acquisition/refusal behavior. I would expose `feature_thread(mode="locations")` and **`feature_thread(mode="edit-basis", sibling="formatDraft")`** as two receipt types under one verb, not create two verbs. This is where clj-surgeon beats a grep script: it can parse the Clojure handler, route vector, menu owner, and test Vars; return exact owner ranges and pre-image hashes; follow route Var identity and `@spec` links; and produce guarded structural selectors. Grep can cheaply nominate candidates, but it cannot honestly claim form boundaries, one-owner identity, parent/insertion guards, or a conflict/selection contract. Keep the JS leg explicitly heuristic.

7. **Gene: build `feature_thread` edit-basis first; it worked if routed correct Dequote/Format edits fall from a predicted 10 tool calls to 4 (at least 3 saved), and stop if the matched median saving is under 2 calls.**

**Convergence with §9 (Fable, written first):** same verb name (`feature_thread`); the receipt is an EDIT BASIS (bodies + sibling + rules), not a richer locator; JS by brace-matched CANDIDATE with lexical states and a labelled fallback, never a claimed parse; the rules row as a second receipt mode; adoption measured on tool calls to a correct edit. Divergence: Sol sizes the cap at 16 KiB (8 KiB too small for the real sibling); §9 said 12 KB default / 32 KB cap — take 16 KiB default. Sol's number: predicted 10 → 4 calls (≥ 3 saved) says it worked; a matched median saving under 2 calls says stop. The mayor's transcript analysis (2026-09-04-smw-five-searches-analysis-mayor.md) corrects the premise: there were no five searches — five FILE READS batched over six rounds, one JS file read at four guessed ranges; the warm-up IS the cost. Human baseline: six rounds.
