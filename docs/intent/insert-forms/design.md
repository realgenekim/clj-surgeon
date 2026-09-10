---
parent: mcp-operation-contract-design
prefix: INSERT-FORMS
status: red witnesses
---

# insert_forms v1

One request inserts a nonempty payload at one structural boundary in one existing
Clojure file. The authoritative contract is reproduced in contract.md, with its
byte examples and refusal vocabulary intact. This leaf adds no batch, preview,
replacement, formatting, alias inference, or behavioral verification.

The pure planner consumes captured source and a closed request; its output carries
candidate bytes, receipt facts, and durable detail. The I/O shell captures under
the workspace lock, uses the guarded transaction and staged atomic publication,
and persists inverse evidence outside the workspace. CLI and MCP delegate to this
shell. The independent acceptance walk shares only the CST parser.

The user authorized the full RED-to-GREEN sequence on fable/insert-forms. Tests
precede implementation; each witness namespace is a separate commit. Requirements
remain unchecked until implementation and its direct witnesses pass.
