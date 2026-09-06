Verdict: HOLD.

Blocking finding: [agent_routing.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:61) computes `line-start` but starts the regex region at `idx`. Consequently, `.matches` enforces only “marker through end-of-line,” not the claimed exact whole-line match. A marker such as `X<!-- BEGIN … -->` is accepted as stale and rewritten. The end-to-end installer probe changed the file and returned success:

```text
{:ok true, :operation :install-agent-routing, :block-hash "65f71528b3329f845ac52fed9c5db311529e0b25ec10e4235ef1bd871ddb79f4", :target-count 1, :changed-count 1, :targets [{:path "/var/tmp/forge/routing-install-review-f05938e5/leading.md", :previous-state :stale, :changed true, :stale-version 1}]}
exit=0 before=de2b1391e55316510df577bc8a2bf8fd5d9f48f63f7b4a45308535da09171c2e after=bcc6a9186c3f0403bbb3a81d7de4957fae9bce0b34c49a6c65126e4c4da6d7a3 unchanged=no
KEEP
X<!-- BEGIN CLJ-SURGEON ROUTING v:2 -->
```

The existing witnesses at [agent_routing_test.clj](/home/forge/src/clj-surgeon-fence/test/clj_surgeon/agent_routing_test.clj:480) cover trailing bytes but not leading bytes. Require `idx == line-start` for a well-formed marker and add pure plus end-to-end refusal witnesses for leading text/whitespace.

All other requested gates passed:

- Exact tip `f05938e5`; `3dda2a61` is an ancestor; clean worktree.
- Astra’s three multi-block/mismatched probes refuse unchanged.
- Lone stale pair is replaced in place with no v1 bytes surviving.
- Focused suite is green. The pinned file has 24 tests, not the stated 14.
- Five routing tables are byte-identical; one-byte scratch drift exits 2.
- `install-agent-routing` depends on parity.
- Installed v1 blocks report `:agent-routing-stale-version`.
- Skill mirrors and fast lane are green.
- Normal CLI confinement refused arbitrary, traversal, and symlink-escape targets.
- Known trailing-byte malformed marker was left byte-identical.

Key command/output receipts:

```console
$ git rev-parse --short=8 HEAD && git merge-base --is-ancestor 3dda2a61 HEAD; printf 'base-is-ancestor-exit=%s\n' "$?" && git status --porcelain
f05938e5
base-is-ancestor-exit=0
```

```console
$ /home/forge/bin/suite-run bb --classpath src:test -e "(require 'clj-surgeon.agent-routing-test) (let [r (clojure.test/run-tests 'clj-surgeon.agent-routing-test)] (System/exit (+ (:fail r) (:error r))))"

Testing clj-surgeon.agent-routing-test

Ran 24 tests containing 222 assertions.
0 failures, 0 errors.
```

```console
$ bb --classpath src -e '…four requested marker probes…'
{:case :v2-plus-v1, :ok false, :error-type :invalid-managed-routing, :previous-state nil, :stale-version nil, :unchanged true, :old-version-survives true, :old-body-survives false}
{:case :two-v1, :ok false, :error-type :invalid-managed-routing, :previous-state nil, :stale-version nil, :unchanged true, :old-version-survives true, :old-body-survives false}
{:case :v1-v3, :ok false, :error-type :invalid-managed-routing, :previous-state nil, :stale-version nil, :unchanged true, :old-version-survives true, :old-body-survives false}
{:case :lone-stale, :ok true, :error-type nil, :previous-state :stale, :stale-version 1, :unchanged false, :old-version-survives false, :old-body-survives false}
```

```console
$ bb bin/check-routing-parity.clj
{:ok true, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :table-bytes 1686, :table-rows 12, :renderings-checked 5, :plate "resources/clj-surgeon-agent-routing.md", :pointer-heading "## Edit routing (policy revision 1, 2026-09-06)"}
```

One-byte scratch mutation:

```text
{:ok false, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :failures [{:check :table-parity, :path "AGENTS.md", :problem "routing table bytes differ from the canonical section", :canonical-bytes 1686, :rendered-bytes 1687, :first-difference {:byte-index 21, :canonical-byte "0x0a", :rendered-byte "0x20", :canonical-context "\" Situation | Route |\\n|---|---|\\n| Owner a\"", :rendered-context "\" Situation | Route | \\n|---|---|\\n| Owner \""}}]}
exit=2
```

```console
$ make -n install-agent-routing
bb "/home/forge/src/clj-surgeon-fence/bin/check-routing-parity.clj"
bb --classpath "/home/forge/src/clj-surgeon-fence/src" -m clj-surgeon.agent-routing install "/home/forge/src/clj-surgeon-fence/resources/clj-surgeon-agent-routing.md" "/home/forge/.codex/AGENTS.md" "/home/forge/.claude/CLAUDE.md"
```

```console
$ make check-agent-routing
…
{:ok false, :operation :check-agent-routing, :error-type :agent-routing-stale-version, :expected-version 2, :targets [{:path "/home/forge/.codex/AGENTS.md", :previous-state :stale, :changed true, :stale-version 1} {:path "/home/forge/.claude/CLAUDE.md", :previous-state :stale, :changed true, :stale-version 1}]}
make: *** [Makefile:195: check-agent-routing] Error 2
exit=2
```

```console
$ make check-clj-surgeon-skill-mirrors
bash bench/sync_clj_surgeon_skill.sh --check
clj-surgeon skill mirrors synchronized (--check)
```

```console
$ /home/forge/bin/suite-run make test-fast
clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-fast
…
Ran 487 tests containing 4807 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 46 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

Outside-target probe:

```text
{:ok false, :error-type :agent-routing-target-refused, :scratch false, :refused [{:path "/var/tmp/forge/routing-outside-canary-f05938e5.md", :canonical-path "/var/tmp/forge/routing-outside-canary-f05938e5.md"}], :allowed ["/home/forge/.claude/CLAUDE.md" "/home/forge/.codex/AGENTS.md"], …}
exit=2 canary-exists=no
```

Symlink-escape probe:

```text
{:ok false, :error-type :agent-routing-target-refused, :scratch true, :refused [{:path "/var/tmp/forge/routing-install-review-f05938e5/escape/routing-symlink-canary-f05938e5.md", :canonical-path "/var/tmp/routing-symlink-canary-f05938e5.md"}], …}
exit=2 canary-exists=no
```

Trailing-byte malformed marker:

```text
{:path "/var/tmp/forge/routing-install-review-f05938e5/trailing.md", :error-type :invalid-managed-routing, … :ok false, …}
exit=2 before=69b79cec5343a62033a8caf54d67fd49ab3c67eb78eb0d58049de305e102c411 after=69b79cec5343a62033a8caf54d67fd49ab3c67eb78eb0d58049de305e102c411 unchanged=yes
```

Temporary fixtures were removed; the repository remains clean. No prohibited ports or `make mcp-test` were used.

LAND: DO NOT LAND `f05938e5` into `MCP/main`; fix and rerun the leading-byte installer witness first.