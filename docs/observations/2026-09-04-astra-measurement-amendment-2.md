# Astra measurement amendment: repaired server pin

Recorded 2026-09-04T23:40:21.562467+00:00, before any model-driven Surgeon migration arm.

The originally preregistered server commit5b531d3b is replaced by da7ba418cbe3e1de22efdd1471a0c295c0422d80 for the first and all subsequent primary tool arms. The new commit repairs independently reproduced loss of unrelated mixed refer bindings and rejects unsupported selected renamed bindings. Source SHA2569528d0290648ea0c7ef4ab21ebf91f61563d75774c031fc7ba1a38684633e361. Public MCP schema and frozen task, prompts, adapter, watcher, oracle, timing rules and models are unchanged. Native calibration is unaffected.

Before use: 44 pure tests/641 assertions pass, independent36-case review GO, lint0errors0warnings; fresh pinned public-wire gate must also pass. Full repository gate remains outstanding: this is an experimental candidate, not a release declaration. Original hand-drive receipts retain their oldserver identity. No tool model arm has run against either pin at amendment time. Source is served from the clean detached server-src checkout; each measured tool arm independently attests the running server and exact fixture defaultroot.
