"""Compare unresolved findings against an independent, exact-base lint capture."""
from collections import Counter
import pathlib
import re
import sys


# @spec NS-SPLIT-046
def findings(path):
    text = pathlib.Path(path).read_text()
    if not text.strip():
        raise ValueError(f"empty analyzer output: {path}")
    result = Counter()
    for line in text.splitlines():
        if re.search(r"unresolved (var|namespace|symbol)", line, re.IGNORECASE):
            match = re.fullmatch(r"(.+?):\d+:\d+: (error|warning|info): (.+)", line)
            if not match:
                raise ValueError(f"unrecognized unresolved finding: {line}")
            result[match.groups()] += 1
    return result


def main(baseline_path, candidate_path):
    baseline, candidate = findings(baseline_path), findings(candidate_path)
    introduced = candidate - baseline
    retained = candidate & baseline
    print(f"A8 baseline unresolved: {sum(baseline.values())}; retained: {sum(retained.values())}; new/increased: {sum(introduced.values())}")
    for label, values in [("BASELINE", baseline), ("NEW", introduced)]:
        for (file, level, message), count in sorted(values.items()):
            print(f"      {label} {count} x {file}: {level}: {message}")
    return bool(introduced)


if __name__ == "__main__":
    try:
        sys.exit(main(*sys.argv[1:]))
    except (ValueError, OSError) as error:
        print(f"A8 comparison error: {error}", file=sys.stderr)
        sys.exit(1)
