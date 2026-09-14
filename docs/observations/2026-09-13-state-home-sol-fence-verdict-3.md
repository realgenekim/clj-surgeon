GO-WITH-FIX

CLASS: final receipts composed after their size gate. Oracle: drive near-ceiling probe responses through every post-encoding composition path, asserting the final UTF-8 size, descriptor provenance, and cumulative truncation accounting.

`SH-ROUND3-01`: [`cli!`](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/probe.clj:301) appended `:image-file` after the 16,384-byte bound. The focused `bb --classpath src -e ...` probe reported:

```clojure
{:inner-bytes 16144,
 :old-final-bytes 16860,
 :fixed-final-bytes 971,
 :bound 16384,
 :image-preserved true,
 :omitted 1}
```

The uncommitted patch applies the whole-receipt bound after provenance is attached and preserves prior truncation counts. Its new regression is in [probe_state_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/probe_state_test.clj:310).

Verification:

- Focused JVM run: `2 tests / 57 assertions`, zero failures/errors.
- `~/bin/clj-kondo`: zero errors/warnings.
- `git diff --check`: clean.
- No `make test` was run.
- The remaining pressure points produced no blocker: canonical state-root admission precedes writes; workspace keys hash canonical paths; publication cleanup covers after-side-effect failures; the 240,000 ms namespace allowance is approximately twice the measured 116,659 ms while the unchanged battery ceiling passed at 1,282,675/1,800,000 ms; and telemetry comparison separately asserts JVM drop history before excluding it as process context.
- `git diff --exit-code 7bf44cfe... fa34f7df...` exited 0, confirming the sealed tree matches the reviewed tip.

I followed the native review/edit route prescribed by the working-tree [skill.md](/home/forge/src/clj-surgeon-fence/skill.md).

```text
<!-- SHIP-FIX-BLOCK
CANDIDATE: fa34f7dfc0b1e58ca2560d555bafe688e4ffebdc
FINDINGS: SH-ROUND3-01
REPAIR: Bound the complete probe receipt after descriptor provenance is attached while preserving cumulative truncation evidence.
PRODUCER: Codex <codex@openai.com> model=GPT-5 session=01a09d45-e3a0-7d61-87c6-628f8d29d47c
PATCH-MANIFEST:
  src/clj_surgeon/probe.clj +37 -27
  test/clj_surgeon/probe_state_test.clj +33 -0
SHIP-FIX-BLOCK -->
```

> END RECEIPT (fence-run): worktree HEAD at review exit = fa34f7dfc0b1e58ca2560d555bafe688e4ffebdc = fenced sha.
