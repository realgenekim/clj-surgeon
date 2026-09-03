# Settings-lens migration — DRY PLAN (native tools only)

Repo `curtaincall-cfp`, worktree `/home/genek-forge/src/curtaincall-cfp-lens-native`,
branch `bridge/settings-lens-native`, base `55d1fd3f`.
Patch: `.plan/native-settings-lens.patch` (unified diff, `a/src/cfp_scheduler_killer/folds.clj`).
**Not applied. Not committed. Not pushed. No stash. Worktree file untouched**
(`git status` shows only the untracked `.plan/`; `sha256 src/cfp_scheduler_killer/folds.clj`
= `5afe41ac8c03c60220755ec713d32fad6637206f8eb68308e8b91fa802206648`, unchanged).

---

## 1. Stopwatch and decisions

| | |
|---|---|
| start | `2026-09-02 22:56:04 UTC` |
| end   | `2026-09-02 23:03:49 UTC` |
| elapsed | 7 min 45 s |

**11 distinct decisions.**

| # | Decision |
|---|---|
| D1 | **`export.generated` (628→709) does NOT migrate.** It shares the guard but writes `[:events slug :exports]`. `update-settings` is settings-scoped *by construction* — its docstring's whole argument is that no `[:events … :settings]` path is exposed to a caller. Widening it with a path parameter, or minting a sibling `update-event`, would relitigate that safety property inside a mechanical migration. It keeps its hand-written `if-let`. Consequence stated out loud: the guard count lands on **1**, not 0. |
| D2 | **The read at 1145 (`announced-speaker-removals`) does NOT migrate.** It is a `when-let`, not one of the 19, and the `slug` binding is load-bearing for the function's *return*: on an absent event it returns `nil`. Swapping the read for `(settings state …)` and dropping the guard would return `#{}` instead — a live behavior change (`some?`/`when` callers differ) in a function outside LENS-001's oracle. Left alone. |
| D3 | **`event.program-speaker-updated` (1201) does NOT migrate.** Different predicate (event **and** person), not in the 19, not in the arm oracle. It would migrate cleanly (`update-settings` re-resolving the slug the outer `if` already proved) but requires deleting the `(let [slug …])` wrapper and reindenting the whole `->` thread — churn outside the guard/path budget, and its own arithmetic. Named as the cheapest follow-up: it would take paths 3 → 2. |
| D4 | **Both `#_{:clj-kondo/ignore [:unused-private-var]}` forms come out and the LENS-002 header comment is rewritten.** The file's own comment says "The two ignores come OUT with the first migrated arm" and "NO CALL SITES YET". After this patch there are 18 + 1 call sites; leaving those lines makes the file lie about itself. Counted as churn below, not hidden. |
| D5 | **The two-path arms `event.day-hours-set` and `event.blind-review-set` collapse to ONE `assoc` with two key/value pairs**, not two threaded `update-settings` calls. Two calls would resolve the slug twice and allocate an intermediate state. Verified print-identical (map key order preserved) — see §7. |
| D6 | **`event.announced-speaker-adopted` uses BOTH lenses**: the read lens for `entries`, `update-settings` for the write. The outer `if-let` disappears entirely and the present-event no-op falls out of the shape — an absent event reads `nil` settings → `entries` is `[]` → `some` is `nil` → the `state` **object** is returned. This is the one arm where the read lens earns its existence. |
| D7 | **`event.speaker-unannounced` hoists `target` ABOVE the removed guard.** `announced-speaker-identity` is total and pure (`blank->nil` / `normalized-name` / `str` ops, no throw for any map), so computing it for an absent event is safe; the anonymous branch and the missing-event branch both still return the state object. |
| D8 | **Two-level writes pass `assoc-in` as `f` with a settings-relative path** — `(update-settings state eid assoc-in [:webhooks (:id payload)] v)` — rather than flattening into a chain. Keeps the shape readable as "assoc-in, inside settings". |
| D9 | **The removal arms pass `update` + `dissoc`** — `(update-settings state eid update :webhooks dissoc id)`. This is what preserves the pinned ragged `{:webhooks nil}` / `{:api-keys nil}` materialisation on a virgin event: `(update nil :webhooks dissoc id)` = `{:webhooks nil}`, exactly as `update-in` on a missing path did. Verified in §7. |
| D10 | **Comment blocks inside migrated arms are dedented 2 spaces** to match their new nesting, rather than left at the old indentation. This is the single largest source of diff lines (43 line-pairs) and it is pure whitespace. |
| D11 | **The patch touches `folds.clj` only.** The characterization test's `expected-guard-sites` / `expected-settings-path-sites` and its `(= arm-count guards)` assertion are *reported* here (§6), not edited — the stated patch scope is a diff of `folds.clj`. §6 spells out what the companion test edit must be, including one that a number change alone cannot fix. |

---

## 2. The nineteen arms

`upd` = `(update-settings state (:event-id payload) …)`.

| # | event type | line @55d1fd3f | shape | migrates | why |
|---|---|---|---|---|---|
| 1 | `schedule.locked` | 628 | A | **yes** | single-key `assoc-in` → `assoc` |
| 2 | `schedule.unlocked` | 636 | A | **yes** | same |
| 3 | `agenda.published` | 644 | A | **yes** | same |
| 4 | `replay.marked` | 673 | A | **yes** | same; collapses to one line |
| 5 | `sink.registered` | 680 | B | **yes** | two-level write → `assoc-in` as `f` (D8) |
| 6 | `sink.removed` | 686 | C | **yes** | `update` + `dissoc`; keeps `{:webhooks nil}` (D9) |
| 7 | `api-key.created` | 695 | B | **yes** | two-level write (D8) |
| 8 | `export.generated` | 709 | **H** | **NO** | writes `[:events slug :exports]`, not settings (D1) |
| 9 | `api-key.revoked` | 722 | C | **yes** | `update` + `dissoc`; keeps `{:api-keys nil}` |
| 10 | `event.hero-set` | 1062 | A | **yes** | single-key |
| 11 | `event.email-notifications-set` | 1067 | A | **yes** | single-key |
| 12 | `event.day-hours-set` | 1073 | D | **yes** | two paths (1075/1076) → one `assoc`, two pairs (D5) |
| 13 | `event.unlisted-set` | 1080 | A | **yes** | single-key |
| 14 | `event.submission-cap-set` | 1086 | A | **yes** | single-key |
| 15 | `event.blind-review-set` | 1091 | D | **yes** | two paths (1093/1095) → one `assoc`, two pairs (D5) |
| 16 | `event.speaker-unannounced` | 1100 | F | **yes** | `target` hoisted above the guard; inner `state` no-op preserved (D7) |
| 17 | `event.speaker-announced` | 1148 | E | **yes** | `update` + fn |
| 18 | `event.announced-speaker-adopted` | 1164 | G | **yes** | read lens + conditional write; two paths (1173/1175) → 0 (D6) |
| 19 | `event.announced-speaker-added` | 1184 | E | **yes** | `update` + fn |

**18 of 19 migrate.** Not in the nineteen and also not migrated: the read at 1145 (D2)
and the write at 1201 (D3).

### Representative before/after, one per shape

**Shape A — single-key `assoc` (8 arms).** `replay.marked`:
```clojure
;; before
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :replay?] true)
    state))
;; after
  (update-settings state (:event-id payload) assoc :replay? true))
```

**Shape B — nested write (2 arms).** `sink.registered`:
```clojure
;; before
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (assoc-in state [:events slug :settings :webhooks (:id payload)]
              (select-keys payload [:id :url :types :created-at]))
    state))
;; after
  (update-settings state (:event-id payload) assoc-in [:webhooks (:id payload)]
                   (select-keys payload [:id :url :types :created-at])))
```

**Shape C — removal (2 arms).** `sink.removed`:
```clojure
;; before
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (update-in state [:events slug :settings :webhooks] dissoc (:id payload))
    state))
;; after
  (update-settings state (:event-id payload) update :webhooks dissoc (:id payload)))
```

**Shape D — two paths, one call (2 arms).** `event.blind-review-set`:
```clojure
;; before
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (-> state
        (assoc-in [:events slug :settings :hide-presenter-info]
                  (boolean (:hide-presenter-info payload)))
        (assoc-in [:events slug :settings :reveal-after-vote]
                  (boolean (:reveal-after-vote payload))))
    state))
;; after
  (update-settings state (:event-id payload) assoc
                   :hide-presenter-info (boolean (:hide-presenter-info payload))
                   :reveal-after-vote (boolean (:reveal-after-vote payload))))
```

**Shape E — `update` + fn (2 arms).** `event.announced-speaker-added`:
```clojure
;; before (comment elided)
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (update-in state [:events slug :settings :announced-speakers]
               #(upsert-by announced-speaker-identity % (:entry payload)))
    state))
;; after
  (update-settings state (:event-id payload) update :announced-speakers
                   #(upsert-by announced-speaker-identity % (:entry payload))))
```

**Shape F — guard hoisted out of a `let`/`if` (1 arm).** `event.speaker-unannounced`:
```clojure
;; before (comment elided)
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (let [target (announced-speaker-identity
                   (select-keys payload [:person-id :name]))]
      (if (= :anonymous (first target))
        state
        (update-in state [:events slug :settings :announced-speakers]
                   (fn [rows]
                     (vec (remove #(= target (announced-speaker-identity %))
                                  rows))))))
    state))
;; after
  (let [target (announced-speaker-identity
                 (select-keys payload [:person-id :name]))]
    (if (= :anonymous (first target))
      state
      (update-settings state (:event-id payload) update :announced-speakers
                       (fn [rows]
                         (vec (remove #(= target (announced-speaker-identity %))
                                      rows)))))))
```

**Shape G — read lens + conditional write (1 arm).** `event.announced-speaker-adopted`:
```clojure
;; before (comment elided)
  (if-let [slug (:slug (event-by-id state (:event-id payload)))]
    (let [target [:name (normalized-name (:name payload))]
          entries (vec (get-in state [:events slug :settings :announced-speakers]))]
      (if (some #(= target (announced-speaker-identity %)) entries)
        (assoc-in state [:events slug :settings :announced-speakers]
                  (mapv #(if (= target (announced-speaker-identity %))
                           (assoc % :person-id (:person-id payload))
                           %)
                        entries))
        state))
    state))
;; after — TWO else-branches collapse into ONE; the absent event and the
;; present-but-unmatched event now reach the same `state` by the same route
  (let [target [:name (normalized-name (:name payload))]
        entries (vec (:announced-speakers (settings state (:event-id payload))))]
    (if (some #(= target (announced-speaker-identity %)) entries)
      (update-settings state (:event-id payload) assoc :announced-speakers
                       (mapv #(if (= target (announced-speaker-identity %))
                                (assoc % :person-id (:person-id payload))
                                %)
                             entries))
      state)))
```

**Shape H — not migrated (1 arm).** `export.generated` is byte-identical to `55d1fd3f`.

---

## 3. Cardinality

Every number below is counted from the generated patch, not estimated.

| metric | count |
|---|---|
| hunks (`@@`) | **7** |
| lines removed | **125** |
| lines added | **82** |
| net | **−43** |
| `update-settings` call sites after | **18** |
| `settings` (read lens) call sites after | **1** |

Classification of every touched line:

| class | removed | added |
|---|---|---|
| the guard's `if-let` line | 18 | 0 |
| the guard's `state))` else line | 19 † | 0 |
| a `[:events slug :settings` path line | 21 ‡ | 0 |
| **subtotal — guard or path** | **58** | **0** |
| pure reindentation (identical after `strip()`, 2 spaces less) | 43 | 43 |
| closing-paren-count only (arity fallout of dropping the `if-let` wrapper) | 10 | 10 |
| genuinely new text | 14 | 29 |
| **subtotal — NOT the guard or the path** | **67** | **82** |

† 19, not 18: `event.announced-speaker-adopted` has two `state))` lines (the inner
`if` else and the outer guard else) and they collapse into one `state)))`.
‡ 21 = the 23 arm path sites minus 1145 (D2) and 1201 (D3).

**Lines touched that are NOT the guard or the path: 149 (67 removed + 82 added).**
It is not zero and could not be — a form cannot lose a wrapper without reindenting
and reparenthesising. The honest decomposition of those 149:

- **86** (43 pairs) — whitespace only. Zero semantic content.
- **20** (10 pairs) — the same expression with one more/less trailing `)`.
- **43** (14 removed / 29 added) — real new text, itemised:
  - **32** are the `update-settings` / `settings` call lines themselves, i.e. the
    replacement *for* the guard and the path. Counting them as churn overstates it.
  - **4 removed + 3 added** — the LENS-002 header comment rewrite (D4).
  - **2 removed** — the two `#_{:clj-kondo/ignore [:unused-private-var]}` forms (D4).
  - **2 added** — two comment lines in the adopted arm explaining how the
    absent-event case now falls out of the read lens (D6).

**So: churn that is neither guard, path, replacement-call, whitespace, nor paren
arity = 11 lines, all of them comment or `#_` reader-macro lines, all of them
required by D4/D6.**

`git apply --check .plan/native-settings-lens.patch` → **clean** against the live
worktree (run with `--check` only; nothing applied).

---

## 4. Preconditions the patch assumes

The diff carries three lines of context per hunk, so `git apply` fails loudly on a
stale tree. Beyond that, these are the exact assertions:

- `git rev-parse HEAD` = `55d1fd3f54082ee25aaaecd7daad8b62695d15ae`
- `sha256sum src/cfp_scheduler_killer/folds.clj` =
  `5afe41ac8c03c60220755ec713d32fad6637206f8eb68308e8b91fa802206648`
- `md5sum` = `0240be3543cebfd1eac014bec181de71`; the file is **1235 lines**.
- The guard string appears **exactly 19 times**, character-for-character:
  `  (if-let [slug (:slug (event-by-id state (:event-id payload)))]`
  at lines 628 636 644 673 680 686 695 709 722 1062 1067 1073 1080 1086 1091 1100 1148 1164 1184.
- The literal `[:events slug :settings` appears **exactly 24 times**, at lines
  174 629 637 645 674 681 687 696 723 1063 1068 1075 1076 1081 1087 1093 1095 1110 1145 1157 1173 1175 1189 1201.
- Line 136 and line 150 are each exactly `#_{:clj-kondo/ignore [:unused-private-var]}`.
- Line 137 is exactly `(defn- settings` and line 151 exactly `(defn- update-settings`.
- Line 174 is exactly `    (apply update-in state [:events slug :settings] f args)`.
- `update-settings`'s signature at line 173 is exactly `  [state event-id f & args]`
  and its guard at 174-176 returns bare `state` on the else branch.
- Line 127 begins `;; INTENT: LENS-002 — one place for the rule that nineteen fold arms currently`.
- Each of the 20 replaced blocks was matched **exactly once** in the whole file
  (the edit script fails hard on 0 or ≥2 matches); that is the strongest staleness
  detector here, stronger than line numbers.
- `announced-speaker-identity`, `normalized-name`, `upsert-by`, `blank->nil` are
  unchanged and pure (needed for D7).

---

## 5. Ambiguities I could not resolve from the code alone

1. **Whether `export.generated` is *meant* to keep sharing the guard shape.**
   The code says only that receipts live beside settings. Whether the intended end
   state is (a) one hand guard forever, (b) a sibling `update-event` lens, or
   (c) `update-settings` generalised to take a sub-key — is a design call, not a
   fact in the file. I took (a) because it is the only option that changes no
   contract. A reviewer may prefer (b); it is a separate commit either way.
2. **Whether the migration is supposed to drive the path count to 1.** The test's
   `expected-settings-path-sites` docstring says "the direction of travel: the
   migration takes this to 1", which implies 1145 and 1201 eventually move too.
   Nothing says whether that is *this* commit. I read "the 19 arms" as the scope
   and landed on 3. If the intent was 1, D2 and D3 flip — and D2 in particular has
   a real behavior question attached (`nil` vs `#{}`) that the file does not answer.
3. **`speaker.reminder-schedule-configured` (line ~336 at base) is a 24th settings
   write that no witness counts.** It writes
   `[:events (:slug payload) :settings :speaker-reminder-schedule]` under a
   *slug-keyed* guard, so it matches neither the guard regex nor the path regex, is
   absent from the 19, and is invisible to the tripwire. I did not touch it. I
   cannot tell from the code whether it was deliberately excluded or simply never
   noticed — it is a genuine blind spot in the inventory, not in my patch.
4. **Whether the golden-history digests are sensitive to map key insertion order.**
   I did not read the digest machinery in `fold-relation-policy-test`. I removed the
   question instead: every rewrite was checked print-identical, not merely `=`
   (§7), so ordering cannot have moved.
5. **Whether a formatter gate (cljfmt / `standard-clj-format`) runs on this repo.**
   I did not look. If one does, my hand indentation of the multi-line
   `update-settings` calls is what it would rewrite first.

---

## 6. Tripwire numbers after the patch

Counted with the test's own two regexes against the patched file:

| | before | after | why |
|---|---|---|---|
| `expected-guard-sites` | 19 | **1** | 18 arms migrated; `export.generated` keeps its guard (D1) |
| `expected-settings-path-sites` | 24 | **3** | the lens's own `update-in`; the read in `announced-speaker-removals` (D2); the write in `event.program-speaker-updated` (D3) |

Companion test edit required in the same commit:

```clojure
(def ^:private expected-guard-sites 1)          ; was 19
(def ^:private expected-settings-path-sites 3)  ; was 24
```

**And one edit a number change cannot cover.** The tripwire's last-but-one block is

```clojure
(testing "the guard count and the enumerated arm list agree"
  (is (= arm-count guards)))
```

`arm-count` is 19 and must stay 19 — the first deftest asserts
`(= arm-count (count settings-arms))`. After the migration `guards` is 1, so this
assertion is structurally false and cannot be repaired by editing a number. It has
to be **deleted or rewritten** (e.g. to assert that the one surviving guard belongs
to `export.generated`), with the reason in the commit message. This is the part of
the gate most likely to be missed by someone updating "just the two numbers", and it
is the reason this section exists.

The behavioral half of the gate needs **no** blessing: the 19-arm oracle, the
`identical?`-on-absent-event witnesses, the absent-key-vs-explicit-nil witness, the
`export.generated`-writes-to-`:exports` witness, the adoption-no-op-returns-the-object
witness, and the golden replay equality should all pass unchanged. If any of them
moves, the patch is wrong — that is what they are for.

---

## 7. Verification actually performed (not the gate)

- **Paren balance**, string/char/comment-aware scanner, both files: `BALANCED`.
- **No orphaned `slug`**: after the patch, `slug` appears only in the lens (167),
  `export.generated` (692), `announced-speaker-removals` (1106),
  `event.program-speaker-updated` (1158), and the unrelated slug-keyed arms.
- **The six rewrite identities**, checked in babashka against three base shapes
  (settings key absent / settings explicitly nil / settings populated), asserting
  both `=` **and** `pr-str` equality so map key order is covered:

  | shape | result |
  |---|---|
  | `assoc-in [… K] V` → `update-in […] assoc K V` | ok |
  | `assoc-in [… K1 K2] V` → `update-in […] assoc-in [K1 K2] V` | ok |
  | `update-in [… K] dissoc X` → `update-in […] update K dissoc X`, miss and hit | ok, ok |
  | two threaded `assoc-in`s → one `assoc` with two pairs | ok (order preserved) |
  | `update-in [… K] f` → `update-in […] update K f`, incl. `f` on nil → `[]` | ok, ok |
  | `get-in [:events slug :settings K]` → `(K (:settings (event-by-id …)))` | ok |

  and `identical?` on the absent-event branch: **true**.
- `git apply --check`: clean.
- **Not run**, deliberately: the Kaocha gate, the golden replay, clj-kondo.

---

## 8. The one risk I most want a reviewer to check

**`event.speaker-unannounced` (D7) — the hoist of `target` above the guard.**

It is the only edit that changes *when* code runs rather than only how it is
spelled. Before, an unannounce fact naming an unknown event never called
`announced-speaker-identity`; after, it always does. I convinced myself the call is
total — `blank->nil` and `normalized-name` both coerce through `str` and `(or … "")`,
so no input map can throw, and `select-keys` on any map is safe. But the argument
rests on the *current* bodies of two helpers, and it is exactly the kind of coupling
that a later "tighten up the identity function" commit breaks silently, in an arm
whose failure mode is an exception during replay of the whole log.

Both the arm oracle and the `identical?` witness cover it today for well-formed
payloads. Neither covers a *malformed* payload against a *missing* event — the one
combination the old guard made unreachable and the new shape does not. If the
reviewer wants one ratchet added with this migration, that is where I would put it:
a witness that folds `event.speaker-unannounced` with a hostile `:name` (a number, a
map, a nil) against an absent event and asserts the state object comes back.

Runner-up: **D6's collapse of two `state` returns into one** in
`event.announced-speaker-adopted`. The old code had two structurally distinct paths
to "unchanged" (event absent; event present, name unmatched). The new code has one.
The behavior is identical and pinned, but a reader debugging a missing adoption now
gets less information from the shape of the code about which case they are in.
