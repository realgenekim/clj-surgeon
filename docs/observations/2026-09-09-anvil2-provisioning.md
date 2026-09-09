# anvil2 — provisioning receipt (2026-09-09)

*Written by forge@anvil, from the box. Every number below is a receipt line the script
printed on anvil2, or a timing measured the same hour on both boxes. Lineage: the mayor's
provisioning item inb-94ba79; template `curtaincall-cfp/docs/anvil-history.md` (2026-08-12,
"seats as users") and `docs/anvil-box-sizing.md` ("RAM is the constraint").*

## The box

| | Anvil | anvil2 |
|---|---|---|
| Hetzner type | CPX62 (shared AMD) | **CX53 (shared Intel)** |
| CPU | AMD EPYC-Genoa | **Intel Xeon (Skylake, IBRS, no TSX)** |
| vCPU / RAM | 16 / 30Gi | 16 / 30Gi |
| Disk | 601 G | **301 G** (half) |
| OS | Ubuntu 26.04 LTS | Ubuntu 26.04.1 LTS |
| `/tmp` | tmpfs 16G | tmpfs 16G — **temp goes to `/var/tmp/<seat>`, never `/tmp`** |
| Swap | 64 G | 4 G (this brief), swappiness 10, earlyoom active |

## The artifact

`bin/provision-anvil2.sh` (sha256 `d8af07b77e3d29ab…`) — runs **as root on anvil2**, idempotent,
one `RECEIPT` line per step, steps selectable (`provision-anvil2.sh 3 4`). It consumes a
payload rsynced from Anvil to `/var/tmp/anvil2-payload` (seat pubkey, the seat one-shots,
and mirror clones of two private repos). Nothing in the payload is a secret; the repo
mirrors carry committed objects only (verified: no `secrets/`, `.pem`, `.key` in either tree).

## Receipts (verbatim, final run)

```
RECEIPT step=1 name=base           status=ok   pkgs=ok unattended=enabled earlyoom=active tz=Etc/UTC swap=4G swappiness=10 /tmp_fstype=tmpfs(temp_rule=/var/tmp/<seat>)
RECEIPT step=2 name=seats          status=ok   users=forge sol astra recorder gene new=+forge+sol+astra+recorder+gene group=seats gene_nopasswd=yes keys->[forge gene] recorder_state=/home/recorder/state(700) passwords=none(locked)
RECEIPT step=3 name=toolchains     status=ok   java=21.0.12 clojure=1.12.5.1664 bb=v1.13.219 clj-kondo=v2026.08.04 node=v22.23.2 codex=0.153.3 claude=2.1.265 gh=2.46.0 bbin=yes logins=NONE
RECEIPT step=4 name=forge-content  status=ok   bin_copied=48 records=9104e7d1@records/MCP-main skills=clojure-fast-feedback,linked-intent-testing,sublime-every-day,tighten-the-loop nrepl_eval=yes creds=NONE_COPIED
RECEIPT step=5 name=retention      status=ok   cron=1 rule='run-bg logs >7d, coldstart wt >2d, warn <15% remaining' first_run=... disk_remaining=93%
RECEIPT step=6 name=proof          status=ok   mvr=nrepl/test-alias@9439370 nrepl_up_s=5 port=33647 probe_ms=371 probe_rc=0 cold_gate_s=44 gate_rc=0 gate=[579 tests, 7833 assertions, 0 failures] proof_nrepl_stopped=yes
```

## THE HEADLINE: anvil2 is ~2.5–3× SLOWER PER CORE than Anvil

Same repo (`marvin-voice-remote@9439370`, branch `nrepl/test-alias`), same commit, same
toolchain versions, measured within the same hour, both warm:

| meter | Anvil (EPYC-Genoa, **at load 3.4**) | anvil2 (Xeon Skylake, **idle**) | ratio |
|---|---|---|---|
| single-core bb loop (20M iters) | **780 ms** | **2504 ms** | **3.2× slower** |
| `make nrepl` → `.nrepl-port` | 3 s | 5 s | 1.7× |
| warm nREPL probe (`clj-nrepl-eval`) | 172 ms | 362 ms | 2.1× |
| cold `bin/kaocha` gate (579 tests) | 16–17 s | **41–43 s** | **2.5×** |

Anvil won every row **while carrying three times anvil2's load.** BogoMIPS is identical on
both (4593 vs 4589) and is worthless as a signal — the difference is real per-core
throughput: CX53 is the Intel shared line, Anvil's CPX62 is the AMD shared line.

**What this means for the migration:** anvil2 is not a drop-in replacement for the JVM
workloads. Anvil's 16 s gate becomes 41 s here; a 83 s ccfp gate would be ~3.5 min. anvil2
is fine as a *second* box for wall-clock-insensitive work (watchers, recorder, long
background rounds) but any vs-native wall measurement must **never** mix boxes — a cohort
split across Anvil and anvil2 would manufacture a 2.5× "regression" out of hardware.
If anvil2 is meant to replace Anvil for the hill-climb, it needs to be a CCX/CPX box.

## Second finding: `tighten` hardcodes the box, so its receipts lie on a new box

```
$ tighten status                      # ON ANVIL2
== tighten status ... seat=forge@anvil ...
```

`bin/tighten` line 42: `SEAT="forge@anvil"`, and line 91 emits `:host "anvil-server"` into
the immutable seat binding. Run on anvil2 it wrote a fresh binding epoch
`61acebfd9b41096e` **claiming to be Anvil's seat**. That is the "a receipt must name its
subject" failure class: the binding is content-addressed over bytes that assert the wrong
box, so nothing downstream can tell the two seats apart.

**Ratchet owed:** derive `SEAT` from `$(id -un)@$(hostname -s)` and `:host` from
`hostname -f`; add a witness that runs `tighten binding` under a faked hostname and
asserts the emitted `:host` follows it. Until then, do not trust a tighten receipt's seat
field to name the box it ran on.

`tighten verify-bundle` on anvil2: `BUNDLE-VERIFIED ok=6 stale=1 corrupt=0 missing=0`
(the one stale row is `tighten` itself — manifest `6b5c7ead` vs installed `9a151e90`,
the same drift Anvil shows; nothing corrupt, nothing missing).

## Deliberately NOT done

- **No Codex or Claude login for any seat.** The CLIs are installed system-wide at Anvil's
  exact versions; the interactive logins are Gene's, and minting them is skiff's job.
- **No secrets copied.** Every seat has an empty `~/secrets` at 700. `maven-r`/`maven-w`
  are installed as wrappers and will fail until Gene places the reader/writer JSON.
- **No GitHub push credential.** `gh` is installed but not logged in; `records-push` will
  refuse until `gh auth login` runs as forge. Only `realgenekim/clj-surgeon` is public —
  `claude-skills`, the `marvin-openclaw777` fork, and `marvin-voice-remote` are **private**
  and could not be cloned anonymously; they came over as mirror clones from Anvil with the
  GitHub URLs set as remotes, ready for a logged-in fetch.
- **No Tailscale, no clj-surgeon worktrees, no 7906 Surgeon server, no cold-start harness**
  (it needs specimen repos and a login). Those are the migration plan, parked.
- **No passwords for any account**, on purpose — keys only, and every account is `passwd -l`
  locked. The seat's key reaches `forge` and `gene` only.

## Seat doctrine, as installed

Five seats — `forge`, `sol`, `astra`, `recorder`, `gene` — in group `seats`; `gene` has
passwordless sudo (`/etc/sudoers.d/gene-nopasswd`, `visudo -cf` verified). `recorder` has
`~/state` at 700 and nothing else: it is the independent receipt issuer (Sol's condition 2),
and it holds no repo, no key, and no tool state anyone else can write.

Each seat carries `bin/`, `.local/bin`, `secrets/` (700) and an env block naming the BOX:

```
GIT_AUTHOR_NAME="<seat>-anvil2"  GIT_AUTHOR_EMAIL="<seat>-anvil2@anvil2"
TMPDIR=/var/tmp/<seat>           JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/var/tmp/<seat>"
```

### Third finding: the seat env has to reach THREE different shells, and two of them are silent

The env lives in one file, `~/.anvil2-seat-env`, sourced from three places, because three
different shells enter a seat and no single file is read by all of them:

| entry path | who uses it | file that must carry it |
|---|---|---|
| `su - <seat> -c …` | provisioning, cron | `~/.profile` |
| interactive login | a human at a tmux pane | `~/.bashrc` |
| **`ssh <seat>@anvil2 'cmd'`** | **every agent driving the box remotely** | **line 1 of `~/.bashrc`, ABOVE Ubuntu's non-interactive guard** |

Both misses were caught on the box, and **both times the receipt still said `status=ok`**,
because nothing checked. Appending to `.bashrc` alone left all five seats with an empty
`GIT_AUTHOR_NAME` and no `TMPDIR` under `su -` (Ubuntu's stock `.bashrc` returns at
`case $- in *i*) ;; *) return`). Fixing that left the *ssh command* path still empty — the
one an agent actually uses. An agent running `ssh forge@anvil2 'clojure …'` would have put
its temp on the tmpfs and committed under no author at all.

Verified across all three paths after the fix, and stable under repeated runs (the block
count stays at 2 in `.bashrc` / 1 in `.profile`, and `authorized_keys` stays at 1 line,
after four consecutive step-2 runs):

```
ssh   forge@anvil2: TMPDIR=/var/tmp/forge ident="forge-anvil2 <forge-anvil2@anvil2>"
su -  sol:  TMPDIR=/var/tmp/sol  ident=sol-anvil2      (all five seats correct)
bash -lic:  TMPDIR=/var/tmp/forge ident=forge-anvil2
```

**The general form:** a provisioning receipt that reports what it *wrote* is not evidence of
what a shell will *read*. Every env step now ends in a read-back through the shell the
consumer will actually use.

## Retention (anvil2 has half Anvil's disk)

`~forge/bin/disk-retention`, in forge's crontab at 04:17 UTC: prunes `/var/tmp/forge/run-bg`
logs older than 7 days and `/var/tmp/forge/coldstart/*/wt` worktrees older than 2 days
(`git worktree remove --force` through the owning repo, `rm -rf` only as fallback), then
emits `disk_remaining=<pct>%` and a `WARN=disk_below_15pct` under 15 %. It reports
**headroom, never consumption** (house rule). First run: `disk_remaining=93%`.

## What was excluded from the one-shot copy, and why

48 of Anvil's 78 `~/bin` entries were copied. Excluded:

| excluded | why |
|---|---|
| `*.bak*` (17 files), `__pycache__`, `*.md`, `*.receipt.edn` | superseded backups / not executables |
| `clj-kondo` (54 MB ELF) | replaced by a per-seat symlink to `/usr/local/bin/clj-kondo` (same version) — the paved entrance still resolves |
| `clj-surgeon` | points at `~/.local/share/clj-surgeon/versions/<sha>`, not installed here |
| `rlwrap` | an Anvil-local shim for a missing package |
| `astra-night`, `astra-watch`, `cadence-watch`, `call-watcher`, `check-prompt-plate.sh` | Anvil-seat watchers bound to Anvil's cron, paths and pids |
| `bridge-reply.sh`, `bridge-selftest.sh`, `connector-doctor.sh`, `mvr-logs.sh` | bridge-seat Slack/GCP tooling; needs creds this box must not hold |
| `nrepl-eval.retired-2026-09-08` | retired |

Hard-coded `/home/forge/...` paths inside the copied scripts were **left alone on purpose**:
the seat is also named `forge` on anvil2, so those paths resolve to the same repos. Five
scripts still name Anvil's Surgeon ports (7888/7890/7894/7895/7906): `round-resume`,
`sol-yolo`, `status`, `surgeon-call`, `verb-sentinel`. No server listens on them here;
they will find nothing rather than reach the wrong box, which is the safe failure.

## Idempotency

The whole script was re-run end to end (`provision-anvil2.sh 1 2 3 4 5 6`) after every fix;
the final full run is green on all six steps with `new=none` on the seats and no duplicated
env blocks, cron lines, or authorized_keys entries. Step 6 now stops the proof JVM it
started, bound to the exact pid the job wrote for itself (never a pattern kill), and
reports `proof_nrepl_stopped=yes`; the box is back to 770 Mi used with no residual JVM.

## Surprises

1. **The CPU generation gap** (above) — the single most consequential fact about this box.
2. **`tighten`'s hardcoded seat id** (above) — a receipt that names the wrong box.
3. **`gh` is 2.46.0 here vs 2.97.0 on Anvil**: Ubuntu 26.04 ships 2.46 and `apt install gh`
   succeeded, so the script's github-cli-repo fallback never fired. It works as a git
   credential helper; if parity matters, add the `cli.github.com` apt source.
4. **`/root` is not readable by the seats**, so the first step-4 run silently failed to clone
   from a `/root` payload — the clones "did not exist" rather than "permission denied".
   The payload now lives at `/var/tmp/anvil2-payload`, world-readable.
5. **git's dubious-ownership guard made a receipt read `records=none`** when root ran
   `git rev-parse` in forge's clone. The receipt now asks forge. A receipt computed by the
   wrong identity reports absence, not an error.

---
*Provisioned 2026-09-09T02:07:21Z by forge@anvil against root@anvil2. Script: `bin/provision-anvil2.sh`.*
