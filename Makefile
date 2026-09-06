CLJ_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
CLI_DEST ?= $(HOME)/bin/clj-surgeon
CODEX_HOME ?= $(HOME)/.codex
CLAUDE_HOME ?= $(HOME)/.claude
INSTALL_ROOT ?= $(HOME)/.local/share/clj-surgeon
CONTROL_PLANE_ROOT_FILE ?= $(INSTALL_ROOT)/control-plane-root
SKILL_SOURCE := $(CLJ_SURGEON_HOME)skills/clj-surgeon
AGENT_ROUTING_SOURCE := $(CLJ_SURGEON_HOME)resources/clj-surgeon-agent-routing.md
CLJ_KONDO_ADMISSION_SOURCE := $(CLJ_SURGEON_HOME)resources/clj-kondo-admission.py
CLJ_KONDO_ADMISSION_DEST ?= $(HOME)/bin/clj-kondo-admission
CLJ_KONDO_SHIM_DEST ?= $(HOME)/bin/clj-kondo
CODEX_SKILL_DEST := $(CODEX_HOME)/skills/clj-surgeon
CLAUDE_SKILL_DEST := $(CLAUDE_HOME)/skills/clj-surgeon
CODEX_GLOBAL_INSTRUCTIONS ?= $(CODEX_HOME)/AGENTS.md
CLAUDE_GLOBAL_INSTRUCTIONS ?= $(CLAUDE_HOME)/CLAUDE.md
SOURCE_COMMIT := $(shell git -C "$(CLJ_SURGEON_HOME)" rev-parse HEAD 2>/dev/null || printf unknown)
CLI_SOURCE_HASH := $(shell cd "$(CLJ_SURGEON_HOME)" && { find src -type f -print; printf '%s\n' bb.edn deps.edn; } | LC_ALL=C sort | while IFS= read -r file; do shasum -a 256 "$$file"; done | shasum -a 256 | awk '{print $$1}')
SKILL_SOURCE_HASH := $(shell cd "$(SKILL_SOURCE)" && find . -type f -print | LC_ALL=C sort | while IFS= read -r file; do shasum -a 256 "$$file"; done | shasum -a 256 | awk '{print $$1}')
VERSION_ROOT := $(INSTALL_ROOT)/versions/$(SOURCE_COMMIT)
CLI_PACKAGE := $(VERSION_ROOT)/cli-$(CLI_SOURCE_HASH)
SKILL_PACKAGE := $(VERSION_ROOT)/skill-$(SKILL_SOURCE_HASH)

MCP_STATE_DIR ?= $(HOME)/.local/state/clj-surgeon/mcp
MCP_PORT ?= 7888
MCP_URL ?= http://127.0.0.1:$(MCP_PORT)/mcp
MCP_PROJECT_DIR ?= $(CLJ_SURGEON_HOME)
MCP_PID_FILE := $(MCP_STATE_DIR)/server.pid
MCP_READY_FILE := $(MCP_STATE_DIR)/ready.edn
MCP_LOG_FILE := $(MCP_STATE_DIR)/server.log
MCP_LAUNCH_LABEL ?= com.realgenekim.clj-surgeon-mcp
MCP_START_ATTEMPTS ?= 120
MCP_STOP_ATTEMPTS ?= 100
MCP_JAVA_HOME ?= $(JAVA_HOME)
MCP_JAVA_CMD ?= $(if $(MCP_JAVA_HOME),$(MCP_JAVA_HOME)/bin/java,$(shell command -v java 2>/dev/null))
MCP_JAVA_OPTS ?= -J-Xms64m -J-Xmx512m
# Scratch root for self-tests that need a real directory. NEVER /tmp: it is a
# RAM-backed tmpfs on this seat, and 82,210 leaked fixture directories took it
# to 96% of its inodes (inb-9483a4). A RAM-backed TMPDIR is REDIRECTED here at
# the Make layer, not merely refused later by the Clojure layer -- these
# recipes hand the value straight to shell harnesses that never reach Clojure.
# @spec MCP-OP-TMPHYG-010
# @spec MCP-OP-TMPHYG-012
SELF_TEST_TMP ?= $(if $(filter /tmp /tmp/% /dev/shm /dev/shm/%,$(TMPDIR)),/var/tmp,$(or $(TMPDIR),/var/tmp))
MCP_DEV_PORT ?= 7889
MCP_DEV_STATE_DIR ?= $(HOME)/.local/state/clj-surgeon/dev-$(MCP_DEV_PORT)
MCP_DEV_URL ?= http://127.0.0.1:$(MCP_DEV_PORT)/mcp
MCP_DEV_PID_FILE := $(MCP_DEV_STATE_DIR)/server.pid
MCP_DEV_READY_FILE := $(MCP_DEV_STATE_DIR)/ready.edn
MCP_DEV_LOG_FILE := $(MCP_DEV_STATE_DIR)/server.log
CLOJURE_BIN ?= $(shell command -v clojure)
CLOJURE_LSP_BIN ?= $(shell command -v clojure-lsp)
CCLSP_HOME ?= $(abspath $(CLJ_SURGEON_HOME)../cclsp-structural-results)
CCLSP_PORT ?= 7890
CCLSP_URL ?= http://127.0.0.1:$(CCLSP_PORT)/mcp
CCLSP_CONFIG ?= $(HOME)/.local/state/clj-surgeon/cclsp.json
CCLSP_STATE_DIR ?= $(HOME)/.local/state/clj-surgeon/cclsp
CCLSP_LOG_FILE := $(CCLSP_STATE_DIR)/server.log
CCLSP_LAUNCH_LABEL ?= com.realgenekim.cclsp-clj-surgeon
CCLSP_HEALTH_ATTEMPTS ?= 20
CCLSP_HEALTH_INTERVAL ?= 0.25
WORKSPACE ?=

.PHONY: repository-hygiene repository-hygiene-self-test test test-full test-fast test-integration test-battery battery-fresh landing-gate test-bb suite-concurrency-battery analyzer-contract-test analyzer-contract-target-self-test runtests mcp-test mcp-operation-oracle mcp-smoke mcp-serve mcp-serve-benchmark mcp-reload mcp-dev-start mcp-dev-stop mcp-dev-status mcp-dev-reload mcp-dev-register mcp-heap-config-self-test clj-kondo-admission-path-self-test admit-analyzer-memory-self-test admit-transaction-recovery-battery cclsp-client-audit cclsp-client-audit-self-test cclsp-start cclsp-start-self-test cclsp-stop cclsp-status workspace-mcp-start workspace-mcp-stop workspace-mcp-status workspace-mcp-onboard workspace-mcp-install-codex install-mcp-codex-dev uninstall-mcp-codex-dev outline help install install-cli install-clj-kondo-admission install-codex-skill install-claude-skill install-agent-routing check-agent-routing check-routing-parity prepare-cli-package prepare-skill-package install-dev install-dev-cli install-dev-codex-skill install-dev-claude-skill sync-clj-surgeon-skill check-clj-surgeon-skill-mirrors nrepl study-agent-usage study-agent-events study-agent-timeline study-agent-read-chains study-agent-usage-self-test benchmark-clean-codex benchmark-edit-portfolio benchmark-edit-portfolio-self-test benchmark-anvil-compiled-edit-canary benchmark-anvil-public-cfp-cleanup benchmark-anvil-format-extraction benchmark-anvil-portfolio-pair benchmark-anvil-portfolio-pair-self-test benchmark-inspect-mcp benchmark-inspect-mcp-self-test benchmark-codex-skill benchmark-claude-skill benchmark-agent-skills benchmark-codex-skill-self-test benchmark-claude-skill-self-test benchmark-agent-skills-self-test clj-surgeon-skill-self-test performance-regression-sentinel-test worktree-lifecycle-test worktree-lifecycle-recovery-test worktree-audit handoff-worktree finish-worktree retain-benchmark-result verify-benchmark-retention benchmark-retention-self-test verify-benchmark-evidence census-battery memory-battery memory-battery-generate memory-battery-reference memory-battery-self-test memory-red memory-red-kernel anvil-arms-self-test txn-kernel-warning-check fanout-selftests tmp-leak-ratchet-self-test

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test                      LANDING GATE (default): battery-fresh receipt + mcp-test + test-bb + hygiene"
	@echo "  make test-full                 Run all tests: analyzer, recovery battery, mcp-test, test-battery, smoke, memory battery, bench tail (CI/nightly)"
	@echo "  make test-fast                 JVM FAST lane (no child process, no port, no network)"
	@echo "  make test-integration          JVM INTEGRATION lane (ephemeral ports, in-process servers)"
	@echo "  make test-battery              JVM BATTERY lane (cold child JVMs; minutes-scale)"
	@echo "  make battery-fresh             refuse if the newest battery receipt is stale"
	@echo "  make landing-gate              THE landing gate ~/bin/land runs (battery-fresh + mcp-test + test-bb + hygiene)"
	@echo "  make test-bb                   babashka lane (was: make test-fast, renamed 2026-09-04)"
	@echo "  make anvil-arms-self-test      PF-5 smoke for the E3/E6 arm apparatus (fake driver)"
	@echo "  make analyzer-contract-test    Run the serialized real-analyzer contracts"
	@echo "  make mcp-test                  Run focused JVM MCP contract and hot-reload tests"
	@echo "  make mcp-smoke                 Verify initialize, five-tool discovery, and refusal over stdio"
	@echo "  make mcp-serve                 Start persistent HTTP MCP with full local telemetry and nREPL"
	@echo "  make mcp-serve-benchmark       Start persistent HTTP MCP without nREPL"
	@echo "  make mcp-reload                Reload live Clojure and publish changed tool schemas"
	@echo "  make mcp-dev-start             Start this worktree's MCP on MCP_DEV_PORT (default 7889)"
	@echo "  make mcp-dev-reload            Reload this worktree's dev MCP handlers and schemas"
	@echo "  make mcp-dev-stop              Stop only this worktree's dev MCP"
	@echo "  make mcp-dev-status            Check this worktree's dev MCP"
	@echo "  make mcp-dev-register          Register clj-surgeon-dev with Codex and .mcp.json"
	@echo "  make cclsp-start               Start branch-live cclsp + clojure-lsp provider"
	@echo "  make cclsp-client-audit        Refuse direct repo cclsp registrations and launchers"
	@echo "  make install-mcp-codex-dev     Install branch-live tools, start MCP, and register it with Codex"
	@echo "  make mcp-status                Check both hot MCPs, nREPL, and Codex registration"
	@echo "  make workspace-mcp-onboard WORKSPACE=/repo  Compatibility alias for clj-surgeon up"
	@echo "  make workspace-mcp-status WORKSPACE=/repo   Verify the shared stack and local config"
	@echo "  make uninstall-mcp-codex-dev   Remove Codex registration and stop the local MCP"
	@echo "  make install                   Stable copied CLI, both skills, and global routing instructions"
	@echo "  make install-with-analyzer     Stable install plus the opt-in clj-kondo admission shim"
	@echo "  make install-cli               Install only the stable copied CLI"
	@echo "  make install-clj-kondo-admission Install the box-wide analyzer gate"
	@echo "  make install-codex-skill       Install only the stable copied Codex skill"
	@echo "  make install-claude-skill      Install only the stable copied Claude skill"
	@echo "  make install-agent-routing     Install compact routing into Codex and Claude globals"
	@echo "  make check-agent-routing       Verify routing-table parity, then both global routing blocks"
	@echo "  make check-routing-parity      Assert every rendered routing table matches the canonical section"
	@echo "  make sync-clj-surgeon-skill    Regenerate Claude/root mirrors from the canonical skill"
	@echo "  make install-dev               Branch-live CLI and skill links (development only)"
	@echo "  make nrepl                     Start bb nREPL"
	@echo "  make study-agent-usage         Join agent routes with Surgeon, cclsp, and LSP telemetry"
	@echo "  make study-agent-events        Count the box-wide MCP call ledger (~/.clj-surgeon/events.jsonl)"
	@echo "  make study-agent-timeline      Render model/tool/gap clocks from RECEIPT=/path/to/receipt.json"
	@echo "  make study-agent-read-chains   Rank repeated Surgeon reads from RECEIPT=/path/to/receipt.json"
	@echo "  make study-agent-usage-self-test Test the bounded cross-agent history collector"
	@echo "  make benchmark-clean-codex     Run the 32-session clean Codex benchmark"
	@echo "  make benchmark-harness-self-test Test benchmark isolation without model calls"
	@echo "  make benchmark-edit-portfolio  Compare representative edits across microscope/current/native"
	@echo "  make benchmark-edit-portfolio-self-test Verify edit capsules and harness without model calls"
	@echo "  make benchmark-anvil-compiled-edit-canary RESULT_DIR=/abs/path LATIN_ROW=1 [REPLICATES=1] Compare transform/edit/native"
	@echo "  make benchmark-anvil-public-cfp-cleanup RESULT_DIR=/abs/path ORDER=compact-first [REPLICATES=1] Compare compact/native extraction cleanup"
	@echo "  make benchmark-anvil-format-extraction RESULT_DIR=/abs/path ORDER=mcp-first [REPLICATES=1] Compare two-call MCP/native extraction"
	@echo "  make benchmark-anvil-portfolio-pair RESULT_DIR=/abs/path TASK=decision-batch-edit ORDER=compact-first [REPLICATES=1] Compare any frozen capsule"
	@echo "  make benchmark-inspect-mcp     Compare persistent inspect, CLI, and native reads"
	@echo "  make benchmark-inspect-mcp-self-test Verify the inspect harness without model calls"
	@echo "  make benchmark-codex-skill     Run the bounded 2-session Codex skill battery"
	@echo "  make benchmark-claude-skill    Run the bounded 4-session Fable/Opus skill battery"
	@echo "  make benchmark-agent-skills    Run both bounded clean-agent skill batteries"
	@echo "  make benchmark-agent-skills-self-test Test both skill harnesses without model calls"
	@echo "  make clj-surgeon-skill-self-test Verify compact routing contract and mirror"
	@echo "  make census-battery           Run the COMMITTED relation-census review battery and print its per-witness composition"
	@echo "  make memory-battery           Measure tree-scale heap at N=100/1k/10k in one bounded JVM (minutes; not in make test)"
	@echo "  make memory-battery-generate  Build/verify the synthetic 100/1k/10k trees (~1 s)"
	@echo "  make memory-battery-reference Rebuild the unbounded reference output hashes"
	@echo "  make memory-battery-self-test Millisecond verdict + gate-placement self-test (runs inside make test)"
	@echo "  make performance-regression-sentinel-test Run the zero-model adaptive sentinel contract"
	@echo "  make worktree-lifecycle-test   Run the zero-model lifecycle contract"
	@echo "  make worktree-lifecycle-recovery-test  Run JVM file-lock/crash recovery tests"
	@echo "  make worktree-audit [OUTPUT=/absolute/path.edn] Read-only worktree inventory"
	@echo "  make handoff-worktree REQUEST=/absolute/close-request.edn Create owner handoff"
	@echo "  make finish-worktree REQUEST=/absolute/close-request.edn Dry-run one close plan"
	@echo "  make finish-worktree PLAN=/absolute/path.edn APPLY=1 Apply one reviewed plan; never deletes its branch"
	@echo "    outcomes: landed | negative-experiment | parked; exactly one target and one outcome"
	@echo "    prune request: :clj-surgeon.worktree-registration-prune-request/v1; one target; branch-backed only"
	@echo "    preservation: :branch-tip-on-remote | :commit-on-remote"
	@echo "    prune removal never force-deletes, never runs global git worktree prune, and never deletes refs"
	@echo "    negative seal markers: <!-- BEGIN CLJ-SURGEON NEGATIVE-EXPERIMENT SEAL --> ... <!-- END CLJ-SURGEON NEGATIVE-EXPERIMENT SEAL -->"
	@echo "    handoff: owner writes a handoff, runs the returned unlock command, then another clean worktree plans/applies"
	@echo "  make retain-benchmark-result RESULT_DIR=... Archive raw logs; retain structured evidence"
	@echo "  make verify-benchmark-retention Refuse tracked raw benchmark logs"
	@echo "  make verify-benchmark-evidence Verify archived evidence paths and hashes"
	@echo ""
	@echo "Installation overrides:"
	@echo "  CLI_DEST=/path/to/clj-surgeon  CLI path (default: $(CLI_DEST))"
	@echo "  CODEX_HOME=/path/to/.codex     Codex home (default: $(CODEX_HOME))"
	@echo "  CLAUDE_HOME=/path/to/.claude   Claude home (default: $(CLAUDE_HOME))"
	@echo "  CODEX_GLOBAL_INSTRUCTIONS=/path Override Codex global instruction file"
	@echo "  CLAUDE_GLOBAL_INSTRUCTIONS=/path Override Claude global instruction file"
	@echo "  INSTALL_ROOT=/path/to/packages Stable copied package root (default: $(INSTALL_ROOT))"
	@echo "  CONTROL_PLANE_ROOT_FILE=/path  Local pointer used only by experimental 'clj-surgeon up'"
	@echo "  MCP_JAVA_HOME=/path/to/jdk     Java home for the shared MCP (default: inherited JAVA_HOME)"
	@echo "  MCP_JAVA_CMD=/path/to/java     Java command (default: MCP_JAVA_HOME/bin/java, then PATH)"
	@echo ""
	@echo "Direct usage:"
	@echo "  bb -m clj-surgeon.core :op :ls :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ."
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ~/src.local/ :grep \"mail|imap\""
	@echo "  bb -m clj-surgeon.core :op :mv :file f :form foo :before bar"
	@echo "  bb -m clj-surgeon.core :op :rename-ns :from old :to new :root ."

install: install-cli install-codex-skill install-claude-skill install-agent-routing

install-with-analyzer: install install-clj-kondo-admission

install-clj-kondo-admission:
	@set -eu; \
	  source="$(CLJ_KONDO_ADMISSION_SOURCE)"; \
	  receipt="$(CLJ_KONDO_ADMISSION_DEST).receipt.edn"; \
	  for dest in "$(CLJ_KONDO_ADMISSION_DEST)" "$(CLJ_KONDO_SHIM_DEST)"; do \
	    if [ -e "$$dest" ] && ! grep -q 'Serialize one analyzer' "$$dest"; then \
	      echo "Refusing to replace unrelated file $$dest" >&2; exit 1; \
	    fi; \
	    mkdir -p "$$(dirname "$$dest")"; \
	    stage="$$dest.tmp.$$$$"; \
	    cp "$$source" "$$stage"; \
	    chmod +x "$$stage"; \
	    mv "$$stage" "$$dest"; \
	  done; \
	  printf '%s\n' '{:artifact :clj-kondo-admission' ' :source-commit "$(SOURCE_COMMIT)"' ' :gate "$(CLJ_KONDO_ADMISSION_DEST)"' ' :shell-entrance "$(CLJ_KONDO_SHIM_DEST)"}' > "$$receipt"
	@echo "Installed analyzer gate $(CLJ_KONDO_ADMISSION_DEST) and shell entrance $(CLJ_KONDO_SHIM_DEST)"

check-routing-parity:
	bb "$(CLJ_SURGEON_HOME)bin/check-routing-parity.clj"

install-agent-routing:
	bb --classpath "$(CLJ_SURGEON_HOME)src" -m clj-surgeon.agent-routing install "$(AGENT_ROUTING_SOURCE)" "$(CODEX_GLOBAL_INSTRUCTIONS)" "$(CLAUDE_GLOBAL_INSTRUCTIONS)"

check-agent-routing: check-routing-parity
	bb --classpath "$(CLJ_SURGEON_HOME)src" -m clj-surgeon.agent-routing check "$(AGENT_ROUTING_SOURCE)" "$(CODEX_GLOBAL_INSTRUCTIONS)" "$(CLAUDE_GLOBAL_INSTRUCTIONS)"

sync-clj-surgeon-skill:
	bash bench/sync_clj_surgeon_skill.sh --write

check-clj-surgeon-skill-mirrors:
	bash bench/sync_clj_surgeon_skill.sh --check

mcp-operation-oracle:
	# @spec MCP-OP-ORACLE-001
	swipl -q -f test/mcp_operation_contract_oracle.pl

repository-hygiene:
	# @spec MCP-OP-ALIAS-036
	# @spec MCP-OP-ALIAS-053
	@sh test/repository_hygiene_gate.sh

repository-hygiene-self-test:
	# @spec MCP-OP-ALIAS-053
	@sh test/repository_hygiene_gate_self_test.sh

runtests: mcp-test

mcp-test: mcp-operation-oracle
	@# @spec MCP-OP-TMPHYG-001
	@# @spec MCP-OP-TMPHYG-002
	clojure $(MCP_JAVA_OPTS) -M:clj-surgeon/mcp-test
	@$(MAKE) --no-print-directory repository-hygiene-self-test
	@$(MAKE) --no-print-directory txn-kernel-warning-check
	@$(MAKE) --no-print-directory mcp-heap-config-self-test
	@$(MAKE) --no-print-directory tmp-leak-ratchet-self-test
	@$(MAKE) --no-print-directory clj-kondo-admission-path-self-test
	@$(MAKE) --no-print-directory analyzer-contract-target-self-test
	@$(MAKE) --no-print-directory cclsp-start-self-test
	@$(MAKE) --no-print-directory cclsp-client-audit-self-test

clj-kondo-admission-path-self-test:
	@sh test/clj_kondo_admission_path_test.sh

# The temp-dir hygiene ratchet's refusal branches, driven as real
# subprocesses. `secure-tmpdir!` re-execs its own suite and exits, so these
# cannot be witnessed from inside a clojure.test run.
tmp-leak-ratchet-self-test:
	@# @spec MCP-OP-TMPHYG-003
	@# @spec MCP-OP-TMPHYG-004
	@# @spec MCP-OP-TMPHYG-006
	@# @spec MCP-OP-TMPHYG-007
	@# @spec MCP-OP-TMPHYG-008
	@# @spec MCP-OP-TMPHYG-011
	@# @spec MCP-OP-TMPHYG-012
	@# @spec MCP-OP-TMPHYG-013
	@sh test/tmp_leak_ratchet_test.sh

# @spec MCP-OP-ADMIT-130
# The admit path's no-OOM proof, at an explicit -Xmx, with a numeric pass line
# per arm. Deliberately NOT part of `test`, `test-fast` or `mcp-test`: it
# starts its own bounded JVM and runs in tens of seconds.
ADMIT_ANALYZER_MEMORY_XMX ?= 512m

admit-transaction-recovery-battery:
	java -cp "$$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main test/admit_transaction_recovery_battery.clj

admit-analyzer-memory-self-test:
	clojure -J-Xms64m -J-Xmx$(ADMIT_ANALYZER_MEMORY_XMX) \
	  -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' \
	  -M test/admit_analyzer_memory_selftest.clj

mcp-smoke:
	bb test/mcp_stdio_smoke.clj

mcp-serve:
	JAVA_HOME="$(MCP_JAVA_HOME)" JAVA_CMD="$(MCP_JAVA_CMD)" clojure $(MCP_JAVA_OPTS) -X:clj-surgeon/mcp :telemetry :full

mcp-serve-benchmark:
	JAVA_HOME="$(MCP_JAVA_HOME)" JAVA_CMD="$(MCP_JAVA_CMD)" clojure $(MCP_JAVA_OPTS) -X:clj-surgeon/mcp :telemetry :full :nrepl-port :none :run-id '"$${RUN_ID:-manual}"'

mcp-heap-config-self-test:
	@sh test/mcp_heap_config_test.sh

mcp-reload:
	@set -eu; \
	  port_file="$(MCP_STATE_DIR)/nrepl-port"; \
	  test -f "$$port_file" || { echo "No live MCP nREPL port at $$port_file; run make mcp-start" >&2; exit 1; }; \
	  port=$$(cat "$$port_file"); \
	  result=$$(clj-nrepl-eval --port "$$port" \
	    "(try (doseq [ns '[clj-surgeon.file-ops clj-surgeon.outline clj-surgeon.structural-lens clj-surgeon.owner-hypotheses clj-surgeon.show-form clj-surgeon.mcp-process clj-surgeon.forward-refs clj-surgeon.fix-declares clj-surgeon.binding-rename clj-surgeon.operation-algebra clj-surgeon.intent-transaction clj-surgeon.diagnostic-delta clj-surgeon.extract-header clj-surgeon.quoted-var-refs clj-surgeon.extract clj-surgeon.mcp-paths clj-surgeon.mcp-workspace clj-surgeon.mcp-workspace-sources clj-surgeon.mcp-schema clj-surgeon.mcp-compact-edit-fields clj-surgeon.mcp-compact-relations clj-surgeon.mcp-extraction clj-surgeon.mcp-contract clj-surgeon.mcp-extraction-plan clj-surgeon.mcp-semantic-client clj-surgeon.mcp-source-anchor clj-surgeon.mcp-hot-verify clj-surgeon.mcp-cold-verify clj-surgeon.mcp-change-buffer clj-surgeon.mcp-formatter clj-surgeon.mcp-operation clj-surgeon.mcp-inspect clj-surgeon.mcp-inspect-tool clj-surgeon.mcp-program-tool clj-surgeon.mcp-tool clj-surgeon.mcp-server clj-surgeon.mcp-http-server]] (require ns :reload)) (let [result (clj-surgeon.mcp-server/sync-tools!)] (if (:ok result) result (throw (ex-info \"MCP tool synchronization failed\" result)))) (catch Throwable error {:ok false :error (.getMessage error) :class (.getName (class error))}))"); \
	  case "$$result" in *":ok true"*) ;; *) echo "$$result" >&2; exit 1 ;; esac; \
	  echo "$$result"; \
	  echo "Live handlers and server tool contracts reloaded at $(MCP_URL); the server process did not restart."; \
	  echo "Clients that honor tools/list_changed re-list automatically. The current Codex turn can cache model-visible schemas until a new session."

mcp-dev-start:
	@set -eu; \
	  mkdir -p "$(MCP_DEV_STATE_DIR)/telemetry"; \
	  health_url="$(patsubst %/mcp,%/healthz,$(MCP_DEV_URL))"; \
	  if curl -fsS --max-time 1 "$$health_url" >/dev/null 2>&1; then \
	    echo "clj-surgeon dev MCP already ready at $(MCP_DEV_URL)"; exit 0; \
	  fi; \
	  if test -f "$(MCP_DEV_PID_FILE)"; then \
	    old_pid=$$(cat "$(MCP_DEV_PID_FILE)"); \
	    if kill -0 "$$old_pid" 2>/dev/null; then echo "Dev MCP PID $$old_pid is alive but unhealthy; refusing a competing launch" >&2; exit 1; fi; \
	  fi; \
	  rm -f "$(MCP_DEV_PID_FILE)" "$(MCP_DEV_READY_FILE)" "$(MCP_DEV_STATE_DIR)/nrepl-port"; \
	  cd "$(CLJ_SURGEON_HOME)"; \
	  nohup env JAVA_HOME="$(MCP_JAVA_HOME)" JAVA_CMD="$(MCP_JAVA_CMD)" \
	    "$(CLOJURE_BIN)" $(MCP_JAVA_OPTS) -X:clj-surgeon/mcp \
	    :project-dir '"$(CLJ_SURGEON_HOME)"' \
	    :port '$(MCP_DEV_PORT)' \
	    :telemetry :full \
	    :telemetry-dir '"$(MCP_DEV_STATE_DIR)/telemetry"' \
	    :run-id '"dev-$(MCP_DEV_PORT)"' \
	    :port-file '"$(MCP_DEV_STATE_DIR)/nrepl-port"' \
	    :ready-file '"$(MCP_DEV_READY_FILE)"' \
	    :log-file '"$(MCP_DEV_LOG_FILE)"' \
	    :nrepl-port '0' \
	    >"$(MCP_DEV_LOG_FILE)" 2>&1 & \
	  launcher_pid=$$!; \
	  ready=false; \
	  for attempt in $$(seq 1 $(MCP_START_ATTEMPTS)); do \
	    if curl -fsS --max-time 1 "$$health_url" >/dev/null 2>&1 && test -f "$(MCP_DEV_READY_FILE)"; then ready=true; break; fi; \
	    if ! kill -0 "$$launcher_pid" 2>/dev/null; then break; fi; \
	    sleep 0.5; \
	  done; \
	  if test "$$ready" != true; then tail -40 "$(MCP_DEV_LOG_FILE)" >&2 || true; kill "$$launcher_pid" 2>/dev/null || true; exit 1; fi; \
	  dev_pid=$$(sed -n 's/.*:pid \([0-9][0-9]*\).*/\1/p' "$(MCP_DEV_READY_FILE)"); \
	  test -n "$$dev_pid"; printf '%s\n' "$$dev_pid" > "$(MCP_DEV_PID_FILE)"; \
	  echo "clj-surgeon dev MCP ready at $(MCP_DEV_URL) (PID $$dev_pid)"

mcp-dev-stop:
	@set -eu; \
	  if test -f "$(MCP_DEV_PID_FILE)"; then \
	    pid=$$(cat "$(MCP_DEV_PID_FILE)"); \
	    if kill -0 "$$pid" 2>/dev/null; then kill "$$pid"; fi; \
	    for attempt in $$(seq 1 $(MCP_STOP_ATTEMPTS)); do if ! kill -0 "$$pid" 2>/dev/null; then break; fi; sleep 0.1; done; \
	    if kill -0 "$$pid" 2>/dev/null; then echo "Dev MCP PID $$pid did not stop" >&2; exit 1; fi; \
	  fi; \
	  rm -f "$(MCP_DEV_PID_FILE)" "$(MCP_DEV_READY_FILE)" "$(MCP_DEV_STATE_DIR)/nrepl-port"; \
	  echo "clj-surgeon dev MCP stopped on port $(MCP_DEV_PORT)"

mcp-dev-status:
	@set -eu; \
	  curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_DEV_URL))"; echo; \
	  test -f "$(MCP_DEV_PID_FILE)"; pid=$$(cat "$(MCP_DEV_PID_FILE)"); kill -0 "$$pid"; \
	  echo "Dev MCP PID $$pid; state $(MCP_DEV_STATE_DIR)"

mcp-dev-reload:
	@set -eu; \
	  port_file="$(MCP_DEV_STATE_DIR)/nrepl-port"; \
	  test -f "$$port_file" || { echo "No dev MCP nREPL port at $$port_file; run make mcp-dev-start" >&2; exit 1; }; \
	  port=$$(cat "$$port_file"); \
	  result=$$(clj-nrepl-eval --port "$$port" \
	    "(try (doseq [ns '[clj-surgeon.file-ops clj-surgeon.outline clj-surgeon.structural-lens clj-surgeon.owner-hypotheses clj-surgeon.show-form clj-surgeon.mcp-process clj-surgeon.forward-refs clj-surgeon.fix-declares clj-surgeon.binding-rename clj-surgeon.operation-algebra clj-surgeon.intent-transaction clj-surgeon.diagnostic-delta clj-surgeon.extract-header clj-surgeon.quoted-var-refs clj-surgeon.extract clj-surgeon.mcp-paths clj-surgeon.mcp-workspace clj-surgeon.mcp-workspace-sources clj-surgeon.mcp-schema clj-surgeon.mcp-compact-edit-fields clj-surgeon.mcp-compact-relations clj-surgeon.mcp-extraction clj-surgeon.mcp-contract clj-surgeon.mcp-extraction-plan clj-surgeon.mcp-semantic-client clj-surgeon.mcp-source-anchor clj-surgeon.mcp-hot-verify clj-surgeon.mcp-cold-verify clj-surgeon.mcp-change-buffer clj-surgeon.mcp-formatter clj-surgeon.mcp-operation clj-surgeon.mcp-inspect clj-surgeon.mcp-inspect-tool clj-surgeon.mcp-program-tool clj-surgeon.mcp-tool clj-surgeon.mcp-server clj-surgeon.mcp-http-server]] (require ns :reload)) (let [result (clj-surgeon.mcp-server/sync-tools!)] (if (:ok result) result (throw (ex-info \"MCP tool synchronization failed\" result)))) (catch Throwable error {:ok false :error (.getMessage error) :class (.getName (class error))}))"); \
	  case "$$result" in *":ok true"*) ;; *) echo "$$result" >&2; exit 1 ;; esac; \
	  echo "$$result"; echo "Dev handlers reloaded at $(MCP_DEV_URL)."

mcp-dev-register:
	@set -eu; \
	  workspace="$${WORKSPACE:-$(CLJ_SURGEON_HOME)}"; mkdir -p "$$workspace"; \
	  echo "codex mcp add clj-surgeon-dev --url $(MCP_DEV_URL)"; \
	  codex mcp add clj-surgeon-dev --url "$(MCP_DEV_URL)"; \
	  python3 -c 'import json,pathlib; p=pathlib.Path("'"$$workspace"'")/".mcp.json"; d=json.loads(p.read_text()) if p.exists() else {}; d.setdefault("mcpServers", {})["clj-surgeon-dev"]={"type":"http","url":"$(MCP_DEV_URL)"}; p.write_text(json.dumps(d, indent=2)+"\n")'; \
	  echo "Updated $$workspace/.mcp.json"

cclsp-start-self-test:
	@sh test/cclsp_start_test.sh
	@sh test/cclsp_launch_path_test.sh

cclsp-client-audit-self-test:
	@sh test/direct_cclsp_client_audit_test.sh

cclsp-client-audit:
	@python3 dev/audit_direct_cclsp_clients.py --root "$(abspath $(CLJ_SURGEON_HOME)..)"

cclsp-start:
	@set -eu; \
	  mkdir -p "$(CCLSP_STATE_DIR)"; \
	  start_status="$(CCLSP_STATE_DIR)/last-start.edn"; \
	  write_status() { \
	    stage="$$start_status.tmp.$$$$"; \
	    printf '{:server-restarted %s :config-path "%s"}\n' "$$1" "$$expected_config" > "$$stage"; \
	    mv "$$stage" "$$start_status"; \
	  }; \
	  rm -f "$$start_status"; \
	  restarted=false; \
	  health_url="$(patsubst %/mcp,%/healthz,$(CCLSP_URL))"; \
	  expected_config=$$(python3 -c 'import pathlib; print(pathlib.Path("$(CCLSP_CONFIG)").resolve())'); \
	  healthy=false; \
	  health=; \
	  for attempt in $$(seq 1 $(CCLSP_HEALTH_ATTEMPTS)); do \
	    if health=$$(curl -fsS --max-time 1 "$$health_url" 2>/dev/null); then healthy=true; break; fi; \
	    if ! launchctl print "gui/$$(id -u)/$(CCLSP_LAUNCH_LABEL)" >/dev/null 2>&1; then break; fi; \
	    sleep $(CCLSP_HEALTH_INTERVAL); \
	  done; \
	  if [ "$$healthy" = true ]; then \
	    actual_config=$$(printf '%s' "$$health" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("config_path") or "")'); \
	    if [ "$$actual_config" = "$$expected_config" ]; then \
	      write_status false; \
	      echo "cclsp already ready at $(CCLSP_URL)"; \
	      exit 0; \
	    fi; \
	    if launchctl print "gui/$$(id -u)/$(CCLSP_LAUNCH_LABEL)" >/dev/null 2>&1; then \
	      echo "cclsp is healthy with a different config; reloading the managed service"; \
	      restarted=true; \
	      launchctl remove "$(CCLSP_LAUNCH_LABEL)" >/dev/null 2>&1 || true; \
	    else \
	      echo "cclsp port $(CCLSP_PORT) is owned by an unmanaged service using '$$actual_config'; refusing to replace it" >&2; \
	      exit 1; \
	    fi; \
	  fi; \
	  test -x "$(CCLSP_HOME)/node_modules/.bin/bun" || { echo "Run make setup in $(CCLSP_HOME)" >&2; exit 1; }; \
	  launchctl remove "$(CCLSP_LAUNCH_LABEL)" >/dev/null 2>&1 || true; \
	  launchctl submit -l "$(CCLSP_LAUNCH_LABEL)" \
	    -o "$(CCLSP_LOG_FILE)" -e "$(CCLSP_LOG_FILE)" -- \
	    /bin/sh -c 'cd "$$1"; export CCLSP_CONFIG_PATH="$$2"; export PATH="$$3"; shift 3; exec "$$@"' _ \
	    "$(CCLSP_HOME)" "$(CCLSP_CONFIG)" "$(PATH)" \
	    /usr/bin/env LANG=C.UTF-8 LC_ALL=C.UTF-8 \
	    CCLSP_BUN="$(CCLSP_HOME)/node_modules/.bin/bun" \
	    node scripts/dev-http-supervisor.mjs serve-http --host 127.0.0.1 --port "$(CCLSP_PORT)"; \
	  ready=false; \
	  for attempt in $$(seq 1 60); do \
	    if curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(CCLSP_URL))" >/dev/null 2>&1; then ready=true; break; fi; \
	    if ! launchctl print "gui/$$(id -u)/$(CCLSP_LAUNCH_LABEL)" >/dev/null 2>&1; then break; fi; \
	    sleep 0.5; \
	  done; \
	  if [ "$$ready" != true ]; then \
	    echo "cclsp did not become ready; recent log:" >&2; \
	    tail -60 "$(CCLSP_LOG_FILE)" >&2 || true; \
	    launchctl remove "$(CCLSP_LAUNCH_LABEL)" >/dev/null 2>&1 || true; \
	    exit 1; \
	  fi; \
	  write_status "$$restarted"; \
	  echo "cclsp ready at $(CCLSP_URL) with restart-on-save TypeScript"

cclsp-stop:
	@launchctl remove "$(CCLSP_LAUNCH_LABEL)" >/dev/null 2>&1 || true
	@echo "cclsp stopped"

cclsp-status:
	@curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(CCLSP_URL))" \
	  || { echo "cclsp endpoint unavailable: $(CCLSP_URL)" >&2; exit 1; }
	@echo

mcp-start: cclsp-start
	@set -eu; \
	  mkdir -p "$(MCP_STATE_DIR)"; \
	  if curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))" >/dev/null 2>&1 \
	    && test -f "$(MCP_READY_FILE)" \
	    && launchctl print "gui/$$(id -u)/$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1; then \
	    echo "clj-surgeon MCP already ready at $(MCP_URL)"; \
	    exit 0; \
	  fi; \
	  test -n "$(CLOJURE_BIN)" || { echo "clojure is required" >&2; exit 1; }; \
	  test -x "$(MCP_JAVA_CMD)" || { echo "MCP Java runtime is not executable: $(MCP_JAVA_CMD)" >&2; exit 1; }; \
	  launchctl remove "$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1 || true; \
	  stopped=false; \
	  for attempt in $$(seq 1 $(MCP_STOP_ATTEMPTS)); do \
	    if ! launchctl print "gui/$$(id -u)/$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1 \
	      && ! curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))" >/dev/null 2>&1; then \
	      stopped=true; break; \
	    fi; \
	    sleep 0.1; \
	  done; \
	  test "$$stopped" = true || { \
	    echo "previous clj-surgeon MCP did not stop cleanly; refusing a competing launch" >&2; \
	    exit 1; \
	  }; \
	  rm -f "$(MCP_READY_FILE)" "$(MCP_PID_FILE)"; \
	  launchctl submit -l "$(MCP_LAUNCH_LABEL)" \
	    -o "$(MCP_LOG_FILE)" -e "$(MCP_LOG_FILE)" -- \
	    /bin/sh -c 'cd "$$1"; shift; export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:$$PATH"; exec "$$@"' _ "$(CLJ_SURGEON_HOME)" \
	    /usr/bin/env JAVA_HOME="$(MCP_JAVA_HOME)" JAVA_CMD="$(MCP_JAVA_CMD)" \
	    "$(CLOJURE_BIN)" $(MCP_JAVA_OPTS) -X:clj-surgeon/mcp \
	    :project-dir '"$(MCP_PROJECT_DIR)"' \
	    :port '$(MCP_PORT)' \
	    :telemetry :full \
	    :telemetry-dir '"$(MCP_STATE_DIR)/telemetry"' \
	    :run-id '"dogfood"' \
	    :cclsp-url '"$(CCLSP_URL)"' \
	    :ready-file '"$(MCP_READY_FILE)"' \
	    :port-file '"$(MCP_STATE_DIR)/nrepl-port"'; \
	  ready=false; \
	  for attempt in $$(seq 1 $(MCP_START_ATTEMPTS)); do \
	    if curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))" >/dev/null 2>&1 \
	      && test -f "$(MCP_READY_FILE)"; then ready=true; break; fi; \
	    if ! launchctl print "gui/$$(id -u)/$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1; then break; fi; \
	    sleep 0.5; \
	  done; \
	  if [ "$$ready" != true ]; then \
	    echo "clj-surgeon MCP did not become ready; recent log:" >&2; \
	    tail -40 "$(MCP_LOG_FILE)" >&2 || true; \
	    launchctl remove "$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1 || true; \
	    exit 1; \
	  fi; \
	  mcp_pid=$$(sed -n 's/.*:pid \([0-9][0-9]*\).*/\1/p' "$(MCP_READY_FILE)"); \
	  printf '%s\n' "$$mcp_pid" > "$(MCP_PID_FILE)"; \
	  echo "clj-surgeon MCP ready at $(MCP_URL) (launchd PID $$mcp_pid)"

mcp-stop:
	@set -eu; \
	  launchctl remove "$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1 || true; \
	  rm -f "$(MCP_PID_FILE)" "$(MCP_READY_FILE)" "$(MCP_STATE_DIR)/nrepl-port"; \
	  echo "clj-surgeon MCP stopped"
	@$(MAKE) --no-print-directory cclsp-stop

mcp-status: cclsp-status
	@set -eu; \
	  if curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))"; then echo; else echo "MCP endpoint unavailable: $(MCP_URL)"; fi; \
	  if [ -f "$(MCP_READY_FILE)" ]; then echo "Readiness: $$(cat "$(MCP_READY_FILE)")"; fi; \
	  if launchctl print "gui/$$(id -u)/$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1; then echo "Service: launchd job $(MCP_LAUNCH_LABEL) is loaded"; else echo "Service: not loaded"; fi; \
	  codex mcp get clj-surgeon 2>/dev/null || echo "Codex registration: absent"

workspace-mcp-start:
	@set -eu; \
	  test -n "$(WORKSPACE)" || { echo "WORKSPACE=/absolute/repository/path is required" >&2; exit 1; }; \
	  bb -m clj-surgeon.core up "$(abspath $(WORKSPACE))"

workspace-mcp-stop:
	@echo "No per-workspace process exists. Use make mcp-stop only to stop the shared stack."

workspace-mcp-status:
	@set -eu; \
	  test -n "$(WORKSPACE)" || { echo "WORKSPACE=/absolute/repository/path is required" >&2; exit 1; }; \
	  curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(CCLSP_URL))"; echo; \
	  curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))"; echo; \
	  test -f "$(abspath $(WORKSPACE))/.codex/config.toml"; \
	  grep -Fq '# BEGIN clj-surgeon workspace tools' "$(abspath $(WORKSPACE))/.codex/config.toml"; \
	  python3 -c 'import pathlib,tomllib; tomllib.loads(pathlib.Path("$(abspath $(WORKSPACE))/.codex/config.toml").read_text())'; \
	  grep -Fq '"rootDir" : "$(abspath $(WORKSPACE))"' "$(HOME)/.local/state/clj-surgeon/cclsp.json"; \
	  echo "Shared MCP stack is ready for $(abspath $(WORKSPACE))."

workspace-mcp-onboard: workspace-mcp-start
	@$(MAKE) --no-print-directory workspace-mcp-status WORKSPACE="$(abspath $(WORKSPACE))"
	@echo "Restart the coding-agent session only if this command changed .codex/config.toml."

workspace-mcp-install-codex: workspace-mcp-onboard
	@echo "workspace-mcp-install-codex is a compatibility alias for workspace-mcp-onboard."

install-mcp-codex-dev: install-dev mcp-start
	@set -eu; \
	  codex mcp remove clj-surgeon >/dev/null 2>&1 || true; \
	  codex mcp add clj-surgeon --url "$(MCP_URL)"; \
	  echo "Restart Codex, then confirm that apply_clojure_changes is available."

uninstall-mcp-codex-dev:
	@codex mcp remove clj-surgeon >/dev/null 2>&1 || true
	@$(MAKE) --no-print-directory mcp-stop
	@echo "Removed the local clj-surgeon MCP registration from Codex."

prepare-cli-package:
	@set -eu; \
	  package="$(CLI_PACKAGE)"; \
	  if [ ! -d "$$package" ]; then \
	    mkdir -p "$(VERSION_ROOT)"; \
	    stage="$$package.tmp.$$$$"; \
	    trap 'chmod -R u+w "$$stage" 2>/dev/null || true; rm -rf "$$stage"' EXIT HUP INT TERM; \
	    mkdir -p "$$stage/src"; \
	    cp -R "$(CLJ_SURGEON_HOME)src/." "$$stage/src/"; \
	    printf '%s\n' '{:artifact :cli-runtime' ' :mode :stable-copy' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(CLI_SOURCE_HASH)"}' > "$$stage/install-receipt.edn"; \
	    chmod -R a-w "$$stage"; \
	    mv "$$stage" "$$package"; \
	    trap - EXIT HUP INT TERM; \
	  fi

install-cli: prepare-cli-package
	@mkdir -p "$$(dirname "$(CLI_DEST)")"
	@command -v bb >/dev/null 2>&1 || { \
	  echo "Warning: 'bb' (babashka) not found on PATH."; \
	  echo "  Install (no sudo): bash <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install) --dir ~/bin"; \
	  echo "  The launcher will be written anyway, but won't run until bb is installed."; \
	}
	@command -v clj-kondo >/dev/null 2>&1 || { \
	  echo "Warning: 'clj-kondo' not found on PATH (required for :ls / :outline / :fix-declares!)."; \
	  echo "  Install (no sudo): bash <(curl -s https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo) --dir ~/bin"; \
	}
	@set -eu; \
	  dest="$(CLI_DEST)"; \
	  receipt="$(CLI_DEST).receipt.edn"; \
	  control_plane="$(CONTROL_PLANE_ROOT_FILE)"; \
	  control_plane_receipt="$(CONTROL_PLANE_ROOT_FILE).receipt.edn"; \
	  if [ -e "$$dest" ] && \
	     ! grep -q '^## clj-surgeon stable launcher' "$$dest" && \
	     ! grep -q '^;; clj-surgeon development launcher' "$$dest" && \
	     ! grep -qF '(require (quote [clj-surgeon.core :as core]))' "$$dest"; then \
	    echo "Refusing to replace unrelated file $$dest"; \
	    exit 1; \
	  fi; \
	  if [ -e "$$receipt" ] && { [ ! -f "$$receipt" ] || ! grep -q ':artifact :cli' "$$receipt"; }; then echo "Refusing to replace unrelated receipt $$receipt"; exit 1; fi; \
	  if [ -e "$$control_plane" ] && { [ ! -f "$$control_plane_receipt" ] || ! grep -q ':artifact :control-plane-root' "$$control_plane_receipt"; }; then echo "Refusing to replace unrelated control-plane pointer $$control_plane"; exit 1; fi; \
	  mkdir -p "$$(dirname "$$control_plane")"; \
	  control_plane_stage="$$control_plane.tmp.$$$$"; \
	  printf '%s\n' "$(CLJ_SURGEON_HOME)" > "$$control_plane_stage"; \
	  mv "$$control_plane_stage" "$$control_plane"; \
	  printf '%s\n' '{:artifact :control-plane-root' ' :mode :local-pointer' ' :source-commit "$(SOURCE_COMMIT)"' ' :path "$(CLJ_SURGEON_HOME)"}' > "$$control_plane_receipt"; \
	  stage="$$dest.tmp.$$$$"; \
	  trap 'rm -f "$$stage" "$$control_plane_stage"' EXIT HUP INT TERM; \
	  printf '%s\n' '#!/bin/sh' '## clj-surgeon stable launcher' 'CLJ_SURGEON_CONTROL_PLANE_ROOT_FILE="$(CONTROL_PLANE_ROOT_FILE)" exec bb --classpath "$(CLI_PACKAGE)/src" -m clj-surgeon.core "$$@"' > "$$stage"; \
	  chmod +x "$$stage"; \
	  mv "$$stage" "$$dest"; \
	  trap - EXIT HUP INT TERM; \
	  printf '%s\n' '{:artifact :cli' ' :mode :stable-copy' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(CLI_SOURCE_HASH)"' ' :destination "$(CLI_DEST)"' ' :package "$(CLI_PACKAGE)"' ' :control-plane-root-file "$(CONTROL_PLANE_ROOT_FILE)"}' > "$$receipt"
	@echo "Installed stable CLI $(CLI_DEST) from commit $(SOURCE_COMMIT), source hash $(CLI_SOURCE_HASH)"
	@echo "Receipt: $(CLI_DEST).receipt.edn"

prepare-skill-package:
	@set -eu; \
	  package="$(SKILL_PACKAGE)"; \
	  if [ ! -d "$$package" ]; then \
	    mkdir -p "$(VERSION_ROOT)"; \
	    stage="$$package.tmp.$$$$"; \
	    trap 'chmod -R u+w "$$stage" 2>/dev/null || true; rm -rf "$$stage"' EXIT HUP INT TERM; \
	    mkdir -p "$$stage"; \
	    cp -R "$(SKILL_SOURCE)/." "$$stage/"; \
	    printf '%s\n' '' 'Stable copy installed from commit $(SOURCE_COMMIT).' 'When working inside the clj-surgeon repository, the working-tree skill.md' 'supersedes this copy.' >> "$$stage/SKILL.md"; \
	    printf '%s\n' '{:artifact :agent-skill' ' :mode :stable-copy' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(SKILL_SOURCE_HASH)"}' > "$$stage/install-receipt.edn"; \
	    chmod -R a-w "$$stage"; \
	    mv "$$stage" "$$package"; \
	    trap - EXIT HUP INT TERM; \
	  fi

install-codex-skill: prepare-skill-package
	@mkdir -p "$(CODEX_HOME)/skills"
	@set -eu; \
	  dest="$(CODEX_SKILL_DEST)"; \
	  receipt="$(CODEX_SKILL_DEST).receipt.edn"; \
	  if { [ -e "$$dest" ] || [ -L "$$dest" ]; } && [ ! -L "$$dest" ]; then \
	    echo "Refusing to replace unrelated path $$dest"; \
	    exit 1; \
	  fi; \
	  if [ -L "$$dest" ]; then \
	    current=$$(readlink "$$dest"); \
	    case "$$current" in \
	      "$(SKILL_SOURCE)"|"$(INSTALL_ROOT)"/versions/*) ;; \
	      *) if [ ! -f "$$receipt" ] || ! grep -q ':artifact :codex-skill' "$$receipt"; then echo "Refusing to replace unrelated symlink $$dest -> $$current"; exit 1; fi ;; \
	    esac; \
	  fi; \
	  if [ -e "$$receipt" ] && { [ ! -f "$$receipt" ] || ! grep -q ':artifact :codex-skill' "$$receipt"; }; then echo "Refusing to replace unrelated receipt $$receipt"; exit 1; fi; \
	  ln -sfn "$(SKILL_PACKAGE)" "$$dest"; \
	  printf '%s\n' '{:artifact :codex-skill' ' :mode :stable-copy' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(SKILL_SOURCE_HASH)"' ' :destination "$(CODEX_SKILL_DEST)"' ' :package "$(SKILL_PACKAGE)"}' > "$$receipt"
	@echo "Installed stable Codex skill $(CODEX_SKILL_DEST) from commit $(SOURCE_COMMIT), source hash $(SKILL_SOURCE_HASH)"
	@echo "Receipt: $(CODEX_SKILL_DEST).receipt.edn"

install-claude-skill: prepare-skill-package
	@mkdir -p "$(CLAUDE_HOME)/skills"
	@set -eu; \
	  dest="$(CLAUDE_SKILL_DEST)"; \
	  receipt="$(CLAUDE_SKILL_DEST).receipt.edn"; \
	  if { [ -e "$$dest" ] || [ -L "$$dest" ]; } && [ ! -L "$$dest" ]; then \
	    echo "Refusing to replace unrelated path $$dest"; \
	    exit 1; \
	  fi; \
	  if [ -L "$$dest" ]; then \
	    current=$$(readlink "$$dest"); \
	    case "$$current" in \
	      "$(SKILL_SOURCE)"|"$(INSTALL_ROOT)"/versions/*) ;; \
	      *) if [ ! -f "$$receipt" ] || ! grep -q ':artifact :claude-skill' "$$receipt"; then echo "Refusing to replace unrelated symlink $$dest -> $$current"; exit 1; fi ;; \
	    esac; \
	  fi; \
	  if [ -e "$$receipt" ] && { [ ! -f "$$receipt" ] || ! grep -q ':artifact :claude-skill' "$$receipt"; }; then echo "Refusing to replace unrelated receipt $$receipt"; exit 1; fi; \
	  ln -sfn "$(SKILL_PACKAGE)" "$$dest"; \
	  printf '%s\n' '{:artifact :claude-skill' ' :mode :stable-copy' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(SKILL_SOURCE_HASH)"' ' :destination "$(CLAUDE_SKILL_DEST)"' ' :package "$(SKILL_PACKAGE)"}' > "$$receipt"
	@echo "Installed stable Claude skill $(CLAUDE_SKILL_DEST) from commit $(SOURCE_COMMIT), source hash $(SKILL_SOURCE_HASH)"
	@echo "Receipt: $(CLAUDE_SKILL_DEST).receipt.edn"

install-dev: install-dev-cli install-clj-kondo-admission install-dev-codex-skill install-dev-claude-skill install-agent-routing
	@echo "DEVELOPMENT INSTALL: all entrances are branch-coupled to $(CLJ_SURGEON_HOME)"

install-dev-cli:
	@mkdir -p "$$(dirname "$(CLI_DEST)")"
	@set -eu; \
	  dest="$(CLI_DEST)"; \
	  receipt="$(CLI_DEST).receipt.edn"; \
	  if [ -e "$$dest" ] && \
	     ! grep -q '^## clj-surgeon stable launcher' "$$dest" && \
	     ! grep -q '^;; clj-surgeon development launcher' "$$dest" && \
	     ! grep -qF '(require (quote [clj-surgeon.core :as core]))' "$$dest"; then \
	    echo "Refusing to replace unrelated file $$dest"; \
	    exit 1; \
	  fi; \
	  if [ -e "$$receipt" ] && { [ ! -f "$$receipt" ] || ! grep -q ':artifact :cli' "$$receipt"; }; then echo "Refusing to replace unrelated receipt $$receipt"; exit 1; fi; \
	  stage="$$dest.tmp.$$$$"; \
	  trap 'rm -f "$$stage"' EXIT HUP INT TERM; \
	  printf '%s\n' '#!/usr/bin/env bb' ';; clj-surgeon development launcher — branch-coupled' '(require (quote [babashka.classpath :as cp]))' '(cp/add-classpath "$(CLJ_SURGEON_HOME)src")' '(require (quote [clj-surgeon.core :as core]))' '(apply core/-main *command-line-args*)' > "$$stage"; \
	  chmod +x "$$stage"; \
	  mv "$$stage" "$$dest"; \
	  trap - EXIT HUP INT TERM; \
	  printf '%s\n' '{:artifact :cli' ' :mode :development-link' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(CLI_SOURCE_HASH)"' ' :destination "$(CLI_DEST)"' ' :source "$(CLJ_SURGEON_HOME)src"}' > "$$receipt"
	@echo "DEVELOPMENT LINK: $(CLI_DEST) loads the active checkout $(CLJ_SURGEON_HOME)src"

install-dev-codex-skill:
	@mkdir -p "$(CODEX_HOME)/skills"
	@set -eu; \
	  dest="$(CODEX_SKILL_DEST)"; \
	  receipt="$(CODEX_SKILL_DEST).receipt.edn"; \
	  if { [ -e "$$dest" ] || [ -L "$$dest" ]; } && [ ! -L "$$dest" ]; then \
	    echo "Refusing to replace unrelated path $$dest"; \
	    exit 1; \
	  fi; \
	  if [ -L "$$dest" ]; then \
	    current=$$(readlink "$$dest"); \
	    case "$$current" in \
	      "$(SKILL_SOURCE)"|"$(INSTALL_ROOT)"/versions/*) ;; \
	      *) if [ ! -f "$$receipt" ] || ! grep -q ':artifact :codex-skill' "$$receipt"; then echo "Refusing to replace unrelated symlink $$dest -> $$current"; exit 1; fi ;; \
	    esac; \
	  fi; \
	  if [ -e "$$receipt" ] && { [ ! -f "$$receipt" ] || ! grep -q ':artifact :codex-skill' "$$receipt"; }; then echo "Refusing to replace unrelated receipt $$receipt"; exit 1; fi; \
	  ln -sfn "$(SKILL_SOURCE)" "$$dest"; \
	  printf '%s\n' '{:artifact :codex-skill' ' :mode :development-link' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(SKILL_SOURCE_HASH)"' ' :destination "$(CODEX_SKILL_DEST)"' ' :source "$(SKILL_SOURCE)"}' > "$$receipt"
	@echo "DEVELOPMENT LINK: $(CODEX_SKILL_DEST) -> $(SKILL_SOURCE) (branch-coupled)"

install-dev-claude-skill:
	@mkdir -p "$(CLAUDE_HOME)/skills"
	@set -eu; \
	  dest="$(CLAUDE_SKILL_DEST)"; \
	  receipt="$(CLAUDE_SKILL_DEST).receipt.edn"; \
	  if { [ -e "$$dest" ] || [ -L "$$dest" ]; } && [ ! -L "$$dest" ]; then \
	    echo "Refusing to replace unrelated path $$dest"; \
	    exit 1; \
	  fi; \
	  if [ -L "$$dest" ]; then \
	    current=$$(readlink "$$dest"); \
	    case "$$current" in \
	      "$(SKILL_SOURCE)"|"$(INSTALL_ROOT)"/versions/*) ;; \
	      *) if [ ! -f "$$receipt" ] || ! grep -q ':artifact :claude-skill' "$$receipt"; then echo "Refusing to replace unrelated symlink $$dest -> $$current"; exit 1; fi ;; \
	    esac; \
	  fi; \
	  if [ -e "$$receipt" ] && { [ ! -f "$$receipt" ] || ! grep -q ':artifact :claude-skill' "$$receipt"; }; then echo "Refusing to replace unrelated receipt $$receipt"; exit 1; fi; \
	  ln -sfn "$(SKILL_SOURCE)" "$$dest"; \
	  printf '%s\n' '{:artifact :claude-skill' ' :mode :development-link' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(SKILL_SOURCE_HASH)"' ' :destination "$(CLAUDE_SKILL_DEST)"' ' :source "$(SKILL_SOURCE)"}' > "$$receipt"
	@echo "DEVELOPMENT LINK: $(CLAUDE_SKILL_DEST) -> $(SKILL_SOURCE) (branch-coupled)"

nrepl:
	cd $(CLJ_SURGEON_HOME) && clojure $(MCP_JAVA_OPTS) -M:clj-surgeon/mcp-test:clj-surgeon/nrepl

benchmark-clean-codex:
	bash bench/run_clean_codex.sh

benchmark-harness-self-test:
	BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_codex.sh

benchmark-edit-portfolio:
	BENCH_PRE_COMMIT="$${BENCH_PRE_COMMIT:-5d3e262}" \
	BENCH_POST_COMMIT="$${BENCH_POST_COMMIT:-WORKTREE}" \
	BENCH_RUN_MATRIX="$${BENCH_RUN_MATRIX:-pre:matched-skill post:matched-skill native:no-skill}" \
	BENCH_TASKS="$${BENCH_TASKS:-decision-batch-edit pair-view-expect-edit dependency-move-edit literal-source-edit native-text-edit}" \
	BENCH_INCLUDE_COMPACT=false \
	BENCH_REPLICATES="$${BENCH_REPLICATES:-1}" \
	bash bench/run_clean_codex.sh

benchmark-edit-portfolio-self-test:
	bb bench/write_mcp_config.clj --self-test
	bb bench/score_format_extraction.clj --self-test
	bb bench/verify_edit_portfolio.clj --self-test
	bb bench/verify_edit_portfolio.clj bench/fixtures/edit_portfolio
	BENCH_SCHEDULE_SELF_TEST=true bash bench/run_clean_codex.sh
	BENCH_PROMPT_SELF_TEST=true bash bench/run_clean_codex.sh
	BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_codex.sh
	$(MAKE) --no-print-directory benchmark-anvil-portfolio-pair-self-test

benchmark-anvil-compiled-edit-canary:
	@test -n "$(RESULT_DIR)" || { echo "RESULT_DIR is required"; exit 2; }
	@test -n "$(LATIN_ROW)" || { echo "LATIN_ROW=1, 2, or 3 is required"; exit 2; }
	bash bench/run_anvil_compiled_edit_canary.sh "$(RESULT_DIR)" "$(LATIN_ROW)" "$(or $(REPLICATES),1)"

benchmark-anvil-public-cfp-cleanup:
	@test -n "$(RESULT_DIR)" || { echo "RESULT_DIR is required"; exit 2; }
	@test -n "$(ORDER)" || { echo "ORDER=compact-first or native-first is required"; exit 2; }
	bash bench/run_anvil_public_cfp_cleanup.sh "$(RESULT_DIR)" "$(ORDER)" "$(or $(REPLICATES),1)"

benchmark-anvil-format-extraction:
	@test -n "$(RESULT_DIR)" || { echo "RESULT_DIR is required"; exit 2; }
	@test -n "$(ORDER)" || { echo "ORDER=mcp-first or native-first is required"; exit 2; }
	bash bench/run_anvil_format_extraction.sh "$(RESULT_DIR)" "$(ORDER)" "$(or $(REPLICATES),1)"

benchmark-anvil-portfolio-pair:
	@test -n "$(RESULT_DIR)" || { echo "RESULT_DIR is required"; exit 2; }
	@test -n "$(TASK)" || { echo "TASK must name a frozen edit-portfolio capsule"; exit 2; }
	@test -n "$(ORDER)" || { echo "ORDER=compact-first or native-first is required"; exit 2; }
	bash bench/run_anvil_portfolio_pair.sh "$(RESULT_DIR)" "$(TASK)" "$(ORDER)" "$(or $(REPLICATES),1)"

benchmark-anvil-portfolio-pair-self-test:
	@# @spec MCP-OP-TMPHYG-010
	ANVIL_PAIR_CONFIG_SELF_TEST=true bash bench/run_anvil_portfolio_pair.sh \
		$(SELF_TEST_TMP)/clj-surgeon-anvil-pair-self-test decision-batch-edit compact-first 2
	ANVIL_PAIR_CONFIG_SELF_TEST=true bash bench/run_anvil_public_cfp_cleanup.sh \
		$(SELF_TEST_TMP)/clj-surgeon-public-cfp-self-test native-first 1
	ANVIL_FORMAT_CONFIG_SELF_TEST=true bash bench/run_anvil_format_extraction.sh \
		$(SELF_TEST_TMP)/clj-surgeon-format-extraction-self-test mcp-first 1

benchmark-inspect-mcp:
	bash bench/run_inspect_mcp_benchmark.sh

benchmark-inspect-mcp-self-test:
	BENCH_RESULT_DIR="$$(mktemp -d "$${TMPDIR:-/var/tmp}/clj-surgeon-inspect-self-test.XXXXXX")" \
	bash bench/run_inspect_mcp_benchmark.sh --self-test

anvil-arms-self-test:
	bash bench/anvil-arms/self-test.sh

benchmark-codex-skill:
	BENCH_POST_COMMIT="$${BENCH_POST_COMMIT:-HEAD}" \
	BENCH_VERSIONS="$${BENCH_VERSIONS:-post}" \
	BENCH_CONTEXTS="$${BENCH_CONTEXTS:-matched-skill}" \
	BENCH_TASKS="$${BENCH_TASKS:-ops-registry-xray pair-view-edit pair-view-expect-edit}" \
	BENCH_INCLUDE_COMPACT="$${BENCH_INCLUDE_COMPACT:-false}" \
	BENCH_REPLICATES="$${BENCH_REPLICATES:-1}" \
	bash bench/run_clean_codex.sh

benchmark-claude-skill:
	bash bench/run_clean_claude.sh

benchmark-agent-skills:
	$(MAKE) benchmark-codex-skill
	$(MAKE) benchmark-claude-skill

benchmark-codex-skill-self-test:
	BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_codex.sh

benchmark-claude-skill-self-test:
	CLAUDE_BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_claude.sh

benchmark-agent-skills-self-test:
	$(MAKE) clj-surgeon-skill-self-test
	$(MAKE) benchmark-codex-skill-self-test
	$(MAKE) benchmark-claude-skill-self-test

clj-surgeon-skill-self-test:
	bb bench/verify_clj_surgeon_skill.clj

performance-regression-sentinel-test:
	bash test/performance_regression_sentinel_intent_test.sh
	clojure $(MCP_JAVA_OPTS) -Sdeps '{:paths ["bench"]}' -M -e \
	  '(require (quote clojure.test) \
	            (quote performance-regression-sentinel-test) \
	            (quote performance-regression-sentinel-io-test)) \
	   (let [result (clojure.test/run-tests \
	                  (quote performance-regression-sentinel-test) \
	                  (quote performance-regression-sentinel-io-test))] \
	     (when (pos? (+ (:fail result) (:error result))) \
	       (System/exit 1)))'
	bash test/performance_regression_sentinel_runner_test.sh

worktree-lifecycle-test:
	clojure $(MCP_JAVA_OPTS) -Sdeps '{:paths ["src" "test"]}' -M -e \
	  '(require (quote clojure.test) \
	            (quote clj-surgeon.worktree-lifecycle-test) \
	            (quote clj-surgeon.worktree-lifecycle-io-test) \
	            (quote clj-surgeon.worktree-lifecycle-prune-test) \
	            (quote clj-surgeon.worktree-lifecycle-cli-test)) \
	   (let [result (clojure.test/run-tests \
	                  (quote clj-surgeon.worktree-lifecycle-test) \
	                  (quote clj-surgeon.worktree-lifecycle-io-test) \
	                  (quote clj-surgeon.worktree-lifecycle-prune-test) \
	                  (quote clj-surgeon.worktree-lifecycle-cli-test))] \
	     (when (pos? (+ (:fail result) (:error result))) \
	       (System/exit 1)))'

worktree-lifecycle-recovery-test:
	clojure $(MCP_JAVA_OPTS) -Sdeps '{:paths ["src" "test"]}' -M -e \
	  '(require (quote clojure.test) \
	            (quote clj-surgeon.worktree-lifecycle-recovery-test)) \
	   (let [result (clojure.test/run-tests \
	                  (quote clj-surgeon.worktree-lifecycle-recovery-test))] \
	     (when (pos? (+ (:fail result) (:error result))) \
	       (System/exit 1)))'

worktree-audit:
	@clojure $(MCP_JAVA_OPTS) -M -m clj-surgeon.worktree-lifecycle-io audit "$(OUTPUT)"

handoff-worktree:
	@test -n "$(REQUEST)" || { echo "REQUEST=/absolute/close-request.edn is required"; exit 2; }
	@clojure $(MCP_JAVA_OPTS) -M -m clj-surgeon.worktree-lifecycle-io handoff "$(REQUEST)"

finish-worktree:
	@if [ "$(APPLY)" = "1" ]; then \
	  test -n "$(PLAN)" || { echo "PLAN=/absolute/path.edn is required with APPLY=1"; exit 2; }; \
	  test -z "$(REQUEST)" || { echo "REQUEST is not accepted with APPLY=1"; exit 2; }; \
	  clojure $(MCP_JAVA_OPTS) -M -m clj-surgeon.worktree-lifecycle-io apply "$(PLAN)"; \
	else \
	  test -n "$(REQUEST)" || { echo "REQUEST=/absolute/close-request.edn is required for dry-run"; exit 2; }; \
	  test -z "$(PLAN)" || { echo "PLAN requires APPLY=1"; exit 2; }; \
	  clojure $(MCP_JAVA_OPTS) -M -m clj-surgeon.worktree-lifecycle-io plan "$(REQUEST)"; \
	fi

retain-benchmark-result:
	@test -n "$(RESULT_DIR)" || { echo "RESULT_DIR is required"; exit 2; }
	bash bench/retain_benchmark_result.sh "$(RESULT_DIR)"

verify-benchmark-retention:
	bash bench/retain_benchmark_result.sh --verify-tracked

benchmark-retention-self-test:
	bash bench/retain_benchmark_result.sh --self-test

verify-benchmark-evidence:
	bb bench/verify_evidence_manifest.clj

# ============================================================
# Tree-scale memory battery (MCP-OP-MEM-011)
#
# Minutes-scale measurement. It is deliberately NOT a prerequisite of `test`,
# `test-fast`, or `mcp-test`; clj-surgeon.memory-battery-test asserts that and
# fails loudly if the target disappears or is wired into a fast gate.
# See docs/memory-battery.md.
# ============================================================
MEMBAT_ROOT ?= /home/forge/tmp/membat
MEMBAT_XMX ?= 512m
MEMBAT_REFERENCE_XMX ?= 4g
MEMBAT_REPS ?= 5
MEMBAT_SCALES ?= 100,1000,10000
MEMBAT_OP_TIMEOUT_MS ?= 600000
# require (default): a stale/missing cached reference REFUSES rather than
# silently launching the 4g reference JVM as a side effect of `make
# memory-battery` — that build must be an explicit, visible act. auto: restore
# the old rebuild-on-stale behavior. See docs/memory-battery.md.
MEMBAT_REFERENCE ?= require
MEMBAT_HEAD_SHA = $(shell git rev-parse HEAD 2>/dev/null || echo unknown)
MEMBAT_ENV = MEMBAT_ROOT="$(MEMBAT_ROOT)" MEMBAT_REPS="$(MEMBAT_REPS)" \
  MEMBAT_SCALES="$(MEMBAT_SCALES)" MEMBAT_OP_TIMEOUT_MS="$(MEMBAT_OP_TIMEOUT_MS)" \
  MEMBAT_HEAD_SHA="$(MEMBAT_HEAD_SHA)"

memory-battery-generate:
	bb bench/memory_battery/generate_tree.clj --root "$(MEMBAT_ROOT)" --scales "$(MEMBAT_SCALES)"

memory-battery-reference: memory-battery-generate
	@# The unbounded reference outputs every bounded run must reproduce exactly.
	$(MEMBAT_ENV) MEMBAT_MODE=reference MEMBAT_REPS=1 \
	  clojure -J-Xmx$(MEMBAT_REFERENCE_XMX) -M:clj-surgeon/memory-battery

memory-battery-attest:
	@# Seconds-scale: is the cached unbounded reference bound to THIS code,
	@# generator, corpus and JVM? Exits non-zero when it is not, so the battery
	@# can rebuild it up front instead of refusing minutes in.
	@$(MEMBAT_ENV) MEMBAT_MODE=attest \
	  clojure -J-Xmx256m -M:clj-surgeon/memory-battery

memory-battery:
	@# @spec MCP-OP-MEM-011
	@$(MAKE) --no-print-directory memory-battery-generate
	@# Existence is not attestation: a shared MEMBAT_ROOT can hold a reference
	@# built from other code over another corpus, or an unanchored/hand-edited
	@# one. MEMBAT_REFERENCE gates what happens when it is stale: "auto"
	@# rebuilds it (a 4g JVM, minutes-scale) as this target's own side effect;
	@# the default "require" refuses, typed, so that JVM only ever starts from
	@# an explicit `make memory-battery-reference`.
	@$(MAKE) --no-print-directory memory-battery-attest || \
	  if [ "$(MEMBAT_REFERENCE)" = "auto" ]; then \
	    $(MAKE) --no-print-directory memory-battery-reference; \
	  elif [ "$(MEMBAT_REFERENCE)" = "require" ]; then \
	    echo "REFUSED: {:reason :membat-reference-required, :configured \"$(MEMBAT_REFERENCE)\", :detail \"cached reference is stale or missing\", :remedy \"run 'make memory-battery-reference' explicitly, or set MEMBAT_REFERENCE=auto to rebuild it as a side effect of this target\"}" >&2; \
	    exit 2; \
	  else \
	    echo "REFUSED: {:reason :membat-reference-invalid, :configured \"$(MEMBAT_REFERENCE)\", :detail \"MEMBAT_REFERENCE must be auto or require\"}" >&2; \
	    exit 2; \
	  fi
	$(MEMBAT_ENV) MEMBAT_MODE=battery \
	  clojure -J-Xmx$(MEMBAT_XMX) -M:clj-surgeon/memory-battery

# ============================================================
# The relation-census review battery (MCP-OP-CENSUS-014 …)
# ============================================================
# Round twenty-one's reviewer could not check the builder's battery figure,
# because there was no target to run: the COMPOSITION lived in
# `test/census_witness_battery.clj` and the RUN lived in a sentence, so
# `grep -rn "census battery"` over docs/ and logs/ found nothing and the
# reviewer built their own battery over five namespaces instead. Two batteries
# with different memberships produce two numbers that cannot be compared, and
# a figure nobody else can reproduce is not a receipt.
#
# So the run is a target. `clojure -Spath` and then `clojure.main` directly,
# rather than `-M:clj-surgeon/mcp-test`, because that alias's `:main-opts`
# name the suite runner: the battery is driven over the same classpath, not
# through the alias's entry point.
#
# It prints MISSING (a var whose name changed is reported, never silently
# dropped from the count), then one line per witness, then :BATTERY-RESULT.
# Exits non-zero on any missing var, failure or error.
census-battery:
	java -cp "$$(clojure -Spath -M:clj-surgeon/mcp-test)" \
	  clojure.main -m census-witness-battery

memory-battery-self-test:
	@# Millisecond-scale. Proves the generator is deterministic, the verdict
	@# applies the published pass lines exactly, and the battery is absent from
	@# every fast gate. This one IS wired into `make test`.
	bb bench/memory_battery/generate_tree.clj --self-test
	bb -e "(require 'clj-surgeon.memory-battery-test 'clojure.test) (let [r (clojure.test/run-tests 'clj-surgeon.memory-battery-test)] (System/exit (+ (:fail r) (:error r))))"

txn-kernel-warning-check:
	@# @spec MCP-OP-MEM-020
	@# Warnings as errors for the transaction kernel: zero reflection, zero
	@# boxed math. Reflective confinement ran twice per staged file and the
	@# boxed arithmetic runs once per admitted file; neither shows up in a
	@# passing suite. Seconds-scale, so it rides mcp-test.
	clojure -M test/kernel_warning_check.clj

# ============================================================
# TEST-ISO-001 -- the JVM test lanes
# ============================================================
# Round one measured `clojure -M:clj-surgeon/mcp-test` at 716.7 s and found
# ELEVEN namespaces that launch cold JVM/bb/CLI children are 674.0 s of it
# (94%), while the other 36 finish 865 tests' worth of work in 20.9 s. These
# targets are that partition. Which namespace is in which lane is declared in
# ONE place, `test/clj_surgeon/lane_manifest.clj`, and the runner refuses a
# namespace that declares none.
#
# RENAME, 2026-09-04, stated loudly because a silent one is the memory-red
# collision all over again: `test-fast` USED TO MEAN `bb test/run_all.clj`.
# It now means the JVM FAST LANE. The babashka lane is unchanged in content
# and moved to `make test-bb`; `make test` runs both. Docs written before
# this date that say "make test-fast (647 tests)" are quoting the bb lane.
test-fast:
	@# @spec TEST-ISO-001
	@# @spec MCP-OP-TMPHYG-001
	@# @spec MCP-OP-TMPHYG-002
	clojure $(MCP_JAVA_OPTS) -M:clj-surgeon/test-fast

test-integration:
	@# @spec TEST-ISO-001
	clojure $(MCP_JAVA_OPTS) -M:clj-surgeon/test-integration

# The eleven. Minutes-scale (~674 s), deliberately OUT of the merge gate.
#
# Taking it out of the merge gate is the point of the partition AND the risk:
# a gate that does not run on every merge is a gate whose ABSENCE is silent.
# So every run appends a receipt to docs/observations/battery-ledger.edn --
# pass or fail, one line, append-only. The RUNNER writes the file; the SEAT
# commits it. `make battery-fresh` is the tripwire that reads it back.
test-battery:
	@# @spec TEST-ISO-001
	@# @spec TEST-ISO-009a
	@started=$$(date -u +%Y-%m-%dT%H:%M:%SZ); t0=$$(date +%s); \
	 clojure $(MCP_JAVA_OPTS) -M:clj-surgeon/test-battery; rc=$$?; \
	 t1=$$(date +%s); verdict=pass; [ $$rc -eq 0 ] || verdict=fail; \
	 bb test/clj_surgeon/battery_ledger.clj append \
	    --sha "$$(git rev-parse HEAD)" --started "$$started" \
	    --wall-s "$$((t1 - t0))" --verdict "$$verdict"; \
	 exit $$rc

# The freshness tripwire. Refuses when the newest battery receipt is older
# than 26 h, failed, or names a commit that is not an ancestor of HEAD or is
# more than 30 commits behind it -- and prints the exact remedy. A stale
# nightly is a refusal, not a silence.
battery-fresh:
	@# @spec TEST-ISO-009b
	@bb test/clj_surgeon/battery_ledger.clj check

test-bb:
	@# @spec MCP-OP-TMPHYG-001
	@# @spec MCP-OP-TMPHYG-002
	bb test/run_all.clj

# ============================================================
# TEST-ISO-009b -- THE LANDING GATE. This is the target `~/bin/land` runs.
# ============================================================
# The round-three landing review's finding 2, verbatim: "`make battery-fresh`
# exists, but neither `land` nor `make mcp-test` invokes it, and `land` does
# not run `test-battery`. Thus the eleven namespaces removed from the merge
# gate are not mechanically required before this landing."
#
# That is the whole risk of the partition in one sentence. Moving 510 of 957
# tests off `make mcp-test` is only safe if SOMETHING on the landing path
# refuses when those tests have not run recently on this tree -- otherwise the
# gate simply got faster by covering less, and nothing on the screen says so.
# A tripwire that no path invokes is a diary entry, not an alarm.
#
# THE FRESHNESS CHECK RUNS FIRST, and it is a second of `bb`. A stale receipt
# refuses before seven minutes of JVM, so the remedy arrives while the seat is
# still looking at the screen.
#
# `~/bin/land` must run exactly this target. It is the single name to change
# when the landing gate's contents change, so the seat tool never drifts from
# what the repository considers a landing.
landing-gate:
	@# @spec TEST-ISO-009b
	@# @spec TEST-ISO-001
	$(MAKE) --no-print-directory battery-fresh
	$(MAKE) --no-print-directory mcp-test
	$(MAKE) --no-print-directory test-bb
	$(MAKE) --no-print-directory repository-hygiene

# ============================================================
# TEST-ISO-009 -- the concurrency battery (the spike's merge gate)
# ============================================================
# N real `git clone`s of the tip, all running `make mcp-test` at once. Passes
# only if ALL N are 0 failures and 0 errors. One clean copy beside one failing
# copy is a FAILURE, never a 50% pass -- round one saw exactly that twice, and
# a gate that averaged them would have called a scheduler race healthy.
N ?= 4

suite-concurrency-battery:
	@# @spec TEST-ISO-009
	@N=$(N) bash test/suite_concurrency_battery.sh

# ============================================================
# MEM-005 parser-admission red witness (heavy; NOT in make test)
# ============================================================
# Isolates the memory battery's two adversarial SHAPE findings to one
# `outline-source` call per JVM, each at an explicit -Xmx, so the defect is
# reproducible in seconds. Takes the exclusive suite lock, like the battery:
# it measures heap and must not share a box lane with another JVM suite.
PARSER_RED_ROOT ?= /home/forge/tmp/admit/parser-red
PARSER_RED_EXPECT ?= red

memory-red:
	@flock /home/forge/tmp/suite.lock \
	  bb bench/parser_admission/red_witness.clj --root "$(PARSER_RED_ROOT)" \
	     --expect "$(PARSER_RED_EXPECT)"

# ============================================================
# MEM-020 transaction-kernel memory witness (heavy; NOT in make test)
# ============================================================
# The memory namespaces are deliberately outside test-fast and mcp-test: they
# spawn child JVMs at explicit heap ceilings, write hundreds of megabytes of
# synthetic scope, and cost minutes of wall. This target carries the whole
# red-to-green history: the frozen read dies of OutOfMemoryError at -Xmx256m on
# a 600-file scope every ceiling admits, the same arm completes when the scope
# fits, and the same scenario at the same ceiling completes through the
# transaction journal with output parity against the unbounded reference.
MEMORY_JAVA_OPTS ?= -J-Xms64m -J-Xmx512m

memory-red-kernel:
	@# The TRANSACTION KERNEL's OOM witness (bridge/txn-journal). It arrived as
	@# a second target literally named `memory-red`, colliding with the
	@# parser-admission red witness above: two different meters, one name, and
	@# whichever recipe parsed last would have silently answered for both. Their
	@# GO evidence even quotes different numbers -- parser-admission "memory-red
	@# 6/6", the kernel "memory-red RED OOM -> GREEN, heap-used-peak 253-254 MB
	@# at 256m". Renamed rather than merged, because they measure different
	@# things. Keeps the exclusive suite.lock: it is a heap measurement.
	@flock /home/forge/tmp/suite.lock \
	  clojure $(MEMORY_JAVA_OPTS) -M:clj-surgeon/memory-test

analyzer-contract-test:
	@# @spec MCP-OP-ANALYZER-008
	clojure $(MCP_JAVA_OPTS) -M:clj-surgeon/analyzer-contract-test

analyzer-contract-target-self-test:
	@sh test/analyzer_contract_target_test.sh

test:
	@# @spec TEST-ISO-001
	@# The default is the landing gate (Gene 2026-09-05: "Tests need to be faster. Integrate
	@# the 2.5m changes immediately"). REAL COVERAGE of the default: battery-fresh (reads the
	@# battery-ledger receipt; it does NOT rerun the battery), mcp-test, test-bb, repository-hygiene.
	@# It OMITS analyzer-contract-test, the admit recovery battery, mcp-smoke, the memory battery and
	@# the bench self-test tail. Those run in `test-full` (CI/nightly). A stale battery receipt fails here.
	@# @spec MCP-OP-ADMIT-150
	@# The default lane OWNS the transaction-recovery battery receipt: the fast lane counts its
	@# absence as a named skip, and this line is what drives that bucket to zero (sub-second arms).
	$(MAKE) --no-print-directory admit-transaction-recovery-battery
	$(MAKE) --no-print-directory landing-gate

test-full:
	$(MAKE) --no-print-directory check-clj-surgeon-skill-mirrors
	$(MAKE) --no-print-directory repository-hygiene
	$(MAKE) --no-print-directory test-bb
	$(MAKE) --no-print-directory analyzer-contract-test
	# @spec MCP-OP-ADMIT-150
	# The lane that OWNS the recovery battery's receipt. `mcp-test` must not
	# depend on a busy-spinning timing bound (a flake there would report `the
	# enumeration claims kinds no fixture drives` and take the enumeration
	# proof down for an unrelated reason), and a fresh clone must not go red
	# on a gitignored artefact it cannot produce -- so the fast lane COUNTS
	# the absence as a named skip and this lane drives that bucket to zero.
	$(MAKE) --no-print-directory admit-transaction-recovery-battery
	$(MAKE) --no-print-directory mcp-test
	$(MAKE) --no-print-directory test-battery
	$(MAKE) --no-print-directory mcp-smoke
	$(MAKE) --no-print-directory memory-battery-self-test
	python3 skills/study-agent-usage/scripts/collect_agent_usage.py --self-test
	bb bench/initialize_benchmark_workspace.clj --self-test
	bb bench/verify_edit_portfolio.clj --self-test
	bb bench/verify_edit_portfolio.clj bench/fixtures/edit_portfolio
	bb bench/summarize_clean_codex.clj --self-test
	bb bench/score_ops_registry.clj --self-test
	BENCH_SCHEDULE_SELF_TEST=true bash bench/run_clean_codex.sh
	BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_codex.sh
	$(MAKE) --no-print-directory benchmark-anvil-portfolio-pair-self-test
	$(MAKE) --no-print-directory benchmark-inspect-mcp-self-test
	CLAUDE_BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_claude.sh
	bash bench/retain_benchmark_result.sh --self-test
	bash bench/retain_benchmark_result.sh --verify-tracked
	bb bench/verify_evidence_manifest.clj --self-test
	bb bench/verify_evidence_manifest.clj

outline:
	bb -m clj-surgeon.core :op :outline :file $(FILE)

study-agent-usage:
	@python3 skills/study-agent-usage/scripts/collect_agent_usage.py --pretty $(AGENT_USAGE_ARGS)

study-agent-events:
	@# TELEMETRY-EVENTS-001 -- the box-wide ledger the public MCP fns append to.
	@python3 skills/study-agent-usage/scripts/collect_agent_usage.py --events $(AGENT_EVENTS_FILE)

study-agent-timeline:
	@test -n "$(RECEIPT)" || { echo "RECEIPT is required"; exit 2; }
	@python3 skills/study-agent-usage/scripts/collect_agent_usage.py --render-receipt "$(RECEIPT)" $(AGENT_TIMELINE_ARGS)

study-agent-read-chains:
	@test -n "$(RECEIPT)" || { echo "RECEIPT is required"; exit 2; }
	@python3 skills/study-agent-usage/scripts/collect_agent_usage.py --render-read-chains "$(RECEIPT)" $(AGENT_READ_CHAIN_ARGS)

study-agent-usage-self-test:
	@python3 skills/study-agent-usage/scripts/collect_agent_usage.py --self-test

# fanout4-opus-review.md finding 11 (non-blocking): before this target existed,
# --selftest-list was a printed roster, not a gate -- a self-test mode could be
# added and never actually run.  This iterates the roster the roster itself
# derives from sabotage-FAN.sh's own dispatch (never a hand-maintained list) and
# fails the target on any mode's non-zero rc.
fanout-selftests:
	@set -eu; \
	modes=$$(bash bench/fanout/sabotage-FAN.sh --selftest-list | tail -n +2); \
	fail=0; ran=0; \
	for m in $$modes; do \
		ran=$$((ran + 1)); \
		echo "=== fanout-selftests: $$m ==="; \
		if bash bench/fanout/sabotage-FAN.sh "$$m"; then \
			echo "=== fanout-selftests: $$m PASS ==="; \
		else \
			rc=$$?; \
			echo "=== fanout-selftests: $$m FAIL (rc=$$rc) ==="; \
			fail=1; \
		fi; \
	done; \
	if [ "$$fail" -ne 0 ]; then \
		echo "fanout-selftests: FAILED ($$ran modes run)"; \
		exit 1; \
	fi; \
	echo "fanout-selftests: all $$ran modes passed"
