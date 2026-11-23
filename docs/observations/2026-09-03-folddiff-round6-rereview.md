# curtain-call fold-diff-tool 66325423 — Sol executed round-6 re-check: NO-GO for the read at the tip (store dependency + Postgres refusal CLOSED; the required-var scan fails open on a comment-only reference + hidden dynamic resolve; fixture not representative) — round 8 launched; the GO at 347fe6d3 stands

## NO-GO

Do not replace `347fe6d3` with `66325423` for the production read yet. The three original defects are substantially addressed, but the required-vars safety gate still has a reproducible fail-open path.

1. **CLOSED — real-store witness dependency.** [fold_diff_test.clj:78](test/cfp_scheduler_killer/fold_diff_test.clj:78): present and absent suites both produced `1100/13409/0`; decoded straces showed real-store opens `0`, with private-store opens `3`, `3`, and `5`.

2. **PARTIAL — required-var guard.** [fold-diff-checkpoint:140](bin/fold-diff-checkpoint:140), [guard:259](bin/fold-diff-checkpoint:259): the dynamic-`requiring-resolve` empty-set scratch correctly returned `REFUSED :required-vars-unresolved`, exit 2, before any worktree or emit; however, an inline-comment-only `checkpoint/validate` plus a hidden dynamic `checkpoint-path` produced `validate`, guard exit 0, and a validate-only own copy was declared complete. This violates FOLD-DIFF-013’s “every referenced var” contract at [registry.edn:781](docs/intent/registry.edn:781).

3. **CLOSED — Postgres/data-dir refusal.** [fold-diff-checkpoint:322](bin/fold-diff-checkpoint:322): with `BASELINE_REF` unset, the probe returned `REFUSED :data-dir-with-postgres`, exit 2; strace recorded zero `connect`/`sendto`/`recvfrom` calls and no JDBC log line. Precision: `BASELINE_REF` is assigned at line 300, but it is not validated or resolved before refusal.

4. **PARTIAL — fixture representativeness.** [fold_diff_test.clj:78](test/cfp_scheduler_killer/fold_diff_test.clj:78): despite its name, `multi-session-log-lines` contains one `event.created` and five speaker announcements—no session event, no `[:sessions <id>]` redaction path, and no forward `live-max > frontier` gap. The isolation witness only checks exact match versus `:log-shorter-than-checkpoint` at [line 878](test/cfp_scheduler_killer/fold_diff_test.clj:878). Separate unit tests cover a real gap at [line 550](test/cfp_scheduler_killer/fold_diff_test.clj:550) and session redaction at [line 1187](test/cfp_scheduler_killer/fold_diff_test.clj:1187), but the new worktree-crossing fixture itself is not representative.

5. **CLOSED — execution gates.** `make compile-check` passed; registry suites were `1/160/0`, `2/612/0`, and `6/298/0`; all JVMs ran under `suite-run` with 512 MiB heaps.

Driver exit was 0. Its summary:

```text
✓ bin/fold-diff-checkpoint: refuses without BASELINE_REF; both-sides-same-ref is IDENTICAL and says it is vacuous;
  origin/main vs HEAD falls back to the candidate's store_checkpoint.clj (own copy lacks: validate), named,
  with a diff-stat, and reaches a real verdict; a forced baseline-emit failure is exit 3; a ref predating
  store_checkpoint.clj falls back to the candidate's, named; HEAD~1's own copy has validate and is used as-is;
  an empty/validate-missing required-var set REFUSES (:required-vars-unresolved) before any emit; and
  FOLD_DIFF_DATA_DIR set together with STORE_BACKEND=postgres REFUSES (:data-dir-with-postgres) before any worktree
```

The checkout remains at `66325423`, `data/store/events.jsonl` is restored absent, and the only Git-visible changes remain the pre-existing `.codex/config.toml` disablement.