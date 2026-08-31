# Invalid prepared-actions measurement attempt 1

This attempt is excluded from every measurement and claim.

The harness accepted a relative `--result-dir` and passed its derived relative
workspace path to MCP processes whose current directories were the separate
control and candidate worktrees. The control's first eligible read therefore
refused `source-read-failed` against a nonexistent path. No candidate server
was launched and no measured comparison completed.

The retained response is
`control/11-eligible-inspect.response.json`. It names the incorrectly resolved
relative workspace. The successor harness resolves all three caller paths to
absolute paths before identity checks, fixture creation, or process launch.

No value from this directory is scored. The valid rerun must use a new output
directory and fresh arm processes.

