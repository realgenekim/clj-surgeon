CLJ_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

# Pick an install location already on the user's $PATH (see script/find_install_dir.bb).
# Override with: make install INSTALL_DIR=/some/path
INSTALL_DIR ?= $(shell bb $(CLJ_SURGEON_HOME)script/find_install_dir.bb)

.PHONY: test outline help install nrepl

help:
	@echo "clj-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test              Run all tests"
	@echo "  make install           Install clj-surgeon to a bin directory on \$$PATH"
	@echo "                         (override with: make install INSTALL_DIR=/path/to/bin)"
	@echo "  make nrepl             Start bb nREPL"
	@echo ""
	@echo "Direct usage:"
	@echo "  bb -m clj-surgeon.core :op :outline :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :declares :file src/my/ns.clj"
	@echo "  bb -m clj-surgeon.core :op :mv :file f :form foo :before bar"
	@echo "  bb -m clj-surgeon.core :op :rename-ns :from old :to new :root ."

install:
	@if [ -z "$(INSTALL_DIR)" ]; then \
	  echo "Error: could not find a writable bin directory under \$$HOME on \$$PATH."; \
	  echo "Create one (e.g. 'mkdir -p ~/.local/bin' and add it to \$$PATH),"; \
	  echo "or override: make install INSTALL_DIR=/path/to/bin"; \
	  exit 1; \
	fi
	@mkdir -p "$(INSTALL_DIR)"
	@echo '#!/usr/bin/env bb' > $(INSTALL_DIR)/clj-surgeon
	@echo '(require (quote [babashka.classpath :as cp]))' >> $(INSTALL_DIR)/clj-surgeon
	@echo '(cp/add-classpath "$(CLJ_SURGEON_HOME)src")' >> $(INSTALL_DIR)/clj-surgeon
	@echo '(require (quote [clj-surgeon.core :as core]))' >> $(INSTALL_DIR)/clj-surgeon
	@echo '(apply core/-main *command-line-args*)' >> $(INSTALL_DIR)/clj-surgeon
	@chmod +x $(INSTALL_DIR)/clj-surgeon
	@echo "Installed $(INSTALL_DIR)/clj-surgeon"

nrepl:
	cd $(CLJ_SURGEON_HOME) && bb nrepl-server 0

test:
	bb test/run_all.clj

outline:
	bb -m clj-surgeon.core :op :outline :file $(FILE)
