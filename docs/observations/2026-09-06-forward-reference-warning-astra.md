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
