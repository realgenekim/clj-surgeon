"""Pure adapter refusals plus filesystem overwrite/config boundaries; no model/JVM."""
import argparse
import json
from pathlib import Path
import tempfile
import subprocess
import sys
import unittest
from unittest.mock import patch

import adapter as a


class AdapterTests(unittest.TestCase):
    def test_requested_models_reach_real_codex(self):
        for model in a.MODELS:
            argv = a.model_argv(model, '/fixture', '12,13')
            self.assertEqual(model, argv[argv.index('-m') + 1])
            self.assertEqual(str(a.CODEX), argv[3])
            self.assertIn('model_reasoning_effort="high"', argv)
            self.assertNotIn('--json', argv)
            self.assertNotIn('sol-yolo', ' '.join(argv))
            self.assertEqual('-', argv[-1])
            self.assertFalse(any('mcp_servers' in x for x in argv))

    def test_bad_model_and_cpu_rejected(self):
        for model, cpus in [('sol', '1'), ('gpt-6-astra', '1;kill')]:
            with self.assertRaises(ValueError):
                a.model_argv(model, '/fixture', cpus)

    def test_root_refusal(self):
        for path in ['/tmp/no', '/home/forge/src/clj-surgeon', str(a.ROOT), str(a.ROOT / '../escape')]:
            with self.assertRaises(ValueError):
                a.confined(path)

    def test_model_session_attestation(self):
        uuid = '12345678-1234-1234-1234-123456789abc'
        rows = [{'type': 'session_meta', 'payload': {'id': uuid}},
                {'type': 'turn_context', 'payload': {'model': 'gpt-6-astra'}}]
        run = {'rollout_binding': 'session-id:' + uuid}
        log = 'session id: ' + uuid
        self.assertEqual('gpt-6-astra', a.resolved_model(rows, run, log, 'gpt-6-astra')['resolved_model'])
        for changed_rows, changed_run, changed_log, model in [
                (rows, run, log, 'gpt-5.6-sol'),
                (rows[:1], run, log, 'gpt-6-astra'),
                (rows, {}, log, 'gpt-6-astra'),
                (rows, run, '', 'gpt-6-astra'),
                (rows, run, log.replace('12345678', '87654321'), 'gpt-6-astra')]:
            with self.assertRaises(ValueError):
                a.resolved_model(changed_rows, changed_run, changed_log, model)

    def test_tool_binding(self):
        url = 'http://127.0.0.1:8300/mcp'
        health = b'{"ok":true,"server":"clj-surgeon","tool_runtime":"ready","tool_registry":"ready"}'
        ready = dict(mcp_url=url, healthz_url='http://127.0.0.1:8300/healthz',
                     server_sha='a'*40, project_root='/fixture', port_pid=123,
                     healthz_sha256=a.digest(health))
        a.validate_ready(ready, url, 'a'*40, Path('/fixture'), health)
        for key, value in [('project_root', '/peer'), ('server_sha', 'b'*40),
                           ('port_pid', 0), ('healthz_sha256', 'bad'),
                           ('healthz_url', 'http://127.0.0.1:8301/healthz')]:
            with self.assertRaises(ValueError):
                a.validate_ready(dict(ready, **{key: value}), url, 'a'*40, Path('/fixture'), health)
        argv = a.model_argv('gpt-6-astra', '/fixture', '12', url)
        self.assertIn('mcp_servers.surgeon.required=true', argv)

    def test_only_allocated_ports_allowed(self):
        for port in (7888, 7890, 8171, 8299, 8340):
            with self.assertRaises(ValueError):
                a.validate_url(f'http://127.0.0.1:{port}/mcp')
        for port in (8300, 8339):
            a.validate_url(f'http://127.0.0.1:{port}/mcp')

    def test_existing_arm_refused_before_launch(self):
        with tempfile.TemporaryDirectory(dir=a.ROOT, prefix='adapter-test-') as tmp:
            args = argparse.Namespace(arm=tmp, worktree=str(a.ROOT / 'fixture'),
                                      prompt=str(a.ROOT / 'prompt'), canonical=str(a.ROOT / 'canonical'))
            with patch.object(a, 'command') as launch:
                with self.assertRaisesRegex(ValueError, 'already exists'):
                    a.prepare(args)
                launch.assert_not_called()

    def test_project_config_refused_before_launch(self):
        with tempfile.TemporaryDirectory(dir=a.ROOT, prefix='adapter-test-') as tmp:
            root = Path(tmp)
            (root / 'fixture/.codex').mkdir(parents=True)
            args = argparse.Namespace(arm=str(root / 'arm'), worktree=str(root / 'fixture'),
                                      prompt=str(root / 'prompt'), canonical=str(root / 'canonical'),
                                      model='gpt-6-astra', cpus='12', mcp_url=None)
            with patch.object(a, 'command') as launch:
                with self.assertRaisesRegex(ValueError, '.codex config forbidden'):
                    a.prepare(args)
                launch.assert_not_called()

    def test_watcher_driver_stdin_and_announced_session_end_to_end(self):
        with tempfile.TemporaryDirectory(dir=a.ROOT, prefix='adapter-test-') as tmp:
            arm = Path(tmp)
            sessions = arm / 'sessions'
            sessions.mkdir()
            uuid = '12345678-1234-1234-1234-123456789abc'
            rows = [{'type': 'session_meta', 'payload': {'id': uuid}},
                    {'type': 'turn_context', 'payload': {'model': 'gpt-6-astra'}},
                    {'type': 'response_item', 'payload': {'type': 'message', 'role': 'assistant'}}]
            (sessions / (uuid + '.jsonl')).write_text('\n'.join(map(json.dumps, rows)) + '\n')
            (arm / 'prompt.txt').write_text('exact stdin prompt')
            fake = "import sys; assert sys.stdin.read() == 'exact stdin prompt'; print('session id: " + uuid + "', flush=True)"
            a.write_json(arm / 'command.json', [sys.executable, '-c', fake])
            a.write_json(arm / 'attest.json', {'attest_ok': True,
                         'start_utc': a.datetime.now(a.timezone.utc).isoformat()})
            result = subprocess.run([sys.executable, str(a.METERS / 'watch.py'), '--arm', str(arm),
                                     '--codex-home', str(sessions), '--max-wall', '10', '--',
                                     sys.executable, str(a.HERE), 'driver', '--arm', str(arm)],
                                    capture_output=True, text=True, timeout=15)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            run = json.loads((arm / 'run.json').read_text())
            self.assertEqual(0, run['driver_rc'])
            self.assertEqual(0, run['orphans_after_reap'])
            checked = a.resolved_model(rows, run, (arm / 'driver-output.log').read_text(), 'gpt-6-astra')
            self.assertEqual(uuid, checked['session_id'])


if __name__ == '__main__':
    unittest.main()
