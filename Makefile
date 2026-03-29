NS_SURGEON_HOME := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

.PHONY: test outline help install nrepl

help:
	@echo "ns-surgeon — structural operations on Clojure namespaces"
	@echo ""
	@echo "  make test              Run all tests"
	@echo "  make outline FILE=...  Show outline for a file"
	@echo "  make fwd-refs FILE=... Show forward references"
	@echo ""
	@echo "Direct usage:"
	@echo "  bb -m ns-surgeon.core :op :outline :file src/my/ns.clj"
	@echo "  bb -m ns-surgeon.core :op :mv :file src/my/ns.clj :form foo :before bar"
	@echo "  bb -m ns-surgeon.core :op :mv :file ... :form ... :before ... :dry-run true"

install:
	@echo '#!/usr/bin/env bb' > ~/bin/ns-surgeon
	@echo '(require (quote [babashka.classpath :as cp]))' >> ~/bin/ns-surgeon
	@echo '(cp/add-classpath "$(NS_SURGEON_HOME)src")' >> ~/bin/ns-surgeon
	@echo '(require (quote [ns-surgeon.core :as core]))' >> ~/bin/ns-surgeon
	@echo '(apply core/-main *command-line-args*)' >> ~/bin/ns-surgeon
	@chmod +x ~/bin/ns-surgeon
	@echo "Installed ~/bin/ns-surgeon"

nrepl:
	cd /Users/genekim/src.local/ns-surgeon && bb nrepl-server 0

test:
	bb -e '(require (quote [clojure.test :refer [run-tests]]) (quote [ns-surgeon.outline-test]) (quote [ns-surgeon.move-test])) (let [r (run-tests (quote ns-surgeon.outline-test) (quote ns-surgeon.move-test))] (System/exit (+ (:fail r) (:error r))))'

outline:
	bb -m ns-surgeon.core :op :outline :file $(FILE)

fwd-refs:
	bb -m ns-surgeon.core :op :outline :file $(FILE) | bb -e '(let [d (read)] (doseq [f (:forward-refs d)] (println (format "  %-40s used at %4d, defined at %4d  (gap: %d)" (:name f) (:used-at f) (:defined-at f) (:gap f)))))'
