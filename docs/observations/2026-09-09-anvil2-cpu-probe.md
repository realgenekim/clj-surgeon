# anvil2 vs Anvil: controlled CPU probe (both boxes at rest; identical script and binaries)

Gene: "test anvil2 -- not sure I believe it can be 2-3x slower than anvil". The provisioning agent's number came from a Babashka loop on a busy Anvil. This rerun uses identical probes on both boxes at load ≤ 1.2 / 0.1, three repetitions where it matters.

| probe | Anvil (EPYC Genoa, 16 vCPU) | anvil2 (Xeon Skylake, 16 vCPU) | anvil2 / Anvil |
|---|---|---|---|
| C tight loop, 2e9 volatile adds (gcc -O1) | 0.57 s | 5.1–5.6 s | **9×** |
| Python loop, 3e7 iterations | 2.3–2.6 s | 13.1–13.5 s | 5.6× |
| JVM hot loop, 2e9 iterations, serial GC | 1.9 s | 7.4–7.7 s | 4× |
| Clojure startup (`clojure -M -e`) | 0.42–0.45 s | 2.8–8.8 s | 7–20× |
| 16 parallel Python loops, 1e7 each | 1.08 s | 6.1 s | 5.7× |
| OpenSSL SHA-256 16 KiB blocks | 1.74 GB/s | 0.18 GB/s | 10× (anvil2 lacks sha_ni) |
| per-vCPU pinned Python (cpus 0,3,7,11,15) | 0.48–0.61 s | 2.9–3.1 s | uniform, 5–6× |

Both report 2.29 GHz nominal, 1 thread per core, 16 cores, KVM, steal 0 in /proc/stat before and during a 5-second loop. anvil2 carries the full Skylake mitigation set (PTI, IBRS, MDS buffer clears, L1TF PTE inversion); Anvil is "not affected" on nearly all of them. Mitigations explain a fraction; they do not explain a 9× compiled loop that does no syscalls.

Reading: Skylake vs Genoa at equal clocks is under 2×. A uniform 5–9× across every core with zero visible steal is the signature of a hypervisor CPU quota or an oversubscribed host on a shared-vCPU tier, where the guest is not shown the stolen time. The anvil2 instance as delivered is roughly one fifth of Anvil per core. It is fine for records, editing and light JVM work; it cannot host the gate or a cohort, and no measurement taken on it may be compared with Anvil's.

Options are Gene's (rung 5): open a Hetzner ticket citing the probe; delete and re-create the CX53 in another location or on another host and re-run this script (`/var/tmp/forge/anvil2-bench.sh` + `bench/Loop.class` + `tight.c` are on both boxes); or move the dev box to a CPX (AMD, shared) or CCX (dedicated) tier. A five-minute sampler of the compiled loop is running on anvil2 (/var/tmp/forge/tight-5min.log) to show whether this is a constant or a noisy period.

## Sampler (2026-09-09T13:24Z)

Ten compiled-loop runs on anvil2 at 35-second spacing, 13:18–13:23Z: 5.04, 5.04, 5.13, 5.17, 5.18, 5.20, 5.42, 5.53, 6.16, 6.22 s (Anvil: 0.57 s). Constant, not a noisy period: 9–11× throughout. Clocks on both boxes agree to the second (NTP active).
