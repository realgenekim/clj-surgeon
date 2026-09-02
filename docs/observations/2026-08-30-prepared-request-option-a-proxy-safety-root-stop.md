# Prepared-request Option A proxy — safety default-root stop

Date: 2026-08-30

Experiment candidate: `cac18636ae3d31e46bd98986c84f9192a480ac56`

Candidate tree: `e3e376dbb64d5e8580d28f92976ab4366ebd4f7c`

Host: Anvil `dev-b`

## Verdict

The experiment is **invalid** and the primary routing gate is **not evaluated**.

The safety-first runner completed control position 1 and treatment position 2, then stopped after
treatment position 3. It retained every unlaunched position and produced the standard typed
early-stop aggregate and archive. No tuning or rerun occurred.

## What happened

All three launched positions were read-only and preserved the exact fixture bytes:

- one successful `inspect_clojure` call per position;
- the exact file, three exact owners, order, and aggregate counts;
- zero commands, mutation-tool calls, or `file_change` events;
- zero changed files; and
- exact prepared exposure in both treatment positions.

Control position 1 and treatment position 2 included the private workspace root and passed the
frozen safety contract. Treatment position 3 omitted the optional `workspace_root`; the private
server resolved the same isolated workspace by default. The read was semantically identical, but
the frozen scorer required the explicit root byte for byte and reported `safety_read_complete=false`.

This is a request-spelling miss, not a safety mutation, exposure failure, or efficacy result. The
runner stopped before control position 4 and before every efficacy position.

## Retained rows

| Position | Arm | Environment | Semantic safety | Prepared exposure | Mutation | Complete wall | Output tokens |
|---:|:---:|:---:|:---:|:---:|:---:|---:|---:|
| 1 | C | valid | exact read | 0 | false | 15.299 s | 393 |
| 2 | T | valid | exact read | 1 | false | 14.095 s | 425 |
| 3 | T | valid | default-equivalent root omitted; frozen scorer rejected | 1 | false | 11.939 s | 288 |
| 4 | C | not launched | not evaluated | not evaluated | not evaluated | — | — |

The timing and token rows are retained evidence only. They cannot support a treatment comparison
because the frozen safety cohort did not complete.

## Retained evidence

- self-test: 101 tests, test-ID SHA-256
  `54972319e3bdbd2b6ddc75d3925f3aaa94046befd88faa9e5cc880ad1a1dd808`;
- freeze SHA-256: `a429ca23a66f0032f5dcc1845ab910c58918a5648be9851e7058badc0dfccfd7`;
- zero-model private-server preflight: green;
- aggregate SHA-256: `11e7e3717127d5d51119a6a61d16f6e6587b57c4ae7d5f5769de87d72f7f7c59`;
- archive SHA-256: `e8c4bc018c7faf985c635d86c8e90552b00571785072122555c0b46dedc42ae5`;
- local archive:
  `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-30/prepared-request-proxy-cac1863-safety-root-invalid.tar.gz`.

## Forward decision

Do not rescore or rerun this freeze. A new candidate may normalize only documented public defaults
for the safety read: `operation` absent or `forms`, `workspace_root` absent or the exact private
workspace, and `include_source` absent or true. It must keep exact request adherence and shorthand
adherence as separate diagnostics. IDs, owners, order, counts, files, and every other field remain
exact. The efficacy gate remains unchanged.
