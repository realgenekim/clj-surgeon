"""Exercise the two scratch writers not reached by the gate's unit witnesses."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path('test/oracles').resolve()))
import namespace_split_papercut_oracle as oracle

class ScratchWriters(unittest.TestCase):
    def test_papercut_baseline_mirror(self):
        observed = []
        def analyzer(root, baseline=False):
            observed.append(Path(root))
            self.assertTrue(Path(root).is_relative_to(Path(os.environ['TMPDIR'])))
            self.assertEqual('(ns fixture)', Path(root, 'src/fixture.clj').read_text())
            self.assertTrue(baseline)
            return [], None
        with patch.object(oracle, 'git', return_value=(0, 'src/fixture.clj\n', '')), \
             patch.object(oracle, 'git_show', return_value='(ns fixture)'), \
             patch.object(oracle, 'run_kondo', side_effect=analyzer):
            self.assertEqual(([], None), oracle.baseline_kondo('.'))
        self.assertEqual(1, len(observed))
        self.assertFalse(observed[0].exists())

    def test_cell_b_shell_default(self):
        source = Path('test/oracles/cell_b_oracle.sh').read_text().split('FAILS=0', 1)[0]
        env = dict(os.environ)
        env.pop('ORACLE_TMP', None)
        result = subprocess.run(['bash', '-c', source + '\nprintf "%s\\n" "$TMP"'],
                                env=env, text=True, capture_output=True)
        print(result.stderr, end='')
        self.assertEqual(0, result.returncode)
        root = Path(result.stdout.strip())
        self.assertTrue(root.is_relative_to(Path(env['TMPDIR'])), str(root))
        self.assertTrue(root.is_dir())
        shutil.rmtree(root)

if __name__ == '__main__':
    unittest.main(verbosity=2)
