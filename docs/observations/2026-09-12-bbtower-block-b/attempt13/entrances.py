"""Real bb startup through the Python spawn adapter, without provider setup."""
import ast
import os
from pathlib import Path
import subprocess

tree = ast.parse(Path("bin/typist-run").read_text())
nodes = [node for node in tree.body
         if isinstance(node, (ast.FunctionDef, ast.ClassDef))
         and node.name in {"bb_command", "_GuardedPopen"}]
scope = {"os": os, "_real_subprocess_popen": subprocess.Popen,
         "guard_spawn": lambda *args, **kwargs: None}
exec(compile(ast.Module(body=nodes, type_ignores=[]), "bin/typist-run", "exec"), scope)
original = os.environ.get("TMPDIR")
try:
    for raw, expected in (("/tmp", "/var/tmp"), ("/dev/shm/x", "/var/tmp"),
                          ("", "/var/tmp"), ("  ", "/var/tmp"),
                          ("/var/tmp/with spaces", "/var/tmp/with spaces")):
        os.environ["TMPDIR"] = raw
        cmd = ["bb", "-e", '(print (System/getProperty "java.io.tmpdir"))']
        child = scope["_GuardedPopen"](cmd, stdout=subprocess.PIPE, text=True)
        output, _ = child.communicate()
        assert child.returncode == 0 and output == expected, (raw, output)
        print(repr(raw), "=>", repr(output))
finally:
    if original is None:
        os.environ.pop("TMPDIR", None)
    else:
        os.environ["TMPDIR"] = original
subprocess.run(["bash", "-n", "bin/mission"], check=True)
subprocess.run(["bin/mission", "--help"], check=True, stdout=subprocess.DEVNULL)
print("Python spawn matrix and mission help: passed")
