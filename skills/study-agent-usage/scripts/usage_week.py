"""Bounded weekly counts. Example: python3 -I usage_week.py --week 2026-09-07."""
# Known unhandled shapes from the independent review, deferred to a later round:
# timeout N env ...; bash -lc '<cmd>' unwrapping; python writes not at command start;
# printf/echo redirects; perl -pi; Error: text inside MCP content lists.
import argparse
import datetime as dt
import importlib.util
import json
from pathlib import Path
import subprocess
import time
import sys
import re

def load(name):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(name + '.py'))
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
    return module
p = load('usage_week_parse')
def instant(value):
    parsed = dt.datetime.fromisoformat(value.replace('Z', '+00:00'))
    return (parsed if parsed.tzinfo else parsed.replace(tzinfo=dt.timezone.utc)).timestamp()
def arguments(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    for flag in ('week', 'since', 'until', 'codex-root', 'claude-root'): parser.add_argument('--' + flag)
    parser.add_argument('--self-test', action='store_true'); args = parser.parse_args(argv)
    if args.self_test: return args, 0, 0
    try:
        if args.week:
            if args.since or args.until or dt.date.fromisoformat(args.week).weekday():
                raise ValueError('week must be a Monday, without since/until')
            since = instant(args.week); until = since + 7 * 86400
        else: since, until = instant(args.since), instant(args.until)
        if until <= since: raise ValueError('until must follow since')
    except (ValueError, TypeError, AttributeError) as e: parser.error(str(e))
    return args, since, until

def scan_records(lines, session, mtime, stats=None):
    turn, pending, seen, cells = [], {}, set(), {}
    for line in lines:
        if p.secret_line(line): continue
        try: record = json.loads(line)
        except ValueError: continue
        if not isinstance(record, dict): continue
        body = record.get('payload') or record.get('message') or {}
        blocks = body.get('content', [])
        blocks = blocks if isinstance(blocks, list) else []
        user = body.get('role') == 'user' or record.get('type') == 'user'
        if user and (not blocks or any(b.get('type') != 'tool_result' for b in blocks)):
            yield from p.fold_turn(turn)
            turn, pending, cells = [], {}, {}
        for b in ([body] if record.get('type') == 'response_item' else blocks):
            kind = b.get('type'); cid = b.get('call_id', b.get('tool_use_id', b.get('id')))
            if kind in ('function_call_output', 'custom_tool_call_output', 'tool_result'):
                targets = pending.get(cid, []) if cid is not None else []
                output = b.get('output', b.get('content', ''))
                running = re.search(r'Script running with cell ID ([\w-]+)', str(output))
                if running:
                    cells[running[1]] = targets
                    if targets: targets[-1]['partial'] = targets[-1].get('partial', '') + str(output)
                    continue  # Incomplete or unjoined continuations remain unknown.
                if targets:
                    output = targets[-1].get('partial', '') + str(output)
                    targets[-1]['output'] = (dict(is_error=True, content=output)
                                             if b.get('is_error') else output)
            elif kind in ('function_call', 'custom_tool_call', 'tool_use'):
                if cid is not None and cid in seen: continue
                if cid is not None: seen.add(cid)
                _, args = p.payload(b)
                if b.get('name', '').split('.')[-1] == 'wait' and isinstance(args, dict):
                    pending[cid] = cells.get(str(args.get('cell_id')), [])
                    continue
                made = list(p.actions(dict(b, timestamp=record.get('timestamp'), session=session)))
                if stats is not None:
                    when = instant(record['timestamp']) if record.get('timestamp') else mtime
                    if stats['since'] <= when < stats['until']:
                        stats['undecodable'] += sum(bool(a.get('undecodable')) for a in made)
                for a in made:
                    a['time'] = instant(a['timestamp']) if a.get('timestamp') else mtime
                    a['output'] = ''
                # One wrapper receipt belongs only to its last Surgeon call.
                if cid is not None: pending[cid] = [a for a in made if p.detect_call(a)]
                turn.extend(made)
    yield from p.fold_turn(turn)
def scan(path, mtime, stats=None):
    with path.open(errors='replace') as stream:
        yield from scan_records(stream, str(path), mtime, stats)
def main(argv=None):
    args, since, until = arguments(argv); start = time.monotonic()
    if args.self_test:
        return max(subprocess.call([sys.executable, '-B', '-I',
                                   str(Path(__file__).with_name(name + '_test.py'))])
                   for name in ('usage_week_parse', 'usage_week'))
    calls = []; files = 0
    stats = dict(since=since, until=until, undecodable=0)
    roots = ((args.codex_root or '~/.codex/sessions', 'rollout-*.jsonl'),
             (args.claude_root or '~/.claude/projects', '*.jsonl'))
    for root, pattern in roots:
        for path in Path(root).expanduser().rglob(pattern):
            mtime = path.stat().st_mtime
            if since - 86400 <= mtime <= until + 8 * 86400:
                files += 1; calls.extend(c for c in scan(path, mtime, stats) if since <= c['time'] < until)
    totals = p.aggregate([dict(c, op='all') for c in calls]).get('all', dict.fromkeys(p.FIELDS, 0))
    totals['undecodable'] = stats['undecodable']
    print(p.render(p.aggregate(calls),
                   dict(totals, files_scanned=files, seconds=f'{time.monotonic()-start:.3f}')))
    return 0
if __name__ == '__main__':
    raise SystemExit(main())
