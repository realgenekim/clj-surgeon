# Warm image identity outside the checkout

The warm image's identity is seat state, not project source. Incident inb-3c65d7
made every clean-checkout PROBE cell refuse after `make warm` wrote its descriptor.
This leaf extends the warm verification design: one canonical workspace path,
hashed with SHA-256, selects one descriptor under the declared seat state root.

Root precedence is CLJ_SURGEON_STATE_HOME, XDG_STATE_HOME/clj-surgeon,
then user.home/.local/state/clj-surgeon. A shared resolver serves startup and
probe; an explicit image-file remains authoritative. Publication admits the
resolved destination through the existing envelope before creating directories.
Native filesystem failures carry the path, exception class and errno category.
Probe receipts include the descriptor path even when reading or transport fails.

Misreadings: ignoring the checkout write is not a repair; hashing repository HEAD
would merge distinct checkouts; missing files are not dead connections; a requested
state path does not authorize bypassing a narrower destination envelope.

Boundary matrix: environment precedence, two workspace roots, override, missing
file, non-writable root, successful write/read, stale descriptor, refused envelope,
and actual Make startup followed by exact-process shutdown and empty git status.
Other .clj-surgeon consumers, telemetry policy and .gitignore are out of scope.

The registered build authorizes the red-first implementation cycle in this leaf.
Verification: named affected namespaces, serialized lint, fixed-point diff-impact
and each namespace it selects. No merge, push or performance claim is included.

The registered requirements and witnesses are in [state-home-specs.md](state-home-specs.md).
