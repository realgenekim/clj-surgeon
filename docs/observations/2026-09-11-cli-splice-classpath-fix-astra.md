# CLI splice classpath fix

Recorded 2026-09-11T14:40:23.167803+00:00.
Worktree `/home/forge/src/clj-surgeon-clifix`, branch `fable/cli-splice-classpath`,
base `eae1e43280635be9b4e317e176d29476fd358702`, tip
`223b31ad994259a3ae919e0ef06bcaa35b92c461`. Clean tree; no push.

The package fix and all non-server verification are complete. **Prewarm remains
unrun pending clarification of the mandate's absolute “no servers” restriction.**
The requested prewarm includes mcp-hot-verify and mcp-http-server integration tests
that start temporary servers on port 0. No gate/server exception was inferred.
Prewarm invocation count: 0; no prewarm or landing authority is claimed.

## Commits

- `87a2653a` — register CLI-PACKAGE-001/002, add installed-launcher and library hash
  regression witnesses, correct AGENTS.md's one stale `linked-intent-dev` reference
  to `linked-intent-testing` as explicitly authorized. No other AGENTS.md change.
- `223b31ad` — copy libs/clj-splice/src into the immutable CLI package, name it in
  the launcher classpath, hash its source and deps.edn, mark witnessed intents.

The canonical skill used was
`/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md`.
The existing bidirectional @spec audit discovers the new leaf and both witnesses;
no separate duplicate registry mechanism was introduced. Babashka supplies the
library's Clojure and rewrite-clj dependencies; no extra bb-only dependency exists.
The MCP catalog and operation implementations are unchanged.

## RED and installed boundary

[red.log](red.log): base installer, fixture under this directory, both write verbs
fail with FileNotFoundException for clj_splice/core. One test: 3 passing assertions,
4 failures, 0 errors; ls remains successful. This is the expected RED.

[fixture-install.log](fixture-install.log): fixed witness, 7/7 assertions.
[tip-fixture-install.log](tip-fixture-install.log): retained final-tip install and
literal refusal lines. Exact install arguments:

```
make --no-print-directory install-cli \
  CLI_DEST=/var/tmp/forge/clifix-fx/tip-install/bin/clj-surgeon \
  CLAUDE_HOME=/var/tmp/forge/clifix-fx/tip-install/claude \
  INSTALL_ROOT=/var/tmp/forge/clifix-fx/tip-install/packages
```

VERSION_ROOT is derived under INSTALL_ROOT/versions/<commit>. Package hash:
`0cc2dce28c7f304a1fc642ae23fa0165f23e92c0a6421741e78d77b86d0801ae`.
Both installed `:op :insert-forms!` and `:op :rename-alias!`, with an empty EDN
request file, return exit 2, `:error-type :invalid-request`, `:state "refused"`,
`:committed false`, and `:mutation_attempted false`. `:op :ls` exits 0 and lists
answer. All three launch from outside the checkout. The permanent witness also
uses a destination containing spaces. The versioned fixture package is retained
for review; no install targeted ~/bin.

## Verification receipts

| Check | Result | Receipt |
| --- | --- | --- |
| Full installer namespace | 13 tests / 399 assertions, 0 failures/errors | [install-tests.log](install-tests.log) |
| Twenty verb groups | 20 tests / 382 assertions, 0 failures/errors | [twenty-verbs.log](twenty-verbs.log) |
| Lint via ~/bin/clj-kondo | 0 errors / 0 warnings, changed Clojure test file | [lint.log](lint.log) |
| Intent audit | :ok true | [intent-audit.log](intent-audit.log) |
| Census regeneration | 1690 deftests, 7 assertions pass; diff read and empty | [census.log](census.log), [census.diff](census.diff) |
| make test-fast | 755 tests / 7932 assertions, 0 failures/errors, 0 isolation violations across 61 namespaces | [test-fast.log](test-fast.log), [fast-receipt.edn](fast-receipt.edn) |
| Fast prerequisite: standalone splice bb | 5 tests / 59 assertions, 0 failures/errors | [test-fast.log](test-fast.log) |
| landing-gate-prewarm | NOT RUN: unresolved no-server constraint | no receipt |
| Patch review | git diff --check passed; named-file diff retained | [branch.diff](branch.diff) |

Direct JVM verification used `clojure -J-Xmx1g`. The unmodified make test-fast
coordinator and children use their repository-pinned smaller 512 MiB heaps.
The fast gate ran with final implementation/spec bytes; the tip commit records
those bytes. Census excludes this existing bb-only install namespace, so its
new tests do not alter the JVM census; test/run_all.clj already runs the namespace.

## Dogfood

Used the WORKTREE Babashka entrance:

```
bb -Djava.io.tmpdir=/var/tmp/forge/clifix-fx \
  --classpath src:libs/clj-splice/src -m clj-surgeon.core \
  :op :insert-forms! :request-file /var/tmp/forge/clifix-fx/dogfood/request.edn
```

It inserted `(defn doubled-answer [] (* 2 (answer)))` after `answer` in
[dogfood/src/sample.clj](dogfood/src/sample.clj).
[dogfood.log](dogfood.log) records committed=true, write_verified=true,
forms_inserted=1, other_forms_checked=2 and other_forms_unchanged=true.
This proves structural publication, not application behavior.

Two setup mistakes were caught and disclosed: an initial bb test invocation set
java.io.tmpdir after babashka.fs initialized and created cleaned-up /tmp fixtures;
the retained RED was rerun with -D at launch entirely under this fixture root.
The first dogfood invocation supplied an unrecognized receipt-root environment
name; its one durable artifact was moved from the default state directory to
[dogfood-details.edn](dogfood-details.edn), and subsequent commands use
CLJ_SURGEON_ARTIFACT_ROOT. The exact original/retained locations are in
[dogfood-artifact-location.txt](dogfood-artifact-location.txt). No mutation replay.
Temporary test fixtures were removed in finally; the explicit tip install,
dogfood specimen, logs and receipts remain under this directory for review.
