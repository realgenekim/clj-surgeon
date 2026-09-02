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

- [x] **MCP-OP-STUDY-007**: Every study receipt shall be bounded to `limit` payload characters, defaulting to 4096 and refusing above 16384; when the complete result does not fit, clj-surgeon shall return whole rows only, set `truncated=true`, report `returned` and `omitted`, and set `read_complete=false`; it shall never return a partially serialized row or a cut payload. It shall emit an executable `next_call` only while raising `limit` can still advance the receipt; at the maximum limit it shall emit `next_action="narrow_scope"` and a remedy and no executable call, because a narrower scope is a caller judgment and a continuation identical to the call just made is a loop.

## One kernel

- [x] **MCP-OP-STUDY-008**: For every study operation, on identical bytes and an untruncated receipt, the MCP receipt payload shall equal the JSON normalization of the `clj-surgeon.study` kernel result, which shall equal what the CLI handler returns; no entrance shall hold a second implementation of any study operation.

## No write authority

- [x] **MCP-OP-STUDY-009**: The MCP read entrance shall expose no write operation. `:mv`, `:rename-ns!`, and `:fix-declares!` shall not appear in the `inspect_clojure` operation vocabulary or mode vocabulary, `inspect_clojure` shall remain annotated read-only, and a study request shall leave every file byte unchanged.

## Refusals

- [x] **MCP-OP-STUDY-010**: If a `deps`, `ls-deps`, or `ls-extract` request names a `form` that is not a top-level owner of the file, clj-surgeon shall refuse that request with `error_type="study-form-not-found"`, the bounded factual owner vocabulary, and an executable `next_call` whose `form` hole must be replaced; the owner list shall carry no selection authority. If the requested file cannot be parsed, clj-surgeon shall refuse with the kernel's typed parse error rather than an empty result.

## Falsifiers

| ID | Defensible opposite to test | Required witness families |
|---|---|---|
| `MCP-OP-STUDY-001` | A directory-scoped read belongs in `requests` beside file reads. | `mode=ls-tree` success; text and edn formats; grep-filtered scan; the tool catalog still holding exactly four tools. |
| `MCP-OP-STUDY-002`–`005` | Each entrance may compute its own answer. | Per-operation MCP receipt versus kernel versus CLI handler on the same real file. |
| `MCP-OP-STUDY-006` | Confinement can be inferred from the caller's string. | `..` traversal; absolute path; missing directory; a file passed as `dir`; a symlink out of the root. |
| `MCP-OP-STUDY-007` | A tree small enough in practice needs no bound; any truncation may serve the same call back. | `:ls-tree :dir .` at 221,018 characters; a 1-character limit; `limit` above the maximum; the truncated receipt's `next_call` replayed; truncation already at the maximum limit serving no executable call. |
| `MCP-OP-STUDY-008` | Byte-identical CLI output proves one kernel. | Kernel/MCP/CLI three-way equality per operation, plus the ten-invocation CLI golden. |
| `MCP-OP-STUDY-009` | A read tool may expose a dry-run of a write. | The operation vocabulary; the mode vocabulary; the read-only annotation. |
| `MCP-OP-STUDY-010` | A missing owner may return an empty result. | Unknown form on each of the three form-taking operations; unparseable source. |
