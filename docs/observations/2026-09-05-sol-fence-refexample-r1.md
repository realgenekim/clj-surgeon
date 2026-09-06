No findings. Reviewed detached HEAD `c5e8be8f0a1bc662ed68b379eb47a2869e0b0f47`; no edits made.

Request-shape refusal coverage in [mcp_helper_extraction.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_helper_extraction.clj:176):

- Line 190: unknown top-level field — enriched directly.
- Lines 201, 206, 212, 219, 225, 231, 240: malformed `from`, `from.file`, `helpers`, `to`, `scope.paths`, `verification.profile`, and optional `expect` — all route through `invalid-request` → `shape-refusal`.
- Every branch returned `:field`, `:decision`, identical `:example`, and `:next_call nil`.

Other refusal sites—lines 312, 342, 390, 604/619/629, 679/687/695/704, 751, 1251, 1306, and 1534—are configuration, filesystem, planning, verification, destination, or transaction refusals after request-shape validation. They carry decisions but correctly do not invent request examples. Planner refusals normalized at line 843 likewise occur only after validation succeeds.

The schema’s refusal row remains unchanged in required fields, pins `next_call` to nil, and merely allows typed optional `decision`, `field`, and `example` properties ([matrix](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:1073), [properties](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_schema.clj:1246)). All eight branches dynamically validated against exactly `#{"refusal"}`.

The example contains only fixture-relative source names and symbols. It deliberately omits `workspace_root`; no absolute workspace path, environment value, credential, or secret is present.

Executed commands and outputs:

```text
$ git status --short --branch
$ git rev-parse HEAD
## HEAD (no branch)
c5e8be8f0a1bc662ed68b379eb47a2869e0b0f47
```

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main -e "(require 'clojure.test 'clj-surgeon.mcp-helper-extraction-test) (clojure.test/run-tests 'clj-surgeon.mcp-helper-extraction-test)"
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge

Testing clj-surgeon.mcp-helper-extraction-test

Ran 51 tests containing 1188 assertions.
0 failures, 0 errors.
{:test 51, :pass 1188, :fail 0, :error 0, :type :summary}
```

```text
$ java ... -e '<eight-branch schema-face probe>'
[[:unknown #{"refusal"}] [:from #{"refusal"}] [:from-file #{"refusal"}]
 [:helpers #{"refusal"}] [:to #{"refusal"}] [:scope #{"refusal"}]
 [:verification #{"refusal"}] [:expect #{"refusal"}]]
```

```text
$ git diff --check HEAD^ HEAD
$ git status --short --branch
## HEAD (no branch)
```

GO