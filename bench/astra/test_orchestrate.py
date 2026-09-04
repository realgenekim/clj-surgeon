import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import orchestrate as o

class Tests(unittest.TestCase):
    def test_exclusive_receipt(self):
        with tempfile.TemporaryDirectory(dir=o.ROOT) as tmp:
            p=Path(tmp)/'receipt.json'
            o.write(p, {'original': True})
            with self.assertRaises(FileExistsError): o.write(p, {'overwrite': True})
            self.assertEqual({'original': True}, json.loads(p.read_text()))

    def test_monitor_retains_failure_without_touching_slot(self):
        with tempfile.TemporaryDirectory(dir=o.ROOT) as tmp:
            receipt=Path(tmp)
            rc=o.monitor([sys.executable,'-c','raise SystemExit(7)'], receipt, dict(o.os.environ))
            self.assertEqual(7, rc)
            data=json.loads((receipt/'orchestration-result.json').read_text())
            self.assertIsNone(data['contamination']['adapter'])
            self.assertTrue(data['retained'])
            self.assertGreater(len((receipt/'load.jsonl').read_text()),0)

    def test_monitor_spawn_failure_has_terminal_receipt(self):
        with tempfile.TemporaryDirectory(dir=o.ROOT) as tmp:
            receipt=Path(tmp)
            self.assertEqual(127,o.monitor(['/definitely/no/executable'], receipt, dict(os.environ)))
            result=json.loads((receipt/'orchestration-result.json').read_text())
            self.assertEqual('spawn-or-monitor-failed',result['terminal'])
            self.assertIn('No such file',result['error'])

    def test_worker_runs_oracle_after_failed_adapter_and_records_guard(self):
        with tempfile.TemporaryDirectory(dir=o.ROOT) as tmp:
            receipt=Path(tmp)
            (receipt/'tmp').mkdir()
            o.write(receipt/'plan.json', dict(worktree=str(receipt/'fixture'), adapter_command=['fake-adapter'], acceptance_command=['fake-oracle'],adapter_timeout_s=1020))
            with patch.object(o,'validate_frozen'), patch.object(o,'verify_oracle'), patch.object(o,'guards',return_value={'runner':['hash',493]}), patch.object(o,'bounded',side_effect=[{'returncode':2},{'returncode':0}]) as run:
                self.assertEqual(1,o.worker(receipt))
            self.assertEqual(2,run.call_count)
            result=json.loads((receipt/'acceptance-result.json').read_text())
            self.assertTrue(result['independently_accepted'])
            self.assertEqual(2,result['adapter_returncode'])
            self.assertGreaterEqual(result['acceptance_wall_s'],0)

    def test_guards_reject_root_and_bin_links(self):
        with tempfile.TemporaryDirectory(dir=o.ROOT) as tmp:
            root=Path(tmp); (root/'external').mkdir(); (root/'external/same').write_text('same')
            (root/'test').symlink_to(root/'external',target_is_directory=True)
            with self.assertRaisesRegex(ValueError,'root symlink'): o.guards(root)
            (root/'test').unlink(); (root/'test').mkdir()
            (root/'bin').symlink_to(root/'external',target_is_directory=True)
            with self.assertRaisesRegex(ValueError,'root symlink'): o.guards(root)

    def test_oracle_mutation_refused(self):
        with tempfile.TemporaryDirectory(dir=o.ROOT) as tmp:
            root=Path(tmp); (root/'oracle-fixtures').mkdir()
            p=root/'oracle-fixtures/manifest-21.edn';p.write_text('original')
            plan={'oracle_hashes':o.tree_hashes(root/'oracle-fixtures')}
            o.verify_oracle(root,plan)
            p.write_text('changed')
            with self.assertRaisesRegex(ValueError,'snapshot changed'):o.verify_oracle(root,plan)

    def test_separate_load_intervals(self):
        rows=[{'monotonic_s':1,'loadavg':'9 0 0'},{'monotonic_s':3,'loadavg':'12 0 0'}]
        self.assertFalse(o.interval_load(rows,0,2)['contaminated'])
        self.assertTrue(o.interval_load(rows,2,4)['contaminated'])
        self.assertTrue(o.interval_load(rows,0,4)['contaminated'])

    def test_timeout_reaps_setsid_descendant_and_leaves_peer_alive(self):
        with tempfile.TemporaryDirectory(dir=o.ROOT) as tmp:
            root=Path(tmp)
            # Peer is sibling of the isolated supervisor, never its descendant.
            peer=subprocess.Popen([sys.executable,'-c','import time; time.sleep(20)'])
            try:
                child="import os,signal,time; os.setsid(); signal.signal(signal.SIGTERM,signal.SIG_IGN); open("+repr(str(root/'grandchild.pid'))+",'w').write(str(os.getpid())); time.sleep(20)"
                driver="import subprocess,sys,time; subprocess.Popen([sys.executable,'-c',"+repr(child)+"]); time.sleep(20)"
                supervise="import json,os,sys; sys.path.insert(0,"+repr(str(o.ROOT))+"); import orchestrate as o; f=open("+repr(str(root/'log'))+",'w'); r=o.bounded([sys.executable,'-c',"+repr(driver)+"],dict(os.environ),f,0.8); print(json.dumps(r))"
                result=subprocess.run([sys.executable,'-c',supervise],capture_output=True,text=True,timeout=10)
                self.assertEqual(0,result.returncode,result.stderr)
                data=json.loads(result.stdout)
                self.assertEqual('timeout',data['terminal'])
                self.assertEqual(124,data['returncode'])
                self.assertEqual([],data['survivors'])
                grandchild=int((root/'grandchild.pid').read_text())
                self.assertFalse(Path(f'/proc/{grandchild}').exists())
                self.assertIsNone(peer.poll())
            finally:
                peer.terminate();peer.wait(timeout=3)

    def test_real_frozen_hashes_still_match(self):
        self.assertIn('hashes',o.validate_frozen())

if __name__=='__main__':unittest.main()
