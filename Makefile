CLJ_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
CLI_DEST ?= $(HOME)/bin/clj-surgeon
CODEX_HOME ?= $(HOME)/.codex
CLAUDE_HOME ?= $(HOME)/.claude
INSTALL_ROOT ?= $(HOME)/.local/share/clj-surgeon
CONTROL_PLANE_ROOT_FILE ?= $(INSTALL_ROOT)/control-plane-root
SKILL_SOURCE := $(CLJ_SURGEON_HOME)skills/clj-surgeon
AGENT_ROUTING_SOURCE := $(CLJ_SURGEON_HOME)resources/clj-surgeon-agent-routing.md
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

.PHONY: test runtests mcp-test mcp-operation-oracle mcp-smoke mcp-serve mcp-serve-benchmark mcp-reload mcp-heap-config-self-test cclsp-start cclsp-start-self-test cclsp-stop cclsp-status workspace-mcp-start workspace-mcp-stop workspace-mcp-status workspace-mcp-onboard workspace-mcp-install-codex install-mcp-codex-dev uninstall-mcp-codex-dev outline help install install-cli install-codex-skill install-claude-skill install-agent-routing check-agent-routing prepare-cli-package prepare-skill-package install-dev install-dev-cli install-dev-codex-skill install-dev-claude-skill sync-clj-surgeon-skill check-clj-surgeon-skill-mirrors nrepl study-agent-usage study-agent-usage-self-test benchmark-clean-codex benchmark-edit-portfolio benchmark-edit-portfolio-self-test benchmark-anvil-compiled-edit-canary benchmark-anvil-public-cfp-cleanup benchmark-anvil-format-extraction benchmark-anvil-portfolio-pair benchmark-anvil-portfolio-pair-self-test benchmark-inspect-mcp benchmark-inspect-mcp-self-test benchmark-codex-skill benchmark-claude-skill benchmark-agent-skills benchmark-codex-skill-self-test benchmark-claude-skill-self-test benchmark-agent-skills-self-test clj-surgeon-skill-self-test retain-benchmark-result verify-benchmark-retention benchmark-retention-self-test verify-benchmark-evidence

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test                      Run all tests"
	@echo "  make mcp-test                  Run focused JVM MCP contract and hot-reload tests"
	@echo "  make mcp-smoke                 Verify initialize, four-tool discovery, and refusal over stdio"
	@echo "  make mcp-serve                 Start persistent HTTP MCP with full local telemetry and nREPL"
	@echo "  make mcp-serve-benchmark       Start persistent HTTP MCP without nREPL"
	@echo "  make mcp-reload                Reload live Clojure and publish changed tool schemas"
	@echo "  make cclsp-start               Start branch-live cclsp + clojure-lsp provider"
	@echo "  make install-mcp-codex-dev     Install branch-live tools, start MCP, and register it with Codex"
	@echo "  make mcp-status                Check both hot MCPs, nREPL, and Codex registration"
	@echo "  make workspace-mcp-onboard WORKSPACE=/repo  Compatibility alias for clj-surgeon up"
	@echo "  make workspace-mcp-status WORKSPACE=/repo   Verify the shared stack and local config"
	@echo "  make uninstall-mcp-codex-dev   Remove Codex registration and stop the local MCP"
	@echo "  make install                   Stable copied CLI, both skills, and global routing instructions"
	@echo "  make install-cli               Install only the stable copied CLI"
	@echo "  make install-codex-skill       Install only the stable copied Codex skill"
	@echo "  make install-claude-skill      Install only the stable copied Claude skill"
	@echo "  make install-agent-routing     Install compact routing into Codex and Claude globals"
	@echo "  make check-agent-routing       Verify both global routing blocks without writing"
	@echo "  make sync-clj-surgeon-skill    Regenerate Claude/root mirrors from the canonical skill"
	@echo "  make install-dev               Branch-live CLI and skill links (development only)"
	@echo "  make nrepl                     Start bb nREPL"
	@echo "  make study-agent-usage         Join agent routes with Surgeon, cclsp, and LSP telemetry"
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

install-agent-routing:
	bb --classpath "$(CLJ_SURGEON_HOME)src" -m clj-surgeon.agent-routing install "$(AGENT_ROUTING_SOURCE)" "$(CODEX_GLOBAL_INSTRUCTIONS)" "$(CLAUDE_GLOBAL_INSTRUCTIONS)"

check-agent-routing:
	bb --classpath "$(CLJ_SURGEON_HOME)src" -m clj-surgeon.agent-routing check "$(AGENT_ROUTING_SOURCE)" "$(CODEX_GLOBAL_INSTRUCTIONS)" "$(CLAUDE_GLOBAL_INSTRUCTIONS)"

sync-clj-surgeon-skill:
	bash bench/sync_clj_surgeon_skill.sh --write

check-clj-surgeon-skill-mirrors:
	bash bench/sync_clj_surgeon_skill.sh --check

mcp-operation-oracle:
	# @spec MCP-OP-ORACLE-001
	swipl -q -f test/mcp_operation_contract_oracle.pl

runtests: mcp-test

mcp-test: mcp-operation-oracle
	clojure $(MCP_JAVA_OPTS) -M:clj-surgeon/mcp-test
	@$(MAKE) --no-print-directory mcp-heap-config-self-test
	@$(MAKE) --no-print-directory cclsp-start-self-test

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
	    "(try (doseq [ns '[clj-surgeon.file-ops clj-surgeon.outline clj-surgeon.structural-lens clj-surgeon.owner-hypotheses clj-surgeon.show-form clj-surgeon.binding-rename clj-surgeon.intent-transaction clj-surgeon.diagnostic-delta clj-surgeon.extract-header clj-surgeon.quoted-var-refs clj-surgeon.extract clj-surgeon.mcp-paths clj-surgeon.mcp-workspace clj-surgeon.mcp-workspace-sources clj-surgeon.mcp-schema clj-surgeon.mcp-extraction clj-surgeon.mcp-contract clj-surgeon.mcp-extraction-plan clj-surgeon.mcp-semantic-client clj-surgeon.mcp-source-anchor clj-surgeon.mcp-process clj-surgeon.mcp-hot-verify clj-surgeon.mcp-cold-verify clj-surgeon.mcp-change-buffer clj-surgeon.mcp-formatter clj-surgeon.mcp-operation clj-surgeon.mcp-inspect clj-surgeon.mcp-inspect-tool clj-surgeon.mcp-program-tool clj-surgeon.mcp-tool clj-surgeon.mcp-server clj-surgeon.mcp-http-server]] (require ns :reload)) (let [result (clj-surgeon.mcp-server/sync-tools!)] (if (:ok result) result (throw (ex-info \"MCP tool synchronization failed\" result)))) (catch Throwable error {:ok false :error (.getMessage error) :class (.getName (class error))}))"); \
	  case "$$result" in *":ok true"*) ;; *) echo "$$result" >&2; exit 1 ;; esac; \
	  echo "$$result"; \
	  echo "Live handlers and server tool contracts reloaded at $(MCP_URL); the server process did not restart."; \
	  echo "Clients that honor tools/list_changed re-list automatically. The current Codex turn can cache model-visible schemas until a new session."

cclsp-start-self-test:
	@sh test/cclsp_start_test.sh
	@sh test/cclsp_launch_path_test.sh

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

install-dev: install-dev-cli install-dev-codex-skill install-dev-claude-skill install-agent-routing
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
	ANVIL_PAIR_CONFIG_SELF_TEST=true bash bench/run_anvil_portfolio_pair.sh \
		/tmp/clj-surgeon-anvil-pair-self-test decision-batch-edit compact-first 2
	ANVIL_PAIR_CONFIG_SELF_TEST=true bash bench/run_anvil_public_cfp_cleanup.sh \
		/tmp/clj-surgeon-public-cfp-self-test native-first 1
	ANVIL_FORMAT_CONFIG_SELF_TEST=true bash bench/run_anvil_format_extraction.sh \
		/tmp/clj-surgeon-format-extraction-self-test mcp-first 1

benchmark-inspect-mcp:
	bash bench/run_inspect_mcp_benchmark.sh

benchmark-inspect-mcp-self-test:
	BENCH_RESULT_DIR="$$(mktemp -d "$${TMPDIR:-/tmp}/clj-surgeon-inspect-self-test.XXXXXX")" \
	bash bench/run_inspect_mcp_benchmark.sh --self-test

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

retain-benchmark-result:
	@test -n "$(RESULT_DIR)" || { echo "RESULT_DIR is required"; exit 2; }
	bash bench/retain_benchmark_result.sh "$(RESULT_DIR)"

verify-benchmark-retention:
	bash bench/retain_benchmark_result.sh --verify-tracked

benchmark-retention-self-test:
	bash bench/retain_benchmark_result.sh --self-test

verify-benchmark-evidence:
	bb bench/verify_evidence_manifest.clj

test:
	$(MAKE) --no-print-directory check-clj-surgeon-skill-mirrors
	bb test/run_all.clj
	$(MAKE) --no-print-directory mcp-test
	$(MAKE) --no-print-directory mcp-smoke
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

study-agent-usage-self-test:
	@python3 skills/study-agent-usage/scripts/collect_agent_usage.py --self-test
