# Astra: warning-only forward-reference analysis

The actual mission_cli.clj outline refused with
`:forward-reference-analysis-failed`, exit 2 and an empty stderr diagnostic.
Read-only diagnosis retained by model_adapter at
/var/tmp/forge/ls-refusal-diagnosis/ established zero lint errors, one redundant-let
warning and 33 definitions/618 usages. Adding only --fail-level error returned
exit zero with the same analysis/findings. The executable help documents that
threshold; we did not guess that arbitrary nonzero exits are safe.

The repair explicitly chooses that analyzer threshold while retaining admitted,
finished, exit-zero process authority. Parsed analysis must contain the requested
vectors of map records; empty vectors remain valid. No new mandatory entry fields
are imposed. Explicit error findings/positive summary errors, malformed JSON and
missing analysis cannot silently become an empty forward-reference answer.

RED: three tests, nine passing assertions, ten failures and one real-warning
error. GREEN: four focused tests, 21 assertions, zero failures/errors, including
the existing real forward-reference schema witness. A minimized valid nested-let
fixture reproduced the warning; its source remained unchanged. Pure/injected
cases cover absent analysis, malformed JSON/container/member data, reported errors,
missing/nonzero exit, unfinished process and unadmitted authority. All runs used
suite-run/nice10, node compile cache disabled and isolated event output.

Source route: Surgeon :cat selected run-kondo and the existing analyzer contract;
the known source branch was changed with native apply_patch, following the
working skill's literal-edit route. No source warning was removed. Changed source
and tests were formatted; the sole lint warning is the inherited redundant let
in detect-forward-refs, now at line59.

The branch public command `bb --classpath src -m clj-surgeon.core :op :ls :file
src/clj_surgeon/mission_cli.clj` exits zero and returns 33 forms. The installed
launcher is pinned to an older version and still reproduces the old refusal;
no installation or shared root was changed. Artifacts are under
/var/tmp/forge/forward-refs-warning-artifacts/ (ls-before.edn, ls-after.edn for the
still-old installation, ls-branch-after.edn, red.log, green.log). No timing or
speedup claim. Three tests added to the existing analyzer contract namespace;
no registry changes. Independent executed review is required before integration.

## Owning gate budget correction

The first full owning make analyzer-contract-test failed 9 tests/45 assertions
with one failure: the new standalone real-warning witness consumed a sixth
launch beyond the fixed five-launch analyzer mission. The failed receipt is
retained at analyzer-contract-gate.log. That standalone witness is superseded
by adding the warning-bearing valid form to the existing real forward-reference
fixture. The same launch now checks the exact forward-reference answer, zero
errors, the retained redundant-let warning and unchanged source. Production
launch limits, expiry, admission and scope are unchanged. Two pure/mock tests
remain new; the final namespace delta is +2 tests, not the initial +3.

The combined fixture's actual pre-fix command exits2 with zero errors/one warning;
the error-threshold command exits0 with the same warning. Raw receipts are
combined-pre-fix.json and combined-fixed.json in the artifact directory.

## Final branch gates

After merging root battery records c1ef2284 and packaging correction2cc32d3a,
the final combined fixture passed its owning `make analyzer-contract-test`:
8 tests,46 assertions,zero skips/failures/errors,exit0. The fixed production
five-launch authority was not changed. Independent reviewer accepted the
combined fixture and this retained gate without a duplicate execution; see
2026-09-06-forward-reference-warning-independent-review.md.

On the same frozen branch code, normal `make test` passed,exit0: visible JVM
662 tests/8009 assertions and BB862 tests/7352 assertions,zero failures/errors;
repository hygiene passed. Both gates ran sequentially through suite-run at
nice10 with NODE_DISABLE_COMPILE_CACHE=1 and isolated events. No slot stderr
redirection, timeout/budget override or installation. Final logs are
analyzer-contract-final.log and normal-gate.log in the existing artifact folder.
The failed initial six-launch owning gate remains retained, not reclassified.
Final change adds two tests to the separate analyzer-contract namespace, excluded
from the JVM lane manifest; no manifest count or namespace enrollment changes.
