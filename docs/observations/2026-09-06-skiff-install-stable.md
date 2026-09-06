# Installing the stable Surgeon value on the skiff (tag stable/2026-09-06-strict = MCP/main 30d7920c; supersedes stable/2026-09-06)

What the tag carries: the STRICTLY-BETTER agent skill and plate (Gene's prime directive: a clean agent is routed to Surgeon only for the two witnessed classes — fan-out 1.75x, alias migration 1.38x — native by rule elsewhere; inspect a supporting read; one repair then native; receipts on values; kill switch; executed examples byte-identical to receipts; Astra red-lined in four rounds), the fan-out route plate (managed prompt block section "Fan-out route (experimental default, 2026-09-06)", checker-enforced), inspect_clojure match results with owner_counts per (file, inside) and the derivable source echo omitted (MCP-OP-FIELD-007/008), the admit gate with proof profiles, the collector fix, and every landing since 181c365c.

On the skiff, from the clj-surgeon checkout:
```
git fetch origin --tags
git checkout stable/2026-09-06-strict
make install            # CLI + Codex skill + Claude skill + agent-routing block (rewrites the managed block in ~/.codex/AGENTS.md and ~/.claude/CLAUDE.md; announce first — it changes every seat's boot prompt on that machine)
make check-agent-routing   # canonical/installed parity; must print :ok true with the block hash
```
Then rebuild/restart the skiff's Surgeon server from the same checkout (the seat's usual `make mcp-serve` / launchd unit) so the running server carries owner_counts; verify with one inspect_clojure match batch — the text line reads `m00 <file>: N matches · owners [...]`.

Tripwire: the hourly ~/bin/check-prompt-plate.sh must read origin/MCP/main (not origin/main) from a checkout that exists on that box; on Anvil it had pointed at a missing checkout and never run (fixed 2026-09-06 19:0xZ). Verify with `tail -1 ~/logs/prompt-plate.log` → `OK main=<sha> :block-hash ...`.

Hardening still open (filed): public-handler ceiling enforcement (inb-b60d6e); compact counts mode (inb-e02822, P2); hash policy for 150–200-owner single calls (inb-a36079); parked refusal-text item (inb-2da8ea).
