# Verdict: HOLD

LAND: **DO NOT LAND `6080cd72` into `MCP/main`.** The normal installer can write outside the two authorized home files when `.codex` or `.claude` is a symlinked directory.

## Blocking finding

[agent_routing.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:292) canonicalizes both the allowed path and the requested target. If `$HOME/.codex` points outside `$HOME`, both resolve to the same outside path, which the installer then authorizes.

Isolated reproduction:

```console
$ mv .../redteam-home/.codex .../redteam-home/.codex.real
$ ln -s .../outside .../redteam-home/.codex
$ clojure -J-Duser.home=.../redteam-home -M \
    -m clj-surgeon.agent-routing install \
    resources/clj-surgeon-agent-routing.md \
    .../redteam-home/.codex/AGENTS.md
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
{:ok true, :operation :install-agent-routing, :block-hash "65f71528b3329f845ac52fed9c5db311529e0b25ec10e4235ef1bd871ddb79f4", :target-count 1, :changed-count 1, :targets [{:path ".../redteam-home/.codex/AGENTS.md", :previous-state :absent, :changed true}]}
exit=0
resolved-target=.../outside/AGENTS.md
before=86c891e96b0ceef0d5e66548016a3e86c5ad7bac05abbf404a48636f9902a14f
after=0734cc4c53389f2ebde4eeff63b7b8301b0bf27c9bd11f5b14039d5f218e3b84
```

Require non-symlink trusted parent directories and revalidate confinement immediately before each atomic publication. Add a CLI-level red-first witness for this exact parent-directory symlink case.

A secondary concern: malformed-file refusals print the entire target as `:source` through `-main`. Global instruction contents should be removed from the CLI failure result before printing.

## Requested gates

1. Marker behavior: green.

```console
$ bb --classpath src -e '<four requested marker probes>'
[{:probe :v2-plus-v1, :ok false, :error-type :invalid-managed-routing, :begin-versions [2 1], :end-versions [2 1], :untouched true}
 {:probe :two-v1, :ok false, :error-type :invalid-managed-routing, :begin-versions [1 1], :end-versions [1 1], :untouched true}
 {:probe :mismatched, :ok false, :error-type :invalid-managed-routing, :begin-versions [1], :end-versions [3], :untouched true}]
{:probe :lone-stale, :ok true, :previous-state :stale, :stale-version 1, :old-version-survives false, :old-body-survives false, :head-and-tail-preserved true}
```

The leading-byte matrix covers space, tab, text, Markdown quote, BOM, malformed END, and stale pairs. The pinned test file contains **27 tests**, not the stated 14:

```console
$ ~/bin/suite-run bb --classpath src:test -e \
  "(require '[clojure.test :as t] '[clj-surgeon.agent-routing-test]) ..."
Testing clj-surgeon.agent-routing-test

Ran 27 tests containing 266 assertions.
0 failures, 0 errors.
```

2. Routing parity: green; one-byte negative control turns red.

```console
$ bb bin/check-routing-parity.clj
{:ok true, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :table-bytes 1686, :table-rows 12, :renderings-checked 5, :plate "resources/clj-surgeon-agent-routing.md", :pointer-heading "## Edit routing (policy revision 1, 2026-09-06)"}
exit=0
```

After changing `No reread` to `No Reread` in scratch:

```console
$ bb bin/check-routing-parity.clj
{:ok false, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :failures [{:check :table-parity, :path "AGENTS.md", :problem "routing table bytes differ from the canonical section", :canonical-bytes 1686, :rendered-bytes 1686, :first-difference {:byte-index 318, :canonical-byte "0x72", :rendered-byte "0x52", :canonical-context "\"eld in context | No reread. |\\n| Known sm\"", :rendered-context "\"eld in context | No Reread. |\\n| Known sm\""}}]}
exit=2
```

3. Make dependency: green; no install executed.

```console
$ make -n install-agent-routing
bb "/home/forge/src/clj-surgeon-fence/bin/check-routing-parity.clj"
bb --classpath "/home/forge/src/clj-surgeon-fence/src" -m clj-surgeon.agent-routing install "/home/forge/src/clj-surgeon-fence/resources/clj-surgeon-agent-routing.md" "/home/forge/.codex/AGENTS.md" "/home/forge/.claude/CLAUDE.md"
exit=0
```

4. Installed v1 blocks correctly report stale:

```console
$ make check-agent-routing
{:ok true, :operation :check-routing-parity, ...}
{:ok false, :operation :check-agent-routing, :error-type :agent-routing-stale-version, :expected-version 2, :targets [{:path "/home/forge/.codex/AGENTS.md", :previous-state :stale, :changed true, :stale-version 1} {:path "/home/forge/.claude/CLAUDE.md", :previous-state :stale, :changed true, :stale-version 1}]}
make: *** [Makefile:195: check-agent-routing] Error 2
exit=2
```

5. Skill mirrors: green.

```console
$ make check-clj-surgeon-skill-mirrors
bash bench/sync_clj_surgeon_skill.sh --check
clj-surgeon skill mirrors synchronized (--check)
exit=0
```

6. Fast lane at `6080cd72`: green.

```console
$ ~/bin/suite-run make test-fast
clojure -J-Xms64m -J-Xmx512m -M:clj-surgeon/test-fast
...
Ran 487 tests containing 4807 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 46 namespace(s) (TEST-ISO-002/003/004/005/007/010)
```

7. Other installer probes:

Arbitrary target refused and unchanged:

```console
{:ok false, :error-type :agent-routing-target-refused, :scratch false, ...}
exit=2
before=5df449b884df804bb0e9124548aa9f08ae1faf6c6e02e2a1f639a103c69a376d
after=5df449b884df804bb0e9124548aa9f08ae1faf6c6e02e2a1f639a103c69a376d
```

Scratch symlink escape refused:

```console
{:ok false, :error-type :agent-routing-target-refused, :scratch true, :canonical-path "/home/forge/routing-redteam-canary.md", ...}
exit=2
target_exists=no
```

Malformed target blocks caused no write to either preflighted target:

```console
{:error-type :invalid-managed-routing, :operation :install-agent-routing, :ok false, ...}
exit=2
healthy_before=6f0a3c779fad2f921afd1cb889168c934f6d730b58206787fdb74d6e9a6be04a
healthy_after=6f0a3c779fad2f921afd1cb889168c934f6d730b58206787fdb74d6e9a6be04a
malformed_before=d7e471719754f41afb2e3d76838bb506d202b9b6d1260767fed5674ca23b7eed
malformed_after=d7e471719754f41afb2e3d76838bb506d202b9b6d1260767fed5674ca23b7eed
```

Malformed canonical source also caused no write:

```console
{:ok false, :error-type :invalid-canonical-routing, ...}
exit=2
before=088eaad0abc6a20fa74af2e005259c1c7f7edeb50dc3a0ac12cbbb2c207289cd
after=088eaad0abc6a20fa74af2e005259c1c7f7edeb50dc3a0ac12cbbb2c207289cd
```

Lint was clean:

```console
$ ~/bin/clj-kondo --lint src/clj_surgeon/agent_routing.clj test/clj_surgeon/agent_routing_test.clj bin/check-routing-parity.clj
linting took 55ms, errors: 0, warnings: 0
```

The repository remained clean, and scratch fixtures were removed. No prohibited ports were contacted and `make mcp-test` was not run.