# Astra rows sublime, batch 4 — row 5

Recorded 2026-09-08T20:36:55.843813+00:00. Work began 2026-09-08 19:43:45 UTC; 90-minute deadline 21:13:45 UTC.
Branch-only in `/home/forge/src/clj-surgeon-split`, `astra/namespace-split`.

## Result and commits

Implemented the five transaction-negative facts and explicit pending-proof context.
`verification_complete` keeps its meaning. Contradictory candidate evidence refuses
before publication. No new routing admission or measured performance claim.

First action fetched origin and merged `origin/MCP/main`, fast-forwarding
`6e8891e6` to `21d57ffd252f94ba96ebff25ae158566af7da482`, including batch 3,
Sol's fence, ruling (a), and the complete adoption study. Final branch commit:
`65a542bb385038fd5ff12b97c70d3e1fb03b20ac`. Author/committer forge-anvil; Gene co-author trailer. No push.

Linked intent uses the authorized linked-intent-testing substitute, with design,
plan, registered EARS NS-SPLIT-060..066, red witnesses, implementation and docs.
Census pins were recomputed by the repository's actual source readers, then tested:
1,586 manifest tests (567 adopted + 1,019 original), 245 non-MCP intents.
See `source-census.edn`, `census.clj`, and `update-pins.clj` under the evidence root.

## Red → green

All paths below are relative to `/var/tmp/forge/rows-sublime-4/`.

| Contract | Red evidence | Green evidence |
|---|---|---|
| Comment edits/policy; stale reference in unchanged test; retained facade; duplicated owner; altered body; refusal without writes | `step1-red.log`: 6 tests, 24 failed assertions | `step1-green.log`: 6 tests, 26 passing assertions |
| Distinguish raw bytes from authorized replay | `raw-body-red.log`: missing raw-hash evidence | Included in final affected set |
| Committed warm proof is pending; text names proof-status | `step2-red.log`: 4 failed assertions | `step2-green.log`, then 68 tests / 610 assertions in `step2-loop.log` |
| Namesake comment swaps, legitimate rewritten namesake, multi-arity/alias facades | `fence-red.log`: 8 failed assertions | `fence-green.log`, final suite |
| Mixed-arity forwarder and retained facade target changes | `fence2-red.log`: 2 failed assertions | `fence2-green.log`, final suite |
| Retained facade fixed-arity mutation | `fence3-red.log`: 1 failed assertion | `fence3-green.log`, final suite |
| Step 1 affected warm set + frozen Cell C papercuts | Initial reds above | `step1-final-loop.log`: 67 tests / 601 assertions, actual 20 destinations / 5 callers, PAPERCUTS 0 |
| Step 2 affected warm set + frozen Cell C papercuts | Proof-context reds above | `step2-loop.log`: 68 / 610, actual 20 destinations / 5 callers, PAPERCUTS 0 |
| Final affected warm and actual CLI dispatch/help/output | — | `final-warm.log`: 191 tests / 1,845 assertions; CLI 3 tests / 36 assertions; zero failures/errors |
| Final Cell C manifest replay and warm split | — | `final-manifest-replay.edn`, `final-real-warm.log`: committed, 168 tests / 1,767 assertions |
| Final Cell C independent acceptance and cold closure | — | `final-real-proof-status.edn`: complete, verification_complete true, proof_pending []; all three checks exit 0; PAPERCUTS 0; unit suite 231 tests / 2,258 assertions |
| Final repository cold gate | — | ONE `make test`, exit 0, 497.989 s; `make-test.log`, `make-test-receipt.json` |
| Kondo, skill mirrors, routing plate | — | `kondo-final.log`: 0 errors / 0 warnings (one existing redundant-str info); `preflight.log`: mirrors and routing plate PASS |

The cold components passed 164 tests / 3,495 assertions (alias migration and receipt artifacts),
846 / 10,562 (fast and integration), and 890 / 7,883 (Babashka), all with zero
failures/errors. Recovery battery 3/3, Python oracle witnesses and final repository
hygiene also passed. These component counts are not a distinct-test census.

Final cold source inventory is unchanged: `True`.
Its SHA-256 is `c9bf36fccac68b297e2adc4ab3a75bee49ed45aead8980c745d568326ae26ced`. The receipt records command,
base HEAD, candidate patch hash, real timestamps, wall and both source inventories.

Sol's executed adversarial fence is GO in `fence/go-verdict.md`, with all earlier
NO-GO findings and probes retained. A repository observation files that verdict
at `docs/observations/2026-09-08-rows-sublime-4-fence.md`. This is a bounded static
inventory: it does not claim arbitrary forwarding-body semantic equivalence.

## Receipt sizes

Measured UTF-8 serialization; enforcement uses the larger of EDN and escaped JSON.
Nothing is silently truncated. `receipt-sizes.edn` and `final-text-size.json` retain
measurements. Historical receipts are retained from batch 3.

| Receipt | EDN bytes | JSON bytes | Headroom below 65,536 |
|---|---:|---:|---:|
| Batch 3 compact/cold receipt (the reported 13.8 KB) | 13,788 | 13,861 | 51,675 |
| Batch 3 warm/background receipt (same mode comparison) | 22,548 | 22,624 | 42,912 |
| Step 1 compact/cold receipt | 52,296 | 52,834 | 12,702 |
| Step 2 compact/cold receipt | 52,736 | 53,281 | 12,255 |
| Final actual warm/background Cell C receipt | 62,616 | **63,167** | **2,369** |

The final MCP text block is **63,291 bytes**, including its status line and the
entire structured JSON: 2,245 bytes below the same ceiling. Full text is
`final-reader-receipt.txt`; immutable original receipt is `final-real-warm-receipt.edn`.
The fixed prepublication reservation is 8,192 bytes, with the final full-receipt
ceiling still enforced. Hash equality markers and indentation deltas compact
repeated evidence without deleting owners or touched comment lines.

## Five real fact examples

These are selections from the final Cell C receipt, not synthetic examples;
`examples.edn` preserves fuller values. All new strings use the declared lossless
JSON string-content encoding (wrap in quotes and JSON-decode). Arrays carry
explicit columns. Source-content controls remain escaped, with whitespace intact.

1. **`:comment_edits`** reports 145 changed comment lines in 13 source/destination
   groups, with `:comment_policy "preserve"`. A real same-file row in
   `src/cfp_scheduler_killer/server.clj` has columns
   `[:line :after_line :before :after]` and values
   `[663 679 "          ;; ONLY in dev." [:indent 11]]`.
   The after value reconstructs the exact suffix with eleven leading spaces.
   Unchanged comment relocation is not an edit. Source `(comment ...)` policy
   removals and an empty edit list with applied policy have regression witnesses.
2. **`:stale_references`** is `{:count 0 :sites [] :expected []}`, scoped to
   `src` and `test`, **75 captured Clojure files**, with snapshot hash
   `b58adc09732c750417debe62704aa4312f1266fd2ee7f8940ac4cc584e8b74d9`.
   It inspects the merged candidate including unchanged test files, qualified
   retired symbols/aliases and require/refer entries. Strings, semicolon prose,
   dynamic resolution and unqualified unresolved symbols are explicitly excluded.
3. **`:facades`** has `:forms []`, `:expected []`, `:unexpected []` for retired
   `src/cfp_scheduler_killer/views.clj`. The policy permits only pre-existing
   unmapped forwarding owners where source retention applies. Evidence records
   symbol/Var/partial/fn aliases and direct single/multi-arity defn/apply wrappers,
   including mixed ordinary/forwarding arities. Expected entries bind owner,
   kind, ordered fixed/variadic signatures and canonical targets.
4. **`:exactly_once`** has columns `[:owner :destination_count :elsewhere_count]`;
   real row `["->instant" 1 0]`, with `:passed true` across all 141 owners and
   the same 75-file roots. Four pre-existing namesakes are explicitly identified:
   `date-fmt`, `not-blank`, `chair-on-event?`, `fmt-stars`. They are separate Vars,
   not duplicate moved owners. Their file/lib/name identity and multiplicity
   are preserved; legitimate caller rewrites may change their hashes. Additional
   definitions still fail the counts; declarations are excluded.
5. **`:bodies_preserved`** has 141 equal and zero unequal SHA-256 comparisons
   against independently parsed destination owners. For `->instant`,
   `:before_hash` is
   `d485d19ad6da8a1c85b109dce26e0dc1bbbc575163e6528fb14f63e88d2b69c2`;
   both after and raw-before columns are `:same` (explicitly equal to that hash).
   Across Cell C, **raw equality is 83 equal / 58 unequal**. The principal hash
   baseline replays only authorized reference, alignment and promotion edits
   over the original owner bytes; it is not a claim that all raw source bytes
   survive unchanged. Both raw and authorized-replay evidence are exposed.

Each constructed stale-reference, facade, extra-owner and body-change violation
also exercises prepublication refusal with unchanged filesystem bytes. Negative
facts are assertions about this guarded transaction snapshot, not timeless
repository guarantees or dynamic-reference proofs.

## Proof interpretation and reader witness

The actual warm receipt begins:

```text
namespace_split: committed-probe-only; split committed; proof pending (warm); next step :proof-status (see proof.next_call)
```

Alongside `verification_complete=false`, it carries
`:proof {:tier :warm :status :pending :execution :background :next_call ...}`.
The next call is `:op :proof-status` with the immutable original receipt path.
Manual and synchronous cases have separate context; a manual receipt does not
pretend a worker exists. The text block contains every structured field.

The fresh Codex one-shot read only the final complete receipt in bounded chunks.
Its retained answer, `final-reader-answer.md`, correctly states the split committed,
full proof is pending rather than failed, and gives the exact proof-status call.
An earlier receipt-only witness also passed; its tool-view truncation caveat is
retained in `reader-one-shot.md` and was avoided in this final witness.

The original receipt remains pending forever. Its actual background closure later
completed all three checks with exit 0, and proof-status joined the immutable
receipt to that closure as complete. Candidate hash:
`d3439cc314a47c2161a3bc71726e66befea721fccb316674a7d6b7e753cb8bb6`.
Both precommit facts and their printed manifest replay succeeded. The final
candidate patch (including new destination files) is `final-cellC.patch`.

## What remains for row 5; prediction

The study at 21d57ffd established in-receipt adoption, not reduced inspection:
6/6 readers, 6/6 acceptance, paired E/D inspection ratio 0.98, E/N wall 0.42.
This batch provides mechanism and correctness evidence, not a replacement cohort.
Next run the matched adoption cohort with native control and the same comment-policy
trap, counting postcommit inspections and complete verified wall from transcripts.

Prediction: **next/E paired postcommit inspection ratio about 0.55**, with a rough
0.4–0.8 expectation range: roughly 4–6 inspections versus E's median 9. Direct
negative evidence should remove stale-reference, duplicate-owner and facade scans
and the search for comment edits. Residual judgment remains around destination
headers/dependency direction, authorized body changes and repository artifacts.
This is a preregisterable prediction, not a measured speed or acceptance gain.

Remaining limits are explicit: static scope excludes dynamic/unresolved references;
raw bytes differ for 58 owners; facade inventory is syntactic; the receipt has
only 2,369 bytes of structured headroom. Larger eligible tasks may refuse the
budget. Cell C still reports advisory prose mentions, unrequired qualified
references and unexpected `00SERVER-LOGS.txt`; these are disclosed, not silently
cleared by the new negative facts. The unchanged oracle treats the named prose
and unresolved-namespace findings as advisory. No row-5 inspection gate is claimed
until callers actually stop doing those inspections.

## Operations and retained evidence

All evidence lives under `/var/tmp/forge/rows-sublime-4/`; this report is at the
exact requested path. JVMs used bounded 512 MB heaps and task-owned `/var/tmp`
temp directories. Only owned nREPL ports 43907 and 38537 were used; no prohibited
port or shared runtime was touched. Cleanup disposition is retained in
`cleanup.json`. The final branch is committed for Gene to ship, with no push.
