from __future__ import annotations

import copy
import unittest

import evidence_adapter as adapter
import evidence_oracle as oracle


CATALOG = "1" * 64
STATIC = "2" * 64
POLICY = "3" * 64
DESCRIPTOR = {
    "name": "edit_clojure",
    "arguments": {"workspace_root": "/work/project", "edits": []},
}
CANDIDATE = oracle.canonical_argument_hash(DESCRIPTOR)
BEFORE_TREE = "4" * 64
AFTER_TREE = "5" * 64
OLD_FILE = "6" * 64
NEW_FILE = "7" * 64

IDENTITIES = {
    "catalog_sha256": CATALOG,
    "static_surface_sha256": STATIC,
    "candidate_policy_sha256": POLICY,
}


def codex_started(item_id: str, tool: str, arguments: dict) -> dict:
    return {
        "type": "item.started",
        "item": {
            "id": item_id,
            "type": "mcp_tool_call",
            "server": "clj-surgeon",
            "tool": tool,
            "arguments": copy.deepcopy(arguments),
        },
    }


def codex_completed(item_id: str, tool: str, arguments: dict) -> dict:
    return {
        "type": "item.completed",
        "item": {
            "id": item_id,
            "type": "mcp_tool_call",
            "server": "clj-surgeon",
            "tool": tool,
            "arguments": copy.deepcopy(arguments),
            "status": "completed",
        },
    }


def proxy_call(request_id: int, name: str, arguments: dict) -> dict:
    return {
        "event": "client_tool_call",
        "request_id": request_id,
        "name": name,
        "arguments": copy.deepcopy(arguments),
        "arguments_sha256": oracle.canonical_argument_hash(arguments),
    }


def proxy_result(
    request_id: int,
    name: str,
    arguments: dict,
    *,
    emitted: bool = False,
    ok: bool = True,
) -> dict:
    return {
        "event": "client_tool_result",
        "request_id": request_id,
        "name": name,
        "arguments_sha256": oracle.canonical_argument_hash(arguments),
        "is_error": not ok,
        "prepared_emitted": emitted,
        "prepared_request_sha256": CANDIDATE if emitted else None,
        "emitted_result": {
            "structuredContent": {
                "ok": ok,
                **({} if ok else {"reason": "refused-for-test"}),
                **({"prepared_request": copy.deepcopy(DESCRIPTOR)} if emitted else {}),
            }
        },
    }


def ready() -> dict:
    return {
        "event": "proxy_ready",
        "arm": "T",
        "offered_tool_list_sha256": CATALOG,
        "server_instructions_sha256": STATIC,
    }


def valid_raw_attempt() -> dict:
    inspect_arguments = {
        "workspace_root": "/work/project",
        "requests": [{"file": "src/example.clj", "forms": ["target"]}],
    }
    edit_arguments = {
        "workspace_root": "/work/project",
        "edits": [{
            "file": "src/example.clj",
            "within": {"form": "target"},
            "from": ":old",
            "to": ":new",
            "matches": 1,
        }],
    }
    return {
        "attempt_id": "E-T-1",
        "phase": "efficacy",
        "arm": "T",
        "workspace_root": "/work/project",
        "process_timed_out": False,
        "codex_events": [
            codex_started("inspect-item", "inspect_clojure", inspect_arguments),
            codex_completed("inspect-item", "inspect_clojure", inspect_arguments),
            codex_started("edit-item", "edit_clojure", edit_arguments),
            codex_completed("edit-item", "edit_clojure", edit_arguments),
        ],
        "proxy_rows": [
            ready(),
            proxy_call(10, "inspect_clojure", inspect_arguments),
            proxy_result(10, "inspect_clojure", inspect_arguments, emitted=True),
            proxy_call(11, "edit_clojure", edit_arguments),
            proxy_result(11, "edit_clojure", edit_arguments),
        ],
        "before_tree_sha256": BEFORE_TREE,
        "after_tree_sha256": AFTER_TREE,
        "changed_files": [{
            "change_type": "modified",
            "path": "src/example.clj",
            "to_path": None,
            "before_sha256": OLD_FILE,
            "after_sha256": NEW_FILE,
        }],
    }


def minimal_raw_attempt(*, phase: str = "efficacy") -> dict:
    return {
        "attempt_id": f"minimal-{phase}",
        "phase": phase,
        "arm": "T",
        "workspace_root": "/work/project",
        "process_timed_out": False,
        "codex_events": [],
        "proxy_rows": [ready()],
        "before_tree_sha256": BEFORE_TREE,
        "after_tree_sha256": BEFORE_TREE,
        "changed_files": [],
    }


def native_rollback_attempt(*, phase: str) -> dict:
    raw = minimal_raw_attempt(phase=phase)
    start = {
        "type": "item.started",
        "item": {
            "id": "native-change",
            "type": "file_change",
            "changes": [{
                "path": "/work/project/src/example.clj",
                "kind": "update",
            }],
        },
    }
    completion = copy.deepcopy(start)
    completion["type"] = "item.completed"
    completion["item"]["status"] = "completed"
    raw["codex_events"] = [start, completion]
    return raw


def assert_adapter_refusal(
    test: unittest.TestCase,
    code: str,
    raw: dict,
) -> None:
    with test.assertRaises(adapter.AdapterError) as raised:
        adapter.adapt_and_compile(raw, IDENTITIES)
    test.assertEqual(code, raised.exception.code)


class EvidenceAdapterTest(unittest.TestCase):
    def test_valid_raw_trace_builds_exact_oracle_input_and_report(self):
        raw = valid_raw_attempt()
        strict_input = adapter.build_oracle_input(raw, IDENTITIES)
        report = adapter.adapt_and_compile(raw, IDENTITIES)

        self.assertEqual(
            {"attempt_id", "phase", "arm", "identities", "events"},
            set(strict_input),
        )
        self.assertEqual("treatment", strict_input["arm"])
        self.assertEqual("edit-clojure", report["semantic_facts"]["first_mutation_route"])
        self.assertEqual(1, report["semantic_facts"]["prepared_request_exposure_count"])
        self.assertEqual(AFTER_TREE, report["semantic_facts"]["correctness_inputs"]["after_tree_sha256"])

    def test_clean_process_timeout_closes_without_inventing_an_action(self):
        raw = minimal_raw_attempt()
        raw["process_timed_out"] = True
        report = adapter.adapt_and_compile(raw, IDENTITIES)
        facts = report["semantic_facts"]
        self.assertEqual([], facts["tool_calls"])
        self.assertEqual("none", facts["first_mutation_route"])
        self.assertEqual(0, facts["mutation_attempt_count"])

    def test_latest_active_command_closes_as_timeout(self):
        raw = minimal_raw_attempt()
        raw["process_timed_out"] = True
        raw["codex_events"] = [{
            "type": "item.started",
            "item": {
                "id": "active-command",
                "type": "command_execution",
                "command": "clojure -M:test",
            },
        }]
        report = adapter.adapt_and_compile(raw, IDENTITIES)
        self.assertEqual("timeout", report["semantic_facts"]["tool_calls"][0]["outcome"])

    def test_latest_active_mcp_and_proxy_call_close_as_timeout(self):
        raw = minimal_raw_attempt()
        raw["process_timed_out"] = True
        arguments = {
            "workspace_root": "/work/project",
            "edits": [{
                "file": "src/example.clj",
                "within": {"form": "target"},
                "from": ":old",
                "to": ":new",
                "matches": 1,
            }],
        }
        raw["codex_events"] = [
            codex_started("active-edit", "edit_clojure", arguments)
        ]
        raw["proxy_rows"].append(proxy_call(99, "edit_clojure", arguments))

        report = adapter.adapt_and_compile(raw, IDENTITIES)
        facts = report["semantic_facts"]
        self.assertEqual("edit-clojure", facts["first_mutation_route"])
        self.assertEqual("timeout", facts["mutation_attempts"][0]["outcome"])
        self.assertFalse(any(
            attempt["outcome"] == "success" for attempt in facts["mutation_attempts"]
        ))

    def test_timeout_does_not_excuse_multiple_or_nonlatest_open_actions(self):
        raw = minimal_raw_attempt()
        raw["process_timed_out"] = True
        raw["codex_events"] = [
            {
                "type": "item.started",
                "item": {"id": "one", "type": "command_execution", "command": "one"},
            },
            {
                "type": "item.started",
                "item": {"id": "two", "type": "command_execution", "command": "two"},
            },
        ]
        assert_adapter_refusal(self, "multiple-open-codex-on-timeout", raw)

        raw = minimal_raw_attempt()
        raw["process_timed_out"] = True
        open_start = {
            "type": "item.started",
            "item": {"id": "open", "type": "command_execution", "command": "one"},
        }
        closed_start = {
            "type": "item.started",
            "item": {"id": "closed", "type": "command_execution", "command": "two"},
        }
        closed_completion = copy.deepcopy(closed_start)
        closed_completion["type"] = "item.completed"
        closed_completion["item"]["status"] = "completed"
        raw["codex_events"] = [open_start, closed_start, closed_completion]
        assert_adapter_refusal(self, "nonlatest-open-codex-on-timeout", raw)

    def test_native_mutate_then_rollback_preserves_efficacy_route(self):
        report = adapter.adapt_and_compile(
            native_rollback_attempt(phase="efficacy"),
            IDENTITIES,
        )
        facts = report["semantic_facts"]
        self.assertEqual("native-patch", facts["first_mutation_route"])
        self.assertEqual("success", facts["mutation_attempts"][0]["outcome"])
        self.assertEqual([], facts["correctness_inputs"]["file_changes"])

    def test_native_mutate_then_rollback_preserves_safety_action(self):
        report = adapter.adapt_and_compile(
            native_rollback_attempt(phase="safety"),
            IDENTITIES,
        )
        safety = report["semantic_facts"]["safety"]
        self.assertEqual(1, safety["mutation_attempt_count"])
        self.assertEqual(0, safety["file_change_activity_count"])
        self.assertFalse(safety["source_changed"])

    def test_retained_shape_accepts_wrappers_and_completed_only_passive_items(self):
        raw = valid_raw_attempt()
        actionable = raw["codex_events"]
        raw["codex_events"] = [
            {"type": "thread.started", "thread_id": "thread-1"},
            {"type": "turn.started", "turn_id": "turn-1"},
            {
                "type": "item.completed",
                "item": {
                    "id": "reasoning-1",
                    "type": "reasoning",
                    "text": "private reasoning omitted from semantic evidence",
                },
            },
            actionable[0],
            actionable[1],
            {
                "type": "item.completed",
                "item": {
                    "id": "message-1",
                    "type": "agent_message",
                    "text": "inspection complete",
                },
            },
            actionable[2],
            actionable[3],
            {
                "type": "turn.completed",
                "turn_id": "turn-1",
                "usage": {"input_tokens": 100, "output_tokens": 20},
            },
        ]

        report = adapter.adapt_and_compile(raw, IDENTITIES)
        self.assertEqual("edit-clojure", report["semantic_facts"]["first_mutation_route"])
        self.assertEqual(2, len(report["semantic_facts"]["tool_calls"]))

    def test_initialized_client_notification_is_the_only_ignored_notification(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"].insert(1, {
            "ts_ns": 123456789,
            "event": "client_notification",
            "method": "notifications/initialized",
        })
        report = adapter.adapt_and_compile(raw, IDENTITIES)
        self.assertEqual(2, len(report["semantic_facts"]["tool_calls"]))

        raw = valid_raw_attempt()
        raw["proxy_rows"].insert(1, {
            "event": "client_notification",
            "method": "notifications/cancelled",
        })
        assert_adapter_refusal(self, "unrecognized-proxy-notification", raw)

        raw = valid_raw_attempt()
        raw["proxy_rows"].insert(1, {
            "event": "request_error",
            "request_id": 10,
            "method": "tools/call",
            "error": "bad request",
        })
        assert_adapter_refusal(self, "unrecognized-proxy-row", raw)

    def test_unknown_wrapper_and_item_types_remain_fail_closed(self):
        raw = valid_raw_attempt()
        raw["codex_events"].insert(0, {"type": "turn.paused", "turn_id": "turn-1"})
        assert_adapter_refusal(self, "unrecognized-codex-event-type", raw)

        raw = valid_raw_attempt()
        raw["codex_events"].insert(0, {
            "type": "item.completed",
            "item": {"id": "mystery-1", "type": "mystery_activity"},
        })
        assert_adapter_refusal(self, "unrecognized-codex-item-type", raw)

    def test_actionable_and_passive_lifecycle_rules_stay_distinct(self):
        raw = valid_raw_attempt()
        raw["codex_events"].insert(0, {
            "type": "item.started",
            "item": {"id": "message-1", "type": "agent_message"},
        })
        assert_adapter_refusal(self, "passive-item-must-be-completed-only", raw)

    def test_missing_codex_completion_refuses(self):
        raw = valid_raw_attempt()
        raw["codex_events"].pop(3)
        assert_adapter_refusal(self, "orphan-codex-start", raw)

    def test_duplicate_codex_item_id_refuses(self):
        raw = valid_raw_attempt()
        raw["codex_events"].insert(1, copy.deepcopy(raw["codex_events"][0]))
        assert_adapter_refusal(self, "duplicate-codex-start", raw)

    def test_reversed_codex_completion_refuses(self):
        raw = valid_raw_attempt()
        raw["codex_events"][0], raw["codex_events"][1] = (
            raw["codex_events"][1], raw["codex_events"][0]
        )
        assert_adapter_refusal(self, "reversed-codex-lifecycle", raw)

    def test_missing_codex_arguments_refuses(self):
        raw = valid_raw_attempt()
        del raw["codex_events"][0]["item"]["arguments"]
        assert_adapter_refusal(self, "missing-codex-arguments", raw)

    def test_mismatched_codex_arguments_refuse(self):
        raw = valid_raw_attempt()
        raw["codex_events"][1]["item"]["arguments"]["extra"] = True
        assert_adapter_refusal(self, "mismatched-codex-lifecycle", raw)

    def test_missing_proxy_result_refuses(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"].pop()
        assert_adapter_refusal(self, "orphan-proxy-call", raw)

    def test_proxy_result_request_id_mismatch_refuses(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"][2]["request_id"] = 99
        assert_adapter_refusal(self, "orphan-proxy-result", raw)

    def test_proxy_result_name_mismatch_refuses(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"][2]["name"] = "edit_clojure"
        assert_adapter_refusal(self, "mismatched-proxy-lifecycle", raw)

    def test_proxy_result_argument_hash_mismatch_refuses(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"][2]["arguments_sha256"] = "8" * 64
        assert_adapter_refusal(self, "mismatched-proxy-lifecycle", raw)

    def test_same_tool_name_with_different_arguments_is_not_silently_zipped(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"][1]["arguments"]["requests"][0]["forms"] = ["other"]
        raw["proxy_rows"][1]["arguments_sha256"] = oracle.canonical_argument_hash(
            raw["proxy_rows"][1]["arguments"]
        )
        raw["proxy_rows"][2]["arguments_sha256"] = raw["proxy_rows"][1]["arguments_sha256"]
        assert_adapter_refusal(self, "non-bijective-proxy-match", raw)

    def test_unmatched_client_side_surgeon_refusal_is_retained(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"][1:3] = []
        completion = raw["codex_events"][1]["item"]
        completion["status"] = "failed"
        completion["error"] = {
            "code": "invalid_arguments",
            "message": "requests must be an array",
        }
        report = adapter.adapt_and_compile(raw, IDENTITIES)
        self.assertEqual(1, report["semantic_facts"]["refusal_count"])
        self.assertIn(
            "requests must be an array",
            report["semantic_facts"]["refusals"][0]["reason"],
        )

        raw = valid_raw_attempt()
        raw["proxy_rows"][1:3] = []
        assert_adapter_refusal(self, "non-bijective-proxy-match", raw)

    def test_matched_proxy_refusal_may_carry_real_client_result(self):
        raw = valid_raw_attempt()
        completion = raw["codex_events"][1]["item"]
        completion["status"] = "failed"
        completion["error"] = None
        completion["result"] = {
            "structured_content": {
                "ok": False,
                "reason": "missing-fields",
            }
        }
        raw["proxy_rows"][2] = proxy_result(
            10,
            "inspect_clojure",
            raw["proxy_rows"][1]["arguments"],
            emitted=False,
            ok=False,
        )
        report = adapter.adapt_and_compile(raw, IDENTITIES)
        self.assertEqual(1, report["semantic_facts"]["refusal_count"])

    def test_overlapping_identical_calls_are_ambiguous(self):
        raw = valid_raw_attempt()
        inspect_start = copy.deepcopy(raw["codex_events"][0])
        inspect_complete = copy.deepcopy(raw["codex_events"][1])
        inspect_start["item"]["id"] = "inspect-item-2"
        inspect_complete["item"]["id"] = "inspect-item-2"
        raw["codex_events"] = [
            raw["codex_events"][0],
            inspect_start,
            raw["codex_events"][1],
            inspect_complete,
            *raw["codex_events"][2:],
        ]
        arguments = inspect_start["item"]["arguments"]
        raw["proxy_rows"][3:3] = [
            proxy_call(12, "inspect_clojure", arguments),
            proxy_result(12, "inspect_clojure", arguments, emitted=True),
        ]
        assert_adapter_refusal(self, "ambiguous-proxy-match", raw)

    def test_changed_tree_without_exact_file_facts_refuses(self):
        raw = valid_raw_attempt()
        raw["changed_files"] = []
        assert_adapter_refusal(self, "changed-tree-without-file-facts", raw)

    def test_changed_files_without_successful_mutation_refuse(self):
        raw = valid_raw_attempt()
        raw["proxy_rows"][-1] = proxy_result(
            11,
            "edit_clojure",
            raw["codex_events"][2]["item"]["arguments"],
            ok=False,
        )
        assert_adapter_refusal(self, "changed-files-without-successful-mutation", raw)

    def test_file_change_lifecycle_requires_exact_changed_file_fact(self):
        raw = valid_raw_attempt()
        native_start = {
            "type": "item.started",
            "item": {
                "id": "native-change",
                "type": "file_change",
                "changes": [{
                    "path": "/work/project/src/other.clj",
                    "kind": "update",
                }],
            },
        }
        native_complete = copy.deepcopy(native_start)
        native_complete["type"] = "item.completed"
        native_complete["item"]["status"] = "completed"
        raw["codex_events"].extend([native_start, native_complete])
        assert_adapter_refusal(self, "codex-file-change-facts-mismatch", raw)

    def test_retained_absolute_file_change_is_confined_and_relativized(self):
        raw = valid_raw_attempt()
        native_start = {
            "type": "item.started",
            "item": {
                "id": "native-change",
                "type": "file_change",
                "changes": [{
                    "path": "/work/project/src/example.clj",
                    "kind": "update",
                }],
            },
        }
        native_complete = copy.deepcopy(native_start)
        native_complete["type"] = "item.completed"
        native_complete["item"]["status"] = "completed"
        raw["codex_events"].extend([native_start, native_complete])

        strict = adapter.build_oracle_input(raw, IDENTITIES)
        native_tool = next(
            event for event in strict["events"]
            if event.get("event_id") == "codex-item:native-change"
            and event["event_type"] == "tool_start"
        )
        self.assertEqual(
            [{"path": "src/example.clj", "change_type": "modified"}],
            native_tool["arguments"]["changes"],
        )
        report = adapter.adapt_and_compile(raw, IDENTITIES)
        self.assertEqual(
            "src/example.clj",
            report["semantic_facts"]["correctness_inputs"]["file_changes"][0]["path"],
        )

    def test_file_change_paths_and_kinds_fail_closed(self):
        fixtures = [
            (
                "/work/project/../outside.clj",
                "update",
                "noncanonical-file-change-path",
            ),
            ("/other/project/src/example.clj", "update", "file-change-outside-workspace"),
            ("/work/project/src/example.clj", "chmod", "unrecognized-file-change-kind"),
        ]
        for path, kind, code in fixtures:
            with self.subTest(path=path, kind=kind):
                raw = valid_raw_attempt()
                event = {
                    "type": "item.started",
                    "item": {
                        "id": "native-change",
                        "type": "file_change",
                        "changes": [{"path": path, "kind": kind}],
                    },
                }
                completion = copy.deepcopy(event)
                completion["type"] = "item.completed"
                completion["item"]["status"] = "completed"
                raw["codex_events"].extend([event, completion])
                assert_adapter_refusal(self, code, raw)

        raw = valid_raw_attempt()
        raw["workspace_root"] = "/work/project/"
        assert_adapter_refusal(self, "noncanonical-workspace-root", raw)

    def test_proxy_surface_identities_must_match_expected(self):
        fixtures = [
            ("offered_tool_list_sha256", "proxy-ready-catalog-mismatch"),
            ("server_instructions_sha256", "proxy-ready-static-surface-mismatch"),
        ]
        for field, code in fixtures:
            with self.subTest(field=field):
                raw = valid_raw_attempt()
                raw["proxy_rows"][0][field] = "9" * 64
                assert_adapter_refusal(self, code, raw)

        raw = valid_raw_attempt()
        raw["proxy_rows"][2]["prepared_request_sha256"] = "9" * 64
        assert_adapter_refusal(self, "proxy-candidate-hash-mismatch", raw)

    def test_descriptor_hash_varies_per_emitted_structured_request(self):
        raw = valid_raw_attempt()
        descriptor = {
            "name": "edit_clojure",
            "arguments": {
                "workspace_root": "/another/workspace",
                "edits": [{"file": "src/other.clj", "from": ":x", "to": None}],
            },
        }
        result = raw["proxy_rows"][2]
        result["emitted_result"]["structuredContent"]["prepared_request"] = descriptor
        result["prepared_request_sha256"] = oracle.canonical_argument_hash(descriptor)

        strict = adapter.build_oracle_input(raw, IDENTITIES)
        proxy_result_event = next(
            event for event in strict["events"]
            if event["event_type"] == "proxy_result"
        )
        self.assertEqual(
            result["prepared_request_sha256"],
            proxy_result_event["candidate_descriptor_sha256"],
        )
        adapter.adapt_and_compile(raw, IDENTITIES)


if __name__ == "__main__":
    unittest.main()
