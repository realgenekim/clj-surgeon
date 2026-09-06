# Verdict: HOLD

**LAND: DO NOT LAND `b2824ee1` onto `3dda2a61+`.**

Blocking findings:

1. [agent_routing.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/agent_routing.clj:20) recognizes only numeric versions. A malformed `v:x` block is classified `:absent`; installation appends v:2 while preserving the malformed block. This violates the fail-closed marker contract.
2. [check-routing-parity.clj](/home/forge/src/clj-surgeon-fence/bin/check-routing-parity.clj:39) uses `split-lines`, so it does not guarantee byte identity. Adding one CR byte to a rendering leaves the checker green.
3. The requested fast lane is red: **486 tests, 1 failure, 0 errors**.
4. The candidate does not merge cleanly onto the pinned base: three conflicts.
5. The default Make invocation names only the two global files, but both Make overrides and the installer CLI accept arbitrary target paths. A scratch installation outside those paths succeeded.

No repository edits or real installation occurred. Scratch writes were confined to `/var/tmp/forge` and removed. No prohibited ports or `make mcp-test` were used.

## Requested evidence

### 1. Marker probes and focused tests

```text
$ bb --classpath src:test -e '(require ...); replay four probes'
[:v2+v1 false :invalid-managed-routing nil true false 2 2]
[:two-v1 false :invalid-managed-routing nil true false 2 2]
[:v1-v3 false :invalid-managed-routing nil true false 1 1]
[:lone-v1 true nil :stale false false 1 1]
```

The booleans show the first three sources stayed byte-unchanged. The lone stale pair was replaced, `OLD-BYTES` did not survive, and exactly one BEGIN/END pair remained.

```text
$ bb --classpath src:test -e '(require (quote clj-surgeon.agent-routing-test) (quote clojure.test)) (let [r (clojure.test/run-tests (quote clj-surgeon.agent-routing-test))] (System/exit (+ (:fail r) (:error r))))'

Testing clj-surgeon.agent-routing-test

Ran 14 tests containing 130 assertions.
0 failures, 0 errors.
```

The uncovered malformed-marker case:

```text
$ bb --classpath src -m clj-surgeon.agent-routing install \
    resources/clj-surgeon-agent-routing.md \
    /var/tmp/forge/routing-malformed-b2824ee1.md
{:ok true, :operation :install-agent-routing, ..., :previous-state :absent, :changed true}

$ rg -n 'ROUTING v:(x|2)|MALFORMED-BODY' /var/tmp/forge/routing-malformed-b2824ee1.md
1:<!-- BEGIN CLJ-SURGEON ROUTING v:x -->
2:MALFORMED-BODY
3:<!-- END CLJ-SURGEON ROUTING v:x -->
5:<!-- BEGIN CLJ-SURGEON ROUTING v:2 -->
29:<!-- END CLJ-SURGEON ROUTING v:2 -->
```

### 2. Routing parity

```text
$ bb bin/check-routing-parity.clj
{:ok true, :operation :check-routing-parity, :canonical "skills/clj-surgeon/SKILL.md", :table-rows 12, :renderings-checked 5, :plate "resources/clj-surgeon-agent-routing.md", :pointer-heading "## Edit routing (policy revision 1, 2026-09-06)"}
```

Independent hashes:

```text
7a613ced...59c7624 skills/clj-surgeon/SKILL.md
7a613ced...59c7624 skill.md
7a613ced...59c7624 skills/safe-refactor/SKILL.md
7a613ced...59c7624 CLAUDE.md
7a613ced...59c7624 AGENTS.md
7a613ced...59c7624 docs/observations/2026-09-06-routing-prompt-surfaces.md
```

Requested ordinary one-byte change goes red:

```text
$ sed -i '0,/Owner and line already known/s//Owner and line already Known/' SCRATCH/skill.md
$ (cd SCRATCH && bb bin/check-routing-parity.clj)
{:ok false, :operation :check-routing-parity, ..., :first-difference {:row 2, :canonical "...known...", :rendered "...Known..."}}
exit=2
```

But a one-byte CR insertion evades the claimed byte-parity gate:

```text
$ sed -n '33p' SCRATCH/skill.md | od -An -tx1
... 7c 0d 0a

$ (cd SCRATCH && bb bin/check-routing-parity.clj)
{:ok true, :operation :check-routing-parity, ..., :renderings-checked 5, ...}
```

The pointer heading resolved at canonical line 13, and the cited `docs/observations/2026-09-06-routing-prompt-surfaces.md` exists.

### 3. Make dependency, without install

```text
$ make -n install-agent-routing
bb "/home/forge/src/clj-surgeon-fence/bin/check-routing-parity.clj"
bb --classpath "/home/forge/src/clj-surgeon-fence/src" -m clj-surgeon.agent-routing install "/home/forge/src/clj-surgeon-fence/resources/clj-surgeon-agent-routing.md" "/home/forge/.codex/AGENTS.md" "/home/forge/.claude/CLAUDE.md"
```

Dependency ordering is correct.

### 4. Installed v:1 blocks report stale

```text
$ make check-agent-routing
...
{:ok false, :operation :check-agent-routing, :error-type :agent-routing-stale-version, :expected-version 2, :targets [{:path "/home/forge/.codex/AGENTS.md", :previous-state :stale, :changed true, :stale-version 1} {:path "/home/forge/.claude/CLAUDE.md", :previous-state :stale, :changed true, :stale-version 1}]}
make: *** [Makefile:195: check-agent-routing] Error 2
exit=2
```

### 5. Skill mirrors

```text
$ make check-clj-surgeon-skill-mirrors
bash bench/sync_clj_surgeon_skill.sh --check
clj-surgeon skill mirrors synchronized (--check)
```

### 6. Fast lane

```text
$ /home/forge/bin/suite-run make test-fast
...
FAIL in (the-default-path-is-the-home-dotdir) (telemetry_events_test.clj:344)
expected: (str/ends-with? (events/default-events-file) "/.clj-surgeon/events.jsonl")
actual: (not (str/ends-with? "/var/tmp/forge/test-events/events-3358876.jsonl" "/.clj-surgeon/events.jsonl"))

Ran 486 tests containing 4800 assertions.
1 failures, 0 errors.
0 preconditions skipped.
0 preconditions failed.

test-isolation: 0 violations across 46 namespace(s)
make: *** [Makefile:1009: test-fast] Error 1
```

### 7. Installer red-team

Arbitrary destination succeeds:

```text
$ bb --classpath src -m clj-surgeon.agent-routing install \
    resources/clj-surgeon-agent-routing.md \
    /var/tmp/forge/routing-outside-b2824ee1.md
{:ok true, :operation :install-agent-routing, ..., :target-count 1, :changed-count 1, :targets [{:path "/var/tmp/forge/routing-outside-b2824ee1.md", :previous-state :absent, :changed true}]}
```

Make overrides expose the same capability:

```text
$ make -n install-agent-routing \
    CODEX_GLOBAL_INSTRUCTIONS=/var/tmp/forge/codex-outside.md \
    CLAUDE_GLOBAL_INSTRUCTIONS=/var/tmp/forge/claude-outside.md
bb ".../bin/check-routing-parity.clj"
bb ... install ".../resources/clj-surgeon-agent-routing.md" "/var/tmp/forge/codex-outside.md" "/var/tmp/forge/claude-outside.md"
```

Numeric malformed pair correctly refuses and preserves the hash:

```text
51ebeb998ebd603d7e0d5180321f85832b3f30768a366ddf1e75830f71f8a3f8  .../routing-mismatch-b2824ee1.md
{:error-type :invalid-managed-routing, ..., :begin-versions [1], :end-versions [3], :ok false}
exit=2
51ebeb998ebd603d7e0d5180321f85832b3f30768a366ddf1e75830f71f8a3f8  .../routing-mismatch-b2824ee1.md
```

Malformed canonical source also refuses and preserves its target:

```text
fbf8a129c10b3a24a2d278437d2a66246175359083b90587e22ce353120efeea  .../target.md
{:ok false, :error-type :invalid-canonical-routing, ...}
exit=2
fbf8a129c10b3a24a2d278437d2a66246175359083b90587e22ce353120efeea  .../target.md
```

### Landing topology

```text
$ git merge-tree 3dda2a61 b2824ee1 | rg 'CONFLICT'
CONFLICT (content): Merge conflict in bin/typist-run-test
CONFLICT (add/add): Merge conflict in docs/observations/2026-09-06-sublime-tool-for-astra.md
CONFLICT (content): Merge conflict in test/clj_surgeon/lane_manifest_test.clj
```

Static lint was otherwise clean:

```text
$ /home/forge/bin/clj-kondo --lint src/clj_surgeon/agent_routing.clj test/clj_surgeon/agent_routing_test.clj
linting took 32ms, errors: 0, warnings: 0
```