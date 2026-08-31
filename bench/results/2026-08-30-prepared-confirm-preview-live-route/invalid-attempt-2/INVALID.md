# Invalid prepared-actions measurement attempt 2

This attempt is excluded from every measurement and claim.

Both real MCP arms completed. Preview, full and compact commits, replay, and
ordinary edit behavior passed. The harness then correctly refused its no-cue
gate because its normalized ineligible-read objects still contained
`inspection_elapsed_ms`, a per-run timing field. That value was 11.319667 ms in
the control and 12.88975 ms in the candidate; it was the only normalized
difference.

The repair adds `inspection_elapsed_ms` to the already-declared timing-field
exclusion and makes the preview validity value explicitly boolean. Raw wire
payloads remain retained and unchanged. No value from this directory is
scored. The valid rerun must use fresh arm processes, workspaces, and a new
output directory.

