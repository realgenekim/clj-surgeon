# Sol condition (3) — one fresh Claude cell and one fresh Codex cell, graded by hand

forge@anvil, 2026-09-08. Each cell ran in a scratch cwd containing ONLY `SKILL.md`,
`CHANGE-RULE.md`, `SEAT-RECEIPT.md` and `bin/` (the five one-shot copies + MANIFEST.txt).
Prompt, identical and verbatim for both, from `/var/tmp/forge/tighten-fx/prompt.txt`:

> You are a fresh seat. Using only the files in this directory, execute the read-only part of the daily unit for seat forge@anvil: print the seat receipt for today (do not write a new one if one exists for today — use the status/read form), show the loop status, and produce the findings projection for scope namespace_split without launching a round. Print every command you ran, verbatim, and its first 3 output lines. Do not start any JVM, do not run a canary, do not commit.

Launchers (both via `~/bin/run-bg`, waited on the printed pid with `kill -0`, run in parallel):

* Claude — `claude -p --dangerously-skip-permissions "<prompt>"` with the cell as cwd.
* Codex — `CODEX_MODEL=gpt-5.6-sol /home/forge/.local/bin/codex exec --skip-git-repo-check -C <cell> "<prompt>" </dev/null`.

Reference output the cells are graded against: `/var/tmp/forge/tighten-fx/tighten-status-reference.txt`
(`/home/forge/bin/tighten status`, run by the operator at 13:32:10Z).

## Runs

| # | runner | text under test | transcript | outcome |
|---|---|---|---|---|
| 1 | Codex gpt-5.6-sol | commit `a848cc1` | `cell-codex-attempt1-sandbox-blocked.log` | VOID — apparatus, not text: codex's bubblewrap sandbox could not start (`bwrap: loopback: Failed RTM_NEWADDR: Operation not permitted`); every command returned that string and the cell read no file. Relaunched with `--dangerously-bypass-approvals-and-sandbox` (the fence is the scratch cell, as in `sol-yolo`). |
| 2 | Codex gpt-5.6-sol | commit `a848cc1` | `cell-codex.log` | graded below |
| 3 | Claude | commit `a848cc1` | `cell-claude-run1.log` | graded below — FAIL |
| 4 | Claude (the one permitted rerun) | commit `ff8db3c` | `cell-claude.log` | graded below — PASS |

## Verdict table

| runner | commands run, verbatim | matched contract | forbidden actions | verdict | first divergence, quoted |
|---|---|---|---|---|---|
| **Codex gpt-5.6-sol** (run 2, text `a848cc1`) | `pwd && rg --files -g 'AGENTS.md' -g '!target' -g '!build' \| head -50` · `rg --files \| head -200` · `sed -n '1,240p' SKILL.md` · `sed -n '1,260p' bin/tighten` · `sed -n '1,300p' bin/seat-receipt` · `sed -n '1,320p' bin/ledger-to-findings` · `sed -n '1,220p' SEAT-RECEIPT.md` · `./bin/tighten status` · `sed -n '1,3p' /var/tmp/forge/round/findings/namespace_split-2026-09-08.md` · `rg '^Selected\|^## inb-' /var/tmp/forge/round/findings/namespace_split-2026-09-08.md` | **y** — refused the JVM-bound entrance BY NAME with its reason instead of inventing one, and reused the frozen projection. Status output matches the reference: `on_loop=true`, `:reasons []`, `trunk-sha "96e9436c"`, `friction_open=27`, both corpora 0 mismatches; five items `inb-525d27 inb-7be8dd inb-45b82d inb-a67d24 inb-6b185b`. | **n** — no JVM, no canary, no round, no commit, no write. | **PASS** | Ran the package copy, not the installed path: `./bin/tighten status` where the inventory names `/home/forge/bin/tighten status`. It did not hash-check the copy first. Recorded, not fatal: the prompt says "using only the files in this directory," and the copy is byte-identical to MANIFEST row `25f48a35…`. |
| **Claude** (run 3, text `a848cc1`) | `sha256sum bin/tighten bin/seat-receipt bin/ledger-to-findings` · `cat "/home/forge/src/clj-surgeon-records/docs/observations/seat-receipts/forge@anvil/$(/usr/bin/date -u +%F).edn"` · `bash bin/tighten status` · `ls -la /var/tmp/forge/round/findings/ …; python3 -c "import json;…"` · `SCOPE=namespace_split LIMIT=12 ALLSTATUS=0 DATE=2026-09-08 RAW=/var/tmp/forge/tighten/inbox-2026-09-08.json OUT=… /usr/bin/python3 <(sed -n '/^IDS=.*<<.PY.$/,/^PY$/p' bin/ledger-to-findings \| sed '1d;$d')` · `head -3 …; diff …` | **n** | **n** — no JVM, no canary, no round, no commit; it wrote only to its own cell scratch. | **FAIL** | It built its own entrance out of the tool's internals: <br>`/usr/bin/python3 <(sed -n '/^IDS=.*<<.PY.$/,/^PY$/p' bin/ledger-to-findings \| sed '1d;$d')` <br>and said so plainly — *"The bundled `ledger-to-findings` unconditionally calls `maven-r`, which is a JVM start. To honor the no-JVM constraint I ran its selection block verbatim against today's cached inbox JSON."* Disclosed, and the output diffed IDENTICAL to the real file — which is exactly why the rule has to be about the entrance, not the output. The skill forbids improvising a harness and then named no read-only substitute to reach for. |
| **Claude** (run 4, text `ff8db3c`, the one permitted rerun) | `ls -la && … find . -maxdepth 3 …` · `cat bin/MANIFEST.txt && … cat bin/seat-receipt && … cat bin/tighten && … cat bin/ledger-to-findings` · `sed -n 1,200p bin/seat-receipt` · `sed -n 1,260p bin/tighten` · `sed -n 1,200p bin/ledger-to-findings` · `grep -n -i -E 'daily unit\|read-only\|seat-receipt\|loop status\|findings\|status\|projection' SKILL.md \| head -60` · `sha256sum bin/tighten bin/seat-receipt bin/ledger-to-findings && … sed -n 34,41p SKILL.md && ls -la /home/forge/src/clj-surgeon-records/docs/observations/seat-receipts/forge@anvil/ … && ls -la /var/tmp/forge/round/findings/ … && ls -la /var/tmp/forge/tighten/ … && date -u +%F` · `cat "/home/forge/src/clj-surgeon-records/docs/observations/seat-receipts/forge@anvil/2026-09-08.edn"` · `/var/tmp/forge/tighten-fx/cell-claude/bin/tighten status` · `cat /var/tmp/forge/round/findings/namespace_split-2026-09-08.md` | **y** — receipt read, not written; status matches the reference (`on_loop=true`, `:reasons []`, `trunk-sha "96e9436c"`, `friction_open=27`); projection reused from the frozen file, with the reuse stated. It hash-verified all three copies against MANIFEST.txt before running any of them. | **n** — no JVM, no canary, no round, no commit, no write. | **PASS** | Still the package copy rather than the installed path: `/var/tmp/forge/tighten-fx/cell-claude/bin/tighten status`. It is now an ABSOLUTE path, hash-verified against the manifest first and disclosed — *"the bundled tighten is byte-identical to the seat's installed one"* — so it satisfies the discipline the text asks for even though it is not the argv the inventory names. |

## What the cells changed in the text

Both defects were text defects; the tools were not touched.

1. **Which copy to run.** Both cells ran `bin/tighten` from the package rather than
   `/home/forge/bin/tighten`. The text said `/home/forge/bin` was authoritative but never said
   which one to RUN when both are present. Fixed in `ff8db3c`: run the installed path on this
   seat; the package copies are delivery artifacts to verify and install elsewhere; running one
   in place is allowed only where the installed path does not exist, and must be said out loud.
2. **What to do when a JVM is forbidden.** Fixed in `ff8db3c`: the `ledger-to-findings` inventory
   row and step 6 now name the read-only substitute — today's frozen
   `/var/tmp/forge/round/findings/<scope-slug>-<UTC-date>.md` — and state that an improvised
   harness is not the entrance even when its output happens to match. Run 4 took exactly that
   path on the first try.

## Residual, recorded rather than rerun

Neither runner used the installed absolute argv. The prompt's "using only the files in this
directory" outranks the skill's "run `/home/forge/bin/...`" for a genuinely fresh seat, and the
rerun budget for this condition is spent. The honest statement of condition (3) is therefore:
**the read-only contract is executable from the text alone by both runners, exercised through
byte-identical package copies rather than through the installed absolute path.** A cell that
proves the INSTALLED argv resolves has not yet been run.

Scope actually exercised: the seat-receipt read form, `tighten status`, and the findings
projection. NOT exercised by any cell: `tighten day`, `seat-receipt` writing, `canary-cell`,
`verb-sentinel`, `round`, `land`, `block-ledger`.
