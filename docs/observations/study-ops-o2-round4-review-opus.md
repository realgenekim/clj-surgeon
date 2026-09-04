## NO-GO

Round-four reviewer: Opus (Sol's content filter refused this brief). Same brief,
substituted paths: verdict here, clone `/home/forge/tmp/sol/o2r3-wt`, fixtures
`/var/tmp/forge/o2r4-review-fx`, server port 8105.

---

### 1. Provenance and scope

I read Sol's round-three verdict first, then `git log --oneline e258519..515e8109`
and the full diffs of `mcp_inspect.clj`, `mcp_inspect_tool.clj`,
`mcp_operation.clj`, the intent diff and the round-four observation, before
probing. The clone was clean at `515e8109` on arrival and is clean now. Nothing
was committed, pushed, stashed, or `git add`ed. One server, explicit port 8105,
stopped. Fixtures under `/var/tmp/forge/o2r4-review-fx`, removed.

```sh
cd /home/forge/tmp/sol/o2r3-wt && git rev-parse HEAD && git status --short && git log --oneline e258519..515e8109
```

```text
515e8109c6b86725deddd4ee9c6dd604f78a7f3c
515e8109 study-ops: O2r4 — the intents say what the code now does, and the round-four observation
760bb151 study-ops: O2r4 GREEN (§5) — the fit measures the envelope the publisher publishes; no reserve
381ece52 study-ops: O2r4 RED (§5) — the 64-byte publish reserve is a constant, not an invariant
8c8e0969 study-ops: O2r4 GREEN (§4) — the allowance is what the budget leaves, and a drop names its leaves
4bb46422 study-ops: O2r4 RED (§4) — an allowance is derived from the budget, never fixed
56f98319 study-ops: O2r4 GREEN (§3, §9) — the refusal enumeration is the runtime, enforced at construction
7fd10495 study-ops: O2r4 RED (§3, §9) — a literal-shape scan is not the refusal ratchet
11fe6f53 study-ops: O2r4 GREEN (§2) — one exclusion mechanism, and a label carries a value-less leaf
0eb9849b study-ops: O2r4 RED (§2) — a value-less leaf is carried by its label or not at all
```

(`git status --short` emitted no line.)

---

### 2. BLOCKER — an ORDINARY two-file `outline` batch over clj-surgeon's own source now publishes a 151-character text block and carries NONE of its 1,137 receipt leaves, with 9,250 bytes of the public budget unspent. This is a round-four REGRESSION: the same call at `e258519` published 9,091 characters of text.

Files: `src/clj_surgeon/mcp_inspect_tool.clj:1999` (`fit-public-result`'s
bisection), `src/clj_surgeon/mcp_inspect.clj:743` (`fact-section`'s unbudgeted
`dropped:` line), `src/clj_surgeon/mcp_inspect.clj:696` (`fact-block`), intent
`docs/intent/study-ops/study-ops-specs.md:104` (MCP-OP-STUDY-044).

**Over the real MCP HTTP wire**, `tools/call inspect_clojure` with two ordinary
`outline` requests on `src/clj_surgeon/mcp_inspect_tool.clj` and
`src/clj_surgeon/mcp_inspect.clj` — the tool's own advertised batching use:

```sh
cd /home/forge/tmp/sol/o2r3-wt && if lsof -nP -iTCP:8105 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8105_BUSY; else echo PORT_8105_FREE; clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p19_http.clj; fi; if lsof -nP -iTCP:8105 -sTCP:LISTEN >/dev/null 2>&1; then echo PORT_8105_STILL_LISTENING; else echo PORT_8105_STOPPED; fi
```

```text
PORT_8105_FREE
server_port= 8105  url= http://127.0.0.1:8105/mcp
http_status= 200  session= true
published_bytes= 23518  budget=32768 headroom= 9250
text_chars= 151  text_omitted= notice
TEXT= "inspect_clojure\n! text omitted · the complete receipt left no room to render it\n→ the complete result is in structuredContent\n→ read_structured_content"
PORT_8105_STOPPED
```

The same call through the in-process callback entrance (`handle-inspect`), and
the same fixture at round three, side by side:

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p17_batch.clj
cd /var/tmp/forge/o2r4-review-fx/costclone && git checkout -q --detach e258519 && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p18.clj
```

```text
batch of 2 real files: ok=true  structured=23328  complete=46800  published=23511  headroom=9257   limit=nil     omitted="notice" uncarried=1137  text_chars=151
   TEXT= "inspect_clojure\n! text omitted · the complete receipt left no room to render it\n→ the complete result is in structuredContent\n→ read_structured_content"
batch of 3 real files: ok=false structured=33405  complete=67531  published=1073   headroom=31695  limit=nil     omitted=nil     uncarried=0     text_chars=473
batch of 4 real files: ok=false structured=42548  complete=85054  published=1073   headroom=31695  limit=nil     omitted=nil     uncarried=0     text_chars=473

e258519 batch of 2 real files: structured=23311 published=32717 headroom=51 limit=8715 omitted=nil uncarried=293 text_chars=9091
```

| batch of 2 real clj-surgeon files, `outline` | `e258519` (round 3) | `515e8109` (round 4) |
|---|---|---|
| published bytes | 32,717 (51 unspent) | 23,511 (**9,257 unspent**) |
| `content[0].text` | **9,091 chars** | **151 chars** |
| uncarried leaves | 293 of 1,137 | **1,137 of 1,137** |

The same shape on a synthetic-but-ordinary 140-defn namespace, swept:

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p10_outline.clj
```

```text
forms=60   structured=8909   complete=14104   published=14104  headroom=18664  limit=nil     omitted=nil      uncarried=0    text_chars=5042
forms=100  structured=14352  complete=22629   published=22629  headroom=10139  limit=nil     omitted=nil      uncarried=0    text_chars=8044
forms=140  structured=19794  complete=34082   published=19977  headroom=12791  limit=nil     omitted="notice" uncarried=863  text_chars=151
forms=180  structured=25234  complete=45978   published=25417  headroom=7351   limit=nil     omitted="notice" uncarried=1103 text_chars=151
forms=220  structured=30674  complete=58148   published=30857  headroom=1911   limit=nil     omitted="notice" uncarried=1343 text_chars=151
forms=260  structured=36136  complete=70052   published=1073   headroom=31695  limit=nil     omitted=nil      uncarried=0    text_chars=473
forms=300  structured=41656  complete=82107   published=1073   headroom=31695  limit=nil     omitted=nil      uncarried=0    text_chars=473
```

The same three sizes at `e258519`:

```text
n=140  structured=19777  published=31139  headroom=1629   limit=12927   omitted=nil  uncarried=0    text_chars=11005
n=180  structured=25217  published=32706  headroom=62     limit=6771    omitted=nil  uncarried=461  text_chars=7235
n=220  structured=30657  published=32690  headroom=78     limit=1455    omitted=nil  uncarried=833  text_chars=1921
```

At 140 forms round three carried **every** leaf in 11,005 characters. Round four
carries none in 151, and leaves 12,791 bytes of the declared budget unused.

**MCP-OP-STUDY-044 says three things this violates at once:** "the complete
rendering shall travel whenever it fits, and an elision shall happen only when
the complete rendering would not"; "the text shall declare how many of how many
it rendered"; and "shall NAME each dropped leaf by its JSON pointer". The notice
rung declares no count and names no leaf. MCP-OP-STUDY-041's "a result whose
receipt is truncated shall spell its `next_call` in the text" is also unmet here.

---

### 3. The root cause, isolated: round four's `dropped:` line is UNBUDGETED, which inverts the monotonicity `fit-public-result`'s bisection depends on, so the bisection searches the half that can never fit and returns nil.

`fact-block` (`mcp_inspect.clj:696`) charges only the fact LINES against its
budget and returns `:dropped-labels`; `fact-section` (`mcp_inspect.clj:724`, the line at `:743`)
then appends `"  dropped: " + (str/join ", " labels)` outside any allowance. The
more the allowance is LOWERED, the more labels that line carries, so the
rendering GROWS as `text_evidence_limit` shrinks:

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p11_cause.clj
```

```text
structured= 19795  budget= 32768  high= 12973
  limit=0      bytes=35120   fits=false text_chars=15270  dropped_line_chars=14732  text_without_dropped_bytes=20387  would_fit=true
      header= "  receipt facts · 0 of 406 rendered · the complete receipt is in structuredContent"
  limit=1000   bytes=35057   fits=false text_chars=15178  dropped_line_chars=13694  text_without_dropped_bytes=21362  would_fit=true
  limit=5000   bytes=33680   fits=false text_chars=13693  dropped_line_chars=8182   text_without_dropped_bytes=25497  would_fit=true
  limit=12974  bytes=31213   fits=true  text_chars=11076  dropped_line_chars=0      text_without_dropped_bytes=31212  would_fit=true
      header= "  receipt facts · 9 of 9 rendered"
```

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p12_mono.clj
```

```text
structured= 19794  search_high= 12974
  limit=12974  bytes=31211   fits=true
  limit=12973  bytes=31211   fits=true
  limit=10000  bytes=31544   fits=true
  limit=9000   bytes=31997   fits=true
  limit=8000   bytes=32453   fits=true
  limit=7000   bytes=32889   fits=false
  limit=6486   bytes=33113   fits=false
  fitting limits (step 250) = [7500 7750 8000 8250 8500 8750 9000 9250 9500 9750 10000 10250 10500 10750 11000 11250 11500 11750 12000 12250 12500 12750]
  FIT CHOSE: text_evidence_limit= nil  text_omitted= notice
```

The fitting allowances form a contiguous **upper** interval (≈7,300–12,974). The
bisection at `mcp_inspect_tool.clj:1999-2013` probes `mid = 6487` first, finds it
does not fit, and recurs into `[0, 6486]` — the half that can never fit —
returning `best = nil`. A rendering that fits at limit 8,000 with 315 bytes to
spare, and at 12,974 with 1,557 to spare, is discarded, and the tool falls
through `notice` → publishes 151 characters.

Two independent defects are stacked here and both need fixing: (a) the
`dropped:` line must be charged against the same allowance as the lines it
describes (and bounded — at 406 dropped leaves it is 14,732 characters on its
own); (b) `fit-public-result` must not assume a monotone `fits?`, or must
establish that monotonicity by construction. Note that (b) alone is not enough:
even a correct search only recovers a rendering that (a) made accidentally
available.

The synthetic form of the same failure, where structured content sits 1,728 bytes
under the budget and the text is abandoned entirely:

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p7_dropline.clj
```

```text
structured_only_bytes= 31040  budget= 32768
complete_rendering_bytes= 64163
  limit=0      bytes=34997   fits=false text_chars=3874   dropped_line_chars=2616   header="  receipt facts · 0 of 194 rendered · the complete receipt is in structuredContent"
FIT RESULT: text_evidence_limit= nil  text_omitted= notice  ok= false  error_type= synthetic
published_bytes= 31223  text= "inspect_clojure\n! text omitted · the complete receipt left no room to render it\n→ the complete result is in structuredContent\n→ read_structured_content"
uncarried_count= 208
fact-block at budget 0: shown= 0  total= 194  dropped= true  dropped_labels_chars= 2605
```

**No witness covers this.** `every-published-mode-text-carries-every-structured-content-leaf`
uses a one-file fixture whose complete rendering always fits;
`a-budget-abridged-text-declares-its-drop-and-is-never-terminal` uses a 200-file
`ls-tree` whose rows dominate and whose fit still lands. The whole middle band —
a receipt between roughly 19 KB and 32 KB with hundreds of leaves — is
unwitnessed, which is why 485 tests and 6,319 assertions are green over it. Any
fix must ship a witness at that band, on a real multi-file `outline` batch, and
that witness must be RED at `515e8109`.

---

### 4. Finding — §2 is NARROWED, not closed: the carriage predicate still accepts COINCIDENCE, so the witness that certifies "the text carries every leaf" cannot distinguish a carried leaf from an unrelated substring.

Files: `src/clj_surgeon/mcp_inspect.clj:585` (`leaf-rendered?`),
`src/clj_surgeon/mcp_inspect.clj:558` (`value-less-leaf?`),
`docs/intent/study-ops/study-ops-specs.md:104`.

Sol's four value-less shapes are genuinely fixed — every one now carries its
label — but `leaf-rendered?` still asks only `str/includes?` for everything else,
and a short scalar's spelling collides with any other number, flag or word in the
text. On a REAL `outline` receipt, `file_read_count` can be changed from 1 to 0
and the published text is BYTE-IDENTICAL, while `uncarried-leaves` reports zero
misses:

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p4_quant.clj
```

```text
== outline  ok=true  scalar_leaves=35  product_uncarried=0  INVISIBLE_LEAVES=9
     [:file_read_count]                                   1 can be replaced by 0 with a BYTE-IDENTICAL text
     [:results 0 :file_hash]                              "3add9a61…" can be replaced by "clj" with a BYTE-IDENTICAL text
     ...
== deps     ok=true  scalar_leaves=30  product_uncarried=0  INVISIBLE_LEAVES=7
     [:results 0 :omitted]                                0 can be replaced by 1 with a BYTE-IDENTICAL text
== topo     ok=true  scalar_leaves=26  product_uncarried=0  INVISIBLE_LEAVES=7
```

A dissoc-dependency audit says the same thing without inventing decoys — these
are the leaves the published text does not depend on at all:

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p5_dissoc.clj
```

```text
== outline                      ok=true  map_scalar_leaves=11  product_uncarried=0  TEXT_DOES_NOT_DEPEND_ON=4
     [:file_read_count]                             = 1
     [:read_complete]                               = true
     [:operation]                                   = "inspect_clojure"
     [:next_action]                                 = "none"
== ls-tree                      ok=true  map_scalar_leaves=16  product_uncarried=0  TEXT_DOES_NOT_DEPEND_ON=3
     [:omitted]                                     = 0
```

Of those, `:operation`, `:read_complete` and `:next_action` are tautological in
these receipts (the structural line spells the same word the leaf holds), so I do
not claim a caller-visible loss for them. `file_read_count` is a real receipt
fact whose label never appears and whose only "1"s in the text mean
`request_count` and `file_count` — a text-only caller cannot read it.

This is the same CLASS Sol blocked on in round three — renderer and witness
sharing an exclusion nobody enumerated — with the hole moved from
`{null, {}, [], ""}` to `{any value whose spelling occurs elsewhere in the text}`.
Honest qualification: the spec text sanctions it ("a number, boolean, or rendered
keyword as the characters `structuredContent` spells it with"), so this is code
matching its intent, and I found no case where the actual rendered value was
wrong. I therefore rank it a FINDING, not the blocker. But it means the O2 leaf
ratchet cannot go red on a renderer that drops a small integer, which is exactly
the property finding 2 needed it to have. Ratchet: extend the label requirement
from `value-less-leaf?` to any leaf whose spelling is short enough to collide, or
add the dissoc-dependency assertion above to the witness.

---

### 5. §3 verified sound — the refusal enumeration IS the runtime, and I could not escape it.

Files: `src/clj_surgeon/mcp_inspect.clj:68` (`refusal-reasons`, 23 members),
`src/clj_surgeon/mcp_inspect.clj:110` (`refuse!`'s construction check, the guard at `:118`),
`test/clj_surgeon/mcp_study_test.clj:3314`, `:3344`, `:3651`.

- The ratchet is `(= enumerated driven)` where `driven` is derived by driving one
  fixture per reason through the public entrance. A new reason needs both an edit
  to the set and a fixture; whichever is missing is a failing test. The literal
  scan survives only as a complement, with an explicit assertion that at least one
  enumerated reason stays invisible to it — so Sol's rung D can no longer hide
  anything, because the scan is no longer load-bearing.
- Rung (e) is present and is a red test:
  `a-refusal-cannot-invent-a-reason-outside-the-enumeration` asserts
  `IllegalArgumentException "not enumerated"` from BOTH `refuse!` directly and
  from the helper route `unique-strings!` — the exact escape Sol found.
- **Attack: a refusal carrying a `:reason` from another namespace.** The only
  `catch clojure.lang.ExceptionInfo` on the validation path is
  `mcp_inspect.clj:455`, and the only namespace-qualified calls inside that `try`
  are `mcp-contract/json-containers->clj` (outside the try) and
  `mcp-paths/relative-source-path?` (a pure predicate that cannot throw
  `ex-info`). `kernel-refusal` (`mcp_inspect.clj:746`) copies `:error-type` and
  twenty other fields but never `:reason`. The only other `:reason` the tool
  builds, `mcp_inspect_tool.clj:428`, is nested inside `unresolved_paths` entries,
  not a top-level refusal reason.
- **Attack: a multimethod default.** There is no `defmulti` in either
  `mcp_inspect.clj` or `mcp_inspect_tool.clj`.

One consequence worth stating, since it is new behaviour: an unenumerated reason
now throws a plain `IllegalArgumentException` out of `execute-inspect!` — the
`catch Exception` at `mcp_inspect_tool.clj:1677` is inside `capture-snapshots`,
not around validation — so the callback never fires. That is the documented
intent ("a defect in this namespace, not a bad request"), and it is the right
trade, but it is a crash rather than a refusal and should be said out loud.

---

### 6. §4's own fixture IS closed, and the 20,000-character clause is honest.

Sol's round-three §4 fixture, reproduced through the real entrance with a
10,000-character source path:

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p15_cause.clj
```

```text
cause=512    public_bytes=1444   <=budget=true  full_cause_in_text=true  uncarried=[]               declares_drop=false dropped_line=nil marker=false
cause=513    public_bytes=1990   <=budget=true  full_cause_in_text=true  uncarried=[]               declares_drop=false dropped_line=nil marker=true
cause=10000  public_bytes=20966  <=budget=true  full_cause_in_text=true  uncarried=[]               declares_drop=false dropped_line=nil marker=true
cause=20000  public_bytes=21090  <=budget=true  full_cause_in_text=false uncarried=[[:error]]       declares_drop=true  dropped_line="  dropped: error" marker=true

== SOL §4 FIXTURE REPRODUCED ==
  error_type= invalid-source-path  structured_bytes= 20526  text_chars= 11455  public_bytes= 32009  limit= 12270
  uncarried= [[:error]]  declares_drop= true  dropped_line= "  dropped: error"
  hypothetical_complete_public_bytes= 41977  fits= false
```

Round three dropped six leaves with 10,921 bytes unspent; round four drops
exactly `[:error]`, names it, and the complete rendering genuinely does not fit
(41,977 against 32,768). The moved MCP-OP-STUDY-046 clause is honest: 10,000
fits and is carried whole and unmarked-as-dropped; 20,000 is dropped, declared,
named, and inside the budget; 512 is unmarked and 513 carries the
`(513 characters)` marker while the complete cause still travels.

**Attacked further, as the brief asks:**
- *One leaf larger than the whole budget* (a 40,000-character `error`): the
  structured content alone exceeds the budget, so it becomes the typed
  `inspect-output-limit` refusal at 1,073 bytes. No mid-leaf cut, no partial
  value in the text (`text_contains_partial_error= false`). Correct.
- *200 tiny leaves*: facts are dropped WHOLE and named, never cut mid-line — but
  this is the fixture that exposed finding 2/3 above.

---

### 7. §5 verified closed — Sol's clock escape is gone, and the publisher adds nothing after the fit.

Files: `src/clj_surgeon/mcp_operation.clj:71` (the fit runs on the FINALIZED
result), `src/clj_surgeon/mcp_inspect_tool.clj:1908` (`max-fitted-result-bytes`
= the budget itself, no reserve), `:1511` (`with-envelope`), `:1984` (the typed
refusal when `:elapsed_ms` is absent).

```sh
cd /home/forge/tmp/sol/o2r3-wt && clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/p14_clock.clj
```

```text
== §5 CALLBACK ENTRANCE: fit_measure vs published, ordinary and maximal clocks ==
  payload=400  fit_measure=31594  normal_published=31571  equal=false maximal(1e308)=31900  bounded=true  longmax=31636  bounded=true
  payload=420  fit_measure=32554  normal_published=32531  equal=false maximal(1e308)=32760  bounded=true  longmax=32596  bounded=true
  payload=430  fit_measure=32535  normal_published=32512  equal=false maximal(1e308)=32761  bounded=true  longmax=32577  bounded=true
  payload=440  fit_measure=32768  normal_published=32763  equal=false maximal(1e308)=32634  bounded=true  longmax=32648  bounded=true
  payload=460  fit_measure=32690  normal_published=32667  equal=false maximal(1e308)=32718  bounded=true  longmax=32732  bounded=true

== §5 EXACT BOUNDARY on PUBLISHED bytes (MCP callback entrance) ==
  target=32768 found=536
  target=32769 found=nil
  max_published_ordinary=32767 max_published_1e308=32768 over_budget_count=0

== §5 MEASURED BLOCK WITH FIVE FIELDS through invoke! ==
  measured= {:elapsed_ms 1.0, :inspection_elapsed_ms 2.0, :job_elapsed_ms 3.0, :scan_ms 4.0, :queue_ms 5.0}  fields= 5  summarize==publish= true  serialize==publish= true  no_top_level_elapsed= true
```

- Sol's round-three escape (`huge_published= 32860 / 32841 / 32912 / 32996` at a
  `1.0E308` clock) is **gone**: across payloads 380–520, with both an ordinary
  clock and the largest the envelope's contract accepts, `over_budget_count= 0`
  and the maximum published size is exactly 32,768. 32,769 is unreachable.
- `Long/MAX_VALUE`-scale elapsed also stays bounded.
- The MEM-003 wire works with a five-field `measured` block: `invoke!` summarizes
  and serializes exactly the map the fit returned, and adds no top-level
  `elapsed_ms` afterwards.
- The `equal=false` column is MY measurement artifact, not a defect: I measured
  the standalone fit at `elapsed_ms 0.001` and the callback at a 1 ms clock. The
  invariant that replaces the reserve is structural — `invoke!` hands the
  finalized result to `fit`, then renders and serializes exactly what `fit`
  returned — and it holds.

---

### 8. The record corrections check out.

- The §3 RED assertion was inverted, and the correction is pinned with a comment
  explaining why: `a-refusal-reason-built-through-a-helper-is-in-the-ratchet`
  now asserts `(not (contains? scanned reachable))`
  (`test/clj_surgeon/mcp_study_test.clj:3651`).
- The test helper `public-bytes` now measures BOTH sides at one clock
  (`test/clj_surgeon/mcp_study_test.clj:2014`, using `clocked` for the text and
  the receipt), closing the 18-byte disagreement.
- The 10,000→20,000 clause is honest; see finding 6.

---

### 9. §6 small-read wire growth reproduced.

Rebuilt cost fixture (one three-defn namespace with a require), same fixture at
three revisions:

```sh
cd /var/tmp/forge/o2r4-review-fx/costclone && for rev in a0b0520 e258519 515e8109; do git checkout -q --detach $rev; echo "--- $(git rev-parse --short HEAD) ---"; clojure -M:clj-surgeon/mcp /var/tmp/forge/o2r4-review-fx/cost.clj; done
```

```text
--- a0b05206 ---
ls-tree  846
outline  1392
topo     1132
deps     1266
--- e258519e ---
ls-tree  935
outline  1725
topo     1398
deps     1526
--- 515e8109 ---
ls-tree  964
outline  1763
topo     1427
deps     1562
```

| mode | a0b0520 | e258519 (r3) | 515e8109 (r4) | r3→r4 | total |
|---|---|---|---|---|---|
| ls-tree | 846 | 935 | 964 | +29 | +14% |
| outline | 1,392 | 1,725 | 1,763 | +38 | +27% |
| topo | 1,132 | 1,398 | 1,427 | +29 | +26% |
| deps | 1,266 | 1,526 | 1,562 | +36 | +23% |

Confirms the claim: round four adds 0–38 bytes per small read on top of round
three's ~25%, and the largest absolute figure is ~2.7 KB against 32,768. Token
cost remains unmeasured, and bytes are not tokens.

---

### 10. Gates at the tip — all green, and none of them covers finding 2.

```sh
cd /home/forge/tmp/sol/o2r3-wt && ~/bin/suite-run bb test/run_all.clj
```
```text
Ran 731 tests containing 6023 assertions.
0 failures, 0 errors.
EXIT_CODE=0
```

```sh
cd /home/forge/tmp/sol/o2r3-wt && ~/bin/suite-run clojure -M:clj-surgeon/mcp-test
```
```text
Ran 485 tests containing 6319 assertions.
0 failures, 0 errors.
EXIT_CODE=0
```

```sh
cd /home/forge/tmp/sol/o2r3-wt && make mcp-operation-oracle
```
```text
# @spec MCP-OP-ORACLE-001
swipl -q -f test/mcp_operation_contract_oracle.pl
mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
EXIT_CODE=0
```

```sh
cd /home/forge/tmp/sol/o2r3-wt && ~/bin/suite-run clojure -M -e "(require '[clj-surgeon.mcp-intent-contract :as audit]) (let [r (audit/audit-current-repository)] (prn (select-keys r [:ok :spec-count :violations])))"
```
```text
{:ok true, :violations []}
EXIT_CODE=0
```

```sh
cd /home/forge/tmp/sol/o2r3-wt && git diff --exit-code 4480e3d..HEAD -- test-fixtures/
```
```text
EXIT_CODE=0
```

Every claimed number matches: 731/6023/0, 485/6319/0, oracle pass, audit ok,
fixtures untouched. I did not see the admission-wait flake the observation
mentions.

---

### 11. RED→GREEN lineage — every pair is red at its sha and green at the following fix.

```sh
cd /var/tmp/forge/o2r4-review-fx/r3clone && for spec in 0eb9849b:11fe6f53 7fd10495:56f98319 4bb46422:8c8e0969 381ece52:760bb151; do red=${spec%%:*}; green=${spec##*:}; for rev in "$red" "$green"; do git checkout --quiet --detach "$rev"; echo "REV=$(git rev-parse --short HEAD) $(git log -1 --format=%s)"; ~/bin/suite-run clojure -M:clj-surgeon/mcp-test 2>&1 | tail -25; done; done
```

```text
REV=0eb9849b study-ops: O2r4 RED (§2) — a value-less leaf is carried by its label or not at all
Ran 478 tests containing 6268 assertions.
12 failures, 0 errors.
REV=11fe6f53 study-ops: O2r4 GREEN (§2) — one exclusion mechanism, and a label carries a value-less leaf
Ran 478 tests containing 6268 assertions.
0 failures, 0 errors.
REV=7fd10495 study-ops: O2r4 RED (§3, §9) — a literal-shape scan is not the refusal ratchet
Ran 479 tests containing 6271 assertions.
2 failures, 0 errors.
REV=56f98319 study-ops: O2r4 GREEN (§3, §9) — the refusal enumeration is the runtime, enforced at construction
Ran 480 tests containing 6284 assertions.
0 failures, 0 errors.
REV=4bb46422 study-ops: O2r4 RED (§4) — an allowance is derived from the budget, never fixed
Ran 482 tests containing 6301 assertions.
9 failures, 0 errors.
REV=8c8e0969 study-ops: O2r4 GREEN (§4) — the allowance is what the budget leaves, and a drop names its leaves
Ran 482 tests containing 6302 assertions.
0 failures, 0 errors.
REV=381ece52 study-ops: O2r4 RED (§5) — the 64-byte publish reserve is a constant, not an invariant
Ran 485 tests containing 6319 assertions.
8 failures, 0 errors.
REV=760bb151 study-ops: O2r4 GREEN (§5) — the fit measures the envelope the publisher publishes; no reserve
Ran 485 tests containing 6319 assertions.
0 failures, 0 errors.
```

The lineage is honest. What it does not do — and this is the point of finding 2
— is exercise the band where the fix it installed goes wrong.

---

## NO-GO

`515e8109` is not GO on its own for MCP/main: an ordinary two-file `outline`
batch over the repository's own sources publishes a 151-character text block
carrying none of its 1,137 receipt leaves with 9,250 bytes of the budget unspent
— a regression from `e258519`'s 9,091-character rendering, caused by round
four's own unbudgeted `dropped:` line inverting the monotonicity
`fit-public-result`'s bisection assumes — and the recomposition onto the MEM-003
landing (`3692e9b`) must watch that the fit still measures the FINAL published
envelope after `elapsed_ms` moves under a nested `measured` block (the
`with-envelope` helper at `mcp_inspect_tool.clj:1511` copies only a top-level
`:elapsed_ms`, so every budget-gate SUBSTITUTE will silently lose the clock the
moment the wire nests it, and `fit-public-result`'s `(contains? raw-result
:elapsed_ms)` guard will throw on the very first request instead).
