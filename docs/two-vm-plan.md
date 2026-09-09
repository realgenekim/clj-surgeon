# The two-VM plan — long-term replacement for Anvil (for Gene's reference)

*Written 2026-09-08 by the Anvil seat after Gene's questions: "what kind of VM or VPS would you ask for", "what would be so bad about an ARM box", "I'm thinking shared VPS for dev box, and on demand dedicated vm for timing."*

## What today measured on Anvil (16 cores, 30 GB, no sudo)

| symptom | evidence (2026-09-08 log) |
|---|---|
| memory ceiling | 25 JVMs at peak; warm images 0.6–2.1 GB each (measured); a wide suite run grew one image to 2.14 GB |
| contention | load 21 at the worst moment; agents refuse above 12; four lanes queued on the suite lock; timing arms waited for a quiet box |
| disk | 99 % full; a 29 MB review archive was a second failure mode |
| no sudo | every toolchain outside Clojure becomes a user-local install and an apparatus action; the cross-stack pilot could not run here |

The constraint is contention, not raw size. Hence two boxes, not one bigger one.

## Box 1 — the dev, build and landing box (shared VPS is acceptable)

- **RAM is the number that matters: 64 GB or more.** Below that we re-hit today's ceiling and swap. 16 vCPU or more; NVMe; root.
- CPU steal on a shared tier costs only jitter on the battery wall (218 s today) and the routine's timings. Those are gates, not measurements; correctness is unaffected.
- Runs: agents (Astra, Sol, Opus builders), the warm images, the 8-lane battery, ship/land, the daily canary and seat receipt.
- **Rule:** no latency number that decides a route, admits a verb, or goes in a report is ever taken here. The daily canary's apparatus SHARE is a count and stays valid; its warm-first seconds are diagnostic only.

## Box 2 — the measurement box (dedicated cores, on demand)

- Dedicated vCPU, 8 cores, 32 GB, x86, hourly billed; spun up for a cohort, destroyed after.
- Every p95, every vs-native wall, every route admission or retirement comes from here.
- **Snapshot image with toolchains and caches baked in** (JDK, Clojure, Maven and gitlibs caches, Node, Rust when the pilot runs, the recorder's private state dir) so spin-up is ~2 minutes and the first arm is not the cold-cache outlier. Provisioning from scratch is 10–15 minutes and skews the first arm.
- The same provisioning script stands up both boxes (inb-94ba79 lineage); the cold-start harness (`coldstart`, `coldstart-grade`) proves a box before it carries a seat.

## ARM: fine in principle, wrong tier in practice

Claude Code, the Codex CLI, Temurin JVMs, Babashka and clj-kondo all ship arm64 Linux builds; the apparatus is bash/Python/Clojure. Two real costs: Hetzner's ARM (CAX) is shared-vCPU only, so it cannot be the measurement box; and Ampere cores are ~1.5–2× slower single-thread than a current Ryzen/EPYC, which is the axis the warm loop lives on. If a provider offers dedicated fast ARM cores, the objection disappears and the harness proves it in an afternoon. Never buy shared cores for box 2.

## Keep Anvil

As the third seat and the cold-start specimen host.

## Concretely, at Hetzner (prices approximate, hourly/monthly as billed)

- Box 1: a shared tier with ≥ 64 GB if one exists at the provider; else the AX102-class dedicated (16c/32t, 128 GB, 2×1.92 TB NVMe, ~€100/month) — the extra cost is the memory and it is worth it.
- Box 2: CCX33 (8 dedicated vCPU, 32 GB, 240 GB NVMe), snapshot kept, ~€10–15/week when used.
- Decisions on Gene's desk: inb-3fa126 (the measurement box; snoozed to 2026-09-22 behind the rows), the provisioning script item, and rung 7 (instance, key, budget line).

*Also filed as docs/observations/2026-09-08-two-box-plan.md; this copy is the reference location Gene asked for.*

## Status 2026-09-08T22:02Z — box 1 purchased

Gene bought a Hetzner **CX53** (16 shared vCPU, 32 GB, 320 GB SSD, shared cost-optimized tier, $34.99/month). Same 32 GB as Anvil, so the memory ceiling depends on the heap caps holding (every JVM now -Xmx 512 MB–1 GB; ~25–30 bounded images fit). Resizable in place if outgrown. Rule from day one: no latency number from this box. Next: hostname + the forge seat's public key on the box → the provisioning script (inb-94ba79) → the cold-start harness proves a fresh agent there before it carries a seat.

## Status 2026-09-09T01:37Z — box 1 = 2.28.125.30 (CX53); provisioning plan
Reuse curtaincall-cfp docs/anvil-history.md's sequence. Users = seats, one per Codex login: forge, sol, astra, recorder (receipt issuance only — makes Sol's condition 2 real on one box), gene (passwordless sudo). Seat key: ~/.ssh/id_ed25519.pub on Anvil (generated 2026-09-09). Then toolchains, one-shots, skills, records repo, cold-start proof, disk retention.

## Status 2026-09-09T01:53Z — anvil2 secured; provisioning in flight
root@2.28.125.30 keys-only (seat key), hostname anvil2, root password rotated (break-glass, ~/secrets on Anvil). Provisioning script being written at /home/forge/bin/provision-anvil2.sh (records repo bin/ when done). Seats as users: forge, sol, astra, recorder, gene. Not yet: Codex/Claude logins (Gene, interactive), tailscale, the surgeon worktrees and 7906 (migration plan, parked).
