# Astra: first raw cohort stopped at its native control check

Recorded 2026-09-06T02:57:30.146958+00:00. Raw artifacts:
`/var/tmp/forge/astra-raw-cohort-fx`.

Six native controls finished:5verified/1failed. Walls20.608,26.170,24.266,23.163,
20.757,36.644s. All terminal walls are retained. One already-started pairednative
run also verified in21.661s before the driver stopped. No fast-provider call ran.
The extra native call occurred because the positive-control assertion was placed
immediately before first T rather than immediately after controls; retain it.

The failure is genuinely in the native model's final result, not capture. In
session01a074a0-4098-7283-8ee1-603b83d1cf46 the model applies the correct patch,
runs2tests20assertions green, then says it acted prematurely and reverts to the
original file. It finishes READY instead of DONE, despite the latest user prompt
explicitly authorizing the edit. The frozen orientation's earlier read-only phase
confused this one continuation. Exact trace is in
`/var/tmp/forge/typist-real-fx/NW-real-1-1788663054-1248360-0/nw-stdout.txt`.

The preregistration required six verified positive controls, so the cohort stopped
before raw arms. That was an overly strict interpretation: a stochastic model's
wrong result is evidence, not automatically a broken positive-control apparatus.
Future fixed-size cohorts should retain real model failures in correctness and
terminal-latency results, while stopping for actual runner/identity/proof failures.
This is a prospective correction, not a reclassification of this aborted cohort.

Before the next fresh cohort, strengthen the native trial prompt to explicitly
end orientation-only mode and authorize keeping the verified change. This gives
the native control a clearer task without supplying a solution. Freeze the changed
prompt and repeat six controls; do not silently reuse this partial cohort as an
unmodified completed comparison. No raw-vs-native speedup claim follows here.
