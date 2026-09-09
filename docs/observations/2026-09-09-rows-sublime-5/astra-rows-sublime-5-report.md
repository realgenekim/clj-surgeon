# Astra — rows sublime, batch 5

Written 2026-09-09T01:34:48.085279+00:00. Branch `astra/receipt-facts-2` in `/home/forge/src/clj-surgeon-astra-consult`.
Commit: **b2beec26743c818e28e9bc2a137dc4e20a427404**. Author/committer: forge-anvil; Gene trailer. **No push.**

Implemented the four missing fact families and the proof presentation change.
The comment identity algorithm remains unchanged. Both the full Cell C fixture
and the exact E3 retained-source fixture commit; their comment facts are identical
to the old receipts, and every Base64 body digest decodes to the old hexadecimal
digest. E3's complete source patch is byte-for-byte identical to accepted Y1:
`22848c8ceb87a902becd1e4f3e8f6b935da6f50b86d14e8fbcb7c095e35a038c`.

Starting HEAD was `8a3254863d594c7f5efbfaae7e01e15d39239e5d`, containing the requested
`571170cc` trunk and the adoption records. It was preserved, not reset. I read
records `2026-09-08-row5-adopt-3.md` in full. All retained evidence is under
`/var/tmp/forge/rows-sublime-5/`; the requested report is this file. The user-authorized
linked-intent-testing substitute owns NS-SPLIT-067..072 and their direct witnesses.

## Red → green

The initial seven new witnesses recorded **61 failed assertions, zero errors**
before implementation. The old warm rollback matrix still passed its existing
assertions. Additional representation and decoding witnesses were also run red.

| Promise / named witness | Red evidence | Final result |
|---|---|---|
| `namespace-entry-facts-exact-diff`: require/import addition, removal, reorder, duplicates, trivia | 14 failures | PASS |
| `footprint-detects-one-byte-outside-owner-change` | 6 failures | PASS |
| `lint-facts-report-executed-delta`: net delta with an introduced error that net counts cannot excuse | 1 failure | PASS |
| `residual-facts-losslessly-encode-hostile-identity`: separator/bidi path identity | 2 failures | PASS |
| `warm-load-facts-name-failed-destination`: valid original, destination-only throw, rollback | 27 failures | PASS |
| `absent-warm-probe-does-not-claim-loads` | 3 failures | PASS |
| `proof-text-orders-commit-proof-and-defined-boolean` | 8 initial MCP failures; 4 later CLI failures | PASS, including both text faces at the data ceiling |
| `body-digest-encoding-is-lossless-sha256` | 2 failures | PASS; known SHA-256 vector and old/new digest equality |
| `large-footprint-names-exceptions-without-repeating-quiet-files` | 3 failures | PASS |
| `byte-identity-refuses-lossy-source-decoding` | 2 failures, 1 error: old code committed and retired the source | PASS; malformed UTF-8 refuses before write |
| actual Babashka CLI ordering/refusal witness | 2 failures plus an initial whitespace-specific assertion error; compact printing then exposed an unflushed nonzero exit | PASS, valid EDN and nonzero refusal output |
| `cli-physical-text-keeps-the-receipt-bound-and-edn-values` | 75,273 printed bytes exceeded 65,536 | PASS, complete values retained |

Logs: `red.log`, `digest-red.log`, `complement-red.log`, `utf8-red.log`,
`cli-order-red.log`, `cli-public-red.log`, `cli-bound-red.log`, and final
`warm-final-cli4.log`. The early CLI assertion was corrected to compare field
prefixes independent of pprint whitespace; the ordering/definition failures
were genuine. The missing flush was caught and fixed by the existing actual
invalid-request CLI witness.

## Gates and scope

- **Final affected warm set:** 195 tests / 2,112 assertions, zero failures/errors.
  Includes split compiler, I/O/MCP, real warm boundary, pure and detached proof
  gates, help, intent audit and lane census. Four CLI witnesses add **41 assertions**, all green.
- **Exactly one JVM cold gate:** 195 tests / 2,100 assertions plus three CLI
  witnesses / 36 assertions, all green (`cold.log`, `cold-results.edn`).
  **Timing caveat:** this cold gate followed all fact/load/encoding/UTF-8 changes
  but preceded the final CLI-only ordering/compact-renderer/flush follow-up.
  The final snapshot then passed the complete affected warm set and fresh
  Babashka CLI witnesses above. No second JVM cold run was launched; this is
  not a claim that the cold JVM gate loaded the final CLI presentation bytes.
- **Cell C:** frozen full mapping, 141 owners / 20 destinations; independent
  structural oracle PASS and **PAPERCUTS: 0** (`cellC-oracle.log`,
  `papercuts-final.log`). Its actual warm probe loaded 37 namespaces and ran
  **168 tests / 1,767 passing assertions**, zero failures/errors. This is
  affected warm application evidence, not fresh-process application proof.
- **Constructed real lint candidate:** `missing-value` plus an unused argument
  produced **errors +1 / warnings +1**, baseline 0/0, introduced_errors 1,
  status failed (`constructed-lint.edn`). The boundary's no-publication lint
  regression witness also passes.
- **Serialized `~/bin/clj-kondo`:** zero errors/warnings across changed source
  and tests (`kondo-final.log`); one existing informational redundant-str notice
  remains in core. Changed split files were formatted. Unrelated existing core
  formatting churn from the whole-file formatter was discarded.
- **Census/intent:** the tests' own `deftest-count`, `spec-ids` and `spec-doc-paths`
  readers derive **604 adopted / 1,629 total JVM declarations**, and **253
  non-MCP intent IDs**. The additional CLI size witness belongs to the BB test
  surface. `derive-pins.clj` and `census.edn` retain the derivation; no hand merging
  of arithmetic pins. Skill mirrors agree.
- Owned JVMs were bounded at 1,536 MiB (tool warm image), 512 MiB (Cell C warm
  image), and 1,024 MiB (single cold gate). Owned nREPL ports were 39433 and
  46477. None of the forbidden ports was used. Source fixtures are removed
  after evidence capture; receipt artifacts and logs remain.

The E3 replay source patch exactly equals accepted Y1, so no second application
cold suite was needed for that size replay. No live shared MCP service was
reloaded, no routing admission was changed, and no independent Sol verdict is
claimed here. Ship v3 / Sol review remains the user's stated landing path.

## Actual receipt facts

These are Cell C values, decoded using the receipt's declared file and entry
tables. Raw receipts are `cellC-after-receipt.edn` and `cellC-cli.edn`; complete
examples, including every loaded namespace and footprint exception, are in
`examples.edn` / `examples.json`.

```clojure
;; :ns_edits row for src/cfp_scheduler_killer/views/format.clj, decoded:
{:file "src/cfp_scheduler_killer/views/format.clj"
 :edits [{:clause :require :removed []
          :added [{:index 0 :entry "[cfp-scheduler-killer.events :as events]"}
                  {:index 1 :entry "[clojure.string :as str]"}]}
         {:clause :import :removed []
          :added [{:index 0 :entry "(java.time LocalDate ZoneId)"}
                  {:index 1 :entry "(java.time.format DateTimeFormatter)"}]}]}
```

`ns_entry_table` interns exact tokens. `ns_edits` rows use `ns_edit_columns`,
with nested `ns_clause_columns`; additions/removals are `[occurrence-index entry-ID]`.
Survivor reorders retain both orders, and a header byte change with no entry
change remains an explicit row. Entry facts cover require/import, not arbitrary
ns clauses or layout details.

The actual `:footprint` says **50 input files are byte-identical**, also outside
the moved owner files. Its universe is the 56 captured input Clojure files in
`["src" "test"]`, snapshot
`43b15c857ebcc69b0a4bb8c051d13a319b6eed823a6330a382191e1bb762f938`.
Twenty destination files are created, views.clj is deleted, and the five changed
files outside owners are exactly:

```clojure
["src/cfp_scheduler_killer/server.clj"
 "test/cfp_scheduler_killer/comms_test.clj"
 "test/cfp_scheduler_killer/forms_test.clj"
 "test/cfp_scheduler_killer/polish_test.clj"
 "test/cfp_scheduler_killer/views_test.clj"]
```

Large identical sets use `{:all_captured_except [file-IDs] :count 50}`, an exact
complement of the explicitly named paths in the captured input snapshot.
Outside-owner identity also excludes source/destination owner files. This does
not claim uncaptured workspace paths or residual portions of moved-owner files.
Malformed UTF-8 refuses because lossy decoded strings cannot prove byte identity.

```clojure
:lint_delta {:errors 0 :warnings 0
             :baseline {:error 108 :warning 195 :info 5}
             :post {:error 108 :warning 195 :info 5}
             :introduced_errors 0 :status "passed"}
```

Those baseline findings come from the existing isolated captured-source lint
comparison; this is **no new lint damage**, not an absolute clean-lint claim.
Signed net counts never hide `introduced_errors`.

```clojure
;; Prefix of the actual 37-name executed :loaded vector:
:loaded ["cfp-scheduler-killer.views.avatar"
         "cfp-scheduler-killer.views.form-controls"
         "cfp-scheduler-killer.views.format"]
:load_errors []
:load_status :passed
```

All 20 destinations occur in the full vector. The first failing require instead
names that attempted namespace in load_errors and causes rollback; failed tests
retain successful loads without inventing load errors. E3's frozen no-op profile
has no warm endpoint, so its actual receipt correctly says `:loaded []`,
`:load_errors []`, `:load_status :not-run`.

The actual MCP leading text is:

```text
:committed true
:proof {"tier":"warm","status":"pending"}; proof pending; :proof-status
  :verification_complete false ; definition: all required substantive cold checks passed over the committed snapshot
```

The complete proof remains in the JSON body. CLI EDN starts with committed,
proof, verification_complete, and verification_complete_definition in that
order. **The boolean's meaning is unchanged.** Cell C remains warm/probe-only;
E3's true-only cold profile remains pending cold-suite. Neither receipt is
retroactively stamped complete by the tool-development tests.

## Receipt sizes and encodings

| Subject / representation | Before | Final after |
|---|---:|---:|
| E3 field CLI, reported cohort baseline | 30,933 B | **34,691 B** replay CLI |
| E3 retained Y1 CLI artifact, exact file | 30,924 B | 34,691 B |
| E3 canonical EDN / escaped JSON data | 28,173 / 28,412 B | 34,547 / 34,814 B |
| Matched full Cell C canonical EDN / escaped JSON | 59,851 / 60,554 B | **62,839 / 63,585 B** |
| Full Cell C final physical CLI / MCP text | — | **62,982 / 63,790 B** |

The cohort's 30,933 B is a pretty-printed CLI size, not canonical EDN or JSON;
those representations must not be conflated. Replay artifact paths/timings also
vary from the cohort. The after CLI is **12.1% larger** than the reported E3
baseline while carrying the new facts. `final-measure.log` and actual output
files retain the byte measurements.

Expanded new facts first cost 74,872 B before publication. Exact path
prefix/basename tables, entry/column tables and snapshot complement statements
remove repetition. Body SHA-256 digests use standard Base64 of all 32 bytes
(`:digest_encoding :base64`); `:same` retains its equality meaning. The decoder
comparison against both old receipts proves no digest information changed.
The initial publication estimate reserves 4 KiB rather than 8 KiB; the final
EDN/JSON ceiling remains **65,536 B** with its existing 256-byte text reserve.
Oversize final publication still rolls back. Physical CLI text uses compact
nested values; MCP keeps its leading summary inside the reserve. Neither face
truncates facts, and both have a ceiling-boundary witness.

## E4 prediction

For the **unchanged E3 task/profile and paired split meter**, predict median
paired **E4/E3 answerable ratio 0.70** (plausible range **0.45–1.00**) and median
caller wall **135 seconds** (range **125–160 seconds**), about **0.95 ×** E3's
141.8-second median.

Reason: namespace and footprint facts now answer the unanimous residual gap,
and early defined proof state removes the long search for the explanation of
false. Removing roughly one of four answerable reads is plausible. The receipt
is 12% larger, its tables need decoding, and the existing two-read floor arms
still dominate paired ratios. E3's no-op profile still earns no executed-load
claim, so cycle/load corroboration cannot simply disappear. This predicts a
modest improvement, not a ≤0.50 gate pass or friction-low status. The earlier
0.55 prediction missed E3's 0.83; I have not treated the 24/29 count fall or the
historical native floor as a fresh matched win. No new caller cohort was run.

## Final handoff

Commit: **b2beec26743c818e28e9bc2a137dc4e20a427404**. Branch only, no push. Report and evidence are ready
for the user's ship v3 / Sol route. The single cold gate's timing limitation is
explicit above; the final CLI behavior has actual fresh Babashka witnesses.
