# Verdict: HOLD

**LAND: DO NOT LAND `2ed38709` onto `origin/MCP/main` (`3dda2a61+`).**

Two blocking fail-closed gaps remain in [agent_routing.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:26):

1. Different oversized numeric versions both make `parse-long` return `nil`; the comparison at [line 109](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:109) therefore considers them matching. The installer reports `:stale`, modifies the file, and loses the version instead of returning `:invalid-managed-routing`.

2. `.lookingAt` at [line 55](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:55) accepts trailing bytes after `-->`, despite the stated exact-rest-of-line marker contract. The installer rewrites such a malformed marker instead of leaving it untouched.

Either defect violates criterion 1; the oversized mismatched-version case is independently decisive.

All other requested gates are green or correctly red.

## Marker probes and focused test

```text
$ bb --classpath src:test -e '<replay v2+v1, two-v1, BEGIN-v1/END-v3, and lone-v1 probes>'
{:probe :v2+v1, :ok false, :error-type :invalid-managed-routing, :begin-versions [2 1], :end-versions [2 1], :file-untouched true}
{:probe :two-v1, :ok false, :error-type :invalid-managed-routing, :begin-versions [1 1], :end-versions [1 1], :file-untouched true}
{:probe :begin-v1-end-v3, :ok false, :error-type :invalid-managed-routing, :begin-versions [1], :end-versions [3], :file-untouched true}
{:stale-version 1, :source "head\n<!-- BEGIN CLJ-SURGEON ROUTING v:2 -->\nCURRENT\n<!-- END CLJ-SURGEON ROUTING v:2 -->\ntail\n", :previous-state :stale, :ok true, :old-body-survives false, :probe :lone-stale-v1, :begin-count 1, :old-version-bytes-survive false, :end-count 1}
```

The current test namespace contains 20 tests, not the stated 14:

```text
$ ~/bin/suite-run bb --classpath src:test -e '(require (quote clj-surgeon.agent-routing-test) (quote clojure.test)) (let [r (clojure.test/run-tests (quote clj-surgeon.agent-routing-test))] (System/exit (+ (:fail r) (:error r))))'

Testing clj-surgeon.agent-routing-test

Ran 20 tests containing 184 assertions.
0 failures, 0 errors.
EXIT=0
```

Blocking counterexample:

```text
$ bb --classpath src -e '<BEGIN v:999999999999999999999999999998, END v:999999999999999999999999999999>'
{:probe :different-overflow-versions, :ok true, :error-type nil, :previous-state :stale, :stale-version nil, :file-untouched false}
```

Trailing-byte counterexample:

```text
$ rg -n 'BEGIN CLJ-SURGEON ROUTING' trailing-bytes-marker.md
1:<!-- BEGIN CLJ-SURGEON ROUTING v:2 -->TRAILING-BYTES

$ bb --classpath src -m clj-surgeon.agent-routing install resources/clj-surgeon-agent-routing.md --scratch trailing-bytes-marker.md
{:ok true, :operation :install-agent-routing, :block-hash "65f71528b3329f845ac52fed9c5db311529e0b25ec10e4235ef1bd871ddb79f4", :target-count 1, :changed-count 1, :targets [{:path ".../trailing-bytes-marker.md", :previous-state :replaced, :changed true}]}
TRAILING_BYTES_EXIT=0
```

## Routing parity

```text
$ bb bin/check-routing-parity.clj
{:ok true, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :table-bytes 1686, :table-rows 12, :renderings-checked 5, :plate "resources/clj-surgeon-agent-routing.md", :pointer-heading "## Edit routing (policy revision 1, 2026-09-06)"}
EXIT=0
```

Each rendering has exactly one table:

```text
skill.md 1
skills/safe-refactor/SKILL.md 1
CLAUDE.md 1
AGENTS.md 1
docs/observations/2026-09-06-routing-prompt-surfaces.md 1
```

Pointer and citation:

```text
13:## Edit routing (policy revision 1, 2026-09-06)
EXISTS docs/observations/2026-09-06-routing-prompt-surfaces.md
```

A one-byte CR insertion goes red:

```text
$ perl -0pi -e 's/\| Situation \| Route \|\n/| Situation | Route |\r\n/' skill.md
$ bb bin/check-routing-parity.clj
{:ok false, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :failures [{:check :table-parity, :path "skill.md", :problem "routing table bytes differ from the canonical section", :canonical-bytes 1686, :rendered-bytes 1687, :first-difference {:byte-index 21, :canonical-byte "0x0a", :rendered-byte "0x0d", :canonical-context "\" Situation | Route |\\n|---|---|\\n| Owner a\"", :rendered-context "\" Situation | Route |\\r\\n|---|---|\\n| Owner \""}}]}
CR_BYTE_EXIT=2
```

BOM detection also goes red:

```text
$ perl -0pi -e 's/\A/\xEF\xBB\xBF/' CLAUDE.md
$ bb bin/check-routing-parity.clj
{:ok false, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :failures [{:check :byte-order-mark, :path "CLAUDE.md", :problem "this rendering's byte-order mark does not match the canonical file's", :canonical-has-bom false, :rendered-has-bom true}]}
BOM_EXIT=2
```

## Make dependency

No installation was performed:

```text
$ make -n install-agent-routing
bb "/home/forge/src/clj-surgeon-fence/bin/check-routing-parity.clj"
bb --classpath "/home/forge/src/clj-surgeon-fence/src" -m clj-surgeon.agent-routing install "/home/forge/src/clj-surgeon-fence/resources/clj-surgeon-agent-routing.md" "/home/forge/.codex/AGENTS.md" "/home/forge/.claude/CLAUDE.md"
```

Parity runs first.

## Installed v:1 state

```text
$ make check-agent-routing
bb "/home/forge/src/clj-surgeon-fence/bin/check-routing-parity.clj"
{:ok true, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :table-bytes 1686, :table-rows 12, :renderings-checked 5, :plate "resources/clj-surgeon-agent-routing.md", :pointer-heading "## Edit routing (policy revision 1, 2026-09-06)"}
bb --classpath "/home/forge/src/clj-surgeon-fence/src" -m clj-surgeon.agent-routing check "/home/forge/src/clj-surgeon-fence/resources/clj-surgeon-agent-routing.md" "/home/forge/.codex/AGENTS.md" "/home/forge/.claude/CLAUDE.md"
{:ok false, :operation :check-agent-routing, :error-type :agent-routing-stale-version, :expected-version 2, :targets [{:path "/home/forge/.codex/AGENTS.md", :previous-state :stale, :changed true, :stale-version 1} {:path "/home/forge/.claude/CLAUDE.md", :previous-state :stale, :changed true, :stale-version 1}]}
make: *** [Makefile:195: check-agent-routing] Error 2
EXIT=2
```

## Skill mirrors

```text
$ make check-clj-surgeon-skill-mirrors
bash bench/sync_clj_surgeon_skill.sh --check
clj-surgeon skill mirrors synchronized (--check)
EXIT=0
```

## Fast lane at the pins

```text
HEAD=2ed387099911ba33a2df1e56da5b8e78d1214167
BASE=3dda2a61e00ab2a51ddcf594a060a4e61fd10ae3
...
Ran 487 tests containing 4807 assertions.
0 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 46 namespace(s) (TEST-ISO-002/003/004/005/007/010)
EXIT=0
HEAD_AFTER=2ed387099911ba33a2df1e56da5b8e78d1214167
```

## Installer red-team

The supported CLI refuses destinations outside the two globals:

```text
{:ok false, :error-type :agent-routing-target-refused, :scratch false, :refused [{:path ".../outside-must-not-appear.md", :canonical-path ".../outside-must-not-appear.md"}], :allowed ["/home/forge/.claude/CLAUDE.md" "/home/forge/.codex/AGENTS.md"], ...}
OUTSIDE_TARGET_EXIT=2
OUTSIDE_TARGET_WRITTEN=no
```

Traversal and symlink escape under `--scratch` are also refused:

```text
SCRATCH_TRAVERSAL_EXIT=2
ESCAPE_TARGET_WRITTEN=no

{:ok false, :error-type :agent-routing-target-refused, :scratch true, :refused [{:path ".../escape-link/escaped.md", :canonical-path "/var/tmp/routing-landing-outside-2ed38709.XzK1JS/escaped.md"}], :allowed ["/var/tmp/forge/..."], ...}
SYMLINK_ESCAPE_EXIT=2
SYMLINK_ESCAPE_WRITTEN=no
```

Known malformed versions and malformed canonical sources correctly preserve targets:

```text
eb46a2683138fa25a1b4f631ed585b5b16ff322cf3a415dea9b8966b63f86407  malformed-target.md
{:error-type :invalid-managed-routing, :operation :install-agent-routing, :ok false, ...}
MALFORMED_TARGET_EXIT=2
eb46a2683138fa25a1b4f631ed585b5b16ff322cf3a415dea9b8966b63f86407  malformed-target.md
PEER_WRITTEN=no

{:ok false, :error-type :invalid-canonical-routing, ...}
MALFORMED_SOURCE_EXIT=2
SOURCE_TARGET_WRITTEN=no
```

But the oversized mismatched versions and trailing-marker bytes above bypass this refusal, so the answer to “can it write when the block is malformed?” remains **yes**.

Static lint was green:

```text
$ ~/bin/clj-kondo --lint src/clj_surgeon/agent_routing.clj test/clj_surgeon/agent_routing_test.clj bin/check-routing-parity.clj
linting took 48ms, errors: 0, warnings: 0
EXIT=0
```

The scratch trees were removed, the checkout remains clean and detached at `2ed38709`, no real installation occurred, and no prohibited ports or `make mcp-test` were used.