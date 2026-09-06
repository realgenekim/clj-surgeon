# Astra: real two-form refusal diagnostic repaired

The installed CLI committed a needed diagnostic fix on the working branch.
A successfully parsed single-form input with the wrong number of syntax units
now reports expected=1, actual=N and the sentence
“<field>: one complete form expected; N supplied.”
The original acceptance predicate is byte-identical. Detached comments keep
their prior explanation; malformed input has no invented count; reader-discard
nodes retain their previous treatment. No sequence lowering was introduced.

The faithful reproduction comes from the actual 2026-09-06 08:18:04.452Z MCP
request: replace [clojure.edn :as edn] with two adjacent require vectors.
The saved draft request differed from that executed call, so the regression
uses the bound call, through actual public validation, lowering, compilation
and normalization. Its public field is :do replacement.

Fable reviewed the design/spec and test phases. Independent static review
checked the exact proposed edit and execution wrapper before use. The actual
baseline ran 2 tests / 72 assertions: 51 pass, 21 expected diagnostic failures,
zero errors. The installed CLI first previewed without changing source, then
committed one guarded change in one file via stdin. The same unchanged tests
then passed all 72 assertions, with zero failures/errors. Paved clj-kondo
reported zero warnings/errors. Independent read-only result review confirmed
that all baseline failures concern the missing counts/message and that current
source equals the approved replacement with unrelated bytes unchanged.

Receipt: 69ed8beb6d4b0753adac9cb98a90ab4de5c0ce483fdf8b30369f8174039884cc.
Post-image: 472ee7ecde22454da7ce6dce874a119e16bed0e86248760a82cb0ca9501558fa.
Artifacts: /var/tmp/forge/astra-cardinality-fx/.
Both focused runs had unchanged proof/source pins during execution, no timeout
and no surviving process group. This is group-containment evidence, not a
general descendant census.

This is CLI dogfood, not a fast-typist attempt or a speed comparison. Preparation
included recovering the actual request, writing/reviewing the proof and coordinating
an uncontended JVM window; no edit-runtime ratio is claimed. Full normal gates
and landing review remain pending. Public main and installed binaries were not
updated by this branch change.
