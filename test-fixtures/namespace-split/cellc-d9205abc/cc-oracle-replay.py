import pathlib
import sys

oracle = pathlib.Path(__file__).with_name('cc-oracle.py')
source = oracle.read_text().replace(
    "'/var/tmp/forge/plan2/cellC/cc-split.json'", repr(sys.argv[2]))
exec(compile(source, str(oracle), 'exec'))
