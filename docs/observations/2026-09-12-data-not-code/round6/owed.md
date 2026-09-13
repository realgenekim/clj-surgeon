# Retained limits and debt

The coverage remains the shared artifact boundary and its inventoried consumers.
These earlier uncovered writers are unchanged by round 6:

- `src/clj_surgeon/mcp_telemetry.clj:70-78`: telemetry's direct directory/file
  writer does not pass through the shared artifact boundary.
- `src/clj_surgeon/mcp_http_server.clj:234-240`: HTTP readiness publication
  writes directly rather than through shared artifact admission.
- Telemetry resolves `user.home`; envelope policy resolves the passwd home.
  That mismatch is not repaired or hidden by this block.

Hard-link admission accounts for inode links visible inside the envelope at
check time. It does not eliminate check-to-write races. Non-POSIX providers
refuse when inode evidence is unavailable; the unavailable-attribute seam is
witnessed, but actual Darwin and non-ext4 filesystems were not available.
The sibling path is exercised by the real journal protocol. No wall bound is
claimed for searching a large envelope when sibling evidence is insufficient.

The diff-impact oracle covers declared namespace dependencies through two
edges, independent of cadence or runtime exclusion. Arbitrarily computed
dynamic require targets are outside that discovery claim. This run complements
prewarm's missing battery coverage; it does not redefine prewarm as a full
battery gate.

Receipts are evidence, not attestation. The retained-path rule excludes scratch
receipts and symlink escapes, but does not authenticate authorship or prevent
coordinated edits to retained evidence. The original shared CLI output-size
contract failure remains real on both runtimes; backing its runtime escape with
the retained failed control does not fix that separate output contract.

No probe/native measurement, live-image attestation, bettor result, routing
admission, push, tag, install or merge is claimed. The amendment-1 measurement
subject remains 759974c718889c52a05a1d0746c9c21d0f00dcdb; this instrument branch
is a different tree. Future experiment setup must create a fresh subject.json
for the amended runner. Independent acceptance/review remains separate from
the builder's executed tests.
