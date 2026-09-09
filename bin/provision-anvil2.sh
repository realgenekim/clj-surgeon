#!/usr/bin/env bash
# provision-anvil2.sh — provision anvil2 (Hetzner CX53, Ubuntu 26.04) as a seat box.
#
# RUNS AS ROOT ON ANVIL2.  Written on forge@anvil, shipped there, run there — the
# script is the artifact (mayor's provisioning item inb-94ba79 lineage).
#
# Idempotent: every step re-runnable.  Prints exactly one RECEIPT line per step.
# Usage:   provision-anvil2.sh [step ...]      (default: 1 2 3 4 5;  6 = proof, explicit)
# Payload: /var/tmp/anvil2-payload/  { seat-key.pub, bin/, repos/*.git }  rsynced from Anvil.
#
# Doctrine carried across from Anvil:
#   - temp under /var/tmp/<seat>, NEVER /tmp (/tmp is tmpfs, filled twice on Anvil)
#   - swap + swappiness 10 + earlyoom (Anvil's 2026-08-13 hard-OOM scar; RAM is the constraint)
#   - seats are JOBS not people; no passwords; keys only; author = "<seat>-anvil2"
#   - NO logins are performed for Codex/Claude and NO secrets are copied.
set -uo pipefail

PAYLOAD=${PAYLOAD:-/var/tmp/anvil2-payload}   # world-readable: step 4 clones from it AS the forge user
WORK=${WORK:-/var/tmp/anvil2-provision}
SEATS="forge sol astra recorder gene"
BOX=anvil2

# ---- pinned to what Anvil runs on 2026-09-09 (read there with --version) -------
V_JDK_MAJOR=21                 # Anvil: openjdk 21.0.12 (Ubuntu build, NOT Temurin)
V_CLOJURE=1.12.5.1664
V_BB=1.13.219
V_KONDO=2026.08.04
V_NODE_MAJOR=22                # Anvil: v22.23.2-1nodesource1
V_CODEX=0.153.3
V_CLAUDE=2.1.265
CLAUDE_HOME=/opt/claude-cli    # claude installed once here, symlinked into /usr/local/bin
# -------------------------------------------------------------------------------

export DEBIAN_FRONTEND=noninteractive
mkdir -p "$WORK"
rc(){ printf 'RECEIPT step=%s name=%-16s status=%-4s %s\n' "$1" "$2" "$3" "${4:-}"; }
have(){ command -v "$1" >/dev/null 2>&1; }
asuser(){ local u=$1; shift; su - "$u" -c "$*"; }

########################################################################
step1_base(){
  local det="" st=ok
  apt-get update -qq >>"$WORK/apt.log" 2>&1 || st=warn
  apt-get install -y -qq unattended-upgrades git tmux curl rsync ripgrep jq \
      python3 python3-venv build-essential unzip htop ca-certificates gnupg \
      earlyoom cron >>"$WORK/apt.log" 2>&1 || st=fail

  # unattended-upgrades on
  cat >/etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF
  systemctl enable --now unattended-upgrades >/dev/null 2>&1
  local uu; uu=$(systemctl is-enabled unattended-upgrades 2>/dev/null)

  # earlyoom (Anvil's real OOM protection — kills one JVM, not the box)
  systemctl enable --now earlyoom >/dev/null 2>&1
  local eo; eo=$(systemctl is-active earlyoom 2>/dev/null)

  timedatectl set-timezone Etc/UTC >/dev/null 2>&1
  local tz; tz=$(timedatectl show -p Timezone --value)

  # 4 GB swapfile, swappiness 10
  if ! swapon --show=NAME --noheadings 2>/dev/null | grep -q '^/swapfile$'; then
    rm -f /swapfile
    fallocate -l 4G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=4096 status=none
    chmod 600 /swapfile; mkswap -q /swapfile >/dev/null; swapon /swapfile
  fi
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >>/etc/fstab
  echo 'vm.swappiness=10' >/etc/sysctl.d/99-anvil2-swappiness.conf
  sysctl -q -w vm.swappiness=10
  local sw; sw=$(swapon --show=SIZE --noheadings 2>/dev/null | tr '\n' ' ')

  # scratch root; per-seat dirs are made in step 2
  mkdir -p /var/tmp; chmod 1777 /var/tmp
  local tmpfs; tmpfs=$(findmnt -no FSTYPE /tmp 2>/dev/null || echo none)

  det="pkgs=ok unattended=$uu earlyoom=$eo tz=$tz swap=${sw:-none} swappiness=$(cat /proc/sys/vm/swappiness) /tmp_fstype=${tmpfs:-rootfs}(temp_rule=/var/tmp/<seat>)"
  rc 1 base "$st" "$det"
}

########################################################################
step2_seats(){
  local st=ok made="" key=$PAYLOAD/seat-key.pub
  groupadd -f seats
  for s in $SEATS; do
    if ! id -u "$s" >/dev/null 2>&1; then
      useradd -m -s /bin/bash "$s" && made="$made+$s"
    fi
    usermod -aG seats "$s"
    passwd -l "$s" >/dev/null 2>&1            # NO password, ever — keys only
    install -d -m 700 -o "$s" -g "$s" "/var/tmp/$s"
    install -d -m 755 -o "$s" -g "$s" "/home/$s/bin" "/home/$s/.local/bin"
    install -d -m 700 -o "$s" -g "$s" "/home/$s/secrets"
    # The seat env lives in ONE file, ~/.anvil2-seat-env, sourced from THREE places, because
    # three different shells enter a seat and none of them reads the same file:
    #   .profile  -> `su - <seat> -c ...` (provisioning, cron)
    #   .bashrc   -> interactive shells
    #   TOP of .bashrc, ABOVE Ubuntu's non-interactive guard -> `ssh <seat>@box 'cmd'`
    # Both misses were caught on the box, 2026-09-09: the first run left every seat with an
    # empty GIT_AUTHOR_NAME and no TMPDIR, and both receipts still said status=ok because
    # nothing checked. An agent running `ssh forge@anvil2 'clojure ...'` would have written
    # temp to the tmpfs and committed under the wrong author.
    local envf="/home/$s/.anvil2-seat-env"
    cat >"$envf" <<EOF
# --- anvil2 seat block (provision-anvil2.sh) — do not edit by hand ---
export PATH="\$HOME/.local/bin:\$HOME/bin:\$PATH"
export TMPDIR=/var/tmp/$s TMP=/var/tmp/$s TEMP=/var/tmp/$s
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/var/tmp/$s"
# house rule: author = the SEAT that typed it; the identity string names the BOX.
export GIT_AUTHOR_NAME="$s-$BOX"    GIT_AUTHOR_EMAIL="$s-$BOX@$BOX"
export GIT_COMMITTER_NAME="$s-$BOX" GIT_COMMITTER_EMAIL="$s-$BOX@$BOX"
# --- end anvil2 seat block ---
EOF
    chown "$s:$s" "$envf"; chmod 644 "$envf"
    local src=". \$HOME/.anvil2-seat-env   # anvil2 seat block"
    local bl
    for bl in "/home/$s/.profile" "/home/$s/.bashrc"; do
      touch "$bl"; chown "$s:$s" "$bl"
      grep -qF '.anvil2-seat-env' "$bl" || echo "$src" >>"$bl"
    done
    # ...and at line 1 of .bashrc, above the `case $- in *i*` guard, for `ssh seat@box cmd`
    if ! head -1 "/home/$s/.bashrc" | grep -qF '.anvil2-seat-env'; then
      printf '%s\n' "$src" | cat - "/home/$s/.bashrc" >"/home/$s/.bashrc.new" \
        && mv "/home/$s/.bashrc.new" "/home/$s/.bashrc" && chown "$s:$s" "/home/$s/.bashrc"
    fi
  done

  # gene: passwordless sudo, visudo-verified
  cat >/tmp/gene-nopasswd.$$ <<'EOF'
gene ALL=(ALL) NOPASSWD:ALL
EOF
  local sudook=no
  if visudo -cf /tmp/gene-nopasswd.$$ >/dev/null 2>&1; then
    install -m 440 -o root -g root /tmp/gene-nopasswd.$$ /etc/sudoers.d/gene-nopasswd
    sudook=$(visudo -cf /etc/sudoers.d/gene-nopasswd >/dev/null 2>&1 && echo yes || echo no)
  fi
  rm -f /tmp/gene-nopasswd.$$

  # seat key into forge's AND gene's authorized_keys (dedup)
  local keyed=""
  if [ -f "$key" ]; then
    for s in forge gene; do
      install -d -m 700 -o "$s" -g "$s" "/home/$s/.ssh"
      touch "/home/$s/.ssh/authorized_keys"
      grep -qxFf "$key" "/home/$s/.ssh/authorized_keys" 2>/dev/null || cat "$key" >>"/home/$s/.ssh/authorized_keys"
      chmod 600 "/home/$s/.ssh/authorized_keys"; chown "$s:$s" "/home/$s/.ssh/authorized_keys"
      keyed="$keyed $s"
    done
  else st=warn; fi

  # recorder: the independent receipt issuer — a private state dir and nothing else
  install -d -m 700 -o recorder -g recorder /home/recorder/state

  rc 2 seats "$st" "users=$SEATS new=${made:-none} group=seats gene_nopasswd=$sudook keys->[${keyed# }] recorder_state=/home/recorder/state(700) passwords=none(locked)"
}

########################################################################
step3_toolchains(){
  local st=ok
  # --- JDK (Ubuntu openjdk, matching Anvil's build, not Temurin) ---
  have javac || apt-get install -y -qq "openjdk-${V_JDK_MAJOR}-jdk-headless" >>"$WORK/apt.log" 2>&1 || st=fail

  # --- Node LTS 22 from NodeSource (same origin as Anvil) ---
  if ! have node || [ "$(node --version 2>/dev/null | cut -c2- | cut -d. -f1)" != "$V_NODE_MAJOR" ]; then
    curl -fsSL "https://deb.nodesource.com/setup_${V_NODE_MAJOR}.x" -o "$WORK/nodesource.sh" \
      && bash "$WORK/nodesource.sh" >>"$WORK/apt.log" 2>&1 \
      && apt-get install -y -qq nodejs >>"$WORK/apt.log" 2>&1 || st=fail
  fi

  # --- gh (the git credential helper Anvil uses; NOT logged in here) ---
  have gh || { apt-get install -y -qq gh >>"$WORK/apt.log" 2>&1 || {
      mkdir -p -m 755 /etc/apt/keyrings
      curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
        | tee /etc/apt/keyrings/githubcli-archive-keyring.gpg >/dev/null
      chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg
      echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
        >/etc/apt/sources.list.d/github-cli.list
      apt-get update -qq >>"$WORK/apt.log" 2>&1; apt-get install -y -qq gh >>"$WORK/apt.log" 2>&1; }; }

  # --- Clojure CLI (pinned) ---
  if [ "$(clojure --version 2>/dev/null | awk '{print $4}')" != "$V_CLOJURE" ]; then
    curl -fsSL -o "$WORK/linux-install.sh" "https://download.clojure.org/install/linux-install-${V_CLOJURE}.sh" \
      && chmod +x "$WORK/linux-install.sh" && "$WORK/linux-install.sh" -p /usr/local >>"$WORK/tool.log" 2>&1 || st=fail
  fi

  # --- Babashka (pinned) ---
  if [ "$(bb --version 2>/dev/null | awk '{print $2}')" != "v$V_BB" ]; then
    curl -fsSL -o "$WORK/bb-install" https://raw.githubusercontent.com/babashka/babashka/master/install \
      && chmod +x "$WORK/bb-install" \
      && "$WORK/bb-install" --dir /usr/local/bin --version "$V_BB" >>"$WORK/tool.log" 2>&1 || st=fail
  fi

  # --- clj-kondo (pinned) ---
  if [ "$(clj-kondo --version 2>/dev/null | awk '{print $2}')" != "v$V_KONDO" ]; then
    curl -fsSL -o "$WORK/kondo-install" https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo \
      && chmod +x "$WORK/kondo-install" \
      && "$WORK/kondo-install" --dir /usr/local/bin --version "$V_KONDO" >>"$WORK/tool.log" 2>&1 || st=fail
  fi

  # --- bbin (from payload — same build Anvil carries) ---
  [ -f "$PAYLOAD/bin/bbin" ] && install -m 755 "$PAYLOAD/bin/bbin" /usr/local/bin/bbin

  # --- Codex CLI, system-wide (NOT logged in) ---
  if [ "$(codex --version 2>/dev/null | awk '{print $2}')" != "$V_CODEX" ]; then
    npm install -g --prefix /usr/local "@openai/codex@${V_CODEX}" >>"$WORK/tool.log" 2>&1 || st=fail
  fi

  # --- Claude Code, installed once under /opt, symlinked system-wide (NOT logged in) ---
  if [ "$(claude --version 2>/dev/null | awk '{print $1}')" != "$V_CLAUDE" ]; then
    mkdir -p "$CLAUDE_HOME"
    curl -fsSL https://claude.ai/install.sh -o "$WORK/claude-install.sh" \
      && HOME="$CLAUDE_HOME" bash "$WORK/claude-install.sh" "$V_CLAUDE" >>"$WORK/tool.log" 2>&1
    if [ -x "$CLAUDE_HOME/.local/bin/claude" ]; then
      chmod -R a+rX "$CLAUDE_HOME"
      ln -sfn "$CLAUDE_HOME/.local/bin/claude" /usr/local/bin/claude
    else st=fail; fi
  fi

  # every seat lints through ~/bin/clj-kondo (the paved entrance) -> the one system binary
  for s in $SEATS; do
    ln -sfn /usr/local/bin/clj-kondo "/home/$s/bin/clj-kondo"; chown -h "$s:$s" "/home/$s/bin/clj-kondo"
  done

  rc 3 toolchains "$st" "java=$(java -version 2>&1|head -1|awk -F'\"' '{print $2}') clojure=$(clojure --version 2>/dev/null|awk '{print $4}') bb=$(bb --version 2>/dev/null|awk '{print $2}') clj-kondo=$(clj-kondo --version 2>/dev/null|awk '{print $2}') node=$(node --version 2>/dev/null) codex=$(codex --version 2>/dev/null|awk '{print $2}') claude=$(claude --version 2>/dev/null|awk '{print $1}') gh=$(gh --version 2>/dev/null|head -1|awk '{print $3}') bbin=$([ -x /usr/local/bin/bbin ] && echo yes || echo no) logins=NONE"
}

########################################################################
step4_forge(){
  local st=ok notes=""
  install -d -m 755 -o forge -g forge /home/forge/src /home/forge/opt /home/forge/.claude/skills /home/forge/.local/state

  # --- seat one-shots ---
  local copied=0 skipped=""
  if [ -d "$PAYLOAD/bin" ]; then
    for f in "$PAYLOAD/bin"/*; do
      b=$(basename "$f"); [ -f "$f" ] || continue
      install -m 755 -o forge -g forge "$f" "/home/forge/bin/$b" && copied=$((copied+1))
    done
  fi
  [ -f "$PAYLOAD/bin-excluded.txt" ] && skipped=$(tr '\n' ',' <"$PAYLOAD/bin-excluded.txt")

  # --- repos ---
  # clj-surgeon-records: PUBLIC, cloned anonymously over https from GitHub (real path).
  if [ ! -d /home/forge/src/clj-surgeon-records/.git ]; then
    asuser forge "git clone -q https://github.com/realgenekim/clj-surgeon.git /home/forge/src/clj-surgeon-records" \
      || { st=warn; notes="$notes records_clone=FAILED"; }
  fi
  if [ -d /home/forge/src/clj-surgeon-records/.git ]; then
    asuser forge "cd /home/forge/src/clj-surgeon-records && git fetch -q origin records/MCP-main:records/MCP-main 2>/dev/null; git checkout -q records/MCP-main 2>/dev/null || true"
  fi

  # claude-skills canon + the nrepl worktree.  The GitHub repos are PRIVATE and cannot be
  # cloned anonymously; the payload carries mirror clones (committed objects only — no
  # working-tree secrets).  GitHub remotes are set so a logged-in Gene can fetch/push.
  if [ -d "$PAYLOAD/repos/claude-skills.git" ] && [ ! -d /home/forge/opt/claude-skills/.git ]; then
    asuser forge "git clone -q --origin mirror '$PAYLOAD/repos/claude-skills.git' /home/forge/opt/claude-skills"
    # materialise every mirror branch as a LOCAL branch first — the worktree below needs a
    # real ref, and the GitHub remotes cannot be fetched without Gene's token.
    asuser forge "cd /home/forge/opt/claude-skills && \
      for b in \$(git branch -r --format='%(refname:short)' | sed 's|^mirror/||' | grep -v '^HEAD'); do \
        git show-ref -q --verify \"refs/heads/\$b\" || git branch -q --no-track \"\$b\" \"mirror/\$b\"; done; \
      git remote add fork https://github.com/marvin-openclaw777/claude-skills.git 2>/dev/null; \
      git remote add origin https://github.com/realgenekim/claude-skills.git 2>/dev/null; \
      git checkout -q anvil/codex-sol-yolo 2>/dev/null || true"
  fi
  if [ -d /home/forge/opt/claude-skills/.git ] && [ ! -e /home/forge/src/claude-skills-nrepl/.git ]; then
    asuser forge "cd /home/forge/opt/claude-skills && git worktree add -q /home/forge/src/claude-skills-nrepl anvil/nrepl-test-alias" \
      || { st=warn; notes="$notes skills_worktree=FAILED"; }
  fi

  # marvin-voice-remote (PRIVATE upstream; payload mirror) — the step-6 proof subject
  if [ -d "$PAYLOAD/repos/marvin-voice-remote.git" ] && [ ! -d /home/forge/src/marvin-voice-remote/.git ]; then
    asuser forge "git clone -q --origin mirror -b nrepl/test-alias '$PAYLOAD/repos/marvin-voice-remote.git' /home/forge/src/marvin-voice-remote"
    asuser forge "cd /home/forge/src/marvin-voice-remote && \
      git remote add origin https://github.com/realgenekim/marvin-voice-remote.git 2>/dev/null || true"
  fi

  # --- skills symlinks ---
  for pair in "clojure-fast-feedback:/home/forge/src/claude-skills-nrepl/clojure-fast-feedback" \
              "sublime-every-day:/home/forge/src/claude-skills-nrepl/sublime-every-day" \
              "tighten-the-loop:/home/forge/src/claude-skills-nrepl/tighten-the-loop" \
              "linked-intent-testing:/home/forge/opt/claude-skills/linked-intent-testing"; do
    n=${pair%%:*}; t=${pair#*:}
    [ -d "$t" ] && { ln -sfn "$t" "/home/forge/.claude/skills/$n"; chown -h forge:forge "/home/forge/.claude/skills/$n"; }
  done

  # --- clj-nrepl-eval via bbin, exactly as Anvil has it ---
  if [ ! -x /home/forge/.local/bin/clj-nrepl-eval ] && [ -x /usr/local/bin/bbin ]; then
    asuser forge "bbin install https://github.com/bhauman/clojure-mcp-light.git \
      --git/sha d341c239d4b1faa58ccefaf8bc0b1e2a312e2af4 \
      --as clj-nrepl-eval --main-opts '[\"-m\" \"clojure-mcp-light.nrepl-eval\"]'" >>"$WORK/tool.log" 2>&1 \
      || { st=warn; notes="$notes bbin_nrepl_eval=FAILED"; }
  fi

  chown -R forge:forge /home/forge/src /home/forge/opt /home/forge/.claude /home/forge/bin /home/forge/.local 2>/dev/null
  rc 4 forge-content "$st" "bin_copied=$copied bin_excluded=[${skipped%,}] records=$(asuser forge "git -C /home/forge/src/clj-surgeon-records rev-parse --short HEAD" 2>/dev/null || echo none)@$(asuser forge "git -C /home/forge/src/clj-surgeon-records rev-parse --abbrev-ref HEAD" 2>/dev/null || echo none) skills=$(ls /home/forge/.claude/skills 2>/dev/null|tr '\n' ',') nrepl_eval=$([ -x /home/forge/.local/bin/clj-nrepl-eval ] && echo yes || echo no) creds=NONE_COPIED$notes"
}

########################################################################
step5_retention(){
  local st=ok
  install -d -m 755 -o forge -g forge /home/forge/bin
  cat >/home/forge/bin/disk-retention <<'EOF'
#!/usr/bin/env bash
# disk-retention — anvil2 has HALF Anvil's disk (301G vs 601G).  Prune run-bg logs and
# coldstart worktrees, then emit a seat-receipt-style disk_remaining line (HEADROOM, never
# consumption — house rule: report what is LEFT).  Warns below 15% free.
set -u
R=/var/tmp/forge; LOG=${RETENTION_LOG:-/var/tmp/forge/retention.log}
mkdir -p "$R" "$(dirname "$LOG")"
n_logs=$(find "$R/run-bg" -maxdepth 1 -type f -mtime +7 -print -delete 2>/dev/null | wc -l)
n_wt=0
for wt in "$R"/coldstart/*/wt; do
  [ -d "$wt" ] || continue
  [ -n "$(find "$wt" -maxdepth 0 -mtime +2 2>/dev/null)" ] || continue
  repo=$(git -C "$wt" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)
  if [ -n "$repo" ] && git -C "${repo%/.git}" worktree remove --force "$wt" 2>/dev/null; then
    n_wt=$((n_wt+1))
  elif rm -rf "$wt" 2>/dev/null; then n_wt=$((n_wt+1)); fi
done
free_pct=$(df --output=avail,size / | awk 'NR==2{printf "%d", $1*100/$2}')
warn=""; [ "$free_pct" -lt 15 ] && warn=" WARN=disk_below_15pct"
echo "RECEIPT at=$(date -u +%FT%TZ) name=disk-retention pruned_logs=$n_logs pruned_worktrees=$n_wt disk_remaining=${free_pct}%$warn" | tee -a "$LOG"
EOF
  chmod 755 /home/forge/bin/disk-retention; chown forge:forge /home/forge/bin/disk-retention

  # forge crontab, idempotent (rewrite the managed block)
  local cur new
  cur=$(crontab -u forge -l 2>/dev/null | grep -v 'disk-retention')
  new=$(printf '%s\n# --- anvil2 retention (provision-anvil2.sh): half of Anvil'"'"'s disk ---\n17 4 * * * /home/forge/bin/disk-retention >> /var/tmp/forge/retention.log 2>&1\n' "$cur")
  echo "$new" | crontab -u forge - || st=fail
  install -d -m 700 -o forge -g forge /var/tmp/forge/run-bg
  rc 5 retention "$st" "cron=$(crontab -u forge -l 2>/dev/null | grep -c disk-retention) rule='run-bg logs >7d, coldstart wt >2d, warn <15% remaining' first_run=$(su - forge -c /home/forge/bin/disk-retention 2>&1 | tail -1)"
}

########################################################################
step6_proof(){
  # Mechanical proof of the box: a real Clojure JVM, a warm nREPL, a warm probe, and one
  # COLD test gate, timed.  The cold-start harness is NOT run here — it needs specimen repos
  # and a Codex/Claude login, and no seat is logged in (by design).
  local st=ok
  local out; out=$(su - forge -c 'set -u
    cd /home/forge/src/marvin-voice-remote 2>/dev/null || { echo "mvr=ABSENT"; exit 0; }
    echo "mvr=$(git rev-parse --abbrev-ref HEAD)@$(git rev-parse --short HEAD)"
    rm -f .nrepl-port
    t0=$(date +%s%N)
    nohup setsid bash -c "echo \$\$ >/var/tmp/forge/proof-nrepl.pid; exec make nrepl" \
      </dev/null >/var/tmp/forge/nrepl.log 2>&1 &
    for i in $(seq 1 300); do [ -s .nrepl-port ] && break; sleep 1; done
    t1=$(date +%s%N); port=$(cat .nrepl-port 2>/dev/null)
    echo "nrepl_up_s=$(( (t1-t0)/1000000000 )) port=${port:-NONE}"
    if [ -n "${port:-}" ]; then
      t0=$(date +%s%N); clj-nrepl-eval --port "$port" "(+ 1 1)" >/var/tmp/forge/probe.log 2>&1; rv=$?
      t1=$(date +%s%N); echo "probe_ms=$(( (t1-t0)/1000000 )) probe_rc=$rv probe_out=$(tr -d \"\\n\" </var/tmp/forge/probe.log | cut -c1-40)"
    fi
    t0=$(date +%s%N); timeout 2400 bin/kaocha </dev/null >/var/tmp/forge/gate.log 2>&1; grc=$?
    t1=$(date +%s%N); echo "cold_gate_s=$(( (t1-t0)/1000000000 )) gate_rc=$grc gate=[$(grep -aoE "[0-9]+ tests?, [0-9]+ assertions.*" /var/tmp/forge/gate.log | tail -1)]"
    # never leave the proof JVM behind (each costs ~700 MB — the Anvil 56-JVM scar).
    # Bound to the EXACT pid the job wrote for itself; never a pattern kill.
    pp=$(cat /var/tmp/forge/proof-nrepl.pid 2>/dev/null)
    if [ -n "${pp:-}" ] && kill -0 "$pp" 2>/dev/null; then kill "$pp" 2>/dev/null; sleep 3; fi
    echo "proof_nrepl_stopped=$([ -n "${pp:-}" ] && ! kill -0 "$pp" 2>/dev/null && echo yes || echo no)"' 2>&1 | tr '\n' ' ')
  rc 6 proof "$st" "$out"
}

########################################################################
STEPS=${*:-1 2 3 4 5}
for s in $STEPS; do
  case "$s" in
    1) step1_base;; 2) step2_seats;; 3) step3_toolchains;;
    4) step4_forge;; 5) step5_retention;; 6) step6_proof;;
    *) echo "unknown step $s";;
  esac
done
