from __future__ import annotations

import copy
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


REPO_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO_ROOT / "bench"))

from splice_reference_proxy import (  # noqa: E402
    ReferenceState,
    add_reference_schema,
    filter_and_annotate_tools,
    sha256_file,
)
from splice_reference_screen import ideal_requests, mutation_request_rows, summarize  # noqa: E402


FIXTURE = REPO_ROOT / "bench/fixtures/splice_reference"


class SpliceReferenceScreenTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = json.loads((FIXTURE / "spans.json").read_text(encoding="utf-8"))

    def test_fixture_is_exact_and_meaty(self) -> None:
        before = (FIXTURE / "before" / self.manifest["file"]).read_text(encoding="utf-8")
        expected = (FIXTURE / "expected" / self.manifest["file"]).read_text(encoding="utf-8")
        actual = before
        candidates = self.manifest["candidates"]
        targets = [spec for spec in candidates if "to" in spec]
        self.assertEqual(16, len(candidates))
        self.assertEqual(4, len(targets))
        self.assertEqual({4}, {
            sum(spec["within"]["form"] == owner for spec in candidates)
            for owner in self.manifest["owners"]
        })
        self.assertEqual([2, 4, 1, 3], [spec["ordinal_in_owner"] for spec in targets])
        self.assertNotEqual(
            [spec["label"] for spec in candidates],
            sorted(spec["label"] for spec in candidates),
        )
        for spec in targets:
            self.assertGreaterEqual(len(spec["from"].encode("utf-8")), 100)
            self.assertEqual(1, actual.count(spec["from"]))
            actual = actual.replace(spec["from"], spec["to"], 1)
        self.assertEqual(expected, actual)
        self.assertIn('(def alpha "alpha")', before)
        self.assertIn('"alpha" :sibling "alphabet"', before)

    def test_zero_model_instrument_can_cross_kill_threshold(self) -> None:
        ideal = ideal_requests(self.manifest)
        self.assertGreaterEqual(ideal["possible_reduction"]["utf8_bytes"], 0.30)
        self.assertGreaterEqual(ideal["possible_reduction"]["tokens"], 0.30)

    def test_proxy_refusal_remains_a_counted_mutation_attempt(self) -> None:
        receipts = [
            {"event": "tool-request", "tool": "inspect_clojure", "model_arguments": {}},
            {"event": "reference-refusal", "model_arguments": {"edits": [{"from_ref": "r01"}]}},
            {"event": "tool-request", "tool": "edit_clojure", "model_arguments": {"edits": []}},
        ]
        self.assertEqual(
            [receipts[1], receipts[2]],
            mutation_request_rows(receipts),
        )

    def test_kill_matrix_keeps_recovered_wrong_subject_loud(self) -> None:
        def score(arm: str, index: int) -> dict:
            return {
                "arm": arm,
                "completed_task": True,
                "exact_bytes": True,
                "environment_valid": True,
                "route_adherent": True,
                "wrong_subject": 1 if arm == "R" and index == 0 else 0,
                "typed_reference_failures": 0,
                "reference_used_strict": arm == "R",
                "readback_behavior": "verified" if arm == "R" else "not_applicable",
                "emitted": {
                    "mutation_utf8_bytes": 2000 if arm == "Q" else 1300,
                    "mutation_tokens": 650 if arm == "Q" else 440,
                    "all_mcp_tokens": 700 if arm == "Q" else 500,
                },
                "turns": {"mcp_round_trips": 2},
            }

        scores = [score("Q", index) for index in range(8)]
        scores += [score("R", index) for index in range(8)]
        summary = summarize(scores, expected_attempts=8)
        self.assertTrue(summary["validity_gate_passed"])
        self.assertTrue(summary["kills"]["any_wrong_subject"])
        self.assertFalse(summary["screen_survives"])

    def test_R_schema_adds_reference_without_removing_ordinary_pair(self) -> None:
        base = {
            "name": "edit_clojure",
            "description": "ordinary",
            "inputSchema": {"properties": {"edits": {"items": {
                "properties": {}, "allOf": [{"oneOf": [{"required": ["from", "to"]}]}]
            }}}},
            "outputSchema": {"properties": {}},
        }
        candidate = add_reference_schema(base)
        item = candidate["inputSchema"]["properties"]["edits"]["items"]
        self.assertIn("from_ref", item["properties"])
        self.assertIn({"required": ["from", "to"]}, item["allOf"][0]["oneOf"])
        self.assertTrue(any(choice.get("required") == ["from_ref", "to"]
                            for choice in item["allOf"][0]["oneOf"]))
        q = filter_and_annotate_tools({"result": {"tools": [base]}}, "Q")
        self.assertNotIn(
            "from_ref",
            q["result"]["tools"][0]["inputSchema"]["properties"]["edits"]["items"]["properties"],
        )

    def test_resolution_is_snapshot_and_target_bound(self) -> None:
        with tempfile.TemporaryDirectory(prefix="splice-ref-unit-") as directory:
            workspace = Path(directory)
            shutil.copytree(FIXTURE / "before", workspace, dirs_exist_ok=True)
            source_file = workspace / self.manifest["file"]
            response = {
                "jsonrpc": "2.0", "id": 3,
                "result": {
                    "content": [],
                    "structuredContent": {
                        "ok": True,
                        "file_hashes": {self.manifest["file"]: sha256_file(source_file)},
                        "results": [{"forms": [{"source": (
                            FIXTURE / "before" / self.manifest["file"]
                        ).read_text(encoding="utf-8")}]}],
                    },
                },
            }
            state = ReferenceState(workspace, self.manifest)
            annotated, labels = state.annotate(response)
            self.assertEqual(16, len(labels))
            self.assertEqual(
                [spec["label"] for spec in self.manifest["candidates"]],
                [label["label"] for label in labels],
            )
            self.assertIn("span_references", annotated["result"]["structuredContent"])
            good = ideal_requests(self.manifest)["R"]["arguments"]
            translated, resolved, refusal = state.translate(good)
            self.assertIsNone(refusal)
            self.assertEqual(4, len(resolved))
            self.assertTrue(all(row["identity_match"] for row in resolved))
            self.assertTrue(all(row["readback_present"] is False for row in resolved))
            self.assertTrue(all("from" in edit and "from_ref" not in edit
                                for edit in translated["edits"]))
            wrong = copy.deepcopy(good)
            wrong["edits"][0]["from_ref"] = "r07"
            translated, wrong_resolved, refusal = state.translate(wrong)
            self.assertIsNone(translated)
            self.assertEqual("splice-reference-wrong-subject", refusal["error_type"])
            self.assertTrue(refusal["wrong_subject"])
            self.assertEqual("alpha-a", wrong_resolved[0]["candidate_id"])
            self.assertEqual("alpha-b", wrong_resolved[0]["intended_candidate_id"])
            self.assertFalse(wrong_resolved[0]["identity_match"])
            checked = copy.deepcopy(good)
            tokens = {row["label"]: row["identity_token"] for row in labels}
            for edit in checked["edits"]:
                edit["ref_readback"] = tokens[edit["from_ref"]]
            translated, resolved, refusal = state.translate(checked)
            self.assertIsNone(refusal)
            self.assertTrue(all(row["readback_match"] for row in resolved))
            source_file.write_text(source_file.read_text(encoding="utf-8") + "; stale\n",
                                   encoding="utf-8")
            translated, _, refusal = state.translate(good)
            self.assertIsNone(translated)
            self.assertEqual("splice-reference-stale-snapshot", refusal["error_type"])

    def test_real_stdio_proxy_lowers_then_product_validates(self) -> None:
        with tempfile.TemporaryDirectory(prefix="splice-ref-stdio-") as directory:
            root = Path(directory)
            workspace = root / "workspace"
            shutil.copytree(FIXTURE / "before", workspace)
            command = [
                sys.executable, str(REPO_ROOT / "bench/splice_reference_proxy.py"),
                "--arm", "R", "--repo-root", str(REPO_ROOT),
                "--workspace", str(workspace), "--manifest", str(FIXTURE / "spans.json"),
                "--receipts", str(root / "receipts.jsonl"),
                "--stream", str(root / "stream.jsonl"),
                "--child-stderr", str(root / "child.stderr"),
                "--telemetry-dir", str(root / "telemetry"), "--run-id", "unit-R",
                "--java-home", str(Path(os.environ["JAVA_HOME"]).resolve()),
            ]
            process = subprocess.Popen(
                command, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                stderr=subprocess.PIPE, text=True,
            )
            assert process.stdin is not None and process.stdout is not None

            def request(value: dict) -> dict:
                process.stdin.write(json.dumps(value, separators=(",", ":")) + "\n")
                process.stdin.flush()
                while True:
                    response = json.loads(process.stdout.readline())
                    if response.get("id") == value.get("id"):
                        return response

            initialize = request({
                "jsonrpc": "2.0", "id": 1, "method": "initialize",
                "params": {"protocolVersion": "2024-11-05", "capabilities": {},
                           "clientInfo": {"name": "splice-test", "version": "0"}},
            })
            self.assertEqual("clj-surgeon", initialize["result"]["serverInfo"]["name"])
            process.stdin.write('{"jsonrpc":"2.0","method":"notifications/initialized"}\n')
            process.stdin.flush()
            tools = request({"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}})
            self.assertEqual(["inspect_clojure", "edit_clojure"],
                             [tool["name"] for tool in tools["result"]["tools"]])
            inspect = request({
                "jsonrpc": "2.0", "id": 3, "method": "tools/call",
                "params": {"name": "inspect_clojure", "arguments": {
                    "requests": [{"file": self.manifest["file"],
                                  "forms": self.manifest["owners"],
                                  "expect": {"forms": 4}}],
                    "expect": {"requests": 1, "files": 1},
                }},
            })
            labels = inspect["result"]["structuredContent"]["span_references"]["labels"]
            self.assertEqual(16, len(labels))
            edit = request({
                "jsonrpc": "2.0", "id": 4, "method": "tools/call",
                "params": {"name": "edit_clojure",
                           "arguments": ideal_requests(self.manifest)["R"]["arguments"]},
            })
            self.assertTrue(edit["result"]["structuredContent"]["ok"])
            self.assertEqual(4, len(edit["result"]["structuredContent"]["resolved_references"]))
            self.assertTrue(all(
                row["identity_match"]
                for row in edit["result"]["structuredContent"]["resolved_references"]
            ))
            self.assertEqual(
                (FIXTURE / "expected" / self.manifest["file"]).read_text(encoding="utf-8"),
                (workspace / self.manifest["file"]).read_text(encoding="utf-8"),
            )
            process.stdin.close()
            process.terminate()
            process.wait(timeout=10)
            process.stdout.close()
            assert process.stderr is not None
            process.stderr.close()


if __name__ == "__main__":
    unittest.main()
