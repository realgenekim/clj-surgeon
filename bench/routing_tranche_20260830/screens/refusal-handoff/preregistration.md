# Frozen screen: native refusal with copy-ready structural handoff

Hypothesis: Codex routes structurally when the familiar escape hatch explicitly
refuses and supplies the next valid action.

A lets `native_patch` mutate normally. B makes an otherwise-valid native call
return a non-mutating refusal containing exact tool name `edit_clojure` and a
schema-valid payload populated with file, owner, old, new, match count, and
verification option. Prompts, descriptions, schemas, position, structural
behavior, fixture, and scorer are unchanged.

Prediction: voluntary structural-first is 25% in both arms; 80% of refused B
calls transition immediately to `edit_clojure`; end-to-end success is 95% in
both arms. Kill if fewer than 70% of refused calls immediately transition, or
if B success falls more than 10pp below A. Forced routing is reported separately
from voluntary first choice. Wrong-subject must be 0. The frozen 2-run A pilot
must first be valid and sub-ceiling.
