CLJ_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
DEST ?= ~/bin/clj-surgeon

.PHONY: test outline help install nrepl

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test              Run all tests"
	@echo "  make install           Install to $$DEST (default: ~/bin/clj-surgeon)"
	@echo "  make nrepl             Start bb nREPL"
	@echo ""
	@echo "Direct usage:"
	@echo "  bb -m clj-surgeon.core :op :ls :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ."
	@echo "  bb -m clj-surgeon.core :op :ls-tree :dir ~/src.local/ :grep \"mail|imap\""
	@echo "  bb -m clj-surgeon.core :op :mv :file f :form foo :before bar"
	@echo "  bb -m clj-surgeon.core :op :rename-ns :from old :to new :root ."

install:
	@echo '#!/usr/bin/env bb' > $(DEST)
	@echo '(require (quote [babashka.classpath :as cp]))' >> $(DEST)
	@echo '(cp/add-classpath "$(CLJ_SURGEON_HOME)src")' >> $(DEST)
	@echo '(require (quote [clj-surgeon.core :as core]))' >> $(DEST)
	@echo '(apply core/-main *command-line-args*)' >> $(DEST)
	@chmod +x $(DEST)
	@echo "Installed $(DEST)"

nrepl:
	cd $(CLJ_SURGEON_HOME) && bb nrepl-server 0

test:
	bb test/run_all.clj

outline:
	bb -m clj-surgeon.core :op :outline :file $(FILE)
