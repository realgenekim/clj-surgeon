.PHONY: test outline help

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

test:
	bb -e '(require (quote [clojure.test :refer [run-tests]]) (quote [ns-surgeon.outline-test]) (quote [ns-surgeon.move-test])) (let [r (run-tests (quote ns-surgeon.outline-test) (quote ns-surgeon.move-test))] (System/exit (+ (:fail r) (:error r))))'

outline:
	bb -m ns-surgeon.core :op :outline :file $(FILE)

fwd-refs:
	bb -m ns-surgeon.core :op :outline :file $(FILE) | bb -e '(let [d (read)] (doseq [f (:forward-refs d)] (println (format "  %-40s used at %4d, defined at %4d  (gap: %d)" (:name f) (:used-at f) (:defined-at f) (:gap f)))))'
