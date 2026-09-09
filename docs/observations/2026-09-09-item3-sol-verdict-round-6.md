NO-GO

# Sol fence review, round 6: preservation brief

- reviewed: sealed candidate `4089ddd2f015971f50ea3767d6eb9f0a6f18f058`
- branch tip accepted by the brief: `470cea34a978281b9d7ac87a02fbd8fe621f76f5`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 5 `NO-GO` on PB-FENCE-010
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-011 — a metadata-prefixed top-level definition is invisible to owner accounting

The clj-kondo resolver repairs PB-FENCE-010, but its definition inventory is not reconciled with the
private scanner's owner inventory. `form-owner` accepts a top-level datum only when its text begins with
`(` (`bin/preservation_scan.clj:199-222`). Valid Clojure may put metadata on the list itself:

```clojure
^:probe (defn hidden [] :hidden)
```

Clj-kondo parses that form and reports `a/hidden` in `:var-definitions`; the private scanner returns no
owner for it. `index-file` constructs A1 exclusively from `sc/form-owner` (`bin/preservation-brief:248-267`),
while the new clj-kondo definition map is used only to decide whether call heads are trusted. There is no
completeness comparison between those inventories.

I planted a two-tree omission witness. The base has ordinary `visible` and metadata-prefixed `hidden` in
namespace `a`. The candidate moves only `visible` to namespace `b` and deletes `hidden`. The brief exits
**0** with `clear: true`, says every changed body was preserved, and reports:

```text
base_owners=1 cand_owners=1 moved=1 omitted=0 preserved_bodies=1
```

The actual base has two Vars and the candidate has one. Thus A1's owner accounting and A3's exactly-once
claim are false, and a planted omitted-owner defect is certified clear. This fires the proof-burden kill
switch.

Retained witness:

- base: `f184d53448bd037d660b42bbbdbece03be980e8b`
- candidate: `f006fd3d0c824f53319504af58475b2fec433803`
- brief: `/var/tmp/forge/sol-r6.PwYvFv/meta-omitted.md`
- machine result: `/var/tmp/forge/sol-r6.PwYvFv/meta-omitted.json`
- recoverable fixture repository: `/var/tmp/forge/sol-r6.PwYvFv/workspace-fixtures/meta-repo`

Required repair: normalize leading metadata before top-level owner recognition, and reconcile every
clj-kondo `:var-definition` in the parsed scope with the scanner inventory. Any unexplained definition on
either side must fail closed. Add this exact omitted-owner pair as a permanent replay row. The repair must
change `bin/`, which the ship-fix contract classifies as `HOLD reason=oracle-changed`; I did not patch it.

## Requested probes and controls

- **Table ratchet:** `bin/preservation-tables-test` exits 0: 202 core function Vars, 72 modelled/frozen
  Vars, 31 claims, 38 external-resolver witnesses, 274 generated shadows, and 269 actual shadows. The five
  exceptions are special forms.
- **Full replay:** all 17 rows completed from real Curtaincall trees. Clean Cell C reports 141 moved,
  99 mechanically preserved relocated bodies, and 32 preserved in-place callers. All 16 existing planted
  defects were surfaced, including `:refer :all`, `:use`, and refer-all from outside both trees. This does
  not cover PB-FENCE-011.
- **Unresolvable/dynamic Var:** both the retained external-macro replay row and an independent macro-emitted
  `generated` Var pair remain uncertified; preservation drops rather than guessing a binding.
- **Position join:** an independent CRLF fixture with tab-indented call heads correctly joins kondo and
  scanner positions. Its `target` to `b/target` caller is certified only as
  `:identical-modulo-requalification`; the macro-emitted Var remains `:changed`.
- **Parser disagreement:** a file rejected by kondo raises `<file could not be analyzed>` in the hard stop.
  The reverse direction is PB-FENCE-011: kondo parses the definition, the scanner omits it, and the brief
  incorrectly clears the candidate.
- **Section B prominence:** the require-order specimen exits 3 and puts the B4 defect in the opening
  `HARD STOP`, under `Do not consume`, with the statement that a section-B signal is not weaker. Promotion
  is similarly hoisted. This is loud enough; those decisions need not be relabelled as A failures.
- **Independence:** none of the three implementation files requires `clj-surgeon.*`. A/B are derived before
  receipt comparison; receipt data feeds only section C.
- **Real-tree provenance:** replay pins Curtaincall CFP `d9205abc` and `65ad613b` and populates the scratch
  repository through `git archive`, not a synthetic seed.
- **Preregistration:** section 9 closes 21 specimens × 3 arms = 63 observations, guarantees two specimens
  per protected class per arm, refuses a catch-rate estimate at n=2, and preregisters strict dominance.
  That is honest.
- **Exact candidate delta:** `83c13814..470cea34` changes only `bin/preservation-brief` (+348/-32),
  `bin/preservation-replay` (+23/-4), `bin/preservation-tables-test` (+92/-61), and
  `bin/preservation_scan.clj` (+33/-17). `git diff --check` is clean; no other candidate path changed.

## Decision

Do not ship this candidate as a proof-burden reducer. The external resolver closes the round-5 symbol
resolution hole and behaves conservatively on unresolved symbols and position drift, but section A can
still omit a real top-level Var and certify an omitted-owner candidate as clear.
