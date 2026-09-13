"""Pure instrument checks. Never invoke init/cell, a JVM, or a timing experiment."""
import ast
import importlib.util
import sys
from pathlib import Path

sys.dont_write_bytecode = True

runner = Path(__file__).resolve().parents[1] / "round5/measure.py"
spec = importlib.util.spec_from_file_location("measure", runner)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
assert module.verdict_status("NATIVE", False, True, None) == "executed-without-refusal"
assert module.verdict_status("NATIVE", False, False, "anything") == "failed"
assert module.verdict_status("PROBE", False, False, "non-test-target") == "refused"
assert module.verdict_status("PROBE", False, False, None) == "failed"
assert module.verdict_status("NATIVE", True, True, None) == "accepted"
assert module.verdict_status("PROBE", True, True, None) == "accepted"
assert module.verdict_status("NATIVE", True, False, None) == "failed"
assert module.verdict_status("NATIVE", True, False, "anything") == "failed"
tree = ast.parse(runner.read_text())
main = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == "main")
assignments = {n.targets[0].id: n.lineno for n in ast.walk(main)
               if isinstance(n, ast.Assign) and isinstance(n.targets[0], ast.Name)}
assert assignments["instruction_t0"] < assignments["t0"] < assignments["t1"]
source = runner.read_text()
assert "secondary_wall_ms=(t1-instruction_t0)/1e6" in source
assert "complete_wall_ms=(t1-t0)/1e6" in source
print("PASS: eight verdict cases; mechanical primary/secondary endpoints; no cell executed")
