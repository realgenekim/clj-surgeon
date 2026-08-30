from __future__ import annotations

import copy
import hashlib
import json
import unittest

import proxy
import run_experiment as runner


HASH_A = "a" * 64


def source_hash(source: str) -> str:
    return hashlib.sha256(source.encode("utf-8")).hexdigest()


def character_count(source: str) -> int:
    return len(source.encode("utf-16-le")) // 2


def form(name: str, source: str, file_name: str = "src/acme/example.clj") -> dict:
    source_count = character_count(source)
    owner_start = source.index(name)
    return {
        "file": file_name,
        "file_hash": HASH_A,
        "name": name,
        "source": source,
        "hash": source_hash(source),
        "form_type": "defn",
        "source_anchor": {
            "file": file_name,
            "owner": name,
            "source_sha256": HASH_A,
            "range": {
                "start": {"line": 0, "character": 0},
                "end": {"line": 0, "character": source_count},
            },
            "selection_range": {
                "start": {"line": 0, "character": owner_start},
                "end": {"line": 0, "character": owner_start + len(name)},
            },
        },
    }


def inspect_result(forms: list[dict] | None = None) -> dict:
    values = forms or [form("alpha", "(defn alpha [] 1)")]
    source_count = sum(character_count(item["source"]) for item in values)
    return {
        "content": [{"type": "text", "text": "inspect_clojure\nread complete"}],
        "isError": False,
        "structuredContent": {
            "ok": True,
            "operation": "inspect_clojure",
            "read_complete": True,
            "next_action": "none",
            "workspace_root": "/tmp/fixture",
            "request_count": 1,
            "file_count": 1,
            "file_hashes": {"src/acme/example.clj": HASH_A},
            "source_character_count": source_count,
            "results": [
                {
                    "id": "request-1",
                    "operation": "forms",
                    "file": "src/acme/example.clj",
                    "file_hash": HASH_A,
                    "form_count": len(values),
                    "source_character_count": source_count,
                    "forms": values,
                }
            ],
        },
    }


def completed_item(kind: str, **fields) -> dict:
    return {"type": "item.completed", "item": {"type": kind, "status": "completed", **fields}}


def started_item(kind: str, **fields) -> dict:
    return {"type": "item.started", "item": {"type": kind, **fields}}


def server_result(name: str, *, ok: bool, committed: bool = False, emitted: bool = False) -> dict:
    return {
        "event": "client_tool_result",
        "name": name,
        "eligible": name == "inspect_clojure" and ok,
        "prepared_emitted": emitted,
        "emitted_result": {
            "structuredContent": {
                "ok": ok,
                "committed": committed,
                "verification_complete": committed,
            }
        },
    }


class ProjectionTest(unittest.TestCase):
    def test_catalog_is_exact_same_object_order_and_hash(self) -> None:
        catalog = {
            "tools": [
                {"name": "transform_clojure"},
                {"name": "inspect_clojure"},
                {"name": "apply_clojure_changes"},
                {"name": "edit_clojure"},
            ]
        }
        projected = proxy.project_tools_list(catalog)
        self.assertIs(projected, catalog)
        self.assertEqual(proxy.digest(projected), proxy.digest(catalog))
        self.assertEqual(
            [row["name"] for row in projected["tools"]],
            ["transform_clojure", "inspect_clojure", "apply_clojure_changes", "edit_clojure"],
        )

    def test_control_is_identity_pass_through(self) -> None:
        upstream = inspect_result()
        projected, evidence = proxy.project_inspect_result(upstream, "C")
        self.assertIs(projected, upstream)
        self.assertFalse(evidence["prepared_emitted"])
        self.assertEqual(proxy.digest(projected), proxy.digest(upstream))

    def test_treatment_is_derived_null_only_and_does_not_mutate_input(self) -> None:
        upstream = inspect_result(
            [
                form("alpha", "(defn alpha [] 1)"),
                form("beta", "(defn beta [] 2)"),
            ]
        )
        frozen = copy.deepcopy(upstream)
        projected, evidence = proxy.project_inspect_result(upstream, "T")
        self.assertEqual(upstream, frozen)
        self.assertTrue(evidence["prepared_emitted"])
        prepared = projected["structuredContent"]["prepared_request"]
        self.assertFalse(prepared["executable"])
        self.assertFalse(prepared["write_authority"])
        self.assertEqual(prepared["tool"], "edit_clojure")
        self.assertEqual(
            prepared["caller_holes"],
            ["arguments.edits[0].to", "arguments.edits[1].to"],
        )
        self.assertTrue(all(edit["to"] is None for edit in prepared["arguments"]["edits"]))
        serialized = json.dumps(prepared, sort_keys=True)
        for future in ("400", "800", "resilient", "retry-jitter-ms"):
            self.assertNotIn(future, serialized)
        delta = projected["content"][0]["text"][len(frozen["content"][0]["text"]) :]
        self.assertEqual(delta, "\n" + proxy.COACHING)
        self.assertEqual(projected["structuredContent"]["next_action"], "none")

    def test_exact_public_edit_shape(self) -> None:
        descriptor, omission = proxy.derive_prepared_request(inspect_result())
        self.assertIsNone(omission)
        self.assertEqual(
            set(descriptor),
            {"tool", "executable", "write_authority", "arguments", "caller_holes"},
        )
        edit = descriptor["arguments"]["edits"][0]
        self.assertEqual(set(edit), {"file", "within", "from", "to", "matches"})
        self.assertEqual(edit["matches"], 1)

    def assert_omitted(self, candidate: dict, reason: str) -> None:
        projected, evidence = proxy.project_inspect_result(candidate, "T")
        self.assertIs(projected, candidate)
        self.assertFalse(evidence["prepared_emitted"])
        self.assertEqual(evidence["omission_reason"], reason)
        self.assertNotIn("prepared_request", candidate.get("structuredContent", {}))

    def test_refusal_and_incomplete_are_not_eligible(self) -> None:
        refusal = inspect_result()
        refusal["structuredContent"]["ok"] = False
        self.assert_omitted(refusal, "read-incomplete")
        partial = inspect_result()
        partial["structuredContent"]["read_complete"] = False
        self.assert_omitted(partial, "read-incomplete")

    def test_requires_exactly_one_forms_result(self) -> None:
        candidate = inspect_result()
        candidate["structuredContent"]["results"].append(
            copy.deepcopy(candidate["structuredContent"]["results"][0])
        )
        self.assert_omitted(candidate, "requires-one-forms-result")
        candidate = inspect_result()
        candidate["structuredContent"]["results"][0]["operation"] = "outline"
        self.assert_omitted(candidate, "non-forms-result")

    def test_one_bad_form_omits_everything(self) -> None:
        candidate = inspect_result(
            [form("alpha", "(defn alpha [] 1)"), form("beta", "(defn beta [] 2)")]
        )
        candidate["structuredContent"]["results"][0]["forms"][1].pop("source")
        self.assert_omitted(candidate, "source-missing")

    def test_duplicate_owner_is_not_identity(self) -> None:
        candidate = inspect_result(
            [form("alpha", "(defn alpha [] 1)"), form("alpha", "(defn alpha [] 2)")]
        )
        self.assert_omitted(candidate, "duplicate-owner")

    def test_basis_or_partial_result_is_not_eligible(self) -> None:
        for key in ("basis", "continuation", "retry_template"):
            candidate = inspect_result()
            candidate["structuredContent"][key] = "present"
            self.assert_omitted(candidate, "basis-or-partial-result")

    def test_cardinality_and_source_count_are_authority(self) -> None:
        candidate = inspect_result()
        candidate["structuredContent"]["results"][0]["form_count"] = 2
        self.assert_omitted(candidate, "form-count-mismatch")
        candidate = inspect_result()
        candidate["structuredContent"]["results"][0]["source_character_count"] += 1
        self.assert_omitted(candidate, "source-character-count-mismatch")

    def test_item_and_byte_budgets_omit_all(self) -> None:
        seven = [form(f"owner-{index}", f"(defn owner-{index} [] {index})") for index in range(7)]
        self.assert_omitted(inspect_result(seven), "forms-missing")
        descriptor, omission = proxy.derive_prepared_request(inspect_result(), max_bytes=10)
        self.assertIsNone(descriptor)
        self.assertEqual(omission, "byte-budget-exceeded")

    def test_missing_concise_text_omits_all(self) -> None:
        candidate = inspect_result()
        candidate["content"] = []
        self.assert_omitted(candidate, "concise-text-missing")


class TraceTest(unittest.TestCase):
    def test_refused_edit_then_native_is_native_not_surgeon(self) -> None:
        events = [
            started_item("mcp_tool_call", server="clj-surgeon", tool="edit_clojure"),
            completed_item("mcp_tool_call", server="clj-surgeon", tool="edit_clojure"),
            started_item("file_change"),
            completed_item("file_change"),
        ]
        trace = runner.analyze_trace(events, [server_result("edit_clojure", ok=False)])
        self.assertEqual(trace["primary_route"], "native")
        self.assertEqual(trace["first_mutation_attempt_route"], "surgeon_mcp")
        self.assertEqual(trace["surgeon_mutations"], 0)
        self.assertEqual(trace["refusal_count"], 1)
        self.assertTrue(trace["surgeon_refusal"])
        self.assertTrue(trace["construction_refusal"])
        self.assertEqual(trace["recovery_action_count"], 1)

    def test_failed_edit_then_native_preserves_attempt_and_success_routes(self) -> None:
        events = [
            started_item("mcp_tool_call", server="clj-surgeon", tool="edit_clojure"),
            completed_item("mcp_tool_call", server="clj-surgeon", tool="edit_clojure"),
            started_item("file_change"),
            completed_item("file_change"),
        ]
        trace = runner.analyze_trace(
            events, [server_result("edit_clojure", ok=True, committed=False)]
        )
        self.assertEqual(trace["first_mutation_attempt_route"], "surgeon_mcp")
        self.assertEqual(trace["primary_route"], "native")
        self.assertFalse(trace["surgeon_refusal"])

    def test_native_then_surgeon_keeps_native_first_for_both_laws(self) -> None:
        events = [
            started_item("file_change"),
            completed_item("file_change"),
            started_item("mcp_tool_call", server="clj-surgeon", tool="edit_clojure"),
            completed_item("mcp_tool_call", server="clj-surgeon", tool="edit_clojure"),
        ]
        trace = runner.analyze_trace(
            events, [server_result("edit_clojure", ok=True, committed=True)]
        )
        self.assertEqual(trace["first_mutation_attempt_route"], "native")
        self.assertEqual(trace["primary_route"], "native")

    def test_only_edit_clojure_advances_other_surgeon_is_loss(self) -> None:
        events = [
            started_item("mcp_tool_call", server="clj-surgeon", tool="apply_clojure_changes"),
            completed_item("mcp_tool_call", server="clj-surgeon", tool="apply_clojure_changes"),
        ]
        trace = runner.analyze_trace(
            events, [server_result("apply_clojure_changes", ok=True, committed=True)]
        )
        self.assertEqual(trace["primary_route"], "other_surgeon")
        self.assertEqual(trace["surgeon_mutations"], 0)
        self.assertEqual(trace["other_surgeon_mutations"], 1)

    def test_no_inspect_no_exposure_remains_a_none_route(self) -> None:
        trace = runner.analyze_trace([], [])
        self.assertEqual(trace["primary_route"], "none")
        self.assertEqual(trace["inspect_calls"], 0)
        self.assertEqual(trace["prepared_exposures"], 0)

    def test_write_like_command_is_visible_to_safety(self) -> None:
        events = [started_item("command_execution", command="sed -i s/a/b/ src/a.clj")]
        trace = runner.analyze_trace(events, [])
        self.assertEqual(len(trace["write_like_commands"]), 1)
        self.assertEqual(trace["first_mutation_attempt_route"], "native")
        self.assertTrue(trace["any_mutation_attempt"])

    def test_client_side_argument_rejection_is_a_refusal(self) -> None:
        events = [
            started_item("mcp_tool_call", server="clj-surgeon", tool="edit_clojure"),
            completed_item(
                "mcp_tool_call",
                server="clj-surgeon",
                tool="edit_clojure",
                error={"message": "invalid tool arguments"},
            ),
        ]
        trace = runner.analyze_trace(events, [])
        self.assertEqual(trace["refusal_count"], 1)
        self.assertTrue(trace["construction_refusal"])

    def test_failed_write_command_is_attempt_but_not_successful_route(self) -> None:
        events = [
            started_item("command_execution", command="sed -i s/a/b/ src/a.clj"),
            completed_item(
                "command_execution",
                command="sed -i s/a/b/ src/a.clj",
                exit_code=1,
            ),
        ]
        trace = runner.analyze_trace(events, [])
        self.assertEqual(trace["first_mutation_attempt_route"], "native")
        self.assertEqual(trace["primary_route"], "none")


class PrivateLifecycleTest(unittest.TestCase):
    def test_private_url_and_reaped_child_are_required(self) -> None:
        valid = {
            "product_checkout": str(runner.PRODUCT_ROOT.resolve()),
            "port": 43123,
            "url": "http://127.0.0.1:43123/mcp",
            "private_port": True,
            "child_dead": True,
            "port_closed_after_reap": True,
        }
        self.assertTrue(runner.private_lifecycle_valid(valid))
        shared = dict(valid, port=7888, url="http://127.0.0.1:7888/mcp")
        self.assertFalse(runner.private_lifecycle_valid(shared))
        self.assertFalse(runner.private_lifecycle_valid(dict(valid, child_dead=False)))


class FilesystemOracleTest(unittest.TestCase):
    def test_staged_and_untracked_paths_are_not_invisible(self) -> None:
        with runner.tempfile.TemporaryDirectory(prefix="prepared-proxy-status-") as value:
            workspace = runner.Path(value)
            (workspace / "tracked.clj").write_text("(ns tracked)\n", encoding="utf-8")
            for argv in (
                ["git", "init", "-q"],
                ["git", "add", "tracked.clj"],
                [
                    "git",
                    "-c",
                    "user.name=test",
                    "-c",
                    "user.email=test@invalid",
                    "commit",
                    "-q",
                    "-m",
                    "fixture",
                ],
            ):
                self.assertEqual(runner.run_capture(argv, workspace)["exit_code"], 0)
            (workspace / "tracked.clj").write_text("(ns changed)\n", encoding="utf-8")
            self.assertEqual(
                runner.run_capture(["git", "add", "tracked.clj"], workspace)["exit_code"],
                0,
            )
            (workspace / "untracked.clj").write_text("(ns untracked)\n", encoding="utf-8")
            self.assertEqual(
                runner.git_changed_paths(workspace),
                ["tracked.clj", "untracked.clj"],
            )


def score(phase: str, arm: str, route: str = "none", **updates) -> dict:
    base = {
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
    }
    base.update(updates)
    return base


class AggregateTest(unittest.TestCase):
    def test_all_started_attempts_are_denominator_and_exact_gate_passes(self) -> None:
        scores = [
            score("efficacy", "C", "surgeon_mcp"),
            score("efficacy", "C", "native"),
            score("efficacy", "C", "native"),
            score("efficacy", "C", "none"),
            score("efficacy", "T", "surgeon_mcp", eligible_results=1, prepared_exposures=1),
            score("efficacy", "T", "surgeon_mcp", eligible_results=1, prepared_exposures=1),
            score("efficacy", "T", "surgeon_mcp", eligible_results=1, prepared_exposures=1),
            score("efficacy", "T", "none"),
        ]
        scores.extend(score("safety", arm) for arm in "CTTC")
        aggregate = runner.aggregate_scores(scores)
        self.assertEqual(aggregate["cells"]["T"]["attempts"], 4)
        self.assertEqual(aggregate["cells"]["T"]["no_mutation"], 1)
        self.assertEqual(aggregate["primary"]["t_minus_c_risk_difference"], 0.5)
        self.assertEqual(aggregate["verdict"], "advance-to-lld")

    def test_attempted_surgeon_does_not_rescue_successful_primary_gate(self) -> None:
        scores = [score("efficacy", "C", "native") for _ in range(4)]
        scores.extend(
            [
                score("efficacy", "T", "surgeon_mcp"),
                score("efficacy", "T", "surgeon_mcp"),
                score(
                    "efficacy",
                    "T",
                    "native",
                    first_mutation_attempt_route="surgeon_mcp",
                ),
                score(
                    "efficacy",
                    "T",
                    "native",
                    first_mutation_attempt_route="surgeon_mcp",
                ),
            ]
        )
        scores.extend(score("safety", arm) for arm in "CTTC")
        aggregate = runner.aggregate_scores(scores)
        self.assertEqual(aggregate["cells"]["T"]["attempted_surgeon_first"], 4)
        self.assertEqual(aggregate["cells"]["T"]["successful_surgeon_first"], 2)
        self.assertFalse(aggregate["primary"]["routing_gate_passed"])
        self.assertEqual(aggregate["verdict"], "kill-option-a")

    def test_correctness_refusal_and_safety_cannot_be_rescued_by_routing(self) -> None:
        scores = [score("efficacy", arm, "surgeon_mcp") for arm in "CCCCTTTT"]
        scores[4]["semantic_correct"] = False
        scores[5]["refusal_count"] = 1
        scores[5]["surgeon_refusal"] = True
        scores.extend(score("safety", arm) for arm in "CTTC")
        scores[-1]["safety_mutation"] = True
        aggregate = runner.aggregate_scores(scores)
        self.assertFalse(aggregate["correctness_gate_passed"])
        self.assertFalse(aggregate["refusal_gate_passed"])
        self.assertFalse(aggregate["safety"]["gate_passed"])
        self.assertEqual(aggregate["verdict"], "kill-option-a")

    def test_refusal_gate_counts_affected_attempts_not_raw_calls(self) -> None:
        scores = [score("efficacy", arm, "surgeon_mcp") for arm in "CCCCTTTT"]
        scores[0].update(refusal_count=2, surgeon_refusal=True)
        scores[4].update(refusal_count=1, surgeon_refusal=True)
        scores.extend(score("safety", arm) for arm in "CTTC")
        aggregate = runner.aggregate_scores(scores)
        self.assertEqual(aggregate["cells"]["C"]["refusal_attempts"], 1)
        self.assertEqual(aggregate["cells"]["T"]["refusal_attempts"], 1)
        self.assertTrue(aggregate["refusal_gate_passed"])

    def test_any_invalid_environment_makes_cohort_invalid(self) -> None:
        scores = [
            score("efficacy", "C", "native") for _ in range(4)
        ] + [
            score("efficacy", "T", "surgeon_mcp") for _ in range(4)
        ]
        scores.extend(score("safety", arm) for arm in "CTTC")
        scores[2]["environment_valid"] = False
        aggregate = runner.aggregate_scores(scores)
        self.assertFalse(aggregate["environment_gate_passed"])
        self.assertEqual(aggregate["verdict"], "invalid")


if __name__ == "__main__":
    unittest.main()
