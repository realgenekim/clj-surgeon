"""Exercise generated launchers entirely in an owned scratch install."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

repo = Path.cwd()
root = Path(tempfile.mkdtemp(prefix="attempt13-launchers-", dir="/var/tmp/forge/bbtower-fx"))
try:
    fake = root / "fake"
    fake.mkdir()
    bb = fake / "bb"
    bb.write_text('#!/bin/sh\nprintf "%s\\n" "$@"\n')
    bb.chmod(0o755)
    for target in ("install-cli", "install-dev-cli"):
        launcher = root / target
        subprocess.run(["make", target, "CLI_DEST=" + str(launcher),
                        "INSTALL_ROOT=" + str(root / "install")], check=True)
        for raw, expected in ((None, "/var/tmp"), ("", "/var/tmp"),
                              ("  ", "/var/tmp"), ("/tmp", "/var/tmp"),
                              ("/tmp/x", "/var/tmp"), ("/dev/shm", "/var/tmp"),
                              ("/dev/shm/x", "/var/tmp"),
                              (str(root / "with spaces"), str(root / "with spaces"))):
            env = dict(os.environ, PATH=str(fake) + os.pathsep + os.environ["PATH"])
            if raw is None:
                env.pop("TMPDIR", None)
            else:
                env["TMPDIR"] = raw
            result = subprocess.run([str(launcher), "arg with spaces"], env=env,
                                    text=True, capture_output=True, check=True)
            argv = result.stdout.splitlines()
            assert argv[0] == "-Djava.io.tmpdir=" + expected, argv
            assert argv[-1] == "arg with spaces", argv
            print(json.dumps({"target": target, "TMPDIR": raw, "argv": argv}))
        result = subprocess.run([str(launcher), "--version"], text=True,
                                capture_output=True, check=True)
        assert ':tool "clj-surgeon"' in result.stdout, result
        print(target, "real bb CLI:", result.stdout.strip())
finally:
    for path in root.rglob("*"):
        if not path.is_symlink():
            path.chmod(0o700 if path.is_dir() else 0o600)
    shutil.rmtree(root)
