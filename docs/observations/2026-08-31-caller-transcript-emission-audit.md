# Caller transcript emission audit: the fast path removed the old body, not the ceremony

**Date:** 2026-08-31  
**Evidence:** one streamed Claude session JSONL, 13,178 records and 27,040,186 bytes  
**Privacy rule:** this note reports byte counts, timestamps, and only the short identifiers needed to distinguish the two calls. It does not reproduce source, prompts, tool results, confirmation values, or shell payloads.

## Executive verdict

| Claim | Verdict | Transcript evidence |
|---|---|---|
| Refactor #1 emitted about 4.3 KB and paid the `from` tax. | **CAVEAT** | The tax is confirmed: `from` was 1,838 B and `to` was 2,946 B. The complete compact tool-input JSON was 5,063 B, however, not about 4.3 KB. |
| Refactor #2 emitted roughly half as much because it sent `to` plus ceremony but no `from`. | **UNSUPPORTED as phrased** | The heredoc `to` body was 2,264 B, which is 47.32% of #1's 4,784 B old-plus-new source subtotal. But the complete command was 4,276 B because ceremony added 2,012 B. That is 84.46% of #1's complete 5,063 B input, not roughly half. |
| Caller-turn walls are consistent with about 60 s versus 25--35 s. | **CAVEAT** | The requested assistant-start-to-result envelopes were 53.695 s for #1 and 18.683 s for #2. They confirm a large directional reduction (2.87x), and #1 is reasonably described as about a minute, but #2 is below the asserted 25--35 s range. |

The important split is therefore real but narrower than the headline: the fast path removed shadow-typed old source. Its transport and confirmation ceremony consumed almost all of that byte saving at the caller-emission boundary.

## Measurement method

The audit read only:

`/Users/genekim/.claude/projects/-Users-genekim-src-local-kiloclaw/568ebcd5-3508-4173-9586-54975a3914ed.jsonl`

Every pass was record-streaming (`jq` over JSONL); no pass slurped the file and no session directory was searched. Byte counts are UTF-8 bytes. A tool-input JSON size is the compact JSON serialization of the recorded `tool_use.input` object. A field size is the decoded string's UTF-8 length. The heredoc count includes the terminal newline written into the temporary `to` file. "Ceremony" is total command bytes minus those heredoc bytes.

Wall time uses the earliest recorded assistant block in the complete caller turn through the matching `tool_result`. The narrower timestamps are included so the transcript's observation boundary is explicit; JSONL timestamps do not reveal when inference began before the first emitted assistant block.

## Refactor #1: `eligible-descriptor`

The unique target was the `edit_clojure` call whose single edit had `eligible-descriptor` in both `from` and `to` (tool fragment `toolu_01Vn...Ruh`).

| Measure | Bytes |
|---|---:|
| `from` field | 1,838 |
| `to` field | 2,946 |
| Source-field subtotal | 4,784 |
| Complete tool-input JSON | 5,063 |
| JSON structure/escaping above decoded source subtotal | 279 |

The `from` field alone was 36.30% of the complete input and 38.42% of the decoded source-field subtotal. The transcript therefore directly confirms that the caller re-emitted the old body despite having just received a visible `prepared_confirmation` result.

### Timestamp envelope (UTC)

| Boundary | Timestamp |
|---|---|
| First assistant block in the caller turn | `2026-08-31T12:28:45.366Z` |
| Visible prepared-confirmation tool result | `2026-08-31T12:28:49.253Z` |
| First assistant block after that result | `2026-08-31T12:29:24.763Z` |
| `edit_clojure` tool use | `2026-08-31T12:29:38.813Z` |
| Matching tool result | `2026-08-31T12:29:39.061Z` |

Measured spans:

- Complete assistant-start-to-result envelope: **53.695 s**.
- Prepared-result-to-edit-result span: **49.808 s**.
- First post-result assistant block to edit result: **14.298 s**.
- Tool-use-to-tool-result execution interval: **0.248 s**.

The 53.695-second envelope is the comparable caller-turn wall requested here. The narrower values show that the transcript cannot attribute the whole envelope to payload construction alone.

## Refactor #2: `form-evidence?`

The unique target was the Bash call containing the `refactor2-to.clj` heredoc and the prepare/confirm/fill sequence (tool fragment `toolu_01Bx...UPw`).

| Measure | Bytes |
|---|---:|
| Heredoc `to` body, including terminal newline | 2,264 |
| Remaining command ceremony | 2,012 |
| Complete command string | 4,276 |
| Complete Bash tool-input JSON | 4,529 |

The command carried no `from` field. Its ceremony was 47.05% of the command string.

### Timestamp envelope (UTC)

| Boundary | Timestamp |
|---|---|
| First assistant block in the caller turn | `2026-08-31T12:47:36.303Z` |
| Visible assistant text block | `2026-08-31T12:47:38.400Z` |
| Bash tool use | `2026-08-31T12:47:54.410Z` |
| Matching tool result | `2026-08-31T12:47:54.986Z` |

Measured spans:

- Complete assistant-start-to-result envelope: **18.683 s**.
- Text-block-to-result span: **16.586 s**.
- Tool-use-to-tool-result execution interval: **0.576 s**.

This is materially faster than #1's 53.695-second envelope, but it does not directly substantiate a 25--35-second measurement. It is lower.

## Like-for-like byte comparison

| Comparison | Result |
|---|---:|
| #2 heredoc `to` / #1 `from + to` | 47.32% |
| #2 command / #1 tool-input JSON | 84.46% |
| #2 Bash tool-input JSON / #1 tool-input JSON | 89.45% |
| Complete command saving (#1 input minus #2 command) | 787 B (15.54%) |

Thus the transcript supports "about half" only for semantic source bytes: one new body versus one old body plus one new body. Once the caller's own shell ceremony is included, the observed emission saving is 15.54% using the requested total-command comparison (or 10.55% when both complete tool-input JSON objects are compared).

## Insight pass: other shadow-typing instances

Across the complete specified transcript, the scan found:

- one `edit_clojure` call with a large `from` field after a visible `prepared_confirmation`: refactor #1, at 1,838 B;
- zero **other** instances in that class;
- one other literal `from` field, only 18 B, with no ambiguity about whether it belongs to the large-body class.

There is therefore **one waste-observatory validation datum total and zero additional examples beyond the audited target**. The corpus does not support multiplying this incident into a broader same-night count, but the one incident is clean: prepared authority was visible, and the caller still shadow-typed 1,838 bytes of old source.

## Bottom line

The caller's own emission record confirms the mechanism but corrects the magnitude. Refactor #1 paid a real 1,838-byte `from` tax. Refactor #2 eliminated that old-body emission, making its semantic source payload about half as large, but 2,012 bytes of command ceremony reduced the end-to-end emission saving to 787 bytes. The wall record shows a strong 53.695 s to 18.683 s reduction, while not reproducing the stated 25--35-second second-leg range.
