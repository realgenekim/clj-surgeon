from __future__ import annotations

import copy
import json
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
from splice_reference_screen import ideal_requests  # noqa: E402


FIXTURE = REPO_ROOT / "bench/fixtures/splice_reference"


class SpliceReferenceScreenTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.manifest = json.loads((FIXTURE / "spans.json").read_text(encoding="utf-8"))

    def test_fixture_is_exact_and_meaty(self) -> None:
        before = (FIXTURE / "before" / self.manifest["file"]).read_text(encoding="utf-8")
        expected = (FIXTURE / "expected" / self.manifest["file"]).read_text(encoding="utf-8")
        actual = before
        for spec in self.manifest["spans"]:
            self.assertGreaterEqual(len(spec["from"].encode("utf-8")), 100)
            self.assertEqual(1, actual.count(spec["from"]))
            actual = actual.replace(spec["from"], spec["to"], 1)
        self.assertEqual(expected, actual)

    def test_zero_model_instrument_can_cross_kill_threshold(self) -> None:
        ideal = ideal_requests(self.manifest)
        self.assertGreaterEqual(ideal["possible_reduction"]["utf8_bytes"], 0.25)
        self.assertGreaterEqual(ideal["possible_reduction"]["tokens"], 0.25)

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
                        "results": [{"forms": [
                            {"source": f"(def {spec['within']['form']}\n  {spec['from']})"}
                            for spec in self.manifest["spans"]
                        ]}],
                    },
                },
            }
            state = ReferenceState(workspace, self.manifest)
            annotated, labels = state.annotate(response)
            self.assertEqual(["s1", "s2", "s3", "s4"], [label["label"] for label in labels])
            self.assertIn("span_references", annotated["result"]["structuredContent"])
            good = ideal_requests(self.manifest)["R"]["arguments"]
            translated, resolved, refusal = state.translate(good)
            self.assertIsNone(refusal)
            self.assertEqual(4, len(resolved))
            self.assertTrue(all("from" in edit and "from_ref" not in edit
                                for edit in translated["edits"]))
            wrong = copy.deepcopy(good)
            wrong["edits"][0]["to"] = self.manifest["spans"][1]["to"]
            translated, _, refusal = state.translate(wrong)
            self.assertIsNone(translated)
            self.assertEqual("splice-reference-wrong-subject", refusal["error_type"])
            self.assertTrue(refusal["wrong_subject"])
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
                                  "forms": [spec["within"]["form"] for spec in self.manifest["spans"]],
                                  "expect": {"forms": 4}}],
                    "expect": {"requests": 1, "files": 1},
                }},
            })
            labels = inspect["result"]["structuredContent"]["span_references"]["labels"]
            self.assertEqual(4, len(labels))
            edit = request({
                "jsonrpc": "2.0", "id": 4, "method": "tools/call",
                "params": {"name": "edit_clojure",
                           "arguments": ideal_requests(self.manifest)["R"]["arguments"]},
            })
            self.assertTrue(edit["result"]["structuredContent"]["ok"])
            self.assertEqual(4, len(edit["result"]["structuredContent"]["resolved_references"]))
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
