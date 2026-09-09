NO-GO

# Sol fence review, round 7: preservation brief

- reviewed: sealed candidate `03b51d2bedd3f67db66397a29de4170990bf5444`
- branch tip accepted by the brief: `33e65cef45cc49c1250dc58ce1b7fcc77dbd2de8`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 6 `NO-GO` on PB-FENCE-011
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-012 — a Var in a valid unanalysed CLJC platform branch is absent from both inventories

PB-FENCE-011 is repaired for definitions that clj-kondo reports and for ordinary owner forms the scanner
recognises. It still fails open when clj-kondo deliberately does not analyse a reader-conditional platform
and the scanner sees only the reader-conditional wrapper. For example, clj-kondo reports no
`:var-definitions` for the valid `.cljc` form below because its configured analysis covers `:clj` and
`:cljs`, not `:bb`:

```clojure
(ns probe)
#?(:bb ^:probe (def hidden 1))
```

The scanner's `form-owner` does not descend into the `#?` form. Therefore deleting `hidden` from the
candidate produces no definition on the kondo side and no scanner-visible owner on the other side. The
two-way reconciliation compares two empty inventories and certifies the omission:

```text
exit=0
clear=true
base_owners=0 cand_owners=0 omitted=0 reconcile_failures=0
change_class="NOT a mechanical relocation — no relocated owner was detected"
```

This is not merely the documented absence of platform elaboration. The report calls the file parsed,
prints A1 owner accounting, emits no hard stop, and marks the candidate clear even though a Var present in
the base tree has disappeared. Section D's reader-conditional caveat does not turn a successful A1 result
into an uncertified file. That is the requested “file kondo skips” attack, and it fires the proof-burden
kill switch.

Retained witness:

- base: `da8ff1903241da51393d02374cd730b6100965b2`
- candidate: `833865ad0c7de496e7692e14b91c1fccf28eac3c`
- brief: `/var/tmp/forge/sol-r7.7ibArB/probes/out/bb-omitted.md`
- machine result: `/var/tmp/forge/sol-r7.7ibArB/probes/out/bb-omitted.json`
- recoverable object-database fixture: `/var/tmp/forge/sol-r7.7ibArB/probes/repo`

Required repair: analyzer coverage must be part of per-file reconciliation. A changed `.cljc` file with a
reader-conditional feature the resolver did not analyse must fail closed and be named, unless an
independent scanner inventories definitions in every branch and reconciles them. The repair belongs in
`bin/`, which the ship-fix contract classifies as `HOLD reason=oracle-changed`; I did not patch it.

## Requested probes and controls

- **Six definition shapes, run separately:** metadata-prefixed `defn` accounted 2/2 owners; `def` inside
  `do` 2/2; `declare` 1/1 owner plus one named declaration; `defmulti`/`defmethod` 2/2 owners plus two named
  no-Var forms; `defprotocol` 3/3; `definterface` 3/3. Every row had zero reconciliation failures. Retained
  under `/var/tmp/forge/sol-r7.7ibArB/probes/out/`.
- **Position join:** with `(def a 1) (def b 2)` on one line and only `b` changed, the brief found two owners,
  zero reconciliation failures, and exactly one changed in-place body. With a spanning
  `#?(:clj (def a 1) :cljs (def b 2))` followed on the same line by `(def c 3)`, changing only `c` found
  four owners and zero reconciliation failures; the two conditional definitions conservatively share the
  wrapper span and are both reported changed, rather than being silently mapped away.
- **Skipped platform branch:** the `:bb` witness above is silently fine. This is the blocker.
- **`defmethod` dispatch identity:** changing dispatch `:old` to `:new` raises the opening hard stop saying
  the forms define no Var and are not certified. The detailed A1 list names both exact forms and dispatch
  values (`base ... :old`, `candidate ... :new`), so this line is loud enough to make a reviewer look.
- **Table ratchet:** `bin/preservation-tables-test` exits 0: 202 core functions, 72 modelled/frozen Vars,
  31 claims, 38 external-resolver witnesses, 274 generated shadows, and 269 actual shadows.
- **Replay:** the time-boxed rerun completed the clean row and the wrong-binding, silent-promotion,
  dropped-comment, omitted-owner, both load-order, local-shadow, symlink, external-macro, `are`, and
  uppercase-alias rows without a harness failure before the decisive PB-FENCE-012 result ended the run.
  I do not claim a fresh complete 19-row replay.
- **Section B prominence:** promotion, load order, and no-Var forms are hoisted into the opening hard stop;
  a section-B signal is explicitly said not to be weaker. No promotion of these checks into A is needed.
- **Independence:** none of `bin/preservation-brief`, `bin/preservation-replay`, or
  `bin/preservation_scan.clj` requires `clj-surgeon.*`. Receipt input is read only after the independently
  derived A/B facts and is consumed only by section C.
- **Real-tree provenance:** replay still pins Curtaincall CFP `d9205abc` and `65ad613b` and populates its
  scratch repository with `git archive`, not a synthetic source seed.
- **Preregistration:** section 9 closes 21 specimens × 3 arms = 63 observations, states two observations
  per protected class per arm, refuses to estimate a catch rate at that denominator, and preregisters
  strict dominance. The co-primary is honest before a run.
- **Exact candidate delta:** `470cea34..33e65cef` changes only `bin/preservation-brief` (+153/-40),
  `bin/preservation-replay` (+51/-3), and `bin/preservation-tables-test` (+2/-1), for +206/-44 total.
  `git diff --check` is clean; no other candidate path changed.

## Decision

Do not ship this candidate as a proof-burden reducer. PB-FENCE-011 is repaired for the advertised ordinary
definition shapes, positional joining works on the attacked layouts, and the `defmethod` warning is loud.
But owner reconciliation is only as complete as the union of kondo's selected platforms and the scanner's
non-descending top-level recognition. A real Var can still disappear from both inventories and receive an
exit-0 `clear: true` report.
