# :ls-tree — Directory-wide namespace map

Map every Clojure namespace in a directory tree: ns name, requires, public
forms with arglists. One command, thousands of files, seconds.

## Usage

```bash
# Full scan of a directory tree
clj-surgeon :op :ls-tree :dir .

# Surgical search across many repos (the killer feature)
clj-surgeon :op :ls-tree :dir ~/src.local/ :grep "mail|imap|gmail"

# EDN output for machine consumption
clj-surgeon :op :ls-tree :dir . :format :edn
```

## Aliases

`:ls-tree`, `:tree`, `:map`, `:outline-tree` — all equivalent.

## Options

| Flag | Description |
|---|---|
| `:dir PATH` | Root directory to scan (required) |
| `:grep PATTERN` | Pre-filter: only show projects/files matching pattern. Pipe-separated terms (`"mail\|imap\|gmail"`). Uses ripgrep if available. |
| `:format :edn` | Output as EDN vector instead of compact text |

## How it works

### Project discovery
1. Finds `deps.edn`, `project.clj`, `bb.edn` under the given directory
2. Reads `:paths` (or `:source-paths` for Leiningen) from each build file
3. Scans only those source paths for `.clj/.cljs/.cljc` files
4. Groups output by project

If no build files are found, falls back to recursive scan of all `.clj` files.

### Grep fast path
When `:grep` is specified, the tool skips expensive directory globbing entirely:
1. Runs `rg` (ripgrep) to find matching files in ~0.3s across the whole tree
2. Maps matched files back to their project roots
3. Only parses the matching files with rewrite-clj

This is why `:grep` turns an 87-second full scan into a 3-second surgical search.

### Output format (text, default)
```
── email-fetch (3 files, 15 forms)

email-fetch/src/email/ops.clj  243 lines, 13 forms
  ns: email.ops
  requires: [clojure-mail.core :as m] [clojure-mail.message :as msg]
  10-11: def secrets
  25-27: defn count-messages [folder]
  31-47: defn parse! [msg]
  163-169: defn delete-message [folder message]

── total: 3 files, 15 forms
```

### Output format (EDN, `:format :edn`)
```clojure
[{:ns email.ops
  :file "email-fetch/src/email/ops.clj"
  :lines 243
  :form-count 13
  :requires ["[clojure-mail.core :as m]" "[clojure-mail.message :as msg]"]
  :forms [{:type defn :name count-messages :args "[folder]" :line 25 :end-line 27} ...]}]
```

## Performance

Measured on a MacBook Pro M1, across ~/src.local/ (350 projects, 4,444 .clj files):

| Scenario | Time | Notes |
|---|---|---|
| Single repo (62 files) | **0.25s** | Instant |
| 10 repos (106 files) | **1.3s** | Fast |
| `:grep` across 4,444 files | **3.4s** | rg finds matches in 0.3s, then only matching files are parsed |
| Full scan, no grep (4,444 files) | **~87s** | Parses every file — use `:grep` for large trees |

### Why `:grep` is fast
The bottleneck without `:grep` is filesystem globbing + rewrite-clj parsing of every file.
With `:grep`, ripgrep searches the entire tree in ~0.3 seconds (it's written in Rust,
respects `.gitignore`, and uses memory-mapped I/O). Only files that match get parsed.

### ripgrep requirement
`:grep` uses `rg` (ripgrep) if available, falling back to system `grep` (much slower).
Install: `brew install ripgrep` or `apt install ripgrep`.

## Use cases

### "Which repo does X?" (cross-repo discovery)
```bash
clj-surgeon :op :ls-tree :dir ~/src.local/ :grep "postgres|jdbc|next.jdbc"
# → finds every repo that talks to Postgres, with full API surface
```

### "What's in this codebase?" (onboarding)
```bash
clj-surgeon :op :ls-tree :dir .
# → complete namespace map with requires and form signatures
```

### "What depends on what?" (dependency mapping)
```bash
clj-surgeon :op :ls-tree :dir . :grep "my.library"
# → every file that requires or mentions a specific namespace
```

### Agent-friendly codebase indexing
```bash
clj-surgeon :op :ls-tree :dir . :format :edn
# → structured data an LLM can reason over
```
