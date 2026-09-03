# Anvil seat — wishlist for the mayor (2026-09-03T03:25Z); Gene drives from here

Gene: *"get all of swipl installed; get anvil environment up to snuff! Get mayor channel up -- tell me
what I need to do from mayor"* / *"Make wishlist for mayor; and I'll drive from there."*

## Done tonight without sudo (no action needed)

- **swipl 10.0.0 with plunit**: user-local via micromamba (`~/opt/mamba/envs/swipl`, linked at
  `~/.local/bin/swipl`); `use_module(library(plunit))` loads. `make mcp-test`'s Prolog oracle can run here now
  (proof run queued). No system package needed.
- `~/opt/claude-skills` cloned (gh access works); `~/.claude/CLAUDE.md` created with the Anvil seat header and
  the two doctrine imports pointed at `~/opt/claude-skills` (both resolve).
- `~/src/the-gene-maven` cloned (CLI present; creds missing, below).
- Connector deps installed (`npm ci` in `channel-connector`, 91 packages); node + npm present.
- `.cpcache/` ignored on clj-surgeon main; the seat's `.mcp.json` repointed locally from 7888 to 7906
  (uncommitted, seat-local) so a restarted session never talks to another seat's production Surgeon.

## Wishlist for the mayor, in priority order

1. **Phone channel.** Deliver `~/secrets/marvin-channel.env` for this seat (`BASE_URL`, `CHANNEL_TOKEN`,
   `SEAT=bridge`, `ALLOWLIST=gene`), 600 in the existing 700 `~/secrets`. **First STOP the Buster connector
   for seat bridge** — two connectors on one seat split the drain (memory `duplicate-connector-splits-drain`).
   Then Gene restarts this session (command in the last section).
2. **codex auth** (Sol): the skiff owns login minting (`codex login` device flow or a copied `~/.codex/auth.json`
   minted for this seat). Until then every review runs on Opus.
3. **maven creds**: the reader/writer cred files the `maven-r`/`maven-w` wrappers expect (names are in the
   wrappers inside the Buster tarball `~/src/seat-move/bridge-seat-20260903T0104Z.tar.gz`), plus the maven
   uberjar (`the-gene-maven/bin/install-maven-uberjar.sh` if it needs a token). This unblocks the inbox: the
   twelve prosecution items, inb-3a9818, and every "closes on merge" note.
4. **`~/bin` scripts + `clj-nrepl-eval`**: from the same tarball (`bin/bridge-reply.sh`, `check-prompt-plate.sh`,
   `mvr-logs.sh`, `maven-r`, `maven-w`, `connector-doctor.sh`, `bridge-selftest.sh`); `clj-nrepl-eval` is
   whatever Buster had on PATH (not in the tarball) — copy it; set `XDG_RUNTIME_DIR=/run/user/$(id -u forge)`
   in the seat's profile.
5. **Crons that follow the seat**: repo-watchstander (`kiloclaw/bin/repo-watchstander.sh`, every 2 h) and
   check-prompt-plate (hourly) — install here AND remove on Buster (duplicates otherwise); I can write the
   crontab lines myself once the scripts and creds exist.
6. **Port table**: reserve 7906–7910 for forge on Anvil wherever the box's port allocation is kept
   (7888–7895 are all held by other seats).
7. **GCP service accounts** (`bridge-cos-sa.json`, `bridge-deployer.json`, `flight-recorder-writer.json`) —
   only needed for marvin-voice-remote deploys/logs and the funnel; not for the Surgeon program; later.
8. **beads**: bd has no database for either repo on this seat and shared-state `bd init` is yours; either
   mirror the prosecution items as beads or keep them in the inbox — say which.

## What Gene does (after item 1 lands)

From this box, as forge:
```
cd ~/src/marvin-voice-remote/channel-connector && set -a && . ~/secrets/marvin-channel.env && set +a && \
claude --dangerously-skip-permissions --dangerously-load-development-channels server:marvin-channel \
  --resume b623492c-458d-4156-a14d-a041f5a37e7c
```
(the Makefile target `start-bridge-cos-opus-with-dictation` does the same but pins `--model opus` and tries
to start a Surgeon on 7888 — do not use it on Anvil). The resumed session then has the reply tool and the
clj-surgeon MCP on 7906; I start that server myself from a worktree with `clojure -X:clj-surgeon/mcp :port 7906`.
