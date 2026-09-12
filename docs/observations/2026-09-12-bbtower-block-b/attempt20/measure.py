"""Sequential six-per-runtime namespace controls; never discard a failed run."""
import datetime
import json
import os
from pathlib import Path
import subprocess
import time

HERE = Path(__file__).resolve().parent
ROOT = Path.cwd().resolve()
ENV = dict(os.environ, TMPDIR='/var/tmp/forge/bbtower-fx',
           JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx')
ENV.pop('_JAVA_OPTIONS', None)
BB = ['bb', '-Xmx1g', '-Djava.io.tmpdir=/var/tmp/forge/bbtower-fx']

def main():
    names = subprocess.check_output(BB + ['-e',
        "(require '[clj-surgeon.lane-manifest :as lm]) "
        "(doseq [n (sort (keys lm/runtime-measurements))] (println n))"],
        env=ENV, text=True).splitlines()
    subject = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    runner = str(HERE / 'measure_runner.clj')
    (HERE / 'measurements').mkdir(exist_ok=True)
    with (HERE / 'measurement-processes.jsonl').open('x') as ledger:
        for name in names:
            for runtime in ['jvm', 'bb']:
                for run in range(1, 7):
                    stem = HERE / 'measurements' / f'{name}-{runtime}-{run}'
                    receipt = str(stem) + '.edn'
                    command = (['clojure', '-J-Xmx1g', '-Sdeps',
                                '{:aliases {:attempt20 {:extra-paths ["' + str(HERE) + '"]}}}',
                                '-M:clj-surgeon/test-deps:attempt20', '-m', 'measure-runner']
                               if runtime == 'jvm' else BB + [runner])
                    command += [runtime, name, receipt]
                    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
                    tick = time.monotonic()
                    with open(str(stem) + '.log', 'x') as log:
                        result = subprocess.run(command, env=ENV, stdout=log, stderr=subprocess.STDOUT)
                    row = dict(namespace=name, runtime=runtime, run=run, subject=subject,
                               started=started, command=command, exit=result.returncode,
                               process_seconds=time.monotonic()-tick,
                               receipt=os.path.relpath(receipt, ROOT))
                    ledger.write(json.dumps(row) + '\n')
                    ledger.flush()
                    print(json.dumps(row), flush=True)
                    if not Path(receipt).exists():
                        raise RuntimeError(f'No namespace receipt: {stem}')

if __name__ == '__main__':
    main()
