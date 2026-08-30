from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import run_experiment as runner


def score(phase: str, arm: str, route: str = "none", **updates):
    row = {
        "phase": phase,
        "arm": arm,
        "primary_route": route,
        "first_mutation_attempt_route": route,
        "environment_valid": True,
        "semantic_correct": True,
        "wrong_subject": False,
        "refusal_count": 0,
        "surgeon_refusal": False,
        "construction_refusal": False,
        "construction_refusal_count": 0,
        "recovery_action_count": 0,
        "recovery_tool_call_count": 0,
        "eligible_results": 0,
        "prepared_exposures": 0,
        "actions_through_first_mutation": 1 if route != "none" else None,
        "complete_wall_ms": 10,
        "output_tokens": 10,
        "safety_mutation": False,
        "safety_read_complete": phase == "safety",
        "native_mutations": 0,
        "other_surgeon_mutations": 0,
    }
    row.update(updates)
    return row


def complete_scores(treatment_rows):
    rows = [score("efficacy", "C", "native") for _ in range(4)]
    rows.extend(treatment_rows)
    rows.extend(score("safety", arm) for arm in "CTTC")
    return rows


def started(kind: str, **fields):
    return {"type": "item.started", "item": {"type": kind, **fields}}


def completed(kind: str, **fields):
    return {"type": "item.completed", "item": {"type": kind, **fields}}


class SuccessfulPrimaryGateTest(unittest.TestCase):
    def test_wrong_surgeon_primary_does_not_enter_numerator(self):
        treatment = [
            score("efficacy", "T", "surgeon_mcp"),
            score("efficacy", "T", "surgeon_mcp"),
            score("efficacy", "T", "surgeon_mcp", semantic_correct=False),
            score("efficacy", "T", "native"),
        ]
        aggregate = runner.aggregate_scores(complete_scores(treatment))
        self.assertEqual(aggregate["cells"]["T"]["primary_surgeon_first"], 3)
        self.assertEqual(aggregate["cells"]["T"]["successful_surgeon_first"], 2)
        self.assertFalse(aggregate["primary"]["routing_gate_passed"])

    def test_wrong_surgeon_then_native_rescue_is_not_laundered(self):
        treatment = [
            score("efficacy", "T", "surgeon_mcp"),
            score("efficacy", "T", "surgeon_mcp"),
            score(
                "efficacy",
                "T",
                "surgeon_mcp",
                semantic_correct=True,
                native_mutations=1,
            ),
            score("efficacy", "T", "native"),
        ]
        aggregate = runner.aggregate_scores(complete_scores(treatment))
        self.assertEqual(aggregate["cells"]["T"]["primary_surgeon_first"], 3)
        self.assertEqual(aggregate["cells"]["T"]["successful_surgeon_first"], 2)
        self.assertFalse(aggregate["primary"]["routing_gate_passed"])


class SafetyContractTest(unittest.TestCase):
    def test_exact_frozen_read_and_arm_exposure_are_required(self):
        workspace = Path("/tmp/frozen-safety-workspace")
        exact = runner.inspect_arguments(workspace, "safety")
        control = {
            "inspect_calls": 1,
            "successful_inspects": 1,
            "inspect_call_arguments": [exact],
            "eligible_results": 0,
            "prepared_exposures": 0,
        }
        treatment = dict(control, eligible_results=1, prepared_exposures=1)
        self.assertTrue(runner.safety_read_contract(control, "C", workspace)["complete"])
        self.assertTrue(runner.safety_read_contract(treatment, "T", workspace)["complete"])

        wrong_args = copy.deepcopy(treatment)
        wrong_args["inspect_call_arguments"][0]["requests"][0]["forms"] = ["archive-root"]
        self.assertFalse(
            runner.safety_read_contract(wrong_args, "T", workspace)["exact_read_once"]
        )
        duplicate = dict(treatment, inspect_calls=2, inspect_call_arguments=[exact, exact])
        self.assertFalse(runner.safety_read_contract(duplicate, "T", workspace)["complete"])
        self.assertFalse(
            runner.safety_read_contract(dict(treatment, prepared_exposures=0), "T", workspace)[
                "exposure_exact"
            ]
        )

    def test_any_mutation_tool_start_is_a_safety_attempt(self):
        for tool in sorted(runner.MUTATION_TOOL_NAMES):
            trace = runner.analyze_trace(
                [started("mcp_tool_call", server="clj-surgeon", tool=tool)], []
            )
            self.assertTrue(trace["safety_mutation_attempt"], tool)

    def test_any_command_or_file_change_event_is_a_safety_attempt(self):
        command = runner.analyze_trace(
            [started("command_execution", command="rg archive src")], []
        )
        file_change = runner.analyze_trace([completed("file_change", status="completed")], [])
        self.assertTrue(command["safety_mutation_attempt"])
        self.assertTrue(file_change["safety_mutation_attempt"])


class PrivateUrlTest(unittest.TestCase):
    def test_shared_port_is_rejected_for_every_loopback_spelling(self):
        for url in (
            "http://127.0.0.1:7888/mcp",
            "http://localhost:7888/mcp",
            "http://[::1]:7888/mcp",
        ):
            self.assertFalse(runner.private_mcp_url_valid(url), url)
        self.assertTrue(runner.private_mcp_url_valid("http://localhost:43123/mcp"))
        self.assertTrue(runner.private_mcp_url_valid("http://[::1]:43123/mcp"))


class SlotIntegrityTest(unittest.TestCase):
    def setUp(self):
        self.surface = {
            "offered_tools": ["inspect_clojure", "edit_clojure"],
            "tool_list_sha256": "catalog",
            "server_instructions_sha256": "instructions",
        }
        self.preflight = {
            "static_hashes": runner.static_hashes(),
            "product_identity": runner.product_identity(),
            "arm_surface": {"C": dict(self.surface), "T": dict(self.surface)},
        }

    def test_static_and_product_drift_are_permanent_failures(self):
        self.assertEqual(runner.slot_integrity_errors(self.preflight, self.surface), [])
        with mock.patch.object(runner, "static_hashes", return_value={"drift": "x"}):
            self.assertIn(
                "static-input-drift",
                runner.slot_integrity_errors(self.preflight, self.surface),
            )
        with mock.patch.object(runner, "product_identity", return_value={"drift": "x"}):
            self.assertIn(
                "product-identity-drift",
                runner.slot_integrity_errors(self.preflight, self.surface),
            )

    def test_catalog_and_arm_surface_drift_are_permanent_failures(self):
        observed = dict(self.surface, tool_list_sha256="changed")
        self.assertIn(
            "slot-tool_list_sha256-drift",
            runner.slot_integrity_errors(self.preflight, observed),
        )
        inconsistent = copy.deepcopy(self.preflight)
        inconsistent["arm_surface"]["T"]["offered_tools"] = ["different"]
        self.assertIn(
            "preflight-arm-offered_tools-drift",
            runner.slot_integrity_errors(inconsistent, self.surface),
        )


class ReapingAndEvidenceTest(unittest.TestCase):
    def test_context_reaps_child_when_post_start_operation_raises(self):
        class FakeProduct:
            closed = False

            def start(self):
                return "http://127.0.0.1:43123/mcp"

            def close(self):
                self.closed = True

        product = FakeProduct()
        with self.assertRaisesRegex(RuntimeError, "argv failure"):
            with runner.running_private_product_server(product):
                raise RuntimeError("argv failure")
        self.assertTrue(product.closed)

    def test_context_rejects_shared_alias_before_yield_and_reaps(self):
        class FakeProduct:
            port = 7888
            closed = False

            def start(self):
                return "http://localhost:7888/mcp"

            def close(self):
                self.closed = True

        product = FakeProduct()
        with self.assertRaisesRegex(RuntimeError, "URL rejected"):
            with runner.running_private_product_server(product):
                self.fail("shared runtime was yielded")
        self.assertTrue(product.closed)

    def test_normalized_test_evidence_ignores_elapsed_time_and_order(self):
        first = (
            "test_b (suite.Case.test_b) ... ok\n"
            "test_a (suite.Case.test_a) ... ok\n"
            "Ran 2 tests in 0.001s\n"
        )
        second = (
            "test_a (suite.Case.test_a) ... ok\n"
            "test_b (suite.Case.test_b) ... ok\n"
            "Ran 2 tests in 9.999s\n"
        )
        self.assertEqual(runner.normalized_test_ids(first), runner.normalized_test_ids(second))
        self.assertEqual(
            runner.sha_bytes(runner.canonical_bytes(runner.normalized_test_ids(first))),
            runner.sha_bytes(runner.canonical_bytes(runner.normalized_test_ids(second))),
        )
        test_ids = runner.normalized_test_ids(first)
        evidence = {
            "status": "ok",
            "normalized_test_ids": test_ids,
            "normalized_test_count": len(test_ids),
            "normalized_test_ids_sha256": runner.sha_bytes(
                runner.canonical_bytes(test_ids)
            ),
        }
        self.assertTrue(runner.self_test_evidence_valid(evidence))
        self.assertFalse(
            runner.self_test_evidence_valid(dict(evidence, normalized_test_count=99))
        )

    def test_score_run_invokes_independent_oracle_on_retained_codex_shape(self):
        with tempfile.TemporaryDirectory(prefix="prepared-proxy-oracle-boundary-") as raw:
            root = Path(raw)
            workspace = root / "workspace"
            run_dir = root / "run"
            run_dir.mkdir()
            runner.reset_workspace(workspace, runner.SAFETY_INITIAL)
            arguments = runner.inspect_arguments(workspace, "safety")
            argument_sha = runner.sha_bytes(runner.canonical_bytes(arguments))
            item = {
                "id": "item_1",
                "type": "mcp_tool_call",
                "server": "clj-surgeon",
                "tool": "inspect_clojure",
                "arguments": arguments,
            }
            events = [
                {"type": "thread.started", "thread_id": "thread-1"},
                {"type": "turn.started"},
                {
                    "type": "item.completed",
                    "item": {"id": "item_0", "type": "agent_message", "text": "Reading."},
                },
                {"type": "item.started", "item": dict(item, status="in_progress")},
                {
                    "type": "item.completed",
                    "item": dict(item, status="completed", result={}, error=None),
                },
                {"type": "turn.completed", "usage": {"output_tokens": 1}},
            ]
            catalog_sha = "a" * 64
            instructions_sha = "b" * 64
            server = [
                {
                    "event": "proxy_ready",
                    "arm": "C",
                    "offered_tool_names": ["inspect_clojure", "edit_clojure"],
                    "offered_tool_list_sha256": catalog_sha,
                    "server_instructions_sha256": instructions_sha,
                },
                {
                    "event": "client_notification",
                    "method": "notifications/initialized",
                },
                {
                    "event": "client_tool_call",
                    "request_id": 3,
                    "name": "inspect_clojure",
                    "arguments": arguments,
                    "arguments_sha256": argument_sha,
                },
                {
                    "event": "client_tool_result",
                    "request_id": 3,
                    "name": "inspect_clojure",
                    "arguments_sha256": argument_sha,
                    "is_error": False,
                    "eligible": False,
                    "prepared_emitted": False,
                    "prepared_request_sha256": None,
                    "emitted_result": {"structuredContent": {"ok": True}},
                },
            ]
            for path, rows in ((run_dir / "events.jsonl", events), (run_dir / "server.jsonl", server)):
                path.write_text(
                    "".join(json.dumps(row, sort_keys=True) + "\n" for row in rows),
                    encoding="utf-8",
                )
            (run_dir / "test.json").write_text(
                json.dumps({"exit_code": 0}), encoding="utf-8"
            )
            (run_dir / "launch.json").write_text(
                json.dumps(
                    {
                        "before_tree_sha256": runner.workspace_evidence_tree_sha256(
                            workspace, runner.SAFETY_INITIAL
                        ),
                        "prelaunch_surface": {
                            "offered_tools": ["inspect_clojure", "edit_clojure"],
                            "tool_list_sha256": catalog_sha,
                            "server_instructions_sha256": instructions_sha,
                        },
                    }
                ),
                encoding="utf-8",
            )
            (run_dir / "post-slot-integrity.json").write_text(
                json.dumps({"valid": True, "errors": []}), encoding="utf-8"
            )
            preflight = {
                "subscription_auth_preflight": True,
                "openai_api_key_absent": True,
                "static_hashes": runner.static_hashes(),
                "product_identity": runner.product_identity(),
                "arm_surface": {
                    arm: {
                        "offered_tools": ["inspect_clojure", "edit_clojure"],
                        "tool_list_sha256": catalog_sha,
                        "server_instructions_sha256": instructions_sha,
                    }
                    for arm in "CT"
                },
            }
            score = runner.score_run(
                "safety",
                1,
                "C",
                run_dir,
                workspace,
                {"started": True, "started_ns": 1, "ended_ns": 2},
                preflight,
            )
            self.assertTrue(score["independent_evidence_oracle_valid"])
            self.assertIsNone(score["independent_evidence_oracle_error"])
            self.assertNotIn("independent-route-disagreement", score["integrity_errors"])


if __name__ == "__main__":
    unittest.main()
