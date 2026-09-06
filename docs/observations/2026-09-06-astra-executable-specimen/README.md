# Reproduce the archived-specimen check

Requirements: Node.js and Babashka (`node` and `bb` on PATH). No MCP server,
provider key or original workspace is required. Paths in `specimens.json` resolve
relative to that manifest, so callers may run these commands from any directory:

```sh
node /absolute/checkout/docs/observations/2026-09-06-astra-executable-specimen/specimen.js /absolute/checkout/docs/observations/2026-09-06-astra-executable-specimen/specimens.json
bb /absolute/checkout/docs/observations/2026-09-06-astra-executable-specimen/specimen.clj /absolute/checkout/docs/observations/2026-09-06-astra-executable-specimen/specimens.json
node /absolute/checkout/docs/observations/2026-09-06-astra-executable-specimen/hand-drive.js /absolute/new-output-directory
```

The output directory must not already exist. The hand-drive checks both renderers,
three byte-identical examples, and four refusal cases per renderer. It retains all
new results there. The checked-in `results/runs.json` remains the original run.

`fixtures/` contains the exact retained request and result files from the synthetic
example execution. Their historical workspace paths are inert data: these scripts
read only the request/receipt files named in the manifest, never the recorded
workspace. No mutation or request replay is performed. Do not replay these requests
against a real repository without choosing and validating its inputs.

These trusted archived fixtures prove internal request/receipt agreement and the
bounded checks described in the report. They do not authenticate execution,
establish current source freshness or prove task semantics. The fan-out count
interpretation is witnessed only on these all-one match fixtures. This is a research
example, not a general input validator. Historical result bytes and hashes are
preserved; fresh timing numbers include local runtime and cache conditions.

For a check that copies this directory to a fresh location, runs from an unrelated
cwd and removes its own temporary output afterward:

```sh
TMPDIR=/var/tmp node /absolute/checkout/docs/observations/2026-09-06-astra-executable-specimen/verify-portable.js
```

This additionally verifies that the historical result file is unchanged.
