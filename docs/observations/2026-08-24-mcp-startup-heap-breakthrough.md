# MCP startup heap breakthrough: 512 MiB is now a measured default

**Question:** Can clj-surgeon MCP stop reserving a 2 GiB heap without giving up
the embedded live-reload channel?

**Answer:** Yes. A production-shaped Streamable HTTP server with an embedded
plain nREPL starts, becomes healthy, captures heap state, forces GC, and reloads
the live tool registry at `-Xmx512m`. It also completes that acceptance at 384
MiB and 256 MiB. We chose 512 MiB as the default because it leaves useful
headroom above the observed live set without paying the 2 GiB reservation.

## What changed

The embedded control channel previously resolved
`cider.nrepl/cider-nrepl-handler` during startup. Hot reload only depends on
standard nREPL `eval`, so the server now starts nREPL's default handler instead.
The production, stdio, and test aliases no longer carry `cider-nrepl`.

The boundary test is deliberately behavioral. It makes resolution of the CIDER
handler throw, then requires the embedded nREPL to start. The existing
acceptance test connects to that nREPL, redefines the live MCP handler Var, and
observes the changed behavior through the already-registered tool. The full
focused suite passes at `-Xmx512m`: 182 tests and 1,482 assertions.

The launch default changed from `-J-Xms64m -J-Xmx2g` to
`-J-Xms64m -J-Xmx512m`. The currently running development MCP was not restarted;
the new limit applies on its next controlled start.

The complete repository gate is also green with its MCP phase under that cap:
604 core tests / 5,221 assertions, 182 MCP tests / 1,482 assertions, stdio
three-tool discovery, benchmark harness self-tests, and retained-evidence
verification. `make mcp-reload` then synchronized the live server at CWD
`/Users/genekim/src.local/clj-surgeon`, changed the contract hash from
`8c84890a` to `4dbb3317`, and upserted `edit_clojure` without restarting it.

## Matched measurements

Every row used `bench/profile_mcp_startup.sh`, an ephemeral port, production
HTTP startup, embedded nREPL, HTTP health, `jcmd` heap capture, live tool sync,
forced GC, and an exact process teardown. Runs were sequential.

| Runtime | Xmx | CWD | Ready | Peak RSS | Heap used ready | Heap used after GC | Hot reload |
|---|---:|---|---:|---:|---:|---:|---|
| CIDER control (`0dede86`) | 2,048 MiB | `/private/tmp/clj-surgeon-cider-baseline.Wr8k9Y` | 7.33 s | 1,003.6 MiB | 129.6 MiB | 60.4 MiB | yes |
| CIDER control (`0dede86`) | 1,024 MiB | `/private/tmp/clj-surgeon-cider-baseline.Wr8k9Y` | 8.52 s | 702.2 MiB | 136.8 MiB | 103.0 MiB | yes |
| CIDER control (`0dede86`) | 512 MiB | `/private/tmp/clj-surgeon-cider-baseline.Wr8k9Y` | 8.08 s | 509.6 MiB | 136.7 MiB | 60.5 MiB | yes |
| Plain nREPL, no CIDER | 2,048 MiB | `/Users/genekim/src.local/clj-surgeon` | 5.79 s | 753.8 MiB | 92.6 MiB | 41.1 MiB | yes |
| Plain nREPL, no CIDER | 512 MiB | `/Users/genekim/src.local/clj-surgeon` | 6.99 s | 465.9 MiB | 82.8 MiB | 41.1 MiB | yes |
| Plain nREPL, no CIDER | 384 MiB | `/Users/genekim/src.local/clj-surgeon` | 7.07 s | 422.9 MiB | 103.2 MiB | 41.1 MiB | yes |
| Plain nREPL, no CIDER | 256 MiB | `/Users/genekim/src.local/clj-surgeon` | 10.45 s | 383.5 MiB | 111.4 MiB | 41.0 MiB | yes |

At the same 2,048 MiB cap, removing eager CIDER cuts measured peak RSS by 24.9%
and reaches readiness 1.54 seconds sooner. At the same 512 MiB cap, it cuts peak
RSS by 8.6%, heap used at readiness by 39.4%, and post-GC heap used by 32.0%.
With CIDER already removed, changing the cap from 2,048 MiB to 512 MiB cuts peak
RSS another 38.2%. The complete old-default-to-new-default move cuts measured
peak RSS by 53.6%: 1,003.6 MiB to 465.9 MiB.

The heap cap itself explains most of the peak reduction. CIDER removal explains
the smaller retained live set and faster readiness. This is why the two changes
are separately committed and separately measured.

## Startup storyboard

```text
OLD — 2 GiB reservation, eager CIDER

  Clojure + surgeon namespaces
            |
            v
  require cider.nrepl handler
  orchard/logjam/CIDER middleware graph
            |                 heap ready: 129.6 MiB
            v                 peak RSS:   1,003.6 MiB
  nREPL eval channel -------- hot reload
            |
            v
  MCP schemas + Jetty -------- ready 7.33 s


NEW — 512 MiB reservation, plain control channel

  Clojure + surgeon namespaces
            |
            v
  nREPL default handler ------ hot reload
            |                 heap ready: 82.8 MiB
            v                 peak RSS:   465.9 MiB
  MCP schemas + Jetty -------- ready 6.99 s

  after forced GC: 41.1 MiB live heap
```

This is not yet a long-running workload soak. Ten isolated fresh-agent workloads
did start the MCP under the 512 MiB cap and complete an exact guarded edit, so
the cap now has representative edit traffic in addition to health and reload
proof. It still needs a broader inspect/edit portfolio and an overnight
idle/active soak before claiming that every workload fits. The 256 MiB result
proves margin exists; it is not a recommendation to make 256 MiB the default.

Machine-readable results are in
`docs/observations/evidence/startup-memory/local-20260824-summary.tsv`. Raw local
profiles were retained outside the repository at
`/tmp/clj-surgeon-mcp-startup-profile-20260824`; each result records its CWD.
