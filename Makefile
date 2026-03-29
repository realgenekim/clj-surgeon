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
	@echo "  bb -m clj-surgeon.core :op :outline :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :declares :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :mv :file f :form foo :before bar"
	@echo "  bb -m clj-surgeon.core :op :rename-ns :from old :to new :root ."

install:
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
