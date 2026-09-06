# Forward-reference analysis with warning-only lint output

The public `:ls` route currently refuses a readable mission_cli.clj because its
analyzer reports a redundant-let warning. The same analyzer emits complete Var
analysis. This repair changes analysis admission, not source or warning levels.

## Contract

Invoke the admitted clj-kondo with explicit `--fail-level error`. The installed
executable's help documents this threshold; default warning and error thresholds
were executed against the same valid warning fixture and actual mission_cli.clj.
Only admitted, finished, exit-zero runs can supply analysis. No arbitrary
nonzero process status is accepted. Timeouts, unavailable authority and real
lint errors continue to refuse.

JSON must parse to a map with an analysis map and vector var-definitions and
var-usages, whose members are maps. Empty vectors are valid, as demonstrated
by the executable on an empty file. Do not add mandatory per-entry fields in
this repair: sparse legitimate entries need their own provider-schema evidence.
An explicitly reported error finding or positive numeric summary error refuses
even if a malformed adapter claims exit zero. Malformed JSON and truly absent
or wrong-container analysis cannot silently become an empty answer.

| Boundary | Result |
|---|---|
| Real valid warning-only source | Retain analysis, no source modification |
| Valid empty source/empty vectors | Empty forward references |
| Process not finished, nonzero or absent exit | analysis-failed |
| Unadmitted analyzer | authority-unverified |
| Malformed JSON, absent/invalid analysis | analysis-invalid |
| Reported error with otherwise complete analysis | analysis-failed |

Tests cover the pure payload matrix, injected subprocess outcomes and one real
warning fixture minimized from mission_cli's redundant nested let. The public
`:ls src/clj_surgeon/mission_cli.clj` command is the dogfood witness; preserve
its old refusal and new result. Existing analyzer/forward-reference contracts
remain in the focused gate. No provider calls, service changes, source-warning
suppression, global severity changes or performance claim. Independent executed
review is required before integration.
