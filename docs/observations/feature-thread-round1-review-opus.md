## NO-GO

Round-one independent review of `bridge/feature-thread-verb` at **02e823e7** (`feature_thread`, the
seventh MCP tool). Reviewer: Opus, forge@anvil. Sol's content filter refused this brief; paths were
substituted (verdict path, fixtures under `/var/tmp/forge/ft1-review-fx/opus`).

Every claim below was reproduced by running it. Nothing in the clone was modified.

```
$ cd /home/forge/tmp/sol/ft1-wt && git rev-parse HEAD && git status --porcelain
02e823e7bdefc8ecf7e5427ac0dd97ae24f0d120
                      <- porcelain empty, tree clean, verified again at the end of the review
```

Server: my own, on an explicit port 8127 (`clojure -X:clj-surgeon/mcp :port 8127 :nrepl-port :none
:telemetry :off`). Seven tools discovered live:

```
$ python3 cli.py tools/list
count 7
- inspect_clojure  - apply_clojure_changes  - edit_clojure  - transform_clojure
- alias_migration  - admit_clojure_patch  - feature_thread
```

---

## Headline

**The builder's five headline claims and all six gates reproduced EXACTLY.** The happy path is real
and the receipt is honest about itself: I recomputed sha256 over each leg's exact line range from
the file and every digest matched; every body was byte-identical to the slice it names; nothing was
read or published outside the workspace root under three separate escape attempts.

**The NO-GO is on adversarial input.** Two defects produce a FALSE GREEN with no signal a caller can
act on — the class that terminates investigation:

* **B1** — a JavaScript body containing a regex literal after `return` is returned with a **wrong
  range labelled `brace-window(lexed,closed)`**, truncated mid-statement, carrying a sha256 offered
  as an edit pre-image. This directly contradicts the namespace's own stated contract that its
  "single failure mode is *did not close*, which is loud."
* **B2** — a subject appearing **only in a comment** promotes a leg to `FOUND` and the thread to
  **`COMPLETE (5 of 5)`** while the real owner is never located. Demonstrated through the
  `route-assembled` evidence kind, which is the brief's own question.

Both are small, local fixes. This branch is one round from GO.

---

# 1. WHAT REPRODUCED EXACTLY

### 1.1 T1 — `formatDraft`, COMPLETE 5/5, ranges and hashes (claim 1) — REPRODUCED

The claimed ranges are the `smw-dequote` fixture (not `smw-dequote-after`; both were run). On
`smw-dequote` the live verb returns precisely the claimed thread:

```
$ feature_thread {"subject":"formatDraft",
                  "also":["/api/transform/format","mechanical-format"],
                  "scope":{"workspace_root":".../test-fixtures/feature-thread/smw-dequote"}}
BEFORE-fixture status: COMPLETE (5 of 5)
  menu-caller FOUND src/writer/views/components.clj          102 113
  js-function FOUND resources/public/js/editor-commands.js   389 454
  route       FOUND src/writer/routes.clj                   2148 2148
  handler     FOUND src/writer/handlers/transform.clj        606 680
  tests       FOUND test/writer/handlers/transform_apply_test.clj 349 384
```

Attack: recompute sha256 over each leg's exact range from the file, and compare each BODY
byte-for-byte with the slice.

```
menu-caller sha True body True bytes True
js-function sha True body True bytes True
route       sha True body True bytes True
handler     sha True body True bytes True
tests       sha True body True bytes True
```

Same check on `smw-dequote-after` (ranges L102-115 / L389-454 / L2148 / L622-696 / L349-384): all
five `SHA OK | BODY OK`. **No digest and no body is wrong anywhere I looked.**

### 1.2 The JS lexer (claim 2) — REPRODUCED on all six named cases

Fixture `jsfx/resources/public/js/attacks.js` (537 lines) built from the brief's own list:

```
attackRegex                L2-6   boundary=brace-window(lexed,closed)          bleed=False
attackTemplate             L9-12  boundary=brace-window(lexed,closed)          bleed=False
attackCommentInString      L15-18 boundary=brace-window(lexed,closed)          bleed=False
attackUnterminated         L1-61  boundary=line-window(+/-40, unclosed at L21) bleed=True
attackDivision             L27-31 boundary=brace-window(lexed,closed)          bleed=False
attackLong (504-line fn)   L1-73  boundary=line-window(+/-40, unclosed at L33) bleed=True*
```

`}` inside a regex, `${ {a:1} }`, `/*` inside a string, `a / b / 2` — all closed at the exact
function. The unterminated comment and the >400-line function downgrade to a **labelled** window.
Six for six. (The seventh case, below, is not in that list — and it is B1.)

### 1.3 Budget and elision (claim 3) — REPRODUCED

| request | result |
|---|---|
| `budget_bytes: 10240` | text 10,081 B ≤ 10,240; five cuts, each labelled |
| `budget_bytes: 1` | typed `feature-thread-budget-exceeded` |
| `budget_bytes: "lots"` | typed `feature-thread-invalid-budget` |
| `budget_bytes: 40000` | typed `feature-thread-budget-above-cap`, "never silently clamped" |

Elision order observed exactly as declared — `secondary-witnesses → tests → sibling → menu-caller →
js-function`, handler never cut — and every cut prints leg, bytes, range, sha256 and an executable
refetch:

```
elided tests 1997B reason=public-budget range=L349-L384 sha256:3f09f5ff...
  refetch=nl -ba test/writer/handlers/transform_apply_test.clj | sed -n '349,384p'
```

**MCP-OP-THREAD-012 (text ⊇ structured) holds on every receipt I produced**, refusals included. I
walked every scalar leaf of `structuredContent` and looked for its Clojure `str` spelling in the
text block: `MISSING: []`.

### 1.4 Typed refusal on an unreadable leg (claim 4) — REPRODUCED

```
$ chmod 000 fx-chmod/src/writer/handlers/transform.clj
status INCOMPLETE (4 of 5)
  handler ABSENT  unreadable: src/writer/handlers/transform.clj (unreadable)
text contains "COMPLETE (5 of 5)": False
```

### 1.5 Path confinement — HELD under three attacks

* A leg file that is a **symlink out of the root** (`src/writer/handlers/leaked.clj →
  /var/tmp/.../outside-secret.clj` containing `SECRET_CANARY_XYZ formatDraft`):
  `CANARY in text: False`, `outside path published: False`. The walk canonicalises and drops it.
* A **symlinked directory** pointing at the parent of the root, and a self-referential `..` symlink:
  no leak, no hang (0.3 s, terminated).
* A convention set whose globs are `["../../../../etc/*", "/etc/passwd", "../*"]`:
  `CANARY: False | /etc leak: False`; the rendered search quotes the glob but reads nothing.

The reason it holds is structural and worth stating: `read-source`
(`src/clj_surgeon/mcp_feature_thread.clj:244`) always re-joins `(io/file root relative)`, and
`relative` only ever comes from the canonicalised walk. **No read outside the root, no path
published outside it.**

### 1.6 Conventions refusals — all typed and bounded

```
malformed EDN   -> feature-thread-conventions-invalid ".clj-surgeon/feature-thread.edn did not read as EDN: Unmatched delimiter: ]"
1 leg           -> feature-thread-conventions-invalid "must declare exactly five leg roles ...; found 1"
file absent     -> feature-thread-conventions-absent   searched ['.clj-surgeon/feature-thread.edn']
unknown field   -> feature-thread-unknown-field        "feature_thread does not accept: evil"
bad root        -> invalid-workspace-root
```
`edn/read-string` is `clojure.edn` (no reader eval). Schema is `additionalProperties: false`,
`readOnlyHint: true`, `destructiveHint: false`.

### 1.7 Gates (claim 6) — ALL REPRODUCED VERBATIM

| gate | claimed | measured |
|---|---|---|
| `make mcp-operation-oracle` | pass | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` exit 0 |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | 728 / 8856 / 0 | **`Ran 728 tests containing 8856 assertions. 0 failures, 0 errors.` EXIT=0** |
| `~/bin/suite-run bb test/run_all.clj` | 814 / 6724 / 0 | **`Ran 814 tests containing 6724 assertions. 0 failures, 0 errors.` EXIT=0** |
| `make mcp-smoke` | seven tools | `{:ok true ... :tools [... "admit_clojure_patch" "feature_thread"]}` exit 0 |
| `make repository-hygiene` | clean | `repository hygiene: no machine-local build cache is tracked at any depth` exit 0 |
| intent audit | 363 specs / 0 violations | **`:ok true :specs 363 :violations 0`** |

### 1.8 The thirteen six→seven witnesses — HONEST, with one undeclared rider

I read every hunk of `git diff 02e823e7^ 02e823e7 -- test/ src/…` (excluding the new test file).
**Every changed assertion keeps exact-list equality** — none was relaxed to `contains?`, `some`, or
`>=`. Counts move 6→7 and 7→8 consistently; `exposes-exactly-six-typed-tools` was *renamed* to
`…-seven-…` rather than loosened; `http-protocol-exposes-five-tools…` likewise. The Prolog oracle
gained `required_outcome(feature_thread, receipt|typed_refusal)` and
`forbids_job_clock(receipt)` — additive, and the registry test names two real witness Vars.

**One change is not a tool count and is not in the commit message** — see finding 6.

---

# 2. BLOCKING FINDINGS

## B1 — a WRONG range labelled `brace-window(lexed,closed)`: `return /…/` lexes as division

`src/clj_surgeon/mcp_feature_thread.clj:310` — `regex-preceding-chars` is a set of *punctuation*.
A `/` preceded by an identifier character is treated as division. `return`, `typeof`, `case`, `in`,
`of`, `new`, `delete`, `void`, `yield`, `do`, `else` all end in an identifier character, so a regex
literal after any of them is lexed as division — and a `}` inside that regex is then counted as a
closing brace.

Fixture `jsfx/resources/public/js/attacks2.js`:

```js
function trapReturnRegex(s) {
  return /[}]/.test(s);
}
function AFTER_trapReturnRegex() { return 'sentinel_A'; }
```

Live call:

```
$ feature_thread {"subject":"trapReturnRegex","scope":{"workspace_root":".../jsfx"}}
trapReturnRegex FOUND 1 2 brace-window(lexed,closed)
  body: 'function trapReturnRegex(s) {\n  return /[}]/.test(s);'
```

The body is **truncated mid-function**, missing its closing brace, and it is labelled **`closed`** —
the label the design reserves for a range it is confident in. A sha256 over that truncation is
published as the pre-image an edit should assert against.

The namespace's own docstring (`:298-308`) states the opposite as a guarantee:

> "its single failure mode is 'did not close', which is loud … A wrong guess walks into a phantom
> regex and never closes, so it fails INTO the downgrade rather than into a wrong answer."

That reasoning covers only one direction of the ambiguity (division read as regex → runs on →
downgrade). **The other direction — regex read as division — fails into a wrong CLOSED answer**, and
`return /re/` is among the most common shapes in real JavaScript. The control case
`function trapAfterParen(s) { const ok = s.match(/x}y/); return ok; }` closes correctly (`(` is in
the set), which isolates the cause precisely.

**The oracle existed and missed it.** `test/clj_surgeon/mcp_feature_thread_test.clj:416-418`:

```clojure
(is (= (count "function a() { const r = /\\d{2}/; return r; }")
       (lex-close "function a() { const r = /\\d{2}/; return r; }"))
    "a brace inside a regex literal closes nothing")
```

Every regex in that witness is preceded by `=` — a character already in the set. Per house doctrine,
correcting the oracle belongs in the same fix.

**Fix:** make the preceding-token check keyword-aware (scan back over the identifier and treat a
JS keyword as regex context), and add the `return /[}]/` case to the lexer witness so it goes red
first.

## B2 — a comment-only mention promotes a leg to FOUND and the thread to COMPLETE (`route-assembled` can produce a FALSE leg)

This is the brief's own question about `route-assembled` (`searches-for-kind`,
`src/clj_surgeon/mcp_feature_thread.clj:769-772`). The search is built from the last two route
segments as adjacent quoted words — for `/api/transform/format`, the literal `"transform" "format"`.

Fixture `routefx`: I replaced the route entry with an assembled call so the literal is gone, and
planted one decoy comment at the end of `routes.clj`:

```clojure
(assembled-route ["api" "transform"] "format" #'transform/handle-format)
;; note: the old names were "transform" "format" before the rename
```

Live call:

```
$ feature_thread {"subject":"formatDraft","also":["/api/transform/format"],
                  "scope":{"workspace_root":".../routefx"}}
status COMPLETE (5 of 5)   route_handler None
route leg: FOUND 2370 2379  evid=route-assembled  boundary=line-window(no-enclosing-top-level-form)
body: '   ;; Dev reload endpoint\n   ["/dev/reload-check" {:get {:handler ...'
searches: ['route-assembled: rg -n -e \'\Q"transform" "format"\E\' -g \'src/**/routes.clj\' ...']
```

The receipt reports the **dev reload endpoint** as the route leg for Dequote/Format, and reports the
thread **COMPLETE (5 of 5)**, while the real route entry was never found and `route_handler` is
`None`. The brief's blocking criterion — *"a receipt that claims COMPLETE while a leg is missing or
wrong"* — is met.

**The class is wider than `route-assembled`.** Every search is `re-find` over a raw line
(`scan`, `:787`) with no lexical context. A subject that exists only in comments and strings:

```
$ feature_thread {"subject":"ghostFeature", ...}   # planted in 3 comments + 1 JS string
=== ghostFeature status: INCOMPLETE (3 of 5)
  menu-caller FOUND components.clj 250-259  evid=identifier-or-route  boundary=line-window(no-enclosing-top-level-form)
      hit line: ';; TODO: someday add ghostFeature to the Edit menu'
  js-function FOUND editor-commands.js 466-507 evid=identifier(def)   boundary=line-window(+/-40, unclosed at L506)
      hit line: 'const NOTE = "ghostFeature is not implemented";'
  handler     FOUND transform.clj 927-936     evid=identifier-or-route boundary=line-window(no-enclosing-top-level-form)
      hit line: ';; ghostFeature will live here one day'
```

Note the **`evid=identifier(def)` on a string constant**. That label is affirmatively wrong:
`alias-hop` (`:840`) falls through its `:else` branch and stamps `"identifier(def)"` on a hit that
matched only the bare-identifier fallback search, never the definition-shaped one. A caller reading
`FOUND … evid=identifier(def)` is told a string literal is the function definition.

Existing witness `no-false-members-in-the-receipt` (`:177`) checks only that no *file* outside an
allowed set is named. It cannot see a false leg inside an allowed file, which is exactly this case.

**Fix (either is sufficient to unblock):** (a) make a hit whose boundary is a line window — or whose
evidence is `route-assembled` / `route-tail` / bare `identifier` — a distinct status
(`FOUND(weak)` / `CANDIDATE`) that does **not** count toward COMPLETE; or (b) at minimum, refuse to
report `COMPLETE` when any leg's only evidence is a non-parsed window, and stop labelling a
fallback-search hit `identifier(def)`.

---

# 3. NON-BLOCKING FINDINGS

### 3. The stale pre-image "REFUSAL" is a printed instruction, not a control — RULED

The receipt's last line and the tool description both assert:

> "before any edit, re-hash each leg's range and compare to its sha256; **a mismatch is a REFUSAL
> (stale pre-image), never a retry**"
> — `src/clj_surgeon/mcp_feature_thread.clj:1127`, `:1750`

Nothing enforces it. `feature_thread` is read-only. The only pre-image binding in the trunk is
`admit_clojure_patch`'s `expect_pre_sha256` (`src/clj_surgeon/mcp_admit_tool.clj:83`,
`pre-image-binding-refusal` `:1326`), and that field is **`{file → sha256}` over whole files** — a
different digest subject from this receipt's **per-line-range** digests. So the receipt's hashes are
not consumable by the one verb that could enforce them, and the receipt emits no `next_call`
carrying anything that is.

**Ruling: a printed instruction is not a control.** The sentence states a refusal that no code can
issue, in the imperative voice the rest of this receipt reserves for things it actually does. It is
the `verify:none` failure shape — a rule addressed to a caller who is free to ignore it, presented
as a gate. Either reword it as advisory ("re-hash before editing; this verb cannot enforce it"), or
wire it: emit `next_call` with `expect_pre_sha256` as whole-file digests admit can bind, and keep
the range digests as the human-checkable detail.

### 4. No admission ceiling on `subject` / `also` length

`admit` (`:1476`) checks that `subject` is a non-blank string and nothing more. A 10,001-character
subject is **not** refused at admission: it is compiled into a regex, scanned across the tree
(333 ms), and only then refused — as a *budget* error, with the caller's field never named:

```
subject 10001 chars -> feature-thread-budget-exceeded
  "the receipt is 91644 bytes with every body elided, above the budget of 16288"
  status INCOMPLETE (0 of 5)   text 10,325 B
```

The brief asked for "a typed, bounded refusal." It is typed and the output is bounded, but the
refusal names the wrong field and the work is done before it fires. Add a `max-subject-chars`
ceiling next to the other named ceilings at `:41-103`, refused with the field named.

### 5. Two receipt figures are wrong about themselves

* **`text_bytes` under-reports the delivered text.** The fixpoint in `measure` (`:1337`) runs before
  `summary` appends the elapsed line and before `elapsed_ms` grows the `structured-only ·`
  completion line. Measured on the T1 receipt: `text_bytes 15392`, actual delivered text
  **15435 B**, of which the elapsed tail is 18 B — so 25 B are unaccounted. The 96-byte
  `elapsed-reserve-bytes` absorbs it, so **the budget is never actually breached** (I confirmed
  10,081 ≤ 10,240 and 15,435 ≤ 16,384), but the number the receipt prints about itself is not the
  number a caller can verify.
* **A refusal quotes the reserve-adjusted budget, not the caller's.** `budget_bytes: 1` refuses with
  `"above the budget of -95"`. A negative budget in a user-facing refusal is a leak of an internal
  subtraction; quote `1` and name the reserve separately.
* In the same refusal path, `:receipt_bytes` is set to the *text* byte count, whereas everywhere
  else `receipt_bytes = text + structured`. Same key, two meanings.

### 6. `workspace_onboarding.clj` quietly enables a WRITE tool in every managed block

`src/clj_surgeon/workspace_onboarding.clj:297` — the managed Codex block's `enabled_tools` changed
from five tools to **seven**, adding `feature_thread` *and* **`admit_clojure_patch`**:

```
-enabled_tools = ["inspect_clojure", "apply_clojure_changes", "edit_clojure", "transform_clojure", "alias_migration"]
+enabled_tools = ["inspect_clojure", ..., "alias_migration", "admit_clojure_patch", "feature_thread"]
```

`admit_clojure_patch` is a commit-capable tool and has nothing to do with this commit. The commit
message describes only "the catalog gained a seventh tool." This is a managed block that is
installed out of band into every onboarded workspace, so the change reaches boxes that never read
this diff. Split it out, or name it in the commit message and say who ratified it. Not blocking for
the verb, but it should not ride in silently.

### 7. Minor observations (no action required)

* `walk-relative-paths` (`:206`) tests containment with `(str/starts-with? abs root-path)` — no
  separator. A sibling directory `<root>-evil` would pass the prefix test; it is harmless only
  because `read-source` re-joins under `root` and the file then reads `:absent`. Append a separator
  anyway.
* The same walk does not de-duplicate: a `..` symlink inside the tree makes the canonical path of a
  file appear many times in `:paths`, so the same file is scanned repeatedly. Bounded and harmless
  in the case I built (0.3 s), but `(vec (sort (distinct @acc)))` costs nothing.
* `found-leg` (`:935`) reports `:searches [(last searches)]` — only the search that hit. For an
  `ABSENT` leg every search is quoted, which is where it matters; noted for completeness.

---

# 4. RULING ON ad49908c (requested addendum)

```
$ git fetch origin bridge/feature-thread-verb && git log --oneline 02e823e7..origin/bridge/feature-thread-verb
ad49908c tweezer r2-1: conventions may declare more than five legs; :def legs recognise Clojure defn/def
```

**Change A — `(= 5 (count (:legs …)))` → `(<= 5 (count …))`, message "exactly five" → "at least the
five".** This does not *weaken* a witness by relaxing an assertion — it **breaks** one and leaves it
red. `test/clj_surgeon/mcp_feature_thread_test.clj:388` asserts the refusal message contains
`"exactly five"`. I ran the suite at that tip, in a clone of the clone under my fixture directory:

```
$ cd /var/tmp/forge/ft1-review-fx/opus/ad49-wt && git rev-parse HEAD
ad49908cc87f1f5932dc68331a247598cb9f6e96
$ ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
FAIL in (conventions-are-data-and-their-absence-names-the-path) (mcp_feature_thread_test.clj:388)
a convention set that is not five leg roles is refused
expected: (str/includes? (:error structured) "exactly five")
  actual: (not (str/includes? "the convention set at inline must declare at least the five leg roles under :legs; found 4" "exactly five"))
Ran 728 tests containing 8856 assertions.
1 failures, 0 errors.
```

**The branch tip is RED.** (Same suite at 02e823e7: `728 / 8856 / 0 failures`.) The behaviour change
itself is sound — `thread-status` (`:1154`) computes `COMPLETE (n of m)` from `(count legs)` and is
never a literal, so a six-leg convention set reports honestly — but the commit shipped a src change
without the witness that pins it. It also loosens an admission bound with no new upper ceiling: the
declared legs now bound only by `max-scanned-files` on the resulting glob set.

**Change B — `:def` definition-shaped search gains `\(defn?-? +ident\b` as its first alternative.**
This **weakens no witness**: `scan` is `re-find` per line, so alternation order does not change
which lines match, and the SMW fixture's `:def` leg globs are JS-only, so T1/T2/T3 are byte-identical
before and after (I confirmed the receipt is unchanged). Two notes for round two: `alias-hop`'s
`real-re` (`:851`) was **not** given the new Clojure alternative, so a genuine `(defn foo …)` hit
falls through to the `:else` branch and is stamped `identifier(def)` without the hop ever
recognising it — the same mislabel as B2; and a Clojure `:def` leg now routes through `clojure-body`
(parsed, exact), which is correct and should get a witness of its own.

**Ruling: neither change weakens a witness. Change A breaks one and left it red, so the tip is not
mergeable as it stands** — restore the assertion to the new message text in the same commit as the
behaviour change.

---

# 5. WHAT MUST HAPPEN BEFORE THIS IS GO

1. **B1** — keyword-aware regex/division disambiguation; add `return /[}]/…` to the lexer witness so
   it goes red first. A wrong range must never be labelled `closed`.
2. **B2** — a leg whose only evidence is a non-parsed line window (or `route-assembled` /
   `route-tail` / bare `identifier`) must not count toward `COMPLETE`; stop stamping
   `identifier(def)` on fallback-search hits. Extend `no-false-members-in-the-receipt` to catch a
   false leg *inside* an allowed file (plant a comment mention; the receipt must go INCOMPLETE).
3. **Finding 3** — reword the pre-image line as advisory, or emit a `next_call` with a
   `expect_pre_sha256` map `admit_clojure_patch` can actually bind.
4. **Finding 4** — a named `subject` / `also` length ceiling refused at admission.
5. **Finding 5** — make `text_bytes` describe the delivered text; quote the caller's budget in the
   refusal; one meaning for `receipt_bytes`.
6. **Finding 6** — split or declare the `admit_clojure_patch` addition to the managed onboarding
   block.
7. **ad49908c** — restore the `mcp_feature_thread_test.clj:388` assertion; get the tip back to green.

---

# 6. HOUSEKEEPING

```
$ curl -s http://127.0.0.1:8127/healthz   # before teardown
{"ok":true,"server":"clj-surgeon","tool_runtime":"ready","tool_registry":"ready"}
```
Teardown receipts:

```
$ kill <server pid>; ss -ltnp | grep ':8127'
NO LISTENER ON 8127
$ curl -s --max-time 2 http://127.0.0.1:8127/healthz
port 8127 closed
$ rm -rf /var/tmp/forge/ft1-review-fx && ls -d /var/tmp/forge/ft1-review-fx
ls: cannot access '/var/tmp/forge/ft1-review-fx': No such file or directory
$ cd /home/forge/tmp/sol/ft1-wt && git status --porcelain; git rev-parse HEAD; git stash list
                                        <- empty
02e823e7bdefc8ecf7e5427ac0dd97ae24f0d120
                                        <- no stashes
```

Ports 7888 / 7890 / 7894 / 7895 / 7906–7910 / 7941–8125 / 8129–8149 were never
contacted. The clone at `/home/forge/tmp/sol/ft1-wt` is unchanged (`git status --porcelain` empty,
`HEAD` still `02e823e7`); no commit, push, stash, or `git add` was run, and no source file was
edited. All sabotage was performed on `git archive` exports under
`/var/tmp/forge/ft1-review-fx/opus`.

---

## NO-GO

**Mergeability:** `git merge-tree --write-tree HEAD origin/MCP/main` exits 0 and prints a single
tree (`65f72cfd91faba25d27daec6e59ec818352ee87d` against `origin/MCP/main` at `8aa45491`) with no
conflict section, so 02e823e7 merges **cleanly** onto MCP/main — but it is not GO on its own,
because the receipt can report `COMPLETE (5 of 5)` over a leg found only in a comment (B2) and can
publish a truncated JavaScript body labelled `brace-window(lexed,closed)` with a pre-image hash over
it (B1), and a false green that terminates investigation is worse than a refusal.
