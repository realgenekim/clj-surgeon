CLJ_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
CODEX_HOME ?= $(HOME)/.codex
CODEX_SKILL_SOURCE := $(CLJ_SURGEON_HOME)skills/clj-surgeon
CODEX_SKILL_DEST := $(CODEX_HOME)/skills/clj-surgeon

.PHONY: test outline help install install-codex-skill nrepl

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test              Run all tests"
	@echo "  make install           Install CLI and Codex skill"
	@echo "  make install-codex-skill  Install only the Codex skill"
	@echo "  make nrepl             Start bb nREPL"
	@echo ""
	@echo "Direct usage:"
	@echo "  bb -m clj-surgeon.core :op :ls :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ."
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ~/src.local/ :grep \"mail|imap\""
	@echo "  bb -m clj-surgeon.core :op :mv :file f :form foo :before bar"
	@echo "  bb -m clj-surgeon.core :op :rename-ns :from old :to new :root ."

install: install-codex-skill
	@mkdir -p ~/bin
	@command -v bb >/dev/null 2>&1 || { \
	  echo "Warning: 'bb' (babashka) not found on PATH."; \
	  echo "  Install (no sudo): bash <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install) --dir ~/bin"; \
	  echo "  The shim will be written anyway, but won't run until bb is installed."; \
	}
	@command -v clj-kondo >/dev/null 2>&1 || { \
	  echo "Warning: 'clj-kondo' not found on PATH (required for :ls / :outline / :fix-declares!)."; \
	  echo "  Install (no sudo): bash <(curl -s https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo) --dir ~/bin"; \
	}
	@echo '#!/usr/bin/env bb' > ~/bin/clj-surgeon
	@echo '(require (quote [babashka.classpath :as cp]))' >> ~/bin/clj-surgeon
	@echo '(cp/add-classpath "$(CLJ_SURGEON_HOME)src")' >> ~/bin/clj-surgeon
	@echo '(require (quote [clj-surgeon.core :as core]))' >> ~/bin/clj-surgeon
	@echo '(apply core/-main *command-line-args*)' >> ~/bin/clj-surgeon
	@chmod +x ~/bin/clj-surgeon
	@echo "Installed ~/bin/clj-surgeon"

install-codex-skill:
	@mkdir -p "$(CODEX_HOME)/skills"
	@if [ -e "$(CODEX_SKILL_DEST)" ] && [ ! -L "$(CODEX_SKILL_DEST)" ]; then \
	  echo "Refusing to replace non-symlink $(CODEX_SKILL_DEST)"; \
	  exit 1; \
	fi
	@ln -sfn "$(CODEX_SKILL_SOURCE)" "$(CODEX_SKILL_DEST)"
	@echo "Installed Codex skill $(CODEX_SKILL_DEST) -> $(CODEX_SKILL_SOURCE)"

nrepl:
	cd $(CLJ_SURGEON_HOME) && bb nrepl-server 0

test:
	bb test/run_all.clj

outline:
	bb -m clj-surgeon.core :op :outline :file $(FILE)
