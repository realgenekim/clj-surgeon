NO-GO

# Sol fence review, round 3: preservation brief

- reviewed: sealed candidate `247592684da908837233c6f9fd6db54f9f954588`
- branch tip accepted by the brief: `1cb98b5e2a59c39da390ee04f20382453cb0f9ed`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 2 `NO-GO` on PB-FENCE-005
- verification: all ten replay rows; the three prior Sol probes; core-function-table audit; independently planted modelled-binding-macro, local-head-shadow, cross-tree kind-collision, and uppercase-alias falsifiers; receipt control; real-tree hashes; source inspection; preregistration arithmetic; exact candidate delta
- excluded as directed: `make test`

## Blocking findings

### PB-FENCE-006 — modelled `clojure.test/are` binding syntax still lets A certify a changed binding

No entry in the 201-name `core-fn-allowlist` resolves to a macro. Two entries do not resolve as
`clojure.core` Vars at all: `cast` and the apparent typo `when-first?`. The binding failure is instead in
the other half of the allowlist: `control-heads` includes bare `are`
(`bin/preservation-brief:275-286`), although `clojure.test/are` is a macro with arglist
`[argv expr & args]` and binds the symbols in `argv`. The walker gives `are` no binding treatment and
falls through to an ordinary recursive walk (`bin/preservation-brief:438`).

I planted a pair on copies of the real Cell C trees. Both files refer `clojure.test/are`. The base added
this inside `committee-page`:

```clojure
(are [header] (= header :probe) :probe)
```

The candidate added:

```clojure
(are [header] (= organizer-layout/header :probe) :probe)
```

The base expression names the `are` binding; the candidate expression names the relocated Var. The brief
nevertheless left `committee-page` at `:identical-modulo-requalification`, kept **103 of 141** relocated
bodies mechanically preserved, and did not name `are` as unmodelled. B3 did list `header`, but that does
not repair A's false statement that the body is preserved. Section 9.8 explicitly makes a defect inside a
body declared preserved the kill switch.

Required repair: remove `are` from the generic control table or model its binding vector explicitly, and
make this pair a permanent replay row. Audit every other macro in the named tables by the same rule.

### PB-FENCE-007 — a candidate `defn` overwrites a base `defmacro` in the shared def-head index

The `owner-kind` comment says “a `defmacro` anywhere wins,” but the implementation merges the cheap base
and candidate maps with candidate precedence, then reduces parsed base owners followed by candidate owners.
Both branches at `bin/preservation-brief:738-740` perform the same unconditional `assoc`; therefore the
candidate kind wins (`bin/preservation-brief:729-742`).

I planted a valid pair with the same `[probe.mac route]` in both trees: the base defines `route` with
`defmacro`, while the candidate defines it with `defn`. The base call uses the macro's `[header]` binding;
the candidate has a valid referred `header` value and changes the body use to
`organizer-layout/header`. The candidate `defn` kind certifies `pm/route` for both sides. The brief again
left `committee-page` at `:identical-modulo-requalification`, reported **103 of 141** preserved, and omitted
`pm/route` from the unmodelled-head list. Separate omitted/new-owner stops expose the kind change, but they
do not make A's body-preservation claim true.

Required repair: preserve per-tree kinds and refuse a cross-tree kind disagreement, or implement the
documented macro-wins join. Add both collision directions as permanent replay rows.

### PB-FENCE-008 — an uppercase require alias bypasses the external-macro fence as “Java interop”

`head-status` resolves the alias at `bin/preservation-brief:336-340`, but then declares any uppercase
prefix safe before consulting what the alias resolved to (`bin/preservation-brief:342-350`). Clojure
allows an uppercase require alias. I repeated PB-FENCE-005 with
`[compojure.core :as Route]` and `Route/GET`. The base use is the route binding and the candidate use is
`organizer-layout/header`. The brief classified `Route/GET` as a safe call, left `committee-page`
`identical-modulo-requalification`, reported **103 of 141** preserved, and did not name `Route/GET`.

Required repair: a require alias must take precedence over the uppercase-class heuristic. Only an
unaliased/import-confirmed class prefix may enter the interop call case. Make this pair a permanent replay
row.

All three repairs change `bin/`, which the ship-fix contract marks `HOLD reason=oracle-changed`; I therefore
did not patch them and cannot answer `GO-WITH-FIX`.

## Requested probes and controls

- **Ten-row replay:** clean plus nine planted rows completed. All 9/9 defects were surfaced. Wrong binding
  moved 103→102 preserved; promotion increased 8→9; dropped comment increased 11→12; omission moved
  141→140; form order added two forward references; require order added one unsorted-require signal;
  symlink escape added one refused entry.
- **Three prior Sol probes:** local-shadow became `:changed`; the symlink target was not read and
  `escaped-owner` was absent; the paired `compojure.core/GET` row named the head, made `committee-page`
  `:changed`, and moved 103→102 preserved.
- **Requested local-head shadow attack:** a tree `defn` named `header`, shadowed by a local head binding in
  the base, does not get incorrectly certified through rule (b). The base local call versus candidate
  `organizer-layout/header` made `committee-page` `:changed` and moved 103→102 preserved.
- **Section-B-only defects:** the promotion and load-order specimens exit 3 and are hoisted above section A
  under the large `HARD STOP`, with “Do not consume the preservation figures below and stop” and explicit
  B2/B4 lines. That is loud enough; reclassifying design obligations as A failures would be less honest.
- **Independence:** none of `bin/preservation-brief`, `bin/preservation-replay`, or
  `bin/preservation_scan.clj` requires `clj-surgeon.*`. The brief requires only its independent bin scanner.
  Receipt reading is invoked at line 945, after the A/B derivations; receipt values flow only to C and its
  corresponding hard stop, not to A/B facts.
- **Real-tree provenance:** replay builds from `git -C /home/forge/src/curtaincall-cfp archive d9205abc`
  and `65ad613b`, not synthetic seed trees. Source/replay tree hashes match byte-for-byte:
  `269dc8638eb697bd7df4aabfa64a4e3204fb705f` and
  `1e8bbfc23069b2cef320ceddcd7849782824b715`.
- **Receipt control:** the retained Cell C receipt produces four disagreements, including
  `verification_complete=false`, state `committed-probe-only`, and
  `proof_pending=["/usr/bin/true"]`; the brief calls the command a no-op that proves nothing.
- **Anti-overclaim control:** sealed candidate versus base reports zero relocated and zero preserved bodies.
- **Section 9 preregistration:** the arithmetic closes at 21 specimens × 3 arms = 63 observations and
  9 reviewers × 7 = 63. It honestly declines to estimate a per-class catch rate at n=2 per class per arm;
  the co-primary is preregistered as strict dominance before any run. The answer-key and independent-planter
  requirements are explicit.
- **Delta confinement:** `8751ed9e..1cb98b5e` changes only `bin/preservation-brief` (+173/-34) and
  `bin/preservation-replay` (+56/-1), total +229/-35. `bin/preservation_scan.clj` is unchanged and
  `git diff --check` is clean.

## Decision

Do not ship this candidate as a proof-burden reducer. PB-FENCE-005 is repaired for the exact lowercase,
fully qualified external macro, and the requested local-shadow/index-positive case is conservative. But a
modelled binding macro, an uppercase external-macro alias, and a base-macro/candidate-function index
collision each reproduce the prohibited false section-A certification.
