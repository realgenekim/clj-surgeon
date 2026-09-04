You are working in a throwaway git worktree of a Clojure project. Do the task below and stop.
Do not commit, do not push, do not create branches or worktrees.

1. THE TASK

The namespace acid.fanout.store is being retired. Its var find-event has moved to
acid.fanout.store2 and been renamed fetch-event. Nothing else in the tree changes.

In EVERY namespace under src/ that requires acid.fanout.store:
  - replace that require with acid.fanout.store2,
  - rewrite every qualified use of the old var to the new namespace and the new var name,
  - choose the alias for the new require by this policy, applied to THAT FILE ALONE:
        prefer  store2
        if store2 is already bound in that file, use  st2
        if st2 is already bound in that file, use     es
        if es is already bound in that file, use      store-2
    Never introduce an alias that shadows an alias or a referred name already bound in
    that file. Different files will legitimately end up with different aliases.

There are exactly 21 such namespaces. src/ contains 100 namespaces in total; the other
79 must not change.

2. WHAT MUST NOT CHANGE

A local binding named find-event, the string literal "find-event", the token find-event
inside a docstring or a comment, and any use of find-event that resolves to a DIFFERENT
required namespace are all unrelated to this change and must be left byte-for-byte alone.
Comments, metadata, reader-conditional branches and #_ discards stay where they are.

3. DONE MEANS

  - every namespace under src/ loads,
  - bin/fan-test is green at its base count with an empty failure set,
  - no occurrence of acid.fanout.store or of the old var name remains in src/.

4. RITUAL

This worktree is throwaway and has no reviewer and no beads workflow: do not run bd, do not
run git status or git diff, do not re-read a file you just patched, do not hand-run
clojure -M -e syntax probes; the apply_patch result is your verification of the edit and
bin/fan-test performs the load check. Every extra command costs a full model turn.

Four specific things this environment will tempt you into, all of which are waste here:
  (i)   run the suite ONCE, with a single blocking wait; do not poll it,
  (ii)  target/ and .cpcache are generated; never clean them, never inspect them,
  (iii) there is no skill or playbook installed for this task; do not search the filesystem
        for one, and never run a find or rg rooted above this worktree,
  (iv)  report your total tool-call count on the last line as  TOOLCALLS: <n>.

5. TOOLING

A clj-surgeon MCP server is configured. It exposes alias_migration, one call that takes the
whole intent — the old lib and var, the new lib and var, the alias policy in order, and the
number of files you expect to be affected — discovers every requiring namespace and every
qualified site itself, chooses each file's alias against that file's own bindings, and
returns one receipt: files changed, sites rewritten, the alias histogram, collisions
resolved, the kondo delta and the focused-test result.

Route the write through that call. Its receipt is your verification of the rewrite; do not
re-read the files it reports as changed. If it refuses, it returns an executable next_call —
send that. You still have your native tools; use them if the tool cannot complete the task.
