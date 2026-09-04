## NO-GO

Round-three independent review of `bridge/feature-thread-verb` at **9139b2c5** (`feature_thread`,
the seventh MCP tool). Reviewer: Opus, forge@anvil. Sol's content filter refused this brief; paths
were substituted (verdict path, fixtures under `/var/tmp/forge/ft3-review-fx/opus`).

Every claim below was reproduced by running it against my own server. Nothing in the clone was
modified.

```
$ cd /home/forge/tmp/sol/ft1-wt && git rev-parse HEAD && git status --porcelain
9139b2c5d269dbf1ca8d08d78d5af81943273a28
                              <- porcelain empty, tree clean, verified again at the end
```

Server: my own, on an explicit port **8127**
(`clojure -X:clj-surgeon/mcp :port 8127 :nrepl-port :none :telemetry :off`). Seven tools live:

```
$ python3 cli.py tools/list
count 7
- inspect_clojure  - apply_clojure_changes  - edit_clojure  - transform_clojure
- alias_migration  - admit_clojure_patch   - feature_thread
```

---

## Headline

**Round two did the work.** Every round-one finding I re-attacked is genuinely closed, not papered
over: the `return /[}]/` false close, the `route-assembled` false leg, the imperative pre-image
line, the missing admission ceilings, the drifting `text_bytes`, and the `admit_clojure_patch`
rider in the managed onboarding block. All six gates reproduce at the claimed numbers. The new
`CANDIDATE` status is a real control, loudly rendered, and it is computed — not printed.

**The NO-GO is on one new false green and one surviving instance of each round-one class.**

* **B3 (new, blocking)** — the automatic `implementation` leg is walked over a file set that was
  bounded before the leg existed. It can report `status: N/A` with the reason *"the only definition
  a seed names is already a leg of this receipt (…editor-commands.js:L389-L454)"* while
  `(defn mechanical-format …)` sits unread in `src/writer/other/dup.clj` — and the thread still
  reads **`COMPLETE (5 of 5)`**. The receipt prints the `rg` line with `-g 'src/**/*.clj'` as a
  search it ran. It did not run it over that file.
* **B1′ (blocking)** — the wrong-range-labelled-`closed` class survives through the *character
  class* door instead of the keyword door: `/[/}]/` is valid JavaScript (verified with node), the
  lexer closes at the `}` inside the regex, and the receipt publishes a two-line truncation labelled
  `brace-window(lexed,closed)` with a sha256 over it. The namespace's stated single failure mode is
  still violated, and the round-two witness — which fixed the keyword direction — has no character
  class case.
* **B2′ (blocking)** — `comment-mention?` recognises only *line* comments (`;`, `//`). A Clojure
  `(comment …)` form is a parsed top-level form, so a subject mentioned only inside one is reported
  **`FOUND`**, `boundary=form(parsed)`, no weak reason. The leg is wrong and unlabelled.

All three are small, local, and of the same family as the round-one two. This branch is one round
from GO for the second time, and for the same reason each time: *the search's scope and the hit's
lexical context are decided by two different pieces of code, and neither tells the receipt when it
is out of its depth.*

---

# 1. WHAT REPRODUCED EXACTLY

## 1.1 T1 — six legs, ranges, hashes, bodies (claim 1) — REPRODUCED

Live verb, `smw-dequote` fixture. Every leg's sha256 recomputed over its exact line range from the
file on disk, and every body compared byte-for-byte to the slice:

```
$ python3 t1.py
status COMPLETE (6 of 6)
text bytes actual 23073   text_bytes field 23073   receipt_bytes 48455   structured_bytes 25382
menu-caller    FOUND src/writer/views/components.clj              102 113  evid=identifier-or-route  bnd=form(parsed, member of L92-L165 top-tabs)
    SHA OK | BODY OK | BYTES OK
js-function    FOUND resources/public/js/editor-commands.js       389 454  evid=identifier(def)      bnd=brace-window(lexed,closed)
    SHA OK | BODY OK | BYTES OK
route          FOUND src/writer/routes.clj                       2148 2148 evid=route-literal        bnd=form(parsed, member of L2083-L2376 make-routes)
    SHA OK | BODY OK | BYTES OK
handler        FOUND src/writer/handlers/transform.clj            606 680  evid=handler-join         bnd=form(parsed)
    SHA OK | BODY OK | BYTES OK
tests          FOUND test/writer/handlers/transform_apply_test.clj 349 384 evid=form(deftest,CALLS-handle-format) bnd=form(parsed)
    SHA OK | BODY OK | BYTES OK
implementation FOUND src/writer/handlers/transform.clj             81 132  evid=identifier(def)      bnd=form(parsed)
    SHA OK | BODY OK | BYTES OK
```

**No digest and no body is wrong anywhere I looked, in any fixture, in any run.** The
`implementation` leg is deduped against the handler (L81-132 vs L606-680) exactly as claimed.

**Common-prefix disambiguation holds.** I appended `function formatDraftX(ed)` at L506-508:

```
formatDraft    js-function FOUND editor-commands.js 389-454
formatDraftX   js-function FOUND editor-commands.js 506-508
```

## 1.2 B1 — the keyword direction is FIXED (round-one blocker) — REPRODUCED

`regex-context-keywords` (`src/clj_surgeon/mcp_feature_thread.clj:404-414`) plus `word-before`
(`:421`) close it. Fixture `jsfx/resources/public/js/attacks3.js`, 602 lines, driven through the
live verb, each case checked against the true end of its function (the line before the next
`function AFTER_…` sentinel):

| subject | status | range | boundary | verdict |
|---|---|---|---|---|
| `trapReturnRegex` `return /[}]/` | FOUND | 1-3 | `brace-window(lexed,closed)` | **correct** |
| `trapTypeofRegex` `typeof /x}/` | FOUND | 6-9 | closed | **correct** |
| `trapCaseRegex` `case /a}/.source:` | FOUND | 12-17 | closed | **correct** |
| `trapYieldRegex` `yield /}/` | CANDIDATE | 20-23 | closed | correct range, weak evidence |
| `trapThrowRegex` `throw /}/` | FOUND | 70-73 | closed | **correct** |
| `trapDivChain` `a / b / 2` | FOUND | 26-29 | closed | **correct** (still division) |
| `trapParenDiv` `(a + b) / 2` | FOUND | 39-42 | closed | **correct** |
| `trapBracketDiv` `arr[0] / 2` | FOUND | 45-48 | closed | **correct** |
| `trapIncDiv` `a++ / 2` | CANDIDATE | 1-72 | `line-window(+/-40, unclosed at L32)` | fails INTO the labelled downgrade |
| `trapTemplateNested` `` `x${ {a:1} }y` `` | FOUND | 76-79 | closed | **correct** |
| `trapCommentInString` `"/* not a comment }"` | FOUND | 82-85 | closed | **correct** |
| `trapRegexWithSlashes` `/a\/\/b[}]/` | FOUND | 58-61 | closed | **correct** |
| `splitPathSegments` `p.split(/[/\\]/)` | FOUND | 598-602 | closed | **correct** |
| `trapUnterminatedComment` | CANDIDATE | 48-128 | `line-window(+/-40, unclosed at L88)` | labelled |
| `trapLongFunction` (504-line body) | CANDIDATE | 53-133 | `line-window(+/-40, unclosed at L93)` | labelled |

`a++ / 2` is read as a regex (`+` is in `regex-preceding-chars`) and therefore never closes — the
safe direction, and it downgrades **labelled**. Fourteen of the brief's fifteen named shapes pass.
The fifteenth is B1′, below.

**The ASI case is not a defect and I withdraw it.** I drove
`const re = s` ⏎ `/[}]/.test(s)` and the lexer read it as division, producing a 51-53 range. Real
JavaScript reads it as division too — `node -e` rejects that source outright as a syntax error — so
there is no correct closed range to compare against. Reporting it would have been a false finding.

## 1.3 B2 — `route-assembled` and comment/string mentions are FIXED (round-one blocker) — REPRODUCED

`leg-strength` (`:1178`), `weak-evidence-kinds` (`:1167`), `comment-mention?` (`:943`) and
`thread-status` (`:1718`) together do the work, and `thread-status` counts only `FOUND`.

I rebuilt round one's exact fixture — route line 2148 replaced with
`(assembled-route ["api" "transform"] "format" #'transform/handle-format)`, plus a decoy comment
`;; note: the old names were "transform" "format" before the rename` at the end of `routes.clj`:

```
STATUS: INCOMPLETE (4 of 5) | complete: False | route_handler: None
  route  CANDIDATE  src/writer/routes.clj 2370-2379 evid=route-assembled bnd=line-window(no-enclosing-top-level-form)
     weak: the hit is a comment mention, not code
'COMPLETE (6 of 6)' in text: False
```

Round one's receipt said `COMPLETE (5 of 5)` on this input. It now says `INCOMPLETE (4 of 5)`.

The comment/string decoy battery (`ghostFeature` planted in a Clojure `;;` line comment, a JS
`/* */` block, and a JS string constant):

```
STATUS: INCOMPLETE (1 of 6) complete: False
  menu-caller CANDIDATE components.clj 250-259  weak: the hit is a comment mention, not code
  js-function CANDIDATE editor-commands.js 467-515 weak: the boundary is not a parsed form or a closed brace window
  route       ABSENT
  handler     FOUND     transform.clj 935-936        <- B2', section 2.3
  tests       ABSENT
```

The rendering is unmissable, which matters as much as the computation:

```
leg menu-caller  CANDIDATE weak=the hit is a comment mention, not code — NOT counted toward COMPLETE — src/writer/views/components.clj L250-L259 sha256:359e621d…
```

`evid=identifier(def)` is no longer stamped on a fallback-search hit: the string-constant hit above
carries `evid=identifier`. Round one's mislabel is gone.

**Answering the brief's question — can a CANDIDATE carry a sha/anchor a reader might edit
against?** Yes: `sha256:359e621d…`, `bytes=478`, `anchor=after:L259` are all present on that
comment-mention candidate, over a range (a `:keydown-handler` string) that has nothing to do with
the subject. The sha is honest (it is over the slice it names) and the `NOT counted toward
COMPLETE` label precedes it on the same line, so a reader is warned. **Non-blocking**, but I would
drop `anchor` from a CANDIDATE: a hash invites verification, an insertion anchor invites a write.

## 1.4 Budget, elision, ceilings, refusals (claims 3, 4; items 20, 25, 26, 27) — REPRODUCED

| request | result |
|---|---|
| default | `text=23073B (budget 24576B)` — default is **24576**, cap **32768** |
| `budget_bytes: 10240` | text **9741 B ≤ 10240**, every cut labelled |
| `budget_bytes: 1` | typed `feature-thread-budget-exceeded`, *"above the budget of **1** this request asked for"* |
| `budget_bytes: "lots"` | typed, `budget_bytes must be a positive integer` |
| `budget_bytes: 40000` | typed, `above the hard cap of 32768; it is never silently clamped` |
| `subject` 10 001 chars | typed at **admission**, `subject is 10001 characters, above the ceiling of 512` |
| `subject` 513 / 512 chars | 513 refused naming the field; 512 accepted |
| `also` 33 / 32 seeds | 33 refused, `also carries 33 seeds, above the ceiling of 32`; 32 accepted |
| unknown field | `feature_thread does not accept: evil` |
| absent root | `workspace root is not an existing directory: …` |

Round-one finding 4 (ceiling, wrong field) and finding 5b (negative budget in a refusal) are both
closed. `receipt_bytes = text_bytes + structured_bytes` on every successful receipt I measured.

**MCP-OP-THREAD-012 (text ⊇ structured) holds on every receipt, refusals included.** I walked every
scalar leaf of `structuredContent` and looked for its Clojure `str` spelling in the text block:
`SUPERSET missing: NONE` on all twelve cases above and on every fixture in this review.

**Elision order is edit-aware as declared** (`elision-order`, `:174-186`), handler and the seed
definitions last; each cut prints leg, bytes, range, sha256 and an executable `nl -ba … | sed -n`
refetch.

## 1.5 Typed refusal and path confinement (claim 4) — HELD under four attacks

```
chmod 000 on the handler file
  status INCOMPLETE (4 of 5); handler ABSENT unreadable=[{transform.clj unreadable}]
  "COMPLETE (5 of 5)" in text: False

leg file is a symlink out of the root (src/writer/handlers/leaked.clj -> …/outside-secret.clj
  containing SECRET_CANARY_XYZ)
  CANARY in text: False | outside path published: False

conventions file malformed
  feature-thread-conventions-invalid ".clj-surgeon/feature-thread.edn did not read as EDN: EOF while reading"

conventions globs ["../../../../etc/*" "/etc/passwd" "../*"]
  menu-caller ABSENT; CANARY: False; no /etc content in the receipt
  (the glob string is echoed inside the rendered rg line — quoted, never read)
```

`read-source` (`:333`) always re-joins `(io/file root relative)` and `relative` only ever comes from
the canonicalised walk, so the confinement is structural, not a check. **No read outside the root,
no path published outside it.**

## 1.6 Items 16–22 — REPRODUCED

* **(16) implementation leg** — automatic, deduped against the handler by `[file from to]`
  (`resolve-implementation`, `:1679`), and `N/A`-and-uncounted when no seed names a definition
  (`thread-status` removes `"N/A"` before counting, `:1724`). Verified on three fixtures. **The
  case the brief asked about last — a seed naming two definitions in two files — is B3.**
* **(17) governance** — `:form_start/:form_end/:anchor` computed by bracket-span parsing
  (`governance-entry`, `:1386`) with a 200-line entry ceiling. Verified against the fixture registry
  by hand: `{:id :EDITOR-JDEAD-009` really does span L270-L299 and `{:id :EDITOR-CONF-005` L382-L400.
  Two rows honestly say `anchor=unparsed` and fall back to a `±(4,20)` line-window refetch instead
  of a confident wrong anchor. **Honest and bounded — but the brief's premise is wrong**: I read the
  fixture registry with `clojure.edn/read-string` and it returns **VALID EDN**. The two `unparsed`
  rows come from the redaction leaving an unterminated multi-line string at L244 that swallows
  L244-L268 and desyncs the span lexer — not from the file failing to parse. The behaviour is right;
  the explanation in the brief is not.
* **(18) tests co-primary per language** — the Clojure primary is `form(parsed)`
  `evid=form(deftest,CALLS-handle-format)`; the JS co-primary is
  `test/js/browser_runtime_classic_script_test.js` L63-94,
  `boundary=brace-window(lexed,closed), test-call at L63`.
* **(19) `rules :verify` from Makefile recipes** — five rows, each with target, Makefile line,
  command, the file it is for, and an `evidence` kind (`alias` vs `names-the-file`). Verified
  against the fixture Makefile: `test-js` is at line 233 and its recipe does name
  `test/js/browser_runtime_classic_script_test.js`.
* **(21) `next_call`** — `admit_clojure_patch` with `expect_pre_sha256` as **whole-file** digests.
  I recomputed all six with `hashlib.sha256(open(f,'rb').read())` and **all six match**:
  ```
  OK resources/public/js/editor-commands.js 61b04938359cd7da
  OK src/writer/handlers/transform.clj      1614a2930df91a43
  OK src/writer/routes.clj                  9620bbb40dfebb18
  OK src/writer/views/components.clj        5795fdf98c7fe608
  OK test/js/browser_runtime_classic_script_test.js da645efaf7e88e49
  OK test/writer/handlers/transform_apply_test.clj  6c6b0f813ecad41e
  ```
* **(22) the header names the governed number** —
  `status=COMPLETE (6 of 6) — legs, not bytes`.

## 1.7 Round-one finding 3 (the printed instruction) — RULED CLOSED

The `:assert` line is now advisory about itself and points at the call that is a gate
(`build-rules`, `:1607-1613`):

> "the per-leg sha256 is the human-checkable detail of what this read-only verb read; **it enforces
> nothing itself**, so do NOT re-read the ranges to check it. Pass `next_call.expect_pre_sha256`
> (whole-file digests) to `admit_clojure_patch`, which BINDS the pre-image at write time and answers
> a mismatch with a typed refusal, never a retry"

That is the correct fix and it is honest. One caveat is in section 3.2.

## 1.8 Item 6 — the onboarding block — CLOSED

`63d1612c` reverts `admit_clojure_patch` out of the managed Codex block. The installed block now
lists exactly six tools, and the witness asserts the **exact string**, not a `contains?`:

```
enabled_tools = ["inspect_clojure", "apply_clojure_changes", "edit_clojure", "transform_clojure", "alias_migration", "feature_thread"]
```

No write-capable tool is enabled out of band. Honest.

## 1.9 Gates (claim 6) — ALL REPRODUCED VERBATIM

| gate | claimed | measured |
|---|---|---|
| `make mcp-operation-oracle` | pass | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` EXIT=0 |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | 742 / 9473 / 0 | **`Ran 742 tests containing 9473 assertions. 0 failures, 0 errors.` EXIT=0** |
| `~/bin/suite-run bb test/run_all.clj` | 814 / 6724 / 0 | **`Ran 814 tests containing 6724 assertions. 0 failures, 0 errors.` EXIT=0** |
| `make mcp-smoke` | seven tools | `{:ok true … :tools [… "admit_clojure_patch" "feature_thread"], :response-count 3}` EXIT=0 |
| `make repository-hygiene` | clean | `repository hygiene: no machine-local build cache is tracked at any depth` EXIT=0 |
| intent audit | THREAD-001..027 implemented | **`:ok true :specs 377 :violations 0`**, and all 27 `MCP-OP-THREAD-001…027` present |

**The `ad49908c` red is gone.** `mcp_feature_thread_test.clj:388` now asserts the new message text,
and `conventions-may-declare-more-than-the-five-leg-roles` is a witness for the changed behaviour.
The tip is green on every gate.

## 1.10 The fifteen new witnesses go RED under sabotage

Fifteen `deftest`s were added between `02e823e7` and `9139b2c5`. I exported the tree with
`git archive` under my fixture directory, sabotaged **one** invariant per run in the source there,
and ran the suite. **Fifteen arms, fifteen hits: every new witness goes RED under the sabotage of
its own invariant, and the baseline arm is green.**

| arm | sabotage applied to the export's `mcp_feature_thread.clj` | witness that went RED |
|---|---|---|
| S1 | `implementation-leg` returns `nil` (no automatic leg) | `t1-smw-thread-returns-six-legs-with-bodies`, `the-implementation-leg-is-automatic-deduped-and-honestly-uncounted` |
| S2 | `(<= 5 …)` → `(= 5 …)` + message back to "exactly five" | `conventions-may-declare-more-than-the-five-leg-roles` |
| S3 | `:def` search loses its `\(defn?-? +` alternative | `def-legs-recognise-clojure-definitions` |
| S4 | `thread-status` counts `N/A` legs | `the-implementation-leg-is-automatic-deduped-and-honestly-uncounted` |
| S5 | governance rows lose `:form_end` / `:anchor` | `governance-rows-carry-an-entry-end-and-an-anchor` |
| S6 | `co-primary-members` returns nothing | `the-tests-leg-has-one-primary-per-language` |
| S7 | `rules :verify` emptied | `the-rules-row-names-how-to-run-the-tests-leg` (**only** that one) |
| S8 | default budget → 16384, elision order reversed | `the-budget-default-and-the-elision-order-are-edit-aware` |
| S9 | `:assert` reverted to the old "a mismatch is a REFUSAL" imperative | `the-assert-line-costs-the-caller-no-calls` |
| S10 | header drops `— legs, not bytes` | `the-header-names-the-number-the-budget-governs` |
| S11 | `regex-context-keywords` emptied | `a-regex-after-a-keyword-is-not-a-division` |
| S12 | `leg-strength` always returns `FOUND` | `a-comment-mention-is-a-candidate-and-never-completes-a-thread` |
| S13 | `:next_call` set to `nil` | `the-receipt-hands-over-a-binding-the-write-gate-can-use` |
| S14 | `receipt-tail-bytes` 96 → 0 (variable-width clock) | `the-receipt-byte-counts-describe-the-delivered-text`, `the-entrance-publishes-a-clock-and-a-structured-receipt` |
| S15 | `max-subject-chars` / `max-also-seeds` raised to 1 000 000 | `subject-and-also-have-named-admission-ceilings` |
| — | **BASELINE (pristine export)** | only the ambient `no-machine-local-build-cache-is-tracked` (§3.7) |

Seven of the fifteen arms fail *only* their own witness plus the ambient one, which is the sharpest
possible result: the witness is bound to the invariant, not to the receipt in general.

---

# 2. BLOCKING FINDINGS

## B3 — `COMPLETE (5 of 5)` while the definition the receipt exists to find is unread, with a FALSE reason and a search line that was never run over the file

This is the brief's own item 16 question — *"a seed that names TWO definitions (same name in two
files)"* — and the answer is worse than a missed secondary.

**Root cause.** `execute-request` bounds the file walk **before** the automatic leg exists
(`src/clj_surgeon/mcp_feature_thread.clj:2346-2352`):

```clojure
candidate-globs (vec (distinct
                       (concat
                         (mapcat :globs (:legs conventions))
                         (get-in conventions [:governance :globs]))))
walk (walk-relative-paths root (:exclude-dirs conventions) candidate-globs)
```

`implementation-leg` (`:1660`) is constructed later, inside `resolve-thread` (`:1703`), and
contributes `implementation-clojure-globs` `["src/**/*.clj" "src/**/*.cljc"]` to **its own
`:globs`** — which `resolve-leg` then filters against `paths` that were selected without them. So
the free implementation leg can only ever find a definition inside a file one of the *declared* legs
already selected. It never looks where it says it looks.

**Reproduction.** Fixture `falsefx`: the `smw-dequote` tree with `(defn mechanical-format …)` moved
verbatim out of `src/writer/handlers/transform.clj` (L81-132) into `src/writer/other/dup.clj` —
still under `src/`, still `.clj`, still matched by `src/**/*.clj`:

```
$ grep -rn "defn mechanical-format" src/
src/writer/other/dup.clj:3:(defn mechanical-format

$ feature_thread {"subject":"formatDraft",
                  "also":["/api/transform/format","mechanical-format"],
                  "scope":{"workspace_root":".../falsefx"}}
STATUS: COMPLETE (5 of 5) | complete: True
  menu-caller     FOUND      src/writer/views/components.clj
  js-function     FOUND      resources/public/js/editor-commands.js
  route           FOUND      src/writer/routes.clj
  handler         FOUND      src/writer/handlers/transform.clj
  tests           FOUND      test/writer/handlers/transform_apply_test.clj
  implementation  N/A        None   the only definition a seed names is already a leg of this
                                    receipt (resources/public/js/editor-commands.js:L389-L454)
dup.clj named anywhere: False
```

Text line 247, verbatim:

```
leg implementation  N/A · implementation: n/a (the only definition a seed names is already a leg of this receipt (resources/public/js/editor-commands.js:L389-L454)) — not counted in the leg status
```

Three failures in one row:

1. **The status is a false green.** `COMPLETE (5 of 5)`, `complete: true` — while the exact form the
   round-two design cites as its whole justification (*"the real edit inserted its new function
   immediately after `(defn mechanical-format …)`"*, `implementation-leg` docstring) is missing from
   the receipt. The denominator silently drops from 6 to 5 because the verb could not see the leg,
   which is indistinguishable from the honest case where the leg does not exist.
2. **The reason is affirmatively false.** `mechanical-format`'s definition is not
   `editor-commands.js:L389-L454`; that is `formatDraft`'s JS function. The receipt makes a specific,
   checkable, wrong statement about a named file and line range.
3. **The receipt publishes a search it did not run.** The leg's `searches` row is an executable
   command:
   ```
   definition-shaped: rg -n -e '\(defn?-? +(?:\QformatDraft\E|\Qgadgetize\E)\b|…' -g 'src/**/*.clj' -g 'src/**/*.cljc' -g 'resources/public/js/*.js' …
   ```
   Pasted into a shell it returns `src/writer/other/dup.clj:3:(defn gadgetize`. The verb never opened
   that file. A search line the caller can run and get a different answer from is the strongest
   possible tell that the receipt does not know its own scope.

I confirmed the same shape with an unambiguous second seed (`gadgetize`, defined **only** in
`src/writer/other/dup.clj`): `implementation` is `N/A`, `dup.clj` is named nowhere in the text or the
structured content, and `scanned_files` is 9.

The two-definitions case is the milder sibling: with `mechanical-format` defined in both
`handlers/transform.clj` and `other/dup.clj`, the receipt reports the first, and `also: []`,
`co_primaries: []` — the second owner is not mentioned at all.

**A convention set that declares its own `implementation` leg is unaffected** (`implementation-leg`
returns `nil`, so the declared globs are in the walk). The defect is specific to the automatic leg —
the thing round two added.

**Fix.** Compute the automatic leg's globs before the walk and union them into `candidate-globs`;
and make the `N/A` reason cite the seed it is talking about, so it cannot name an unrelated leg. A
witness: two definitions of one seed in two files, one outside every declared glob — the receipt
must name both, and must not report the thread `COMPLETE` with the leg silently uncounted.

## B1′ — a WRONG range still labelled `brace-window(lexed,closed)`: an unescaped `/` inside a regex character class

`lexed-brace-match`'s `:regex` mode (`:481-485`) ends the literal at the **first** unescaped `/`:

```clojure
:regex
(cond
  (= c \\) (recur (+ i 2) :regex …)
  (= c \newline) nil
  (= c \/) (recur (inc i) :code depth opened? template-stack \/)   ; <- :484
  :else (recur (inc i) :regex …))
```

A `/` inside a character class does **not** end a JavaScript regex. `/[/}]/` is valid:

```
$ node -e "const re = /[/}]/; console.log('charclass-slash regex VALID, test:', re.test('}'))"
charclass-slash regex VALID, test: true
```

The lexer closes the regex at the inner `/`, returns to `:code`, and counts the `}` as the closing
brace of the function:

```
$ feature_thread {"subject":"trapRegexCharClassSlash","scope":{"workspace_root":".../jsfx"}}
{
 "status": "FOUND",
 "file": "resources/public/js/attacks3.js",
 "from": 64, "to": 65,
 "evidence": "identifier(def)",
 "boundary": "brace-window(lexed,closed)",
 "sha256": "3f84cfb59455757a1904751261d40def9f332144074e00df166a035c165d5679",
 "bytes": 58,
 "body": "function trapRegexCharClassSlash(s) {\n  const re = /[/}]/;",
 "anchor": "after:L65",
 "weak_reason": null
}
```

The true function ends at L67. The body is **truncated mid-function**, it is labelled **`closed`**
— the label the design reserves for a range it is confident in — the `weak_reason` is `null`, and
`anchor: "after:L65"` points **inside** the function. A sha256 over that truncation is published,
and `leg-strength` promotes it to `FOUND` because `strong-boundary?` (`:1171`) only tests the label
string.

This is the same contract the namespace still asserts three lines above the fix
(`:395-397`): *"A wrong guess walks into a phantom regex and never closes, so it fails INTO the
downgrade rather than into a wrong answer."* Round two closed the keyword door and left this one.

**The oracle exists and misses it.** `a-regex-after-a-keyword-is-not-a-division`
(`test/clj_surgeon/mcp_feature_thread_test.clj:968-990`) drives fourteen keywords and one division
case; `javascript-bodies-are-lexed-brace-matches-or-labelled-windows` (`:407-428`) drives strings,
templates, comments and `/\d{2}/`. **No case contains a `/` inside a character class.** Per house
doctrine, correcting the oracle belongs in the same fix.

**Frequency, stated honestly.** This is narrower than round one's `return /re/`: it needs an
unescaped `/` in a character class *and* an odd `}` count between the mis-lex and the true end.
`p.split(/[/\\]/)` — the most natural instance — happens to recover and closes correctly (verified,
598-602). But `/[/}]/`, `/[^/}]/` and `/[/{}]/` are exactly the shapes a route or template lexer
writes, and a wrong `closed` with a pre-image hash is the class that terminates investigation.

**Fix.** Track character-class state in `:regex` mode (`[` … `]`, honouring `\]`), so a `/` inside
a class does not end the literal. Add `/[/}]/` to the lexer witness so it goes red first.

## B2′ — a Clojure `(comment …)` form is reported `FOUND`, `boundary=form(parsed)`, no weak reason

`comment-mention?` (`:943-968`) scans the hit's own line for a preceding `;` (Clojure) or `//`
(scripts). A `(comment …)` form and `#_` are not line comments, so a subject that exists only inside
one is indistinguishable from live code:

```
;; fixture blockfx: the ONLY mention of `widgetize` in src/ is
;;   (comment
;;     (widgetize {:draft "x"}))
;; appended to src/writer/handlers/transform.clj

$ feature_thread {"subject":"widgetize","scope":{"workspace_root":".../blockfx"}}
STATUS: INCOMPLETE (1 of 5)
  handler  FOUND  src/writer/handlers/transform.clj  935-936
           evid=identifier-or-route  bnd=form(parsed)  weak=None
           body: '(comment\n  (widgetize {:draft "x"}))'
```

`FOUND`, a parsed boundary, a sha256, an insertion anchor — over a form that never executes. The
same fixture reproduces it under the `ghostFeature` battery in §1.3, where the four other legs are
correctly `CANDIDATE`/`ABSENT` and only the `(comment …)` leg is promoted.

I did not construct an input where this alone carries a thread to `COMPLETE`, and I say so plainly:
the aggregate status was `INCOMPLETE` in both of my constructions. But the brief's blocking
criterion is *"a receipt that claims COMPLETE while a leg is missing **or wrong**"*, and this leg is
wrong, unlabelled, and of exactly the class round two set out to close. `leg-strength`'s docstring
says *"a labelled line window, an assembled or tail-matched route, a bare identifier, **or a comment
mention** is a lead"* — and for the two most idiomatic Clojure comment forms it is not.

**Fix.** Extend `comment-mention?` (or a companion predicate over the enclosing form) to recognise
a hit whose enclosing top-level form is `(comment …)` and a hit preceded by `#_` — the enclosing
form is already computed by `clojure-body`, so the information is in hand. For scripts, `/* … */`
spanning lines has the same hole; it happened to fall to a labelled window in my fixture, which is
luck, not a control. Have the reviewer re-plant the decoy and watch the witness go red.

---

# 3. NON-BLOCKING FINDINGS

### 3.1 `text_bytes` still over-reports the delivered text, intermittently — item 25 is not fully closed

The fixed-width clock (`receipt-tail`, `:1873-1888`) fixed the *width* but not the *content
coupling*. `ensure-superset` (`:1785`) decides whether to append a `structured-only ·` line by
substring-testing each structured leaf against the text — and the text now contains the clock's
digits. A leaf whose value happens to appear inside `elapsed_ms=111.905389` is "found" at delivery
but was "missing" during `measure`'s fixpoint, so the delivered text is shorter than the number the
receipt prints about itself.

Identical request, twenty-five consecutive runs:

```
run0: actual=17465 text_bytes=17488 delta=+23 elapsed_ms=130.729768
run5: actual=17465 text_bytes=17488 delta=+23 elapsed_ms=111.905389
run9: actual=17465 text_bytes=17488 delta=+23 elapsed_ms=111.139252
nonzero deltas: 3 of 25
```

`the-receipt-byte-counts-describe-the-delivered-text` is a deterministic witness over a receipt
whose clock is `pending`, so it cannot see this. **The direction is safe** — the clock can only add
substrings, so the delivered text is never *larger* than the declared count and the budget is never
breached — but the receipt's self-description is wrong 12% of the time on this input. Exclude the
tail from `ensure-superset`'s substring test, or run the completion pass before the tail is
composed.

Also: on a `budget_bytes: 1` refusal, `text_bytes` is `9737` while the delivered text is `410` — the
field describes the receipt that could not be sent. Defensible, but it is a second meaning for the
key that item 25 gave one meaning to. Name it `would_be_text_bytes` there.

### 3.2 `next_call` is not directly executable in the common case

`next_call.expect_pre_sha256` names **all six leg files**. `admit_clojure_patch` requires it to name
**exactly** the files the patch touches (`src/clj_surgeon/mcp_admit_tool.clj:1338-1346`):

```clojure
(not= admitted declared)
… "expect_pre_sha256 must name exactly the files the patch touches. This patch touches …"
```

So the receipt's imperative — *"Pass `next_call.expect_pre_sha256` (whole-file digests) to
`admit_clojure_patch`"* — is obeyable only when the caller's patch touches all six. The normal edit
(handler + its test) will be refused with `invalid-admit-request`. The refusal is typed and names
both sets, so nothing is silently wrong; but the hand-off is advertised as a copy-paste and it is
not one. Either say "the subset that your patch touches", or emit per-leg maps the caller can
select from. *(Ruled from the code; I did not drive `admit_clojure_patch` end to end, and I say so.)*

### 3.3 A `verify` row hands the caller a make directive as a shell command

```
{"target":"test-js","line":233,"command":"@node --test test/js/browser_runtime_classic_script_test.js","evidence":"names-the-file"}
```

The leading `@` is Makefile recipe syntax, not shell. Strip it, or label the row as a recipe line.
(The `runtests-*` rows have no prefix and are runnable.)

### 3.4 Duplicated `unreadable` entries

The `chmod 000` receipt lists the same file twice on one leg:

```
handler ABSENT unreadable=[{'file': 'src/writer/handlers/transform.clj', 'reason': 'unreadable'},
                           {'file': 'src/writer/handlers/transform.clj', 'reason': 'unreadable'}]
```

One entry per file per leg; `scan` accumulates across searches without `distinct`.

### 3.5 Carried over from round one, still open (both harmless, both one line)

* `walk-relative-paths` (`:296`) tests containment with `(str/starts-with? abs root-path)` and no
  separator, so a sibling `<root>-evil` passes the prefix test. Harmless only because `read-source`
  re-joins under `root`. Append a separator.
* The same walk does not de-duplicate; a `..` symlink inside the tree makes one file appear many
  times in `:paths`. `(vec (sort (distinct @acc)))` costs nothing.

### 3.6 A correction to the brief, not a defect

Item 17 states the fixture registry "is a lossy redaction (not valid EDN)". It **is** valid EDN —
`clojure.edn/read-string` over `docs/intent/registry.edn` returns a value. The two `anchor=unparsed`
rows are produced by the redaction leaving an unterminated multi-line string at L244 that desyncs
the bracket-span lexer, which is the mechanism the docstring at `:1400-1412` actually describes. The
behaviour is correct and bounded either way.

### 3.7 Ambient failures I did NOT count against the branch

Under sabotage runs on my `git archive` export, `no-machine-local-build-cache-is-tracked`
(`repository_hygiene_test.clj:64`) and two `mcp_process_test` cases fail on every arm, including
arms whose sabotage cannot reach them. Both are precondition-sensitive to my export directory
(`.cpcache` written by my own `clojure` invocation) and to `PATH`, not to this branch. They are
green in the clone.

---

# 4. WHAT MUST HAPPEN BEFORE THIS IS GO

1. **B3** — union the automatic implementation leg's globs into `candidate-globs` before the walk;
   make its `N/A` reason name the seed it is about; never report `COMPLETE` when a leg was
   uncounted because it was out of scope rather than inapplicable. Witness: two definitions of one
   seed in two files, one outside every declared glob.
2. **B1′** — character-class state in the regex lexer; `/[/}]/` added to the lexer witness so it
   goes red first. A wrong range must never be labelled `closed`.
3. **B2′** — a hit whose enclosing top-level form is `(comment …)`, or which follows `#_`, is a
   comment mention; the same for a multi-line `/* … */`. Witness: plant the decoy, require
   `CANDIDATE`.
4. **3.1** — exclude the clock tail from `ensure-superset`'s substring test so `text_bytes` equals
   the delivered text on every run, not most of them; give the refusal path its own key name.
5. **3.2 / 3.3 / 3.4** — make the `next_call` hand-off obeyable or reword it; strip the `@`; dedupe
   `unreadable`.

---

# 5. HOUSEKEEPING

```
$ curl -s http://127.0.0.1:8127/healthz          # before teardown
{"ok":true,"server":"clj-surgeon","tool_runtime":"ready","tool_registry":"ready"}

$ PID=$(ss -ltnp | grep ':8127' | grep -o 'pid=[0-9]*' | cut -d= -f2); kill $PID
server pid=2402561
$ ss -ltn | grep ':8127' || echo "NO LISTENER ON 8127"
NO LISTENER ON 8127
$ curl -s --max-time 2 http://127.0.0.1:8127/healthz || echo "port 8127 closed"
port 8127 closed

$ rm -rf /var/tmp/forge/ft3-review-fx && ls -d /var/tmp/forge/ft3-review-fx
ls: cannot access '/var/tmp/forge/ft3-review-fx': No such file or directory

$ cd /home/forge/tmp/sol/ft1-wt && git status --porcelain; git rev-parse HEAD; git stash list
                                        <- empty
9139b2c5d269dbf1ca8d08d78d5af81943273a28
                                        <- no stashes
```

Ports 7888 / 7890 / 7894 / 7895 / 7906–7910 / 7941–8125 / 8129–8158 were never contacted; my server
ran on an explicit `:port 8127` and nothing else was dialled. The clone at
`/home/forge/tmp/sol/ft1-wt` is unchanged (`git status --porcelain` empty, `HEAD` still
`9139b2c5`); no commit, push, stash, or `git add` was run, and no source file in it was edited. All
sabotage was performed on `git archive` exports under `/var/tmp/forge/ft3-review-fx/opus`, which is
now removed. Fixtures never touched `/tmp`. JVM suites ran through `~/bin/suite-run`.

---

## NO-GO

**Mergeability:** `git merge-tree --write-tree HEAD origin/MCP/main` exits 0 and prints a single
trunk (`ec06ea6aa023cd27539dca4d3803e632ce6a42dd`, against `origin/MCP/main` at `217dfb27`) with no
conflict section, so **9139b2c5 merges cleanly** onto MCP/main and is green on all six gates — but
it is not GO on its own, because the receipt can report `COMPLETE (5 of 5)` while the definition
leg it exists to deliver is unread, giving a false reason that names an unrelated file and printing
an `rg` line it never ran over that file (B3), can still publish a truncated JavaScript body
labelled `brace-window(lexed,closed)` with a pre-image hash over it (B1′), and can still mark a
`(comment …)` form `FOUND` with no weak label (B2′) — and a false green that terminates
investigation is worse than a refusal.
