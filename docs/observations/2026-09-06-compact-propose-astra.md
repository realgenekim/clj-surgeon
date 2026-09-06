# Astra: compact proposal stdout

2026-09-06. Parent live dogfood retained a 60,118-byte proposal at
`/var/tmp/forge/astra-live-count-name-fx/propose.edn`. It repeated saved dossier,
prompt and source data. This is a byte count, not a latency measurement.

The public CLI now prints the existing bounded saved `mission-show` view after
an id-bearing `propose`. The view carries state, effective next action, route,
and a full-details command bound to the actual workspace and state home.
`propose --full` prints the previous complete response. Internal propose!, run!,
ledger bytes and exit classification remain unchanged. An unsaved refusal keeps
its previous response. The view describes authority, not a generated diff.

A real public-CLI test derives a local-rename request from the count-name task
and copies the real diagnostic_delta.clj into a disposable workspace. Its
source-policy/rate/proof fields are explicitly synthetic display-test facts;
proposal runs neither provider nor proof. Default output was 1,107 bytes and
full output was 33,243 bytes. Full output equals the saved ledger map exactly;
source bytes stay unchanged. The retained live baseline and this derived
fixture are different requests; do not turn their byte counts into a matched
performance or adoption claim.

RED: six expected failures in the new proposal witness, including its 33,243-byte
default output. The final focused suite covers display, commit CLI and run,
including unsaved-refusal response/exit parity: 28 tests, 223 assertions, zero
failures or errors. Verification receipt:
`/var/tmp/forge/mission-propose-compact-final2.log`.

Two tests were added to the existing mission-run battery namespace, with no
registry edits. Formatting was applied to changed forms. Lint has zero errors
and the same two inherited warnings (mission_cli redundant let; mission_run_test
unused testing refer), confirmed against the base. All test telemetry uses a
scratch events file. No Git/publication/provider semantics changed.
