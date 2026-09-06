"""Fable real-1 regression: a terminal newline is not hunk context."""
import ast
from pathlib import Path
import re
import unittest

source = Path(__file__).resolve().parents[2] / 'bin' / 'typist-run'
tree = ast.parse(source.read_text())
function = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == 'parse_unified')
namespace = {'HUNK_HEADER': re.compile(r'^@@')}
exec(compile(ast.Module(body=[function], type_ignores=[]), str(source), 'exec'), namespace)

class MidFileHunk(unittest.TestCase):
    def test_terminal_newline_does_not_become_context(self):
        prefix = '--- a/src/a.clj\n+++ b/src/a.clj\n@@\n-old\n+new'
        for suffix in ['', '\n']:
            self.assertEqual([{'path': 'src/a.clj', 'hunks': [[('-', 'old'), ('+', 'new')]]}], namespace['parse_unified'](prefix + suffix))

    def test_explicit_blank_context_survives(self):
        parsed = namespace['parse_unified']('--- a/src/a.clj\n+++ b/src/a.clj\n@@\n-old\n+new\n \n')
        self.assertEqual((' ', ''), parsed[0]['hunks'][0][-1])

if __name__ == '__main__':
    unittest.main()
