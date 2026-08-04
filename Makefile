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

.PHONY: test outline help install install-cli install-codex-skill install-claude-skill prepare-cli-package prepare-skill-package install-dev install-dev-cli install-dev-codex-skill install-dev-claude-skill nrepl benchmark-clean-codex benchmark-harness-self-test benchmark-codex-skill benchmark-claude-skill benchmark-agent-skills benchmark-codex-skill-self-test benchmark-claude-skill-self-test benchmark-agent-skills-self-test retain-benchmark-result verify-benchmark-retention benchmark-retention-self-test verify-benchmark-evidence

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test                      Run all tests"
	@echo "  make install                   Stable copied CLI, Codex skill, and Claude skill"
	@echo "  make install-cli               Install only the stable copied CLI"
	@echo "  make install-codex-skill       Install only the stable copied Codex skill"
	@echo "  make install-claude-skill      Install only the stable copied Claude skill"
	@echo "  make install-dev               Branch-live CLI and skill links (development only)"
	@echo "  make nrepl                     Start bb nREPL"
	@echo "  make benchmark-clean-codex     Run the 32-session clean Codex benchmark"
	@echo "  make benchmark-harness-self-test Test benchmark isolation without model calls"
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
	     ! grep -q '^;; clj-surgeon \(stable\|development\) launcher' "$$dest" && \
	     ! grep -qF '(require (quote [clj-surgeon.core :as core]))' "$$dest"; then \
	    echo "Refusing to replace unrelated file $$dest"; \
	    exit 1; \
	  fi; \
	  if [ -e "$$receipt" ] && { [ ! -f "$$receipt" ] || ! grep -q ':artifact :cli' "$$receipt"; }; then echo "Refusing to replace unrelated receipt $$receipt"; exit 1; fi; \
	  stage="$$dest.tmp.$$$$"; \
	  trap 'rm -f "$$stage"' EXIT HUP INT TERM; \
	  printf '%s\n' '#!/usr/bin/env bb' ';; clj-surgeon stable launcher' '(require (quote [babashka.classpath :as cp]))' '(cp/add-classpath "$(CLI_PACKAGE)/src")' '(require (quote [clj-surgeon.core :as core]))' '(apply core/-main *command-line-args*)' > "$$stage"; \
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
	     ! grep -q '^;; clj-surgeon \(stable\|development\) launcher' "$$dest" && \
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
	bb bench/summarize_clean_codex.clj --self-test
	bb bench/score_ops_registry.clj --self-test
	BENCH_SCHEDULE_SELF_TEST=true bash bench/run_clean_codex.sh
	BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_codex.sh
	CLAUDE_BENCH_HARNESS_SELF_TEST=true bash bench/run_clean_claude.sh
	bash bench/retain_benchmark_result.sh --self-test
	bash bench/retain_benchmark_result.sh --verify-tracked
	bb bench/verify_evidence_manifest.clj --self-test
	bb bench/verify_evidence_manifest.clj

outline:
	bb -m clj-surgeon.core :op :outline :file $(FILE)
