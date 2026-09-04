/* Independent MCP operation-clock shadow model.

   This model earned retention by finding that the original
   `verification-result` outcome class admitted one completion witness while
   leaving the distinct pending and failed clock laws unwitnessed.
*/

%% @spec MCP-OP-ORACLE-001

required_outcome(inspect_clojure, read_success).
required_outcome(inspect_clojure, prepared_basis).
required_outcome(inspect_clojure, verification_pending).
required_outcome(inspect_clojure, verification_complete).
required_outcome(inspect_clojure, verification_failed).
required_outcome(inspect_clojure, typed_refusal).

required_outcome(apply_clojure_changes, committed).
required_outcome(apply_clojure_changes, verification_pending).
required_outcome(apply_clojure_changes, typed_refusal).

required_outcome(edit_clojure, committed).
required_outcome(edit_clojure, typed_refusal).

required_outcome(alias_migration, committed).
required_outcome(alias_migration, typed_refusal).

required_outcome(transform_clojure, preview).
required_outcome(transform_clojure, committed).
required_outcome(transform_clojure, typed_refusal).

required_outcome(admit_clojure_patch, preview).
required_outcome(admit_clojure_patch, committed).
required_outcome(admit_clojure_patch, typed_refusal).

required_outcome(feature_thread, receipt).
required_outcome(feature_thread, typed_refusal).

declared(Tool, Outcome) :- required_outcome(Tool, Outcome).
witnessed(Tool, Outcome) :- required_outcome(Tool, Outcome).

requires_job_clock(verification_complete).
requires_job_clock(verification_failed).

forbids_job_clock(read_success).
forbids_job_clock(prepared_basis).
forbids_job_clock(verification_pending).
forbids_job_clock(typed_refusal).
forbids_job_clock(committed).
forbids_job_clock(preview).
forbids_job_clock(receipt).

requires_clock_labels(verification_complete).
requires_clock_labels(verification_failed).

valid_observation(Tool, Outcome, request, Job, Labels) :-
    declared(Tool, Outcome),
    ((requires_job_clock(Outcome), Job = job, Labels = labeled);
     (forbids_job_clock(Outcome), Job = no_job, Labels = request_only)).

expected_pass(inspect_clojure, verification_pending,
              request, no_job, request_only).
expected_pass(inspect_clojure, verification_complete,
              request, job, labeled).
expected_pass(inspect_clojure, verification_failed,
              request, job, labeled).
expected_pass(edit_clojure, typed_refusal,
              request, no_job, request_only).

expected_fail(inspect_clojure, verification_pending,
              request, job, request_only).
expected_fail(inspect_clojure, verification_complete,
              request, no_job, request_only).
expected_fail(inspect_clojure, verification_complete,
              request, job, request_only).
expected_fail(transform_clojure, preview,
              no_request, no_job, request_only).

missing_current(Tool, Outcome) :-
    required_outcome(Tool, Outcome),
    (\+ declared(Tool, Outcome); \+ witnessed(Tool, Outcome)).

legacy_witness(inspect_clojure, verification_result,
               verification_complete).

legacy_counterexample(State) :-
    member(State, [verification_pending,
                   verification_complete,
                   verification_failed]),
    \+ legacy_witness(inspect_clojure, verification_result, State).

all_expected_pass :-
    \+ (expected_pass(Tool, Outcome, Request, Job, Labels),
        \+ valid_observation(Tool, Outcome, Request, Job, Labels)).

all_expected_fail :-
    \+ (expected_fail(Tool, Outcome, Request, Job, Labels),
        valid_observation(Tool, Outcome, Request, Job, Labels)).

main :-
    findall(Tool-Outcome, missing_current(Tool, Outcome), Missing),
    findall(State, legacy_counterexample(State), Legacy0),
    sort(Legacy0, Legacy),
    ExpectedLegacy = [verification_failed, verification_pending],
    (Missing = [], Legacy = ExpectedLegacy,
     all_expected_pass, all_expected_fail ->
        format('mcp-operation oracle: pass; legacy counterexamples=~w~n',
               [Legacy]),
        halt(0)
    ;
        format(user_error,
               'mcp-operation oracle: fail; missing=~w legacy=~w~n',
               [Missing, Legacy]),
        halt(1)).

:- initialization(main, main).
