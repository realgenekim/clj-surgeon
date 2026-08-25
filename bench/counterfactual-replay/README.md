# Commit counterfactual replay capsules

These capsules replay real historical changes from their parent commits. The caller receives only
`task.md` and a parent checkout. `capsule.edn`, the child commit, Git history, child tree, prior
runs, and oracle hashes are withheld.

Run the zero-model contract verifier from the repository root:

```bash
bb bench/verify_counterfactual_replay.clj
```

Materialize a blinded parent workspace and an external receipt with:

```bash
bench/materialize_counterfactual_replay.sh \
  cclsp-optional \
  /absolute/path/to/fresh-workspace \
  /absolute/path/to/materialization.edn
```

The destination must not exist. The materializer exports the parent tree, initializes an unrelated
one-commit Git repository with no remote, verifies every declared starting hash, and leaves the
task/capsule/oracle outside the caller workspace.

Run one scored arm with:

```bash
REPLAY_MCP_URL=http://127.0.0.1:7888/mcp \
bench/run_counterfactual_replay.sh cclsp-optional structural /absolute/fresh/result-dir
```

Valid arms are `native`, `structural`, and `production`. Set `REPLAY_EXEC_USER` when a root-owned
Anvil harness should execute Codex as a named seat. Set `REPLAY_MCP_WRITE_USER=surgeon` to grant
that shared service access only to the disposable workspace. `REPLAY_DRY_RUN=true` exercises
materialization, auth/config isolation, prompt construction, and refusal gates without a model
call.

For a production-arm prompt intervention, set `REPLAY_ROUTE_CARD_FILE` to a versioned Markdown
file. The runner appends it to the normal production route, copies it into the result directory,
and refuses the override for forced native or structural arms. This preserves the standard
safety/blinding prompt while making routing experiments attributable.

The initial cases were selected by change shape before their diffs were reviewed:

| Case | Stratum | Historical change |
| --- | --- | --- |
| `cclsp-optional` | small owner edit | one source owner and its focused assertion |
| `plain-nrepl` | native-positive | one source simplification and one regression test |
| `failure-atomic-commit` | multi-form transaction | source substrate, six failure-path tests, and observation update |

The frozen alternate candidates are `d0afc776`, `e93faa7b`, and `44bf0cb7`. Do not substitute an
easier-looking case after seeing a treatment failure; add a new versioned cohort instead.
