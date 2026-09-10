# Plan of record: merging MCP/main toward public main (Gene: "I love it. Make it so.", 2026-09-10)

Authority: Gene's 2026-09-04 freeze stands ("no publish until a months-dogfooded winner"); this plan prepares, it does not publish. Astra's consult: 2026-09-10-astra-merge-main-plan.md (11,900 words; read its stages, the receipt-bootstrap problem, and "Exposure and reversibility"). Divergence at start: MCP/main 59d8bc0c (tag stable/2026-09-10) is 2,562 commits / 1,388 files ahead of public main (unchanged since 2026-09-04); 832 docs/observations files sit on the code ancestry (~38% of added text).

## Assurance layers (Gene's chosen order)
1. Sealed candidate, Sol fence, full landing gate, receipt re-derived at landing (exists).
2. Clean-clone `make install && make test` on a box that is not Anvil (anvil2 today; a Mac for the skiff receipt) (exists as a practice; becomes a gate).
3. Fail-closed consumers (exists).
4. Fast-forward only, every candidate tagged; rollback = move main back to the previous tag (policy).
5. Byte-parity on the retained specimens: routed ops on stable tag vs candidate, same inputs → same tree bytes and same receipts modulo a declared-volatile list (building: fable/parity-harness).
6. Public-surface scan, red first, then a standing watch on main (building: fable/public-candidate, bin/public-surface-scan).
7. Two reviewers for public candidates: Sol plus the independent Claude reviewer (policy).
8. Dogfood window on the skiff: Astra proposes 60 calendar days of regular, varied, eligible real use with failure and rollback receipts (Gene decides).
9. One theme per candidate; the 2,562-commit stream never lands as one merge.

## Stages (Astra), each with its gate
- Stage 0 hygiene and public contract: file manifest (include product code, direct witnesses, portable build inputs, approved public intent; exclude raw records, private automation, caches, provider experiments, unsupported claims); scan green; README truth; the battery-receipt bootstrap policy (a clean clone cannot inherit a 26-hour ledger; a repository-owned command must generate a fresh receipt for the exact candidate). Gate: Sol on the public boundary; scan over tracked text, binaries, packages, ancestry, refs; clean-user clone on a non-Anvil box passes install and test.
- Stage 1 portable platform: `public-candidate` built FROM frozen public main, importing reviewed squashes of the qualified platform closure (coordinator, slots, guards, censuses, installer, prerequisites, their tests and specs), one green commit per coherent slice, an internal manifest mapping public commits to original SHAs. Gate: fresh Sol adversarial review; scan; clean Linux and real macOS clones; CI from repository-owned commands only.
- Stage 2 routed operations with intent: alias migration first, then the exact witnessed cold split, each a separately reviewable squash with contract and sanitized receipt; default native outside the admitted classes. Gate: fence of the public entrances, confinement, undo; scan; clean clones; per-verb behavioural acceptance; Gene's named-winner decision and the agreed months of real use before landing on main.
- Stage 3 explicitly chosen remainder, one capability at a time; no generic "the rest".

## Execution split (Gene's question "can you do locally?")
Anvil does everything that can be verified: candidate, scan, strip, parity, Sol fence, clean-clone proof on anvil2, the manifest. The candidate lives on the private fork, never on origin, because a push to origin is a publication. The skiff does the dogfood window and, when Gene lifts the freeze for a candidate, the one act that is its power: `make install && make test` as the Mac receipt, then fast-forward main to the tagged candidate.

## Gene's decisions (filed to the inbox)
What "months" means (Astra: 60 days, minimum frequency, varied tasks); whether gate infrastructure may land before the product winners; squash vs development history; whether sanitized intent/specs are public (Astra: yes); routing plate in default install or an explicit optional target; the receipt bootstrap/storage contract; the disposition of internal material already in old public main's history (this plan authorizes no rewrite of public history).

## This week's one small step (Astra)
An unpublished README truth patch on a local hygiene branch pinned to 59d8bc0c: actual prerequisites, default global-routing installation, the abstract/path-socket guarantees, and the distinction between dated narrow benchmark results and current automatic routes. Reviewed against bin/install-preflight, the Makefile, test/gate_slot.py and resources/clj-surgeon-agent-routing.md. In flight as part of stage 0 (fable/public-candidate).
