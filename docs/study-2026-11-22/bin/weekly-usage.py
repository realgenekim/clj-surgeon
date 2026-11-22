#!/usr/bin/env python3
"""Read-only weekly Surgeon usage; Python stdlib, no subprocesses or network."""
import argparse
import collections
import csv
import datetime as dt
import importlib.util
from pathlib import Path
import re
import sys

sys.dont_write_bytecode = True
STUDY = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('session_miner', STUDY / 'bin/mine-sessions.py')
miner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(miner)

SOURCE = re.compile(r'\.(?:clj|cljc|edn)\b')
HEREDOC = re.compile(r'\b(?:python[23]?|bb)\b[^\n]*<<')
RECEIPT_READ = re.compile(r'''open\([^\n]*?(?:receipt|facts|\.edn["'])[^\n]*?\)\s*\.read\s*\(''', re.I)
WRITE = re.compile(r'\.write(?:_text|_bytes)?\s*\(|open\([^\n]*,[ \t]*["\'][wax]|\b(?:spit|sed\s+-\S*i|perl\s+-\S*i)\b|\*\*\* (?:Update|Add|Delete) File:')


def monday(value):
    day = dt.date.fromisoformat(value[:10])
    return (day - dt.timedelta(days=day.weekday())).isoformat()


def native_edit(action):
    cmd = action['cmd']
    if HEREDOC.search(cmd) and RECEIPT_READ.search(cmd) and not WRITE.search(cmd):
        return False
    edit = action['cat'] == 'native-edit' or bool(HEREDOC.search(cmd))
    return edit and (any(SOURCE.search(p) for p in action['files']) or bool(SOURCE.search(cmd)))


class WeeklyMiner(miner.Miner):
    def __init__(self, week):
        super().__init__(STUDY)
        self.week = week
        self.weeks = collections.defaultdict(list)

    def finish_turn(self, actions, store, session, session_key):
        super().finish_turn(actions, store, session, session_key)
        calls = [(i, a) for i, a in enumerate(actions) for _ in a['ops']]
        assert len(calls) == len(self.rows)
        for row, (index, action) in zip(self.rows, calls):
            try:
                week = monday(row['date'])
            except ValueError:
                continue
            if self.week is not None and week != self.week:
                continue
            fallback = False
            for later in actions[index + 1:]:
                # Preserve the old next-Surgeon boundary except for refusals.
                if later['surface'] and row['outcome'] != 'refusal':
                    break
                if native_edit(later):
                    fallback = True
                    break
            self.weeks[week].append((row['op'], row['outcome'], int(fallback), (store, session_key)))
        self.rows.clear()
        self.native.clear()
        self.controls.clear()


def session_dates(path, store, reader):
    """Bound selection by first/last dated records, including week-spanning files.

    Read only metadata at the ends before handing selected files to the miner.
    Tail parsing is JSON metadata decoding, not another transcript tool parser.
    """
    first = None
    for record in reader.records(path, store, sample=True):
        try:
            first = dt.date.fromisoformat(str(record.get('timestamp', ''))[:10])
        except ValueError:
            continue
        break
    if first is None:
        return None
    # An oversized final record may leave no dated complete line in the tail.
    # In that case select conservatively rather than miss later-week calls.
    last = dt.date.max
    try:
        with path.open('rb') as handle:
            handle.seek(0, 2)
            size = handle.tell()
            offset = max(0, size - 131072)
            handle.seek(offset)
            lines = handle.read().decode('utf-8', errors='replace').splitlines()
        if offset:
            lines = lines[1:]
        for line in reversed(lines):
            if miner.secret_line(line):
                continue
            try:
                record = miner.json.loads(line)
                day = dt.date.fromisoformat(str(record.get('timestamp', ''))[:10])
            except (ValueError, AttributeError):
                continue
            last = max(first, day)
            break
    except OSError:
        pass
    return first, last


def render(week, rows):
    if not rows:
        return week + '\tno calls'
    ops = collections.defaultdict(lambda: [0, 0, 0])
    for op, outcome, fallback, session in rows:
        counts = ops[op]
        counts[0] += 1
        counts[1] += outcome == 'refusal'
        counts[2] += fallback
    fields = [week, len(rows), len({r[3] for r in rows}),
              sum(r[1] == 'refusal' for r in rows),
              sum(r[1] == 'error' for r in rows), sum(r[2] for r in rows)]
    fields += [op + '=' + '/'.join(map(str, counts))
               for op, counts in sorted(ops.items(), key=lambda item: (-item[1][0], item[0]))]
    return '\t'.join(map(str, fields))


def compare(weeks, path=STUDY / 'calls_per_week.tsv'):
    expected = collections.Counter()
    try:
        with path.open(encoding='utf-8', newline='') as handle:
            for row in csv.DictReader(handle, delimiter='\t'):
                expected[row['week']] += int(row['calls'])
    except (OSError, ValueError, KeyError):
        print('mismatch note: calls_per_week.tsv unavailable or invalid', file=sys.stderr)
        return
    for week in sorted(expected.keys() | weeks.keys()):
        actual = len(weeks.get(week, []))
        if actual != expected[week]:
            print(f'mismatch note: {week}: scanned={actual}, calls_per_week.tsv={expected[week]}', file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--week', type=dt.date.fromisoformat, metavar='YYYY-MM-DD')
    group.add_argument('--all-weeks', action='store_true')
    parser.add_argument('--codex', type=Path, default=Path.home() / '.codex/sessions')
    parser.add_argument('--claude', type=Path, default=Path.home() / '.claude/projects')
    args = parser.parse_args()
    week = None if args.all_weeks else monday((args.week or dt.date.today()).isoformat())
    reader = WeeklyMiner(week)
    start = dt.date.fromisoformat(week) if week else None
    end = start + dt.timedelta(days=7) if start else None
    for store, root, pattern in [('codex', args.codex, '**/rollout-*.jsonl'),
                                  ('claude', args.claude, '**/*.jsonl')]:
        for path in sorted(root.expanduser().glob(pattern)):
            if week:
                dates = session_dates(path, store, reader)
                if dates is None or dates[0] >= end or dates[1] < start:
                    continue
            reader.scan(path, store)
    if args.all_weeks:
        for key in sorted(reader.weeks):
            print(render(key, reader.weeks[key]))
        if not reader.weeks:
            print('no calls')
        compare(reader.weeks)
    else:
        print(render(week, reader.weeks.get(week, [])))
    return 0


if __name__ == '__main__':
    sys.exit(main())
