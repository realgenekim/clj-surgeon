import io,json,os,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
import server_lifecycle as s

class Tests(unittest.TestCase):
 def test_ready_parser_closed_subset(self):
  value=s.parse_ready('{:project-root "/fixture", :pid 123, :ok true, :port 8301, :server :clj-surgeon}')
  self.assertEqual(123,value['pid']);self.assertTrue(value['ok'])
  for text in ['{:x #=(do bad)}','{:x []}','{:x 1 :x 2}','{:x 1} trailing']:
   with self.assertRaises(ValueError):s.parse_ready(text)
 def test_frozen_command_bounds_and_correct_alias(self):
  cmd=s.launch_command(s.ROOT/'servers/one',s.ROOT/'fixtures/one')
  self.assertEqual(['/usr/bin/taskset','-c','12,13'],cmd[:3])
  self.assertIn('-J-Xmx512m',cmd);self.assertIn('-X:clj-surgeon/mcp',cmd)
  self.assertEqual('8301',cmd[cmd.index(':port')+1]);self.assertIn(':none',cmd)
 def test_ready_evidence_actual_cwd_and_owned_pid(self):
  with tempfile.TemporaryDirectory(dir=s.ROOT) as tmp:
   run=Path(tmp);fixture=s.ROOT/'fixtures/example';pid=os.getpid();born=s.watch.proc_stat(pid)['starttime']
   text='{:project-root '+json.dumps(str(fixture))+', :host "127.0.0.1", :port 8301, :server :clj-surgeon, :ok true, :url "http://127.0.0.1:8301/mcp", :pid '+str(pid)+'}'
   (run/'ready.edn').write_text(text)
   health=b'{"ok":true,"server":"clj-surgeon","tool_runtime":"ready","tool_registry":"ready"}'
   with patch.object(s,'SOURCE',Path.cwd()),patch.object(s,'check_source'),patch.object(s.adapter,'pid_listens',return_value=True),patch.object(s,'urlopen',return_value=io.BytesIO(health)):
    e,_=s.ready_evidence(run,fixture,'a'*40,{pid:born});self.assertEqual(pid,e['port_pid'])
    with self.assertRaisesRegex(ValueError,'not owned'):s.ready_evidence(run,fixture,'a'*40,{pid:'wrongbirth'})
    with self.assertRaisesRegex(ValueError,'binding differs'):s.ready_evidence(run,fixture/'other','a'*40,{pid:born})
   with patch.object(s,'SOURCE',s.ROOT/'not-the-live-cwd'):
    with self.assertRaisesRegex(ValueError,'cwd differs'):s.ready_evidence(run,fixture,'a'*40,{pid:born})
 def test_source_mismatch_refused(self):
  with patch.object(s,'git',return_value='b'*40):
   with self.assertRaisesRegex(ValueError,'exactly requested'):s.check_source('a'*40)
 def test_cleanup_does_not_signal_reused_pid_or_peer(self):
  fake=987654321
  with patch.object(s.watch,'descendants_of',return_value={os.getpid():'ours'}),patch.object(s.watch,'proc_stat',return_value={'starttime':'new','state':'S'}),patch.object(s.os,'kill') as kill:
   self.assertEqual([],s.cleanup_owned({fake:'old'}));kill.assert_not_called()
 def test_stop_owner_mismatch_is_non_signalling(self):
  with tempfile.TemporaryDirectory(dir=s.ROOT) as tmp:
   run=Path(tmp);s.write(run/'owner.json',{'supervisor_pid':987654321,'supervisor_starttime':'old'})
   with patch.object(s.watch,'proc_stat',return_value={'starttime':'new'}),patch.object(s.os,'kill') as kill:
    with self.assertRaisesRegex(ValueError,'absent/reused'):s.stop(run)
    kill.assert_not_called();self.assertFalse((run/'stop-request.json').exists())

if __name__=='__main__':unittest.main()
