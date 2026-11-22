#!/usr/bin/env python3
"""Synthetic regression checks; no private transcripts or dependencies."""
import importlib.util
import pathlib
import sys
import unittest

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('miner', pathlib.Path(__file__).with_name('mine-sessions.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class MinerChecks(unittest.TestCase):
    def test_source_text_is_never_outcome_evidence(self):
        source = '{:operation :show-form :source "(defn x [] {:status :refused :error-type :oops :timeout true})"}'
        self.assertEqual(m.outcome(source), ('success', ''))
        self.assertEqual(m.outcome('{:ns example :forms [{:name timeout :source "REFUSED"}]}'), ('success',''))

    def test_receipts_and_summary_codes(self):
        self.assertEqual(m.outcome('{:status :refused :refusal-code :stale-snapshot}'), ('refusal','stale-snapshot'))
        self.assertEqual(m.outcome('admit_clojure_patch refused · invalid-admit-request · 1 ms'), ('refusal','invalid-admit-request'))
        self.assertEqual(m.outcome('{"ok":false,"error_type":"invalid-workspace-root"}'), ('refusal','invalid-workspace-root'))
        self.assertEqual(m.outcome('{"exit_code":127,"output":"missing binary"}')[0], 'error')
        self.assertEqual(m.outcome('command timed out')[0], 'timeout')
        self.assertEqual(m.outcome('Script running with cell ID 7')[0], 'unknown')

    def test_commands_not_mentions(self):
        for text in ['cat ~/bin/clj-surgeon', 'echo "clj-surgeon :op :oops"', "python3 - <<'PY'\nclj-surgeon :op :oops\nPY"]:
            self.assertEqual(m.cli_ops(text), [])
        self.assertEqual(m.cli_ops('PATH=/bin ~/bin/clj-surgeon :op :split-ns!'), ['split-ns!'])
        self.assertEqual(m.cli_ops('clj-surgeon :ls'), ['ls'])

    def test_literal_wrapper_and_inert_strings(self):
        stats=m.C.Counter()
        self.assertEqual(m.unwrap('exec','text(await tools.exec_command({cmd: "rg foo", max_output_tokens: 3}));',stats)[0][1]['cmd'], 'rg foo')
        self.assertEqual(m.unwrap('exec','text("tools.exec_command({cmd: evil})");',stats)[0][0], 'exec')

    def test_secret_lines_including_escaped_keys(self):
        for line in ['{"password":"value"}', r'{"input":"{\"secret\": \"value\"}"}', 'Bearer '+'a'*25]:
            self.assertTrue(m.secret_line(line))
        self.assertFalse(m.secret_line('ordinary text'))
        self.assertNotIn('/home/forge',m.safe('/home/forge/src/project'))

    def test_turn_boundaries_fallback_and_counts(self):
        miner=m.Miner(pathlib.Path('.'))
        aa=[]
        miner.add_call(aa,'codex','/repo/demo','2026-11-22','mcp__clj_surgeon__edit_clojure',{},'1')
        aa[0]['out']='{"ok":false,"error_type":"stale-snapshot"}'
        miner.add_call(aa,'codex','/repo/demo','2026-11-22','Read',{'file_path':'x.clj'},'2')
        miner.add_call(aa,'codex','/repo/demo','2026-11-22','apply_patch','*** Begin Patch\n*** Update File: x.clj\n-old\n+new\n*** End Patch','3')
        miner.finish_turn(aa,'codex','fixture.jsonl','fixture')
        self.assertEqual(miner.rows[0]['fallback'],1)
        self.assertEqual(miner.rows[0]['next_action'],'native-read')
        self.assertEqual(miner.rows[0]['actions_to_turn_end'],3)

    def test_continuation_output_is_joined(self):
        miner=m.Miner(pathlib.Path('.'));aa=[]
        initial=miner.add_call(aa,'codex','/repo','2026-11-22','mcp__clj_surgeon__inspect_clojure',{},'one')
        miner.attach(initial,'Script running with cell ID 7\nWall time 1 seconds\nOutput:\n',False)
        wait=miner.add_call(aa,'codex','/repo','2026-11-22','wait',{'cell_id':'7'},'two')
        miner.attach(wait,'Script completed\nOutput:\n{"ok":true}',False)
        self.assertEqual(m.outcome(initial[0]['out'])[0],'success')
        self.assertEqual(miner.stats['continuation_outputs_joined'],1)

    def test_move_var_and_single_file_extraction(self):
        miner=m.Miner(pathlib.Path('.'));aa=[]
        patch='*** Begin Patch\n*** Update File: a.clj\n-(defn foo [x] x)\n*** Update File: b.clj\n+(defn foo [x] x)\n*** End Patch'
        miner.add_call(aa,'codex','/repo','2026-11-22','apply_patch',patch,'one')
        self.assertEqual(miner.candidate(aa,2),'move-var')
        aa=[]
        miner.add_call(aa,'claude','/repo','2026-11-22','Edit',{'file_path':'a.clj','old_string':'(inc x)','new_string':'(foo x)'},'one')
        miner.add_call(aa,'claude','/repo','2026-11-22','Edit',{'file_path':'a.clj','old_string':'','new_string':'(defn foo [x] (inc x))'},'two')
        miner.finish_turn(aa,'claude','fixture.jsonl','fixture')
        self.assertEqual(miner.native[0]['class'],'extract-function')



if __name__ == '__main__':
    unittest.main()
