> WITHDRAWN AS A SCORED COHORT — Astra audit, 2026-09-05T22:10:55.577460+00:00
>
> The table below is historical smoke output, not accepted preregistered controls.
> The runners stopped their clocks BEFORE external acceptance and AFTER server startup;
> did not attest resolved models, sessions, server identity, prompt/runner hashes;
> retained output tails rather than complete rollouts; ran modifiable in-fixture
> acceptance with loading/marker checks and no behavioral assertions; omitted CPU
> pinning/suite lanes and interrupted attempts. The p90 calculation used floor indexing.
> Thus 6/6 below means only that the weak local check exited zero. All 12 native
> controls must be rerun with the registered meter. No speed ratio is established.
>
> The assertion that only `fast` is admitted was also unverified. Source inspection
> shows workspace-configured synchronous external argv profiles are admitted; naming
> `fast` alone does not configure proof. Epoch 2 will configure and hand-drive an
> actual behavioral command before model trials. Prior artifacts remain retained.

# Astra fair comparison result — 2026-09-05

| arm | runs | median wall | p90 wall | correct? |
|---|---:|---:|---:|---|
| native / gpt-5.6-sol | 6 accepted | 36.8295 s | 39.702 s | 6/6 |
| native / gpt-6-astra | 6 accepted | 34.2085 s | 34.605 s | 6/6 |
| helper_extraction / gpt-5.6-sol | 2 attempted, 2 refused | — | — | 0 accepted; refusal retained |
| helper_extraction / gpt-6-astra | 0 launched | — | — | no result |

## Method

The fresh fixture was provisioned at `/var/tmp/forge/astra-fair-fx/fixture-base` from
baseline `209d6da051a01439ab854f41fc43908a7f0d61ae`. Each arm received a byte-identical
copy. The task moved `response`, `ok`, and `missing` from `acme.core` to
`acme.response`, rewrote two callers, and removed the source namespace. An independent
`accept.sh` checked the exact closure and loaded all resulting namespaces.

Native controls ran serially, six per requested model, using pinned Codex CLI 0.153.3
with high reasoning effort. The requested identities were `gpt-5.6-sol` and
`gpt-6-astra`; the surrounding pane status was `gpt-5.6-luna`, so it is not relabeled.
The first native attempt exposed an acceptance-path mistake (59.462 s, return 127); it
was retained under `native/sol-1`, excluded, and the six-control cohort restarted from
fresh copies after the harness was corrected.

## Tool result and stopping rule

Two Sol tool attempts reached the public `helper_extraction` operation on independently
started, attested servers. Both returned the typed refusal
`helper-extraction-verification-preflight-unavailable`: the named `helper-proof` profile
was not admitted as synchronous and rollback-capable; no mutation was attempted and the
tree remained unchanged. The preregistration says to stop and repair rather than score
when the first accepted tool arm cannot complete its proof. Therefore no Astra tool arm
was launched, and no native/tool ratio is reported.

This is a valid apparatus finding, not evidence that Surgeon is slower. A follow-up must
either admit a fresh rollback-capable `helper-proof` profile or preregister a different
profile and repeat the complete tool cohort. The prior 409 s typist-inside-Sol result is
a separate coordination route and is not pooled with this experiment.
