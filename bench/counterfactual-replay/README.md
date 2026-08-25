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

The initial cases were selected by change shape before their diffs were reviewed:

| Case | Stratum | Historical change |
| --- | --- | --- |
| `cclsp-optional` | small owner edit | one source owner and its focused assertion |
| `plain-nrepl` | native-positive | one source simplification and one regression test |
| `failure-atomic-commit` | multi-form transaction | source substrate, six failure-path tests, and observation update |

The frozen alternate candidates are `d0afc776`, `e93faa7b`, and `44bf0cb7`. Do not substitute an
easier-looking case after seeing a treatment failure; add a new versioned cohort instead.
