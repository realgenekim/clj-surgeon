# Historical require headers

The eight golden headers are exact namespace forms from Marvin commit
9f9cf61441a7f2a984b55226d5d1ca6cbeb6706f. Six seed headers delete exactly
one target line. `capture_archive.clj` and `reducer_session.clj` use their exact parent d170f3d5 headers:
their golden targets share the `(:require` opener, so whole-line deletion creates
an invalid seed. This unit-fixture repair is historical data, not an amendment
to Sol's frozen full-program seed. That construction conflict remains separately
reported and must be resolved before cohort admission. No call sites are included
in these pure header fixtures; the hand-drive uses the isolated full tree.
