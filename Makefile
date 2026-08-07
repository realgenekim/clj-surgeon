CLJ_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
CLI_DEST ?= $(HOME)/bin/clj-surgeon
CODEX_HOME ?= $(HOME)/.codex
CLAUDE_HOME ?= $(HOME)/.claude
INSTALL_ROOT ?= $(HOME)/.local/share/clj-surgeon
SKILL_SOURCE := $(CLJ_SURGEON_HOME)skills/clj-surgeon
CODEX_SKILL_DEST := $(CODEX_HOME)/skills/clj-surgeon
CLAUDE_SKILL_DEST := $(CLAUDE_HOME)/skills/clj-surgeon
SOURCE_COMMIT := $(shell git -C "$(CLJ_SURGEON_HOME)" rev-parse HEAD 2>/dev/null || printf unknown)
CLI_SOURCE_HASH := $(shell cd "$(CLJ_SURGEON_HOME)" && { find src -type f -print; printf '%s\n' bb.edn deps.edn; } | LC_ALL=C sort | while IFS= read -r file; do shasum -a 256 "$$file"; done | shasum -a 256 | awk '{print $$1}')
SKILL_SOURCE_HASH := $(shell cd "$(SKILL_SOURCE)" && find . -type f -print | LC_ALL=C sort | while IFS= read -r file; do shasum -a 256 "$$file"; done | shasum -a 256 | awk '{print $$1}')
VERSION_ROOT := $(INSTALL_ROOT)/versions/$(SOURCE_COMMIT)
CLI_PACKAGE := $(VERSION_ROOT)/cli-$(CLI_SOURCE_HASH)
SKILL_PACKAGE := $(VERSION_ROOT)/skill-$(SKILL_SOURCE_HASH)

MCP_STATE_DIR ?= $(HOME)/.local/state/clj-surgeon/mcp
MCP_URL ?= http://127.0.0.1:7888/mcp
MCP_PID_FILE := $(MCP_STATE_DIR)/server.pid
MCP_READY_FILE := $(MCP_STATE_DIR)/ready.edn
MCP_LOG_FILE := $(MCP_STATE_DIR)/server.log
MCP_LAUNCH_LABEL ?= com.realgenekim.clj-surgeon-mcp
CLOJURE_BIN ?= $(shell command -v clojure)

.PHONY: test mcp-test mcp-smoke mcp-serve mcp-serve-benchmark mcp-start mcp-stop mcp-status install-mcp-codex-dev uninstall-mcp-codex-dev outline help install install-cli install-codex-skill install-claude-skill prepare-cli-package prepare-skill-package install-dev install-dev-cli install-dev-codex-skill install-dev-claude-skill nrepl study-agent-usage study-agent-usage-self-test benchmark-clean-codex benchmark-harness-self-test benchmark-edit-portfolio benchmark-edit-portfolio-self-test benchmark-inspect-mcp benchmark-inspect-mcp-self-test benchmark-codex-skill benchmark-claude-skill benchmark-agent-skills benchmark-codex-skill-self-test benchmark-claude-skill-self-test benchmark-agent-skills-self-test retain-benchmark-result verify-benchmark-retention benchmark-retention-self-test verify-benchmark-evidence

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test                      Run all tests"
	@echo "  make mcp-test                  Run focused JVM MCP contract and hot-reload tests"
	@echo "  make mcp-smoke                 Verify initialize, two-tool discovery, and refusal over stdio"
	@echo "  make mcp-serve                 Start persistent HTTP MCP with full local telemetry and nREPL"
	@echo "  make mcp-serve-benchmark       Start persistent HTTP MCP without nREPL"
	@echo "  make install-mcp-codex-dev     Install branch-live tools, start MCP, and register it with Codex"
	@echo "  make mcp-status                Check the local MCP process, endpoint, and Codex registration"
	@echo "  make uninstall-mcp-codex-dev   Remove Codex registration and stop the local MCP"
	@echo "  make install                   Stable copied CLI, Codex skill, and Claude skill"
	@echo "  make install-cli               Install only the stable copied CLI"
	@echo "  make install-codex-skill       Install only the stable copied Codex skill"
	@echo "  make install-claude-skill      Install only the stable copied Claude skill"
	@echo "  make install-dev               Branch-live CLI and skill links (development only)"
	@echo "  make nrepl                     Start bb nREPL"
	@echo "  make study-agent-usage         Compare Codex and Claude since the latest study marker"
	@echo "  make study-agent-usage-self-test Test the bounded cross-agent history collector"
	@echo "  make benchmark-clean-codex     Run the 32-session clean Codex benchmark"
	@echo "  make benchmark-harness-self-test Test benchmark isolation without model calls"
	@echo "  make benchmark-edit-portfolio  Compare representative edits across microscope/current/native"
	@echo "  make benchmark-edit-portfolio-self-test Verify edit capsules and harness without model calls"
	@echo "  make benchmark-inspect-mcp     Compare persistent inspect, CLI, and native reads"
	@echo "  make benchmark-inspect-mcp-self-test Verify the inspect harness without model calls"
	@echo "  make benchmark-codex-skill     Run the bounded 2-session Codex skill battery"
	@echo "  make benchmark-claude-skill    Run the bounded 4-session Fable/Opus skill battery"
	@echo "  make benchmark-agent-skills    Run both bounded clean-agent skill batteries"
	@echo "  make benchmark-agent-skills-self-test Test both skill harnesses without model calls"
	@echo "  make retain-benchmark-result RESULT_DIR=... Archive raw logs; retain structured evidence"
	@echo "  make verify-benchmark-retention Refuse tracked raw benchmark logs"
	@echo "  make verify-benchmark-evidence Verify archived evidence paths and hashes"
	@echo ""
	@echo "Installation overrides:"
	@echo "  CLI_DEST=/path/to/clj-surgeon  CLI path (default: $(CLI_DEST))"
	@echo "  CODEX_HOME=/path/to/.codex     Codex home (default: $(CODEX_HOME))"
	@echo "  CLAUDE_HOME=/path/to/.claude   Claude home (default: $(CLAUDE_HOME))"
	@echo "  INSTALL_ROOT=/path/to/packages Stable copied package root (default: $(INSTALL_ROOT))"
	@echo ""
	@echo "Direct usage:"
	@echo "  bb -m clj-surgeon.core :op :ls :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ."
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ~/src.local/ :grep \"mail|imap\""
	@echo "  bb -m clj-surgeon.core :op :mv :file f :form foo :before bar"
	@echo "  bb -m clj-surgeon.core :op :rename-ns :from old :to new :root ."

install: install-cli install-codex-skill install-claude-skill

mcp-test:
	clojure -M:clj-surgeon/mcp-test

mcp-smoke:
	bb test/mcp_stdio_smoke.clj

mcp-serve:
	clojure -X:clj-surgeon/mcp :telemetry :full

mcp-serve-benchmark:
	clojure -X:clj-surgeon/mcp :telemetry :full :nrepl-port :none :run-id '"$${RUN_ID:-manual}"'

mcp-start:
	@set -eu; \
	  mkdir -p "$(MCP_STATE_DIR)"; \
	  if curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))" >/dev/null 2>&1; then \
	    echo "clj-surgeon MCP already ready at $(MCP_URL)"; \
	    exit 0; \
	  fi; \
	  test -n "$(CLOJURE_BIN)" || { echo "clojure is required" >&2; exit 1; }; \
	  launchctl remove "$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1 || true; \
	  rm -f "$(MCP_READY_FILE)" "$(MCP_PID_FILE)"; \
	  launchctl submit -l "$(MCP_LAUNCH_LABEL)" \
	    -o "$(MCP_LOG_FILE)" -e "$(MCP_LOG_FILE)" -- \
	    /bin/sh -c 'cd "$$1"; shift; exec "$$@"' _ "$(CLJ_SURGEON_HOME)" \
	    "$(CLOJURE_BIN)" -X:clj-surgeon/mcp \
	    :project-dir '"$(CLJ_SURGEON_HOME)"' \
	    :telemetry :full \
	    :telemetry-dir '"$(MCP_STATE_DIR)/telemetry"' \
	    :run-id '"dogfood"' \
	    :ready-file '"$(MCP_READY_FILE)"' \
	    :port-file '"$(MCP_STATE_DIR)/nrepl-port"'; \
	  ready=false; \
	  for attempt in $$(seq 1 60); do \
	    if curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))" >/dev/null 2>&1; then ready=true; break; fi; \
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

mcp-status:
	@set -eu; \
	  if curl -fsS --max-time 1 "$(patsubst %/mcp,%/healthz,$(MCP_URL))"; then echo; else echo "MCP endpoint unavailable: $(MCP_URL)"; fi; \
	  if [ -f "$(MCP_READY_FILE)" ]; then echo "Readiness: $$(cat "$(MCP_READY_FILE)")"; fi; \
	  if launchctl print "gui/$$(id -u)/$(MCP_LAUNCH_LABEL)" >/dev/null 2>&1; then echo "Service: launchd job $(MCP_LAUNCH_LABEL) is loaded"; else echo "Service: not loaded"; fi; \
	  codex mcp get clj-surgeon 2>/dev/null || echo "Codex registration: absent"

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
	  printf '%s\n' '#!/bin/sh' '## clj-surgeon stable launcher' 'exec bb --classpath "$(CLI_PACKAGE)/src" -m clj-surgeon.core "$$@"' > "$$stage"; \
	  chmod +x "$$stage"; \
	  mv "$$stage" "$$dest"; \
	  trap - EXIT HUP INT TERM; \
	  printf '%s\n' '{:artifact :cli' ' :mode :stable-copy' ' :source-commit "$(SOURCE_COMMIT)"' ' :source-hash "$(CLI_SOURCE_HASH)"' ' :destination "$(CLI_DEST)"' ' :package "$(CLI_PACKAGE)"}' > "$$receipt"
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

install-dev: install-dev-cli install-dev-codex-skill install-dev-claude-skill
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
	cd $(CLJ_SURGEON_HOME) && bb nrepl-server 0

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
	bb bench/verify_edit_portfolio.clj --self-test
	bb bench/verify_edit_portfolio.clj bench/fixtures/edit_portfolio
	BENCH_SCHEDULE_SELF_TEST=true bash bench/run_clean_codex.sh
	BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_codex.sh

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
	$(MAKE) benchmark-codex-skill-self-test
	$(MAKE) benchmark-claude-skill-self-test

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
