from __future__ import annotations

import json
import shutil
import tempfile
import unittest
from pathlib import Path

import proxy
import run_experiment as runner


class ProxyTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = Path(tempfile.mkdtemp(prefix="ordinal-refusal-test-"))
        self.workspace = self.temporary / "workspace"
        shutil.copytree(runner.INITIAL, self.workspace)

    def tearDown(self) -> None:
        shutil.rmtree(self.temporary)

    def screen(self, arm: str) -> proxy.Screen:
        return proxy.Screen(arm, self.workspace, self.temporary / f"{arm}.jsonl")

    def test_control_is_truncated_and_has_no_template(self) -> None:
        screen = self.screen("C")
        result = screen.apply(runner.exact_first_arguments())
        structured = result["structuredContent"]
        self.assertTrue(result["isError"])
        self.assertEqual("batch-form-selection-failed", structured["error_type"])
        self.assertEqual(27, structured["available_form_count"])
        self.assertEqual(10, structured["returned_form_count"])
        self.assertTrue(structured["truncated"])
        self.assertNotIn("prepared_corrected_request", structured)
        self.assertTrue(structured["experiment_only"])
        self.assertFalse(structured["product_contract"])

    def test_treatment_has_complete_numbered_list_and_one_inert_hole(self) -> None:
        screen = self.screen("T")
        result = screen.apply(runner.exact_first_arguments())
        structured = result["structuredContent"]
        self.assertEqual(27, structured["returned_form_count"])
        self.assertFalse(structured["truncated"])
        self.assertEqual(
            {"index": 19, "owner": proxy.TARGET_OWNER}, structured["form_candidates"][18]
        )
        template = structured["prepared_corrected_request"]
        self.assertFalse(template["executable"])
        self.assertFalse(template["authority"])
        self.assertFalse(template["write_authority"])
        self.assertEqual(["arguments.candidate_index"], template["confirmation_holes"])
        self.assertEqual("<CALLER_CONFIRMATION_HOLE>", template["arguments"]["candidate_index"])

    def test_correct_ordinal_mutates_only_target(self) -> None:
        screen = self.screen("T")
        refusal = screen.apply(runner.exact_first_arguments())["structuredContent"]
        result = screen.confirm(
            {"refusal_id": refusal["refusal_id"], "candidate_index": 19}
        )
        self.assertTrue(result["structuredContent"]["ok"])
        self.assertEqual(proxy.TARGET_OWNER, result["structuredContent"]["mutated_owner"])
        self.assertEqual(
            (runner.EXPECTED / runner.TARGET).read_bytes(),
            (self.workspace / runner.TARGET).read_bytes(),
        )

    def test_wrong_ordinal_is_observable_wrong_subject(self) -> None:
        screen = self.screen("T")
        refusal = screen.apply(runner.exact_first_arguments())["structuredContent"]
        result = screen.confirm(
            {"refusal_id": refusal["refusal_id"], "candidate_index": 18}
        )
        self.assertEqual("render-dashboards", result["structuredContent"]["mutated_owner"])
        rows = [json.loads(line) for line in (self.temporary / "T.jsonl").read_text().splitlines()]
        mutation = next(row for row in rows if row["event"] == "mutation_committed")
        self.assertTrue(mutation["wrong_subject"])
        self.assertNotEqual(
            (runner.EXPECTED / runner.TARGET).read_bytes(),
            (self.workspace / runner.TARGET).read_bytes(),
        )

    def test_apply_retry_can_retype_correct_owner(self) -> None:
        screen = self.screen("C")
        screen.apply(runner.exact_first_arguments())
        corrected = runner.exact_first_arguments()
        corrected["changes"][0]["forms"] = [proxy.TARGET_OWNER]
        result = screen.apply(corrected)
        self.assertTrue(result["structuredContent"]["ok"])
        self.assertEqual("apply_clojure_changes", result["structuredContent"]["operation"])

    def test_catalog_is_arm_independent(self) -> None:
        first = proxy.canonical_bytes(proxy.tool_catalog())
        second = proxy.canonical_bytes(proxy.tool_catalog())
        self.assertEqual(first, second)
        self.assertEqual(3, len(proxy.tool_catalog()["tools"]))


if __name__ == "__main__":
    unittest.main()
