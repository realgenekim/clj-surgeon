CLJ_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

.PHONY: test outline help install nrepl

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test              Run all tests"
	@echo "  make install           Install to ~/bin/clj-surgeon"
	@echo "  make nrepl             Start bb nREPL"
	@echo ""
	@echo "Direct usage:"
	@echo "  bb -m clj-surgeon.core :op :ls :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ."
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ~/src.local/ :grep \"mail|imap\""
	@echo "  bb -m clj-surgeon.core :op :mv :file f :form foo :before bar"
	@echo "  bb -m clj-surgeon.core :op :rename-ns :from old :to new :root ."

install:
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

nrepl:
	cd $(CLJ_SURGEON_HOME) && bb nrepl-server 0

test:
	bb test/run_all.clj

outline:
	bb -m clj-surgeon.core :op :outline :file $(FILE)
