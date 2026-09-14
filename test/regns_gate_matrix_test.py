"""REGNS-012: independent preservation and inherited-routing ratchets."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("matrix", Path(__file__).with_name("regns_gate_matrix.py"))
matrix = importlib.util.module_from_spec(spec)
spec.loader.exec_module(matrix)


# INTENT-TEST: REGNS-012
# @spec REGNS-012
class MatrixIsolationTest(unittest.TestCase):
    def test_inherited_live_routing_cannot_reach_a_cell(self):
        poisoned = {key: "/live" for key in [
            "CLJ_SURGEON_STATE_HOME", "CLJ_SURGEON_ARTIFACT_ROOT",
            "CLJ_SURGEON_CONTROL_PLANE_ROOT_FILE", "CLJ_SURGEON_TMPDIR_REEXEC",
            "GIT_DIR", "GIT_WORK_TREE", "GIT_INDEX_FILE", "CLASSPATH", "CLJ_CONFIG",
            "MAKEFLAGS", "MAKEFILES", "MAKEOVERRIDES", "MAKELEVEL", "MFLAGS",
            "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "XDG_STATE_HOME",
            "BATTERY_LEDGER_APPEND", "CENSUS_REGENERATE"]}
        poisoned.update(PATH="/tools", HOME="/seat")
        env = matrix.cell_environment(poisoned, Path("/cell"))
        self.assertEqual("/cell/state", env["CLJ_SURGEON_STATE_HOME"])
        self.assertEqual("/cell/state", env["CLJ_SURGEON_ARTIFACT_ROOT"])
        self.assertEqual("/tools", env["PATH"])
        self.assertEqual("/seat", env["HOME"])
        self.assertNotIn("/live", env.values())
        self.assertEqual("/cell/temp", env["TMPDIR"])
        self.assertNotIn("MAKELEVEL", env)
        self.assertNotIn("CENSUS_REGENERATE", env)
        self.assertNotIn("BATTERY_LEDGER_APPEND", env)

    def test_snapshot_rejects_changed_bytes_even_with_identical_git_status(self):
        before = {"status": " M test/example.clj\n", "sha256": {"test/example.clj": "original"}}
        after = {"status": before["status"], "sha256": {"test/example.clj": "plant"}}
        with self.assertRaisesRegex(AssertionError, "test/example.clj"):
            matrix.assert_unchanged(before, after)
        matrix.assert_unchanged(before, before)

    def test_snapshot_rejects_added_and_removed_state_controls(self):
        before = {"status": "", "sha256": {"STATE/battery/walls.edn": "original"}}
        for hashes in [{}, {**before["sha256"], "STATE/controls/plant.edn": "plant"}]:
            with self.assertRaises(AssertionError):
                matrix.assert_unchanged(before, {"status": "", "sha256": hashes})

    def test_snapshot_hashes_state_and_registration_files(self):
        with tempfile.TemporaryDirectory(prefix="regns-snapshot-") as tmp:
            root = Path(tmp) / "repo"
            state = Path(tmp) / "state"
            (root / "test").mkdir(parents=True)
            (state / "battery").mkdir(parents=True)
            source = root / "test/example.clj"
            wall = state / "battery/walls.edn"
            source.write_text("original")
            wall.write_text("wall")
            before = matrix.live_snapshot(root, state)
            self.assertEqual(2, len(before["sha256"]))
            source.write_text("plant")
            with self.assertRaises(AssertionError):
                matrix.assert_unchanged(before, matrix.live_snapshot(root, state))
            source.write_text("original")
            wall.write_text("changed")
            with self.assertRaises(AssertionError):
                matrix.assert_unchanged(before, matrix.live_snapshot(root, state))


if __name__ == "__main__":
    unittest.main()
