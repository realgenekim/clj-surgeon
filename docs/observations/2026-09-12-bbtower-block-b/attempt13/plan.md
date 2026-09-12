# Attempt 13: descendant temp and fixture-state boundaries

Starting subject: b45d3eb113b5d258ba3601023a8c64fc9fce8339 on
bb-rewrite-tower-local. The binding user request approves the repair scope and
verification sequence. Existing untracked prewarm-checkonly evidence is preserved.

The owning HLD links to temp-dir-hygiene; its LLD and MCP-OP-TMPHYG-005 now
clarify the product launch boundary. This is a correctness repair, with no
performance claim or changes to budgets, membership, or runtime assignment.

Contract matrix:

| Boundary | Observable promise | Witness |
| --- | --- | --- |
| bb startup | Nonblank TMPDIR is retained except /tmp and /dev/shm trees, which fall back to /var/tmp; blank/unset falls back | Pure matrix, real CLI-shaped child property, generated launcher matrix |
| staged formatter | Explicit selected temp base survives a disagreeing read-only JVM temp property | Root trace and real formatter with java.io.tmpdir=/sys |
| formatter refusal | Native message, path and process evidence survive the typist adapter; live source stays unchanged | Throwing runner plus typist diagnostic fixture |
| staged failure | Every allocated candidate is removed on success or exception | Formatter exception witness |
| durable fixture | tmpfs symlink parent lives beneath product artifact root | Existing artifact refusal, same symlink target and assertions |
| Git fixture | Repository scratch uses selected TMPDIR | Existing mission-git boundary suite |

Required completion checks run sequentially: the three previously red namespaces
once with a fresh empty TMPDIR and /tmp endpoint listings; generated launcher
checks in a scratch installation; relevant shell/Python checks; test-fast once;
landing-gate-prewarm once, with at most one repair and a final prewarm if needed.
No shared installation, push, tag, service change or landing is authorized here.

Prior attempt 1 evidence was moved out of its original directory; read from
commit d4736b46 at docs/observations/2026-09-12-bbtower-block-b/{REPORT.md,probe-contract.md}.
