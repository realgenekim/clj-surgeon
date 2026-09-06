# The sublime tool, for Astra — proposal from the ethnography (2026-09-06, 03:0xZ)

Gene: "Writing python, babashka, is like super ridiculous. Like using emacs, and firing up echo and cat to modify files. Not acceptable. We can do better for Astra!!!"

## The measurement that motivates it
Of Astra's last 128 commands: 37 python3 (his own harness), 29 git (receipts, merges, hygiene), 8 ps (polling another seat's review), 8 bb one-liners (reading EDN receipts), 14 Surgeon reads (:cat :forms, :ls — adopted, chosen freely), 7 raw sed/rg, 6 native edits, 2 executor missions. Four of five actions were apparatus. The tool is sublime exactly where it exists (reads) and absent everywhere else in his loop.

## The principle
Every action in the table above that is not "describe the change" or "read the answer" is a verb the tool should own. Sublime = the agent touches nothing but intent and receipts. The bar is beads': every verb one line, every answer a receipt, every refusal a runnable next call.

## The verbs, ordered by actions removed per night

| his action tonight | count | the verb that replaces it | shape |
|---|---|---|---|
| grep/rg for owners and call sites | many, inside python | `callers <var>` | every site with span, across the repo; the dossier half-built; feeds `mission open` directly |
| python3 harness runs (cohorts, receipts, timings) | 37 | `mission bench <mission> --arms N,F --rounds 6` | the A/B is a ledger verb: preregistration, quiet window, slot, interleave, receipts, table — one command; the runner's logic moves into the ledger |
| bb one-liners over receipt EDN | 8 | `events --last 20`, `mission show M-1`, `mission cost` | receipts and the JSONL ledger are queryable; never parsed by hand |
| ps to see whether the box is free | 8 | `status` | quiet window owner and until, slots held, reviews running, resident gates alive |
| git commit with a hand-pasted receipt | several | `mission commit M-1` | the kernel commit writes the receipt into the commit body itself |
| two cold JVM starts per mission (propose 6.8 s, apply 8.2 s around 2.3 s of work) | every mission | a resident CLI: `mission serve` + thin client | the resident-gate pattern (liveness, classpath hash, cold fallback) applied to the whole CLI; 15 s → ~3 s |
| typed refusals paid by hand (missing receipt-out; source string required) | 2 | `:example` on every CLI refusal | the 13c12401 class extended to every verb; a refusal is a runnable next call |
| refused on comments → native fallback | 2 of 2 eligible | comment-preserving lowering | building on fable/forms-comments |
| "did the native fallback happen?" inferred from commits | — | `mission fallback <reason>` | a native fallback is itself a mission event; the dogfood ledger falls out of the JSONL |
| checking whether trunk moved / merging trunk / fence status | ~10 git | `land status`, `land request <sha>` | landing is a queue with receipts, not a conversation |

## The flywheel this makes
Dogfood → every refusal and every hand-paid action becomes a ledger line with a reason → the reasons rank the next verb → the verb ships behind the gate → the eligible surface widens → more dogfood. Tonight's own run is the first turn of that wheel: the comment gap surfaced at 01:50Z, was ranked at 02:30Z by the dogfood ledger, and is being built at 02:40Z. The tool improves at the rate its user is honest about friction, and the ledger makes the honesty automatic.

## What "sublime" feels like at the end
Astra types one line: `mission open --intent "rename finding-identity → finding-fingerprint everywhere" --proof bb`. The tool finds the sites, plans, types, proves, commits with the receipt, and answers in under three seconds. He types `ready` and sees verified/red/blocked. He never opens Python, never reads EDN, never runs ps, never pastes a receipt. When the tool cannot, it says exactly why in a line he can run.

## Asked of Astra
When the current round closes: riff with Fable on this list — strike what is wrong, reorder by his own pain, add the verbs only he can see — and fold the top three into the following round. Gene's words: "you and Astra riff, and figure out how to make tool more perfect, and make flywheel even better!"
