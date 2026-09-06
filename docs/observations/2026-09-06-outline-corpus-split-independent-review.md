# Astra independent outline split invariant review

**GO for the test-only split at 7e65353f4fff6772132e1be8b64dd76388888217. Clean merged-tree gate remains HOLD pending the landing owner's run.**

Executed the exact-owner comparison under nice10 Babashka with isolated events, after independently verifying that original.clj equals base2cc32d3a and every changed file equals the reviewed7e65353f tip:

- Moved source-files and full-corpus deftest are byte-identical.
- All nine frozen helper bodies are preserved; legacy-outline-source changes only defn- to defn visibility.
- All four bounded tests remain byte-identical in the fast namespace.
- Independently counted87 unique manifest namespaces:49 fast,6 integration,32 battery.
- No production source, Makefile, budget, exclusion or corpus-sampling change.

Nonblocking editorial finding: the integration grouping comment still says5 and the new integration entry appears inside the fast comment block. Actual metadata and manifest lane are correct. Regrouping that entry and correcting the comment should preserve the parsed manifest exactly; this report does not certify an unreviewed later tip.

The builder's662-test/8011-assertion full run remains a FAILED gate because the plan changed during its working-tree isolation check. The interrupted rerun is not a pass. Focused30-test/133-assertion green is builder evidence; this independent review executed source invariants only. No duplicate JVM/full gate, providers or services were launched.
