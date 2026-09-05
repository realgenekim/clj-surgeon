import json,tempfile,unittest
from datetime import datetime,timezone
from pathlib import Path
from unittest.mock import patch
import launch_pairs as p

class Tests(unittest.TestCase):
 def test_quiet_owner_expiry_and_missing_are_strict(self):
  now=datetime(2026,9,5,tzinfo=timezone.utc)
  self.assertEqual('astra',p.quiet('owner=astra until=2026-09-05T00:20:00Z',now)['owner'])
  for value in ['owner=fable until=2026-09-05T00:20:00Z','owner=astra end=00:20Z','owner=astra until=2026-09-04T23:59:59Z','owner=astra until=2026-09-05T00:20:00','owner=astra owner=fable until=2026-09-05T00:20:00Z']:
   with self.assertRaises(ValueError):p.quiet(value,now)
 def test_explicit_slice_and_native_no_server(self):
  rows=p.selection(5,6)
  self.assertEqual([5,6],[i for i,_ in rows])
  native=p.commands(rows[0][1]);tool=p.commands(rows[1][1])
  self.assertIsNone(native['start']);self.assertIsNone(native['stop'])
  self.assertIn('--mcp-url',tool['orchestrate']);self.assertIn(p.SERVER_SHA,tool['start'])
  for first,last in [(0,1),(5,4),(1,25)]:
   with self.assertRaises(ValueError):p.selection(first,last)
 def test_structured_nested_refusal_both_clients(self):
  old={'payload':{'type':'mcp_tool_call_end','result':{'Ok':{'structuredContent':{'ok':False,'error_type':'refused-old'}}}}}
  new={'payload':{'type':'item_completed','item':{'type':'McpToolCall','tool':'alias_migration','status':'completed','result':{'structuredContent':{'ok':False,'error_type':'refused-new'}}}}}
  result=p.observed_refusals([old,new])
  self.assertEqual(2,result['mcp_completed_events']);self.assertEqual(2,len(result['typed_refusals']))
  self.assertEqual(0,p.observed_refusals([{'payload':{'type':'custom_tool_call','input':'some dynamic JS'}}])['mcp_completed_events'])
 def test_tool_always_stopped_on_orchestrator_failure(self):
  with tempfile.TemporaryDirectory(dir=p.ROOT) as tmp:
   root=Path(tmp);server=root/'server';server.mkdir();(server/'owner.json').write_text('{}')
   ps={'server':server,'arm':root/'arm','receipt':root/'receipt','fixture':root/'fixture'}
   row={'name':'fake-tool','route':'tool','model':'gpt-6-astra'}
   with patch.object(p,'unused',return_value=ps),patch.object(p,'check_quiet',return_value={'owner':'astra'}),patch.object(p,'invoke',side_effect=[0,7,0]) as invoke:
    with self.assertRaisesRegex(ValueError,'orchestrator-nonzero'):p.run_row(5,row,root)
   self.assertEqual(3,invoke.call_count)
   self.assertIn('stop',invoke.call_args_list[-1].args[0])
   self.assertEqual('orchestrator-nonzero',json.loads((root/'05-fake-tool/result.json').read_text())['failure'])
 def test_refusal_stops_successful_outer_process(self):
  with tempfile.TemporaryDirectory(dir=p.ROOT) as tmp:
   root=Path(tmp);row={'name':'fake-native','route':'native','model':'gpt-6-astra'}
   with patch.object(p,'unused',return_value={}),patch.object(p,'check_quiet',return_value={}),patch.object(p,'invoke',return_value=0) as invoke,patch.object(p,'outcome',return_value={'failures':['detected-tool-refusal']}):
    with self.assertRaisesRegex(ValueError,'detected-tool-refusal'):p.run_row(6,row,root)
   self.assertEqual(1,invoke.call_count)
 def test_existing_arm_never_skipped(self):
  with tempfile.TemporaryDirectory(dir=p.ROOT) as tmp:
   root=Path(tmp);(root/'arm').mkdir();ps={k:root/k for k in ['arm','receipt','server','fixture']}
   with patch.object(p,'paths',return_value=ps):
    with self.assertRaisesRegex(ValueError,'existing arm'):p.unused({'name':'existing'})

if __name__=='__main__':unittest.main()
