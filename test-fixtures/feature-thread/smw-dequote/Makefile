# Social Media Writer — Clojure Web App

.DEFAULT_GOAL := help

PORT ?= 15000
SERVER_PID_FILE ?= .smw-server.pid
SERVER_LABEL ?= com.social-media-writer.dev.$(PORT)
MOTHERSHIP_READER_PORT ?= 9998
MOTHERSHIP_READER_LABEL ?= com.social-media-writer.mothership.$(MOTHERSHIP_READER_PORT)
MOTHERSHIP_READER_PID_FILE ?= .mothership-reader.pid
INHERITED_JAVA_HOME := $(JAVA_HOME)
JAVA_RUNTIME_HOME ?= $(INHERITED_JAVA_HOME)
JAVA_RUNTIME_CMD ?= $(if $(JAVA_RUNTIME_HOME),$(JAVA_RUNTIME_HOME)/bin/java,$(shell command -v java 2>/dev/null))

export JAVA_HOME := $(JAVA_RUNTIME_HOME)
export JAVA_CMD := $(JAVA_RUNTIME_CMD)

# Add ~/bin to PATH for Clojure CLI tools
export PATH := $(HOME)/bin:$(PATH)




















































































































































































	clojure -X writer.editorial-packet/print! :project-id '"$(CEO_FORUM_PROJECT_ID)"' :section-id '"$(NODE_ID)"'

# Refresh the source-hashed compact Book Map + structural Part contexts used by
# :part and :section AI operations. Reads durable Book files; never mutates prose.
.PHONY: forum-ceo-paper-contexts
forum-ceo-paper-contexts:
	clojure -X writer.context-cli/generate! :project-id '"$(CEO_FORUM_PROJECT_ID)"' :model :codex-sol

# Audit exactly what the configured CEO-paper Compile would include. Reports
# per-node prose word counts and fails when compiled nodes look like notes,
# outlines, fact files, editorial tasks, or placeholders. Never writes prose.
.PHONY: ceo-project-node-scan
ceo-project-node-scan:
	clojure -M bin/ceo_project_node_scan.clj --project-id "$(CEO_FORUM_PROJECT_ID)"

.PHONY: propose-editor-change
propose-editor-change:
	@test -n "$(BEFORE)" || (echo "BEFORE is required" >&2; exit 2)
	@test -n "$(AFTER)" || (echo "AFTER is required" >&2; exit 2)
	bin/propose-editor-change --before "$(BEFORE)" --after "$(AFTER)" $(if $(WHY),--why "$(WHY)")

# One-time backfill: recover accepted-edit {before/after/instruction} triples
# frozen in [:transform :history] across every drafts/**/current-session.edn
# and drafts/**/older/session-*.edn snapshot ring, dedupe globally, and land
# them as logs/events-backfill-2026-07-14.jsonl. Refuses if that file already
# exists — one-time semantics, never appended twice (bd social-media-writer-pq3).
.PHONY: backfill-transform-rounds
backfill-transform-rounds:
	clojure -X writer.transform-backfill/run!

# Fast feedback: JavaScript plus Clojure unit and fast tests. Expensive
# component, integration, and fault coverage remains in runtests-all.
.PHONY: test-js
test-js:
	@echo "Checking JS syntax..."
	@node -c resources/public/js/editorial-diff.js
	@node --test test/js/editorial_diff_test.js
	@node -c resources/public/js/datastar-runtime.js
	@node --test test/js/authoritative_editor_frame_test.js
	@node -c resources/public/js/editor-controller.js
	@node --test test/js/editor_controller_test.js
	@node -c resources/public/js/editor-commands.js
	@node --test test/js/draft_markdown_preview_test.js
	@node --test test/js/browser_runtime_classic_script_test.js
	@node -c resources/public/js/edge.js
	@node --test test/js/edge_test.js
	@node --test test/js/book_node_open_latency_test.js
	@node -c resources/public/js/outline-nav.js
	@node --test test/js/outline_nav_test.js
	@node --test test/js/distillery_outline_zoom_test.js
	@node --test test/js/book_deep_link_test.js
	@node --test test/js/file_document_editor_test.js
	@node --test test/js/smw_post_cli_test.js
	@node --test test/js/smw_write_test.js
	@node --test test/js/smw_write_cli_test.js
	@node --test test/js/smw_doc_cli_test.js
	@node --test test/js/draft_conflict_buttons_test.js
	@node --test test/js/editor_safety_recovery_test.js
	@node --test test/js/editor_conflict_quarantine_test.js
	@node --test test/js/editor_journal_settle_test.js
	@node --test test/js/editor_revision_monotonicity_test.js
	@node --test test/js/editor_conflict_response_test.js
	@node --test test/js/editor_durable_ack_test.js
	@node --test test/js/editor_reload_settle_test.js
	@node --test test/js/editor_cross_document_recovery_test.js
	@node --test test/js/editor_snapshot_refusal_test.js
	@node --test test/js/editor_journal_dead_test.js
	@node --test test/js/book_node_delete_test.js
	@node --test test/js/book_node_create_test.js
	@node --test test/js/smw_child_cli_test.js
	@node -c resources/public/js/smw-datastar-kit.js
	@node test/js/smw_datastar_kit_test.js
	@node -c resources/public/js/editor-safety.js
	@node -c resources/public/js/app-safe.js
	@node --test test/js/sse_disconnect_hysteresis_test.js
	@node -c resources/public/js/app-purgatory.js
	@node -c resources/public/js/app-state-doomed.js

.PHONY: runtests-once
runtests-once: test-js java-runtime-self-test
	clojure -M:test:run-tests unit fast 2>&1 | tee 00TESTLOG.txt

.PHONY: runtests-unit
runtests-unit:
	clojure -M:test:run-tests unit

.PHONY: runtests-integration
runtests-integration:
	clojure -M:test:run-tests integration 2>&1 | tee 00TESTLOG-integration.txt

.PHONY: runtests-component
runtests-component:
	clojure -M:test:run-tests component 2>&1 | tee 00TESTLOG-component.txt

.PHONY: runtests-fault
runtests-fault:
	clojure -M:test:run-tests fault 2>&1 | tee 00TESTLOG-fault.txt

.PHONY: runtests-all
runtests-all: test-js
	clojure -M:test:run-tests 2>&1 | tee 00TESTLOG.txt

.PHONY: runtests-profile
runtests-profile:
	clojure -M:test:run-tests unit fast component integration --profiling --profiling-count 10

# Run tests in watch mode
.PHONY: runtests
runtests:
	clojure -M:test:run-tests unit fast --watch

# ========================================
# Code Quality
# ========================================

# Format Clojure source code
.PHONY: format
format:
	npx @chrisoakman/standard-clojure-style fix src test deps.edn

# Import clj-kondo configs from dependency jars












































































































































































































































































































































