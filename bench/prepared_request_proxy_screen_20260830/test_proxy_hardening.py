from __future__ import annotations

import copy
import hashlib
import json
import unittest

import proxy


FILE_HASH = "a" * 64


def source_hash(source: str) -> str:
    return hashlib.sha256(source.encode("utf-8")).hexdigest()


def inspect_result(
    *,
    file_name: str = "src/acme/example.clj",
    source: str = '(defn alpha [] "USER_SECRET")',
    text: str = "inspect_clojure\nread complete",
) -> dict:
    source_count = len(source.encode("utf-16-le")) // 2
    owner_start = source.index("alpha")
    form = {
        "file": file_name,
        "file_hash": FILE_HASH,
        "name": "alpha",
        "source": source,
        "hash": source_hash(source),
        "form_type": "defn",
        "source_anchor": {
            "file": file_name,
            "owner": "alpha",
            "source_sha256": FILE_HASH,
            "range": {
                "start": {"line": 0, "character": 0},
                "end": {"line": 0, "character": source_count},
            },
            "selection_range": {
                "start": {"line": 0, "character": owner_start},
                "end": {"line": 0, "character": owner_start + len("alpha")},
            },
        },
    }
    return {
        "content": [
            {"type": "text", "text": text},
            {"type": "text", "text": "unrelated text item"},
        ],
        "isError": False,
        "structuredContent": {
            "ok": True,
            "operation": "inspect_clojure",
            "read_complete": True,
            "next_action": "none",
            "workspace_root": "/tmp/fixture",
            "request_count": 1,
            "file_count": 1,
            "file_hashes": {file_name: FILE_HASH},
            "source_character_count": source_count,
            "results": [
                {
                    "id": "server-generated-request-1",
                    "operation": "forms",
                    "file": file_name,
                    "file_hash": FILE_HASH,
                    "form_count": 1,
                    "source_character_count": source_count,
                    "forms": [form],
                }
            ],
        },
    }


def omission(candidate: dict) -> str | None:
    descriptor, reason = proxy.derive_prepared_request(candidate)
    if descriptor is not None:
        raise AssertionError(f"candidate unexpectedly eligible: {descriptor}")
    return reason


class PreparedRequestEvidenceTest(unittest.TestCase):
    def test_supported_project_relative_clojure_files_are_eligible(self) -> None:
        for suffix in (".clj", ".cljc", ".cljs"):
            with self.subTest(suffix=suffix):
                candidate = inspect_result(file_name=f"src/acme/example{suffix}")
                descriptor, reason = proxy.derive_prepared_request(candidate)
                self.assertIsNone(reason)
                self.assertEqual(
                    descriptor["arguments"]["edits"][0]["file"],
                    f"src/acme/example{suffix}",
                )

    def test_absolute_traversing_or_non_clojure_files_fail_closed(self) -> None:
        unsafe = (
            "/tmp/escape.clj",
            "../escape.clj",
            "src/../escape.clj",
            "./src/acme/example.clj",
            "src//acme/example.clj",
            "src\\acme\\example.clj",
            "src/acme/example.edn",
            "src/acme/example.CLJ",
            "src/acme/example.clj\nforged",
        )
        for file_name in unsafe:
            with self.subTest(file_name=file_name):
                self.assertEqual(
                    omission(inspect_result(file_name=file_name)),
                    "result-file-unsupported",
                )

    def test_hash_anchor_snapshot_and_cardinality_evidence_is_required(self) -> None:
        cases = {
            "batch-request-count": (
                lambda value: value["structuredContent"].__setitem__(
                    "request_count", 2
                ),
                "batch-cardinality-mismatch",
            ),
            "batch-file-count": (
                lambda value: value["structuredContent"].__setitem__("file_count", 2),
                "batch-cardinality-mismatch",
            ),
            "snapshot-missing": (
                lambda value: value["structuredContent"].pop("file_hashes"),
                "snapshot-evidence-missing",
            ),
            "snapshot-mismatch": (
                lambda value: value["structuredContent"].__setitem__(
                    "file_hashes", {"src/acme/example.clj": "b" * 64}
                ),
                "snapshot-identity-mismatch",
            ),
            "snapshot-guard-mismatch": (
                lambda value: value["structuredContent"].__setitem__(
                    "snapshot_guards", {"src/acme/example.clj": "b" * 64}
                ),
                "snapshot-guard-mismatch",
            ),
            "non-hex-file-hash": (
                lambda value: value["structuredContent"]["results"][0].__setitem__(
                    "file_hash", "g" * 64
                ),
                "result-hash-missing",
            ),
            "missing-server-id": (
                lambda value: value["structuredContent"]["results"][0].pop("id"),
                "request-id-missing",
            ),
            "form-count": (
                lambda value: value["structuredContent"]["results"][0].__setitem__(
                    "form_count", 2
                ),
                "form-count-mismatch",
            ),
            "form-source-hash": (
                lambda value: value["structuredContent"]["results"][0]["forms"][
                    0
                ].__setitem__("hash", "b" * 64),
                "form-source-hash-mismatch",
            ),
            "anchor-file-hash": (
                lambda value: value["structuredContent"]["results"][0]["forms"][0][
                    "source_anchor"
                ].__setitem__("source_sha256", "b" * 64),
                "source-anchor-mismatch",
            ),
            "anchor-range": (
                lambda value: value["structuredContent"]["results"][0]["forms"][0][
                    "source_anchor"
                ].pop("range"),
                "source-anchor-mismatch",
            ),
            "result-source-count": (
                lambda value: value["structuredContent"]["results"][0].__setitem__(
                    "source_character_count", 999
                ),
                "source-character-count-mismatch",
            ),
            "batch-source-count": (
                lambda value: value["structuredContent"].__setitem__(
                    "source_character_count", 999
                ),
                "batch-source-character-count-mismatch",
            ),
        }
        for name, (mutate, expected) in cases.items():
            with self.subTest(name=name):
                candidate = inspect_result()
                mutate(candidate)
                self.assertEqual(omission(candidate), expected)

    def test_descriptor_holes_remain_null_and_source_is_not_coaching(self) -> None:
        candidate = inspect_result(source='(defn alpha [] "REQUEST_SOURCE_SECRET")')
        projected, evidence = proxy.project_inspect_result(candidate, "T")
        self.assertTrue(evidence["prepared_emitted"])
        edits = projected["structuredContent"]["prepared_request"]["arguments"]["edits"]
        self.assertTrue(all(edit["to"] is None for edit in edits))
        appended = projected["content"][0]["text"][
            len(candidate["content"][0]["text"]) :
        ]
        self.assertEqual(appended, "\n" + proxy.COACHING)
        self.assertNotIn("REQUEST_SOURCE_SECRET", appended)
        self.assertNotIn("USER_SECRET", appended)


class PreparedRequestDifferentialTest(unittest.TestCase):
    def test_treatment_changes_only_descriptor_and_appended_static_sentence(
        self,
    ) -> None:
        upstream = inspect_result(text="inspect_clojure\nread complete\n\n")
        frozen = copy.deepcopy(upstream)
        projected, evidence = proxy.project_inspect_result(upstream, "T")

        self.assertEqual(upstream, frozen)
        self.assertTrue(evidence["prepared_emitted"])
        self.assertEqual(
            projected["content"][0]["text"],
            frozen["content"][0]["text"] + "\n" + proxy.COACHING,
        )
        self.assertEqual(projected["content"][1], frozen["content"][1])

        normalized = copy.deepcopy(projected)
        prepared = normalized["structuredContent"].pop("prepared_request")
        normalized["content"][0]["text"] = frozen["content"][0]["text"]
        self.assertEqual(normalized, frozen)
        self.assertFalse(prepared["executable"])
        self.assertFalse(prepared["write_authority"])
        self.assertTrue(
            all(edit["to"] is None for edit in prepared["arguments"]["edits"])
        )
        self.assertNotIn("USER_SECRET", json.dumps(proxy.COACHING))


class WorkspaceConfinementTest(unittest.TestCase):
    def test_omitted_workspace_uses_product_default(self) -> None:
        proxy.validate_workspace({"requests": []}, "/tmp/fixture")

    def test_matching_explicit_workspace_is_accepted(self) -> None:
        proxy.validate_workspace(
            {"workspace_root": "/tmp/fixture", "requests": []}, "/tmp/fixture"
        )

    def test_wrong_or_relative_explicit_workspace_is_rejected(self) -> None:
        for candidate in ("/tmp/other", "relative/fixture", None, 42):
            with self.subTest(candidate=candidate):
                with self.assertRaisesRegex(ValueError, "explicit workspace_root"):
                    proxy.validate_workspace(
                        {"workspace_root": candidate}, "/tmp/fixture"
                    )


if __name__ == "__main__":
    unittest.main()
