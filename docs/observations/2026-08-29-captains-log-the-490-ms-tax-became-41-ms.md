# Captain's Log: The 490 ms tax became 41 ms

The attractive hypothesis was simple: merely declaring clj-surgeon's tools
cost every Codex turn 490 milliseconds. Across hundreds of turns, removing
that fixed tax would have been worth more than the entire write-grammar
portfolio.

It was also wrong.

The 490 ms number compared two small, sequential cohorts whose combined noise
was larger than their difference. We froze seven catalog shapes, from no MCP
through a real four-tool catalog and synthetic 64 KiB declarations, then ran 98
counterbalanced Sol/low calls on Anvil. Every call returned exactly `ok`; none
called a tool, shell, or file operation.

The local clock found a real effect: enabling a tiny MCP server added about
**41 ms** before the turn started. Adding another **64 KiB** to that tool added
**no measurable time**. The real four-tool catalog was not slower than its
one-tool projection. Complete turn times varied by seconds because of service
and answer latency, not catalog size.

That closes this hill. We should not hide tools to save milliseconds that the
catalog does not consume. Under-declaring a catalog can silently remove a tool
from the model; it buys no demonstrated speed. The better architecture is the
clear, safe surface we already have, with optimization effort returned to
turn-count and output construction where the measured costs actually live.

The durable evidence is in
[`2026-08-29-codex-catalog-floor-sweep-protocol.md`](2026-08-29-codex-catalog-floor-sweep-protocol.md).
