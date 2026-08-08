# Captain's Log: The namespace chose the provider

**Date:** 2026-08-08

## The decision

A caller should name the Clojure Var it needs, not guess which shared language
server owns it. `workspace_root` identifies the caller's project. The fully
qualified namespace identifies the code. The tool must join those coordinates.

## What failed first

The first correct sibling-workspace implementation searched every configured
Clojure workspace. A real server2 request for
`reddit.mongodb.mongodb/start` queried six LSP processes. It found the right
definition, exact source SHA-256, and 81 references, but took 27.371 seconds.
Correctness without routing was bounded boofarama.

## The smaller contract

`clj-surgeon up` now publishes each workspace's deterministic confined source
roots. cclsp converts `reddit.mongodb.mongodb` to
`reddit/mongodb/mongodb.{clj,cljc,cljs}`, checks those source roots, and asks
only the workspaces that contain a candidate file. The semantic result retains
both coordinates:

```text
requested workspace      server2
fully qualified Var      reddit.mongodb.mongodb/start
candidate workspace      reddit-scraper
authoritative workspace  reddit-scraper
```

The LSP still proves definitions and references. The filesystem shortlist only
chooses which LSP may answer. Incomplete metadata falls back to every configured
Clojure workspace. Zero, ambiguity, errors, and timeouts remain typed evidence.

## Live result

| Route | Workspaces queried | Wall time | Definition SHA | References |
|---|---:|---:|---|---:|
| All configured roots | 6 | 27.371 s | `ebf3e2...ab70` | 81 |
| Source-root shortlist, first run | 1 | 9.237 s | `ebf3e2...ab70` | 81 |
| Source-root shortlist, warm | 1 | 0.210 s | `ebf3e2...ab70` | 81 |

The cold route improved by 66%. The warm route was about 130 times faster. More
important, the result now explains why one provider was queried.

## Dogfood finding

Sequential onboarding was correct and preserved all six workspace blocks. Two
simultaneous onboarding commands exposed a lost-update race in the shared JSON
configuration. That is separate from semantic routing and is tracked as
`clj-surgeon-5ss`. A successful `up` command must eventually mean both
"service ready" and "my exact workspace metadata survived concurrent writers."

## Contract cleanup completed beside the routing work

Direct `apply_clojure_changes` requests previously published `verify` even
though the runtime validator refused it. The schema now reserves `verify` for
the prepared-basis route. Direct requests accept only `changes` and `expect`.
Two contract tests compare the published branch with the runtime path, and a
live direct request containing `verify` returned `unknown-fields`, the exact
allowed fields, and `source_unchanged=true`. This closes `clj-surgeon-pgu`
without weakening verification: direct transactions still parse and read back
every changed file; prepared transactions own the fast/full verification
profiles.

## Bottom line

The join is now explicit:

```text
caller workspace + fully qualified Var + source-root index
    -> one candidate provider
    -> one LSP proof
    -> one exact-source transaction
```

The caller describes intent in Clojure coordinates. The tool owns process
selection.
