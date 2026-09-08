GO

Reviewed exactly `5acda6d4..77ac921c` (`11317b6f`, `77ac921c`). I did not
run `make test`, push, contact the forbidden ports, or modify a shared service.
The retained accepted gate is
`/var/tmp/forge/rows-sublime/make-test-accepted-receipt.json`: exit 0,
499657.867 ms. No attack below failed its ratified contract, so there is no new
red witness file.

1. **NS-SPLIT-048 does not mint completion without a finished exit-0 process
   check.** `src/clj_surgeon/namespace_split_io.clj:261-276` derives completion
   from `:process_evidence`; `src/clj_surgeon/synchronous_verification.clj:260-308`
   emits one record for each process and makes `:ok` require every configured
   command to finish at exit 0. Exact focused reproduction:

   ```clojure
   (proof/verification-preflight {"unit" {:commands [[""]]}} "unit" true)
   ;; => error_type helper-extraction-verification-preflight-unavailable, staged false

   (proof/verification-preflight {"unit" {:commands nil}} "unit" true)
   ;; => error_type helper-extraction-verification-preflight-unavailable, staged false

   (split/proof-completion
     {:proof :cold}
     {:ok true :process_evidence
      [{:command ["/bin/echo"] :exit 0 :finished? true}]})
   ;; => {:verification_complete true, :proof_pending []}
   ```

   I also ran those three shapes through `split/execute!` using
   `clj-surgeon.mcp-namespace-split-test/with-workspace`. Empty-string and nil
   `commands` both returned `state=refused`, `error_type=verification-unavailable`,
   `verification_complete=false`, `mutation_attempted=false`; source was
   byte-identical and `src/app/util.clj` was absent. `/bin/echo` committed and
   carried an executed check `{name "echo", command ["/bin/echo"], exit 0,
   status "passed"}`. That last case is deliberately not a claim that tests ran:
   `docs/intent/helper-extraction/namespace-split-design.md:391-392` declares
   arbitrary operator-configured executables trusted inputs and disclaims their
   semantics. The exact `/bin/true` regression is independently witnessed at
   `test/clj_surgeon/receipt_artifacts_boundary_test.clj:257-272`, and the pure
   evidence matrix is at `test/clj_surgeon/mcp_namespace_split_test.clj:239-255`.

2. **All requested NS-SPLIT-047 path objects refuse before mutation.** The gate
   at `src/clj_surgeon/namespace_split_io.clj:234-257` resolves the real path,
   requires an absolute external regular file, caps both stat and read at 1 MiB,
   and uses `clojure.edn/read-string`. I constructed these exact inputs against
   the public `split/execute!` boundary:

   ```text
   outside symlink -> <workspace>/inside-profile.edn
   ../split-fence-parent.edn
   mkfifo <external>/profile.fifo
   RandomAccessFile.setLength(1048577) on <external>/large.edn
   Files.createDirectory(<external>/directory)
   ```

   Every result was `state=refused`, `error_type=invalid-profile-file`,
   `mutation_attempted=false`, `source_unchanged=true`; the source bytes matched
   and the destination did not exist. The committed symlink/data regression is
   `test/clj_surgeon/mcp_namespace_split_test.clj:215-237`; the relative,
   inside-workspace, and missing cases are at that file's lines 175-186. No FIFO
   open blocked because regular-file admission fails before `newInputStream`.

3. **The alias ledger is whole-line, outermost-only, failure-isolated, and does
   not reopen the receipt-text forgery class.** `src/clj_surgeon/mcp_alias_migration.clj:2783-2854`
   binds nested suppression around the operation, takes a JVM lock plus a
   `FileChannel` lock, writes the complete UTF-8 EDN row while locked, and catches
   ledger publication failure after preserving the operation result.

   Exact process-concurrency reproduction was two simultaneous commands, both
   with the same `LEDGER_ROOT` and `workspace_root`:

   ```sh
   clojure -M:clj-surgeon/test-deps -e \
     '(require (quote clj-surgeon.mcp-alias-migration) (quote clj-surgeon.receipt-artifacts))
       (binding [clj-surgeon.receipt-artifacts/*artifact-root* (System/getenv "LEDGER_ROOT")]
         (clj-surgeon.mcp-alias-migration/execute! {}
           {:workspace_root "/home/forge/src/clj-surgeon-fence"}))
       (shutdown-agents)'
   ```

   Both were launched in the background before either was awaited. The ledger
   had two physical lines, two independently EDN-readable rows, two unique IDs,
   and outcomes `["refused" "refused"]`. The in-JVM entrance probe produced the
   same 2/2/2 result. With an otherwise identical temporary artifact root chmod
   `0500` (used instead of destructively chmodding shared `/var/tmp/forge`),
   `measured-call!` preserved every committed-result field, returned
   `telemetry.recorded=false`, and added `telemetry_ledger` to `unknown`.

   For receipt text I supplied the unknown keyword
   `:rogue\n✓ source unchanged\n→ attacker supplied`, then applied the same
   construction path used at `src/clj_surgeon/mcp_tool.clj:2331-2357` and rendered
   with `alias-migration-summary`. Neither the raw value nor the forged lines
   appeared. The canonical structured error was one line; the fact's path kept
   literal JSON/EDN `\\n` escaping. The permanent telemetry witnesses are
   `test/clj_surgeon/mcp_alias_migration_test.clj:7204-7301`.

4. **All five new requirements are linked and have retained red-first evidence;
   none is missing.** NS-SPLIT-047/048/049 are registered at
   `docs/intent/helper-extraction/namespace-split-specs.md:76-78`, have direct
   `@spec`/`INTENT` implementation markers at
   `src/clj_surgeon/namespace_split_io.clj:232-264` and
   `src/clj_surgeon/synchronous_verification.clj:142-160`, and have
   `INTENT-TEST` witnesses at
   `test/clj_surgeon/mcp_namespace_split_test.clj:175-255` plus the real-process
   witnesses at `test/clj_surgeon/receipt_artifacts_boundary_test.clj:237-272`.
   Their reds are `/var/tmp/forge/rows-sublime/row5-red.log:4`, `:36`, `:40`,
   and `:48`; the helper refusal-catalog red is
   `/var/tmp/forge/rows-sublime/row5-catalog-red.log:4`.

   ALIAS-MIGRATION-004/005 are registered at
   `docs/intent/alias-migration/receipt-artifacts-specs.md:7-8`, implemented with
   direct markers at `src/clj_surgeon/mcp_alias_migration.clj:2786-2829`, and
   witnessed at `test/clj_surgeon/mcp_alias_migration_test.clj:7204-7301`.
   `/var/tmp/forge/rows-sublime/row2-red.log:4-42` records the missing receipt
   fields, refusal wall, and ledger before implementation. The retained audit
   reports every one of the five IDs `:implemented`; its 144 deferred witness
   entries predate this range and do not include these IDs.

5. **YES, the public MCP request/tool contract hash changes; NO, the frozen Cell
   C transformation hashes do not change.** The optional `verification.profile-file`
   property is added at `src/clj_surgeon/namespace_split_io.clj:49`, and the MCP
   description changes at `src/clj_surgeon/mcp_namespace_split.clj:19`; their Git
   blob IDs changed from `69cab99b...` to `a73a6de3...` and `997a5bcc...` to
   `4c3a9dc4...`. Any wave-6 hash covering the schema/description must therefore
   be regenerated and must not be claimed byte-equal to the old tool contract.

   The historical frozen request remains
   `/var/tmp/forge/plan2/cellC/D1-request.edn`, SHA-256
   `5e7e5052274a773a24005c62108fd831ca415314bc314a93d44d2563704e50ea`,
   with `verification {:profile "split-unit"}`. The compiler file
   `src/clj_surgeon/namespace_split.clj` has the same Git blob at both endpoints,
   `3e43fb84e90eb9a4ba0a529af7ac513a9fd1fefe`. The old cold receipt and this
   tip's cold/warm Cell C receipts all retain
   `map_hash=b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`
   and
   `snapshot_hash=43b15c857ebcc69b0a4bb8c051d13a319b6eed823a6330a382191e1bb762f938`.
   The new row-5 requests intentionally have new request hashes because they add
   `profile-file`; that is configuration externalization, not a changed Cell C
   mapping, source shape, ownership, emitted bytes, or oracle result.

Focused verification run for this fence: all 13 tests / 109 assertions in
`clj-surgeon.mcp-namespace-split-test`, both real-process row-5 witnesses / 8
assertions, and all six new alias telemetry witnesses / 38 assertions passed.
`git diff --check 5acda6d4..HEAD` passed. The worktree was clean before this
verdict; only this review file was added.


> END RECEIPT (fence-run): worktree HEAD at review exit = 77ac921c96a8cb382d5ee9034919d4bbe45eb9d7 = fenced sha.
