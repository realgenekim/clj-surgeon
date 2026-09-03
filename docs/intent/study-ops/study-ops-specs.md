---
parent: study-ops-design
prefix: MCP-OP-STUDY
issue: clj-surgeon-0me
status: implemented; awaiting ratification
---

# Study operations at the MCP read entrance — specifications

IDs are stable and must not be reused if a requirement is deleted.

## Entrance and operations

- [x] **MCP-OP-STUDY-001**: When a caller submits `inspect_clojure` with top-level `mode="ls-tree"`, clj-surgeon shall scan the requested workspace-confined directory through the one `clj-surgeon.study/ls-tree` kernel and shall return a bounded receipt carrying `dir`, `grep`, `format`, `limit`, `project_count`, `file_count`, `returned`, `omitted`, `truncated`, and either `tree` (text) or `files` (edn); it shall introduce no new public MCP tool.
- [x] **MCP-OP-STUDY-002**: When an `inspect_clojure` request item declares `operation="deps"` with a project-relative `file`, clj-surgeon shall return the intra-namespace call graph produced by `clj-surgeon.study/deps` for that file's snapshot; when the item also declares `form`, it shall return only that owner's adjacency row.
- [x] **MCP-OP-STUDY-003**: When an `inspect_clojure` request item declares `operation="topo"` with a project-relative `file`, clj-surgeon shall return the `sorted`, `cycles`, and `has_cycles?` result produced by `clj-surgeon.study/topo` for that file's snapshot.
- [x] **MCP-OP-STUDY-004**: When an `inspect_clojure` request item declares `operation="ls-deps"` with a project-relative `file` and an exact `form`, clj-surgeon shall return the transitive dependency tree produced by `clj-surgeon.study/ls-deps`; `form` shall be required for this operation.
- [x] **MCP-OP-STUDY-005**: When an `inspect_clojure` request item declares `operation="ls-extract"` with a project-relative `file` and an exact `form`, clj-surgeon shall return the minimal extractable closure produced by `clj-surgeon.study/ls-extract`; `form` shall be required for this operation.

## Confinement

- [x] **MCP-OP-STUDY-006**: If an `ls-tree` request supplies a `dir` that is absolute, contains a parent-traversal or NUL segment, resolves outside the canonical workspace root through a symlink, does not exist, or is not a directory, clj-surgeon shall refuse before scanning with a typed `error_type`, `source_unchanged=true`, `read_complete=false`, and an executable `next_call`; it shall publish no file listing, no host-absolute path, and no partial tree.

## Bounding

- [x] **MCP-OP-STUDY-007**: Every study receipt shall be bounded to `limit` payload characters, defaulting to 4096 and refusing above 16384; when the complete result does not fit, clj-surgeon shall return whole rows only, set `truncated=true`, report `returned` and `omitted`, and set `read_complete=false`; it shall never return a partially serialized row or a cut payload. It shall emit an executable `next_call` only while raising `limit` can still advance the receipt; at the maximum limit it shall emit `next_action="narrow_scope"` and a remedy and no executable call, because a narrower scope is a caller judgment and a continuation identical to the call just made is a loop. This applies equally to an atomic result that refuses with `study-output-limit` rather than truncating, and to an `ls-tree` refusal whose proposed continuation would repeat the request just made.

## One kernel

- [x] **MCP-OP-STUDY-008**: For every study operation, on identical bytes and an untruncated receipt, the MCP receipt payload shall equal the JSON normalization of the `clj-surgeon.study` kernel result, which shall equal what the CLI handler returns; no entrance shall hold a second implementation of any study operation.

## No write authority

- [x] **MCP-OP-STUDY-009**: The MCP read entrance shall expose no write operation. `:mv`, `:rename-ns!`, and `:fix-declares!` shall not appear in the `inspect_clojure` operation vocabulary or mode vocabulary, `inspect_clojure` shall remain annotated read-only, and a study request shall leave every file byte unchanged.

## Refusals

- [x] **MCP-OP-STUDY-010**: If a `deps`, `ls-deps`, or `ls-extract` request names a `form` that is not a top-level owner of the file, clj-surgeon shall refuse that request with `error_type="study-form-not-found"`, the bounded factual owner vocabulary, and an executable `next_call` whose `form` hole must be replaced; the owner list shall carry no selection authority. If the requested file cannot be parsed, clj-surgeon shall refuse with the kernel's typed parse error rather than an empty result.

## Safety of discovery

- [x] **MCP-OP-STUDY-013**: When `ls-tree` discovery reads a `deps.edn`, `bb.edn`, or `project.clj` to learn a project's source paths, clj-surgeon shall READ that file and shall never evaluate it: EDN build files shall be read with `clojure.edn/read-string`, `project.clj` shall be read with `*read-eval*` bound false, and a build file that cannot be read shall fall back to the documented default source paths without executing any part of it.

- [x] **MCP-OP-STUDY-014**: `ls-tree` discovery shall be confined to the canonical realpath of the scanned directory: every path a directory walk produces shall be dropped unless its own realpath resolves inside that root, and every source directory a build file declares shall be lexically normalized and dropped unless it stays inside that root; a directory that cannot be canonicalized shall refuse with `error-type=:dir-not-found` rather than scan. No file outside the root shall appear in `tree`, `files`, or `file_count`, and no such file shall be opened.

- [x] **MCP-OP-STUDY-015**: `ls-tree` shall bound the work it does BEFORE any receipt bound applies. Discovery shall list file names without parsing any file and shall refuse with `error_type="study-tree-too-large"` — naming the discovered count, the cap, and the remedy — when it finds more than `max_files` source files (default 2000, caller-settable up to 20000). Outlining shall then parse only as far as the receipt's byte budget reaches, in bounded-parallel batches, so the number of files parsed never exceeds the number returned by more than one batch.

- [x] **MCP-OP-STUDY-016**: Every study receipt shall report the source it actually read and shall keep every key it returns inside `limit`. `source_character_count` shall equal the character count of the file snapshot the request was evaluated against, and shall be a REPORT only — the falsified half of this requirement was the assumption that a count of source READ could also serve as the charge against a budget on source RETURNED; see MCP-OP-STUDY-020. `topo` shall count and budget `cycles` alongside `sorted`, so an all-cycle file reports a non-zero `form_count`; `ls-extract` shall charge its closure's non-`forms` keys to the same budget; and an atomic single-row result (`deps` with `form`, `ls-deps`) that does not fit shall refuse with `study-output-limit` rather than be returned over budget.

- [x] **MCP-OP-STUDY-017**: The `format="text"` payload's trailing total line shall agree with the receipt it travels in: it shall report the true discovered `file_count`, and when the receipt is truncated it shall also report how many files were shown and how many omitted, and shall omit the form total, which is unknowable for files that were never parsed. A complete receipt's total line shall be unchanged.

- [x] **MCP-OP-STUDY-018**: A bounded row payload shall charge the serialized array's own brackets and separators to `limit`, so a kept payload is never larger than the limit it was bounded by; the empty array's two characters are the floor below which no bound can go.

- [x] **MCP-OP-STUDY-019**: The `ls-tree` branch shall validate its own parameters server-side, because it never reaches `validate-inspect-params`. An unrecognized top-level key shall refuse with `error_type="unknown-parameter"` naming the unknown keys and the complete vocabulary; a `format` outside `{names, text, edn}` shall refuse with `error_type="invalid-format"` naming what was rejected and what is supported, and its continuation shall drop the rejected value rather than echo it.

## Budgets, discovery, and continuations (re-review round three)

- [x] **MCP-OP-STUDY-020**: The per-request source budget shall be charged against the source a result RETURNS, never against the source it read. `outline` and every study operation return a derived structure and no source, so they shall succeed with `ok=true` and `read_complete=true` on a file of any size the per-request RESULT budget can carry, while still reporting `source_character_count` as the characters read (MCP-OP-STUDY-016). A result that does return source shall still refuse when that returned source exceeds the budget.

## Table-of-contents rendering

- [x] **MCP-OP-STUDY-011**: `ls-tree` shall support `format="names"`, whose per-file entry shall be exactly `{file, ns, form_count, line_count}` and no other key; when an `ls-tree` request declares no `format`, clj-surgeon shall render `format="names"` by default when `grep` is absent from the request, and `format="text"` by default when `grep` is present; `format="text"` and `format="edn"` shall remain available on explicit request regardless of `grep`.
- [x] **MCP-OP-STUDY-012**: `ls-tree` shall support an `ns_grep` parameter that filters files by each file's project-relative path (which the Clojure require convention keeps in lockstep with its declared namespace — path segment vs. ns segment, `_` vs. `-`), never by file contents; it shall compose with `grep` (an existing content match) to narrow further, and shall be documented as distinct from `grep`, which matches file bodies via ripgrep and can match comments, strings, and unrelated substrings.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-STUDY-001` | A directory-scoped read belongs in `requests` beside file reads. | `mode=ls-tree` success; text and edn formats; grep-filtered scan; the tool catalog still holding exactly four tools. |
| `MCP-OP-STUDY-002`–`005` | Each entrance may compute its own answer. | Per-operation MCP receipt versus kernel versus CLI handler on the same real file. |
| `MCP-OP-STUDY-006` | Confinement can be inferred from the caller's string. | `..` traversal; absolute path; missing directory; a file passed as `dir`; a symlink out of the root. |
| `MCP-OP-STUDY-007` | A tree small enough in practice needs no bound; any truncation may serve the same call back. | `:ls-tree :dir .` at 221,018 characters; a 1-character limit; `limit` above the maximum; the truncated receipt's `next_call` replayed; truncation already at the maximum limit serving no executable call; an atomic `study-output-limit` refusal at the ceiling serving no executable call; an `ls-tree` refusal whose `{:dir "."}` continuation would equal the request just made serving none. |
| `MCP-OP-STUDY-008` | Byte-identical CLI output proves one kernel. | Kernel/MCP/CLI three-way equality per operation, plus the ten-invocation CLI golden. |
| `MCP-OP-STUDY-009` | A read tool may expose a dry-run of a write. | The operation vocabulary; the mode vocabulary; the read-only annotation. |
| `MCP-OP-STUDY-010` | A missing owner may return an empty result. | Unknown form on each of the three form-taking operations; unparseable source. |
| `MCP-OP-STUDY-013` | A build file in a scanned tree is trusted input, so the ordinary Clojure reader is fine. | A `deps.edn`, `bb.edn`, and `project.clj` each carrying `#=(clojure.core/spit …)`: the named file is never written, and clean build files still yield their declared source paths. |
| `MCP-OP-STUDY-014` | A path found under the root is by construction inside the root. | A `.clj` symlink whose target is outside the root, and a sibling `deps.edn` declaring `:paths ["../../.."]`: neither the link nor anything the traversal reaches appears in `tree`/`files`/`file_count`, at every format. |
| `MCP-OP-STUDY-015` | A tree small enough to return is small enough to parse whole; the receipt bound is the only bound needed. | A 3000-file tree at the default limit: a typed refusal naming count and cap, nothing parsed, elapsed inside a fixed budget. A 400-file tree under the cap: the receipt returns a few dozen files and at most one batch beyond that is parsed. |
| `MCP-OP-STUDY-016` | A receipt's declared counts are decoration; only the payload matters. | `source_character_count` equal to `(count (slurp file))` for every study operation and for `outline`; an all-cycle fixture reporting its forms and truncating its cycles inside `limit`; a single adjacency row and a closure envelope each refusing rather than exceeding a tight `limit`. |
| `MCP-OP-STUDY-017` | The body of a truncated payload may describe only itself. | A truncated `text` receipt whose `tree` contains `total: <file_count>` and `<omitted> omitted`; an untruncated one whose total line is byte-identical to the frozen CLI golden's. |
| `MCP-OP-STUDY-018` | Charging each row one separator accounts for the array. | Every limit from 1 to 400 over a fixed-width row set: the kept payload's JSON character count never exceeds the limit. |
| `MCP-OP-STUDY-019` | A JSON schema with `additionalProperties false` is a server-side check. | `format="EDN"` refusing with `invalid-format` and a continuation carrying no `format`; an unknown key refusing with `unknown-parameter`; every documented parameter together still succeeding. |
| `MCP-OP-STUDY-020` | A receipt that declares how much it read may be charged that number by the output budget. | `outline` on each of the seven files in this repository above 65,536 characters returning `ok=true`, `read_complete=true` and `source_character_count = (count (slurp file))`; a `forms` result whose returned source still refuses at a lowered budget. |
| `MCP-OP-STUDY-011` | A whole tree can be rendered without a compact per-file rendering; the default format may stay `text` regardless of `grep`. | `names` rendering of a many-file fixture fits the default limit with `read_complete=true`; a `names` entry has exactly the four keys `file`/`ns`/`form_count`/`line_count`; the default is `names` with no `grep` and `text` with `grep`; `text`/`edn` remain reachable on explicit request. |
| `MCP-OP-STUDY-012` | `grep`'s content match already answers a namespace/path question; a new parameter is unneeded. | `ns_grep` on a fixture whose file bodies mention the pattern outside the matching namespaces returns only the path/namespace-matching files, not the content-matching decoys; `ns_grep` composes with `grep`. |
