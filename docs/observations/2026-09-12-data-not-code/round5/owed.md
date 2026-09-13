# Coverage and remaining work

Coverage is the shared artifact boundary and its inventoried consumers, not
every writer. Round 5 does not widen that coverage.

At subject base 4658dc124ee243300d19861402b2696ae1046321 (unchanged here):

- `src/clj_surgeon/mcp_telemetry.clj:70-78`: start! creates the caller's
  directory and writes session JSONL without admit-target!.
- `src/clj_surgeon/mcp_http_server.clj:234-240`: write-ready-file! creates
  parents and spits to arbitrary :ready-file without admit-target!.
- Home mismatch: telemetry's default-directory at mcp_telemetry.clj:17-20
  resolves the user.home system property; envelope passwd-home at
  receipt_artifacts.clj:62-71 resolves passwd. A -Duser.home override can move
  telemetry outside that envelope. Launch order does not supply admission:
  mcp_server.clj:345-348 and mcp_http_server.clj:250-259 initialize then start.
- Additional red-team inventory: mcp_recovery.clj:228-229 and
  telemetry_events.clj:340 remain outside this coverage.

Owed separately: cover these writers and reconcile home authority; independent
review of this correction; Darwin validation; filesystem races remain outside
the contract. No probe/native run or performance result exists in this block.

Other review notes retained: F4's shared symlink fixture weakens failure
localization; F5's test-owner sleep rule can require helper duplication; F8's
shortened remedy traded explanatory detail for the unchanged output ceiling.
