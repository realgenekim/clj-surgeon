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

Four classes have enumerating oracles. The in-process admission matrix crosses
all 24 required cells with checkout, admitted external, and outside-envelope
destinations (72 total). Canonical root admission excludes the workspace first, then checks the existing
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
the checkout is deliberately writable. The Cartesian admission matrix and selected Make boundaries are its class
oracles. The registered red-first cycle covers all four requested classes without
further phase approval; evidence stays in the external Round 5 report.

Empty environment values fall through precedence; an empty user.home denotes
the current directory and is subject to workspace exclusion. After both root
and descriptor admission, startup materializes the canonical state directory
before loading other state consumers, so a dangling configured home link has
an admitted target. No directory is created before both checks pass.


Round 7 supersedes the Round 5 oracle placement and cost allowance. The 72-cell
source × shape × envelope × destination class is enumerated by
state-home-admission-test/configured-root-cartesian-matrix in :fast against
state-root, canonicalization and admit-state-root!, using real temporary paths
and symlinks without child JVMs. The default envelope is represented by its
fixture runtime root; a narrow launcher envelope admits only runtime/allowed.
probe-state-test/configured-root-warm-cells runs five fresh committed Make copies:
two root refusal kinds once each and one valid destination per source, under
120 seconds total. The original four-mode warm witness remains unchanged.
The real-process helper stops the exact warm PID, waits for Make and bounded
process-group disappearance, then deletes. Cleanup retries once after 250 ms,
records the original error in the cell row, and propagates a second failure.
The complete battery must pass below 1,800,000 ms serial-equivalent with margin;
a namespace allowance or fixed-point impact run cannot establish that gate.


Round 8 (Sol SH-ROUND2-01) enumerates throw-before and throw-after for each
real publication stage and registered errno. Failure cleanup attempts removal
without relying on create returning successfully. A cleanup failure supplements
the original refusal with cleanup-failure (temporary path and native errno);
it never replaces the original kind, path or errno. No new refusal kind is needed.
The atomic move remains the commit boundary: failures before it preserve prior
bytes; an injected error after it reports failure with the new descriptor already
published. Rollback after a completed atomic move would introduce a restore race.
The cleanup oracle also crosses every primary errno with every cleanup errno and
both removal timings. No residue is allowed when removal succeeds; a failed
removal must be named, and the fixture removes its deliberately retained residue.
