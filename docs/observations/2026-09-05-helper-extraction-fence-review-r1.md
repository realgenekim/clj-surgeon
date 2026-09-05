# NO-GO

Independent FENCE REVIEW of frozen `helper_extraction` candidate
`ee03b49a945dcf86fd3ee54a3ee7d93aef633ba9`, completed at
`2026-09-05T06:39:58Z`. Reviewer: Codex, the independent reviewer named by the
lead; no builder role. The checkout was a clean detached HEAD at the named tip.
I reviewed only the requested helper-extraction boundary/planner closure,
registration/schema, and witness surfaces.

The candidate is **NO-GO**. A real successful kernel commit can be followed by
a throw that escapes the boundary and leaves the extraction standing. Empty
configured profile authority also admits the server's built-in `fast` profile.
Additional high-severity failures admit malformed/nonexistent commands, guess a
destination when `from.file` disagrees with its declared namespace, publish
linear restored-file data in terminal receipts, and permit the details directory
inside the workspace.

## Apparatus and baseline

All fixtures and probe programs were under
`/var/tmp/forge/helper-fence-fx`. No MCP server was started and no port was
contacted. Exactly two JVMs were used, both pinned as required:

```text
env TMPDIR=/var/tmp/forge/helper-fence-fx \
  JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/tmp/forge/helper-fence-fx \
  CLJ_SURGEON_HELPER_TMP=/var/tmp/forge/helper-fence-fx \
  taskset -c 6-9 nice -n 10 \
  clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-deps \
  /var/tmp/forge/helper-fence-fx/fence_probe.clj
```

The second command was identical except for `fence_probe_2.clj`. Its requested
product probes completed; a final reviewer-written attempt to call the public
callback used the wrong callback arity and made that JVM exit 1. No finding
relies on that trailing harness error.

The supplied witnesses are green:

```text
Testing clj-surgeon.helper-extraction-test
Testing clj-surgeon.mcp-helper-extraction-test
Ran 50 tests containing 619 assertions.
0 failures, 0 errors.
PROBE witness-summary => {:test 50, :pass 619, :fail 0, :error 0}
```

## Findings

### 1. A throw after the kernel has committed escapes without inverse rollback

- File: `src/clj_surgeon/mcp_helper_extraction.clj:891` and
  `src/clj_surgeon/mcp_helper_extraction.clj:953`
- Severity: **CRITICAL / merge blocker**
- Exact probe: materialize the supplied happy tree, redefine the extraction
  kernel's two-arity `commit!` seam to call the real kernel and then throw, call
  `helper/execute!`, and read the source and destination from disk.
- Output:

```text
PROBE2 throw-after-kernel-commit => {:outcome {:threw true, :message "injected throw after kernel commit", :data {:kernel-ok true}}, :destination-exists true, :source-equals-pre false}
```

`result (extraction/commit! compiled)` is evaluated before the `try` beginning
at line 953. The claimed encompassing guard therefore does not encompass the
write boundary. The actual destination remained and the source did not equal
its frozen pre-image.

Required fix: make the kernel handoff itself part of a lifecycle that cannot
lose the inverse receipt. Every Throwable after the first possible write must
either be converted by the kernel into a result carrying its recovery/inverse
authority or be caught by a boundary that already owns that authority. Then
route it through the same finalizer as proof/publication/mapping failures and
add this exact post-real-commit throw as a witness.

### 2. Empty configured authority still admits a built-in profile

- File: `src/clj_surgeon/mcp_helper_extraction.clj:324`,
  `src/clj_surgeon/mcp_helper_extraction.clj:391`, and
  `src/clj_surgeon/mcp_helper_extraction.clj:855`
- Severity: **HIGH / merge blocker**
- Exact probe: call `admitted-profiles` with its zero-arity server-default path,
  then call `verification-preflight` with `{}` and the returned name.
- Output:

```text
PROBE configured-empty-builtins => {:admitted-with-no-config ["fast"], :selected "fast", :preflight-with-empty-config nil}
```

The preflight and execution lookup merge `(admitted-profiles)` into the
configured map. This directly violates “only profiles from the CONFIGURED map,
never a built-in.”

Required fix: remove the zero-arity built-in source from both plan and execute
admission. Compute capability only from the routed workspace's configured
`:verification-profiles`; nil or `{}` must admit nothing. Add a negative test
that `{}` refuses `fast` even when the server registry contains it.

### 3. Non-argv and nonexistent-command profiles are admitted and can stage

- File: `src/clj_surgeon/mcp_helper_extraction.clj:281-310` and
  `src/clj_surgeon/mcp_helper_extraction.clj:362-372`
- Severity: **HIGH / merge blocker**
- Exact probes: submit an exact-shaped profile whose `:commands` contains a
  string rather than an argv vector; then preflight a nonexistent absolute
  executable.
- Output:

```text
PROBE profile-shape-admission => {:hot {}, :cold {}, :invalid-command-shape {"bad" {:synchronous? true, :rollback-capable? true, :fresh-process? true, :commands ["/bin/true"], :timeout-ms 10, :shape :exact}}}
PROBE2 missing-absolute-executable-preflight => nil
```

The malformed profile proceeded through a real extraction before ending as a
verification timeout and rolling back; admission therefore did not refuse it
before staging. `runnable-command?` treats any resolved spelling containing `/`
as runnable without checking that the executable exists.

Required fix: admit only a non-empty vector of non-empty argv vectors whose
members are strings; validate timeout as a positive bounded integer; reject
every profile carrying `:hot` or `:cold`; and require an absolute/slashed
executable to be a real executable file. Preflight shape/runnability errors must
be typed refusals before staging and must not throw.

### 4. Destination derivation guesses the project root on source-path/ns mismatch

- File: `src/clj_surgeon/helper_extraction.clj:972-980` and
  `src/clj_surgeon/mcp_helper_extraction.clj:438-465`
- Severity: **HIGH / merge blocker**
- Exact probe: configure `lib` as a source root, put `(ns demo.core)` in
  `lib/odd.clj`, request destination `demo.extracted`, and plan.
- Output:

```text
PROBE destination-source-path-mismatch => {:ok true, :error_type nil, :destination {:lib "demo.extracted", :file "demo/extracted.clj", :source "(ns demo.extracted)\n\n(defn helper [x] (inc x))\n", :omitted_requires []}}
```

When `from.file` does not end in the declared namespace path, `destination-file`
sets `root` to `""` and silently invents `demo/extracted.clj` at project root.
It is project-relative but is not derived from the source's admitted root.

Required fix: require an exact source-root/ns-path decomposition of `from.file`.
On mismatch return `helper-extraction-destination-not-derivable` with
`next_call nil`; never fall back to the empty prefix. Prove the destination is
under the same confined admitted source root.

### 5. Restored failure receipts are O(N), not O(1)

- File: `src/clj_surgeon/mcp_helper_extraction.clj:663-666` and
  `src/clj_surgeon/mcp_helper_extraction.clj:933-936`
- Severity: **HIGH / merge blocker**
- Exact probe: map otherwise identical `verification-failed` kernel facts with
  one and 1,000 restored files and measure the printed receipt.
- Output:

```text
PROBE failure-receipt-growth => {:n1-bytes 282, :n1000-bytes 37029, :n1000-restored-files 1000, :n1000-read-back 1000}
```

Both `:restored_files` and the per-file
`:restoration_read_back` hash map are copied into the terminal receipt.

Required fix: for verified rollback states publish only constant-size counts,
aggregate/hash evidence, and a `details_path` under local state; put the file
manifest and per-file hashes in that details file. Preserve the explicitly
required unrestored-file/recovery authority for `rollback-failed`, but do not
copy successful restoration detail into ordinary failure receipts.

### 6. The configured receipt directory may be inside the mutated workspace

- File: `src/clj_surgeon/mcp_helper_extraction.clj:884-888` and
  `src/clj_surgeon/mcp_tool.clj:2034-2042`
- Severity: **HIGH / merge blocker**
- Exact probe: execute the happy extraction successfully with `:receipt-dir`
  set to `<workspace>/.local-receipts`.
- Output:

```text
PROBE details-dir-inside-workspace => {:status "committed", :details_path "/var/tmp/forge/helper-fence-fx/details-inside-2028434301452654/.local-receipts/helper-extraction-05c67278-201f-4894-9dc7-cfe90630b529.edn", :inside-workspace? true}
```

Required fix: canonicalize and confine receipt publication to the kernel's
local-state receipt directory and explicitly reject any configured path inside
the real workspace (including symlink aliases). Add a negative witness; the
current test only checks that the result begins with the path supplied by its
own test config.

### 7. A source symlink is enumerated and aborts the entire operation instead of being dropped

- File: `src/clj_surgeon/mcp_helper_extraction.clj:207-260`
- Severity: **MEDIUM**
- Exact probe: add `src/escape.clj` as a symlink to a Clojure file outside the
  workspace and plan the otherwise valid happy tree.
- Output:

```text
PROBE2 symlink-walk => {:ok false, :error_type "helper-extraction-unreadable-source", :file "src/escape.clj", :cause "Source symlink resolves outside the configured project root", :decision "which paths this workspace's roots may contain"}
```

This is fail-closed for confidentiality, but it violates the repository fence
rule that symlinks produced by a walk are dropped. One unrelated symlink under
an admitted root can deny every extraction.

Required fix: prune symlink entries during enumeration before completeness and
read sets are formed; test both outward and inward symlinks and ensure neither
is read nor counted.

### 8. Traversing configured source roots are called admitted and silently ignored

- File: `src/clj_surgeon/mcp_helper_extraction.clj:186-223`
- Severity: **MEDIUM**
- Exact probe: put a sibling traversal in `.clj-surgeon.edn` as
  `{:source-roots ["../<sibling>"]}` and plan a valid tree.
- Output:

```text
PROBE configured-root-traversal => {:ok true}
```

The outside file was not enumerated, which is safe, but the boundary reports
the traversal as an admitted root in its internal result while the public
closure receipt remains the planner's fixed `["src" "test"]`. That makes
admission and closure evidence disagree.

Required fix: validate configured roots before `root-globs`: only normalized
project-relative directories whose real paths remain inside the workspace may
be admitted. Refuse or drop traversal/absolute/symlink relocations consistently,
and build the closure receipt from the actual admitted set.

### 9. Timeout evidence falsely says no fresh process ran

- File: `src/clj_surgeon/mcp_helper_extraction.clj:708-749`
- Severity: **MEDIUM**
- Exact probe: run configured argv `["/bin/sleep" "1"]` with a 40 ms timeout.
- Output:

```text
PROBE subprocess-timeout => {:ok false, :fresh_process false, :timed_out true, :cwd "/var/tmp/forge/helper-fence-fx", :process_evidence [{:exit nil, :elapsed_ms 58.134803, :finished? false, :output "", :output-bytes 0, :output-sha256 "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", :output-truncated false, :command ["/bin/sleep" "1"]}]}
```

The child demonstrably started and timed out, but `fresh_process` is computed
from `(some :exit outcomes)`; a timed-out process has `:exit nil` and is
therefore reported as not fresh.

Required fix: carry an explicit process-started/fresh-child fact from the
process runner and use it independently of exit completion. Add a timeout
witness requiring both `timed_out true` and `fresh_process true`.

### 10. The public uninitialized refusal omits the mandatory `next_call nil`

- File: `src/clj_surgeon/mcp_tool.clj:2021-2027`
- Severity: **MEDIUM**
- Exact structural probe: select the map containing the
  `"server-not-initialized"` value from `handle-helper-extraction`.

```text
{:ok false
            :operation "helper_extraction"
            :error_type "server-not-initialized"
            :error "helper_extraction server is not initialized"
            :source_unchanged true
            :remedy "Restart the configured clj-surgeon MCP server."}
```

Required fix: normalize every refusal emitted by the public helper-extraction
handler, including initialization and workspace-routing refusals, through the
closed refusal envelope with an explicit `:next_call nil`. Add public-handler
witnesses, not only direct planner/boundary tests.

### 11. The registered output schema does not describe required rollback authority

- File: `src/clj_surgeon/mcp_schema.clj:710-736`
- Severity: **MEDIUM**
- Exact probe: compute the registered output property's complete sorted key set
  with `clj-surgeon :op :xray`.
- Output:

```text
["alias_histogram" "caller_files" "closure" "committed"
 "destination_created" "details_path" "elapsed_ms" "helpers"
 "kernel_status" "ok" "operation" "partition" "receipt_hash"
 "restored" "retained_sites" "sites" "source_retired"
 "source_unchanged" "status" "undo_receipt" "verification"]
```

The terminal receipt correctly carries `:files`, `:recovery_required`, and
`:cause_error`, but none is declared by the registered output schema. The same
schema omits `restored_files` and `restoration_read_back`, while requiring
`elapsed_ms` even though pre-staging refusals do not supply it.

Required fix: define explicit terminal/refusal variants that accurately expose
the recovery fields and their requiredness, then add schema-validation witnesses
for all four terminal states plus pre-staging refusals.

## Passing bounded edges

The following requested edges passed the independent probes and are not reasons
for NO-GO:

- Direct subprocess execution preserved metacharacters literally and did not
  invoke a shell or add a command:

  ```text
  PROBE subprocess-literal-argv => {:ok true, :fresh_process true, :timed_out false, :cwd "/var/tmp/forge/helper-fence-fx", :process_evidence [{:exit 0, :elapsed_ms 4.730766, :finished? true, :output "literal;$HOME;$(touch nope)", :output-bytes 27, :output-sha256 "014d72bd333b6eba146cab3de63fd8d7187c01f038d1781f7700ac3bb99c838d", :output-truncated false, :command ["/usr/bin/printf" "%s" "literal;$HOME;$(touch nope)"]}]}
  ```

- The child cwd was the candidate workspace supplied to `run-proof!`:

  ```text
  PROBE subprocess-cwd => {:ok true, :fresh_process true, :timed_out false, :cwd "/var/tmp/forge/helper-fence-fx", :process_evidence [{:exit 0, :elapsed_ms 4.476944, :finished? true, :output "/var/tmp/forge/helper-fence-fx\n", :output-bytes 31, :output-sha256 "b5a6a6f0dcfcfd27bc18bb6fec3b7140b7655db68d908422178ecc37b6f504c5", :output-truncated false, :command ["/bin/pwd"]}]}
  ```

- Configured `:hot` and `:cold` profiles were rejected.
- The timeout was honored in 58.1 ms for a 40 ms bound; the false
  `fresh_process` evidence is finding 9.
- Unknown nested request data refused with `next_call nil`:
  `{:ok false, :error_type "helper-extraction-invalid-request", :next_call nil,
  :path ["from"]}`.
- A proof throw and ordinary receipt-publication failure restored the supplied
  happy tree in the existing witnesses.
- An injected undo throw returned terminal `rollback-failed`, named unrestored
  files, carried recovery authority, and did not claim the source unchanged.
- The normal plan-to-compile path carries the frozen `:sources` snapshot into
  the kernel; no second planning read of source bytes was found.

The critical post-commit throw, profile-authority leak, malformed admission,
destination guess, linear receipts, and in-workspace detail publication must be
fixed and re-probed before this candidate can enter the merge queue.
