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

Round 5 closes four classes with enumerating oracles before implementation.
Canonical root admission excludes the workspace first, then checks the existing
envelope without extending it. The same exclusion applies to the final descriptor
destination, including explicit overrides and redirected descendants. Publication
uses a same-directory temporary file, complete write, fsync, and atomic rename;
no failure path truncates or restores the old file. One function var supplies the
injectable stage seam. Native exception class/message mapping remains bounded.
All local probe resolution and reads occur inside a filesystem receipt boundary,
before transport. Paths remain data, bounded at the receipt crossing with explicit
truncation metadata. The generated portability header derives its population from
the same rows as its table; a single independent reading witness compares every
inventory. Existing battery and fast namespaces own these tests and their lanes.

The second envelope-writer recurrence is invisible to the prior Landlock oracle:
the checkout is deliberately writable. The Cartesian Make matrix is its class
oracle. The registered red-first cycle covers all four requested classes without
further phase approval; evidence stays in the external Round 5 report.
