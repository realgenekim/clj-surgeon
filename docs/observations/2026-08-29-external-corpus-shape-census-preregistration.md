# External-corpus edit-shape census: preregistration

Date: 2026-08-29 PT  
Status: frozen before any native-write, subject-repetition, edit-shape, or reach count

## Question

Do the five Grade-A stops measured in clj-surgeon's circular research corpus
survive in unrelated Clojure repositories?

The five claims under stress are:

1. declared-intent compression loses because write subjects rarely repeat;
2. guarded mnemonic labels lose because the declaration cost is almost always consumed;
3. mnemonic sizing does not amortize;
4. insertion is too small a share for a splice-specialized representation;
5. the write-side structural-tool ceiling is small.

This is a counting study, not a model cohort. It cannot promote a product
mechanism. It may only confirm a stop or reopen one mechanism for a new screen.

## Frozen window and providers

Use the same UTC window as the circular-corpus ledger:

```text
[2026-08-22T00:00:00Z, 2026-08-30T02:09:33.141926Z)
```

Codex and Claude are separate populations at every rung. No Claude result may
be transferred to Codex and no Codex adoption rate may be described as a fleet
rate. If no eligible Claude session exists for the selected repositories, the
Claude denominator is reported as zero/unmeasured rather than inferred from the
separate 16/16 Surgeon-first result.

## Repositories selected before counting shapes

A bounded session-metadata-only inventory found two non-Surgeon Clojure roots
with more than one Codex session start in the date directories. Selection used
only session availability and a repository-owned `deps.edn`; it did not inspect
write actions or source:

| neutral label | root SHA-256 | session starts visible in date inventory | why selected |
|---|---|---:|---|
| external A | `11c8bfbec76cd2a8757a1799f1d85ec034b2aa60b4140e9001019e55803f38cf` | 17 | largest non-Surgeon Clojure population; also owns `bb.edn` |
| external B | `8577c2c0088a5424feb4c00d812b0de677dc09c8de12ff4419b41f96be474006` | 12 | second-largest non-Surgeon Clojure population |

Single-session Clojure roots are excluded from the primary comparison because
they cannot distinguish repo shape from one task. They remain a named coverage
limit rather than being pooled to inflate N.

## Frozen units and eligibility

### Mission

One Codex task turn (or one Claude conversation turn, reported separately)
whose recorded working directory is inside external A or B and whose event
timestamp is inside the frozen window.

### Native write

One retained native write action that updates an existing `.clj`, `.cljc`, or
`.cljs` file. New files and prose remain valid native controls but are kept in
the raw reach denominator and excluded from the existing-Clojure edit-shape
population. Shell writes count only when the retained classifier proves a
bounded target and write effect; mentions and read commands never count.

### Subject repetition

The primary conservative subject is the canonical repository-relative target
file within one mission. Repetition is `occurrences >= 2` for that subject.
This is an **upper bound** on true owner/subform repetition because independent
owners in one file collapse together. When retained patch context provides a
stable named owner, also report the narrower `(file, owner)` result; missing
owner evidence remains missing and is never guessed.

Report occurrences, distinct subjects, repeated subjects, and the fraction of
subjects repeated. A file-level threshold crossing only reprices the mechanism
after the owner-level evidence is reported; a file-level miss confirms the stop
because a narrower identity cannot repeat more often.

### Edit shape

Classify each retained patch hunk independently:

- insertion: added bytes > 0 and removed bytes = 0;
- deletion: removed bytes > 0 and added bytes = 0;
- replacement: both are > 0;
- unknown: the retained action does not expose faithful hunk bytes.

Count-weighting uses classified hunks. Byte-weighting uses UTF-8 changed-byte
mass (`added + removed`) for the same hunks. Unknowns remain a separate
denominator and cannot be silently assigned to a favorable class.

### Reach ladder

Report these cumulative Codex rungs per repository:

1. all native write actions;
2. updates to existing Clojure files;
3. minus comment-only and at-most-four-line single-hunk edits;
4. minus files created earlier in the same mission;
5. remaining edits under `src/` or `test/`;
6. successful structural-tool mutation calls, separately by public tool.

Every exclusion is reported both as a count and as a fraction of the all-native
denominator. The ladder is a routing ceiling, not an adoption mandate.

## Registered predictions

Predictions apply separately to external A and B:

1. fewer than 20% of conservative file subjects repeat within a mission;
2. insertion remains below 40% by hunk count and by changed-byte mass;
3. successful Surgeon mutation calls remain below 5% of native plus Surgeon
   mutation actions in Codex sessions;
4. the final `src/` or `test/` addressable rung remains below 15% of all native
   writes because new files and small literal work stay native-shaped.

The prior supplied by the dispatch is adopted rather than tuned after seeing
external data.

## Kill and reprice laws

- If either external repository has `occurrences >= 2` for at least 30% of
  conservative file subjects, declared-intent and label arithmetic **REPRICE**.
  Reopening product work still requires owner-level evidence or a dedicated
  screen; file repetition alone is not semantic authority.
- If insertion reaches at least 50% of classified changed-byte mass in either
  repository, the splice-shape argument **REPRICES**.
- If neither threshold fires, the Grade-A graveyard is **CONFIRMED on the
  external denominator**.
- A reach result above 5% falsifies the Codex-specific adoption prediction but
  does not by itself validate any grammar. Claude remains separate.

No model calls, source reads, product changes, installation, reload, or shared
runtime mutation are authorized.

