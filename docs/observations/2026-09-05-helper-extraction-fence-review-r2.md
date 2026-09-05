# GO-WITH-FIX

1. **Finding 1 — CLOSED** — a reviewer-owned wrapper observed commit entry, a successful real kernel return, destination existence, and changed source bytes before throwing; the boundary then restored the complete pre-tree and removed the destination.
2. **Finding 2 — CLOSED** — empty configured profile authority admits nothing, including the server's built-in `fast` name.
3. **Finding 3 — CLOSED** — malformed argv profiles and nonexistent absolute executables are not admitted and refuse before staging.
4. **Finding 4 — CLOSED** — a source path that disagrees with its declared namespace refuses with `helper-extraction-destination-not-derivable`; no project-root destination is guessed.
5. **Finding 5 — CLOSED** — verified-restoration receipts are O(1); the 1-file and 1,000-file receipts were 902 and 908 printed bytes.
6. **Finding 6 — CLOSED** — a configured details/receipt directory inside the workspace refuses before staging and is not created.
7. **Finding 7 — CLOSED** — outward and inward source symlinks are both pruned, unread, and uncounted without aborting the valid extraction plan.
8. **Finding 8 — CLOSED** — a traversing configured source root is explicitly refused and is not reported as admitted.
9. **Finding 9 — CLOSED** — a timed-out child reports both `timed_out true` and `fresh_process true`.
10. **Finding 10 — CLOSED** — the registered public tool function's uninitialized refusal carries an explicit `next_call nil`.
11. **Finding 11 — PARTIAL** — the schema now names rollback authority and public refusals carry valid `elapsed_ms`, but the schema still does not encode the documented terminal/refusal variants or make each variant's authority conditionally required.

Independent FENCE REVIEW round 2 of frozen `helper_extraction` candidate
`d337964eac2d8046e0d785f8ec29824db6e48fd4`, completed at
`2026-09-05T07:22:24Z`. Reviewer: Codex, the independent reviewer named by the
lead; no builder role. Review scope remained bounded to reader/path-walk,
subprocess, profile admission, rollback, and exposed authority.

The candidate is **GO-WITH-FIX**. The product's round-1 CRITICAL and HIGH
failures closed under execution. Two bounded review defects remain: finding
11's schema correction is descriptive rather than a machine-enforced variant
contract, and the branch's own post-commit regression test is false-green
because it resolves a nonexistent kernel Var. The independent probe used the
actual production kernel and proves that the corrected product behavior works.

## Apparatus

All reviewer fixtures and probe programs lived under
`/var/tmp/forge/helper-fence-fx` and were deleted after the verdict was filed.
No MCP server was started and no port was contacted. Exactly two constrained
JVMs were launched:

```text
env TMPDIR=/var/tmp/forge/helper-fence-fx \
  JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/helper-fence-fx \
  CLJ_SURGEON_HELPER_TMP=/var/tmp/forge/helper-fence-fx \
  taskset -c 6-9 nice -n 10 \
  clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps \
  /var/tmp/forge/helper-fence-fx/fence_probe.clj
```

The second command differed only in the final program name,
`fence_probe_2.clj`. The first JVM emitted every requested result and the
completed test summary, then retained background agent threads; it was
interrupted after five idle seconds. The second JVM exited 0.

The corrected branch's supplied suites reported:

```text
Ran 64 tests containing 1025 assertions.
0 failures, 0 errors.
SUPPLIED WITNESS SUMMARY => {:test 64, :pass 1025, :fail 0, :error 0, :type :summary}
```

That green summary does not cure the new false-green witness finding below.

## Eleven probe results

### 1. Post-commit throw — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:1119-1352`.

Exact probe: materialize the happy pre-tree; inject `:commit!`; record entry;
call the actual production Var `clj-surgeon.mcp-extraction/commit!`; after it
returns, read destination existence and hash the source; record those facts;
throw; then compare every original path and the destination on the final tree.

```text
PROBE 1B strengthened-post-commit-throw =>
{:observations
 [{:event :commit-entry}
  {:event :after-real-commit-return,
   :kernel-ok true,
   :destination-exists true,
   :source-hash-before "e35743d6e4acaa6eb5aab4abac7f091a3fc412de73cf4cf89a0e1ab2796b2409",
   :source-hash-after "85f8510801388bb6e7bf4710ceed973dbb3aa95a00935715da6d79ead261fde3",
   :source-changed true}],
 :terminal
 {:ok false,
  :status "verification-failed",
  :committed false,
  :restored true,
  :source_unchanged true,
  :destination_created false,
  :cause_error "reviewer throw after observed commit"},
 :final-tree-restored true,
 :final-destination-exists false}
```

This meets the lead's R2 requirement. The witness saw both COMMIT ENTRY and
CHANGED BYTES before the injected throw; restoration is a separate final-tree
observation, not an inferred mapper flag.

### 2. Empty configured profile authority — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:438-558`.

Exact probe: call `admitted-profiles` with `{}`, then preflight the built-in
name `fast` against that same empty configured map.

```text
PROBE 2 configured-empty-builtins =>
{:admitted-with-no-config [],
 :selected "fast",
 :preflight-error-type "helper-extraction-verification-preflight-unavailable",
 :staged false}
```

### 3. Profile shape and executable admission — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:356-475`.

Exact probe: admit and execute the former string-command profile; separately
admit and preflight a nonexistent absolute executable; compare the disk tree.

```text
PROBE 3 profile-shape-and-runnability =>
{:admitted-bad {},
 :admitted-missing {},
 :missing-preflight-error-type "helper-extraction-verification-preflight-unavailable",
 :bad-execute
 {:ok false,
  :error_type "helper-extraction-verification-preflight-unavailable",
  :staged false,
  :next_call nil},
 :destination-exists false,
 :source-tree-unchanged true}
```

### 4. Destination derivation — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:578-730`.

Exact probe: declare source root `lib`, place `(ns demo.core)` at
`lib/odd.clj`, and request `demo.extracted`.

```text
PROBE 4 destination-source-path-mismatch =>
{:ok false,
 :error_type "helper-extraction-destination-not-derivable",
 :limitation "destination-not-an-exact-source-root-decomposition",
 :from_file "lib/odd.clj",
 :next_call nil}
```

### 5. Restored receipt growth — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:839-903`.

Exact probe: map otherwise identical `verification-failed` facts with one and
1,000 restored files and measure the printed public receipt.

```text
PROBE 5 failure-receipt-growth =>
{:n1-bytes 902,
 :n1000-bytes 908,
 :n1000-restored-files nil,
 :n1000-read-back
 {:files 1000,
  :aggregate_sha256 "6cb64db69b3c4b4ff78ec63eb6a09a8cab5dbbc675ab0e14af7c6fb99ca562da",
  :manifest_in "details_path"},
 :restored-file-count 1000,
 :has-details-path true}
```

### 6. Details directory inside workspace — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:1056-1099`.

Exact probe: execute the happy extraction with `:receipt-dir` set to
`<workspace>/.local-receipts`, then inspect publication, destination, and every
pre-tree path.

```text
PROBE 6 details-dir-inside-workspace =>
{:ok false,
 :error_type "helper-extraction-receipt-dir-inside-workspace",
 :status nil,
 :published false,
 :destination-exists false,
 :tree-unchanged true}
```

### 7. Symlink walk — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:271-327`.

Exact probe: add one source symlink to an outside `.clj` and one source
symlink to an inside `.clj`, then plan the unchanged happy extraction.

```text
PROBE 7 symlink-walk =>
{:ok true,
 :error_type nil,
 :pruned_symlinks ["src/escape.clj" "src/inward.clj"],
 :escape-counted false,
 :inward-counted false}
```

### 8. Traversing configured source root — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:187-242`.

Exact probe: add `../reviewer-sibling` to `.clj-surgeon.edn :source-roots` and
plan the otherwise valid happy tree.

```text
PROBE 8 configured-root-traversal =>
{:ok false,
 :error_type "helper-extraction-invalid-source-root",
 :rejected_source_roots ["../reviewer-sibling"],
 :admitted_source_roots ["src" "test"],
 :next_call nil}
```

### 9. Timeout subprocess evidence — CLOSED

File under review: `src/clj_surgeon/mcp_helper_extraction.clj:934-995`.

Exact probe: run `[/bin/sleep 1]` with a 40 ms timeout.

```text
PROBE 9 subprocess-timeout =>
{:ok false,
 :fresh_process true,
 :timed_out true,
 :process_evidence
 [{:elapsed_ms 76.594938,
   :exit nil,
   :command ["/bin/sleep" "1"],
   :started? true,
   :finished? false}]}
```

The two round-1 subprocess controls also stayed closed:

```text
CONTROL subprocess-literal-argv =>
{:ok true,
 :fresh_process true,
 :timed_out false,
 :process_evidence
 [{:exit 0,
   :command ["/usr/bin/printf" "%s" "literal;$HOME;$(touch nope)"],
   :output "literal;$HOME;$(touch nope)",
   :started? true,
   :finished? true}]}

CONTROL subprocess-cwd =>
{:ok true,
 :fresh_process true,
 :timed_out false,
 :process_evidence
 [{:exit 0,
   :command ["/bin/pwd"],
   :output "/var/tmp/forge/helper-fence-fx/subprocess-2031167865125877\n",
   :started? true,
   :finished? true}]}
```

No shell interpreted the metacharacters, and the child cwd was the reviewer
fixture workspace.

### 10. Public uninitialized refusal — CLOSED

File under review: `src/clj_surgeon/mcp_tool.clj:1969-1984` and
`src/clj_surgeon/mcp_tool.clj:2041-2086`.

Exact probe: obtain `:tool-fn` from the registered `helper/tool`, disarm the
runtime with `mcp-tool/init! nil`, invoke the public function, and capture its
structured callback result.

```text
PROBE 10 public-uninitialized-refusal =>
{:ok false,
 :operation "helper_extraction",
 :error_type "server-not-initialized",
 :next_call nil,
 :source_unchanged true,
 :elapsed_ms 6.431366,
 :next-call-present true,
 :elapsed-valid true}
```

### 11. Registered output authority and elapsed time — PARTIAL

Files under review: `src/clj_surgeon/mcp_schema.clj:710-806` and
`src/clj_surgeon/mcp_operation.clj:25-39`.

Exact probe: call the registered public tool function with an initialized
runtime but an unavailable profile, capture that pre-staging refusal at the
callback, and print the complete output property and required key sets.

```text
PROBE 11 public-prestaging-refusal-and-schema =>
{:wire
 {:ok false,
  :error_type "helper-extraction-verification-preflight-unavailable",
  :staged false,
  :next_call nil,
  :elapsed_ms 1.215934,
  :elapsed-valid true},
 :schema-property-keys
 ["alias_histogram" "caller_files" "cause_error" "changed_files" "closure"
  "committed" "destination_created" "details_path" "elapsed_ms" "error"
  "error_type" "files" "helpers" "kernel_status" "limitation" "next_call"
  "ok" "operation" "partition" "receipt_hash" "recovery_required" "remedy"
  "restoration_read_back" "restored" "restored_file_count" "retained_sites"
  "sites" "source_file" "source_retired" "source_unchanged" "status"
  "undo_receipt" "verification"],
 :schema-required ["ok" "elapsed_ms"]}
```

The round-1 assertion **“`elapsed_ms` must not be required because a
pre-staging refusal does not supply it” is withdrawn**. Its replacement
authority is `clj-surgeon.mcp-operation/finalize-result`, which associates
`:elapsed_ms` onto every domain result at line 39 before public callback
publication. Both independently observed public failures carried finite,
non-negative values. Keeping `elapsed_ms` required also preserves the
nine-tool merge-lane invariant.

The remaining partial defect is different: the prose enumerates five faces,
but the machine schema is one flat object whose only required properties are
`ok` and `elapsed_ms`. It therefore does not require `files` and
`recovery_required` when `status` is `rollback-failed`, `undo_receipt` and
`receipt_hash` when committed, or restoration authority on the two restored
failure states. Round 1 required explicit terminal/refusal variants; adding
property names alone does not satisfy that requirement.

Required fix: encode committed, restored failure/timeout, rollback-failed, and
pre-staging refusal as conditional/union schema variants with each face's
authority fields required, and validate representative results for all five
faces against the registered schema.

## Request-workspace profile-routing obligation — PASSED

Files under review: `src/clj_surgeon/mcp_tool.clj:736-756` and
`src/clj_surgeon/mcp_tool.clj:2041-2086`.

The reviewer materialized two literal, independent projects. Both contained
`deps.edn`, one source file, and their own `.clj-surgeon.edn`. The server
workspace declared only `server-only`; the explicitly requested workspace
declared only `request-only`. The public registered tool function was called
twice with `workspace_root` equal to the request workspace, never the server
default.

```text
OBLIGATION request-workspace-profile-routing =>
{:roots-differ true,
 :server-declared ("server-only"),
 :request-declared ("request-only"),
 :server-profile-at-request
 {:ok false,
  :error_type "helper-extraction-verification-preflight-unavailable",
  :staged false,
  :next_call nil,
  :workspace_root "/var/tmp/forge/helper-fence-fx/request-workspace-2031167957251703"},
 :request-profile-at-request
 {:ok true,
  :status "committed",
  :committed true,
  :workspace_root "/var/tmp/forge/helper-fence-fx/request-workspace-2031167957251703",
  :verification
  {:status "checks-completed",
   :profile "request-only",
   :ok true,
   :fresh_process true}}}
```

Admission follows the request workspace, not the server default.

## New finding: the branch's post-commit regression witness is false-green

- File: `test/clj_surgeon/mcp_helper_extraction_test.clj:650-652` and witness
  use at `test/clj_surgeon/mcp_helper_extraction_test.clj:661-710`
- Severity: **MEDIUM / required test fix**
- Exact probe: use the test's own `real-commit!` implementation as the injected
  `:commit!`, record wrapper entry, and run the happy extraction. The test
  helper calls `(requiring-resolve 'clj-surgeon.extract/commit!)`; the actual
  production Var is `clj-surgeon.mcp-extraction/commit!`.
- Output:

```text
PROBE 1 post-commit-throw =>
{:observations [{:event :commit-entry}],
 :terminal
 {:ok false,
  :status "verification-failed",
  :committed false,
  :restored true,
  :source_unchanged true,
  :destination_created false,
  :cause_error
  "Cannot invoke \"clojure.lang.IFn.invoke(Object)\" because \"this.real_commit\" is null"},
 :final-tree-restored true,
 :final-destination-exists false}
```

The source never changed and the destination never existed in that witness,
yet the supplied suite passed it. The test's `:commit!` wrapper sets its
`injected` atom before invoking `nil`; `execute!` correctly catches that throw
and returns a restored-looking terminal receipt for a tree that was never
written. This is exactly the mapper/“original equals original” false witness
the lead's R2 requirement excludes.

Required fix: resolve `clj-surgeon.mcp-extraction/commit!` and make the test
record/assert, inside the wrapper after the real kernel returns, successful
kernel return, destination existence, and a source hash different from its
pre-image. Retain the final full-tree equality and destination-absence
assertions as the separate restoration proof.

No other new finding was produced within the five bounded edges.
