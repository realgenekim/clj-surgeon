# Transformation walls of text — eight redesign exemplars

## Verdict

The win is not a shorter spelling of `find` and `replace`. It is a closed JSON
operation language in which the server owns the snapshot, labels, copied source,
counts, and retry state, while the model emits only the delta.

Across the eight largest distinct transformation shapes, the honest outcome is
bimodal:

- when the source already exists in a read or refused write, label-addressed
  verbs or splices reduce one successful request by **10.8–24.0×**, and a retry
  chain by **43.5–96.1×**;
- when the source did not exist upstream, representation cannot repeal
  authorship: the largest such wall falls only **1.96×**, from 28,537 bytes to
  a 14,575-byte synthetic-twin request whose 13,923-byte decision dominates it.

The recommended wire is therefore **typed JSON verb arguments with a closed
label-addressed splice escape hatch**. It keeps constrained decoding on the
JSON arguments, where the measured caller record is strongest. It does not put
EDN, a unified diff, or an arbitrary program inside a string.

No model was called for this study.

## Method and privacy boundary

This pass reuses the inclusive window and privacy contract from
[the 2026-08-29 emission study](2026-08-29-write-side-emission-and-read-side-encoding-study.md):
`2026-08-22T00:00:00Z` through `2026-08-30T02:09:33.141926Z`.

The sibling walls-of-text lane supplied the authoritative extraction after this
pass had started. Its receipt covers 247 structured writes: 195
`apply_clojure_changes` calls and 52 `edit_clojure` calls. The Top 20 are ranked
by compact canonical payload bytes, with SHA-256 as the tie-break. Exact token
counts use `tiktoken 0.11.0`, `o200k_base`. Decode seconds are a projection:

```text
canonical UTF-8 bytes × 3.5237 ms/byte ÷ 1000
```

The sibling receipt corrected one preflight assumption: insertion fields can
contain arrays. Its final leaf rule counts direct string values and direct
string array members under `insert_after` and `insert_before`, but not structured
insertion objects. That rule reproduces all six prior service field totals and
the 630,138-byte service checksum.

### How eight exemplars were selected

The eight panels are the first eight distinct delta families in the sibling
Top 20. Adjacent near-identical retry attempts are one exemplar, not separate
design problems:

| Exemplar | Real ranks and hash prefixes | Distinct shape |
|---|---|---|
| E1 | 1–3: `1410ea4c5402`, `5a5d90602f4d`, `f453dbabbda2` | five-target mixed batch; three-attempt chain |
| E2 | 4: `e8cbc63a1b83` | five paired replacements plus seven insertion leaves |
| E3 | 5: `36d95f176d5f` | four paired targets plus two insertion leaves |
| E4 | 6–9: `1d038cd880bc`, `5cea2f5bcfcb`, `747dd2c62815`, `d29368f6d656` | extraction-like split and insertion; four-attempt chain |
| E5 | 10: `f28f442c4891` | sixteen insertion leaves, no old-text leaf |
| E6 | 11: `8756522402af` | one large insertion plus two paired extensions |
| E7 | 12: `82d43204c2f5` | four paired targets, one 604→4,960-byte expansion |
| E8 | 13–14: `1dfbe10effbf`, `94e37e581690` | three paired extensions plus one insertion leaf; two-attempt chain |

### Mandatory privacy device

Every illustration below is a **synthetic structural twin**. Each twin preserves
the real payload's power-of-two byte class, JSON tree shape, leaf counts,
source/destination size classes, line-count class, and edit-run structure. All
names, paths, keywords, strings, numbers, and code are invented. Ellipses
abbreviate invented twin text, never private text.

Real payloads appear here only as byte counts, token counts, projected seconds,
success/refusal state, ranks, and hash prefixes. No real path, owner, source,
destination, prose, timestamp, session identifier, or reversible fragment is
present.

`DECIDED` uses the prior study's exact priority rule: remove rule-generated
grammar and guards first; then remove bytes copyable from mission reads or the
immediately previous write/refusal; what remains is information no available
upstream source supplied. Theoretical minima are lower bounds, not promised
encodings.

The Infinite Freedom byte/token counts are exact for the fully materialized
synthetic twins. The displayed snippets abbreviate their invented decision
material. Each full twin carries the exemplar's measured `DECIDED` byte count
inside the shown operation fields, so it cannot win by replacing novel code
with an uncharged placeholder.

---

## E1 — the 29.6 KB mixed batch, emitted three times

### 1. TODAY

The twin has five change objects: one insertion-only target and four guarded
`find`→`replace` targets. The paired bodies are 1,340→1,497 bytes,
5,551→5,717, 4,017→5,061, and 2,032→2,121, with 2, 7, 49, and 3 non-equal
edit runs respectively.

```json
{
  "changes": [
    {"files":["src/nebula/relay.clj"],"forms":["open-gate"],
     "insert_after":"(defn invented-probe ... ; 12 invented lines)"},
    {"files":["src/nebula/relay.clj"],"forms":["open-gate"],
     "find":"(defn open-gate ... ; 26 invented lines)",
     "replace":"(defn open-gate ... ; 30 invented lines)","expect":1,"id":"twin-a"},
    "... three more invented paired targets ..."
  ],
  "expect":{"changes":5},
  "verify":"exact"
}
```

| Real original | Outcome | Bytes | Tokens | Projected decode |
|---|---:|---:|---:|---:|
| `1410ea4c5402` | success | 29,598 | 7,903 | 104.294 s |
| `5a5d90602f4d` | refused | 29,592 | 7,902 | 104.273 s |
| `f453dbabbda2` | refused | 29,591 | 7,901 | 104.270 s |
| **chain** | — | **88,781** | **23,706** | **312.837 s** |

### 2. INFINITE FREEDOM

The parsimonious twin names the snapshot labels and emits four local operations;
it never echoes the four old bodies. One high-entropy rewrite remains as
invented source material inside the relevant `splice`.

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"merge-map","path":[":transport"],
   "with":{":connect-ms":2750,":idle-ms":9100}},
  {"at":"L2","do":"splice","after":"body/3","insert":"... invented ..."},
  {"at":"L3","do":"rewrite","rule":"thread-result-through-audit"},
  {"at":"L4","do":"splice","before":"body/9","insert":"... invented ..."}
]}
```

Full twin wire: **1,437 bytes / 325 tokens**. That is **20.60×** smaller than
the successful original and **61.78×** smaller than the three-call chain.

### 3. THEORETICAL MINIMUM

The successful call contains **1,126 DECIDED bytes**: 539 novel subject bytes
and 587 novel destination bytes. In twin prose, the decision is: four
target-local changes, including two new timeout values and one medium novel
rewrite, totaling about 1.1 KB. The remaining 28,472 bytes were upstream-copyable
or mechanically generated under the registered methodology.

The 311-byte gap between the 1,126-byte floor and the 1,437-byte twin is
addressing and operation grammar.

### 4. LLM-NATURAL

1. **Closed JSON verb arguments plus a typed splice — winner.** The easy map
   update stays semantic; only the irreducible rewrite pays source carriage.
2. **All-JSON label splices.** Reliable and general, but 61 edit runs create
   more coordinates and more opportunities to select the wrong local boundary.
3. **Unified-diff string.** Familiar to coding models, but it moves syntax and
   subject validation out of constrained JSON into an unconstrained text blob.

## E2 — the honest hard case: 13.9 KB was actually decided

### 1. TODAY

The twin contains five paired `find`→`replace` targets and seven direct
insertion leaves. The five paired destinations are 658, 2,179, 1,399, 2,311,
and 6,303 bytes. Four are mostly extensions; the last is a 63-run restructure.

```json
{"changes":[
  {"files":["src/orbit/harbor.clj"],"owner":"admit-vessel",
   "find":"(defn admit-vessel ...)","replace":"(defn admit-vessel ...)",
   "insert_after":["(defn invented-meter ...)","(defn invented-quota ...)"]},
  "... four more invented paired targets and five insertion leaves ..."
],"expect":{"changes":5},"verify":"exact"}
```

Real original `e8cbc63a1b83` was refused: **28,537 bytes / 6,768 tokens /
100.556 projected seconds**.

### 2. INFINITE FREEDOM

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"splice","after":"body/2","insert":"... invented ..."},
  {"at":"L2","do":"rewrite","rule":"partition-batch-by-tenant"},
  {"at":"L3","do":"rewrite","rule":"replace-retry-state-machine"},
  {"at":"L4","do":"set","path":[":limits",":burst"],"value":48},
  {"at":"L5","do":"replace","insert":"... invented ..."}
]}
```

Full twin wire: **14,575 bytes / 2,950 tokens**, only **1.96×** smaller.

### 3. THEORETICAL MINIMUM

This call had no preceding read. Its floor is **13,923 DECIDED bytes**:
5,506 subject and 8,417 destination. In twin prose, the decision is five
independent owner rewrites plus seven new blocks; about 14 KB genuinely was
not supplied upstream.

This exemplar is the limit case that prevents a fake “everything becomes 100
bytes” story. The twin pays only 652 bytes above the information floor.

### 4. LLM-NATURAL

1. **Closed JSON label splices/replacements — winner.** High-entropy new code
   must still be emitted, but JSON can constrain target, operation, and counts.
2. **Unified-diff string.** It may be locally readable at this size, but its
   hunk headers and context become a second, unconstrained subject language.
3. **Semantic rewrite verbs.** Shortest only by hiding undecided semantics in a
   broad verb; that is not an honest encoding for a 63-run novel rewrite.

## E3 — four augmentations, one real restructure

### 1. TODAY

The twin has four paired targets and two insertion leaves. Three pairs are
mostly insertion: 951→985 bytes in one run, 1,379→1,439 in one run, and
186→609 in one run. The fourth grows 1,651→2,803 bytes across 30 runs.

```json
{"changes":[
  {"files":["src/comet/buffer.clj"],"find":"(defn open-buffer ...)",
   "replace":"(defn open-buffer ...)","insert_after":["(def invented-limit 96)"]},
  "... three invented paired targets and one further insertion leaf ..."
],"expect":{"changes":4},"verify":"exact"}
```

Real original `36d95f176d5f` was refused: **12,495 bytes / 3,143 tokens /
44.029 projected seconds**.

### 2. INFINITE FREEDOM

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"splice","after":"body/6","insert":"... invented ..."},
  {"at":"L2","do":"splice","after":"body/8"},
  {"at":"L3","do":"splice","after":"body/1"},
  {"at":"L4","do":"rewrite","rule":"wrap-dispatch-with-budget"}
]}
```

Full twin wire: **4,880 bytes / 1,006 tokens**, **2.56×** smaller.

### 3. THEORETICAL MINIMUM

The floor is **4,536 DECIDED bytes**: 1,954 subject and 2,582 destination. In
twin prose, the decision is three short insertions plus one approximately
2.6-KB novel wrapped dispatch body. Grammar costs 344 bytes above the floor.

### 4. LLM-NATURAL

1. **Typed JSON `insert` plus bounded `rewrite` — winner.** It matches the
   actual asymmetry: three tiny structural deltas and one authored body.
2. **JSON label splices.** General and constrained, but the 30-run target is
   noisy if represented as many tiny splices.
3. **Full sexp-as-string replacement.** Natural Clojure, but repeats the old
   body and gives up constrained decoding inside the string.

## E4 — a four-attempt split/extract wall

### 1. TODAY

The twin family preserves each attempt's exact tree variation. Its common core
has two paired targets, 847→1,128 bytes over 22 runs and 3,311→874 over 27
runs, plus a 2,523/2,524-byte insertion and 1,919 bytes of following material.
Some attempts carried the latter as one string; others as two array leaves.
One attempt re-expanded both insertion leaves into paired rows.

```json
{"changes":[
  {"files":["src/aurora/index.clj"],"find":"(defn route-signal ...)",
   "replace":"(defn route-signal ...)","expect":1},
  {"files":["src/aurora/index.clj"],"forms":["route-signal"],
   "insert_before":["(defn invented-decoder ... ; 49 lines)"]},
  {"files":["src/aurora/index.clj"],"find":"(defn old-bundle ...)",
   "replace":"(defn thin-shell ...)","insert_after":["... invented ..."]}
]}
```

| Real original | Bytes | Tokens | Projected decode |
|---|---:|---:|---:|
| `1d038cd880bc` | 11,631 | 2,574 | 40.984 s |
| `5cea2f5bcfcb` | 11,631 | 2,575 | 40.984 s |
| `747dd2c62815` | 11,627 | 2,573 | 40.970 s |
| `d29368f6d656` | 11,627 | 2,574 | 40.970 s |
| **chain** | **46,516** | **10,296** | **163.908 s** |

All four attempts were refused; this panel makes no final-correctness claim.

### 2. INFINITE FREEDOM

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"splice","after":"body/5","insert":"... invented ..."},
  {"at":"L2","do":"insert-before"},
  {"at":"L3","do":"extract","range":"body/2..11","to":"L2"},
  {"at":"L4","do":"insert-after"}
]}
```

Full twin wire: **484 bytes / 127 tokens**. That is **24.02×** smaller than a
single 11,627-byte attempt and **96.11×** smaller than the emitted chain.

### 3. THEORETICAL MINIMUM

Per-attempt DECIDED floors were 274, 275, 791, and 275 bytes. The theoretical
minimum for the best supplied state is therefore **274 bytes**. In twin prose,
the decision is one extraction range, two placement choices, and a small local
adjustment. The 210-byte gap is closed operation/address grammar.

### 4. LLM-NATURAL

1. **JSON `extract`/`insert` verb arguments — winner.** The delta is structural;
   describing the move is more natural than re-authoring the source twice.
2. **JSON label splices.** Still safe, but makes the model calculate cut and
   paste mechanics the server can derive.
3. **Unified-diff splice.** Compact visually, but weak on move identity and
   dangerous when the same body occurs twice.

## E5 — sixteen insertions should be one insertion chord

### 1. TODAY

The twin has sixteen direct insertion leaves and no `from`/`find` leaf. Their
invented bodies range from 58 bytes to 3,793 bytes and from two to 75 lines.

```json
{"changes":[
  {"files":["src/quasar/catalog.clj"],"forms":["alpha"],
   "insert_after":["(def invented-a 1)","(def invented-b 2)","..."]},
  "... remaining invented insertion owners; 16 direct leaves total ..."
],"expect":{"changes":16},"verify":"exact"}
```

Real original `f28f442c4891` was refused: **11,392 bytes / 2,874 tokens /
40.142 projected seconds**.

### 2. INFINITE FREEDOM

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"insert-after","insert":"... invented ..."},
  {"at":"L2","do":"insert-after"},
  "... fourteen more label-addressed insertions ..."
]}
```

Full sixteen-op twin wire: **3,342 bytes / 731 tokens**, **3.41×** smaller.

### 3. THEORETICAL MINIMUM

The floor is **2,717 DECIDED bytes**: one subject byte and 2,716 destination
bytes. In twin prose, the decision is sixteen new blocks and their ordering;
the server can supply virtually all subject identity. The 625-byte remainder is
16-operation grammar.

### 4. LLM-NATURAL

1. **JSON `insert-many` as an ordered label→code array — winner.** It matches
   the chord and keeps every target independently guardable.
2. **Sixteen JSON splice ops.** Equally expressive and constrained, but repeats
   the verb and relation.
3. **One unified diff string.** Short punctuation, poor independent cardinality,
   and one malformed hunk can invalidate the whole chord.

## E6 — one 5 KB insertion plus two extensions

### 1. TODAY

The twin has one 5,143-byte insertion leaf and two paired extensions:
1,504→2,373 bytes over 12 runs and 403→599 over two runs.

```json
{"changes":[
  {"files":["src/pulsar/worker.clj"],"forms":["run-cycle"],
   "insert_after":["(defn invented-supervisor ... ; 109 lines)"]},
  {"files":["src/pulsar/worker.clj"],"find":"(defn run-cycle ...)",
   "replace":"(defn run-cycle ...)","expect":1},
  {"files":["src/pulsar/worker_test.clj"],"find":"(deftest invented-old ...)",
   "replace":"(deftest invented-new ...)","expect":1}
]}
```

Real original `8756522402af` was refused: **11,019 bytes / 2,392 tokens /
38.828 projected seconds**.

### 2. INFINITE FREEDOM

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"insert-after","insert":"... invented 109-line block ..."},
  {"at":"L2","do":"splice","after":"body/5"},
  {"at":"L3","do":"splice","after":"body/2"}
]}
```

Full twin wire: **4,471 bytes / 916 tokens**, **2.46×** smaller.

### 3. THEORETICAL MINIMUM

The floor is **4,209 DECIDED bytes**: 1,081 subject and 3,128 destination. In
twin prose, the decision is one new supervisor body plus two coupled local
extensions. The new body dominates; the 262-byte remainder is grammar.

### 4. LLM-NATURAL

1. **Typed JSON batch: one insertion plus two splices — winner.** It exposes the
   heterogeneous delta without repeating any old body.
2. **JSON full-form replacements.** Constrained outside the strings, but pays
   thousands of copied source bytes again.
3. **Unified diff string.** Familiar, but the large new block receives no
   structural target/cardinality protection of its own.

## E7 — copy, parameterize, and delete instead of expanding 604 bytes to 4,960

### 1. TODAY

The twin has four paired targets: 1,161→1,250 bytes, 604→4,960,
469→674, and 414→335. Their non-equal run counts are 3, 42, 9, and 8.

```json
{"changes":[
  {"files":["src/meteor/lease.clj"],"find":"(defn begin-lease ...)",
   "replace":"(defn begin-lease ...)","expect":1},
  {"files":["src/meteor/lease.clj"],"find":"(defn tiny-policy ...)",
   "replace":"(defn expanded-policy ... ; 117 invented lines)","expect":1},
  "... two more invented paired targets ..."
]}
```

Real original `82d43204c2f5` succeeded: **10,918 bytes / 2,654 tokens /
38.472 projected seconds**.

### 2. INFINITE FREEDOM

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"set","path":[":clock",":idle-ms"],"value":9100,
   "insert":"... invented ..."},
  {"at":"L2","do":"copy","source":"R8",
   "bind":{"old-name":"lumen","new-name":"prism"}},
  {"at":"L3","do":"splice","after":"body/4"},
  {"at":"L4","do":"delete","range":"body/7..8"}
]}
```

Full twin wire: **1,011 bytes / 246 tokens**, **10.80×** smaller.

### 3. THEORETICAL MINIMUM

The floor is **717 DECIDED bytes**: 83 subject and 634 destination. In twin
prose, the decision is one source-label copy, a small binding map, one timeout,
one short splice, and one deletion range. Most of the 4,960-byte destination
was already available upstream.

### 4. LLM-NATURAL

1. **JSON `copy` + `bind` + local verbs — winner.** The model states the
   relationship it appears to have decided instead of materializing the copy.
2. **JSON label splices.** General, but loses the explicit reuse relationship.
3. **Sexp-as-string full replacement.** Easy to read as Clojure, expensive and
   outside constrained decoding where the payload matters most.

## E8 — a two-attempt, three-owner augmentation

### 1. TODAY

The twin has three paired targets and one insertion leaf. The pairs are
500→550 bytes over two runs, 1,184→1,774 over three runs, and
2,168→3,060/3,044 over ten runs.

```json
{"changes":[
  {"files":["src/zenith/queue.clj"],"find":"(defn queue-state ...)",
   "replace":"(defn queue-state ...)","insert_after":["(def invented-capacity 96)"]},
  {"files":["src/zenith/queue.clj"],"find":"(defn enqueue ...)",
   "replace":"(defn enqueue ... ; 38 invented lines)"},
  {"files":["src/zenith/queue_test.clj"],"find":"(deftest old-cases ...)",
   "replace":"(deftest new-cases ... ; 55 invented lines)"}
]}
```

| Real original | Outcome | Bytes | Tokens | Projected decode |
|---|---:|---:|---:|---:|
| `1dfbe10effbf` | refused | 10,862 | 2,590 | 38.274 s |
| `94e37e581690` | success | 10,846 | 2,585 | 38.218 s |
| **chain** | — | **21,708** | **5,175** | **76.492 s** |

### 2. INFINITE FREEDOM

```json
{"snapshot":"S7","ops":[
  {"at":"L1","do":"repair","request":"P1",
   "subject":"three-current-owners","with":"... invented ..."},
  {"at":"L2","do":"set","path":[":queue",":capacity"],"value":96}
]}
```

Full twin wire: **499 bytes / 122 tokens**. That is **21.74×** smaller than the
successful call and **43.50×** smaller than the chain.

### 3. THEORETICAL MINIMUM

The successful retry's floor is **318 DECIDED bytes**: 116 subject and 202
destination. In twin prose, the decision after the refusal is one corrected
three-owner subject plus one capacity value and a small authored extension.
The 181-byte gap is retry reference and operation grammar.

### 4. LLM-NATURAL

1. **JSON request repair plus typed `set` — winner.** The server already holds
   the refused payload; make the correction the payload.
2. **Fresh JSON label splices.** Reliable but needlessly re-expresses the
   unchanged portions of the refused decision.
3. **Unified-diff retry.** It discards the server's exact prior-request identity
   and asks the model to rebuild context.

---

## Synthesis — the win table

The following exploratory classifier was run over all 195 service writes. It
uses only field shape and lexical/token edit structure: consistent symbol-only
substitution is `rename`; literal-only substitution is `value-swap`; a retained
old body plus bounded additions is `insert-block`; otherwise paired high-edit
content is `restructure`; heterogeneous batches are `mixed`; and calls with no
destination leaf are reported separately. It is a design census, not a semantic
ground truth labeler.

Today's columns are exact nearest-rank medians. The theoretical-minimum column
is the exact median DECIDED floor. Infinite Freedom is an exact fully
materialized representative structural twin: the median DECIDED bytes plus the
closed JSON operation/address wrapper. The multiple is an analytical byte
compression multiple, not measured model performance.

| Delta class | n | TODAY median bytes / tokens | INFINITE FREEDOM bytes / tokens | THEORETICAL MIN bytes | Recommended LLM-natural encoding | Achievable byte multiple |
|---|---:|---:|---:|---:|---|---:|
| rename | 4 | 199 / 62 | 117 / 37 | 23 | JSON `rename(at, from, to)` | 1.70× |
| value swap | 25 | 350 / 104 | 136 / 42 | 37 | JSON `set(at, path, value)` | 2.57× |
| insert / wrap block | 103 | 1,575 / 379 | 272 / 64 | 196 | JSON `insert(at, relation, code)` | 5.79× |
| restructure | 10 | 1,878 / 558 | 297 / 72 | 206 | JSON label `splice`; `replace` escape hatch | 6.32× |
| mixed batch | 45 | 4,045 / 952 | 351 / 80 | 280 | ordered JSON op array | 11.52× |
| no destination leaf | 8 | 94 / 29 | 65 / 23 | 0 | JSON delete or operation-specific verb | 1.45× |

The small rename multiple is useful discipline: today's compact JSON is already
near the floor for tiny deltas. The dramatic wins are the cases where a small
decision is wrapped in a whole old/new body or re-emitted after refusal.

The heavy-tail warning is equally important. The restructure-class median has
a 206-byte floor, but E2 has a 13,923-byte floor. The grammar should make the
common copied-body case cheap while retaining honest source carriage for the
rare genuinely novel body.

## One unified wire grammar

This grammar covers **8/8 distinct top exemplar families**. The common verbs
cover rename, value replacement, insertion, deletion, copy/bind, extraction,
and retry repair. `splice` is the closed general escape hatch for the remaining
restructure. `replace` exists only for content that is genuinely authored as a
whole form.

The important boundary is not the exact field spelling. It is that every
subject is a server-issued label in one immutable snapshot, every operation is
a closed discriminated JSON object, and every widening count is either derived
or explicit.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["snapshot", "ops"],
  "additionalProperties": false,
  "properties": {
    "snapshot": {"type": "string", "pattern": "^S[0-9A-Za-z_-]+$"},
    "base_request": {"type": "string", "pattern": "^P[0-9A-Za-z_-]+$"},
    "ops": {
      "type": "array",
      "minItems": 1,
      "items": {
        "oneOf": [
          {"$ref": "#/$defs/rename"},
          {"$ref": "#/$defs/set"},
          {"$ref": "#/$defs/insert"},
          {"$ref": "#/$defs/delete"},
          {"$ref": "#/$defs/copy"},
          {"$ref": "#/$defs/extract"},
          {"$ref": "#/$defs/splice"},
          {"$ref": "#/$defs/replace"},
          {"$ref": "#/$defs/repair"}
        ]
      }
    }
  },
  "$defs": {
    "at": {"type": "string", "pattern": "^L[0-9A-Za-z_.-]+$"},
    "rename": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "at", "from", "to"],
      "properties": {
        "do": {"const": "rename"}, "at": {"$ref": "#/$defs/at"},
        "from": {"type": "string"}, "to": {"type": "string"}
      }
    },
    "set": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "at", "path", "value"],
      "properties": {
        "do": {"const": "set"}, "at": {"$ref": "#/$defs/at"},
        "path": {"type": "array", "minItems": 1, "items": {"type": "string"}},
        "value": {}
      }
    },
    "insert": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "at", "relation", "code"],
      "properties": {
        "do": {"const": "insert"}, "at": {"$ref": "#/$defs/at"},
        "relation": {"enum": ["before", "after", "first-child", "last-child"]},
        "code": {"type": "string"}
      }
    },
    "delete": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "at"],
      "properties": {"do": {"const": "delete"}, "at": {"$ref": "#/$defs/at"}}
    },
    "copy": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "source", "at"],
      "properties": {
        "do": {"const": "copy"}, "source": {"$ref": "#/$defs/at"},
        "at": {"$ref": "#/$defs/at"},
        "bind": {"type": "object", "additionalProperties": {"type": "string"}}
      }
    },
    "extract": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "at", "range", "to"],
      "properties": {
        "do": {"const": "extract"}, "at": {"$ref": "#/$defs/at"},
        "range": {"type": "string", "pattern": "^body/[0-9]+(\\.\\.[0-9]+)?$"},
        "to": {"$ref": "#/$defs/at"}
      }
    },
    "splice": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "at", "range", "insert"],
      "properties": {
        "do": {"const": "splice"}, "at": {"$ref": "#/$defs/at"},
        "range": {"type": "string"}, "insert": {"type": "string"}
      }
    },
    "replace": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "at", "with"],
      "properties": {
        "do": {"const": "replace"}, "at": {"$ref": "#/$defs/at"},
        "with": {"type": "string"}
      }
    },
    "repair": {
      "type": "object", "additionalProperties": false,
      "required": ["do", "request", "ops"],
      "properties": {
        "do": {"const": "repair"},
        "request": {"type": "string", "pattern": "^P[0-9A-Za-z_-]+$"},
        "ops": {"type": "array", "minItems": 1}
      }
    }
  }
}
```

In a production schema, `repair.ops` should reference a small, non-recursive
correction union rather than the open array shown in this abbreviated fragment.
Labels and request handles are result capabilities, not model-invented IDs.

## Why this is the most natural encoding for an LLM

The choice is empirical, not aesthetic:

1. The Spark caller screen at commit `2b2a417` produced guarded compact JSON
   writes **3/3 exact one-shot** and refusal recoveries **3/3 exact in the
   prescribed two calls**. On the newer permissive read schema it was **0/3
   one-shot**, accumulating nine avoidable schema refusals. The strong surface
   was strict, guarded JSON; the weak surface invited field invention.
2. The wrapped-EDN carriage screen at commit `d0c11a5` round-tripped all 1,437
   requests but made write tokens **1.389% worse** and read tokens **12.253%
   worse**. EDN inside a JSON string both lost constrained decoding and failed
   its token criterion.
3. A unified diff or sexp string can be one field in valid JSON, but the model's
   meaningful choices then occur inside an unconstrained mini-language. Typed
   JSON keeps the discriminant, subject, cardinality, and argument kinds visible
   to the decoder and validator.

This does not mean JSON is intrinsically the smallest text. It means the best
current reliability evidence says to spend a small JSON grammar tax to avoid a
much larger refusal or wrong-subject tax.

## Three sharp wrong-subject risks and their screens

| Risk | Failure mode | Required authority | Screen that would test it |
|---|---|---|---|
| **Stale label reuse** | `L4` once named the intended form, but the file changed before apply. | Every label is scoped to one immutable snapshot digest; any byte drift refuses before mutation. | Issue labels, then change one byte before, inside, and after the target in three cases. All three requests must refuse with `source_unchanged=true`; zero rebinding. |
| **Isomorphic-sibling aliasing** | Two equal subtrees or repeated keys make a structural path appear to identify either occurrence. | Label identity includes canonical file, owner, concrete span identity, and issuance snapshot; ordinals are presentation only. | Build twins with duplicate sibling forms, repeated map keys in separate owners, and reordered equal blocks. Apply each old label. Exactly the issued node changes or the request refuses; the other node never changes. |
| **Verb widening** | `rename`, `set`, `copy`, or `splice` matches more occurrences than the model intended. | Each op compiles to an exact bounded effect with derived cardinality; cross-owner or cross-file widening requires an explicit label set. | Seed the same symbol/value at nested, quoted, metadata, comment, and second-owner sites. Request one labeled effect. The compiled preview must contain exactly one authorized effect; any second candidate refuses before write. |

The release screen should add malformed-union and unknown-field cases because
Spark's permissive-schema failures were field hallucinations, not merely wrong
values. Every operation object must have `additionalProperties:false`, and the
runtime must enforce the published schema rather than treating it as advisory.

## Evidence ledger and limits

- Sibling walls receipt schema: `clj-surgeon.walls-of-text-exhibit.v1`.
- Sibling extraction commit: `5d3834e`; receipt file SHA-256:
  `e28940013b8c346b3fb1ae0c2e4a08090ba3b076bbb2f4fa577891c68bb191f5`.
- Sibling analysis script SHA-256:
  `e92390ad2d621fad5553ca638dde78f7d830becf3cbc35e6badf463672fa0415`.
- Service telemetry inventory SHA-256:
  `a8b43f7ed7990bf5cec8f400afea934908190ca84de00458dba06803f3335f02`.
- Prior aggregate receipt SHA-256:
  `21c42301e162ed0543f292d9be06083fd16820c6e844b7c30627602ab2a38bc3`.
- This pass's privacy-safe structural-summary payload SHA-256:
  `4d67ae8da78c95a4befab07ec53601e118cc252747f8936597c5249b86299d48`.
- All sibling receipt invariants were true, including the 1,437/1,242/195
  service checksum, 630,138 canonical service bytes, complete matching of the
  frozen 52-call `edit_clojure` multiset, sorted Top 20, and absence of content
  or paths in the receipt.
- The four-panel candidate costs are deterministic synthetic-twin measurements,
  not model-call outcomes. The unified grammar is a design recommendation, not
  implemented product behavior.
- Retry groups are grouped by matching privacy-safe structural signatures and
  the registered previous-write methodology. This report does not expose or
  infer private task semantics.

The next empirical step, if this design is pursued, is not another broad model
beauty contest. It is one frozen same-decision screen per winning delta class:
current wire versus strict JSON verb/splice wire, with exact-effect identity,
first-call schema validity, wrong-subject zero, emitted tokens, and complete
verified wall as joint gates.
