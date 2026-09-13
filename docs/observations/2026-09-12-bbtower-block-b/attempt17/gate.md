{"command": ["make", "landing-gate-prewarm"], "writable_roots": ["/home/forge/src/clj-surgeon-bbtower", "/var/tmp/forge/bbtower-fx/packets/3e047b7d-f7e6-4270-a334-b9d540b28789/tmp", "/home/forge/.local/state/clj-surgeon"], "started": 1789219856.046348, "diagnostic": true}
/home/forge/src/clj-surgeon-bbtower/docs/observations/2026-09-12-bbtower-block-b/attempt17/restricted-gate.py:32: DeprecationWarning: Due to '_pack_', the 'Rule' Structure will use memory layout compatible with MSVC (Windows). If this is intended, set _layout_ to 'ms'. The implicit default is deprecated and slated to become an error in Python 3.19.
  class Rule(ctypes.Structure):
/bin/sh: 1: cannot create /dev/null: Permission denied
/bin/sh: 1: cannot create /dev/null: Permission denied
/bin/sh: 1: cannot create /dev/null: Permission denied
gate-refused: SWI-Prolog (swipl) is not installed.
  The MCP operation contract oracle needs it. That oracle runs
  inside the `mcp-test` gate stage (mcp-test -> mcp-test-checks
  -> mcp-operation-oracle), the 4th of 7 stages.
  `make test` refuses HERE, at the gate entrance, so you do not
  pay the three stages before it to learn this.
  Installing clj-surgeon does NOT need swipl; the gate does.
  macOS:  brew install swi-prolog
  Debian: sudo apt-get install swi-prolog-nox
make: *** [Makefile:260: require-swipl] Error 1
{"exit": 2, "wall_seconds": 1.9989320416934788}
