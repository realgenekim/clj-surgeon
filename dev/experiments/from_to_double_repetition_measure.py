#!/usr/bin/env python3
import argparse
import ctypes
import datetime as dt
import glob
import hashlib
import itertools
import json
import math
from pathlib import Path

from rapidfuzz import __version__ as rapidfuzz_version
from rapidfuzz.distance import LCSseq


def sha256_bytes(value):
    return hashlib.sha256(value).hexdigest()


def compact_json(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")


def nearest_rank(values, percentiles=(0, 25, 50, 75, 90)):
    ordered = sorted(values)
    if not ordered:
        return {"n": 0, "min": None, "p25": None, "median": None, "p75": None, "p90": None}
    labels = ("min", "p25", "median", "p75", "p90")
    result = {"n": len(ordered)}
    for label, p in zip(labels, percentiles):
        index = 0 if p == 0 else math.ceil(p * len(ordered) / 100) - 1
        result[label] = ordered[index]
    return result


def percent(value):
    return round(value * 100, 6)


def ratio_quantiles(numerators, denominators):
    values = [percent(n / d) for n, d in zip(numerators, denominators) if d]
    return nearest_rank(values)


def suffix_automaton(data):
    transitions = [{}]
    links = [-1]
    lengths = [0]
    last = 0
    for byte in data:
        cur = len(transitions)
        transitions.append({})
        lengths.append(lengths[last] + 1)
        links.append(0)
        p = last
        while p >= 0 and byte not in transitions[p]:
            transitions[p][byte] = cur
            p = links[p]
        if p < 0:
            links[cur] = 0
        else:
            q = transitions[p][byte]
            if lengths[p] + 1 == lengths[q]:
                links[cur] = q
            else:
                clone = len(transitions)
                transitions.append(dict(transitions[q]))
                lengths.append(lengths[p] + 1)
                links.append(links[q])
                while p >= 0 and transitions[p].get(byte) == q:
                    transitions[p][byte] = clone
                    p = links[p]
                links[q] = clone
                links[cur] = clone
        last = cur
    return transitions, links, lengths


def greedy_six_gram_coverage(source, target):
    if len(source) < 6 or len(target) < 6:
        return 0
    transitions, links, lengths = suffix_automaton(source[::-1])
    state = 0
    matched = 0
    longest_ending = []
    for byte in target[::-1]:
        while state and byte not in transitions[state]:
            state = links[state]
            matched = min(matched, lengths[state])
        if byte in transitions[state]:
            state = transitions[state][byte]
            matched += 1
        else:
            state = 0
            matched = 0
        longest_ending.append(matched)
    longest_starting = list(reversed(longest_ending))
    covered = 0
    index = 0
    while index < len(target):
        length = longest_starting[index]
        if length >= 6:
            covered += length
            index += length
        else:
            index += 1
    return covered


def naive_six_gram_coverage(source, target):
    covered = 0
    i = 0
    while i < len(target):
        best = 0
        for j in range(len(source)):
            length = 0
            while i + length < len(target) and j + length < len(source) and target[i + length] == source[j + length]:
                length += 1
            best = max(best, length)
        if best >= 6:
            covered += best
            i += best
        else:
            i += 1
    return covered


def load_min_runs(path):
    library = ctypes.CDLL(path)
    function = library.min_lcs_edit_runs
    pointer = ctypes.POINTER(ctypes.c_ubyte)
    function.argtypes = [pointer, ctypes.c_int, pointer, ctypes.c_int, ctypes.POINTER(ctypes.c_int)]
    function.restype = ctypes.c_int

    def invoke(a, b):
        aa = (ctypes.c_ubyte * len(a)).from_buffer_copy(a)
        bb = (ctypes.c_ubyte * len(b)).from_buffer_copy(b)
        lcs = ctypes.c_int()
        runs = function(aa, len(a), bb, len(b), ctypes.byref(lcs))
        return lcs.value, runs

    return invoke


def brute_lcs_runs(a, b):
    n, m = len(a), len(b)
    bad = (-10**9, 10**9)
    match = [[bad] * (m + 1) for _ in range(n + 1)]
    edit = [[bad] * (m + 1) for _ in range(n + 1)]
    match[0][0] = (0, 0)

    def better(x, y):
        return x if x[0] > y[0] or (x[0] == y[0] and x[1] < y[1]) else y

    for i in range(n + 1):
        for j in range(m + 1):
            if i == 0 and j == 0:
                continue
            if i and j and a[i - 1] == b[j - 1]:
                match[i][j] = better((match[i - 1][j - 1][0] + 1, match[i - 1][j - 1][1]),
                                     (edit[i - 1][j - 1][0] + 1, edit[i - 1][j - 1][1]))
            candidates = []
            if i:
                candidates.extend((edit[i - 1][j], (match[i - 1][j][0], match[i - 1][j][1] + 1)))
            if j:
                candidates.extend((edit[i][j - 1], (match[i][j - 1][0], match[i][j - 1][1] + 1)))
            for candidate in candidates:
                edit[i][j] = better(edit[i][j], candidate)
    return better(match[n][m], edit[n][m])


def self_test(min_runs):
    strings = [b""]
    for length in range(1, 6):
        strings.extend(bytes(value) for value in itertools.product(range(2), repeat=length))
    cases = 0
    for a in strings:
        for b in strings:
            expected = brute_lcs_runs(a, b)
            actual = min_runs(a, b)
            assert actual == expected, (a, b, expected, actual)
            assert actual[0] == LCSseq.similarity(a, b)
            assert greedy_six_gram_coverage(a, b) == naive_six_gram_coverage(a, b)
            cases += 1
    longer = [(b"abcdefabcdef", b"xxabcdefyyabcdefzz"),
              (b"0123456789", b"012xxx345yyy6789"),
              (b"same", b"same")]
    for a, b in longer:
        assert greedy_six_gram_coverage(a, b) == naive_six_gram_coverage(a, b)
        cases += 1
    return cases


def parse_timestamp(value):
    return dt.datetime.fromisoformat(value.replace("Z", "+00:00"))


def round_half_up(value):
    return int(math.floor(value + 0.5))


def token_projection(byte_count):
    return round_half_up(byte_count / 4)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--telemetry-root", required=True)
    parser.add_argument("--since", required=True)
    parser.add_argument("--until", required=True)
    parser.add_argument("--helper-lib", required=True)
    parser.add_argument("--helper-source", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    min_runs = load_min_runs(args.helper_lib)
    self_test_cases = self_test(min_runs)
    since = parse_timestamp(args.since)
    until = parse_timestamp(args.until)
    calls = []
    contributing = {}
    for filename in sorted(glob.glob(str(Path(args.telemetry_root) / "*.jsonl"))):
        path = Path(filename)
        used = False
        with path.open("rb") as stream:
            for line_number, line in enumerate(stream, 1):
                try:
                    event = json.loads(line)
                    timestamp = parse_timestamp(event.get("timestamp", ""))
                except (ValueError, json.JSONDecodeError):
                    continue
                if since <= timestamp <= until and event.get("event") == "tool.call":
                    calls.append((timestamp, path.name, line_number, event))
                    used = True
        if used:
            content = path.read_bytes()
            contributing[path.name] = {"bytes": len(content), "sha256": sha256_bytes(content)}
    calls.sort(key=lambda row: (row[0], row[1], row[2]))
    tool_counts = {}
    for _, _, _, event in calls:
        tool = event.get("tool")
        tool_counts[tool] = tool_counts.get(tool, 0) + 1
    writes = [event for _, _, _, event in calls if event.get("tool") == "apply_clojure_changes"]

    canonical_parts = []
    canonical_sizes = []
    pairs = []
    exclusions = {"changes_one_sided_or_non_string": 0, "edits_one_sided_or_non_string": 0}
    families = {"find_replace": 0, "from_to": 0}
    write_pair_counts = [0] * len(writes)
    for write_index, event in enumerate(writes):
        request = dict(event["request"])
        request.pop("workspace_root", None)
        canonical = compact_json(request)
        canonical_parts.append(canonical)
        canonical_sizes.append(len(canonical))
        edits = request.get("edits", [])
        if isinstance(edits, list):
            for item in edits:
                if isinstance(item, dict) and isinstance(item.get("from"), str) and isinstance(item.get("to"), str):
                    pairs.append({"family": "from_to", "write": write_index,
                                  "from": item["from"].encode("utf-8"), "to": item["to"].encode("utf-8")})
                    families["from_to"] += 1
                    write_pair_counts[write_index] += 1
                elif isinstance(item, dict) and ("from" in item or "to" in item):
                    exclusions["edits_one_sided_or_non_string"] += 1
        changes = request.get("changes", [])
        if isinstance(changes, list):
            for item in changes:
                if isinstance(item, dict) and isinstance(item.get("find"), str) and isinstance(item.get("replace"), str):
                    pairs.append({"family": "find_replace", "write": write_index,
                                  "from": item["find"].encode("utf-8"), "to": item["replace"].encode("utf-8")})
                    families["find_replace"] += 1
                    write_pair_counts[write_index] += 1
                elif isinstance(item, dict) and ("find" in item or "replace" in item):
                    exclusions["changes_one_sided_or_non_string"] += 1

    for pair in pairs:
        lcs_fast = LCSseq.similarity(pair["from"], pair["to"])
        lcs_exact, runs = min_runs(pair["from"], pair["to"])
        assert lcs_fast == lcs_exact
        pair["lcs"] = lcs_exact
        pair["six"] = greedy_six_gram_coverage(pair["from"], pair["to"])
        pair["k"] = runs

    total_from = sum(len(pair["from"]) for pair in pairs)
    total_to = sum(len(pair["to"]) for pair in pairs)
    total_lcs = sum(pair["lcs"] for pair in pairs)
    total_six = sum(pair["six"] for pair in pairs)
    total_request = sum(canonical_sizes)
    paired_writes = sum(count > 0 for count in write_pair_counts)

    def metric_receipt(key):
        pair_bytes = [pair[key] for pair in pairs]
        pair_denominators = [len(pair["to"]) for pair in pairs]
        write_bytes = [0] * len(writes)
        write_denominators = [0] * len(writes)
        for pair in pairs:
            write_bytes[pair["write"]] += pair[key]
            write_denominators[pair["write"]] += len(pair["to"])
        return {
            "overlap_bytes": sum(pair_bytes),
            "to_bytes": total_to,
            "share_of_to_percent": percent(sum(pair_bytes) / total_to) if total_to else None,
            "share_of_full_request_percent": percent(sum(pair_bytes) / total_request) if total_request else None,
            "pair_coverage_percent_quantiles": ratio_quantiles(pair_bytes, pair_denominators),
            "pair_overlap_byte_quantiles": nearest_rank(pair_bytes),
            "write_coverage_percent_quantiles_paired_writes": ratio_quantiles(write_bytes, write_denominators),
            "write_overlap_byte_quantiles_all_writes": nearest_rank(write_bytes),
            "writes_with_nonzero_overlap": sum(value > 0 for value in write_bytes),
        }

    family_metrics = {}
    for family in families:
        subset = [pair for pair in pairs if pair["family"] == family]
        denom = sum(len(pair["to"]) for pair in subset)
        family_metrics[family] = {
            "pairs": len(subset),
            "from_bytes": sum(len(pair["from"]) for pair in subset),
            "to_bytes": denom,
            "lcs_bytes": sum(pair["lcs"] for pair in subset),
            "lcs_share_of_to_percent": percent(sum(pair["lcs"] for pair in subset) / denom) if denom else None,
            "six_gram_bytes": sum(pair["six"] for pair in subset),
            "six_gram_share_of_to_percent": percent(sum(pair["six"] for pair in subset) / denom) if denom else None,
        }

    realistic = round_half_up(0.095 * ((total_from + total_to) / len(pairs)))
    overheads = {"optimistic": round_half_up(0.5 * realistic),
                 "realistic": realistic,
                 "pessimistic": round_half_up(2 * realistic)}
    anchor_bytes = 162345
    decided_bytes = 105177

    def savings_projection(overlap_key):
        projection = {}
        gross = sum(pair[overlap_key] for pair in pairs)
        for name, overhead in overheads.items():
            nets = [max(0, pair[overlap_key] - pair["k"] * overhead) for pair in pairs]
            penalties = [pair["k"] * overhead for pair in pairs]
            write_nets = [0] * len(writes)
            for pair, net in zip(pairs, nets):
                write_nets[pair["write"]] += net
            net_total = sum(nets)
            combined = anchor_bytes + net_total
            remaining = total_request - combined
            projection[name] = {
                "overlap_basis": overlap_key,
                "overhead_bytes_per_splice_op": overhead,
                "gross_overlap_bytes": gross,
                "unclamped_grammar_penalty_bytes": sum(penalties),
                "net_removable_bytes": net_total,
                "net_removable_tokens_projection_4_bytes_per_token": token_projection(net_total),
                "net_share_of_full_request_percent": percent(net_total / total_request),
                "economic_pairs": sum(net > 0 for net in nets),
                "economic_pair_share_percent": percent(sum(net > 0 for net in nets) / len(pairs)),
                "pair_net_byte_quantiles": nearest_rank(nets),
                "write_net_byte_quantiles_all_writes": nearest_rank(write_nets),
                "write_net_token_quantiles_all_writes_projection_4_bytes_per_token": nearest_rank(
                    [token_projection(value) for value in write_nets]),
                "writes_with_nonzero_net": sum(value > 0 for value in write_nets),
                "combined_with_anchor_bytes": combined,
                "combined_share_of_full_request_percent": percent(combined / total_request),
                "projected_remaining_request_bytes": remaining,
                "projected_remaining_share_percent": percent(remaining / total_request),
                "remaining_bytes_above_decided_floor": remaining - decided_bytes,
            }
        return projection

    registered_six_savings = savings_projection("six")
    splice_lcs_savings = savings_projection("lcs")

    histogram = {}
    for pair in pairs:
        histogram[str(pair["k"])] = histogram.get(str(pair["k"]), 0) + 1

    script_bytes = Path(__file__).read_bytes()
    helper_source = Path(args.helper_source).read_bytes()
    inventory = sorted(({"bytes": item["bytes"], "sha256": item["sha256"]}
                        for item in contributing.values()), key=lambda item: item["sha256"])
    receipt = {
        "schema": "from-to-double-repetition-study.v1",
        "window": {"since": args.since, "until": args.until, "bounds": "inclusive"},
        "method": {
            "model_calls": 0,
            "unit": "UTF-8 bytes",
            "quantiles": "nearest-rank",
            "six_gram": "left-to-right greedy longest match, minimum 6 bytes, source reuse allowed",
            "lcs": "exact LCSseq plus independent affine edit-run DP",
            "rapidfuzz_version": rapidfuzz_version,
            "self_test_cases": self_test_cases,
            "script_sha256": sha256_bytes(script_bytes),
            "helper_source_sha256": sha256_bytes(helper_source),
        },
        "telemetry_inventory": {"contributing_file_count": len(inventory), "files": inventory},
        "population": {
            "tool_calls": len(calls),
            "tool_counts": tool_counts,
            "writes": len(writes),
            "writes_with_full_request": sum("request" in write for write in writes),
            "writes_with_eligible_pairs": paired_writes,
            "writes_without_eligible_pairs": len(writes) - paired_writes,
            "eligible_pairs": len(pairs),
            "pair_families": families,
            "exclusions": exclusions,
            "empty_from_pairs": sum(len(pair["from"]) == 0 for pair in pairs),
            "empty_to_pairs": sum(len(pair["to"]) == 0 for pair in pairs),
        },
        "canonical_requests": {
            "workspace_root_rule": "removed",
            "bytes": total_request,
            "sha256_of_length_prefixed_requests": sha256_bytes(b"".join(len(part).to_bytes(8, "big") + part for part in canonical_parts)),
            "write_byte_quantiles": nearest_rank(canonical_sizes),
        },
        "pair_bytes": {"from": total_from, "to": total_to, "from_plus_to": total_from + total_to},
        "overlap": {
            "lcs": metric_receipt("lcs"),
            "six_gram": metric_receipt("six"),
            "family": family_metrics,
            "double_emission": {
                "basis": "one-to-one LCS",
                "repeated_byte_units": total_lcs,
                "emitted_occurrence_bytes_anchor_plus_transform": 2 * total_lcs,
                "redundant_second_occurrence_bytes": total_lcs,
            },
        },
        "expressibility": {
            "k_quantiles": nearest_rank([pair["k"] for pair in pairs]),
            "k_histogram": dict(sorted(histogram.items(), key=lambda item: int(item[0]))),
            "identical_pairs_k_zero": sum(pair["k"] == 0 for pair in pairs),
            "overhead_basis": {
                "structure_share": 0.095,
                "pair_payload_bytes": total_from + total_to,
                "pair_count": len(pairs),
                "mean_pair_payload_bytes": round((total_from + total_to) / len(pairs), 6),
                "realistic_formula": "round_half_up(0.095 * mean_pair_payload_bytes)",
            },
            "overhead_assumptions_bytes_per_op": overheads,
            "registered_six_gram_savings_projections": registered_six_savings,
            "splice_lcs_savings_projections": splice_lcs_savings,
        },
        "combination_constants_from_prior_receipt": {
            "anchor_bytes": anchor_bytes,
            "anchor_share_of_leaf_percent": 28.5,
            "leaf_bytes": 570355,
            "anchor_share_of_full_request_percent_recomputed": percent(anchor_bytes / total_request),
            "decided_floor_bytes": decided_bytes,
            "decided_floor_share_percent": percent(decided_bytes / total_request),
            "full_request_bytes": total_request,
        },
        "invariants": {
            "call_checksum_1437_1242_195": len(calls) == 1437 and tool_counts == {"inspect_clojure": 1242, "apply_clojure_changes": 195},
            "canonical_request_checksum_630138": total_request == 630138,
            "all_writes_full_mode": all("request" in write for write in writes),
            "lcs_bounds": all(0 <= pair["lcs"] <= min(len(pair["from"]), len(pair["to"])) for pair in pairs),
            "six_gram_bounds": all(0 <= pair["six"] <= len(pair["to"]) for pair in pairs),
            "k_identity_law": all((pair["k"] == 0) == (pair["from"] == pair["to"]) for pair in pairs),
            "overlap_sum_law": total_six == sum(pair["six"] for pair in pairs),
            "all_invariants_true": False,
        },
    }
    receipt["invariants"]["all_invariants_true"] = all(value for key, value in receipt["invariants"].items() if key != "all_invariants_true")
    assert receipt["invariants"]["all_invariants_true"]
    payload_hash = sha256_bytes(compact_json(receipt))
    receipt["receipt_payload_sha256"] = payload_hash
    Path(args.output).write_bytes(json.dumps(receipt, indent=2, ensure_ascii=False, sort_keys=True).encode("utf-8") + b"\n")
    print(json.dumps({"status": "ok", "receipt_payload_sha256": payload_hash,
                      "output_bytes": Path(args.output).stat().st_size,
                      "script_sha256": receipt["method"]["script_sha256"],
                      "helper_source_sha256": receipt["method"]["helper_source_sha256"],
                      "self_test_cases": self_test_cases,
                      "all_invariants_true": True}, sort_keys=True))


if __name__ == "__main__":
    main()
