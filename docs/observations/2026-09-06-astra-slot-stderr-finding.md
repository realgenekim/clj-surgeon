# Slot stderr loss: confirmed shell semantics

`/home/forge/bin/slot` executes `exec {fd}>"$D/$i" 2>/dev/null` without a command. Bash applies both redirections to its own shell, so its subsequently launched command inherits `/dev/null` on stderr. Exit status is unaffected. This explains missing direct JVM/report stderr while test stdout and exit accounting remain available.

Pure shell reproducer in result.json: identical construct loses `lost-stderr`, preserves stdout and exit7; restoring a pre-saved descriptor preserves `restored-stderr` and exit7. No actual slot, shared lock, JVM, provider, service, quiet file or shared launcher was changed.

Per-invocation recovery (including explicit temporary-directory guard):

```sh
SLOT_OWNER=astra ~/bin/slot -t bash -c 'exec 2>&3 3>&-; source "$HOME/bin/seat-tmp-guard.sh" || exit; exec "$@"' slot-stderr make test 3>&2
```

For a combined log, put the capture before descriptor duplication:

```sh
SLOT_OWNER=astra ~/bin/slot -t bash -c 'exec 2>&3 3>&-; source "$HOME/bin/seat-tmp-guard.sh" || exit; exec "$@"' slot-stderr make test > own.log 2>&1 3>&2
```

The command child restores stderr. Slot's own messages after its faulty exec remain suppressed; this wrapper does not repair the shared script. It assumes fd3 is available as a temporary caller-owned descriptor. Slot allocates its lock descriptor through Bash's dynamic descriptor allocation, which uses higher descriptors.

A third executed witness redirects the child's stderr explicitly to its own file after the shell redirection; its output is retained. Thus subprocess PIPE/file capture such as the raw cohort's explicit per-child receipts is not inherently erased by inherited slot stderr. No cohort was re-run or reinterpreted here; ambient uncaptured diagnostics were lost.

`slot` does not source seat-tmp-guard; `suite-run` does. The command above sources it explicitly, and fails before the gate if that source operation fails. The current running gate cannot retroactively recover discarded bytes.
